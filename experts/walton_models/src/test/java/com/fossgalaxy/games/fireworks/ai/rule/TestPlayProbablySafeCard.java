package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.ai.rule.random.PlayProbablySafeCard;
import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 24/11/16.
 */
@RunWith(JUnitParamsRunner.class)
public class TestPlayProbablySafeCard {

    private BasicState state;
    private PlayProbablySafeCard instance;

    @Before
    public void setup() {
        state = new BasicState(2);
        state.init();
        instance = new PlayProbablySafeCard();
    }

    // Make it so that there are fives down and a single 4? otherwise this will always fire
    public Object[] parametersForTestPlayProbablySafeCard() {
        return $(
                $(0, .2, true), $(0, .4, false), $(0, .6, false), $(0, .8, false), $(0, 1, false), // 5 fives in deck, one is playable 20%
                $(1, .2, true), $(1, .4, false), $(1, .6, false), $(1, .8, false), $(1, 1, false), // 4 fives in deck, one is playable 25%
                $(2, .2, true), $(2, .4, false), $(2, .6, false), $(2, .8, false), $(2, 1, false), // 3 fives in deck, one is playable 33%
                $(3, .2, true), $(3, .4, true), $(3, .6, false), $(3, .8, false), $(3, 1, false), // 2 fives in deck, one is playable 50%
                $(4, .2, true), $(4, .4, true), $(4, .6, true), $(4, .8, true), $(4, 1, true), // 1 five in deck, one is playable 100%
                $(5, .2, false), $(5, .4, false), $(5, .6, false), $(5, .8, false), $(5, 1, false) // 0 fives in deck, none are playable 0%
        );
    }

    // Need to create certain probabilities
    @Test
    @Parameters(method = "parametersForTestPlayProbablySafeCard")
    public void testPlayProbablySafe(int fivesToFill, double threshold, boolean expected) {
        instance = new PlayProbablySafeCard(threshold);

        // Take all 5's out of the hands
        for(int i = 0; i < state.getPlayerCount(); i++){
            for(int slot = 0; slot < state.getHandSize(); slot++) {
                Card card = state.getHand(i).getCard(slot);
                if(card.value == 5){
                    // Change it for one in the deck
                    // Start with 1's and work up
                    outer:
                    for(int cardToFind = 1; cardToFind < 5; cardToFind++){
                        for(CardColour colourToFind : CardColour.values()) {
                            if(state.getDeck().toList().remove(new Card(cardToFind, colourToFind))){
                                // We removed a card - set it to the one in the hand and put the hand card in the deck
                                state.getHand(i).setCard(slot, new Card(cardToFind, colourToFind));
                                state.getDeck().add(new Card(card.value, card.colour));
                                break outer;
                            }
                        }
                    }
                }
            }
        }

        // No twos in the hands, so now all in the deck
        for (int i = 0; i < CardColour.values().length; i++) {
            CardColour colour = CardColour.values()[i];
            if(i < fivesToFill){
                state.setTableValue(colour, 5);
                // take this out the deck
                state.getDeck().remove(new Card(5, colour));
            }
        }
        if(fivesToFill < CardColour.values().length){
            state.setTableValue(CardColour.values()[fivesToFill], 4);
        }
        // For this to work - all 2's in hands must be swapped with the deck again
        // Also need to remove the twos we added to the table


        // They have a two and know it is a two
        state.getHand(0).setCard(0, new Card(5, CardColour.BLUE));
        state.getHand(0).setKnownValue(5, new Integer[]{0});


        boolean canFire = instance.canFire(0, state);
        if(canFire != expected){
            System.out.println(fivesToFill + " " + threshold + " " + expected);
        }
        assertEquals(expected, instance.canFire(0, state));
    }
}
