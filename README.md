# ganabi

Because DeepMind wrote their Rainbow agents in Py 2.7 and tf 1.x, the data creation script, which interfaces with that code, uses Py 2.7 and tf 1.x. However, once the data is produced, we only use Py 3.6 and tf 2.0 for building and training our models.

### Getting Started:
```
fork/clone repo into your home folder
cd ~/ganabi/hanabi-env
cmake .
make
cd ..
source /data1/shared/venvg2/bin/activate # use venvg for python 3 
mkdir data # FIXME: should get created if it doesn't exist
python create_data.py
```

### Framework Specification

**utils/parse_args.py**: functions to aid in parsing command line arguments. The parser is designed to be used for all the scripts. Helper functions with the word "resolve" are included which contain various logic to determine things like the full name of the data path, the config path, the model checkpoint path, etc. Note: if a --xxxxpath (be it data, or config, or whatever) is provided on the command line, the specified path takes precedence over the "resolved" path.

**run_experiment.py**: this script is meant to run full end to data creation, training, and evaluation based on command line arguments and the gin config file. It will need to change a little to accommodate the structural changes in create_data and load_data. The config file of the mode and the current git hash are saved to the output directory called runxxx, so that the experiment can be rerun with the exact same inputs at a later date.

**create_data.py and load_data.py**: some pretty major refactoring is necessary here. Because we will be making more agents available for use, and we will often want to include/exlude certain agents from training runs, it will make most sense to create data files such that each agent's games are contained in one file, instead of as values in a dictionary as was done previously. However the raw data returned by the new script create_load_data.py will still be in the exact same format as before. This format is shown below (courtesy of Tim): 

Since the new agents we'll make available are all written in different languages, we'll need python wrapper functions for each. Pseudocode for this script is included in create_load_data.py. One thing that is missing from this file is the generator function. This functionality has been moved to the DataGenerator class. More on that later.

**train.py**: this file is not changing structurally; only debugging type changes. The purpose of this file is to load in the model architecture, and to train the model based on the gin config file. Saves the model in checkpoint file when done. #TODO: implement regular checkpointing once the training process is taking a long time so if interrupted, the trainer knows where to pick back up. If the -evalonly flag is set, then train attempts to load the model instead of training it. Note: the checkpoint file is saved to outputs/runxxx/checkpoints. Any tensorboard info is saved to outputs/runxxx/results

**eval.py**: this script is provided so that given just a runxxx directory, you can recreate results months from now. Also so that other researchers can recreate our results. To evaulate the performance of a model on a dataset, this script uses a gin config file, an optionally specified set of agents, and an optionally specified checkpoint path. It loads in the checkpoint, calls create_load_data, and then on each agent in the returned dictionary it runs a bunch of forward passes of the model on the data to get an accuracy, which it prints. 
