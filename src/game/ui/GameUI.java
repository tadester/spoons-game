package game.ui;

import game.Game;
import game.cards.Card;
import game.players.Player;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameUI {

    private Game game;
    private List<Label> playerLabels;
    private List<HBox> playerHands;
    private List<ImageView> playerSpoons;
    private VBox root;
    private Player currentPlayer;
    private Card selectedCard = null;
    private ScheduledExecutorService executor;
    private HBox spoonsBox;

    public GameUI() {
        setupGame();
    }

    public VBox createContent() {
        root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        root.setBackground(new Background(new BackgroundFill(Color.LIGHTBLUE, null, null)));

        StackPane gameBoard = new StackPane();
        gameBoard.setAlignment(Pos.CENTER);

        VBox centerBox = new VBox(10);
        centerBox.setAlignment(Pos.CENTER);

        spoonsBox = new HBox(10);
        spoonsBox.setAlignment(Pos.CENTER);
        for (int i = 0; i < 3; i++) {
            ImageView spoonImage = new ImageView(new Image("file:src/images/spoon.png"));
            spoonImage.setFitHeight(50);
            spoonImage.setFitWidth(50);
            spoonsBox.getChildren().add(spoonImage);
        }

        ImageView deckImage = new ImageView(new Image("file:src/images/card_back.png"));
        deckImage.setFitHeight(100);
        deckImage.setFitWidth(70);
        centerBox.getChildren().addAll(deckImage, spoonsBox);

        gameBoard.getChildren().add(centerBox);

        BorderPane board = new BorderPane();

        playerLabels = new ArrayList<>();
        playerHands = new ArrayList<>();
        playerSpoons = new ArrayList<>();

        for (Player player : game.getPlayers()) {
            if (player.getName().equals("Player 1")) {
                VBox playerBox = createPlayerBox(player);
                board.setBottom(playerBox);
            } else if (player.getName().equals("Player 2")) {
                VBox playerBox = createPlayerBox(player);
                board.setLeft(playerBox);
            } else if (player.getName().equals("Player 3")) {
                VBox playerBox = createPlayerBox(player);
                board.setTop(playerBox);
            } else if (player.getName().equals("Player 4")) {
                VBox playerBox = createPlayerBox(player);
                board.setRight(playerBox);
            }
        }

        board.setCenter(gameBoard);
        root.getChildren().add(board);

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

        startExecutor();

        return root;
    }

    private VBox createPlayerBox(Player player) {
        VBox playerBox = new VBox(5);
        playerBox.setAlignment(Pos.CENTER);

        Label label = new Label(player.getName());
        playerLabels.add(label);

        HBox handBox = new HBox(5);
        if (player.getName().equals("Player 2") || player.getName().equals("Player 4")) {
            handBox.setRotate(player.getName().equals("Player 2") ? 90 : -90);
        } else if (player.getName().equals("Player 3")) {
            handBox.setRotate(180);
        }
        playerHands.add(handBox);

        ImageView spoonImage = new ImageView(new Image("file:src/images/spoon.png"));
        spoonImage.setFitHeight(30);
        spoonImage.setFitWidth(30);
        spoonImage.setVisible(false);
        playerSpoons.add(spoonImage);

        playerBox.getChildren().addAll(label, handBox, spoonImage);
        return playerBox;
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

    private void startExecutor() {
        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(() -> {
            Platform.runLater(this::updateUI);
        }, 0, 33, TimeUnit.MILLISECONDS); // 30 FPS
    }

    private void stopExecutor() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
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
            game.nextTurn();
            currentPlayer = game.getCurrentPlayer();
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
            if (player.getName().equals("Player 1")) {
                for (Card card : player.getHand()) {
                    ImageView cardImage = new ImageView(new Image("file:src/images/cards/" + card.toString() + ".png"));
                    cardImage.setFitHeight(100);
                    cardImage.setFitWidth(70);
                    handBox.getChildren().add(cardImage);
                }
            } else {
                for (int j = 0; j < player.getHand().size(); j++) {
                    ImageView cardImage = new ImageView(new Image("file:src/images/card_back.png"));
                    cardImage.setFitHeight(100);
                    cardImage.setFitWidth(70);
                    handBox.getChildren().add(cardImage);
                }
            }

            playerSpoons.get(i).setVisible(player.hasSpoon());
        }
    }

    private void handleSpoonPick() {
        // Remove one spoon from the table
        if (!spoonsBox.getChildren().isEmpty()) {
            spoonsBox.getChildren().remove(0);
        }

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
        stopExecutor();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(null);
        alert.setContentText("Game Over! " + game.getPlayers().get(0).getName() + " wins!");
        alert.showAndWait();
    }

    private void pickSpoon() {
        game.pickSpoon(currentPlayer);
        handleSpoonPick();
    }
}
