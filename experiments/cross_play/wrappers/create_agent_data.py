from hanabi_env import rl_env
from experiments.cross_play.wrappers import agent_wrapper


def one_hot_vectorized_action(agent, num_moves, obs):
    action, action_idx = agent.act(obs,num_moves)
    one_hot_action_vector = [0]*num_moves
    one_hot_action_vector[action_idx] = 1

    return one_hot_action_vector, action

class DataCreator(object):
    def __init__(self, num_games, path_model_0, path_model_1 ):
        config = {'colors': 5,
          'ranks': 5,
          'players': 2,
          'hand_size': 5,
          'max_information_tokens': 8,
          'max_life_tokens': 3,
          'seed': 1,
          'observation_type': 1,  # FIXME: NEEDS CONFIRMATION
          'random_start_player': False}
        self.num_players = 2
        self.num_games = num_games
        self.environment = rl_env.HanabiEnv(config)
        self.agent_object = []
        self.agent_object.append(agent_wrapper.Agent(path_model_0))
        self.agent_object.append(agent_wrapper.Agent(path_model_1))

    def create_data(self):
        raw_data = []
        scores = []
        for game_num in range(self.num_games):
            raw_data.append([[],[]])
            observations = self.environment.reset()
            game_done = False

            while not game_done:
                for agent_id in range(self.num_players):
                    observation = observations['player_observations'][agent_id]
                    one_hot_action_vector, action = one_hot_vectorized_action(
                            self.agent_object[agent_id],
                            self.environment.num_moves(),
                            observation)
                    if observation['current_player'] == agent_id:
                        assert action is not None
                        current_player_action = action
                    else:
                        assert action is None

                    observations, _, game_done, _ = self.environment.step(
                            current_player_action)
                    if game_done:
                        scores.append(self.environment.state.score())
                        break
        return scores
