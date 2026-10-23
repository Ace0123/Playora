package gamebacklog;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class GameLibraryView {
    private static final double GAP = 18, MIN_W = 230, MAX_W = 600;   // card width limits for the responsive grid

    private final MainController c;
    // Filter state is kept so filters survive navigating away and back.
    private String search = "", genre = "All genres", platform = "All platforms", tab = "All";

    public GameLibraryView(MainController c) { this.c = c; }

    /** Used by the dashboard's "View all" buttons ("All", "To Play", "Playing" or "Completed"). */
    public void setTab(String t) { tab = t; }

    public Node build() {
        GameService s = c.service();

        TextField sf = new TextField(search);
        sf.setPromptText("🔍  Search by title…");
        sf.setPrefWidth(260);
        ComboBox<String> gf = new ComboBox<>();
        gf.getItems().add("All genres");
        gf.getItems().addAll(s.genres());
        if (!gf.getItems().contains(genre)) genre = "All genres";
        gf.setValue(genre);
        ComboBox<String> pf = new ComboBox<>();
        pf.getItems().add("All platforms");
        pf.getItems().addAll(GameService.PLATFORMS);
        pf.setValue(platform);

        GridPane grid = new GridPane();
        grid.setHgap(GAP);
        grid.setVgap(GAP);
        grid.setPadding(new Insets(6));
        ScrollPane sp = new ScrollPane(grid);
        sp.setFitToWidth(true);
        BorderPane body = new BorderPane();
        VBox.setVgrow(body, Priority.ALWAYS);
        List<Game> shown = new ArrayList<>();

        // Responsive grid: pick the number of columns that fits, then stretch the cards to fill the row.
        Runnable layoutGrid = () -> {
            grid.getChildren().clear();
            grid.getColumnConstraints().clear();
            double avail = sp.getWidth() - 30;           // grid padding + room for the scrollbar (keeps the layout stable)
            if (shown.isEmpty() || avail < 100) return;
            int cols = Math.max(1, (int) ((avail + GAP) / (MIN_W + GAP)));
            cols = Math.min(cols, shown.size());
            double w = Math.floor(Math.min(MAX_W, (avail - (cols - 1) * GAP) / cols));
            for (int i = 0; i < cols; i++) grid.getColumnConstraints().add(new ColumnConstraints(w));
            for (int i = 0; i < shown.size(); i++) grid.add(card(c, shown.get(i), w), i % cols, i / cols);
        };
        PauseTransition resizePause = new PauseTransition(Duration.millis(120));   // wait until resizing stops
        resizePause.setOnFinished(e -> layoutGrid.run());
        sp.widthProperty().addListener((o, a, b) -> resizePause.playFromStart());

        Runnable refresh = () -> {
            shown.clear();
            shown.addAll(s.filter(search, genre.equals("All genres") ? null : genre,
                    platform.equals("All platforms") ? null : platform, tab.equals("All") ? null : tab));
            if (shown.isEmpty()) {
                grid.getChildren().clear();
                body.setCenter(s.all().isEmpty() ? UI.empty(c) : UI.label("No games match your filters.", "sub"));
                return;
            }
            body.setCenter(sp);
            layoutGrid.run();
        };

        sf.textProperty().addListener((o, a, n) -> { search = n; refresh.run(); });
        gf.setOnAction(e -> { genre = gf.getValue(); refresh.run(); });
        pf.setOnAction(e -> { platform = pf.getValue(); refresh.run(); });

        ToggleGroup tg = new ToggleGroup();
        HBox tabs = new HBox(8);
        for (String t : List.of("All", "To Play", "Playing", "Completed")) {
            ToggleButton b = new ToggleButton(t);
            b.getStyleClass().add("tab-btn");
            b.setToggleGroup(tg);
            b.setSelected(t.equals(tab));
            b.setOnAction(e -> { tab = t; b.setSelected(true); refresh.run(); });
            tabs.getChildren().add(b);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(UI.label("My Games", "page-title"), spacer,
                UI.button("➕  Add Game", "btn-primary", () -> c.showForm(null)));
        sf.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(sf, Priority.ALWAYS);
        sf.setPrefHeight(40);
        gf.setPrefHeight(40);
        pf.setPrefHeight(40);
        gf.setPrefWidth(180);
        pf.setPrefWidth(190);
        HBox filters = new HBox(12, sf, gf, pf);
        filters.setAlignment(Pos.CENTER_LEFT);
        refresh.run();

        VBox page = new VBox(16, header, filters, tabs, body);
        page.setPadding(new Insets(28));
        return page;
    }

    /** Default-size card (used by the dashboard). */
    static Node card(MainController c, Game g) { return card(c, g, 222); }

    /** A clickable game card of the given width; the cover scales with it. */
    static Node card(MainController c, Game g, double w) {
        double cw = w - 32;                                   // card padding is 16 on each side
        Label name = UI.label(g.getTitle(), "card-title");
        name.setMaxWidth(cw);
        Label meta = UI.label(g.getGenre() + "  •  " + g.getPlatform(), "sub");
        meta.setMaxWidth(cw);
        Button play = UI.playButton(c, g);
        play.setMaxWidth(Double.MAX_VALUE);
        VBox v = new VBox(6, UI.cover(g, cw, Math.round(cw * 0.62)), name, meta, UI.badge(g.getStatus()),                UI.label(UI.stars(g.getRating()), "stars"), UI.label("⏱  " + UI.hours(g.getHours()), "sub"), play);
        v.getStyleClass().add("game-card");
        v.setMinWidth(w);
        v.setPrefWidth(w);
        v.setMaxWidth(w);
        v.setOnMouseClicked(e -> c.showDetails(g));
        return v;
    }
}