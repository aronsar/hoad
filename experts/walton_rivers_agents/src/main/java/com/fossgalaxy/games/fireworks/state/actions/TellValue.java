package com.fossgalaxy.games.fireworks.state.actions;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.RulesViolation;
import com.fossgalaxy.games.fireworks.state.events.CardInfoValue;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TellValue implements Action {
    public final int player;
    public final int value;

    public TellValue(int player, int value) {
        this.player = player;
        this.value = value;
    }

    @Override
    public List<GameEvent> apply(int playerID, GameState game) {

        int turnNumber = game.getTurnNumber();

        Hand hand = game.getHand(player);
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < hand.getSize(); i++) {
            Card card = hand.getCard(i);
            if (card != null && value == card.value) {
                slots.add(i);
            }
        }

        if (playerID == player) {
            throw new RulesViolation("you cannot tell yourself things", this);
        }

        if (slots.isEmpty()) {
            throw new RulesViolation("you cannot tell a player about a lack of cards", this);
        }

        int information = game.getInfomation();
        if (information <= 0) {
            throw new RulesViolation("you have no information left", this);
        }

        game.setInformation(information - 1);
        hand.setKnownValue(value, slots.toArray(new Integer[slots.size()]));

        GameEvent cardInformation = new CardInfoValue(playerID, player, value, slots, turnNumber);

        //update state history
        List<GameEvent> effects = Collections.singletonList(cardInformation);
        game.addAction(playerID, this, effects);
        game.actionTick();

        return effects;
    }

    @Override
    public boolean isLegal(int playerId, GameState state) {
        return state.getInfomation() > 0 && state.getHand(player).hasValue(value) && player != playerId;
    }

    @Override
    public String toString() {
        return String.format("tell %d about their %ss", player, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TellValue tellValue = (TellValue) o;

        if (player != tellValue.player) return false;
        return value == tellValue.value;

    }

    @Override
    public int hashCode() {
        int result = player;
        result = 31 * result + value;
        return result;
    }
}
