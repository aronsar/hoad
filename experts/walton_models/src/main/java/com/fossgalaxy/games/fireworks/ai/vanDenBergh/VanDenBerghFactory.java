package com.fossgalaxy.games.fireworks.ai.vanDenBergh;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.rule.*;
import com.fossgalaxy.games.fireworks.ai.rule.random.*;
import com.fossgalaxy.games.fireworks.ai.rule.wrapper.IfRule;
import com.fossgalaxy.games.fireworks.annotations.AgentBuilderStatic;

/**
 * Created by piers on 09/12/16.
 */
public class VanDenBerghFactory {

    private VanDenBerghFactory() {

    }

    @AgentBuilderStatic("vdb-paper")
    public static Agent buildAgent() {
        return buildAgent(
                0.6,
                1.0,
                HintRules.NEXT_USEFUL_THEN_MOST_CARDS,
                DiscardRules.MOST_CERTAIN_IS_USELESS
        );
    }

    @AgentBuilderStatic("VanDenBergh")
    public static Agent buildAgent(double wp, double wd, HintRules hintRule, DiscardRules discardRule) {
        ProductionRuleAgent pra = new ProductionRuleAgent();

        // If there is a card in my hand of which I am "certain enough" that ic an be played, I play it
        pra.addRule(
                new IfRule(
                        (playerID, state) -> state.getLives() > 1,
                        new PlayProbablySafeCard(wp),
                        new PlaySafeCard()
                )
        );

        // Otherwise if there is a card in my hand of which I am "certain enough" that it is useless, I discard it
        pra.addRule(new DiscardProbablyUselessCard(wd));

        // Otherwise if there is a hint token available, I give a hint
        switch (hintRule) {
            case RANDOM:
                pra.addRule(new TellRandomly());
                break;
            case MOST_CARDS:
                pra.addRule(new TellMostInformation());
                break;
            case NEXT_USEFUL_THEN_MOST_CARDS:
                pra.addRule(new TellAnyoneAboutUsefulCard());
                pra.addRule(new TellMostInformation(true));
                break;
            case NEXT_USEFUL_THEN_NEXT_USELESS_THEN_MOST_CARDS:
                pra.addRule(new TellAnyoneAboutUsefulCard());
                pra.addRule(new TellAnyoneAboutUselessCard());
                pra.addRule(new TellMostInformation(true));
                break;
        }
        // Otherwise I discard a card
        switch (discardRule) {
            case RANDOM:
                pra.addRule(new DiscardRandomly());
                break;
            case LONGEST_HELD:
                pra.addRule(new DiscardOldestFirst());
                break;
            case LEAST_LIKELY_TO_BE_NECESSARY:
                pra.addRule(new DiscardLeastLikelyToBeNecessary());
                break;
            case MOST_CERTAIN_IS_USELESS:
                pra.addRule(new DiscardProbablyUselessCard(0.0));
                break;
        }
        return pra;
    }
}
