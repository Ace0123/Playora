package gamebacklog;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.File;

/** Small shared helpers for building the interface. */
final class UI {
    private UI() {}

    static Label label(String text, String... styleClasses) {
        Label l = new Label(text);
        l.getStyleClass().addAll(styleClasses);
        return l;
    }

    static Button button(String text, String styleClass, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().addAll("btn", styleClass);
        b.setOnAction(e -> action.run());
        return b;
    }

    static String stars(int r) { return "★".repeat(Math.max(0, r)) + "☆".repeat(Math.max(0, 5 - r)); }
    static String hours(double h) { return h == Math.rint(h) ? (long) h + " h" : String.format("%.1f h", h); }
    static String plain(double h) { return h == Math.rint(h) ? String.valueOf((long) h) : String.valueOf(h); }

    static Label badge(String status) {
        return label(status, "badge", "badge-" + status.toLowerCase().replace(' ', '-'));
    }

    private static String hex(Color c) {
        return String.format("#%02X%02X%02X", (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }

    /** Fixed-size rounded cover: placeholder at once, real image swapped in when it has loaded in the background. */
    static StackPane cover(Game g, double w, double h) {
        StackPane p = new StackPane();
        p.setMinSize(w, h); p.setPrefSize(w, h); p.setMaxSize(w, h);
        Rectangle clip = new Rectangle(w, h);
        clip.setArcWidth(20); clip.setArcHeight(20);
        p.setClip(clip);
        fill(p, g, w, h);
        return p;
    }

    // REMOVED: liveCover() method because it relied on the deleted GameCoverService

    static void fill(StackPane p, Game g, double w, double h) {
        double hue = Math.abs(g.getTitle().hashCode()) % 360;
        p.setStyle("-fx-background-color:linear-gradient(to bottom right," + hex(Color.hsb(hue, 0.55, 0.6))
                + "," + hex(Color.hsb((hue + 50) % 360, 0.65, 0.3)) + ");");
        Label l = new Label(g.getTitle().isEmpty() ? "?" : g.getTitle().substring(0, 1).toUpperCase());
        l.setStyle("-fx-font-size:" + (int) (h / 2.5) + "px;-fx-font-weight:bold;-fx-text-fill:rgba(255,255,255,0.85);");
        p.getChildren().setAll(l);
        String path = g.getCoverPath();
        if (path == null || path.isBlank() || !new File(path).isFile()) return;
        Image img = new Image(new File(path).toURI().toString(), w * 2, 0, true, true, true); // background loading
        Runnable show = () -> {
            if (img.isError() || img.getWidth() <= 0) return;   // keep the placeholder
            double iw = img.getWidth(), ih = img.getHeight(), want = w / h;
            ImageView iv = new ImageView(img);
            iv.setFitWidth(w); iv.setFitHeight(h);
            iv.setViewport(iw / ih > want ? new Rectangle2D((iw - ih * want) / 2, 0, ih * want, ih)
                    : new Rectangle2D(0, (ih - iw / want) / 2, iw, iw / want));   // centre-crop to the card shape
            p.setStyle("");
            p.getChildren().setAll(iv);
        };
        if (img.getProgress() >= 1.0) show.run();
        else img.progressProperty().addListener((o, old, now) -> { if (now.doubleValue() >= 1.0) show.run(); });
    }

    static String when(String iso) {
        return iso == null || iso.isBlank() ? "Never" : iso.replace('T', ' ').substring(0, Math.min(16, iso.length()));
    }

    static String whenMs(long ms) {
        return when(java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime().toString());
    }

    /** "Play Game" button; shows "Currently Playing" while this game's session is active. */
    static Button playButton(MainController c, Game g) {
        Button b = button("▶  Play Game", "btn-play", () -> c.play(g));
        if (c.sessions().activeGameId() == g.getId()) { b.setText("●  Currently Playing"); b.setDisable(true); }
        b.addEventHandler(MouseEvent.MOUSE_CLICKED, MouseEvent::consume); // don't open details when clicking Play
        return b;
    }

    static Node empty(MainController c) {
        VBox v = new VBox(12, label("🎮", "empty-icon"), label("Your library is empty", "page-title"),
                label("Add your first game to start tracking your backlog.", "sub"),
                button("➕  Add your first game", "btn-primary", () -> c.showForm(null)));
        v.setAlignment(Pos.CENTER);
        v.setPadding(new Insets(60));
        return v;
    }
}