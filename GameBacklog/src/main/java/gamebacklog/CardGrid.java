package gamebacklog;

import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * A grid of game cards that chooses how many columns fit the available width and stretches the cards
 * so every row is filled. It re-fits (after a short pause) whenever the container is resized.
 *
 * @param containerWidth width of the surrounding ScrollPane (stable, so the layout can't flicker)
 * @param reserved       space the container uses around the grid: page padding, grid padding, scrollbar
 */
public class CardGrid extends GridPane {
    private static final double GAP = 18, MIN_W = 230, MAX_W = 600;

    private final MainController c;
    private final ReadOnlyDoubleProperty containerWidth;
    private final double reserved;
    private final List<Game> games = new ArrayList<>();
    private final PauseTransition pause = new PauseTransition(Duration.millis(120));

    public CardGrid(MainController c, ReadOnlyDoubleProperty containerWidth, double reserved, List<Game> list) {
        this.c = c;
        this.containerWidth = containerWidth;
        this.reserved = reserved;
        games.addAll(list);
        setHgap(GAP);
        setVgap(GAP);
        setPadding(new Insets(6));
        pause.setOnFinished(e -> layoutCards());
        containerWidth.addListener((o, a, b) -> pause.playFromStart());
        layoutCards();
    }

    private void layoutCards() {
        getChildren().clear();
        getColumnConstraints().clear();
        double avail = containerWidth.get() - reserved;
        if (games.isEmpty() || avail < 100) return;
        int cols = Math.min(Math.max(1, (int) ((avail + GAP) / (MIN_W + GAP))), games.size());
        double w = Math.floor(Math.min(MAX_W, (avail - (cols - 1) * GAP) / cols));
        for (int i = 0; i < cols; i++) getColumnConstraints().add(new ColumnConstraints(w));
        for (int i = 0; i < games.size(); i++) add(GameLibraryView.card(c, games.get(i), w), i % cols, i / cols);
    }
}