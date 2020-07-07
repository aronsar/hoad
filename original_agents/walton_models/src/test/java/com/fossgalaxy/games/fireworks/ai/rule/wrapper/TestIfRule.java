package com.fossgalaxy.games.fireworks.ai.rule.wrapper;

import com.fossgalaxy.games.fireworks.ai.rule.random.DiscardRandomly;
import com.fossgalaxy.games.fireworks.ai.rule.random.TellRandomly;
import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by piers on 07/12/16.
 */
public class TestIfRule {

    private IfRule instance;
    private BasicState state;

    @Before
    public void setup(){
        this.instance = new IfRule(
                (id, state) -> state.getInfomation() > 5,
                new TellRandomly(),
                new DiscardRandomly()
        );
        this.state = new BasicState(2);
        this.state.init();
    }

    @Test
    public void testIfRuleFiresWhenPredicateTrue(){
        // Can always fire in default state
        assertEquals(true, instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertNotNull(action);
        assertEquals(true, action instanceof TellValue || action instanceof TellColour);
    }

    @Test
    public void testIfRuleFiresWhenPredicateFalse(){
        state.setInformation(4);
        assertEquals(true, instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertNotNull(action);
        assertEquals(DiscardCard.class, action.getClass());
    }
}
