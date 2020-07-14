import warnings
warnings.filterwarnings("ignore", category=FutureWarning)

import sys
import random
import argparse
import csv
import os
import tensorflow as tf
# import multiprocessing
from mlp import *
from gen_hdf5 import *
from tensorflow.keras.callbacks import ModelCheckpoint, CSVLogger, EarlyStopping
from tensorflow.keras.layers import ReLU

class bc:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'
    BOLD = "\033[1m"

# multiprocessing.set_start_method('spawn', force=True)

def model_exists(path_m, dir_agent, trial):
    """ Check if model exists or is corrupted.

    Model Structure should look like this:

    @path_m
      |== @dir_agent
      |     |--  0
      |     |    |-- seed.txt
      |     |    |-- training.log
      |     |    |__ ckpts
      |     |        |-- 01-0.42.h5
      |     |        |-- 02-0.49.h5
      |     |       ...
      |     |--  @trial
      |     |    |-- seed.txt
      |     |    |-- training.log
      |     |    |__ ckpts
      |     |        |-- 01-0.33.h5
      |     |        |-- 02-0.38.h5
     ...   ...      ...

    Arguments:
        - path_m: str
            Path to the root directory containing the model subdirectories.
        - dir_agent: str
            Name of the subdirectory containing the saved model.
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

    PATH_DIR_SAVE = os.path.join(path_m, dir_agent, trial)
    PATH_DIR_CKPT = os.path.join(PATH_DIR_SAVE, 'ckpts')
    PATH_LOG = os.path.join(PATH_DIR_SAVE, 'training.log')
    # PATH_BEST = os.path.join(PATH_DIR_SAVE, 'best.h5')

    # Directory does not exist or is empty
    if not os.path.exists(PATH_DIR_SAVE) or len(os.listdir(PATH_DIR_SAVE)) == 0:
        return False, 0
    else:
        # Missing any one of the files
        missing_files = (
            not os.path.exists(PATH_LOG)
            # or not os.path.exists(PATH_BEST)
            or not os.path.exists(PATH_DIR_CKPT)
            or len(os.listdir(PATH_DIR_CKPT)) == 0
        )
        if missing_files:
            # msg = 'Corruption: missing training.log, best.h5, or ckpts'
            msg = 'Corruption: missing training.log or ckpts'
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
    """ Training the 12-layer MLP model on 10 games.

    Model Structure should look like this:

    @path
      |== @dir_agent
      |     |--  0
      |     |    |-- seed.txt
      |     |    |-- training.log
      |     |    |__ ckpts
      |     |        |-- 01-0.42.h5
      |     |        |-- 02-0.49.h5
      |     |       ...
      |     |--  1
      |     |    |-- seed.txt
      |     |    |-- training.log
      |     |    |__ ckpts
      |     |        |-- 01-0.33.h5
      |     |        |-- 02-0.38.h5
     ...   ...      ...

    """
    if tf.test.is_gpu_available():
        print(bc.OKGREEN + bc.BOLD + '#'*9 + ' USING GPU ' + '#'*9 + bc.ENDC)
    else:
        print(bc.FAIL + bc.BOLD + '#'*9 + ' NOT USING GPU ' + '#'*9 + bc.ENDC)

    # Get agent name
    tokens = args.p.split('/')
    if args.p[-1] == '/':
        assert(tokens.pop() == '')
    dir_agent = '-'.join(tokens[-1].split('_')[:-3])
    print(dir_agent)

    # 10 games as the training set
    RATIO = 10 / 25000

    # run this first to avoid failing after huge overhead
    model_ok, initial_epoch = model_exists(
        args.m, dir_agent, str(args.trial_num))

    PATH_DIR_SAVE = os.path.join(args.m, dir_agent, str(args.trial_num))
    PATH_DIR_CKPT = os.path.join(PATH_DIR_SAVE, 'ckpts')
    os.makedirs(PATH_DIR_CKPT, exist_ok=True)

    PATH_SEED = os.path.join(PATH_DIR_SAVE, 'seed.txt')

    if model_ok:
        # loading in the seed used
        with open(PATH_SEED, 'r') as f:
            reader = csv.reader(f)
            cur_row = next(reader)
        seed = int(cur_row[0])
        print("seed found:", seed)
    else:
        with open(PATH_SEED, 'w+') as f:
            writer = csv.writer(f)
            seed = random.randint(0, 2**32-1)
            writer.writerow([seed])
        print("Seed created:", seed)

    random.seed(seed)


    n_epoch = args.epochs

    hl_sizes = [2048, 2048, 1024, 1024, 512, 512, 256, 256, 128, 128, 64, 64]
    hl_acts = [LeakyReLU] * len(hl_sizes)

    hypers = {'lr': 0.0001,
              'batch_size': 32,
              'hl_activations': hl_acts,
              'hl_sizes': hl_sizes,
              'decay': 0.,
              'bNorm': True,
              'dropout': False,
              'regularizer': None}

    # randomly choose one of the N 25000 data dirs
    dirs = [
        f for f in os.listdir(args.p) if os.path.isdir(os.path.join(args.p, f))]
    picked = random.choice(dirs)

    X, Y, mask = CV(os.path.join(args.p, picked), RATIO, seed=seed)
    gen_tr = DataGenerator(X[mask], Y[mask], hypers['batch_size'])

    # Use only 10 times the number of observations as validation
    n_samples = X[mask].shape[0] * 10
    idx = np.random.choice(X[~mask].shape[0], n_samples, replace=False)
    val_bs = int(n_samples * 0.1)
    gen_va = DataGenerator(X[~mask][idx, :], Y[~mask][idx, :], val_bs)

    # Callbacks: save best & latest models.
    callbacks = [
        ModelCheckpoint(
            # os.path.join(PATH_DIR_SAVE, 'best.h5'), monitor='val_loss',
            os.path.join(PATH_DIR_CKPT, 'best.h5'), monitor='val_loss',
            verbose=1, save_best_only=True, save_weights_only=True,
            mode='auto', period=1
        ),
        # ModelCheckpoint(
        #     os.path.join(PATH_DIR_CKPT, '{epoch:02d}-{val_accuracy:.2f}.h5'),
        #     monitor='val_loss', verbose=1, save_best_only=False,
        #     save_weights_only=True, mode='auto', period=1
        # ),
        # EarlyStopping(monitor='val_loss', mode='min', verbose=1, patience=5),
        CSVLogger(os.path.join(PATH_DIR_SAVE, 'training.log'), append=True)
        ]

    m = Mlp(
        io_sizes=(glb.SIZE_OBS_VEC, glb.SIZE_ACT_VEC),
        out_activation=Softmax, loss='categorical_crossentropy',
        metrics=['accuracy'], **hypers, verbose=1)

    if model_ok:
        # continue from previously saved
        msg = "Saved model found. Resuming training."
        print(bc.OKGREEN + bc.BOLD + msg + bc.ENDC)
        h5s = os.listdir(PATH_DIR_CKPT)
        h5s.sort()
        saved_h5 = os.path.join(PATH_DIR_CKPT, h5s[-1])
        m.construct_model(saved_h5, weights_only=True)
    else:
        # create new model
        msg = "{} doesn't exist or is empty. Creating new model."
        print(bc.WARNING + bc.BOLD + msg.format(PATH_DIR_SAVE) + bc.ENDC)
        os.makedirs(PATH_DIR_CKPT, exist_ok=True)
        m.construct_model()

    m.train_model(
        gen_tr, gen_va, n_epoch=n_epoch, callbacks=callbacks, verbose=False,
        workers=args.w, use_mp=True, max_q_size=args.q,
        initial_epoch=initial_epoch)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    msg_h = 'path/to/root/pickle/data/directory/ or path/to/data.hdf5'
    parser.add_argument('--p', type=str, help=msg_h)
    msg_h = ('Path to parent directory where the subdirs of models are or will '
             'be saved. If the directory exists, trianing will continue from '
             'where it was left off. Otherwise, a new directory will be created '
             'and a new training will begin.')
    parser.add_argument('--m', type=str, help=msg_h)
    msg_h = 'Number of workers. Default 2.'
    parser.add_argument('--w', type=int, default=2, help=msg_h)
    msg_h = 'Size of queue of the pipline. Deefault 3.'
    parser.add_argument('--q', type=int, default=3, help=msg_h)
    msg_h = 'Number of training epochs. Default 50'
    parser.add_argument('--epochs', type=int, default=50, help=msg_h)
    msg_h = 'Trial number. Defaule 0'
    parser.add_argument('--trial_num', type=int, default=0, help=msg_h)
    args = parser.parse_args()

    main(args)
