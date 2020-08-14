import multiprocessing as mp
import pickle
import random
import re
from itertools import repeat
from pprint import pprint
import time
import os
import numpy as np
import tensorflow as tf
import sys

sys.path.append('..')            # nopep8
sys.path.append('../../utils/')  # nopep8

from utils import binary_list_to_int  # nopep8
import DataGenerator as dg           # nopep8

mp.set_start_method('spawn', True)  # Allow Debugging MultiProcess in VSCode


def revert(x, length):
    """ Revert an integer back to a binary list.
    Arguments:
        - x: int
            An integer that is to be reverted back to a binary list.
        - length: int
            Total length of the binary list that will be returned.
            If x >= (2^length), there will be no padding.
    """
    return [bool(i) for i in list(format(x, '0%db' % length))]


def sample_task_batch(labels, config):
    """
    A Multi-Processing Function that returns a batch of an agent's data
    """
    # start = time.time()
    # Sanity Check
    if len(labels) != 1:
        raise("Incorrect Agent Label Number. Should Only sample one agent per task")

    agent_label = labels[0]
    is_train = config[0]

    res = dg.DataGenerator.dataset_obj.retrieve_agent_batch(
        agent_label, is_train)

    # end = time.time()
    # print("Time Spent for batching a task {}".format(end - start))
    # print("Start at {} Ends at {}".format(start, end))

    # Set train batch
    return res


def read_pkl(agent_top_level_path, config):
    """
    A Multi-Processing Function that reads an agent's pickle file and instantiate a generator
    """
    epoch_num, obs_dim, act_dim, batch_size, num_support, num_query, shuffle, preprocess = config
    name = re.split('/', agent_top_level_path)[-1]
    pkl_path = os.path.join(agent_top_level_path, epoch_num, name + '.pkl')
    
    with open(pkl_path, 'rb') as f:
        print(pkl_path)
        data = pickle.load(f)

    if preprocess:
        X, Y = data
    else:
        data = np.array(data)
        X = data[:, 0]
        Y = data[:, 1]

    del data
    return AgentGenerator(X=X,
                          Y=Y,
                          name=name,
                          epoch_num=epoch_num,
                          obs_dim=obs_dim,
                          act_dim=act_dim,
                          batch_size=batch_size,
                          num_support=num_support,
                          num_query=num_query,
                          shuffle=shuffle,
                          preprocess=preprocess)


class AgentGenerator(tf.keras.utils.Sequence):
    """Generate data from preset directory containing pickle files"""

    def __init__(self, X, Y, name, epoch_num, obs_dim=658, act_dim=20, 
                 batch_size=32, num_support=10, num_query=1, 
                 shuffle=True, preprocess=False):
        """
        Arguments:

        """
        self.epoch_num = epoch_num
        self.name = name
        self.obs_dim = obs_dim
        self.act_dim = act_dim
        self.preprocess = preprocess

        print("##### Init Agent {:>17}'s Generator #####".format(self.name))

        assert(X.shape[0] == Y.shape[0])
        if self.preprocess:
            self.X = X
            self.Y = Y
        else:
            self.X = self.transform_x(X)
            self.Y = self.transform_y(Y)
        assert(self.X.shape[0] == self.Y.shape[0])

        self.num_support = num_support
        self.num_query = num_query
        self.shot_size = batch_size
        self.batch_size = self.shot_size * (self.num_support + self.num_query)

        self.shuffle = shuffle
        self.indices = None  # keep track of indices during training
        self.index = 0
        self.on_epoch_end()

    def set_params_for_test_agent(self, test_support, test_query):
        self.num_support = test_support
        self.num_query = test_query
        self.batch_size = self.shot_size * (self.num_support + self.num_query)

    def transform_x(self, data):
        """ Convert the obs into a 1-D vector"""
        res = []
        for i in range(data.shape[0]):
            res.append(np.array(data[i]))

        res = np.concatenate(res)
        # # Convert back to 658 dim beforehand - Costly
        # if self.preprocess:
        #     res = np.expand_dims(res, axis=1)
        #     res = np.apply_along_axis(
        #         lambda x: revert(x[0], self.obs_dim), 1, res)

        return res

    def transform_y(self, data):
        """ Convert the y into a 1-D sparse vector"""
        res = []
        for i in range(data.shape[0]):
            res.append(np.argmax(np.array(data[i]), axis=1))

        return np.concatenate(res)

    def __len__(self):
        """Denotes the number of batches per epoch"""
        return int(np.floor(self.X.shape[0] / self.batch_size))

    def __getitem__(self, index):
        """Generate one batch of data"""
        # get the subset; index = 0, 1, 2, ...
        start, end = index * self.batch_size, (index+1) * self.batch_size
        indices = self.indices[start:end]
        # Take the subsets
        X = self.X[indices]
        Y = self.Y[indices]

        if self.preprocess:
            X = X.astype('float32')
        else:
            X = np.vstack([binary_list_to_int.revert(X[i], self.obs_dim)
                           for i in range(X.shape[0])])

        return X, Y

    def get_next_batch(self):
        """
        Retrieve a batch of x_support, y_support, x_query, y_query
        """
        x, y = self.__getitem__(self.index)
        self.index += 1
        self.check_epoch_end()

        x_support = x[:self.shot_size*self.num_support]
        y_support = y[:self.shot_size*self.num_support]
        x_query = x[self.shot_size*self.num_support:]
        y_query = y[self.shot_size*self.num_support:]

        x_support = x_support.reshape(
            self.num_support, self.shot_size, self.obs_dim)
        y_support = y_support.reshape(self.num_support, self.shot_size)
        x_query = x_query.reshape(self.num_query, self.shot_size, self.obs_dim)
        y_query = y_query.reshape(self.num_query, self.shot_size)

        # x_support = x_support.reshape(
        #     self.num_support, self.shot_size, 1)
        # y_support = y_support.reshape(self.num_support, self.shot_size)
        # x_query = x_query.reshape(self.num_query, self.shot_size, 1)
        # y_query = y_query.reshape(self.num_query, self.shot_size)

        return (x_support, y_support, x_query, y_query)

    def epoch_is_over(self):
        if self.index >= self.__len__() - 120:
            return True
    
    def check_epoch_end(self):
        """Calls on_epoch_end if reaches end of an epoch"""
        if self.index >= self.__len__():
            self.on_epoch_end()

    def on_epoch_end(self):
        """Updates indices after each epoch"""
        self.index = 0
        self.indices = np.arange(self.X.shape[0])
        if self.shuffle == True:
            np.random.shuffle(self.indices)


class Dataset(object):
    def __init__(self, config_obj):
        self.obs_dim = config_obj.get("obs_dim")
        self.act_dim = config_obj.get("act_dim")
        self.batch_size = config_obj.get("batch_size")
        self.train_support = config_obj.get("train_support")
        self.train_query = config_obj.get("train_query")
        self.test_support = config_obj.get("test_support")
        self.test_query = config_obj.get("test_query")
        self.shuffle = config_obj.get("shuffle")
        self.preprocess = config_obj.get("data_preprocess")
        self.task_num = config_obj.get("num_tasks")
        self.agent_path = config_obj.get("data_path")

        self.mp_func = read_pkl

        self.all_agent_names = os.listdir(self.agent_path)
        self.train_test_agent_split(
            test_agent_name=config_obj.get("test_agent"))

        self.train_labels_len = len(self.train_agent_name)
        self.test_labels_len = len(self.test_agent_name)

        self.generators = self._read_agent_data(self.all_agent_names)
        self.generators[self.test_agent_index].set_params_for_test_agent(
            self.test_support, self.test_query)

    def train_test_agent_split(self, test_agent_name):
        self.test_agent_index = self.all_agent_names.index(test_agent_name)
        self.train_agent_indices = list(range(len(self.all_agent_names)))
        self.train_agent_indices.remove(self.test_agent_index)

        self.train_agent_name = [self.all_agent_names[i]
                                 for i in self.train_agent_indices]
        self.test_agent_name = self.all_agent_names[self.test_agent_index]

    def _read_agent_data(self, agent_name, process_count=15):
        """
        Multi-Process Wrapper function that reads the pickle data files and initialize agent data generator
        """
        # Define multi-process function arguments
        agent_paths = [os.path.join(self.agent_path, name)
                       for name in agent_name]

        # Argument wrapper
        mp_args = zip(agent_paths,
                      repeat((str(0), # epoch num
                              self.obs_dim,
                              self.act_dim,
                              self.batch_size,
                              self.train_support,
                              self.train_query,
                              self.shuffle,
                              self.preprocess)))

        with mp.Pool(process_count) as p:
            return p.starmap(self.mp_func, mp_args)

    def get_agent_idx_by_name(self, agent_name, is_train):
        if is_train:
            return self.all_agent_names.index(agent_name)
        else:
            return self.test_agent_index

    def retrieve_agent_batch(self, agent_name, is_train=True):
        agent_idx = self.get_agent_idx_by_name(agent_name, is_train)

        return self.generators[agent_idx].get_next_batch()

    def sample_task(self, N, is_train=True):
        if is_train:
            return random.sample(self.train_agent_name, N)
        else:
            return [self.test_agent_name]

    def next_batch(self, is_train=True):
        # start_time = time.time()
        tasks = self.sample_task(self.task_num, is_train)

        batch = []
        for agent_name in tasks:
            agent_idx = self.get_agent_idx_by_name(agent_name, is_train)
            #print(agent_name, self.generators[agent_idx].epoch_num, self.generators[agent_idx].index, end='')
            if self.generators[agent_idx].epoch_is_over():
                next_epoch = str((int(self.generators[agent_idx].epoch_num) + 1) % 10)
                agent_top_level_path = os.path.join(self.agent_path, agent_name)
                config = (next_epoch,
                          self.obs_dim,
                          self.act_dim,
                          self.batch_size,
                          self.train_support,
                          self.train_query,
                          self.shuffle,
                          self.preprocess)

                self.generators[agent_idx] = read_pkl(agent_top_level_path, config)
            
            data = self.retrieve_agent_batch(agent_name, is_train)
            batch.append(data)

        # print(f"Time: {time.time() - start_time}")
        return batch, tasks
