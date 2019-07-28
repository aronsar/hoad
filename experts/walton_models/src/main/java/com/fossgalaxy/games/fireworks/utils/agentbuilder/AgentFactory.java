package com.fossgalaxy.games.fireworks.utils.agentbuilder;

import com.fossgalaxy.games.fireworks.ai.Agent;

/**
 * An interface to tag which constructor should be used to support agent creation.
 *
 * Created by webpigeon on 06/04/17.
 */
@FunctionalInterface
public interface AgentFactory {

    Agent build(String[] args);

    default Agent build() {
        return build(new String[0]);
    }

    default String name(){ return ""; }

}
