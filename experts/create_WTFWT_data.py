from os.path import dirname, abspath, join
ganabi_path = dirname(dirname(abspath(__file__)))
hanabi_env_path = join(ganabi_path, "hanabi_env")
import sys
sys.path.insert(0, ganabi_path)
sys.path.insert(0, hanabi_env_path)

import rl_env

config={'colors': 5,
        'ranks': 5,
        'players': 5 ,
        'hand_size': 5,
        'max_information_tokens': 8,
        'max_life_tokens': 3,
        'observation_type': 1, # FIXME: NEEDS CONFIRMATION
        'seed': 1234,
        'random_start_player': False}

# FIXME: currently have to manually do `cmake . && make` for hanabi_env
env = rl_env.HanabiEnv(config)
