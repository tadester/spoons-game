package game;

import game.cards.Deck;
import game.players.Player;
import javafx.application.Platform;

import java.util.List;

public class Game {
    private final List<Player> players;
    private final Deck deck;
    private int numSpoons;
    private boolean gameOver;
    private int gameSpeed = 1000; // Default speed is 1 card per second

    public Game(List<Player> players) {
        this.players = players;
        this.deck = new Deck();
        this.numSpoons = players.size() - 1;
        this.gameOver = false;
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
        }
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public void removePlayer(Player player) {
        players.remove(player);
        numSpoons--;
    }

    public void setGameSpeed(int speed) {
        this.gameSpeed = speed;
    }

    public void startNPCPlayers() {
        for (Player player : players) {
            if (!player.getName().equals("Player 1")) {
                new Thread(() -> {
                    while (!isGameOver()) {
                        try {
                            Thread.sleep(gameSpeed);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Platform.runLater(() -> {
                            if (!isGameOver()) {
                                player.addCard(deck.drawCard());
                                checkForMatchAndSpoon(player);
                            }
                        });
                    }
                }).start();
            }
        }
    }
}
