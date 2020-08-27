import os
import numpy as np
import gin
import TrainConfig
from models import GanabiModel
import tensorflow.keras as tk
import tensorflow as tf



class MamlAgent:
    def __init__(self, model_path):
        config_file = 'adhoc_agents/maml/config/ganabi.config.gin'
        config_file = os.path.join(os.path.dirname(os.path.dirname(model_path)), 'config.gin')
        gin.parse_config_file(config_file)

        self.meta_model = GanabiModel(model_name="Meta")
        self.meta_model.build(input_shape=(None, 658))
        self.meta_model.load_weights(model_path)
        self.task_model = GanabiModel(model_name="Task")
        self.task_model.build(input_shape=(None, 658))
        self.task_model.load_weights(model_path)
        self.task_optimizer = tk.optimizers.SGD(learning_rate=3e-4, clipvalue=10)
        self.task_loss_op = tk.losses.SparseCategoricalCrossentropy()

    def _convert_to_support(self, imitator_games):
        # x needs to be (10, 64, 658) dimensional
        # y needs to be (10, 64) dimensional
        x, y = [], []

        for game in imitator_games:
            observations, actions = game
            num_turns = len(observations)
            while num_turns < 64:
                observations += observations
                actions += actions
                num_turns *= 2
            observations = observations[:64]
            actions = actions[:64]
            x.append(observations)
            y.append([act.index(max(act)) for act in actions])
        
        return np.array(x), np.array(y)
                

    def task_update(self, imitator_games):
        self.task_model.set_weights(self.meta_model.get_weights()) # reset task weights
        x_support, y_support = self._convert_to_support(imitator_games)
        for s_shot in range(x_support.shape[0]):
            # convert ragged tensor to normal tensor
            X = x_support[s_shot]
            Y = y_support[s_shot]

            # Step 1: Forward Pass
            with tf.GradientTape() as task_tape:
                predictions = self.task_model(X)
                loss = self.task_loss_op(Y, predictions)
            grads = task_tape.gradient(loss, self.task_model.trainable_variables)

            # Step 2: Update params
            self.task_optimizer.apply_gradients(
                    zip(grads, self.task_model.trainable_variables))

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
        action_raw = self.task_model.predict(observation_vector)
        action_idx = np.argmax(action_raw)
        one_hot_action_vector = [0]*num_moves
        if action_idx in obs['legal_moves_as_int']:
            action_leg_idx = obs['legal_moves_as_int'].index(action_idx)
        else:
            action_leg_idx = self._choose_legal_action(action_raw, obs)
        action = obs['legal_moves'][action_leg_idx]
        return action, action_idx


