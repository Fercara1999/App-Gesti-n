package com.readingtracker.models;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:reading_tracker.db";

    public Database() { initializeDatabase(); }

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS entries (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT NOT NULL, type TEXT NOT NULL, description TEXT," +
                    "date DATE NOT NULL, cover_path TEXT," +
                    "chapters INTEGER, author TEXT, season INTEGER, episode INTEGER," +
                    "venue TEXT, is_single_volume BOOLEAN, comic_volume INTEGER, comic_issue INTEGER," +
                    "director TEXT, seen_in_cinema BOOLEAN," +
                    "rating INTEGER, finished BOOLEAN, season_finished BOOLEAN, series_finished BOOLEAN)");
            // Migración automática
            String[][] cols = {
                {"chapters","INTEGER"},{"author","TEXT"},{"season","INTEGER"},{"episode","INTEGER"},
                {"venue","TEXT"},{"is_single_volume","BOOLEAN"},{"comic_volume","INTEGER"},
                {"comic_issue","INTEGER"},{"director","TEXT"},{"seen_in_cinema","BOOLEAN"},
                {"rating","INTEGER"},{"finished","BOOLEAN"},{"season_finished","BOOLEAN"},{"series_finished","BOOLEAN"}
            };
            for (String[] col : cols) {
                try { stmt.execute("ALTER TABLE entries ADD COLUMN " + col[0] + " " + col[1]); }
                catch (SQLException e) { /* ya existe */ }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static final String INSERT_SQL =
        "INSERT INTO entries(title,type,description,date,cover_path,chapters,author,season,episode," +
        "venue,is_single_volume,comic_volume,comic_issue,director,seen_in_cinema,rating,finished,season_finished,series_finished) " +
        "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    public void addEntry(Entry e) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement p = conn.prepareStatement(INSERT_SQL)) {
            p.setString(1,e.getTitle()); p.setString(2,e.getType()); p.setString(3,e.getDescription());
            p.setDate(4,Date.valueOf(e.getDate())); p.setString(5,e.getCoverPath());
            p.setObject(6,e.getChapters()); p.setString(7,e.getAuthor());
            p.setObject(8,e.getSeason()); p.setObject(9,e.getEpisode());
            p.setString(10,e.getVenue()); p.setObject(11,e.getIsSingleVolume());
            p.setObject(12,e.getComicVolume()); p.setObject(13,e.getComicIssue());
            p.setString(14,e.getDirector()); p.setObject(15,e.getSeenInCinema());
            p.setObject(16,e.getRating()); p.setObject(17,e.getFinished());
            p.setObject(18,e.getSeasonFinished()); p.setObject(19,e.getSeriesFinished());
            p.executeUpdate();
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private static final String UPDATE_SQL =
        "UPDATE entries SET title=?,type=?,description=?,date=?,cover_path=?,chapters=?,author=?,season=?,episode=?," +
        "venue=?,is_single_volume=?,comic_volume=?,comic_issue=?,director=?,seen_in_cinema=?," +
        "rating=?,finished=?,season_finished=?,series_finished=? WHERE id=?";

    public void updateEntry(Entry e) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement p = conn.prepareStatement(UPDATE_SQL)) {
            p.setString(1,e.getTitle()); p.setString(2,e.getType()); p.setString(3,e.getDescription());
            p.setDate(4,Date.valueOf(e.getDate())); p.setString(5,e.getCoverPath());
            p.setObject(6,e.getChapters()); p.setString(7,e.getAuthor());
            p.setObject(8,e.getSeason()); p.setObject(9,e.getEpisode());
            p.setString(10,e.getVenue()); p.setObject(11,e.getIsSingleVolume());
            p.setObject(12,e.getComicVolume()); p.setObject(13,e.getComicIssue());
            p.setString(14,e.getDirector()); p.setObject(15,e.getSeenInCinema());
            p.setObject(16,e.getRating()); p.setObject(17,e.getFinished());
            p.setObject(18,e.getSeasonFinished()); p.setObject(19,e.getSeriesFinished());
            p.setInt(20,e.getId());
            p.executeUpdate();
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private Entry mapResultSet(ResultSet rs) throws SQLException {
        Entry e = new Entry(
            rs.getString("title"), rs.getString("type"), rs.getString("description"),
            rs.getDate("date").toLocalDate(), rs.getString("cover_path"),
            rs.getObject("chapters")!=null?rs.getInt("chapters"):null,
            rs.getObject("season")  !=null?rs.getInt("season")  :null,
            rs.getObject("episode") !=null?rs.getInt("episode") :null);
        e.setId(rs.getInt("id"));
        e.setAuthor(rs.getString("author"));
        e.setVenue(rs.getString("venue"));
        e.setIsSingleVolume(rs.getObject("is_single_volume")!=null?rs.getBoolean("is_single_volume"):null);
        e.setComicVolume(rs.getObject("comic_volume")!=null?rs.getInt("comic_volume"):null);
        e.setComicIssue(rs.getObject("comic_issue") !=null?rs.getInt("comic_issue") :null);
        e.setDirector(rs.getString("director"));
        e.setSeenInCinema(rs.getObject("seen_in_cinema")!=null?rs.getBoolean("seen_in_cinema"):null);
        e.setRating(rs.getObject("rating")!=null?rs.getInt("rating"):null);
        e.setFinished(rs.getObject("finished")!=null?rs.getBoolean("finished"):null);
        e.setSeasonFinished(rs.getObject("season_finished")!=null?rs.getBoolean("season_finished"):null);
        e.setSeriesFinished(rs.getObject("series_finished")!=null?rs.getBoolean("series_finished"):null);
        return e;
    }

    public List<Entry> getAllEntries() {
        List<Entry> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM entries ORDER BY date DESC, id DESC")) {
            while (rs.next()) list.add(mapResultSet(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Entry> getRecentByType(String typeLike, int limit) {
        List<Entry> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement p = conn.prepareStatement(
                "SELECT * FROM entries WHERE type LIKE ? ORDER BY date DESC, id DESC LIMIT ?")) {
            p.setString(1,"%"+typeLike+"%"); p.setInt(2,limit);
            ResultSet rs = p.executeQuery();
            while (rs.next()) list.add(mapResultSet(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void deleteEntry(int id) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement p = conn.prepareStatement("DELETE FROM entries WHERE id=?")) {
            p.setInt(1,id); p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public String getAuthorForTitle(String title) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement p = conn.prepareStatement(
                "SELECT author FROM entries WHERE title=? AND type LIKE '%Libro%' AND author IS NOT NULL AND author != '' LIMIT 1")) {
            p.setString(1,title); ResultSet rs = p.executeQuery();
            if (rs.next()) return rs.getString("author");
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public String getDirectorForTitle(String title) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement p = conn.prepareStatement(
                "SELECT director FROM entries WHERE title=? AND type LIKE '%Pel%' AND director IS NOT NULL LIMIT 1")) {
            p.setString(1,title); ResultSet rs = p.executeQuery();
            if (rs.next()) return rs.getString("director");
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public Integer getLastChapterForTitle(String title, String type) {
        String sql = type.contains("Libro") ?
            "SELECT MAX(chapters) as val FROM entries WHERE title=? AND type LIKE '%Libro%'" :
            type.contains("Serie") ?
            "SELECT MAX(episode) as val FROM entries WHERE title=? AND type LIKE '%Serie%'" : "";
        if (sql.isEmpty()) return null;
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1,title); ResultSet rs = p.executeQuery();
            if (rs.next() && rs.getObject("val")!=null) return rs.getInt("val");
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public SeriesInfo getLastSeriesInfo(String title) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement p = conn.prepareStatement(
                "SELECT season,episode FROM entries WHERE title=? AND type LIKE '%Serie%' ORDER BY season DESC, episode DESC LIMIT 1")) {
            p.setString(1,title); ResultSet rs = p.executeQuery();
            if (rs.next()) return new SeriesInfo(
                rs.getObject("season") !=null?rs.getInt("season") :null,
                rs.getObject("episode")!=null?rs.getInt("episode"):null);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public List<String> getTitleSuggestions(String type) {
        List<String> list = new ArrayList<>();
        String pattern = type.contains("Libro") ? "%Libro%" :
                         type.contains("Serie") ? "%Serie%" :
                         type.contains("Pel")   ? "%Pel%"   :
                         type.contains("\u00f3mic")  ? "%\u00f3mic%" : "%";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement p = conn.prepareStatement(
                "SELECT DISTINCT title FROM entries WHERE type LIKE ? ORDER BY title ASC")) {
            p.setString(1,pattern); ResultSet rs = p.executeQuery();
            while (rs.next()) list.add(rs.getString("title"));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Entry> searchByTitle(String term) {
        List<Entry> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement p = conn.prepareStatement(
                "SELECT * FROM entries WHERE LOWER(title) LIKE LOWER(?) ORDER BY date DESC")) {
            p.setString(1,"%"+term+"%"); ResultSet rs = p.executeQuery();
            while (rs.next()) list.add(mapResultSet(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Entry> searchByDate(LocalDate date) {
        List<Entry> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement p = conn.prepareStatement(
                "SELECT * FROM entries WHERE date=? ORDER BY date DESC")) {
            p.setDate(1,Date.valueOf(date)); ResultSet rs = p.executeQuery();
            while (rs.next()) list.add(mapResultSet(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Entry> searchByTitleAndType(String term, String type) {
        List<Entry> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement p = conn.prepareStatement(
                "SELECT * FROM entries WHERE LOWER(title) LIKE LOWER(?) AND type LIKE ? ORDER BY date DESC")) {
            p.setString(1,"%"+term+"%"); p.setString(2,type);
            ResultSet rs = p.executeQuery();
            while (rs.next()) list.add(mapResultSet(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static class SeriesInfo {
        public int season, episode;
        public SeriesInfo(Integer season, Integer episode) {
            this.season  = season  != null ? season  : 1;
            this.episode = episode != null ? episode : 1;
        }
    }
}
