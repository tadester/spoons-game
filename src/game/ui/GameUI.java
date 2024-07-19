package game.ui;

import game.Game;
import game.cards.Card;
import game.players.Player;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class GameUI {

    private Game game;
    private List<Label> playerLabels;
    private List<ImageView> playerCards;
    private VBox root;
    private Player currentPlayer;

    public GameUI() {
        setupGame();
    }

    public VBox createContent() {
        root = new VBox(10);
        root.setAlignment(Pos.CENTER);

        HBox playerBox = new HBox(10);
        playerBox.setAlignment(Pos.CENTER);
        playerLabels = new ArrayList<>();
        playerCards = new ArrayList<>();

        for (Player player : game.getPlayers()) {
            Label label = new Label(player.getName());
            playerLabels.add(label);
            playerBox.getChildren().add(label);

            ImageView cardBack = new ImageView(new Image("file:src/images/card_back.png"));
            cardBack.setFitHeight(150);
            cardBack.setFitWidth(100);
            playerCards.add(cardBack);
            playerBox.getChildren().add(cardBack);
        }

        root.getChildren().add(playerBox);

        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER);

        Button drawCardButton = new Button("Draw Card");
        drawCardButton.setOnAction(e -> drawCard());

        Button pickSpoonButton = new Button("Pick Spoon");
        pickSpoonButton.setOnAction(e -> pickSpoon());

        buttonsBox.getChildren().addAll(drawCardButton, pickSpoonButton);
        root.getChildren().add(buttonsBox);

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
            playerLabels.get(i).setText(player.getName() + "'s hand: " + player.getHand().toString());
            if (!player.getHand().isEmpty()) {
                Card lastCard = player.getHand().get(player.getHand().size() - 1);
                ImageView cardImage = new ImageView(new Image("file:src/images/cards/" + lastCard.toString() + ".png"));
                cardImage.setFitHeight(150);
                cardImage.setFitWidth(100);
                playerCards.set(i, cardImage);
                root.getChildren().set(i + 1, cardImage);
            }
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

    private void pickSpoon() {
        currentPlayer.grabSpoon();
        handleSpoonPick();
    }
}
