package com.fossgalaxy.games.fireworks.state;

import com.fossgalaxy.games.fireworks.state.actions.Action;

/**
 * Exception that is triggered when a player does something bad.
 */
public class RulesViolation extends RuntimeException {
    private final Action action;

    public RulesViolation(Action action) {
        super("RuleViolation: " + action);
        this.action = action;
    }

    public RulesViolation(String msg, Action action) {
        super("RuleViolation: " + msg + " " + action);
        this.action = action;
    }
}
