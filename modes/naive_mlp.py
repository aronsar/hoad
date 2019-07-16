from utils import parse_args
from utils import dir_utils
import gin
from subprocess import call
import random
import numpy as np
import keras
import os, glob, re
import datagenerator as dg

@gin.configurable
def build_model(cfg={}):
    #TODO define your model here
    # return Model(inputs=, outputs=)
    return None

@gin.configurable
class DataGenerator(dg.BaseDataGenerator):
    @gin.configurable
    def __init__(self, raw_data, cfg={}):
        # Set Before calling init of base class
        self.batch_size = cfg["batch_size"]
        self.shuffle = cfg["shuffle"]
        self.obs_dim = cfg["obs_dim"]
        self.act_dim = cfg["act_dim"]
        self.verbose = cfg["verbose"]

        super().__init__(raw_data)

    """
    FYI: This is an example of overriding methods in BaseDataGenerator
    """
    def batch_sampler(self, index):        
        """
        batch_sampler(self)
        Args: None
        Usage:
            Specificies the logic of sampling the data. This is an implementation of random sampling with repitive possibility
        """

        return np.random.randint(low=0, high=self.epoch_len, size=self.batch_size)
        
