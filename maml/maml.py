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
        self.task_optimizer = tk.optimizers.SGD(self.task_lr, clipvalue=10)

        # tk.metrics are cumulative until calling reset (Tested)
        self.task_train_loss = tk.metrics.Mean(name='task_train_loss')
        self.task_train_accuracy = tk.metrics.SparseCategoricalAccuracy(
            name='task_train_accuracy')

        # Meta specific
        self.meta_loss_op = tk.losses.SparseCategoricalCrossentropy()
        self.meta_optimizer = tk.optimizers.Adam(
            self.meta_lr, clipvalue=10, amsgrad=True)

        self.meta_train_loss = tk.metrics.Mean(name='meta_train_loss')
        self.meta_train_accuracy = tk.metrics.SparseCategoricalAccuracy(
            name='meta_train_accuracy')

        # Summary Writer for Tensorboard
        current_time = datetime.datetime.now().strftime("%Y%m%d-%H%M%S")
        log_dir = 'logs/' + current_time
        self.summary_writer = tf.summary.create_file_writer(log_dir)
        # Build model
        self.model = models.SimpleModel(self.N_WAYS_NUM)
        # 32 models
        self.task_models = [models.SimpleModel(self.N_WAYS_NUM)
                            for _ in range(self.TASK_NUM)]

    @tf.function
    def compute_gradients(self, images, labels, model):
        with tf.GradientTape() as task_tape:
            predictions = model(images)
            loss = self.task_loss_op(labels, predictions)

        grads = task_tape.gradient(loss, model.trainable_variables)
        clipped_grads = [tf.clip_by_value(g, -10, 10) for g in grads]

        return loss, predictions, clipped_grads

    @tf.function
    def train_fomaml_v2(self, train_batch):
        """
        Low Level Overview of
            1. Update meta network.
        """

        tasks_gradients = []
        for task in range(self.TASK_NUM):
            for shot in range(0, self.K_SHOTS_NUM + 1):
                images = train_batch[task][0][shot]
                labels = train_batch[task][1][shot]
                for _ in range(self.TASK_TRAIN_STEPS):
                    # Step 1: Forward Pass
                    _, _, grads = self.compute_gradients(
                        images, labels, self.task_models[task])

                    # with tf.GradientTape() as task_tape:
                    #     predictions = self.task_models[task](images)
                    #     loss = self.task_loss_op(labels, predictions)

                    # task_gradients = task_tape.gradient(
                    #     loss, self.task_models[task].trainable_variables)
                    # task_gradients = [tf.clip_by_value(grads, -10, 10)
                    #                   for grads in task_gradients]

                    # Step 2: Update params
                    if shot < self.K_SHOTS_NUM:
                        self.task_optimizer.apply_gradients(
                            zip(grads, self.task_models[task].trainable_variables))
                    else:
                        tasks_gradients.append(grads)
                        break

        # Step 3 : get gFOMAML
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

    @tf.function
    def eval_fomaml_v2(self, eval_batch):
        """
        High Level Overview of
            1. Evaluating MAML.
        """

        for task in range(self.TASK_NUM):
            for shot in range(0, self.K_SHOTS_NUM + 1):
                images = eval_batch[task][0][shot]
                labels = eval_batch[task][1][shot]
                for _ in range(self.TASK_TRAIN_STEPS):
                    loss, predictions, grads = self.compute_gradients(
                        images, labels, self.task_models[task])

                    if shot < self.K_SHOTS_NUM:
                        self.task_optimizer.apply_gradients(
                            zip(grads, self.task_models[task].trainable_variables))
                    else:
                        self.meta_train_loss(loss)
                        self.meta_train_accuracy(labels, predictions)
                        break

    def train_manager(self, data_generator):
        """
        High Level Overview of training MAML, including generating data & weight changing
        """
        # Build(Init) Models
        input_dim = (self.N_WAYS_NUM, 28, 28, 1)
        self.model.build(input_dim)
        for model in self.task_models:
            model.build(input_dim)

        start_time = time.time()
        for meta_step in range(self.META_TRAIN_STEPS):
            if meta_step % self.VERBOSE_INTERVAL == 0:
                train_batch, eval_batch = data_generator.next_batch(
                    is_train=True, is_eval=True)
            else:
                train_batch, _ = data_generator.next_batch(
                    is_train=True, is_eval=False)

            for i, model in enumerate(self.task_models):
                model.set_weights(self.model.get_weights())

            self.train_fomaml_v2(train_batch)

            if meta_step % self.VERBOSE_INTERVAL == 0:
                for model in self.task_models:
                    model.set_weights(self.model.get_weights())

                self.eval_fomaml_v2(eval_batch)

                with self.summary_writer.as_default():
                    tf.summary.scalar(
                        'loss', self.meta_train_loss.result(), step=meta_step)
                    tf.summary.scalar(
                        'accuracy', self.meta_train_accuracy.result() * 100, step=meta_step)

                template = 'Meta  : Iteration {}, Loss: {:.3f}, Accuracy: {:.3f}, Time: {:.3f}'
                print(template.format(meta_step,
                                      self.meta_train_loss.result(),
                                      (self.meta_train_accuracy.result() * 100),
                                      (time.time() - start_time)))

                self.meta_train_loss.reset_states()
                self.meta_train_accuracy.reset_states()
                start_time = time.time()
