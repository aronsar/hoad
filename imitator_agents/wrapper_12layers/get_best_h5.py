import argparse
import numpy as np
import os
import glob
import shutil
import random

def main(dir_models='/Volumes/ext_ssd/jlab/data_imi_10games/saved_models', dir_out='best_models'):
    """ Search through all saved checkpoints for all agents and find the models
        with the best validation loss for all agents.

    Args:
        - dir_models: str
            The root directory containing all of the saved models.
        - dir_out: str
            The directory where the best models will be saved under
    """
    random.seed(1234)

    subdirs = [f for f in os.listdir(dir_models) if os.path.isdir(os.path.join(dir_models, f))]

    path_best_models = []
    for subdir in subdirs:
        trial_num = str(random.randint(0,49))

        PATH_SUB = os.path.join(dir_models, subdir, trial_num)
        PATH_BEST = os.path.join(PATH_SUB, 'ckpts/best.h5')

        # print(PATH_BEST, os.path.join(dir_out, subdir + '_best.h5'))
        shutil.copyfile(PATH_BEST, os.path.join(dir_out, subdir + '_best.h5'))

if __name__ == '__main__':
    main()
