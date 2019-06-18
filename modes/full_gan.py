#This mode is for the full model with all the bells and whistles.

import tensorflow as tf
from keras.layers import Input
from keras.models import Model
import network_elements as ne

@gin_configurable
def build_model(cfg={}):
    adhoc_games = Input(shape=(658,))
    input_obs = Input(shape=())
    input_act = Input(shape=())
    
    strat_vec = ne.LSTM_strategy_embedder(adhoc_games, cfg)
    agent_act = ne.generated_agent_action_taker(input_obs, strat_vec, cfg)
    same_agent = ne.discriminator(input_obs, agent_act, input_act, cfg)

    return Model(inputs=[adhoc_games, input_obs, input_act], outputs=same_agent)
