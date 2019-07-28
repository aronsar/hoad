package com.fossgalaxy.games.fireworks.state.events;

import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 24/11/16.
 */
public class TestCardDiscarded {

    private BasicState state;

    @Before
    public void setup(){
        state = new BasicState(2);
        state.setInformation(7);
        state.init();
    }

    @Test
    public void testDiscardCard(){
        Card card = state.getHand(0).getCard(0);
        CardDiscarded cardDiscarded = new CardDiscarded(0, 0, card.colour, card.value, GameEvent.UNKNOWN_TURN);

        cardDiscarded.apply(state, 0);

        assertEquals(null, state.getHand(0).getCard(0));
        assertEquals(8, state.getInfomation());
        assertEquals(1, state.getDiscards().size());
        assertEquals(true, state.getDiscards().contains(card));
    }
}
