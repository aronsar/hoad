from mlp import *

def main():

    from tensorflow.keras.callbacks import ModelCheckpoint
    from tensorflow.keras.layers import ReLU
    # from keras.callbacks import ModelCheckpoint
    # from keras.layers import ReLU
    X, Y, mask = CV('output/rainbow_data_2_500000')

    #X, Y, mask = CV('/Volumes/ext_ssd/jlab/rainbow_data_2_500000/0/')

    n_epoch = 50
    hypers = {'lr': 0.00015,
              'batch_size': 512,
              'hl_activations': [ReLU, ReLU, ReLU],
              'hl_sizes': [1024, 512, 256],
              'decay': 0.,
              'bNorm': False,
              'dropout': True,
              'regularizer': None}

    gen_tr = DataGenerator(X[mask], Y[mask], hypers['batch_size'])
    gen_va = DataGenerator(X[~mask], Y[~mask], 1000)

    callbacks = [
        ModelCheckpoint(
            './ckpts/rainbow_best.h5', monitor='val_loss', verbose=1, save_best_only=True,
            save_weights_only=True, mode='auto', period=1)]

    m = Mlp(
        io_sizes=(glb.SIZE_OBS_VEC, glb.SIZE_ACT_VEC),
        out_activation=Softmax, loss='categorical_crossentropy',
        metrics=['accuracy'], **hypers, verbose=1)
    m.construct_model()
    m.train_model(
        gen_tr, gen_va, n_epoch=n_epoch, callbacks=callbacks,
        verbose=True, workers=4, use_mp=True, max_q_size=5)

    with open('./ckpts/rainbow_history_{}.pkl'.format(n_epoch), 'wb') as f:
        pickle.dump(m.hist.history, f)

    m.model.save('./ckpts/rainbow_model_{}.h5'.format(n_epoch)) # TODO: make it userdefined

if __name__ == '__main__':
    main()
