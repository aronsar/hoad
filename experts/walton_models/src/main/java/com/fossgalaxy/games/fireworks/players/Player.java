package com.fossgalaxy.games.fireworks.players;

import com.fossgalaxy.games.fireworks.state.GameType;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;

import java.util.List;

/**
 * Low-level event-driven interface.
 *
 * The player gets given a series of events that describe the game, which it then will apply to it's own copy of the
 * state. This ensures that infomation cannot be leaked to the agent - as they only see events from their perspective.
 */
public interface Player {

    /**
     * Query the player for an action.
     *
     * @return The action the player wishes to perform
     */
    Action getAction();

    /**
     * Receive a message from the game engine.
     *
     * @param msg the message that was received.
     */
    //void sendMessage(GameEvent msg);


    /**
     * Tell the player than an action has occoured, and what events resulted from it.
     *
     * The action itself cannot be executed directly by the player, as some infomation (eg, what card was drawn from
     * the deck) is not visible to it. Instead, it relies on the game engine to inform it of what happened though
     * events.
     *
     * @param agentID the player performing the action
     * @param action the action made by the player
     * @param events the effects that action had on the game state
     */
    void resolveTurn(int agentID, Action action, List<GameEvent> events);

    /**
     * Inform the player of their player ID.
     *
     * @param id the ID of this player
     * @param nPlayers the number of players in the game
     * @param playerNames the players you are playing with (Array of nulls if unknown)
     * @param type the type of game being played
     */
    void setID(int id, int nPlayers, String[] playerNames, GameType type);

    String getName();


    default void onGameOver() {

    }

}