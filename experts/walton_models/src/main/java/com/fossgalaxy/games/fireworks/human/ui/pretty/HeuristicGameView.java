package com.fossgalaxy.games.fireworks.human.ui.pretty;

import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A version of the pretty game view that points out extra information.
 *
 * Created by webpigeon on 20/04/17.
 */
public class HeuristicGameView extends PrettyGameView {
    private final GameState state;

    public HeuristicGameView(GameState state, int playerID, HumanUIAgent player) {
        super(state, playerID, player);

        this.state = state;
    }

    protected JComponent buildHand(Hand hand, int player) {
        JComponent handView = Box.createHorizontalBox();
        handView.setBorder(hanabiBorder("player "+(player+1) ));

        final CardHinter hinter = new CardHinter(hand);

        for (int i=0; i<hand.getSize(); i++) {
            JComponent cardView = Box.createVerticalBox();
            final int slot = i;

            CardComponent cardComp = new CardComponent(hand, i);
            hinter.setCard(i, cardComp);

            Box box = Box.createHorizontalBox();

            IndicatorPanel colourIndicator = new IndicatorPanel(hand, i, true);
            hinter.setColourIndicator(i, colourIndicator);


            IndicatorPanel valueIndicator = new IndicatorPanel(hand, i, false);
            hinter.setValueIndicator(i, valueIndicator);

            if (player == playerID) {
                MouseAdapter colourAdapter = new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        int ord = e.getY() / (colourIndicator.getHeight()/5);

                        if (ord >= CardColour.values().length) {
                            return;
                        }

                        CardColour selected = CardColour.values()[ord];
                        hinter.hintValuesFor(slot, selected, state.getDeck().toList());
                    }

                    @Override
                    public void mouseMoved(MouseEvent e) {
                        mouseEntered(e);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hinter.resetHintValues(slot);
                    }
                };

                MouseAdapter valueAdapter = new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        int ord = e.getY() / (valueIndicator.getHeight()/5);
                        hinter.hintColoursFor(slot, ord+1, state.getDeck().toList());
                    }

                    @Override
                    public void mouseMoved(MouseEvent e) {
                        mouseEntered(e);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hinter.resetHintColours(slot);
                    }
                };

                colourIndicator.addMouseListener(colourAdapter);
                colourIndicator.addMouseMotionListener(colourAdapter);
                valueIndicator.addMouseListener(valueAdapter);
                valueIndicator.addMouseMotionListener(valueAdapter);
            }

            box.add(colourIndicator);
            box.add(cardComp);
            box.add(valueIndicator);

            cardView.add(box);

            Box buttons = Box.createHorizontalBox();

            if (player == playerID) {
                JButton playBtn = new JButton("Play");
                playBtn.addActionListener(e -> forceMove(new PlayCard(slot)));
                playBtn.setToolTipText("Play this card");
                buttons.add(playBtn);

                JButton discardBtn = new JButton("Discard");
                discardBtn.addActionListener(e -> validateDiscard(new DiscardCard(slot)));
                discardBtn.setToolTipText("Discard this card");
                buttons.add(discardBtn);
            } else {
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

}
