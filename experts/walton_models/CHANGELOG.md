# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## v0.2.5

### Changed
* The type of game (zero life varient or classic) is passed to the agent

## v0.2.4

### Added
- New unit tests to check basic state properties
- New unit tests to check endgame conditions fire correctly

### Fixes
- Fix regression in end of game detection ( issue #23 )

## v0.2.3

### Added
- New history features allow for easier processing of game events ( issue #6 )
- GetMovesLeft is now visible ( issue #7 )
- Allow overwriting the default policy for production rule agents ( issue #9 )
- Better documentation for the state interface
- ZeroLife state which matches the suggestion by deepmind with regards to the handling of no lives left
- New methods to allow agents to track who they are playing with (required for learning track)

### Changes
- Agents no longer need to manage history when forwarding the state, this will be handled by the action
- The copy constructor is no longer visible for BasicState, instead the getCopy method should be used
- The game will handle calling tick for the agents, calling tick will now result in a warning message being printed.
- Agent SetID now takes an array of names, corrisponding to the players

### Fixes
- Fix spelling of information in state interface
- Cloning a 'no life state' should result in a no-life state

### Removed
- addEvent method has been removed as it is not compatible with new history system, getHistory has been ported
- Usage of deprecated methods has been reduced
- Old experiments MixedAgentGame and RiskyRunner have been deprecated
- human UI no longer outputs the current turn number to terminal

