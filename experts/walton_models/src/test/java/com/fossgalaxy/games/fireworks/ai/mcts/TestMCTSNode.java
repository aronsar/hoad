package com.fossgalaxy.games.fireworks.ai.mcts;

import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by webpigeon on 23/11/16.
 */
public class TestMCTSNode {

    @Test
    public void testRootNodeIsRoot() {
        MCTSNode node = new MCTSNode(MCTSNode.DEFAULT_EXP_CONST, Collections.emptyList());

        assertEquals(0, node.getDepth());
        assertEquals(true, node.isLeaf());
    }

    @Test
    public void testCreateChild() {
        Action testAction = new PlayCard(0);
        List<Action> possibleActions = new ArrayList<>();
        possibleActions.add(testAction);

        MCTSNode root = new MCTSNode(possibleActions);
        assertEquals(true, root.isLeaf());
        assertEquals(false, root.containsChild(testAction));

        MCTSNode child = new MCTSNode(root, 1, testAction, Collections.emptyList());
        root.addChild(child);

        assertEquals(true, root.containsChild(testAction));
        assertEquals(false, root.isLeaf());
        assertEquals(1, child.getDepth());
        assertEquals(Arrays.asList(child), root.getChildren());
        assertEquals(1, root.getChildSize());
        assertEquals(possibleActions, root.getAllActionsExpandedAlready());
    }

    @Test
    public void testCreateChildDoesntFuckWithInput() {
        Action testAction = new PlayCard(0);
        List<Action> possibleActions = new ArrayList<>();
        possibleActions.add(testAction);

        MCTSNode root = new MCTSNode(MCTSNode.DEFAULT_EXP_CONST, possibleActions);
        assertEquals(true, root.isLeaf());
        assertEquals(false, root.containsChild(testAction));

        MCTSNode child = new MCTSNode(root, 1, testAction, Collections.emptyList());
        root.addChild(child);

        assertEquals(false, possibleActions.isEmpty());
        assertEquals(true, possibleActions.contains(testAction));
        assertEquals(1, possibleActions.size());
    }

    @Test
    public void testSelectBestNode() {

        Action a1 = new PlayCard(1);
        Action a2 = new PlayCard(2);
        Action a3 = new PlayCard(3);
        Action a4 = new PlayCard(4);

        List<Action> possibleActions = new ArrayList<>();
        possibleActions.add(a1);
        possibleActions.add(a2);
        possibleActions.add(a3);
        possibleActions.add(a4);

        MCTSNode root = new MCTSNode(possibleActions);
        MCTSNode c1 = new MCTSNode(root, 1, a1, Collections.emptyList());
        root.addChild(c1);

        MCTSNode c2 = new MCTSNode(root, 1, a2, Collections.emptyList());
        root.addChild(c2);

        MCTSNode c3 = new MCTSNode(root, 1, a3, Collections.emptyList());
        root.addChild(c3);

        c1.backup(10);
        c2.backup(-50);

        assertEquals(3, root.getChildSize());
        assertEquals(c1, root.getBestNode());
    }

    @Test
    public void testChildDoesNotExist() {
        Action a1 = new PlayCard(0);
        Action a2 = new PlayCard(1);

        List<Action> possibleActions = new ArrayList<>();
        possibleActions.add(a1);
        possibleActions.add(a2);

        MCTSNode root = new MCTSNode(possibleActions);

        assertEquals(false, root.containsChild(a1));
        assertEquals(null, root.getChild(a1));
    }

    @Test
    public void testChildDoesExist() {
        Action a1 = new PlayCard(0);
        Action a2 = new PlayCard(1);

        List<Action> possibleActions = new ArrayList<>();
        possibleActions.add(a1);
        possibleActions.add(a2);

        MCTSNode root = new MCTSNode(possibleActions);
        MCTSNode child = new MCTSNode(root, 2, a2, Collections.emptyList());
        root.addChild(child);

        assertEquals(true, root.containsChild(a2));
        assertEquals(child, root.getChild(a2));
    }

    public void testFullyExpanded() {
        Action a1 = new PlayCard(0);
        Action a2 = new PlayCard(1);

        List<Action> possibleActions = new ArrayList<>();
        possibleActions.add(a1);

        MCTSNode root = new MCTSNode(possibleActions);

        MCTSNode child = new MCTSNode(root, 2, a2, Collections.emptyList());
        root.addChild(child);

        assertEquals(true, root.fullyExpanded(null, 1));
    }


}
