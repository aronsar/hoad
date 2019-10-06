
import os
import numpy as np
import multiprocessing as mp
from itertools import repeat


import data_generator as dg
import dataset_utils as d_utils


class Omniglot(object):
    def __init__(self, data_dir):
        self.color_mode = 'grayscale'
        self.target_size = (28, 28)
        self.train_path = os.path.join(data_dir, 'omniglot/images_background')
        self.test_path = os.path.join(data_dir, 'omniglot/images_evaluation')

        self.raw_train_labels = self._get_omniglot_raw_labels(self.train_path)
        self.raw_test_labels = self._get_omniglot_raw_labels(self.test_path)
        self.train_labels_len = len(self.raw_train_labels)
        self.test_labels_len = len(self.raw_test_labels)

        # Define multi-process function name, can be reused
        self.mp_func = d_utils.read_imgs_in_directory

        self.x_train = self._read_img_data(self.train_path,
                                           self.raw_train_labels)
        self.x_test = self._read_img_data(self.test_path,
                                          self.raw_test_labels)

    def _get_omniglot_raw_labels(self, path):
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
