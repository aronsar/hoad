/**
 * A package containing the lowest level interface for the framework.
 *
 * This keeps track of the game state on behalf of the agents and ensures that their game states are kept in sync
 * with the game engine. The game state is not shared between the engine and the agents to prevent tampering - only the
 * changes in state are communicated (this also makes network play possible).
 */
package com.fossgalaxy.games.fireworks.players;