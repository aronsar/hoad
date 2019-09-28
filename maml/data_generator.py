"""
Name: maml.py

Usage:
    A data_generator design specifically for MAML

Author: Chu-Hung Cheng
"""
import tensorflow.keras as tk
import numpy as np
import os
import random
import time


import dataset as d
import data_generator_utils as dg_utils


class DataGenerator(object):
    # Static Variables for Multi-Processing Support
    dataset_obj = None

    def __init__(self, config):
        # num_classes = K way
        self.num_classes = config.get("num_classes")
        self.num_shots = config.get("num_shots")
        self.num_tasks = config.get("num_tasks")
        self.num_process = config.get("num_process")

        self.batch_size = self.num_classes * self.num_shots
        self.config = (self.num_shots+1, self.num_classes)
        self.mp_func = dg_utils.sample_task_batch

        # Retrieve a dataset used for setting static variables
        data_dir = config.get("data_dir")
        dataset_name = config.get("dataset")
        DataGenerator.set_static_vars(data_dir, dataset_name)

    @staticmethod
    def set_static_vars(data_dir, dataset_name):
        if dataset_name == 'omniglot':
            DataGenerator.dataset_obj = d.Omniglot(data_dir)
        else:
            raise("Unknown Dataset")

    def next_batch(self, is_train=True, is_eval=False):
        """
        Definition:
            Retrieve a train batch and task batch

            T - Task_num, K - K-ways, N - N-shot

            Train batch: (T, 2, N+1, K, 28, 28, 1)
                First N are used for update task (N-Shots)
                Last 1 is used for update meta network

                For each shot, we will have K items to train on (no repitition)

                2 => index 0 are imgs (x), index 1 are labels (y)

            Eval batch: (T, 2, 1, K)
                Used for evaluation of meta
        Return:
            train_batch, eval_batch
        """

        # Sample from same characters
        # rand_train_task = self.sample_task()
        # train_task_ids_list = [rand_train_task for _ in range(self.num_tasks)]
        # rand_train_task = self.sample_task()
        # eval_task_ids_list = [rand_train_task for _ in range(self.num_tasks)]

        # Sample from different characters
        train_task_ids_list = [self.sample_task()
                               for _ in range(self.num_tasks)]
        eval_task_ids_list = [self.sample_task()
                              for _ in range(self.num_tasks)]

        train_batch, eval_batch = [], []
        # if self.num_process > 1:
        #     if is_train:
        #         train_batch = dg_utils._mp_batching(self.mp_func,
        #                                             train_task_ids_list,
        #                                             self.config,
        #                                             self.num_process)

        #     if is_eval:
        #         eval_batch = dg_utils._mp_batching(self.mp_func,
        #                                            eval_task_ids_list,
        #                                            self.config,
        #                                            self.num_process)

        # elif self.num_process == 1:
        if is_train:
            train_batch = dg_utils._loop_batching(self.mp_func,
                                                  eval_task_ids_list,
                                                  self.config)
        if is_eval:
            eval_batch = dg_utils._loop_batching(self.mp_func,
                                                 eval_task_ids_list,
                                                 self.config)

        else:
            raise("Incorrect Number of Processes used")

        return train_batch, eval_batch

    def sample_task(self):
        """
        Definition:
            Sample a task of a K-way of label/class/category combination.
            Not rly an "unique task id", but more like a "unique list of label ids"
        Return:
            List: [label_1, label_2 ....]
        """
        return random.sample(range(DataGenerator.dataset_obj.train_labels_len),
                             self.num_classes)
