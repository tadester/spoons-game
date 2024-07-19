package game.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MenuUI {

    public VBox createContent() {
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);

        Button playButton = new Button("Play");
        playButton.setOnAction(e -> showGame());

        Button aboutButton = new Button("About");
        aboutButton.setOnAction(e -> showAbout());

        Button settingsButton = new Button("Settings");
        settingsButton.setOnAction(e -> showSettings());

        root.getChildren().addAll(playButton, aboutButton, settingsButton);
        return root;
    }

    private void showGame() {
        Stage stage = new Stage();
        GameUI gameUI = new GameUI();
        Scene scene = new Scene(gameUI.createContent(), 800, 600);
        stage.setScene(scene);
        stage.setTitle("Spoons Game");
        stage.show();
    }

    private void showAbout() {
        Stage stage = new Stage();
        AboutUI aboutUI = new AboutUI();
        Scene scene = new Scene(aboutUI.createContent(), 400, 300);
        stage.setScene(scene);
        stage.setTitle("About");
        stage.show();
    }

    private void showSettings() {
        Stage stage = new Stage();
        SettingsUI settingsUI = new SettingsUI();
        Scene scene = new Scene(settingsUI.createContent(), 400, 300);
        stage.setScene(scene);
        stage.setTitle("Settings");
        stage.show();
    }
}
