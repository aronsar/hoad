package com.fossgalaxy.games.fireworks.cluster;

import com.fossgalaxy.games.fireworks.GameStats;
import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;
import com.fossgalaxy.games.fireworks.utils.GameUtils;
import com.fossgalaxy.games.fireworks.utils.SetupUtils;
import org.slf4j.MDC;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;

/**
 * A runner capable of playing the games for every legal hand size
 * <p>
 * This runner paired with
 */
public class MixedAgentGameSingle {
    private static final String SEPERATOR = "###########################";
    private static final String GAME_NORMAL = "normal";
    private static final String GAME_CHEAT = "cheat";

    private MixedAgentGameSingle() {

    }

    public static void main(String[] args) {

        int repeats = SetupUtils.getRepeatCount();

        //arguments for script
        String agentUnderTest = args[0];
        String agentPaired = args[1];
        long seed = Long.parseLong(args[2]);
        String gameType = args.length > 3 ? args[3] : GAME_NORMAL;


        Random random = new Random(seed);

        String taskId = System.getenv("SGE_TASK_ID");
        PrintStream log = System.err;

        MDC.put("seed", Long.toString(seed));
        MDC.put("agentUnderTest",agentUnderTest);
        MDC.put("agentPaired", agentPaired);
        MDC.put("gameType", gameType);
        MDC.put("taskId", taskId);

        //perform a warmup game because jvms... mumble mumble...
        PredictorRunnerSingle.doWarmup(5, 1, agentUnderTest, agentPaired, agentPaired);

        for (int run = 0; run < repeats; run++) {
            for (int nPlayers = 2; nPlayers <= 5; nPlayers++) {
                int agentUnderTestIndex = random.nextInt(nPlayers);

                //figure out if we need to generate a taskID or if one was provided by the runner
                String gameID;
                if (taskId == null) {
                    gameID = String.format("%d-%s-%s-%d-%d", seed, agentUnderTest, agentPaired, nPlayers, run);
                } else {
                    //the same taskId can correspond to multiple games - this helps us track what run the taskID was for
                    gameID = String.format("%s-%d-%d", taskId, nPlayers, run);
                }

                log.println(SEPERATOR);
                log.println("# begin game " + gameID);
                log.println(SEPERATOR);

                Agent[] agents = new Agent[nPlayers];
                String[] agentStr = new String[5];

                //generate agent under test
                String realAgentUnderTest = PredictorRunnerSingle.generatePredictorString(agentUnderTest, nPlayers, agentPaired);

                //generate agent under test
                agents[agentUnderTestIndex] = AgentUtils.buildAgent(realAgentUnderTest);
                agentStr[agentUnderTestIndex] = agentUnderTest;
                for (int i = 0; i < nPlayers; i++) {
                    if(i == agentUnderTestIndex){
                        continue;
                    }
                    agents[i] = AgentUtils.buildAgent(agentPaired);
                    agentStr[i] = agentPaired;
                }

                GameStats stats;
                if (gameType.equals(GAME_CHEAT)) {
                    stats = GameUtils.runCheatGame(gameID, seed, SetupUtils.toPlayers(agentStr, agents));
                } else if (SetupUtils.isZeroLifeVersion()) {
                    stats = GameUtils.runZeroLifeGame(gameID, seed, SetupUtils.toPlayers(agentStr, agents));
                } else {
                    stats = GameUtils.runGame(gameID, seed, SetupUtils.toPlayers(agentStr, agents));
                }

                //ensure that agent names are escaped for the CSV file
                String[] agentStrEscape = new String[agentStr.length];
                for (int i=0; i<agentStr.length; i++) {
                    agentStrEscape[i] = String.format("\"%s\"", agentStr[i]);
                }

                String agentList = String.join(",", Arrays.asList(agentStrEscape));
                String csvLine = String.format("%s,\"%s\",\"%s\",%s,%s,%d,%d,%d,%d,%d,%d,%d",
                        gameID,
                        agentUnderTest,
                        agentPaired,
                        gameType,
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

}
