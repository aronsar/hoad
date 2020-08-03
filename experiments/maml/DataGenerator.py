"""
Name: maml.py

Usage:
    A data_generator design specifically for MAML

Author: Chu-Hung Cheng
"""
import multiprocessing as mp
import tensorflow.keras as tk
import numpy as np
import os
import random
import time
from itertools import repeat

from Dataset import PKL_Ganabi, Omniglot


def _mp_batching(mp_func, task_ids_list, config, process_count=4):
    """
    Definition:
        Multi-Proceesing support for batching
    Return:
        train_batch, eval_batch
    """
    batch = []
    mp_args = zip(task_ids_list, repeat(config))
    # start = time.time()
    # print("Start Mp : {}".format(start))
    with mp.Pool(process_count) as p:
        batch = p.starmap(mp_func, mp_args)
    # end = time.time()
    # print("End Mp : {} Diff : {}".format(end, end - start))

    return batch


def _loop_batching(func, task_ids_list, config):
    """
    Definition:
        Loop batching. Faster when batching is a light task
    Return:
        train_batch, eval_batch
    """
    batch = []
    for task in range(len(task_ids_list)):
        task_ids = task_ids_list[task]

        data = func(task_ids, config)
        batch.append(data)

    return batch


def _sample_task(N, is_train=True):
    """
    Definition:
        Sample a task of a K-way of label/class/category combination.
        Not rly an "unique task id", but more like a "unique list of label ids"
    Return:
        List: [label_1, label_2 ....]
    """

    def validation():
        if (is_train and N > DataGenerator.dataset_obj.train_labels_len) or (not is_train and N > DataGenerator.dataset_obj.test_labels_len):
            raise("Sample categories number exceeds avaiable labels")

    validation()

    if is_train:
        return random.sample(range(DataGenerator.dataset_obj.train_labels_len),
                             N)
    else:
        return random.sample(range(DataGenerator.dataset_obj.test_labels_len),
                             N)


class DataGenerator(object):
    # Static Variables for Multi-Processing Support
    dataset_obj = None

    def __init__(self, config_obj):
        # num_classes = K way
        self.num_classes = config_obj.get("num_classes")
        self.num_shots = config_obj.get("train_support")
        self.num_tasks = config_obj.get("num_tasks")
        self.num_process = config_obj.get("num_process")

        # Retrieve a dataset used for setting static variables
        data_dir = config_obj.get("data_dir")
        self.dataset_name = config_obj.get("dataset")

        if self.dataset_name == 'omniglot':
            DataGenerator.dataset_obj = Omniglot.Dataset(data_dir)
            self.train_config = (self.num_shots, self.num_classes, True)
            self.eval_config = (self.num_shots, self.num_classes, False)
            self.mp_func = Omniglot.sample_task_batch_v2
        elif self.dataset_name == 'ganabi':
            DataGenerator.dataset_obj = Ganabi.Dataset(config_obj, data_dir)
            self.batch_size = config_obj.get("batch_size")
            self.train_config = (True, 1)
            self.eval_config = (False, 1)
            self.mp_func = Ganabi.sample_task_batch
        else:
            raise("Unknown Dataset")

    def get_task_ids(self, is_train=True):
        # For ganabi, self.num_classes should be 10 games
        if self.dataset_name == 'omniglot':
            return [_sample_task(self.num_classes, is_train=is_train)
                    for _ in range(self.num_tasks)]
        elif self.dataset_name == 'ganabi':
            num_classes = self.num_classes if is_train else 1  # Only one test agent availble
            return [[t] for t in _sample_task(num_classes, is_train=is_train)]
        else:
            raise("Unknown Dataset")

    def next_batch(self, is_train=True):
        """
        Definition:
            Retrieve a train batch and task batch
            T - Task_num, K - K-ways, N - N-shot

            batch: (T, 2, N+1, K, 28, 28, 1)
                First N are used for update task (N-Shots)
                Last 1 is used for update meta network

                For each shot, we will have K items to train on (no repitition)

                2 => index 0 are imgs (x), index 1 are labels (y)
        Return:
            batch
        """
        task_ids_list = self.get_task_ids(is_train)

        batch = []
        if self.num_process > 1:
            batch = _mp_batching(self.mp_func,
                                 task_ids_list,
                                 self.train_config if is_train else self.eval_config,
                                 self.num_process)
        elif self.num_process == 1:
            batch = _loop_batching(self.mp_func,
                                   task_ids_list,
                                   self.train_config if is_train else self.eval_config)
        else:
            raise("Incorrect Number of Processes used")

        return batch
