package game;

import game.cards.Deck;
import game.players.Player;

import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.io.*;

public class Game {
    private final List<Player> players;
    private final Deck deck;
    private int numSpoons;
    private boolean gameOver;
    private int currentPlayerIndex;
    private Timer timer;
    private boolean raceStarted;
    private boolean initialDrawComplete;
    private int gameSpeed;
    private Random random;
    private Player currentPlayer;

    public Game(List<Player> players) {
        this.players = players;
        this.deck = new Deck();
        this.numSpoons = players.size() - 1;
        this.gameOver = false;
        this.currentPlayerIndex = 0;
        this.timer = new Timer();
        this.raceStarted = false;
        this.initialDrawComplete = false;
        this.random = new Random();
        this.currentPlayer = players.get(0);
        loadSettings();
    }

    public Deck getDeck() {
        return deck;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public boolean isRaceStarted() {
        return raceStarted;
    }

    public void checkForMatchAndSpoon(Player player) {
        if (player.checkForMatch()) {
            if (!raceStarted) {
                raceStarted = true;
                player.grabSpoon();
                startRaceTimer();
            }
        }
    }

    private void startRaceTimer() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                for (Player player : players) {
                    if (!player.hasSpoon()) {
                        int delay = random.nextInt(10000) + 1000; // Random delay between 1-10 seconds
                        startCPUSpoonTimer(player, delay);
                    }
                }
            }
        }, 0);
    }

    private void startCPUSpoonTimer(Player player, int delay) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!gameOver && !player.hasSpoon()) {
                    player.grabSpoon();
                    handleSpoonPick(player);
                }
            }
        }, delay);
    }

    public void pickSpoon(Player player) {
        player.grabSpoon();
        handleSpoonPick(player);
    }

    private void handleSpoonPick(Player currentPlayer) {
        // Check other players for spoon pick
        for (Player player : players) {
            if (player != currentPlayer && !player.hasSpoon()) {
                player.grabSpoon();
            }
        }

        // Remove the player who didn't pick a spoon
        boolean playerOut = false;
        for (Player player : players) {
            if (!player.hasSpoon()) {
                removePlayer(player);
                playerOut = true;
                break;
            }
        }

        if (playerOut) {
            if (players.size() == 1) {
                gameOver = true;
                System.out.println("Game Over! " + players.get(0).getName() + " wins!");
            } else {
                startNewRound();
            }
        }
    }

    public void startNewRound() {
        System.out.println("Starting a new round.");
        deck.reset();
        for (Player player : players) {
            player.resetHand();
            player.resetSpoon();
        }
        numSpoons = players.size() - 1;
        currentPlayerIndex = 0;
        raceStarted = false;
        gameOver = false;
        initialDrawComplete = false;
        currentPlayer = players.get(0);
    }

    public void removePlayer(Player player) {
        System.out.println("Removing player: " + player.getName());
        players.remove(player);
        numSpoons--;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public void nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        currentPlayer = players.get(currentPlayerIndex);
        System.out.println("Next turn: " + currentPlayer.getName());
    }

    public void startNPCPlayers() {
        new Thread(() -> {
            while (!isGameOver()) {
                try {
                    Thread.sleep(gameSpeed); // Use gameSpeed for NPC turns
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Player currentPlayer = getCurrentPlayer();
                if (!currentPlayer.getName().equals("Player 1") && !isGameOver()) {
                    if (!initialDrawComplete) {
                        if (currentPlayer.getHand().size() < 4) {
                            currentPlayer.addCard(deck.drawCard());
                        }
                    } else {
                        if (currentPlayer.getHand().size() == 4) {
                            currentPlayer.addCard(deck.drawCard());
                            currentPlayer.replaceCard(random.nextInt(4));
                        }
                    }
                    checkForMatchAndSpoon(currentPlayer);
                    nextTurn();
                }
            }
        }).start();
    }

    public void setGameSpeed(int speed) {
        this.gameSpeed = speed;
    }

    public boolean isInitialDrawComplete() {
        for (Player player : players) {
            if (player.getHand().size() < 4) {
                return false;
            }
        }
        initialDrawComplete = true;
        return true;
    }

    private void loadSettings() {
        try (BufferedReader reader = new BufferedReader(new FileReader("settings.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("gameSpeed=")) {
                    gameSpeed = Integer.parseInt(line.split("=")[1]);
                }
            }
        } catch (IOException e) {
            gameSpeed = 5000; // default value
        }
    }
}
