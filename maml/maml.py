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
        self.PATIENCE = config.get("num_patience")
        self.REDUCE_LR_RATE = config.get("reduce_lr_rate")

        # self.task_lr = 1e-4  # Later Verify this self.task_lr = 1e-4 / self.TASK_TRAIN_STEPS
        self.task_lr = config.get("task_lr")
        self.meta_lr = config.get("meta_lr")

        # Check more at https://www.tensorflow.org/api_docs/python/tf/keras/optimizers/schedules/ExponentialDecay
        self.meta_lr_schedule = tk.optimizers.schedules.ExponentialDecay(
            self.meta_lr,  # initial_lr
            decay_steps=self.PATIENCE,
            decay_rate=self.REDUCE_LR_RATE,
            staircase=True
        )

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
            self.meta_lr_schedule, clipvalue=10, amsgrad=True)
        self.meta_train_loss = tk.metrics.Mean(name='meta_train_loss')
        self.meta_train_accuracy = tk.metrics.SparseCategoricalAccuracy(
            name='meta_train_accuracy')

        # Summary Writer for Tensorboard
        current_time = datetime.datetime.now().strftime("%Y%m%d-%H%M%S")
        self.log_dir = 'logs/' + current_time
        self.save_path = 'ckpt/' + current_time
        # if not os.path.isdir(self.log_dir):
        #     os.mkdir(self.log_dir)
        if not os.path.isdir(self.save_path):
            os.mkdir(self.save_path)
        self.summary_writer = tf.summary.create_file_writer(self.log_dir)
        self.best_eval_acc = 0.0  # controls model saving

        # Build models
        # Meta Model
        self.model = models.SimpleModel(self.N_WAYS_NUM)
        # T Tasks models
        self.task_models = [models.SimpleModel(self.N_WAYS_NUM)
                            for _ in range(self.TASK_NUM)]
        self.INPUT_DIM = (self.N_WAYS_NUM, 28, 28, 1)  # TODO: Feed from config
        self.instantiate_models(self.INPUT_DIM)

    def instantiate_models(self, input_dim):
        '''
        Build / Instantiate TF Keras Models
        '''
        # Meta Model
        self.model.build(input_dim)

        # Task Models
        for i, model in enumerate(self.task_models):
            self.task_models[i].build(input_dim)

    @tf.function
    def compute_gradients(self, images, labels, model):
        '''
        Compute Gradients

        Return:
            loss
            predictions
            grads
        '''
        with tf.GradientTape() as task_tape:
            predictions = model(images)
            loss = self.task_loss_op(labels, predictions)

        grads = task_tape.gradient(loss, model.trainable_variables)
        # clipped at optimizer for now
        # clipped_grads = [tf.clip_by_value(g, -10, 10) for g in grads]

        return loss, predictions, grads

    @tf.function
    def train_fomaml(self, train_batch):
        """
        Low Level Overview of Training FOMAML.
        """

        tasks_gradients = []
        for task in range(self.TASK_NUM):
            for shot in range(0, self.K_SHOTS_NUM + 1):
                images = train_batch[task][0][shot]
                labels = train_batch[task][1][shot]
                for _ in range(self.TASK_TRAIN_STEPS):
                    # Step 1: Forward Pass
                    loss, predictions, grads = self.compute_gradients(
                        images, labels, self.task_models[task])

                    # Step 2: Update params
                    if shot < self.K_SHOTS_NUM:
                        self.task_optimizer.apply_gradients(
                            zip(grads, self.task_models[task].trainable_variables))
                    else:
                        self.task_train_loss(loss)
                        self.task_train_accuracy(labels, predictions)
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

        # Apply Gradient on meta model
        self.meta_optimizer.apply_gradients(zip(meta_gradients,
                                                self.model.trainable_variables))

    @tf.function
    def eval_fomaml(self, eval_batch):
        """
        Low Level Overview of Evaluating FOMAML.
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

    def record_metrics(self, meta_step, is_train=True):
        '''
        Record Metrics at Tensorboard
        Includes: accuracy & loss at train or eval phase
        '''
        if is_train:
            # Record & Reset train loss & train acc
            train_loss = self.task_train_loss.result()
            train_acc = self.task_train_accuracy.result() * 100
            with self.summary_writer.as_default():
                tf.summary.scalar('train_loss', train_loss, step=meta_step)
                tf.summary.scalar('train_accuracy', train_acc, step=meta_step)
            self.task_train_loss.reset_states()
            self.task_train_accuracy.reset_states()

            return train_loss, train_acc
        else:
            # Record & Reset eval loss & eval acc
            eval_loss = self.meta_train_loss.result()
            eval_acc = self.meta_train_accuracy.result() * 100
            with self.summary_writer.as_default():
                tf.summary.scalar('eval_loss', eval_loss, step=meta_step)
                tf.summary.scalar('eval_accuracy', eval_acc, step=meta_step)
            self.meta_train_loss.reset_states()
            self.meta_train_accuracy.reset_states()

            return eval_loss, eval_acc

    def reset_task_weights(self):
        '''
        Reset all trainable variables in the task model to be equivalent to meta model

        Reason to not place this inside train_fomaml / eval_fomaml
        => Cannot set_weights in a @tf.function
        '''
        for i, model in enumerate(self.task_models):
            self.task_models[i].set_weights(self.model.get_weights())

    def train_manager(self, data_generator):
        """
        High Level Overview of training MAML
        """

        start_time = time.time()
        for meta_step in range(self.META_TRAIN_STEPS):
            # Train
            train_batch = data_generator.next_batch_v2(is_train=True)
            self.reset_task_weights()
            self.train_fomaml(train_batch)
            _, _ = self.record_metrics(meta_step, is_train=True)

            # Eval
            if meta_step % self.VERBOSE_INTERVAL == 0:
                # Eval
                eval_batch = data_generator.next_batch_v2(is_train=False)
                self.reset_task_weights()
                self.eval_fomaml(eval_batch)
                eval_loss, eval_acc = self.record_metrics(
                    meta_step, is_train=False)

                # Save model
                if self.best_eval_acc < eval_acc:
                    self.best_eval_acc = eval_acc
                    self.model.save_weights(
                        self.save_path + "/{}-weights.h5".format(meta_step))

                # Print
                template = 'Meta  : Iteration {}, Loss: {:.3f}, Accuracy: {:.3f}, Time: {:.3f}'
                print(template.format(meta_step, eval_loss, eval_acc,
                                      (time.time() - start_time)))
                start_time = time.time()
