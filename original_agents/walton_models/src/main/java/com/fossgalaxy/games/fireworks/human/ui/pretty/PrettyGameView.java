package com.fossgalaxy.games.fireworks.human.ui.pretty;

import com.fossgalaxy.games.fireworks.human.ui.GameView;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;
import com.fossgalaxy.games.fireworks.state.events.CardInfoColour;
import com.fossgalaxy.games.fireworks.state.events.CardInfoValue;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by webpigeon on 20/04/17.
 */
public class PrettyGameView extends GameView {
    private final GameState state;
    protected final int playerID;
    private final HumanUIAgent player;

    private final boolean TALL_MODE = false;

    private final int MOVE_HINT_TIME = 1500;

    protected final Map<Integer, CardHinter> hinters;
    protected final Map<Integer, JComponent> playerHands;

    public PrettyGameView(GameState state, int playerID, HumanUIAgent player) {
        super();

        this.hinters = new HashMap<>();
        this.playerHands = new HashMap<>();

        this.state = state;
        this.playerID = playerID;
        this.player = player;

        this.setLayout(new FlowLayout());
        buildUI();
    }

    protected Border hanabiBorder(String title) {
        return BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title), BorderFactory.createEmptyBorder(5,5,5,5));
    }

    protected void buildUI() {
        this.setLayout(new BorderLayout());
        Box box = Box.createHorizontalBox();

        Box handBox = Box.createVerticalBox();
        for (int i=0; i<state.getPlayerCount(); i++) {
            if (i != playerID) {
                JComponent hand = buildHand(state.getHand(i), i);
                hand.setBorder(hanabiBorder("Player "+i));
                handBox.add(hand);
                playerHands.put(i, hand);
            }
        }

        handBox.add(Box.createVerticalStrut(50));

        JComponent myHand = buildHand(state.getHand(playerID), playerID);
        playerHands.put(playerID, myHand);

        myHand.setBorder(hanabiBorder("Your hand (Player " + playerID + ")"));
        handBox.add(myHand);

        box.add(handBox);

        Box middleBox = Box.createVerticalBox();

        Box table = Box.createVerticalBox();
        table.setBorder(hanabiBorder("Table"));
        for (CardColour colour : CardColour.values()) {
            table.add(Box.createVerticalStrut(5));
            table.add(new TableCard(state, colour));
            table.add(Box.createVerticalStrut(5));
        }
        middleBox.add(table);
        middleBox.add(new DeckComponent(state));
        box.add(middleBox);

        if (TALL_MODE) {
            Box topBox = Box.createHorizontalBox();

            JComponent informationComponent = new InfoComponent(state);
            informationComponent.setBorder(hanabiBorder("Information Tokens"));
            topBox.add(informationComponent);

            JComponent lifeComponent = new LifeComponent(state);
            lifeComponent.setBorder(hanabiBorder("Life Tokens"));
            topBox.add(lifeComponent);

            add(topBox, BorderLayout.NORTH);
        } else {
            Box right = Box.createVerticalBox();

            JPanel infoPanel = new JPanel(new BorderLayout());
            infoPanel.setBorder(hanabiBorder("Information Tokens"));
            infoPanel.add(new InfoComponent(state));
            right.add(infoPanel);

            JPanel lifePanel = new JPanel(new BorderLayout());
            lifePanel.setBorder(hanabiBorder("Life Tokens"));
            lifePanel.add(new LifeComponent(state));
            right.add(lifePanel);

            JPanel discardPanel = new JPanel(new BorderLayout());
            discardPanel.add(new DiscardComponent(state));
            discardPanel.setBorder(hanabiBorder("Discard Pile"));
            right.add(discardPanel);

            box.add(right);
        }

        add(box);
    }

    protected JComponent buildHand(Hand hand, int player) {
        JComponent handView = Box.createHorizontalBox();
        handView.setBorder(hanabiBorder("player "+(player+1) ));

        final CardHinter hinter = new CardHinter(hand);

        for (int i=0; i<hand.getSize(); i++) {
            JComponent cardView = Box.createVerticalBox();

            CardComponent cardComp = new CardComponent(hand, i);
            hinter.setCard(i, cardComp);

            Box box = Box.createHorizontalBox();
            box.add(buildIndicators(hand, i, true));
            box.add(cardComp);
            box.add(buildIndicators(hand, i, false));

            cardView.add(box);

            Box buttons = Box.createHorizontalBox();

            if (player == playerID) {
                final int slot = i;
                JButton playBtn = new JButton("Play");
                playBtn.addActionListener(e -> forceMove(new PlayCard(slot)));
                playBtn.setToolTipText("Play this card");
                buttons.add(playBtn);

                JButton discardBtn = new JButton("Discard");
                discardBtn.addActionListener(e -> validateDiscard(new DiscardCard(slot)));
                discardBtn.setToolTipText("Discard this card");
                buttons.add(discardBtn);
            } else {
                final int slot = i;
                JButton playBtn = new JButton("Colour");
                playBtn.addActionListener(e -> validateTell(new TellColour(player, hand.getCard(slot).colour)));
                playBtn.setToolTipText("Point out this card's colour to this player");
                buttons.add(playBtn);


                playBtn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hinter.hoverColour(slot);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hinter.clearHover();
                    }
                });

                JButton discardBtn = new JButton("Value");
                discardBtn.addActionListener(e -> validateTell(new TellValue(player, hand.getCard(slot).value)));
                discardBtn.setToolTipText("Point out this card's value to this player");
                buttons.add(discardBtn);

                discardBtn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hinter.hoverValue(slot);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hinter.clearHover();
                    }
                });

            }
            cardView.add(buttons);

            handView.add(cardView);
            handView.add(Box.createHorizontalStrut(5));
        }

        hinters.put(player, hinter);

        return handView;
    }

    //TODO find out why is it never legal to play cards.
    public void forceMove(com.fossgalaxy.games.fireworks.state.actions.Action action) {
        player.setMove(action);
    }

    public void validateDiscard(com.fossgalaxy.games.fireworks.state.actions.Action action) {
        if (state.getInfomation() != state.getStartingInfomation()) {
            player.setMove(action);
        } else {
            JOptionPane.showMessageDialog(this, "You cannot discard with full information");
        }
    }

    public void validateTell(com.fossgalaxy.games.fireworks.state.actions.Action action) {
        if (action.isLegal(playerID, state)) {
            player.setMove(action);
        } else {
            JOptionPane.showMessageDialog(this, "You have no information tokens");
        }
    }


    public JComponent buildIndicators(Hand hand, int slot, boolean isColour) {
        return new IndicatorPanel(hand, slot, isColour);
    }


    @Override
    public void setState(GameState state, int id) {
        //this.state = state;
        //this.playerID = id;
    }

    @Override
    public void setPlayerMoveRequest(boolean playerMoveRequest) {
        super.setPlayerMoveRequest(playerMoveRequest);
        setTurnHint(playerID, playerMoveRequest);
    }

    private final Color TURN_HINT_BACKGROUND = new Color(252, 233, 79, 100);
    public void setTurnHint(int playerID, boolean active) {
        JComponent handBox = playerHands.get(playerID);

        if (active) {
            handBox.setOpaque(true);
            handBox.setBackground(TURN_HINT_BACKGROUND);
        } else {
            handBox.setOpaque(false);
        }

    }

    @Override
    public void animateTell(CardInfoValue valueTold) {
        super.animateTell(valueTold);

        CardHinter hinter = hinters.get(valueTold.getPlayerTold());
        hinter.hoverValue2(valueTold.getValue());
        setTurnHint(valueTold.getPerformer(), true);
        repaint();

        try {
            Thread.sleep(MOVE_HINT_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        setTurnHint(valueTold.getPerformer(), false);
        hinter.clearHover();
        repaint();
    }

    @Override
    public void animateTell(CardInfoColour colourTold) {
        super.animateTell(colourTold);

        CardHinter hinter = hinters.get(colourTold.getPlayerTold());
        hinter.hoverColour2(colourTold.getColour());
        setTurnHint(colourTold.getPerformer(), true);
        repaint();


        try {
            Thread.sleep(MOVE_HINT_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        setTurnHint(colourTold.getPerformer(), false);
        hinter.clearHover();
        repaint();
    }
}
