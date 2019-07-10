from utils import parse_args
from utils import dir_utils
import gin
from subprocess import call
import random
import numpy as np
import keras
import os, glob, re

@gin_configurable
def build_model(cfg={}):
    #TODO define your model here
    return Model(inputs=, outputs=)
    
# TODO: implement/merge below
class DataGenerator(keras.utils.Sequence):
    def __init__(self, default_path, agentname):
        self.batch_size = 32
        self.shuffle = True

        self.agent_name = agentname
        self.obs_path = os.path.join(os.path.join(default_path, agentname), 'obs')
        self.act_path = os.path.join(os.path.join(default_path, agentname), 'act')
        
        # Read in all filenames
        self.filenames, self.max_steps = self.get_filenames()
        self.num_files = len(self.filenames) 
        self.on_epoch_end()

    def get_filenames(self):
        origin_path = os.getcwd()

        os.chdir(self.obs_path)

        filenames = []
        max_steps = []
        for i, file in enumerate(glob.glob('*npy') ):
            max_game_step = int(re.split('_|\.', file)[-2])

            if max_game_step > self.batch_size:
                filenames.append(file)
                max_steps.append(max_game_step)


        os.chdir(origin_path) # Avoid Incorrect Assumption In The Future
        return filenames, max_steps

    # Required
    def __len__(self):
        return self.num_files

    # Required
    def __getitem__(self, index):
        indices, filename = self.batch_sampler(index)
        obs_full = np.load(os.path.join(self.obs_path, filename)) 
        act_full = np.load(os.path.join(self.act_path, filename)) 


        obs = np.zeros((self.batch_size, np.shape(obs_full)[-1]))
        act = np.zeros((self.batch_size, np.shape(act_full)[-1]))
        for i, index in enumerate(indices):
            obs[i, :] = obs_full[index, :] 
            act[i, :] = act_full[index, :]

        if np.all(obs==0):
            raise("Bad obs {} {}".format(filename, indices))
        
        if np.all(act==0):
            raise("Bad act {} {}".format(filename, indices))

        return obs, act 
    
    '''
    This is a naive implementaion. It definately needs some more modifications
    1. Retrieve 1 npy file, which includes the data of a single game, at once
    2. Retrieve self.batch_size count of obs, act data use for training
    3. Sample "Random" "Valid" game_step indices
    '''
    def batch_sampler(self, index):        
        target_filename = self.filenames[index]
        random_game_step_indices = np.random.choice(self.max_steps[index], self.batch_size, replace=False)

        return random_game_step_indices, target_filename


    def on_epoch_end(self):
        # print("-----------On Epoch End------------------")
        tmp = list(zip(self.filenames, self.max_steps))
        if self.shuffle == True:
            random.shuffle(tmp)
        
        self.filenames, self.max_steps = zip(*tmp)