package com.fossgalaxy.games.fireworks;

public class GameStats {
    /**
     * A unique ID for this game.
     */
    public final String gameID;

    /**
     * The number of players that were in this game
     */
    public final int nPlayers;

    /**
     * The score at the final move of the game.
     */
    public final int score;

    /**
     * The number of lives remaining at the end of the game.
     * <p>
     * This value being zero indicates that the game ended due to the player's losing their lives
     */
    public final int lives;

    /**
     * The number of moves made in the game
     */
    public final int moves;

    /**
     * The number of information tokens remaining at the end of the game
     */
    public final int information;

    /**
     * The number of illegal (cheating) moves made during this game.
     * <p>
     * If this value is greater than zero, then at least one agent in the game should be considered buggy.
     */
    public final int disqal;

    public GameStats(String gameID, int players, int score, int lives, int moves, int information, int disqual) {
        this.gameID = gameID;
        this.nPlayers = players;
        this.score = score;
        this.lives = lives;
        this.moves = moves;
        this.information = information;
        this.disqal = disqual;
    }

    @Override
    public String toString() {
        return String.format("%d in %d moves (%d lives left)", score, moves, lives);
    }
}
