package com.fossgalaxy.games.fireworks.engine;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.Deck;

/**
 * Created by Piers on 11/07/2016.
 */
public class DeckTest {

	@Test
	public void testAddCard() {
		Deck deck = new Deck();

		assertEquals(0, deck.getCardsLeft());
		assertEquals(false, deck.hasCardsLeft());

		Card top = new Card(1, CardColour.BLUE);
		deck.add(top);

		assertEquals(1, deck.getCardsLeft());
		assertEquals(true, deck.hasCardsLeft());
		assertEquals(top, deck.getTopCard());
	}

	@Test
	public void testEmptyDeck() {
		Deck deck = new Deck();
		assertEquals(false, deck.hasCardsLeft());
		assertEquals(0, deck.getCardsLeft());
	}

	@Test
	public void testRemovalOfCardDecrementsLeft() {
		Deck deck = new Deck();

		Card top = new Card(1, CardColour.BLUE);
		deck.add(top);

		assertEquals(top, deck.getTopCard());
		assertEquals(0, deck.getCardsLeft());
		assertEquals(false, deck.hasCardsLeft());
	}

	@Test
	public void testShuffle() {
		Deck deck = new Deck();
		deck.shuffle();
	}

}
