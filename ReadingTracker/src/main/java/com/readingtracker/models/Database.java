package com.readingtracker.models;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
                    "comic_number INTEGER)";
            stmt.execute(sql);

            String[] columns = {"chapters", "season", "episode", "venue", "is_single_volume", "comic_number"};
            for (String col : columns) {
                try {
                    stmt.execute("ALTER TABLE entries ADD COLUMN " + col + (col.equals("venue") ? " TEXT" : col.equals("is_single_volume") ? " BOOLEAN" : " INTEGER"));
                } catch (SQLException e) {
                    // Columna ya existe
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addEntry(Entry entry) {
        String sql = "INSERT INTO entries(title, type, description, date, cover_path, chapters, season, episode, venue, is_single_volume, comic_number) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
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
            pstmt.setObject(11, entry.getComicNumber());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateEntry(Entry entry) {
        String sql = "UPDATE entries SET title=?, type=?, description=?, date=?, cover_path=?, chapters=?, season=?, episode=?, venue=?, is_single_volume=?, comic_number=? WHERE id=?";
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
            pstmt.setObject(11, entry.getComicNumber());
            pstmt.setInt(12, entry.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Entry mapResultSet(ResultSet rs) throws SQLException {
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
        entry.setComicNumber(rs.getObject("comic_number") != null ? rs.getInt("comic_number") : null);
        return entry;
    }

    public List<Entry> getAllEntries() {
        List<Entry> entries = new ArrayList<>();
        String sql = "SELECT * FROM entries ORDER BY date DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                entries.add(mapResultSet(rs));
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
            if (rs.next() && rs.getObject("max_chapter") != null) {
                return rs.getInt("max_chapter");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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
            String typePattern = type.contains("Libro") ? "%Libro%" : type.contains("Serie") ? "%Serie%" : "%Pel\u00edcula%";
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
            while (rs.next()) entries.add(mapResultSet(rs));
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
            while (rs.next()) entries.add(mapResultSet(rs));
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
            while (rs.next()) entries.add(mapResultSet(rs));
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
