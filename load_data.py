# Copy over code from earlier version.
# Load the data if it already exists, otherwise create it

from utils import parse_args

@gin.configurable
class DataReader:
    def __init__(self, 
                 args, 
                 batch_size=1, 
                 train_test_split=.9, 
                 num_unseen_agents=1,
                 test_agent=0):
        data_path = #TODO
        self.all_data = pickle.load(open(data_path, "rb"))
        self.train_data = {} # gameplay data given to model
        self.validation_data = {} # data not given to model, from same agents as train
        self.test_data = {} # data from agents totally unseen to model
        self.batch_size = batch_size
        self.test_agent = random.choice(list(self.all_data.keys()))
        
        # split up all_data into train, validation, and test
        for agent in self.all_data:
            if agent == self.test_agent:
                continue
            split_idx = int(0.9 * len(self.all_data[agent]))
            self.train_data[agent] = self.all_data[agent][:split_idx]
            self.validation_data[agent] = self.all_data[agent][split_idx:]

        self.test_data[self.test_agent] = self.all_data[self.test_agent]
    
    def next_batch(eval=False):
        
        return adhoc_games, gen_agent_games


def main(args):
    #TODO
    data_reader = DataReader(args)
    return data_reader


if __name__ == "__main__":
    args = parse_args.parse()
    main(args)
