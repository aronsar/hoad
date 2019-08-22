package com.fossgalaxy.games.fireworks.ai.hat;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 05/12/16.
 */
@RunWith(JUnitParamsRunner.class)
public class TestHatGuessing {


    public Object[] parametersForTestGetEncodedValue() {
        return $(
                $(1, 2, 0),
                $(1, 3, 1),
                $(1, 4, 2),
                $(1, 0, 3)
        );
    }

    @Test
    @Parameters(method = "parametersForTestGetEncodedValue")
    public void testGetEncodedValue(int whoTold, int toldWho, int expected) {
        HatGuessing guessing = new HatGuessing();
        guessing.receiveID(0);

        assertEquals(expected, guessing.getEncodedValue(whoTold, toldWho));
    }
}
