package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.ai.osawa.rules.OsawaDiscard;
import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.Deck;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 24/11/16.
 */
public class TestOsawaDiscard {

    private BasicState state;
    private OsawaDiscard instance;

    @Before
    public void setup(){
        state = new BasicState(2);
        state.init();
        state.setInformation(7);
        instance = new OsawaDiscard();
    }

    @Test
    public void testDiscardSafeWhenPossible(){
        // need a useless card and a safe card. Check it discards the safe one
        state.setTableValue(CardColour.BLUE, 3);

        state.getHand(0).setCard(0, new Card(2, CardColour.BLUE));
        state.getHand(0).setKnownColour(CardColour.BLUE, new Integer[]{0});
        state.getHand(0).setKnownValue(2, new Integer[]{0});

        Deck deck = state.getDeck();

        for (int i = 0; i < 3; i++) {
            deck.remove(new Card(1, CardColour.RED));
            state.addToDiscard(new Card(1, CardColour.RED));
        }

        state.getHand(1).setCard(0, new Card(2, CardColour.RED));
        state.getHand(1).setKnownColour(CardColour.RED, new Integer[]{0});

        assertEquals(true, instance.canFire(0, state));

        Action action = instance.execute(0, state);
        assertEquals(true, action != null);
        assertEquals(true, action instanceof DiscardCard);
        DiscardCard discardCard = (DiscardCard) action;

        assertEquals(0, discardCard.slot);
    }

    @Test
    public void testDiscardUselessWhenNoSafe(){
        Deck deck = state.getDeck();

        for (int i = 0; i < 3; i++) {
            deck.remove(new Card(1, CardColour.RED));
            state.addToDiscard(new Card(1, CardColour.RED));
        }

        state.getHand(1).setCard(0, new Card(2, CardColour.RED));
        state.getHand(1).setKnownColour(CardColour.RED, new Integer[]{0});

        assertEquals(true, instance.canFire(1, state));

        Action action = instance.execute(1, state);
        assertEquals(true, action != null);
        assertEquals(true, action instanceof DiscardCard);
        DiscardCard discardCard = (DiscardCard) action;
        assertEquals(0, discardCard.slot);
    }

    @Test
    public void testNothingToDiscard(){
        assertEquals(false, instance.canFire(0, state));
    }
}
