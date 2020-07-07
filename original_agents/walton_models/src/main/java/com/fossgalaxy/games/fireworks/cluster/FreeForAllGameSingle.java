package com.fossgalaxy.games.fireworks.cluster;

import com.fossgalaxy.games.fireworks.App;
import com.fossgalaxy.games.fireworks.App2Csv;
import com.fossgalaxy.games.fireworks.ai.Agent;

/**
 * A runner capable of playing the games
 * <p>
 * This runner allows setting of all agent positions and hand size
 */
public class FreeForAllGameSingle {

    private FreeForAllGameSingle() {

    }

    public static void main(String[] args) {

        int runCount = App2Csv.DEFAULT_NUM_RUNS;
        //allow setting of run count via env variable
        String runCountEnv = System.getenv("FIREWORKS_RUN_COUNT");
        if (runCountEnv != null) {
            runCount = Integer.parseInt(runCountEnv);
        }

        //new format for FFA games <gameid> <numplayers> <seed> <p1> <p2> [p3] [p4] [p5]
        String gameID = args[0];
        int numPlayers = Integer.parseInt(args[1]);
        long seed = Long.parseLong(args[2]);

        int preambleArgs = 3; //number of args before player names

        String[] agentStr = new String[5];
        for (int i = 0; i < 5; i++) {
            if (i < numPlayers) {
                agentStr[i] = args[i + preambleArgs];
            } else {
                agentStr[i] = null;
            }
        }

        int repeats = 2;
        for (int j = 0; j < repeats; j++) {
            for (int run = 0; run < runCount; run++) {
                Agent[] agents = new Agent[numPlayers];

                //generate agent under test
                for (int i = 0; i < numPlayers; i++) {
                    agents[i] = App.buildAgent(agentStr[i], i, agentStr);
                }

                //System.out.println("name,seed,players,information,lives,moves,score");
                App2Csv.playGameErrTrace(gameID, agentStr[0], agentStr, seed, agents);
            }
        }
    }

}
