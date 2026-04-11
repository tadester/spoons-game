package game.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MenuUI {

    private final Stage primaryStage;
    private final int gameSpeed;

    public MenuUI(Stage primaryStage) {
        this.primaryStage = primaryStage;
        SettingsUI settingsUI = new SettingsUI(primaryStage);
        gameSpeed = settingsUI.getGameSpeed();  // Get the game speed from SettingsUI
    }

    public VBox createContent() {
        VBox root = new VBox(18);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("screen");

        Label titleLabel = new Label("Spoons");
        titleLabel.getStyleClass().add("screen-title");

        Label subtitleLabel = new Label("Grab four-of-a-kind, then grab a spoon before the table beats you to it.");
        subtitleLabel.getStyleClass().add("lead-text");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(520);

        Button startButton = new Button("Start Game");
        startButton.getStyleClass().add("primary-button");
        startButton.setOnAction(e -> startGame());

        Button aboutButton = new Button("How to Play");
        aboutButton.getStyleClass().add("secondary-button");
        aboutButton.setOnAction(e -> showAbout());

        Button settingsButton = new Button("Settings");
        settingsButton.getStyleClass().add("secondary-button");
        settingsButton.setOnAction(e -> showSettings());

        root.getChildren().addAll(titleLabel, subtitleLabel, startButton, aboutButton, settingsButton);
        return root;
    }

    private void startGame() {
        GameUI gameUI = new GameUI(primaryStage, gameSpeed); // Pass gameSpeed to GameUI
        primaryStage.setScene(SceneFactory.createScene(gameUI.createContent()));
    }

    private void showAbout() {
        AboutUI aboutUI = new AboutUI(primaryStage);
        primaryStage.setScene(SceneFactory.createScene(aboutUI.createContent()));
    }

    private void showSettings() {
        SettingsUI settingsUI = new SettingsUI(primaryStage);
        primaryStage.setScene(SceneFactory.createScene(settingsUI.createContent()));
    }
}
