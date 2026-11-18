# ADR: inferencia local de un modelo académico congelado

Decisión: entrenar en Python/Keras con datos sintéticos explícitos y ejecutar los pesos exportados en Spring. React usa la API autenticada existente y el sistema de roles existente. La pestaña Flutter calcula estadísticas desde el repositorio de pedidos; no mezcla esas ventas con el conjunto de minería.

Razones: el usuario necesita una demostración sencilla, funcional y explicable. Un servicio Python adicional y aprendizaje continuo añadirían operación sin mejorar este alcance. La red tiene solo tres capas densas y ReLU, por lo que la evaluación en Java es pequeña y comprobable contra Keras. Los artefactos JSON se empaquetan juntos en el JAR. El precio y los indicadores son del catálogo académico fijo, por lo que no se presenta el resultado como pronóstico del restaurante autenticado.

Cobertura del núcleo compartido: reutiliza Spring Security/JWT, API React, roles, tema y repositorio Flutter. No crea usuarios o roles nuevos, ni una base de producción, ni una segunda fuente de pedidos. La separación explícita de datos sintéticos es necesaria para no aparentar que el modelo utiliza ventas reales.

Consecuencias: reentrenar requiere validación y reconstruir el JAR. Cambiar arquitectura, orden de entradas o activaciones requiere cambiar y probar el evaluador Java. Debe validarse con datos reales antes de decisiones de inventario. La comparación registrada favorece ligeramente a la red sobre la regresión lineal en el generador utilizado; no demuestra superioridad general.

Evidencia: `mineria/README.md`, pruebas de servicio, pipeline Python y recorrido HTTP/React local. Stripe y teléfono físico se mantienen aplazados por instrucción del usuario.
