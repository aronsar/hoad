package com.fossgalaxy.games.fireworks.state;


import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 29/11/16.
 */
@RunWith(JUnitParamsRunner.class)
public class TestTimedHand {

    private TimedHand hand;

    @Before
    public void setup() {
        this.hand = new TimedHand(5);

        for (int i = 0; i < 5; i++) {
            hand.setCard(i, new Card(i + 1, CardColour.BLUE));
            //hand.setHasCard(i, true);
        }
    }

    @Test
    public void testGetOldestSlot() {
        assertEquals(0, hand.getOldestSlot());

        hand.setCard(0, new Card(2, CardColour.GREEN));
        //hand.setHasCard(0, true);

        assertEquals(1, hand.getOldestSlot());
    }

    @Test
    public void testGetNewestSlot() {
        assertEquals(4, hand.getNewestSlot());
    }

    public Object[] parametersForTestGetAge() {
        return $(
                $(0, 0),
                $(1, 1),
                $(2, 2),
                $(3, 3),
                $(4, 4)
        );
    }

    @Test
    @Parameters(method = "parametersForTestGetAge")
    public void testGetAge(int slot, int expected) {
        assertEquals(expected, hand.getAge(slot));
    }
}
