package game.ui;

import game.Game;
import game.cards.Card;
import game.players.Player;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private Stage primaryStage;
    private Label turnLabel;
    private boolean cheatMode = false;
    private boolean selectingReplacement = false;
    private Map<String, Image> cardImagesCache = new HashMap<>();

    private static final int CARD_WIDTH = 70;
    private static final int CARD_HEIGHT = 100;

    public GameUI(Stage primaryStage) {
        this.primaryStage = primaryStage;
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
        for (int i = 0; i < game.getPlayers().size() - 1; i++) {
            ImageView spoonImage = new ImageView(loadImage("file:src/images/spoon.png"));
            spoonImage.setFitHeight(50);
            spoonImage.setFitWidth(50);
            spoonsBox.getChildren().add(spoonImage);
        }

        ImageView deckImage = new ImageView(loadImage("file:src/images/card_back.png"));
        deckImage.setFitHeight(CARD_HEIGHT);
        deckImage.setFitWidth(CARD_WIDTH);
        deckImage.setOnMouseClicked(e -> drawCard());

        centerBox.getChildren().addAll(deckImage, spoonsBox);

        gameBoard.getChildren().add(centerBox);

        BorderPane board = new BorderPane();

        playerLabels = new ArrayList<>();
        playerHands = new ArrayList<>();
        playerSpoons = new ArrayList<>();

        for (Player player : game.getPlayers()) {
            VBox playerBox = createPlayerBox(player);

            if (player.getName().equals("Player 1")) {
                board.setBottom(playerBox);
            } else if (player.getName().equals("Player 2")) {
                board.setLeft(playerBox);
            } else if (player.getName().equals("Player 3")) {
                board.setTop(playerBox);
            } else if (player.getName().equals("Player 4")) {
                board.setRight(playerBox);
            }
        }

        board.setCenter(gameBoard);
        root.getChildren().add(board);

        turnLabel = new Label();
        turnLabel.setText("Turn: " + game.getCurrentPlayer().getName());
        root.getChildren().add(turnLabel);

        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER);

        Button drawCardButton = new Button("Draw Card");
        drawCardButton.setOnAction(e -> drawCard());

        Button pickSpoonButton = new Button("Pick Spoon");
        pickSpoonButton.setOnAction(e -> pickSpoon());

        Button selectCardButton = new Button("Select Card to Replace");
        selectCardButton.setOnAction(e -> {
            showAlert("Replace Card", "Click the card you want to replace.");
            selectingReplacement = true;
            System.out.println("Select Card to Replace button clicked. selectingReplacement set to true.");
        });

        Button confirmReplaceButton = new Button("Confirm Replace");
        confirmReplaceButton.setOnAction(e -> confirmReplaceCard());

        Button cheatButton = new Button("Cheat");
        cheatButton.setOnAction(e -> toggleCheatMode());

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> showPauseMenu());

        buttonsBox.getChildren().addAll(drawCardButton, pickSpoonButton, selectCardButton, confirmReplaceButton, cheatButton, pauseButton);
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
        handBox.setAlignment(Pos.CENTER);

        if (player.getName().equals("Player 2") || player.getName().equals("Player 4")) {
            handBox.setRotate(player.getName().equals("Player 2") ? 90 : -90);
        } else if (player.getName().equals("Player 3")) {
            handBox.setRotate(180);
        }
        playerHands.add(handBox);

        ImageView spoonImage = new ImageView(loadImage("file:src/images/spoon.png"));
        spoonImage.setFitHeight(30);
        spoonImage.setFitWidth(30);
        spoonImage.setVisible(false);
        playerSpoons.add(spoonImage);

        playerBox.getChildren().addAll(label, handBox, spoonImage);

        // Do not consume the event here, just log it
        playerBox.setOnMouseClicked(e -> System.out.println("PlayerBox VBox clicked: " + player.getName()));

        return playerBox;
    }

    private ImageView createCardImageView(Card card) {
        ImageView cardImageView = new ImageView(loadImage("file:src/images/cards/" + card.toString() + ".png"));
        cardImageView.setFitHeight(CARD_HEIGHT);
        cardImageView.setFitWidth(CARD_WIDTH);

        // Setting the event handler for the image
        cardImageView.setOnMouseClicked(e -> {
            System.out.println("Card image clicked: " + card); // Ensure this is printed
            e.consume(); // Prevent event propagation
            cardImageView.setStyle("-fx-border-color: yellow; -fx-border-width: 2px;"); // Highlight to confirm click
            selectedCard = card; // Set the selected card
        });

        return cardImageView;
    }

    private void confirmReplaceCard() {
        System.out.println("Confirming replacement. Selected card: " + selectedCard);
        if (selectedCard != null && currentPlayer.getName().equals("Player 1")) {
            System.out.println("Replacing card: " + selectedCard);
            currentPlayer.getHand().remove(selectedCard);
            Card newCard = game.getDeck().drawCard();
            currentPlayer.addCard(newCard);
            Platform.runLater(this::updateUI);
            selectedCard = null; // Reset after confirming replacement
            selectingReplacement = false;
            System.out.println("Card replaced successfully.");
            // Move to next player
            game.nextTurn();
            currentPlayer = game.getCurrentPlayer();
            turnLabel.setText("Turn: " + currentPlayer.getName());
        } else {
            System.out.println("No card selected or it's not your turn.");
            showAlert("Replace Card", "No card selected or it's not your turn.");
        }
    }

    private void toggleCheatMode() {
        cheatMode = !cheatMode;
        updateUI();
    }

    private void updateUI() {
        for (int i = 0; i < game.getPlayers().size(); i++) {
            Player player = game.getPlayers().get(i);
            HBox handBox = playerHands.get(i);
            handBox.getChildren().clear();
            if (player.getName().equals("Player 1")) {
                for (Card card : player.getHand()) {
                    ImageView cardImageView = createCardImageView(card);
                    handBox.getChildren().add(cardImageView);
                }
            } else {
                for (int j = 0; j < player.getHand().size(); j++) {
                    Card card = player.getHand().get(j);
                    ImageView cardImage;
                    if (cheatMode) {
                        cardImage = new ImageView(loadImage("file:src/images/cards/" + card.toString() + ".png"));
                    } else {
                        cardImage = new ImageView(loadImage("file:src/images/card_back.png"));
                    }
                    cardImage.setFitHeight(CARD_HEIGHT);
                    cardImage.setFitWidth(CARD_WIDTH);
                    handBox.getChildren().add(cardImage);
                }
            }

            playerSpoons.get(i).setVisible(player.hasSpoon());
        }

        turnLabel.setText("Turn: " + game.getCurrentPlayer().getName());
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

    private void handleSpoonPick() {
        if (!spoonsBox.getChildren().isEmpty()) {
            spoonsBox.getChildren().remove(0);
        }

        for (Player player : game.getPlayers()) {
            if (!player.hasSpoon()) {
                player.grabSpoon();
            }
        }

        boolean playerOut = false;
        for (Player player : game.getPlayers()) {
            if (!player.hasSpoon()) {
                game.removePlayer(player);
                playerOut = true;
                break;
            }
        }

        if (playerOut) {
            Platform.runLater(this::updateUI);
            if (game.getPlayers().size() == 1) {
                endGame();
            } else {
                game.startNewRound();
                turnLabel.setText("Turn: " + game.getCurrentPlayer().getName());
            }
        } else {
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
        showMainMenu();
    }

    private void showMainMenu() {
        primaryStage.setScene(new Scene(new MenuUI(primaryStage).createContent(), 800, 600));
    }

    private void showPauseMenu() {
        stopExecutor();
        Alert pauseAlert = new Alert(Alert.AlertType.INFORMATION);
        pauseAlert.setTitle("Game Paused");
        pauseAlert.setHeaderText(null);
        pauseAlert.setContentText("The game is paused. Would you like to return to the main menu?");

        ButtonType resumeButton = new ButtonType("Resume");
        ButtonType mainMenuButton = new ButtonType("Main Menu");

        pauseAlert.getButtonTypes().setAll(resumeButton, mainMenuButton);

        Optional<ButtonType> result = pauseAlert.showAndWait();
        if (result.isPresent() && result.get() == mainMenuButton) {
            showMainMenu();
        } else {
            startExecutor();
        }
    }

    private void pickSpoon() {
        if (currentPlayer.checkForMatch() || game.isRaceStarted()) {
            game.pickSpoon(currentPlayer);
            handleSpoonPick();
        } else {
            showAlert("Cannot Grab Spoon", "You cannot grab a spoon without having four cards of the same suit or someone else grabbing a spoon.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private Image loadImage(String path) {
        if (!cardImagesCache.containsKey(path)) {
            cardImagesCache.put(path, new Image(path));
        }
        return cardImagesCache.get(path);
    }
}
