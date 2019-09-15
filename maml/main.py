from __future__ import absolute_import, division, print_function, unicode_literals
from PIL import Image

import tensorflow as tf
import tensorflow.keras as tk
import matplotlib.pyplot as plt
import time
import numpy as np
import os
import random


from data_generator import OmniglotDataGenerator
from maml import MAML


def train(config):
    data_generator = OmniglotDataGenerator(config)
    maml = MAML(config)
    maml.train(data_generator)


def main():
    NUM_CLASSES = 5        # K-way
    NUM_SHOTS = 2          # N-shot
    NUM_TASK = 32          # Number of task sampled per meta update
    NUM_TASK_TRAIN = 1     # Number of inner task update
    NUM_META_TRAIN = 1000  # Number of total meta update count
    # Number of processors used for batching, use 1 unless batching is a heavy task
    NUM_PROCESS = 1

    config = {
        "num_classes": NUM_CLASSES,
        "num_shots": NUM_SHOTS,
        "num_tasks": NUM_TASK,
        "num_task_train": NUM_TASK_TRAIN,
        "num_meta_train": NUM_META_TRAIN,
        'num_process': NUM_PROCESS
    }

    train(config)


if __name__ == "__main__":
    main()
