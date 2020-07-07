import gin
import os
import pickle
import random
import multiprocessing as mp
from datetime import datetime
import shutil

import subprocess
from utils import parse_args

PATH_GANABI = os.path.dirname(os.path.abspath(__file__))
PATH_HANABI_ENV = os.path.join(PATH_GANABI, "hanabi_env")
PATH_EXPERTS = os.path.join(PATH_GANABI, 'experts')

def multi_gen(create_fn, datapath, num_players, num_games_per_proc, num_proc):
    """ Invoke multiple generation functions.
    Arguments:
        - create_fn: func
            The `create_*_data` functions.
        - datapath: str
            Path to the directory including the file name to save the data
            WITHOUT the .pkl extension.
            Ex: 'output/rainbow_2_500000'
        - num_players: int
            Number of players in the game
        - num_games_per_proc: int
            Number of games for each process to geenrate.
        - num_proc: int
            Number of processes.
            Note: the total number of games that will be generated is
              (@num_games_per_proc * @num_games_per_proc)

    Output:
        A compressed file will be generated under @datapath. The compressed
          file contains the data in the following format:

        @datapath
            |
            |____ 0
            |     |_____ [agent_name]_[@num_players]_[@num_games_per_proc].pkl
            |
            |____ 1
            |     |_____ [agent_name]_[@num_players]_[@num_games_per_proc].pkl
            |
            |____ 2
            |     |_____ [agent_name]_[@num_players]_[@num_games_per_proc].pkl
           ...
            |__ [num_proc - 1]
                  |_____ [agent_name]_[@num_players]_[@num_games_per_proc].pkl
    """
    agent_name = create_fn.__name__.split('_')[1:-1]
    agent_name = '-'.join(agent_name)


    ts = hex(int((datetime.now()).timestamp()))[4:] # timestamp
    PATH_TMP = '.{}_TMP_'.format(agent_name) + ts
    os.mkdir(PATH_TMP)
    pool = []
    # Generate the data in parallel
    for i in range(num_proc):
        subdir = os.path.join(PATH_TMP, str(i))
        os.mkdir(subdir)
        path = '{}_{}_{}.pkl'.format(
            agent_name, num_players, num_games_per_proc)
        path = os.path.join(subdir, path)
        p = mp.Process(
            target=create_fn, args=(path, num_players, num_games_per_proc))
        p.start()
        pool.append(p)

    for p in pool:
        p.join()

    for root, dirs, files in os.walk(PATH_TMP):
        for file in files:
            tokens = file.split('.')
            if tokens[-1] == 'csv':
                os.remove(os.path.join(root, file))

    cmd = 'tar -czvf {}.tar.gz '.format(datapath)
    cmd += '--transform s/{}/{}/ '.format(PATH_TMP, datapath.split('/')[-1])
    cmd += '{}/*'.format(PATH_TMP)
    process = subprocess.Popen(cmd, shell=True)
    process.wait()

    shutil.rmtree(PATH_TMP)

# Sanity check
def data_test(rDir, fn=None, N=20, G=10, T=2, n_total_games=500000,
                 n_games_per_proc=25000):
    """
    Sanity check for the generated datasets.
    Arguments
        - rDir: str
            Name prefix of the root directory of the data to be checked.
            E.g.: 'quux_cheatbot'
        - fn: str, default None
            Name prefix of the individual pickle file.
            @rDir is used if fn = None.
        - N: int, default 10
            The first N subdirectories in the extracted directory to check for.
        - G: int, default 2
            The first G games to check for.
        - T: int, default 4
            The first T turns to check for.
            Note: some agents have lower number of turns, so a high value for T
              results in an error in that case.
        - n_total_games: int, default 500000
            Total number of games generated.
        - n_games_per_proc: int, default 25000
            Number of games geenrated per process.
    Outputs:
        - Observations will be printed out for eyeballing. Look for repetitions
            for observations as repetitions likely indicate an error.
            Repetitions are also automatically checked by the script, and if no
            repetition occurs, `[]` will be printed in the end.
    """
    if fn == None:
        fn = rDir

    pkls = []
    obs = set()
    repeats = []
    # load
    for i in range(N):
        f = open('outdir/{}_data_2_{}/{}/{}_2_{}.pkl'.format(
            rDir, n_total_games, i, fn, n_games_per_proc), 'rb')
        pkl = pickle.load(f)
        pkls.append(pkl)
        print(len(pkl), end=' ', flush=True)
    print()
    # print turns
    for g in range(G):
        print('Number of turns')
        for pkl in pkls:
            print(len(pkl[g][0]))
        for t in range(T):
            print('Turn %d observations:' % t)
            for pkl in pkls:
                ob = pkl[g][0][t]
                if ob in obs:
                    print('#' * 20, 'REPETITION', '#' * 20)
                    repeats.append((t, ob))
                obs.add(ob)
                print(ob)
    print(repeats)

def create_rainbow_data(datapath, num_players, num_games):
    '''Call the script responsible for creating gameplay data using the
       rainbow agent.

    Inputs:
        datapath (str): the path/to/the/data/file being created, including the
                        filename, with .pkl extension
        num_players (int): the number of players in the game
        num_games (int): the number of games to play and save the data for
    '''
    # NOTE: since there are several rainbow agents right now, there's a
    # hardcoded way of specifying which one to use; in the future there will
    # be only one rainbow agent
    default_rainbow_agent_name = 'rainbow1'
    rainbowdir = './experts/rainbow_models'
    process = subprocess.Popen(["/data1/shared/venvg2/bin/python",
                                "experts/create_rainbow_data.py",
                                "--datapath", datapath,
                                "--num_players", str(num_players),
                                "--num_games", str(num_games),
                                "--agent_name", default_rainbow_agent_name,
                                "--rainbowdir", rainbowdir])
    process.communicate()  # solves issue where Popen hangs

# Walton Agent Caller
def create_walton_data(datapath, num_players, num_games, agent_name,
                       seed=None):
    if seed == None:
        seed = random.randint(0, 2**31-1)
    print('Seed Used:', seed)

    args = ["python", "experts/create_walton_data.py",
            "--datapath", datapath,
            "--num_players", str(num_players),
            "--num_games", str(num_games),
            "--seed", str(seed),
            "--agent_name", agent_name]

    process = subprocess.Popen(args)
    process.communicate()  # solves issue where Popen hangs

# Walton Agent: IGGI Agent
def create_iggi_data(datapath, num_players, num_games):
    default_walton_agent_name = 'iggi'
    create_walton_data(datapath, num_players, num_games,
                       default_walton_agent_name)

# Walton Agent: Outer Agent
def create_outer_data(datapath, num_players, num_games):
    default_walton_agent_name = 'outer'
    create_walton_data(datapath, num_players, num_games,
                       default_walton_agent_name)

# Walton Agent: Legal Random Agent
def create_legal_random_data(datapath, num_players, num_games):
    default_walton_agent_name = 'legal_random'
    create_walton_data(datapath, num_players, num_games,
                       default_walton_agent_name)

# Walton Agent: Van den Bergh Rule Agent
def create_van_den_bergh_data(datapath, num_players, num_games):
    default_walton_agent_name = 'vdb-paper'
    create_walton_data(datapath, num_players, num_games,
                       default_walton_agent_name)

# Walton Agent: Flawed Agent
def create_flawed_data(datapath, num_players, num_games):
    default_walton_agent_name = 'flawed'
    create_walton_data(datapath, num_players, num_games,
                       default_walton_agent_name)

# Walton Agent: Piers Agent
def create_piers_data(datapath, num_players, num_games):
    default_walton_agent_name = 'piers'
    create_walton_data(datapath, num_players, num_games,
                       default_walton_agent_name)

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


def create_WTFWT_data(datapath, num_players, num_games, workers=0):
    args = ["python3", PATH_EXPERTS + "/create_WTFWT_data.py", "-q",
            "--n", str(num_games),
            "--p", str(num_players),
            "--P", datapath,
            "--m", str(workers)]
    print(args)
    process = subprocess.Popen(args)
    process.wait()


def create_fireflower_data(datapath, num_players, num_games):
    args = ["python", "experts/create_fireflower_data.py",
                      "--datapath", datapath,
                      "--num_players", str(num_players),
                      "--num_games", str(num_games)]
    process = subprocess.Popen(args)
    process.communicate()


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
    "wtftf": create_WTFWT_data
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
        self.raw_data = {}
        self.test_data = {}  # data from agents totally unseen to model

    def train_val_test_split(self, raw_data):  # previously named "read"
        '''Split up raw_data into train, validation, and test, and save in self.

        Inputs:
            raw_data: a dictionary mapping agent names to a list of their games

        This function randomly picks an agent to be the test agent, and sets it aside
        in self.test_data; this data is not to be trained or validated on. It splits the
        data of the remaining agents up so that 90% of the games of each agent are saved
        in self.train_data, and the rest in self.validation_data.
        '''
        # TODO: What kind of data should test_data have?
        # test_agent = random.choice(list(raw_data.keys()))

        for agent in raw_data:
            # if agent == test_agent:
            #     continue
            split_idx = int(0.9 * len(raw_data[agent]))
            self.train_data[agent] = raw_data[agent][:split_idx]
            self.validation_data[agent] = raw_data[agent][split_idx:]
            self.raw_data[agent] = raw_data[agent]
         #self.test_data[test_agent] = raw_data[test_agent]


def create_load_data(args):
    loader = DataLoader()  # gin configured
    raw_data = {}

    print(args)
    # composing a dictionary mapping agent names to a list of their games
    for agent_name in args.agents_to_use:
        agent_data_filename = agent_name + "_" + \
            str(loader.num_players) + "_" + str(loader.num_games) + ".pkl"

        datapath = os.path.join(args.data_dir, agent_data_filename)

        if agent_data_filename not in os.listdir(args.data_dir):
            CREATE_DATA_FOR[agent_name](
                datapath, loader.num_players, loader.num_games)

        agent_data = pickle.load(open(datapath, "rb"), encoding='latin1')

        # placing agent_data into a dictionary
        raw_data[agent_name] = agent_data

    loader.train_val_test_split(raw_data)
    return loader


if __name__ == "__main__":
    args = parse_args.parse()
    args = parse_args.resolve_configpath(args)
    # args = parse_args.resolve_agents_to_use(args)
    create_load_data(args)
