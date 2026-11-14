"""Escenario docente, no ventas reales. Generador fijado antes de evaluar la red."""
from pathlib import Path
import argparse
import hashlib
import json
import numpy as np
import pandas as pd

def generate(output):
    rng = np.random.default_rng(20261114)
    dates = pd.date_range('2025-09-01', periods=365, freq='D')
    catalog = [('Alitas BBQ',12000,30,False,False), ('Burrito',14000,40,False,True),
               ('Ensalada',10000,24,True,False), ('Hamburguesa',15000,55,False,False),
               ('Pizza',22000,38,False,False)]
    rows = []
    for pid, (name, price, level, vegetarian, seasonal) in enumerate(catalog, 1):
        for i, date in enumerate(dates):
            day = date.dayofweek
            # Diferencia semanal plausible y distinta para platos de fin de semana/ensalada.
            weekly = ([1.1,1.05,1.1,1.05,1.0,0.75,0.7] if vegetarian else [0.75,0.8,0.9,1.0,1.25,1.6,1.4])[day]
            slow_trend = 1 + 0.12 * i / 364
            noise = rng.normal(0, 0.09 * level)  # variación no explicada por el modelo
            units = max(0, int(round(level * weekly * slow_trend + noise)))
            rows.append(dict(restaurant_id=1, food_id=pid, fecha=date, unidades_vendidas=units,
                categoria=name, precio_catalogo=price, disponible_catalogo='Si',
                es_vegetariano='Si' if vegetarian else 'No', es_estacional='Si' if seasonal else 'No',
                dia_semana=day+1, mes=date.month, es_fin_semana='Si' if day >=5 else 'No'))
    df = pd.DataFrame(rows)
    groups = df.groupby('food_id').unidades_vendidas
    df['demanda_lag_1'] = groups.shift(1)
    df['demanda_lag_7'] = groups.shift(7)
    df['media_movil_7d'] = groups.transform(lambda x: x.shift(1).rolling(7).mean())
    output.parent.mkdir(exist_ok=True, parents=True)
    df.to_csv(output, index=False, date_format='%Y-%m-%d', float_format='%.10g')
    manifest = dict(synthetic=True, seed=20261114, rows=len(df), products=5, dates=365,
        start='2025-09-01', end='2026-08-31', sha256=hashlib.sha256(output.read_bytes()).hexdigest(),
        assumptions=['Restaurante abierto todos los días y catálogo disponible.',
            'Patrón semanal por producto, tendencia lenta del 12% y ruido gaussiano del 9% del nivel.',
            'Sin festivos, promociones, quiebres de inventario ni demanda no satisfecha.',
            'Precio y atributos fijos; no identifica efecto causal de cambiar precios.',
            'No representa un restaurante real. No garantiza desempeño en datos reales.'])
    output.with_suffix('.manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(manifest,ensure_ascii=False,indent=2))

if __name__ == '__main__':
    p=argparse.ArgumentParser(); p.add_argument('--output',type=Path,default=Path(__file__).with_name('dataset_sintetico.csv'))
    generate(p.parse_args().output)
