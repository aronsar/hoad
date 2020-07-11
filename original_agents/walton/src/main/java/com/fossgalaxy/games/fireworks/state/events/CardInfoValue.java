package com.fossgalaxy.games.fireworks.state.events;

import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;

import java.util.Arrays;
import java.util.Collection;

public class CardInfoValue extends CardInfo {
    private static final String CARD_FORMAT = "player %d has %d cards, in slot(s) %s.";

    private final int value;

    public CardInfoValue(int performer, int playerId, int value, Collection<Integer> slotsList, int turnNumber) {
        super(MessageType.CARD_INFO_VALUE, playerId, performer, slotsList, turnNumber);
        this.value = value;
    }

    public CardInfoValue(int performer, int playerId, int value, Integer... slots) {
        super(MessageType.CARD_INFO_VALUE, playerId, performer, slots);
        this.value = value;
    }

    @Override
    public void apply(GameState state, int myPlayerID) {
        Hand playerHand = state.getHand(playerTold);
        playerHand.setKnownValue(value, slots);
        state.setInformation(state.getInfomation() - 1);
    }

    @Override
    public String toString() {
        return String.format(CARD_FORMAT, playerTold, value, Arrays.toString(slots));
    }

    public int getValue() {
        return value;
    }

    public Integer[] getSlots() {
        return slots;
    }
}
