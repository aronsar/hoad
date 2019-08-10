package com.fossgalaxy.games.fireworks.state.events;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;

import java.util.*;

/**
 * Created by webpigeon on 28/11/16.
 */
public class CheatEvent extends GameEvent {
    private int playerID;
    private Hand hand;

    public CheatEvent(int playerID, Hand hand, int turnNumber) {
        super(MessageType.CHEAT, turnNumber);
        this.playerID = playerID;
        this.hand = Objects.requireNonNull(hand);
    }

    @Override
    public void apply(GameState state, int myPlayerID) {
        Hand hand = state.getHand(playerID);
        Hand serverHand = this.hand;

        Map<Integer, List<Integer>> values = new HashMap<>();
        Map<CardColour, List<Integer>> colours = new EnumMap<>(CardColour.class);

        //compute the colours for all cards in hand
        for (int i=0; i<hand.getSize(); i++) {
            Card card = serverHand.getCard(i);
            values.computeIfAbsent(card.value, ArrayList::new).add(i);
            colours.computeIfAbsent(card.colour, (CardColour c) -> new ArrayList()).add(i);
        }

        //report the values of every card in the hand
        for (Map.Entry<Integer, List<Integer>> entry : values.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            hand.setKnownValue(entry.getKey(), entry.getValue().toArray(new Integer[0]));
        }

        //report the colours of every card in the hand
        for (Map.Entry<CardColour, List<Integer>> entry : colours.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            hand.setKnownColour(entry.getKey(), entry.getValue().toArray(new Integer[0]));
        }
    }

    public int getPlayerID(){
        return playerID;
    }

    public Hand getHand(){
        return hand;
    }

    @Override
    public boolean isVisibleTo(int playerID) {
        return playerID == this.playerID;
    }

    @Override
    public String toString() {
        return String.format("Cheating: sending hand of %d", playerID);
    }
}
