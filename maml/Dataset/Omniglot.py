
import os
import random
import numpy as np
import multiprocessing as mp
from itertools import repeat
from PIL import Image


import DataGenerator as dg


def sample_task_batch(labels, config):
    """
    Sample a batch for training / evaluating a task based on given class labels to be sampled from
    Shape: x_task (batch_size, height width, channel)
           y_task (batch_size, 1)
    """
    num_shots, num_classes, is_train = config[0], config[1], config[2]
    x_task_batch, y_task_batch = [], []

    # N-Way K-shot Sampling
    # First Sample first by N (num classes) then by K (num instances per class)
    n_way_labels = random.sample(range(len(labels)), len(labels))
    for i in range(num_classes):
        # FIXME: enumerate throught labels
        raw_label_id = labels[i]
        true_label = n_way_labels[i]

        # Set train batch
        img_count = dg.DataGenerator.dataset_obj.get_image_count_by_label(
            raw_label_id, is_train)
        sampled_imgs_id = random.sample(range(img_count), num_shots)
        task_imgs, task_labels = [], []
        for j, img_id in enumerate(sampled_imgs_id):
            sampled_img = dg.DataGenerator.dataset_obj.get_image(raw_label_id,
                                                                 img_id,
                                                                 is_train)

            task_imgs.append(sampled_img)
            task_labels.append(true_label)  # y is 0 ~ self.num_classes

        x_task_batch.append(task_imgs)
        y_task_batch.append(task_labels)

    # Convert to numpy array
    x_task_batch, y_task_batch = np.array(x_task_batch), np.array(y_task_batch)

    # Convert from (N, K, 28, 28, 1) to (K, N, 28, 28, 1)
    x_task_batch = np.swapaxes(x_task_batch, 0, 1)
    y_task_batch = np.swapaxes(y_task_batch, 0, 1)

    # Shuffle along N mutually
    for i in range(num_shots):
        shuffled_id = random.sample(
            range(num_classes), num_classes)
        x_task_batch[i], y_task_batch[i] = x_task_batch[i][shuffled_id], y_task_batch[i][shuffled_id]

    return x_task_batch, y_task_batch


# Seperate Support Set for train task, Query Set for train meta
def sample_task_batch_v2(labels, config):
    """
    Sample a batch for training / evaluating a task based on given class labels to be sampled from
    Shape: x_task (batch_size, height width, channel)
           y_task (batch_size, 1)
    """
    num_query_shots = 1
    num_support_shots, num_classes, is_train = config[0], config[1], config[2]
    x_support, y_support, x_query, y_query = [], [], [], []

    # N-Way K-shot Sampling
    # First Sample first by N (num classes) then by K (num instances per class)
    n_way_labels = random.sample(range(len(labels)), len(labels))
    for i, raw_label_id in enumerate(labels):
        # FIXME: enumerate throught labels
        # raw_label_id = labels[i]
        true_label = n_way_labels[i]

        # Set train batch
        img_count = dg.DataGenerator.dataset_obj.get_image_count_by_label(
            raw_label_id, is_train)
        sampled_imgs_id = random.sample(
            range(img_count), num_support_shots+num_query_shots)
        task_imgs, task_labels = [], []
        for j, img_id in enumerate(sampled_imgs_id):
            sampled_img = dg.DataGenerator.dataset_obj.get_image(raw_label_id,
                                                                 img_id,
                                                                 is_train)
            task_imgs.append(sampled_img)
            task_labels.append(true_label)  # y is 0 ~ self.num_classes

        x_support.append(task_imgs[:-num_query_shots])
        y_support.append(task_labels[:-num_query_shots])
        x_query.append(task_imgs[-num_query_shots:])
        y_query.append(task_labels[-num_query_shots:])

    # TODO: Write Utilitiy function to handle the post-processing
    # Convert to numpy array
    x_support, y_support = np.array(x_support), np.array(y_support)
    x_query, y_query = np.array(x_query), np.array(y_query)

    # Convert from (N, K, 28, 28, 1) to (K, N, 28, 28, 1)
    x_support = np.swapaxes(x_support, 0, 1)
    y_support = np.swapaxes(y_support, 0, 1)
    x_query = np.swapaxes(x_query, 0, 1)
    y_query = np.swapaxes(y_query, 0, 1)

    # Shuffle along N mutually
    for i in range(num_support_shots):
        shuffled_id = random.sample(range(num_classes), num_classes)
        x_support[i] = x_support[i][shuffled_id]
        y_support[i] = y_support[i][shuffled_id]

    for i in range(num_query_shots):
        shuffled_id = random.sample(range(num_classes), num_classes)
        x_query[i] = x_query[i][shuffled_id]
        y_query[i] = y_query[i][shuffled_id]

    return x_support, y_support, x_query, y_query


def read_imgs_in_directory(img_path, config):
    """
    Definition:
        Reads Images within the given directory / path.
        Convert the image into grayscale and resize to (28 * 28)

    Returns:
        List: [img1, img2 ....]
    """
    color_mode, target_size = config[0], config[1]
    imgs = os.listdir(img_path)
    imgs_in_dir = []

    for img in imgs:
        img_name = os.path.join(img_path, img)
        img_obj = Image.open(img_name)
        resized_img_obj = img_obj.resize(target_size, Image.ANTIALIAS)
        img_arr = np.array(resized_img_obj.getdata(),
                           dtype=np.float32).reshape(target_size)
        img_arr = img_arr / 255.0
        img_arr = 1 - img_arr
        img_arr = np.expand_dims(img_arr, axis=2)  # channel dimension
        imgs_in_dir.append(img_arr)

    return imgs_in_dir


class Dataset(object):
    def __init__(self, data_dir):
        self.color_mode = 'grayscale'
        self.target_size = (28, 28)
        self.train_path = os.path.join(data_dir, 'omniglot/images_background')
        self.test_path = os.path.join(data_dir, 'omniglot/images_evaluation')

        self.raw_train_labels = self._get_raw_labels(self.train_path)
        self.raw_test_labels = self._get_raw_labels(self.test_path)
        self.train_labels_len = len(self.raw_train_labels)
        self.test_labels_len = len(self.raw_test_labels)

        # Define multi-process function name, can be reused
        self.mp_func = read_imgs_in_directory

        self.x_train = self._read_img_data(self.train_path,
                                           self.raw_train_labels)
        self.x_test = self._read_img_data(self.test_path,
                                          self.raw_test_labels)

    def _get_raw_labels(self, path):
        languages = os.listdir(path)
        labels = []
        for language in languages:
            char_path = os.path.join(path, language)
            chars = os.listdir(char_path)
            label = [os.path.join(language, char) for char in chars]
            labels.extend(label)

        return labels

    def _read_img_data(self, path, labels, process_count=4):
        # Define multi-process function arguments
        img_paths = [os.path.join(path, label_name)
                     for label_name in labels]

        # Argument wrapper
        mp_args = zip(img_paths,
                      repeat((self.color_mode, self.target_size)))

        with mp.Pool(process_count) as p:
            img_data = p.starmap(self.mp_func, mp_args)

        return img_data

    def get_image(self, raw_label_id, img_id, train=True):
        if train:
            return np.array(self.x_train[raw_label_id][img_id])
        else:
            return np.array(self.x_test[raw_label_id][img_id])

    def get_image_count_by_label(self, raw_label_id, train=True):
        if train:
            return len(self.x_train[raw_label_id])
        else:
            return len(self.x_test[raw_label_id])
