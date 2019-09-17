"""
Name: models.py

Usage:
    Stores define models used for MAML.
    Expecting using Tensroflow Keras API only. Keras Function Model or Sequential Model.


Author: Chu-Hung Cheng
"""

import tensorflow as tf
import tensorflow.keras as tk
import model_utils as m_utils


# MAML is model agnostic, so its better to have a util function that allows user to feed in any type of model
# Ideally, we can have as many model in this function

def build_simple_model(output_shape):
    # Note: Calling this function creates an "unique" model
    ins = tk.Input(shape=(28, 28, 1))

    out = m_utils.conv_block(ins, 64, 3, [1, 1], 'SAME', conv_name="conv1")
    out = m_utils.conv_block(out, 64, 3, [1, 1], 'SAME', conv_name="conv2")
    out = m_utils.conv_block(out, 64, 3, [1, 1], 'SAME', conv_name="conv3")
    out = m_utils.conv_block(out, 64, 3, [1, 1], 'SAME', conv_name="conv4")

    out = tf.math.reduce_mean(out, [1, 2])  # According to Paper
    out = tk.layers.Dense(output_shape, activation='softmax')(out)
    return tk.Model(inputs=ins, outputs=out)  # Keras Functional API


class SimpleModel(tk.Model):
    def __init__(self, output_shape):
        super().__init__()
        self.conv1 = tk.layers.Conv2D(64, 3, [1, 1], 'SAME',
                                      input_shape=(28, 28, 1))
        # self.bn1 = tk.layers.BatchNormalization(axis=3)
        self.act1 = tk.activations.relu
        self.pool1 = tk.layers.MaxPool2D(pool_size=(2, 2),
                                         padding='valid',
                                         data_format='channels_last')
        self.conv2 = tk.layers.Conv2D(64, 3, [1, 1], 'SAME')
        # self.bn2 = tk.layers.BatchNormalization(axis=3)
        self.act2 = tk.activations.relu
        self.pool2 = tk.layers.MaxPool2D(pool_size=(2, 2),
                                         padding='valid',
                                         data_format='channels_last')

        self.conv3 = tk.layers.Conv2D(64, 3, [1, 1], 'SAME')
        # self.bn3 = tk.layers.BatchNormalization(axis=3)
        self.act3 = tk.activations.relu
        self.pool3 = tk.layers.MaxPool2D(pool_size=(2, 2),
                                         padding='valid',
                                         data_format='channels_last')

        self.conv4 = tk.layers.Conv2D(64, 3, [1, 1], 'SAME')
        # self.bn4 = tk.layers.BatchNormalization(axis=3)
        self.act4 = tk.activations.relu
        self.pool4 = tk.layers.MaxPool2D(pool_size=(2, 2),
                                         padding='valid',
                                         data_format='channels_last')

        self.dense1 = tk.layers.Dense(output_shape, activation='softmax')

    def forward(self, x):
        # x = self.pool1(self.act1(self.bn1(self.conv1(x))))
        # x = self.pool2(self.act2(self.bn2(self.conv2(x))))
        # x = self.pool3(self.act3(self.bn3(self.conv3(x))))
        # x = self.pool4(self.act4(self.bn4(self.conv4(x))))
        x = self.pool1(self.act1(self.conv1(x)))
        x = self.pool2(self.act2(self.conv2(x)))
        x = self.pool3(self.act3(self.conv3(x)))
        x = self.pool4(self.act4(self.conv4(x)))
        x = tf.math.reduce_mean(x, [1, 2])
        x = self.dense1(x)
        return x
