package com.fossgalaxy.games.fireworks.graphing;

import com.fossgalaxy.games.fireworks.GameRunner;
import com.fossgalaxy.games.fireworks.GameStats;
import com.fossgalaxy.games.fireworks.ai.AgentPlayer;
import com.fossgalaxy.games.fireworks.players.Player;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;

import java.util.UUID;

/**
 * Created by piers on 23/03/17.
 */
public class Graphing {

    public static void main(String[] args) {

        FirstMoveGameRunner runner = new FirstMoveGameRunner("0", 3);
        //Player flatmc = new AgentPlayer("flatmc-iggi", AgentUtils.buildAgent("flatmc-iggi"));
        Player mcts = new AgentPlayer("mctsND", AgentUtils.buildAgent("mcts"));
        Player iggi = new AgentPlayer("iggi", AgentUtils.buildAgent("iggi"));
        Player iggi2 = new AgentPlayer("iggi", AgentUtils.buildAgent("iggi"));

        runner.addPlayer(mcts);
        runner.addPlayer(iggi);
        runner.addPlayer(iggi2);
        runner.playGame(0L);
    }
}

class FirstMoveGameRunner extends GameRunner {
    public FirstMoveGameRunner(UUID id, int playersCount) {
        super(id.toString(), playersCount);
    }

    public FirstMoveGameRunner(String gameID, int expectedPlayers) {
        super(gameID, expectedPlayers);
    }

    @Override
    public GameStats playGame(Long seed) {
        init(seed);
        //state.tick();
        nextMove();

        return null;
    }
}
