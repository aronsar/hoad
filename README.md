SAVE LOCALLY**********************************************************

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

**run_experiment.py**: this script is meant to run full end to data creation, training, and evaluation based on command line arguments and the gin config file. It will need to change a little to accommodate the structural changes in create_data and load_data

**create_data.py and load_data.py**: some pretty major refactoring is necessary here. Because we will be making more agents available for use, and we will often want to include/exlude certain agents from training runs, it will make most sense to create data files such that each agent's games are contained in one file, instead of as values in a dictionary as was done previously. However the raw data returned by the new script create_load_data.py will still be in the exact same format as before. This format is shown below: 



Since the new agents we'll make available are all written in different languages, we'll need python wrapper functions for each. Pseudocode for this script is included in create_load_data.py. One thing that is missing from this file is the generator function. This functionality has been moved to the DataGenerator class. More on that later.

```
# create_load_data.py

def main(agents_to_use):
    for agent_name in agents_to_use:
        if resolve_agentpath(agent_name, datadir) does not exist:
            call function that creates data for that agent
        agent_data = load_data(agent_name)
        raw_data\[agent_name\] = agent_data     # placing agent_data into a dictionary
    
    return raw_data
 ```          

**train.py**: this file is not changing structurally. 

this is the only python 2 script we use, and moreover we use it with tensorflow 1.14 instead of 2.0, because that's the only way we can load in DeepMind's Rainbow agents. Moving forward, we will also use different agents, and each agent will need its own creation script. As such, we will need to change the format of the data. so create_data will need to be refactored so that it can call on other scripts which generate data from other agents (since some of the agents will be in different languages, lua being common). At that time, we will need another layer of abstraction, so create_data will be upgraded to python 3, and the existing code in it will be put in a file called create_rainbow_data.py and that will be in python 2. This additional layer of abstraction will also perform the duty of load_data of checking to see if the data has already been created

load_data.py: this script is being replaced by the DataGenerator class

