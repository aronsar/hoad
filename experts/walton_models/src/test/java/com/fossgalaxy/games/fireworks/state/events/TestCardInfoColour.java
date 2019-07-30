package com.fossgalaxy.games.fireworks.state.events;

import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 24/11/16.
 */
@RunWith(JUnitParamsRunner.class)
public class TestCardInfoColour {

    private BasicState state;

    @Before
    public void setup() {
        state = new BasicState(2);
        state.init();
    }

    public Object[] parametersForGetters() {
        return $(
                $(0, 1, CardColour.BLUE, new Integer[]{0}),
                $(0, 1, CardColour.BLUE, new Integer[]{0, 1}),
                $(0, 1, CardColour.BLUE, new Integer[]{0, 1, 2}),
                $(0, 1, CardColour.BLUE, new Integer[]{0, 1, 2, 3}),
                $(0, 1, CardColour.BLUE, new Integer[]{0, 1, 2, 3, 4}),
                $(1, 2, CardColour.BLUE, new Integer[]{0, 1, 2, 3, 4})
        );
    }

    @Test
    @Parameters(method = "parametersForGetters")
    public void testGetters(int performer, int playerId, CardColour colour, Integer... slots) {
        CardInfoColour cardInfoColour = new CardInfoColour(performer, playerId, colour, slots);

        assertEquals(performer, cardInfoColour.getPerformer());
        assertEquals(playerId, cardInfoColour.getPlayerTold());
        assertEquals(colour, cardInfoColour.getColour());
        assertEquals(true, Arrays.equals(slots, cardInfoColour.getSlots()));
    }

    @Test
    public void testCardInfoColour() {
        CardInfoColour cardInfoColour = new CardInfoColour(0, 1, CardColour.BLUE, 0, 1);
        state.getHand(1).setCard(0, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(1, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(2, new Card(1, CardColour.RED));
        state.getHand(1).setCard(3, new Card(1, CardColour.GREEN));
        state.getHand(1).setCard(4, new Card(1, CardColour.ORANGE));

        cardInfoColour.apply(state);

        assertEquals(CardColour.BLUE, state.getHand(1).getKnownColour(0));
        assertEquals(CardColour.BLUE, state.getHand(1).getKnownColour(1));
        assertEquals(null, state.getHand(1).getKnownColour(2));
        assertEquals(null, state.getHand(1).getKnownColour(3));
        assertEquals(null, state.getHand(1).getKnownColour(4));
    }
}
