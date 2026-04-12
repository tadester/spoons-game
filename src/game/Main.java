package game;

import javafx.application.Application;
import javafx.stage.Stage;
import game.ui.MenuUI;
import game.ui.SceneFactory;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        MenuUI menuUI = new MenuUI(primaryStage);
        primaryStage.setScene(SceneFactory.createScene(menuUI.createContent()));
        primaryStage.setTitle("Spoons Game");
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
