package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.ai.rule.random.TellRandomly;
import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 24/11/16.
 */
public class TestTellRandomly {

    private BasicState state;
    private TellRandomly instance;

    @Before
    public void setup(){
        state = new BasicState(2);
        instance = new TellRandomly();
        state.setInformation(8);
        state.init();
    }

    @Test
    public void testTellsWhenAbleTo(){
        assertEquals(true, instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertEquals(true, action != null);
        assertEquals(true, action instanceof TellColour || action instanceof TellValue);
    }

    @Test
    public void testTellsNothingWhenAllCardsNull(){
        for(int i = 0; i < 5; i++){
            state.getHand(0).setCard(i, null);
        }

        assertEquals(false, instance.canFire(1, state));
    }
}
