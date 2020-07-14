package com.fossgalaxy.games.fireworks.ai.rule.wrapper;

import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;

/**
 * Created by piers on 04/05/17.
 */
public class ConcatRule implements Rule{

    private final Rule left;
    private final Rule right;

    public ConcatRule(Rule left, Rule right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean canFire(int playerID, GameState state) {
        return left.canFire(playerID, state) || right.canFire(playerID, state);
    }

    @Override
    public Action execute(int playerID, GameState state) {
        if(!left.couldFire(playerID, state)){
            return right.execute(playerID, state);
        }
        Action leftAction = left.execute(playerID, state);
        return (leftAction == null) ? right.execute(playerID, state) : leftAction;
    }
}
