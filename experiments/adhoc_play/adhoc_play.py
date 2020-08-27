import sys
from os.path import dirname as opd
from os.path import realpath as opr
sys.path.insert(0, opd(opd(opd(opr(__file__))))) # place /path/to/hoad/ in sys.path

import os
import numpy as np
import argparse
from utils import binary_list_to_int as b2int
from experiments.adhoc_play import imitator_wrapper
from experiments.adhoc_play import maml_agent_wrapper
from hanabi_env import rl_env

parser = argparse.ArgumentParser()
parser.add_argument(
    '--num_games', '--n', type=int, default=10,
    help='The number of games to run per agent-agent combo.')
parser.add_argument(
    '--imitators_path', '--i', type=str, default='./imitator_models',
    help='')
parser.add_argument(
    '--maml_agents_path', '--m', type=str, default='./maml_models',
    help='')

args = parser.parse_args()


env_config = {'colors': 5,
          'ranks': 5,
          'players': 2,
          'hand_size': 5,
          'max_information_tokens': 8,
          'max_life_tokens': 3,
          'seed': 1,
          'observation_type': 1,  # FIXME: NEEDS CONFIRMATION
          'random_start_player': False}
env = rl_env.HanabiEnv(env_config)
NUM_ADHOC_TESTS = 3
GAMES_PER_TEST = 3


def one_hot_vectorized_action(agent, num_moves, obs):
    action, action_idx = agent.act(obs,num_moves)
    one_hot_action_vector = [0]*num_moves
    one_hot_action_vector[action_idx] = 1

    return one_hot_action_vector, action

def play_games(agent0, agent1, num_games):
    scores = []
    game_data = []
    agents = (agent0, agent1)
    for game_num in range(num_games):
        game_data.append([[],[]])
        observations = env.reset()
        game_done = False

        while not game_done:
            for agent_id in range(env_config['players']):
                observation = observations['player_observations'][agent_id]
                one_hot_action_vector, action = one_hot_vectorized_action(
                        agents[agent_id],
                        env.num_moves(),
                        observation)

                game_data[game_num][0].append(observation['vectorized'])
                game_data[game_num][1].append(one_hot_action_vector)
                
                if observation['current_player'] == agent_id:
                    assert action is not None
                    current_player_action = action
                else:
                    assert action is None

                observations, _, game_done, _ = env.step(
                        current_player_action)
                if game_done:
                    scores.append(env.state.score())
                    break
    return scores, game_data

def adhoc_play(imitator_agents, maml_agents, names):
    scores_dict = {}
    avg_scores = {}
    std_scores = {}
    names = [n for n in names if n in imitator_agents and n in maml_agents]
    for name in names:
        print("test agent: " + str(name))
        imitator_agent = imitator_agents[name]
        maml_agent = maml_agents[name]
        scores_list = []
        for test in range(NUM_ADHOC_TESTS):
            print("num: " + str(test))
            _, imitator_games = play_games(imitator_agent, imitator_agent, num_games=10)
            maml_agent.task_update(imitator_games)
            scores, _ = play_games(imitator_agent, maml_agent, num_games=GAMES_PER_TEST)
            scores_list.append(scores)
        scores_dict[name] = np.array(scores_list)
        avg_scores[name] = np.average(scores_dict[name]) 
        std_scores[name] = np.std(scores_dict[name])
    return scores_dict, avg_scores, std_scores

def create_imitator_agents(imitators_path):
    imitator_agents = {}
    for agent in os.listdir(imitators_path):
        path = os.path.join(imitators_path, agent, agent + '.h5')
        agent_obj = imitator_wrapper.Agent(path)
        imitator_agents[agent] = agent_obj
    return imitator_agents

def create_maml_agents(maml_agents_path):
    maml_agents = {}
    for agent in os.listdir(maml_agents_path):
        path = os.path.join(maml_agents_path, agent, 'weights', '30000-weights.h5')
        agent_obj = maml_agent_wrapper.MamlAgent(path)
        maml_agents[agent] = agent_obj
    return maml_agents


if __name__ == "__main__":
    imitator_agents = create_imitator_agents(args.imitators_path)
    maml_agents = create_maml_agents(args.maml_agents_path)
    names = os.listdir(args.maml_agents_path)
    score_dict, avg_scores, std_scores = adhoc_play(imitator_agents, maml_agents, names)
    import pdb; pdb.set_trace()
    pass

