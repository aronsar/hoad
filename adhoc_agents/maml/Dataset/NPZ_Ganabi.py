import multiprocessing as mp
import pickle
import random
import re
from itertools import repeat
from pprint import pprint
from collections import ChainMap
import time
import os
import numpy as np
import tensorflow as tf
import sys
from concurrent.futures.thread import ThreadPoolExecutor
import concurrent

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


def read_npz(npz_path, config):
    """
    A Multi-Processing Function that reads an agent's pickle file and instantiate a generator
    """
    num_support, num_query, shuffle = config
    name = re.split('/', npz_path)[-1]

    return AgentGenerator(name=name,
                          npz_path=npz_path,
                          num_support=num_support,
                          num_query=num_query,
                          shuffle=shuffle)


class AgentGenerator(tf.keras.utils.Sequence):
    """Generate data from preset directory containing pickle files"""

    def __init__(self, name, npz_path, num_support=10, num_query=1, shuffle=True):
        """
        Arguments:
.
        """
        print(f"##### Init Agent {name:>17}'s Generator #####")
        self.name = name
        # List of NPZ files
        self.npz_files = sorted([os.path.join(npz_path, f)
                                 for f in os.listdir(npz_path)])[:2]

        self.num_support = num_support  # Number of Games used for training task
        self.num_query = num_query      # Number of Games used for training meta
        self.batch_size = num_support + num_query

        self.shuffle = shuffle
        self.game_indices = None  # keep track of indices during training

    def set_params_for_test_agent(self, test_support, test_query):
        self.num_support = test_support
        self.num_query = test_query
        self.batch_size = test_support + test_query

    def build(self):
        print(f"##### Build Agent {self.name:>17}'s Generator #####")

        # Cannot be serialized => Set individual after MultiProcessing Construction
        # This is a list of npz dict
        npz_data = [np.load(f) for f in self.npz_files]
        # Use Chain Map for O(1) multi dict access
        # self.data = ChainMap(*npz_data)
        # Divide Two for obs, act
        self.game_count = int(sum([len(d) for d in npz_data]) / 2)
        self.on_epoch_end()

        return ChainMap(*npz_data)

    def __len__(self):
        """Denotes the number of batches per epoch"""
        return int(np.floor((self.game_count / self.batch_size)))

    def getitem(self, data, index):
        """Generate one batch of data"""
        # start_time = time.time()
        start, end = index * self.batch_size, (index+1) * self.batch_size
        indices = self.game_indices[start:end]

        x_support = np.vstack([data[f"obs{game_id}"].astype('float32')
                               for game_id in indices[:self.num_support]])
        y_support = np.hstack([data[f"act{game_id}"]
                               for game_id in indices[:self.num_support]])
        x_query = np.vstack([data[f"obs{game_id}"].astype('float32')
                             for game_id in indices[self.num_support:]])
        y_query = np.hstack([data[f"act{game_id}"]
                             for game_id in indices[self.num_support:]])

        # print(f"Time Item: {time.time() -start_time}")

        return (x_support, y_support, x_query, y_query)

    def check_epoch_end(self, index):
        """
        Calls on_epoch_end if reaches end of an epoch
        Return:
            Int := index to be used to access __getitem__
        """
        if index >= len(self):
            self.on_epoch_end()
            index = 0

        return index

    def on_epoch_end(self):
        """Updates indices after each epoch"""
        self.game_indices = np.arange(self.game_count)
        if self.shuffle == True:
            np.random.shuffle(self.game_indices)


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
        self.task_num = config_obj.get("num_tasks")

        # self.agent_path = "/media/james/D/Ganabi/raw_npz"
        # self.agent_path = "/home/james/Coding/ganabi/james_ganabi/maml/data/ganabi/train_npz"
        self.agent_path = config_obj.get("data_path")

        # Define multi-process function name, can be reused
        self.mp_func = read_npz

        self.all_agent_names = os.listdir(self.agent_path)
        self.train_test_agent_split(
            test_agent_name=config_obj.get("test_agent"))

        self.generators = self._read_agent_data(self.all_agent_names)
        self.generators[self.test_agent_index].set_params_for_test_agent(
            self.test_support, self.test_query)
        self.generators_config = [{"step": 0, "epoch": -1}
                                  for i in range(len(self.all_agent_names))]

        self.generators_data = []
        for i in range(len(self.generators)):
            self.generators_data.append(self.generators[i].build())

    def train_test_agent_split(self, test_agent_name):
        self.test_agent_index = self.all_agent_names.index(test_agent_name)
        self.train_agent_indices = list(range(len(self.all_agent_names)))
        self.train_agent_indices.remove(self.test_agent_index)

        self.train_agent_name = [self.all_agent_names[i]
                                 for i in self.train_agent_indices]
        self.test_agent_name = self.all_agent_names[self.test_agent_index]

    def _read_agent_data(self, agent_name, process_count=15):
        """
        Multi-Process Wrapper function that reads the piclke data files and initialize agent data generator
        """
        # Define multi-process function arguments
        agent_paths = [os.path.join(self.agent_path, name)
                       for name in agent_name]

        # Argument wrapper
        mp_args = zip(agent_paths,
                      repeat((self.train_support,
                              self.train_query,
                              self.shuffle)))

        with mp.Pool(process_count) as p:
            return p.starmap(self.mp_func, mp_args)

    def get_agent_idx_by_name(self, agent_name, is_train=True):
        if is_train:
            return self.all_agent_names.index(agent_name)
        else:
            return self.test_agent_index

    def get_agent_generator_info(self, agent_name, is_train=True):
        agent_idx = self.get_agent_idx_by_name(agent_name, is_train)
        index = self.generators_config[agent_idx]["step"]
        epoch = self.generators_config[agent_idx]["epoch"]

        return agent_idx, index, epoch

    def retrieve_agent_batch(self, agent_name, is_train=True):
        agent_idx, index, epoch = self.get_agent_generator_info(
            agent_name, is_train)
        index = self.generators[agent_idx].check_epoch_end(index)
        data = self.generators_data[agent_idx]

        # Increment Epoch & Step
        if index == 0:
            self.generators_config[agent_idx]["epoch"] = epoch + 1
        self.generators_config[agent_idx]["step"] = index + 1

        return self.generators[agent_idx].getitem(data, index)

    def sample_task(self, N, is_train=True):
        if is_train:
            return random.sample(self.train_agent_name, N)
        else:
            return [self.test_agent_name]

    def get_mp_args(self, agent_names, is_train=True):
        generators = []
        indices = []
        data = []
        for agent_name in agent_names:
            agent_idx, index, epoch = self.get_agent_generator_info(
                agent_name, is_train)
            index = self.generators[agent_idx].check_epoch_end(index)

            # Increment Epoch & Step
            if index == 0:
                self.generators_config[agent_idx]["epoch"] = epoch + 1
            self.generators_config[agent_idx]["step"] = index + 1

            generators.append(self.generators[agent_idx])
            indices.append(index)
            data.append(self.generators_data[agent_idx])

        return zip(agent_names, generators, data, indices)

    def next_batch(self, is_train=True, threads=0):
        tasks = self.sample_task(self.task_num, is_train)
        if threads == 0:
            # Serial
            batch = []
            for agent_name in tasks:
                data = self.retrieve_agent_batch(agent_name, is_train)
                batch.append(data)

            return batch, tasks
        else:
            # Multi-Thread - Not faster
            mp_args = self.get_mp_args(tasks)
            batch = []
            names = []
            with ThreadPoolExecutor(max_workers=5) as executor:
                future_dict = {executor.submit(retrieve_agent_batch_mp, g, d, i): n
                               for n, g, d, i in mp_args}
                for future in concurrent.futures.as_completed(future_dict):
                    batch.append(future.result())
                    names.append(future_dict[future])

            return batch, names


def retrieve_agent_batch_mp(generator, data, index):
    return generator.getitem(data, index)
