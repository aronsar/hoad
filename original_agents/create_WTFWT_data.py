import sys
import csv
import argparse
import subprocess
import random
import pickle
import os
import shutil
from datetime import datetime

PATH_GANABI = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PATH_HANABI_ENV = os.path.join(PATH_GANABI, "hanabi_env")
PATH_EXPERTS = os.path.join(PATH_GANABI, 'original_agents')
PATH_UTILS = os.path.join(PATH_GANABI, 'utils')

sys.path.insert(0, PATH_GANABI)
sys.path.insert(0, PATH_HANABI_ENV)
sys.path.insert(0, PATH_UTILS)

import binary_list_to_int as b2int

def run(cmd, suppress=True):
    """Print and run a command in shell. Exit only upon finishing.
    Args:
        - cmd: str
            Shell command to be executed.
        - suppress: bool, default True
            If True, stdout from executing the command will be suppressed.
    """
    stdout = [None, subprocess.DEVNULL][suppress]
    print(cmd)
    process = subprocess.Popen(cmd, shell=True, stdout=stdout)
    process.wait()

def one_hot_vectorized_action(action, num_moves, cur_obs):
    """ Converts a HanabiEnv action dictionary into a one-hot encoded vector.
        Originally written by Aron Sarmasi.
    Args:
        - action: dict
            Action of the player in the format specified by HanabiEnv.
        num_moves: int
            Length of the action vector.
        cur_obs: dict
            Observations of the current player. See returned value @observation
            in rl_env.py:step() for details.

    Returns:
        one_hot_action_vector: one hot action vector
    """
    one_hot_action_vector = [0]*num_moves
    cur_act_idx = cur_obs['legal_moves'].index(action)
    action_idx = cur_obs['legal_moves_as_int'][cur_act_idx]
    one_hot_action_vector[action_idx] = 1

    return one_hot_action_vector

def parse_action(row, p):
    """ Parse a dict-zipped row in the .csv log file and return a corresponding
        HanabiEnv action.

    Examples of the formats in WTFWT .csv game log files:
        - "play-0": play card at index 0.
        - "discard-0": discard card at index 0.
        - "hint-0-5": give a value hint of 5 to player whose ID is 0.
        - "hint-0-r": give a color hint of red to player whose ID is 0.

    Arguments:
        - row: dict
            A dictionary that has header as its keys and row elements as its
            values.
        - p: int
            Number of players.
    Returns:
        - HanabiEnv action translated from the action in the current row.
    Raises:
        - Value Errors for parsing unknown formats.
    """
    # Actions in forms of
    tks = row['action'].split('-')
    if tks[0] in ['play', 'discard']:
        action = {
            'action_type': tks[0].upper(),
            'card_index': int(tks[1])}
    elif tks[0] == 'hint':
        # offset = (target_id - cur_id) % num_players
        offset = (int(tks[1]) - int(row['pid'])) % p
        if tks[2].isdigit():
            action = {
                'action_type': 'REVEAL_RANK',
                'target_offset': offset,
                'rank': int(tks[2])-1} # HanabiEnv adds 1
        elif tks[2].isalpha():
            action = {
                'action_type': 'REVEAL_COLOR',
                'target_offset': offset,
                'color': tks[2].upper()}
        else:
            msg = 'Unknown hint format found in rust_agent.csv.'
            raise ValueError(msg)
    else:
        raise ValueError('Unknown action found in rust_agent.csv.')
    return action

def comp_test(env, row, obs, args):
    """ Test by comparing HanabiEnv attributes with game logs from WTFWT.
    Args:
        - env: HanabiEnv
            Current HanabiEnv instance of the game.
        - row: dict
            Dict-zipped row elements with header being the keys.
        - obs: list
            List of the observations returned by HanabiEnv.step()[0]
        - args:
            Command line arguments passed to the script.
    Raises:
        - AssertionError when a mismatch occurs.
    """
    assert(env.state.cur_player() == int(row['pid'])
                                  == obs['current_player'] )
    assert(env.state.information_tokens() == int(row['rem_info']))
    assert(env.state.life_tokens() == int(row['rem_life']))
    assert(env.state.deck_size() == int(row['rem_deck']))

    # Compare fireworks. HanabiEnv: r-y-g-w-b; WTFWT: b-g-w-y-r
    csv_fw = [int(x[1]) for x in row['firework'].split('-')]
    csv_fw_keys = [x[0] for x in row['firework'].split('-')]
    csv_fw = dict(zip(csv_fw_keys, csv_fw))
    env_fw = dict(zip(['r', 'y', 'g', 'w', 'b'],
                      env.state.fireworks()))
    assert(csv_fw == env_fw)
    # b2-y1-b2-w5,g2-b4-r4-w2,g1-r3-b4-g3,y1-w3-g1-r4,w3-b3-y5-w2
    env_hands = env.state.player_hands()
    for i in range(args.num_players):
        # [B2, Y1, B2, W5] as in HanabiCard class obj
        cur_env_hands = env_hands[i]
        # ['b2', 'y1', 'b2', 'w5']
        cur_csv_hands = row['p%d_cards' % i].split('-')
        assert(len(cur_env_hands) == len(cur_csv_hands))
        for card_idx in range(len(cur_env_hands)):
            # {'color': 'B', 'rank': 1} b/c internally values are
            #   indexed from 0 in DM HanabiEnv
            env_card = cur_env_hands[card_idx].to_dict()
            csv_card = cur_csv_hands[card_idx] # 'b2'
            # -1 to match values being indexed from 0 in DM Env
            csv_card = [csv_card[0].upper(), int(csv_card[1])-1]
            csv_card = dict(zip(['color', 'rank'], csv_card))
            assert(csv_card == env_card)

def parallel_generation(args):
    """ Generate the data in parallel.

    Arguments:
        - args: Namespace
            Arguments taken from command line. To see details, run
            python3 create_WTFWT_data.py --help
    """
    ts = hex(int((datetime.now()).timestamp()))[4:] # timestamp
    PATH_TMP = '.WTFWT_TMP_' + ts
    os.mkdir(PATH_TMP)
    pool = []
    # Generate the data in parallel
    for i in range(args.m):
        os.mkdir(os.path.join(PATH_TMP, str(i)))
        cmd = '(cd {}/{} && '.format(PATH_TMP, i)
        cmd += 'python3 {}/create_WTFWT_data.py '.format(PATH_EXPERTS)
        cmd += '--n {} --p {} -q --m 1)'.format(args.num_games, args.num_players)
        pool.append(subprocess.Popen(cmd, shell=True))

    code = [p.wait() for p in pool]
    print('Exit codes:', code)
    # Combine the generated data
    combined = []
    for i in range(args.m):
        filename = 'WTFWT_{}_{}.pkl'.format(args.num_players, args.num_games)
        with open(os.path.join(PATH_TMP, str(i), filename), 'rb') as f:
            combined += pickle.load(f)

    name_tar = 'WTFWT_data_{}_{}'.format(args.num_players, args.m * args.num_games)
    cmd = 'tar -czvf {}/{}.tar.gz '.format(args.savedir, name_tar)
    cmd += '--transform s/.WTFWT_TMP_{}/{}/ '.format(ts, name_tar)
    cmd += '.WTFWT_TMP_{}/*'.format(ts)
    run(cmd)

    shutil.rmtree(PATH_TMP)

def main(args):
    """ Observations & actions generation.

    Generate binary observations & one-hot encoded action vectors based on game
    logs from running WTFWT agent.

    Observations are saved in the following format:

               turn 0   ...  turn n        turn 0   ...  turn n
    Game 0   [[[obs_0], ..., [obs_n]],    [[act_0], ..., [act_n]],
    Game 1    [[obs_0], ..., [obs_n]],    [[act_0], ..., [act_n]],
      ...
    Game m    [[obs_0], ..., [obs_n]],    [[act_0], ..., [act_n]]]

    Arguments:
        - args: Namespace
            Arguments taken from command line. To see details, run
            python3 create_WTFWT_data.py --help
    Raises:
        - Assertion errors for mismatches in WTFWT and DM HanabiEnv
        - Value Errors for parsing unknown formats.
    """
    print('Seed used: %d' % args.seed)
    # Handle by build_env.sh
    # Make hanabi_env & import it
    # run('(cd {}/ && cmake -Wno-dev . && make)'.format(PATH_HANABI_ENV), args.q)
    import rl_env

    random.seed(args.seed)
    combined_data = []
    # For specified number of games
    for i in range(args.num_games):
        game_data = [[], []]
        # Generate the game logs and decks
        s = random.randint(0, 2**31-1) # seed for WTFWT
        cmd = ('cargo run -q --manifest-path {}/WTFWT/Cargo.toml -- -n 1 -o 1 '
               '-s {} -p {} -g info').format(PATH_EXPERTS, s, args.num_players)
        debug = ['', ' -l debug'][args.debug]
        run(cmd + debug, args.q)

        with open('dk_cards.csv') as f_dk, open('rust_agent.csv') as f_log:
            reader = csv.reader(f_dk)
            dk = next(reader)[0].upper()
            # Deck in Rust Env starts from right and indexed from 1
            dk = [x[0] + str(int(x[1])-1) for x in dk.split('-')[::-1]]

            env = rl_env.make('Hanabi-Full', num_players=args.num_players)
            obs = env.reset(dk)

            header = (['pid', 'turn']
                   + ['p%d_cards' % i for i in range(args.num_players)]
                   + ['discards', 'action', 'firework',
                      'rem_life', 'rem_info', 'rem_deck'])

            reader = csv.reader(f_log)
            # For each turn in a game
            for row in reader:
                row = dict(zip(header, row))
                if args.debug:
                    comp_test(env, row, obs, args)
                action = parse_action(row, args.num_players)
                # Store the data
                cur_obs = obs['player_observations'][obs['current_player']]
                vec_act = one_hot_vectorized_action(
                    action, env.num_moves(), cur_obs)
                game_data[0].append(b2int.convert(cur_obs['vectorized']))
                game_data[1].append(vec_act)
                # Advance the state
                obs, reward, done, info = env.step(action)
        assert(done is True)
        combined_data.append(game_data)

    savepath = os.path.join(args.savedir, 'wtfwt_' + str(args.num_players) + '_' + str(args.num_games) + '.pkl')
    with open(savepath, 'wb') as f:
        pickle.dump(combined_data, f)
    os.remove('dk_cards.csv')
    os.remove('rust_agent.csv')

if __name__ == '__main__':
    s = random.randint(0, 2**31-1) # seed for random.seed()
    parser = argparse.ArgumentParser()
    parser.add_argument('--num_games', '--n', type=int, default=10, help='Number of games to produce.')
    parser.add_argument('--num_players', '--p', type=int, default=2, help='Num of Players')
    parser.add_argument('--seed', type=int, default=s, help='Seed for RNG')
    parser.add_argument('--savedir', '--s', type=str, default='.',
        help='Path to the directory where the .pkl data file is saved.')
    parser.add_argument('-q', action='store_true', help='Quiet mode')
    parser.add_argument('-debug', action='store_true',
        help='Run with debug mode for WTFWT and assertion tests.')
    parser.add_argument('--m', type=int, default=0,
        help='Number of processes to run in parallel. M >= 0.')
    args = parser.parse_args()

    
    if args.m == 0: # Childless parent: single process, so make once
        cmd = '(cd {}/ && cmake -Wno-dev . && make)'.format(PATH_HANABI_ENV)
        run(cmd, args.q)
        main(args)
    elif args.m == 1: # Child: no need to make
        main(args)
    elif args.m > 1: # Parent: make once, so children don't have to make
        cmd = '(cd {}/ && cmake -Wno-dev . && make)'.format(PATH_HANABI_ENV)
        run(cmd, args.q)
        parallel_generation(args)
    else:
        msg = 'M cannot be negative in --m. Use -h to see details.'
        raise ValueError(msg)
    
