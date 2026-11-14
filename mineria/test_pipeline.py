import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import numpy as np
import pandas as pd
from entrenar import prepare, infer

ROOT=Path(__file__).parent
class PipelineTests(unittest.TestCase):
    def test_temporal_split_and_training_scaler(self):
        raw,data,masks,products,features,X,y=prepare(ROOT/'dataset_sintetico.csv',60,30)
        model=json.loads((ROOT.parent/'src/main/resources/mineria/model.json').read_text(encoding='utf-8'))
        self.assertEqual([int(m.sum()) for m in masks],[1340,150,300])
        self.assertLess(data.loc[masks[0],'fecha'].max(),data.loc[masks[1],'fecha'].min())
        self.assertLess(data.loc[masks[1],'fecha'].max(),data.loc[masks[2],'fecha'].min())
        np.testing.assert_allclose(X[masks[0],:5].astype('float64').mean(axis=0),model['mean'],rtol=1e-6)
        # Incorrect global scaling must be detected by this independent expectation.
        self.assertFalse(np.allclose(X[:,:5].mean(axis=0),model['mean'],rtol=1e-6))
        expected=np.array([r['predicted'] for r in json.loads((ROOT.parent/'src/main/resources/mineria/report.json').read_text(encoding='utf-8'))['predictions']])
        np.testing.assert_allclose(infer(model,X[masks[2]]),expected,atol=0.0001)
        model['layers'][-1]['bias'][0]+=1
        self.assertGreater(float(np.max(abs(infer(model,X[masks[2]])-expected))),1)

    def test_rejects_leaked_lag_and_missing_date(self):
        frame=pd.read_csv(ROOT/'dataset_sintetico.csv')
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'mutant.csv'
            broken=frame.copy();broken.loc[20,'demanda_lag_1']=broken.loc[20,'unidades_vendidas']+999
            broken.to_csv(path,index=False)
            with self.assertRaisesRegex(ValueError,'Historia inconsistente'): prepare(path,60,30)
            frame.drop(index=20).to_csv(path,index=False)
            with self.assertRaisesRegex(ValueError,'Faltan fechas'):prepare(path,60,30)

if __name__=='__main__':unittest.main()
