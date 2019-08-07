import csv
import sys

from datetime import datetime
from mlp import *
from tensorflow.keras.layers import ReLU, PReLU
from tensorflow.keras.activations import selu
from tensorflow import keras

SIZE_OBS_VEC = 658
SIZE_ACT_VEC = 20

#FIX ME  need the correct path of the pkl file
PATH = os.path.dirname(os.path.abspath(__file__))+'/'

#hyper-parameters for baseline
hypers = {'lr': 0.00034,
          'batch_size': 44,
          'hl_activations': ReLU,
          'hl_sizes': 366,
          'decay': 0.00037,
          'bNorm': False,
          'dropout': True,
          'regularizer': None}

def run_exp():
    #FIX ME since idk how the X and Y will be loaded
    with open(PATH, 'rb') as f:
        X, Y, masks, ind, cutoffs = pickle.load(f)

    for mask in masks:
        X_tr, Y_tr = X[mask], Y[mask]
        X_va, Y_va = X[~mask], Y[~mask]

        m = Mlp(X_tr, Y_tr, X_va, Y_va,
                io_sizes=(SIZE_OBS_VEC, SIZE_ACT_VEC),
                out_activation=Softmax, loss='categorical_crossentropy',
                metrics=['accuracy'], **hypers, verbose=0)

        m.construct_model()
        #FIX ME not sure what should be the training epochs
        m.train_model(n_epoch=100, verbose=False)
        m.save('imitator.h5') #FIX ME we might want to change the dir of file

if __name__ == '__main__':
    run_exp()
