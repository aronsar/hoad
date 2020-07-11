package com.fossgalaxy.games.fireworks.ai.rule.wrapper;

import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;

import java.util.function.BiFunction;

/**
 * Created by piers on 07/12/16.
 */
public class IfRule implements Rule {

    private final BiFunction<Integer, GameState, Boolean> predicate;
    private final Rule success;
    private final Rule failure;


    public IfRule(BiFunction<Integer, GameState, Boolean> predicate, Rule success) {
        this(predicate, success, null);
    }

    public IfRule(BiFunction<Integer, GameState, Boolean> predicate, Rule success, Rule failure) {
        this.predicate = predicate;
        this.success = success;
        this.failure = failure;
    }

    @Override
    public boolean canFire(int playerID, GameState state) {
        if (predicate.apply(playerID, state)) {
            return success.canFire(playerID, state);
        } else {
            return failure != null && failure.canFire(playerID, state);
        }
    }

    @Override
    public Action execute(int playerID, GameState state) {
        if (predicate.apply(playerID, state)) {
            return success.execute(playerID, state);
        } else {
            if (failure != null) {
                return failure.execute(playerID, state);
            }
        }
        return null;
    }
}
