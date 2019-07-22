package com.fossgalaxy.games.fireworks.cluster;

import com.fossgalaxy.games.fireworks.utils.SetupUtils;

import java.util.Random;

/**
 * Generate matchups between an agent and other agents.
 */
public class GeneratePurePredictorGames {

    private GeneratePurePredictorGames() {

    }

    public static void main(String[] args) {
        String[] agentsPaired = SetupUtils.getPairedNames();
        int numSeeds = SetupUtils.getSeedCount();

        printMatchups(agentsPaired, numSeeds);
    }

    static void printMatchups(String[] agentsPaired, int numSeeds) {

        //allow generation of known seeds (useful for comparisons between pure and mixed games)
        Random r;
        String metaSeed = System.getenv("FIREWORKS_META_SEED");
        if (metaSeed != null) {
            r = new Random(Long.parseLong(metaSeed));
        } else {
            r = new Random();
        }

        for (int seedID = 0; seedID < numSeeds; seedID++) {
            long seed = r.nextLong();
                for (String agentPaired : agentsPaired) {
                    String predictorType = agentPaired; //the paired agent will only be used for the model, all agents are really predictorMCTSND
                    System.out.println(String.format("%s %s %d %s %s", "predictorMCTSND", "predictorMCTSND", seed, "normal", predictorType));
                }
        }
    }

}
