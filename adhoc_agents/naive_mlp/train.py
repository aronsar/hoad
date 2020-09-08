import os
import numpy as np
import random
import pickle
import argparse 
from mlp import Mlp
from utils import binary_list_to_int as b2int
import tensorflow as tf
tf.compat.v1.logging.set_verbosity(tf.compat.v1.logging.FATAL)
from tensorflow.keras.layers import ReLU, Softmax

parser = argparse.ArgumentParser() 
parser.add_argument(
    '--agent', 
    type=str, 
    default='iggi', 
    help='') 
parser.add_argument(
    '--savedir', 
    type=str, 
    default='saved_models', 
    help='') 
parser.add_argument(
    '--epochs', 
    type=int, 
    default=100, 
    help='') 
parser.add_argument(
    '--datapath', 
    type=str, 
    default='/data/imitator_replay_data/replay_data', 
    help='') 
args = parser.parse_args()



def load_data(datapath, agent, num_games):
    agentpath = os.path.join(datapath, agent)
    batch = random.choice(os.listdir(agentpath))
    batchpath = os.path.join(agentpath, batch)
    gamefile = os.listdir(batchpath)[0]
    gamespath = os.path.join(batchpath, gamefile)
    with open(gamespath, "rb") as f:
        games = pickle.load(f)

    games = random.sample(games, num_games)
    X = []
    Y = []
    for game in games:
        observations, actions = game
        for obs, act in zip(observations, actions):
            X.append(b2int.revert(obs, 658))
            Y.append(act)

    X = np.array(X)
    Y = np.array(Y)
    return X, Y


if __name__ ==  '__main__':
    # given an agent name as input, load 10 games of agent data, and train train an mlp to predict action given obs

    model_savepath = os.path.join(args.savedir, args.agent)
    n_epochs = args.epochs
    hypers = {'lr': 0.00015,
              'batch_size': 128,
              'hl_activations': [ReLU, ReLU, ReLU, ReLU],
              'hl_sizes': [512,512,512,256],
              'decay': 0.,
              'bNorm': True,
              'dropout': True,
              'regularizer': None}

    X_tr, Y_tr = load_data(args.datapath, args.agent, num_games=10)
    X_val, Y_val = load_data(args.datapath, args.agent, num_games=30)

    m = Mlp(
        io_sizes=(658, 20),
        out_activation=Softmax, loss='categorical_crossentropy',
        metrics=['accuracy'], **hypers, verbose=0)
    m.construct_model()

    m.hist = m.model.fit(X_tr, Y_tr, hypers['batch_size'], args.epochs, validation_data=(X_val, Y_val), verbose=0)
    
    val_acc_str = str(m.hist.history['val_accuracy'][-1])[:6]
    print("Agent %s got val acc: %s" % (args.agent, val_acc_str))
    savepath = os.path.join(args.savedir, args.agent, val_acc_str + '.h5')
    m.model.save_weights(savepath)
