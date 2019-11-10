import os
import random, pickle
import numpy as np
from DataLoader import DataLoader

def main():
   data_loader = DataLoader(datapath = "/data1/shared/agent_data/",
           arff_data_path = "raw/",
           target_name = "",
           num_gamese = 1000
           )
   data_loader.all_agents_datadir = [name for name in os.listdir(data_loader.datapath)]
   data_loader.get_source_data()
    
    
    
    
if __name__== '__main__':
    main()
