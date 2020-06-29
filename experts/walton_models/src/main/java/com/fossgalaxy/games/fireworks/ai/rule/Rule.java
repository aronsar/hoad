package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;

/**
 * Base class for
 *
 */
@FunctionalInterface
public interface Rule {

    /**
     * return true if the rule should execute.
     *
     * If this returns true, the rule will be selected for execution. The rule MUST NOT return null if this is the case.
     *
     * @param playerID the playerID of the current agent
     * @param state the current game state
     * @return true if this rule should be selected, false otherwise.
     */
    default boolean canFire(int playerID, GameState state) {
        Action returnedAction = execute(playerID, state);
        return returnedAction != null;
    }

    /**
     * Return true if it is possible this rule could fire, and return false if you can guarantee that it cannot fire.
     * @param playerID The player id that is this turn
     * @param state The state of the board
     * @return false if we should skip this rule, true if we should consult canFire
     */
    default boolean couldFire(int playerID, GameState state){
        return true;
    }

    Action execute(int playerID, GameState state);

    default String fancyName(){
        return String.join(" ", (this.getClass().getSimpleName().split("(?=\\p{Upper})")));
    }

}
