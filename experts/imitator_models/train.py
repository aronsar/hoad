import argparse
import csv
import os
# import multiprocessing
from mlp import *
from gen_hdf5 import *
from tensorflow.keras.callbacks import ModelCheckpoint, CSVLogger
from tensorflow.keras.layers import ReLU
import h5py_cache

# multiprocessing.set_start_method('spawn', force=True)

def model_exists(path):
    """ Check if model exists or is corrupted.
    Arguments:
        - path: str
            Path to the directory containing the model.
    Returns:
        - boolean
            True if model exists and is not corrupted.
            False if @path is empty or doesn't exist.
        - int
            What the initial epoch should be for training.
            0 if no saved model found.
            [latest epoch from saved] + 1 if found.

    Raise: ValueError if model directory is corrupted.
    """
    PATH_DIR_CKPT = os.path.join(path, 'ckpts')
    PATH_LOG = os.path.join(path, 'training.log')
    PATH_BEST = os.path.join(path, 'best.h5')

    # Directory does not exist or is empty
    if not os.path.exists(path) or len(os.listdir(path)) == 0:
        return False, 0
    else:
        # Missing any one of the files
        missing_files = (
            not os.path.exists(PATH_LOG)
            or not os.path.exists(PATH_BEST)
            or not os.path.exists(PATH_DIR_CKPT)
            or len(os.listdir(PATH_DIR_CKPT)) == 0
        )
        if missing_files:
            msg = 'Corruption: missing training.log, best.h5, or ckpts'
            raise ValueError(msg)

        # check log file
        epochs = []
        with open(PATH_LOG, 'r') as f:
            reader = csv.reader(f)
            # skip header
            next(reader)
            for row in reader:
                epochs.append(row[0])
        # Not continuously increasing or epoch doesnt start from 0
        log_corruption = (
            not all(int(b) - int(a) == 1 for a,b in zip(epochs, epochs[1:]))
            or epochs[0] != '0'
        )
        if log_corruption:
            msg = ('Corruption: training.log is not continuously increasing or '
                    'epoch doesnt start from 0')
            raise ValueError(msg)

        # check ckpts
        h5s = os.listdir(PATH_DIR_CKPT)
        h5s.sort()
        latest_epoch = int(h5s[-1].split('-')[0]) - 1
        if  latest_epoch != int(epochs[-1]):
            msg = ('Corruption: latest epoch # in trianing.log does not match '
                   'that in /ckpts.')
            raise ValueError(msg)

        return True, int(epochs[-1]) + 1

def main(args):
    # run this first to avoid failing after huge overhead
    model_ok, initial_epoch = model_exists(args.m)

    PATH_DIR_CKPT = os.path.join(args.m, 'ckpts')

    n_epoch = 50
    hypers = {'lr': 0.00015,
              'batch_size': 512,
              'hl_activations': [ReLU, ReLU, ReLU],
              'hl_sizes': [1024, 512, 256],
              'decay': 0.,
              'bNorm': False,
              'dropout': True,
              'regularizer': None}

    # checking input data format.
    if args.p.split('.')[-1] in ['hdf5', 'HDF5']:
        f = h5py_cache.File(p, 'r', chunk_cache_mem_size=1*1024**3, swmr=True)
        gen_tr = Gen4h5(f['X_tr'], f['Y_tr'], hypers['batch_size'], False)
        gen_va = Gen4h5(f['X_va'], f['Y_va'], hypers['batch_size'], False)
    else:
        X, Y, mask = CV(args.p)
        gen_tr = DataGenerator(X[mask], Y[mask], hypers['batch_size'])
        gen_va = DataGenerator(X[~mask], Y[~mask], 1000)


    os.makedirs(PATH_DIR_CKPT, exist_ok=True)

    # Callbacks: save best & latest models.
    callbacks = [
        ModelCheckpoint(
            os.path.join(args.m, 'best.h5'), monitor='val_loss', verbose=1,
            save_best_only=True,
            save_weights_only=True, mode='auto', period=1
        ),
        ModelCheckpoint(
            os.path.join(PATH_DIR_CKPT, '{epoch:02d}-{val_accuracy:.2f}.h5'),
            monitor='val_loss', verbose=1, save_best_only=False,
            save_weights_only=True, mode='auto', period=1
        ),
        CSVLogger(os.path.join(args.m, 'training.log'), append=True)
        ]

    m = Mlp(
        io_sizes=(glb.SIZE_OBS_VEC, glb.SIZE_ACT_VEC),
        out_activation=Softmax, loss='categorical_crossentropy',
        metrics=['accuracy'], **hypers, verbose=1)


    if model_ok:
        # continue from previously saved
        msg = "Saved model found. Resuming training".format(args.p)
        print(msg)
        saved_h5 = os.path.join(args.m, 'best.h5') # FIXME: TAKE LATEST
        m.construct_model(saved_h5, weights_only=True)
    else:
        # create new model
        msg = "{} doesn't exist or is empty. Creating new model.".format(args.p)
        print(msg)
        os.makedirs(PATH_DIR_CKPT, exist_ok=True)
        m.construct_model()

    m.train_model(
        gen_tr, gen_va, n_epoch=n_epoch, callbacks=callbacks, verbose=True,
        workers=args.w, use_mp=True, max_q_size=args.q,
        initial_epoch=initial_epoch)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    msg_h = 'Name of the model. This will be used for directory naming.'
    parser.add_argument('--name', type=str, help=msg_h)
    msg_h = 'path/to/root/pickle/data/directory/ or path/to/data.hdf5'
    parser.add_argument('--p', type=str, help=msg_h)
    msg_h = ('Path to directory where the models are or will be saved. '
             'If the directory exists, trianing will continue from where it was'
             ' left off. Otherwise, a new directory will be created and a new'
             ' training will begin.')
    parser.add_argument('--m', type=str, help=msg_h)
    msg_h = 'Number of workers. Default 2.'
    parser.add_argument('--w', type=int, default=2, help=msg_h)
    msg_h = 'Size of queue of the pipline. Deefault 3.'
    parser.add_argument('--q', type=int, default=3, help=msg_h)
    args = parser.parse_args()

    main(args)
