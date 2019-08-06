import keras
import numpy as np


# TODO: implement/merge below
# instead of current implementation, have each mode file subclass this class, and
# move DataGenerator to its own .py file
class BaseDataGenerator(keras.utils.Sequence):
    def __init__(self, raw_data, cfg={}):
        """
        __init__(self, raw_data)
        Args:
            raw_data: [agent_name][game_number][0=obs, 1=act][game_step][vec_dim]
            cfg: hyperparams injected from gin config. Should be empty in base class and used only in child classes. 
        Usage:
            Initialization
        """

        self.epoch_len = 0  # total game steps, N

        self.obs = np.array([], dtype=np.float64).reshape(0, self.obs_dim)
        self.act = np.array([], dtype=np.float64).reshape(0, self.act_dim)
        self.parse_data(raw_data)

        self.on_epoch_end()

    # Customizable
    def parse_data(self, raw_data):
        """
        parse_data(self, raw_data)
        Args:
            raw_data: pickle data from loader
        Usage:
            Convert raw pickle data to numpy array.
        """

        # Ideally, a pickle file only has data for a particular agent
        agent_names = list(raw_data.keys())

        for agent_name in agent_names:
            for game_number, _ in enumerate(raw_data[agent_name]):
                new_obs = np.array(raw_data[agent_name][game_number][0])
                new_act = np.array(raw_data[agent_name][game_number][1])
                self.obs = np.vstack([self.obs, new_obs])
                self.act = np.vstack([self.act, new_act])

        if np.shape(self.obs)[0] != np.shape(self.act)[0]:
            raise ("Incorrect Behavior occur during parse_data")
        else:
            self.epoch_len = np.shape(self.obs)[0]

    # Required
    def __len__(self):
        """
        __len__(self)
        Args: None
        Usage:
            Specificies the length of generator. The length should be equal to 
            total sample count / batch size. 
        """

        return int(self.epoch_len / self.batch_size)

    # Required
    def __getitem__(self, index):
        """
        __getitem__(self, index)
        Args:
            index - This argument is fed from keras.utils.Sequence. The index will never exceed the value specified in __len().
        Usage:
            Return a batch of data. Act as a generator.
        """

        sampled_indices = self.batch_sampler(index)
        return self.obs[sampled_indices, :], self.act[sampled_indices, :]

    # Customizable
    def batch_sampler(self, index):
        """
        batch_sampler(self)
        Args: None
        Usage:
            Specificies the logic of sampling the data. Current behavior set to sample sequential non-repitive indices which gradually increases until all data is used. 
        """

        self.logging(BaseDataGenerator.batch_sampler)

        lower_bound = index * self.batch_size
        upper_bound = min((index + 1) * self.batch_size, self.epoch_len)

        return range(lower_bound, upper_bound)

        # Customizable

    def on_epoch_end(self):
        """
        on_epoch_end(self)
        Args: None
        Usage:
            Specificies the action to perform at the end of an epoch. Current behavior set to shuffle self.obs & self.act correspondingly.
        """

        ids = np.arange(0, self.epoch_len, 1)
        np.random.shuffle(ids)
        if self.shuffle == True:
            self.obs = self.obs[ids]
            self.act = self.act[ids]
