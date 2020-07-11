package com.fossgalaxy.games.fireworks.engine;

import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class CardTest {

	public Object[] parametersForEqualsAndHashCode() {
		return $($(CardColour.BLUE, 0, CardColour.BLUE, 0, true), $(CardColour.BLUE, 0, CardColour.GREEN, 0, false),
				$(CardColour.BLUE, 1, CardColour.BLUE, 0, false), $(CardColour.BLUE, 0, CardColour.BLUE, 1, false));
	}

	@Test
	@Parameters(method = "parametersForEqualsAndHashCode")
	public void testEqualsAndHashCode(CardColour fc, int fv, CardColour sc, int sv, boolean expected) {
		Card c1 = new Card(fv, fc);
		Card c2 = new Card(sv, sc);
		assertEquals(expected, c1.equals(c2));
		assertEquals(expected, c2.equals(c1));
		assertEquals(expected, c1.hashCode() == c2.hashCode());
	}

	@Test
	public void testEqualsEdgeCases() {
		Card card = new Card(0, CardColour.BLUE);
		Card c2 = new Card(null, CardColour.BLUE);
		assertEquals(true, card.equals(card));
		assertEquals(false, card.equals(null));
		assertEquals(false, card.equals(new Object()));
		assertEquals(true, c2.equals(c2));
		assertEquals(false, c2.equals(card));
	}

}
