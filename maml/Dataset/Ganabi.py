
import DataGenerator as dg
import os
import random
import numpy as np
import pickle
import multiprocessing as mp
from itertools import repeat
from PIL import Image
import tensorflow as tf
import sys

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
    num_shots, num_classes, is_train = config[0], config[1], config[2]
    x_task_batch, y_task_batch = [], []

    # The for loop should only loop once for now => Each task only contains one agent
    for i, agent_label in enumerate(labels):
        # Set train batch
        game_count = dg.DataGenerator.dataset_obj.get_game_count_by_label(
            agent_label, is_train)
        sampled_game_id = random.sample(range(game_count), num_shots)
        task_obs, task_acts = [], []
        for j, game_id in enumerate(sampled_game_id):
            sampled_obs, sampled_act = dg.DataGenerator.dataset_obj.get_game_data(agent_label,
                                                                                  game_id,
                                                                                  is_train)
            sampled_obs = np.array([revert(obs, 658) for obs in sampled_obs])
            sampled_act = np.argmax(sampled_act, axis=1)

            # import pdb
            # pdb.set_trace()

            task_obs.append(sampled_obs)
            task_acts.append(sampled_act)

        x_task_batch.append(np.concatenate(task_obs))
        y_task_batch.append(np.concatenate(task_acts))

    # Convert to numpy array
    x_task_batch, y_task_batch = np.array(x_task_batch), np.array(y_task_batch)

    # Convert from (N, K, 28, 28, 1) to (K, N, 28, 28, 1)
    x_task_batch = np.swapaxes(x_task_batch, 0, 1)
    y_task_batch = np.swapaxes(y_task_batch, 0, 1)

    return x_task_batch, y_task_batch


def sample_task_batch_v2(labels, config):
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

    task_obs, task_acts = [], []
    for j, game_id in enumerate(sampled_game_id):
        sampled_obs, sampled_act = dg.DataGenerator.dataset_obj.get_game_data(
            agent_label, game_id, is_train)
        sampled_obs = np.array([revert(obs, 658) for obs in sampled_obs])
        sampled_act = np.argmax(sampled_act, axis=1)

        task_obs.append(sampled_obs)
        task_acts.append(sampled_act)

    # Ragged Tensor => Two Slow
    # Shape: [game_num][step_num]
    # x_support = tf.ragged.constant(task_obs[:-num_query_shots])
    # y_support = tf.ragged.constant(task_acts[:-num_query_shots])
    # x_query = tf.ragged.constant(task_obs[-num_query_shots:])
    # y_query = tf.ragged.constant(task_acts[-num_query_shots:])

    x_support = np.array(task_obs[:-num_query_shots])
    y_support = np.array(task_acts[:-num_query_shots])
    x_query = np.array(task_obs[-num_query_shots:])
    y_query = np.array(task_acts[-num_query_shots:])

    return x_support, y_support, x_query, y_query


def read_pkl(pkl_path, config):
    """
    Definition:
        Reads Images within the given directory / path.
        Convert the image into grayscale and resize to (28 * 28)

    Returns:
        List: [img1, img2 ....]
    """
    print(pkl_path)
    pkl_files = os.listdir(pkl_path)
    for pkl_file in pkl_files:
        with open(os.path.join(pkl_path, pkl_file), 'rb') as f:
            f.seek(0)
            pkl = pickle.load(f)
            f.close()

    return pkl


class Dataset(object):
    def __init__(self, data_dir):
        self.obs_dim = 658
        self.act_dim = 20
        self.train_path = os.path.join(data_dir, 'ganabi/train')
        self.test_path = os.path.join(data_dir, 'ganabi/test')

        self.raw_train_labels = self._get_raw_labels(self.train_path)
        self.raw_test_labels = self._get_raw_labels(self.test_path)
        self.train_labels_len = len(self.raw_train_labels)
        self.test_labels_len = len(self.raw_test_labels)

        # Define multi-process function name, can be reused
        self.mp_func = read_pkl

        self.train_data = self._read_agent_data(self.train_path,
                                                self.raw_train_labels)
        self.test_data = self._read_agent_data(self.test_path,
                                               self.raw_test_labels)

    def _get_raw_labels(self, path):
        agent_names = os.listdir(path)
        return agent_names

    def _read_agent_data(self, path, labels, process_count=8):
        # Define multi-process function arguments
        agent_paths = [os.path.join(path, label_name)
                       for label_name in labels]

        # Argument wrapper
        mp_args = zip(agent_paths,
                      repeat((self.obs_dim, self.act_dim)))

        with mp.Pool(process_count) as p:
            agent_data = p.starmap(self.mp_func, mp_args)

        return agent_data

    def get_game_count_by_label(self, raw_label_id, train=True):
        if train:
            return len(self.train_data[raw_label_id])
        else:
            return len(self.test_data[raw_label_id])

    def get_game_steps_by_label(self, raw_label_id, game_id, train=True):
        if train:
            return len(self.train_data[raw_label_id][0][game_id])
        else:
            return len(self.test_data[raw_label_id][0][game_id])

    def get_game_data(self, raw_label_id, game_id, train=True):
        '''
        Data Layout: train_data[agent_label][game_number][0 => obs, 1 => act][int => obs, one_hot => act]
        '''
        if train:
            return np.array(self.train_data[raw_label_id][game_id][0]), np.array(self.train_data[raw_label_id][game_id][1])
        else:
            return np.array(self.test_data[raw_label_id][game_id][0]), np.array(self.test_data[raw_label_id][game_id][1])
