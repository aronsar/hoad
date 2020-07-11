package com.fossgalaxy.games.fireworks.state.actions;

import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.actions.*;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 23/11/16.
 */
@RunWith(JUnitParamsRunner.class)
public class ActionEquals {

    public Object[] parametersForTestDiscardEquals() {
        return $(
                $(new DiscardCard(0), new DiscardCard(0), true),
                $(new DiscardCard(0), new DiscardCard(1), false),
                $(new DiscardCard(0), new PlayCard(0), false),
                $(new DiscardCard(0), new PlayCard(1), false),
                $(new TellColour(0, CardColour.BLUE), new TellColour(0, CardColour.BLUE), true),
                $(new TellColour(0, CardColour.BLUE), new TellColour(1, CardColour.BLUE), false),
                $(new TellColour(0, CardColour.BLUE), new TellColour(0, CardColour.RED), false),
                $(new TellColour(0, CardColour.BLUE), new TellColour(1, CardColour.RED), false),
                $(new TellColour(0, CardColour.BLUE), new TellValue(0, 1), false),
                $(new TellValue(0, 1), new TellValue(0, 1), true),
                $(new TellValue(0, 1), new TellValue(1, 1), false),
                $(new TellValue(0, 1), new TellValue(0, 2), false),
                $(new TellValue(0, 1), new TellValue(2, 2), false),
                $(new PlayCard(0), new PlayCard(0), true),
                $(new PlayCard(0), new PlayCard(1), false),
                $(new PlayCard(0), new DiscardCard(0), false)
        );
    }

    @Test
    @Parameters(method = "parametersForTestDiscardEquals")
    public void testDiscardEquals(Action first, Action second, boolean expected) {
        assertEquals(expected, first.equals(second));
        assertEquals(expected, second.equals(first));
    }
}
