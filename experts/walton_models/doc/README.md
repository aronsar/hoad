# Walton-Rivers

# Run
```java
//  Use Java 8
// Switch between java version
sudo update-alternatives --config java

//  Run
./run_experiment.sh agent_name agent_name seed_num
./run_experiment.sh iggi iggi2 1
```

# Agent Names
There exists two types of wrapper, constructor type and static method type.

```java
//  In MCTS.java

// Constructor Type
@AgentConstructor("mcts")
public MCTS(int roundLength, int rolloutDepth, int treeDepthMul)    

// Static Method Type
@AgentBuilderStatic("mctsND")
public static MCTS buildMCTSND() 
```

The two wrapper types seems not make a difference for the end user. Instead, user should be aware of when a constructor or method requires argument

To pass in argument, use "[]" follow immediately after the agent name. Place argument within the bracket and sperate using ":" if many argument exist.

```bash
% Example of using mcts & mctsND
java -jar /path/to/fireworks-0.2.6-SNAPSHOT-jar-with-dependencies.jar mcts[1:1:1] mctsND 1
``` 

# Research
For more information, please refer to canocial_encoder.cc.
```c
//  In canocial_encoder.cc

/*
 * (2 - 1) * 5 * 25 + 2 = 127
 */
int HandsSectionLength(const HanabiGame& game) {
 return (game.NumPlayers() - 1) * game.HandSize() * BitsPerCard(game) +
        game.NumPlayers();
}

/*
 * 50 - 2 * 5 +
 * 5 * 5 +
 * 8 +
 * 3 
 * = 76
 */
int BoardSectionLength(const HanabiGame& game) {
 return game.MaxDeckSize() - game.NumPlayers() * game.HandSize() +  // deck
        game.NumColors() * game.NumRanks() +  // fireworks
        game.MaxInformationTokens() +         // info tokens
        game.MaxLifeTokens();                 // life tokens
}

/*
 * 50
 */
int DiscardSectionLength(const HanabiGame& game) { return game.MaxDeckSize(); }

/*
 * 2 + 
 * 4 + 
 * 2 + 
 * 5 +
 * 5 +
 * 5 +
 * 5 +
 * 25 + 
 * 2 
 * = 55
 */
int LastActionSectionLength(const HanabiGame& game) {
 return game.NumPlayers() +  // player id
        4 +                  // move types (play, dis, rev col, rev rank)
        game.NumPlayers() +  // target player id (if hint action)
        game.NumColors() +   // color (if hint action)
        game.NumRanks() +    // rank (if hint action)
        game.HandSize() +    // outcome (if hint action)
        game.HandSize() +    // position (if play action)
        BitsPerCard(game) +  // card (if play or discard action)
        2;                   // play (successful, added information token)
}

/*
 * 2 * 5 * (25 + 5 + 5) = 350
 */
int CardKnowledgeSectionLength(const HanabiGame& game) {
 return game.NumPlayers() * game.HandSize() *
        (BitsPerCard(game) + game.NumColors() + game.NumRanks());
}

/* 127 + 76 + 50 + 55 + 350 = 658 */
std::vector<int> CanonicalObservationEncoder::Shape() const {
  return {HandsSectionLength(*parent_game_) +
          BoardSectionLength(*parent_game_) +
          DiscardSectionLength(*parent_game_) +
          LastActionSectionLength(*parent_game_) +
          (parent_game_->ObservationType() == HanabiGame::kMinimal
               ? 0
               : CardKnowledgeSectionLength(*parent_game_))};
}
```