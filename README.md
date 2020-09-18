# HOAD

Because DeepMind wrote their Rainbow agents in Py 2.7 and tf 1.x, the data
creation script, which interfaces with that code, uses Py 2.7 and tf 1.x.
However, once the data is produced, we only use Py 3.6 and tf 2.1 for building
and training our models.

### Getting Started:
```
# Install necessary programs/packages:
sudo apt install cmake

# Build the Hanabi Environment
cd hanabi_env
sh build_hanabi.sh
```


### Framework Specification

**utils/parse_args.py**: functions to aid in parsing command line arguments. The parser is designed to be used for all the scripts. Helper functions with the word "resolve" are included which contain various logic to determine things like the full name of the data path, the config path, the model checkpoint path, etc. Note: if a --xxxxpath (be it data, or config, or whatever) is provided on the command line, the specified path takes precedence over the "resolved" path.

**utils/random_search.py**: a flexible function for optimizing hyper-parameters. Hyper-parameters are passed into the function by the user which allows custom random distributions, and results for each set of hyper-parameters are saved under the user-defined path. File locking is also implemented which allows the output file to be safely modified concurrently by multiple processes.

Argument `acc_measure` is a user-defined function that is responsible for measuring the accuracy with the metrics that the user desires. The input arguments must take one set of hyper-parameters that are required to build and train user's model, and the returned value should be a floating number of the accuracy or other measuring metrics of the performance of the model that is built with the input set of hyper-parameters.

Argument `naming` is a dictionary of user-defined functions that allows specified hyper-parameters to use custom naming schemes for better clarity. For instance, if the hyper-parameter variable for activation function is called `act_func`, then setting `naming={'act_func': lambda x: x.__name__}` will save "ReLU" in the output file instead of "<class 'tensorflow.python.keras.layers.advanced_activations.ReLU'>". Hyper-parameters that are not specified here will be saved by first passing into `str()`. *Note: this example only works for advanced functions in Keras. Users should define their own functions to support all variables in the search space.*

Sample code for defining `acc_measure` and `naming`:

```py
# Let this file be `ganabi/rs_sample.py`
import numpy as np
from numpy.random import uniform, randint, choice
from tensorflow.keras.utils import to_categorical
from tensorflow.keras.models import Model
from tensorflow.keras.layers import Dense, Input, ReLU, PReLU, LeakyReLU, Softmax
from tensorflow.keras.optimizers import Adam

from utils.random_search import random_search

def acc_measure(lr, bs, n_neuron, act_func):
    SIZE_INPUT = 3
    SIZE_OUTPUT = 5

    # Model Construction
    input = Input(shape=SIZE_INPUT)
    layer = Dense(n_neuron)(input)
    layer = act_func()(layer)
    out = Dense(SIZE_OUTPUT)(layer)
    out = Softmax()(out)
    model = Model(inputs=input, outputs=out)
    model.compile(optimizer=Adam(lr=lr, decay=0),
                  loss='categorical_crossentropy',
                  metrics=['accuracy'])

    # Dummy function to be modeled:  int(3 * feat1 * feat2 + feat3 + noise)
    def f(X):
        size = X.shape[0]
        y = 3 * X[:,0] * X[:,1] + X[:, 2] + np.random.uniform(-0.2, 0.2, size)
        y = to_categorical(y.astype(int), SIZE_OUTPUT)
        return y

    # Dummy data creation
    np.random.seed(0)
    X_tr = np.random.uniform(0, 1, [1000, SIZE_INPUT])
    X_va = np.random.uniform(0, 1, [100, SIZE_INPUT])
    Y_tr = f(X_tr)
    Y_va = f(X_va)

    # Model fitting
    hist = model.fit(X_tr, Y_tr, epochs=30, verbose=1,
                     validation_data=(X_va, Y_va), batch_size=bs)
    best_acc = max(hist.history['val_accuracy'])
    return best_acc

def naming():
    return {'act_func': lambda x: x.__name__}
```

Then, the search can be performed with the following and results will be saved in `ganabi/output/random_search/results.csv`

```py
$ python3 -i rs_sample.py
>>> N = 10 # Number of sets of hyper-parameters to run
>>> params = {
...     'lr':       uniform(0, 0.01, size=N),
...     'bs':       randint(32, 65, size=N),
...     'n_neuron': randint(50, 200, size=N),
...     'act_func': choice(a=[ReLU, PReLU, LeakyReLU],
...                        p=[0.4, 0.4, 0.2], size=N)
... }
>>> path_out = 'output/random_search'
>>> naming = {'act_func': lambda x: x.__name__}
>>> random_search(path_out, acc_measure, params, naming)
```

Gin-config can be used instead. With Gin-config, the above can be achieved with the following config.gin file

```gin
# This is rs.config.gin
N=10 # Number of sets of hyper-parameters to run

random_search.path_out = 'output/random_search'   # Using a relative path here for demo only
random_search.acc_measure = @acc_measure
random_search.params = {
    'lr':       @lr/uniform(),
    'bs':       @bs/randint(),
    'n_neuron': @n_neuron/randint(),
    'act_func': @act_func/choice()
}
random_search.naming = @naming()
random_search.seed = None

lr/uniform.low = 0
lr/uniform.high = 0.01
lr/uniform.size = %N

bs/randint.low = 32
bs/randint.high = 64
bs/randint.size = %N

n_neuron/randint.low = 50
n_neuron/randint.high = 200
n_neuron/randint.size = %N

act_func/choice.a = [@ReLU, @PReLU, @LeakyReLU]
act_func/choice.p = [0.4, 0.4, 0.2]
act_func/choice.size = %N
```

and the actual search can be performed with the code below

```py
# This is where `random_search()` is invoked
import gin
from utils.random_search import random_search
from rs_sample import *

# Include activations used in random search
EXTERN_FNS = [ReLU, PReLU, LeakyReLU, uniform, randint, choice]
for fn in EXTERN_FNS:
    gin.external_configurable(fn)
gin.external_configurable(acc_measure)
gin.external_configurable(naming)
gin.parse_config_file('rs.config.gin')
random_search()
```


**run_experiment.py**: this script is meant to run full end to data creation, training, and evaluation based on command line arguments and the gin config file. It will need to change a little to accommodate the structural changes in create_data and load_data. The config file of the mode and the current git hash are saved to the output directory called runxxx, so that the experiment can be rerun with the exact same inputs at a later date.

**create_data.py and load_data.py**: some pretty major refactoring is necessary here. Because we will be making more agents available for use, and we will often want to include/exlude certain agents from training runs, it will make most sense to create data files such that each agent's games are contained in one file, instead of as values in a dictionary as was done previously. However the raw data returned by the new script create_load_data.py will still be in the exact same format as before. This format is shown below (courtesy of Tim): 

Since the new agents we'll make available are all written in different languages, we'll need python wrapper functions for each. Pseudocode for this script is included in create_load_data.py. One thing that is missing from this file is the generator function. This functionality has been moved to the DataGenerator class. More on that later.

**train.py**: this file is not changing structurally; only debugging type changes. The purpose of this file is to load in the model architecture, and to train the model based on the gin config file. Saves the model in checkpoint file when done. #TODO: implement regular checkpointing once the training process is taking a long time so if interrupted, the trainer knows where to pick back up. If the -evalonly flag is set, then train attempts to load the model instead of training it. Note: the checkpoint file is saved to outputs/runxxx/checkpoints. Any tensorboard info is saved to outputs/runxxx/results

**eval.py**: this script is provided so that given just a runxxx directory, you can recreate results months from now. Also so that other researchers can recreate our results. To evaulate the performance of a model on a dataset, this script uses a gin config file, an optionally specified set of agents, and an optionally specified checkpoint path. It loads in the checkpoint, calls create_load_data, and then on each agent in the returned dictionary it runs a bunch of forward passes of the model on the data to get an accuracy, which it prints. 
<!--stackedit_data:
eyJoaXN0b3J5IjpbLTQ5MzQ5OTc1MCwtMTMxMjAwMTY0NF19
-->
