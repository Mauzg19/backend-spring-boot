"""Selección exclusivamente por validación temporal, conservando las 13 entradas."""
import argparse
import json
import hashlib
from pathlib import Path

from entrenar import prepare, infer, metrics, NUM, tf, np, pd, plt
from sklearn.preprocessing import StandardScaler

ROOT = Path(__file__).resolve().parent
CONFIGS = [
    dict(name='relu32_mse', widths=[32, 16], lr=0.003, l2=0.0001, loss='mse'),
    dict(name='relu32_huber', widths=[32, 16], lr=0.003, l2=0.0001, loss='huber'),
    dict(name='relu64_mse', widths=[64, 32], lr=0.001, l2=0.0001, loss='mse'),
    dict(name='relu32_regularizada', widths=[32, 16], lr=0.003, l2=0.001, loss='mse'),
]


def run(csv, output):
    output.mkdir(parents=True, exist_ok=False)
    tf.config.threading.set_intra_op_parallelism_threads(2)
    tf.config.threading.set_inter_op_parallelism_threads(2)
    tf.config.experimental.enable_op_determinism()
    raw, data, masks, products, features, X, y = prepare(csv, 60, 30)
    tr, va, te = [m.to_numpy() for m in masks]
    scaler = StandardScaler().fit(X[tr, :len(NUM)])
    scaled = X.copy()
    scaled[:, :len(NUM)] = scaler.transform(X[:, :len(NUM)])
    target_mean, target_scale = float(y[tr].mean()), float(y[tr].std())
    ys = (y - target_mean) / target_scale

    def dataset(mask):
        ds = tf.data.Dataset.from_tensor_slices((scaled[mask], ys[mask])).batch(32)
        opts = tf.data.Options()
        opts.threading.private_threadpool_size = 1
        return ds.with_options(opts)

    baseline = json.loads((ROOT / 'referencia-v1/model.json').read_text(encoding='utf-8'))
    trials = [dict(name='modelo_anterior', validation=metrics(y[va], infer(baseline, X[va])))]
    best_mae = trials[0]['validation']['MAE']
    best_model = None
    best_config = None
    best_history = None
    for config in CONFIGS:
        tf.keras.backend.clear_session()
        tf.keras.utils.set_random_seed(42)
        model = tf.keras.Sequential([
            tf.keras.layers.Input(shape=(len(features),)),
            *[tf.keras.layers.Dense(n, activation='relu', kernel_regularizer=tf.keras.regularizers.l2(config['l2'])) for n in config['widths']],
            tf.keras.layers.Dense(1),
        ])
        model.compile(optimizer=tf.keras.optimizers.Adam(config['lr']), loss=config['loss'], metrics=['mae'])
        early = tf.keras.callbacks.EarlyStopping(monitor='val_mae', patience=45, restore_best_weights=True)
        history = model.fit(dataset(tr), validation_data=dataset(va), epochs=500, callbacks=[early], verbose=0, shuffle=False).history
        p = np.maximum(0, model(scaled[va], training=False).numpy().ravel() * target_scale + target_mean)
        score = metrics(y[va], p)
        trial = dict(**config, seed=42, epochs=len(history['loss']), bestEpoch=int(np.argmin(history['val_mae']) + 1), validation=score)
        trials.append(trial)
        print(json.dumps(trial), flush=True)
        if score['MAE'] < best_mae:
            best_mae = score['MAE']
            best_model, best_config, best_history = model, trial, history
            model.save(output / 'modelo_demanda.keras')

    decision = dict(selection='Menor MAE en validación; 4 configuraciones fijadas antes de evaluar.', selected=best_config or trials[0], trials=trials)
    (output / 'seleccion_validacion.json').write_text(json.dumps(decision, indent=2, ensure_ascii=False), encoding='utf-8')
    if best_model is None:
        raise RuntimeError('Ningún candidato mejoró la validación. Conservar el modelo anterior.')
    artifact = dict(baseline)
    artifact.update(modelId='demanda-mlp-v2-seed42', datasetSha256=hashlib.sha256(csv.read_bytes()).hexdigest(),
                    mean=scaler.mean_.tolist(), scale=scaler.scale_.tolist(), targetMean=target_mean, targetScale=target_scale,
                    layers=[dict(kernel=layer.get_weights()[0].tolist(), bias=layer.get_weights()[1].tolist(), activation=layer.activation.__name__) for layer in best_model.layers])
    for example in artifact['examples']:
        example['expected'] = float(infer(artifact, [example['features']])[0])
    pred = np.maximum(0, best_model(scaled, training=False).numpy().ravel() * target_scale + target_mean)
    exported = infer(artifact, X)
    parity = float(np.max(abs(exported - pred)))
    assert parity < 0.0001, parity
    # El modelo ya fue seleccionado. La prueba no interviene en la decisión.
    report = json.loads((ROOT / 'referencia-v1/report.json').read_text(encoding='utf-8'))
    old_test = metrics(y[te], infer(baseline, X[te]))
    new_test = metrics(y[te], pred[te])
    report.update(modelId=artifact['modelId'], architecture=[len(features), *best_config['widths'], 1],
                  parameters=best_model.count_params(), epochs=best_config['epochs'], bestEpoch=best_config['bestEpoch'],
                  validationMetrics=metrics(y[va], pred[va]), trainMetrics=metrics(y[tr], pred[tr]),
                  exportMaxError=parity, examples=artifact['examples'], selection=decision,
                  comparison=dict(previous=old_test, candidate=new_test, maeReductionPercent=100 * (old_test['MAE'] - new_test['MAE']) / old_test['MAE']),
                  evaluation='Misma prueba temporal sintética previamente usada por v1. Comparación retrospectiva de 60 días, un día adelante con historia observada. No es una nueva prueba ciega.',
                  history=[dict(epoch=i+1, train=float(a*target_scale), validation=float(b*target_scale)) for i,(a,b) in enumerate(zip(best_history['mae'],best_history['val_mae']))])
    report['metrics']['Red neuronal anterior'] = old_test
    report['metrics']['Red neuronal'] = new_test
    report['historyMetric'] = 'MAE'
    report['training'] = dict(dropout=0, l2=best_config['l2'], learningRate=best_config['lr'], batchSize=32, loss=best_config['loss'], selectionMetric='MAE de validación')
    report['bestTestMAE'] = min(report['metrics'], key=lambda name: report['metrics'][name]['MAE'])
    report['limitations'] = [
        'Datos enteramente sintéticos, un restaurante y cinco productos de catálogo fijo.',
        'Evaluación retrospectiva sobre la misma prueba previamente utilizada por v1; requiere otra prueba futura independiente.',
        'El formulario usa historial ingresado y no consulta automáticamente pedidos del restaurante.',
        'El resultado es una estimación de ventas y no mide demanda insatisfecha ni confianza estadística.',
        'El modelo requiere reentrenamiento explícito y validación con datos reales antes de decisiones operativas.',
    ]
    report['predictions'] = [dict(date=str(row.fecha.date()), product=row.categoria, actual=float(row.unidades_vendidas), predicted=float(pred[i]), movingAverage=float(X[i,4])) for i,(_,row) in enumerate(data.iterrows()) if te[i]]
    report['series'] = [dict(date=str(row.fecha.date()), product=row.categoria, units=int(row.unidades_vendidas)) for _,row in raw.iterrows()]
    for name,obj in [('model.json',artifact),('report.json',report)]:
        (output/name).write_text(json.dumps(obj,indent=2,ensure_ascii=False,allow_nan=False),encoding='utf-8')
    pd.DataFrame(report['predictions']).to_csv(output/'predicciones_prueba.csv',index=False)
    frame=pd.DataFrame(report['predictions'])
    per_product={p:metrics(g.actual,g.predicted) for p,g in frame.groupby('product')}
    (output/'metricas_por_producto.json').write_text(json.dumps(per_product,indent=2,ensure_ascii=False),encoding='utf-8')
    fig,ax=plt.subplots(figsize=(9,4.5))
    scores=report['metrics']
    ax.barh(list(scores),[v['MAE'] for v in scores.values()],color='#2863af')
    ax.set(xlabel='MAE en unidades por producto y día',title='Comparación en la misma prueba sintética')
    fig.tight_layout();fig.savefig(output/'comparacion.png',dpi=160);plt.close(fig)
    fig,ax=plt.subplots(figsize=(9,4.5))
    ax.plot([r['epoch'] for r in report['history']],[r['train'] for r in report['history']],label='Entrenamiento')
    ax.plot([r['epoch'] for r in report['history']],[r['validation'] for r in report['history']],label='Validación')
    ax.set(xlabel='Época',ylabel='MAE en unidades',title='Evolución del entrenamiento')
    ax.legend();fig.tight_layout();fig.savefig(output/'aprendizaje.png',dpi=160);plt.close(fig)
    print(json.dumps(report['comparison'],indent=2),flush=True)


if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--csv',type=Path,default=ROOT/'dataset_sintetico.csv')
    parser.add_argument('--output',type=Path,default=ROOT/'resultados-v2')
    args=parser.parse_args()
    run(args.csv,args.output)
