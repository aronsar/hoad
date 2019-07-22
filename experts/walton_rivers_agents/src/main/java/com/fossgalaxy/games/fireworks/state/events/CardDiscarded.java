package com.fossgalaxy.games.fireworks.state.events;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;

public class CardDiscarded extends GameEvent {
    private final static String CARD_FORMAT = "player %d discarded slot %s, it was a %s %d.";

    private final int playerId;
    private final int slotId;
    private final int value;
    private final CardColour colour;

    public CardDiscarded(int playerId, int slotId, CardColour colour, int value, int turnNumber) {
        super(MessageType.CARD_DISCARDED, turnNumber);
        this.playerId = playerId;
        this.slotId = slotId;
        this.colour = colour;
        this.value = value;
    }

    @Override
    public void apply(GameState state, int myPlayerId) {
        Card card = new Card(value, colour);

        state.addToDiscard(card);
        state.setCardAt(playerId, slotId, null);
        state.setInformation(state.getInfomation() + 1);

        //if this was my card, I think it is still in the deck, time to delete it
        if (myPlayerId == playerId) {
            state.getDeck().remove(card);
        }
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


    @Override
    public String toString() {
        return String.format(CARD_FORMAT, playerId, slotId, colour, value);
    }

}
