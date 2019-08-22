import csv
import os
import fcntl
import numpy as np
import gin

@gin.configurable
def random_search(path_out, acc_measure, params, naming={}, seed=None):
    """ Perform random hyperparameter search on given param space and save the
         accuracy to `{path_out}/results.csv`.

         See `README.md` for sample code.

    Arguments:
        - path_out: str
            Absolute path to the directory where `results.csv` will be saved.
            E.g.: '/user/ganabi/output'
        - acc_measure: func
            Function that builds the model with a given set of parameters and
            returns accuracy.
        - params: dict
            Sets of parameters to be used for the search.
        - naming: dict, default {}
            Custom naming schemes to be used. Use this for non-integer or
            non-string hyperparameters, such as activation functions. For
            instance, 'ReLU' will be stored in `results.csv` instead of
           "<class 'tensorflow.python.keras.layers.advanced_activations.ReLU'>"
        - seed: int, default None
            Seed to be used for RNG. Current time is used if None.

    """
    np.random.seed(seed)
    # All parameter sets should have the same number of elements
    lengths = np.array([len(i) for i in [*params.values()]])
    assert np.all(lengths == lengths[0])
    n = lengths[0]

    os.makedirs(path_out, exist_ok=True)

    fn = path_out + '/results.csv'

    with open(fn, 'a') as f:
        writer = csv.writer(f, delimiter=',')

        fcntl.flock(f, fcntl.LOCK_EX)
        # write header if file is empty
        if f.seek(0, 2) == 0:
            header = ['acc'] + [*params]
            writer.writerow(header)
            f.flush()
        fcntl.flock(f, fcntl.LOCK_UN)

        for i in range(n):
            # Extract the ith set of hyperparameters and make them a dict
            hypers = [params[key][i] for key in params]
            hypers = dict(zip(params.keys(), hypers))

            acc = acc_measure(**hypers)

            # Generate hyperparams values into readable .csv supported format
            row = [acc]
            for key in hypers:
                # Use user-defined naming schemes if specified
                try:
                    scheme = naming[key]
                except KeyError:
                    scheme = str

                row += [scheme(hypers[key])]

            fcntl.flock(f, fcntl.LOCK_EX)
            writer.writerow(row)
            f.flush()
            fcntl.flock(f, fcntl.LOCK_UN)
