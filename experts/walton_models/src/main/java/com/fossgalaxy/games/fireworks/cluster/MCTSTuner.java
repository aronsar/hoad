package com.fossgalaxy.games.fireworks.cluster;

import com.fossgalaxy.games.fireworks.App;
import com.fossgalaxy.games.fireworks.App2Csv;
import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;

/**
 * Created by piers on 18/10/16.
 */
public class MCTSTuner {

    private MCTSTuner() {

    }

    /*
    Pass in the same arguments as MixedAgentGameSingle

    Though it will ignore the first argument.
     */
    public static void main(String[] args) {
        String agentPaired = args[1];
        long seed = Long.parseLong(args[2]);

        int runCount = App2Csv.DEFAULT_NUM_RUNS;
        String runCountEnv = System.getenv("FIREWORKS_RUN_COUNT");
        if (runCountEnv != null) {
            runCount = Integer.parseInt(runCountEnv);
        }

        for (int run = 0; run < runCount; run++) {
            for (int treeDepthMul = 1; treeDepthMul <= 3; treeDepthMul++) {
                for (int roundLength = 5000; roundLength <= 50_000; roundLength += 5000) {
                    for (int rolloutDepth = 5; rolloutDepth < 25; rolloutDepth += 5) {
                        for (int nPlayers = 2; nPlayers <= 5; nPlayers++) {
                            Agent[] agents = new Agent[nPlayers];
                            String[] agentStr = new String[5];
                            agentStr[0] = "mcts";

                            //Piers, this should emulate what you were doing with the mcts params :)
                            //I added a gameID field to make re-combining easier (populate it with SGE_TASK_ID for the
                            //other runners, for your use case this should be fine).
                            String gameID = String.format("%d,%d,%d", treeDepthMul, rolloutDepth, rolloutDepth);
                            agents[0] = App.buildAgent("predictorMCTS", 0, agentPaired, nPlayers, roundLength, rolloutDepth, treeDepthMul);
                            for (int agent = 1; agent < nPlayers; agent++) {
                                agents[agent] = AgentUtils.buildAgent(agentPaired);
                                agentStr[agent] = agentPaired;
                            }
                            App2Csv.playGameErrTrace(gameID, "predictorMCTS", agentStr, seed, agents);
                        }
                    }
                }
            }
        }
    }
}
