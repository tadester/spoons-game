import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class ImageViewTest extends Application {

    private static final int CARD_WIDTH = 70;
    private static final int CARD_HEIGHT = 100;
    private Map<String, Image> cardImagesCache = new HashMap<>();
    private String selectedCard = null; // This will store the name of the selected card

    @Override
    public void start(Stage primaryStage) {
        // Set up a test environment similar to GameUI
        HBox handBox = new HBox(10);

        // Display a few cards as an example
        for (int i = 1; i <= 3; i++) {
            String cardName = "card_" + i; // Assuming card images are named like card_1.png, card_2.png, etc.
            ImageView cardImageView = createCardImageView(cardName);
            handBox.getChildren().add(cardImageView);
        }

        StackPane root = new StackPane(handBox);
        Scene scene = new Scene(root, 300, 200);
        primaryStage.setTitle("ImageView Click Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private ImageView createCardImageView(String cardName) {
        // Load the image for the card with absolute path for testing
        String imagePath = "file:C:/Users/tader/OneDrive/Documents/Desktop/Projects/spoons-game/src/images/" + cardName + ".png";
        ImageView cardImageView = new ImageView(loadImage(imagePath));
        cardImageView.setFitHeight(CARD_HEIGHT);
        cardImageView.setFitWidth(CARD_WIDTH);

        // Adding a border to see if the image is visually placed correctly
        cardImageView.setStyle("-fx-border-color: red; -fx-border-width: 2px;");

        // Setting the event handler for the image
        cardImageView.setOnMouseClicked(e -> {
            System.out.println("Card image clicked: " + cardName); // Check if this is printed
            cardImageView.setStyle("-fx-border-color: yellow; -fx-border-width: 2px;"); // Highlight to confirm click
            selectedCard = cardName;
            System.out.println("Selected card to replace: " + selectedCard);
        });

        return cardImageView;
    }

    private Image loadImage(String path) {
        if (!cardImagesCache.containsKey(path)) {
            cardImagesCache.put(path, new Image(path));
        }
        return cardImagesCache.get(path);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
