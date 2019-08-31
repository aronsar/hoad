import argparse

import os
# import multiprocessing
from mlp import *
from gen_hdf5 import *
from tensorflow.keras.callbacks import ModelCheckpoint
from tensorflow.keras.layers import ReLU
import h5py_cache

# multiprocessing.set_start_method('spawn', force=True)

def new(args):
    """ Creates new model since the model directory does not exist.
    """

    print('{} does not exist or is empty. Creating new model.'.format(args.p))

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
        )]

    m = Mlp(
        io_sizes=(glb.SIZE_OBS_VEC, glb.SIZE_ACT_VEC),
        out_activation=Softmax, loss='categorical_crossentropy',
        metrics=['accuracy'], **hypers, verbose=1)
    m.construct_model()
    m.train_model(
        gen_tr, gen_va, n_epoch=n_epoch, callbacks=callbacks,
        verbose=True, workers=args.w, use_mp=True, max_q_size=args.q)

    with open('./ckpts/rainbow_history_{}.pkl'.format(n_epoch), 'wb') as f:
        pickle.dump(m.hist.history, f)

    m.model.save('./ckpts/rainbow_model_{}.h5'.format(n_epoch)) # TODO: make it userdefined


def main(args):
    new(args)

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
