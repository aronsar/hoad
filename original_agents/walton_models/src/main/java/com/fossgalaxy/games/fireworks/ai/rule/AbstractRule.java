package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;

import java.util.function.BiFunction;

/**
 * A base rule with some utility methods for writing rules
 */
public abstract class AbstractRule implements Rule {

    /**
     * Return the next player's playerID
     *
     * @param playerID the current playerID
     * @param state the current game state
     * @return the playerID of the next player
     */
    public int selectPlayer(int playerID, GameState state) {
        return (playerID + 1) % state.getPlayerCount();
    }

    /**
     * Iterate though the players applying a rule function.
     *
     * Loop though each player, applying a function. If that function returns an action that action is selected
     * for our rule to use.
     *
     * @param playerID our player ID
     * @param state the current state
     * @param function the function to apply
     * @return the first action selected, else null.
     */
    public Action loopThoughPlayers(int playerID, GameState state, BiFunction<GameState, Integer, Action> function) {

        for (int i=0; i<state.getPlayerCount(); i++) {
           int nextPlayer = selectPlayer(playerID + i, state);
           if (nextPlayer != playerID) {
               Action action = function.apply(state, nextPlayer);
               if (action != null) {
                   return action;
               }
           }
        }

        return null;
    }

    /**
     * Simple check to see if the rule can fire.
     *
     * This is very inefficient but guaranteed to work assuming that the rule does not decide to fire stochasticaly.
     * It will ask the rule to return a move, if it does then we say the rule will fire else we say the rule does not
     * fire.
     *
     * @param playerID the current playerID
     * @param state the current game state
     * @return true if the rule will return a move, false otherwise
     */
    @Override
    public boolean canFire(int playerID, GameState state) {
        Action action = execute(playerID, state);
        return action != null;
    }

    public String toString() {
        return fancyName();
    }

}
