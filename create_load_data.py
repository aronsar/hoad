# create_load_data.py

def create_rainbow_data():
  call("source /data1/shared/venvg2/bin/activate;" \
       "python experts/create_rainbow_data.py --datapath " + args.datapath + ";", shell=True)

def create_example_data():
  # do any necessary stuff
  # call the creation script for this agent

CREATE_DATA_FOR = {
  'rainbow': create_rainbow_data,
  'example': create_example_data}

@gin.configurable
class DataLoader(object):
  @gin.configurable
  def __init__(self, 
      game_type='Hanabi-Full',
      num_players=2,
      num_games=150):

    self.game_type = game_type
    self.num_players = num_players
    self.num_games = num_games

    self.train_data = {} # gameplay data given to model
    self.validation_data = {} # data not given to model, from same agents as train
    self.test_data = {} # data from agents totally unseen to model
    
  def load_data(datadir, agent_name)
    agent_data_filename = agent_name + "_" + str(num_players + "_" \
            + str(num_games) + ".pkl"
    agent_datapath = os.path.join(datadir, agent_data_filename)
    return pickle.load(open(agent_datapath, "rb"), encoding='latin1')
    
  def train_val_test_split(raw_data): #previously named "read"
    # split up raw_data into train, validation, and test
    test_agent = random.choice(list(raw_data.keys()))

    for agent in raw_data:
      if agent == test_agent:
        continue
      split_idx = int(0.9 * len(raw_data[agent]))
      self.train_data[agent] = raw_data[agent][:split_idx]
      self.validation_data[agent] = raw_data[agent][split_idx:]
    
    self.test_data[test_agent] = raw_data[test_agent]

def main(args):
  loader = DataLoader() #gin configured
  raw_data = {}
  
  for agent_name in args.agents_to_use:
    try:
      agent_data = loader.load_data(agent_name)
    except IOError:
      CREATE_DATA_FOR[agent_name]
      agent_data = loader.load_data(agent_name)
    
    raw_data[agent_name] = agent_data   # placing agent_data into a dictionary
  
  loader.train_val_test_split(raw_data)
  return loader
  
if __name__ == "__main__":
  args = parse_args.parse()
  args = parse_args.resolve_configpath(args)
  args = parse_args.resolve_agents_to_use(args)
  main(args)