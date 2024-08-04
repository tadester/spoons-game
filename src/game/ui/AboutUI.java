package game.ui;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AboutUI {

    private Stage primaryStage;

    public AboutUI(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public VBox createContent() {
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);

        Label aboutLabel = new Label("This is the About page.");
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> goToMainMenu());

        root.getChildren().addAll(aboutLabel, backButton);
        return root;
    }

    private void goToMainMenu() {
        primaryStage.setScene(new Scene(new MenuUI(primaryStage).createContent(), 800, 600));
    }
}
