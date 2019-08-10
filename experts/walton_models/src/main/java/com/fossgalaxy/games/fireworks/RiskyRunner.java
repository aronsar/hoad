package com.fossgalaxy.games.fireworks;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.utils.GameUtils;
import com.fossgalaxy.games.fireworks.utils.SetupUtils;

import java.util.Random;

/**
 * Created by piers on 08/11/16.
 */
@Deprecated
public class RiskyRunner {

    public static final String IGGI_RISKY = "iggi_risky";

    private RiskyRunner() {

    }

    /**
     * Runs an experiment calculating for each threshold the average score a game of 2-5 players achieved
     * if they are all risky players.
     *
     * @param args Not used.
     */
    public static void main(String[] args) {

        int runCount = 1000;

        //allow setting of run count via env variable
        String runCountEnv = System.getenv("FIREWORKS_RUN_COUNT");
        if (runCountEnv != null) {
            runCount = Integer.parseInt(runCountEnv);
        }

        System.out.println("name,threshold,1,2,3,4,seed,players,information,lives,moves,score,disqual");
        for (double threshold = 0.1; threshold <= 1.0; threshold += 0.1) {

            Random random = new Random();

            for (int i = 2; i <= 5; i++) {
                for (int run = 0; run < runCount; run++) {
                    long seed = random.nextLong();
                    Agent[] agents = new Agent[i];
                    String[] agentStr = new String[5];

                    agents[0] = App.buildAgent(IGGI_RISKY, threshold);
                    agentStr[0] = IGGI_RISKY + "," + threshold;
                    //populate list of agents
                    for (int agent = 1; agent < agents.length; agent++) {
                        agents[agent] = App.buildAgent(IGGI_RISKY, threshold);
                        agentStr[agent] = "iggi";
                    }
                    GameStats stats = GameUtils.runGame(agentStr[0], seed, SetupUtils.toPlayers(agentStr, agents));
                    System.out.println(stats);

                }
            }
        }
    }
}
