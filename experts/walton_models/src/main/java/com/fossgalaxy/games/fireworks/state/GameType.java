package com.fossgalaxy.games.fireworks.state;

public enum GameType {

    /**
     * The histroic mode used for the games
     *
     * When the team runs out of lives, the agents get the score achieved at the end of the game
     */
    NO_LIVES_CURRENT,

    /**
     * The re-interpreted version of the game
     *
     * When the team runs out of lives, the agents get a score of zero
     */
    NO_LIVES_ZERO
}
