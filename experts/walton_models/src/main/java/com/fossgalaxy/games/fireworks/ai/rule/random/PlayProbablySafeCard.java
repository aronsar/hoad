package com.fossgalaxy.games.fireworks.ai.rule.random;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractRule;
import com.fossgalaxy.games.fireworks.ai.rule.logic.DeckUtils;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;

import java.util.List;
import java.util.Map;

/**
 * Created by piers on 08/11/16.
 */
public class PlayProbablySafeCard extends AbstractRule {

    private final double threshold;

    public PlayProbablySafeCard() {
        this.threshold = 0.95;
    }

    public PlayProbablySafeCard(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public Action execute(int playerID, GameState state) {
        Map<Integer, List<Card>> possibleCards = DeckUtils.bindBlindCard(playerID, state.getHand(playerID), state.getDeck().toList());

        double bestSoFar = threshold;
        int bestSlot = -1;
        for (Map.Entry<Integer, List<Card>> entry : possibleCards.entrySet()) {
            double probability = DeckUtils.getProbablity(entry.getValue(), x -> isPlayable(x, state));
            if (probability >= bestSoFar) {
                bestSlot = entry.getKey();
                bestSoFar = probability;
            }
        }
        if (bestSlot == -1) return null;
        return new PlayCard(bestSlot);
    }

    public boolean isPlayable(Card card, GameState state) {
        return state.getTableValue(card.colour) + 1 == card.value;
    }

    @Override
    public String toString() {
        return super.toString() + " : Threshold: " + threshold;
    }

    public double getThreshold() {
        return threshold;
    }

    @Override
    public String fancyName() {
        return super.fancyName() + " : " + threshold;
    }
}
