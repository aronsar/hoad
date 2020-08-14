import os
import gin
from pprint import pprint


@gin.configurable
class TrainConfig(object):
    def __init__(self,
                 dataset,
                 test_agent,
                 num_task,
                 num_classes,
                 train_support,
                 train_query,
                 test_support,
                 test_query,
                 batch_size,
                 shuffle,
                 data_type,
                 data_path,
                 data_preprocess,
                 num_meta_train,
                 num_task_train,
                 meta_lr,
                 task_lr,
                 reduce_lr_rate,
                 patience,
                 num_process,
                 num_verbose_interval,
                 obs_dim,
                 act_dim):

        all_agents = [
            "flawed",
            "outer",
            "quux_cheatbot",
            "quux_newcheatbot",
            "rainbow",
            "iggi",
            "piers",
            "quux_holmesbot",
            "quux_simplebot",
            "van_den_bergh",
            "legal_random",
            "quux_blindbot",
            "quux_infobot",
            "quux_valuebot",
            "WTFWT"
        ]

        if dataset == "ganabi":
            num_classes = num_task

            #if test_agent not in all_agents:
            #    raise("Unknown Test Agent {}".format(test_agent))

        # Dataset
        self.dataset = dataset
        self.test_agent = test_agent

        # MAML hyper params
        self.num_tasks = num_task
        self.num_classes = num_classes
        self.train_support = train_support
        self.train_query = train_query
        self.test_support = test_support
        self.test_query = test_query
        self.batch_size = batch_size
        self.shuffle = shuffle
        self.data_type = data_type
        self.data_path = data_path
        self.data_preprocess = data_preprocess

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
        self.obs_dim = obs_dim
        self.act_dim = act_dim

        # All Configuration
        self.config = {
            "dataset": self.dataset,
            "test_agent": self.test_agent,
            "num_tasks": self.num_tasks,
            "num_classes": self.num_classes,
            "train_support": self.train_support,
            "train_query": self.train_query,
            "test_support": self.test_support,
            "test_query": self.test_query,
            "batch_size": self.batch_size,
            "shuffle": self.shuffle,
            "data_type": self.data_type,
            "data_path": self.data_path,
            "data_preprocess": self.data_preprocess,
            "num_meta_train": self.num_meta_train,
            "num_task_train": self.num_task_train,
            "meta_lr": self.meta_lr,
            "task_lr": self.task_lr,
            "reduce_lr_rate": self.reduce_lr_rate,
            "patience": self.patience,
            'num_process': self.num_process,
            "num_verbose_interval": self.num_verbose_interval,
            'obs_dim': self.obs_dim,
            "act_dim": self.act_dim,
            "data_dir": self.data_dir,
        }

        pprint(self.config)

    def get(self, key):
        if key not in self.config.keys():
            raise("Unknown Key to recieve when Retrieving config")

        return self.config[key]

    def get_config(self):
        return self.config
