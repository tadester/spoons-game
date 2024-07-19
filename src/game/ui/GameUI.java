package game.ui;

import game.Game;
import game.cards.Card;
import game.players.Player;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.ArrayList;
import java.util.List;

public class GameUI {

    private Game game;
    private List<Label> playerLabels;
    private VBox root;
    private Player currentPlayer;

    public GameUI() {
        setupGame();
    }

    public VBox createContent() {
        root = new VBox(10);
        root.setAlignment(Pos.CENTER);

        playerLabels = new ArrayList<>();

        for (Player player : game.getPlayers()) {
            Label label = new Label(player.toString());
            playerLabels.add(label);
            root.getChildren().add(label);
        }

        Button drawCardButton = new Button("Draw Card");
        drawCardButton.setOnAction(e -> drawCard());
        root.getChildren().add(drawCardButton);

        return root;
    }

    private void setupGame() {
        List<Player> players = new ArrayList<>();
        players.add(new Player("Player 1"));
        players.add(new Player("Player 2"));
        players.add(new Player("Player 3"));
        players.add(new Player("Player 4"));

        game = new Game(players);
        currentPlayer = players.get(0);
    }

    private void drawCard() {
        if (game.isGameOver()) return;

        Card drawnCard = game.getDeck().drawCard();
        if (drawnCard != null) {
            currentPlayer.addCard(drawnCard);
            updateUI();
            game.checkForMatchAndSpoon(currentPlayer);

            if (currentPlayer.hasSpoon()) {
                handleSpoonPick();
            }

            // Move to next player
            int currentIndex = game.getPlayers().indexOf(currentPlayer);
            currentPlayer = game.getPlayers().get((currentIndex + 1) % game.getPlayers().size());
        } else {
            endGame();
        }
    }

    private void updateUI() {
        for (int i = 0; i < game.getPlayers().size(); i++) {
            Player player = game.getPlayers().get(i);
            playerLabels.get(i).setText(player.toString());
        }
    }

    private void handleSpoonPick() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Spoon Picked!");
        alert.setHeaderText(null);
        alert.setContentText(currentPlayer.getName() + " picked a spoon!");

        alert.showAndWait();

        // Check other players for spoon pick
        for (Player player : game.getPlayers()) {
            if (player != currentPlayer) {
                player.grabSpoon();
            }
        }

        // Remove the player who didn't pick a spoon
        boolean playerOut = false;
        for (Player player : game.getPlayers()) {
            if (!player.hasSpoon()) {
                game.removePlayer(player);
                playerOut = true;
                break;
            }
        }

        if (playerOut) {
            updateUI();
            if (game.getPlayers().size() == 1) {
                endGame();
            }
        } else {
            // Reset spoons for the next round
            for (Player player : game.getPlayers()) {
                player.resetSpoon();
            }
        }
    }

    private void endGame() {
        game.setGameOver(true);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(null);
        alert.setContentText("Game Over! " + game.getPlayers().get(0).getName() + " wins!");
        alert.showAndWait();
    }
}
