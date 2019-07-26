package com.fossgalaxy.games.fireworks.cluster;

import com.fossgalaxy.games.fireworks.App;
import com.fossgalaxy.games.fireworks.GameStats;
import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;
import com.fossgalaxy.games.fireworks.utils.GameUtils;
import com.fossgalaxy.games.fireworks.utils.SetupUtils;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;

/**
 * A runner capable of playing the games for every legal hand size
 * <p>
 * This allows injecting of new types of predicition model to look at the
 * effects of imperfect prediction.
 */
public class PredictorRunnerSingle {
    private static final String SEPERATOR = "###########################";
    private static final String GAME_NORMAL = "normal";
    private static final String GAME_CHEAT = "cheat";

    private PredictorRunnerSingle() {

    }

    public static void main(String[] args) {
        // System.out.println("Start at PredictorRunningSingle");
        // int repeats = SetupUtils.getRepeatCount();
        int repeats = 5;

        /*
         * Arguments for script: agentUnderTest - main agent, agentPaired - other agents
         */
        String agentUnderTest = args[0];
        String agentPaired = args[1];
        long seed = Long.parseLong(args[2]);
        String gameType = args.length > 3 ? args[3] : GAME_NORMAL;
        String predictorType = args.length > 4 ? args[4] : agentPaired;

        Random random = new Random(seed);

        String taskId = System.getenv("SGE_TASK_ID");
        PrintStream log = System.err;

        // perform a warmup game because jvms... mumble mumble...
        // doWarmup(5, 1, agentUnderTest, predictorType, agentPaired);

        for (int run = 0; run < repeats; run++) {
            for (int nPlayers = 2; nPlayers <= 2; nPlayers++) {
                int agentUnderTestIndex = random.nextInt(nPlayers);

                // figure out if we need to generate a taskID or if one was provided by the
                // runner
                String gameID;
                if (taskId == null) {
                    gameID = String.format("%d-%s-%s-%d-%d", seed, agentUnderTest, agentPaired, nPlayers, run);
                } else {
                    // the same taskId can correspond to multiple games - this helps us track what
                    // run the taskID was for
                    gameID = String.format("%s-%d-%d", taskId, nPlayers, run);
                }

                log.println(SEPERATOR);
                log.println("# begin game " + gameID);
                log.println(SEPERATOR);

                Agent[] agents = new Agent[nPlayers];
                String[] agentStr = new String[5];

                // generate agent under test
                String realAgentUnderTest = generatePredictorString(agentUnderTest, nPlayers, predictorType);

                // generate the agent list
                agents[agentUnderTestIndex] = AgentUtils.buildAgent(realAgentUnderTest);
                agentStr[agentUnderTestIndex] = agentUnderTest;
                for (int i = 0; i < nPlayers; i++) {
                    if (i == agentUnderTestIndex) {
                        continue;
                    }
                    agents[i] = AgentUtils.buildAgent(agentPaired);
                    agentStr[i] = agentPaired;
                }

                // figure out what kind of game we are playing
                GameStats stats;
                if (gameType.equals(GAME_CHEAT)) {
                    stats = GameUtils.runCheatGame(gameID, seed, SetupUtils.toPlayers(agentStr, agents));
                } else if (SetupUtils.isZeroLifeVersion()) {
                    stats = GameUtils.runZeroLifeGame(gameID, seed, SetupUtils.toPlayers(agentStr, agents));
                } else {
                    stats = GameUtils.runGame(gameID, seed, SetupUtils.toPlayers(agentStr, agents));
                }

                // // ensure that agent names are escaped for the CSV file
                // String[] agentStrEscape = new String[agentStr.length];
                // for (int i = 0; i < agentStr.length; i++) {
                // agentStrEscape[i] = String.format("\"%s\"", agentStr[i]);
                // }

                // generate the CSV file name
                // String agentList = String.join(",", Arrays.asList(agentStrEscape));
                // String csvLine =
                // String.format("%s,\"%s\",\"%s\",%s,%s,%d,%d,%d,%d,%d,%d,%d,\"%s\"", gameID,
                // agentUnderTest, agentPaired, gameType, agentList, seed, stats.nPlayers,
                // stats.information,
                // stats.lives, stats.moves, stats.score, stats.disqal, predictorType);
                // System.out.println(csvLine);

                log.println(SEPERATOR);
                log.println("# end game " + gameID);
                log.println(SEPERATOR);
            }
        }
    }

    public static String makeCSVLine(String gameID, String agentUnderTest, String agentPaired, String gameType,
            String[] agentStr, long seed, GameStats stats, String predictorType) {
        String agentList = String.join(",", Arrays.asList(agentStr));
        return String.format("%s,\"%s\",\"%s\",%s,%s,%d,%d,%d,%d,%d,%d,%d,\"%s\"", gameID, agentUnderTest, agentPaired,
                gameType, agentList, seed, stats.nPlayers, stats.information, stats.lives, stats.moves, stats.score,
                stats.disqal, predictorType);
    }

    public static String generatePredictorString(String agentUnderTest, int nPlayers, String model) {

        if ("pmcts".equalsIgnoreCase(agentUnderTest) || "pmctsND".equalsIgnoreCase(agentUnderTest)) {
            StringBuilder builder = new StringBuilder();
            builder.append(agentUnderTest);
            builder.append(AgentUtils.PARAM_START);
            for (int i = 0; i < nPlayers; i++) {
                if (i != 0) {
                    builder.append("|");
                }
                builder.append(model);
            }
            builder.append(AgentUtils.PARAM_END);
            return builder.toString();
        }
        return agentUnderTest;
    }

    public static void doWarmup(int nPlayers, int agentUnderTestIndex, String agentUnderTest, String predictorType,
            String agentPaired) {

        Agent[] agents = new Agent[nPlayers];
        String[] agentStr = new String[5];

        // generate agent under test
        String realAgentUnderTest = agentUnderTest;
        if ("pmcts".equalsIgnoreCase(agentUnderTest) || "pmctsND".equalsIgnoreCase(agentUnderTest)) {
            StringBuilder builder = new StringBuilder();
            builder.append(agentUnderTest);
            builder.append(AgentUtils.PARAM_START);
            for (int i = 0; i < nPlayers; i++) {
                if (i != 0) {
                    builder.append("|");
                }
                builder.append(predictorType);
            }
            builder.append(AgentUtils.PARAM_END);
            realAgentUnderTest = builder.toString();
        }

        agents[agentUnderTestIndex] = AgentUtils.buildAgent(realAgentUnderTest);
        agentStr[agentUnderTestIndex] = agentUnderTest;
        for (int i = 0; i < nPlayers; i++) {
            if (i == agentUnderTestIndex) {
                continue;
            }
            agents[i] = AgentUtils.buildAgent(agentPaired);
            agentStr[i] = agentPaired;
        }

        GameUtils.runGame("WARMUP", 0l, SetupUtils.toPlayers(agentStr, agents));
    }

}
