package gamebacklog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** All SQLite access lives here. */
public class DatabaseManager {
    private final String url;

    public DatabaseManager(String file) {
        url = "jdbc:sqlite:" + file;
        try (Connection c = conn(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS games(id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, "
                    + "genre TEXT, platform TEXT, status TEXT, rating INTEGER, hours REAL, cover TEXT, notes TEXT)");

            // Added 'priority' to the migration list and removed RAWG columns
            for (String col : new String[]{"exe TEXT DEFAULT ''", "procname TEXT DEFAULT ''", "last_played TEXT DEFAULT ''", "priority TEXT DEFAULT 'Medium'"})
                try { s.execute("ALTER TABLE games ADD COLUMN " + col); } catch (SQLException ignored) { /* already exists */ }

            s.execute("CREATE TABLE IF NOT EXISTS sessions(id INTEGER PRIMARY KEY AUTOINCREMENT, game_id INTEGER NOT NULL, "
                    + "start_ms INTEGER, end_ms INTEGER, last_beat_ms INTEGER, seconds INTEGER DEFAULT 0, pid INTEGER DEFAULT 0, active INTEGER DEFAULT 1)");
        } catch (SQLException e) { throw new RuntimeException("Could not open database", e); }
    }

    private Connection conn() throws SQLException { return DriverManager.getConnection(url); }

    public List<Game> findAll() {
        List<Game> list = new ArrayList<>();
        try (Connection c = conn(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT * FROM games ORDER BY title COLLATE NOCASE")) {
            while (r.next()) {
                Game g = new Game(r.getString("title"), r.getString("genre"), r.getString("platform"), r.getString("status"),
                        r.getInt("rating"), r.getDouble("hours"), r.getString("cover"), r.getString("notes"));
                g.setId(r.getInt("id"));
                g.setExePath(r.getString("exe"));
                g.setProcessName(r.getString("procname"));
                g.setLastPlayed(r.getString("last_played"));
                g.setPriority(r.getString("priority"));
                list.add(g);
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public void insert(Game g) {
        write("INSERT INTO games(title,genre,platform,status,rating,hours,cover,notes,exe,procname,last_played,priority) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", g, false);
    }

    public void update(Game g) {
        write("UPDATE games SET title=?,genre=?,platform=?,status=?,rating=?,hours=?,cover=?,notes=?,exe=?,procname=?,last_played=?,priority=? WHERE id=?", g, true);
    }

    private void write(String sql, Game g, boolean withId) {
        try (Connection c = conn(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, g.getTitle()); p.setString(2, g.getGenre()); p.setString(3, g.getPlatform());
            p.setString(4, g.getStatus()); p.setInt(5, g.getRating()); p.setDouble(6, g.getHours());
            p.setString(7, g.getCoverPath()); p.setString(8, g.getNotes());
            p.setString(9, g.getExePath()); p.setString(10, g.getProcessName()); p.setString(11, g.getLastPlayed());
            p.setString(12, g.getPriority());
            if (withId) p.setInt(13, g.getId());
            p.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void delete(int id) {
        try (Connection c = conn(); PreparedStatement p = c.prepareStatement("DELETE FROM games WHERE id=?")) {
            p.setInt(1, id); p.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void deleteAll() {
        try (Connection c = conn(); Statement s = c.createStatement()) { s.execute("DELETE FROM games"); s.execute("DELETE FROM sessions"); }
        catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Game find(int id) { return findAll().stream().filter(g -> g.getId() == id).findFirst().orElse(null); }

    public static class SessionRow { public int id, gameId; public long startMs, beatMs, pid; }

    private int exec(String sql, Object... a) {
        try (Connection c = conn(); PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 0; i < a.length; i++) p.setObject(i + 1, a[i]);
            return p.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public int startSession(int gameId, long startMs, long pid) {
        try (Connection c = conn(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO sessions(game_id,start_ms,last_beat_ms,pid) VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            p.setInt(1, gameId); p.setLong(2, startMs); p.setLong(3, startMs); p.setLong(4, pid);
            p.executeUpdate();
            try (ResultSet k = p.getGeneratedKeys()) { return k.next() ? k.getInt(1) : -1; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void beat(int id, long ms) { exec("UPDATE sessions SET last_beat_ms=? WHERE id=? AND active=1", ms, id); }

    public int finishSession(int id, long endMs, long secs) {
        return exec("UPDATE sessions SET active=0,end_ms=?,seconds=?,last_beat_ms=? WHERE id=? AND active=1", endMs, secs, endMs, id);
    }

    public void cancelSession(int id) { exec("DELETE FROM sessions WHERE id=?", id); }

    public void addPlaytime(int gameId, double hours, String lastPlayed) {
        exec("UPDATE games SET hours=hours+?, last_played=? WHERE id=?", hours, lastPlayed, gameId);
    }

    public SessionRow activeSession() {
        try (Connection c = conn(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT * FROM sessions WHERE active=1 ORDER BY id DESC LIMIT 1")) {
            if (!r.next()) return null;
            SessionRow x = new SessionRow();
            x.id = r.getInt("id"); x.gameId = r.getInt("game_id"); x.startMs = r.getLong("start_ms");
            x.beatMs = r.getLong("last_beat_ms"); x.pid = r.getLong("pid");
            return x;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<long[]> sessions(int gameId) {
        List<long[]> l = new ArrayList<>();
        try (Connection c = conn(); PreparedStatement p = c.prepareStatement(
                "SELECT start_ms,seconds FROM sessions WHERE game_id=? AND active=0 ORDER BY id DESC LIMIT 5")) {
            p.setInt(1, gameId);
            try (ResultSet r = p.executeQuery()) { while (r.next()) l.add(new long[]{r.getLong(1), r.getLong(2)}); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return l;
    }
}