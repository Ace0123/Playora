package gamebacklog;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardView {
    private static final int RECENT_LIMIT = 8;
    private static final double RESERVED = 28 * 2 + 12 + 16;   // page padding + grid padding + scrollbar room
    private final MainController c;

    public DashboardView(MainController c) { this.c = c; }

    public Node build() {
        GameService s = c.service();
        List<Game> all = s.all();
        HBox stats = new HBox(16,
                stat("🎮", "Total Games", String.valueOf(all.size())),
                stat("🕑", "To Play", String.valueOf(s.count("To Play"))),
                stat("▶", "Currently Playing", String.valueOf(s.count("Playing"))),
                stat("🏆", "Completed", String.valueOf(s.count("Completed"))),
                stat("⏱", "Total Playtime", UI.hours(s.totalHours())));
        VBox box = new VBox(26, UI.label("My Game Backlog", "page-title"),
                UI.label("Track what you want to play, what you're playing, and what you've beaten.", "sub"), stats);
        box.setPadding(new Insets(28));
        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);

        if (c.sessions().isActive()) {
            Game ag = s.find(c.sessions().activeGameId());
            if (ag != null) box.getChildren().add(banner(ag));
        }
        if (all.isEmpty()) {
            box.getChildren().add(UI.empty(c));
            return sp;
        }

        List<Game> playing = all.stream().filter(g -> g.getStatus().equals("Playing"))
                .sorted(Comparator.comparing(Game::getLastPlayed).reversed()).collect(Collectors.toList());
        Node playingBody = playing.isEmpty()
                ? UI.label("Nothing in progress. Pick a game and press Play.", "sub")
                : new CardGrid(c, sp.widthProperty(), RESERVED, playing);
        box.getChildren().add(section("▶  Continue playing", playing.size(), playingBody, null));

        // Most recently played first, then the rest A-Z; the grid fills every row to the full width
        List<Game> recent = all.stream().sorted(Comparator.comparing(Game::getLastPlayed).reversed())
                .limit(RECENT_LIMIT).collect(Collectors.toList());
        box.getChildren().add(section("🕒  Recent Games", all.size(),
                new CardGrid(c, sp.widthProperty(), RESERVED, recent), null));

        Node most = mostPlayed(all);
        if (most != null) box.getChildren().add(most);
        return sp;
    }

    /** Heading (title, count, optional button on the right) above some content. */
    private Node section(String title, int count, Node body, Node action) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox head = new HBox(10, UI.label(title, "section-title"), UI.label("(" + count + ")", "sub"), spacer);
        if (action != null) head.getChildren().add(action);
        head.setAlignment(Pos.CENTER_LEFT);
        return new VBox(10, head, body);
    }

    /** Top 5 games by hours, drawn as bars. */
    private Node mostPlayed(List<Game> all) {
        List<Game> top = all.stream().filter(g -> g.getHours() > 0)
                .sorted(Comparator.comparingDouble(Game::getHours).reversed()).limit(5).collect(Collectors.toList());
        if (top.isEmpty()) return null;
        double max = top.get(0).getHours();
        VBox v = new VBox(12, UI.label("⏱  Most played", "section-title"));
        for (Game g : top) {
            Label name = UI.label(g.getTitle(), "text");
            name.setMinWidth(240);
            name.setPrefWidth(240);
            ProgressBar bar = new ProgressBar(g.getHours() / max);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.setPrefHeight(12);
            HBox.setHgrow(bar, Priority.ALWAYS);
            Label hrs = UI.label(UI.hours(g.getHours()), "sub");
            hrs.setMinWidth(70);
            hrs.setAlignment(Pos.CENTER_RIGHT);
            HBox row = new HBox(14, name, bar, hrs);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-cursor: hand;");
            row.setOnMouseClicked(e -> c.showDetails(g));
            v.getChildren().add(row);
        }
        v.getStyleClass().add("stat-card");
        return v;
    }

    private Node banner(Game g) {
        Label live = UI.label("", "live-time");
        live.textProperty().bind(c.sessions().liveTextProperty());
        live.setMinWidth(Region.USE_PREF_SIZE);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox b = new HBox(14, UI.label("● Currently Playing:", "section-title"), UI.label(g.getTitle(), "banner-title"), sp,
                live, UI.button("⏹  End Session", "btn-danger", c::endSession));
        b.getStyleClass().add("stat-card");
        b.setAlignment(Pos.CENTER_LEFT);
        return b;
    }

    private Node stat(String icon, String name, String value) {
        VBox v = new VBox(6, UI.label(icon + "  " + name, "sub"), UI.label(value, "stat-value"));
        v.getStyleClass().add("stat-card");
        v.setMinWidth(150);
        v.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(v, Priority.ALWAYS);
        return v;
    }
}