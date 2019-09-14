package com.fossgalaxy.games.fireworks.state.events;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.Deck;
import com.fossgalaxy.games.fireworks.state.GameState;

public class CardDrawn extends GameEvent {
    private final int playerId;
    private final int slotId;
    private final int cardValue;
    private final CardColour colour;

    public CardDrawn(int playerId, int slotId, CardColour colour, int cardValue, int turnNumber) {
        super(MessageType.CARD_DRAWN, turnNumber);
        this.playerId = playerId;
        this.slotId = slotId;
        this.cardValue = cardValue;
        this.colour = colour;
    }

    @Override
    public void apply(GameState state, int myPlayerID) {
        Card card = new Card(cardValue, colour);

        state.setCardAt(playerId, slotId, card);

        Deck deck = state.getDeck();
        deck.remove(card);
    }

    public int getPlayerId(){
        return playerId;
    }

    public int getSlotId(){
        return slotId;
    }

    public int getCardValue(){
        return cardValue;
    }

    public CardColour getCardColour(){
        return colour;
    }

    @Override
    public boolean isVisibleTo(int playerID) {
        return playerID != this.playerId;
    }

    @Override
    public String toString() {
        return String.format("player %s draw card %s %d in slot %d", playerId, colour, cardValue, slotId);
    }

}
