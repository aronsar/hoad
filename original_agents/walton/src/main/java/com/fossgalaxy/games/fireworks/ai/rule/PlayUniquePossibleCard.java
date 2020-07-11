package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;
import com.fossgalaxy.games.fireworks.state.events.*;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * I have been involved in a finesse in the workplace and may be entitled to compensation.
 */
public class PlayUniquePossibleCard extends AbstractRule {

    @Override
    public Action execute(int playerID, GameState state) {

        LinkedList<GameEvent> history = state.getHistory();
        ListIterator<GameEvent> iterator = history.listIterator(history.size());

        Hand hand = state.getHand(playerID);

        boolean[] invalidated = new boolean[hand.getSize()];
        while (iterator.hasPrevious()) {
            GameEvent event = iterator.previous();


            if (event.getEvent() == MessageType.CARD_INFO_COLOUR) {
                CardInfoColour colourEvent = (CardInfoColour)event;
                if (colourEvent.wasToldTo(playerID) && colourEvent.getSlots().length == 1) {
                    int slot = colourEvent.getSlots()[0];
                    if (!invalidated[slot]) {
                        int nextValue = state.getTableValue(colourEvent.getColour());
                        if (state.getDeck().toList().contains(new Card(nextValue, colourEvent.getColour()))) {
                            return new PlayCard(slot);
                        }
                    }
                }
            }

            if (event.getEvent() == MessageType.CARD_INFO_VALUE) {
                CardInfoValue colourEvent = (CardInfoValue) event;
                if (colourEvent.wasToldTo(playerID) && colourEvent.getSlots().length == 1) {
                    for (CardColour colour : CardColour.values()) {
                        int onTable = state.getTableValue(colour);
                        if (onTable == colourEvent.getValue()-1) {
                            if (state.getDeck().toList().contains(new Card(colourEvent.getValue(), colour))) {
                                return new PlayCard(colourEvent.getSlots()[0]);
                            }
                        }
                    }
                }
            }

            if (event.getEvent() == MessageType.CARD_RECEIVED) {
                CardReceived drawn = (CardReceived)event;
                if (drawn.getPlayerId() == playerID && drawn.isReceived()) {
                    invalidated[drawn.getSlotId()] = true;

                    boolean allInvalidated = true;
                    for (boolean value : invalidated) {
                        if (!value) {
                            allInvalidated = false;
                            break;
                        }
                    }

                    if (allInvalidated) {
                        return null;
                    }
                }
            }

        }

        return null;
    }

}
