package game.ui;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;

public final class SceneFactory {

    private static final String STYLESHEET = "file:src/game/styles.css";

    private SceneFactory() {
    }

    public static Scene createScene(Parent content) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double width = Math.max(960, bounds.getWidth());
        double height = Math.max(720, bounds.getHeight());
        Scene scene = new Scene(content, width, height);
        scene.getStylesheets().add(STYLESHEET);
        return scene;
    }
}
