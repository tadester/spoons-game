package game.ui;

import game.Game;
import game.cards.Card;
import game.players.Player;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;

public class GameUI {

    private Game game;
    private List<Label> playerLabels;
    private List<HBox> playerHands;
    private List<ImageView> playerSpoons;
    private VBox root;
    private Player currentPlayer;
    private Card selectedCard = null;

    public GameUI() {
        setupGame();
    }

    public VBox createContent() {
        root = new VBox(10);
        root.setAlignment(Pos.CENTER);

        HBox playersBox = new HBox(20);
        playersBox.setAlignment(Pos.CENTER);
        playerLabels = new ArrayList<>();
        playerHands = new ArrayList<>();
        playerSpoons = new ArrayList<>();

        for (Player player : game.getPlayers()) {
            VBox playerBox = new VBox(5);
            playerBox.setAlignment(Pos.CENTER);

            Label label = new Label(player.getName());
            playerLabels.add(label);

            HBox handBox = new HBox(5);
            playerHands.add(handBox);

            ImageView spoonImage = new ImageView(new Image("file:src/images/spoon.png"));
            spoonImage.setFitHeight(30);
            spoonImage.setFitWidth(30);
            spoonImage.setVisible(false);
            playerSpoons.add(spoonImage);

            playerBox.getChildren().addAll(label, handBox, spoonImage);
            playersBox.getChildren().add(playerBox);
        }

        root.getChildren().add(playersBox);

        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER);

        Button drawCardButton = new Button("Draw Card");
        drawCardButton.setOnAction(e -> drawCard());

        Button pickSpoonButton = new Button("Pick Spoon");
        pickSpoonButton.setOnAction(e -> pickSpoon());

        Button replaceCardButton = new Button("Replace Card");
        replaceCardButton.setOnAction(e -> replaceCard());

        buttonsBox.getChildren().addAll(drawCardButton, pickSpoonButton, replaceCardButton);
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
        game.startNPCPlayers();
    }

    private void drawCard() {
        if (game.isGameOver()) return;

        Card drawnCard = game.getDeck().drawCard();
        if (drawnCard != null) {
            if (currentPlayer.getHand().size() < 4) {
                currentPlayer.addCard(drawnCard);
            } else {
                selectedCard = drawnCard;
            }
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

    private void replaceCard() {
        if (selectedCard != null) {
            currentPlayer.addCard(selectedCard);
            updateUI();
            selectedCard = null;
        }
    }

    private void updateUI() {
        for (int i = 0; i < game.getPlayers().size(); i++) {
            Player player = game.getPlayers().get(i);
            HBox handBox = playerHands.get(i);
            handBox.getChildren().clear();
            for (Card card : player.getHand()) {
                ImageView cardImage = new ImageView(new Image("file:src/images/cards/" + card.toString() + ".png"));
                cardImage.setFitHeight(100);
                cardImage.setFitWidth(70);
                handBox.getChildren().add(cardImage);
            }

            playerSpoons.get(i).setVisible(player.hasSpoon());
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
