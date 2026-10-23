package gamebacklog;

import java.io.File;

/** Where the app keeps its data. Installed programs can't write next to themselves, so we use the user's AppData folder. */
final class AppPaths {
    static final File DIR;

    static {
        String appData = System.getenv("APPDATA");
        DIR = appData != null ? new File(appData, "Playora") : new File(System.getProperty("user.home"), ".playora");
        DIR.mkdirs();
    }

    private AppPaths() { }

    static File file(String name) { return new File(DIR, name); }
}
