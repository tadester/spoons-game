package game.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AboutUI {

    private final Stage primaryStage;

    public AboutUI(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public VBox createContent() {
        VBox root = new VBox(24);
        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("screen");

        Label titleLabel = new Label("How to Play Spoons");
        titleLabel.getStyleClass().add("screen-title");

        Label introLabel = new Label(
            "Spoons is a fast-paced card game for 3 to 13 players. Your goal is to collect four cards of the same rank "
                + "and grab a spoon before someone else leaves you empty-handed."
        );
        introLabel.getStyleClass().add("lead-text");
        introLabel.setWrapText(true);
        introLabel.setMaxWidth(640);

        VBox rulesCard = new VBox(18);
        rulesCard.getStyleClass().add("content-card");
        rulesCard.getChildren().addAll(
            createSection(
                "Setup",
                "Place one fewer spoon than the number of players in the center of the table. Deal four cards to each player, "
                    + "and keep the remaining deck ready to draw from."
            ),
            createSection(
                "How a Round Works",
                "Players quickly draw one card, choose one card to discard, and pass that discard to the player on their left. "
                    + "This continues around the table as everyone tries to improve their hand."
            ),
            createSection(
                "When Spoons Matter",
                "As soon as a player makes four-of-a-kind, they quietly grab a spoon. Once one spoon is taken, everyone else can "
                    + "race for the remaining spoons, even if they do not have four-of-a-kind yet."
            ),
            createSection(
                "Losing the Round",
                "The player left without a spoon loses the round and receives a letter in the word SPOONS."
            ),
            createSection(
                "Winning the Game",
                "Keep playing rounds until a player has collected all six letters: S, P, O, O, N, and S. "
                    + "That player is eliminated, and the last player still in the game wins."
            )
        );

        ScrollPane scrollPane = new ScrollPane(rulesCard);
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxWidth(700);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("rules-scroll");

        Button backButton = new Button("Back");
        backButton.getStyleClass().add("primary-button");
        backButton.setOnAction(e -> goToMainMenu());

        root.getChildren().addAll(titleLabel, introLabel, scrollPane, backButton);
        return root;
    }

    private VBox createSection(String heading, String description) {
        VBox section = new VBox(8);

        Label headingLabel = new Label(heading);
        headingLabel.getStyleClass().add("section-title");

        Label bodyLabel = new Label(description);
        bodyLabel.getStyleClass().add("body-text");
        bodyLabel.setWrapText(true);

        Region accent = new Region();
        accent.getStyleClass().add("section-accent");
        HBox titleRow = new HBox(12, accent, headingLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(accent, new Insets(0, 0, 0, 2));

        section.getChildren().addAll(titleRow, bodyLabel);
        return section;
    }

    private void goToMainMenu() {
        primaryStage.setScene(SceneFactory.createScene(new MenuUI(primaryStage).createContent()));
    }
}
