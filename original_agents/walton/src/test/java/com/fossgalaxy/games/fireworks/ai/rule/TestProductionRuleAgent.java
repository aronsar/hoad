package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.actions.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 23/11/16.
 */
public class TestProductionRuleAgent {

    private ProductionRuleAgent agent;
    private BasicState state;

    @Before
    public void setup() {
        state = new BasicState(2);
        state.init();

        agent = new ProductionRuleAgent();
        agent.addRule(new TellAnyoneAboutUsefulCard());
        agent.addRule(new PlaySafeCard());
    }


    @Test
    public void testWillTellAboutUsefulCard() {
        // This is a useful card
        state.getHand(1).setCard(0, new Card(1, CardColour.BLUE));

        Action action = agent.doMove(0, state);

        assertEquals(true, action != null);
        assertEquals(true, action instanceof TellColour || action instanceof TellValue);

    }

    @Test
    public void testWillPlaySafeCardIfNoUsefulCard() {
        // None of these are useful
        for (CardColour colour : CardColour.values()) {
            state.getHand(1).setCard(colour.ordinal(), new Card(1, colour));
            state.setTableValue(colour, 1);
        }

        // We have a useful card and we know it
        state.getHand(0).setCard(0, new Card(2, CardColour.BLUE));
        state.getHand(0).setKnownValue(2, new Integer[]{0});
        Action action = agent.doMove(0, state);
        assertEquals(true, action != null);
        assertEquals(PlayCard.class, action.getClass());
    }

    @Test(expected = IllegalStateException.class)
    public void testDefaultBehaviourOfDiscard(){
        // None of these are useful
        for (CardColour colour : CardColour.values()) {
            state.getHand(1).setCard(colour.ordinal(), new Card(1, colour));
            state.setTableValue(colour, 1);
        }

        agent.doMove(0, state);
    }
}
