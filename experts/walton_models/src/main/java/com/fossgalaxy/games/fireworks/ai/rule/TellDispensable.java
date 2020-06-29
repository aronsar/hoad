package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.ai.rule.logic.HandUtils;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;

/**
 * Tell an agent about a card that can be safely chucked away.
 *
 * Created by piers on 01/12/16.
 */
public class TellDispensable extends AbstractTellRule {

    @Override
    public Action execute(int playerID, GameState state) {
        // need to inform all players of their hands
        for (int player = 0; player < state.getPlayerCount(); player++) {
            if (player == playerID) continue;
            Hand hand = state.getHand(player);
            for (int slot = 0; slot < state.getHandSize(); slot++) {
                Card actualCard = hand.getCard(slot);
                if (actualCard == null) continue;
                CardColour knownColour = hand.getKnownColour(slot);
                CardColour actualColour = actualCard.colour;
                if (knownColour == null) {
                    if (HandUtils.isSafeBecauseFiveAlreadyPlayed(state, actualColour)) {
                        return new TellColour(player, actualColour);
                    }
                }
                Integer knownValue = hand.getKnownValue(slot);
                Integer actualValue = actualCard.value;
                if (knownValue == null) {
                    if (HandUtils.isSafeBecauseValueLowerThanMinOnTable(state, actualValue)) {
                        return new TellValue(player, actualValue);
                    }
                }
                if (knownColour == null ^ knownValue == null) {
                    if (HandUtils.isSafeBecauseValueLowerThanPlayed(state, actualColour, actualValue)) {
                        return (knownColour == null) ? new TellColour(player, actualColour) : new TellValue(player, actualValue);
                    }
                }
            }
        }
        return null;
    }
}
