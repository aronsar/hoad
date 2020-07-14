package com.fossgalaxy.games.fireworks.cluster;

import com.fossgalaxy.games.fireworks.GameStats;
import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;
import com.fossgalaxy.games.fireworks.utils.GameUtils;
import com.fossgalaxy.games.fireworks.utils.SetupUtils;

import java.io.PrintStream;
import java.util.Arrays;

/**
 * Created by newowner on 21/12/2016.
 */
public class ValidationGameSingle {
    private static final String SEPERATOR = "###########################";

    private ValidationGameSingle() {

    }

    public static void main(String[] args) {
        int repeats = SetupUtils.getRepeatCount();
        String agentName = args[0];
        int numPLayers = Integer.parseInt(args[1]);
        long seed = Long.parseLong(args[2]);

        String taskId = System.getenv("SGE_TASK_ID");
        PrintStream log = System.err;

        for (int run = 0; run < repeats; run++) {
            String gameID;
            if (taskId == null) {
                gameID = String.format("%d-%s-%d-%d", seed, agentName, numPLayers, run);
            } else {
                //the same taskId can correspond to multiple games - this helps us track what run the taskID was for
                gameID = String.format("%s-%d-%d", taskId, numPLayers, run);
            }

            log.println(SEPERATOR);
            log.println("# begin game " + gameID);
            log.println(SEPERATOR);

            Agent[] agents = new Agent[numPLayers];
            String[] agentStr = new String[5];

            for (int i = 0; i < numPLayers; i++) {
                agents[i] = AgentUtils.buildAgent(agentName);
                agentStr[i] = agentName;
            }

            GameStats stats = GameUtils.runGame(gameID, seed, SetupUtils.toPlayers(agentStr, agents));
            String agentList = String.join(",", Arrays.asList(agentStr));
            String csvLine = String.format("%s,%s,%s,%d,%d,%d,%d,%d,%d,%d",
                    gameID,
                    agentName,
                    agentList,
                    seed,
                    stats.nPlayers,
                    stats.information,
                    stats.lives,
                    stats.moves,
                    stats.score,
                    stats.disqal
            );
            System.out.println(csvLine);

            log.println(SEPERATOR);
            log.println("# end game " + gameID);
            log.println(SEPERATOR);
        }
    }
}
