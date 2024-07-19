package game.ui;

import javafx.geometry.Pos;
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

        root.getChildren().add(aboutLabel);
        return root;
    }
}
