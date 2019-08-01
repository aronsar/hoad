package com.fossgalaxy.games.fireworks.cluster;

import com.fossgalaxy.games.fireworks.utils.SetupUtils;

import java.util.Random;

/**
 * Created by newowner on 21/12/2016.
 */
public class GenerateValidationGames {

    private GenerateValidationGames(){

    }

    public static void main(String[] args){
        String agentName = args[0];
        int numSeeds = SetupUtils.getSeedCount();
        int numPlayers = Integer.parseInt(args[1]);

        Random random;
        String metaSeed = System.getenv("FIREWORKS_META_SEED");
        if(metaSeed != null){
            random = new Random(Long.parseLong(metaSeed));
        }else{
            random = new Random();
        }

        for(int seedID = 0; seedID < numSeeds; seedID++){
            long seed = random.nextLong();
            System.out.println(String.format("%s %d %d", agentName, numPlayers, seed));
        }

    }
}
