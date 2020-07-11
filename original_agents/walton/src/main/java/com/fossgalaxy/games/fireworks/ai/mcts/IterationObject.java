package com.fossgalaxy.games.fireworks.ai.mcts;

/**
 * Created by piers on 05/10/16.
 */
public class IterationObject {

    private int agentID;
    private int livesLostMyGo;
    private int pointsGainedMyGo;

    public IterationObject(int agentID) {
        this.agentID = agentID;
    }

    public void incrementLivesLostMyGo() {
        livesLostMyGo++;
    }

    public int getLivesLostMyGo() {
        return livesLostMyGo;
    }

    public void incrementPointsGainedMyGo() {
        pointsGainedMyGo++;
    }

    public int getPointsGainedMyGo() {
        return pointsGainedMyGo;
    }

    public boolean isMyGo(int currentAgentID) {
        return currentAgentID == agentID;
    }


}
