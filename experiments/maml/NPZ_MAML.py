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
import shutil
import logging


def print_seperator(logger, seperator, count):
    seperator = seperator * count
    print(seperator)
    logger.warning(seperator)


class MAML(object):
    def __init__(self, config_obj):
        # Dataset
        self.dataset = config_obj.get("dataset")

        # MAML Hyper Params
        self.num_tasks = config_obj.get("num_tasks")
        self.num_classes = config_obj.get("num_classes")
        self.train_support = config_obj.get("train_support")
        self.train_query = config_obj.get("train_query")
        self.test_support = config_obj.get("test_support")
        self.test_query = config_obj.get("test_query")
        self.batch_size = config_obj.get("batch_size")

        # Learning Rates
        self.meta_lr = config_obj.get("meta_lr")
        self.task_lr = config_obj.get("task_lr")
        self.reduce_lr_rate = config_obj.get("reduce_lr_rate")
        self.patience = config_obj.get("patience")

        # Train Iter
        self.num_verbose_interval = config_obj.get("num_verbose_interval")
        self.num_task_train = config_obj.get("num_task_train")
        self.num_meta_train = config_obj.get("num_meta_train")

        self.test_agent = config_obj.get("test_agent")

        self.init_tk_params()
        self.init_models()
        self.init_tensorboard_params()

        # Logger
        formatter = logging.Formatter('%(message)s')

        fn = logging.FileHandler(
            filename=self.base_dir + 'logger.log', mode='w')
        fn.setLevel(logging.INFO)
        fn.setFormatter(formatter)

        self.logger = logging.getLogger('Ganabi Agents')
        self.logger.addHandler(fn)

        # Others
        self.best_eval_acc = 0.0  # controls model saving

    def init_tensorboard_params(self):
        #  Summary Writer for Tensorboard
        current_time = datetime.datetime.now().strftime("%Y%m%d-%H%M%S")
        self.base_dir = 'result/{}/{}/'.format(self.test_agent, current_time)
        self.log_dir = self.base_dir + 'logs/'
        self.ckpt_dir = self.base_dir + 'ckpt/'
        self.save_path = self.base_dir + 'weights/'
        if not os.path.isdir(self.save_path):
            os.makedirs(self.save_path, exist_ok=True)
        self.summary_writer = tf.summary.create_file_writer(self.log_dir)
        self.checkpoint = tf.train.Checkpoint(
            optimizer=self.meta_optimizer, model=self.model)
        self.ckpt_manager = tf.train.CheckpointManager(
            self.checkpoint, directory=self.ckpt_dir, max_to_keep=5)

    def init_tk_params(self):
        self.task_lr_schedule = tk.optimizers.schedules.ExponentialDecay(
            self.task_lr,  # initial_lr
            decay_steps=self.patience,
            decay_rate=self.reduce_lr_rate,
            staircase=True
        )

        self.meta_lr_schedule = tk.optimizers.schedules.ExponentialDecay(
            self.meta_lr,  # initial_lr
            decay_steps=self.patience,
            decay_rate=self.reduce_lr_rate,
            staircase=True
        )

        self.task_loss_op = tk.losses.SparseCategoricalCrossentropy()
        self.task_optimizer = tk.optimizers.SGD(
            self.task_lr_schedule, clipvalue=10)
        self.task_loss = tk.metrics.Mean(name='task_loss')
        self.task_accuracy = tk.metrics.SparseCategoricalAccuracy(
            name='task_accuracy')

        self.train_loss = tk.metrics.Mean(name='train_loss')
        self.train_accuracy = tk.metrics.Mean(name='train_accuracy')

        # Meta specific
        self.meta_loss_op = tk.losses.SparseCategoricalCrossentropy()
        self.meta_optimizer = tk.optimizers.Adam(
            self.meta_lr_schedule, clipvalue=10, amsgrad=True)
        self.eval_loss = tk.metrics.Mean(name='eval_loss')
        self.eval_accuracy = tk.metrics.SparseCategoricalAccuracy(
            name='eval_accuracy')

    def init_models(self):
        '''
        Build / Instantiate TF Keras Models
        '''

        # Build models
        if self.dataset == 'omniglot':
            self.model = models.SimpleOmniglotModel(self.num_classes)
            self.task_models = [models.SimpleOmniglotModel(self.num_classes)
                                for _ in range(self.num_tasks)]
            # input_dim = (self.num_classes, 28, 28, 1)
            input_dim = (None, 28, 28, 1)
        elif self.dataset == 'ganabi':
            self.model = models.GanabiModel(model_name="Meta")
            self.task_models = [models.GanabiModel(model_name="Task{}".format(i))
                                for i in range(self.num_tasks)]
            # input_dim = (1, 658)
            input_dim = (None, 658)
        else:
            raise("Unknown dataset {}. No appropriate model architechure.".format(
                self.dataset))

        # Meta Model
        self.model.build(input_shape=input_dim)

        # Task Models
        for i, model in enumerate(self.task_models):
            self.task_models[i].build(input_shape=input_dim)

        print(self.model.summary())

    def init_agent_metrics(self, agent_names):
        self.agent_metric = {}

        for agent_name in agent_names:
            self.agent_metric[agent_name] = {
                'loss': tk.metrics.Mean(name="{}-loss".format(agent_name)),
                'acc': tk.metrics.Mean(name="{}-acc".format(agent_name)),
                'count': 0
            }

    def save_gin_config(self, config_file):
        shutil.copyfile(config_file, self.base_dir + 'config.gin')

    @tf.function(input_signature=(tf.TensorSpec(shape=[None, 658], dtype=tf.float32),
                                  tf.TensorSpec(
                                      shape=[None, ], dtype=tf.int32),
                                  tf.TensorSpec(
                                      shape=[None, 658], dtype=tf.float32),
                                  tf.TensorSpec(shape=[None, ], dtype=tf.int32)))
    def train_task(self, x_support, y_support, x_query, y_query):
        # Retrace Makes the code 20 times slower
        print("##### Retrace Train Task {:>3d} #####".format(self.task_id))
        # print(x_support.shape, y_support.shape, x_query.shape, y_query.shape)

        # Support Set
        # Step 1: Forward Pass
        with tf.GradientTape() as task_tape:
            predictions = self.task_models[self.task_id](x_support)
            loss = self.task_loss_op(y_support, predictions)

        grads = task_tape.gradient(
            loss, self.task_models[self.task_id].trainable_variables)

        # Step 2: Update params
        self.task_optimizer.apply_gradients(
            zip(grads, self.task_models[self.task_id].trainable_variables))

        # Query Set
        # Step 1: Forward Pass
        with tf.GradientTape() as task_tape:
            predictions = self.task_models[self.task_id](x_query)
            loss = self.task_loss_op(y_query, predictions)

        grads = task_tape.gradient(
            loss, self.task_models[self.task_id].trainable_variables)

        # Step 2: Record Gradients for Meta Gradients
        self.task_loss(loss)
        self.task_accuracy(y_query, predictions)

        return grads

    @tf.function
    def train_meta(self, tasks_gradients):
        print("##### Retrace Train Meta #####")

        # Step 3 : get gFOMAML
        meta_gradients = []
        for i in range(len(tasks_gradients[0])):
            meta_grads = []
            for task in range(0, self.num_tasks):
                meta_grads.append(tasks_gradients[task][i])

            tf.stack(meta_grads)
            meta_grads = tf.math.reduce_mean(meta_grads, axis=0)
            meta_gradients.append(meta_grads)

        # Apply Gradient on meta model
        self.meta_optimizer.apply_gradients(zip(meta_gradients,
                                                self.model.trainable_variables))
        return meta_gradients

    @tf.function(input_signature=(tf.TensorSpec(shape=[None, 658], dtype=tf.float32),
                                  tf.TensorSpec(
                                      shape=[None, ], dtype=tf.int32),
                                  tf.TensorSpec(
                                      shape=[None, 658], dtype=tf.float32),
                                  tf.TensorSpec(shape=[None, ], dtype=tf.int32)))
    def eval_task(self, x_support, y_support, x_query, y_query):
        # Retrace Makes the code 20 times slower
        print("##### Retrace Eval Task {:>3d} ######".format(self.task_id))

        # Support Set
        with tf.GradientTape() as task_tape:
            predictions = self.task_models[self.task_id](x_support)
            loss = self.task_loss_op(y_support, predictions)

        grads = task_tape.gradient(
            loss, self.task_models[self.task_id].trainable_variables)

        # Step 2: Update params
        self.task_optimizer.apply_gradients(
            zip(grads, self.task_models[self.task_id].trainable_variables))

        # Query Set
        # Step 1: Forward Pass
        with tf.GradientTape() as task_tape:
            predictions = self.task_models[self.task_id](x_query)
            loss = self.task_loss_op(y_query, predictions)

        # Step 2: Record Gradients for Meta Gradients
        self.eval_loss(loss)
        self.eval_accuracy(y_query, predictions)

    def train_step(self, train_batch, train_classes, step):
        # Reset Weights
        self.reset_task_weights()
        # Train Task
        tasks_gradients = []
        for task in range(self.num_tasks):
            x_support, y_support, x_query, y_query = train_batch[task]

            if step % (self.num_verbose_interval) == 0:
                tf.summary.trace_on(graph=True, profiler=False)

            self.task_id = task
            grads = self.train_task(x_support,
                                    y_support,
                                    x_query,
                                    y_query)

            if step % (self.num_verbose_interval) == 0:
                with self.summary_writer.as_default():
                    tf.summary.trace_export(name="train_task_trace",
                                            step=step,
                                            profiler_outdir=self.log_dir)

            tasks_gradients.append(grads)

            # Handle loss acc for each agent
            loss = self.task_loss.result()
            acc = self.task_accuracy.result() * 100
            self.task_loss.reset_states()
            self.task_accuracy.reset_states()

            self.train_loss(loss)
            self.train_accuracy(acc)

            if self.dataset == "ganabi":
                agent_name = train_classes[task]
                self.agent_metric[agent_name]['loss'](loss)
                self.agent_metric[agent_name]['acc'](acc)
                self.agent_metric[agent_name]['count'] += 1

        # Train Meta
        if step % (self.num_verbose_interval) == 0:
            tf.summary.trace_on(graph=True, profiler=False)

        meta_grads = self.train_meta(tasks_gradients)

        if step % (self.num_verbose_interval) == 0:
            with self.summary_writer.as_default():
                tf.summary.trace_export(name="train_meta_trace",
                                        step=step,
                                        profiler_outdir=self.log_dir)

        # Record Metrics
        if step % (self.num_verbose_interval / 5) == 0:
            train_loss, train_acc = self.record_metrics(step, is_train=True)

            # Print and Log
            template = 'Train - Iteration {:>7d}, Loss: {:>7.3f}, Accuracy: {:>7.3f}'
            print(template.format(step, train_loss, train_acc))
            self.logger.warning(template.format(step, train_loss, train_acc))

    def eval_step(self, eval_batch, eval_classes, step):
        # Reset Weights
        self.reset_task_weights()

        # Eval Task
        for task in range(len(eval_batch)):
            x_support, y_support, x_query, y_query = eval_batch[task]
            self.task_id = task
            self.eval_task(x_support,
                           y_support,
                           x_query,
                           y_query)

        # Record Metrics
        eval_loss, eval_acc = self.record_metrics(step, is_train=False)

        # Print and Log
        template = 'Test - Iteration {:>7d}, Loss: {:>7.3f}, Accuracy: {:>7.3f}'
        print(template.format(step, eval_loss, eval_acc))
        self.logger.warning(template.format(step, eval_loss, eval_acc))

        return eval_loss, eval_acc

    def record_metrics(self, meta_step, is_train=True):
        '''
        Record Metrics at Tensorboard
        Includes: accuracy & loss at train or eval phase
        '''
        if is_train:
            # Record & Reset train loss & train acc
            train_loss = self.train_loss.result()
            train_acc = self.train_accuracy.result()
            with self.summary_writer.as_default():
                tf.summary.scalar('train_loss', train_loss, step=meta_step)
                tf.summary.scalar('train_accuracy', train_acc, step=meta_step)
            self.train_loss.reset_states()
            self.train_accuracy.reset_states()

            return train_loss, train_acc
        else:
            # Record & Reset eval loss & eval acc
            eval_loss = self.eval_loss.result()
            eval_acc = self.eval_accuracy.result() * 100
            with self.summary_writer.as_default():
                tf.summary.scalar('eval_loss', eval_loss, step=meta_step)
                tf.summary.scalar('eval_accuracy', eval_acc, step=meta_step)
            self.eval_loss.reset_states()
            self.eval_accuracy.reset_states()

            if self.dataset == 'ganabi':
                self.record_agent_metrics(meta_step)

            return eval_loss, eval_acc

    def record_agent_metrics(self, step):
        print_seperator(self.logger, seperator="\n", count=1)

        for agent_name, value in self.agent_metric.items():
            # Get All Metrics
            loss = value['loss'].result()
            acc = value['acc'].result()
            count = value['count']

            # Print & Log
            template = "Agent: {:>17} Loss: {:>7.3f} Acc {:>7.3f} Count: {:>3}"
            print(template.format(agent_name, loss, acc, count))
            self.logger.warning(template.format(agent_name, loss, acc, count))

            # Record to Tensorboard
            with self.summary_writer.as_default():
                tf.summary.scalar(
                    '{}-loss'.format(agent_name), loss, step=step)
                tf.summary.scalar('{}-acc'.format(agent_name), acc, step=step)

            # Reset
            value['loss'].reset_states()
            value['acc'].reset_states()
            value['count'] = 0

        print_seperator(self.logger, seperator="\n", count=1)

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
        print("######### NPZ MAML ##############")
        for meta_step in range(self.num_meta_train):

            # Train
            # start_time = time.time()
            train_batch, train_classes = data_generator.next_batch(
                is_train=True)
            # print(f"Sample Time {time.time() - start_time}")

            # start_time = time.time()
            self.train_step(train_batch, train_classes, meta_step)
            # print(f"Train Time {time.time() - start_time}")

            # Eval
            if meta_step % self.num_verbose_interval == 0:
                # Eval
                eval_batch, eval_classes = data_generator.next_batch(
                    is_train=False)
                eval_loss, eval_acc = self.eval_step(
                    eval_batch, eval_classes, meta_step)

                # Save model
                if self.best_eval_acc < eval_acc:
                    self.best_eval_acc = eval_acc
                    self.ckpt_manager.save()
                    self.model.save_weights(
                        self.save_path + "/{}-weights.h5".format(meta_step))
                else:
                    if meta_step % 10000 == 0:
                        self.ckpt_manager.save()
                        self.model.save_weights(
                            self.save_path + "/{}-weights.h5".format(meta_step))

                # Print
                template = 'Time to finish {:>4d} Meta Updates: {:>7.3f}'
                print(template.format(self.num_verbose_interval,
                                      (time.time() - start_time)))
                self.logger.warning(template.format(self.num_verbose_interval,
                                                    (time.time() - start_time)))

                # seperator = "#" * 100
                # print(seperator)
                # self.logger.warning(seperator)
                print_seperator(self.logger, seperator="#", count=100)

                start_time = time.time()
