package gamebacklog;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/** Business logic: validation, filtering, statistics. */
public class GameService {
    public static final List<String> STATUSES = List.of("To Play", "Playing", "Completed");
    public static final List<String> PLATFORMS = List.of("PC", "PlayStation 5", "PlayStation 4", "Xbox Series X/S",
            "Xbox One", "Nintendo Switch", "Mobile", "Other");
    private static final List<String> GENRES = List.of("Action", "Adventure", "RPG", "Strategy", "Simulation",
            "Sports", "Racing", "Puzzle", "Shooter", "Horror", "Platformer", "Indie");

    private final DatabaseManager db;

    public GameService(DatabaseManager db, boolean seed) {
        this.db = db;
        if (seed) loadSamples();
    }

    public List<Game> all() { return db.findAll(); }
    public Game find(int id) { return db.find(id); }
    public List<long[]> sessions(int gameId) { return db.sessions(gameId); }

    public List<Game> filter(String query, String genre, String platform, String status) {
        String q = query == null ? "" : query.trim().toLowerCase();
        return db.findAll().stream()
                .filter(g -> g.getTitle().toLowerCase().contains(q))
                .filter(g -> genre == null || genre.equalsIgnoreCase(g.getGenre()))
                .filter(g -> platform == null || platform.equals(g.getPlatform()))
                .filter(g -> status == null || status.equals(g.getStatus()))
                .collect(Collectors.toList());
    }

    /** Returns an error message, or null when valid. */
    public String validate(Game g) {
        if (g.getTitle() == null || g.getTitle().isBlank()) return "Game title is required.";
        if (g.getTitle().length() > 100) return "Title must be 100 characters or fewer.";
        if (g.getGenre() == null || g.getGenre().isBlank()) return "Genre is required.";
        if (g.getPlatform() == null || g.getPlatform().isBlank()) return "Please choose a platform.";
        if (!STATUSES.contains(g.getStatus())) return "Please choose a status.";
        if (g.getRating() < 0 || g.getRating() > 5) return "Rating must be between 1 and 5 stars.";
        double h = g.getHours();
        if (Double.isNaN(h) || Double.isInfinite(h) || h < 0) return "Hours played cannot be negative or invalid.";
        if (h > 100000) return "Hours played is unrealistically high.";
        String exe = g.getExePath();
        if (exe != null && !exe.isBlank() && !exe.trim().toLowerCase().endsWith(".exe")) return "Executable path must point to a .exe file.";
        return null;
    }

    /** Validates then inserts (id == 0) or updates. Returns an error message or null on success. */
    public String save(Game g) {
        g.setTitle(g.getTitle() == null ? "" : g.getTitle().trim());
        g.setGenre(g.getGenre() == null ? "" : g.getGenre().trim());
        g.setExePath(cleanPath(g.getExePath()));
        String err = validate(g);
        if (err != null) return err;
        if (g.getId() == 0) db.insert(g); else db.update(g);
        return null;
    }

    public void delete(Game g) { db.delete(g.getId()); }
    public void clear() { db.deleteAll(); }

    public long count(String status) { return db.findAll().stream().filter(g -> g.getStatus().equals(status)).count(); }
    public double totalHours() { return db.findAll().stream().mapToDouble(Game::getHours).sum(); }

    public List<String> genres() {
        TreeSet<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        set.addAll(GENRES);
        db.findAll().forEach(g -> { if (g.getGenre() != null && !g.getGenre().isBlank()) set.add(g.getGenre()); });
        return new ArrayList<>(set);
    }

    public void loadSamples() {
        Object[][] d = {
            {"The Legend of Zelda: Tears of the Kingdom", "Adventure", "Nintendo Switch", "Playing", 5, 62.5, "Building a flying machine took me hours."},
            {"Elden Ring", "RPG", "PC", "Completed", 5, 140.0, "Brutal but unforgettable open world."},
            {"Hades", "Indie", "PC", "Completed", 4, 48.0, "Great combat loop and story."},
            {"God of War Ragnarök", "Action", "PlayStation 5", "Playing", 5, 21.0, "Stunning visuals."},
            {"Baldur's Gate 3", "RPG", "PC", "To Play", 0, 0.0, "Everyone recommends it."},
            {"Hollow Knight", "Platformer", "Nintendo Switch", "To Play", 0, 0.0, "Waiting for a long weekend."},
            {"Forza Horizon 5", "Racing", "Xbox Series X/S", "Completed", 4, 35.5, "Perfect for relaxing."},
            {"Stardew Valley", "Simulation", "PC", "Playing", 4, 80.0, "Just one more day…"}
        };
        for (Object[] r : d)
            db.insert(new Game((String) r[0], (String) r[1], (String) r[2], (String) r[3], (int) r[4], (double) r[5], "", (String) r[6]));
    }

    /** Trims spaces and the quotation marks that Windows adds with "Copy as path". */
    public static String cleanPath(String p) {
        if (p == null) return "";
        String s = p.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length() - 1).trim();
        return s;
    }

    /** Used by the Add/Edit form. Empty is fine (launching is optional); otherwise the file must exist and end in .exe. */
    public String checkExe(String raw) {
        String p = cleanPath(raw);
        if (p.isEmpty()) return null;
        if (!p.toLowerCase().endsWith(".exe"))
            return "The game path must point to the actual .exe file, not a folder or another file type. Click ? for help.";
        if (!new File(p).isFile())
            return "Executable not found: " + p + ". Check the path or use Browse… to select the file.";
        return null;
    }
}
