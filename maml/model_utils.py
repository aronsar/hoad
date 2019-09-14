"""
Name: model_utils.py

Usage:
    Stores Utility Functions for buildling Models for MAMLQ

Author: Chu-Hung Cheng
"""

import tensorflow.keras as tk


def conv_block(ins, filters, kernel_size, stride, padding, conv_name):
    out = tk.layers.Conv2D(filters, kernel_size, stride,
                           padding, name=conv_name)(ins)
    out = tk.layers.BatchNormalization()(out)
    out = tk.activations.relu(out)
    return out
