package gamebacklog;

import javafx.animation.PauseTransition;
import javafx.application.HostServices;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Builds the main window and coordinates navigation between views. */
public class MainController {
    private final Stage stage;
    private final GameService service;
    private final GameLibraryView library;
    private final SessionManager sessions;
    private final AppSettings settings;
    private final ThemeManager theme;
    private final HostServices hosts;
    private final Button themeBtn = new Button();
    private StackPane root;
    private Runnable currentRefresh = () -> { };
    private final BorderPane layout = new BorderPane();
    private final Label toast = new Label();
    private final PauseTransition toastTimer = new PauseTransition(Duration.seconds(2.8));
    private final List<Button> navButtons = new ArrayList<>();
    private final String css = getClass().getResource("/styles.css").toExternalForm();

    public MainController(Stage stage, HostServices hosts) {
        this.stage = stage;
        this.hosts = hosts;
        settings = new AppSettings();
        theme = new ThemeManager(settings);
        boolean firstRun = !AppPaths.file("games.db").exists();
        DatabaseManager db = new DatabaseManager(AppPaths.file("games.db").getAbsolutePath());
        service = new GameService(db, firstRun);
        sessions = new SessionManager(db, () -> currentRefresh.run(), m -> toast(m, m.contains("not detected")));
        library = new GameLibraryView(this);

        Label brand = UI.label("🎮  My Game", "app-title");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        themeBtn.getStyleClass().add("theme-btn");
        themeBtn.setTooltip(new Tooltip("Switch light / dark mode"));
        themeBtn.setOnAction(e -> { theme.toggle(); applyTheme(); });
        HBox topBar = new HBox(brand, grow, themeBtn);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        Region sgrow = new Region();
        VBox.setVgrow(sgrow, Priority.ALWAYS);

        VBox sidebar = new VBox(8,
                nav("🏠   Dashboard", this::showDashboard), nav("📚   My Games", this::showLibrary),
                nav("➕   Add Game", () -> showForm(null)), nav("⚙   Settings", this::showSettings), sgrow);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);
        layout.setTop(topBar);
        layout.setLeft(sidebar);
        layout.getStyleClass().add("content");

        toast.setVisible(false);
        toast.setMouseTransparent(true);
        toastTimer.setOnFinished(e -> toast.setVisible(false));
        StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new Insets(0, 0, 28, 0));
        root = new StackPane(layout, toast);
        applyTheme();

        Scene scene = new Scene(root, 1200, 780);
        scene.getStylesheets().add(css);
        stage.setTitle("Playora");
        stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/icon.png")));
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
        showDashboard();
    }

    public GameService service() { return service; }

    private Button nav(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-btn");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> action.run());
        navButtons.add(b);
        return b;
    }

    private void show(int activeNav, Node view) {
        if (activeNav == 3) currentRefresh = this::showSettings;
        for (int i = 0; i < navButtons.size(); i++) {
            navButtons.get(i).getStyleClass().remove("active");
            if (i == activeNav) navButtons.get(i).getStyleClass().add("active");
        }
        layout.setCenter(view);
    }

    public SessionManager sessions() { return sessions; }
    public void showDashboard() { currentRefresh = this::showDashboard; show(0, new DashboardView(this).build()); }
    public void showLibrary() { currentRefresh = this::showLibrary; show(1, library.build()); }
    public void showForm(Game g) {
        currentRefresh = () -> { };
        Game f = g == null ? null : service.find(g.getId());
        show(g == null ? 2 : 1, new GameFormView(this, f != null ? f : g).build());
    }
    public void showDetails(Game old) {
        Game fresh = service.find(old.getId());
        Game g = fresh != null ? fresh : old;
        currentRefresh = () -> showDetails(g);
        show(1, new GameDetailsView(this, g).build());
    }

    public void play(Game g) {
        if (sessions.isActive()) {
            if (sessions.activeGameId() == g.getId()) { toast("This game is already being tracked.", true); return; }
            Game cur = service.find(sessions.activeGameId());
            if (!confirm("\"" + (cur == null ? "Another game" : cur.getTitle()) + "\" is currently being tracked.\nEnd that session and launch \"" + g.getTitle() + "\"?")) return;
            sessions.end();
        }
        String err = sessions.launch(g);
        if (err != null) toast(err, true);
        else toast("Launched " + g.getTitle() + " – playtime is being tracked.", false);
    }

    public void endSession() { sessions.end(); }

    public void showSettings() {
        VBox v = new VBox(16, UI.label("Settings", "page-title"),
                UI.label("Database file: " + AppPaths.file("games.db").getAbsolutePath(), "sub"),
                UI.label("Games stored: " + service.all().size(), "text"),
                new HBox(12,
                        UI.button("Load sample games", "btn-secondary", () -> {
                            service.loadSamples(); toast("Sample games added.", false); showSettings();
                        }),
                        UI.button("Delete all games", "btn-danger", () -> {
                            if (confirm("Delete ALL games permanently?")) {
                                sessions.end(); service.clear(); toast("Library cleared.", false); showSettings();
                            }
                        })));
        v.setPadding(new Insets(28));
        show(3, v);
    }

    public void toast(String msg, boolean error) {
        toast.setText((error ? "✖  " : "✔  ") + msg);
        toast.getStyleClass().setAll("toast", error ? "toast-error" : "toast-ok");
        toast.setVisible(true);
        toastTimer.playFromStart();
    }

    public boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        a.setTitle("Please confirm");
        a.initOwner(stage);
        a.getDialogPane().getStylesheets().add(css);
        theme.apply(a.getDialogPane());
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    public void openUrl(String url) { hosts.showDocument(url); }

    private void applyTheme() {
        theme.apply(root);
        themeBtn.setText(theme.isDark() ? "☀" : "🌙");
    }

    private static Label wrap(Label l) { l.setWrapText(true); return l; }

    public void showExeHelp() {
        String[] steps = {
                "Open your Windows Start Menu or Desktop and find the game you want to add.",
                "Right-click the game.",
                "Select Open file location.",
                "If this opens a shortcut folder, right-click the game shortcut again and select Open file location to reach the actual game installation folder.",
                "Find the game's executable file, usually named after the game and ending in .exe.",
                "Hold Shift and right-click the actual .exe file, then select Copy as path if that option is available.",
                "Return to Playora and paste the copied path into the Game executable (.exe) field."};
        VBox body = new VBox(10, UI.label("Follow these steps:", "section-title"));
        for (int i = 0; i < steps.length; i++) body.getChildren().add(wrap(UI.label((i + 1) + ".  " + steps[i], "text")));
        body.getChildren().addAll(UI.label("Example path:", "sub"),
                UI.label("\"C:\\Games\\ExampleGame\\ExampleGame.exe\"", "help-code"),
                wrap(UI.label("The path must point to the actual .exe file, not just the folder that contains it. "
                        + "Quotation marks added by Copy as path are fine; Playora removes them for you.", "text")));
        VBox note = new VBox(wrap(UI.label("Can't find the .exe file? Some games installed through Steam, the Xbox app, or other launchers "
                + "may use protected folders or special launch methods. You may need to locate the game's installation directory "
                + "or use its launcher instead.", "text")));
        note.getStyleClass().add("help-note");
        body.getChildren().add(note);
        body.setPadding(new Insets(4, 10, 4, 4));
        ScrollPane sp = new ScrollPane(body);
        sp.setFitToWidth(true);
        sp.setPrefViewportWidth(560);
        sp.setPrefViewportHeight(470);
        Dialog<Void> d = new Dialog<>();
        d.setTitle("How to Find Your Game's .exe File");
        d.setHeaderText("How to Find Your Game's .exe File");
        d.initOwner(stage);
        d.initModality(Modality.APPLICATION_MODAL);
        d.getDialogPane().setContent(sp);
        d.getDialogPane().getButtonTypes().add(new ButtonType("Got it", ButtonBar.ButtonData.OK_DONE));
        d.getDialogPane().getStylesheets().add(css);
        theme.apply(d.getDialogPane());
        d.showAndWait();
    }
}