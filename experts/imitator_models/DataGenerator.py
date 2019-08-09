import os
import logging
import pickle
import numpy as np
from tensorflow import keras

from cross_validation import *

# suppress TF warnings
logging.getLogger("tensorflow").setLevel(logging.ERROR)
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'

# Original code by Afshine Amidi & Shervine Amidi from
#   https://stanford.edu/~shervine/blog/keras-how-to-generate-data-on-the-fly
class DataGenerator(keras.utils.Sequence):
    """Generate data from preset directory containing pickle files"""
    def __init__(self, X, Y, batch_size=32, shuffle=True):
        """
        Arguments:
            - X: np.matrix
                Single column matrix that contains the integer observations.
            - Y: np.matrix
                Matrix that contains the actions in one-hot encoded vectors.
            - batch_size: int, default 32
                Size of a training batch.
            - shuffle: boolean, default True
                Shuffle the indices after each epoch.
        """
        assert(X.shape[0] == Y.shape[0])
        self.X, self.Y = X, Y
        self.batch_size = batch_size
        self.shuffle = shuffle
        self.indices = None # keep track of indices during training
        self.on_epoch_end()

    def __len__(self):
        """Denotes the number of batches per epoch"""
        return int(np.floor(self.X.shape[0] / self.batch_size))

    def __getitem__(self, index):
        """Generate one batch of data"""
        # get the subset; index = 0, 1, 2, ...
        start, end = index * self.batch_size, (index+1) * self.batch_size
        indices = self.indices[start:end]
        # Take the subsets
        X = self.X[indices]
        Y = self.Y[indices]

        # Revert the integer observations back to binary lists
        X = np.apply_along_axis(
            lambda x: b2int.revert(x[0], glb.SIZE_OBS_VEC), 1, X)

        return X, Y

    def on_epoch_end(self):
        """Updates indices after each epoch"""
        self.indices = np.arange(self.X.shape[0])
        if self.shuffle == True:
            np.random.shuffle(self.indices)

