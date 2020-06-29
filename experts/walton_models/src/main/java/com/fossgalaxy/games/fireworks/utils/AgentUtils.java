package com.fossgalaxy.games.fireworks.utils;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.RandomAgent;
import com.fossgalaxy.games.fireworks.ai.ga.RMHC;
import com.fossgalaxy.games.fireworks.ai.hat.HatGuessing;
import com.fossgalaxy.games.fireworks.ai.iggi.IGGIFactory;
import com.fossgalaxy.games.fireworks.ai.mcs.MonteCarloSearch;
import com.fossgalaxy.games.fireworks.ai.mcts.MCTS;
import com.fossgalaxy.games.fireworks.ai.mcts.NoisyPredictor;
import com.fossgalaxy.games.fireworks.ai.osawa.OsawaFactory;
import com.fossgalaxy.games.fireworks.ai.rule.ProductionRuleAgent;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.ai.rule.RuleSet;
import com.fossgalaxy.games.fireworks.ai.rule.random.DiscardRandomly;
import com.fossgalaxy.games.fireworks.ai.rule.random.TellRandomly;
import com.fossgalaxy.games.fireworks.ai.vanDenBergh.VanDenBerghFactory;
import com.fossgalaxy.games.fireworks.annotations.AgentBuilderStatic;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.utils.agentbuilder.AgentFactory;
import com.fossgalaxy.games.fireworks.utils.agentbuilder.AgentFinder;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by webpigeon on 01/12/16.
 */
public class AgentUtils {
    public static final String PARAM_START = "[";
    public static final String PARAM_END = "]";
    public static final String PARAM_SEPARATOR = ":";

    public static final AgentFinder finder = buildFinder();

    private AgentUtils() {

    }

    private static AgentFinder buildFinder() {
        AgentFinder finder = new AgentFinder();

        //add all rule based agent factory methods - it doesn't make sense to make classes for each of these...
        //for (Map.Entry<String, Supplier<Agent>> ruleBased : buildMap().entrySet()) {
        //    finder.addFactory(ruleBased.getKey(), i(ruleBased.getValue()));
        //}

        return finder;
    }

    @AgentBuilderStatic("Funky")
    public static Agent funkyAgent(Agent first, int second, Agent third, int fourth){
        return new Agent() {
            @Override
            public Action doMove(int agentID, GameState state) {
                return null;
            }

            @Override
            public String toString() {
                return first + ":" + second + ":" + third + ":" + fourth;
            }
        };
    }

    public static void main(String[] args) {
        //        Agent agent2 = finder.buildAgent("pmcts", "noisy,0.9,iggi|noisy,0.9,legal_random|noisy,0.9,legal_random|noisy,0.9,legal_random|noisy,0.9,legal_random");

        Agent agent = buildAgent("VanDenBergh[0:1:NEXT_USEFUL_THEN_MOST_CARDS:MOST_CERTAIN_IS_USELESS]");
        System.out.println(agent);

        Agent agent2 = buildAgent("noisy[0.1:noisy[0.2:noisy[0.3:noisy[0.4:noisy[0.5:iggi]]]]]");
        System.out.println(agent2);

        Agent agent3 = buildAgent("Funky[iggi:1:Funky[noisy[0.1:iggi]:1:iggi:2]:2]");
        System.out.println(agent3);
    }

    private static AgentFactory i(Supplier<Agent> s) {
        return (x -> s.get());
    }

    private static Map<String, Supplier<Agent>> buildMap() {
        Map<String, Supplier<Agent>> map = new HashMap<>();
        map.put("pure_random", RandomAgent::new);
        map.put("random", OsawaFactory::buildRandom);
        map.put("internal", OsawaFactory::buildInternalState);
        map.put("outer", OsawaFactory::buildOuterState);
        map.put("cautious", IGGIFactory::buildCautious);
        map.put("iggi", IGGIFactory::buildIGGIPlayer);
        map.put("iggi_risky", IGGIFactory::buildRiskyPlayer);
        map.put("legal_random", IGGIFactory::buildRandom);
        map.put("mcts", MCTS::new);
        map.put("cautiousMCTS", IGGIFactory::buildCautiousMCTS);
        map.put("hat", HatGuessing::new);
        map.put("piers", IGGIFactory::buildPiersPlayer);
        map.put("flatmc-legal_random", () -> new MonteCarloSearch(buildAgent("legal_random")));
        map.put("flatmc-inner", () -> new MonteCarloSearch(buildAgent("inner")));
        map.put("flatmc-iggi", () -> new MonteCarloSearch(buildAgent("iggi")));
        map.put("flatmc-flawed", () -> new MonteCarloSearch(buildAgent("flawed")));
        map.put("vandenbergh", VanDenBerghFactory::buildAgent);
        map.put("flawed", IGGIFactory::buildFlawedPlayer);
        map.put("rmhc", RMHC::new);

        //Non-depth limited mcts versions
        map.put("mctsND", () -> new MCTS(MCTS.DEFAULT_ITERATIONS, MCTS.NO_LIMIT, MCTS.NO_LIMIT));

        return map;
    }

    public static Agent buildAgent(String name, String... args) {
        return finder.buildAgent(name, args);
    }

    //noisy[0.9:iggi]

    // Need to split only args and ignore the brackets
    public static Agent buildAgent(String name) {
        if (name.contains(PARAM_START) && name.contains(PARAM_END)) {
            String args = name.substring(name.indexOf(PARAM_START) + 1, name.lastIndexOf(PARAM_END));
            String[] splitArgs = splitArgs(args);
            String firstPart = name.substring(0, name.indexOf(PARAM_START));
            return finder.buildAgent(firstPart, splitArgs);
        }
        return finder.buildAgent(name);
    }

    private static String[] splitArgs(String args) {
        int opens = 0;
        List<String> partsFound = new ArrayList<>();

        StringBuilder currentParamBuilder = new StringBuilder();

        for (int index = 0; index < args.length(); index++) {
            char c = args.charAt(index);
            // handle params open/close
            if(c == PARAM_START.charAt(0)){
                opens++;
            }else if(c == PARAM_END.charAt(0)){
                opens--;
            }else if(c == PARAM_SEPARATOR.charAt(0)){
                if(opens == 0){
                    partsFound.add(currentParamBuilder.toString());
                    currentParamBuilder.setLength(0);
                    continue;
                }
            }
            currentParamBuilder.append(Character.toString(c));
        }
        if(!(currentParamBuilder.length() == 0)){
            partsFound.add(currentParamBuilder.toString());
        }
        return partsFound.toArray(new String[partsFound.size()]);
    }

    /**
     * Allow creation of other forms of predictors
     * <p>
     * This allows the creation of noisey/learned models to be injected into the agent.
     *
     * @param args the name to generate the predictor from
     * @return the new predictor
     */
    public static Agent buildPredictor(String ... args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("You must supply a model to use");
        }

        String name = args[0];

        if (name.startsWith("noisy")) {
            String[] parts = name.split(":");
            double th = Double.parseDouble(parts[1]);
            return new NoisyPredictor(th, buildAgent(parts[2]));
        }

        if (name.startsWith("model")) {
            String[] parts = name.split(":");
            Integer[] rules = Arrays.stream(parts[1].split(",")).map(Integer::parseInt).collect(Collectors.toList()).toArray(new Integer[0]);
            return buildAgent(rules);
        }

        return buildAgent(name);
    }

    public static Agent[] buildPredictors(int myID, int size, String paired) {
        Agent[] agents = new Agent[size];
        for (int i = 0; i < size; i++) {
            if (i == myID) {
                agents[i] = null;
            } else {
                agents[i] = buildPredictor(paired);
            }
        }

        return agents;
    }

    public static Agent[] buildPredictors(int myID, String... paired) {
        Agent[] agents = new Agent[paired.length];
        for (int i = 0; i < paired.length; i++) {
            if (i == myID) {
                agents[i] = null;
            } else {
                agents[i] = buildAgent(paired[i]);
            }
        }

        return agents;
    }

    public static Agent buildAgent(Integer[] rules) {
        ProductionRuleAgent pra = new ProductionRuleAgent();
        ArrayList<Rule> actualRules = RuleSet.getRules();
        for (int rule : rules) {
            if (rule == -1) {
                break;
            }
            pra.addRule(actualRules.get(rule));
        }

        actualRules.add(new TellRandomly());
        actualRules.add(new DiscardRandomly());

        return pra;
    }

    @AgentBuilderStatic("model")
    public static Agent buildAgent(int[] rules) {
        ProductionRuleAgent pra = new ProductionRuleAgent();
        ArrayList<Rule> actualRules = RuleSet.getRules();
        for (int rule : rules) {
            if (rule == -1) {
                break;
            }
            pra.addRule(actualRules.get(rule));
        }

        actualRules.add(new TellRandomly());
        actualRules.add(new DiscardRandomly());

        return pra;
    }

}
