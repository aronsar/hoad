# Hanabi Open Agent Dataset (HOAD)

This is a repository for Hanabi playing agents that can all play with each
other in the Hanabi Learning Environment (HLE). One of the main advantages of
this repository is the ease with which additional agents can be added from
online sources. The codes along with descriptions for each of the source agents
can be found in `original_agents`. In `imitator_agents`, we convert the
original agents to neural imitator models, which can play with each other in
the HLE. In `experiments`, we include our code for the cross-play results of
the imitator agents, as well as the adhoc play results of the meta-learning
agents and the naive agents. 

### Datasets and Pretrained Models

At the below link you will find the original agent replay data, the imitator agent replay data, and a zipped directory structure containing multiple models:
https://drive.google.com/drive/folders/1BTI3NPEs1BYNhrY-J42_X-KgZklkyWJK?usp=sharing

### Getting Started:
```
# Build the Hanabi Environment
cd hanabi_env
sh build_hanabi.sh
```

Follow setup instructions in `original_agents/README.md`  
Follow setup instructions in `imitator_agents/README.md`  
At this point you may run the cross-play experiments.  
Follow setup instructions in `adhoc_play/maml/README.md`  
At this point you may run the maml adhoc-play experiment.  
Follow setup instructions in `adhoc_play/naive_mlp/README.md`  
At this point you may run the naive mlp adhoc-play experiment

