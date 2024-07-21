package game;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import game.ui.MenuUI;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        MenuUI menuUI = new MenuUI(primaryStage);
        Scene scene = new Scene(menuUI.createContent(), 800, 600);
        scene.getStylesheets().add("file:src/styles.css");
        primaryStage.setScene(scene);
        primaryStage.setTitle("Spoons Game");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
