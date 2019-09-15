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

    @tf.function
    def task_train_step(self, images, labels):
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
    def meta_train_step(self, images, labels, task_models, original_weights):
        # tf.print(original_weights[1])
        # for task in range(self.TASK_NUM):
        #     task_weights = task_models[task]
        #     tf.print(task)
        #     tf.print(task_weights[1])

        with tf.GradientTape() as tape:
            meta_loss = []
            for task in range(self.TASK_NUM):
                task_weights = task_models[task]
                # Set original meta weights to task weight
                for i, (new, old) in enumerate(zip(self.model.trainable_variables, task_weights)):
                    new.assign(old)

                predictions = self.model(images[task])
                task_loss = self.task_loss_op(labels[task], predictions)
                meta_loss.append(task_loss)  # Better Approach ?

            # This should be first order MAML. Verified that the calculation is expected
            meta_loss = tf.math.reduce_sum(meta_loss) / tf.cast(tf.shape(meta_loss)[0],
                                                                dtype=tf.float32)

        # Reset to original meta weights before apply gradients
        for i, (new, old) in enumerate(zip(self.model.trainable_variables, original_weights)):
            new.assign(old)

        gradients = tape.gradient(meta_loss, self.model.trainable_variables)
        gradients = [tf.clip_by_value(
            grads, -10, 10) for grads in gradients]
        self.meta_optimizer.apply_gradients(
            zip(gradients, self.model.trainable_variables))

    @tf.function
    def eval_step(self, images, labels):
        # After update - pure eval, no gradient update
        with tf.GradientTape() as tape:
            predictions = self.model(images)
            loss = self.meta_loss_op(labels, predictions)

        # tf.print(predictions)
        # tf.print(labels)
        self.meta_train_loss(loss)
        self.meta_train_accuracy(labels, predictions)

    def train(self, data_generator):
        start_time = time.time()
        for meta_step in range(self.META_TRAIN_STEPS):

            # Save original meta weights
            original_weights = [weight.numpy() for weight in
                                self.model.trainable_variables]

            # print("Original###################################")
            # print(original_weights[-1])
            # print("New ########################################")
            # Generate Data
            if meta_step % self.VERBOSE_INTERVAL == 0:
                train_batch, eval_batch = data_generator.sample_batch(
                    is_train=True, is_eval=True)
            else:
                train_batch, _ = data_generator.sample_batch(
                    is_train=True, is_eval=False)

            # Update / Train tasks weight
            task_weights = []
            for task in range(self.TASK_NUM):
                for task_step in range(self.TASK_TRAIN_STEPS):
                    for shot in range(self.K_SHOTS_NUM):
                        x_task_train, y_task_train = train_batch[task][
                            0][shot], train_batch[task][1][shot]
                        self.task_train_step(x_task_train, y_task_train)
                    # if meta_step % self.VERBOSE_INTERVAL == 0:
                    #     self.print_task_info(meta_step, task_step, task)

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
                x_task_train, y_task_train = train_batch[task][0][-1], train_batch[task][1][-1]
                # Select only one image for meta update ?
                x_task_train = x_task_train[0]
                y_task_train = y_task_train[0]
                x_meta_train.append(x_task_train)
                y_meta_train.append(y_task_train)

            # Select only one image for meta update
            x_meta_train = np.array(x_meta_train)
            y_meta_train = np.array(y_meta_train)
            x_meta_train = np.expand_dims(x_meta_train, axis=1)
            y_meta_train = np.expand_dims(y_meta_train, axis=1)

            # print(y_meta_train.shape, x_meta_train.shape)

            # Update meta weight
            self.meta_train_step(x_meta_train, y_meta_train,
                                 task_weights, original_weights)

            if meta_step % self.VERBOSE_INTERVAL == 0:
                self.eval(eval_batch, meta_step, start_time)
                start_time = time.time()  # Restart time after printing

    def eval(self, eval_batch, meta_step, start_time):
        avg_acc, avg_loss = 0, 0
        for task in range(self.TASK_NUM):
            x_task_eval, y_task_eval = eval_batch[task][0][0], eval_batch[task][1][0]

            # Select only image for eval
            x_task_eval, y_task_eval = x_task_eval[0], y_task_eval[0]
            x_task_eval = np.expand_dims(x_task_eval, axis=0)
            y_task_eval = np.expand_dims(y_task_eval, axis=0)

            self.eval_step(x_task_eval, y_task_eval)

            avg_loss += self.meta_train_loss.result()
            avg_acc += self.meta_train_accuracy.result() * 100
            self.meta_train_loss.reset_states()
            self.meta_train_accuracy.reset_states()

        avg_acc, avg_loss = avg_acc / self.TASK_NUM, avg_loss / self.TASK_NUM
        template = 'Meta  : Iteration {}, Loss: {:.3f}, Accuracy: {:.3f}, Time: {:.3f}'
        print(template.format(meta_step,
                              avg_loss,
                              avg_acc,
                              (time.time() - start_time)))

    def print_task_info(self, meta_step, task_step, task):
        template = 'Task {}: Iteration {}, Step {}, Loss: {:.3f}, Accuracy: {:.3f}'
        print(template.format(task,
                              meta_step+1,
                              task_step,
                              self.task_train_loss.result(),
                              self.task_train_accuracy.result()*100))

        self.task_train_loss.reset_states()
        self.task_train_accuracy.reset_states()
