import os
import sys
import pickle
import numpy as np
import random
from datetime import datetime

# Set path TODO: remove the ones that won't be used in the end
PATH_IMI = os.path.dirname(os.path.abspath(__file__))
PATH_GANABI = os.path.dirname(os.path.dirname(PATH_IMI))
PATH_HANABI_ENV = os.path.join(PATH_GANABI, "hanabi_env")
PATH_EXPERTS = os.path.join(PATH_GANABI, 'experts')
PATH_UTILS = os.path.join(PATH_GANABI, 'utils')
sys.path.insert(0, PATH_GANABI)
sys.path.insert(0, PATH_HANABI_ENV)
sys.path.insert(0, PATH_UTILS)

import binary_list_to_int as b2int

SIZE_OBS_VEC = 658
SIZE_ACT_VEC = 20

# TODO: remove this before shipping
PATH_EX_PKL = os.path.join(PATH_GANABI,
    'output/WTFWT_data_2_1000000/0/WTFWT_2_25000.pkl')

def CV(path_pkl=PATH_EX_PKL):
    """ Convert a Pickle file of observations and actions into np matrices with
        a boolean mask indicating the training rows.

    Arguments:
        - path_pkl: str
            Path to the pickle file containing the game data.
    Returns:
      - X: np.matrix
          Matrix that contains the observations in following format:
          [[observatoin of round 1 in game 1],
           [observatoin of round 2 in game 1],
           ...
           [observatoin of round 1 in game 2],
           ...
           [observatoin of round N in game M]]
      - Y: np.matrix
          Matrix that contains the actions in following format:
          [[action of round 1 in game 1],
           [action of round 2 in game 1],
           ...
           [action of round 1 in game 2],
           ...
           [action of round N in game M]]
      - mask: np.array
          A boolean mask for the training set.
          Training and validation sets can be accessed by:
            - Training pair:   X[masks[0], :]  Y[masks[0], :]
            - Validation pair: X[~masks[0], :] Y[~masks[0], :]
    """
    with open(path_pkl, 'rb') as f:
        pkl = pickle.load(f)

    # Number of rows == total number of turns across all games
    n_rows = 0
    # for each game
    for game in range(len(pkl)):
        # add number of turns in this game
        n_rows += len(pkl[game][0])

    X = np.zeros([n_rows, SIZE_OBS_VEC])
    Y = np.zeros([n_rows, SIZE_ACT_VEC])

    cur_idx = 0
    for game in range(len(pkl)):
        print(cur_idx)
        # Revert the integer back to binary list
        obs = np.matrix([b2int.revert(i, SIZE_OBS_VEC) for i in pkl[game][0]])
        act = np.matrix(pkl[game][1])
        X[cur_idx:(cur_idx + obs.shape[0]), :] = obs
        Y[cur_idx:(cur_idx + act.shape[0]), :] = act
        cur_idx += act.shape[0]
        assert(obs.shape[0] == act.shape[0])

    mask = None

    return X, Y, mask