package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 22/11/16.
 */
public class TestDiscardSafeCard {

    private BasicState state;
    private DiscardSafeCard instance;

    @Before
    public void setup() {
        state = new BasicState(2);
        instance = new DiscardSafeCard();
        state.setInformation(7);
    }


    @Test
    public void discardIsIllegalWithAllInfo() {
        state.setInformation(8);

        boolean result = instance.canFire(0, state);

        assertEquals(false, result);
    }

    @Test
    public void willDiscardSafeCardKnownValueOnly() {

        // All ones are present
        for (CardColour colour : CardColour.values()) {
            state.setTableValue(colour, 1);
        }

        // Ensure the hand contains a one and only one one
        state.getHand(0).setCard(0, new Card(1, CardColour.RED));
        state.getHand(0).setKnownValue(1, new Integer[]{0});
        state.getHand(0).setCard(1, new Card(2, CardColour.BLUE));
        state.getHand(0).setCard(2, new Card(2, CardColour.BLUE));
        state.getHand(0).setCard(3, new Card(2, CardColour.GREEN));
        state.getHand(0).setCard(4, new Card(2, CardColour.ORANGE));

        assertEquals(true, instance.canFire(0, state));

        // Should discard card 0 only
        Action action = instance.execute(0, state);
        assertEquals(true, action instanceof DiscardCard);

        DiscardCard discardCard = (DiscardCard) action;
        assertEquals(0, discardCard.slot);

    }

    @Test
    public void willDiscardSafeCardColourOnly(){
        // Blues useless
        state.setTableValue(CardColour.BLUE, 5);

        // Exactly one disposable card
        state.getHand(0).setCard(0, new Card(1, CardColour.BLUE));
        state.getHand(0).setKnownColour(CardColour.BLUE, new Integer[]{0});
        state.getHand(0).setCard(1, new Card(2, CardColour.RED));
        state.getHand(0).setCard(2, new Card(2, CardColour.ORANGE));
        state.getHand(0).setCard(3, new Card(2, CardColour.GREEN));
        state.getHand(0).setCard(4, new Card(2, CardColour.ORANGE));

        assertEquals(true, instance.canFire(0, state));

        // Should discard card 0 only
        Action action = instance.execute(0, state);
        assertEquals(true, action instanceof DiscardCard);

        DiscardCard discardCard = (DiscardCard) action;
        assertEquals(0, discardCard.slot);
    }

    @Test
    public void willDiscardSafeCardColourAndValue(){
        state.setTableValue(CardColour.BLUE, 3);

        state.getHand(0).setCard(0, new Card(2, CardColour.BLUE));
        state.getHand(0).setKnownColour(CardColour.BLUE, new Integer[]{0});
        state.getHand(0).setKnownValue(2, new Integer[]{0});

        assertEquals(true, instance.canFire(0, state));

        Action action = instance.execute(0, state);
        assertEquals(true, action != null);
        assertEquals(true, action instanceof DiscardCard);
        DiscardCard discardCard = (DiscardCard) action;

        assertEquals(0, discardCard.slot);
    }

    @Test
    public void willNotDiscardWhenNoSafeCard(){
        for(CardColour colour : CardColour.values()){
            state.getHand(0).setCard(colour.ordinal(), new Card(1, colour));
            state.getHand(0).setKnownColour(colour, new Integer[]{colour.ordinal()});
            state.getHand(0).setKnownValue(1, new Integer[]{colour.ordinal()});
        }

        // Knows all about its ones - all playable
        assertEquals(false, instance.canFire(0, state));

    }
}
