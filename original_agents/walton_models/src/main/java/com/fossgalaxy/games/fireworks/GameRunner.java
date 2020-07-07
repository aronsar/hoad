package com.fossgalaxy.games.fireworks;

import com.fossgalaxy.games.fireworks.ai.AgentPlayer;
import com.fossgalaxy.games.fireworks.players.Player;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.events.CardDrawn;
import com.fossgalaxy.games.fireworks.state.events.CardReceived;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;
import com.fossgalaxy.games.fireworks.state.events.GameInformation;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;
import com.fossgalaxy.games.fireworks.utils.DebugUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import com.fossgalaxy.games.fireworks.utils.DataParserUtils;;

/**
 * A basic runner for the game of Hanabi.
 */
public class GameRunner {
    private static final int RULE_STRIKES = 1; // how many times can a player return an illegal move before we give up?
    private static final int[] HAND_SIZE = { -1, -1, 5, 5, 4, 4 };
    private final Logger logger = LoggerFactory.getLogger(GameRunner.class);
    private final String gameID;
    private final GameType type;

    protected final Player[] players;
    protected final String[] playerNames;

    protected final GameState state;

    private int nPlayers;
    private int moves;

    private int nextPlayer;

    /**
     * Create a game runner with a given ID and a number of players.
     *
     * @param id           the Id of the game
     * @param playersCount the number of players that will be playing
     * @deprecated use string IDs instead
     */
    @Deprecated
    public GameRunner(UUID id, int playersCount) {
        this(id.toString(), playersCount, false);
    }

    /**
     * Create a game runner with a given ID and number of players.
     *
     * This is backwards compatable (ie, no lives means score at last move).
     *
     * @param id              the game id
     * @param expectedPlayers the number of players that will be in the game
     */
    public GameRunner(String id, int expectedPlayers) {
        this(id, expectedPlayers, false);
    }

    /**
     * Create a game runner with a given ID and number of players.
     *
     * @param gameID           the ID of the game
     * @param expectedPlayers  the number of players we expect to be playing.
     * @param noLivesMeansZero true if no lives means the players get zero.
     */
    public GameRunner(String gameID, int expectedPlayers, boolean noLivesMeansZero) {
        this(gameID, noLivesMeansZero ? new NoLifeState(HAND_SIZE[expectedPlayers], expectedPlayers)
                : new BasicState(HAND_SIZE[expectedPlayers], expectedPlayers));
    }

    public GameRunner(String gameID, GameState state) {
        this.players = new Player[state.getPlayerCount()];
        this.playerNames = new String[state.getPlayerCount()];
        this.state = Objects.requireNonNull(state);
        this.type = state instanceof NoLifeState ? GameType.NO_LIVES_ZERO : GameType.NO_LIVES_CURRENT;
        this.nPlayers = 0;
        this.nextPlayer = 0;
        this.moves = 0;
        this.gameID = gameID;
    }

    /**
     * Add a player to the game.
     * <p>
     * This should not be attempted once the game has started.
     *
     * @param player the player to add to the game
     */
    public void addPlayer(Player player) {
        logger.debug("player {} is {}", nPlayers, player);
        players[nPlayers++] = Objects.requireNonNull(player);
    }

    /**
     * Add a named player.
     *
     * Player names will be revealed to all players at the start of a game.
     *
     * @param name   the name of the player
     * @param player the player object
     */
    public void addNamedPlayer(String name, Player player) {
        addPlayer(player);
        playerNames[nPlayers - 1] = name;
    }

    /**
     * Initialise the game for the players.
     * <p>
     * This method does the setup phase for the game.
     * <p>
     * this method is responsible for: 1) telling player their IDs 2) initialising
     * the game state and deck order 3) informing players about the number of
     * players and starting resource values 4) dealing and declaring the values in
     * the player's initial hands.
     * <p>
     * You should <b>not</b> call this method directly - calling playGame calls it
     * for you on your behalf!
     *
     * @param seed the random seed to use for deck ordering.
     */
    protected void init(Long seed) {
        logger.debug("game init started - {} player game with seed {}", players.length, seed);
        long startTime = getTick();

        // step 1: tell all players their IDs
        for (int i = 0; i < players.length; i++) {
            logger.debug("player {} is {}", i, players[i]);
            players[i].setID(i, players.length, playerNames, type);
        }

        state.init(seed);

        // New
        Deck mydeck = new Deck(state.getDeck());
        LinkedList<Card> mycards = mydeck.cards;
        DataParserUtils.RecordStartDeck(mycards);

        // keep track of the messages that should be sent as part of the game setup
        List<GameEvent> initEvents = new ArrayList<>();

        // tell the players the rules
        GameEvent gameInfo = new GameInformation(nPlayers, HAND_SIZE[nPlayers], state.getInfomation(),
                state.getLives());
        initEvents.add(gameInfo);

        // System.out.print(mycards.size());
        // Card curr_card;

        // while (!mycards.isEmpty()) {
        // curr_card = mycards.pop();
        // System.out.printf("Card: %s %d,", curr_card.colour, curr_card.value);
        // }

        LinkedList<Card> start_hands = new LinkedList<Card>();
        // tell players about the initial state
        for (int player = 0; player < players.length; player++) {
            Hand hand = state.getHand(player);

            for (int slot = 0; slot < hand.getSize(); slot++) {
                Card cardInSlot = hand.getCard(slot);

                GameEvent cardDrawn = new CardDrawn(player, slot, cardInSlot.colour, cardInSlot.value, 0);
                GameEvent cardRecv = new CardReceived(player, slot, state.getDeck().hasCardsLeft(), 0);
                initEvents.add(cardDrawn);
                initEvents.add(cardRecv);
                start_hands.add(cardInSlot);
            }
        }
        DataParserUtils.RecordStartHands(start_hands);

        // dispatch the events to the players
        notifyAction(-2, null, initEvents);

        long endTime = getTick();
        logger.debug("Game init complete: took {} ms", endTime - startTime);
    }

    // TODO find a better way of doing this logging.
    protected void writeState(GameState state) {
        DebugUtils.printState(logger, state);
    }

    private long getTick() {
        return System.currentTimeMillis();
    }

    // TODO time limit the agent

    /**
     * Ask the next player for their move.
     */
    protected void nextMove(DataParserUtils parser) {
        Player player = players[nextPlayer];
        assert player != null : "that player is not valid";

        // logger.debug("asking player {} for their move", nextPlayer);
        long startTime = getTick();

        // get the action and try to apply it
        Action action = player.getAction();

        long endTime = getTick();
        // logger.debug("agent {} took {} ms to make their move", nextPlayer, endTime -
        // startTime);
        logger.debug("move {}: player {} made move {}", moves, nextPlayer, action);

        // if the more was illegal, throw a rules violation
        if (!action.isLegal(nextPlayer, state)) {
            throw new RulesViolation(action);
        }

        // perform the action and get the effects
        // logger.debug("player {} made move {} as turn {}", nextPlayer, action, moves);
        moves++;

        Collection<GameEvent> events = action.apply(nextPlayer, state);
        // Ensure parser exist
        if (parser != null) {
            parser.writeData(state, action);
            // parser.writeAction(action);
            // parser.writeObservation(state);
        }
        notifyAction(nextPlayer, action, events);

        // make sure it's the next player's turn
        nextPlayer = (nextPlayer + 1) % players.length;
    }

    /**
     * Play the game and generate the outcome.
     * <p>
     * This will play the game and generate a result.
     *
     * @param seed the seed to use for deck ordering
     * @return the result of the game
     */
    public GameStats playGame(Long seed) {
        int strikes = 0;
        DataParserUtils parser = new DataParserUtils();

        try {
            assert nPlayers == players.length;
            init(seed);

            while (!state.isGameOver()) {
                try {
                    writeState(state);
                    nextMove(parser);
                } catch (RulesViolation rv) {
                    logger.warn("got rules violation when processing move", rv);
                    strikes++;

                    // If we're not being permissive, end the game.
                    if (strikes <= RULE_STRIKES) {
                        logger.error("Maximum strikes reached, ending game");
                        break;
                    }
                }
            }
            parser.writeToDisk();
            return new GameStats(gameID, players.length, state.getScore(), state.getLives(), moves,
                    state.getInfomation(), strikes);
        } catch (Exception ex) {
            logger.error("the game went bang", ex);
            return new GameStats(gameID, players.length, state.getScore(), state.getLives(), moves,
                    state.getInfomation(), 1);
        }

    }

    /**
     * Tell the players about an action that has occurred
     *
     * @param actor  the player who performed the action
     * @param action the action the player performed
     * @param events the events that resulted from that action
     */
    protected void notifyAction(int actor, Action action, Collection<GameEvent> events) {

        for (int i = 0; i < players.length; i++) {
            int currPlayer = i; // use of lambda expression must be effectively final

            // filter events to just those that are visible to the player
            List<GameEvent> visibleEvents = events.stream().filter(e -> e.isVisibleTo(currPlayer))
                    .collect(Collectors.toList());
            players[i].resolveTurn(actor, action, visibleEvents);

            logger.debug("for {}, sent {} to {}", action, visibleEvents, currPlayer);
        }

    }

    // send messages as soon as they are available
    /*
     * protected void send(GameEvent event) { logger.debug("game sent event: {}",
     * event); for (int i = 0; i < players.length; i++) { if (event.isVisibleTo(i))
     * { players[i].sendMessage(event); } } }
     */

    public static void main(String[] args) {
        System.out.println("Start at GameRunner");
        Random random = new Random();
        List<GameStats> results = new ArrayList<>();
        for (int players = 2; players <= 5; players++) {
            for (int gameNumber = 0; gameNumber < 10; gameNumber++) {
                GameRunner runner = new GameRunner("IGGI2-" + gameNumber, players, true);

                int evalAgent = random.nextInt(players);

                for (int i = 0; i < players; i++) {
                    if (evalAgent == i) {
                        runner.addPlayer(
                                new AgentPlayer("eval", AgentUtils.buildAgent("pmctsND[iggi|iggi|iggi|iggi|iggi]")));
                    } else {
                        runner.addPlayer(new AgentPlayer("iggi", AgentUtils.buildAgent("iggi")));
                    }
                }

                GameStats stats = runner.playGame(random.nextLong());
                results.add(stats);
            }
        }

        System.out.println(results.stream().mapToInt(x -> x.score).summaryStatistics());
    }

}
