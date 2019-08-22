package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 05/05/17.
 */
public class TestTellFives {

    private BasicState state;
    private TellFives instance;

    @Before
    public void setup(){
        state = new BasicState(2);
        instance = new TellFives();
    }

    @Test
    public void testWillTellAnUnidentifiedFive(){
        state.getHand(1).setCard(0, new Card(5, CardColour.BLUE));
        assertEquals(true, instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertEquals(true, action != null);
        assertEquals(TellValue.class, action.getClass());
        TellValue tellValue = (TellValue) action;
        assertEquals(5, tellValue.value);
        assertEquals(1, tellValue.player);
    }

    @Test
    public void testWillNotTellAnIdentifiedFive(){
        state.getHand(1).setCard(0, new Card(5, CardColour.BLUE));
        state.getHand(1).setKnownValue(5, new Integer[]{0});
        assertEquals(false, instance.canFire(0, state));
    }
}
