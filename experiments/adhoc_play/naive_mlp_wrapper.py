import numpy as np
from adhoc_agents.naive_mlp.mlp import Mlp
from tensorflow.keras.layers import ReLU, Softmax

class Agent:
    def __init__(self, model_path):
        hypers = {'lr': 0.00015,
              'batch_size': 128,
              'hl_activations': [ReLU, ReLU, ReLU, ReLU],
              'hl_sizes': [512,512,512,256],
              'decay': 0.,
              'bNorm': True,
              'dropout': True,
              'regularizer': None}
        
        self.m = Mlp(
        io_sizes=(658, 20),
        out_activation=Softmax, loss='categorical_crossentropy',
        metrics=['accuracy'], **hypers, verbose=1)

        self.m.construct_model(model_path, weights_only=True)


    def _choose_legal_action(self, action_raw, obs):
        while True:
            action_idx = np.argmax(action_raw)
            if action_idx in obs['legal_moves_as_int']:
                action_leg_idx = obs['legal_moves_as_int'].index(action_idx)
                break
            else:
                action_raw[0][action_idx] = np.NINF
        return action_leg_idx

    def _parse_observation(self, current_player_observation):
        observation_vector = np.array(current_player_observation['vectorized'])
        return observation_vector

    def act(self, obs, num_moves):
        if obs['current_player_offset'] != 0:
            return None

        observation_vector = self._parse_observation(obs)
        observation_vector = observation_vector.reshape((1,658))
        action_raw = self.m.model.predict(observation_vector)
        action_idx = np.argmax(action_raw)
        one_hot_action_vector = [0]*num_moves
        if action_idx in obs['legal_moves_as_int']:
            action_leg_idx = obs['legal_moves_as_int'].index(action_idx)
        else:
            action_leg_idx = self._choose_legal_action(action_raw, obs)
        action = obs['legal_moves'][action_leg_idx]
        return action, action_idx


