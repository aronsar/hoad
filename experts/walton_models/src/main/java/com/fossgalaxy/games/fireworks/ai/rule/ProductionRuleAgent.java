package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * An agent capable of applying a set of @link{Rule} rules.
 */
public class ProductionRuleAgent implements Agent {
    private final Logger logger = LoggerFactory.getLogger(ProductionRuleAgent.class);

    protected List<Rule> rules;
    protected Agent defaultPolicy;

    public ProductionRuleAgent() {
        this.rules = new ArrayList<>();
        this.defaultPolicy = null;
    }

    public void addRule(Rule rule) {
        rules.add(rule);
    }

    /**
     * Policy to delegate to if no rule fired.
     *
     * If this is set to null, the agent will throw an illegal state exception if the ruleset is incomplete.
     * If set, then this policy will be called when no rules fired. If you wish to use the old 'forgiving' behaviour,
     * set this to IGGIFactory.buildForgivingPolicy().
     *
     * If you call this method, you must guarantee the policy used is complete.
     *
     * @param policy the policy of last resort for the agent.
     */
    public void setDefaultPolicy(Agent policy){
        this.defaultPolicy = policy;
    }

    @Override
    public Action doMove(int agentID, GameState state) {

        for (Rule rule : rules) {
            if (rule.canFire(agentID, state)) {
                Action selected = rule.execute(agentID, state);
                if (selected == null) {
                    logger.warn("rule "+rule+" reported it could fire, but then did not.");
                    continue;
                }

                return selected;
            }
        }

        return doDefaultBehaviour(agentID, state);
    }

    //default rule based behaviour, discard random if legal, else play random
    public Action doDefaultBehaviour(int playerID, GameState state) {
        if (defaultPolicy == null) {
            throw new IllegalStateException("No rule fired - your rules are incomplete.");
        } else {
            Action defaultAction = defaultPolicy.doMove(playerID, state);

            if (defaultAction == null) {
                //hey! that's not allowed!
                throw new IllegalStateException("Default policy failed to return an action.");
            }

            return defaultAction;
        }
    }

    @Override
    public String toString() {
        return "ProductionRuleAgent";
    }

    public List<Rule> getRules() {
        return rules;
    }
}
