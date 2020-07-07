package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;

/**
 * Abstract class for writing tell rules.
 *
 * This method provides some utility methods and checks to ensure that the tell rule will not violate the game rules,
 * it will not allow the rule to fire if there is no information. It will not prevent the rule from returning an
 * illegal tell action though (telling ourselves about cards) - this is the responsibility of the rule writer.
 */
public abstract class AbstractTellRule extends AbstractRule {

    /**
     * Check if the rule can fire.
     *
     * This method checks that there is at least 1 information left before delegating to the parent class.
     *
     * @param playerID the current playerID
     * @param state the current game state
     * @return true if the rule can fire, false otherwise
     */
    @Override
    public boolean canFire(int playerID, GameState state) {
        if (state.getInfomation() == 0) {
            return false;
        }

        return super.canFire(playerID, state);
    }

    /**
     * Tell a missing piece of information, preferring to tell colour if not known over value.
     *
     * This will see what the player knows about a card and will tell one of the missing pieces of information,
     * if both the colour and value are unknown, tell the colour. If both the colour and value are known return a
     * null action.
     *
     * @param hand the player who is being told's hand
     * @param playerID the playerId of the player being told
     * @param slot the slot the card you wish to tell about is in
     * @return the tell action, or null of no tell action is needed
     */
    public Action tellMissingPrioritiseColour(Hand hand, int playerID, int slot) {
        Card card = hand.getCard(slot);
        if (hand.getKnownColour(slot) == null) {
            return new TellColour(playerID, card.colour);
        } else if (hand.getKnownValue(slot) == null) {
            return new TellValue(playerID, card.value);
        }

        return null;
    }

    /**
     * Tell a missing piece of information, preferring to tell value if not known over colour.
     *
     * This will see what the player knows about a card and will tell one of the missing pieces of information,
     * if both the colour and value are unknown, tell the value. If both the colour and value are known return a
     * null action.
     *
     * @param hand the player who is being told's hand
     * @param playerID the playerId of the player being told
     * @param slot the slot the card you wish to tell about is in
     * @return the tell action, or null of no tell action is needed
     */
    public Action tellMissingPrioritiseValue(Hand hand, int playerID, int slot){
        Card card = hand.getCard(slot);
        if(hand.getKnownValue(slot) == null){
            return new TellValue(playerID, card.value);
        }else if(hand.getKnownColour(slot) == null){
            return new TellColour(playerID, card.colour);
        }

        return null;
    }

    /**
     * Quick check - ensure that there is at least 1 piece of information.
     *
     * @param playerID The player id that is this turn
     * @param state The state of the board
     * @return false if this will definitely cannot fire, true otherwise.
     */
    @Override
    public boolean couldFire(int playerID, GameState state) {
        return state.getInfomation() != 0;
    }
}
