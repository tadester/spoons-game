package game;

import game.cards.Deck;
import game.players.Player;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Game {
    private final List<Player> players;
    private final Deck deck;
    private int numSpoons;
    private boolean gameOver;
    private int currentPlayerIndex;
    private Timer timer;
    private boolean player1HasSpoon;
    private int gameSpeed;

    public Game(List<Player> players) {
        this.players = players;
        this.deck = new Deck();
        this.numSpoons = players.size() - 1;
        this.gameOver = false;
        this.currentPlayerIndex = 0;
        this.timer = new Timer();
        this.player1HasSpoon = false;
        this.gameSpeed = 5000; // Default speed 5 seconds per turn
    }

    public Deck getDeck() {
        return deck;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void checkForMatchAndSpoon(Player player) {
        if (player.checkForMatch()) {
            player.grabSpoon();
            if (player.getName().equals("Player 1")) {
                startPlayer1SpoonTimer();
            }
        }
    }

    private void startPlayer1SpoonTimer() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!player1HasSpoon && !gameOver) {
                    gameOver = true;
                    // Player 1 loses
                    System.out.println("Player 1 did not pick the spoon in time and loses!");
                    removePlayer(players.get(0));
                }
            }
        }, 5000); // 5 seconds to pick the spoon
    }

    public void pickSpoon(Player player) {
        if (player.getName().equals("Player 1")) {
            player1HasSpoon = true;
            timer.cancel();
        }
        player.grabSpoon();
        handleSpoonPick(player);
    }

    private void handleSpoonPick(Player currentPlayer) {
        // Check other players for spoon pick
        for (Player player : players) {
            if (player != currentPlayer) {
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

        if (!playerOut) {
            // Reset spoons for the next round
            for (Player player : players) {
                player.resetSpoon();
            }
        }

        if (players.size() == 1) {
            gameOver = true;
            System.out.println("Game Over! " + players.get(0).getName() + " wins!");
        }
    }

    public void removePlayer(Player player) {
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
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
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
                    currentPlayer.addCard(deck.drawCard());
                    checkForMatchAndSpoon(currentPlayer);
                    nextTurn();
                }
            }
        }).start();
    }

    public void setGameSpeed(int speed) {
        this.gameSpeed = speed;
    }
}
