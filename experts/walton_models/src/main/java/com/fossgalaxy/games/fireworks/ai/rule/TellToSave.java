package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.ai.rule.logic.HandUtils;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;

import java.util.Collection;

/**
 * Tell a player about a card that is useful and unique even if it is not immediately playable.
 *
 * This rule is designed to stop another agent throwing away a card that is critical to a perfect score without them
 * knowing what it is.
 */
public class TellToSave extends AbstractTellRule {
    static final int[] COPIES = {0, 3, 2, 2, 2, 1};

    @Override
    public Action execute(int playerID, GameState state) {

        Collection<Card> discards = state.getDiscards();

        for (int i = 0; i < state.getPlayerCount(); i++) {
            int nextPlayerID = this.selectPlayer(playerID + i, state);
            if (nextPlayerID == playerID) continue;
            Hand hand = state.getHand(nextPlayerID);
            // Find unique card in hand
            for (int slot = 0; slot < hand.getSize(); slot++) {
                Card card = hand.getCard(slot);
                if (card == null) continue;

//                System.out.println("Player: " + nextPlayerID + " Has Card: " + card);
                if (HandUtils.isSafeToDiscard(state, card.colour, card.value)) {
//                    System.out.println("Card is useless: " + card);
                    continue;
                }
                long duplicatesDiscarded = discards.stream().filter((x) -> x.equals(card)).count();
                if (duplicatesDiscarded == COPIES[card.value] - 1) {
                    // Save it
//                    System.out.println("Last of its kind: " + card);
                    Action action = tellMissingPrioritiseValue(hand, nextPlayerID, slot);
                    if(action != null) return action;
                }
            }
        }
        return null;
    }
}
