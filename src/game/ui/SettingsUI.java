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
        speedComboBox.getItems().addAll("1 card/second", "2 cards/second", "5 cards/second");
        speedComboBox.setValue("1 card/second");

        Button saveButton = new Button("Save");
        saveButton.setOnAction(e -> {
            String selectedSpeed = speedComboBox.getValue();
            int speed = 1000; // Default to 1 card per second
            if (selectedSpeed.equals("2 cards/second")) {
                speed = 500;
            } else if (selectedSpeed.equals("5 cards/second")) {
                speed = 200;
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
