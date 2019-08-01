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
 *
 * Returns the slot that is least likely to be necessary.
 *
 * Necessary is defined as a card that can't be thrown away due to it being the last
 * of its kind, and could still be played in the future.
 */
public class DiscardLeastLikelyToBeNecessary extends AbstractDiscardRule {

    private static int[] cardCopies = new int[]{-1, 3, 2, 2, 2, 1};

    @Override
    public Action execute(int playerID, GameState state) {
        Map<Integer, List<Card>> possibleCards = DeckUtils.bindBlindCard(playerID, state.getHand(playerID), state.getDeck().toList());

        double bestSoFar = 1.0;
        int bestSlot = -1;
        for (Map.Entry<Integer, List<Card>> entry : possibleCards.entrySet()) {
            double probability = DeckUtils.getProbablity(entry.getValue(), x -> isNecessary(x, state));
            if (probability <= bestSoFar) {
                bestSlot = entry.getKey();
                bestSoFar = probability;
            }
        }
        if (bestSlot == -1) {
            return null;
        }

        return new DiscardCard(bestSlot);
    }

    // Is necessary if remaining copies of this card
    private boolean isNecessary(Card card, GameState state) {
        // Can't be necessary if needed again.
        if (HandUtils.isSafeToDiscard(state, card.colour, card.value)) {
            return false;
        }

        // Quicker to process this one.
        if (card.value == 5) return true;

        // Is it last one?
        // Is a card necessary if someone else has it?
//        long count = state.getDeck().toList().stream().filter(x -> x.equals(card)).count();
        long discardCount = state.getDiscards().stream().filter(x -> x.equals(card)).count();
//        return count == 0;
        return discardCount == cardCopies[card.value] - 1;
    }
}
