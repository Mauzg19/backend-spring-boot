"""Entrenamiento temporal reproducible; CSV -> Keras -> pesos JSON para Spring.

python entrenar.py --output resultados
Conserva TODAS las ventas. No infiere cierres ni elimina ceros.
"""
import os
os.environ.setdefault('TF_CPP_MIN_LOG_LEVEL', '2')
os.environ.setdefault('TF_ENABLE_ONEDNN_OPTS', '0')
os.environ.setdefault('CUDA_VISIBLE_DEVICES', '-1')
import argparse
import hashlib
import json
from pathlib import Path
import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.preprocessing import StandardScaler
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

NUM = ['precio_catalogo', 'dia_semana', 'demanda_lag_1', 'demanda_lag_7', 'media_movil_7d']
BIN = ['es_vegetariano', 'es_estacional', 'es_fin_semana']

def prepare(path, test_days=4, validation_days=3):
    raw = pd.read_csv(path, parse_dates=['fecha']).sort_values(['fecha', 'food_id']).reset_index(drop=True)
    if raw.duplicated(['restaurant_id', 'food_id', 'fecha']).any():
        raise ValueError('Hay producto-fecha duplicados')
    if raw.restaurant_id.nunique() != 1 or raw.unidades_vendidas.isna().any():
        raise ValueError('Se requiere un restaurante y ventas completas')
    if (raw.unidades_vendidas < 0).any():
        raise ValueError('Las ventas no pueden ser negativas')
    for _, group in raw.groupby('food_id'):
        if not group.fecha.diff().dropna().eq(pd.Timedelta(days=1)).all():
            raise ValueError('Faltan fechas; no se debe confundir ausencia con ventas cero')
    # Recalcular historia con shift: nunca incluye la venta del día a predecir.
    grouped = raw.groupby('food_id').unidades_vendidas
    for col, values in {
        'demanda_lag_1': grouped.shift(1),
        'demanda_lag_7': grouped.shift(7),
        'media_movil_7d': grouped.transform(lambda s: s.shift(1).rolling(7).mean()),
    }.items():
        if not np.allclose(raw[col], values, equal_nan=True):
            raise ValueError(f'Historia inconsistente en {col}')
        raw[col] = values
    if not raw.dia_semana.eq(raw.fecha.dt.dayofweek + 1).all():
        raise ValueError('Día de semana inconsistente')
    for col in BIN:
        if not raw[col].isin(['Si', 'No']).all():
            raise ValueError(f'Valor no reconocido en {col}')
    data = raw.dropna(subset=NUM).copy()
    dates = sorted(data.fecha.unique())
    if len(dates) < test_days + validation_days + 3:
        raise ValueError('Se requieren al menos 10 fechas con historia completa')
    masks = [data.fecha < dates[-test_days-validation_days], (data.fecha >= dates[-test_days-validation_days]) & (data.fecha < dates[-test_days]), data.fecha >= dates[-test_days]]
    products = sorted(data.loc[masks[0], 'categoria'].unique())
    if not data.categoria.isin(products).all():
        raise ValueError('Producto no visto en entrenamiento')
    features = NUM + BIN + ['producto_' + p for p in products]
    X = data[NUM].copy()
    for col in BIN:
        X[col] = data[col].map({'Si': 1, 'No': 0})
    for product in products:
        X['producto_' + product] = (data.categoria == product).astype(int)
    return raw, data, masks, products, features, X.to_numpy(dtype='float32'), data.unidades_vendidas.to_numpy(dtype='float32')

def metrics(actual, pred):
    return {'MAE': float(mean_absolute_error(actual, pred)), 'RMSE': float(np.sqrt(mean_squared_error(actual, pred))), 'R2': float(r2_score(actual, pred))}

def infer(artifact, values):
    x = np.array(values, dtype='float64', copy=True)
    x[:, :len(NUM)] = (x[:, :len(NUM)] - artifact['mean']) / artifact['scale']
    for layer in artifact['layers']:
        x = x @ np.array(layer['kernel']) + layer['bias']
        if layer['activation'] == 'relu': x = np.maximum(0, x)
    return np.maximum(0, x.ravel() * artifact['targetScale'] + artifact['targetMean'])

def train(path, output, test_days=4, validation_days=3):
    output.mkdir(parents=True, exist_ok=True)
    tf.keras.utils.set_random_seed(42)
    tf.config.experimental.enable_op_determinism()
    tf.config.threading.set_intra_op_parallelism_threads(2)
    tf.config.threading.set_inter_op_parallelism_threads(2)
    raw, data, masks, products, features, X, y = prepare(path, test_days, validation_days)
    tr, va, te = [m.to_numpy() for m in masks]
    scaler = StandardScaler().fit(X[tr, :len(NUM)])
    scaled = X.copy()
    scaled[:, :len(NUM)] = scaler.transform(X[:, :len(NUM)])
    y_mean, y_scale = float(y[tr].mean()), float(y[tr].std() or 1)
    ys = (y - y_mean) / y_scale
    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(X.shape[1],)),
        tf.keras.layers.Dense(16, activation='relu', kernel_regularizer=tf.keras.regularizers.l2(1e-3)),
        tf.keras.layers.Dropout(0.1),
        tf.keras.layers.Dense(8, activation='relu', kernel_regularizer=tf.keras.regularizers.l2(1e-3)),
        tf.keras.layers.Dense(1),
    ])
    model.compile(optimizer=tf.keras.optimizers.Adam(0.01), loss='mse')
    def dataset(mask):
        ds = tf.data.Dataset.from_tensor_slices((scaled[mask], ys[mask])).batch(8)
        opts = tf.data.Options()
        opts.threading.private_threadpool_size = 1
        return ds.with_options(opts)
    early = tf.keras.callbacks.EarlyStopping(monitor='val_loss', patience=30, restore_best_weights=True)
    history = model.fit(dataset(tr), validation_data=dataset(va), epochs=500, callbacks=[early], verbose=0, shuffle=False).history
    predict = lambda values: np.maximum(0, model(values, training=False).numpy().ravel() * y_scale + y_mean)
    pred = predict(scaled)
    linear = LinearRegression().fit(scaled[tr], y[tr])
    alternatives = {'Red neuronal': pred[te], 'Media móvil 7 días': X[te, 4], 'Regresión lineal': np.maximum(0, linear.predict(scaled[te])), 'Último día': X[te, 2]}
    scores = {name: metrics(y[te], p) for name, p in alternatives.items()}
    artifact = {
        'schemaVersion': 1, 'modelId': 'demanda-mlp-v1-seed42',
        'datasetSha256': hashlib.sha256(path.read_bytes()).hexdigest(),
        'features': features, 'products': products, 'mean': scaler.mean_.tolist(), 'scale': scaler.scale_.tolist(),
        'targetMean': y_mean, 'targetScale': y_scale,
        'layers': [{'kernel': layer.get_weights()[0].tolist(), 'bias': layer.get_weights()[1].tolist(),
                    'activation': layer.activation.__name__} for layer in model.layers if isinstance(layer, tf.keras.layers.Dense)],
    }
    exported = infer(artifact, X)
    parity = float(np.max(np.abs(pred - exported)))
    if parity > 0.0001: raise AssertionError(f'Error al exportar pesos: {parity}')
    splits = {}
    for name, mask in zip(['train', 'validation', 'test'], masks):
        group = data.loc[mask]
        splits[name] = {'rows': int(mask.sum()), 'start': str(group.fecha.min().date()), 'end': str(group.fecha.max().date())}
    next_day = raw.fecha.max() + pd.Timedelta(days=1)
    examples = []
    for product in products:
        h = raw.loc[raw.categoria.eq(product)].sort_values('fecha')
        row = h.iloc[-1]
        request = {'product': product, 'date': str(next_day.date()), 'lag1': float(h.unidades_vendidas.iloc[-1]),
                   'lag7': float(h.unidades_vendidas.iloc[-7]), 'mean7': float(h.unidades_vendidas.tail(7).mean())}
        values = [float(row.precio_catalogo), next_day.dayofweek + 1, request['lag1'], request['lag7'], request['mean7'],
                  int(row.es_vegetariano == 'Si'), int(row.es_estacional == 'Si'), int(next_day.dayofweek >= 5)] + [int(p == product) for p in products]
        examples.append({'request': request, 'features': values, 'expected': float(infer(artifact, [values])[0]),
                         'price': float(row.precio_catalogo), 'vegetarian': row.es_vegetariano == 'Si', 'seasonal': row.es_estacional == 'Si'})
    artifact['examples'] = examples
    totals = raw.groupby('fecha').unidades_vendidas.sum()
    low_days = [str(d.date()) for d in totals[totals < 0.2 * totals.median()].index]
    report = {
        'modelId': artifact['modelId'], 'datasetSha256': artifact['datasetSha256'], 'rawRows': len(raw),
        'usableRows': len(data), 'warmupRows': len(raw) - len(data), 'products': products, 'splits': splits,
        'architecture': [len(features), 16, 8, 1], 'parameters': model.count_params(), 'seed': 42,
        'epochs': len(history['loss']), 'bestEpoch': int(np.argmin(history['val_loss']) + 1),
        'metrics': scores, 'trainMetrics': metrics(y[tr], pred[tr]),
        'bestTestMAE': min(scores, key=lambda k: scores[k]['MAE']),
        'lowDaysRetained': low_days, 'exportMaxError': parity,
        'history': [{'epoch': i + 1, 'train': float(a * y_scale**2), 'validation': float(b * y_scale**2)} for i, (a, b) in enumerate(zip(history['loss'], history['val_loss']))],
        'predictions': [{'date': str(row.fecha.date()), 'product': row.categoria, 'actual': float(row.unidades_vendidas),
                         'predicted': float(pred[i]), 'movingAverage': float(X[i, 4])} for i, (_, row) in enumerate(data.iterrows()) if te[i]],
        'series': [{'date': str(row.fecha.date()), 'product': row.categoria, 'units': int(row.unidades_vendidas)} for _, row in raw.iterrows()],
        'examples': examples,
        'synthetic': True,
        'scope': f'Prototipo académico con datos sintéticos, un restaurante, {raw.fecha.nunique()} días.',
        'evaluation': 'Un día adelante con historia observada hasta el día anterior. La prueba es secuencial, no un pronóstico simultáneo de todo el período.',
        'limitations': ['Se conservan los días de ventas bajas; su causa no está confirmada.',
            'Ventas simuladas no equivalen a demanda no satisfecha.', f'{int(te.sum())} observaciones de prueba sintética; sin garantía de generalización a datos reales.',
            'Las predicciones de la web usan este conjunto académico, no el historial del restaurante autenticado.',
            'El resultado es una estimación puntual, no una probabilidad ni un porcentaje de confianza.'],
    }
    for filename, obj in [('model.json', artifact), ('report.json', report)]:
        (output / filename).write_text(json.dumps(obj, ensure_ascii=False, indent=2, allow_nan=False), encoding='utf-8')
    model.save(output / 'modelo_demanda.keras')
    pd.DataFrame(report['predictions']).to_csv(output / 'predicciones_prueba.csv', index=False)
    plt.rcParams.update({'font.size': 12, 'axes.spines.top': False, 'axes.spines.right': False})
    fig, ax = plt.subplots(figsize=(11, 5))
    for p, g in raw.groupby('categoria'): ax.plot(g.fecha, g.unidades_vendidas, marker='o', ms=3, label=p)
    ax.set(title=f'Ventas sintéticas · {len(raw)} registros', ylabel='Unidades por día')
    ax.legend(ncol=3, fontsize=10); fig.autofmt_xdate(); fig.tight_layout(); fig.savefig(output / 'serie.png', dpi=160); plt.close(fig)
    fig, ax = plt.subplots(figsize=(10, 4.5))
    ax.plot([h['train'] for h in report['history']], label='Entrenamiento')
    ax.plot([h['validation'] for h in report['history']], label='Validación')
    ax.set(title='Aprendizaje y validación temporal', xlabel='Época (base 0)', ylabel='Pérdida reescalada (MSE + L2)'); ax.legend()
    fig.tight_layout(); fig.savefig(output / 'aprendizaje.png', dpi=160); plt.close(fig)
    fig, ax = plt.subplots(figsize=(10, 4.5))
    ax.barh(list(scores), [s['MAE'] for s in scores.values()], color=['#2b59d1','#a0b5eb','#ff9473','#ecda98'])
    ax.set(title='Error absoluto medio en prueba · menor es mejor', xlabel='Unidades por producto y día')
    fig.tight_layout(); fig.savefig(output / 'comparacion.png', dpi=160); plt.close(fig)
    print(json.dumps({k: report[k] for k in ['splits', 'epochs', 'bestEpoch', 'metrics', 'bestTestMAE', 'exportMaxError']}, ensure_ascii=False, indent=2))

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--csv', type=Path, default=Path(__file__).with_name('dataset_demanda.csv'))
    parser.add_argument('--output', type=Path, default=Path(__file__).with_name('resultados'))
    parser.add_argument('--test-days', type=int, default=4)
    parser.add_argument('--validation-days', type=int, default=3)
    args = parser.parse_args()
    train(args.csv, args.output, args.test_days, args.validation_days)
