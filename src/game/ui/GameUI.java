package game.ui;

import game.Game;
import game.cards.Card;
import game.players.Player;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
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
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.util.Duration;

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
    private Label pendingCardLabel;
    private StackPane pendingCardSlot;
    private ImageView pendingCardView;
    private boolean cheatMode = false;
    private final Map<String, Image> cardImagesCache = new HashMap<>();
    private final Map<String, List<Card>> renderedHands = new HashMap<>();
    private final int npcReactionTimeRange;
    private int availableSpoons;  // Track the number of available spoons
    private Card pendingDrawCard;
    private Card renderedPendingCard;
    private Card renderedSelectedCard;
    private boolean renderedCheatMode;

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

        pendingCardLabel = new Label("No pending draw");
        pendingCardLabel.getStyleClass().add("mini-label");

        pendingCardSlot = new StackPane();
        pendingCardSlot.getStyleClass().add("pending-card-slot");
        pendingCardSlot.setMinSize(CARD_WIDTH + 28, CARD_HEIGHT + 28);
        pendingCardSlot.setPrefSize(CARD_WIDTH + 28, CARD_HEIGHT + 28);

        headerBox.getChildren().addAll(titleLabel, turnLabel, hintLabel, pendingCardLabel, pendingCardSlot);

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
            spoonImage.setOnMouseClicked(e -> pickSpoon());
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

        Button discardButton = createActionButton("Discard Selected", "secondary-button");
        discardButton.setOnAction(e -> confirmReplaceCard());

        Button cheatButton = createActionButton("Cheat View", "ghost-button");
        cheatButton.setOnAction(e -> toggleCheatMode());

        Button pauseButton = createActionButton("Pause", "ghost-button");
        pauseButton.setOnAction(e -> showPauseMenu());

        buttonsBox.getChildren().addAll(drawCardButton, pickSpoonButton, discardButton, cheatButton, pauseButton);
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
            if (pendingDrawCard != null) {
                showAlert("Finish Your Turn", "Discard one of your current cards before drawing again.");
                return;
            }

            Card drawnCard = game.getDeck().drawCard();
            if (drawnCard == null) {
                endGame();
                return;
            }

            if (!game.isInitialDrawComplete() || currentPlayer.getHand().size() < 4) {
                currentPlayer.addCard(drawnCard);
                animateCardToHand(drawnCard);
                game.checkForMatchAndSpoon(currentPlayer);

                game.nextTurn();
                currentPlayer = game.getCurrentPlayer();
                turnLabel.setText("Turn: " + currentPlayer.getName());
                if (!currentPlayer.getName().equals("Player 1")) {
                    npcTurn();
                }
            } else {
                pendingDrawCard = drawnCard;
                selectedCard = null;
                updatePendingCardPreview();
                animatePendingCardArrival();
                hintLabel.setText("You drew a card. Click one of your four cards, then press Discard Selected.");
            }
        } else {
            showAlert("Not Your Turn", "Please wait for your turn to draw a card.");
        }
    }

    private VBox createPlayerBox(Player player) {
        VBox playerBox = new VBox(player.getName().equals("Player 1") ? 10 : 16);
        playerBox.setAlignment(Pos.CENTER);
        playerBox.setPadding(new Insets(8));
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
        System.out.println("Confirming discard. Selected card: " + selectedCard);
        if (pendingDrawCard != null && selectedCard != null && currentPlayer.getName().equals("Player 1")) {
            System.out.println("Discarding card: " + selectedCard);
            currentPlayer.getHand().remove(selectedCard);
            currentPlayer.addCard(pendingDrawCard);
            animateCardToHand(pendingDrawCard);
            selectedCard = null;
            pendingDrawCard = null;
            updatePendingCardPreview();
            System.out.println("Card discarded successfully.");
            game.checkForMatchAndSpoon(currentPlayer);
            game.nextTurn();
            currentPlayer = game.getCurrentPlayer();
            turnLabel.setText("Turn: " + currentPlayer.getName());
            if (!currentPlayer.getName().equals("Player 1")) {
                npcTurn();
            }
        } else {
            System.out.println("No card selected, no pending draw, or it's not your turn.");
            showAlert("Discard Card", "Draw a fifth card first, then select one of your hand cards to discard.");
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
            List<Card> currentHand = new ArrayList<>(player.getHand());
            List<Card> renderedHand = renderedHands.get(player.getName());
            boolean needsHandRefresh = !currentHand.equals(renderedHand)
                || renderedCheatMode != cheatMode
                || (player.getName().equals("Player 1") && !Objects.equals(renderedSelectedCard, selectedCard));

            if (needsHandRefresh) {
                handBox.getChildren().clear();
                for (Card card : player.getHand()) {
                    ImageView cardImage;
                    if (cheatMode || player.getName().equals("Player 1")) {
                        cardImage = new ImageView(loadImage("file:src/images/cards/" + card.toString() + ".png"));
                    } else {
                        cardImage = new ImageView(loadImage("file:src/images/card_back.png"));
                    }
                    cardImage.setFitHeight(CARD_HEIGHT);
                    cardImage.setFitWidth(CARD_WIDTH);
                    cardImage.getStyleClass().add("play-card");
                    if (player.getName().equals("Player 1")) {
                        Card handCard = card;
                        cardImage.setOnMouseClicked(e -> {
                            if (pendingDrawCard == null) {
                                hintLabel.setText("Draw a card first. Then choose one card to discard.");
                                return;
                            }
                            selectedCard = handCard;
                            updateUI();
                            hintLabel.setText("Selected " + handCard.getValue() + " of " + handCard.getSuit() + ". Press Discard Selected to finish your turn.");
                        });
                        if (selectedCard != null && selectedCard.equals(handCard)) {
                            cardImage.getStyleClass().add("selected-card");
                        }
                    }
                    handBox.getChildren().add(cardImage);
                }
                renderedHands.put(player.getName(), currentHand);
            }
            playerSpoons.get(i).setVisible(player.hasSpoon());
        }
        renderedCheatMode = cheatMode;
        renderedSelectedCard = selectedCard;
        turnLabel.setText("Turn: " + game.getCurrentPlayer().getName());
        if (game.isRaceStarted()) {
            hintLabel.setText("Spoon race active. Tap the spoon button or the spoon pile now.");
        } else if (pendingDrawCard == null && selectedCard == null) {
            hintLabel.setText("Collect four cards of the same rank, then grab a spoon.");
        }
        updatePendingCardPreview();
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

    private Button createActionButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        return button;
    }

    private void updatePendingCardPreview() {
        if (pendingDrawCard == null) {
            pendingCardSlot.getChildren().clear();
            pendingCardView = null;
            renderedPendingCard = null;
            pendingCardLabel.setText("No pending draw");
            return;
        }

        if (pendingDrawCard.equals(renderedPendingCard) && pendingCardView != null) {
            pendingCardLabel.setText("New draw");
            return;
        }

        pendingCardSlot.getChildren().clear();
        pendingCardLabel.setText("New draw");
        pendingCardView = new ImageView(loadImage("file:src/images/cards/" + pendingDrawCard.toString() + ".png"));
        pendingCardView.setFitWidth(CARD_WIDTH);
        pendingCardView.setFitHeight(CARD_HEIGHT);
        pendingCardView.getStyleClass().add("play-card");
        pendingCardSlot.getChildren().add(pendingCardView);
        renderedPendingCard = pendingDrawCard;
    }

    private void animatePendingCardArrival() {
        if (pendingCardView == null) {
            return;
        }

        pendingCardView.setTranslateY(-90);
        pendingCardView.setScaleX(0.65);
        pendingCardView.setScaleY(0.65);
        pendingCardView.setOpacity(0.0);

        TranslateTransition move = new TranslateTransition(Duration.millis(900), pendingCardView);
        move.setFromY(-90);
        move.setToY(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(900), pendingCardView);
        scale.setFromX(0.65);
        scale.setFromY(0.65);
        scale.setToX(1.0);
        scale.setToY(1.0);

        FadeTransition fade = new FadeTransition(Duration.millis(900), pendingCardView);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        new ParallelTransition(move, scale, fade).play();
    }

    private void animateCardToHand(Card drawnCard) {
        hintLabel.setText("Drew " + drawnCard.getValue() + " of " + drawnCard.getSuit() + ".");
        Platform.runLater(() -> {
            updateUI();
            if (playerHands.isEmpty()) {
                return;
            }
            HBox handBox = playerHands.get(0);
            if (handBox.getChildren().isEmpty()) {
                return;
            }
            javafx.scene.Node latestCard = handBox.getChildren().get(handBox.getChildren().size() - 1);
            latestCard.setTranslateY(70);
            latestCard.setScaleX(0.75);
            latestCard.setScaleY(0.75);
            latestCard.setOpacity(0.0);

            TranslateTransition move = new TranslateTransition(Duration.millis(950), latestCard);
            move.setFromY(70);
            move.setToY(0);

            ScaleTransition scale = new ScaleTransition(Duration.millis(950), latestCard);
            scale.setFromX(0.75);
            scale.setFromY(0.75);
            scale.setToX(1.0);
            scale.setToY(1.0);

            FadeTransition fade = new FadeTransition(Duration.millis(950), latestCard);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);

            new ParallelTransition(move, scale, fade).play();
        });
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
