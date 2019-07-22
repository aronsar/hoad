package com.fossgalaxy.games.fireworks.ai.osawa;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.*;

import java.util.Random;

/**
 * Make a random (possibly illegal) move.
 * <p>
 * Random biases as presented in 10167-45957-1-PB
 */
public class RandomAgent implements Agent {
    private Random random;

    public RandomAgent() {
        this.random = new Random();
    }

    @Override
    public Action doMove(int agentID, GameState state) {
        double selection = random.nextInt(10);

        //0-2 -> infomation
        //3-6 -> discard
        //7-9 -> play
        if (selection < 3) {

            //coin toss for Value/Colour
            if (random.nextBoolean()) {
                return new TellValue(random.nextInt(state.getPlayerCount()), random.nextInt(5) + 1);
            } else {
                CardColour[] possible = CardColour.values();
                int selected = random.nextInt(possible.length);
                return new TellColour(random.nextInt(state.getPlayerCount()), possible[selected]);
            }

        } else if (selection < 7) {
            return new DiscardCard(random.nextInt(state.getHandSize()));
        } else {
            return new PlayCard(random.nextInt(state.getHandSize()));
        }
    }

    @Override
    public String toString() {
        return "RandomAgent";
    }

}
