package com.fossgalaxy.games.fireworks.cluster;

import com.fossgalaxy.games.fireworks.utils.SetupUtils;

/**
 * Generate matchups between an agent and itself.
 * <p>
 * Created by webpigeon on 10/10/16.
 */
public class GeneratePureGames {

    private GeneratePureGames() {

    }

    public static void main(String[] args) {
        String[] agentsUnderTest = SetupUtils.getAgentNames();
        int numSeeds = SetupUtils.getSeedCount();

        GenerateGames.printMatchups(agentsUnderTest, agentsUnderTest, numSeeds);
    }

}
