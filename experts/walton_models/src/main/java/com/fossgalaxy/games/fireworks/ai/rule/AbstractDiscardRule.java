package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;

/**
 * Abstract rule that checks that it is currently legal to discard a card before executing the body of the rule.
 *
 * If a rule uses the couldFire method and canFire methods from this class, it is guaranteed not to violate the game
 * rules about when discards are legal (A card cannot be discarded when there are the maximum number information tokens
 * present in the game.
 */
public abstract class AbstractDiscardRule implements Rule {

    /**
     * Check that this rule is legal to execute.
     *
     * For AbstractDiscard, this means checking that there is at least 1 missing information token before querying the
     * rule.
     *
     * @param playerID the playerID of the agent making the decision
     * @param state the current state of the game
     * @return true if the rule should execute, false otherwise
     */
    @Override
    public boolean canFire(int playerID, GameState state) {
        //discard rules can never fire if there is full information
        if (state.getInfomation() == state.getStartingInfomation()) {
            return false;
        }

        Action action = execute(playerID, state);
        return action != null;
    }

    /**
     * Optimised method for checking if the rule will fire.
     *
     * For AbstractDiscard, this means checking that there is at least 1 missing information token before querying the
     * rule.
     *
     * This rule returning true does not mean that the rule can execute (canFire should be consulted in this case), but
     * returning false means it cannot execute. It exists to allow the rule based agent to make optimisations when
     * evaluating rules.
     *
     * @param playerID The player id that is this turn
     * @param state The state of the board
     * @return true if this rule <i>could</i> fire, false if it will not fire.
     */
    @Override
    public boolean couldFire(int playerID, GameState state) {
        return state.getInfomation() != state.getStartingInfomation();
    }
}
