package com.fossgalaxy.games.fireworks.ai.mcts.expconst;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fossgalaxy.games.fireworks.App;
import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.iggi.Utils;
import com.fossgalaxy.games.fireworks.ai.mcts.IterationObject;
import com.fossgalaxy.games.fireworks.ai.mcts.MCTSNode;
import com.fossgalaxy.games.fireworks.ai.rule.logic.DeckUtils;
import com.fossgalaxy.games.fireworks.annotations.AgentBuilderStatic;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.annotations.Parameter;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.Deck;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;

/**
 * Created by webpigeon on 14/08/17.
 */
public class MCTSPredictorFixedDet extends MCTSPredictorExpConst {
    public static final int ITERATION_BUDGET = 10_000;
    public static final int DEFAULT_DET_COUNT = 40;

    protected final int iterationBudget;
    protected final int determinsations;

    protected final Random random;
    protected final Logger logger = LoggerFactory.getLogger(MCTSFixedDet.class);

    private final boolean calcTree = false;

    /**
     * Create an MCTS agent which has the parameters.
     *
     * @param iterationBudget the total number of iterations to use
     * @param determinsations the number of worlds to explore
     * @param expConst the exploration constant to use for the search
     * @param agents the agents we are playing with
     */
    @AgentConstructor(App.PREDICTOR_MCTS+"Fixed")
    @Parameter(id=3, func="parseAgents")
    public MCTSPredictorFixedDet(int iterationBudget, int determinsations, double expConst, Agent[] agents) {
        super(expConst, agents);
        this.iterationBudget = iterationBudget;
        this.determinsations = determinsations;

        this.random = new Random();
    }

    /**
     * Create an MCTS agent which has the parameters.
     *
     * @param iterationBudget the total number of iterations to use
     * @param determinsations the number of worlds to explore
     * @param agents the agents we are playing with
     */
    public MCTSPredictorFixedDet(int iterationBudget, int determinsations, Agent[] agents) {
        this(iterationBudget, determinsations, MCTSNode.DEFAULT_EXP_CONST, agents);
    }

    @AgentBuilderStatic(App.PREDICTOR_MCTSND+"Fixed")
    @Parameter(id=2, func="parseThemAsClones")
    public static MCTSFixedDet buildMCTSND(int iterationBudget, int determinsations, Agent[] agents) {
        return new MCTSFixedDet(iterationBudget, determinsations, MCTSNode.DEFAULT_EXP_CONST);
    }

    @AgentBuilderStatic(App.PREDICTOR_MCTSND+"FixedDefaults")
    @Parameter(id=0, func="parseThemAsClones")
    public static MCTSFixedDet buildMCTSDefaults(Agent[] agents) {
        return new MCTSFixedDet(ITERATION_BUDGET, DEFAULT_DET_COUNT, MCTSNode.DEFAULT_EXP_CONST);
    }

    public static Agent[] parseThemAsClones(String agentStr) {

        Agent[] predictors = new Agent[5];
        for (int i=0; i<predictors.length; i++) {
            predictors[i] = AgentUtils.buildAgent(agentStr);
        }

        return predictors;
    }

    public static Agent[] parseAgents(String agentsStr) {
        String[] agentStr = agentsStr.split("\\|");

        Agent[] predictors = new Agent[agentStr.length];
        for (int i=0; i<agentStr.length; i++) {
            predictors[i] = AgentUtils.buildAgent(agentStr[i]);
        }

        return predictors;
    }

    @Override
    public Action doMove(int agentID, GameState state) {

        MCTSNode root = new MCTSNode(
                (agentID + state.getPlayerCount() - 1) % state.getPlayerCount(),
                null,
                expConst,
                Utils.generateAllActions(agentID, state.getPlayerCount())
        );

        Map<Integer, List<Card>> possibleCards = DeckUtils.bindCard(agentID, state.getHand(agentID), state.getDeck().toList());
        List<Integer> bindOrder = DeckUtils.bindOrder(possibleCards);

        int iterationsPerWorld = iterationBudget / determinsations;

        for (int det = 0; det<determinsations; det++) {

            GameState determinize = state.getCopy();
            IterationObject iterationObject = new IterationObject(agentID);

            Map<Integer, Card> myHandCards = DeckUtils.bindCards(bindOrder, possibleCards);

            Deck deck = determinize.getDeck();
            Hand myHand = determinize.getHand(agentID);
            for (int slot = 0; slot < myHand.getSize(); slot++) {
                Card hand = myHandCards.get(slot);
                myHand.bindCard(slot, hand);
                deck.remove(hand);
            }
            deck.shuffle();


            for (int itr = 0; itr < iterationsPerWorld; itr++) {
                GameState currentState = determinize.getCopy();

                MCTSNode current = select(root, currentState, iterationObject);
                int score = rollout(currentState, agentID, current);
                current.backup(score);
                if (calcTree) {
                    System.err.println(root.printD3());
                }
            }

        }

        return root.getBestNode().getAction();
    }

    @Override
    public String toString() {
        return "MCTSWorlds";
    }
}
