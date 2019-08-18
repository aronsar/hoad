import os
import numpy as np
import pickle
import logging
import tensorflow.keras.backend as K
from tensorflow.keras.models import Model
from tensorflow.keras.layers import Dense, Embedding, Input, Flatten, Dropout
from tensorflow.keras.layers import BatchNormalization, LeakyReLU, ELU, Softmax
from tensorflow.keras.layers import PReLU
from tensorflow.keras.activations import sigmoid, tanh
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.regularizers import l2
# from tensorflow.keras.utils import plot_model
from DataGenerator import *

# suppress TF warnings
logging.getLogger("tensorflow").setLevel(logging.ERROR)
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'

class Mlp(object):
    def __init__(self, io_sizes, out_activation, loss,
                 metrics, lr, batch_size, hl_activations, hl_sizes, decay,
                 bNorm=False, dropout=False, regularizer=None, verbose=1):
        """ Initialize parameters required for training & testing the network.
            Structure:
                          [Input Layer]
                      length: @io_sizes[0]
                                |
                                V
                         [Hidden Layer 1]
                      length: @hl_sizes[0]
                                |
                                V
                               ...
                         [Hidden Layer n]
                      length: @hl_sizes[n-1]
                                |
                                V
                             [Output]
                      length: @io_sizes[1]
        Arguments:
            - lr: int
                Learning rate of the network.
            - io_sizes: tuple
                Sizes of the input and output layers.
            - out_activation: func
                Activation function for the output layer.
            - loss: str
                Name of the loss function to be used.
            - metrics: list
                List of metrics to be used for training.
            - batch_size: int
                Size of each mini-batch.
            - hl_activations: tuple
                Hidden layer activation functions that connect the layers.
            - hl_sizes: tuple
                Sizes of each hidden layer.
            - decay: float
                Learning rate decay.
             - bNorm: boolean, default False
                 Indicates whether to use Batch Normalization on all hiddens.
            - dropout: boolean, default False
                Incidates whether to use dropout for hidden layers.
            - regularizer: func, default None
                Regularizer to use.
            - verbose: int, default 1
                Value for `verbose` in keras fit() function.
        Returns:
            - None
        """

        assert(len(hl_sizes) == len(hl_activations))

        self.lr = lr
        self.decay = decay
        self.batch_size = batch_size
        self.hl_activations = hl_activations
        self.hl_sizes = hl_sizes
        self.io_sizes = io_sizes
        self.out_activation = out_activation
        self.bNorm = bNorm
        self.dropout = dropout
        self.regularizer = regularizer
        self.loss = loss
        self.metrics = metrics
        self.verbose = verbose
        self.model = None # Model will be stored here after construct_model()
        self.hist = None  # History will be stored here after train_model()

    def construct_model(self):
        """ Construct model based on attributes
        """

        input = Input(shape=self.io_sizes[0], name='input')

        layer = input
        for i in range(len(self.hl_sizes)):
            n = self.hl_sizes[i]
            a = self.hl_activations[i]
            # Connect layers
            z = Dense(n, kernel_regularizer=self.regularizer,
                      name='hidden_%d' % i)(layer)

            if a._keras_api_names[0].split('.')[1] == 'activations':
                z = a(z)
            else: # Keras advance functions
                z = a()(z)

            if self.bNorm:
                z = BatchNormalization()(z)
            if self.dropout:
                z = Dropout(0.5)(z)
            layer = z

        out = Dense(self.io_sizes[-1], name='output')(layer)
        out = self.out_activation()(out)

        self.model =  Model(inputs=input, outputs=out)

    def train_model(self, gen_tr, gen_va, n_epoch=100, callbacks=None,
                    verbose=False, workers=1, use_mp=False):
        """
        Train self.model with dataset stored in attributes.
        Arguments:
            - gen_tr: keras.utils.Sequence
                Generator that will generate the training data batch-by-batch
                during training.
            - gen_va: keras.utils.Sequence
                Generator that will generate the validation data batch-by-batch
                during validation.
            - Y_va: np.matrix
                Validation matrix that contains the actions.
            - n_epoch: int, default 150
                Number of epochs to train.
            - callbacks: list, default None
                List of keras.callbacks.Callback objects to run
            - verbose: boolean, default True
                If true, model info will be displayed.
            - workers: int, default 1
                Number of workers for fit_generator().
        """
        if verbose:
            print("Learning Rate:\t", self.lr)
            print("LR Decay:\t", self.decay)
            print("Batch Size:\t", self.batch_size)
            print("Regularizer:\t", self.regularizer)
            print("Loss function:\t", self.loss)
            print("Callbacks:\t", callbacks)
            self.model.summary()
            print()

        self.model.compile(optimizer=Adam(lr=self.lr, decay=self.decay),
                           loss=self.loss, metrics=self.metrics)

        self.hist = self.model.fit_generator(generator=gen_tr,
                                             validation_data=gen_va,
                                             epochs=n_epoch,
                                             verbose=self.verbose,
                                             use_multiprocessing=use_mp,
                                             workers=workers,
                                             callbacks=callbacks)
