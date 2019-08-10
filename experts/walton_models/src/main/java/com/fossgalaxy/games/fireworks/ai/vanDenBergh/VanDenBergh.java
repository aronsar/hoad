package com.fossgalaxy.games.fireworks.ai.vanDenBergh;

import com.fossgalaxy.games.fireworks.ai.rule.*;
import com.fossgalaxy.games.fireworks.ai.rule.random.*;
import com.fossgalaxy.games.fireworks.ai.rule.wrapper.IfRule;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;


/**
 * Created by piers on 07/04/17.
 */
public class VanDenBergh extends ProductionRuleAgent {

    @AgentConstructor("vandenbergh")
    public VanDenBergh(double wp, double wd, HintRules hintRule, DiscardRules discardRule){
        this.addRule(
                new IfRule(
                        (playerID, state) -> state.getLives() > 1,
                        new PlayProbablySafeCard(wp),
                        new PlaySafeCard()
                )
        );

        // Otherwise if there is a card in my hand of which I am "certain enough" that it is useless, I discard it
        this.addRule(new DiscardProbablyUselessCard(wd));

        // Otherwise if there is a hint token available, I give a hint
        switch (hintRule) {
            case RANDOM:
                this.addRule(new TellRandomly());
                break;
            case MOST_CARDS:
                this.addRule(new TellMostInformation());
                break;
            case NEXT_USEFUL_THEN_MOST_CARDS:
                this.addRule(new TellAnyoneAboutUsefulCard());
                this.addRule(new TellMostInformation(true));
                break;
            case NEXT_USEFUL_THEN_NEXT_USELESS_THEN_MOST_CARDS:
                this.addRule(new TellAnyoneAboutUsefulCard());
                this.addRule(new TellAnyoneAboutUselessCard());
                this.addRule(new TellMostInformation(true));
                break;
        }
        // Otherwise I discard a card
        switch (discardRule) {
            case RANDOM:
                this.addRule(new DiscardRandomly());
                break;
            case LONGEST_HELD:
                this.addRule(new DiscardOldestFirst());
                break;
            case LEAST_LIKELY_TO_BE_NECESSARY:
                this.addRule(new DiscardLeastLikelyToBeNecessary());
                break;
            case MOST_CERTAIN_IS_USELESS:
                this.addRule(new DiscardProbablyUselessCard(0.0));
                break;
        }
    }
}
