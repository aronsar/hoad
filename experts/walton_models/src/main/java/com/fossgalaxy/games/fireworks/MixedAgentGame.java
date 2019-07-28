package com.fossgalaxy.games.fireworks;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;
import com.fossgalaxy.games.fireworks.utils.SetupUtils;

import java.util.Random;

/**
 * A runner capable of playing the games for every legal hand size
 * <p>
 * This runner paired with
 */
@Deprecated
public class MixedAgentGame {

    // utility class - no instances required.
    private MixedAgentGame() {

    }

    public static void main(String[] args) {
        System.out.println("Start at MixedAgentGame");
        int runCount = App2Csv.DEFAULT_NUM_RUNS;

        // allow setting of run count via env variable
        String runCountEnv = System.getenv("FIREWORKS_RUN_COUNT");
        if (runCountEnv != null) {
            runCount = Integer.parseInt(runCountEnv);
        }

        String[] agentNames = SetupUtils.getAgentNames();
        String[] agentsPaired = SetupUtils.getPairedNames();

        Random random = new Random();

        System.out.println("name,seed,players,information,lives,moves,score");
        for (String paired : agentsPaired) {
            for (int i = 2; i <= 5; i++) {
                for (int run = 0; run < runCount; run++) {

                    long seed = random.nextLong();
                    Agent[] agents = new Agent[i];
                    String[] agentStr = new String[5];

                    for (String name : agentNames) {

                        // populate list of agents
                        agents[0] = App.buildAgent(name, 0, paired, i);
                        agentStr[0] = name;
                        for (int agent = 1; agent < agents.length; agent++) {
                            agents[agent] = AgentUtils.buildAgent(paired);
                            agentStr[agent] = paired;
                        }

                        App2Csv.playGame(agentStr[0], agentStr, seed, agents);
                    }

                }
            }
        }
    }

}
