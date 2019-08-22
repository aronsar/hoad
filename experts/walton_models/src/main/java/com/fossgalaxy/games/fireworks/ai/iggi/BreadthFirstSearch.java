package com.fossgalaxy.games.fireworks.ai.iggi;

import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;

import java.util.*;

public class BreadthFirstSearch {
    private int playerID;
    private Queue<Node> expansionList;
    private Map<Node, Node> cameFrom;

    public BreadthFirstSearch() {
        this.expansionList = new LinkedList<>();
        this.cameFrom = new HashMap<>();
    }

    public List<Action> search(int player, GameState state) {
        this.playerID = player;

        //expand the root node
        generateChildren(new Node(null, state, player, 0));

        while (!expansionList.isEmpty()) {
            Node node = expansionList.poll();

            if (node.parent.isGameOver()) {
                return buildChain(node);
            }

            generateChildren(node);
        }

        return null;
    }

    public List<Action> buildChain(Node best) {
        List<Action> list = new ArrayList<Action>();

        Node current = best;
        while (best != null) {
            list.add(current.move);
            current = cameFrom.get(current);
        }

        return list;
    }


    private void generateChildren(Node node) {
        GameState state = node.parent;

        if (node.move != null) {
            node.move.apply(node.player, state);
        }

        if (!state.isGameOver()) {
            Collection<Action> actions = Utils.generateSuitableActions(playerID, state);
            int nextPlayer = (playerID + 1) % state.getPlayerCount();
            for (Action action : actions) {
                Node child = new Node(action, state.getCopy(), node.depth + 1, nextPlayer);
                expansionList.add(child);
                cameFrom.put(child, node);
            }
        }
    }

    private class Node {
        public final Action move;
        public final GameState parent;
        public final int depth;
        public final int player;

        public Node(Action move, GameState state, int depth, int player) {
            this.move = move;
            this.parent = state;
            this.depth = depth;
            this.player = player;
        }
    }

}
