package gamebacklog;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class GameLibraryView {
    private final MainController c;
    // Filter state is kept so filters survive navigating away and back.
    private String search = "", genre = "All genres", platform = "All platforms", tab = "All";

    public GameLibraryView(MainController c) { this.c = c; }

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

        FlowPane grid = new FlowPane(18, 18);
        grid.setPadding(new Insets(6));
        ScrollPane sp = new ScrollPane(grid);
        sp.setFitToWidth(true);
        BorderPane body = new BorderPane();
        VBox.setVgrow(body, Priority.ALWAYS);

        Runnable refresh = () -> {
            grid.getChildren().clear();
            List<Game> list = s.filter(search, genre.equals("All genres") ? null : genre,
                    platform.equals("All platforms") ? null : platform, tab.equals("All") ? null : tab);
            if (list.isEmpty()) {
                body.setCenter(s.all().isEmpty() ? UI.empty(c) : UI.label("No games match your filters.", "sub"));
                return;
            }
            list.forEach(g -> grid.getChildren().add(card(c, g)));
            body.setCenter(sp);
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
        HBox filters = new HBox(12, sf, gf, pf);
        refresh.run();

        VBox page = new VBox(16, header, filters, tabs, body);
        page.setPadding(new Insets(28));
        return page;
    }

    /** A clickable game card, also used by the dashboard. */
    static Node card(MainController c, Game g) {
        // FIXED: Changed UI.liveCover to UI.cover
        Node cover = UI.cover(g, 190, 120);
        VBox v = new VBox(6, cover, UI.label(g.getTitle(), "card-title"),
                UI.label(g.getGenre() + "  •  " + g.getPlatform(), "sub"), UI.badge(g.getStatus()),
                UI.label(UI.stars(g.getRating()), "stars"), UI.label("⏱  " + UI.hours(g.getHours()), "sub"), UI.playButton(c, g));
        v.getStyleClass().add("game-card");
        v.setPrefWidth(222);
        v.setMaxWidth(222);
        v.setOnMouseClicked(e -> c.showDetails(g));
        return v;

    }

}