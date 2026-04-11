package game.ui;

import game.Game;
import game.cards.Card;
import game.players.Player;
import javafx.application.Platform;
import javafx.geometry.Insets;
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

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameUI {

    private final Game game;
    private final List<Label> playerLabels;
    private final List<HBox> playerHands;
    private final List<ImageView> playerSpoons;
    private VBox root;
    private Player currentPlayer;
    private Card selectedCard = null;
    private ScheduledExecutorService executor;
    private HBox spoonsBox;
    private final Stage primaryStage;
    private Label turnLabel;
    private Label hintLabel;
    private boolean cheatMode = false;
    private boolean selectingReplacement = false;
    private final Map<String, Image> cardImagesCache = new HashMap<>();
    private final int npcReactionTimeRange;
    private int availableSpoons;  // Track the number of available spoons

    private static final int CARD_WIDTH = 70;
    private static final int CARD_HEIGHT = 100;

    public GameUI(Stage primaryStage, int npcReactionTimeRange) {
        this.primaryStage = primaryStage;
        this.npcReactionTimeRange = npcReactionTimeRange;
        playerLabels = new ArrayList<>();
        playerHands = new ArrayList<>();
        playerSpoons = new ArrayList<>();
        game = setupGame();
    }

    public VBox createContent() {
        root = new VBox(18);
        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().addAll("screen", "game-screen");

        VBox headerBox = new VBox(8);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.getStyleClass().add("hud-panel");

        Label titleLabel = new Label("Spoons Table");
        titleLabel.getStyleClass().add("hud-title");

        turnLabel = new Label("Turn: " + game.getCurrentPlayer().getName());
        turnLabel.getStyleClass().add("status-pill");

        hintLabel = new Label("Collect four cards of the same rank, then grab a spoon.");
        hintLabel.getStyleClass().add("hud-subtitle");
        hintLabel.setWrapText(true);
        hintLabel.setMaxWidth(520);

        headerBox.getChildren().addAll(titleLabel, turnLabel, hintLabel);

        StackPane gameBoard = new StackPane();
        gameBoard.setAlignment(Pos.CENTER);
        gameBoard.getStyleClass().add("table-surface");

        spoonsBox = new HBox(10);
        spoonsBox.setAlignment(Pos.CENTER);
        spoonsBox.getStyleClass().add("spoons-row");
        availableSpoons = game.getPlayers().size() - 1; // Initialize available spoons

        for (int i = 0; i < availableSpoons; i++) {
            ImageView spoonImage = new ImageView(loadImage("file:src/images/spoon.png"));
            spoonImage.setFitHeight(50);
            spoonImage.setFitWidth(50);
            spoonImage.getStyleClass().add("spoon-icon");
            spoonsBox.getChildren().add(spoonImage);
        }

        ImageView deckImage = new ImageView(loadImage("file:src/images/card_back.png"));
        deckImage.setFitHeight(CARD_HEIGHT);
        deckImage.setFitWidth(CARD_WIDTH);
        deckImage.getStyleClass().add("center-deck");
        deckImage.setOnMouseClicked(e -> drawCard());

        Label deckLabel = new Label("Draw Pile");
        deckLabel.getStyleClass().add("mini-label");

        VBox centerBox = new VBox(14, deckLabel, deckImage, spoonsBox);
        centerBox.setAlignment(Pos.CENTER);

        StackPane centerOrb = new StackPane(centerBox);
        centerOrb.getStyleClass().add("center-orb");
        centerOrb.setPadding(new Insets(28));
        gameBoard.getChildren().add(centerOrb);

        BorderPane board = new BorderPane();
        board.setPadding(new Insets(24));
        board.getStyleClass().add("game-board");

        for (Player player : game.getPlayers()) {
            VBox playerBox = createPlayerBox(player);

            if (player.getName().equals("Player 1")) {
                board.setBottom(playerBox);
                BorderPane.setMargin(playerBox, new Insets(18, 0, 0, 0));
            } else if (player.getName().equals("Player 2")) {
                board.setLeft(playerBox);
                BorderPane.setMargin(playerBox, new Insets(0, 18, 0, 0));
            } else if (player.getName().equals("Player 3")) {
                board.setTop(playerBox);
                BorderPane.setMargin(playerBox, new Insets(0, 0, 18, 0));
            } else if (player.getName().equals("Player 4")) {
                board.setRight(playerBox);
                BorderPane.setMargin(playerBox, new Insets(0, 0, 0, 18));
            }
        }

        board.setCenter(gameBoard);
        VBox.setVgrow(board, Priority.ALWAYS);

        HBox buttonsBox = new HBox(12);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.getStyleClass().add("controls-bar");

        Button drawCardButton = createActionButton("Draw Card", "primary-button");
        drawCardButton.setOnAction(e -> drawCard());

        Button pickSpoonButton = createActionButton("Grab Spoon", "accent-button");
        pickSpoonButton.setOnAction(e -> pickSpoon());

        Button selectCardButton = createActionButton("Choose Replace", "secondary-button");
        selectCardButton.setOnAction(e -> {
            if (currentPlayer.getName().equals("Player 1")) {
                showReplacementMenu();
            } else {
                showAlert("Not Your Turn", "It's not your turn.");
            }
        });

        Button confirmReplaceButton = createActionButton("Confirm Replace", "secondary-button");
        confirmReplaceButton.setOnAction(e -> confirmReplaceCard());

        Button cheatButton = createActionButton("Cheat View", "ghost-button");
        cheatButton.setOnAction(e -> toggleCheatMode());

        Button pauseButton = createActionButton("Pause", "ghost-button");
        pauseButton.setOnAction(e -> showPauseMenu());

        buttonsBox.getChildren().addAll(drawCardButton, pickSpoonButton, selectCardButton, confirmReplaceButton, cheatButton, pauseButton);
        root.getChildren().addAll(headerBox, board, buttonsBox);

        startExecutor();
        updateUI();

        return root;
    }

    private Game setupGame() {
        List<Player> players = new ArrayList<>();
        players.add(new Player("Player 1"));
        players.add(new Player("Player 2"));
        players.add(new Player("Player 3"));
        players.add(new Player("Player 4"));

        Game newGame = new Game(players);
        currentPlayer = players.get(0);
        newGame.setOnSpoonTakenCallback(this::updateSpoons);
        newGame.startNPCPlayers();
        return newGame;
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
                    if (!currentPlayer.getName().equals("Player 1")) {
                        npcTurn();
                    }
                } else {
                    endGame();
                }
            }
        } else {
            showAlert("Not Your Turn", "Please wait for your turn to draw a card.");
        }
    }

    private VBox createPlayerBox(Player player) {
        VBox playerBox = new VBox(8);
        playerBox.setAlignment(Pos.CENTER);
        playerBox.getStyleClass().add(player.getName().equals("Player 1") ? "player-panel-self" : "player-panel");
        playerBox.setPrefWidth(player.getName().equals("Player 1") ? 540 : 190);

        Label label = new Label(player.getName());
        label.getStyleClass().add("player-name");
        playerLabels.add(label);

        HBox handBox = new HBox(5);
        handBox.setAlignment(Pos.CENTER);
        handBox.getStyleClass().add("player-hand");

        // Set rotation based on player position
        if (player.getName().equals("Player 2") || player.getName().equals("Player 4")) {
            handBox.setRotate(player.getName().equals("Player 2") ? 90 : -90);
        } else if (player.getName().equals("Player 3")) {
            handBox.setRotate(180);
        }
        playerHands.add(handBox);

        ImageView spoonImage = new ImageView(loadImage("file:src/images/spoon.png"));
        spoonImage.setFitHeight(30);
        spoonImage.setFitWidth(30);
        spoonImage.getStyleClass().add("player-spoon");
        spoonImage.setVisible(false);
        playerSpoons.add(spoonImage);

        playerBox.getChildren().addAll(label, handBox, spoonImage);

        return playerBox;
    }

    private ImageView createCardImageView(Card card) {
        ImageView cardImageView = new ImageView(loadImage("file:src/images/cards/" + card.toString() + ".png"));
        cardImageView.setFitHeight(CARD_HEIGHT);
        cardImageView.setFitWidth(CARD_WIDTH);
        cardImageView.getStyleClass().add("play-card");

        cardImageView.setOnMouseClicked(e -> {
            System.out.println("Card image clicked: " + card);
            e.consume();
            cardImageView.setStyle("-fx-border-color: yellow; -fx-border-width: 2px;");
            selectedCard = card;
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
            selectedCard = null;
            selectingReplacement = false;
            System.out.println("Card replaced successfully.");
            game.nextTurn();
            currentPlayer = game.getCurrentPlayer();
            turnLabel.setText("Turn: " + currentPlayer.getName());
            if (!currentPlayer.getName().equals("Player 1")) {
                npcTurn();
            }
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
            for (int j = 0; j < player.getHand().size(); j++) {
                Card card = player.getHand().get(j);
                ImageView cardImage;
                if (cheatMode || player.getName().equals("Player 1")) {
                    cardImage = new ImageView(loadImage("file:src/images/cards/" + card.toString() + ".png"));
                } else {
                    cardImage = new ImageView(loadImage("file:src/images/card_back.png"));
                }
                cardImage.setFitHeight(CARD_HEIGHT);
                cardImage.setFitWidth(CARD_WIDTH);
                cardImage.getStyleClass().add("play-card");
                handBox.getChildren().add(cardImage);
            }
            playerSpoons.get(i).setVisible(player.hasSpoon());
        }
        turnLabel.setText("Turn: " + game.getCurrentPlayer().getName());
        hintLabel.setText(game.isRaceStarted()
            ? "Spoon race active. Grab one now."
            : "Collect four cards of the same rank, then grab a spoon.");
    }

    private void startExecutor() {
        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(() -> Platform.runLater(this::updateUI), 0, 33, TimeUnit.MILLISECONDS); // 30 FPS
    }

    private void stopExecutor() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    private void updateSpoons() {
        Platform.runLater(() -> {
            if (!spoonsBox.getChildren().isEmpty()) {
                spoonsBox.getChildren().remove(0);
                availableSpoons--;
                System.out.println("Spoons after grab: " + availableSpoons);

                if (game.isGameOver() && !game.getPlayers().get(0).hasSpoon()) {
                    showGameOverMenu();
                }
            }
        });
    }

    private void showGameOverMenu() {
        Platform.runLater(() -> {
            Alert gameOverAlert = new Alert(Alert.AlertType.INFORMATION);
            gameOverAlert.setTitle("Game Over");
            gameOverAlert.setHeaderText("You have lost!");
            gameOverAlert.setContentText("You did not grab a spoon in time.");

            ButtonType mainMenuButton = new ButtonType("Main Menu");
            ButtonType quitButton = new ButtonType("Quit");

            gameOverAlert.getButtonTypes().setAll(mainMenuButton, quitButton);

            Optional<ButtonType> result = gameOverAlert.showAndWait();
            if (result.isPresent() && result.get() == mainMenuButton) {
                showMainMenu();
            } else {
                Platform.exit();
            }
        });
    }

    private void handleSpoonPick(Player spoonPicker) {
        System.out.println("Spoons before grab: " + availableSpoons);
        
        if (availableSpoons > 0 && !isRaceFinished()) {
            spoonPicker.grabSpoon();
            availableSpoons--;
            removeSpoonFromBoard();
        }
    
        if (isRaceFinished()) {
            eliminatePlayersWithoutSpoons();
            if (game.getPlayers().size() > 1) {
                game.startNewRound();
                turnLabel.setText("Turn: " + game.getCurrentPlayer().getName());
                updateUI();
            } else {
                declareWinner(game.getPlayers().get(0));
            }
        } else {
            raceForSpoons(spoonPicker);
        }
    }

    private void removeSpoonFromBoard() {
        if (!spoonsBox.getChildren().isEmpty()) {
            spoonsBox.getChildren().remove(0);
        }
        System.out.println("Spoons after grab: " + availableSpoons);
    }

    private void raceForSpoons(Player initialPicker) {
        List<Player> remainingPlayers = new ArrayList<>(game.getPlayers());
        remainingPlayers.remove(initialPicker);
        Collections.shuffle(remainingPlayers);

        for (Player player : remainingPlayers) {
            if (!player.getName().equals("Player 1") && !player.hasSpoon() && availableSpoons > 0) {
                int delay = new Random().nextInt(npcReactionTimeRange) + 1; // Random delay between 1 and npcReactionTimeRange seconds
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                scheduler.schedule(() -> {
                    Platform.runLater(() -> {
                        if (availableSpoons > 0 && !isRaceFinished()) {
                            System.out.println("Spoons before grab: " + availableSpoons);
                            player.grabSpoon();
                            availableSpoons--;
                            removeSpoonFromBoard();
                            if (isRaceFinished()) {
                                eliminatePlayersWithoutSpoons();
                                if (game.getPlayers().size() > 1) {
                                    game.startNewRound();
                                    turnLabel.setText("Turn: " + game.getCurrentPlayer().getName());
                                    updateUI();
                                } else {
                                    declareWinner(game.getPlayers().get(0));
                                }
                            }
                        }
                    });
                }, delay, TimeUnit.SECONDS);
            }
        }
    }

    private boolean isRaceFinished() {
        return availableSpoons == 0;
    }

    private void eliminatePlayersWithoutSpoons() {
        List<Player> eliminatedPlayers = new ArrayList<>();
        for (Player player : game.getPlayers()) {
            if (!player.hasSpoon()) {
                eliminatedPlayers.add(player);
            }
        }

        game.getPlayers().removeAll(eliminatedPlayers);

        if (!eliminatedPlayers.isEmpty()) {
            Platform.runLater(() -> showEliminationPopup(eliminatedPlayers));
        }

        if (game.getPlayers().size() == 1) {
            declareWinner(game.getPlayers().get(0));
        }
    }

    private void showEliminationPopup(List<Player> eliminatedPlayers) {
        StringBuilder message = new StringBuilder("The following players have been eliminated:\n");
        for (Player player : eliminatedPlayers) {
            message.append(player.getName()).append("\n");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Elimination");
        alert.setHeaderText(null);
        alert.setContentText(message.toString());
        alert.showAndWait();
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

    private void declareWinner(Player winner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(null);
        alert.setContentText("Game Over! " + winner.getName() + " has won the game!");
        alert.showAndWait();
        showMainMenu();
    }

    private void showMainMenu() {
        primaryStage.setScene(SceneFactory.createScene(new MenuUI(primaryStage).createContent()));
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
            handleSpoonPick(currentPlayer);

            // Start the race for spoons
            raceForSpoons(currentPlayer);
        } else {
            showAlert("Cannot Grab Spoon", "You cannot grab a spoon until you have four cards of the same rank or another player starts the spoon race.");
        }
    }

    private void npcTurn() {
        if (currentPlayer != null && !currentPlayer.getName().equals("Player 1")) {
            Platform.runLater(() -> {
                System.out.println("NPC Turn: " + currentPlayer.getName());
                System.out.println("Current Hand Before: " + currentPlayer.getHand());

                // Draw a card if needed
                if (!game.isInitialDrawComplete() || currentPlayer.getHand().size() < 4) {
                    if (currentPlayer.getHand().size() < 4) {  // Only draw if less than 4 cards
                        Card drawnCard = game.getDeck().drawCard();
                        if (drawnCard != null) {
                            currentPlayer.addCard(drawnCard);
                            System.out.println(currentPlayer.getName() + " drew card: " + drawnCard);
                            game.checkForMatchAndSpoon(currentPlayer);
                        }
                    }
                }

                // Replace a card if there are more than 4 cards
                if (currentPlayer.getHand().size() >= 4) {
                    Card cardToReplace = selectBestCardToReplace(currentPlayer);
                    System.out.println(currentPlayer.getName() + " replacing card: " + cardToReplace);
                    currentPlayer.getHand().remove(cardToReplace);
                    Card drawnCard = game.getDeck().drawCard();
                    if (drawnCard != null) {
                        currentPlayer.addCard(drawnCard);
                        System.out.println(currentPlayer.getName() + " drew card: " + drawnCard);
                        game.checkForMatchAndSpoon(currentPlayer);
                    }
                    System.out.println("New Hand After Replacement: " + currentPlayer.getHand());
                    Platform.runLater(this::updateUI);
                }

                game.nextTurn();
                currentPlayer = game.getCurrentPlayer();
                turnLabel.setText("Next turn: " + currentPlayer.getName());

                // Continue with the next NPC's turn
                if (!currentPlayer.getName().equals("Player 1")) {
                    npcTurn();
                }
            });
        }
    }

    private Card selectBestCardToReplace(Player player) {
        // Group cards by rank so NPCs chase four-of-a-kind.
        Map<String, List<Card>> cardsByValue = new HashMap<>();
        for (Card card : player.getHand()) {
            cardsByValue.putIfAbsent(card.getValue(), new ArrayList<>());
            cardsByValue.get(card.getValue()).add(card);
        }

        String targetValue = cardsByValue.entrySet().stream()
                .max(Comparator.comparingInt(entry -> entry.getValue().size()))
                .map(Map.Entry::getKey)
                .orElse(null);

        System.out.println(player.getName() + " targeting rank: " + targetValue);

        return player.getHand().stream()
                .filter(card -> !card.getValue().equals(targetValue))
                .findFirst()
                .orElse(player.getHand().get(0));
    }

    private void showReplacementMenu() {
        Stage dialog = new Stage();
        VBox dialogVbox = new VBox(20);
        dialogVbox.setAlignment(Pos.CENTER);

        Label instructions = new Label("Select the card you want to replace:");
        dialogVbox.getChildren().add(instructions);

        HBox cardsBox = new HBox(10);
        cardsBox.setAlignment(Pos.CENTER);
        Player player1 = game.getPlayers().get(0);
        for (Card card : player1.getHand()) {
            ImageView cardImageView = createCardImageView(card);
            cardsBox.getChildren().add(cardImageView);
        }
        dialogVbox.getChildren().add(cardsBox);

        Button confirmButton = new Button("Confirm Replace");
        confirmButton.setOnAction(e -> {
            confirmReplaceCard();
            dialog.close();
        });
        dialogVbox.getChildren().add(confirmButton);

        Scene dialogScene = new Scene(dialogVbox, 400, 300);
        dialog.setScene(dialogScene);
        dialog.show();
    }

    private Button createActionButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        return button;
    }

    // Method to load images and cache them
    private Image loadImage(String path) {
        if (!cardImagesCache.containsKey(path)) {
            cardImagesCache.put(path, new Image(path));
        }
        return cardImagesCache.get(path);
    }

    // Method to show alerts
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
