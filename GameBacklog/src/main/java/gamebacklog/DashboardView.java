package gamebacklog;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.util.List;

public class DashboardView {
    private final MainController c;

    public DashboardView(MainController c) { this.c = c; }

    public Node build() {
        GameService s = c.service();
        List<Game> all = s.all();
        HBox stats = new HBox(16,
                stat("", "Total Games", String.valueOf(all.size())),
                stat("", "To Play", String.valueOf(s.count("To Play"))),
                stat("", "Currently Playing", String.valueOf(s.count("Playing"))),
                stat("", "Completed", String.valueOf(s.count("Completed"))),
                stat("", "Total Playtime", UI.hours(s.totalHours())));
        VBox box = new VBox(24, UI.label("My Game Backlog", "page-title"),
                UI.label("Track what you want to play, what you're playing, and what you've beaten.", "sub"), stats);
        if (c.sessions().isActive()) {
            Game ag = s.find(c.sessions().activeGameId());
            if (ag != null) box.getChildren().add(banner(ag));
        }
        if (all.isEmpty()) {
            box.getChildren().add(UI.empty(c));
        } else {
            for (Node n : new Node[]{
                    section(s, "Continue playing", "Playing", 6, "Nothing in progress. Pick a game from Up next and press Play."),
                    section(s, "  Recent Games", "To Play", 6, null),
                    section(s, "  Recently completed", "Completed", 6, null)})
                if (n != null) box.getChildren().add(n);
        }
        box.setPadding(new Insets(28));
        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        return sp;
    }

    /**
     * A titled row of game cards for one status. Returns null when there are no games and no hint,
     * so the dashboard only ever shows content that exists.
     */
    private Node section(GameService s, String title, String status, int limit, String emptyHint) {
        List<Game> games = s.filter("", null, null, status);
        if (games.isEmpty() && emptyHint == null) return null;

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, UI.label(title, "section-title"), UI.label("(" + games.size() + ")", "sub"), spacer);
        header.setAlignment(Pos.CENTER_LEFT);
        if (games.size() > limit) {
            Hyperlink all = new Hyperlink("View all");
            all.getStyleClass().add("hyperlink");
            all.setOnAction(e -> c.showLibrary());
            header.getChildren().add(all);
        }

        VBox sec = new VBox(10, header);
        if (games.isEmpty()) {
            sec.getChildren().add(UI.label(emptyHint, "sub"));
        } else {
            FlowPane row = new FlowPane(18, 18);
            row.setPadding(new Insets(6));
            games.stream().limit(limit).forEach(g -> row.getChildren().add(GameLibraryView.card(c, g)));
            sec.getChildren().add(row);
        }
        return sec;
    }

    private Node stat(String icon, String name, String value) {
        VBox v = new VBox(6, UI.label(icon + "  " + name, "sub"), UI.label(value, "stat-value"));
        v.getStyleClass().add("stat-card");
        v.setMinWidth(150);
        v.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(v, Priority.ALWAYS);
        return v;
    }

    private Node banner(Game g) {
        Label live = UI.label("", "live-time");
        live.textProperty().bind(c.sessions().liveTextProperty());
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label name = UI.label(g.getTitle(), "card-title");
        name.setStyle("-fx-padding: 0;");   // card-title has a top padding meant for game cards; it pushed this text down
        HBox b = new HBox(14, UI.label("● Currently Playing:", "section-title"),name, sp,
                live, UI.button("⏹  End Session", "btn-danger", c::endSession));
        b.getStyleClass().add("stat-card");
        b.setAlignment(Pos.CENTER_LEFT);
        return b;
    }
}