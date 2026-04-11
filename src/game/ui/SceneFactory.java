package game.ui;

import javafx.scene.Parent;
import javafx.scene.Scene;

public final class SceneFactory {

    private static final double WIDTH = 960;
    private static final double HEIGHT = 720;
    private static final String STYLESHEET = "file:src/game/styles.css";

    private SceneFactory() {
    }

    public static Scene createScene(Parent content) {
        Scene scene = new Scene(content, WIDTH, HEIGHT);
        scene.getStylesheets().add(STYLESHEET);
        return scene;
    }
}
