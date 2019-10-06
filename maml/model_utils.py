"""
Name: model_utils.py

Usage:
    Stores Utility Functions for buildling Models for MAMLQ

Author: Chu-Hung Cheng
"""

import tensorflow.keras as tk


def conv_block(ins, filters, kernel_size, stride, padding, conv_name):
    out = tk.layers.Conv2D(filters, kernel_size, stride,
                           padding)(ins)
    out = tk.layers.BatchNormalization()(out)
    out = tk.activations.relu(out)
    out = tk.layers.MaxPool2D(pool_size=(
        2, 2), padding='valid', data_format='channels_last')(out)

    return out


def fc_block(ins, out_shape):
    out = tk.layers.Dense(units=out_shape, activation=None)(ins)
    out = tk.activations.relu(out)
    out = tk.layers.BatchNormalization()(out)

    return out
