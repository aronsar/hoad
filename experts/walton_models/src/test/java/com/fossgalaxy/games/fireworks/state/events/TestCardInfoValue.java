package com.fossgalaxy.games.fireworks.state.events;

import com.fossgalaxy.games.fireworks.state.BasicState;
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
public class TestCardInfoValue {

    private BasicState state;

    @Before
    public void setup() {
        state = new BasicState(2);
        state.init();
    }

    public Object[] parametersForGetters() {
        return $(
                $(0, 1, 1, new Integer[]{0}),
                $(0, 1, 1, new Integer[]{0, 1}),
                $(0, 1, 1, new Integer[]{0, 1, 2}),
                $(0, 1, 1, new Integer[]{0, 1, 2, 3}),
                $(0, 1, 1, new Integer[]{0, 1, 2, 3, 4}),
                $(1, 2, 1, new Integer[]{0, 1, 2, 3, 4})
        );
    }

    @Test
    @Parameters(method = "parametersForGetters")
    public void testGetters(int performer, int playerId, int value, Integer... slots) {
        CardInfoValue cardInfoValue = new CardInfoValue(performer, playerId, value, slots);

        assertEquals(performer, cardInfoValue.getPerformer());
        assertEquals(playerId, cardInfoValue.getPlayerTold());
        assertEquals(value, cardInfoValue.getValue());
        assertEquals(true, Arrays.equals(slots, cardInfoValue.getSlots()));
    }

    @Test
    public void testApply(){
        CardInfoValue cardInfoValue =new CardInfoValue(0, 1, 1, 1, 2);

        cardInfoValue.apply(state);

        assertEquals(null, state.getHand(1).getKnownValue(0));
        assertEquals(1, state.getHand(1).getKnownValue(1).intValue());
        assertEquals(1, state.getHand(1).getKnownValue(2).intValue());
        assertEquals(null, state.getHand(1).getKnownValue(3));
        assertEquals(null, state.getHand(1).getKnownValue(4));
    }
}
