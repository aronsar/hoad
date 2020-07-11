package com.fossgalaxy.games.fireworks.ai.rule.random;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractTellRule;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Tell the next player about a card randomly.
 */
public class TellRandomly extends AbstractTellRule {
    private Random random;

    public TellRandomly() {
        this.random = new Random();
    }

    @Override
    public Action execute(int playerID, GameState state) {

        int nextAgent = selectPlayer(playerID, state);
        Hand hand = state.getHand(nextAgent);

        List<Card> possibleCards = new ArrayList<>();
        for (int slot=0; slot<hand.getSize(); slot++) {
            if (hand.hasCard(slot)) {
                possibleCards.add(hand.getCard(slot));
            }
        }

        if(possibleCards.isEmpty()) return null;

        Card card = possibleCards.get(random.nextInt(possibleCards.size()));

        //decide if we should describe the colour or number
        if (random.nextBoolean()) {
            return new TellValue(nextAgent, card.value);
        } else {
            return new TellColour(nextAgent, card.colour);
        }
    }

}
