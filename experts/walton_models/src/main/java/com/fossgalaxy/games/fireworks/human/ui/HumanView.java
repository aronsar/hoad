package com.fossgalaxy.games.fireworks.human.ui;

import com.fossgalaxy.games.fireworks.GameRunner;
import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.players.Player;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;
import com.fossgalaxy.games.fireworks.utils.SetupUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Random;
import java.util.UUID;

/**
 * Created by piers on 11/04/17.
 */
public class HumanView extends GameRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(HumanView.class);

    private GameView gameView;

    public HumanView(UUID id, int playersCount) {
        super(id.toString(), playersCount);
        gameView = new BasicGameView(800, 600);
    }

    public HumanView(String gameID, int expectedPlayers) {
        super(gameID, expectedPlayers);
        gameView = new BasicGameView(800, 600);
    }

    public static void main(String[] args) {
        int nPlayers = (args.length < 1) ? 5 : Integer.parseInt(args[0]);
        String agentName = (args.length < 2) ? "iggi" : args[1];

        JFrame frame = new JFrame();
        HumanView view = new HumanView("", nPlayers);

        Long seed = (args.length < 3) ? 42l : Long.parseLong(args[2]);

        Random random = new Random(seed);
        Agent[] agents = new Agent[nPlayers];
        String[] agentStr = new String[nPlayers];
        int playerIndex = random.nextInt(nPlayers);
        for (int i = 0; i < nPlayers; i++) {
            agentStr[i] = (i == playerIndex) ? "BasicHuman" : agentName;
            agents[i] = AgentUtils.buildAgent(agentStr[i]);
        }

        Player[] players = SetupUtils.toPlayers(agentStr, agents);
        for (Player player : players) {
            view.addPlayer(player);
        }

        Thread thread = new Thread(() -> view.playGame(seed));

        thread.start();

        frame.add(view.gameView);

        frame.pack();
        frame.setVisible(true);
    }

    @Override
    protected void writeState(GameState state) {
        // Put it in the view!
        gameView.setState(state, -1);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            LOGGER.error("interrupted, ", e);
        }
        super.writeState(state);
    }
}
