package game.ui;

import game.Game;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SettingsUI {

    private Game game;

    public VBox createContent() {
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);

        ComboBox<String> speedComboBox = new ComboBox<>();
        speedComboBox.getItems().addAll("1 second", "2 seconds", "3 seconds", "4 seconds", "5 seconds", "6 seconds", "7 seconds", "8 seconds", "9 seconds", "10 seconds");
        speedComboBox.setValue("5 seconds");

        Button saveButton = new Button("Save");
        saveButton.setOnAction(e -> {
            String selectedSpeed = speedComboBox.getValue();
            int speed = 5000; // Default to 5 seconds
            switch (selectedSpeed) {
                case "1 second": speed = 1000; break;
                case "2 seconds": speed = 2000; break;
                case "3 seconds": speed = 3000; break;
                case "4 seconds": speed = 4000; break;
                case "5 seconds": speed = 5000; break;
                case "6 seconds": speed = 6000; break;
                case "7 seconds": speed = 7000; break;
                case "8 seconds": speed = 8000; break;
                case "9 seconds": speed = 9000; break;
                case "10 seconds": speed = 10000; break;
            }
            if (game != null) {
                game.setGameSpeed(speed);
            }
            Stage stage = (Stage) root.getScene().getWindow();
            stage.close();
        });

        root.getChildren().addAll(speedComboBox, saveButton);
        return root;
    }

    public void setGame(Game game) {
        this.game = game;
    }
}
