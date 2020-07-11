package com.fossgalaxy.games.fireworks.state.events;

import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 24/11/16.
 */
public class TestCardDrawn {

    private BasicState state;

    @Before
    public void setup(){
        state = new BasicState(2);
        state.init();
    }

    @Test
    public void testCardDrawn(){
        CardDrawn cardDrawn = new CardDrawn(0, 0, CardColour.BLUE, 1, GameEvent.UNKNOWN_TURN);
        cardDrawn.apply(state);

        assertEquals(new Card(1, CardColour.BLUE), state.getHand(0).getCard(0));
    }
}
