package gamebacklog;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Duration;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Launches games and tracks ONE play session at a time.
 * Time comes from real timestamps (never UI ticks); a background thread watches the process,
 * and a heartbeat is saved every ~30 s so a crash/shutdown never counts time the PC was off.
 */
public class SessionManager {
    private final DatabaseManager db;
    private final Runnable onChange;
    private final Consumer<String> onMessage;
    private final ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "session-watch");
        t.setDaemon(true);
        return t;
    });
    private final StringProperty liveText = new SimpleStringProperty("");

    private volatile boolean active;
    private volatile long startMs;
    private volatile int gameId;
    private int sessionId;
    private long ticks;
    private boolean seen;
    private String procName = "";
    private ProcessHandle handle;

    public SessionManager(DatabaseManager db, Runnable onChange, Consumer<String> onMessage) {
        this.db = db; this.onChange = onChange; this.onMessage = onMessage;
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(1),
                e -> liveText.set(active ? fmt(elapsedSeconds()) : "")));
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();
        recover();
        pool.scheduleWithFixedDelay(this::tick, 2, 2, TimeUnit.SECONDS);
    }

    public StringProperty liveTextProperty() { return liveText; }
    public boolean isActive() { return active; }
    public int activeGameId() { return active ? gameId : -1; }
    public long elapsedSeconds() { return active ? Math.max(0, (System.currentTimeMillis() - startMs) / 1000) : 0; }
    public static String fmt(long s) { return String.format("%02d:%02d:%02d", s / 3600, s % 3600 / 60, s % 60); }

    /** Launches the game. Returns an error message, or null on success (tracking then starts automatically). */
    public synchronized String launch(Game g) {
        if (active) return "Another game session is already being tracked.";
        String exe = GameService.cleanPath(g.getExePath());
        if (exe.isEmpty()) return "No .exe is set for this game. Edit the game to add one (use Log time for consoles).";
        File f = new File(exe);
        if (!f.isFile()) return "Executable not found: " + exe;
        Process p;
        try { p = new ProcessBuilder(f.getAbsolutePath()).directory(f.getParentFile()).start(); }
        catch (Exception e) {
            String m = String.valueOf(e.getMessage());
            if (m.contains("error=740")) m = "this game needs administrator rights. Start it from its launcher or shortcut instead.";
            return "Could not launch the game: " + m;
        }
        // Only reached when the launch succeeded, so playtime is never recorded for a failed launch.
        gameId = g.getId();
        procName = g.getProcessName() == null ? "" : g.getProcessName().trim().toLowerCase();
        handle = p.toHandle();
        seen = procName.isEmpty();
        ticks = 0;
        long now = System.currentTimeMillis();
        sessionId = db.startSession(gameId, now, p.pid());
        startMs = now;
        active = true;
        notifyChange(null);
        return null;
    }

    /** Manual stop (also the recovery option when automatic detection fails). */
    public synchronized void end() { finish(System.currentTimeMillis(), ""); }

    private void finish(long endMs, String prefix) {
        if (!active) return;
        active = false;
        long secs = Math.max(0, (endMs - startMs) / 1000);
        // finishSession only succeeds once per session, so time can never be added twice.
        if (db.finishSession(sessionId, endMs, secs) == 1)
            db.addPlaytime(gameId, secs / 3600.0, Instant.ofEpochMilli(endMs).atZone(ZoneId.systemDefault()).toLocalDateTime().toString());
        notifyChange(prefix + "Session ended: " + fmt(secs) + " added to playtime."
                + (secs < 10 ? " (Very short. If this game uses a launcher, set its Process name in Edit Game.)" : ""));
    }

    private synchronized void tick() {
        if (!active) return;
        try {
            long now = System.currentTimeMillis();
            boolean alive = procName.isEmpty() ? (handle != null && handle.isAlive()) : findByName();
            if (alive) seen = true;
            if (++ticks % 15 == 0 && alive) db.beat(sessionId, now);
            if (alive) return;
            if (seen) finish(now, "");
            else if (now - startMs > 90_000) {
                active = false;
                db.cancelSession(sessionId);
                notifyChange("Process \"" + procName + "\" was not detected, so no playtime was recorded.");
            }
        } catch (Exception ignored) { /* keep tracking, retry on the next tick */ }
    }

    private boolean findByName() {
        return ProcessHandle.allProcesses().anyMatch(ph -> ph.info().command()
                .map(c -> new File(c).getName().equalsIgnoreCase(procName)).orElse(false));
    }

    /** After a restart: resume if the game is still running, otherwise close at the last heartbeat. */
    private void recover() {
        DatabaseManager.SessionRow r = db.activeSession();
        if (r == null) return;
        Game g = db.find(r.gameId);
        if (g == null) { db.cancelSession(r.id); return; }
        sessionId = r.id; gameId = r.gameId; startMs = r.startMs; ticks = 0; seen = true;
        procName = g.getProcessName() == null ? "" : g.getProcessName().trim().toLowerCase();
        handle = r.pid > 0 ? ProcessHandle.of(r.pid).filter(ProcessHandle::isAlive).orElse(null) : null;
        active = true;
        if (procName.isEmpty() ? handle != null : findByName()) notifyChange("Resumed tracking \"" + g.getTitle() + "\".");
        else finish(Math.max(r.startMs, r.beatMs), "Recovered an unfinished session. ");
    }

    private void notifyChange(String msg) {
        Platform.runLater(() -> {
            liveText.set(active ? fmt(elapsedSeconds()) : "");
            onChange.run();
            if (msg != null) onMessage.accept(msg);
        });
    }
}
