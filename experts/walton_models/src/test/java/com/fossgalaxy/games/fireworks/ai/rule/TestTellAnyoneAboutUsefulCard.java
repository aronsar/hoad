package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 22/11/16.
 */
@RunWith(JUnitParamsRunner.class)
public class TestTellAnyoneAboutUsefulCard {

    private BasicState state;
    private TellAnyoneAboutUsefulCard instance;

    @Before
    public void setup() {
        state = new BasicState(5);
        instance = new TellAnyoneAboutUsefulCard();
    }

    public Object[] parametersForTestAll() {
        return $(
                $(0, 1), $(0, 2), $(0, 3), $(0, 4),
                $(1, 0), $(1, 2), $(1, 3), $(1, 4),
                $(2, 0), $(2, 1), $(2, 3), $(2, 4),
                $(3, 0), $(3, 1), $(3, 2), $(3, 4),
                $(4, 0), $(4, 1), $(4, 2), $(4, 3)
        );
    }

    @Test
    @Parameters(method="parametersForTestAll")
    public void testAll(int first, int second) {
        // For each player, test if a useful card in each other players hand will be told
        state = new BasicState(5);
        // Set it up so that second has a useful card
        state.getHand(second).setCard(0, new Card(1, CardColour.RED));

        assertEquals(true, instance.canFire(first, state));
        Action action = instance.execute(first, state);
        assertEquals(true, action != null);
        assertEquals(true, action instanceof TellValue || action instanceof TellColour);
        if (action instanceof TellValue) {
            TellValue tellValue = (TellValue) action;
            assertEquals(true, tellValue.value == 1);
            assertEquals(true, tellValue.player == second);
        }
        if (action instanceof TellColour) {
            TellColour tellColour = (TellColour) action;
            assertEquals(true, tellColour.colour == CardColour.RED);
            assertEquals(true, tellColour.player == second);
        }
    }

    @Test
    public void testKnowsValueNeedsColourOfUsefulCard(){
        state.getHand(0).setCard(0, new Card(1, CardColour.BLUE));
        state.getHand(0).setKnownValue(1, new Integer[]{0});

        assertEquals(true, instance.canFire(1, state));
        Action action = instance.execute(1, state);
        assertEquals(true, action != null);
        assertEquals(true, action instanceof TellColour);
        TellColour tellColour = (TellColour) action;
        assertEquals(true, tellColour.colour == CardColour.BLUE);
    }

    @Test
    public void testKnowsNothingButUselessHand(){
        for(CardColour colour : CardColour.values()){
            state.getHand(0).setCard(0, new Card(1, colour));
            state.setTableValue(colour, 1);
        }

        assertEquals(false, instance.canFire(1, state));
    }
}
