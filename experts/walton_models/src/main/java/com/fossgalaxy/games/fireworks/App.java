package com.fossgalaxy.games.fireworks;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.iggi.IGGIFactory;
import com.fossgalaxy.games.fireworks.ai.mcts.MCTS;
import com.fossgalaxy.games.fireworks.ai.mcts.MCTSNode;
import com.fossgalaxy.games.fireworks.ai.mcts.MCTSPredictor;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;
import com.fossgalaxy.games.fireworks.utils.GameUtils;
import com.fossgalaxy.games.fireworks.utils.SetupUtils;
import com.fossgalaxy.stats.BasicStats;
import com.fossgalaxy.stats.StatsSummary;

import java.util.Random;

/**
 * Hello world!
 */
public class App {

    public static final String PREDICTOR_MCTS = "pmcts";
    public static final String IGGI_RISKY = "iggi_risky";
    public static final String MCTS = "mcts";
    public static final String PREDICTOR_MCTSND = "pmctsND";

    //utility class - don't create instances of it
    private App() {

    }

    /**
     * Plays a series of games with a single agent mixed with another agent
     *
     * @param args Ignored
     */
    public static void main(String[] args) {

        double sum = 0;
        int games = 0;
        System.out.println("Start");

        Random r = new Random();

        String[] agents = new String[] {
            "iggi", "piers", "flawed", "outer", "vdb-paper", "legal_random"
        };

        long[] seeds = new long[100];


        //values from the IS-MCTS paper
        double[] p = new double[] {
                0,
                0.25,
                0.5,
                0.75,
                1,
                1.25,
                1.5,
                1.75,
                2
        };

        /*double[] p = new double[]{
            0.0,
            0.1,
            0.2,
            0.3,
            0.4,
            0.5,
            0.6,
            0.7,
            0.8,
            0.9,
            1.0,
            2.0,
            3.0,
            4.0,
            5.0,
            6.0,
            7.0
        };*/

        StatsSummary[][] ss = new StatsSummary[agents.length][p.length];
        for (int j=0; j<agents.length; j++) {
            for (int i = 0; i < ss.length; i++) {
                ss[j][i] = new BasicStats();
            }
        }

        for (int i=0; i<seeds.length; i++) {
            seeds[i] = r.nextLong();
        }

        for (int agent=0; agent<agents.length; agent++) {
            for (int i = 0; i < p.length; i++) {

                for (int run = 0; run < seeds.length; run++) {
                    GameStats stats = playMixed("mctsExpConstND["+p[i]+"]", agents[agent], seeds[i]);
                    sum += stats.score;
                    games++;
                    ss[agent][i].add(stats.score);
                    System.out.println(String.format("line,%f,%s,%d,%d,%d,%d,%d", p[i], agents[agent], seeds[i], stats.score, stats.lives, stats.moves, stats.disqal));
                }

                if (games == 0) {
                    return;
                }

                System.out.println("exp: " + p[i] + "avg: " + sum / games);
                System.out.println("exp: " + p[i] + " stats: " + ss[i]);

                StatsSummary ssi = ss[agent][i];
                System.out.println(String.format("summary,%f,%s,%f,%f,%f,%f", p[i], agents[agent], ssi.getMin(), ssi.getMax(), ssi.getMean(), ssi.getRange()));
            }
        }
    }

    /**
     * Plays a game with the given agent
     *
     * @param agent The given agent to play the game
     * @return GameStats for the game.
     */
    public static GameStats playGame(String agent) {
        String[] names = new String[5];
        Agent[] players = new Agent[5];

        for (int i = 0; i < 5; i++) {
            names[i] = agent;
            players[i] = AgentUtils.buildAgent(names[i]);
        }

        GameStats stats = GameUtils.runGame("", null, SetupUtils.toPlayers(names, players));
        System.out.println("the agents scored: " + stats);
        return stats;
    }

    /**
     * Plays a mixed game with the agent under test and all other agents as the agent
     *
     * @param agentUnderTest The agent to be player 0
     * @param agent          The agent to make all the others
     * @param seed the seed for the deck ordering
     * @return GameStats for the game
     */
    public static GameStats playMixed(String agentUnderTest, String agent, long seed) {
        Random r = new Random(seed);
        int whereToPlace = r.nextInt(5);

        String[] names = new String[5];
        for (int i = 0; i < names.length; i++) {
            names[i] = whereToPlace == i ? agentUnderTest : agent;
        }

        Agent[] players = new Agent[5];
        for (int i = 0; i < names.length; i++) {
            players[i] = buildAgent(names[i], i, agent, names.length);
        }

        GameStats stats = GameUtils.runGame("", seed, SetupUtils.toPlayers(names, players));
        System.out.println("the agents scored: " + stats);
        return stats;
    }

    /**
     * Build an agent
     *
     * @param name    The name the agent will believe it has
     * @param agentID The AgentID it will have
     * @param paired  Who it is paired with
     * @param size    The size of the game
     * @return The agent created
     */
    public static Agent buildAgent(String name, int agentID, String paired, int size) {
        switch (name) {
            case PREDICTOR_MCTS:
            case PREDICTOR_MCTSND:
                Agent[] agents = AgentUtils.buildPredictors(agentID, size, paired);
                if (name.contains("ND")) {
                    return new MCTSPredictor(agents, 50_000, 100, 100);
                }
                return new MCTSPredictor(agents);
            default:
                return AgentUtils.buildAgent(name);
        }
    }

    /**
     * Build an agent
     *
     * @param name    The name the agent will believe it has
     * @param agentID The AgentID it will have
     * @param paired  Who it is paired with
     * @return The agent created
     */
    public static Agent buildAgent(String name, int agentID, String[] paired) {
        switch (name) {
            case PREDICTOR_MCTS:
            case PREDICTOR_MCTSND:
                Agent[] agents = AgentUtils.buildPredictors(agentID, paired);
                if (name.contains("ND")) {
                    return new MCTSPredictor(agents, 50_000, 100, 100);
                }
                return new MCTSPredictor(agents);
            default:
                return AgentUtils.buildAgent(name);
        }
    }

    public static Agent buildAgent(String name, int agentID, int[] model){
        switch (name){
            case PREDICTOR_MCTS:
            case PREDICTOR_MCTSND:
                Agent[] agents = new Agent[5];
                for(int i = 0; i < agents.length; i++){
                    if(i != agentID) {
                        agents[i] = AgentUtils.buildAgent(model);
                    }
                }
                if(name.contains("ND")){
                    return new MCTSPredictor(agents, 50_000, 100, 100);
                }
                return new MCTSPredictor(agents);
            default:
                return AgentUtils.buildAgent(name);
        }
    }


    /**
     * Allows for creating MCTS specifically with some fields
     *
     * @param name         The name of the agent
     * @param roundLength  The round length to use for MCTS
     * @param rolloutDepth The rollout depth to use for MCTS
     * @param treeDepth    The tree depth to use for MCTS
     * @return The Agent
     */
    public static Agent buildAgent(String name, int roundLength, int rolloutDepth, int treeDepth) {
        return MCTS.equals(name) ? new MCTS(roundLength, rolloutDepth, treeDepth) : AgentUtils.buildAgent(name);
    }

    /**
     * Allows for creating Predictor MCTS with some fields
     *
     * @param name         The name for the agent
     * @param agentID      The agent id
     * @param paired       Who the agent is paired with
     * @param size         The size of the game
     * @param roundLength  The round length to use for MCTS
     * @param rolloutDepth The rollout depth to use for MCTS
     * @param treeDepth    The tree depth to use for MCTS
     * @return The agent
     */
    public static Agent buildAgent(String name, int agentID, String paired, int size, int roundLength, int rolloutDepth, int treeDepth) {
        if (!PREDICTOR_MCTS.equals(name)) {
            return AgentUtils.buildAgent(name);
        }
        Agent[] agents = new Agent[size];
        for (int i = 0; i < size; i++) {
            if (i == agentID) {
                agents[i] = null;
            }
            //TODO is this ever paired with MCTS? if not this should be AgentUtils.buildAgent(agentID, size, paired)
            agents[i] = buildAgent(paired, roundLength, rolloutDepth, treeDepth);
        }
        return new MCTSPredictor(agents);
    }

    /**
     * Builds a risky agent with a given threshold
     *
     * @param name      The name for the agent
     * @param threshold The threshold to give to the agent
     * @return The agent
     */
    public static Agent buildAgent(String name, double threshold) {
        return IGGI_RISKY.equals(name) ? IGGIFactory.buildRiskyPlayer(threshold) : AgentUtils.buildAgent(name);
    }


}
