package com.readingtracker.models;

import java.time.LocalDate;

public class Entry {
    private int id;
    private String title;
    private String type;
    private String description;
    private LocalDate date;
    private String coverPath;
    private Integer chapters;        // Para libros
    private Integer season;          // Para series
    private Integer episode;         // Para series
    private String venue;            // Para teatro (lugar)
    private Boolean isSingleVolume;  // Para cómic (es tomo único)
    private Integer comicNumber;     // Para cómic (número de la serie/tomo)

    public Entry(String title, String type, String description, LocalDate date, String coverPath) {
        this.title = title;
        this.type = type;
        this.description = description;
        this.date = date;
        this.coverPath = coverPath;
    }

    public Entry(String title, String type, String description, LocalDate date, String coverPath,
                 Integer chapters, Integer season, Integer episode) {
        this.title = title;
        this.type = type;
        this.description = description;
        this.date = date;
        this.coverPath = coverPath;
        this.chapters = chapters;
        this.season = season;
        this.episode = episode;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getCoverPath() { return coverPath; }
    public void setCoverPath(String coverPath) { this.coverPath = coverPath; }

    public Integer getChapters() { return chapters; }
    public void setChapters(Integer chapters) { this.chapters = chapters; }
    public Integer getSeason() { return season; }
    public void setSeason(Integer season) { this.season = season; }
    public Integer getEpisode() { return episode; }
    public void setEpisode(Integer episode) { this.episode = episode; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public Boolean getIsSingleVolume() { return isSingleVolume; }
    public void setIsSingleVolume(Boolean isSingleVolume) { this.isSingleVolume = isSingleVolume; }
    public Integer getComicNumber() { return comicNumber; }
    public void setComicNumber(Integer comicNumber) { this.comicNumber = comicNumber; }
}
