package com.fossgalaxy.games.fireworks.cluster;

import com.fossgalaxy.games.fireworks.App;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;
import com.fossgalaxy.games.fireworks.utils.SetupUtils;

import java.util.Random;

/**
 * Generate matchups between an agent and other agents.
 */
public class GeneratePredictorGames {

    private GeneratePredictorGames() {

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
                    for (double x=0; x<1; x+=0.1) {
                        String predictorType = String.format("%s%s%f%s%s%s", "noisy", AgentUtils.PARAM_START, x, AgentUtils.PARAM_SEPARATOR, agentPaired, AgentUtils.PARAM_END );
                        System.out.println(String.format("%s %s %d %s %s", App.PREDICTOR_MCTSND, agentPaired, seed, "normal", predictorType));
                    }
                }
        }
    }

}
