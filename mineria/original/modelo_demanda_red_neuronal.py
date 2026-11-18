# =============================================================================
# MINERÍA DE DATOS II - Predicción de demanda con redes neuronales
# Librerías: TensorFlow/Keras y NumPy (+ pandas, matplotlib y scikit-learn)
# Ejecutable en Google Colab o Jupyter (cada bloque "# %%" es una celda).
# En Colab: sube "dataset_demanda.csv" al panel de archivos (o usa files.upload()).
# =============================================================================

# %% ---------------------------------------------------------------------------
# 0. LIBRERÍAS Y SEMILLAS
# -----------------------------------------------------------------------------
import os
import random
import numpy as np
import pandas as pd
import tensorflow as tf
import matplotlib.pyplot as plt

from sklearn.preprocessing import StandardScaler
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score

# Semillas para reproducibilidad (Python, NumPy y TensorFlow)
SEMILLA = 42
os.environ["PYTHONHASHSEED"] = str(SEMILLA)
random.seed(SEMILLA)
np.random.seed(SEMILLA)
tf.random.set_seed(SEMILLA)

RUTA = "dataset_demanda.csv"   # <-- cambia la ruta si es necesario
DIAS_TEST = 4                  # últimos 4 días de cada producto para prueba
EXCLUIR_DIAS_ATIPICOS = True   # True: quita días con ventas ~0 (posible fallo de registro)

# %% ---------------------------------------------------------------------------
# 1. CARGA Y EXPLORACIÓN (EDA)
# -----------------------------------------------------------------------------
df = pd.read_csv(RUTA, parse_dates=["fecha"])

print("Dimensiones:", df.shape)
print("\nTipos de datos:\n", df.dtypes)
print("\nValores nulos por columna:\n", df.isna().sum()[df.isna().sum() > 0])
print("\nColumnas constantes:",
      [c for c in df.columns if df[c].nunique() == 1])
print("\nEstadísticas de la variable objetivo:\n", df["unidades_vendidas"].describe())

# Demanda promedio por producto y por día de la semana
print("\nDemanda media por producto:\n",
      df.groupby("categoria")["unidades_vendidas"].mean().round(2))
print("\nDemanda media por día de la semana (1=lunes ... 7=domingo):\n",
      df.groupby("dia_semana")["unidades_vendidas"].mean().round(2))

# Detección de días atípicos: ventas totales del día < 20% de la mediana diaria.
# En este dataset, 2026-09-18, 19 y 20 tienen ventas ~0 en los 5 productos a la
# vez: parece un fallo de registro o un cierre, no una caída real de demanda.
total_dia = df.groupby("fecha")["unidades_vendidas"].sum()
DIAS_ATIPICOS = total_dia[total_dia < 0.2 * total_dia.median()].index
print("\nDías atípicos (ventas ~0):", [d.date() for d in DIAS_ATIPICOS])

# Gráfico: serie de tiempo por producto
plt.figure(figsize=(10, 4))
for cat, g in df.groupby("categoria"):
    plt.plot(g["fecha"], g["unidades_vendidas"], marker="o", ms=3, label=cat)
plt.title("Unidades vendidas por producto")
plt.xlabel("Fecha"); plt.ylabel("Unidades"); plt.legend(); plt.xticks(rotation=45)
plt.tight_layout(); plt.show()

# %% ---------------------------------------------------------------------------
# 2. PREPROCESAMIENTO
# -----------------------------------------------------------------------------
# 2.0 Excluir días atípicos (si está activado). Se eliminan los últimos días, así que
#     los lags de las filas restantes no se ven afectados.
if EXCLUIR_DIAS_ATIPICOS and len(DIAS_ATIPICOS) > 0:
    df = df[~df["fecha"].isin(DIAS_ATIPICOS)]
    print(f"Se excluyeron {len(DIAS_ATIPICOS)} días atípicos.")

# 2.1 Orden cronológico (clave en series de tiempo: la partición y el
#     validation_split de Keras toman el FINAL de los datos como validación).
df = df.sort_values(["fecha", "food_id"]).reset_index(drop=True)

# 2.2 Eliminar columnas constantes o redundantes.
#   - restaurant_id, disponible_catalogo, mes: constantes -> varianza 0, no aportan.
#   - food_id: redundante con 'categoria' (se usa solo una, con one-hot).
#   - fecha: se conserva aparte para particionar, no entra como variable.
fechas = df["fecha"].copy()
df = df.drop(columns=["restaurant_id", "disponible_catalogo", "mes", "food_id", "fecha"])

# 2.3 Nulos: los lags/medias no existen en los primeros días de cada producto
#     (no hay historia previa). Imputar inventaría ventas pasadas que no
#     existen, por eso se ELIMINAN esas filas (35 filas -> quedan 65).
antes = len(df)
mask_ok = df[["demanda_lag_1", "demanda_lag_7", "media_movil_7d"]].notna().all(axis=1)
df, fechas = df[mask_ok].reset_index(drop=True), fechas[mask_ok].reset_index(drop=True)
print(f"\nFilas eliminadas por nulos: {antes - len(df)} | Filas restantes: {len(df)}")

# 2.4 Variables Si/No -> 1/0
for col in ["es_vegetariano", "es_estacional", "es_fin_semana"]:
    df[col] = df[col].map({"Si": 1, "No": 0})

# 2.5 One-hot de 'categoria' (producto)
df = pd.get_dummies(df, columns=["categoria"], dtype=int)

# 2.6 Partición CRONOLÓGICA: últimos DIAS_TEST días -> prueba
fecha_corte = fechas.sort_values().unique()[-DIAS_TEST]
es_test = (fechas >= fecha_corte).values

y = df["unidades_vendidas"].values.astype("float32")
X = df.drop(columns=["unidades_vendidas"])
nombres_features = X.columns.tolist()

X_train, X_test = X[~es_test].values, X[es_test].values
y_train, y_test = y[~es_test], y[es_test]
print(f"Entrenamiento: {X_train.shape[0]} filas | Prueba: {X_test.shape[0]} filas "
      f"(desde {pd.Timestamp(fecha_corte).date()})")
print("Variables de entrada:", nombres_features)

# 2.7 Escalado: el escalador se AJUSTA solo con entrenamiento (evita fuga).
#     Se escalan únicamente las columnas numéricas continuas; las binarias
#     y el one-hot ya están en 0/1.
cols_num = ["precio_catalogo", "dia_semana", "demanda_lag_1",
            "demanda_lag_7", "media_movil_7d"]
idx_num = [nombres_features.index(c) for c in cols_num]

scaler = StandardScaler()
X_train_s, X_test_s = X_train.astype("float32").copy(), X_test.astype("float32").copy()
X_train_s[:, idx_num] = scaler.fit_transform(X_train[:, idx_num])
X_test_s[:, idx_num] = scaler.transform(X_test[:, idx_num])

# %% ---------------------------------------------------------------------------
# 3. CONSTRUCCIÓN DEL MODELO
# -----------------------------------------------------------------------------
# Arquitectura pequeña (16 -> 8 -> 1): con ~45 filas de entrenamiento, una red
# grande memorizaría los datos (sobreajuste). Se agrega:
#   - L2 en las capas ocultas (penaliza pesos grandes)
#   - Dropout (apaga neuronas al azar al entrenar)
# Salida lineal de 1 neurona porque es regresión (valor continuo >= 0).
def construir_modelo(n_entradas):
    modelo = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(n_entradas,)),
        tf.keras.layers.Dense(16, activation="relu",
                              kernel_regularizer=tf.keras.regularizers.l2(1e-3)),
        tf.keras.layers.Dropout(0.1),
        tf.keras.layers.Dense(8, activation="relu",
                              kernel_regularizer=tf.keras.regularizers.l2(1e-3)),
        tf.keras.layers.Dense(1, activation="linear"),
    ])
    modelo.compile(optimizer=tf.keras.optimizers.Adam(learning_rate=0.01),
                   loss="mse", metrics=["mae"])
    return modelo

modelo = construir_modelo(X_train_s.shape[1])
modelo.summary()

# %% ---------------------------------------------------------------------------
# 4. ENTRENAMIENTO
# -----------------------------------------------------------------------------
# validation_split=0.2 toma el último 20% del entrenamiento (sin mezclar),
# o sea los días más recientes -> validación coherente con una serie de tiempo.
# EarlyStopping detiene el entrenamiento cuando la pérdida de validación
# deja de mejorar y restaura los mejores pesos.
early_stop = tf.keras.callbacks.EarlyStopping(
    monitor="val_loss", patience=30, restore_best_weights=True)

historia = modelo.fit(
    X_train_s, y_train,
    validation_split=0.2,
    epochs=500,
    batch_size=8,
    shuffle=False,            # respeta el orden temporal
    callbacks=[early_stop],
    verbose=0)

print(f"Entrenamiento detenido en la época {len(historia.history['loss'])} "
      f"| mejor val_loss = {min(historia.history['val_loss']):.3f}")

# Curva de pérdida
plt.figure(figsize=(7, 4))
plt.plot(historia.history["loss"], label="Entrenamiento")
plt.plot(historia.history["val_loss"], label="Validación")
plt.title("Curva de pérdida (MSE)"); plt.xlabel("Época"); plt.ylabel("MSE")
plt.legend(); plt.tight_layout(); plt.show()

# %% ---------------------------------------------------------------------------
# 5. EVALUACIÓN (MAE, RMSE, R²) + MODELOS BASE
# -----------------------------------------------------------------------------
def metricas(y_real, y_pred):
    return {"MAE": mean_absolute_error(y_real, y_pred),
            "RMSE": np.sqrt(mean_squared_error(y_real, y_pred)),
            "R2": r2_score(y_real, y_pred)}

pred_train = modelo.predict(X_train_s, verbose=0).ravel()
pred_test = modelo.predict(X_test_s, verbose=0).ravel()

# Base 1: usar directamente la media móvil de 7 días como predicción
i_mm = nombres_features.index("media_movil_7d")
base_mm_train, base_mm_test = X_train[:, i_mm], X_test[:, i_mm]

# Base 2: regresión lineal con las mismas variables
reg = LinearRegression().fit(X_train_s, y_train)
reg_train, reg_test = reg.predict(X_train_s), reg.predict(X_test_s)

tabla = pd.DataFrame({
    "Red neuronal (train)": metricas(y_train, pred_train),
    "Red neuronal (test)": metricas(y_test, pred_test),
    "Media móvil 7d (test)": metricas(y_test, base_mm_test),
    "Reg. lineal (train)": metricas(y_train, reg_train),
    "Reg. lineal (test)": metricas(y_test, reg_test),
}).T.round(3)
print("\n=== COMPARACIÓN DE MODELOS ===\n", tabla)

mejor = tabla.loc[["Red neuronal (test)", "Media móvil 7d (test)",
                   "Reg. lineal (test)"], "MAE"].idxmin()
print(f"\nMenor MAE en prueba: {mejor}")

# Gráfico: reales vs predichos (test)
fig, ax = plt.subplots(1, 2, figsize=(11, 4))
ax[0].scatter(y_test, pred_test, alpha=0.8)
lim = [0, max(y_test.max(), pred_test.max()) + 1]
ax[0].plot(lim, lim, "r--", label="Predicción perfecta")
ax[0].set_xlabel("Real"); ax[0].set_ylabel("Predicho")
ax[0].set_title("Reales vs predichos (prueba)"); ax[0].legend()

ax[1].plot(y_test, marker="o", label="Real")
ax[1].plot(pred_test, marker="s", label="Red neuronal")
ax[1].plot(base_mm_test, marker="^", alpha=0.6, label="Media móvil 7d")
ax[1].set_title("Conjunto de prueba (orden cronológico)")
ax[1].set_xlabel("Observación"); ax[1].set_ylabel("Unidades"); ax[1].legend()
plt.tight_layout(); plt.show()

# %% ---------------------------------------------------------------------------
# 6. PREDICCIÓN PARA UN PRODUCTO Y DÍA NUEVO
# -----------------------------------------------------------------------------
# Ejemplo: Hamburguesa el día siguiente al último dato válido.
# Los lags se calculan con la historia real del CSV original.
raw = pd.read_csv(RUTA, parse_dates=["fecha"]).sort_values("fecha")
if EXCLUIR_DIAS_ATIPICOS:
    raw = raw[~raw["fecha"].isin(DIAS_ATIPICOS)]
producto = "Hamburguesa"
fecha_nueva = raw["fecha"].max() + pd.Timedelta(days=1)   # día siguiente al último válido
h = raw[raw["categoria"] == producto].set_index("fecha")["unidades_vendidas"]

lag_1 = h.loc[fecha_nueva - pd.Timedelta(days=1)]
lag_7 = h.loc[fecha_nueva - pd.Timedelta(days=7)]
media_7 = h.loc[fecha_nueva - pd.Timedelta(days=7): fecha_nueva - pd.Timedelta(days=1)].mean()
info = raw[raw["categoria"] == producto].iloc[0]
dia_sem = fecha_nueva.dayofweek + 1

nuevo = pd.DataFrame([{
    "precio_catalogo": info["precio_catalogo"],
    "es_vegetariano": int(info["es_vegetariano"] == "Si"),
    "es_estacional": int(info["es_estacional"] == "Si"),
    "dia_semana": dia_sem,
    "es_fin_semana": int(dia_sem >= 6),
    "demanda_lag_1": lag_1, "demanda_lag_7": lag_7, "media_movil_7d": media_7,
    **{c: int(c == f"categoria_{producto}") for c in nombres_features
       if c.startswith("categoria_")},
}])[nombres_features]

nuevo_s = nuevo.values.astype("float32")
nuevo_s[:, idx_num] = scaler.transform(nuevo.values[:, idx_num])
pred_nueva = float(modelo.predict(nuevo_s, verbose=0)[0, 0])
print(f"\nPredicción para {producto} el {fecha_nueva.date()}: "
      f"{pred_nueva:.1f} unidades (≈ {max(0, round(pred_nueva))})")

# %% ---------------------------------------------------------------------------
# 7. INTERPRETACIÓN Y LIMITACIONES
# -----------------------------------------------------------------------------
print("""
INTERPRETACIÓN
- Compara las filas 'test' de la tabla: si la red neuronal no supera a la media
  móvil o a la regresión lineal, la complejidad extra no se justifica con estos datos.
- Un R² de entrenamiento mucho mayor que el de prueba indica sobreajuste.
- La demanda depende sobre todo del producto, del día de la semana y de la
  historia reciente (lags / media móvil).

LIMITACIONES
- Muy pocos datos: 100 filas; tras quitar nulos y días atípicos quedan muy pocas para entrenar.
- Los días 18-20 de septiembre muestran ventas ~0 en todos los productos: conviene
  verificar con el negocio si fue un cierre o un error de registro.
- Un solo mes (septiembre) y un solo restaurante: no se captura estacionalidad
  anual, festivos ni promociones.
- precio_catalogo, es_vegetariano y es_estacional son constantes por producto,
  por lo que equivalen a identificar el producto.
- La prueba son solo 20 observaciones: las métricas son muy sensibles.

MEJORAS
- Ampliar la historia (meses/años) y sumar más restaurantes.
- Agregar variables externas: clima, festivos, promociones, precio variable.
- Probar validación con ventanas móviles (TimeSeriesSplit) y modelos de secuencia
  (LSTM/GRU) o árboles (Random Forest, XGBoost) cuando haya más datos.
""")
