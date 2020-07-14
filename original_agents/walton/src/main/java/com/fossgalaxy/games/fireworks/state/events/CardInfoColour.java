package com.fossgalaxy.games.fireworks.state.events;

import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;

import java.util.Arrays;
import java.util.Collection;

public class CardInfoColour extends CardInfo {
    private static final String CARD_FORMAT = "player %d has %s cards, in slot(s) %s.";

    private final CardColour colour;

    public CardInfoColour(int performer, int playerId, CardColour colour, Collection<Integer> slotsList, int turnNumber) {
        super(MessageType.CARD_INFO_COLOUR, playerId, performer, slotsList, turnNumber);
        this.colour = colour;
    }

    public CardInfoColour(int performer, int playerId, CardColour colour, Integer... slots) {
        super(MessageType.CARD_INFO_COLOUR, playerId, performer, slots);
        this.colour = colour;
    }

    @Override
    public void apply(GameState state, int myPlayerID) {
        assert state.getInfomation() > 0 : "got told information with no information left?!";

        Hand playerHand = state.getHand(playerTold);
        playerHand.setKnownColour(colour, slots);
        state.setInformation(state.getInfomation() - 1);

        assert state.getInfomation() >= 0 : "negative infomation happend?!";
    }

    @Override
    public String toString() {
        return String.format(CARD_FORMAT, playerTold, colour, Arrays.toString(slots));
    }

    public CardColour getColour() {
        return colour;
    }

    public Integer[] getSlots() {
        return slots;
    }
}
