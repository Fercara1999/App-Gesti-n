package com.readingtracker;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.readingtracker.models.Database;
import com.readingtracker.models.Entry;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class Main extends Application {
    private Database db;
    private VBox entriesContainer;
    private TextArea descriptionArea;
    private ComboBox<String> typeCombo;
    private ComboBox<String> titleField;
    private TextField titleTextField;
    private DatePicker datePicker;
    private ImageView coverPreview;
    private String selectedCoverPath = null;

    private VBox dynamicFieldsBox;
    private TextField chapterTextField;
    private TextField seasonTextField;
    private TextField episodeTextField;
    private TextField venueTextField;
    private CheckBox isSingleVolumeCheckBox;
    private TextField comicSeriesNumberTextField;
    private TextField comicIssueNumberTextField;
    private TextField searchTitleField;
    private DatePicker searchDatePicker;
    private ComboBox<String> searchTypeCombo;

    private VBox statsContainer;

    private static final String PRIMARY_COLOR = "#6C63FF";
    private static final String PRIMARY_DARK = "#5A52D5";
    private static final String ACCENT_COLOR = "#FF6584";
    private static final String BG_COLOR = "#F0F2F5";
    private static final String CARD_BG = "#FFFFFF";
    private static final String TEXT_PRIMARY = "#2D3436";
    private static final String TEXT_SECONDARY = "#636E72";
    private static final String SUCCESS_COLOR = "#00B894";
    private static final String BORDER_COLOR = "#DFE6E9";

    @Override
    public void start(Stage stage) throws Exception {
        db = new Database();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-font-family: 'Segoe UI', 'Arial', sans-serif; -fx-background-color: " + BG_COLOR + ";");

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-tab-min-height: 40; -fx-tab-max-height: 40; -fx-background-color: " + CARD_BG + ";");

        Tab registerTab = new Tab("Registrar", createInputPanel());
        registerTab.setStyle("-fx-font-size: 13;");

        Tab searchTab = new Tab("Buscar", createSearchPanel());
        searchTab.setStyle("-fx-font-size: 13;");

        Tab statsTab = new Tab("Estadisticas", createStatsPanel());
        statsTab.setStyle("-fx-font-size: 13;");

        tabPane.getTabs().addAll(registerTab, searchTab, statsTab);
        root.setTop(tabPane);

        updateTitleSuggestions();
        updateDynamicFields();

        entriesContainer = new VBox(12);
        entriesContainer.setPadding(new Insets(15));
        entriesContainer.setStyle("-fx-background-color: " + BG_COLOR + ";");

        ScrollPane scrollPane = new ScrollPane(entriesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: " + BG_COLOR + "; -fx-background: " + BG_COLOR + ";");
        root.setCenter(scrollPane);

        loadEntries();

        Scene scene = new Scene(root, 1100, 800);
        stage.setTitle("Registro de Lectura y Visualizacion");
        stage.setScene(scene);
        stage.show();
    }

    private VBox createInputPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(20, 25, 20, 25));
        panel.setStyle("-fx-background-color: " + CARD_BG + "; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 0 0 2 0;");

        Label titleLabel = new Label("Registrar nueva entrada");
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_COLOR + ";");
        panel.getChildren().add(titleLabel);

        Separator sep = new Separator();
        panel.getChildren().add(sep);

        HBox row1 = new HBox(15);
        row1.setAlignment(Pos.BOTTOM_LEFT);

        VBox titleBox = new VBox(5);
        Label label1 = createFieldLabel("Titulo:");
        titleField = new ComboBox<>();
        titleField.setEditable(true);
        titleField.setPrefWidth(350);
        titleField.setStyle(createInputStyle());
        titleTextField = titleField.getEditor();
        titleTextField.setPromptText("Ej: El Quijote, Spiderman...");
        titleTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty() && typeCombo.getValue() != null) {
                updateDynamicFields();
            }
        });
        titleBox.getChildren().addAll(label1, titleField);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        VBox typeBox = new VBox(5);
        Label label2 = createFieldLabel("Tipo:");
        typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Libro", "Serie", "Pelicula", "Teatro", "Comic");
        typeCombo.setValue("Libro");
        typeCombo.setPrefWidth(160);
        typeCombo.setStyle(createInputStyle());
        typeCombo.setOnAction(e -> { updateTitleSuggestions(); updateDynamicFields(); });
        typeBox.getChildren().addAll(label2, typeCombo);

        VBox dateBox = new VBox(5);
        Label label3 = createFieldLabel("Fecha:");
        datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now());
        datePicker.setPrefWidth(160);
        datePicker.setStyle(createInputStyle());
        dateBox.getChildren().addAll(label3, datePicker);

        row1.getChildren().addAll(titleBox, typeBox, dateBox);
        panel.getChildren().add(row1);

        createDynamicFields();
        panel.getChildren().add(dynamicFieldsBox);

        VBox descBox = new VBox(5);
        Label label4 = createFieldLabel("Descripcion/Notas:");
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("Que te parecio?");
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setStyle("-fx-control-inner-background: #FAFBFC; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 13;");
        descBox.getChildren().addAll(label4, descriptionArea);
        VBox.setVgrow(descBox, Priority.SOMETIMES);
        panel.getChildren().add(descBox);

        HBox row3 = new HBox(20);
        row3.setAlignment(Pos.CENTER_LEFT);

        VBox coverBox = new VBox(5);
        Label label5 = createFieldLabel("Portada (arrastra imagen):");
        coverPreview = new ImageView();
        coverPreview.setFitWidth(100);
        coverPreview.setFitHeight(140);
        StackPane coverPane = new StackPane(coverPreview);
        coverPane.setStyle("-fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 2; -fx-border-style: dashed; -fx-border-radius: 8; -fx-background-color: #FAFBFC; -fx-background-radius: 8;");
        coverPane.setPrefSize(104, 144);
        setupDragAndDrop();
        coverBox.getChildren().addAll(label5, coverPane);

        Button addButton = new Button("Guardar Entrada");
        addButton.setStyle(createPrimaryButtonStyle());
        addButton.setOnAction(e -> onAddEntry());

        Button clearButton = new Button("Limpiar");
        clearButton.setStyle(createSecondaryButtonStyle());
        clearButton.setOnAction(e -> clearForm());

        VBox buttonBox = new VBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(addButton, clearButton);

        row3.getChildren().addAll(coverBox, buttonBox);
        HBox.setHgrow(buttonBox, Priority.ALWAYS);
        panel.getChildren().add(row3);

        return panel;
    }

    private VBox createSearchPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(20, 25, 20, 25));
        panel.setStyle("-fx-background-color: " + CARD_BG + "; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 0 0 2 0;");

        Label titleLabel = new Label("Buscar registros");
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_COLOR + ";");
        panel.getChildren().add(titleLabel);
        panel.getChildren().add(new Separator());

        HBox row1 = new HBox(15);
        row1.setAlignment(Pos.BOTTOM_LEFT);
        VBox titleSearchBox = new VBox(5);
        Label label1 = createFieldLabel("Buscar por nombre:");
        searchTitleField = new TextField();
        searchTitleField.setPromptText("Ej: Breaking Bad, Harry Potter");
        searchTitleField.setPrefWidth(350);
        searchTitleField.setStyle(createInputStyle());
        titleSearchBox.getChildren().addAll(label1, searchTitleField);
        HBox.setHgrow(titleSearchBox, Priority.ALWAYS);
        Button searchTitleButton = new Button("Buscar");
        searchTitleButton.setStyle(createPrimaryButtonStyle());
        searchTitleButton.setOnAction(e -> onSearchByTitle());
        row1.getChildren().addAll(titleSearchBox, searchTitleButton);
        panel.getChildren().add(row1);

        HBox row2 = new HBox(15);
        row2.setAlignment(Pos.BOTTOM_LEFT);
        VBox dateBox = new VBox(5);
        Label label2 = createFieldLabel("Buscar por fecha:");
        searchDatePicker = new DatePicker();
        searchDatePicker.setPrefWidth(200);
        searchDatePicker.setStyle(createInputStyle());
        dateBox.getChildren().addAll(label2, searchDatePicker);

        VBox typeBox = new VBox(5);
        Label label3 = createFieldLabel("Filtrar por tipo:");
        searchTypeCombo = new ComboBox<>();
        searchTypeCombo.getItems().addAll("Todo", "Libro", "Serie", "Pelicula", "Teatro", "Comic");
        searchTypeCombo.setValue("Todo");
        searchTypeCombo.setPrefWidth(160);
        searchTypeCombo.setStyle(createInputStyle());
        typeBox.getChildren().addAll(label3, searchTypeCombo);

        Button searchDateButton = new Button("Buscar por fecha");
        searchDateButton.setStyle(createPrimaryButtonStyle());
        searchDateButton.setOnAction(e -> onSearchByDate());
        Button showAllButton = new Button("Ver todos");
        showAllButton.setStyle(createSecondaryButtonStyle());
        showAllButton.setOnAction(e -> loadEntries());
        row2.getChildren().addAll(dateBox, typeBox, searchDateButton, showAllButton);
        panel.getChildren().add(row2);

        Label infoLabel = new Label("Usa el campo de nombre para busquedas parciales");
        infoLabel.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 11; -fx-font-style: italic;");
        panel.getChildren().add(infoLabel);

        return panel;
    }

    private VBox createStatsPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20, 25, 20, 25));
        panel.setStyle("-fx-background-color: " + CARD_BG + "; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 0 0 2 0;");

        Label titleLabel = new Label("Estadisticas");
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_COLOR + ";");
        panel.getChildren().add(titleLabel);
        panel.getChildren().add(new Separator());

        HBox filters = new HBox(15);
        filters.setAlignment(Pos.CENTER_LEFT);

        VBox monthBox = new VBox(5);
        Label monthLabel = createFieldLabel("Mes:");
        ComboBox<String> monthCombo = new ComboBox<>();
        monthCombo.getItems().add("Todos");
        for (Month m : Month.values()) {
            monthCombo.getItems().add(m.getDisplayName(TextStyle.FULL, new Locale("es", "ES")));
        }
        monthCombo.setValue("Todos");
        monthCombo.setPrefWidth(160);
        monthCombo.setStyle(createInputStyle());
        monthBox.getChildren().addAll(monthLabel, monthCombo);

        VBox yearBox = new VBox(5);
        Label yearLabel = createFieldLabel("Ano:");
        ComboBox<Integer> yearCombo = new ComboBox<>();
        int currentYear = LocalDate.now().getYear();
        for (int y = currentYear; y >= currentYear - 10; y--) {
            yearCombo.getItems().add(y);
        }
        yearCombo.setValue(currentYear);
        yearCombo.setPrefWidth(120);
        yearCombo.setStyle(createInputStyle());
        yearBox.getChildren().addAll(yearLabel, yearCombo);

        Button refreshBtn = new Button("Actualizar");
        refreshBtn.setStyle(createPrimaryButtonStyle());
        filters.getChildren().addAll(monthBox, yearBox, refreshBtn);
        panel.getChildren().add(filters);

        statsContainer = new VBox(15);
        statsContainer.setPadding(new Insets(10, 0, 0, 0));
        panel.getChildren().add(statsContainer);

        refreshBtn.setOnAction(e -> {
            String selectedMonth = monthCombo.getValue();
            Integer selectedYear = yearCombo.getValue();
            Integer monthNum = null;
            if (!"Todos".equals(selectedMonth)) {
                for (Month m : Month.values()) {
                    if (m.getDisplayName(TextStyle.FULL, new Locale("es", "ES")).equals(selectedMonth)) {
                        monthNum = m.getValue();
                        break;
                    }
                }
            }
            refreshStats(monthNum, selectedYear);
        });

        refreshStats(null, currentYear);
        return panel;
    }

    private void refreshStats(Integer month, Integer year) {
        statsContainer.getChildren().clear();
        Map<String, Integer> counts = db.getStatsByTypeAndPeriod(month, year);
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();

        String periodText;
        if (month != null) {
            String monthName = Month.of(month).getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
            periodText = monthName.substring(0, 1).toUpperCase() + monthName.substring(1) + " " + year;
        } else {
            periodText = "Todo el ano " + year;
        }

        Label periodLabel = new Label("Periodo: " + periodText);
        periodLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");
        statsContainer.getChildren().add(periodLabel);

        HBox totalBox = new HBox(10);
        totalBox.setAlignment(Pos.CENTER_LEFT);
        totalBox.setPadding(new Insets(12));
        totalBox.setStyle("-fx-background-color: linear-gradient(to right, " + PRIMARY_COLOR + ", " + PRIMARY_DARK + "); -fx-background-radius: 10;");
        Label totalLabel = new Label("Total registros: " + total);
        totalLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: white;");
        totalBox.getChildren().add(totalLabel);
        statsContainer.getChildren().add(totalBox);

        HBox cardsRow = new HBox(12);
        cardsRow.setAlignment(Pos.CENTER_LEFT);
        String[][] typeConfig = {
            {"Libro", "#4A90D9", "Libros"},
            {"Serie", "#E67E22", "Series"},
            {"Pelicula", "#E74C3C", "Peliculas"},
            {"Teatro", "#9B59B6", "Teatro"},
            {"Comic", "#2ECC71", "Comics"}
        };
        for (String[] config : typeConfig) {
            String type = config[0];
            String color = config[1];
            String displayName = config[2];
            int count = 0;
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                if (entry.getKey().contains(type)) {
                    count = entry.getValue();
                    break;
                }
            }
            VBox card = new VBox(8);
            card.setPadding(new Insets(15));
            card.setAlignment(Pos.CENTER);
            card.setPrefWidth(150);
            card.setStyle("-fx-background-color: " + CARD_BG + "; -fx-border-color: " + color + "; -fx-border-width: 0 0 3 0; -fx-background-radius: 10; -fx-border-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
            Label countLabel = new Label(String.valueOf(count));
            countLabel.setStyle("-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
            Label nameLabel = new Label(displayName);
            nameLabel.setStyle("-fx-font-size: 13; -fx-text-fill: " + TEXT_SECONDARY + ";");
            card.getChildren().addAll(countLabel, nameLabel);
            cardsRow.getChildren().add(card);
        }
        statsContainer.getChildren().add(cardsRow);

        List<Entry> periodEntries = db.getEntriesByPeriod(month, year);
        if (!periodEntries.isEmpty()) {
            Label detailLabel = new Label("Detalle de registros:");
            detailLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");
            detailLabel.setPadding(new Insets(10, 0, 0, 0));
            statsContainer.getChildren().add(detailLabel);
            VBox detailList = new VBox(6);
            for (Entry entry : periodEntries) {
                HBox row = new HBox(10);
                row.setPadding(new Insets(8, 12, 8, 12));
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: #FAFBFC; -fx-background-radius: 6; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 6;");
                Label typeLbl = new Label(entry.getType());
                typeLbl.setStyle("-fx-font-size: 11; -fx-text-fill: " + getTypeColor(entry.getType()) + "; -fx-font-weight: bold;");
                Label titleLbl = new Label(entry.getTitle());
                titleLbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");
                HBox.setHgrow(titleLbl, Priority.ALWAYS);
                Label detailLbl = new Label(getEntryDetail(entry));
                detailLbl.setStyle("-fx-font-size: 11; -fx-text-fill: " + TEXT_SECONDARY + ";");
                Label dateLbl = new Label(entry.getDate().toString());
                dateLbl.setStyle("-fx-font-size: 11; -fx-text-fill: " + TEXT_SECONDARY + ";");
                row.getChildren().addAll(typeLbl, titleLbl, detailLbl, dateLbl);
                detailList.getChildren().add(row);
            }
            statsContainer.getChildren().add(detailList);
        } else {
            Label emptyLabel = new Label("No hay registros para este periodo");
            emptyLabel.setStyle("-fx-font-size: 14; -fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-style: italic;");
            emptyLabel.setPadding(new Insets(20));
            statsContainer.getChildren().add(emptyLabel);
        }
    }

    private String getEntryDetail(Entry entry) {
        if (entry.getType().contains("Libro") && entry.getChapters() != null) {
            return "Cap. " + entry.getChapters();
        }
        if (entry.getType().contains("Serie") && entry.getSeason() != null && entry.getEpisode() != null) {
            return "T" + entry.getSeason() + " E" + entry.getEpisode();
        }
        if (entry.getType().contains("Teatro") && entry.getVenue() != null) {
            return entry.getVenue();
        }
        if (entry.getType().contains("Comic")) {
            if (entry.getIsSingleVolume() != null && entry.getIsSingleVolume()) {
                return "Tomo unico";
            }
            String detail = "";
            if (entry.getComicSeriesNumber() != null) {
                detail += "Serie #" + entry.getComicSeriesNumber();
            }
            if (entry.getComicIssueNumber() != null) {
                detail += (detail.isEmpty() ? "" : " - ") + "Num. " + entry.getComicIssueNumber();
            }
            return detail;
        }
        return "";
    }

    private void createDynamicFields() {
        dynamicFieldsBox = new VBox(10);
        dynamicFieldsBox.setPadding(new Insets(10));
        dynamicFieldsBox.setStyle("-fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 1; -fx-padding: 12; -fx-border-radius: 8; -fx-background-color: #FAFBFC; -fx-background-radius: 8;");
        Label placeholder = new Label("Selecciona un tipo para ver opciones adicionales");
        placeholder.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-style: italic;");
        dynamicFieldsBox.getChildren().add(placeholder);
    }

    private void updateDynamicFields() {
        String selectedType = typeCombo.getValue();
        dynamicFieldsBox.getChildren().clear();
        if (selectedType == null) {
            Label placeholder = new Label("Selecciona un tipo para ver opciones adicionales");
            placeholder.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-style: italic;");
            dynamicFieldsBox.getChildren().add(placeholder);
            return;
        }
        if (selectedType.contains("Libro")) {
            VBox bookBox = new VBox(5);
            Label bookLabel = createFieldLabel("Capitulo leido:");
            chapterTextField = new TextField();
            chapterTextField.setPromptText("Ej: 5");
            chapterTextField.setPrefWidth(200);
            chapterTextField.setStyle(createInputStyle());
            String titleText = getTitleFieldValue();
            String lastChapter = suggestNextChapter(titleText, selectedType);
            if (lastChapter != null) {
                chapterTextField.setPromptText("Sugerencia: " + lastChapter);
            }
            bookBox.getChildren().addAll(bookLabel, chapterTextField);
            dynamicFieldsBox.getChildren().add(bookBox);
        } else if (selectedType.contains("Serie")) {
            HBox seriesBox = new HBox(15);
            VBox seasonBox = new VBox(5);
            Label seasonLabel = createFieldLabel("Temporada:");
            seasonTextField = new TextField();
            seasonTextField.setPromptText("Ej: 2");
            seasonTextField.setPrefWidth(100);
            seasonTextField.setStyle(createInputStyle());
            seasonBox.getChildren().addAll(seasonLabel, seasonTextField);
            VBox episodeBox = new VBox(5);
            Label episodeLabel = createFieldLabel("Capitulo:");
            episodeTextField = new TextField();
            episodeTextField.setPromptText("Ej: 8");
            episodeTextField.setPrefWidth(100);
            episodeTextField.setStyle(createInputStyle());
            String titleText = getTitleFieldValue();
            Database.SeriesInfo seriesInfo = suggestNextSeriesInfo(titleText);
            if (seriesInfo != null) {
                seasonTextField.setText(String.valueOf(seriesInfo.season));
                episodeTextField.setText(String.valueOf(seriesInfo.episode));
            }
            episodeBox.getChildren().addAll(episodeLabel, episodeTextField);
            seriesBox.getChildren().addAll(seasonBox, episodeBox);
            dynamicFieldsBox.getChildren().add(seriesBox);
        } else if (selectedType.contains("Pelicula")) {
            Label movieLabel = new Label("Solo se requiere el titulo de la pelicula");
            movieLabel.setStyle("-fx-text-fill: " + SUCCESS_COLOR + "; -fx-font-style: italic;");
            dynamicFieldsBox.getChildren().add(movieLabel);
        } else if (selectedType.contains("Teatro")) {
            VBox theaterBox = new VBox(5);
            Label venueLabel = createFieldLabel("Lugar donde se vio:");
            venueTextField = new TextField();
            venueTextField.setPromptText("Ej: Teatro Nacional");
            venueTextField.setPrefWidth(300);
            venueTextField.setStyle(createInputStyle());
            theaterBox.getChildren().addAll(venueLabel, venueTextField);
            dynamicFieldsBox.getChildren().add(theaterBox);
        } else if (selectedType.contains("Comic")) {
            VBox comicBox = new VBox(10);
            HBox checkboxRow = new HBox(10);
            checkboxRow.setAlignment(Pos.CENTER_LEFT);
            isSingleVolumeCheckBox = new CheckBox("Es tomo unico?");
            isSingleVolumeCheckBox.setStyle("-fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");
            isSingleVolumeCheckBox.setSelected(false);
            isSingleVolumeCheckBox.setOnAction(e -> updateComicFields());
            checkboxRow.getChildren().add(isSingleVolumeCheckBox);
            comicBox.getChildren().add(checkboxRow);

            VBox seriesNumberBox = new VBox(5);
            Label seriesLabel = createFieldLabel("Numero de la serie (tomo):");
            comicSeriesNumberTextField = new TextField();
            comicSeriesNumberTextField.setPromptText("Ej: 1 (Tomo 1 de Spiderman)");
            comicSeriesNumberTextField.setPrefWidth(200);
            comicSeriesNumberTextField.setStyle(createInputStyle());
            seriesNumberBox.getChildren().addAll(seriesLabel, comicSeriesNumberTextField);
            seriesNumberBox.setId("comicSeriesBox");
            comicBox.getChildren().add(seriesNumberBox);

            VBox issueNumberBox = new VBox(5);
            Label issueLabel = createFieldLabel("Numero leido (dentro del tomo):");
            comicIssueNumberTextField = new TextField();
            comicIssueNumberTextField.setPromptText("Ej: 3 (Numero 3 del Tomo 1)");
            comicIssueNumberTextField.setPrefWidth(200);
            comicIssueNumberTextField.setStyle(createInputStyle());
            issueNumberBox.getChildren().addAll(issueLabel, comicIssueNumberTextField);
            issueNumberBox.setId("comicIssueBox");
            comicBox.getChildren().add(issueNumberBox);

            dynamicFieldsBox.getChildren().add(comicBox);
        }
    }

    private void updateComicFields() {
        if (isSingleVolumeCheckBox == null) return;
        for (javafx.scene.Node node : dynamicFieldsBox.getChildren()) {
            if (node instanceof VBox) {
                VBox vbox = (VBox) node;
                for (javafx.scene.Node child : vbox.getChildren()) {
                    if (child instanceof VBox) {
                        String childId = child.getId();
                        if ("comicSeriesBox".equals(childId) || "comicIssueBox".equals(childId)) {
                            child.setVisible(!isSingleVolumeCheckBox.isSelected());
                            child.setManaged(!isSingleVolumeCheckBox.isSelected());
                        }
                    }
                }
            }
        }
    }

    private String getTitleFieldValue() {
        String value = titleField.getValue();
        return value != null ? value.trim() : "";
    }

    private Database.SeriesInfo suggestNextSeriesInfo(String title) {
        if (title.isEmpty()) return null;
        Database.SeriesInfo seriesInfo = db.getLastSeriesInfo(title);
        if (seriesInfo == null) return new Database.SeriesInfo(1, 1);
        int nextEpisode = seriesInfo.episode + 1;
        int nextSeason = seriesInfo.season;
        if (nextEpisode > 16) {
            nextEpisode = 1;
            nextSeason++;
        }
        return new Database.SeriesInfo(nextSeason, nextEpisode);
    }

    private String suggestNextChapter(String title, String type) {
        if (title.isEmpty()) return null;
        Integer lastChapter = db.getLastChapterForTitle(title, type);
        if (lastChapter == null) return null;
        if (type.contains("Libro")) return String.valueOf(lastChapter + 1);
        if (type.contains("Serie")) return "Temporada 1, Capitulo " + (lastChapter + 1);
        return null;
    }

    private void updateTitleSuggestions() {
        String selectedType = typeCombo.getValue();
        if (selectedType == null) return;
        List<String> suggestions = db.getTitleSuggestions(selectedType);
        titleField.setItems(FXCollections.observableArrayList(suggestions));
    }

    private void setupDragAndDrop() {
        coverPreview.setOnDragOver(event -> {
            event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            event.consume();
        });
        coverPreview.setOnDragDropped(event -> {
            List<java.io.File> files = event.getDragboard().getFiles();
            if (!files.isEmpty()) {
                java.io.File file = files.get(0);
                selectedCoverPath = file.getAbsolutePath();
                try {
                    Image image = new Image(new FileInputStream(file), 100, 140, true, true);
                    coverPreview.setImage(image);
                } catch (Exception e) {
                    showAlert("Error", "No se pudo cargar la imagen");
                }
            }
            event.consume();
        });
    }

    private void onAddEntry() {
        if (getTitleFieldValue().isEmpty() || typeCombo.getValue() == null) {
            showAlert("Error", "Por favor completa titulo y tipo");
            return;
        }
        String type = typeCombo.getValue();
        Integer chapters = null;
        Integer season = null;
        Integer episode = null;
        String venue = null;
        Boolean isSingleVolume = null;
        Integer comicSeriesNumber = null;
        Integer comicIssueNumber = null;

        if (type.contains("Libro")) {
            if (chapterTextField.getText().trim().isEmpty()) {
                showAlert("Error", "Ingresa el capitulo");
                return;
            }
            try {
                chapters = Integer.parseInt(chapterTextField.getText().trim());
            } catch (NumberFormatException e) {
                showAlert("Error", "Capitulo debe ser numero");
                return;
            }
        } else if (type.contains("Serie")) {
            if (seasonTextField.getText().trim().isEmpty() || episodeTextField.getText().trim().isEmpty()) {
                showAlert("Error", "Ingresa temporada y capitulo");
                return;
            }
            try {
                season = Integer.parseInt(seasonTextField.getText().trim());
                episode = Integer.parseInt(episodeTextField.getText().trim());
            } catch (NumberFormatException e) {
                showAlert("Error", "Temporada y capitulo deben ser numeros");
                return;
            }
        } else if (type.contains("Teatro")) {
            if (venueTextField.getText().trim().isEmpty()) {
                showAlert("Error", "Ingresa el lugar");
                return;
            }
            venue = venueTextField.getText();
        } else if (type.contains("Comic")) {
            isSingleVolume = isSingleVolumeCheckBox.isSelected();
            if (!isSingleVolume) {
                if (comicSeriesNumberTextField.getText().trim().isEmpty()) {
                    showAlert("Error", "Ingresa el numero de serie");
                    return;
                }
                try {
                    comicSeriesNumber = Integer.parseInt(comicSeriesNumberTextField.getText().trim());
                } catch (NumberFormatException e) {
                    showAlert("Error", "Numero de serie debe ser numero");
                    return;
                }
                if (!comicIssueNumberTextField.getText().trim().isEmpty()) {
                    try {
                        comicIssueNumber = Integer.parseInt(comicIssueNumberTextField.getText().trim());
                    } catch (NumberFormatException e) {
                        showAlert("Error", "Numero leido debe ser numero");
                        return;
                    }
                }
            }
        }

        Entry entry = new Entry(getTitleFieldValue(), type, descriptionArea.getText(), datePicker.getValue(), selectedCoverPath, chapters, season, episode);
        entry.setVenue(venue);
        entry.setIsSingleVolume(isSingleVolume);
        entry.setComicSeriesNumber(comicSeriesNumber);
        entry.setComicIssueNumber(comicIssueNumber);
        db.addEntry(entry);
        clearForm();
        loadEntries();
        showAlert("Exito", "Entrada registrada correctamente");
    }

    private void onSearchByTitle() {
        String searchTerm = searchTitleField.getText().trim();
        if (searchTerm.isEmpty()) {
            showAlert("Info", "Ingresa un termino");
            return;
        }
        String selectedType = searchTypeCombo.getValue();
        List<Entry> results;
        if ("Todo".equals(selectedType)) {
            results = db.searchByTitle(searchTerm);
        } else {
            results = db.searchByTitleAndType(searchTerm, selectedType);
        }
        displaySearchResults(results, "busqueda: '" + searchTerm + "'");
    }

    private void onSearchByDate() {
        LocalDate selectedDate = searchDatePicker.getValue();
        if (selectedDate == null) {
            showAlert("Info", "Selecciona una fecha");
            return;
        }
        displaySearchResults(db.searchByDate(selectedDate), "del " + selectedDate);
    }

    private void displaySearchResults(List<Entry> entries, String desc) {
        entriesContainer.getChildren().clear();
        if (entries.isEmpty()) {
            Label emptyLabel = new Label("No se encontraron registros para " + desc);
            emptyLabel.setStyle("-fx-font-size: 14; -fx-text-fill: " + TEXT_SECONDARY + ";");
            entriesContainer.getChildren().add(emptyLabel);
            return;
        }
        Label resultLabel = new Label(entries.size() + " registro(s) para " + desc);
        resultLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: " + SUCCESS_COLOR + ";");
        entriesContainer.getChildren().add(resultLabel);
        for (Entry entry : entries) {
            entriesContainer.getChildren().add(createEntryCard(entry));
        }
    }

    private void loadEntries() {
        entriesContainer.getChildren().clear();
        List<Entry> entries = db.getAllEntries();
        if (entries.isEmpty()) {
            Label emptyLabel = new Label("No hay registros. Comienza a registrar!");
            emptyLabel.setStyle("-fx-font-size: 16; -fx-text-fill: " + TEXT_SECONDARY + ";");
            entriesContainer.getChildren().add(emptyLabel);
            return;
        }
        for (Entry entry : entries) {
            entriesContainer.getChildren().add(createEntryCard(entry));
        }
    }

    private VBox createEntryCard(Entry entry) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: " + CARD_BG + "; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10, 0, 0, 3);");
        card.setPadding(new Insets(15));

        HBox header = new HBox(15);
        header.setAlignment(Pos.TOP_LEFT);
        if (entry.getCoverPath() != null) {
            try {
                ImageView coverImg = new ImageView(new Image(new FileInputStream(entry.getCoverPath()), 80, 120, true, true));
                header.getChildren().add(coverImg);
            } catch (Exception e) {
                header.getChildren().add(createPlaceholderCover());
            }
        } else {
            header.getChildren().add(createPlaceholderCover());
        }

        VBox info = new VBox(6);
        Label typeLabel = new Label(entry.getType());
        String typeColor = getTypeColor(entry.getType());
        typeLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: " + typeColor + "; -fx-background-color: " + typeColor + "22; -fx-padding: 3 10; -fx-background-radius: 12;");
        Label titleLabelCard = new Label(entry.getTitle());
        titleLabelCard.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");

        VBox specificInfo = new VBox(3);
        if (entry.getType().contains("Libro") && entry.getChapters() != null) {
            specificInfo.getChildren().add(createDetailLabel("Capitulo leido: " + entry.getChapters()));
        } else if (entry.getType().contains("Serie") && entry.getSeason() != null && entry.getEpisode() != null) {
            specificInfo.getChildren().add(createDetailLabel("Temporada " + entry.getSeason() + ", Capitulo " + entry.getEpisode()));
        } else if (entry.getType().contains("Teatro") && entry.getVenue() != null) {
            specificInfo.getChildren().add(createDetailLabel("Lugar: " + entry.getVenue()));
        } else if (entry.getType().contains("Comic")) {
            if (entry.getIsSingleVolume() != null && entry.getIsSingleVolume()) {
                specificInfo.getChildren().add(createDetailLabel("Tomo unico"));
            } else {
                if (entry.getComicSeriesNumber() != null) {
                    specificInfo.getChildren().add(createDetailLabel("Numero de serie: " + entry.getComicSeriesNumber()));
                }
                if (entry.getComicIssueNumber() != null) {
                    specificInfo.getChildren().add(createDetailLabel("Numero leido: " + entry.getComicIssueNumber()));
                }
            }
        }
        specificInfo.getChildren().add(createDetailLabel("Fecha: " + entry.getDate()));
        info.getChildren().addAll(typeLabel, titleLabelCard, specificInfo);
        HBox.setHgrow(info, Priority.ALWAYS);
        header.getChildren().add(info);

        VBox actionButtons = new VBox(8);
        actionButtons.setAlignment(Pos.TOP_RIGHT);
        Button editBtn = new Button("Editar");
        editBtn.setStyle(createSmallButtonStyle(PRIMARY_COLOR));
        editBtn.setOnAction(e -> openEditDialog(entry));
        Button deleteBtn = new Button("Eliminar");
        deleteBtn.setStyle(createSmallButtonStyle(ACCENT_COLOR));
        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmar");
            confirm.setHeaderText(null);
            confirm.setContentText("Seguro que deseas eliminar?");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                db.deleteEntry(entry.getId());
                loadEntries();
            }
        });
        actionButtons.getChildren().addAll(editBtn, deleteBtn);
        header.getChildren().add(actionButtons);
        card.getChildren().add(header);

        if (entry.getDescription() != null && !entry.getDescription().isEmpty()) {
            TextArea descLabel = new TextArea(entry.getDescription());
            descLabel.setWrapText(true);
            descLabel.setPrefRowCount(2);
            descLabel.setEditable(false);
            descLabel.setStyle("-fx-control-inner-background: #FAFBFC; -fx-font-size: 12; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 6; -fx-background-radius: 6;");
            card.getChildren().add(descLabel);
        }
        return card;
    }

    private Label createDetailLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 12;");
        return label;
    }

    private StackPane createPlaceholderCover() {
        StackPane placeholder = new StackPane();
        placeholder.setPrefSize(80, 120);
        placeholder.setStyle("-fx-background-color: #F0F2F5; -fx-background-radius: 8; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 8;");
        Label icon = new Label("?");
        icon.setStyle("-fx-font-size: 24; -fx-text-fill: #CCC;");
        placeholder.getChildren().add(icon);
        return placeholder;
    }

    private String getTypeColor(String type) {
        if (type.contains("Libro")) return "#4A90D9";
        if (type.contains("Serie")) return "#E67E22";
        if (type.contains("Pelicula")) return "#E74C3C";
        if (type.contains("Teatro")) return "#9B59B6";
        if (type.contains("Comic")) return "#2ECC71";
        return PRIMARY_COLOR;
    }

    private void openEditDialog(Entry entry) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar Registro");
        dialog.setHeaderText("Modifica los datos del registro");

        VBox editForm = new VBox(10);
        editForm.setPadding(new Insets(20));
        editForm.setPrefWidth(450);

        Label tl = createFieldLabel("Titulo:");
        TextField editTitleField = new TextField(entry.getTitle());
        editTitleField.setStyle(createInputStyle());

        Label tyl = createFieldLabel("Tipo:");
        ComboBox<String> editTypeCombo = new ComboBox<>();
        editTypeCombo.setItems(FXCollections.observableArrayList("Libro", "Serie", "Pelicula", "Teatro", "Comic"));
        editTypeCombo.setValue(entry.getType());
        editTypeCombo.setPrefWidth(Double.MAX_VALUE);
        editTypeCombo.setStyle(createInputStyle());

        Label dl = createFieldLabel("Descripcion:");
        TextArea editDescArea = new TextArea(entry.getDescription() != null ? entry.getDescription() : "");
        editDescArea.setWrapText(true);
        editDescArea.setPrefRowCount(4);
        editDescArea.setStyle(createInputStyle());

        Label dal = createFieldLabel("Fecha:");
        DatePicker editDatePicker = new DatePicker(entry.getDate());
        editDatePicker.setStyle(createInputStyle());

        VBox dynamicEditBox = new VBox(8);
        TextField editChapterField = new TextField();
        TextField editSeasonField = new TextField();
        TextField editEpisodeField = new TextField();
        TextField editVenueField = new TextField();
        CheckBox editSingleVolumeCheckBox = new CheckBox("Es tomo unico?");
        TextField editComicSeriesField = new TextField();
        TextField editComicIssueField = new TextField();

        updateEditDynamicFields(entry, editTypeCombo, dynamicEditBox, editChapterField, editSeasonField, editEpisodeField, editVenueField, editSingleVolumeCheckBox, editComicSeriesField, editComicIssueField);
        editTypeCombo.setOnAction(e -> updateEditDynamicFields(entry, editTypeCombo, dynamicEditBox, editChapterField, editSeasonField, editEpisodeField, editVenueField, editSingleVolumeCheckBox, editComicSeriesField, editComicIssueField));

        editForm.getChildren().addAll(tl, editTitleField, tyl, editTypeCombo, dl, editDescArea, dal, editDatePicker, dynamicEditBox);
        dialog.getDialogPane().setContent(editForm);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            entry.setTitle(editTitleField.getText());
            entry.setType(editTypeCombo.getValue());
            entry.setDescription(editDescArea.getText());
            entry.setDate(editDatePicker.getValue());
            String type = editTypeCombo.getValue();
            if (type.contains("Libro")) {
                try {
                    entry.setChapters(Integer.parseInt(editChapterField.getText()));
                } catch (NumberFormatException ex) {
                    showAlert("Error", "Capitulo invalido");
                    return;
                }
            } else if (type.contains("Serie")) {
                try {
                    entry.setSeason(Integer.parseInt(editSeasonField.getText()));
                    entry.setEpisode(Integer.parseInt(editEpisodeField.getText()));
                } catch (NumberFormatException ex) {
                    showAlert("Error", "Temporada/capitulo invalido");
                    return;
                }
            } else if (type.contains("Teatro")) {
                entry.setVenue(editVenueField.getText());
            } else if (type.contains("Comic")) {
                entry.setIsSingleVolume(editSingleVolumeCheckBox.isSelected());
                if (!editSingleVolumeCheckBox.isSelected()) {
                    try {
                        entry.setComicSeriesNumber(Integer.parseInt(editComicSeriesField.getText()));
                    } catch (NumberFormatException ex) {
                        showAlert("Error", "Numero de serie invalido");
                        return;
                    }
                    if (!editComicIssueField.getText().trim().isEmpty()) {
                        try {
                            entry.setComicIssueNumber(Integer.parseInt(editComicIssueField.getText()));
                        } catch (NumberFormatException ex) {
                            showAlert("Error", "Numero leido invalido");
                            return;
                        }
                    }
                }
            }
            db.updateEntry(entry);
            loadEntries();
            showAlert("Exito", "Registro actualizado");
        }
    }

    private void updateEditDynamicFields(Entry entry, ComboBox<String> editTypeCombo, VBox dynamicEditBox, TextField editChapterField, TextField editSeasonField, TextField editEpisodeField, TextField editVenueField, CheckBox editSingleVolumeCheckBox, TextField editComicSeriesField, TextField editComicIssueField) {
        dynamicEditBox.getChildren().clear();
        String type = editTypeCombo.getValue();
        if (type == null) return;
        if (type.contains("Libro")) {
            VBox b = new VBox(5);
            editChapterField.setStyle(createInputStyle());
            if (entry.getChapters() != null) editChapterField.setText(entry.getChapters().toString());
            b.getChildren().addAll(createFieldLabel("Capitulo leido:"), editChapterField);
            dynamicEditBox.getChildren().add(b);
        } else if (type.contains("Serie")) {
            VBox b = new VBox(5);
            editSeasonField.setStyle(createInputStyle());
            editEpisodeField.setStyle(createInputStyle());
            if (entry.getSeason() != null) editSeasonField.setText(entry.getSeason().toString());
            if (entry.getEpisode() != null) editEpisodeField.setText(entry.getEpisode().toString());
            b.getChildren().addAll(createFieldLabel("Temporada:"), editSeasonField, createFieldLabel("Capitulo:"), editEpisodeField);
            dynamicEditBox.getChildren().add(b);
        } else if (type.contains("Teatro")) {
            VBox b = new VBox(5);
            editVenueField.setStyle(createInputStyle());
            if (entry.getVenue() != null) editVenueField.setText(entry.getVenue());
            b.getChildren().addAll(createFieldLabel("Lugar:"), editVenueField);
            dynamicEditBox.getChildren().add(b);
        } else if (type.contains("Comic")) {
            VBox comicBox = new VBox(8);
            if (entry.getIsSingleVolume() != null) editSingleVolumeCheckBox.setSelected(entry.getIsSingleVolume());
            editSingleVolumeCheckBox.setOnAction(e -> {
                boolean show = !editSingleVolumeCheckBox.isSelected();
                editComicSeriesField.setVisible(show);
                editComicSeriesField.setManaged(show);
                editComicIssueField.setVisible(show);
                editComicIssueField.setManaged(show);
            });
            editComicSeriesField.setStyle(createInputStyle());
            editComicIssueField.setStyle(createInputStyle());
            if (entry.getComicSeriesNumber() != null && !editSingleVolumeCheckBox.isSelected()) {
                editComicSeriesField.setText(entry.getComicSeriesNumber().toString());
            }
            if (entry.getComicIssueNumber() != null && !editSingleVolumeCheckBox.isSelected()) {
                editComicIssueField.setText(entry.getComicIssueNumber().toString());
            }
            boolean show = !editSingleVolumeCheckBox.isSelected();
            editComicSeriesField.setVisible(show);
            editComicSeriesField.setManaged(show);
            editComicIssueField.setVisible(show);
            editComicIssueField.setManaged(show);
            comicBox.getChildren().addAll(editSingleVolumeCheckBox, createFieldLabel("Numero de serie (tomo):"), editComicSeriesField, createFieldLabel("Numero leido:"), editComicIssueField);
            dynamicEditBox.getChildren().add(comicBox);
        }
    }

    private void clearForm() {
        titleField.setValue(null);
        titleTextField.clear();
        descriptionArea.clear();
        typeCombo.setValue("Libro");
        datePicker.setValue(LocalDate.now());
        coverPreview.setImage(null);
        selectedCoverPath = null;
        if (chapterTextField != null) chapterTextField.clear();
        if (seasonTextField != null) seasonTextField.clear();
        if (episodeTextField != null) episodeTextField.clear();
        if (venueTextField != null) venueTextField.clear();
        if (isSingleVolumeCheckBox != null) isSingleVolumeCheckBox.setSelected(false);
        if (comicSeriesNumberTextField != null) comicSeriesNumberTextField.clear();
        if (comicIssueNumberTextField != null) comicIssueNumberTextField.clear();
        updateDynamicFields();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + "; -fx-font-size: 12;");
        return label;
    }

    private String createInputStyle() {
        return "-fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6; -fx-font-size: 13;";
    }

    private String createPrimaryButtonStyle() {
        return "-fx-font-size: 13; -fx-padding: 10 28; -fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold;";
    }

    private String createSecondaryButtonStyle() {
        return "-fx-font-size: 12; -fx-padding: 8 20; -fx-background-color: " + CARD_BG + "; -fx-text-fill: " + TEXT_PRIMARY + "; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;";
    }

    private String createSmallButtonStyle(String color) {
        return "-fx-font-size: 11; -fx-padding: 5 14; -fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;";
    }

    public static void main(String[] args) {
        launch(args);
    }
}
