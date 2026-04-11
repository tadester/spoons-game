package game;

import game.cards.Card;
import game.cards.Deck;
import game.players.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SpoonsEngine {

    private final List<Player> players;
    private final Deck deck;
    private final List<Card> discardPile;

    private int currentPlayerIndex;
    private int spoonsRemaining;
    private boolean raceActive;
    private boolean gameOver;
    private Card pendingDraw;

    public SpoonsEngine(List<String> names) {
        this.players = new ArrayList<>();
        for (String name : names) {
            this.players.add(new Player(name));
        }
        this.deck = new Deck();
        this.discardPile = new ArrayList<>();
        startNewRound();
    }

    public void startNewRound() {
        deck.reset();
        discardPile.clear();
        pendingDraw = null;
        raceActive = false;
        spoonsRemaining = Math.max(0, players.size() - 1);
        currentPlayerIndex = 0;

        for (Player player : players) {
            player.resetRoundState();
        }

        for (int cardIndex = 0; cardIndex < 4; cardIndex++) {
            for (Player player : players) {
                Card drawnCard = drawFromDeck();
                if (drawnCard != null) {
                    player.addCard(drawnCard);
                }
            }
        }
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public int getSpoonsRemaining() {
        return spoonsRemaining;
    }

    public boolean isRaceActive() {
        return raceActive;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public Card getPendingDraw() {
        return pendingDraw;
    }

    public boolean humanCanDraw() {
        return !gameOver && !raceActive && currentPlayerIndex == 0 && pendingDraw == null;
    }

    public boolean humanCanGrabSpoon() {
        if (gameOver || players.isEmpty()) {
            return false;
        }
        Player human = players.get(0);
        return !human.hasSpoon() && (raceActive || human.checkForMatch());
    }

    public Card drawForHuman() {
        if (!humanCanDraw()) {
            return null;
        }
        pendingDraw = drawFromDeck();
        return pendingDraw;
    }

    public TurnOutcome discardForHuman(Card selectedDiscard) {
        if (currentPlayerIndex != 0 || pendingDraw == null || selectedDiscard == null || raceActive) {
            return null;
        }

        Player human = players.get(0);
        Card drawnCard = pendingDraw;
        applyDiscard(human, drawnCard, selectedDiscard);
        pendingDraw = null;

        boolean matched = human.checkForMatch();
        advanceTurn();

        return new TurnOutcome(0, human.getName(), drawnCard, selectedDiscard, matched, currentPlayerIndex);
    }

    public TurnOutcome takeNpcTurn() {
        if (gameOver || raceActive || currentPlayerIndex == 0) {
            return null;
        }

        int actingPlayerIndex = currentPlayerIndex;
        Player player = players.get(actingPlayerIndex);
        Card drawnCard = drawFromDeck();
        if (drawnCard == null) {
            return null;
        }

        Card selectedDiscard = chooseDiscard(player, drawnCard);
        applyDiscard(player, drawnCard, selectedDiscard);
        boolean matched = player.checkForMatch();
        advanceTurn();

        return new TurnOutcome(actingPlayerIndex, player.getName(), drawnCard, selectedDiscard, matched, currentPlayerIndex);
    }

    public SpoonGrabResult grabSpoon(int playerIndex) {
        if (gameOver || playerIndex < 0 || playerIndex >= players.size() || spoonsRemaining <= 0) {
            return SpoonGrabResult.invalid();
        }

        Player player = players.get(playerIndex);
        if (player.hasSpoon() || (!raceActive && !player.checkForMatch())) {
            return SpoonGrabResult.invalid();
        }

        if (!raceActive) {
            raceActive = true;
        }

        player.grabSpoon();
        spoonsRemaining--;

        RoundResult roundResult = null;
        if (spoonsRemaining == 0) {
            roundResult = finishRace();
        }

        return SpoonGrabResult.success(playerIndex, player.getName(), roundResult);
    }

    private RoundResult finishRace() {
        Player loser = null;
        for (Player player : players) {
            if (!player.hasSpoon()) {
                loser = player;
                break;
            }
        }

        if (loser == null) {
            return null;
        }

        loser.addLetter();
        String loserName = loser.getName();
        String letters = loser.getLetterProgress();
        boolean eliminated = loser.isEliminated();

        if (eliminated && loser == players.get(0)) {
            gameOver = true;
            return new RoundResult(loserName, letters, true, "The table", true);
        }

        if (eliminated) {
            players.remove(loser);
        }

        if (players.size() == 1) {
            gameOver = true;
            return new RoundResult(loserName, letters, eliminated, players.get(0).getName(), true);
        }

        startNewRound();
        return new RoundResult(loserName, letters, eliminated, null, false);
    }

    private void applyDiscard(Player player, Card drawnCard, Card selectedDiscard) {
        if (!Objects.equals(selectedDiscard, drawnCard)) {
            player.getHand().remove(selectedDiscard);
            player.addCard(drawnCard);
        }
        discardPile.add(selectedDiscard);
    }

    private Card chooseDiscard(Player player, Card drawnCard) {
        List<Card> candidates = new ArrayList<>(player.getHand());
        candidates.add(drawnCard);

        return candidates.stream()
            .max(Comparator.comparingInt(candidate -> scoreKeeping(player, drawnCard, candidate)))
            .orElse(drawnCard);
    }

    private int scoreKeeping(Player player, Card drawnCard, Card discardCandidate) {
        List<Card> keptCards = new ArrayList<>(player.getHand());
        if (!Objects.equals(discardCandidate, drawnCard)) {
            keptCards.remove(discardCandidate);
            keptCards.add(drawnCard);
        }

        Map<String, Integer> counts = new HashMap<>();
        int bestGroup = 0;
        for (Card keptCard : keptCards) {
            int count = counts.getOrDefault(keptCard.getValue(), 0) + 1;
            counts.put(keptCard.getValue(), count);
            bestGroup = Math.max(bestGroup, count);
        }

        int pairs = 0;
        for (int valueCount : counts.values()) {
            if (valueCount >= 2) {
                pairs++;
            }
        }

        return bestGroup * 100 + pairs;
    }

    private void advanceTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    private Card drawFromDeck() {
        Card drawnCard = deck.drawCard();
        if (drawnCard != null) {
            return drawnCard;
        }

        if (discardPile.isEmpty()) {
            return null;
        }

        deck.replenish(new ArrayList<>(discardPile));
        discardPile.clear();
        return deck.drawCard();
    }

    public static final class TurnOutcome {
        private final int playerIndex;
        private final String playerName;
        private final Card drawnCard;
        private final Card discardedCard;
        private final boolean matched;
        private final int nextPlayerIndex;

        public TurnOutcome(int playerIndex, String playerName, Card drawnCard, Card discardedCard, boolean matched, int nextPlayerIndex) {
            this.playerIndex = playerIndex;
            this.playerName = playerName;
            this.drawnCard = drawnCard;
            this.discardedCard = discardedCard;
            this.matched = matched;
            this.nextPlayerIndex = nextPlayerIndex;
        }

        public int getPlayerIndex() {
            return playerIndex;
        }

        public String getPlayerName() {
            return playerName;
        }

        public Card getDrawnCard() {
            return drawnCard;
        }

        public Card getDiscardedCard() {
            return discardedCard;
        }

        public boolean isMatched() {
            return matched;
        }

        public int getNextPlayerIndex() {
            return nextPlayerIndex;
        }
    }

    public static final class SpoonGrabResult {
        private final boolean success;
        private final int playerIndex;
        private final String playerName;
        private final RoundResult roundResult;

        private SpoonGrabResult(boolean success, int playerIndex, String playerName, RoundResult roundResult) {
            this.success = success;
            this.playerIndex = playerIndex;
            this.playerName = playerName;
            this.roundResult = roundResult;
        }

        public static SpoonGrabResult invalid() {
            return new SpoonGrabResult(false, -1, "", null);
        }

        public static SpoonGrabResult success(int playerIndex, String playerName, RoundResult roundResult) {
            return new SpoonGrabResult(true, playerIndex, playerName, roundResult);
        }

        public boolean isSuccess() {
            return success;
        }

        public int getPlayerIndex() {
            return playerIndex;
        }

        public String getPlayerName() {
            return playerName;
        }

        public RoundResult getRoundResult() {
            return roundResult;
        }
    }

    public static final class RoundResult {
        private final String loserName;
        private final String letters;
        private final boolean eliminated;
        private final String winnerName;
        private final boolean gameFinished;

        public RoundResult(String loserName, String letters, boolean eliminated, String winnerName, boolean gameFinished) {
            this.loserName = loserName;
            this.letters = letters;
            this.eliminated = eliminated;
            this.winnerName = winnerName;
            this.gameFinished = gameFinished;
        }

        public String getLoserName() {
            return loserName;
        }

        public String getLetters() {
            return letters;
        }

        public boolean isEliminated() {
            return eliminated;
        }

        public String getWinnerName() {
            return winnerName;
        }

        public boolean isGameFinished() {
            return gameFinished;
        }
    }
}
