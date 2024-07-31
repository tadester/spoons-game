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
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

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
    private ImageView selectedCardImage = null;
    private Map<String, Image> cardImagesCache = new HashMap<>(); // Cache for card images

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
        deckImage.setFitHeight(100);
        deckImage.setFitWidth(70);
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

        Button replaceCardButton = new Button("Replace Card");
        replaceCardButton.setOnAction(e -> {
            showAlert("Replace Card", "Click the card you want to replace.");
            selectingReplacement = true;
        });

        Button cheatButton = new Button("Cheat");
        cheatButton.setOnAction(e -> toggleCheatMode());

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> showPauseMenu());

        buttonsBox.getChildren().addAll(drawCardButton, pickSpoonButton, replaceCardButton, cheatButton, pauseButton);
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

        if (currentPlayer.getName().equals("Player 1")) {
            if (!game.isInitialDrawComplete() || currentPlayer.getHand().size() < 4) {
                Card drawnCard = game.getDeck().drawCard();
                if (drawnCard != null) {
                    if (currentPlayer.getHand().size() < 4) {
                        currentPlayer.addCard(drawnCard);
                    } else {
                        selectedCard = drawnCard;
                    }
                    Platform.runLater(this::updateUI);
                    game.checkForMatchAndSpoon(currentPlayer);

                    // Move to next player
                    game.nextTurn();
                    currentPlayer = game.getCurrentPlayer();
                    turnLabel.setText("Turn: " + currentPlayer.getName());
                } else {
                    endGame();
                }
            }
        } else {
            showAlert("Not Your Turn", "Please wait for your turn to draw a card.");
        }
    }

    private void replaceCard() {
        if (selectedCard != null && currentPlayer.getName().equals("Player 1")) {
            System.out.println("Replacing card: " + selectedCard);
            currentPlayer.replaceCard(currentPlayer.getHand().indexOf(selectedCard));
            currentPlayer.addCard(selectedCard);
            Platform.runLater(this::updateUI);
            selectedCard = null;
            selectedCardImage = null;
            selectingReplacement = false;
            // Move to next player
            game.nextTurn();
            currentPlayer = game.getCurrentPlayer();
            turnLabel.setText("Turn: " + currentPlayer.getName());
        } else if (!selectingReplacement) {
            showAlert("Replace Card", "Select a card to replace.");
        } else {
            System.out.println("Selecting card to replace...");
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
                    ImageView cardImage = createCardImageView(card);
                    handBox.getChildren().add(cardImage);
                }
            } else {
                for (int j = 0; j < player.getHand().size(); j++) {
                    Card card = player.getHand().get(j);
                    ImageView cardImage;
                    if (cheatMode) {
                        cardImage = createCardImageView(card);
                    } else {
                        cardImage = new ImageView(loadImage("file:src/images/card_back.png"));
                        cardImage.setFitHeight(100);
                        cardImage.setFitWidth(70);
                    }
                    handBox.getChildren().add(cardImage);
                }
            }

            playerSpoons.get(i).setVisible(player.hasSpoon());
        }

        turnLabel.setText("Turn: " + game.getCurrentPlayer().getName());
    }

    private ImageView createCardImageView(Card card) {
        ImageView cardImage = new ImageView(loadImage("file:src/images/cards/" + card.toString() + ".png"));
        cardImage.setFitHeight(100);
        cardImage.setFitWidth(70);

        if (selectingReplacement && currentPlayer.getName().equals("Player 1")) {
            cardImage.setOnMouseClicked(e -> {
                selectedCard = card;
                selectedCardImage = cardImage;
                System.out.println("Card selected for replacement: " + card);
                replaceCard();
            });
        }

        return cardImage;
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
