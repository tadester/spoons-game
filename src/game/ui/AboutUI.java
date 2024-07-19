package game.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class AboutUI {

    public VBox createContent() {
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);

        Label aboutLabel = new Label("Spoons Game\nDeveloped by [Your Name]\nVersion 1.0");

        root.getChildren().add(aboutLabel);
        return root;
    }
}
