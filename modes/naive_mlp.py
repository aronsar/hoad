import gin
import numpy as np
from keras.layers import Input, Activation, Dense, ReLU, Softmax
from keras.models import Model
from modes import data_generator as dg

# TODO: Fix to allow Activation Classes, not just namespace
@gin.configurable
def build_model(cfg={}):
    #TODO define your model here
    observation_input = Input(shape=(658,))
    h1 = Dense(256, activation='relu')(observation_input)
    action_output = Dense(20, activation='softmax')(h1)

    return Model(inputs=observation_input, outputs=action_output)


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
        
