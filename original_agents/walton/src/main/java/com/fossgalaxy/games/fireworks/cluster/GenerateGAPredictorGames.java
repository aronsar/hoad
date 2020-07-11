package com.fossgalaxy.games.fireworks.cluster;

import com.fossgalaxy.games.fireworks.utils.SetupUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

/**
 * Generate matchups between an agent and other agents.
 */
public class GenerateGAPredictorGames {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateGAPredictorGames.class);

    private GenerateGAPredictorGames() {

    }

    public static void main(String[] args) {
        String[] agentsPaired = SetupUtils.getPairedNames();
        int numSeeds = SetupUtils.getSeedCount();

        if (args.length != 1){
            System.exit(1);
        }

        printMatchups(agentsPaired, numSeeds, args[0]);
    }

    static void printMatchups(String[] agentsPaired, int numSeeds, String modelLocation) {

        //allow generation of known seeds (useful for comparisons between pure and mixed games)
        Random r;
        String metaSeed = System.getenv("FIREWORKS_META_SEED");
        if (metaSeed != null) {
            r = new Random(Long.parseLong(metaSeed));
        } else {
            r = new Random();
        }

        Map<String, String> models = new HashMap<>();
        File modelFolder = new File(modelLocation);
        for (File file : modelFolder.listFiles()) {
            if (file.getName().endsWith(".mdl")) {
                String agentName = file.getName().replace("-model.mdl", "");
                String model = readFile(file);

                models.put(agentName, model);
            }
        }


        for (int seedID = 0; seedID < numSeeds; seedID++) {
            long seed = r.nextLong();
                for (Map.Entry<String, String> agentModel : models.entrySet()) {
                    String agentPaired = agentModel.getKey();
                    String predictorType = "model:"+agentModel.getValue();

                    System.out.println(String.format("%s %s %d %s %s", "predictorMCTSND", agentPaired, seed, "normal", agentPaired));
                    System.out.println(String.format("%s %s %d %s %s", "predictorMCTSND", agentPaired, seed, "normal", predictorType));

                    //used as measure the seed difficulty.
                    System.out.println(String.format("%s %s %d %s %s", agentPaired, agentPaired, seed, "normal", agentPaired));
                    System.out.println(String.format("%s %s %d %s %s", predictorType, predictorType, seed, "normal", agentPaired));
                }
        }
    }


    public static String readFile(File file) {
        String fileBody = "";
        try (
                Scanner scanner = new Scanner(file);
                ){
            while (scanner.hasNextLine()) {
                fileBody = fileBody.concat(scanner.nextLine());
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("error reading file, ", e);
        }
        return fileBody.replace(";",",");
    }
}
