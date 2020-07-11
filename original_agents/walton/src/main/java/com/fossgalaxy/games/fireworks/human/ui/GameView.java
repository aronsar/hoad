package com.fossgalaxy.games.fireworks.human.ui;

import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.events.CardInfoColour;
import com.fossgalaxy.games.fireworks.state.events.CardInfoValue;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by webpigeon on 20/04/17.
 */
public abstract class GameView extends JComponent {
    public abstract void setState(GameState state, int id);

    public static final Color TANGO_DARK = new Color(46, 52, 54);
    public static final Color TANGO_BLUE = new Color(52, 101, 164);
    public static final Color TANGO_RED = new Color(204, 0, 0);
    public static final Color TANGO_GREY = new Color(211, 215, 207);
    public static final Color TANGO_CHOC = new Color(193, 125, 17);
    public static final Color TANGO_GREEN = new Color(115, 210, 22);

    protected boolean waitingForMove = false;

    protected static final Map<Color, Color> textColors = new HashMap<>();

    static{
        textColors.put(Color.WHITE, Color.BLACK);
        textColors.put(Color.BLACK, Color.WHITE);
        textColors.put(Color.YELLOW, Color.BLACK);
        textColors.put(Color.RED, Color.BLACK);
        textColors.put(Color.BLUE, Color.WHITE);
        textColors.put(Color.GREEN, Color.BLACK);
        textColors.put(TANGO_DARK, Color.WHITE);
        textColors.put(TANGO_BLUE, Color.WHITE);
        textColors.put(TANGO_RED, Color.WHITE);
        textColors.put(TANGO_GREY, Color.BLACK);
        textColors.put(TANGO_CHOC, Color.WHITE);
        textColors.put(TANGO_GREEN, Color.BLACK);

    }

    public static Color getTextColour(Color background) {
        return textColors.getOrDefault(background, TANGO_DARK);
    }

    public static Color getColor(CardColour colour) {
        if (colour == null) {
            return Color.BLACK;
        }
        switch (colour) {
            case BLUE:
                return TANGO_BLUE;
            case RED:
                return TANGO_RED;
            case WHITE:
                return TANGO_GREY;
            case ORANGE:
                return TANGO_CHOC;
            case GREEN:
                return TANGO_GREEN;
            default:
                return Color.BLACK;
        }
    }

    public void setPlayerMoveRequest(boolean playerMoveRequest) {
        this.waitingForMove = playerMoveRequest;
    }

    //animation/feedback hooks

    public void animateTell(CardInfoValue valueTold) {
    }

    public void animateTell(CardInfoColour colourTold) {
    }
}
