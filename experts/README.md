Below are introductions to each of the agents contained herein.

## WTFWThat

### To run:

The following command will create 10 games of data with 5 players under existing directory `output`:

`python3 experts/create_WTFWT_data.py --n 10 --p 5 --P output -q `

Run `python3  experts/create_WTFWT_data.py -h` for details.

Overview
------

### Output Data format
The output data is saved as a Pickle file with the following format:

```py
"""
                 turn 0   ...  turn n        turn 0   ...  turn n
    Game 0   [[ [[obs_0], ..., [obs_n]],    [[act_0], ..., [act_n]] ],
    Game 1    [ [[obs_0], ..., [obs_n]],    [[act_0], ..., [act_n]] ],
      ...
    Game m    [ [[obs_0], ..., [obs_n]],    [[act_0], ..., [act_n]] ]]

"""
```

### Goal

Our goal is to incorporate WTFWThat, which is implemented in Rust, into our existing code base that uses Deepmind's Hanabi Learning Environment (HLE) written in Python. In order to do so, we have to convert the observations of the agents at each turn to the same format that HLE uses, which can be challenging since the format involves binary encodings that contain detailed information of game states at various stages. This implies that we'd have to translate HLE's encoding functions to Rust and even perhaps rebuild the WTFWThat's environment, and this complicated process leaves a lot of room for mistakes. 

### Implementation

To overcome this challenge, we developed an approach that does not involve how encoding works but replies on simply replaying the WTFWThat agent's actions in HLE and encode the observations with HLE's existing API. We first create a history of self-play games played by the WTFWThat agent and saved it in a .csv file where each line contains crucial information of a turn, such as the current hands of all players and the corresponding actions. For each of the self-play games, we also save the order of the deck generated at the beginning. With these data, we then replay these games in HLE by initializing the deck and dealing the cards to each player in the same order and performing the actions accordingly at each turn. After each turn is finished, we simply saved the encoded observations with HLE's encoding functions. 

### Agent Strategy
WTFWThat Agent utilized a variation of ["information strategy"](https://sites.google.com/site/rmgpgrwc/research-papers/Hanabi_final.pdf?attredirects=0) which is a type of hat guessing strategy where all players follow a predefined protocol to give hint at each turn. The given hint does not carry its original meaning and is encoded to carry more information which can be decoded by the other plays with their common protocol. With this technique, players are able to gain much more information from every hint to the extent that they can perform almost as well as if they were playing knowing their own cards as shown in the table below (`cheat` strategy means players can see their own cards, and `info` strategy is the information strategy used by WTFWThat. Result are created from 20,000 games).

| Strategy  |   2p    |   3p    |   4p    |   5p    |
|---------|------------------|------------------|------------------|------------------|
| cheat   | 24.8594 ± 0.0036 | 24.9785 ± 0.0012 | 24.9720 ± 0.0014 | 24.9557 ± 0.0018 |
|         | 90.59 ± 0.21 % | 98.17 ± 0.09 % | 97.76 ± 0.10 % | 96.42 ± 0.13 % |
| info    | 22.5194 ± 0.0125 | 24.7942 ± 0.0039 | 24.9354 ± 0.0022 | 24.9220 ± 0.0024 |
|         | 12.58 ± 0.23 % | 84.46 ± 0.26 % | 95.03 ± 0.15 % | 94.01 ± 0.17 % |


## Walton-Rivers Bots

## FireFlower

## Rainbow

## Smartbot AI set
