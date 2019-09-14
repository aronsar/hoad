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


class MAML:
    def __init__(self, config):
        self.TASK_NUM = config.get("num_tasks")
        self.N_WAYS_NUM = config.get("num_classes")
        self.K_SHOTS_NUM = config.get("num_shots")
        self.TASK_TRAIN_STEPS = config.get("num_task_train")
        self.META_TRAIN_STEPS = config.get("num_meta_train")

        self.task_lr = 1e-4  # Later Verify this self.task_lr = 1e-4 / self.TASK_TRAIN_STEPS
        self.meta_lr = 1e-4

        # Task specific
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

    @tf.function
    def task_train_step(self, images, labels):
        with tf.GradientTape() as tape:
            predictions = self.model(images)
            loss = self.task_loss_op(labels, predictions)

        self.task_train_loss(loss)
        self.task_train_accuracy(labels, predictions)

        gradients = tape.gradient(loss, self.model.trainable_variables)
        self.task_optimizer.apply_gradients(
            zip(gradients, self.model.trainable_variables))

    @tf.function
    def meta_train_step(self, images, labels, task_models, original_weights):
        with tf.GradientTape() as tape:
            meta_loss = []
            for task in range(self.TASK_NUM):
                task_weights = task_models[task]

                # Set original meta weights to task weight
                for new, old in zip(self.model.trainable_variables, task_weights):
                    new.assign(old)

                predictions = self.model(images[task])
                task_loss = self.task_loss_op(labels[task], predictions)
                meta_loss.append(task_loss)  # Better Approach ?

            # This should be first order MAML. Verified that the calculation is expected
            meta_loss = tf.math.reduce_sum(meta_loss) / tf.cast(tf.shape(meta_loss)[0],
                                                                dtype=tf.float32)

        # Reset to original meta weights before apply gradients
        for new, old in zip(self.model.trainable_variables, original_weights):
            new.assign(old)

        gradients = tape.gradient(meta_loss, self.model.trainable_variables)
        self.task_optimizer.apply_gradients(
            zip(gradients, self.model.trainable_variables))

    @tf.function
    def meta_eval_step(self, images, labels):
        # After update - pure eval, no gradient update
        with tf.GradientTape() as tape:
            images = tf.cast(images, tf.float32)
            predictions = self.model(images)
            loss = self.meta_loss_op(labels, predictions)

        self.meta_train_loss(loss)
        self.meta_train_accuracy(labels, predictions)

    def train(self, data_generator):
        META_VERBOSE, TASK_VERBOSE = False, False
        for meta_step in range(self.META_TRAIN_STEPS):
            start_time = time.time()

            # Save original meta weights
            original_weights = [weight.numpy() for weight in
                                self.model.trainable_variables]

            # Generate Data
            if META_VERBOSE or (meta_step % 100 == 0):
                x_train_batch, y_train_batch, x_eval_batch, y_eval_batch = data_generator.sample_batch(
                    is_train=True, is_eval=True)
            else:
                x_train_batch, y_train_batch, _, _ = data_generator.sample_batch(
                    is_train=True, is_eval=False)

            # Update / Train tasks weight
            task_weights = []
            for task in range(self.TASK_NUM):
                x_task, y_task = x_train_batch[task], y_train_batch[task]
                x_task_train, y_task_train = x_task[:
                                                    self.K_SHOTS_NUM], y_task[:self.K_SHOTS_NUM]
                for task_step in range(self.TASK_TRAIN_STEPS):
                    x_train, y_train = x_task_train[task_step], y_task_train[task_step]
                    self.task_train_step(x_train, y_train)
                    if TASK_VERBOSE:
                        self.print_task_info(meta_step, task_step, task)

                # Save task weights
                task_weight = [weight.numpy() for weight in
                               self.model.trainable_variables]
                task_weights.append(task_weight)

                # Reset to original weights
                for new, old in zip(self.model.trainable_variables, original_weights):
                    new.assign(old)

            # Prepare data to train meta
            x_meta_train, y_meta_train = [], []
            for task in range(self.TASK_NUM):
                x_task_train, y_task_train = x_train_batch[task][
                    self.K_SHOTS_NUM:], y_train_batch[task][self.K_SHOTS_NUM:]
                x_task_train, y_task_train = np.squeeze(
                    x_task_train, axis=0), np.squeeze(y_task_train, axis=0)
                x_meta_train.append(x_task_train)
                y_meta_train.append(y_task_train)

            # Update meta weight
            self.meta_train_step(x_meta_train, y_meta_train,
                                 task_weights, original_weights)

            if META_VERBOSE or (meta_step % 100 == 0):
                for task in range(self.TASK_NUM):
                    x_task_eval, y_task_eval = x_eval_batch[task], y_eval_batch[task]
                    x_task_eval, y_task_eval = np.squeeze(
                        x_task_eval, axis=0), np.squeeze(y_task_eval, axis=0)
                    self.meta_eval_step(x_task_eval, y_task_eval)
                    self.print_meta_info(meta_step, start_time)
            else:
                template = "Meta  : Iteration {} Time: {:.3f}"
                print(template.format(meta_step,
                                      (time.time() - start_time)))

    def print_meta_info(self, meta_step, start_time):
        template = 'Meta  : Iteration {}, Loss: {:.3f}, Accuracy: {:.3f}, Time: {:.3f}'
        print(template.format(meta_step,
                              self.meta_train_loss.result(),
                              (self.meta_train_accuracy.result()*100),
                              (time.time() - start_time)))

        self.meta_train_loss.reset_states()
        self.meta_train_accuracy.reset_states()

    def print_task_info(self, meta_step, task_step, task):
        template = 'Task {}: Iteration {}, Step {}, Loss: {:.3f}, Accuracy: {:.3f}'
        print(template.format(task,
                              meta_step+1,
                              task_step,
                              self.task_train_loss.result(),
                              self.task_train_accuracy.result()*100))

        self.task_train_loss.reset_states()
        self.task_train_accuracy.reset_states()
