package com.readingtracker;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.readingtracker.models.Database;
import com.readingtracker.models.Entry;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.List;
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
    
    // Campos dinámicos
    private VBox dynamicFieldsBox;
    private TextField chapterTextField;
    private TextField seasonTextField;
    private TextField episodeTextField;
    private TextField venueTextField;
    private CheckBox isSingleVolumeCheckBox;
    private TextField comicNumberTextField;
    private TextField searchTitleField;
    private DatePicker searchDatePicker;
    private ComboBox<String> searchTypeCombo;

    @Override
    public void start(Stage stage) throws Exception {
        db = new Database();
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-font-family: 'Segoe UI';");
        
        // Crear TabPane con dos pestañas
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Tab 1: Registrar
        Tab registerTab = new Tab("📝 Registrar", createInputPanel());
        registerTab.setStyle("-fx-font-size: 12;");
        
        // Tab 2: Buscar
        Tab searchTab = new Tab("🔍 Buscar", createSearchPanel());
        searchTab.setStyle("-fx-font-size: 12;");
        
        tabPane.getTabs().addAll(registerTab, searchTab);
        root.setTop(tabPane);
        
        // Actualizar sugerencias iniciales
        updateTitleSuggestions();
        updateDynamicFields();
        
        // Panel central - Lista de registros
        entriesContainer = new VBox(10);
        entriesContainer.setPadding(new Insets(10));
        entriesContainer.setStyle("-fx-background-color: #f5f5f5;");
        
        ScrollPane scrollPane = new ScrollPane(entriesContainer);
        scrollPane.setFitToWidth(true);
        root.setCenter(scrollPane);
        
        loadEntries();
        
        Scene scene = new Scene(root, 1100, 800);
        stage.setTitle("📚 Registro de Lectura y Visualización");
        stage.setScene(scene);
        stage.show();
    }

    private VBox createInputPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
        
        // Título
        Label titleLabel = new Label("📚 Registrar nueva entrada");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        panel.getChildren().add(titleLabel);
        
        // Fila 1: Título y Tipo
        HBox row1 = new HBox(15);
        row1.setPadding(new Insets(0));
        
        VBox titleBox = new VBox(5);
        Label label1 = new Label("Título:");
        label1.setStyle("-fx-font-weight: bold;");
        titleField = new ComboBox<>();
        titleField.setEditable(true);
        titleField.setPrefWidth(300);
        titleTextField = titleField.getEditor();
        titleTextField.setPromptText("Ej: El Quijote");
        // Actualizar sugerencias cuando cambia el tipo o el título
        titleTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty() && typeCombo.getValue() != null) {
                updateDynamicFields();
            }
        });
        titleBox.getChildren().addAll(label1, titleField);
        HBox.setHgrow(titleBox, javafx.scene.layout.Priority.ALWAYS);
        
        VBox typeBox = new VBox(5);
        Label label2 = new Label("Tipo:");
        label2.setStyle("-fx-font-weight: bold;");
        typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("📚 Libro", "🎬 Serie", "🎥 Película", "🎭 Teatro", "💭 Cómic");
        typeCombo.setValue("📚 Libro");  // Por defecto seleccionar Libro
        typeCombo.setPrefWidth(150);
        typeCombo.setOnAction(e -> {
            updateTitleSuggestions();
            updateDynamicFields();
        });
        typeBox.getChildren().addAll(label2, typeCombo);
        
        VBox dateBox = new VBox(5);
        Label label3 = new Label("Fecha:");
        label3.setStyle("-fx-font-weight: bold;");
        datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now());
        datePicker.setPrefWidth(150);
        dateBox.getChildren().addAll(label3, datePicker);
        
        row1.getChildren().addAll(titleBox, typeBox, dateBox);
        panel.getChildren().add(row1);
        
        // Campos dinámicos para datos específicos de cada tipo
        createDynamicFields();
        panel.getChildren().add(dynamicFieldsBox);
        
        // Fila 2: Descripción
        VBox descBox = new VBox(5);
        Label label4 = new Label("Descripción/Notas:");
        label4.setStyle("-fx-font-weight: bold;");
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("¿Qué te pareció? ¿Cuántas páginas leíste? ¿Qué episodio ves?");
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setStyle("-fx-control-inner-background: #fafafa;");
        descBox.getChildren().addAll(label4, descriptionArea);
        VBox.setVgrow(descBox, javafx.scene.layout.Priority.SOMETIMES);
        panel.getChildren().add(descBox);
        
        // Fila 3: Portada y Botón
        HBox row3 = new HBox(15);
        
        VBox coverBox = new VBox(5);
        Label label5 = new Label("Portada (arrastra imagen aquí):");
        label5.setStyle("-fx-font-weight: bold;");
        coverPreview = new ImageView();
        coverPreview.setFitWidth(100);
        coverPreview.setFitHeight(140);
        coverPreview.setStyle("-fx-border-color: #cccccc; -fx-border-width: 2; -fx-border-style: dashed;");
        setupDragAndDrop();
        coverBox.getChildren().addAll(label5, coverPreview);
        
        Button addButton = new Button("✅ Guardar Entrada");
        addButton.setStyle("-fx-font-size: 14; -fx-padding: 10 30; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        addButton.setOnAction(e -> onAddEntry());
        
        Button clearButton = new Button("🔄 Limpiar");
        clearButton.setStyle("-fx-font-size: 12; -fx-padding: 10 20;");
        clearButton.setOnAction(e -> clearForm());
        
        VBox buttonBox = new VBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
        buttonBox.getChildren().addAll(addButton, clearButton);
        
        row3.getChildren().addAll(coverBox, buttonBox);
        HBox.setHgrow(buttonBox, javafx.scene.layout.Priority.ALWAYS);
        panel.getChildren().add(row3);
        
        return panel;
    }
    
    private VBox createSearchPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
        
        // Título
        Label titleLabel = new Label("🔍 Buscar registros");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        panel.getChildren().add(titleLabel);
        
        // Fila 1: Búsqueda por título
        HBox row1 = new HBox(15);
        
        VBox titleSearchBox = new VBox(5);
        Label label1 = new Label("Buscar por nombre:");
        label1.setStyle("-fx-font-weight: bold;");
        searchTitleField = new TextField();
        searchTitleField.setPromptText("Ej: Breaking Bad, Harry Potter");
        searchTitleField.setPrefWidth(300);
        titleSearchBox.getChildren().addAll(label1, searchTitleField);
        HBox.setHgrow(titleSearchBox, javafx.scene.layout.Priority.ALWAYS);
        
        Button searchTitleButton = new Button("🔎 Buscar");
        searchTitleButton.setStyle("-fx-font-size: 12; -fx-padding: 8 20;");
        searchTitleButton.setOnAction(e -> onSearchByTitle());
        
        row1.getChildren().addAll(titleSearchBox, searchTitleButton);
        panel.getChildren().add(row1);
        
        // Fila 2: Búsqueda por fecha y tipo
        HBox row2 = new HBox(15);
        
        VBox dateBox = new VBox(5);
        Label label2 = new Label("Buscar por fecha:");
        label2.setStyle("-fx-font-weight: bold;");
        searchDatePicker = new DatePicker();
        searchDatePicker.setPrefWidth(200);
        dateBox.getChildren().addAll(label2, searchDatePicker);
        
        VBox typeBox = new VBox(5);
        Label label3 = new Label("Filtrar por tipo:");
        label3.setStyle("-fx-font-weight: bold;");
        searchTypeCombo = new ComboBox<>();
        searchTypeCombo.getItems().addAll("Todo", "📚 Libro", "🎬 Serie", "🎥 Película", "🎭 Teatro", "💭 Cómic");
        searchTypeCombo.setValue("Todo");
        searchTypeCombo.setPrefWidth(150);
        typeBox.getChildren().addAll(label3, searchTypeCombo);
        
        Button searchDateButton = new Button("📅 Buscar por fecha");
        searchDateButton.setStyle("-fx-font-size: 12; -fx-padding: 8 20;");
        searchDateButton.setOnAction(e -> onSearchByDate());
        
        Button showAllButton = new Button("📋 Ver todos");
        showAllButton.setStyle("-fx-font-size: 12; -fx-padding: 8 20;");
        showAllButton.setOnAction(e -> loadEntries());
        
        row2.getChildren().addAll(dateBox, typeBox, searchDateButton, showAllButton);
        panel.getChildren().add(row2);
        
        // Información
        Label infoLabel = new Label("💡 Usa el campo de nombre para búsquedas parciales (insensible a mayúsculas)");
        infoLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11; -fx-font-style: italic;");
        panel.getChildren().add(infoLabel);
        
        return panel;
    }
    
    private void createDynamicFields() {
        dynamicFieldsBox = new VBox(10);
        dynamicFieldsBox.setPadding(new Insets(10));
        dynamicFieldsBox.setStyle("-fx-border-color: #f0f0f0; -fx-border-width: 1; -fx-padding: 10; -fx-border-radius: 3;");
        
        // Inicialmente estará vacío, se poblará en updateDynamicFields
        Label placeholder = new Label("Selecciona un tipo para ver opciones adicionales");
        placeholder.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
        dynamicFieldsBox.getChildren().add(placeholder);
    }
    
    private void updateDynamicFields() {
        String selectedType = typeCombo.getValue();
        dynamicFieldsBox.getChildren().clear();
        
        if (selectedType == null) {
            Label placeholder = new Label("Selecciona un tipo para ver opciones adicionales");
            placeholder.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
            placeholder.setId("placeholder");
            dynamicFieldsBox.getChildren().add(placeholder);
            return;
        }
        
        if (selectedType.contains("Libro")) {
            VBox bookBox = new VBox(5);
            Label bookLabel = new Label("📖 Capítulo leído:");
            bookLabel.setStyle("-fx-font-weight: bold;");
            chapterTextField = new TextField();
            chapterTextField.setPromptText("Ej: 5 (ingresa el capítulo específico)");
            chapterTextField.setPrefWidth(200);
            
            // Sugerir siguiente capítulo si el título ya existe
            String titleText = getTitleFieldValue();
            String lastChapter = suggestNextChapter(titleText, selectedType);
            if (lastChapter != null) {
                chapterTextField.setStyle("-fx-text-fill: #4CAF50;");
                chapterTextField.setPromptText("Sugerencia: " + lastChapter + " (siguiente al último visto)");
            }
            
            bookBox.getChildren().addAll(bookLabel, chapterTextField);
            dynamicFieldsBox.getChildren().add(bookBox);
        } else if (selectedType.contains("Serie")) {
            HBox seriesBox = new HBox(15);
            
            VBox seasonBox = new VBox(5);
            Label seasonLabel = new Label("🎬 Temporada:");
            seasonLabel.setStyle("-fx-font-weight: bold;");
            seasonTextField = new TextField();
            seasonTextField.setPromptText("Ej: 2");
            seasonTextField.setPrefWidth(100);
            seasonBox.getChildren().addAll(seasonLabel, seasonTextField);
            
            VBox episodeBox = new VBox(5);
            Label episodeLabel = new Label("🎞️ Capítulo:");
            episodeLabel.setStyle("-fx-font-weight: bold;");
            episodeTextField = new TextField();
            episodeTextField.setPromptText("Ej: 8");
            episodeTextField.setPrefWidth(100);
            
            // Sugerir siguiente capítulo
            String titleText = getTitleFieldValue();
            Database.SeriesInfo seriesInfo = suggestNextSeriesInfo(titleText);
            if (seriesInfo != null) {
                seasonTextField.setText(String.valueOf(seriesInfo.season));
                episodeTextField.setText(String.valueOf(seriesInfo.episode));
                seasonTextField.setStyle("-fx-text-fill: #4CAF50;");
                episodeTextField.setStyle("-fx-text-fill: #4CAF50;");
            }
            
            episodeBox.getChildren().addAll(episodeLabel, episodeTextField);
            seriesBox.getChildren().addAll(seasonBox, episodeBox);
            dynamicFieldsBox.getChildren().add(seriesBox);
        } else if (selectedType.contains("Película")) {
            Label movieLabel = new Label("🎥 Solo se requiere el título de la película");
            movieLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-style: italic;");
            dynamicFieldsBox.getChildren().add(movieLabel);
        } else if (selectedType.contains("Teatro")) {
            VBox theaterBox = new VBox(5);
            Label venueLabel = new Label("🎤 Lugar donde se vio:");
            venueLabel.setStyle("-fx-font-weight: bold;");
            venueTextField = new TextField();
            venueTextField.setPromptText("Ej: Teatro Nacional");
            venueTextField.setPrefWidth(300);
            theaterBox.getChildren().addAll(venueLabel, venueTextField);
            dynamicFieldsBox.getChildren().add(theaterBox);
        } else if (selectedType.contains("Cómic")) {
            VBox comicBox = new VBox(10);
            
            // Checkbox para tomo único
            HBox checkboxRow = new HBox(10);
            isSingleVolumeCheckBox = new CheckBox("¿Es tomo único?");
            isSingleVolumeCheckBox.setStyle("-fx-font-weight: bold;");
            isSingleVolumeCheckBox.setSelected(false);
            isSingleVolumeCheckBox.setOnAction(e -> updateComicFields());
            checkboxRow.getChildren().add(isSingleVolumeCheckBox);
            comicBox.getChildren().add(checkboxRow);
            
            // Campo para número de serie (visible solo si no es tomo único)
            VBox seriesNumberBox = new VBox(5);
            Label seriesLabel = new Label("📚 Número de la serie:");
            seriesLabel.setStyle("-fx-font-weight: bold;");
            comicNumberTextField = new TextField();
            comicNumberTextField.setPromptText("Ej: 5");
            comicNumberTextField.setPrefWidth(200);
            seriesNumberBox.getChildren().addAll(seriesLabel, comicNumberTextField);
            seriesNumberBox.setId("comicSeriesBox");
            comicBox.getChildren().add(seriesNumberBox);
            
            dynamicFieldsBox.getChildren().add(comicBox);
        }
    }
    
    private void updateComicFields() {
        if (isSingleVolumeCheckBox == null) return;
        
        // Encontrar el VBox con id "comicSeriesBox"
        for (javafx.scene.Node node : dynamicFieldsBox.getChildren()) {
            if (node instanceof VBox) {
                VBox vbox = (VBox) node;
                for (javafx.scene.Node child : vbox.getChildren()) {
                    if (child instanceof VBox && "comicSeriesBox".equals(child.getId())) {
                        child.setVisible(!isSingleVolumeCheckBox.isSelected());
                        child.setManaged(!isSingleVolumeCheckBox.isSelected());
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
        if (seriesInfo == null) {
            return new Database.SeriesInfo(1, 1);
        }
        
        // Incrementar episodio, si llega a 100 pasa a siguiente temporada
        int nextEpisode = seriesInfo.episode + 1;
        int nextSeason = seriesInfo.season;
        
        if (nextEpisode > 16) {  // Suponiendo máximo 16 episodios por temporada
            nextEpisode = 1;
            nextSeason++;
        }
        
        return new Database.SeriesInfo(nextSeason, nextEpisode);
    }
    
    private String suggestNextChapter(String title, String type) {
        if (title.isEmpty()) return null;
        
        Integer lastChapter = db.getLastChapterForTitle(title, type);
        if (lastChapter == null) return null;
        
        if (type.contains("Libro")) {
            return String.valueOf(lastChapter + 1);
        } else if (type.contains("Serie")) {
            // Para series, devuelve "Temporada X, Capítulo Y"
            return "Temporada 1, Capítulo " + (lastChapter + 1);
        }
        
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
                    showAlert("Éxito", "Portada cargada correctamente");
                } catch (Exception e) {
                    showAlert("Error", "No se pudo cargar la imagen");
                }
            }
            event.consume();
        });
    }

    private void onAddEntry() {
        if (getTitleFieldValue().isEmpty() || typeCombo.getValue() == null) {
            showAlert("Error", "Por favor completa título y tipo");
            return;
        }

        String type = typeCombo.getValue();
        Integer chapters = null;
        Integer season = null;
        Integer episode = null;
        String venue = null;
        Boolean isSingleVolume = null;
        Integer comicNumber = null;

        // Validar campos específicos según tipo
        if (type.contains("Libro")) {
            if (chapterTextField.getText().trim().isEmpty()) {
                showAlert("Error", "Por favor ingresa el número de capítulo");
                return;
            }
            try {
                chapters = Integer.parseInt(chapterTextField.getText().trim());
                if (chapters <= 0) {
                    showAlert("Error", "El capítulo debe ser mayor a 0");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Error", "El capítulo debe ser un número válido");
                return;
            }
        } else if (type.contains("Serie")) {
            if (seasonTextField.getText().trim().isEmpty()) {
                showAlert("Error", "Por favor ingresa la temporada");
                return;
            }
            if (episodeTextField.getText().trim().isEmpty()) {
                showAlert("Error", "Por favor ingresa el capítulo");
                return;
            }
            try {
                season = Integer.parseInt(seasonTextField.getText().trim());
                episode = Integer.parseInt(episodeTextField.getText().trim());
                if (season <= 0 || episode <= 0) {
                    showAlert("Error", "Temporada y capítulo deben ser mayores a 0");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Error", "Temporada y capítulo deben ser números válidos");
                return;
            }
        } else if (type.contains("Teatro")) {
            if (venueTextField.getText().trim().isEmpty()) {
                showAlert("Error", "Por favor ingresa el lugar donde se vio");
                return;
            }
            venue = venueTextField.getText();
        } else if (type.contains("Cómic")) {
            isSingleVolume = isSingleVolumeCheckBox.isSelected();
            if (!isSingleVolume) {
                if (comicNumberTextField.getText().trim().isEmpty()) {
                    showAlert("Error", "Por favor ingresa el número de la serie");
                    return;
                }
                try {
                    comicNumber = Integer.parseInt(comicNumberTextField.getText().trim());
                    if (comicNumber <= 0) {
                        showAlert("Error", "El número de serie debe ser mayor a 0");
                        return;
                    }
                } catch (NumberFormatException e) {
                    showAlert("Error", "El número de serie debe ser un número válido");
                    return;
                }
            }
        }

        Entry entry = new Entry(
                getTitleFieldValue(),
                type,
                descriptionArea.getText(),
                datePicker.getValue(),
                selectedCoverPath,
                chapters,
                season,
                episode
        );
        entry.setVenue(venue);
        entry.setIsSingleVolume(isSingleVolume);
        entry.setComicNumber(comicNumber);

        db.addEntry(entry);
        clearForm();
        loadEntries();
        showAlert("Éxito", "Entrada registrada correctamente ✨");
    }
    
    private void onSearchByTitle() {
        String searchTerm = searchTitleField.getText().trim();
        if (searchTerm.isEmpty()) {
            showAlert("Información", "Por favor ingresa un término de búsqueda");
            return;
        }
        
        String selectedType = searchTypeCombo.getValue();
        List<Entry> results;
        
        if ("Todo".equals(selectedType)) {
            results = db.searchByTitle(searchTerm);
        } else {
            results = db.searchByTitleAndType(searchTerm, selectedType);
        }
        
        displaySearchResults(results, "búsqueda: '" + searchTerm + "'");
    }
    
    private void onSearchByDate() {
        LocalDate selectedDate = searchDatePicker.getValue();
        if (selectedDate == null) {
            showAlert("Información", "Por favor selecciona una fecha");
            return;
        }
        
        List<Entry> results = db.searchByDate(selectedDate);
        displaySearchResults(results, "del " + selectedDate);
    }
    
    private void displaySearchResults(List<Entry> entries, String searchDescription) {
        entriesContainer.getChildren().clear();
        
        if (entries.isEmpty()) {
            Label emptyLabel = new Label("📭 No se encontraron registros para " + searchDescription);
            emptyLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #999;");
            entriesContainer.getChildren().add(emptyLabel);
            return;
        }
        
        Label resultLabel = new Label("📊 Se encontraron " + entries.size() + " registro(s) para " + searchDescription);
        resultLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
        entriesContainer.getChildren().add(resultLabel);
        
        for (Entry entry : entries) {
            VBox entryCard = createEntryCard(entry);
            entriesContainer.getChildren().add(entryCard);
        }
    }

    private void loadEntries() {
        entriesContainer.getChildren().clear();
        List<Entry> entries = db.getAllEntries();

        if (entries.isEmpty()) {
            Label emptyLabel = new Label("📭 No hay registros. ¡Comienza a registrar tu lectura!");
            emptyLabel.setStyle("-fx-font-size: 16; -fx-text-fill: #999;");
            entriesContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Entry entry : entries) {
            VBox entryCard = createEntryCard(entry);
            entriesContainer.getChildren().add(entryCard);
        }
    }

    private VBox createEntryCard(Entry entry) {
        VBox card = new VBox(10);
        card.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 15; -fx-border-radius: 5; -fx-background-color: white;");
        card.setPadding(new Insets(15));

        HBox header = new HBox(15);
        header.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        
        // Portada
        if (entry.getCoverPath() != null) {
            try {
                ImageView coverImg = new ImageView(new Image(new FileInputStream(entry.getCoverPath()), 80, 120, true, true));
                header.getChildren().add(coverImg);
            } catch (Exception e) {
                Label placeholder = new Label("📷");
                placeholder.setStyle("-fx-font-size: 40;");
                header.getChildren().add(placeholder);
            }
        } else {
            Label placeholder = new Label("📷");
            placeholder.setStyle("-fx-font-size: 40; -fx-text-fill: #ccc;");
            header.getChildren().add(placeholder);
        }

        // Información
        VBox info = new VBox(8);
        Label typeLabel = new Label(entry.getType());
        typeLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
        Label titleLabel = new Label(entry.getTitle());
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        
        // Información específica según tipo
        VBox specificInfo = new VBox(3);
        if (entry.getType().contains("Libro") && entry.getChapters() != null) {
            Label chaptersLabel = new Label("📖 Capítulo leído: " + entry.getChapters());
            chaptersLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");
            specificInfo.getChildren().add(chaptersLabel);
        } else if (entry.getType().contains("Serie") && entry.getSeason() != null && entry.getEpisode() != null) {
            Label seasonEpisodeLabel = new Label("🎬 Temporada " + entry.getSeason() + ", Capítulo " + entry.getEpisode());
            seasonEpisodeLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");
            specificInfo.getChildren().add(seasonEpisodeLabel);
        } else if (entry.getType().contains("Teatro") && entry.getVenue() != null) {
            Label venueLabel = new Label("🎭 Lugar: " + entry.getVenue());
            venueLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");
            specificInfo.getChildren().add(venueLabel);
        } else if (entry.getType().contains("Cómic")) {
            if (entry.getIsSingleVolume() != null && entry.getIsSingleVolume()) {
                Label singleVolumeLabel = new Label("📕 Tomo único");
                singleVolumeLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");
                specificInfo.getChildren().add(singleVolumeLabel);
            } else if (entry.getComicNumber() != null) {
                Label comicNumberLabel = new Label("📕 Número: " + entry.getComicNumber());
                comicNumberLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");
                specificInfo.getChildren().add(comicNumberLabel);
            }
        }
        
        Label dateLabel = new Label("📅 " + entry.getDate());
        dateLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");
        specificInfo.getChildren().add(dateLabel);

        info.getChildren().addAll(typeLabel, titleLabel, specificInfo);
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().add(info);

        // Botones de acción
        VBox actionButtons = new VBox(5);
        
        Button editBtn = new Button("✏️");
        editBtn.setStyle("-fx-padding: 5 10; -fx-font-size: 12;");
        editBtn.setOnAction(e -> openEditDialog(entry));
        
        Button deleteBtn = new Button("🗑️");
        deleteBtn.setStyle("-fx-padding: 5 10; -fx-font-size: 12;");
        deleteBtn.setOnAction(e -> {
            db.deleteEntry(entry.getId());
            loadEntries();
        });
        
        actionButtons.getChildren().addAll(editBtn, deleteBtn);
        header.getChildren().add(actionButtons);

        card.getChildren().add(header);

        // Descripción
        if (entry.getDescription() != null && !entry.getDescription().isEmpty()) {
            TextArea descLabel = new TextArea(entry.getDescription());
            descLabel.setWrapText(true);
            descLabel.setPrefRowCount(2);
            descLabel.setEditable(false);
            descLabel.setStyle("-fx-control-inner-background: #fafafa; -fx-font-size: 11;");
            card.getChildren().add(descLabel);
        }

        return card;
    }

    private void openEditDialog(Entry entry) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("✏️ Editar Registro");
        dialog.setHeaderText("Modifica los datos del registro");

        // Crear forma de edición similar a la de registro
        VBox editForm = new VBox(10);
        editForm.setPadding(new Insets(20));

        // Título
        Label titleLabel = new Label("Título:");
        TextField editTitleField = new TextField(entry.getTitle());
        
        // Tipo
        Label typeLabel = new Label("Tipo:");
        ComboBox<String> editTypeCombo = new ComboBox<>();
        editTypeCombo.setItems(FXCollections.observableArrayList(
                "📚 Libro", "🎬 Serie", "🎥 Película", "🎭 Teatro", "📕 Cómic"
        ));
        editTypeCombo.setValue(entry.getType());
        editTypeCombo.setPrefWidth(Double.MAX_VALUE);

        // Descripción
        Label descLabel = new Label("Descripción:");
        TextArea editDescArea = new TextArea(entry.getDescription() != null ? entry.getDescription() : "");
        editDescArea.setWrapText(true);
        editDescArea.setPrefRowCount(4);

        // Fecha
        Label dateLabel = new Label("Fecha:");
        DatePicker editDatePicker = new DatePicker(entry.getDate());

        // Campos dinámicos según tipo
        VBox dynamicEditBox = new VBox(5);
        TextField editChapterField = new TextField();
        TextField editSeasonField = new TextField();
        TextField editEpisodeField = new TextField();
        TextField editVenueField = new TextField();
        CheckBox editSingleVolumeCheckBox = new CheckBox("¿Es tomo único?");
        TextField editComicNumberField = new TextField();

        // Configurar campos según tipo actual
        updateEditDynamicFields(entry, editTypeCombo, dynamicEditBox, editChapterField, 
                                editSeasonField, editEpisodeField, editVenueField, 
                                editSingleVolumeCheckBox, editComicNumberField);

        editTypeCombo.setOnAction(e -> updateEditDynamicFields(entry, editTypeCombo, dynamicEditBox, 
                                editChapterField, editSeasonField, editEpisodeField, editVenueField, 
                                editSingleVolumeCheckBox, editComicNumberField));

        editForm.getChildren().addAll(
                titleLabel, editTitleField,
                typeLabel, editTypeCombo,
                descLabel, editDescArea,
                dateLabel, editDatePicker,
                dynamicEditBox
        );

        dialog.getDialogPane().setContent(editForm);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Actualizar la entrada
            entry.setTitle(editTitleField.getText());
            entry.setType(editTypeCombo.getValue());
            entry.setDescription(editDescArea.getText());
            entry.setDate(editDatePicker.getValue());

            String type = editTypeCombo.getValue();
            if (type.contains("Libro")) {
                try {
                    entry.setChapters(Integer.parseInt(editChapterField.getText()));
                } catch (NumberFormatException ex) {
                    showAlert("Error", "Capítulo debe ser un número válido");
                    return;
                }
            } else if (type.contains("Serie")) {
                try {
                    entry.setSeason(Integer.parseInt(editSeasonField.getText()));
                    entry.setEpisode(Integer.parseInt(editEpisodeField.getText()));
                } catch (NumberFormatException ex) {
                    showAlert("Error", "Temporada y capítulo deben ser números válidos");
                    return;
                }
            } else if (type.contains("Teatro")) {
                entry.setVenue(editVenueField.getText());
            } else if (type.contains("Cómic")) {
                entry.setIsSingleVolume(editSingleVolumeCheckBox.isSelected());
                if (!editSingleVolumeCheckBox.isSelected()) {
                    try {
                        entry.setComicNumber(Integer.parseInt(editComicNumberField.getText()));
                    } catch (NumberFormatException ex) {
                        showAlert("Error", "Número de serie debe ser un número válido");
                        return;
                    }
                }
            }

            db.updateEntry(entry);
            loadEntries();
            showAlert("Éxito", "Registro actualizado correctamente ✨");
        }
    }

    private void updateEditDynamicFields(Entry entry, ComboBox<String> editTypeCombo, VBox dynamicEditBox,
                                         TextField editChapterField, TextField editSeasonField, TextField editEpisodeField,
                                         TextField editVenueField, CheckBox editSingleVolumeCheckBox, TextField editComicNumberField) {
        dynamicEditBox.getChildren().clear();

        String type = editTypeCombo.getValue();
        if (type == null) return;

        if (type.contains("Libro")) {
            VBox bookBox = new VBox(5);
            Label chapterLabel = new Label("📖 Capítulo leído:");
            if (entry.getChapters() != null) {
                editChapterField.setText(entry.getChapters().toString());
            }
            bookBox.getChildren().addAll(chapterLabel, editChapterField);
            dynamicEditBox.getChildren().add(bookBox);
        } else if (type.contains("Serie")) {
            VBox seriesBox = new VBox(5);
            Label seasonLabel = new Label("🎬 Temporada:");
            if (entry.getSeason() != null) {
                editSeasonField.setText(entry.getSeason().toString());
            }
            Label episodeLabel = new Label("🎬 Capítulo:");
            if (entry.getEpisode() != null) {
                editEpisodeField.setText(entry.getEpisode().toString());
            }
            seriesBox.getChildren().addAll(seasonLabel, editSeasonField, episodeLabel, editEpisodeField);
            dynamicEditBox.getChildren().add(seriesBox);
        } else if (type.contains("Teatro")) {
            VBox theaterBox = new VBox(5);
            Label venueLabel = new Label("🎤 Lugar donde se vio:");
            if (entry.getVenue() != null) {
                editVenueField.setText(entry.getVenue());
            }
            theaterBox.getChildren().addAll(venueLabel, editVenueField);
            dynamicEditBox.getChildren().add(theaterBox);
        } else if (type.contains("Cómic")) {
            VBox comicBox = new VBox(5);
            if (entry.getIsSingleVolume() != null) {
                editSingleVolumeCheckBox.setSelected(entry.getIsSingleVolume());
            }
            editSingleVolumeCheckBox.setOnAction(e -> updateComicEditFields(editSingleVolumeCheckBox, editComicNumberField));
            
            if (entry.getComicNumber() != null && !editSingleVolumeCheckBox.isSelected()) {
                editComicNumberField.setText(entry.getComicNumber().toString());
            }
            if (!editSingleVolumeCheckBox.isSelected()) {
                editComicNumberField.setVisible(true);
            } else {
                editComicNumberField.setVisible(false);
            }

            comicBox.getChildren().addAll(
                    editSingleVolumeCheckBox,
                    new Label("📕 Número de serie:"),
                    editComicNumberField
            );
            dynamicEditBox.getChildren().add(comicBox);
        }
    }

    private void updateComicEditFields(CheckBox singleVolumeCheckBox, TextField comicNumberField) {
        comicNumberField.setVisible(!singleVolumeCheckBox.isSelected());
    }

    private void clearForm() {

        titleField.setValue(null);
        titleTextField.clear();
        descriptionArea.clear();
        typeCombo.setValue("📚 Libro");
        datePicker.setValue(LocalDate.now());
        coverPreview.setImage(null);
        selectedCoverPath = null;
        
        // Limpiar campos dinámicos
        if (chapterTextField != null) chapterTextField.clear();
        if (seasonTextField != null) seasonTextField.clear();
        if (episodeTextField != null) episodeTextField.clear();
        if (venueTextField != null) venueTextField.clear();
        if (isSingleVolumeCheckBox != null) isSingleVolumeCheckBox.setSelected(false);
        if (comicNumberTextField != null) comicNumberTextField.clear();
        
        updateDynamicFields();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
