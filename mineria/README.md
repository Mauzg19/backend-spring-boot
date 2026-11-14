# Minería de ventas para la sustentación

Prototipo académico entrenado con **datos sintéticos**. Estima unidades vendidas por producto un día adelante. No consulta pedidos operativos ni mide demanda insatisfecha. No expresa confianza ni exactitud porcentual. El nombre «demanda» identifica el módulo académico.

## Datos y resultado conservado

El ZIP recibido contenía 100 filas, cinco productos y veinte días. Se conservan su CSV y su programa sin modificar en `original/`. Los días de ventas casi nulas no se borran ni se interpretan automáticamente como cierre. La primera corrida del pipeline corregido sobre ese CSV se conserva localmente en `resultados/`; no es el modelo desplegado.

Con autorización del autor del proyecto, `generar_sinteticos.py` produce 1.825 filas: cinco productos × 365 días, del 01/09/2025 al 31/08/2026. Semilla 20261114, patrones semanales por producto, tendencia suave y ruido gaussiano del 9%. Supone operación todos los días, sin cierres, promociones, escasez ni cambios de precio. La manifestación de procedencia está en `dataset_sintetico.manifest.json`. Más filas sintéticas permiten demostrar el entrenamiento, pero no garantizan generalización a restaurantes reales.

SHA-256 del CSV empleado: `f431d83716ad361aeca71dd4ab1e2732b18e9475c33d939a770481e635abaca6`.

| Partición | Fechas | Filas |
| --- | --- | ---: |
| Historia inicial, no usada como objetivo de entrenamiento | 01–07/09/2025 | 35 |
| Entrenamiento | 08/09/2025–02/06/2026 | 1.340 |
| Validación | 03/06/2026–02/07/2026 | 150 |
| Prueba final | 03/07/2026–31/08/2026 | 300 |

Los rezagos y la media móvil usan únicamente días anteriores. Los escaladores de entradas y objetivo se ajustan solo con entrenamiento. La prueba es secuencial de un día adelante con historia observada, no un pronóstico simultáneo de sesenta días. No se eliminaron observaciones por su error.

Red Keras de 13 → 16 → 8 → 1, 369 parámetros; ReLU, Dropout 10%, L2 0,001, Adam 0,01 y lotes de ocho. Semilla 42, máximo 500 épocas, parada temprana con paciencia 30. Ejecutó 54 épocas y restauró los pesos de la época 24 usando validación. Las cinco entradas numéricas son precio, día de semana, ventas del día anterior, ventas de hace siete días y promedio de los siete días anteriores; se agregan tres indicadores binarios y cinco columnas de producto.

| Modelo | MAE (unidades) | RMSE (unidades) | R² |
| --- | ---: | ---: | ---: |
| Red neuronal | 3,1716 | 4,1231 | 0,9552 |
| Regresión lineal | 3,6158 | 4,5797 | 0,9448 |
| Media móvil de siete días | 11,1248 | 13,6446 | 0,5097 |
| Último día | 10,4967 | 14,0315 | 0,4815 |

Comparación descriptiva en la misma prueba sintética. R² no significa «95,52% de exactitud». Una mejora frente a los comparadores en este generador no prueba ventaja comercial.

## Reproducir

Desde la raíz del backend, en Windows con Python 3.12 (corrida realizada con 3.12.10). Una ruta corta para el entorno evita los límites de rutas de TensorFlow:

```powershell
py -3.12 -m venv C:/rf-ml
& C:/rf-ml/Scripts/python.exe -m pip install -r ./mineria/requirements.txt
& C:/rf-ml/Scripts/python.exe ./mineria/generar_sinteticos.py
& C:/rf-ml/Scripts/python.exe ./mineria/entrenar.py --csv ./mineria/dataset_sintetico.csv --output ./mineria/resultados-sinteticos --test-days 60 --validation-days 30
& C:/rf-ml/Scripts/python.exe -m unittest discover -s ./mineria -p test_pipeline.py -v
```

El entorno usado se congeló en `requirements-lock.txt`. Los resultados incluyen `modelo_demanda.keras`, pesos JSON, informe, predicciones de prueba y tres figuras. Las semillas y versiones permiten reproducir el procedimiento; diferencias de plataforma pueden causar pequeñas variaciones numéricas. Para una nueva corrida de investigación use otra carpeta `--output` y compare antes de sustituir los artefactos publicados.

## Integración real

Los pesos y el informe del entrenamiento seleccionado están versionados en `src/main/resources/mineria/`. Spring evalúa las tres capas densas, el escalado y la transformación inversa en Java; el servidor no necesita Python ni TensorFlow. El Dropout solo se aplica durante el entrenamiento. La diferencia máxima entre Keras y la exportación JSON fue 0,00001068 unidades.

`GET /api/admin/demand/report` devuelve el informe. `POST /api/admin/demand/predict` recibe `product`, `date`, `lag1`, `lag7` y `mean7`. Requiere JWT de `ROLE_ADMIN` o `ROLE_RESTAURANT_OWNER`; cliente y gerente reciben 403. Los valores negativos, no finitos, mayores que 10.000 o productos desconocidos se rechazan. El precio y los indicadores corresponden al catálogo académico fijo; el formulario no pronostica productos nuevos del restaurante autenticado.

React muestra el módulo en `/admin/restaurant/demand`. El ejemplo de Hamburguesa para 01/09/2026 da **45,272876 unidades**, redondeables a 45 como referencia. Cambiar la fecha no actualiza por sí solo el historial ingresado.

Para sustituir el modelo, copiar conjuntamente `model.json` y `report.json` de la corrida validada a los recursos y reconstruir el JAR. Ejecutar las pruebas Python y Maven y volver a comprobar HTTP y React. Las pruebas actuales comparan las predicciones contra el entrenamiento congelado: una nueva versión requiere revisar explícitamente ese contrato.

## Evidencia y límites

Se verificaron 29 pruebas Java, dos pruebas del pipeline, cinco predicciones HTTP/UI, permisos de cuatro roles, valores inválidos, reintento tras 503 y tres anchos de pantalla. Una alteración intencional de +10 unidades fue detectada por el recorrido E2E. Evidencia local en `../mineria-video-20261114/evidence/`.

Antes de uso real: recoger ventas con calidad y continuidad, identificar cierres y falta de inventario, separar una prueba real futura y evaluar por producto y temporada. Stripe Test Mode, teléfono físico, publicación y aceptación del profesor siguen fuera de esta validación local.
