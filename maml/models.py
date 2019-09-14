"""
Name: models.py

Usage:
    Stores define models used for MAML.
    Expecting using Tensroflow Keras API only. Keras Function Model or Sequential Model. 


Author: Chu-Hung Cheng
"""

import tensorflow as tf
import tensorflow.keras as tk
import model_utils as mutils


# MAML is model agnostic, so its better to have a util function that allows user to feed in any type of model
# Ideally, we can have as many model in this function

def build_simple_model(output_shape):
    # Note: Calling this function creates an "unique" model
    ins = tk.Input(shape=(28, 28, 1))

    out = mutils.conv_block(ins, 64, 3, [2, 2], 'SAME', conv_name="conv1")
    out = mutils.conv_block(out, 64, 3, [2, 2], 'SAME', conv_name="conv2")
    out = mutils.conv_block(out, 64, 3, [2, 2], 'SAME', conv_name="conv3")
    out = mutils.conv_block(out, 64, 3, [2, 2], 'SAME', conv_name="conv4")

    out = tf.math.reduce_mean(out, [1, 2])  # According to Paper
    out = tk.layers.Dense(output_shape, activation='softmax')(out)
    return tk.Model(inputs=ins, outputs=out)  # Keras Functional API
