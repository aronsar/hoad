package com.fossgalaxy.games.fireworks.state;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;

public class TestBasicState {
	
	/**
	 * Game logic test: check that initial conditions hold
	 */
	@Test
	public void testCreate() {
		GameState state = new BasicState(2);
		assertEquals(8, state.getInformation());
		assertEquals(3, state.getLives());
		assertEquals(2, state.getPlayerCount());
		
		assertEquals(8, state.getStartingInfomation());
		assertEquals(3, state.getStartingLives());
		assertEquals(0, state.getScore());
	}
	
	@Test
	public void testNoLivesIsGameOver() {
		
		GameState state = new BasicState(2);
		state.setLives(0);
		assertEquals(true, state.isGameOver());
	}
	
	@Test
	public void testPerfectScoreIsGameOver() {
		GameState state = new BasicState(2);
	
		for (CardColour colour : CardColour.values()) {
			state.setTableValue(colour, 5);
		}
		
		assertEquals(25, state.getScore());
		assertEquals(true, state.isGameOver());
	}
	
	/**
	 * Game logic test: check the end game plays out correctly
	 */
	@Test
	public void testEndgame() {
		
		//setup a two player game
		GameState state = new BasicState(2);
		state.init();
		Deck deck =  state.getDeck();
		
		// empty the deck
		while(deck.hasCardsLeft()) {
			deck.getTopCard();
		}
		
		// draw the last card as player 1
		Action action = new PlayCard(1);
		Action action2 = new PlayCard(2); //slot 1 will be null
		
		// check the game is not over
		action.apply(0, state);
		assertEquals("Game was over when it was my go", false, state.isGameOver());
		
		// let player 1 have a go
		action.apply(1, state);
		assertEquals("Game was over after other player's go", false, state.isGameOver());
		
		// we make the last move
		action2.apply(0, state);
		assertEquals("game was not over at the end", true, state.isGameOver());
	}
	
}
