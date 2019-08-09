from mlp import *

def main():
    hypers = {'lr': 0.00015,
              'batch_size': 32,
              'hl_activations': [PReLU],
              'hl_sizes': [463],
              'decay': 0.00032,
              'bNorm': False,
              'dropout': True,
              'regularizer': None}

    X, Y, mask = CV(PATH_EX_PKL)
    gen_tr = DataGenerator(X[mask], Y[mask], hypers['batch_size'])
    gen_va = DataGenerator(X[~mask], Y[~mask], 100000)

    m = Mlp(
        io_sizes=(glb.SIZE_OBS_VEC, glb.SIZE_ACT_VEC),
        out_activation=Softmax, loss='categorical_crossentropy',
        metrics=['accuracy'], **hypers, verbose=1)
    m.construct_model()
    m.train_model(gen_tr, gen_va, n_epoch=1, verbose=True)
    m.model.save('example_path.h5') # TODO: make it userdefined

if __name__ == '__main__':
    main()
