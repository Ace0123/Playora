package gamebacklog;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Tiny key/value settings stored in settings.properties (theme, RAWG API key). */
public class AppSettings {
    private final Properties props = new Properties();
    private final File file = AppPaths.file("settings.properties");

    public AppSettings() {
        if (file.isFile())
            try (Reader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) { props.load(r); }
            catch (IOException ignored) { }
    }

    public String get(String key, String def) { return props.getProperty(key, def); }

    public synchronized void set(String key, String value) {
        props.setProperty(key, value);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) { props.store(w, "My Game Backlog settings"); }
        catch (IOException ignored) { }
    }

    /** The RAWG key: environment variable RAWG_API_KEY wins, otherwise the key saved from the Settings page. */
    public String rawgKey() {
        String env = System.getenv("RAWG_API_KEY");
        return env != null && !env.isBlank() ? env.trim() : props.getProperty("rawg_api_key", "").trim();
    }
}
