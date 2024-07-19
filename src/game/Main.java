package game;

import game.ui.GameUI;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        GameUI gameUI = new GameUI();
        Scene scene = new Scene(gameUI.createContent());

        primaryStage.setScene(scene);
        primaryStage.setTitle("Spoons Game");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
