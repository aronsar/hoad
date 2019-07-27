#This mode is for the full model with all the bells and whistles.

import gin
import tensorflow as tf
from keras.layers import Input
from keras.models import Model
from modes import network_elements as ne, datagenerator as dg

@gin.configurable
def build_model(cfg={}):
    adhoc_games = Input(shape=(658,))
    input_obs = Input(shape=())
    input_act = Input(shape=())
    
    strat_vec = ne.LSTM_strategy_embedder(adhoc_games, cfg)
    agent_act = ne.generated_agent_action_taker(input_obs, strat_vec, cfg)
    same_agent = ne.discriminator(input_obs, agent_act, input_act, cfg)

    return Model(inputs=[adhoc_games, input_obs, input_act], outputs=same_agent)
    
#TODO: this stuff below
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
