package com.fossgalaxy.games.fireworks.state.events;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;

public class CardPlayed extends GameEvent {
    private static final String CARD_FORMAT = "player %d played slot %s, it was a %s %d.";

    private final int playerId;
    private final int slotId;
    private final int value;
    private final CardColour colour;

    public CardPlayed(int playerId, int slotId, CardColour colour, int value, int turnNumber) {
        super(MessageType.CARD_PLAYED, turnNumber);
        this.playerId = playerId;
        this.slotId = slotId;
        this.colour = colour;
        this.value = value;
    }

    @Override
    public void apply(GameState state, int myPlayerID) {

        Card oldCard = new Card(value, colour);

        // figure out next number
        int nextValue = state.getTableValue(oldCard.colour) + 1;

        if (nextValue != oldCard.value) {
            state.setLives(state.getLives() - 1);
            state.addToDiscard(oldCard);
        } else {
            state.setTableValue(oldCard.colour, nextValue);

            // next value is 5, get free infomations
            if (nextValue == 5) {
                int currInfo = state.getInfomation();
                int maxInfo = state.getStartingInfomation();

                if (maxInfo != currInfo) {
                    state.setInformation(currInfo + 1);
                }
            }
        }

        //if this was us, we think it could still be in the deck.
        if (myPlayerID == playerId) {
            state.getDeck().remove(oldCard);
        }

        state.setCardAt(playerId, slotId, null);

    }

    public int getPlayerId(){
        return playerId;
    }

    public int getSlotId() {
        return slotId;
    }

    public int getValue(){
        return value;
    }

    public CardColour getColour() {
        return colour;
    }

    @Override
    public String toString() {
        return String.format(CARD_FORMAT, playerId, slotId, colour, value);
    }

}
