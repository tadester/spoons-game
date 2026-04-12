package game.ui;

import game.SpoonsEngine;
import game.cards.Card;
import game.players.Player;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class GameUI {

    private static final double CARD_WIDTH = 88;
    private static final double CARD_HEIGHT = 124;

    private final Stage primaryStage;
    private final double npcDelaySeconds;
    private final SpoonsEngine engine;
    private final Map<String, Image> imageCache;
    private final Map<Integer, StackPane> drawTargets;
    private final List<PauseTransition> scheduledActions;
    private final Random random;

    private StackPane root;
    private StackPane boardPane;
    private Pane animationLayer;
    private Label titleLabel;
    private Label statusLabel;
    private Label hintLabel;
    private Button drawButton;
    private Button discardButton;
    private Button spoonButton;
    private ImageView deckImage;
    private StackPane pendingCardSlot;

    private Card selectedDiscard;
    private boolean animating;

    private enum SeatPosition {
        BOTTOM,
        LEFT,
        TOP,
        RIGHT
    }

    public GameUI(Stage primaryStage, int npcReactionTimeRange) {
        this.primaryStage = primaryStage;
        this.npcDelaySeconds = Math.max(0.75, npcReactionTimeRange * 0.6);
        this.engine = new SpoonsEngine(Arrays.asList("You", "Ava", "Milo", "Zara"));
        this.imageCache = new HashMap<>();
        this.drawTargets = new HashMap<>();
        this.scheduledActions = new ArrayList<>();
        this.random = new Random();
    }

    public StackPane createContent() {
        root = new StackPane();
        root.getStyleClass().add("game-root");

        VBox screen = new VBox(18);
        screen.setPadding(new Insets(20));
        screen.getStyleClass().add("game-shell");

        VBox headerPanel = new VBox(8);
        headerPanel.getStyleClass().add("hud-panel");
        headerPanel.setAlignment(Pos.CENTER);

        titleLabel = new Label("Spoons");
        titleLabel.getStyleClass().add("hud-title");

        statusLabel = new Label();
        statusLabel.getStyleClass().add("status-pill");

        hintLabel = new Label();
        hintLabel.getStyleClass().add("hud-subtitle");
        hintLabel.setWrapText(true);
        hintLabel.setMaxWidth(620);

        headerPanel.getChildren().addAll(titleLabel, statusLabel, hintLabel);

        boardPane = new StackPane();
        boardPane.getStyleClass().add("table-board");
        boardPane.setPadding(new Insets(24));

        animationLayer = new Pane();
        animationLayer.setMouseTransparent(true);

        StackPane boardStack = new StackPane(boardPane, animationLayer);
        VBox.setVgrow(boardStack, Priority.ALWAYS);

        HBox actionsBar = new HBox(14);
        actionsBar.setAlignment(Pos.CENTER);
        actionsBar.getStyleClass().add("controls-bar");

        drawButton = new Button("Draw");
        drawButton.getStyleClass().add("primary-button");
        drawButton.setOnAction(e -> handleHumanDraw());

        discardButton = new Button("Discard Selected");
        discardButton.getStyleClass().add("secondary-button");
        discardButton.setOnAction(e -> handleHumanDiscard());

        spoonButton = new Button("Grab Spoon");
        spoonButton.getStyleClass().add("accent-button");
        spoonButton.setOnAction(e -> handleHumanSpoonGrab());

        Button menuButton = new Button("Main Menu");
        menuButton.getStyleClass().add("ghost-button");
        menuButton.setOnAction(e -> showPauseMenu());

        actionsBar.getChildren().addAll(drawButton, discardButton, menuButton);

        screen.getChildren().addAll(headerPanel, boardStack, actionsBar);
        root.getChildren().add(screen);

        updateUI();
        scheduleNpcTurnIfNeeded();
        return root;
    }

    private void handleHumanDraw() {
        if (animating) {
            return;
        }

        Card drawnCard = engine.drawForHuman();
        if (drawnCard == null) {
            hintLabel.setText("You can only draw on your turn when there is no pending card.");
            updateActionState();
            return;
        }

        selectedDiscard = null;
        updateStatusText();
        hintLabel.setText("Pick one of the five cards to discard.");
        animateCardFlight(loadCardImage(drawnCard), pendingCardSlot, () -> {
            updateUI();
            highlightPendingDraw();
        });
    }

    private void handleHumanDiscard() {
        if (animating) {
            return;
        }

        SpoonsEngine.TurnOutcome outcome = engine.discardForHuman(selectedDiscard);
        if (outcome == null) {
            hintLabel.setText("Draw a card first, then click a card to discard.");
            updateActionState();
            return;
        }

        Card selectedCard = selectedDiscard;
        selectedDiscard = null;
        updateStatusText();

        if (outcome.isMatched()) {
            hintLabel.setText("Four of a kind. Grab a spoon before someone else does.");
        } else {
            hintLabel.setText("Turn passed. Watch the table.");
        }

        updateUI();
        scheduleNpcTurnIfNeeded();
    }

    private void handleHumanSpoonGrab() {
        if (animating) {
            return;
        }
        resolveSpoonGrab(0);
    }

    private void resolveSpoonGrab(int playerIndex) {
        SpoonsEngine.SpoonGrabResult result = engine.grabSpoon(playerIndex);
        if (!result.isSuccess()) {
            hintLabel.setText("You can only grab when you have four of a kind or the spoon race has started.");
            updateActionState();
            return;
        }

        cancelScheduledActions();
        updateUI();

        if (result.getRoundResult() != null) {
            handleRoundResult(result.getRoundResult());
            return;
        }

        hintLabel.setText(result.getPlayerName() + " grabbed a spoon. Everyone else scramble.");
        scheduleRaceGrabs();
    }

    private void scheduleNpcTurnIfNeeded() {
        cancelScheduledActions();
        if (engine.isGameOver() || engine.isRaceActive() || engine.getCurrentPlayerIndex() == 0 || animating) {
            return;
        }

        PauseTransition pause = new PauseTransition(Duration.seconds(npcDelaySeconds));
        pause.setOnFinished(event -> executeNpcTurn());
        scheduledActions.add(pause);
        pause.play();
    }

    private void executeNpcTurn() {
        if (engine.isGameOver() || engine.isRaceActive() || engine.getCurrentPlayerIndex() == 0) {
            return;
        }

        int actingIndex = engine.getCurrentPlayerIndex();
        StackPane target = drawTargets.get(actingIndex);
        SpoonsEngine.TurnOutcome outcome = engine.takeNpcTurn();
        if (outcome == null) {
            updateUI();
            return;
        }

        Node animationTarget = target == null ? boardPane : target;
        animateCardFlight(loadImage("file:src/images/card_back.png"), animationTarget, () -> {
            updateUI();
            if (outcome.isMatched()) {
                hintLabel.setText(outcome.getPlayerName() + " looks ready to grab a spoon.");
                scheduleNpcSpoonAttempt(outcome.getPlayerIndex());
            } else {
                hintLabel.setText(outcome.getPlayerName() + " discarded and passed the turn.");
                scheduleNpcTurnIfNeeded();
            }
        });
    }

    private void scheduleNpcSpoonAttempt(int playerIndex) {
        cancelScheduledActions();
        if (engine.isGameOver()) {
            return;
        }

        PauseTransition pause = new PauseTransition(Duration.seconds(0.45 + random.nextDouble() * 0.85));
        pause.setOnFinished(event -> {
            SpoonsEngine.SpoonGrabResult result = engine.grabSpoon(playerIndex);
            if (!result.isSuccess()) {
                scheduleNpcTurnIfNeeded();
                return;
            }

            updateUI();
            if (result.getRoundResult() != null) {
                handleRoundResult(result.getRoundResult());
            } else {
                hintLabel.setText(result.getPlayerName() + " started the spoon race.");
                scheduleRaceGrabs();
            }
        });
        scheduledActions.add(pause);
        pause.play();
    }

    private void scheduleRaceGrabs() {
        cancelScheduledActions();
        for (int index = 1; index < engine.getPlayers().size(); index++) {
            Player player = engine.getPlayers().get(index);
            if (player.hasSpoon()) {
                continue;
            }
            PauseTransition pause = new PauseTransition(Duration.seconds(0.35 + random.nextDouble() * 1.25));
            final int targetIndex = index;
            pause.setOnFinished(event -> {
                SpoonsEngine.SpoonGrabResult result = engine.grabSpoon(targetIndex);
                if (!result.isSuccess()) {
                    return;
                }
                updateUI();
                if (result.getRoundResult() != null) {
                    handleRoundResult(result.getRoundResult());
                }
            });
            scheduledActions.add(pause);
            pause.play();
        }
    }

    private void handleRoundResult(SpoonsEngine.RoundResult roundResult) {
        cancelScheduledActions();
        selectedDiscard = null;
        updateUI();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(roundResult.isGameFinished() ? "Game Over" : "Round Result");
        alert.setHeaderText(null);

        StringBuilder message = new StringBuilder();
        message.append(roundResult.getLoserName())
            .append(" takes a letter: ")
            .append(roundResult.getLetters().isEmpty() ? "-" : roundResult.getLetters())
            .append(".");
        if (roundResult.isEliminated()) {
            message.append("\n").append(roundResult.getLoserName()).append(" is out of the game.");
        }
        if (roundResult.isGameFinished()) {
            message.append("\n").append(roundResult.getWinnerName()).append(" wins the table.");
        } else {
            message.append("\nNew round starts now.");
        }

        alert.setContentText(message.toString());
        alert.showAndWait();

        if (roundResult.isGameFinished()) {
            primaryStage.setScene(SceneFactory.createScene(new MenuUI(primaryStage).createContent()));
        } else {
            updateUI();
            scheduleNpcTurnIfNeeded();
        }
    }

    private void showPauseMenu() {
        cancelScheduledActions();

        Alert pauseAlert = new Alert(Alert.AlertType.INFORMATION);
        pauseAlert.setTitle("Pause");
        pauseAlert.setHeaderText(null);
        pauseAlert.setContentText("Return to the main menu or resume the round?");

        ButtonType resumeButton = new ButtonType("Resume");
        ButtonType mainMenuButton = new ButtonType("Main Menu");
        pauseAlert.getButtonTypes().setAll(resumeButton, mainMenuButton);

        Optional<ButtonType> result = pauseAlert.showAndWait();
        if (result.isPresent() && result.get() == mainMenuButton) {
            primaryStage.setScene(SceneFactory.createScene(new MenuUI(primaryStage).createContent()));
            return;
        }

        scheduleNpcTurnIfNeeded();
    }

    private void updateUI() {
        drawTargets.clear();
        boardPane.getChildren().clear();

        Node centerArena = createCenterArena();
        StackPane.setAlignment(centerArena, Pos.CENTER);
        boardPane.getChildren().add(centerArena);

        List<Player> players = engine.getPlayers();
        if (!players.isEmpty()) {
            Node panel = createPlayerPanel(players.get(0), 0, SeatPosition.BOTTOM, true);
            StackPane.setAlignment(panel, Pos.BOTTOM_CENTER);
            StackPane.setMargin(panel, new Insets(0, 0, 24, 0));
            boardPane.getChildren().add(panel);
        }
        if (players.size() > 1) {
            Node panel = createPlayerPanel(players.get(1), 1, SeatPosition.LEFT, false);
            StackPane.setAlignment(panel, Pos.CENTER_LEFT);
            StackPane.setMargin(panel, new Insets(0, 0, 0, 22));
            boardPane.getChildren().add(panel);
        }
        if (players.size() > 2) {
            Node panel = createPlayerPanel(players.get(2), 2, SeatPosition.TOP, false);
            StackPane.setAlignment(panel, Pos.TOP_CENTER);
            StackPane.setMargin(panel, new Insets(20, 0, 0, 0));
            boardPane.getChildren().add(panel);
        }
        if (players.size() > 3) {
            Node panel = createPlayerPanel(players.get(3), 3, SeatPosition.RIGHT, false);
            StackPane.setAlignment(panel, Pos.CENTER_RIGHT);
            StackPane.setMargin(panel, new Insets(0, 22, 0, 0));
            boardPane.getChildren().add(panel);
        }

        updateStatusText();
        updateActionState();
    }

    private Node createCenterArena() {
        VBox centerBox = new VBox(18);
        centerBox.setAlignment(Pos.CENTER);

        StackPane tableCore = new StackPane();
        tableCore.getStyleClass().add("table-core");
        tableCore.setPadding(new Insets(28));

        VBox centerContent = new VBox(14);
        centerContent.setAlignment(Pos.CENTER);

        Label deckLabel = new Label("Draw Pile");
        deckLabel.getStyleClass().add("mini-label");

        deckImage = new ImageView(loadImage("file:src/images/card_back.png"));
        deckImage.setFitWidth(CARD_WIDTH);
        deckImage.setFitHeight(CARD_HEIGHT);
        deckImage.getStyleClass().add("center-deck");
        deckImage.setOnMouseClicked(event -> handleHumanDraw());

        Label spoonLabel = new Label("Spoons Left: " + engine.getSpoonsRemaining());
        spoonLabel.getStyleClass().add("mini-label");

        HBox spoonPile = new HBox(10);
        spoonPile.setAlignment(Pos.CENTER);
        spoonPile.getStyleClass().add("spoons-row");

        for (int spoonIndex = 0; spoonIndex < engine.getSpoonsRemaining(); spoonIndex++) {
            ImageView spoonIcon = new ImageView(loadImage("file:src/images/spoon.png"));
            spoonIcon.setFitWidth(52);
            spoonIcon.setFitHeight(52);
            spoonIcon.getStyleClass().add("spoon-icon");
            spoonIcon.setOnMouseClicked(event -> handleHumanSpoonGrab());
            spoonPile.getChildren().add(spoonIcon);
        }

        centerContent.getChildren().addAll(deckLabel, deckImage, spoonLabel, spoonPile);
        tableCore.getChildren().add(centerContent);

        if (!spoonButton.getStyleClass().contains("spoon-call-button")) {
            spoonButton.getStyleClass().add("spoon-call-button");
        }
        StackPane.setAlignment(spoonButton, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(spoonButton, new Insets(0, 8, 18, 0));

        StackPane arena = new StackPane(tableCore, spoonButton);
        arena.setPickOnBounds(false);
        centerBox.getChildren().add(arena);
        return centerBox;
    }

    private VBox createPlayerPanel(Player player, int playerIndex, SeatPosition seatPosition, boolean human) {
        VBox panel = new VBox(12);
        panel.setAlignment(Pos.CENTER);
        panel.getStyleClass().add(human ? "player-panel-self" : "player-panel");
        panel.getStyleClass().add("seat-" + seatPosition.name().toLowerCase());
        panel.setPadding(new Insets(14));
        panel.setMaxWidth(human ? 820 : 210);
        panel.setFillWidth(false);

        Label nameLabel = new Label(player.getName());
        nameLabel.getStyleClass().add("player-name");

        Label lettersLabel = new Label(player.getLetterProgress().isEmpty() ? "Clean" : player.getLetterProgress());
        lettersLabel.getStyleClass().add("letters-chip");

        HBox cardsRow = new HBox(human ? 10 : 6);
        cardsRow.setAlignment(Pos.CENTER);
        cardsRow.getStyleClass().add("player-hand");
        cardsRow.getStyleClass().add("seat-hand-" + seatPosition.name().toLowerCase());

        boolean cardsFaceUp = human;

        for (Card card : player.getHand()) {
            ImageView cardView = createCardView(card, cardsFaceUp, human);
            if (human && engine.getPendingDraw() != null) {
                cardView.setOnMouseClicked(event -> {
                    selectedDiscard = card;
                    updateActionState();
                    updateUI();
                    hintLabel.setText("Selected " + card.getValue() + " of " + card.getSuit() + " to discard.");
                });
                if (selectedDiscard != null && selectedDiscard.equals(card)) {
                    cardView.getStyleClass().add("selected-card");
                }
            }
            cardsRow.getChildren().add(cardView);
        }

        StackPane targetAnchor = new StackPane();
        targetAnchor.setMinSize(CARD_WIDTH, CARD_HEIGHT);
        targetAnchor.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        targetAnchor.getStyleClass().add("draw-anchor");
        drawTargets.put(playerIndex, targetAnchor);

        if (human) {
            pendingCardSlot = new StackPane();
            pendingCardSlot.setMinSize(CARD_WIDTH + 20, CARD_HEIGHT + 20);
            pendingCardSlot.setPrefSize(CARD_WIDTH + 20, CARD_HEIGHT + 20);
            pendingCardSlot.getStyleClass().add("pending-card-slot");

            Label pendingLabel = new Label("Drawn Card");
            pendingLabel.getStyleClass().add("mini-label");

            if (engine.getPendingDraw() != null) {
                ImageView pendingView = createCardView(engine.getPendingDraw(), true, true);
                pendingView.setOnMouseClicked(event -> {
                    selectedDiscard = engine.getPendingDraw();
                    updateActionState();
                    updateUI();
                    hintLabel.setText("Selected the new draw to discard.");
                });
                if (selectedDiscard != null && selectedDiscard.equals(engine.getPendingDraw())) {
                    pendingView.getStyleClass().add("selected-card");
                }
                pendingCardSlot.getChildren().add(pendingView);
            }

            VBox pendingBox = new VBox(8, pendingLabel, pendingCardSlot);
            pendingBox.setAlignment(Pos.CENTER);
            HBox lowerRow = new HBox(18, cardsRow, pendingBox);
            lowerRow.setAlignment(Pos.CENTER);
            panel.getChildren().addAll(nameLabel, lettersLabel, lowerRow, targetAnchor);
        } else {
            panel.getChildren().addAll(nameLabel, lettersLabel, cardsRow, targetAnchor);
        }

        if (seatPosition == SeatPosition.LEFT || seatPosition == SeatPosition.RIGHT) {
            cardsRow.setRotate(seatPosition == SeatPosition.LEFT ? 90 : -90);
            panel.setPrefWidth(220);
        }

        if (seatPosition == SeatPosition.TOP) {
            cardsRow.setRotate(180);
        }

        return panel;
    }

    private ImageView createCardView(Card card, boolean faceUp, boolean human) {
        ImageView cardView = new ImageView(faceUp ? loadCardImage(card) : loadImage("file:src/images/card_back.png"));
        cardView.setFitWidth(human ? CARD_WIDTH : CARD_WIDTH * 0.74);
        cardView.setFitHeight(human ? CARD_HEIGHT : CARD_HEIGHT * 0.74);
        cardView.getStyleClass().add("play-card");
        return cardView;
    }

    private void updateStatusText() {
        Player currentPlayer = engine.getCurrentPlayer();
        String turnText = engine.isRaceActive()
            ? "Spoon race"
            : "Turn: " + currentPlayer.getName();
        statusLabel.setText(turnText);

        if (engine.isRaceActive()) {
            hintLabel.setText("Tap the spoon pile or the button before the NPCs beat you to it.");
        } else if (engine.getPendingDraw() != null) {
            hintLabel.setText("Choose which card to throw away.");
        } else if (engine.getPlayers().get(0).checkForMatch()) {
            hintLabel.setText("You have four of a kind. Grab a spoon whenever you are ready.");
        } else {
            hintLabel.setText("Draw from the deck, build four of a kind, and stay ahead of the table.");
        }
    }

    private void updateActionState() {
        boolean humanTurn = engine.getCurrentPlayerIndex() == 0;
        boolean hasPending = engine.getPendingDraw() != null;
        boolean canGrab = engine.humanCanGrabSpoon();

        drawButton.setDisable(animating || !engine.humanCanDraw());
        discardButton.setDisable(animating || !humanTurn || !hasPending || selectedDiscard == null);
        spoonButton.setDisable(animating || !canGrab);
    }

    private void highlightPendingDraw() {
        if (pendingCardSlot == null || pendingCardSlot.getChildren().isEmpty()) {
            return;
        }
        Node cardNode = pendingCardSlot.getChildren().get(0);
        ScaleTransition pulse = new ScaleTransition(Duration.millis(220), cardNode);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.08);
        pulse.setToY(1.08);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.play();
    }

    private void animateCardFlight(Image image, Node targetNode, Runnable onFinished) {
        if (deckImage == null || targetNode == null) {
            onFinished.run();
            return;
        }

        Bounds startBounds = deckImage.localToScene(deckImage.getBoundsInLocal());
        Bounds endBounds = targetNode.localToScene(targetNode.getBoundsInLocal());
        if (startBounds == null || endBounds == null) {
            onFinished.run();
            return;
        }

        Point2D startPoint = animationLayer.sceneToLocal(
            startBounds.getMinX() + startBounds.getWidth() / 2,
            startBounds.getMinY() + startBounds.getHeight() / 2
        );
        Point2D endPoint = animationLayer.sceneToLocal(
            endBounds.getMinX() + endBounds.getWidth() / 2,
            endBounds.getMinY() + endBounds.getHeight() / 2
        );

        ImageView flyingCard = new ImageView(image);
        flyingCard.setFitWidth(CARD_WIDTH);
        flyingCard.setFitHeight(CARD_HEIGHT);
        flyingCard.getStyleClass().add("flying-card");
        flyingCard.setManaged(false);
        flyingCard.relocate(startPoint.getX() - CARD_WIDTH / 2, startPoint.getY() - CARD_HEIGHT / 2);
        animationLayer.getChildren().add(flyingCard);

        animating = true;
        updateActionState();

        TranslateTransition move = new TranslateTransition(Duration.millis(980), flyingCard);
        move.setByX(endPoint.getX() - startPoint.getX());
        move.setByY(endPoint.getY() - startPoint.getY());

        ScaleTransition scale = new ScaleTransition(Duration.millis(980), flyingCard);
        scale.setFromX(0.92);
        scale.setFromY(0.92);
        scale.setToX(1.0);
        scale.setToY(1.0);

        FadeTransition fade = new FadeTransition(Duration.millis(980), flyingCard);
        fade.setFromValue(1.0);
        fade.setToValue(0.94);

        ParallelTransition transition = new ParallelTransition(move, scale, fade);
        transition.setOnFinished(event -> {
            animationLayer.getChildren().remove(flyingCard);
            animating = false;
            onFinished.run();
            updateActionState();
        });
        transition.play();
    }

    private void cancelScheduledActions() {
        for (PauseTransition action : scheduledActions) {
            action.stop();
        }
        scheduledActions.clear();
    }

    private Image loadCardImage(Card card) {
        return loadImage("file:src/images/cards/" + card.toString() + ".png");
    }

    private Image loadImage(String path) {
        return imageCache.computeIfAbsent(path, Image::new);
    }
}
