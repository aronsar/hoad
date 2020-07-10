# this script creates data using python 2 and rainbow agents

# here we add the repo's root directory to the path variable; everything
# is imported relative to that to avoid problems
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.realpath(__file__))))

from utils import parse_args
from utils import binary_list_to_int as b2int
from collections import defaultdict
from original_agents.rainbow import rainbow_agent_wrapper as rainbow
import pickle
from hanabi_env import rl_env
import gin
import tensorflow as tf # version 1.x
import importlib
import argparse


def one_hot_vectorized_action(agent, num_moves, obs):
    '''
    Inputs:
        agent: agent object we imported and initialized with agent_config
        num_moves: length of the action vector
        obs: observation object (has lots of good info, run print(obs.keys()) to see)

    Returns:
        one_hot_action_vector: one hot action vector
        action: action in the form recognizable by the Hanabi environment
                (idk something like {'discard': 5})
    '''
    action = agent.act(obs)
    one_hot_action_vector = [0]*num_moves
    action_idx = obs['legal_moves_as_int'][obs['legal_moves'].index(action)]
    one_hot_action_vector[action_idx] = 1

    return one_hot_action_vector, action

class DataCreator(object):
    def __init__(self, args):
        self.num_players = args.num_players
        self.num_games = args.num_games
        self.environment = rl_env.make('Hanabi-Full', num_players=self.num_players)
        self.agent_config = {
                'players': self.num_players,
                'num_moves': self.environment.num_moves(),
                'observation_size': self.environment.vectorized_observation_shape()[0]}
        self.agent_object = rainbow.Agent(self.agent_config)


    def create_data(self):
        '''Create and return a list of games. Each game has the following structure:
            [ [[obs_0], [obs_1], ..., [obs_n]], [[act_0], [act_1], ..., [act_n]] ]
        where each obs_i and act_i are the observation and resultant action that
        an agent took at game step i. Each game round consists of num_players game
        steps. A game can have a variable amount of rounds--you can lose early.
        '''
        raw_data = []

        for game_num in range(self.num_games):
            raw_data.append([[],[]])
            observations = self.environment.reset()
            game_done = False

            while not game_done:
                for agent_id in range(self.num_players):
                    observation = observations['player_observations'][agent_id]
                    one_hot_action_vector, action = one_hot_vectorized_action(
                            self.agent_object,
                            self.environment.num_moves(),
                            observation)
                    # raw_data[game_num][0].append(
                    #         observation['vectorized'])
                    raw_data[game_num][0].append(
                            b2int.convert(observation['vectorized']))
                    raw_data[game_num][1].append(one_hot_action_vector)

                    if observation['current_player'] == agent_id:
                        assert action is not None
                        current_player_action = action
                    else:
                        assert action is None

                    observations, _, game_done, _ = self.environment.step(
                            current_player_action)
                    if game_done:
                        break

        return raw_data


def parse():
  parser = argparse.ArgumentParser()
  parser.add_argument('--agent_name', '--a', type=str, default='rainbow')
  parser.add_argument('--num_games', '--n', type=int, default=10, help='Number of games to produce')
  parser.add_argument('--num_players', '--p', type=int, default=2, help='Number of players.')
  parser.add_argument('--savedir', '--s', type=str, default='.')

  args = parser.parse_args()
  return args


def main(args):
    data_creator = DataCreator(args)
    rainbow_data = data_creator.create_data()
    savepath = os.path.join(args.savedir, "rainbow_" + str(args.num_players) + "_" + str(args.num_games) + ".pkl")
    pickle.dump(rainbow_data, open(savepath, "wb"))

if __name__ == '__main__':
    args = parse()
    main(args)
