
import os
from itertools import repeat


import data_generator as dg
import dataset_utils as d_utils


class Omniglot(object):
    def __init__(self, data_dir):
        self.color_mode = 'grayscale'
        self.target_size = (28, 28)
        self.train_path = os.path.join(data_dir, 'omniglot/images_background')
        self.test_path = os.path.join(data_dir, 'omniglot/images_evaluation')

        # Define multi-process function name, can be reused
        self.mp_func = d_utils.read_imgs_in_directory

    def _get_omniglot_raw_labels(self):
        train_languages = os.listdir(self.train_path)
        train_labels = []
        for language in train_languages:
            char_path = os.path.join(self.train_path, language)
            chars = os.listdir(char_path)
            label = [os.path.join(language, char) for char in chars]
            train_labels.extend(label)

        test_languages = os.listdir(self.test_path)
        test_labels = []
        for language in test_languages:
            char_path = os.path.join(self.test_path, language)
            chars = os.listdir(char_path)
            label = [os.path.join(language, char) for char in chars]
            test_labels.extend(label)

        return train_labels, test_labels

    def read_wrapper(self):
        """
        Dataset specific reading function for omniglot
        """

        raw_train_labels, raw_test_labels = self._get_omniglot_raw_labels()

        # Update static vars for data generator
        dg.DataGenerator.update_static_vars(self.train_path, self.test_path,
                                            raw_train_labels, raw_test_labels)

        # Define multi-process function arguments
        train_img_paths = [os.path.join(self.train_path, label_name)
                           for label_name in raw_train_labels]

        test_img_paths = [os.path.join(self.test_path, label_name)
                          for label_name in raw_test_labels]

        # Argument wrapper
        mp_train_args = zip(train_img_paths,
                            repeat((self.color_mode, self.target_size)))
        mp_test_args = zip(test_img_paths,
                           repeat((self.color_mode, self.target_size)))

        return self.mp_func, mp_train_args, mp_test_args
