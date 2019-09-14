package com.fossgalaxy.games.fireworks.ai.iggi;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.mcts.MCTSPredictor;
import com.fossgalaxy.games.fireworks.ai.osawa.rules.OsawaDiscard;
import com.fossgalaxy.games.fireworks.ai.osawa.rules.TellPlayableCardOuter;
import com.fossgalaxy.games.fireworks.ai.rule.*;
import com.fossgalaxy.games.fireworks.ai.rule.random.DiscardRandomly;
import com.fossgalaxy.games.fireworks.ai.rule.random.PlayProbablySafeCard;
import com.fossgalaxy.games.fireworks.ai.rule.random.TellRandomly;
import com.fossgalaxy.games.fireworks.ai.rule.simple.DiscardIfCertain;
import com.fossgalaxy.games.fireworks.ai.rule.simple.PlayIfCertain;
import com.fossgalaxy.games.fireworks.ai.rule.wrapper.IfRule;
import com.fossgalaxy.games.fireworks.annotations.AgentBuilderStatic;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;

/**
 * Stratergies used/theorised about by IGGI students.
 */
public class IGGIFactory {

    private IGGIFactory() {

    }

    /**
     * This bot does nothing but tell and discard cards at random.
     *
     * This agent is always guaranteed to return a move, but will never play cards.
     *
     * @return the old default policy for the production rule agent.
     */
    @AgentBuilderStatic("forgiving")
    public static Agent buildForgivingPolicy(){
        ProductionRuleAgent pra = new ProductionRuleAgent();
        pra.addRule(new TellRandomly());
        pra.addRule(new DiscardRandomly());
        return pra;
    }

    /**
     * Cautious but helpful bot.
     * <p>
     * This policy will only play cards it is sure about, it will discard cards it knows are useless
     * before considering discarding randomly if it has no other choice.
     *
     * @return an agent implementing the appropriate strategy
     */
    @AgentBuilderStatic("cautious")
    public static Agent buildCautious() {
        ProductionRuleAgent pra = new ProductionRuleAgent();
        pra.addRule(new PlayIfCertain());
        pra.addRule(new PlaySafeCard());
        pra.addRule(new TellAnyoneAboutUsefulCard());
        pra.addRule(new OsawaDiscard());
        pra.addRule(new DiscardRandomly());

        pra.addRule(new TellRandomly());
        return pra;
    }

    @AgentBuilderStatic("iggi")
    public static Agent buildIGGIPlayer() {
        ProductionRuleAgent pra = new ProductionRuleAgent();
        pra.addRule(new PlayIfCertain());
        pra.addRule(new PlaySafeCard());
        pra.addRule(new TellAnyoneAboutUsefulCard());
        pra.addRule(new OsawaDiscard());

        pra.addRule(new DiscardOldestFirst());
        pra.addRule(new TellRandomly());

        return pra;
    }

    @AgentBuilderStatic("iggi2")
    public static Agent buildIGGI2Player() {
        ProductionRuleAgent pra = new ProductionRuleAgent();

        // Its yolo time
        pra.addRule(
                new IfRule(
                        (id, state) -> state.getLives() > 1 && !state.getDeck().hasCardsLeft(),
                        new PlayProbablySafeCard(0.0)
                )
        );

        pra.addRule(new PlayIfCertain());
        pra.addRule(new PlaySafeCard());

        pra.addRule(
                new IfRule(
                        (id, state) -> state.getLives() > 1,
                        new PlayProbablySafeCard(.6)
                )
        );


        pra.addRule(new OsawaDiscard());

        pra.addRule(new TellAnyoneAboutOldestUsefulCard());
        pra.addRule(new CompleteTellUsefulCard());
        //pra.addRule(new TellFives());

        pra.addRule(new DiscardOldestNoInfoFirst());
        pra.addRule(new DiscardOldestFirst());

        pra.addRule(new TellMostInformation(true));
        pra.addRule(new TellRandomly());

        return pra;
    }

    @AgentBuilderStatic("flawed")
    public static Agent buildFlawedPlayer(){
        ProductionRuleAgent pra = new ProductionRuleAgent();
        pra.addRule(new PlaySafeCard());
        pra.addRule(new PlayProbablySafeCard(0.25));

        pra.addRule(new TellRandomly());

        pra.addRule(new OsawaDiscard());
        pra.addRule(new DiscardOldestFirst());
        pra.addRule(new DiscardRandomly());

        return pra;
    }

    @AgentBuilderStatic("piers")
    public static Agent buildPiersPlayer() {
        ProductionRuleAgent pra = new ProductionRuleAgent();
        // Its yolo time
        pra.addRule(
                new IfRule(
                        (id, state) -> state.getLives() > 1 && !state.getDeck().hasCardsLeft(),
                        new PlayProbablySafeCard(0.0)
                )
        );
        pra.addRule(new PlaySafeCard());
        pra.addRule(
                new IfRule(
                        (id, state) -> state.getLives() > 1,
                        new PlayProbablySafeCard(.6)
                )
        );
        pra.addRule(new TellAnyoneAboutUsefulCard());
        pra.addRule(
                new IfRule(
                        (id, state) -> state.getInfomation() < 4,
                        new TellDispensable()
                )
        );
        pra.addRule(new OsawaDiscard());
        pra.addRule(new DiscardOldestFirst());
        pra.addRule(new TellRandomly());
        pra.addRule(new DiscardRandomly());
        return pra;
    }

    @AgentBuilderStatic("risky")
    public static Agent buildRiskyPlayer(double threshold) {
        ProductionRuleAgent pra = new ProductionRuleAgent();
        pra.addRule(new PlayIfCertain());
        pra.addRule(new PlayProbablySafeCard(threshold));
        pra.addRule(new TellAnyoneAboutUsefulCard());
        pra.addRule(new OsawaDiscard());
        pra.addRule(new DiscardOldestFirst());
        return pra;
    }

    public static Agent buildRiskyPlayer() {
        return buildRiskyPlayer(0.75);
    }

    public static Agent buildTest2() {
        ProductionRuleAgent pra = new ProductionRuleAgent();
        pra.addRule(new TellPlayableCardOuter());
        pra.addRule(new PlayIfCertain());
        pra.addRule(new DiscardIfCertain());
        return pra;
    }

    @AgentBuilderStatic("legal_random")
    public static Agent buildRandom() {
        ProductionRuleAgent pra = new ProductionRuleAgent();
        pra.addRule(new LegalRandom());
        return pra;
    }

    @AgentBuilderStatic("cautiousMCTS")
    public static Agent buildCautiousMCTS() {
        Agent[] agents = AgentUtils.buildPredictors(-1, 5, "cautious");
        return new MCTSPredictor(agents);
    }
}
