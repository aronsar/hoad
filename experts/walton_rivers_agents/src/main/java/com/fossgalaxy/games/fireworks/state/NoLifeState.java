package com.fossgalaxy.games.fireworks.state;

/**
 * A state that returns 0 if the game is lost.
 */
public class NoLifeState extends BasicState {

    public NoLifeState(int playerCount) {
        super(playerCount);
    }

    public NoLifeState(int handSize, int playerCount) {
        super(handSize, playerCount);
    }

    public NoLifeState(BasicState state) {
        super(state);
    }


    @Override
    public GameState getCopy() {
        return new NoLifeState(this);
    }

    @Override
    public int getScore() {

        if (getLives() == 0) {
            return 0;
        }

        return super.getScore();
    }
}
