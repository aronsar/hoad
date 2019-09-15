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
import multiprocessing as mp
from itertools import repeat

import dataset as d


def sample_task_batch(labels, config):
    """
    Sample a batch for training / evaluating a task based on given class labels to be sampled from
    Shape: x_task (batch_size, height width, channel)
           y_task (batch_size, 1)
    """
    num_shots, num_classes = config[0], config[1]
    x_task_batch, y_task_batch = [], []

    # N-Way K-shot Sampling
    # First Sample first by N (num classes) then by K (num instances per class)
    n_way_labels = random.sample(range(len(labels)), len(labels))
    for i in range(num_classes):
        raw_label_id = labels[i]
        true_label = n_way_labels[i]
        # Set train batch
        sampled_imgs_id = random.sample(
            range(len(DataGenerator.x_train[raw_label_id])), num_shots)
        task_imgs, task_labels = [], []
        for j, img_id in enumerate(sampled_imgs_id):
            sampled_img = np.array(
                DataGenerator.x_train[raw_label_id][img_id])
            task_imgs.append(sampled_img)
            task_labels.append(true_label)  # y is 0 ~ self.num_classes
            # x_task_batch.append(sampled_imgs)
            # y_task_batch.append(i) # y is 0 ~ self.num_classes

        x_task_batch.append(task_imgs)
        y_task_batch.append(task_labels)

    # Convert to numpy array
    x_task_batch, y_task_batch = np.array(
        x_task_batch), np.array(y_task_batch)
    # Convert from (N, K, 28, 28, 1) to (K, N, 28, 28, 1)
    x_task_batch = np.swapaxes(x_task_batch, 0, 1)
    y_task_batch = np.swapaxes(y_task_batch, 0, 1)

    # Shuffle along N mutually
    for i in range(num_shots):
        shuffled_id = random.sample(
            range(num_classes), num_classes)
        x_task_batch[i], y_task_batch[i] = x_task_batch[i][shuffled_id], y_task_batch[i][shuffled_id]

    return x_task_batch, y_task_batch


class DataGenerator(object):
    # Static Variables for Multi-Processing Support
    x_train, x_test = [], []
    raw_train_labels, raw_test_labels = [], []
    train_labels_len, test_labels_len = 0, 0
    train_path, test_path = "", ""

    def __init__(self, config):
        # num_classes = K way
        self.num_classes = config.get("num_classes")
        self.num_shots = config.get("num_shots")
        self.num_tasks = config.get("num_tasks")
        self.num_process = config.get("num_process")
        self.batch_size = self.num_classes * self.num_shots

        self.dataset = config.get("dataset")
        self.data_dir = config.get("data_dir")

        # self._read_data()
        self.read_data(4)  # 4 is good for IO task

    @staticmethod
    def update_static_vars(train_path, test_path, raw_train_labels, raw_test_labels):
        DataGenerator.train_path = train_path
        DataGenerator.test_path = test_path
        DataGenerator.raw_train_labels = raw_train_labels
        DataGenerator.raw_test_labels = raw_test_labels
        DataGenerator.train_labels_len = len(raw_train_labels)
        DataGenerator.test_labels_len = len(raw_test_labels)

    def read_data(self, process_count):
        """
        Definition:
            Reads the training data, testing data with multi-processing support

        Returns:
            None
        """

        if self.dataset == 'omniglot':
            omniglot_obj = d.Omniglot(self.data_dir)
            mp_func, mp_train_args, mp_test_args = omniglot_obj.read_wrapper()
        else:
            raise("Unknown Dataset")

        with mp.Pool(process_count) as p:
            DataGenerator.x_train = p.starmap(mp_func, mp_train_args)

        with mp.Pool(process_count) as p:
            DataGenerator.x_test = p.starmap(mp_func, mp_test_args)

    def get_raw_label_by_id(self, id, label_type=None):
        if label_type == 'train':
            return DataGenerator.raw_train_labels[id]
        elif label_type == 'test':
            return DataGenerator.raw_test_label[id]
        else:
            raise("Reading Unknown label type {}".format(label_type))

    def new_sample_batch(self, is_train=True, is_eval=False):
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

        def _multiprocess_batching(task_ids_list):
            """
            Definition:
                Multi-Proceesing support for batching
            Return:
                train_batch, eval_batch
            """
            # start_time = time.time()
            task_batch = []
            meta_batch = []

            t_config = (self.num_shots, self.num_classes)
            m_config = (1, 1)

            with mp.Pool(self.num_process) as p:
                task_batch = p.starmap(sample_task_batch,
                                       zip(task_ids_list, repeat(t_config)))

            with mp.Pool(self.num_process) as p:
                meta_batch = p.starmap(sample_task_batch,
                                       zip(task_ids_list, repeat(m_config)))

            meta_batch = self._process_single_img_batch(meta_batch)

            # print("MultiProcess Batch_Time: {:.3f}".format(
            #    time.time() - start_time))
            return task_batch, meta_batch

        def _loop_batching(task_ids_list):
            """
            Definition:
                Loop batching. Faster when batching is a light task
            Return:
                train_batch, eval_batch
            """
            # start_time = time.time()
            task_batch = []
            meta_batch = []

            t_config = (self.num_shots, self.num_classes)
            m_config = (1, 1)

            for task in range(self.num_tasks):
                task_ids = task_ids_list[task]

                x_task_batch, y_task_batch = sample_task_batch(task_ids,
                                                               t_config)
                task_batch.append((x_task_batch,
                                   y_task_batch))

                x_meta_batch, y_meta_batch = sample_task_batch(task_ids,
                                                               m_config)
                meta_batch.append((x_meta_batch,
                                   y_meta_batch))

            meta_batch = self._process_single_img_batch(
                meta_batch)

            # print("Loop Batch_Time: {:.3f}".format(time.time() - start_time))
            return task_batch, meta_batch

        train_task_ids_list = [self.sample_task()
                               for _ in range(self.num_tasks)]
        eval_task_ids_list = [self.sample_task()
                              for _ in range(self.num_tasks)]

        train_task_batch, train_meta_batch, = [], []
        eval_task_batch, eval_meta_batch = [], []
        if self.num_process > 1:
            if is_train:
                train_task_batch, train_meta_batch = _multiprocess_batching(
                    train_task_ids_list)
            if is_eval:
                eval_task_batch, eval_meta_batch = _multiprocess_batching(
                    eval_task_ids_list)
            return train_task_batch, train_meta_batch, eval_task_batch, eval_meta_batch
        elif self.num_process == 1:
            if is_train:
                train_task_batch, train_meta_batch = _loop_batching(
                    train_task_ids_list)
            if is_eval:
                eval_task_batch, eval_meta_batch = _loop_batching(
                    eval_task_ids_list)
            return train_task_batch, train_meta_batch, eval_task_batch, eval_meta_batch
        else:
            raise("Incorrect Number of Processes used")

    def sample_task(self):
        """
        Definition:
            Sample a task of a K-way of label/class/category combination.
            Not rly an "unique task id", but more like a "unique list of label ids"
        Return:
            List: [label_1, label_2 ....]
        """
        return random.sample(range(DataGenerator.train_labels_len), self.num_classes)

    def _process_single_img_batch(self, batch):
        x_batch = np.array([batch[task][0][-1]
                            for task in range(self.num_tasks)])
        y_batch = np.array([batch[task][1][-1]
                            for task in range(self.num_tasks)])
        return [x_batch, y_batch]
