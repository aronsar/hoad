#This mode is for the full model with all the bells and whistles.

import tensorflow as tf
from tensorflow.keras import layers

# subclassing tf.keras.layers.Layer
class StrategyEmbedder(layers.Layer):
    def __init__(self, adhoc_games):
        self.config args = config args
        super().__init__()

    def build(self, input_shape):
        super().build(input_shape)

    def call(self, adhoc_games)
        return strat_vec

    def compute_output_shape(self, input_shape):
        return tf.TensorShape(output_shape)


class ActionTaker(layers.Layer):
    def __init__(self, input_obs, strat_vec):
        super().__init__()

    def build(self, input_shape):
        super().build(input_shape)

    def call(self, adhoc_games)
        return strat_vec

    def compute_output_shape(self, input_shape):
        return tf.TensorShape(output_shape)


class Discriminator(layers.Layer):
    def __init__(self, agent_act, input_act):
        super().__init__()

    def build(self, input_shape):
        super().build(input_shape)

    def call(self, (agent_act, input_act))
        return same_agent

    def compute_output_shape(self, input_shape):
        return tf.TensorShape(output_shape)

@gin_configurable
class Model(tf.keras.Model):
    def __init__(self, data_loader, args):
        super().__init__()

    def call(self, inputs):
        adhoc_games, input_obs, input_act = inputs
        strat_vec = StrategyEmbedder(adhoc_games)
        agent_act = ActionTaker(input_obs, strat_vec)
        same_agent = Discriminator(agent_act, input_act)
    
        return same_agent

    
    def compute_output_shape(self, input_shape):
        return tf.TensorShape(output_shape)

