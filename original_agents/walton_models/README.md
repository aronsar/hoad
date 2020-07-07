# An implementation of Hanabi in Java
This is designed to allow the development of bots which play the board game [hanabi][hanabi].

## Authors
* Joseph Walton-Rivers
* Piers Williams

## Directories
* src/ - Source Code
* research/ - Materials for research purposes
* target/ - Maven compiled artifacts

## Assumptions
* stdout is for data (we write the code as filters)
* stderr is for debug logging (this might change if we use it for competitions)
* enviroment variables are awesome (webpigeon spent too much time around docker) and therefor we'll use them for configuration

## Adding new agents
New agents must be added to either buildAgent method in order to run. Most of these are calls to factory methods elseware in the codebase. This is because most of the rule based AIs are the same agent with different rules set. Therefore, your agent is expected to be present as a case statement in build agent.

Again, for a competition runner, this might change (to use reflection) - at the moment it's coded this way for ease of development.

Sample agents live under ```com.fossgalaxy.games.fireworks.ai```. 

## Running the code

### Running locally
1. checkout this repo
2. use ```run_experiment.sh``` or ```run_mixed.sh``` to collect data - these scripts will also generate a log for you
3. you can also run App2CsvMulti or MixedAgentGame from the jarfile instead (they will write their csv data to standard out).
4. analyise data

### Running me on YARCC
The game engine has been designed to make it possible to run on YARCC. In src/main/scripts you will find the jobfile and scripts needed to make this work. The basic process can be summed up as:

1. checkout this repo
2. use src/main/scripts/compileCluter.sh to generate matchups
3. use qsub to submit a job (edit runParamterJob in src/main/scripts before running!)
4. use filter.sh to generate results.csv from the job outputs
5. copy results.csv to somewhere else and analyise it (my analysis scripts will be made public soon)

### Running me on YARCC (new)
1. checkout this repo
2. run ./src/main/scripts/buildAndExecuteMixed.sh to do all the above
3. cat all of your ```*.o*``` files together to form your csv file
4. copy results.csv to somewhere to analyise it.

## framework parameters

### Enviroment Variables
The runner recognises the following enviroment variables:

* ```FIREWORKS_AGENTS``` - a comma seperated list of agents to evaluate (see SetupUtils for default)
* ```FIREWORKS_AGENTS_PAIRED``` - a comma seperated list of agents to evaluate against (see SetupUtils for default)
* ```FIREWORKS_NUM_SEEDS``` - the number of seeds to generate when generating argument files (default is 10)
* ```FIREWORKS_REPEAT_COUNT``` - the number of times to repeat a given seed (useful for non deterministic agents) (default is 2)
* ```FIREWORKS_META_SEED``` - the seed used for the RNG which generates seeds for games (default is to use java default)

[hanabi]: https://boardgamegeek.com/boardgame/98778/hanabi