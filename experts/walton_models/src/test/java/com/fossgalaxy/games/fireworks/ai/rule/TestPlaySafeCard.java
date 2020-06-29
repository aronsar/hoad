package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 22/11/16.
 */
public class TestPlaySafeCard {

    @Test
    public void testPlaySafeCard(){
        // Any card that is definately playable - even if we don't know it all
        BasicState state = new BasicState(2);
        state.init();
        RuleUtils.setHasCards(state);

        int value = 1;

        // This card is playable
        state.getHand(0).setCard(0, new Card(value, CardColour.BLUE));

        // This card is a one - the table is clear so it is playable
        state.getHand(0).setKnownValue(value, new Integer[]{0});
        PlaySafeCard instance = new PlaySafeCard();

        assertEquals(true, instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertEquals(true, action instanceof PlayCard);
        PlayCard playCard = (PlayCard) action;
        assertEquals(0, playCard.slot);
    }

    @Test
    public void testPlaySafeCardDoesNotPlayWhenNoSafeCard(){
        BasicState state = new BasicState(2);
        PlaySafeCard instance = new PlaySafeCard();

        state.init();

        state.getHand(1).setCard(0, new Card(2, CardColour.BLUE));
        state.getHand(1).setCard(1, new Card(2, CardColour.BLUE));
        state.getHand(1).setCard(2, new Card(3, CardColour.BLUE));
        state.getHand(1).setCard(3, new Card(3, CardColour.BLUE));
        state.getHand(1).setCard(4, new Card(4, CardColour.BLUE));

        assertEquals(false, instance.canFire(1, state));
        Action action = instance.execute(1, state);
        assertEquals(null, action);
    }
}
