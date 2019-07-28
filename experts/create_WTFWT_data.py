from os.path import dirname, abspath, join
ganabi_path = dirname(dirname(abspath(__file__)))
hanabi_env_path = join(ganabi_path, "hanabi_env")
import sys
sys.path.insert(0, ganabi_path)
sys.path.insert(0, hanabi_env_path)

import subprocess
cmd =  '(cd {}/hanabi_env/ && cmake . && make)'.format(ganabi_path)
process = subprocess.Popen(cmd, shell=True)
process.wait()

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

# Predetermined deck of cards to be used for the game from top to bottom.
# Random cards will be dealt if @deck is None or @deck runs out before the game
#   ends. Note: Ranks are indexed from 0 for all vairables; however, ranks are
#   indexed from 1 when it is displayed by HanabiEnv functions. For instance,
#   ranks in @deck are all indexed from 0, but ranks are indexed from 1 when
#   you invoke `print(env.state)`.
deck = ['Y4', 'Y3', 'Y2', 'Y1', 'Y0',    'Y3', 'Y2', 'Y1', 'Y0', 'Y0',
        'W4', 'W3', 'W2', 'W1', 'W0',    'W3', 'W2', 'W1', 'W0', 'W0',
        'R4', 'R3', 'R2', 'R1', 'R0',    'R3', 'R2', 'R1', 'R0', 'R0',
        'G4', 'G3', 'G2', 'G1', 'G0',    'G3', 'G2', 'G1', 'G0', 'G0',
        'B4', 'B3', 'B2', 'B1', 'B0',    'B3', 'B2', 'B1', 'B0', 'B0']

# Create the Hanabi Environment with the defined configuration.
env = rl_env.HanabiEnv(config)

# Initialize the game with @deck. The arg is None by default.
obs = env.reset(deck)

# Now if you do `print(env.state)`, you will see that the cards are in the same
#    order as variable deck. Now current player's 0th card is Y5 (which is 'Y4'
#    in @deck above for reason mentioned previously). Let's try to play that
#    card by
action = { 'action_type': 'PLAY', 'card_index': 0 }
obs, reward, done, info = env.step(action)
# Now if you check env.state, you will see that Y5 is discarded because it's
#   invalid, and the player now got a new card from the deck, R4 which is 'R3'
#   as shown above in @deck. For the format of actions, see rl_env.py:246.

# Vectorized observations after each turn for each player can be retrieved by
player_id = 0
vectorized_obs = obs['player_observations'][player_id]['vectorized']

# Let's end the game by performing 2 more invalid actions
obs, reward, done, info = env.step(action)
obs, reward, done, info = env.step(action)

# Once the game is finished, @done will become True. Check rl_env.py:262 for
#   details.
assert(done is True)

# Everything is the same as the original Deepmind Hanabi Environment except
#   that now it has the option for userdefined deck. This means that the
#   example below can still be used:
# github.com/deepmind/hanabi-learning-environment/blob/master/rl_env_example.py
