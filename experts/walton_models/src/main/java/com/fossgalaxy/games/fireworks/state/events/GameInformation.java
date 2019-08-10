package com.fossgalaxy.games.fireworks.state.events;

import com.fossgalaxy.games.fireworks.state.GameState;

public class GameInformation extends GameEvent {
    private final int players;
    private final int handSize;
    private final int infoTokens;
    private final int lives;

    public GameInformation(int players, int handSize, int infoTokens, int lives) {
        super(MessageType.GAME_INFO);
        this.players = players;
        this.handSize = handSize;
        this.infoTokens = infoTokens;
        this.lives = lives;
    }

    @Override
    public void apply(GameState state, int myPlayerID) {
        state.setInformation(infoTokens);
        state.setLives(lives);
        state.getDeck().init();
    }

    @Override
    public String toString() {
        return String.format("Game Information: %d players (hand size of %d), info: %d, lives: %d", players, handSize, infoTokens, lives);
    }

}
