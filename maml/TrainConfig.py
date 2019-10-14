import os
import gin
from pprint import pprint


@gin.configurable
class TrainConfig(object):
    def __init__(self,
        dataset,
        num_task,
        num_classes,
        num_support_shots,
        num_query_shots,
        num_shots,
        batch_size,
        num_meta_train,
        num_task_train,
        meta_lr,
        task_lr,
        reduce_lr_rate,
        patience,
        num_process,
        num_verbose_interval):

        dataset = "ganabi"
        if dataset == "ganabi":
            num_classes = num_task

        # Dataset
        self.dataset = dataset
  
        # MAML hyper params
        self.num_tasks = num_task
        self.num_classes = num_classes
        self.num_support_shots = num_support_shots
        self.num_query_shots = num_query_shots
        self.num_shots = num_shots         # TODO: Remove num_shots
        self.batch_size = batch_size
        
        # Iterations
        self.num_meta_train = num_meta_train
        self.num_task_train = num_task_train

        # Learning rates
        self.meta_lr = meta_lr
        self.task_lr = task_lr
        self.reduce_lr_rate = reduce_lr_rate
        self.patience = patience

        # Less Likely to change
        self.num_process = num_process
        self.num_verbose_interval = num_verbose_interval
        self.data_dir = os.path.join(os.getcwd(), "data")

        # All Configuration
        self.config = {
            "dataset": self.dataset,
            "num_tasks": self.num_tasks,
            "num_classes": self.num_classes,
            "num_support_shots": self.num_support_shots,
            "num_query_shots": self.num_query_shots,
            "num_shots": self.num_shots,
            "batch_size": self.batch_size,
            "num_meta_train": self.num_meta_train,
            "num_task_train": self.num_task_train,
            "meta_lr": self.meta_lr,
            "task_lr": self.task_lr,
            "reduce_lr_rate": self.reduce_lr_rate,
            "patience": self.patience,
            'num_process': self.num_process,
            "num_verbose_interval": self.num_verbose_interval,
            "data_dir": self.data_dir,
        }

    def get(self, key):
        if key not in self.config.keys():
            raise("Unknown Key to recieve when Retrieving config")

        return self.config[key]

    def get_config(self):
        return self.config