package com.readingtracker.models;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:reading_tracker.db";

    public Database() {
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS entries (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT NOT NULL," +
                    "type TEXT NOT NULL," +
                    "description TEXT," +
                    "date DATE NOT NULL," +
                    "cover_path TEXT," +
                    "chapters INTEGER," +
                    "season INTEGER," +
                    "episode INTEGER," +
                    "venue TEXT," +
                    "is_single_volume BOOLEAN," +
                    "comic_series_number INTEGER," +
                    "comic_issue_number INTEGER)";
            stmt.execute(sql);
            
            // Agregar columnas si no existen (para bases de datos existentes)
            try {
                stmt.execute("ALTER TABLE entries ADD COLUMN chapters INTEGER");
            } catch (SQLException e) {
                // Columna ya existe
            }
            try {
                stmt.execute("ALTER TABLE entries ADD COLUMN season INTEGER");
            } catch (SQLException e) {
                // Columna ya existe
            }
            try {
                stmt.execute("ALTER TABLE entries ADD COLUMN episode INTEGER");
            } catch (SQLException e) {
                // Columna ya existe
            }
            try {
                stmt.execute("ALTER TABLE entries ADD COLUMN venue TEXT");
            } catch (SQLException e) {
                // Columna ya existe
            }
            try {
                stmt.execute("ALTER TABLE entries ADD COLUMN is_single_volume BOOLEAN");
            } catch (SQLException e) {
                // Columna ya existe
            }
            try {
                stmt.execute("ALTER TABLE entries ADD COLUMN comic_series_number INTEGER");
            } catch (SQLException e) {
                // Columna ya existe
            }
            try {
                stmt.execute("ALTER TABLE entries ADD COLUMN comic_issue_number INTEGER");
            } catch (SQLException e) {
                // Columna ya existe
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addEntry(Entry entry) {
        String sql = "INSERT INTO entries(title, type, description, date, cover_path, chapters, season, episode, venue, is_single_volume, comic_series_number, comic_issue_number) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, entry.getTitle());
            pstmt.setString(2, entry.getType());
            pstmt.setString(3, entry.getDescription());
            pstmt.setDate(4, Date.valueOf(entry.getDate()));
            pstmt.setString(5, entry.getCoverPath());
            pstmt.setObject(6, entry.getChapters());
            pstmt.setObject(7, entry.getSeason());
            pstmt.setObject(8, entry.getEpisode());
            pstmt.setString(9, entry.getVenue());
            pstmt.setObject(10, entry.getIsSingleVolume());
            pstmt.setObject(11, entry.getComicSeriesNumber());
            pstmt.setObject(12, entry.getComicIssueNumber());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateEntry(Entry entry) {
        String sql = "UPDATE entries SET title=?, type=?, description=?, date=?, cover_path=?, chapters=?, season=?, episode=?, venue=?, is_single_volume=?, comic_series_number=?, comic_issue_number=? WHERE id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, entry.getTitle());
            pstmt.setString(2, entry.getType());
            pstmt.setString(3, entry.getDescription());
            pstmt.setDate(4, Date.valueOf(entry.getDate()));
            pstmt.setString(5, entry.getCoverPath());
            pstmt.setObject(6, entry.getChapters());
            pstmt.setObject(7, entry.getSeason());
            pstmt.setObject(8, entry.getEpisode());
            pstmt.setString(9, entry.getVenue());
            pstmt.setObject(10, entry.getIsSingleVolume());
            pstmt.setObject(11, entry.getComicSeriesNumber());
            pstmt.setObject(12, entry.getComicIssueNumber());
            pstmt.setInt(13, entry.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Entry> getAllEntries() {
        List<Entry> entries = new ArrayList<>();
        String sql = "SELECT * FROM entries ORDER BY date DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Entry entry = new Entry(
                        rs.getString("title"),
                        rs.getString("type"),
                        rs.getString("description"),
                        rs.getDate("date").toLocalDate(),
                        rs.getString("cover_path"),
                        rs.getObject("chapters") != null ? rs.getInt("chapters") : null,
                        rs.getObject("season") != null ? rs.getInt("season") : null,
                        rs.getObject("episode") != null ? rs.getInt("episode") : null
                );
                entry.setId(rs.getInt("id"));
                entry.setVenue(rs.getString("venue"));
                entry.setIsSingleVolume(rs.getObject("is_single_volume") != null ? rs.getBoolean("is_single_volume") : null);
                entry.setComicSeriesNumber(rs.getObject("comic_series_number") != null ? rs.getInt("comic_series_number") : null);
                entry.setComicIssueNumber(rs.getObject("comic_issue_number") != null ? rs.getInt("comic_issue_number") : null);
                entries.add(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entries;
    }

    public void deleteEntry(int id) {
        String sql = "DELETE FROM entries WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public Integer getLastChapterForTitle(String title, String type) {
        Integer lastChapter = null;
        String sql = "";
        
        if (type.contains("Libro")) {
            sql = "SELECT MAX(chapters) as max_chapter FROM entries WHERE title = ? AND type LIKE '%Libro%'";
        } else if (type.contains("Serie")) {
            sql = "SELECT MAX(episode) as max_chapter FROM entries WHERE title = ? AND type LIKE '%Serie%' ORDER BY season DESC LIMIT 1";
        }
        
        if (sql.isEmpty()) return null;
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, title);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Object obj = rs.getObject("max_chapter");
                if (obj != null) {
                    lastChapter = rs.getInt("max_chapter");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return lastChapter;
    }
    
    public SeriesInfo getLastSeriesInfo(String title) {
        String sql = "SELECT season, episode FROM entries WHERE title = ? AND type LIKE '%Serie%' ORDER BY date DESC LIMIT 1";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, title);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Integer season = rs.getObject("season") != null ? rs.getInt("season") : null;
                Integer episode = rs.getObject("episode") != null ? rs.getInt("episode") : null;
                return new SeriesInfo(season, episode);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public List<String> getTitleSuggestions(String type) {
        List<String> suggestions = new ArrayList<>();
        String sql = "SELECT DISTINCT title FROM entries WHERE type LIKE ? ORDER BY date DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String typePattern = type.contains("Libro") ? "%Libro%" : type.contains("Serie") ? "%Serie%" : "%Película%";
            pstmt.setString(1, typePattern);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                suggestions.add(rs.getString("title"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return suggestions;
    }
    
    public List<Entry> searchByTitle(String searchTerm) {
        List<Entry> entries = new ArrayList<>();
        String sql = "SELECT * FROM entries WHERE LOWER(title) LIKE LOWER(?) ORDER BY date DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + searchTerm + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Entry entry = new Entry(
                        rs.getString("title"),
                        rs.getString("type"),
                        rs.getString("description"),
                        rs.getDate("date").toLocalDate(),
                        rs.getString("cover_path"),
                        rs.getObject("chapters") != null ? rs.getInt("chapters") : null,
                        rs.getObject("season") != null ? rs.getInt("season") : null,
                        rs.getObject("episode") != null ? rs.getInt("episode") : null
                );
                entry.setId(rs.getInt("id"));
                entry.setVenue(rs.getString("venue"));
                entry.setIsSingleVolume(rs.getObject("is_single_volume") != null ? rs.getBoolean("is_single_volume") : null);
                entry.setComicSeriesNumber(rs.getObject("comic_series_number") != null ? rs.getInt("comic_series_number") : null);
                entry.setComicIssueNumber(rs.getObject("comic_issue_number") != null ? rs.getInt("comic_issue_number") : null);
                entries.add(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entries;
    }
    
    public List<Entry> searchByDate(LocalDate date) {
        List<Entry> entries = new ArrayList<>();
        String sql = "SELECT * FROM entries WHERE date = ? ORDER BY date DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(date));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Entry entry = new Entry(
                        rs.getString("title"),
                        rs.getString("type"),
                        rs.getString("description"),
                        rs.getDate("date").toLocalDate(),
                        rs.getString("cover_path"),
                        rs.getObject("chapters") != null ? rs.getInt("chapters") : null,
                        rs.getObject("season") != null ? rs.getInt("season") : null,
                        rs.getObject("episode") != null ? rs.getInt("episode") : null
                );
                entry.setId(rs.getInt("id"));
                entry.setVenue(rs.getString("venue"));
                entry.setIsSingleVolume(rs.getObject("is_single_volume") != null ? rs.getBoolean("is_single_volume") : null);
                entry.setComicSeriesNumber(rs.getObject("comic_series_number") != null ? rs.getInt("comic_series_number") : null);
                entry.setComicIssueNumber(rs.getObject("comic_issue_number") != null ? rs.getInt("comic_issue_number") : null);
                entries.add(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entries;
    }
    
    public List<Entry> searchByTitleAndType(String searchTerm, String type) {
        List<Entry> entries = new ArrayList<>();
        String sql = "SELECT * FROM entries WHERE LOWER(title) LIKE LOWER(?) AND type LIKE ? ORDER BY date DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + searchTerm + "%");
            pstmt.setString(2, type);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Entry entry = new Entry(
                        rs.getString("title"),
                        rs.getString("type"),
                        rs.getString("description"),
                        rs.getDate("date").toLocalDate(),
                        rs.getString("cover_path"),
                        rs.getObject("chapters") != null ? rs.getInt("chapters") : null,
                        rs.getObject("season") != null ? rs.getInt("season") : null,
                        rs.getObject("episode") != null ? rs.getInt("episode") : null
                );
                entry.setId(rs.getInt("id"));
                entry.setVenue(rs.getString("venue"));
                entry.setIsSingleVolume(rs.getObject("is_single_volume") != null ? rs.getBoolean("is_single_volume") : null);
                entry.setComicSeriesNumber(rs.getObject("comic_series_number") != null ? rs.getInt("comic_series_number") : null);
                entry.setComicIssueNumber(rs.getObject("comic_issue_number") != null ? rs.getInt("comic_issue_number") : null);
                entries.add(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entries;
    }
    
    public Map<String, Integer> getStatsByTypeAndPeriod(Integer month, Integer year) {
        Map<String, Integer> stats = new HashMap<>();
        StringBuilder sql = new StringBuilder("SELECT type, COUNT(*) as count FROM entries WHERE 1=1");
        if (year != null) {
            sql.append(" AND strftime('%Y', date) = ?");
        }
        if (month != null) {
            sql.append(" AND CAST(strftime('%m', date) AS INTEGER) = ?");
        }
        sql.append(" GROUP BY type");
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (year != null) {
                pstmt.setString(paramIndex++, String.valueOf(year));
            }
            if (month != null) {
                pstmt.setInt(paramIndex++, month);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                stats.put(rs.getString("type"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }

    public List<Entry> getEntriesByPeriod(Integer month, Integer year) {
        List<Entry> entries = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM entries WHERE 1=1");
        if (year != null) {
            sql.append(" AND strftime('%Y', date) = ?");
        }
        if (month != null) {
            sql.append(" AND CAST(strftime('%m', date) AS INTEGER) = ?");
        }
        sql.append(" ORDER BY date DESC");
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (year != null) {
                pstmt.setString(paramIndex++, String.valueOf(year));
            }
            if (month != null) {
                pstmt.setInt(paramIndex++, month);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Entry entry = new Entry(
                        rs.getString("title"),
                        rs.getString("type"),
                        rs.getString("description"),
                        rs.getDate("date").toLocalDate(),
                        rs.getString("cover_path"),
                        rs.getObject("chapters") != null ? rs.getInt("chapters") : null,
                        rs.getObject("season") != null ? rs.getInt("season") : null,
                        rs.getObject("episode") != null ? rs.getInt("episode") : null
                );
                entry.setId(rs.getInt("id"));
                entry.setVenue(rs.getString("venue"));
                entry.setIsSingleVolume(rs.getObject("is_single_volume") != null ? rs.getBoolean("is_single_volume") : null);
                entry.setComicSeriesNumber(rs.getObject("comic_series_number") != null ? rs.getInt("comic_series_number") : null);
                entry.setComicIssueNumber(rs.getObject("comic_issue_number") != null ? rs.getInt("comic_issue_number") : null);
                entries.add(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entries;
    }

    public static class SeriesInfo {
        public int season;
        public int episode;
        
        public SeriesInfo(Integer season, Integer episode) {
            this.season = season != null ? season : 1;
            this.episode = episode != null ? episode : 1;
        }
    }
}
