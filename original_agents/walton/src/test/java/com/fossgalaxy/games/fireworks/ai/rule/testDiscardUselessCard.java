package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.Deck;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;
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
public class testDiscardUselessCard {

    private BasicState state;
    private DiscardUselessCard instance;

    @Before
    public void setup() {
        state = new BasicState(2);
        instance = new DiscardUselessCard();
        state.setInformation(7);
    }

    @Test
    public void testAll3sDiscardedSoWillDiscard4() {

        state.getHand(0).setCard(0, new Card(4, CardColour.BLUE));
        state.getHand(0).setKnownValue(4, new Integer[]{0});
        state.getHand(0).setKnownColour(CardColour.BLUE, new Integer[]{0});

        // Need to discard the correct cards
        state.addToDiscard(new Card(3, CardColour.BLUE));
        state.addToDiscard(new Card(3, CardColour.BLUE));

        assertEquals(true, instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertEquals(true, action != null);
        assertEquals(true, action instanceof DiscardCard);
        DiscardCard discardCard = (DiscardCard) action;

        assertEquals(true, discardCard.slot == 0);
    }

    @Test
    public void testDiscardPossibleCardThatIsUseless() {
        state.init();
        // Throw away all the ones and take them out the deck
        Deck deck = state.getDeck();

        for (int i = 0; i < 3; i++) {
            deck.remove(new Card(1, CardColour.BLUE));
            state.addToDiscard(new Card(1, CardColour.BLUE));
        }

        state.getHand(1).setCard(0, new Card(2, CardColour.BLUE));
        state.getHand(1).setKnownColour(CardColour.BLUE, new Integer[]{0});

        assertEquals(true, instance.canFire(1, state));

        Action action = instance.execute(1, state);
        assertEquals(true, action != null);
        assertEquals(true, action instanceof DiscardCard);
        DiscardCard discardCard = (DiscardCard) action;
        assertEquals(0, discardCard.slot);
    }

    @Test
    public void testDoesNotDiscardIfDoesNotKnowColour() {
        Action action = instance.execute(1, state);
        assertEquals(true, action == null);
    }
}

