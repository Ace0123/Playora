package gamebacklog;

import javafx.scene.Node;

/** Dark/light theme. Colours live in styles.css; the ".light" class on a root node switches them. */
public class ThemeManager {
    private final AppSettings settings;
    private boolean dark;

    public ThemeManager(AppSettings settings) {
        this.settings = settings;
        dark = !"light".equals(settings.get("theme", "dark"));
    }

    public boolean isDark() { return dark; }

    public void toggle() {
        dark = !dark;
        settings.set("theme", dark ? "dark" : "light");
    }

    /** Apply to the scene root and to any dialog pane. */
    public void apply(Node root) {
        root.getStyleClass().remove("light");
        if (!dark) root.getStyleClass().add("light");
    }
}
