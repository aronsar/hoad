package com.fossgalaxy.games.fireworks.state.actions;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.RulesViolation;
import com.fossgalaxy.games.fireworks.state.events.CardDrawn;
import com.fossgalaxy.games.fireworks.state.events.CardPlayed;
import com.fossgalaxy.games.fireworks.state.events.CardReceived;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;

import java.util.ArrayList;
import java.util.List;

public class PlayCard implements Action {
    public final int slot;

    public PlayCard(int slot) {
        this.slot = slot;
    }

    @Override
    public List<GameEvent> apply(int playerID, GameState game) {
        if (!isLegal(playerID, game)) {
            throw new RulesViolation("this is a violation of the game rules!", this);
        }

        // deal with the old card first
        Card oldCard = game.getCardAt(playerID, slot);
        assert oldCard != null : "old card was unknown or did not exist";

        // figure out the next value
        int nextValue = game.getTableValue(oldCard.colour) + 1;
        int nextTurn = game.getTurnNumber() + 1;

        // check if the card was valid
        if (nextValue == oldCard.value) {
            game.setTableValue(oldCard.colour, nextValue);

            // if you complete a firework, you get an information back
            if (nextValue == 5) {
                int currentInfo = game.getInfomation();
                int maxInfo = game.getStartingInfomation();
                if (currentInfo < maxInfo) {
                    game.setInformation(currentInfo + 1);
                }
            }

        } else {
            // if this card wasn't valid, discard it and lose a life.
            game.addToDiscard(oldCard);
            game.setLives(game.getLives() - 1);
        }

        ArrayList<GameEvent> events = new ArrayList<>();
        events.add(new CardPlayed(playerID, slot, oldCard.colour, oldCard.value, nextTurn));
        events.add(new CardReceived(playerID, slot, game.getDeck().hasCardsLeft(), nextTurn));

        // deal with the new card
        // XXX null pointer exception if next card was null.
        if (game.getDeck().hasCardsLeft()) {
            Card newCard = game.drawFromDeck();
            game.setCardAt(playerID, slot, newCard);
            //game.getHand(playerID).setHasCard(slot, true);
            events.add(new CardDrawn(playerID, slot, newCard.colour, newCard.value, nextTurn));
        } else {
            game.setCardAt(playerID, slot, null);
            //game.getHand(playerID).setHasCard(slot, false);
        }

        //update state history
        game.addAction(playerID, this, events);
        game.actionTick();

        return events;
    }

    @Override
    public boolean isLegal(int playerID, GameState state) {
        Card card = state.getHand(playerID).getCard(slot);
        return card != null;
    }

    @Override
    public String toString() {
        return String.format("play %d", slot);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlayCard playCard = (PlayCard) o;

        return slot == playCard.slot;

    }

    @Override
    public int hashCode() {
        return slot;
    }
}
