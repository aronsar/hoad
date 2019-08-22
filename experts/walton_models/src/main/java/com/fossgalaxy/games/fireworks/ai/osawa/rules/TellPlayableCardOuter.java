package com.fossgalaxy.games.fireworks.ai.osawa.rules;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractTellRule;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;

/**
 * Tell the next player about a playable card in their hand.
 * <p>
 * This rule makes use of information the player already has, avoiding telling duplicate information.
 */
public class TellPlayableCardOuter extends AbstractTellRule {

    @Override
    public Action execute(int playerID, GameState state) {

        int nextPlayer = selectPlayer(playerID, state);
        Hand hand = state.getHand(nextPlayer);

        for (int slot = 0; slot < state.getHandSize(); slot++) {

            Card card = hand.getCard(slot);
            if (card == null) {
                continue;
            }

            int currTable = state.getTableValue(card.colour);
            if (card.value != currTable + 1) {
                continue;
            }

            if (hand.getKnownValue(slot) == null) {
                return new TellValue(nextPlayer, card.value);
            } else if (hand.getKnownColour(slot) == null) {
                return new TellColour(nextPlayer, card.colour);
            }
        }

        return null;
    }

}
