package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.Deck;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by piers on 12/12/16.
 */
public class TestTellAnyoneAboutUselessCard {

    private BasicState state;
    private TellAnyoneAboutUselessCard instance;

    @Before
    public void setup() {
        this.state = new BasicState(2);
        this.state.init();
        this.state.setInformation(7);
        this.instance = new TellAnyoneAboutUselessCard();

        /*for (int player = 0; player < state.getPlayerCount(); player++) {
            for (int slot = 0; slot < state.getHandSize(); slot++) {
                state.getHand(player).setHasCard(slot, true);
            }
        }*/
    }

    @Test
    public void testTellsAboutAll3sDiscardedSoWillDiscard4() {
        state.getHand(0).setCard(0, new Card(4, CardColour.BLUE));
//        state.getHand(0).setKnownValue(4, new Integer[]{0});
//        state.getHand(0).setKnownColour(CardColour.BLUE, new Integer[]{0});

        // Need to discard the correct cards
        state.addToDiscard(new Card(3, CardColour.BLUE));
        state.addToDiscard(new Card(3, CardColour.BLUE));

        assertEquals(true, instance.canFire(1, state));
        Action action = instance.execute(1, state);
        assertNotNull(action);
        assertEquals(true, action instanceof TellColour || action instanceof TellValue);
        if (action instanceof TellColour) {
            TellColour tellColour = (TellColour) action;
            assertEquals(CardColour.BLUE, tellColour.colour);
            assertEquals(0, tellColour.player);
        }
        if (action instanceof TellValue) {
            TellValue tellValue = (TellValue) action;
            assertEquals(4, tellValue.value);
            assertEquals(0, tellValue.player);
        }
    }

    @Test
    public void testTellsAboutDiscardPossibleCardThatIsUseless() {
        // Throw away all the ones and take them out the deck
        Deck deck = state.getDeck();

        for (int i = 0; i < 3; i++) {
            deck.remove(new Card(1, CardColour.BLUE));
            state.addToDiscard(new Card(1, CardColour.BLUE));
        }

        state.getHand(1).setCard(0, new Card(2, CardColour.BLUE));


    }
}
