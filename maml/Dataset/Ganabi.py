
import DataGenerator as dg
import os
import random
import numpy as np
import pickle
import multiprocessing as mp
from itertools import repeat
from PIL import Image
import tensorflow as tf
from tensorflow import keras
from pprint import pprint
import sys
import time
import re

sys.path.append('..')
mp.set_start_method('spawn', True)  # Allow Debugging MultiProcess in VSCode


def convert(bin_list):
    """ Convert a binary list into an integer.
    Arguments:
        - vec: list
            List containing 0s and 1s.
    Returns:
        - Converted integer.
    """
    return sum(x << i for i, x in enumerate(reversed(bin_list)))


def revert(x, length):
    """ Revert an integer back to a binary list.
    Arguments:
        - x: int
            An integer that is to be reverted back to a binary list.
        - length: int
            Total length of the binary list that will be returned.
            If x >= (2^length), there will be no padding.
    """
    return [int(i) for i in list(format(x, '0%db' % length))]


def sample_task_batch(labels, config):
    """
    Sample a batch for training / evaluating a task based on given class labels to be sampled from
    Shape: x_task (batch_size, height width, channel)
           y_task (batch_size, 1)
    """

    num_query_shots = 1
    num_support_shots, is_train = config[0], config[2]
    x_support, y_support, x_query, y_query = [], [], [], []
    # Sanity Check
    if len(labels) != 1:
        raise("Incorrect Agent Label Number. Should Only sample one agent per task")
    agent_label = labels[0]

    # Set train batch
    game_count = dg.DataGenerator.dataset_obj.get_game_count_by_label(
        agent_label, is_train)
    sampled_game_id = random.sample(range(game_count),
                                    num_support_shots+num_query_shots)

    sum_game_steps = 0
    task_obs, task_acts, game_steps = [], [], []
    for j, game_id in enumerate(sampled_game_id):
        sampled_obs, sampled_act = dg.DataGenerator.dataset_obj.get_game_data(
            agent_label, game_id, is_train)
        sampled_obs = np.array([revert(obs, 658) for obs in sampled_obs])
        sampled_act = np.argmax(sampled_act, axis=1)
        curr_game_steps = sampled_obs.shape[0]

        task_obs.append(sampled_obs)
        task_acts.append(sampled_act)
        game_steps.append([sum_game_steps,
                           sum_game_steps + curr_game_steps])  # step per game
        sum_game_steps += curr_game_steps

    # Ragged Tensor => Two Slow
    # Shape: [game_num][step_num]

    # start_time = time.time()
    # x_support = tf.ragged.constant(task_obs[:-num_query_shots])
    # y_support = tf.ragged.constant(task_acts[:-num_query_shots])
    # x_query = tf.ragged.constant(task_obs[-num_query_shots:])
    # y_query = tf.ragged.constant(task_acts[-num_query_shots:])
    # print(time.time() - start_time)

    # start_time = time.time()
    x_support = np.vstack(task_obs[:-num_query_shots])
    y_support = np.concatenate(task_acts[:-num_query_shots])
    step_support = np.array(game_steps[:-num_query_shots])
    x_query = np.vstack(task_obs[-num_query_shots:])
    y_query = np.concatenate(task_acts[-num_query_shots:])
    step_query = np.subtract(np.array(game_steps[-num_query_shots:]),
                             np.min(game_steps[-num_query_shots:]))
    # print(time.time() - start_time)

    # start_time = time.time()
    x_support2 = np.array(task_obs[:-num_query_shots])
    y_support2 = np.array(task_acts[:-num_query_shots])
    x_query2 = np.array(task_obs[-num_query_shots:])
    y_query2 = np.array(task_acts[-num_query_shots:])
    # print(time.time() - start_time)

    return (x_support, y_support, step_support, x_query, y_query, step_query)
    # return (x_support2, y_support2, x_query2, y_query2)


def sample_task_batch_v2(labels, config):
    """
    Sample a batch for training / evaluating a task based on given class labels to be sampled from
    Shape: x_task (batch_size, height width, channel)
           y_task (batch_size, 1)
    """

    # num_query_shots = 1
    # num_support_shots, num_batch, is_train = config[0], config[1], config[2]

    is_train = config[2]
    # Sanity Check
    if len(labels) != 1:
        raise("Incorrect Agent Label Number. Should Only sample one agent per task")

    agent_label = labels[0]

    # Set train batch
    x_support, y_support, x_query, y_query = dg.DataGenerator.dataset_obj.retrieve_games_v2(
        agent_label, is_train)

    # total_batch_size = num_batch * (num_query_shots + num_support_shots)
    # obs, act = dg.DataGenerator.dataset_obj.retrieve_games(
    #     agent_label, total_batch_size, is_train)

    # x_support = obs[:num_batch*num_support_shots]
    # y_support = act[:num_batch*num_support_shots]
    # x_query = obs[num_batch*num_support_shots:total_batch_size]
    # y_query = act[num_batch*num_support_shots:total_batch_size]

    # x_support = x_support.reshape(num_support_shots, num_batch, 658)
    # y_support = y_support.reshape(num_support_shots, num_batch)
    # x_query = x_query.reshape(num_query_shots, num_batch, 658)
    # y_query = y_query.reshape(num_query_shots, num_batch)

    return (x_support, y_support, x_query, y_query)


def read_pkl(pkl_path, config):
    print(pkl_path)

    obs_dim, act_dim, batch_size, num_support, num_query, shuffle = config
    name = re.split('/', pkl_path)[-1]
    pkl_files = os.listdir(pkl_path)
    # TODO: Handle Multiple Pickle Files in a given directory
    for pkl_file in pkl_files:
        with open(os.path.join(pkl_path, pkl_file), 'rb') as f:
            f.seek(0)
            data = pickle.load(f)
            f.close()

    data = np.array(data)
    return AgentGenerator(X=data[:, 0],
                          Y=data[:, 1],
                          name=name,
                          obs_dim=obs_dim,
                          act_dim=act_dim,
                          batch_size=batch_size,
                          num_support=num_support,
                          num_query=num_query,
                          shuffle=shuffle)


class AgentGenerator(keras.utils.Sequence):
    """Generate data from preset directory containing pickle files"""

    def __init__(self, X, Y, name, obs_dim=658, act_dim=20, batch_size=32, num_support=10,
                 num_query=1, shuffle=True):
        """
        Arguments:
            - X: np.matrix
                Single column matrix that contains the integer observations.
            - Y: np.matrix
                Matrix that contains the actions in one-hot encoded vectors.
            - batch_size: int, default 32
                Size of a training batch.
            - shuffle: boolean, default True
                Shuffle the indices after each epoch.
        """
        self.name = name
        print("########################## Init Agent {}'s Generator ##########################".format(self.name))

        assert(X.shape[0] == Y.shape[0])
        self.X = self.transform_x(X)
        self.Y = self.transform_y(Y)
        assert(self.X.shape[0] == self.Y.shape[0])

        self.obs_dim = obs_dim
        self.act_dim = act_dim

        self.num_support = num_support
        self.num_query = num_query
        self.shot_size = batch_size
        self.batch_size = batch_size * (self.num_support + self.num_query)
        self.shuffle = shuffle
        self.indices = None  # keep track of indices during training
        self.index = 0
        self.on_epoch_end()

    def transform_x(self, data):
        res = []
        for i in range(data.shape[0]):
            res.append(np.array(data[i]))

        return np.concatenate(res)

    def transform_y(self, data):
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
        X = np.expand_dims(self.X[indices], axis=1)
        Y = self.Y[indices]

        # Revert the integer observations back to binary lists
        X = np.apply_along_axis(lambda x: revert(x[0], self.obs_dim), 1, X)

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
        x_query = x_query.reshape(
            self.num_query, self.shot_size, self.obs_dim)
        y_query = y_query.reshape(self.num_query, self.shot_size)

        return x_support, y_support, x_query, y_query

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
    def __init__(self, data_dir):
        self.obs_dim = 658
        self.act_dim = 20
        self.batch_size = 64
        self.num_support = 10
        self.num_query = 1
        self.shuffle = True
        self.train_path = os.path.join(data_dir, 'ganabi/train')
        self.test_path = os.path.join(data_dir, 'ganabi/test')

        self.raw_train_labels = self._get_raw_labels(self.train_path)
        self.raw_test_labels = self._get_raw_labels(self.test_path)
        self.train_labels_len = len(self.raw_train_labels)
        self.test_labels_len = len(self.raw_test_labels)

        # Define multi-process function name, can be reused
        self.mp_func = read_pkl

        self.train_generators = self._read_agent_data(self.train_path,
                                                      self.raw_train_labels,
                                                      train=True)
        self.test_generators = self._read_agent_data(self.test_path,
                                                     self.raw_test_labels,
                                                     train=False)

        # self.train_games = [
        #     list(range(self.get_game_count_by_label(raw_label_id, True)))
        #     for raw_label_id in range(self.train_labels_len)
        # ]

        # self.test_games = [
        #     list(range(self.get_game_count_by_label(raw_label_id, False)))
        #     for raw_label_id in range(self.test_labels_len)
        # ]

    def _get_raw_labels(self, path):
        agent_names = os.listdir(path)
        return agent_names

    def _read_agent_data(self, path, labels, train=True, process_count=8):
        # Define multi-process function arguments
        agent_paths = [os.path.join(path, label_name)
                       for label_name in labels]

        # Argument wrapper
        mp_args = zip(agent_paths,
                      repeat((self.obs_dim,
                              self.act_dim,
                              self.batch_size,
                              self.num_support,
                              self.num_query,
                              self.shuffle)))

        with mp.Pool(process_count) as p:
            generators = p.starmap(self.mp_func, mp_args)

        return generators

    def reset_agent_games(self, raw_label_id, train=True):
        if train:
            self.train_games[raw_label_id] = list(
                range(self.get_game_count_by_label(raw_label_id, train)))
        else:
            self.test_games[raw_label_id] = list(
                range(self.get_game_count_by_label(raw_label_id, train)))

    def retrieve_games(self, raw_label_id, batch_size, train=True):
        curr_size = 0
        game_ids = []

        while(curr_size < batch_size):
            if train:
                if len(self.train_games[raw_label_id]) <= 0:
                    self.reset_agent_games(raw_label_id, train)

                new_game_id = random.sample(
                    self.train_games[raw_label_id], 1)[0]
                self.train_games[raw_label_id].remove(new_game_id)
            else:
                if len(self.test_games[raw_label_id]) <= 0:
                    self.reset_agent_games(raw_label_id, train)

                new_game_id = random.sample(
                    self.test_games[raw_label_id], 1)[0]
                self.test_games[raw_label_id].remove(new_game_id)

            game_ids.append(new_game_id)
            new_game_size = self.get_game_steps_by_label(
                raw_label_id, new_game_id, train)
            curr_size += new_game_size

        res_obs, res_act = [], []
        for game_id in game_ids:
            obs, act = self.get_game_data(raw_label_id, game_id, train)
            obs = np.array([revert(o, 658) for o in obs])
            act = np.argmax(act, axis=1)

            res_obs.append(obs)
            res_act.append(act)

        res_obs = np.vstack(res_obs)
        res_act = np.concatenate(res_act)

        return res_obs, res_act

    def retrieve_games_v2(self, raw_label_id, train=True):
        if train:
            return self.train_generators[raw_label_id].get_next_batch()
        else:
            return self.test_generators[raw_label_id].get_next_batch()

    def get_game_count_by_label(self, raw_label_id, train=True):
        if train:
            return len(self.train_data[raw_label_id])
        else:
            return len(self.test_data[raw_label_id])

    def get_game_steps_by_label(self, raw_label_id, game_id, train=True):
        if train:
            return len(self.train_data[raw_label_id][game_id][0])
        else:
            return len(self.test_data[raw_label_id][game_id][0])

    def get_game_data(self, raw_label_id, game_id, train=True):
        '''
        Data Layout: train_data[agent_label][game_number][0 => obs, 1 => act][int => obs, one_hot => act]
        '''
        if train:
            return np.array(self.train_data[raw_label_id][game_id][0]), np.array(self.train_data[raw_label_id][game_id][1])
        else:
            return np.array(self.test_data[raw_label_id][game_id][0]), np.array(self.test_data[raw_label_id][game_id][1])
