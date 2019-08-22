package com.fossgalaxy.games.fireworks.ai.rule.random;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractDiscardRule;
import com.fossgalaxy.games.fireworks.ai.rule.logic.DeckUtils;
import com.fossgalaxy.games.fireworks.ai.rule.logic.HandUtils;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;

import java.util.List;
import java.util.Map;

/**
 * Created by piers on 12/12/16.
 * <p>
 * CardComponent is useless if it cannot be played at all.
 * <p>
 * Probability that it is useless.
 * <p>
 * Can be useless because of a number of reasons
 * CardComponent.value &lt;= table[CardComponent.colour]
 * CardComponent.value &lt;= min(table)
 * CardComponent.value &gt; max(possible(CardComponent.colour))
 */
public class DiscardProbablyUselessCard extends AbstractDiscardRule {

    private final double threshold;

    public DiscardProbablyUselessCard() {
        this(0.75);
    }

    public DiscardProbablyUselessCard(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public Action execute(int playerID, GameState state) {
        Map<Integer, List<Card>> possibleCards = DeckUtils.bindBlindCard(playerID, state.getHand(playerID), state.getDeck().toList());

        double bestSoFar = threshold;
        int bestSlot = -1;
        for (Map.Entry<Integer, List<Card>> entry : possibleCards.entrySet()) {
            double probability = DeckUtils.getProbablity(entry.getValue(), x -> isUseless(x, state));
            if(probability >= bestSoFar){
                bestSlot = entry.getKey();
                bestSoFar = probability;
            }
        }
        if(bestSlot == -1) return null;
        return new DiscardCard(bestSlot);
    }

    private boolean isUseless(Card card, GameState state) {
        return HandUtils.isSafeToDiscard(state, card.colour, card.value);
    }

    @Override
    public String fancyName() {
        return super.fancyName() + " : " + threshold;
    }
}
