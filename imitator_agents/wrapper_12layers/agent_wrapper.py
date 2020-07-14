import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.realpath(__file__))))

import numpy as np
from experts.rainbow_models.run_experiment import format_legal_moves

import keras
from keras.layers import Input, Dense, ReLU, Dropout, Softmax, BatchNormalization
from keras.models import Model, load_model
from keras.optimizers import Adam

def choose_legal_action(action_raw, obs):
  while True:
      action_idx = np.argmax(action_raw)
      if action_idx in obs['legal_moves_as_int']:
          action_leg_idx = obs['legal_moves_as_int'].index(action_idx)
          break
      else:
          action_raw[0][action_idx] = np.NINF
  return action_leg_idx

class Agent():
  """
  path_to_my_model - file that saved the model
  """
  def __init__(self, path_to_my_model):
    """Initialize the agent."""
    # self.pre_trained = keras.models.load_model(path_to_my_model)

    input = Input(shape=(658, ))
    h1 = BatchNormalization()(Dense(2048)(input))
    h2 = BatchNormalization()(Dense(2048)(h1))
    h3 = BatchNormalization()(Dense(1024)(h2))
    h4 = BatchNormalization()(Dense(1024)(h3))
    h5 = BatchNormalization()(Dense(512)(h4))
    h6 = BatchNormalization()(Dense(512)(h5))
    h7 = BatchNormalization()(Dense(256)(h6))
    h8 = BatchNormalization()(Dense(256)(h7))
    h9 = BatchNormalization()(Dense(128)(h8))
    h10 = BatchNormalization()(Dense(128)(h9))
    h11 = BatchNormalization()(Dense(64)(h10))
    h12 = BatchNormalization()(Dense(64)(h11))
    out = Softmax()(Dense(20)(h12))
    m = Model(inputs=input, outputs=out)
    m.load_weights(path_to_my_model)
    m.compile(optimizer=Adam(lr=0), loss='categorical_crossentropy',
    metrics=['accuracy'])

    self.pre_trained = m

  def _parse_observation(self, current_player_observation):
    observation_vector = np.array(current_player_observation['vectorized']) #FIXME: this may need to be cast as np.float64
    return observation_vector

  def act(self, obs, num_moves):
    if obs['current_player_offset'] != 0:
      return None

    observation_vector = self._parse_observation(obs)
    observation_vector = observation_vector.reshape((1,658))
    action_raw = self.pre_trained.predict(observation_vector)
    action_idx = np.argmax(action_raw)
    one_hot_action_vector = [0]*num_moves

    if action_idx in obs['legal_moves_as_int']:
        action_leg_idx = obs['legal_moves_as_int'].index(action_idx)
    else:
        action_leg_idx = choose_legal_action(action_raw, obs)

    action = obs['legal_moves'][action_leg_idx]
    return action, action_idx
