# this script creates data using python 2 and rainbow agents

from utils import dir_utils
from utils import parse_args
from collections import defaultdict
import pickle
import sys
sys.path.insert() #FIXME
from hanabi_env import rl_env


def one_hot_vectorized_action(agent, obs):
    action = agent.act(obs)
    one_hot_vector = [0]*len(obs['legal_moves'])
    action_idx = obs['legal_moves_as_int'][obs['legal_moves'].index(action)]
    one_hot_vector[action_idx] = 1

    return one_hot_vector, action

@gin_configurable
class DataCreator:
    def __init__(self, args
            num_players=2
            num_games=50
            game_type='Hanabi-Full'):
        self.num_players = num_players
        self.num_games = num_games
        self.environment = rl_env.make(game_type, num_players=self.num_players)
        self.agent_config = {
                'players': self.num_players,
                'num_moves': environment.num_moves(),
                'observation_size': environment.vectorized_observation_shape()[0]}


    def create_data():
        raw_data = defaultdict(list)

        for playing_agent in available_agents:
            for game_num in range(self.num_games):
                raw_data[playing_agent].append([[],[]])
                observations = environment.reset()
                game_done = False

                while not game_done:
                    for agent_id in range(self.num_players):
                        observation = observations['player_observations'][agent_id]
                        action_vec, action = one_hot_vectorized_action(
                                AGENT_CLASSES[playing_agent], observation)
                        raw_data[playing_agent][game_num][0].append(
                                observation['vectorized'])
                        raw_data[playing_agent][game_num][1].append(action_vec)

                        if observation['current_player'] == agent_id:
                            assert action is not None
                            current_player_action = action
                        else:
                            assert action is None

                        observations, _, game_done, _ = environment.step(
                                current_player_action)
                        if game_done:
                            break

        return raw_data

def main(args):
    creator = DataCreator()
    raw_data = creator.create_data()
    datapath = dir_utils.resolve_datapath(creator, args.datadir)
    pickle.dump(raw_data, open(datapath, "wb"))

if __name__ == '__main__':
    args = parse_args.parse()
    main(args)
