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
import java.util.List;
import java.util.Optional;

public class Main extends Application {

    private Database db;
    private StatsPanel statsPanel;
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
    private TextField authorTextField;
    private TextField seasonTextField;
    private TextField episodeTextField;
    private TextField venueTextField;
    private CheckBox isSingleVolumeCheckBox;
    private TextField comicVolumeTextField;
    private TextField comicIssueTextField;
    private TextField directorTextField;
    private CheckBox seenInCinemaCheckBox;

    private TextField searchTitleField;
    private DatePicker searchDatePicker;
    private ComboBox<String> searchTypeCombo;

    // raíces que necesitamos reestilizar al cambiar tema
    private BorderPane root;
    private HBox header;
    private VBox inputPanel;
    private VBox searchPanel;
    private ScrollPane entriesScroll;
    private TabPane tabPane;
    private VBox dynamicCard;
    private Button themeBtn;
    private Tab statsTab;

    @Override
    public void start(Stage stage) {
        db = new Database();
        statsPanel = new StatsPanel(db);

        root = new BorderPane();
        applyRootStyle();

        // Header
        header = new HBox();
        header.setPadding(new Insets(14, 24, 14, 24));
        header.setAlignment(Pos.CENTER_LEFT);
        applyHeaderStyle();

        Label appTitle = new Label("📚  Mi Registro Cultural");
        appTitle.setStyle("-fx-font-size:22; -fx-font-weight:bold; -fx-text-fill:" + Theme.C_ACCENT + ";");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        themeBtn = new Button("🌙 Modo oscuro");
        themeBtn.setStyle("-fx-background-color:transparent; -fx-border-color:" + Theme.C_ACCENT +
                "; -fx-border-radius:20; -fx-text-fill:" + Theme.C_ACCENT +
                "; -fx-padding:6 16; -fx-cursor:hand; -fx-font-size:12;");
        themeBtn.setOnAction(e -> toggleTheme());

        header.getChildren().addAll(appTitle, spacer, themeBtn);
        root.setTop(header);

        // TabPane
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        applyTabPaneStyle();

        inputPanel  = createInputPanel();
        searchPanel = createSearchPanel();

        Tab registerTab = buildTab("📝  Registrar",    inputPanel);
        Tab searchTab2  = buildTab("🔍  Buscar",        searchPanel);
        statsTab        = buildTab("📊  Estadísticas", statsPanel.getRoot());
        tabPane.getTabs().addAll(registerTab, searchTab2, statsTab);
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, old, nw) -> {
            if (nw == statsTab) statsPanel.refresh();
        });

        // Lista de entradas
        entriesContainer = new VBox(12);
        entriesContainer.setPadding(new Insets(16));
        applyEntriesContainerStyle();

        entriesScroll = new ScrollPane(entriesContainer);
        entriesScroll.setFitToWidth(true);
        applyScrollStyle();

        VBox center = new VBox(tabPane, entriesScroll);
        VBox.setVgrow(entriesScroll, Priority.ALWAYS);
        root.setCenter(center);

        // Listener de tema → reestilizar todo
        Theme.darkMode.addListener((obs, old, nw) -> reapplyAllStyles());

        updateTitleSuggestions();
        updateDynamicFields();
        loadEntries();

        Scene scene = new Scene(root, 1200, 860);
        stage.setTitle("📚 Registro Cultural");
        stage.setScene(scene);
        stage.show();
    }

    // ══ TEMA ══════════════════════════════════════════════════════════════
    private void toggleTheme() {
        Theme.darkMode.set(!Theme.isDark());
    }

    private void reapplyAllStyles() {
        applyRootStyle();
        applyHeaderStyle();
        applyTabPaneStyle();
        applyEntriesContainerStyle();
        applyScrollStyle();
        if (inputPanel  != null) applyPanelStyle(inputPanel);
        if (searchPanel != null) applyPanelStyle(searchPanel);
        if (dynamicCard != null) applyDynamicCardStyle();
        // Actualizar botón tema
        themeBtn.setText(Theme.isDark() ? "☀️ Modo claro" : "🌙 Modo oscuro");
        themeBtn.setStyle("-fx-background-color:transparent; -fx-border-color:" + Theme.C_ACCENT +
                "; -fx-border-radius:20; -fx-text-fill:" + Theme.C_ACCENT +
                "; -fx-padding:6 16; -fx-cursor:hand; -fx-font-size:12;");
        // Recargar campos con nuevo estilo
        refreshFieldStyles();
        loadEntries();
        statsPanel.refresh();
    }

    private void applyRootStyle()     { root.setStyle("-fx-background-color:" + Theme.bg() + ";"); }
    private void applyHeaderStyle()   {
        header.setStyle("-fx-background-color:" + Theme.surface() +
                "; -fx-effect:dropshadow(gaussian," + Theme.shadow() + ",6,0,0,2);");
    }
    private void applyTabPaneStyle()  { tabPane.setStyle("-fx-background-color:" + Theme.bg() + ";"); }
    private void applyScrollStyle()   {
        entriesScroll.setStyle("-fx-background-color:" + Theme.bg() +
                "; -fx-background:" + Theme.bg() + ";");
    }
    private void applyEntriesContainerStyle() {
        entriesContainer.setStyle("-fx-background-color:" + Theme.bg() + ";");
    }
    private void applyPanelStyle(VBox panel) {
        panel.setStyle("-fx-background-color:" + Theme.surface() + ";");
    }
    private void applyDynamicCardStyle() {
        if (dynamicCard != null)
            dynamicCard.setStyle("-fx-background-color:" + Theme.card() +
                    "; -fx-background-radius:10; -fx-padding:12;" +
                    " -fx-border-color:" + Theme.border() + "; -fx-border-radius:10;");
    }

    private void refreshFieldStyles() {
        // Reestilizar campos del panel de registro
        String fs = Theme.fieldStyle();
        if (titleField   != null) titleField.setStyle(fs);
        if (typeCombo    != null) typeCombo.setStyle(fs);
        if (datePicker   != null) datePicker.setStyle(fs);
        if (descriptionArea != null) descriptionArea.setStyle(fs);
        if (searchTitleField   != null) searchTitleField.setStyle(fs);
        if (searchDatePicker   != null) searchDatePicker.setStyle(fs);
        if (searchTypeCombo    != null) searchTypeCombo.setStyle(fs);
        // Reconstruir campos dinámicos
        updateDynamicFields();
    }

    private Tab buildTab(String text, javafx.scene.Node content) {
        Tab tab = new Tab(text, content);
        tab.setStyle("-fx-font-size:13;");
        return tab;
    }

    // ══ PANEL REGISTRAR ══════════════════════════════════════════════════
    private VBox createInputPanel() {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(22));
        panel.setStyle("-fx-background-color:" + Theme.surface() + ";");

        Label heading = new Label("📝 Registrar nueva entrada");
        heading.setStyle("-fx-font-size:17; -fx-font-weight:bold; -fx-text-fill:" + Theme.C_ACCENT + ";");
        panel.getChildren().add(heading);

        HBox row1 = new HBox(15);
        VBox titleBox = new VBox(5);
        titleBox.getChildren().add(styledLabel("Título:"));
        titleField = new ComboBox<>();
        titleField.setEditable(true);
        titleField.setPrefWidth(320);
        titleField.setStyle(Theme.fieldStyle());
        titleTextField = titleField.getEditor();
        titleTextField.setPromptText("Ej: El Quijote");
        titleTextField.textProperty().addListener((o, ov, nv) -> {
            if (!nv.isBlank() && typeCombo.getValue() != null) updateDynamicFields();
        });
        titleField.setOnAction(e -> updateDynamicFields());
        titleBox.getChildren().add(titleField);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        VBox typeBox = new VBox(5);
        typeBox.getChildren().add(styledLabel("Tipo:"));
        typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("📚 Libro", "🎬 Serie", "🎥 Película", "🎭 Teatro", "💭 Cómic");
        typeCombo.setValue("📚 Libro");
        typeCombo.setPrefWidth(160);
        typeCombo.setStyle(Theme.fieldStyle());
        typeCombo.setOnAction(e -> { updateTitleSuggestions(); updateDynamicFields(); });
        typeBox.getChildren().add(typeCombo);

        VBox dateBox = new VBox(5);
        dateBox.getChildren().add(styledLabel("Fecha:"));
        datePicker = new DatePicker(LocalDate.now());
        datePicker.setPrefWidth(160);
        datePicker.setStyle(Theme.fieldStyle());
        dateBox.getChildren().add(datePicker);

        row1.getChildren().addAll(titleBox, typeBox, dateBox);
        panel.getChildren().add(row1);

        dynamicCard = new VBox(10);
        dynamicFieldsBox = dynamicCard;
        applyDynamicCardStyle();
        panel.getChildren().add(dynamicCard);

        VBox descBox = new VBox(5);
        descBox.getChildren().add(styledLabel("Descripción / Notas:"));
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("¿Qué te pareció?");
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setStyle(Theme.fieldStyle());
        descBox.getChildren().add(descriptionArea);
        panel.getChildren().add(descBox);

        HBox row3 = new HBox(20);
        row3.setAlignment(Pos.CENTER_LEFT);
        VBox coverBox = new VBox(5);
        coverBox.getChildren().add(styledLabel("Portada (arrastra):"));
        coverPreview = new ImageView();
        coverPreview.setFitWidth(90); coverPreview.setFitHeight(130);
        coverPreview.setStyle("-fx-border-color:" + Theme.C_ACCENT +
                "; -fx-border-width:2; -fx-border-style:dashed; -fx-border-radius:6;");
        setupDragAndDrop();
        coverBox.getChildren().add(coverPreview);
        VBox btnBox = new VBox(10);
        btnBox.setAlignment(Pos.CENTER);
        Button btnSave  = new Button("✅  Guardar entrada");
        btnSave.setStyle(Theme.btnPrimary());
        btnSave.setOnAction(e -> onAddEntry());
        Button btnClear = new Button("🔄  Limpiar");
        btnClear.setStyle(Theme.btnSecondary());
        btnClear.setOnAction(e -> clearForm());
        btnBox.getChildren().addAll(btnSave, btnClear);
        row3.getChildren().addAll(coverBox, btnBox);
        HBox.setHgrow(btnBox, Priority.ALWAYS);
        panel.getChildren().add(row3);
        return panel;
    }

    // ══ CAMPOS DINÁMICOS ══════════════════════════════════════════════════
    private void updateDynamicFields() {
        if (dynamicFieldsBox == null) return;
        dynamicFieldsBox.getChildren().clear();
        String type = typeCombo.getValue();
        if (type == null) return;
        String titleVal = getTitleFieldValue();
        String fs = Theme.fieldStyle();

        if (type.contains("Libro")) {
            VBox authorBox = new VBox(4);
            authorBox.getChildren().add(styledLabel("✍️ Autor:"));
            authorTextField = new TextField();
            authorTextField.setStyle(fs);
            authorTextField.setPromptText("Ej: Stephen King");
            if (!titleVal.isEmpty()) {
                String saved = db.getAuthorForTitle(titleVal);
                if (saved != null) authorTextField.setText(saved);
            }
            authorBox.getChildren().add(authorTextField);

            VBox chapBox = new VBox(4);
            chapBox.getChildren().add(styledLabel("📖 Capítulo leído:"));
            chapterTextField = new TextField();
            chapterTextField.setStyle(fs);
            Integer lastChap = titleVal.isEmpty() ? null : db.getLastChapterForTitle(titleVal, type);
            if (lastChap != null) {
                chapterTextField.setPromptText("Sugerencia: " + (lastChap + 1));
                chapterTextField.setStyle(fs + "-fx-prompt-text-fill:" + Theme.C_SUCCESS + ";");
            } else chapterTextField.setPromptText("Ej: 5");
            chapBox.getChildren().add(chapterTextField);

            HBox r = new HBox(15, authorBox, chapBox);
            HBox.setHgrow(authorBox, Priority.ALWAYS); HBox.setHgrow(chapBox, Priority.ALWAYS);
            dynamicFieldsBox.getChildren().add(r);

        } else if (type.contains("Serie")) {
            VBox sBox = new VBox(4); sBox.getChildren().add(styledLabel("📺 Temporada:"));
            seasonTextField = new TextField(); seasonTextField.setStyle(fs); seasonTextField.setPromptText("1");
            VBox eBox = new VBox(4); eBox.getChildren().add(styledLabel("🎞️ Capítulo:"));
            episodeTextField = new TextField(); episodeTextField.setStyle(fs); episodeTextField.setPromptText("1");
            if (!titleVal.isEmpty()) {
                Database.SeriesInfo info = db.getLastSeriesInfo(titleVal);
                if (info != null) {
                    int nextEp = info.episode + 1; int nextSea = info.season;
                    if (nextEp > 30) { nextEp = 1; nextSea++; }
                    seasonTextField.setText(String.valueOf(nextSea));
                    episodeTextField.setText(String.valueOf(nextEp));
                    seasonTextField.setStyle(fs + "-fx-text-fill:" + Theme.C_SUCCESS + ";");
                    episodeTextField.setStyle(fs + "-fx-text-fill:" + Theme.C_SUCCESS + ";");
                }
            }
            sBox.getChildren().add(seasonTextField); eBox.getChildren().add(episodeTextField);
            HBox r = new HBox(15, sBox, eBox);
            HBox.setHgrow(sBox, Priority.ALWAYS); HBox.setHgrow(eBox, Priority.ALWAYS);
            dynamicFieldsBox.getChildren().add(r);

        } else if (type.contains("Pel")) {
            VBox dirBox = new VBox(4); dirBox.getChildren().add(styledLabel("🎬 Director:"));
            directorTextField = new TextField(); directorTextField.setStyle(fs);
            directorTextField.setPromptText("Ej: Christopher Nolan");
            if (!titleVal.isEmpty()) { String d = db.getDirectorForTitle(titleVal); if (d != null) directorTextField.setText(d); }
            dirBox.getChildren().add(directorTextField);
            seenInCinemaCheckBox = new CheckBox("🎫 Vista en el cine");
            seenInCinemaCheckBox.setStyle("-fx-text-fill:" + Theme.text() + "; -fx-font-size:12;");
            dynamicFieldsBox.getChildren().addAll(dirBox, seenInCinemaCheckBox);

        } else if (type.contains("Teatro")) {
            VBox vBox = new VBox(4); vBox.getChildren().add(styledLabel("🎤 Lugar:"));
            venueTextField = new TextField(); venueTextField.setStyle(fs); venueTextField.setPromptText("Ej: Teatro Nacional");
            vBox.getChildren().add(venueTextField);
            dynamicFieldsBox.getChildren().add(vBox);

        } else if (type.contains("Cómic")) {
            isSingleVolumeCheckBox = new CheckBox("¿Es tomo único?");
            isSingleVolumeCheckBox.setStyle("-fx-text-fill:" + Theme.text() + "; -fx-font-size:12;");
            VBox volBox = new VBox(4); volBox.getChildren().add(styledLabel("📕 Número de tomo:"));
            comicVolumeTextField = new TextField(); comicVolumeTextField.setStyle(fs); comicVolumeTextField.setPromptText("Ej: 1");
            volBox.getChildren().add(comicVolumeTextField);
            VBox issBox = new VBox(4); issBox.getChildren().add(styledLabel("📖 Número de serie:"));
            comicIssueTextField = new TextField(); comicIssueTextField.setStyle(fs); comicIssueTextField.setPromptText("Ej: 1");
            issBox.getChildren().add(comicIssueTextField);
            HBox comicRow = new HBox(15, volBox, issBox);
            HBox.setHgrow(volBox, Priority.ALWAYS); HBox.setHgrow(issBox, Priority.ALWAYS);
            isSingleVolumeCheckBox.setOnAction(e -> {
                boolean s = isSingleVolumeCheckBox.isSelected();
                comicRow.setVisible(!s); comicRow.setManaged(!s);
            });
            dynamicFieldsBox.getChildren().addAll(isSingleVolumeCheckBox, comicRow);
        }
    }

    // ══ PANEL BÚSQUEDA ═══════════════════════════════════════════════════
    private VBox createSearchPanel() {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(22));
        panel.setStyle("-fx-background-color:" + Theme.surface() + ";");
        Label heading = new Label("🔍 Buscar registros");
        heading.setStyle("-fx-font-size:17; -fx-font-weight:bold; -fx-text-fill:" + Theme.C_ACCENT + ";");
        panel.getChildren().add(heading);

        HBox row1 = new HBox(12); row1.setAlignment(Pos.BOTTOM_LEFT);
        VBox tBox = new VBox(5); tBox.getChildren().add(styledLabel("Buscar por nombre:"));
        searchTitleField = new TextField(); searchTitleField.setStyle(Theme.fieldStyle()); searchTitleField.setPrefWidth(300);
        searchTitleField.setPromptText("Ej: Breaking Bad");
        tBox.getChildren().add(searchTitleField); HBox.setHgrow(tBox, Priority.ALWAYS);
        Button btnSearch = new Button("🔎  Buscar"); btnSearch.setStyle(Theme.btnPrimary()); btnSearch.setOnAction(e -> onSearchByTitle());
        row1.getChildren().addAll(tBox, btnSearch);
        panel.getChildren().add(row1);

        HBox row2 = new HBox(12); row2.setAlignment(Pos.BOTTOM_LEFT);
        VBox dBox = new VBox(5); dBox.getChildren().add(styledLabel("Buscar por fecha:"));
        searchDatePicker = new DatePicker(); searchDatePicker.setStyle(Theme.fieldStyle());
        dBox.getChildren().add(searchDatePicker);
        VBox typeBox2 = new VBox(5); typeBox2.getChildren().add(styledLabel("Filtrar por tipo:"));
        searchTypeCombo = new ComboBox<>();
        searchTypeCombo.getItems().addAll("Todo", "📚 Libro", "🎬 Serie", "🎥 Película", "🎭 Teatro", "💭 Cómic");
        searchTypeCombo.setValue("Todo"); searchTypeCombo.setStyle(Theme.fieldStyle());
        typeBox2.getChildren().add(searchTypeCombo);
        Button btnDate = new Button("📅  Por fecha"); btnDate.setStyle(Theme.btnSecondary()); btnDate.setOnAction(e -> onSearchByDate());
        Button btnAll  = new Button("📋  Ver todos");  btnAll.setStyle(Theme.btnSecondary());  btnAll.setOnAction(e -> loadEntries());
        row2.getChildren().addAll(dBox, typeBox2, btnDate, btnAll);
        panel.getChildren().add(row2);

        Label hint = new Label("💡 La búsqueda por nombre es parcial e insensible a mayúsculas");
        hint.setStyle("-fx-text-fill:" + Theme.muted() + "; -fx-font-size:11; -fx-font-style:italic;");
        panel.getChildren().add(hint);
        return panel;
    }

    // ══ GUARDAR ═══════════════════════════════════════════════════════════
    private void onAddEntry() {
        if (getTitleFieldValue().isEmpty() || typeCombo.getValue() == null) {
            showAlert("Error", "Por favor completa título y tipo"); return;
        }
        String type = typeCombo.getValue();
        Integer chapters = null, season = null, episode = null, comicVolume = null, comicIssue = null;
        String venue = null, author = null, director = null;
        Boolean isSingleVolume = null, seenInCinema = null;

        if (type.contains("Libro")) {
            author = authorTextField != null ? authorTextField.getText().trim() : null;
            if (chapterTextField.getText().trim().isEmpty()) { showAlert("Error", "Ingresa el capítulo"); return; }
            try { chapters = Integer.parseInt(chapterTextField.getText().trim()); if (chapters <= 0) throw new NumberFormatException(); }
            catch (NumberFormatException e) { showAlert("Error", "Capítulo debe ser un número mayor a 0"); return; }
        } else if (type.contains("Serie")) {
            try { season = Integer.parseInt(seasonTextField.getText().trim()); episode = Integer.parseInt(episodeTextField.getText().trim()); }
            catch (NumberFormatException e) { showAlert("Error", "Temporada y capítulo deben ser números"); return; }
        } else if (type.contains("Teatro")) {
            venue = venueTextField != null ? venueTextField.getText().trim() : null;
        } else if (type.contains("Pel")) {
            director = directorTextField != null ? directorTextField.getText().trim() : null;
            seenInCinema = seenInCinemaCheckBox != null && seenInCinemaCheckBox.isSelected();
        } else if (type.contains("Cómic")) {
            isSingleVolume = isSingleVolumeCheckBox != null && isSingleVolumeCheckBox.isSelected();
            if (!Boolean.TRUE.equals(isSingleVolume)) {
                try { comicVolume = Integer.parseInt(comicVolumeTextField.getText().trim()); }
                catch (NumberFormatException e) { showAlert("Error", "Número de tomo debe ser un número"); return; }
                try { comicIssue = Integer.parseInt(comicIssueTextField.getText().trim()); }
                catch (NumberFormatException e) { showAlert("Error", "Número de serie debe ser un número"); return; }
            }
        }

        Entry entry = new Entry(getTitleFieldValue(), type, descriptionArea.getText(),
                datePicker.getValue(), selectedCoverPath, chapters, season, episode);
        entry.setAuthor(author); entry.setVenue(venue); entry.setIsSingleVolume(isSingleVolume);
        entry.setComicVolume(comicVolume); entry.setComicIssue(comicIssue);
        entry.setDirector(director); entry.setSeenInCinema(seenInCinema);
        db.addEntry(entry);
        clearForm(); loadEntries();
        showAlert("Éxito", "Entrada registrada correctamente ✨");
    }

    // ══ TARJETAS ══════════════════════════════════════════════════════════
    private void loadEntries() {
        entriesContainer.getChildren().clear();
        List<Entry> entries = db.getAllEntries();
        if (entries.isEmpty()) {
            Label empty = new Label("📭 No hay registros todavía. ¡Empieza a registrar!");
            empty.setStyle("-fx-font-size:15; -fx-text-fill:" + Theme.muted() + ";");
            entriesContainer.getChildren().add(empty); return;
        }
        for (Entry e : entries) entriesContainer.getChildren().add(createEntryCard(e));
    }

    private VBox createEntryCard(Entry entry) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setStyle(Theme.cardStyle());
        HBox head = new HBox(14);
        head.setAlignment(Pos.TOP_LEFT);
        if (entry.getCoverPath() != null) {
            try {
                ImageView img = new ImageView(new Image(new FileInputStream(entry.getCoverPath()), 75, 110, true, true));
                img.setStyle("-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),6,0,0,2);");
                head.getChildren().add(img);
            } catch (Exception ex) { head.getChildren().add(placeholderIcon()); }
        } else { head.getChildren().add(placeholderIcon()); }

        VBox info = new VBox(5);
        Label typeL  = new Label(entry.getType());
        typeL.setStyle("-fx-font-size:11; -fx-text-fill:" + Theme.C_ACCENT2 + "; -fx-font-weight:bold;");
        Label titleL = new Label(entry.getTitle());
        titleL.setStyle("-fx-font-size:15; -fx-font-weight:bold; -fx-text-fill:" + Theme.text() + ";");
        info.getChildren().addAll(typeL, titleL);
        if (entry.getType().contains("Libro")) {
            if (entry.getAuthor() != null && !entry.getAuthor().isBlank()) info.getChildren().add(chip("✍️ " + entry.getAuthor()));
            if (entry.getChapters() != null) info.getChildren().add(chip("📖 Cap. " + entry.getChapters()));
        } else if (entry.getType().contains("Serie") && entry.getSeason() != null) {
            info.getChildren().add(chip("T" + entry.getSeason() + " E" + entry.getEpisode()));
        } else if (entry.getType().contains("Pel")) {
            if (entry.getDirector() != null && !entry.getDirector().isBlank()) info.getChildren().add(chip("🎬 " + entry.getDirector()));
            if (Boolean.TRUE.equals(entry.getSeenInCinema())) info.getChildren().add(chip("🎫 Vista en cine"));
        } else if (entry.getType().contains("Teatro") && entry.getVenue() != null) {
            info.getChildren().add(chip("🎭 " + entry.getVenue()));
        } else if (entry.getType().contains("Cómic")) {
            if (Boolean.TRUE.equals(entry.getIsSingleVolume())) info.getChildren().add(chip("Tomo único"));
            else {
                String ci = "";
                if (entry.getComicVolume() != null) ci += "Tomo " + entry.getComicVolume();
                if (entry.getComicIssue()  != null) ci += (ci.isEmpty()?"":"  ") + "#" + entry.getComicIssue();
                if (!ci.isEmpty()) info.getChildren().add(chip("📕 " + ci));
            }
        }
        info.getChildren().add(chip("📅 " + entry.getDate()));
        HBox.setHgrow(info, Priority.ALWAYS);
        head.getChildren().add(info);

        VBox btns = new VBox(6); btns.setAlignment(Pos.TOP_RIGHT);
        Button editBtn = iconBtn("✏️"); editBtn.setOnAction(e -> openEditDialog(entry));
        Button delBtn  = iconBtn("🗑️");  delBtn.setOnAction(e -> { db.deleteEntry(entry.getId()); loadEntries(); });
        btns.getChildren().addAll(editBtn, delBtn);
        head.getChildren().add(btns);
        card.getChildren().add(head);

        if (entry.getDescription() != null && !entry.getDescription().isBlank()) {
            TextArea desc = new TextArea(entry.getDescription());
            desc.setWrapText(true); desc.setPrefRowCount(2); desc.setEditable(false);
            desc.setStyle("-fx-control-inner-background:" + Theme.input() +
                    "; -fx-text-fill:" + Theme.muted() +
                    "; -fx-font-size:11; -fx-background-radius:6;");
            card.getChildren().add(desc);
        }
        return card;
    }

    // ══ EDICIÓN ═══════════════════════════════════════════════════════════
    private void openEditDialog(Entry entry) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("✏️ Editar Registro");
        dialog.setHeaderText("Modifica los datos del registro");
        dialog.getDialogPane().setStyle("-fx-background-color:" + Theme.surface() + ";");
        VBox form = new VBox(10); form.setPadding(new Insets(18));
        form.setStyle("-fx-background-color:" + Theme.surface() + ";");
        String fs = Theme.fieldStyle();

        TextField eTitleF = styledField(entry.getTitle());
        ComboBox<String> eTypeC = new ComboBox<>();
        eTypeC.getItems().addAll("📚 Libro","🎬 Serie","🎥 Película","🎭 Teatro","💭 Cómic");
        eTypeC.setValue(entry.getType()); eTypeC.setStyle(fs);
        TextArea eDescA = new TextArea(entry.getDescription() != null ? entry.getDescription() : "");
        eDescA.setWrapText(true); eDescA.setPrefRowCount(3); eDescA.setStyle(fs);
        DatePicker eDateP = new DatePicker(entry.getDate()); eDateP.setStyle(fs);

        TextField eChapF = styledField(entry.getChapters() != null ? entry.getChapters().toString() : "");
        TextField eAuthF = styledField(entry.getAuthor()   != null ? entry.getAuthor()   : "");
        TextField eSeaF  = styledField(entry.getSeason()   != null ? entry.getSeason().toString()  : "");
        TextField eEpF   = styledField(entry.getEpisode()  != null ? entry.getEpisode().toString() : "");
        TextField eVenF  = styledField(entry.getVenue()    != null ? entry.getVenue()    : "");
        TextField eDirF  = styledField(entry.getDirector() != null ? entry.getDirector() : "");
        CheckBox eCinC   = new CheckBox("🎫 Vista en el cine"); eCinC.setStyle("-fx-text-fill:" + Theme.text() + ";");
        eCinC.setSelected(Boolean.TRUE.equals(entry.getSeenInCinema()));
        CheckBox eSingC  = new CheckBox("¿Es tomo único?"); eSingC.setStyle("-fx-text-fill:" + Theme.text() + ";");
        eSingC.setSelected(Boolean.TRUE.equals(entry.getIsSingleVolume()));
        TextField eVolF  = styledField(entry.getComicVolume() != null ? entry.getComicVolume().toString() : "");
        TextField eIssF  = styledField(entry.getComicIssue()  != null ? entry.getComicIssue().toString()  : "");

        VBox dynEdit = new VBox(8);
        Runnable refreshDyn = () -> {
            dynEdit.getChildren().clear();
            String t = eTypeC.getValue(); if (t == null) return;
            if (t.contains("Libro"))       dynEdit.getChildren().addAll(styledLabel("✍️ Autor:"),eAuthF,styledLabel("📖 Capítulo:"),eChapF);
            else if (t.contains("Serie"))  dynEdit.getChildren().addAll(styledLabel("Temporada:"),eSeaF,styledLabel("Capítulo:"),eEpF);
            else if (t.contains("Pel"))    dynEdit.getChildren().addAll(styledLabel("Director:"),eDirF,eCinC);
            else if (t.contains("Teatro")) dynEdit.getChildren().addAll(styledLabel("Lugar:"),eVenF);
            else if (t.contains("Cómic"))  dynEdit.getChildren().addAll(eSingC,styledLabel("Nº tomo:"),eVolF,styledLabel("Nº serie:"),eIssF);
        };
        refreshDyn.run(); eTypeC.setOnAction(e -> refreshDyn.run());
        form.getChildren().addAll(styledLabel("Título:"),eTitleF,styledLabel("Tipo:"),eTypeC,
                styledLabel("Descripción:"),eDescA,styledLabel("Fecha:"),eDateP,dynEdit);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            entry.setTitle(eTitleF.getText()); entry.setType(eTypeC.getValue());
            entry.setDescription(eDescA.getText()); entry.setDate(eDateP.getValue());
            String t = eTypeC.getValue();
            try {
                if (t.contains("Libro"))       { entry.setAuthor(eAuthF.getText()); entry.setChapters(Integer.parseInt(eChapF.getText().trim())); }
                else if (t.contains("Serie"))  { entry.setSeason(Integer.parseInt(eSeaF.getText().trim())); entry.setEpisode(Integer.parseInt(eEpF.getText().trim())); }
                else if (t.contains("Teatro")) { entry.setVenue(eVenF.getText()); }
                else if (t.contains("Pel"))    { entry.setDirector(eDirF.getText()); entry.setSeenInCinema(eCinC.isSelected()); }
                else if (t.contains("Cómic"))  {
                    entry.setIsSingleVolume(eSingC.isSelected());
                    if (!eSingC.isSelected()) { entry.setComicVolume(Integer.parseInt(eVolF.getText().trim())); entry.setComicIssue(Integer.parseInt(eIssF.getText().trim())); }
                }
            } catch (NumberFormatException ex) { showAlert("Error", "Revisa los campos numéricos"); return; }
            db.updateEntry(entry); loadEntries();
            showAlert("Éxito", "Registro actualizado ✨");
        }
    }

    // ══ BÚSQUEDA ══════════════════════════════════════════════════════════
    private void onSearchByTitle() {
        String term = searchTitleField.getText().trim();
        if (term.isEmpty()) { showAlert("Info", "Introduce un término de búsqueda"); return; }
        String sel = searchTypeCombo.getValue();
        displayResults("Todo".equals(sel) ? db.searchByTitle(term) : db.searchByTitleAndType(term, sel),
                "búsqueda '" + term + "'");
    }
    private void onSearchByDate() {
        LocalDate d = searchDatePicker.getValue();
        if (d == null) { showAlert("Info", "Selecciona una fecha"); return; }
        displayResults(db.searchByDate(d), "fecha " + d);
    }
    private void displayResults(List<Entry> entries, String desc) {
        entriesContainer.getChildren().clear();
        if (entries.isEmpty()) {
            Label l = new Label("📭 Sin resultados para " + desc);
            l.setStyle("-fx-font-size:14; -fx-text-fill:" + Theme.muted() + ";");
            entriesContainer.getChildren().add(l); return;
        }
        Label l = new Label("📊 " + entries.size() + " resultado(s) para " + desc);
        l.setStyle("-fx-font-size:13; -fx-font-weight:bold; -fx-text-fill:" + Theme.C_SUCCESS + ";");
        entriesContainer.getChildren().add(l);
        for (Entry e : entries) entriesContainer.getChildren().add(createEntryCard(e));
    }

    // ══ HELPERS ═══════════════════════════════════════════════════════════
    private void updateTitleSuggestions() {
        if (typeCombo == null || titleField == null) return;
        List<String> s = db.getTitleSuggestions(typeCombo.getValue() != null ? typeCombo.getValue() : "");
        titleField.setItems(FXCollections.observableArrayList(s));
    }
    private void setupDragAndDrop() {
        coverPreview.setOnDragOver(ev -> { ev.acceptTransferModes(javafx.scene.input.TransferMode.COPY); ev.consume(); });
        coverPreview.setOnDragDropped(ev -> {
            List<java.io.File> files = ev.getDragboard().getFiles();
            if (!files.isEmpty()) {
                selectedCoverPath = files.get(0).getAbsolutePath();
                try { coverPreview.setImage(new Image(new FileInputStream(files.get(0)), 90, 130, true, true)); }
                catch (Exception e) { showAlert("Error", "No se pudo cargar la imagen"); }
            }
            ev.consume();
        });
    }
    private void clearForm() {
        titleField.setValue(null); titleTextField.clear(); descriptionArea.clear();
        typeCombo.setValue("📚 Libro"); datePicker.setValue(LocalDate.now());
        coverPreview.setImage(null); selectedCoverPath = null;
        updateDynamicFields();
    }
    private String getTitleFieldValue() {
        String v = titleField.getValue();
        return v != null ? v.trim() : (titleTextField != null ? titleTextField.getText().trim() : "");
    }
    private Label styledLabel(String text) { Label l = new Label(text); l.setStyle(Theme.labelBold()); return l; }
    private TextField styledField(String text) { TextField f = new TextField(text); f.setStyle(Theme.fieldStyle()); return f; }
    private Label chip(String text) { Label l = new Label(text); l.setStyle(Theme.chipStyle()); return l; }
    private Label placeholderIcon() { Label l = new Label("📷"); l.setStyle("-fx-font-size:36; -fx-text-fill:" + Theme.muted() + ";"); return l; }
    private Button iconBtn(String icon) {
        Button b = new Button(icon);
        b.setStyle("-fx-background-color:transparent; -fx-font-size:15; -fx-cursor:hand; -fx-padding:4 8;");
        return b;
    }
    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.getDialogPane().setStyle("-fx-background-color:" + Theme.surface() + "; -fx-font-size:13;");
        a.showAndWait();
    }
    public static void main(String[] args) { launch(args); }
}
