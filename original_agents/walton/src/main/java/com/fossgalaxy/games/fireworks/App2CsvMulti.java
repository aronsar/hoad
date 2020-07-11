package com.fossgalaxy.games.fireworks;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;

import java.util.Random;

/**
 * A runner capable of playing the games for every legal hand size
 */
@Deprecated
public class App2CsvMulti {

    // utility class - no instances permitted.
    private App2CsvMulti() {

    }

    public static void main(String[] args) {
        System.out.println("Start at App2CsvMulti");
        int runCount = App2Csv.DEFAULT_NUM_RUNS;

        // allow setting of run count via env variable
        String runCountEnv = System.getenv("FIREWORKS_RUN_COUNT");
        if (runCountEnv != null) {
            runCount = Integer.parseInt(runCountEnv);
        }

        // agents which will be playing
        String[] agentNames = App2Csv.AGENT_NAMES;
        String envAgents = System.getenv("FIREWORKS_AGENTS");
        if (envAgents != null) {
            agentNames = envAgents.split(",");
        }

        Random random = new Random();

        System.out.println("name,seed,players,information,lives,moves,score");
        for (int i = 2; i <= 5; i++) {
            for (int run = 0; run < runCount; run++) {

                long seed = random.nextLong();
                Agent[] agents = new Agent[i];
                String[] agentStr = new String[5];

                for (String name : agentNames) {

                    // populate list of agents
                    for (int agent = 0; agent < agents.length; agent++) {
                        agents[agent] = AgentUtils.buildAgent(name);
                        agentStr[agent] = name;
                    }

                    App2Csv.playGame(agentStr[0], agentStr, seed, agents);
                }

            }
        }
    }

}
