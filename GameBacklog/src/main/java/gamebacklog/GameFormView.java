package gamebacklog;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

/** Add (game == null) or edit form. */
public class GameFormView {
    private final MainController c;
    private final Game game;
    private int rating;

    public GameFormView(MainController c, Game game) { this.c = c; this.game = game; }

    public Node build() {
        boolean edit = game != null;
        Game g = edit ? game : new Game();
        rating = g.getRating();

        TextField title = new TextField(g.getTitle());
        title.setPromptText("e.g. Elden Ring");
        ComboBox<String> genre = new ComboBox<>(FXCollections.observableArrayList(c.service().genres()));
        genre.setEditable(true);
        genre.getEditor().setText(g.getGenre());
        genre.setMaxWidth(Double.MAX_VALUE);
        ComboBox<String> platform = combo(GameService.PLATFORMS, g.getPlatform());
        ComboBox<String> status = combo(GameService.STATUSES, g.getStatus());

        ComboBox<String> priority = new ComboBox<>(FXCollections.observableArrayList("High", "Medium", "Low"));
        priority.setValue(g.getPriority() != null ? g.getPriority() : "Medium");
        priority.setMaxWidth(Double.MAX_VALUE);

        TextField hours = new TextField(String.format("%.2f", g.getHours()));

        StackPane preview = new StackPane();
        preview.setMaxWidth(240);
        java.util.function.Consumer<String> showPreview = p -> {
            Game t = new Game();
            t.setTitle(title.getText().isBlank() ? "?" : title.getText());
            t.setCoverPath(p);
            preview.getChildren().setAll(UI.cover(t, 240, 135));
        };

        TextField cover = new TextField(g.getCoverPath());
        cover.setPromptText("Optional – path to an image");
        HBox.setHgrow(cover, Priority.ALWAYS);
        Button browse = UI.button("Browse…", "btn-secondary", () -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
            File f = fc.showOpenDialog(cover.getScene().getWindow());
            if (f != null) {
                cover.setText(f.getAbsolutePath());
                showPreview.accept(f.getAbsolutePath());
            }
        });

        TextField exe = new TextField(g.getExePath());
        exe.setPromptText("PC games only – path to the game's .exe");
        HBox.setHgrow(exe, Priority.ALWAYS);
        exe.focusedProperty().addListener((o, was, now) -> { if (!now) exe.setText(GameService.cleanPath(exe.getText())); });
        Button exeBrowse = UI.button("Browse…", "btn-secondary", () -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select the game's .exe file");
            fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Windows programs (*.exe)", "*.exe"),
                    new FileChooser.ExtensionFilter("All files", "*.*"));
            File cur = new File(GameService.cleanPath(exe.getText()));
            File dir = cur.isDirectory() ? cur : cur.getParentFile();
            if (dir != null && dir.isDirectory()) fc.setInitialDirectory(dir);
            File f = fc.showOpenDialog(exe.getScene().getWindow());
            if (f == null) return;
            if (!f.getName().toLowerCase().endsWith(".exe")) { c.toast("Please choose a file ending in .exe.", true); return; }
            exe.setText(f.getAbsolutePath());
        });
        Button exeHelp = UI.button("?", "btn-help", c::showExeHelp);
        exeHelp.setTooltip(new Tooltip("How do I find my game's .exe file?"));
        TextField proc = new TextField(g.getProcessName());
        proc.setPromptText("Optional – e.g. eldenring.exe (only if the game starts through a launcher)");

        showPreview.accept(cover.getText());

        HBox stars = new HBox(4);
        renderStars(stars);
        TextArea notes = new TextArea(g.getNotes());
        notes.setPromptText("Optional – your notes or review");
        notes.setPrefRowCount(4);
        notes.setWrapText(true);
        Label err = UI.label("", "error-text");

        Runnable save = () -> {
            try {
                String gameTitle = title.getText().trim();
                String hoursStr = hours.getText().trim();

                if (gameTitle.isEmpty() || hoursStr.isEmpty()) {
                    err.setText("Title and Hours played cannot be empty.");
                    return;
                }

                double h = Double.parseDouble(hoursStr);
                if (rating < 1 || rating > 5) {
                    err.setText("Please select a rating between 1 and 5 stars.");
                    return;
                }
            } catch (NumberFormatException ex) {
                err.setText("Hours played must be a valid number.");
                return;
            }

            double h;
            try { h = Double.parseDouble(hours.getText().trim()); }
            catch (NumberFormatException ex) { err.setText("Hours played must be a number."); return; }

            String exeErr = c.service().checkExe(exe.getText());
            if (exeErr != null) { err.setText(exeErr); c.toast(exeErr, true); return; }

            Game n = new Game(title.getText(), genre.getEditor().getText(), platform.getValue(), status.getValue(),
                    rating, h, cover.getText().trim(), notes.getText());
            n.setId(g.getId());
            n.setExePath(GameService.cleanPath(exe.getText()));
            n.setProcessName(proc.getText().trim());
            n.setLastPlayed(g.getLastPlayed());
            n.setPriority(priority.getValue());

            String e = c.service().save(n);
            if (e != null) {
                err.setText(e);
            } else {
                c.toast(edit ? "Game updated successfully!" : "Game added to your library!", false);
                c.showLibrary();
            }
        };

        Runnable cancel = () -> { if (edit) c.showDetails(game); else c.showLibrary(); };

        VBox form = new VBox(14, UI.label(edit ? "Edit Game" : "Add Game", "page-title"),
                group("Title *", title), group("Genre *", genre), group("Platform *", platform),
                group("Status *", status),
                group("Priority *", priority),
                group("Personal rating (click a star again to clear)", stars),
                group("Hours played", hours),
                group("Cover image", new VBox(8, new HBox(8, cover, browse), preview)),
                group("Game executable (.exe)", new HBox(8, exe, exeBrowse, exeHelp)),
                group("Process name (launcher games)", proc),
                group("Notes / review", notes), err,
                new HBox(12, UI.button("💾  Save", "btn-primary", save), UI.button("Cancel", "btn-secondary", cancel)));

        form.setMaxWidth(640);
        form.setPadding(new Insets(28));
        ScrollPane sp = new ScrollPane(form);
        sp.setFitToWidth(true);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return sp;

    }

    private void renderStars(HBox box) {
        box.getChildren().clear();
        for (int i = 1; i <= 5; i++) {
            int v = i;
            Label l = UI.label(i <= rating ? "★" : "☆", "star-pick");
            l.setOnMousePressed(e -> { rating = (rating == v) ? 0 : v; renderStars(box); });
            box.getChildren().add(l);
        }
    }

    private ComboBox<String> combo(List<String> items, String value) {
        ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList(items));
        cb.setValue(items.contains(value) ? value : items.get(0));
        cb.setMaxWidth(Double.MAX_VALUE);
        return cb;
    }

    private VBox group(String label, Node n) { return new VBox(6, UI.label(label, "sub"), n); }
}