package com.fossgalaxy.games.fireworks.human.ui;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.AgentPlayer;
import com.fossgalaxy.games.fireworks.human.ui.pretty.HeuristicGameView;
import com.fossgalaxy.games.fireworks.human.ui.pretty.HumanUIAgent;
import com.fossgalaxy.games.fireworks.state.GameType;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.events.CardInfoColour;
import com.fossgalaxy.games.fireworks.state.events.CardInfoValue;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;
import com.fossgalaxy.games.fireworks.state.events.MessageType;

import javax.swing.*;
import java.util.List;

/**
 * Created by webpigeon on 11/04/17.
 */
public class UIPlayer extends AgentPlayer {
    private GameView view;
    private JFrame frame;

    private boolean weKnowTheGameIsOver = false;
    private boolean interactive = false;

    /**
     * Create a player with a given name and policy.
     *
     * @param name   the name of this player
     * @param policy the policy this player should use
     */
    public UIPlayer(String name, Agent policy) {
        this(name, policy, false);
    }

    public UIPlayer(String name, Agent policy, boolean interactive) {
        super(name, policy);
        this.interactive = interactive;
    }


    @Override
    public Action getAction() {
        view.setPlayerMoveRequest(true);
        Action action = super.getAction();
        view.setPlayerMoveRequest(false);
        return action;
    }

    @Override
    public void setID(int id, int nPlayers, String[] player, GameType type) {
        super.setID(id, nPlayers, player, type);

        if (interactive) {
            this.view = new HeuristicGameView(state, id, (HumanUIAgent)policy);
        } else {
            this.view = new BasicGameView(800, 600);
        }


        this.frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(view);
        frame.pack();
        frame.setVisible(true);

        view.setState(state, id);
    }

    @Override
    public void resolveTurn(int actor, Action action, List<GameEvent> events) {
        super.resolveTurn(actor, action, events);

        events.forEach(this::sendMessage);
    }

    public void sendMessage(GameEvent msg) {
        if (view != null) {

            if (msg.getEvent().equals(MessageType.CARD_INFO_COLOUR)) {
                CardInfoColour colourTold = (CardInfoColour)msg;
                view.animateTell(colourTold);
            } else if (msg.getEvent().equals(MessageType.CARD_INFO_VALUE)) {
                CardInfoValue colourValue = (CardInfoValue)msg;
                view.animateTell(colourValue);
            }

            view.repaint();

            if (state.isGameOver() && !weKnowTheGameIsOver) {
                JOptionPane.showMessageDialog(view, "The game is over, you scored "+state.getScore());
                weKnowTheGameIsOver = true;
            }
        }
    }

    @Override
    public void onGameOver() {

        if (!weKnowTheGameIsOver) {
            JOptionPane.showMessageDialog(view, "The game is over, you scored " + state.getScore());
            weKnowTheGameIsOver = true;
        }

        if (frame != null) {
            frame.dispose();
        }
    }
}
