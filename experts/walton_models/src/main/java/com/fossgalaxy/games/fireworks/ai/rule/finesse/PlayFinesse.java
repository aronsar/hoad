package com.fossgalaxy.games.fireworks.ai.rule.finesse;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractRule;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.TimedHand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;
import com.fossgalaxy.games.fireworks.state.events.CardInfo;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;

import java.util.LinkedList;

/**
 * Created by webpigeon on 11/05/17.
 *
 * Figure out if we are being indicated to play a card via a fineese action by the player before
 *
 */
public class PlayFinesse extends AbstractRule {

    @Override
    public boolean canFire(int playerID, GameState state) {
        if(state.getPlayerCount() == 2) return false;
        return super.canFire(playerID, state);
    }

    @Override
    public Action execute(int playerID, GameState state) {
        LinkedList<GameEvent> eventHistory = state.getHistory();
        GameEvent lastEvent = eventHistory.getLast();

        if (! (lastEvent instanceof CardInfo) ) {
            return null;
        }

        CardInfo infoValue = (CardInfo) lastEvent;
        int playerTold = infoValue.getPlayerTold();
        Integer[] playerSlots = infoValue.getSlots();

        // this was not a finesse tell
        if (!infoValue.wasToldTo(selectPlayer(playerID, state))) {
            return null;
        }

        // is one of the cards indicated 1 card away from being playable
        Hand playerHand = state.getHand(playerTold);
        for (int slot : playerSlots) {
            if(!playerHand.hasCard(slot)) continue;
            Card card = playerHand.getCard(slot);

            //find out if someone is trying to finesse this card
            int currValue = state.getTableValue(card.colour);
            if (currValue +2 != card.value) {
                continue;
            }

            //get the newest card (or fail or timed hands are not permitted)
            int newestCard = getNewestCard(state, playerID);
            if (newestCard == -1) {
                return null;
            }

            //it was a finesse move, we execute the play.
            return new PlayCard(getNewestCard(state, playerID));
        }

        return null;
    }

    /**
     * This method will return -1 if someone doesn't want us to know what order our cards were delt in.
     *
     * @param state the current game state
     * @param playerID the current playerID
     * @return -1 if the newest card is not known, else the slot containing the newest card
     */
    public static int getNewestCard(GameState state, int playerID) {
        try {
            TimedHand hand = (TimedHand) state.getHand(playerID);
            return hand.getNewestSlot();
        } catch (ClassCastException ex) {
            return -1;
        }
    }

}
