package com.fossgalaxy.games.fireworks;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.AgentPlayer;
import com.fossgalaxy.games.fireworks.players.Player;
import com.fossgalaxy.games.fireworks.state.GameType;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

/**
 * @deprecated This class should be migrated away from
 */
@Deprecated
public class App2Csv {
    public static final Integer GAME_SIZE = 3;
    public static final Integer DEFAULT_NUM_RUNS = 100;

    // pure games - Careful.
    // protected static final String[] AGENT_NAMES = {"pure_random", "internal",
    // "outer", "legal_random", "cautious", "mctsND", "cautiousMCTSND"};

    protected static final String[] AGENT_NAMES = { "internal", "outer", "legal_random", "cautious", "mctsND" };

    // Utility class - instances not permitted.
    private App2Csv() {

    }

    public static void main(String[] args) {
        System.out.println("Start at App2Csv");
        int runCount = DEFAULT_NUM_RUNS;

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

        // play the games
        System.out.println("name,seed,players,information,lives,moves,score");

        for (int run = 0; run < runCount; run++) {
            Agent[] agents = new Agent[GAME_SIZE];
            String[] agentStr = new String[5]; // an array containing all 5 agent names

            // use the same seed for 1 game for all agents (for fairness)
            long seed = random.nextLong();

            for (int i = 0; i < agents.length; i++) {
                for (int agent = 0; agent < agents.length; agent++) {
                    agents[agent] = AgentUtils.buildAgent(agentNames[i]);
                    agentStr[agent] = agentNames[i];
                }

                playGame(agentStr[0], agentStr, seed, agents);
            }
        }
    }

    @Deprecated
    public static GameStats playGame(String agentUnderTest, String[] names, Long seed, Player... players) {
        UUID id = UUID.randomUUID();
        try (FileOutputStream fos = new FileOutputStream(String.format("trace_%s.csv", id));
                PrintStream ps = new PrintStream(fos)) {
            GameRunner runner = new GameRunner(id.toString(), players.length);

            for (int i = -0; i < players.length; i++) {
                runner.addPlayer(players[i]);
                players[i].setID(i, players.length, new String[5], GameType.NO_LIVES_CURRENT);
            }

            GameStats stats = runner.playGame(seed);
            ps.println("DEBUG,game is over");
            System.out.println(String.format("%s,%s,%d,%d,%d,%d,%d,%d,%d", agentUnderTest,
                    String.join(",", Arrays.asList(names)), seed, stats.nPlayers, stats.information, stats.lives,
                    stats.moves, stats.score, stats.disqal));
            return stats;
        } catch (IOException ex) {
            System.err.println("error: " + ex.toString());
        }
        return null;
    }

    /**
     * Play a standard Hanabi game with agents.
     *
     * This is a convenience wrapper around the player version of this function.
     *
     * @param gameID  The gameID to log this game under
     * @param agentUT agent under test
     * @param name    the names of the agents that are playing
     * @param seed    the seed to use for deck ordering
     * @param agents  the agents to use for the game
     * @return the outcome of the game
     */
    public static GameStats playGameErrTrace(String gameID, String agentUT, String[] name, Long seed, Agent... agents) {
        return playGameErrTrace(gameID, agentUT, name, seed, toPlayers(agents));
    }

    @Deprecated
    public static GameStats playGame(String agentUnderTest, String[] name, Long seed, Agent... agents) {
        return playGame(agentUnderTest, name, seed, toPlayers(agents));
    }

    /**
     * Wrap agents into players.
     *
     * The agent class provides a high level interface for the game that tracks game
     * state for the agent. Player is a lower level API used internally by the game
     * system that does not store any game state directly, this is to allow
     * networked or remote players in the future.
     *
     * For the agents to play the game, a wrapper is needed to convert the low level
     * (event based) player API into the higher level (polling based) API for
     * agents.
     *
     * @param agents the agents to convert
     * @return player versions of the agents
     */
    public static Player[] toPlayers(Agent... agents) {
        Player[] wrapper = new Player[agents.length];
        for (int i = 0; i < agents.length; i++) {
            wrapper[i] = new AgentPlayer(agents[i].getClass().getSimpleName(), agents[i]);
        }
        return wrapper;
    }

    /**
     * Play a standard version of hanabi.
     *
     * This is the default game rules that the game comes with.
     *
     * @param gameID  The gameID to log this game under
     * @param agentUT agent under test
     * @param name    the names of the agents that are playing
     * @param seed    the seed to use for deck ordering
     * @param players the players to use for the game
     * @return the outcome of the game
     */
    public static GameStats playGameErrTrace(String gameID, String agentUT, String[] name, Long seed,
            Player... players) {
        GameRunner runner = new GameRunner(gameID, players.length);
        return playGame(gameID, agentUT, name, seed, runner, players);
    }

    /**
     * Play a complete information (cheated) version of Hanabi
     *
     * @param gameID  The gameID to log this game under
     * @param agentUT agent under test
     * @param name    the names of the agents that are playing
     * @param seed    the seed to use for deck ordering
     * @param players the players to use for the game
     * @return the outcome of the game
     */
    public static GameStats playCheatGame(String gameID, String agentUT, String[] name, Long seed, Player... players) {
        GameRunner cheatRunner = new GameRunnerCheat(gameID, players.length);
        return playGame(gameID, agentUT, name, seed, cheatRunner, players);
    }

    /**
     * Utility function that handles running and logging of the game to stdout in
     * csv format
     *
     * @param gameID         The gameID to log this game under
     * @param agentUnderTest agent under test
     * @param name           the name of the agents that are playing
     * @param seed           the seed to use for the deck ordering
     * @param runner         the runner to use for this game
     * @param players        the players that will be playing this game
     * @return the outcome of the game
     */
    private static GameStats playGame(String gameID, String agentUnderTest, String[] name, Long seed, GameRunner runner,
            Player... players) {
        try {
            for (int i = -0; i < players.length; i++) {
                runner.addPlayer(players[i]);
                players[i].setID(i, players.length, new String[5], GameType.NO_LIVES_CURRENT);
            }

            GameStats stats = runner.playGame(seed);
            System.out.println(String.format("%s,%s,%s,%d,%d,%d,%d,%d,%d,%d", gameID, agentUnderTest,
                    String.join(",", Arrays.asList(name)), seed, stats.nPlayers, stats.information, stats.lives,
                    stats.moves, stats.score, stats.disqal));
            return stats;
        } catch (Exception ex) {
            System.err.println("error: " + ex.toString());
        }
        return null;
    }

}
