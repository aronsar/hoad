package com.fossgalaxy.games.fireworks.ai.mcts;

import java.util.Arrays;

import com.fossgalaxy.games.fireworks.App;
import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.annotations.AgentBuilderStatic;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.annotations.Parameter;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;
import com.fossgalaxy.games.fireworks.utils.DebugUtils;

/**
 * Use MCTS for our nodes, and policies for other players.
 *
 * This agent will use UCT for our nodes, but paired agents will consult the policy rather than performing a standard
 * expansion. This is often quicker than performing a complete expansion of the nodes and will take into account what
 * the paired agent will do rather than settling on the highest scoring nodes for them.
 */
public class MCTSPredictor extends MCTS {
    protected Agent[] agents;
    protected int agentID;

    //usage: if pmcts=2 then iggi|iggi|null|iggi|iggi
    //usage: if pmcts=2 && model=1,2,3,4,5,5 then 1,2,3,4,5,6|1,2,3,4,5,6|null|1,2,3,4,5,6|1,2,3,4,5,6
    @AgentConstructor(App.PREDICTOR_MCTS)
    @Parameter(id=0, func="parseAgents")
    public MCTSPredictor(Agent[] others) {
        super();
        this.agents = others;
    }

    public MCTSPredictor(Agent[] agents, int roundLength) {
        super(roundLength);
        this.agents = agents;
    }

    public MCTSPredictor(Agent[] agents, int roundLength, int rolloutDepth, int treeDepthMul) {
        super(roundLength, rolloutDepth, treeDepthMul);
        this.agents = agents;
    }

    @AgentBuilderStatic(App.PREDICTOR_MCTSND)
    @Parameter(id=0, func="parseAgents")
    public static MCTSPredictor buildPMCTSND(Agent[] agents) {
        return new MCTSPredictor(agents, MCTS.DEFAULT_ITERATIONS, MCTS.NO_LIMIT, MCTS.NO_LIMIT);
    }

    @Override
    public Action doMove(int agentID, GameState state) {
        agents[agentID] = null;
        return super.doMove(agentID, state);
    }

    public static Agent[] parseAgents(String agentsStr) {
        String[] agentStr = agentsStr.split("\\|");

        Agent[] predictors = new Agent[agentStr.length];
        for (int i=0; i<agentStr.length; i++) {
            predictors[i] = AgentUtils.buildAgent(agentStr[i]);
        }

        return predictors;
    }

    protected Action doSuperMove(int agentID, GameState state) {
        return super.doMove(agentID, state);
    }

    @Override
    protected MCTSNode select(MCTSNode root, GameState state, IterationObject iterationObject) {
        MCTSNode current = root;
        int treeDepth = calculateTreeDepthLimit(state);
        boolean expanded = false;

        while (!state.isGameOver() && current.getDepth() < treeDepth && !expanded) {
            MCTSNode next;
            if (current.fullyExpanded(state)) {
                next = current.getUCTNode(state);
            } else {
                int numChildren = current.getChildSize();
                next = expand(current, state);

                //trip if the move is illegal
                if (!next.getAction().isLegal(next.getAgent(), state)) {
                    logger.error("Illegal move {} selected during select", next.getAction());
                }

                if (numChildren != current.getChildSize()) {
                    // It is new
                	expanded = true;
                    //return next;
                }
            }
            
            // Forward the state
            if (next == null) {
            	//ok to early return, current advanced in last game tick
                return current;
            }
            current = next;

            int score = state.getScore();
            int lives = state.getLives();
            int agent = current.getAgent();
            Action action = current.getAction();
            if (action != null) {
                action.apply(agent, state);
            }

            if (iterationObject.isMyGo(agent)) {
                if (state.getLives() < lives) iterationObject.incrementLivesLostMyGo();
                if (state.getScore() > score) iterationObject.incrementPointsGainedMyGo();
            }
        }

        return current;
    }

    /**
     * Select a new action for the expansion node.
     *
     * @param state   the game state to travel from
     * @param agentID the AgentID to use for action selection
     * @return the next action to be added to the tree from this state.
     */
    @Override
    protected Action selectActionForExpand(GameState state, MCTSNode node, int agentID) {
        if (agents[agentID] == null) {
            return super.selectActionForExpand(state, node, agentID);
        }
        GameState fiddled = fiddleTheDeck(state, agentID);

        try {
            return agents[agentID].doMove(agentID, fiddled);
        } catch (IllegalStateException ex) {
            logger.error("agent could not make a move.", ex);
            DebugUtils.printState(logger, state);
            return super.selectActionForExpand(state, node, agentID);
        }
    }

    @Override
    protected Action selectActionForRollout(GameState state, int agentID) {
        if (agents[agentID] == null) {
            return super.selectActionForRollout(state, agentID);
        }

        try {
            GameState fiddled = fiddleTheDeck(state, agentID);
            return agents[agentID].doMove(agentID, fiddled);
        } catch (IllegalStateException ex) {
            logger.error("agent could not make a move.", ex);
            DebugUtils.printState(logger, state);
            return super.selectActionForRollout(state, agentID);
        }
    }


    /**
     * Method that gets the current game state and returns the cards present in an agent's hand to the deck.
     *
     * This is important when passing the state to an agent that uses cards in the deck to figure out what cards
     * could be in its hand - in our state those cards are no longer in the deck (as they are bound to that agent's
     * hand).
     *
     * This is useful step for attempting to make a fully observable game state into a partially observable one.
     * This will work on a copy and will not alter the state passed in.
     *
     * @param state the game state to manipulate
     * @param agentID the agentID to de-allocate
     * @return the altered state.
     */
    private GameState fiddleTheDeck(GameState state, int agentID) {
        GameState copy = state.getCopy();
        Hand hand = copy.getHand(agentID);
        for (int slot = 0; slot < hand.getSize(); slot++) {
            if (hand.hasCard(slot)) {
                copy.getDeck().add(hand.getCard(slot));
            }
        }
        return copy;
    }

    @Override
    public String toString() {
        return String.format("MCTS(%s)", Arrays.toString(agents));
    }
}
