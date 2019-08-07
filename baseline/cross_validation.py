import os
import pickle
import numpy as np
import random
from datetime import datetime

# Set path
# os.chdir(os.path.dirname(os.path.abspath(__file__)))
PATH = os.path.dirname(os.path.abspath(__file__))+'/'

SIZE_OBS_VEC = 658
SIZE_ACT_VEC = 20


#FIX ME change the path of cross_validation
def CV(pkl_path=PATH+'whateverthisis.pkl',
       agent='rainbow_agent_6', save_pkl=True, shuffle=True, seed=1234):
    """ Generate [# of Games]/10 pairs of training and validation sets where
        training set in each pair has a size of 10. [# of Games] Must be a
        multiple of 10.
    Arguments:
        - pkl_path: str, default './data/Hanabi-Full_2_6_150.pkl'
            Path to the pickle file that contains the output generated from
            create_data.py. First 10 games are chosen for training, and the
            rest will be for validation.
        - agent: str, default 'rainbow_agent_6'
            Name of the agent to use.
        - save_pkl: boolean, default True
            If true, outputs will be saved under baseline/pkl/
        - shuffle: boolean, default True
            If true, 10 games are randomly chosen instead of simply choosing
            the first 10.
        - seed: int, default 1234
            Seed for shuffling. Use None to set current time as seed.
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
        - masks: list
            List of booleans masks for each training set.
            EX:
            1st training pair:   X[masks[0], :]  Y[masks[0], :]
            1st validation pair: X[~masks[0], :] Y[~masks[0], :]
        - ind: list
            List of game indices in order that appear in X & Y
        - cutoffs: list
            Cutoffs of row IDs in X & Y for each training set.
            1st train set will be X[cutoffs[0]:cutoffs[1], :].
            2nd train set will be X[cutoffs[1]:cutoffs[2], :], and so on.
    """
    with open(pkl_path, 'rb') as f:
        raw = pickle.load(f)

    lst = raw[agent] # list of info for the chosen agent
    ind = list(range(len(lst))) # game indices

    if len(ind) % 10 != 0:
        raise ValueError('Number of games must be a multiple of 10')

    if shuffle:
        random.seed(seed)
        random.shuffle(ind)

    n_pairs = int(len(ind)/10)
    print('{} pairs will be created.'.format(n_pairs))

    # Cutoffs of row IDs in X & Y for each training set.
    #   1st train set will be X[cutoffs[0]:cutoffs[1], :].
    #   2nd train set will be X[cutoffs[1]:cutoffs[2], :], and so on.
    cutoffs = [0]

    # Determine the size of X & Y and generate row IDs for training cutoffs.
    n_rows = 0
    for n, i in enumerate(ind): # [n]umber of games gone thru & [i]dx of game
        n_rows += len(lst[i][0]) # add number of rounds in this game
        # add a cutoff every 10 games
        if n % 10 == 9:
            cutoffs.append(n_rows)


    X = np.zeros([n_rows, SIZE_OBS_VEC])
    Y = np.zeros([n_rows, SIZE_ACT_VEC])

    # Insert observations and actions into X & Y according to order of `ind`
    cur_idx = 0
    for n, i in enumerate(ind):
        obs = np.matrix(lst[i][0])
        act = np.matrix(lst[i][1])
        X[cur_idx:(cur_idx + obs.shape[0]), :] = obs
        Y[cur_idx:(cur_idx + act.shape[0]), :] = act
        cur_idx += act.shape[0]
        assert(obs.shape[0] == act.shape[0])

    assert(cur_idx == X.shape[0])

    # Boolean masks. Each element contains boolean masks of the training
    #   samples of that split.
    masks = []

    for i in range(len(cutoffs) - 1):
        mask = np.full(n_rows, False)
        mask[cutoffs[i]:cutoffs[i+1]] = True
        masks.append(mask)

    if save_pkl:
        # Time-stamp for saving to avoid replacing existing file.
        ts = hex(int((datetime.now()).timestamp()))[4:]
        fn = PATH+'pkl/cvout_{}_{}_{}.pkl'.format(n_pairs, agent, ts)
        with open(fn, 'wb') as f:
            pickle.dump((X, Y, masks, ind, cutoffs), f)

    return X, Y, masks, ind, cutoffs
