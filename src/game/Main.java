package game;

import game.ui.MenuUI;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        MenuUI menuUI = new MenuUI(primaryStage);
        primaryStage.setScene(new Scene(menuUI.createContent(), 800, 600));
        primaryStage.setTitle("Spoons Game");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
