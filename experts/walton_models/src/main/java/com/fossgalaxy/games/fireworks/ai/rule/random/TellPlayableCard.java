package com.fossgalaxy.games.fireworks.ai.rule.random;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractTellRule;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;

import java.util.Random;

/**
 * Tell the next player about a playable card in their hand.
 * <p>
 * This does not make use of any information about what the other player has been told in
 * the past and non-determisticlly selects between telling the colour and the number.
 */
public class TellPlayableCard extends AbstractTellRule {
    private Random random;

    public TellPlayableCard() {
        this.random = new Random();
    }

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

            if (random.nextBoolean()) {
                return new TellValue(nextPlayer, card.value);
            } else {
                return new TellColour(nextPlayer, card.colour);
            }
        }

        return null;
    }

}
