package com.fossgalaxy.games.fireworks.state;

import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;

import java.util.List;
import java.util.Objects;

public class HistoryEntry {
	
	/**
	 * The player that performed this action.
	 * 
	 * If the player ID is negative, the events occoured without a player making an action.
	 * This should only happen at the start of the game to deal cards.
	 */
    public final int playerID;
    
    /**
     * The action the player made.
     * 
     * If this is null, the events are from the game rather than another player's action
     * (ie, dealing cards at the start of the game).
     * 
     * JWR: yes, it's a little hacky - I couldn't think of a better way of doing it, I'm sorry.
     */
    public final Action action;
    
    /**
     * Our observations of the action.
     */
    public final List<GameEvent> history;

    public HistoryEntry(int playerID, Action action, List<GameEvent> history){
        this.playerID = playerID;
        this.action = action;
        this.history = history;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HistoryEntry that = (HistoryEntry) o;
        return playerID == that.playerID &&
                Objects.equals(action, that.action) &&
                Objects.equals(history, that.history);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, action, history);
    }
}
