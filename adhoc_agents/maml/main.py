from __future__ import absolute_import, division, print_function, unicode_literals
from PIL import Image

import tensorflow as tf
import tensorflow.keras as tk
import matplotlib.pyplot as plt
import time
import numpy as np
import os
import random
import gin
from pprint import pprint

from DataGenerator import DataGenerator
from Dataset.PKL_Ganabi import Dataset as pkl_dataset
from PKL_MAML import MAML as pkl_maml

from Dataset.NPZ_Ganabi import Dataset as npz_dataset
from NPZ_MAML import MAML as npz_maml

from TrainConfig import TrainConfig


def train_omniglot():
    config_file = './config/omniglot.config.gin'
    gin.parse_config_file(config_file)
    config_obj = TrainConfig()

    maml = MAML(config_obj)
    maml.save_gin_config(config_file)
    data_generator = DataGenerator(config_obj)
    maml.train_manager(data_generator)


def train_ganabi():
    config_file = './config/ganabi.config.gin'
    gin.parse_config_file(config_file)
    config_obj = TrainConfig()

    data_type = config_obj.get("data_type")
    if data_type == "npz":
        maml = npz_maml(config_obj)
        data_generator = npz_dataset(config_obj)
    elif data_type == "pkl":
        maml = pkl_maml(config_obj)
        data_generator = pkl_dataset(config_obj)
    else:
        raise(BaseException, f"Unknow data type {data_type}")

    maml.save_gin_config(config_file)
    maml.init_agent_metrics(data_generator.all_agent_names)
    maml.train_manager(data_generator)


# [2048,2048,1024,1024,512,512,256,256,128,128,64,64]
def main():
    # Set Memory growth
    physical_devices = tf.config.experimental.list_physical_devices('GPU')
    tf.config.experimental.set_memory_growth(physical_devices[0], True)

    # train_omniglot()
    train_ganabi()


if __name__ == "__main__":
    main()
