# Instructions

We use the same virtual environment for this agent as we did for the imitator
training. So please run: 
``` 
$ source ../../imitator_agents/venv3/bin/activate
$ python train.py --agent <agent_name> 
```

The possible agent names are `iggi`, `outer`, `piers, `quux-holmesbot`,
`quux-simplebot`, `quux-valuebot`, `rainbow`, and `van-den-bergh`.

Be sure to run the script at least 10 times for each agent. This is necessary
to reduce the variation of the adhoc play results, since the naive mlp trains
on only 10 games. The adhoc play script randomly chooses one of the naive mlp
models each time it plays a game.
