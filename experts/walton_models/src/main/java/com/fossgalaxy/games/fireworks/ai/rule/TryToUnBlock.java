package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.ai.rule.logic.HandUtils;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;

/**
 * Created by piers on 04/05/17.
 *
 *  I think I can build something pretty simple and very good by combining that paper's thoughts with my own existing ones. I'll confine myself to:
• Four players
• Include multicoloured cards (treating them as being every colour)
• Null clues are permitted

Each person has 30 clues available to them (5 ranks and 5 colours to each of 3 other players). Label these so each is the composition of a rank clue modulo 5 and a colour clue modulo 6.

Every player's hand contains a mixture of identified and unidentified cards. Whenever a player gives a clue, they identify each other player's newest unidentified card (omitting players with no unidentified cards). Sum all the identified ranks modulo 5, sum all the identified colours modulo 6, give the corresponding clue.

Now just add some heuristics. Off the top of my head...

Define a card as "playable", or "dead" = a duplicate of something already played, "safe" = an unplayable duplicate of something not yet discarded, "unsafe" = an unplayable card that isn't safe.

Define a player as "blocked" if none of their identified cards is playable, dead or safe, and there are no cluestones available.

Define a player as "ill-informed" if no identified card is playable or dead, but at least one unidentified card is.

Define a player as "unblocking" if they know of at least one playable 5 or safe card.

Then do the first of these which is possible:
• If there is no unblocking player between you and a blocked one, unblock
• Play
• If any other player is ill-informed, clue
• Discard a dead card
• If any other player has unidentified cards, clue
• Discard a safe card
• Give a dummy clue
• Discard an unidentified card (risking an imperfect score)
• Discard the highest-ranked unsafe card (guaranteeing an imperfect score)

 *
 * How complex? Is Player 2 able to play if it relies on Player 1 playing a card that they know about?
 */
public class TryToUnBlock extends AbstractRule {

    @Override
    public Action execute(int playerID, GameState state) {

        // Need to track this
        int information = state.getInfomation();

        //if there is information, no one can be considered blocked.
        if (information > 0) {
            return null;
        }

        Action unblockingAction = isUnblocking(state, playerID);
        if (unblockingAction == null) {
            return null;
        }

        for (int i = 0; i < state.getPlayerCount(); i++) {
            int lookingAt = (playerID + i) % state.getPlayerCount();
            if (lookingAt == playerID) {
                continue;
            }

            if (isBlocked(state, lookingAt)) {
                return unblockingAction;
            }

            if (isUnblocking(state, lookingAt) != null) {
                return null;
            }
            // Is this player blocking
        }

        return null;
    }

    /**
     * Can a player cause an information token to become available during their turn?
     *
     * @param state the game state to check against
     * @param playerID the player to check
     * @return the move we can make to give this player a move.
     */
    public Action isUnblocking(GameState state, int playerID) {
        Hand hand = state.getHand(playerID);

        for (int slot = 0; slot < hand.getSize(); slot++) {
            if (!hand.hasCard(slot)) {
                continue;
            }

            if (HandUtils.isSafeToDiscard(state, hand.getKnownColour(slot), hand.getKnownValue(slot))) {
                return new DiscardCard(slot);
            }

            if (hand.getKnownValue(slot) != null && hand.getKnownValue(slot) == 5) {
                if (hand.getKnownColour(slot) != null) {
                    if (state.getTableValue(hand.getKnownColour(slot)) == 4) {
                        return new PlayCard(slot);
                    }
                }
            }
        }

        return null;
    }

    /**
     * A blocked player is a player who has no safe play or discard move and has no information tokens to tell.
     *
     * @param state the state to check against
     * @param playerID the playerID to check against
     * @return true if the player is blocked, false otherwise
     */
    public boolean isBlocked(GameState state, int playerID) {
        Hand hand = state.getHand(playerID);

        if (state.getInfomation() >= 1) {
            return false;
        }

        for (int slot = 0; slot < hand.getSize(); slot++) {
            Integer value = hand.getKnownValue(slot);
            CardColour colour = hand.getKnownColour(slot);

            if ( state.getInfomation() != state.getStartingInfomation() && HandUtils.isSafeToDiscard(state, colour, value) ) {
                return false;
            }

            // Return false if this card is either playable or discardable
            if (colour != null && value != null) {
                int tableValue = state.getTableValue(colour);
                if (tableValue+1 == value) {
                    return false;
                }
            }

        }

        return true;
    }

    public static boolean isUsableCard(GameState state, CardColour colour, Integer value) {
        if ( state.getInfomation() != state.getStartingInfomation() && HandUtils.isSafeToDiscard(state, colour, value) ) {
            return true;
        }

        // Return false if this card is either playable or discardable
        if (colour != null && value != null) {
            int tableValue = state.getTableValue(colour);
            if (tableValue+1 == value) {
                return true;
            }
        }

        return false;
    }
}
