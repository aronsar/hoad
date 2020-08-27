# MAML


### Setup
```
virtualenv venv3 -p python3
source venv3/bin/activate
pip install Pillow tensorflow-gpu==2.1 matplotlib gin-config
vim config/hoad.config.gin
    --> set data_path to imitator replay data parent folder (subfolders must be named after the agents, and must be structured like the replay data of the original agents
    --> set test_agent as desired (this agent will not be used to train the meta learner; all others will)
    --> set num_meta_train (it takes about 30000 meta steps to finish with one epoch)

python main.py # takes me 7 hours per epoch per test agent on an RTX 2060 Super, though I can train 4 at a time
# training checkpoints, logs, and weights are saved in ./results for each test agent
```


