# this script creates data using python 2 and rainbow agents

from utils import dir_utils
from utils import parse_args
from collections import defaultdict
import pickle
import sys
sys.path.insert(0, './hanabi-env') #FIXME
import rl_env
import gin
import os
import tensorflow as tf # version 1.x
import importlib

def import_agents(expertdir, agent_config):
    '''Import every agent present in expertdir, and return a dictionary 
    mapping the name of the agent to the agent object. Takes a long time.'''
    # FIXME: only import num_unique_agent number of agents
    available_agents = {}
    sys.path.insert(0, expertdir)

    for agent_filename in os.listdir(expertdir):
        if 'agent' not in agent_filename:
            continue 
        agent_name = os.path.splitext(agent_filename)[0]
        agent_module = importlib.import_module(agent_name)
        available_agents[agent_name] = agent_module.Agent(agent_config)

    return available_agents

def one_hot_vectorized_action(agent, num_moves, obs):
    '''
    Inputs:
        agent: agent object we imported and initialized with agent_config
        num_moves: length of the action vector
        obs: observation object (has lots of good info, run print(obs.keys()) to see)

    Returns:
        one_hot_vector: one hot action vector
        action: action in the form recognizable by the Hanabi environment
                (idk something like {'discard': 5})
    '''
    action = agent.act(obs)
    one_hot_vector = [0]*num_moves
    action_idx = obs['legal_moves_as_int'][obs['legal_moves'].index(action)]
    one_hot_vector[action_idx] = 1

    return one_hot_vector, action

@gin.configurable
class Dataset(object):
    @gin.configurable
    def __init__(self, args,
            game_type='Hanabi-Full',
            num_players=2,
            num_unique_agents=6,
            num_games=None):
        
        self.game_type = game_type
        self.num_players = num_players
        self.num_unique_agents = num_unique_agents
        self.num_games = num_games
        self.environment = rl_env.make(game_type, num_players=self.num_players)
        self.agent_config = {
                'players': self.num_players,
                'num_moves': self.environment.num_moves(),
                'observation_size': self.environment.vectorized_observation_shape()[0]}
        self.available_agents = import_agents(args.expertdir, self.agent_config)


    def create_data(self):
        '''Iterate over all available agents, and have each play self.num_games.
        The games are self-play, so agent A plays A, B plays B, etc. Each game 
        has the following structure:
            [ [[obs_0], [obs_1], ..., [obs_n]], [[act_0], [act_1], ..., [act_n]] ]
        where each obs_i and act_i are the observation and resultant action that
        an agent took at game step i. Each game round consists of num_players game
        steps. A game can have a variable amount of rounds; you can lose early.
        
        The output, raw_data, is a dictionary with (key, value) pairs:
            key: agent name (string)
            value: list of games played by this agent, self.num_games long; each
                game has the format as shown above'''
        raw_data = defaultdict(list)
        
        for playing_agent in self.available_agents.keys():
            for game_num in range(self.num_games):
                raw_data[playing_agent].append([[],[]])
                observations = self.environment.reset()
                game_done = False

                while not game_done:
                    for agent_id in range(self.num_players):
                        observation = observations['player_observations'][agent_id]
                        action_vec, action = one_hot_vectorized_action(
                                self.available_agents[playing_agent],
                                self.environment.num_moves(),
                                observation)
                        raw_data[playing_agent][game_num][0].append(
                                observation['vectorized'])
                        raw_data[playing_agent][game_num][1].append(action_vec)

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

def main(args):
    data_creator = Dataset(args)
    # FIXME: all parse_args functions with "resolve" in the name should happen
    # in one function somewhere else
    args = parse_args.resolve_datapath(args,
            data_creator.game_type,
            data_creator.num_players,
            data_creator.num_unique_agents,
            data_creator.num_games)

    raw_data = data_creator.create_data()
    pickle.dump(raw_data, open(args.datapath, "wb"))

if __name__ == '__main__':
    args = parse_args.parse()
    args = parse_args.resolve_configpath(args)

    gin.parse_config_file(args.configpath)
    main(args)
