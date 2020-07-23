import argparse
import numpy as np
import os
import glob
import shutil

def main(dir_models='/Volumes/ext_ssd/jlab/saved_models', dir_out='best_models'):
    """ Search through all saved checkpoints for all agents and find the models
        with the best validation loss for all agents.

    Args:
        - dir_models: str
            The root directory containing all of the saved models.
        - dir_out: str
            The directory where the best models will be saved under
    """
    subdirs = os.listdir(dir_models)

    path_best_models = []
    for subdir in subdirs:
        PATH_SUB = os.path.join(dir_models, subdir)
        PATH_LOG = os.path.join(PATH_SUB, 'training.log')
        PATH_CKPTS = os.path.join(PATH_SUB, 'ckpts')

        mtx = np.genfromtxt(PATH_LOG, delimiter=',')[1:,:] # ignore heading
        best_epoch = np.argmin(mtx[:, 4]) + 1 # min loss. ckpts indexed from 1
        best_h5 = '{:02d}-*.h5'.format(best_epoch)
        best_h5 = os.path.join(PATH_CKPTS, best_h5)
        best_h5 = glob.glob(best_h5)[0]
        path_best_models.append(best_h5)

    for path in path_best_models:
        name = path.split('/')[-3].split('.')[0]
        epoch = path.split('/')[-1].split('-')[0] + '.h5'
        out = '-'.join([name, epoch])
        shutil.copyfile(path, os.path.join(dir_out, out))
        # print(path, os.path.join(dir_out, out))


if __name__ == '__main__':
    main()
