package com.fossgalaxy.games.fireworks.human;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.iggi.Utils;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.utils.DebugUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by piers on 07/04/17.
 */
public class BasicHuman implements Agent {

    @Override
    public Action doMove(int agentID, GameState state) {
        // Print state
        System.out.println(calcBasicInfo(state));

        System.out.println("Discard: " + calcDiscard(state));

        for(int agent = 0; agent < state.getPlayerCount(); agent++){
            if(agent != agentID){
                System.out.println(calcHandOtherInfo(agent, state));
                System.out.println(calcHandKnownInfo(agent, state));
            }
        }

        System.out.println(calcHandKnownInfo(agentID, state));
        System.out.println(calcTable(state));

        // Obtain user input
        System.out.println("\n\nWhatcha doing?");

        ArrayList<Action> actions = new ArrayList<>(Utils.generateActions(agentID, state));
        int i = 0;
        for(Action action : actions){
            if(i % 5 == 0) System.out.println("");
            System.out.print(i + ":" + action + "\t");
            i++;
        }

        // Return that move
        Scanner scanner = new Scanner(System.in);
        if (scanner.hasNextInt()){
            int answer = scanner.nextInt();
            return actions.get(answer);
        }

        // Human said no
        return null;
    }

    private String calcTable(GameState state){
        return String.format("B:%s\tG:%s\tO:%s\tR:%s\tY:%s",
                state.getTableValue(CardColour.BLUE),
                state.getTableValue(CardColour.GREEN),
                state.getTableValue(CardColour.ORANGE),
                state.getTableValue(CardColour.RED),
                state.getTableValue(CardColour.WHITE)
        );
    }

    private String calcBasicInfo(GameState state) {
        return String.format("Information: %d Lives: %s Deck Size: %d",state.getInfomation(), state.getLives(), state.getDeck().getCardsLeft());
    }

    private String calcHandOtherInfo(int agentID, GameState state) {
        StringBuilder builder = new StringBuilder();
        Hand hand = state.getHand(agentID);
        builder.append("Player: ").append(agentID).append("has hand\t");
        for (int slot = 0; slot < hand.getSize(); slot++) {
            builder.append(String.format("%s", hand.getCard(slot)));
            builder.append("\t");
        }
        return builder.toString();
    }

    private String calcHandKnownInfo(int agentID, GameState state){
        StringBuilder builder = new StringBuilder();
        builder.append("Player: ").append(agentID).append("Knows: \t");
        Hand hand = state.getHand(agentID);
        for(int slot = 0; slot < hand.getSize(); slot++){
            builder.append(String.format("%s %s", hand.getKnownColour(slot), hand.getKnownValue(slot)));
            builder.append("\t");
        }
        return builder.toString();
    }

    private String calcDiscard(GameState state){
        StringBuilder builder = new StringBuilder();

        Map<Card, Long> cardCounts = DebugUtils.histogram(state.getDiscards());
        cardCounts.forEach((card, count) -> builder.append(String.format("\t%s x %s", card, count)));

        return builder.toString();
    }

}
