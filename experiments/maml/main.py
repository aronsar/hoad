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
from maml import MAML

from TrainConfig import TrainConfig


def get_Omniglot_config():
    NUM_CLASSES = 5        # K-way
    NUM_SHOTS = 2          # N-shot
    NUM_TASK = 32           # Number of task sampled per meta update
    NUM_TASK_TRAIN = 1     # Number of inner task update
    NUM_META_TRAIN = 50000  # Number of total meta update count
    # Number of processors used for batching, use 1 unless batching is a heavy task
    NUM_PROCESS = 1
    NUM_VERBOSE_INTERVAL = 100
    META_LR = 1e-4
    TASK_LR = 0.4
    DATASET = "omniglot"
    DATA_DIR = os.path.join(os.getcwd(), "data")
    PATIENCE = 5000
    REDUCE_LR_RATE = 0.1

    config = {
        "num_classes": NUM_CLASSES,
        "num_shots": NUM_SHOTS,
        "num_tasks": NUM_TASK,
        "num_task_train": NUM_TASK_TRAIN,
        "num_meta_train": NUM_META_TRAIN,
        'num_process': NUM_PROCESS,
        "num_verbose_interval": NUM_VERBOSE_INTERVAL,
        "meta_lr": META_LR,
        "task_lr": TASK_LR,
        "dataset": DATASET,
        "data_dir": DATA_DIR,
        "num_patience": PATIENCE,
        "reduce_lr_rate": REDUCE_LR_RATE
    }

    pprint(config)

    return config


def train_omniglot():
    config = get_Omniglot_config()
    data_generator = DataGenerator(config)
    maml = MAML(config)
    maml.train_manager(data_generator)


def train_ganabi():
    config_file = './config/ganabi.config.gin'
    gin.parse_config_file(config_file)
    config_obj = TrainConfig()
    # config = config_obj.get_config()

    data_generator = DataGenerator(config_obj)
    maml = MAML(config_obj)
    maml.save_gin_config(config_file)
    maml.train_manager(data_generator)

# Demo Metrics at ~/Coding/ganabi/james_ganabi/maml/logs/DeleteMe-20191014-000256/20191014-000256

# [512,512,512,512,256,256,256,256,128,128,128,128,64,64,64,64,32,32,32,32]


def main():
    # Set Memory growth
    physical_devices = tf.config.experimental.list_physical_devices('GPU')
    tf.config.experimental.set_memory_growth(physical_devices[0], True)

    train_ganabi()


if __name__ == "__main__":
    main()
