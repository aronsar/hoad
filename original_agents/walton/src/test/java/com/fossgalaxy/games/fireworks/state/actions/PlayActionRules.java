package com.fossgalaxy.games.fireworks.state.actions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fossgalaxy.games.fireworks.state.*;
import org.junit.Test;

/**
 * High level game-play tests for play action.
 */
public class PlayActionRules {

	@Test
	public void testPlayCard5GivesInfomation() {
		int slot = 0;
		int player = 0;

		CardColour colour = CardColour.BLUE;
		Card nextCard = new Card(5, CardColour.GREEN);

		// setup
		GameState state = new BasicState(2, 5);
		state.setCardAt(player, slot, new Card(5, colour));
		Deck deck = state.getDeck();
		deck.add(nextCard);

		// the table has a 4 of the correct colour on it
		state.setTableValue(colour, 4);
		state.setInformation(0);

		// checks for invariants
		int lives = state.getLives();
		int infomation = state.getInfomation();

		// check that the table is setup for that colour
		assertEquals(4, state.getTableValue(colour));

		// play the card
		Action play = new PlayCard(slot);
		play.apply(player, state);

		// check the result is as expected
		assertEquals(5, state.getTableValue(colour));
		assertEquals(lives, state.getLives());
		assertEquals(infomation + 1, state.getInfomation());
		assertEquals(nextCard, state.getCardAt(player, slot));
	}

	@Test
	public void testPlayCard5GivesNoInfomation() {
		int slot = 0;
		int player = 0;

		CardColour colour = CardColour.BLUE;
		Card nextCard = new Card(5, CardColour.GREEN);

		// setup
		GameState state = new BasicState(2, 5);
		state.setCardAt(player, slot, new Card(5, colour));
		Deck deck = state.getDeck();
		deck.add(nextCard);

		// the table has a 4 of the correct colour on it
		state.setTableValue(colour, 4);
		state.setInformation(state.getStartingInfomation());

		// checks for invariants
		int lives = state.getLives();
		int infomation = state.getInfomation();

		// check that the table is setup for that colour
		assertEquals(4, state.getTableValue(colour));

		// play the card
		Action play = new PlayCard(slot);
		play.apply(player, state);

		// check the result is as expected
		assertEquals(5, state.getTableValue(colour));
		assertEquals(lives, state.getLives());
		assertEquals(infomation, state.getInfomation());
		assertEquals(nextCard, state.getCardAt(player, slot));
	}

	@Test
	public void testPlayCardInvalid() {

		int slot = 0;
		int player = 0;

		CardColour colour = CardColour.BLUE;

		Card nextCard = new Card(5, CardColour.GREEN);

		// setup
		GameState state = new BasicState(2, 5);
		state.setCardAt(player, slot, new Card(4, colour));
		Deck deck = state.getDeck();
		deck.add(nextCard);

		// checks for invariants
		int lives = state.getLives();
		int infomation = state.getInfomation();

		// check that the table is setup for that colour
		assertEquals(0, state.getTableValue(colour));

		// play the card
		Action play = new PlayCard(slot);
		play.apply(player, state);

		// check the result is as expected
		assertEquals(0, state.getTableValue(colour));
		assertEquals(lives - 1, state.getLives());
		assertEquals(infomation, state.getInfomation());
		assertEquals(nextCard, state.getCardAt(player, slot));
	}

	@Test
	public void testPlayCardInvalidEmptyDeck() {
		int slot = 0;
		int player = 0;

		CardColour colour = CardColour.BLUE;

		// setup
		GameState state = new BasicState(2, 5);
		state.setCardAt(player, slot, new Card(4, colour));

		// checks for invariants
		int lives = state.getLives();
		int infomation = state.getInfomation();

		// check that the table is setup for that colour
		assertEquals(0, state.getTableValue(colour));

		// play the card
		Action play = new PlayCard(slot);
		play.apply(player, state);

		// check the result is as expected
		assertEquals(0, state.getTableValue(colour));
		assertEquals(lives - 1, state.getLives());
		assertEquals(infomation, state.getInfomation());
		assertEquals(null, state.getCardAt(player, slot));
	}

	@Test
	public void testIsIllegalIfNullCard() {
		final int playerID = 1;
		final int slotID = 1;

		Hand mockHand = mock(Hand.class);
		when(mockHand.getCard(slotID)).thenReturn(null);
		when(mockHand.hasCard(slotID)).thenReturn(false);

		GameState state = mock(GameState.class);
		when(state.getHand(playerID)).thenReturn(mockHand);

		PlayCard instance = new PlayCard(slotID);

		assertEquals(false, instance.isLegal(playerID, state));
	}

	@Test(expected = RulesViolation.class)
	public void testIllegalActionTripsException() {
		final int playerID = 1;
		final int slotID = 1;

		Hand mockHand = mock(Hand.class);
		when(mockHand.getCard(slotID)).thenReturn(null);
		when(mockHand.hasCard(slotID)).thenReturn(false);

		GameState state = mock(GameState.class);
		when(state.getHand(playerID)).thenReturn(mockHand);

		PlayCard instance = new PlayCard(slotID);
		instance.apply(playerID, state);
	}

	@Test
	public void testTwoActionsWithSameSlotAreEqual() {
		PlayCard play1 = new PlayCard(1);
		PlayCard play2 = new PlayCard(1);

		assertEquals(true, play1.equals(play2));
		assertEquals(true, play2.equals(play1));
		assertEquals(play1.hashCode(), play2.hashCode());
	}

	@Test
	public void testTwoActionsWithDifferentSlotsAreNotEqual() {
		PlayCard play1 = new PlayCard(1);
		PlayCard play2 = new PlayCard(2);

		assertEquals(false, play1.equals(play2));
		assertEquals(false, play2.equals(play1));
	}

	@Test
	public void testIslegalIfNullCard() {
		final int playerID = 1;
		final int slotID = 1;
		final Card card = new Card(1, CardColour.BLUE);

		Hand mockHand = mock(Hand.class);
		when(mockHand.getCard(slotID)).thenReturn(card);
		when(mockHand.hasCard(slotID)).thenReturn(true);

		GameState state = mock(GameState.class);
		when(state.getHand(playerID)).thenReturn(mockHand);

		PlayCard instance = new PlayCard(slotID);

		assertEquals(true, instance.isLegal(playerID, state));
	}

	@Test
	public void testPlayCardValid() {

		int slot = 0;
		int player = 0;

		CardColour colour = CardColour.BLUE;

		Card nextCard = new Card(5, CardColour.GREEN);

		// setup
		GameState state = new BasicState(2, 5);
		state.setCardAt(player, slot, new Card(1, colour));
		Deck deck = state.getDeck();
		deck.add(nextCard);

		// checks for invariants
		int lives = state.getLives();
		int infomation = state.getInfomation();

		// check that the table is setup for that colour
		assertEquals(0, state.getTableValue(colour));

		// play the card
		Action play = new PlayCard(slot);
		play.apply(player, state);

		// check the result is as expected
		assertEquals(1, state.getTableValue(colour));
		assertEquals(lives, state.getLives());
		assertEquals(infomation, state.getInfomation());
		assertEquals(nextCard, state.getCardAt(player, slot));
	}

	@Test
	public void testPlayCardValidEmptyDeck() {
		int slot = 0;
		int player = 0;

		CardColour colour = CardColour.BLUE;

		// setup
		GameState state = new BasicState(2, 5);
		state.setCardAt(player, slot, new Card(1, colour));

		// checks for invariants
		int lives = state.getLives();
		int infomation = state.getInfomation();

		// check that the table is setup for that colour
		assertEquals(0, state.getTableValue(colour));

		// play the card
		Action play = new PlayCard(slot);
		play.apply(player, state);

		// check the result is as expected
		assertEquals(1, state.getTableValue(colour));
		assertEquals(lives, state.getLives());
		assertEquals(infomation, state.getInfomation());
		assertEquals(null, state.getCardAt(player, slot));
	}

}
