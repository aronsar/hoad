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


def get_omniglot_labels():
    current_path = os.getcwd()

    train_path = os.path.join(current_path, 'data/omniglot/images_background')
    train_languages = os.listdir(train_path)
    train_labels = []
    for language in train_languages:
        char_path = os.path.join(train_path, language)
        chars = os.listdir(char_path)
        label = [os.path.join(language, char) for char in chars]
        train_labels.extend(label)

    test_path = os.path.join(current_path, 'data/omniglot/images_evaluation')
    test_languages = os.listdir(test_path)
    test_labels = []
    for language in test_languages:
        char_path = os.path.join(test_path, language)
        chars = os.listdir(char_path)
        label = [os.path.join(language, char) for char in chars]
        test_labels.extend(label)

    return train_path, test_path, train_labels, test_labels


class OmniglotDataGenerator(object):
    def __init__(self, config):
        # num_classes = K way
        self.num_classes = config.get("num_classes")
        self.num_shots = config.get("num_shots")
        self.num_tasks = config.get("num_tasks")
        self.batch_size = self.num_classes * self.num_shots
        self.train_path, self.test_path, self.raw_train_labels, self.raw_test_labels = get_omniglot_labels()

        # Use for training and evulating
        self.x_train = []
        self.y_train = []
        self.train_labels_len = len(self.raw_train_labels)

        # Use only for testing
        self.x_test = []
        self.y_test = []
        self.test_labels_len = len(self.raw_test_labels)

        self._read_data()

    def _read_data(self):
        """
        Definition:
            Reads the training data, training labels, testing data, test labels

        Returns:
            None
        """
        for label_id, label_name in enumerate(self.raw_train_labels):
            train_img_path = os.path.join(self.train_path, label_name)
            self.x_train.append(self._read_imgs_in_directory(train_img_path))
            self.y_train.append(label_id)

        for label_id, label_name in enumerate(self.raw_test_labels):
            test_img_path = os.path.join(self.test_path, label_name)
            self.x_test.append(self._read_imgs_in_directory(test_img_path))
            self.y_train.append(label_id)

    def _read_imgs_in_directory(self, img_path):
        """
        Definition:
            Reads Images within the given directory / path.
            Convert the image into grayscale and resize to (28 * 28)

        Returns:
            List: [img1, img2 ....]
        """
        imgs = os.listdir(img_path)
        imgs_in_dir = []
        for img in imgs:
            img_name = os.path.join(img_path, img)
            img_obj = tk.preprocessing.image.load_img(img_name,
                                                      color_mode="grayscale",
                                                      target_size=(28, 28))
            img_arr = tk.preprocessing.image.img_to_array(img_obj)
            imgs_in_dir.append(img_arr)

        return imgs_in_dir

    def get_raw_label_by_id(self, id, label_type=None):
        if label_type == 'train':
            return self.raw_train_labels[id]
        elif label_type == 'test':
            return self.raw_test_label[id]
        else:
            raise("Reading Unknown label type {}".format(label_type))

    def sample_batch(self, is_train=True, is_eval=False):
        """
        Definition:
            Retrieve a train batch
            T - Task_num, K - K-ways, N - N-shot
            Train batch: (T, K+1, N, 28, 28, 1)
                First K are used for update task
                Last 1 is used for update meta network
            Eval batch: (T, 1, 1, 28, 28, 1)
                Used for evaluation of meta
        Return:
        """
        #start_time = time.time()
        x_train_batch = []
        y_train_batch = []

        x_eval_batch = []
        y_eval_batch = []

        for t in range(self.num_tasks):
            task_ids = self.sample_task()

            if is_train:
                x_train_task_batch, y_train_task_batch = self.sample_task_batch(
                    task_ids, self.num_shots+1)
                x_train_batch.append(x_train_task_batch)
                y_train_batch.append(y_train_task_batch)

            if is_eval:
                x_eval_task_batch, y_eval_task_batch = self.sample_task_batch(
                    task_ids, 1)
                x_eval_batch.append(x_eval_task_batch)
                y_eval_batch.append(y_eval_task_batch)

        #print("Batch_Time: {:.3f}".format(time.time() - start_time))
        return x_train_batch, y_train_batch, x_eval_batch, y_eval_batch

    def sample_task(self):
        """
        Definition:
            Sample a task of a K-way of label/class/category combination.
            Not rly an "unique task id", but more like a "unique list of label ids"
        Return:
            List: [label_1, label_2 ....]
        """
        return random.sample(range(self.train_labels_len), self.num_classes)

    def sample_task_batch(self, labels, num_shots):
        """
        Sample a batch for training / evaluating a task based on given class labels to be sampled from
        Shape: x_task (batch_size, height width, channel)
               y_task (batch_size, 1)
        """
        x_task_batch, y_task_batch = [], []

        # N-Way K-shot Sampling
        # First Sample first by N (num classes) then by K (num instances per class)
        for i, label_id in enumerate(labels):
            # Set train batch
            sampled_imgs_id = random.sample(
                range(len(self.x_train[label_id])), num_shots)
            task_imgs, task_labels = [], []
            for j, img_id in enumerate(sampled_imgs_id):
                sampled_img = np.array(self.x_train[label_id][img_id])
                task_imgs.append(sampled_img)
                task_labels.append(i)  # y is 0 ~ self.num_classes
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
                range(self.num_classes), self.num_classes)
            x_task_batch[i], y_task_batch[i] = x_task_batch[i][shuffled_id], y_task_batch[i][shuffled_id]

        # x_task_batch, y_task_batch = x_task_batch[shuffled_id], y_task_batch[shuffled_id]
        # x_task_batch, y_task_batch = np.array(x_task_batch), np.array(y_task_batch)

        return x_task_batch, y_task_batch
