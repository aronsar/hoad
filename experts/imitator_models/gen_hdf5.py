import h5py
import argparse
from tensorflow.keras.utils import Progbar, HDF5Matrix

from DataGenerator import *

def save_as_hdf5(X, Y, mask, path_save, bs=1024, shuffle=False,
                 compression='gzip'):
    """ Save training and validation X and Y in hdf5.

    Arguments:
        - X: np.matrix
            Returned from CV().
        - Y: np.matrix
            Returned from CV().
        - mask: np.array
            Returned from CV().
        - path_save: str
            path/to/save/data.hdf5.
    """
    gen_tr = DataGenerator(X[mask], Y[mask], bs, shuffle)
    gen_va = DataGenerator(X[~mask], Y[~mask], bs, shuffle)

    def save(ds_X, ds_Y, gen):
        prog = Progbar(len(gen))
        cur_idx = 0
        for idx, (x, y) in enumerate(gen):
            rows = x.shape[0]
            assert(rows == y.shape[0])
            ds_X[cur_idx:(cur_idx+rows), :] = x
            ds_Y[cur_idx:(cur_idx+rows), :] = y
            cur_idx += rows
            prog.update(idx)
        print()

    with h5py.File(path_save, 'w') as f:
        ds = {}
        ds['X_tr'] = f.create_dataset('X_tr',
                                      (X[mask].shape[0], glb.SIZE_OBS_VEC),
                                      dtype='i8',
                                      chunks=True,
                                      compression=compression)
        ds['X_va'] = f.create_dataset('X_va',
                                      (X[~mask].shape[0], glb.SIZE_OBS_VEC),
                                      dtype='i8',
                                      chunks=True,
                                      compression=compression)
        ds['Y_tr'] = f.create_dataset('Y_tr',
                                      (X[mask].shape[0], Y.shape[1]),
                                      dtype='i8',
                                      chunks=True,
                                      compression=compression)
        ds['Y_va'] = f.create_dataset('Y_va',
                                      (X[~mask].shape[0], Y.shape[1]),
                                      dtype='i8',
                                      chunks=True,
                                      compression=compression)
        print('Converting training sets')
        save(ds['X_tr'], ds['Y_tr'], gen_tr)
        f.flush()
        print('Converting validation sets')
        save(ds['X_va'], ds['Y_va'], gen_va)
# X = f['X_tr']

class Gen4h5(keras.utils.Sequence):
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
        # import pdb; pdb.set_trace()
        start, end = index * self.batch_size, (index+1) * self.batch_size
        indices = self.indices[start:end].tolist()
        indices.sort()
        # Take the subsets
        X = self.X[indices]
        Y = self.Y[indices]

        return X, Y

    def on_epoch_end(self):
        """Updates indices after each epoch"""
        self.indices = np.arange(self.X.shape[0])
        if self.shuffle == True:
            np.random.shuffle(self.indices)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    msg_h = 'path/to/pickle/directory/'
    parser.add_argument('--p', type=str, help=msg_h)
    msg_h = 'path/to/output/file.hdf5'
    parser.add_argument('--o', type=str, help=msg_h)
    args = parser.parse_args()

    X, Y, mask = CV(args.p)
    save_as_hdf5(X, Y, mask, args.o, 1024, True)
