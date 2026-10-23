package gamebacklog;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class GameDetailsView {
    private final MainController c;
    private final Game g;

    public GameDetailsView(MainController c, Game g) { this.c = c; this.g = g; }

    public Node build() {
        Label title = UI.label(g.getTitle(), "page-title");
        title.setWrapText(true);

        // Status: one-click segmented buttons
        ToggleGroup statusGroup = new ToggleGroup();
        FlowPane statusRow = new FlowPane(8, 8);
        for (String st : GameService.STATUSES) {
            ToggleButton b = new ToggleButton(st);
            b.getStyleClass().add("tab-btn");
            b.setToggleGroup(statusGroup);
            b.setSelected(st.equals(g.getStatus()));
            b.setOnAction(e -> {
                b.setSelected(true);
                if (st.equals(g.getStatus())) return;
                g.setStatus(st);
                c.service().save(g);
                c.toast("Status changed to " + st, false);
                c.showDetails(g);
            });
            statusRow.getChildren().add(b);
        }

        // Log extra playtime
        TextField add = new TextField();
        add.setPromptText("Hours");
        add.setPrefWidth(90);
        add.setMinHeight(40);
        Button log = UI.button("+ Log time", "btn-secondary", () -> {
            double v;
            try { v = Double.parseDouble(add.getText().trim()); }
            catch (NumberFormatException ex) { c.toast("Enter a valid number of hours.", true); return; }
            if (Double.isNaN(v) || Double.isInfinite(v) || v <= 0) { c.toast("Hours must be greater than 0.", true); return; }
            double old = g.getHours();
            g.setHours(old + v);
            String err = c.service().save(g);
            if (err != null) { g.setHours(old); c.toast(err, true); return; }
            c.toast("Added " + UI.hours(v) + " of playtime", false);
            c.showDetails(g);
        });
        log.setMinHeight(40);
        HBox logRow = new HBox(8, add, log);
        logRow.setAlignment(Pos.CENTER_LEFT);

        String exe = g.getExePath() == null ? "" : g.getExePath();
        Label exeLabel = UI.label("📁  " + (exe.isBlank() ? "No game .exe set. Click Edit to add one." : exe), "sub");
        exeLabel.setWrapText(true);

        // Quick actions now live in the left column, under the cover
        VBox actions = new VBox(16, UI.label("Quick actions", "section-title"), sessionRow(),
                new VBox(8, UI.label("Log playtime", "sub"), logRow),
                new VBox(8, UI.label("Status", "sub"), statusRow), exeLabel);
        actions.getStyleClass().add("stat-card");

        Label notes = UI.label(g.getNotes() == null || g.getNotes().isBlank() ? "No notes added." : g.getNotes(), "text");
        notes.setWrapText(true);
        VBox notesBox = new VBox(6, UI.label("Notes / review", "section-title"), notes);
        notesBox.getStyleClass().add("stat-card");

        VBox left = new VBox(18, UI.cover(g, 360, 230), actions, notesBox);        left.setMinWidth(360);
        left.setPrefWidth(360);
        left.setMaxWidth(360);

        // Right column: title, stats, sessions, buttons (stretches to fill the width)
        HBox meta = new HBox(12, UI.badge(g.getStatus()), UI.label(g.getGenre() + "  •  " + g.getPlatform(), "sub"));
        meta.setAlignment(Pos.CENTER_LEFT);
        HBox tiles = new HBox(14,
                tile("Rating", UI.label(UI.stars(g.getRating()), "stars-big")),
                tile("Total playtime", UI.label(UI.hours(g.getHours()), "stat-value")),
                tile("Last played", UI.label(UI.when(g.getLastPlayed()), "text")));
        HBox buttons = new HBox(12, UI.button("✏  Edit", "btn-primary", () -> c.showForm(g)),
                UI.button("🗑  Delete", "btn-danger", () -> {
                    if (c.sessions().activeGameId() == g.getId()) { c.toast("End the current session before deleting this game.", true); return; }
                    if (c.confirm("Delete \"" + g.getTitle() + "\"? This cannot be undone.")) {
                        c.service().delete(g);
                        c.toast("Game deleted.", false);
                        c.showLibrary();
                    }
                }));
        VBox info = new VBox(16, title, meta, tiles, recent(), buttons);
        info.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(info, Priority.ALWAYS);
        HBox row = new HBox(28, left, info);

        VBox page = new VBox(18, UI.button("←  Back to library", "btn-secondary", c::showLibrary), row);
        page.setPadding(new Insets(28));
        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        return sp;
    }

    private Node tile(String name, Node value) {
        VBox v = new VBox(6, UI.label(name, "sub"), value);
        v.getStyleClass().add("stat-card");
        v.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(v, Priority.ALWAYS);
        return v;
    }

    /** Big Play button, or (while this game runs) the live timer and End Session, wrapping so nothing is cut off. */
    private Node sessionRow() {
        if (c.sessions().activeGameId() != g.getId()) {
            Button play = UI.playButton(c, g);
            play.setMaxWidth(Double.MAX_VALUE);
            return play;
        }
        Label live = UI.label("", "live-time");
        live.textProperty().bind(c.sessions().liveTextProperty());
        Label now = UI.label("● Currently Playing", "section-title");
        now.setMinWidth(Region.USE_PREF_SIZE);
        live.setMinWidth(Region.USE_PREF_SIZE);
        FlowPane r = new FlowPane(16, 10, now, live, UI.button("⏹  End Session", "btn-danger", c::endSession));
        r.setAlignment(Pos.CENTER_LEFT);
        r.setRowValignment(VPos.CENTER);
        return r;
    }

    private Node recent() {
        VBox v = new VBox(8, UI.label("Recent sessions", "section-title"));
        java.util.List<long[]> list = c.service().sessions(g.getId());
        if (list.isEmpty()) v.getChildren().add(UI.label("No tracked sessions yet.", "sub"));
        for (long[] s : list) {
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            v.getChildren().add(new HBox(UI.label(UI.whenMs(s[0]), "sub"), sp, UI.label(SessionManager.fmt(s[1]), "text")));
        }
        v.getStyleClass().add("stat-card");
        v.setMaxWidth(Double.MAX_VALUE);
        return v;
    }
}