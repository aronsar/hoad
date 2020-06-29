package com.fossgalaxy.games.fireworks.utils;

import com.fossgalaxy.games.fireworks.App;
import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.AgentPlayer;
import com.fossgalaxy.games.fireworks.players.Player;

/**
 * Utility Methods for setting up game runners.
 */
public final class SetupUtils {

    private static final String[] DEFAULT_PAIRED = {"iggi", "internal", "outer", "legal_random", "vdb-paper", "flawed", "piers"};
    private static final String[] DEFAULT_AGENTS = {"iggi", "outer", "flatmc[legal_random]", "piers", "flatmc[flawed]", "flawed", "vdb-paper", "flatmc[iggi]", "mctsPolicyND[flawed]", "mctsPolicyND[legal_random]", "mctsPolicyND[iggi]", "legal_random", "mctsND", App.PREDICTOR_MCTSND};
    private static final Integer DEFAULT_SEED_COUNT = 250;
    private static final Integer DEFAULT_REPEAT_COUNT = 3;

    private SetupUtils() {

    }

    public static boolean isZeroLifeVersion() {
        String env = System.getenv("FIREWORKS_ZERO_LIFE");
        if (env == null){
            return false;
        }
        return env.equalsIgnoreCase("yes");
    }

    /**
     * Get the list of agents to use for evaluation.
     * <p>
     * This can be overridden using the environment variable FIREWORKS_AGENTS. These names will be passed into the
     * agent factory in order to be created.
     * <p>
     * These are the agents that are to be considered under test.
     *
     * @return A list of agent names to be used in the evaluation
     */
    public static String[] getAgentNames() {
        String[] agentNames = DEFAULT_AGENTS;
        String envAgents = System.getenv("FIREWORKS_AGENTS");
        if (envAgents != null) {
            agentNames = envAgents.split(",");
        }
        return agentNames;
    }

    /**
     * Get the list of agents to use for pairing with evaluation.
     * <p>
     * These are the agents that should be used as the agents to evaluate the agents under test with. These can be
     * set using the FIREWORKS_AGENTS_PAIRED environment variable - if this is not set, it will use some sane defaults.
     * <p>
     * These are the agents that are used for testing the agents.
     *
     * @return a list of agent names to be paired with for evaluation
     */
    public static String[] getPairedNames() {
        String[] agentNames = DEFAULT_PAIRED;
        String envAgents = System.getenv("FIREWORKS_AGENTS_PAIRED");
        if (envAgents != null) {
            agentNames = envAgents.split(",");
        }
        return agentNames;
    }


    /**
     * Get the number of seeds we should be using.
     * <p>
     * You can set this using the FIREWORKS_NUM_SEEDS envrioment variable, if not set it will return a sensible default.
     *
     * @return the number of seeds to use for this run.
     */
    public static int getSeedCount() {
        String numSeedsEnv = System.getenv("FIREWORKS_NUM_SEEDS");
        if (numSeedsEnv != null) {
            return Integer.parseInt(numSeedsEnv);
        }
        return DEFAULT_SEED_COUNT;
    }

    /**
     * Get the default number of repeats to do.
     * <p>
     * This controls how many times a game should be repeated, the environment variable FIREWORKS_REPEAT_COUNT controls
     * this value. If not set, it will default to a sane value.
     * <p>
     * A repeat is the same agents running on the same seed.
     *
     * @return the number of runs to do.
     */
    public static int getRepeatCount() {
        String runCountEnv = System.getenv("FIREWORKS_REPEAT_COUNT");
        if (runCountEnv != null) {
            return Integer.parseInt(runCountEnv);
        }
        return DEFAULT_REPEAT_COUNT;
    }


    public static Player[] toPlayers(String[] names, Agent[] policies) {
        assert names.length == policies.length : "agent names and policies are different lengths";

        Player[] players = new Player[policies.length];
        for (int i = 0; i < players.length; i++) {
            players[i] = new AgentPlayer(names[i], policies[i]);
        }
        return players;
    }

}
