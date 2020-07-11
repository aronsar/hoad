package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by webpigeon on 26/10/16.
 */
public class TestTellAboutOnes {

    private BasicState state;
    private TellAboutOnes instance;

    @Before
    public void setup() {
        state = new BasicState(2);
        state.setInformation(8);
        instance = new TellAboutOnes();
    }

    /**
     * A tell action is illegal if there is no information, the rule shouild report it cannot fire in this case.
     */
    @Test
    public void tellIsIllegalWithNoInfo() {
        state.setInformation(0);

        boolean result = instance.canFire(0, state);

        assertEquals(result, false);
    }

    /**
     * If the next player has a one they do not know about, the rule can fire.
     */
    @Test
    public void tellNextPlayerHasOnes() {
        state.getHand(1).setCard(0, new Card(1, CardColour.RED));
        state.getHand(1).setCard(1, new Card(1, CardColour.RED));

        assertTrue(instance.canFire(0, state));
        Action action = instance.execute(0, state);
        if(action instanceof TellValue){
            TellValue tellValue = (TellValue) action;
            assertEquals(1, tellValue.player);
            assertEquals(1, tellValue.value);
        }
        else if(action instanceof TellColour){
            TellColour tellColour = (TellColour) action;
            assertEquals(1, tellColour.player);
            assertEquals(CardColour.RED, tellColour.colour);
        }
    }

    /**
     * If the next player has a one, but they know about it, the rule should not fire.
     */
    @Test
    public void tellNextPlayerHasOnesButTheyKnowAlready() {
        state.getHand(1).setCard(0, new Card(1, CardColour.RED));
        state.getHand(1).setKnownValue(1, new Integer[]{0});
        state.getHand(1).setKnownColour(CardColour.RED, new Integer[]{0});

        assertEquals(false, instance.canFire(0, state));
    }

    @Test
    public void tellNextPlayerKnowsValueButNotColour() {
        state.getHand(1).setCard(0, new Card(1, CardColour.RED));
        state.getHand(1).setKnownValue(1, new Integer[]{0});

        assertEquals(true, instance.canFire(0, state));
    }

    @Test
    public void tellNextPlayerWhenTheyHaveNoOnes() {
        state.getHand(1).setCard(0, new Card(2, CardColour.BLUE));
        state.getHand(1).setCard(1, new Card(2, CardColour.BLUE));
        state.getHand(1).setCard(2, new Card(3, CardColour.BLUE));
        state.getHand(1).setCard(3, new Card(3, CardColour.BLUE));
        state.getHand(1).setCard(4, new Card(4, CardColour.BLUE));

        assertEquals(false, instance.canFire(0, state));
    }

}
