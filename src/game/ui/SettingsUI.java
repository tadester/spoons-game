package game.ui;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;

public class SettingsUI {

    private Stage primaryStage;
    private int gameSpeed;

    public SettingsUI(Stage primaryStage) {
        this.primaryStage = primaryStage;
        loadSettings();
    }

    public VBox createContent() {
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);

        ComboBox<String> speedComboBox = new ComboBox<>();
        speedComboBox.getItems().addAll("1 second", "2 seconds", "3 seconds", "4 seconds", "5 seconds", "6 seconds", "7 seconds", "8 seconds", "9 seconds", "10 seconds");
        speedComboBox.setValue(getSpeedString());

        Button saveButton = new Button("Save");
        saveButton.setOnAction(e -> {
            String selectedSpeed = speedComboBox.getValue();
            saveSettings(selectedSpeed);
            goToMainMenu();
        });

        root.getChildren().addAll(speedComboBox, saveButton);
        return root;
    }

    private void saveSettings(String selectedSpeed) {
        switch (selectedSpeed) {
            case "1 second": gameSpeed = 1; break;
            case "2 seconds": gameSpeed = 2; break;
            case "3 seconds": gameSpeed = 3; break;
            case "4 seconds": gameSpeed = 4; break;
            case "5 seconds": gameSpeed = 5; break;
            case "6 seconds": gameSpeed = 6; break;
            case "7 seconds": gameSpeed = 7; break;
            case "8 seconds": gameSpeed = 8; break;
            case "9 seconds": gameSpeed = 9; break;
            case "10 seconds": gameSpeed = 10; break;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("settings.txt"))) {
            writer.write("gameSpeed=" + gameSpeed);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSettings() {
        try (BufferedReader reader = new BufferedReader(new FileReader("settings.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("gameSpeed=")) {
                    gameSpeed = Integer.parseInt(line.split("=")[1]);
                }
            }
        } catch (IOException e) {
            gameSpeed = 5; // default value
        }
    }

    private String getSpeedString() {
        switch (gameSpeed) {
            case 1: return "1 second";
            case 2: return "2 seconds";
            case 3: return "3 seconds";
            case 4: return "4 seconds";
            case 5: return "5 seconds";
            case 6: return "6 seconds";
            case 7: return "7 seconds";
            case 8: return "8 seconds";
            case 9: return "9 seconds";
            case 10: return "10 seconds";
            default: return "5 seconds";
        }
    }

    private void goToMainMenu() {
        primaryStage.setScene(new Scene(new MenuUI(primaryStage).createContent(), 800, 600));
    }

    public int getGameSpeed() {
        return gameSpeed;
    }
}
