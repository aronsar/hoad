package com.fossgalaxy.games.fireworks.state.events;

import com.fossgalaxy.games.fireworks.state.GameState;

import java.io.Serializable;

public abstract class GameEvent implements Serializable {
    public static final int UNKNOWN_TURN = -1;

    private final MessageType id;
    private final int turnNumber;

    public GameEvent(MessageType id){
        this(id, UNKNOWN_TURN);
    }

    public GameEvent(MessageType id, int turnNumber) {
        this.id = id;
        this.turnNumber = turnNumber;
    }

    public abstract void apply(GameState state, int myPlayerID);

    @Deprecated
    public void apply(GameState state){
        apply(state, -1);
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public MessageType getEvent() {
        return id;
    }

    public boolean isVisibleTo(int playerID) {
        return true;
    }

}
