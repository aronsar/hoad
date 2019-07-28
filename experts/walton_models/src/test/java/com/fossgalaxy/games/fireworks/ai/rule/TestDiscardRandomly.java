package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.ai.rule.random.DiscardRandomly;
import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 24/11/16.
 */
public class TestDiscardRandomly {

    @Test
    public void testDiscardRandomly(){
        BasicState state = new BasicState(2);
        state.init();
        state.setInformation(7);

        DiscardRandomly instance = new DiscardRandomly();
        assertEquals(true, instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertEquals(true, action != null);
        assertEquals(DiscardCard.class, action.getClass());
    }
}
