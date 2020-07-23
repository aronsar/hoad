First we give instructions on how to install the necessary packages and languages to run each of the original agents in this repository (ie. agents taken from other authors), as well as the command to generate new replay data. TODO: Then we present a short exposition on the play-style of each agent.
TODO: add table of contents
TODO: make the fireflower agent work

## Setup Instructions
Before setting up any of the agents below, please be sure to follow the setup directions in `hoad/README.md`. The instructions below are written to be followed in order from Rainbow to Quux.
### Rainbow
```
# ROOT=/path/to/hoad
cd $ROOT/original_agents
virtualenv venv2 -p python2 --no-site-packages
source venv2/bin/activate
pip install gin-config tensorflow-gpu==1.15.0 numpy cffi absl-py
```

To train the rainbow agent from scratch takes about a day on a K80. Now, I know it's strange to run the training script from the root directory, but Python 2's import scheme is very obtuse, and this worked for me. Please feel free to file a pull request if you can do better.
```
cd $ROOT 
RAINBOW=$ROOT/original_agents/rainbow
python2 -um $RAINBOW/train --base_dir=$RAINBOW/tmp/ --gin_files=$RAINBOW/configs/hanabi_rainbow.gin
```
Once the above script has ran for about 2000 iterations, it should be close to convergence, although marginal performance could be gained by training for a few more days.
```
cd original_agent/rainbow
mkdir pretrained_model
cd pretrained_model
cp ../tmp/checkpoints/*2000* . # to copy over the checkpoint files corresponding to the 2000th iteration
cp ../tmp/checkpoints/checkpoint .
cd ../.. # back to hoad/original_agents

python create_rainbow_data.py --num_games 10 --savedir ../replay_data/ # for one batch
bash batched_create_data.sh rainbow rainbow # for many batches using correct file tree structure
```
### WTFWThat
```
cd $ROOT/original_agents
deactivate # make sure you're not still in venv2
virtualenv venv3 -p python3
source venv3/bin/activate
pip install cffi
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
source $HOME/.cargo/env

python create_WTFWT_data.py --num_games 10 --savedir ../replay_data/ # for one batch
bash batched_create_data.sh WTFWT WTFWT # for many batches using correct file tree structure
```
### Walton-Rivers
```
cd $ROOT/original_agents
deactivate # make sure you're not still in venv2
source venv3/bin/activate # same virtualenv as for WTFWThat
pip install numpy pandas
sudo apt install default-jdk # this installs OpenJDK 11 for me on my Ubuntu 18.04 system (as of July 2020)
sudo apt install maven
sh build_scripts/ build_walton.sh
```
The following Walton-Rivers agents are available: `iggi`, `internal`, `outer`, `legal_random`, `vdb-paper`, `flawed`, and `piers`. Create the data for each of these agents by running:
```
python create_walton_data.py --num_games 10 --savedir ../replay_data/ --agent_name <agent name> # for one batch
bash batched_create_data.sh walton <agent name> # for many batches using correct file tree structure
```
### Quux
```
cd $ROOT/original_agents
deactivate # make sure you're not still in venv2
source venv3/bin/activate # same virtualenv as for WTFWThat and Walton-Rivers
sh build_scripts/build_quux.sh
```
The following Quux agents are available: `blindbot`, `simplebot`, `valuebot`, `holmesbot`, `smartbot`, `infobot`, `cheatbot`, `newcheatbot`.
```
python create_quux_data.py --num_games 10 --savedir ../replay_data/ --agent_name <agent name> # for one batch
bash batched_create_data.sh quux <agent name> # for many batches using correct file tree structure
```
## 


## Implementation Details
### Replay Data Format
The output data is saved as a Pickle file with the following format:

```py
"""
                 turn 0   ...  turn n        turn 0   ...  turn n
    Game 0   [[ [[obs_0], ..., [obs_n]],    [[act_0], ..., [act_n]] ],
    Game 1    [ [[obs_0], ..., [obs_n]],    [[act_0], ..., [act_n]] ],
      ...
    Game m    [ [[obs_0], ..., [obs_n]],    [[act_0], ..., [act_n]] ]]

"""
```

### How Games are Recorded - Rust Example

Our goal is to incorporate WTFWThat, which is implemented in Rust, into our existing code base that uses Deepmind's Hanabi Learning Environment (HLE) written in Python. In order to do so, we have to convert the observations of the agents at each turn to the same format that HLE uses, which can be challenging since the format involves binary encodings that contain detailed information of game states at various stages. This implies that we'd have to translate HLE's encoding functions to Rust and even perhaps rebuild the WTFWThat's environment, and this complicated process leaves a lot of room for mistakes. 

To overcome this challenge, we developed an approach that does not involve how encoding works but replies on simply replaying the WTFWThat agent's actions in HLE and encode the observations with HLE's existing API. We first create a history of self-play games played by the WTFWThat agent and saved it in a .csv file where each line contains crucial information of a turn, such as the current hands of all players and the corresponding actions. For each of the self-play games, we also save the order of the deck generated at the beginning. With these data, we then replay these games in HLE by initializing the deck and dealing the cards to each player in the same order and performing the actions accordingly at each turn. After each turn is finished, we simply saved the encoded observations with HLE's encoding functions. 
