'''
This script must be in hoad/imitator_agents/
'''

from sklearn.ensemble import ExtraTreesClassifier
from cross_validation import CV as convert_data, glb
from sklearn.metrics import accuracy_score
import numpy as np
from utils import binary_list_to_int as b2int
import argparse 

parser = argparse.ArgumentParser() 
parser.add_argument(
    '--datapath', 
    type=str, 
    default='../replay_data/iggi_data_2_500000', 
    help='The parent path at which the data is stored.') 
args = parser.parse_args()


print("Loading data...")
X, y, mask = convert_data(args.datapath)
X = np.apply_along_axis(lambda x: b2int.revert(x[0], glb.SIZE_OBS_VEC), 1, X)
y = np.argmax(y, axis=1)

X_train, y_train = X[mask, :], y[mask]
X_test, y_test = X[~mask, :], y[~mask]
extra_clf = ExtraTreesClassifier(n_estimators=50, max_leaf_nodes=16, n_jobs=1)

print("Fitting classifier to data.")
extra_clf.fit(X_train, y_train)

y_pred = extra_clf.predict(X_test)
import pdb; pdb.set_trace()
print("Test accuracy is %f: " % accuracy_score(y_test, y_pred))
