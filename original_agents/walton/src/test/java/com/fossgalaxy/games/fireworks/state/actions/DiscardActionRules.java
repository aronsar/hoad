package com.fossgalaxy.games.fireworks.state.actions;

import static org.junit.Assert.assertEquals;

import com.fossgalaxy.games.fireworks.state.*;
import org.junit.Test;

/**
 * High level game-play tests for play action.
 */
public class DiscardActionRules {

    @Test
    public void testDiscardCardEmptyDeck() {
        int slot = 0;
        int player = 0;

        CardColour colour = CardColour.BLUE;

        // setup
        GameState state = new BasicState(2, 5);
        state.setCardAt(player, slot, new Card(4, colour));
        state.setInformation(1);

        // checks for invariants
        int lives = state.getLives();
        int infomation = state.getInfomation();

        // check that the table is setup for that colour
        assertEquals(0, state.getTableValue(colour));

        // play the card
        Action discard = new DiscardCard(slot);
        discard.apply(player, state);

        // check the result is as expected
        assertEquals(0, state.getTableValue(colour));
        assertEquals(lives, state.getLives());
        assertEquals(infomation + 1, state.getInfomation());
        assertEquals(null, state.getCardAt(player, slot));
    }

    @Test
    public void testDiscardCardValid() {

        int slot = 0;
        int player = 0;

        CardColour colour = CardColour.BLUE;

        Card nextCard = new Card(5, CardColour.GREEN);

        // setup
        GameState state = new BasicState(2, 5);
        state.setCardAt(player, slot, new Card(1, colour));
        Deck deck = state.getDeck();
        deck.add(nextCard);

        state.setInformation(1);

        // checks for invariants
        int lives = state.getLives();
        int infomation = state.getInfomation();

        // check that the table is setup for that colour
        assertEquals(0, state.getTableValue(colour));

        // play the card
        Action discard = new DiscardCard(slot);
        discard.apply(player, state);

        // check the result is as expected
        assertEquals(0, state.getTableValue(colour));
        assertEquals(lives, state.getLives());
        assertEquals(infomation + 1, state.getInfomation());
        assertEquals(nextCard, state.getCardAt(player, slot));
    }

    @Test
    public void testDiscardHasFullInfomationIsinvalid() {
        int slot = 0;
        int player = 0;
        CardColour colour = CardColour.BLUE;

        GameState state = new BasicState(2, 5);
        state.setCardAt(player, slot, new Card(4, colour));

        Action discard = new DiscardCard(slot);

        assertEquals(false, discard.isLegal(player, state));
    }

    @Test(expected = RulesViolation.class)
    public void testDiscardInvalidAndApply(){
        int slot = 0;
        int player = 0;
        CardColour colour = CardColour.BLUE;

        GameState state = new BasicState(2, 5);
        state.setCardAt(player, slot, new Card(4, colour));

        Action discard = new DiscardCard(slot);
        discard.apply(player, state);
    }
    @Test
    public void testDiscardIsNullInvalid() {
        int slot = 0;
        int player = 0;

        GameState state = new BasicState(2, 5);
        state.setCardAt(player, slot, null);

        Action discard = new DiscardCard(slot);

        assertEquals(false, discard.isLegal(player, state));
    }

    @Test
    public void testDiscardIsValid() {
        int slot = 0;
        int player = 0;
        CardColour colour = CardColour.RED;

        GameState state = new BasicState(2, 5);
        state.setCardAt(player, slot, new Card(4, colour));
        state.setInformation(0);

        Action discard = new DiscardCard(slot);
        assertEquals(true, discard.isLegal(player, state));
    }
}
