import gin
import os
import pickle
import random
import subprocess
from utils import parse_args

PATH_GANABI = os.path.dirname(os.path.abspath(__file__))
PATH_HANABI_ENV = os.path.join(PATH_GANABI, "hanabi_env")
PATH_EXPERTS = os.path.join(PATH_GANABI, 'experts')


def create_rainbow_data(datapath, num_players, num_games):
    '''Call the script responsible for creating gameplay data using the rainbow agent.
    
    Inputs:
        datapath (str): the path/to/the/data/file being created, including the filename, with .pkl extension
        num_players (int): the number of players in the game
        num_games (int): the number of games to play and save the data for
    '''
    # NOTE: since there are several rainbow agents right now, there's a hardcoded
    # way of specifying which one to use; in the future there will be only one rainbow agent
    default_rainbow_agent_name = 'rainbow1'
    rainbowdir = './experts/rainbow_models'
    subprocess.Popen(["/data1/shared/venvg2/bin/python", "experts/create_rainbow_data.py",
                      "--datapath", datapath,
                      "--num_players", str(num_players),
                      "--num_games", str(num_games),
                      "--agent_name", default_rainbow_agent_name,
                      "--rainbowdir", rainbowdir])
    subprocess.Popen.communicate()  # solves issue where Popen hangs


# Walton Agent Caller
def create_walton_data(datapath, num_players, num_games, agent_name):
    args = ["python", "experts/create_walton_data.py",
            "--datapath", datapath,
            "--num_players", str(num_players),
            "--num_games", str(num_games),
            "--agent_name", agent_name]

    process = subprocess.Popen(args)
    process.communicate()  # solves issue where Popen hangs


# Walton Agent: IGGI Agent
def create_iggi_data(datapath, num_players, num_games):
    default_walton_agent_name = 'iggi'
    create_walton_data(datapath, num_players, num_games, default_walton_agent_name)


# Walton Agent: Outer Agent
def create_outer_data(datapath, num_players, num_games):
    default_walton_agent_name = 'outer'
    create_walton_data(datapath, num_players, num_games, default_walton_agent_name)


# Walton Agent: Legal Random Agent
def create_legal_random_data(datapath, num_players, num_games):
    default_walton_agent_name = 'legal_random'
    create_walton_data(datapath, num_players, num_games, default_walton_agent_name)


# Walton Agent: Van den Bergh Rule Agent
def create_van_den_bergh_data(datapath, num_players, num_games):
    default_walton_agent_name = 'vdb-paper'
    create_walton_data(datapath, num_players, num_games, default_walton_agent_name)


# Walton Agent: Flawed Agent
def create_flawed_data(datapath, num_players, num_games):
    default_walton_agent_name = 'flawed'
    create_walton_data(datapath, num_players, num_games, default_walton_agent_name)


# Walton Agent: Piers Agent
def create_piers_data(datapath, num_players, num_games):
    default_walton_agent_name = 'piers'
    create_walton_data(datapath, num_players, num_games, default_walton_agent_name)


# Quuxplusone Agent Caller
def create_quux_data(datapath, num_players, num_games, agent_name):
    args = ["python", "experts/create_quux_data.py",
            "--datapath", datapath,
            "--num_players", str(num_players),
            "--num_games", str(num_games),
            "--agent_name", agent_name]

    process = subprocess.Popen(args)
    process.communicate()  # solves issue where Popen hangs


# Quuxplusone Agent: Smart Agent
def create_blindbot_data(datapath, num_players, num_games):
    create_quux_data(datapath, num_players, num_games, "quux_blindbot")


# Quuxplusone Agent: Smart Agent
def create_simplebot_data(datapath, num_players, num_games):
    create_quux_data(datapath, num_players, num_games, "quux_simplebot")


# Quuxplusone Agent: Smart Agent
def create_valuebot_data(datapath, num_players, num_games):
    create_quux_data(datapath, num_players, num_games, "quux_valuebot")


# Quuxplusone Agent: Smart Agent
def create_holmesbot_data(datapath, num_players, num_games):
    create_quux_data(datapath, num_players, num_games, "quux_holmesbot")


# Quuxplusone Agent: Smart Agent
def create_smartbot_data(datapath, num_players, num_games):
    create_quux_data(datapath, num_players, num_games, "quux_smartbot")


# Quuxplusone Agent: Smart Agent
def create_infobot_data(datapath, num_players, num_games):
    create_quux_data(datapath, num_players, num_games, "quux_infobot")


# Quuxplusone Agent: Smart Agent
def create_cheatbot_data(datapath, num_players, num_games):
    create_quux_data(datapath, num_players, num_games, "quux_cheatbot")


# Quuxplusone Agent: Smart Agent
def create_newcheatbot_data(datapath, num_players, num_games):
    create_quux_data(datapath, num_players, num_games, "quux_newcheatbot")


def create_WTFWT_data(datapath, num_players, num_games):
    args = ['python3', PATH_EXPERTS + '/create_WTFWT_data.py', '-q',
            '--n', num_games,
            '--p', num_players,
            '--P', datapath]
    process = subprocess.Popen(args)
    process.wait()

def create_fireflower_data(datapath, num_players, num_games):
    args = ["python", "experts/create_fireflower_data.py",
                      "--datapath", datapath,
                      "--num_players", str(num_players),
                      "--num_games", str(num_games)]
    subprocess.Popen(args)
    subprocess.Popen.communicate()

def create_example_data():
    # TODO: insert your Popen for your script here
    # do any necessary stuff
    # call the creation script for this agent
    pass


# dictionary for mapping agent names to wrapper function names
# TODO: add your string name to function name mapping here
CREATE_DATA_FOR = {
    'rainbow': create_rainbow_data,
    'iggi': create_iggi_data,
    'outer': create_outer_data,
    'legal_random': create_legal_random_data,
    'vdb-paper': create_van_den_bergh_data,
    'flawed': create_flawed_data,
    'piers': create_piers_data,
    'example': create_example_data,
    'quux_blindbot': create_blindbot_data,
    'quux_simplebot': create_simplebot_data,
    'quux_valuebot': create_valuebot_data,
    'quux_holmesbot': create_holmesbot_data,
    'quux_smartbot': create_smartbot_data,
    'quux_infobot': create_infobot_data,
    'quux_cheatbot': create_cheatbot_data,
    'quux_newcheatbot': create_newcheatbot_data,
    'fireflower': create_fireflower_data,
}


@gin.configurable
class DataLoader(object):
    @gin.configurable
    def __init__(self,
                 num_players=2,
                 num_games=10):

        self.num_players = num_players
        self.num_games = num_games

        self.train_data = {}  # gameplay data given to model
        self.validation_data = {}  # data not given to model, from same agents as train
        self.test_data = {}  # data from agents totally unseen to model

    def resolve_datapath(self, datadir, agent_name):
        '''Compose and return the full relative path of where the agent's data will be saved.
        
        Inputs:
            datadir (str): base directory in which to put the data files (default ./data)
            agent_name (str): name of the agent which is passed in on the command line; mapped to wrapper function name
                    
        Outputs:
            (str): the full relative path of where the agent's data will be saved
        '''
        agent_data_filename = agent_name + "_" + str(self.num_players) + "_" \
                              + str(self.num_games) + ".pkl"
        return os.path.join(datadir, agent_data_filename)

    def train_val_test_split(self, raw_data):  # previously named "read"
        '''Split up raw_data into train, validation, and test, and save in self.
        
        Inputs:
            raw_data: a dictionary mapping agent names to a list of their games
            
        This function randomly picks an agent to be the test agent, and sets it aside
        in self.test_data; this data is not to be trained or validated on. It splits the 
        data of the remaining agents up so that 90% of the games of each agent are saved
        in self.train_data, and the rest in self.validation_data.
        '''
        test_agent = random.choice(list(raw_data.keys()))

        for agent in raw_data:
            if agent == test_agent:
                continue
            split_idx = int(0.9 * len(raw_data[agent]))
            self.train_data[agent] = raw_data[agent][:split_idx]
            self.validation_data[agent] = raw_data[agent][split_idx:]

        self.test_data[test_agent] = raw_data[test_agent]


def main(args):
    loader = DataLoader()  # gin configured
    raw_data = {}

    print(args)
    # composing a dictionary mapping agent names to a list of their games
    for agent_name in args.agents_to_use:
        datapath = loader.resolve_datapath(args.datadir, agent_name)
        try:
            agent_data = pickle.load(open(datapath, "rb"), encoding='latin1')
        except IOError:
            CREATE_DATA_FOR[agent_name](datapath, loader.num_players, loader.num_games)
            agent_data = pickle.load(open(datapath, "rb"), encoding='latin1')

        raw_data[agent_name] = agent_data  # placing agent_data into a dictionary

    loader.train_val_test_split(raw_data)
    return loader


if __name__ == "__main__":
    args = parse_args.parse()
    args = parse_args.resolve_configpath(args)
    # args = parse_args.resolve_agents_to_use(args)
    main(args)
