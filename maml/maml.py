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
        self.VERBOSE_INTERVAL = config.get("num_verbose_interval")

        # self.task_lr = 1e-4  # Later Verify this self.task_lr = 1e-4 / self.TASK_TRAIN_STEPS
        self.task_lr = config.get("task_lr")
        self.meta_lr = config.get("meta_lr")

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
        self.original_weights = [weight.numpy()
                                 for weight in self.model.trainable_variables]

    def reset_model_weights(self, new_weights):
        for model_weight, new_weight in zip(self.model.trainable_variables,
                                            new_weights):
            model_weight.assign(new_weight)

    @tf.function
    def task_train_step(self, images, labels):
        """
        Low Level Overview of
            1. Update task network.
        """
        with tf.GradientTape() as tape:
            predictions = self.model(images)
            loss = self.task_loss_op(labels, predictions)

        self.task_train_loss(loss)
        self.task_train_accuracy(labels, predictions)

        gradients = tape.gradient(loss, self.model.trainable_variables)
        gradients = [tf.clip_by_value(
            grads, -10, 10) for grads in gradients]
        self.task_optimizer.apply_gradients(
            zip(gradients, self.model.trainable_variables))

    @tf.function
    def meta_train_step(self, images, labels, task_models):
        """
        Low Level Overview of
            1. Update meta network.
        """
        with tf.GradientTape() as tape:
            meta_loss = []
            for task in range(self.TASK_NUM):
                # Set original meta weights to task weight
                task_weights = task_models[task]
                self.reset_model_weights(task_weights)

                predictions = self.model(images[task])
                task_loss = self.task_loss_op(labels[task], predictions)
                meta_loss.append(task_loss)  # Better Approach ?

            # This should be first order MAML. Verified that the calculation is expected
            meta_loss = tf.math.reduce_sum(meta_loss) / tf.cast(tf.shape(meta_loss)[0],
                                                                dtype=tf.float32)

        # Reset to original meta weights before apply gradients
        self.reset_model_weights(self.original_weights)

        gradients = tape.gradient(meta_loss, self.model.trainable_variables)
        gradients = [tf.clip_by_value(grads, -10, 10) for grads in gradients]
        self.meta_optimizer.apply_gradients(
            zip(gradients, self.model.trainable_variables))

    @tf.function
    def meta_eval_step(self, images, labels):
        """
        Low Level Overview of
            1. Evaluating MAML using meta model.
        """
        # After update - pure eval, no gradient update
        with tf.GradientTape() as tape:
            predictions = self.model(images)
            loss = self.meta_loss_op(labels, predictions)

        self.meta_train_loss(loss)
        self.meta_train_accuracy(labels, predictions)

    def train(self, train_task_batch, train_meta_batch):
        """
        High Level Overview of
            1. Training MAML.
            2. A gradient update made in meta model
        """

        # Update / Train tasks weight
        task_weights = []
        for task in range(self.TASK_NUM):
            for task_step in range(self.TASK_TRAIN_STEPS):
                for shot in range(self.K_SHOTS_NUM):
                    x_task_train = train_task_batch[task][0][shot]
                    y_task_train = train_task_batch[task][1][shot]
                    self.task_train_step(x_task_train, y_task_train)

            # Save task weights
            task_weight = [weight.numpy() for weight in
                           self.model.trainable_variables]
            task_weights.append(task_weight)

            # Reset to original weights
            self.reset_model_weights(self.original_weights)

        # Prepare data to train meta
        x_meta_train, y_meta_train = train_meta_batch[0], train_meta_batch[1]

        # Update meta weight
        self.meta_train_step(x_meta_train, y_meta_train, task_weights)

    def eval(self, eval_task_batch, eval_meta_batch, meta_step, start_time):
        """
        High Level Overview of
            1. Evaluating MAML.
        """
        avg_acc, avg_loss = 0, 0
        for eval_task in range(self.TASK_NUM):
            # First PreTrain K_SHOTS
            for task_step in range(self.TASK_TRAIN_STEPS):
                for shot in range(self.K_SHOTS_NUM):
                    x_task_eval = eval_task_batch[eval_task][0][shot]
                    y_task_eval = eval_task_batch[eval_task][1][shot]
                    self.task_train_step(x_task_eval, y_task_eval)

            # Select only image for eval from the last shot
            x_meta_eval = eval_meta_batch[0][eval_task]
            y_meta_eval = eval_meta_batch[1][eval_task]

            # Run Eval Result
            self.meta_eval_step(x_meta_eval, y_meta_eval)

            avg_loss += self.meta_train_loss.result()
            avg_acc += self.meta_train_accuracy.result() * 100
            self.meta_train_loss.reset_states()
            self.meta_train_accuracy.reset_states()

            # Reset to original weights
            self.reset_model_weights(self.original_weights)

        avg_acc /= self.TASK_NUM
        avg_loss /= self.TASK_NUM

        # Print Result
        template = 'Meta  : Iteration {}, Loss: {:.3f}, Accuracy: {:.3f}, Time: {:.3f}'
        print(template.format(meta_step,
                              avg_loss,
                              avg_acc,
                              (time.time() - start_time)))

    def train_manager(self, data_generator):
        """
        High Level Overview of training MAML, including generating data & weight changing
        """
        start_time = time.time()
        for meta_step in range(self.META_TRAIN_STEPS):
            if meta_step % self.VERBOSE_INTERVAL == 0:
                train_task_batch, train_meta_batch, eval_task_batch, eval_meta_batch = data_generator.new_sample_batch(
                    is_train=True, is_eval=True)
            else:
                train_task_batch, train_meta_batch, _, _ = data_generator.new_sample_batch(
                    is_train=True, is_eval=False)

            self.train(train_task_batch, train_meta_batch)

            self.original_weights = [weight.numpy() for weight in
                                     self.model.trainable_variables]

            if meta_step % self.VERBOSE_INTERVAL == 0:
                self.eval(eval_task_batch, eval_meta_batch,
                          meta_step, start_time)
                start_time = time.time()

    def print_task_info(self, meta_step, task_step, task):
        template = 'Task {}: Iteration {}, Step {}, Loss: {:.3f}, Accuracy: {:.3f}'
        print(template.format(task,
                              meta_step+1,
                              task_step,
                              self.task_train_loss.result(),
                              self.task_train_accuracy.result()*100))

        self.task_train_loss.reset_states()
        self.task_train_accuracy.reset_states()
