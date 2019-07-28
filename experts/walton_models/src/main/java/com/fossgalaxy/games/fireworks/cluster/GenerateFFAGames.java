package com.fossgalaxy.games.fireworks.cluster;

import com.fossgalaxy.games.fireworks.utils.SetupUtils;

import java.util.Random;

/**
 * Generate matchups between an agent and other agents.
 * The order of the agents in the group are randomised.
 */
public class GenerateFFAGames {

    private GenerateFFAGames() {

    }

    public static void main(String[] args) {
        String[] agentsUnderTest = SetupUtils.getAgentNames();
        String[] agentsPaired = SetupUtils.getPairedNames();

        //number of seeds
        String numSeedsEnv = System.getenv("FIREWORKS_NUM_SEEDS");
        int numSeeds = 10;
        if (numSeedsEnv != null) {
            numSeeds = Integer.parseInt(numSeedsEnv);
        }

        printMatchups(agentsUnderTest, agentsPaired, numSeeds);
    }

    static void printMatchups(String[] agentsUnderTest, String[] agentsPaired, int numSeeds) {


        //new format for FFA games <gameid> <numplayers> <seed> <p1> <p2> [p3] [p4] [p5]

        //allow generation of known seeds (useful for comparisons between pure and mixed games)
        Random r;
        String metaSeed = System.getenv("FIREWORKS_META_SEED");
        if (metaSeed != null) {
            r = new Random(Long.parseLong(metaSeed));
        } else {
            r = new Random();
        }


        for (int seedID = 0; seedID < numSeeds; seedID++) {
            long seed = r.nextLong();
            int numPlayers = r.nextInt(3) + 2;

            int myAgent = r.nextInt(numPlayers);

            for (String agentPaired : agentsPaired) {
                for (String agentUnderTest : agentsUnderTest) {

                    String[] agentStr = new String[numPlayers];
                    for (int i = 0; i < numPlayers; i++) {
                        agentStr[i] = (myAgent == i) ? agentUnderTest : agentPaired;
                    }

                    System.out.println(String.format("%d %d %s", numPlayers, seed, String.join(" ", agentStr)));
                }
            }
        }

    }

}
