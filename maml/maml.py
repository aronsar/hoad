"""
Name: maml.py

Usage:
    A recreation of MAML algorithm

Author: Chu-Hung Cheng
"""


import tensorflow as tf
import tensorflow.keras as tk
import numpy as np
import models
import time
import datetime
import os


class MAML:
    def __init__(self, config):
        self.TASK_NUM = config.get("num_tasks")
        self.N_WAYS_NUM = config.get("num_classes")
        self.K_SHOTS_NUM = config.get("num_shots")
        self.TASK_TRAIN_STEPS = config.get("num_task_train")
        self.META_TRAIN_STEPS = config.get("num_meta_train")
        self.VERBOSE_INTERVAL = config.get("num_verbose_interval")

        # self.task_lr = 1e-4  # Later Verify this self.task_lr = 1e-4 / self.TASK_TRAIN_STEPS
        self.task_lr = config.get("task_lr")
        self.meta_lr = config.get("meta_lr")

        # Task specific
        # FIXME: Bugged in TF2.0rc1
        self.task_loss_op = tk.losses.SparseCategoricalCrossentropy()
        self.task_optimizer = tk.optimizers.Adam(self.task_lr)

        self.task_train_loss = tk.metrics.Mean(name='task_train_loss')
        self.task_train_accuracy = tk.metrics.SparseCategoricalAccuracy(
            name='task_train_accuracy')

        # Meta specific
        self.meta_loss_op = tk.losses.SparseCategoricalCrossentropy()
        self.meta_optimizer = tk.optimizers.Adam(self.meta_lr)

        self.meta_train_loss = tk.metrics.Mean(name='meta_train_loss')
        self.meta_train_accuracy = tk.metrics.SparseCategoricalAccuracy(
            name='meta_train_accuracy')

        # Build model
        self.model = models.build_simple_model(self.N_WAYS_NUM)
        # 32 models
        self.task_models = [models.build_simple_model(self.N_WAYS_NUM)
                            for _ in range(self.TASK_NUM)]

    @tf.function
    def train_fomaml(self, train_batch):
        """
        Low Level Overview of
            1. Update meta network.
        """

        tasks_gradients = []
        for task in range(self.TASK_NUM):
            for shot in range(0, self.K_SHOTS_NUM):
                images = train_batch[task][0][shot]
                labels = train_batch[task][1][shot]

                with tf.GradientTape() as task_tape:
                    predictions = self.task_models[task](images)
                    loss = self.task_loss_op(labels, predictions)

                task_gradients = task_tape.gradient(
                    loss, self.task_models[task].trainable_variables)
                task_gradients = [tf.clip_by_value(grads, -10, 10)
                                  for grads in task_gradients]
                self.task_optimizer.apply_gradients(
                    zip(task_gradients, self.task_models[task].trainable_variables))

            images = train_batch[task][0][-1]
            labels = train_batch[task][1][-1]
            with tf.GradientTape() as task_tape:
                predictions = self.task_models[task](images)
                task_loss = self.task_loss_op(labels, predictions)

            task_gradients = task_tape.gradient(
                task_loss, self.task_models[task].trainable_variables)
            task_gradients = [tf.clip_by_value(grads, -10, 10)
                              for grads in task_gradients]

            tasks_gradients.append(task_gradients)

        meta_gradients = []
        for i in range(len(tasks_gradients[0])):
            meta_grads = []
            for task in range(0, self.TASK_NUM):
                meta_grads.append(tasks_gradients[task][i])

            tf.stack(meta_grads)
            meta_grads = tf.math.reduce_mean(meta_grads, axis=0)
            meta_gradients.append(meta_grads)

        self.meta_optimizer.apply_gradients(zip(meta_gradients,
                                                self.model.trainable_variables))

    def eval_fomaml(self, eval_batch):
        """
        High Level Overview of
            1. Evaluating MAML.
        """
        for task in range(self.TASK_NUM):
            for shot in range(0, self.K_SHOTS_NUM):
                images = eval_batch[task][0][shot]
                labels = eval_batch[task][1][shot]

                with tf.GradientTape() as task_tape:
                    predictions = self.task_models[task](images)
                    loss = self.task_loss_op(labels, predictions)

                task_gradients = task_tape.gradient(
                    loss, self.task_models[task].trainable_variables)
                task_gradients = [tf.clip_by_value(grads, -10, 10)
                                  for grads in task_gradients]

                self.task_optimizer.apply_gradients(
                    zip(task_gradients, self.task_models[task].trainable_variables))

            images = eval_batch[task][0][-1]
            labels = eval_batch[task][1][-1]
            with tf.GradientTape() as task_tape:
                predictions = self.task_models[task](images)
                task_loss = self.task_loss_op(labels, predictions)

            self.meta_train_loss(task_loss)
            self.meta_train_accuracy(labels, predictions)

    @tf.function
    def train(self, train_batch):
        """
        Low Level Overview of
            1. Update meta network.
        """
        li = [1, 4, 7, 10, 14]  # layers to update. TODO: FIX Later
        with tf.GradientTape() as meta_tape:
            meta_loss = []
            meta_gradients = []
            for task in range(self.TASK_NUM):
                images = train_batch[task][0][0]
                labels = train_batch[task][1][0]

                with tf.GradientTape() as task_tape:
                    predictions = self.model(images)
                    loss = self.task_loss_op(labels, predictions)

                task_gradients = task_tape.gradient(
                    loss, self.model.trainable_variables)
                task_gradients = [tf.clip_by_value(grads, -10, 10)
                                  for grads in task_gradients]

                k = 0
                for i in li:
                    self.task_models[task].layers[i].kernel = tf.subtract(self.model.layers[i].kernel,
                                                                          tf.multiply(self.task_lr, task_gradients[k]))
                    self.task_models[task].layers[i].bias = tf.subtract(self.model.layers[i].bias,
                                                                        tf.multiply(self.task_lr, task_gradients[k+1]))
                    k += 2

                for shot in range(1, self.K_SHOTS_NUM):
                    images = train_batch[task][0][shot]
                    labels = train_batch[task][1][shot]
                    with tf.GradientTape() as task_tape:
                        predictions = self.task_models[task](images)
                        loss = self.task_loss_op(labels, predictions)
                    task_gradients = task_tape.gradient(
                        loss, self.task_models[task].trainable_variables)
                    tf.print(self.task_models[task].trainable_variables)
                    task_gradients = [tf.clip_by_value(grads, -10, 10)
                                      for grads in task_gradients]
                    self.task_optimizer.apply_gradients(
                        zip(task_gradients, self.task_models[task].trainable_variables))

                images = train_batch[task][0][-1]
                labels = train_batch[task][1][-1]
                with tf.GradientTape() as task_tape:
                    predictions = self.task_models[task](images)
                    task_loss = self.task_loss_op(labels, predictions)

                meta_loss.append(task_loss)

                task_gradients = task_tape.gradient(
                    loss, self.task_models[task].trainable_variables)
                task_gradients = [tf.clip_by_value(grads, -10, 10)
                                  for grads in task_gradients]

                # tf.print(self.task_models[task].trainable_variables)
                meta_gradients.append(task_gradients)

            # This should be first order MAML. Verified that the calculation is expected
            meta_loss = tf.math.reduce_mean(meta_loss)

        # FIXME: TF2.0 Doesn't support 2nd Derivative for Sparse Softmax
        meta_gradients = meta_tape.gradient(meta_loss,
                                            self.model.trainable_variables)
        meta_gradients = [tf.clip_by_value(grads, -10, 10)
                          for grads in meta_gradients]

        self.meta_optimizer.apply_gradients(
            zip(meta_gradients[i], self.model.trainable_variables))

    # FIXME: Not yet implemented fully
    def eval(self, eval_batch, meta_step, start_time):
        """
        High Level Overview of
            1. Evaluating MAML.
        """
        li = [1, 4, 7, 10, 14]  # layers to update. TODO: FIX Later
        with tf.GradientTape() as meta_tape:
            for task in range(self.TASK_NUM):
                images = eval_batch[task][0][0]
                labels = eval_batch[task][1][0]
                with tf.GradientTape() as task_tape:
                    predictions = self.model(images)
                    loss = self.task_loss_op(labels, predictions)
                task_gradients = task_tape.gradient(
                    loss, self.model.trainable_variables)
                task_gradients = [tf.clip_by_value(grads, -10, 10)
                                  for grads in task_gradients]

                task_model = self.task_models[task]

                k = 0
                for i in li:
                    task_model.layers[i].kernel = tf.subtract(self.model.layers[i].kernel,
                                                              tf.multiply(self.task_lr, task_gradients[k]))
                    task_model.layers[i].bias = tf.subtract(self.model.layers[i].bias,
                                                            tf.multiply(self.task_lr, task_gradients[k+1]))
                    k += 2

                for shot in range(1, self.K_SHOTS_NUM):
                    images = eval_batch[task][0][shot]
                    labels = eval_batch[task][1][shot]
                    with tf.GradientTape() as task_tape:
                        predictions = task_model(images)
                        loss = self.task_loss_op(labels, predictions)
                    task_gradients = task_tape.gradient(
                        loss, task_model.trainable_variables)
                    task_gradients = [tf.clip_by_value(grads, -10, 10)
                                      for grads in task_gradients]
                    self.task_optimizer.apply_gradients(
                        zip(task_gradients, task_model.trainable_variables))

                images = eval_batch[task][0][-1]
                labels = eval_batch[task][1][-1]
                with tf.GradientTape() as task_tape:
                    predictions = task_model(images)
                    task_loss = self.task_loss_op(labels, predictions)

                self.meta_train_loss(task_loss)
                self.meta_train_accuracy(labels, predictions)

    def train_manager(self, data_generator):
        """
        High Level Overview of training MAML, including generating data & weight changing
        """
        start_time = time.time()
        for meta_step in range(self.META_TRAIN_STEPS):
            if meta_step % self.VERBOSE_INTERVAL == 0:
                train_batch, eval_batch = data_generator.next_batch(
                    is_train=True, is_eval=True)
            else:
                train_batch, _ = data_generator.next_batch(
                    is_train=True, is_eval=False)

            for model in self.task_models:
                model.set_weights(self.model.get_weights())

            self.train(train_batch)

            if meta_step % self.VERBOSE_INTERVAL == 0:
                for model in self.task_models:
                    model.set_weights(self.model.get_weights())

                self.eval(eval_batch)

                template = 'Meta  : Iteration {}, Loss: {:.3f}, Accuracy: {:.3f}, Time: {:.3f}'
                print(template.format(meta_step,
                                      self.meta_train_loss.result(),
                                      (self.meta_train_accuracy.result() * 100),
                                      (time.time() - start_time)))
                self.meta_train_loss.reset_states()
                self.meta_train_accuracy.reset_states()
                start_time = time.time()
