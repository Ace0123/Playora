package gamebacklog;

public class Game {
    private int id, rating;
    private double hours;
    private String title, genre, platform, status, coverPath, notes, exePath = "", processName = "", lastPlayed = "";

    // FIELD FOR PRIORITY
    private String priority; // "High", "Medium", "Low"

    public Game() {
        title = "";
        status = "To Play";
        platform = "PC";
        genre = "";
        notes = "";
        coverPath = "";
        priority = "Medium"; // Default priority
    }

    public Game(String title, String genre, String platform, String status, int rating, double hours, String coverPath, String notes) {
        this.title = title;
        this.genre = genre;
        this.platform = platform;
        this.status = status;
        this.rating = rating;
        this.hours = hours;
        this.coverPath = coverPath;
        this.notes = notes;
        this.priority = "Medium"; // Default priority
    }

    public int getId() { return id; }            public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }   public void setTitle(String t) { title = t; }
    public String getGenre() { return genre; }   public void setGenre(String g) { genre = g; }
    public String getPlatform() { return platform; } public void setPlatform(String p) { platform = p; }
    public String getStatus() { return status; } public void setStatus(String s) { status = s; }
    public int getRating() { return rating; }    public void setRating(int r) { rating = r; }
    public double getHours() { return hours; }   public void setHours(double h) { hours = h; }
    public String getCoverPath() { return coverPath; } public void setCoverPath(String c) { coverPath = c; }
    public String getNotes() { return notes; }   public void setNotes(String n) { notes = n; }

    public String getExePath() { return exePath; }     public void setExePath(String e) { exePath = e == null ? "" : e; }
    public String getProcessName() { return processName; } public void setProcessName(String p) { processName = p == null ? "" : p; }
    public String getLastPlayed() { return lastPlayed; } public void setLastPlayed(String l) { lastPlayed = l == null ? "" : l; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}