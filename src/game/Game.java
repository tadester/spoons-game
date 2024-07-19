package game;

import game.cards.Deck;
import game.players.Player;
import java.util.List;

public class Game {
    private final List<Player> players;
    private final Deck deck;
    private int numSpoons;
    private boolean gameOver;

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
}
