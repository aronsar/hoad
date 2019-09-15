from __future__ import absolute_import, division, print_function, unicode_literals
from PIL import Image

import tensorflow as tf
import tensorflow.keras as tk
import matplotlib.pyplot as plt
import time
import numpy as np
import os
import random


from data_generator import DataGenerator
from maml import MAML


def train(config):
    data_generator = DataGenerator(config)
    print(DataGenerator.train_labels_len)
    print(DataGenerator.test_labels_len)
    maml = MAML(config)
    maml.train_manager(data_generator)


def main():
    NUM_CLASSES = 5        # K-way
    NUM_SHOTS = 1          # N-shot
    NUM_TASK = 32          # Number of task sampled per meta update
    NUM_TASK_TRAIN = 1     # Number of inner task update
    NUM_META_TRAIN = 20000  # Number of total meta update count
    # Number of processors used for batching, use 1 unless batching is a heavy task
    NUM_PROCESS = 1
    NUM_VERBOSE_INTERVAL = 50
    META_LR = 1e-4
    TASK_LR = META_LR
    DATASET = "omniglot"
    DATA_DIR = os.path.join(os.getcwd(), "data")
    print(DATA_DIR)

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
        "data_dir": DATA_DIR
    }

    print(DataGenerator.train_labels_len)
    print(DataGenerator.test_labels_len)
    train(config)


if __name__ == "__main__":
    main()
