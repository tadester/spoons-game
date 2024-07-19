package game.ui;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MenuUI {

    private Stage primaryStage;

    public MenuUI(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public VBox createContent() {
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);

        Button playButton = new Button("Play");
        playButton.setOnAction(e -> startGame());

        Button aboutButton = new Button("About");
        aboutButton.setOnAction(e -> showAbout());

        Button settingsButton = new Button("Settings");
        settingsButton.setOnAction(e -> showSettings());

        root.getChildren().addAll(playButton, aboutButton, settingsButton);
        return root;
    }

    private void startGame() {
        GameUI gameUI = new GameUI(primaryStage);
        primaryStage.setScene(new Scene(gameUI.createContent(), 800, 600));
    }

    private void showAbout() {
        AboutUI aboutUI = new AboutUI(primaryStage);
        primaryStage.setScene(new Scene(aboutUI.createContent(), 800, 600));
    }

    private void showSettings() {
        SettingsUI settingsUI = new SettingsUI(primaryStage);
        primaryStage.setScene(new Scene(settingsUI.createContent(), 800, 600));
    }
}
