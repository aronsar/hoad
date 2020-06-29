package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.ai.rule.logic.DeckUtils;
import com.fossgalaxy.games.fireworks.ai.rule.logic.HandUtils;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;

import java.util.List;
import java.util.Map;

/**
 * This rule is damage control - we can't get a perfect score because we have discarded a prerequisite card, now we
 * know it's safe to discard higher value cards of the same suit.
 */
public class DiscardUselessCard extends AbstractDiscardRule {

    @Override
    public Action execute(int playerID, GameState state) {

        Hand myHand = state.getHand(playerID);
        Map<Integer, List<Card>> possibleCards = null;
        for (int slot = 0; slot < myHand.getSize(); slot++) {
            if ( !myHand.hasCard(slot) ) {
                continue;
            }

            CardColour c = myHand.getKnownColour(slot);
            if (c == null) {
                continue;
            }

            int highestPossible = HandUtils.getHighestScorePossible(state, c);
            Integer knownValue = myHand.getKnownValue(slot);
            if (HandUtils.isSafeToDiscardHigherThanPossible(state, c, knownValue)) {
                return new DiscardCard(slot);
            }

            if (possibleCards == null) {
                possibleCards = DeckUtils.bindBlindCard(playerID, myHand, state.getDeck().toList());
            }

            if (possibleCards.containsKey(slot) && !possibleCards.get(slot).isEmpty()) {
                int minimum = possibleCards.get(slot).stream().mapToInt(x -> x.value).min().getAsInt();
                if (minimum > highestPossible) {
                    return new DiscardCard(slot);
                }
            }
        }

        return null;
    }

}
