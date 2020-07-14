package com.fossgalaxy.games.fireworks.cluster;

import java.util.Random;

/**
 * Created by webpigeon on 16/11/16.
 */
public class GenerateValidation {

    private GenerateValidation() {

    }

    public static void main(String[] args) {

        //allow generation of known seeds (useful for comparisons between pure and mixed games)
        Random r;
        String metaSeed = System.getenv("FIREWORKS_META_SEED");
        if (metaSeed != null) {
            r = new Random(Long.parseLong(metaSeed));
        } else {
            r = new Random();
        }

        //validation criteria
        int seedCount = 100;
        String[] agents = {"outer", "inner"};

        for (int seedID = 0; seedID < seedCount; seedID++) {
            long seed = r.nextLong();
            for (String agentUnderTest : agents) {
                System.out.println(String.format("%s %s %d", agentUnderTest, agentUnderTest, seed));
            }
        }
    }
}
