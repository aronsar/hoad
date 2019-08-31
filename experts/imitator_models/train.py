# import multiprocessing
from mlp import *
from gen_hdf5 import *
from tensorflow.keras.callbacks import ModelCheckpoint
from tensorflow.keras.layers import ReLU
import h5py_cache

# multiprocessing.set_start_method('spawn', force=True)

def train(p = 'output/rainbow_data_2_500000', workers=2, use_mp=True, max_q_size=3):
    n_epoch = 50
    hypers = {'lr': 0.00015,
              'batch_size': 512,
              'hl_activations': [ReLU, ReLU, ReLU],
              'hl_sizes': [1024, 512, 256],
              'decay': 0.,
              'bNorm': False,
              'dropout': True,
              'regularizer': None}

    if p.split('.')[-1] in ['hdf5', 'HDF5']:
        f = h5py_cache.File(p, 'r', chunk_cache_mem_size=1*1024**3, swmr=True)
        gen_tr = Gen4h5(f['X_tr'], f['Y_tr'], hypers['batch_size'], False)
        gen_va = Gen4h5(f['X_va'], f['Y_va'], hypers['batch_size'], False)
    else:
        X, Y, mask = CV(p)
        gen_tr = DataGenerator(X[mask], Y[mask], hypers['batch_size'])
        gen_va = DataGenerator(X[~mask], Y[~mask], 1000)

    callbacks = [
        ModelCheckpoint(
            './ckpts/rainbow_best.h5', monitor='val_loss', verbose=1,
            save_best_only=True,
            save_weights_only=True, mode='auto', period=1)]

    m = Mlp(
        io_sizes=(glb.SIZE_OBS_VEC, glb.SIZE_ACT_VEC),
        out_activation=Softmax, loss='categorical_crossentropy',
        metrics=['accuracy'], **hypers, verbose=1)
    m.construct_model()
    m.train_model(
        gen_tr, gen_va, n_epoch=n_epoch,
        verbose=True, workers=workers, use_mp=use_mp, max_q_size=max_q_size)

    with open('./ckpts/rainbow_history_{}.pkl'.format(n_epoch), 'wb') as f:
        pickle.dump(m.hist.history, f)

    m.model.save('./ckpts/rainbow_model_{}.h5'.format(n_epoch)) # TODO: make it userdefined


def main():
    train()

if __name__ == '__main__':
    main()
