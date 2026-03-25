package com.readingtracker;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.readingtracker.models.Database;
import com.readingtracker.models.Entry;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    private StackPane coverDropZone;
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
    private CheckBox finishedCheck;
    private CheckBox seasonFinishedCheck;
    private CheckBox seriesFinishedCheck;
    private int currentRating = 0;
    private Label[] starLabels;

    private TextField searchTitleField;
    private DatePicker searchDatePicker;
    private ComboBox<String> searchTypeCombo;

    private BorderPane root;
    private HBox header;
    private VBox inputPanel;
    private VBox searchPanel;
    private ScrollPane entriesScroll;
    private TabPane tabPane;
    private VBox dynamicCard;
    private Button themeBtn;
    private Tab statsTab;

    /** Directorio local donde se guardan las portadas descargadas desde el navegador */
    private static final String COVERS_DIR = "covers";

    @Override
    public void start(Stage stage) {
        // Crear directorio de portadas si no existe
        try { Files.createDirectories(Paths.get(COVERS_DIR)); } catch (Exception ignored) {}

        db = new Database();
        statsPanel = new StatsPanel(db);

        root = new BorderPane();
        applyRootStyle();

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
        themeBtn.setOnAction(e -> Theme.darkMode.set(!Theme.isDark()));
        header.getChildren().addAll(appTitle, spacer, themeBtn);
        root.setTop(header);

        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        applyTabPaneStyle();

        inputPanel  = createInputPanel();
        searchPanel = createSearchPanel();
        Tab registerTab = buildTab("📝  Registrar",     inputPanel);
        Tab searchTab2  = buildTab("🔍  Buscar",         searchPanel);
        statsTab        = buildTab("📊  Estadísticas",  statsPanel.getRoot());
        tabPane.getTabs().addAll(registerTab, searchTab2, statsTab);
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, old, nw) -> {
            if (nw == statsTab) statsPanel.refresh();
        });

        entriesContainer = new VBox(12);
        entriesContainer.setPadding(new Insets(16));
        applyEntriesContainerStyle();
        entriesScroll = new ScrollPane(entriesContainer);
        entriesScroll.setFitToWidth(true);
        applyScrollStyle();

        VBox center = new VBox(tabPane, entriesScroll);
        VBox.setVgrow(entriesScroll, Priority.ALWAYS);
        root.setCenter(center);

        Theme.darkMode.addListener((obs, old, nw) -> reapplyAllStyles());
        updateTitleSuggestions();
        updateDynamicFields();
        loadEntries();

        Scene scene = new Scene(root, 1200, 860);
        stage.setTitle("📚 Registro Cultural");
        stage.setScene(scene);
        stage.show();
    }

    // ═ TEMA ══════════════════════════════════════════════════
    private void reapplyAllStyles() {
        applyRootStyle(); applyHeaderStyle(); applyTabPaneStyle();
        applyEntriesContainerStyle(); applyScrollStyle();
        if (inputPanel  != null) inputPanel.setStyle("-fx-background-color:" + Theme.surface() + ";");
        if (searchPanel != null) searchPanel.setStyle("-fx-background-color:" + Theme.surface() + ";");
        if (dynamicCard != null) applyDynamicCardStyle();
        themeBtn.setText(Theme.isDark() ? "☀️ Modo claro" : "🌙 Modo oscuro");
        themeBtn.setStyle("-fx-background-color:transparent; -fx-border-color:" + Theme.C_ACCENT +
            "; -fx-border-radius:20; -fx-text-fill:" + Theme.C_ACCENT +
            "; -fx-padding:6 16; -fx-cursor:hand; -fx-font-size:12;");
        refreshFieldStyles(); loadEntries(); statsPanel.refresh();
    }
    private void applyRootStyle()   { root.setStyle("-fx-background-color:" + Theme.bg() + ";"); }
    private void applyHeaderStyle() { header.setStyle("-fx-background-color:" + Theme.surface() +
        "; -fx-effect:dropshadow(gaussian," + Theme.shadow() + ",6,0,0,2);"); }
    private void applyTabPaneStyle(){ tabPane.setStyle("-fx-background-color:" + Theme.bg() + ";"); }
    private void applyScrollStyle() { entriesScroll.setStyle("-fx-background-color:" + Theme.bg() +
        "; -fx-background:" + Theme.bg() + ";"); }
    private void applyEntriesContainerStyle() { entriesContainer.setStyle("-fx-background-color:" + Theme.bg() + ";"); }
    private void applyDynamicCardStyle() {
        dynamicCard.setStyle("-fx-background-color:" + Theme.card() +
            "; -fx-background-radius:10; -fx-padding:12; -fx-border-color:" + Theme.border() + "; -fx-border-radius:10;");
    }
    private void refreshFieldStyles() {
        String fs = Theme.fieldStyle();
        if (titleField    != null) titleField.setStyle(fs);
        if (typeCombo     != null) typeCombo.setStyle(fs);
        if (datePicker    != null) datePicker.setStyle(fs);
        if (descriptionArea!=null) descriptionArea.setStyle(fs);
        if (searchTitleField  !=null) searchTitleField.setStyle(fs);
        if (searchDatePicker  !=null) searchDatePicker.setStyle(fs);
        if (searchTypeCombo   !=null) searchTypeCombo.setStyle(fs);
        updateDynamicFields();
    }
    private Tab buildTab(String text, javafx.scene.Node content) {
        Tab t = new Tab(text, content); t.setStyle("-fx-font-size:13;"); return t;
    }

    // ═ PANEL REGISTRAR ═══════════════════════════════════════════════
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
            if (!nv.isBlank() && typeCombo != null && typeCombo.getValue() != null) updateDynamicFields();
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

        HBox row3 = new HBox(24);
        row3.setAlignment(Pos.CENTER_LEFT);

        // ── Zona de portada ──────────────────────────────────────────────
        VBox coverBox = new VBox(5);
        coverBox.getChildren().add(styledLabel("Portada:"));
        coverPreview = new ImageView();
        coverPreview.setFitWidth(90); coverPreview.setFitHeight(130);
        coverPreview.setPreserveRatio(true);
        Label coverHint = new Label("📸\nArrastra desde\nel navegador\no haz clic");
        coverHint.setStyle("-fx-text-fill:" + Theme.muted() + "; -fx-font-size:10; -fx-text-alignment:center;");
        coverHint.setAlignment(Pos.CENTER);
        coverDropZone = new StackPane(coverHint, coverPreview);
        coverDropZone.setPrefSize(90, 130);
        coverDropZone.setMinSize(90, 130);
        coverDropZone.setStyle("-fx-border-color:" + Theme.C_ACCENT +
            "; -fx-border-width:2; -fx-border-style:dashed; -fx-border-radius:8; -fx-cursor:hand;");
        setupDragAndDrop();
        coverDropZone.setOnMouseClicked(ev -> openFilePicker());
        coverBox.getChildren().add(coverDropZone);

        // ── Estrellas ────────────────────────────────────────────────────
        VBox starBox = new VBox(6);
        starBox.getChildren().add(styledLabel("⭐ Valoración:"));
        HBox starsRow = buildStarWidget(10, 0);
        starBox.getChildren().add(starsRow);

        VBox btnBox = new VBox(10);
        btnBox.setAlignment(Pos.CENTER);
        Button btnSave = new Button("✅  Guardar entrada");
        btnSave.setStyle(Theme.btnPrimary());
        btnSave.setOnAction(e -> onAddEntry());
        btnBox.getChildren().add(btnSave);
        HBox.setHgrow(btnBox, Priority.ALWAYS);

        row3.getChildren().addAll(coverBox, starBox, btnBox);
        panel.getChildren().add(row3);
        return panel;
    }

    // ═ DRAG & DROP (archivo local + URL del navegador) ══════════════════
    private void setupDragAndDrop() {
        coverDropZone.setOnDragOver((DragEvent ev) -> {
            Dragboard db2 = ev.getDragboard();
            // Aceptar si trae archivos, URL o texto plano (la URL de la imagen)
            if (db2.hasFiles() || db2.hasUrl() || db2.hasString()) {
                ev.acceptTransferModes(TransferMode.COPY);
            }
            ev.consume();
        });

        coverDropZone.setOnDragDropped((DragEvent ev) -> {
            Dragboard db2 = ev.getDragboard();
            boolean ok = false;

            if (db2.hasFiles() && !db2.getFiles().isEmpty()) {
                // Caso 1: archivo local arrastrado desde el explorador
                loadCoverFile(db2.getFiles().get(0));
                ok = true;
            } else {
                // Caso 2: imagen arrastrada desde el navegador (URL)
                String rawUrl = db2.hasUrl() ? db2.getUrl() : (db2.hasString() ? db2.getString() : null);
                if (rawUrl != null && !rawUrl.isBlank()) {
                    // Limpiar posible prefijo "file://" o saltos de línea
                    String imageUrl = rawUrl.trim().split("\n")[0].trim();
                    // Si es una URL HTTP(S) que parece imagen, descargar
                    if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                        ok = downloadAndLoadUrl(imageUrl);
                    } else if (imageUrl.startsWith("file:")) {
                        // URL de archivo local
                        try {
                            File f = new File(new URI(imageUrl));
                            if (f.exists()) { loadCoverFile(f); ok = true; }
                        } catch (Exception ex) { /* ignorar */ }
                    }
                }
            }

            ev.setDropCompleted(ok);
            ev.consume();
        });
    }

    /**
     * Descarga una imagen desde una URL HTTP(S), la guarda en covers/ con un nombreúnico
     * y la carga como portada. Devuelve true si tuvo éxito.
     */
    private boolean downloadAndLoadUrl(String imageUrl) {
        try {
            // Extensión desde la URL (png, jpg...)
            String lower = imageUrl.toLowerCase();
            String ext = "jpg";
            for (String e : new String[]{"png","jpg","jpeg","webp","gif"}) {
                if (lower.contains("." + e)) { ext = e; break; }
            }
            String fileName = UUID.randomUUID() + "." + ext;
            Path dest = Paths.get(COVERS_DIR, fileName);

            URLConnection conn = new URL(imageUrl).openConnection();
            // Simular un navegador para evitar rechazos 403
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/120.0 Safari/537.36");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(15000);

            // Intentar guardar con ImageIO (soporta webp si hay plugin)
            try (InputStream in = conn.getInputStream()) {
                if (ext.equals("webp")) {
                    // Para webp: convertir a PNG via BufferedImage si está disponible
                    BufferedImage img = ImageIO.read(in);
                    if (img != null) {
                        fileName = UUID.randomUUID() + ".png";
                        dest = Paths.get(COVERS_DIR, fileName);
                        ImageIO.write(img, "png", dest.toFile());
                    } else {
                        // Fallback: copiar bytes directamente
                        Files.copy(conn.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                } else {
                    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // Cargar en la UI
            selectedCoverPath = dest.toAbsolutePath().toString();
            coverPreview.setImage(new Image(dest.toUri().toString(), 90, 130, true, true));
            return true;
        } catch (Exception ex) {
            showAlert("Error de portada",
                "No se pudo descargar la imagen desde el navegador.\n"
                + "Prueba a guardarla primero y arrástrala desde el explorador de archivos.\n"
                + "Detalle: " + ex.getMessage());
            return false;
        }
    }

    private void openFilePicker() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Seleccionar portada");
        fc.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter(
                "Imágenes", "*.png","*.jpg","*.jpeg","*.webp","*.gif"));
        File f = fc.showOpenDialog(coverDropZone.getScene().getWindow());
        if (f != null) loadCoverFile(f);
    }

    private void loadCoverFile(File f) {
        try {
            selectedCoverPath = f.getAbsolutePath();
            coverPreview.setImage(new Image(f.toURI().toString(), 90, 130, true, true));
        } catch (Exception ex) { showAlert("Error", "No se pudo cargar la imagen"); }
    }

    // ═ WIDGET ESTRELLAS ═══════════════════════════════════════════════
    private HBox buildStarWidget(int count, int initial) {
        currentRating = initial;
        starLabels = new Label[count];
        HBox row = new HBox(3); row.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < count; i++) {
            final int idx = i + 1;
            Label star = new Label("☆");
            star.setStyle("-fx-font-size:22; -fx-cursor:hand; -fx-text-fill:#cccccc;");
            star.setOnMouseEntered(e -> highlightStars(starLabels, idx));
            star.setOnMouseExited(e  -> highlightStars(starLabels, currentRating));
            star.setOnMouseClicked(e -> { currentRating = idx; highlightStars(starLabels, currentRating); });
            starLabels[i] = star; row.getChildren().add(star);
        }
        highlightStars(starLabels, initial);
        return row;
    }
    private void highlightStars(Label[] stars, int upTo) {
        for (int i = 0; i < stars.length; i++) {
            boolean on = i < upTo;
            stars[i].setText(on ? "★" : "☆");
            stars[i].setStyle("-fx-font-size:22; -fx-cursor:hand; -fx-text-fill:" + (on ? "#f5c518" : "#cccccc") + ";");
        }
    }

    // ═ CAMPOS DINÁMICOS ═══════════════════════════════════════════════
    private void updateDynamicFields() {
        if (dynamicFieldsBox == null) return;
        dynamicFieldsBox.getChildren().clear();
        String type = typeCombo.getValue();
        if (type == null) return;
        String titleVal = getTitleFieldValue();
        String fs = Theme.fieldStyle();
        String cbStyle = "-fx-text-fill:" + Theme.text() + "; -fx-font-size:12;";

        if (type.contains("Libro")) {
            VBox authorBox = new VBox(4);
            authorBox.getChildren().add(styledLabel("✍️ Autor:"));
            authorTextField = new TextField();
            authorTextField.setStyle(fs);
            authorTextField.setPromptText("Ej: Stephen King");
            if (!titleVal.isEmpty()) { String saved = db.getAuthorForTitle(titleVal); if (saved!=null) authorTextField.setText(saved); }
            authorBox.getChildren().add(authorTextField);
            VBox chapBox = new VBox(4);
            chapBox.getChildren().add(styledLabel("📖 Capítulo leído:"));
            chapterTextField = new TextField(); chapterTextField.setStyle(fs);
            Integer lastChap = titleVal.isEmpty() ? null : db.getLastChapterForTitle(titleVal, type);
            if (lastChap != null) {
                chapterTextField.setPromptText("Sugerencia: " + (lastChap + 1));
                chapterTextField.setStyle(fs + "-fx-prompt-text-fill:" + Theme.C_SUCCESS + ";");
            } else chapterTextField.setPromptText("Ej: 5");
            chapBox.getChildren().add(chapterTextField);
            HBox r = new HBox(15, authorBox, chapBox);
            HBox.setHgrow(authorBox, Priority.ALWAYS); HBox.setHgrow(chapBox, Priority.ALWAYS);
            dynamicFieldsBox.getChildren().add(r);
            finishedCheck = new CheckBox("📘 Libro terminado"); finishedCheck.setStyle(cbStyle);
            dynamicFieldsBox.getChildren().add(finishedCheck);

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
            seasonFinishedCheck = new CheckBox("🌟 Fin de temporada"); seasonFinishedCheck.setStyle(cbStyle);
            seriesFinishedCheck = new CheckBox("🏆 Serie terminada"); seriesFinishedCheck.setStyle(cbStyle);
            dynamicFieldsBox.getChildren().add(new HBox(20, seasonFinishedCheck, seriesFinishedCheck));

        } else if (type.contains("Pel")) {
            VBox dirBox = new VBox(4); dirBox.getChildren().add(styledLabel("🎬 Director:"));
            directorTextField = new TextField(); directorTextField.setStyle(fs);
            directorTextField.setPromptText("Ej: Christopher Nolan");
            if (!titleVal.isEmpty()) { String d = db.getDirectorForTitle(titleVal); if (d!=null) directorTextField.setText(d); }
            dirBox.getChildren().add(directorTextField);
            seenInCinemaCheckBox = new CheckBox("🎫 Vista en el cine"); seenInCinemaCheckBox.setStyle(cbStyle);
            dynamicFieldsBox.getChildren().addAll(dirBox, seenInCinemaCheckBox);

        } else if (type.contains("Teatro")) {
            VBox vBox = new VBox(4); vBox.getChildren().add(styledLabel("🎤 Lugar:"));
            venueTextField = new TextField(); venueTextField.setStyle(fs); venueTextField.setPromptText("Ej: Teatro Nacional");
            vBox.getChildren().add(venueTextField);
            dynamicFieldsBox.getChildren().add(vBox);

        } else if (type.contains("Cómic")) {
            isSingleVolumeCheckBox = new CheckBox("¿Es tomo único?"); isSingleVolumeCheckBox.setStyle(cbStyle);
            VBox volBox = new VBox(4); volBox.getChildren().add(styledLabel("📕 Número de tomo:"));
            comicVolumeTextField = new TextField(); comicVolumeTextField.setStyle(fs); comicVolumeTextField.setPromptText("Ej: 1");
            volBox.getChildren().add(comicVolumeTextField);
            VBox issBox = new VBox(4); issBox.getChildren().add(styledLabel("📖 Número de serie:"));
            comicIssueTextField = new TextField(); comicIssueTextField.setStyle(fs); comicIssueTextField.setPromptText("Ej: 1");
            issBox.getChildren().add(comicIssueTextField);
            HBox comicRow = new HBox(15, volBox, issBox);
            HBox.setHgrow(volBox, Priority.ALWAYS); HBox.setHgrow(issBox, Priority.ALWAYS);
            isSingleVolumeCheckBox.setOnAction(e -> { boolean s=isSingleVolumeCheckBox.isSelected(); comicRow.setVisible(!s); comicRow.setManaged(!s); });
            finishedCheck = new CheckBox("📘 Tomo terminado"); finishedCheck.setStyle(cbStyle);
            seriesFinishedCheck = new CheckBox("🏆 Serie terminada"); seriesFinishedCheck.setStyle(cbStyle);
            dynamicFieldsBox.getChildren().addAll(isSingleVolumeCheckBox, comicRow, new HBox(20, finishedCheck, seriesFinishedCheck));
        }
    }

    // ═ PANEL BÚSQUEDA ═══════════════════════════════════════════════
    private VBox createSearchPanel() {
        VBox panel = new VBox(14); panel.setPadding(new Insets(22));
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
        row1.getChildren().addAll(tBox, btnSearch); panel.getChildren().add(row1);
        HBox row2 = new HBox(12); row2.setAlignment(Pos.BOTTOM_LEFT);
        VBox dBox = new VBox(5); dBox.getChildren().add(styledLabel("Buscar por fecha:"));
        searchDatePicker = new DatePicker(); searchDatePicker.setStyle(Theme.fieldStyle());
        dBox.getChildren().add(searchDatePicker);
        VBox typeBox2 = new VBox(5); typeBox2.getChildren().add(styledLabel("Filtrar por tipo:"));
        searchTypeCombo = new ComboBox<>();
        searchTypeCombo.getItems().addAll("Todo","📚 Libro","🎬 Serie","🎥 Película","🎭 Teatro","💭 Cómic");
        searchTypeCombo.setValue("Todo"); searchTypeCombo.setStyle(Theme.fieldStyle());
        typeBox2.getChildren().add(searchTypeCombo);
        Button btnDate = new Button("📅  Por fecha"); btnDate.setStyle(Theme.btnSecondary()); btnDate.setOnAction(e -> onSearchByDate());
        Button btnAll  = new Button("📋  Ver todos");  btnAll.setStyle(Theme.btnSecondary());  btnAll.setOnAction(e -> loadEntries());
        row2.getChildren().addAll(dBox, typeBox2, btnDate, btnAll); panel.getChildren().add(row2);
        Label hint = new Label("💡 La búsqueda por nombre es parcial e insensible a mayúsculas");
        hint.setStyle("-fx-text-fill:" + Theme.muted() + "; -fx-font-size:11; -fx-font-style:italic;");
        panel.getChildren().add(hint);
        return panel;
    }

    // ═ GUARDAR ═════════════════════════════════════════════════════════
    private void onAddEntry() {
        if (getTitleFieldValue().isEmpty() || typeCombo.getValue() == null) {
            showAlert("Error", "Por favor completa título y tipo"); return;
        }
        String type = typeCombo.getValue();
        Integer chapters = null, season = null, episode = null, comicVolume = null, comicIssue = null;
        String venue = null, author = null, director = null;
        Boolean isSingleVolume = null, seenInCinema = null, finished = null, seasonFin = null, seriesFin = null;

        if (type.contains("Libro")) {
            author = authorTextField != null ? authorTextField.getText().trim() : null;
            if (chapterTextField.getText().trim().isEmpty()) { showAlert("Error", "Ingresa el capítulo"); return; }
            try { chapters = Integer.parseInt(chapterTextField.getText().trim()); if (chapters<=0) throw new NumberFormatException(); }
            catch (NumberFormatException e) { showAlert("Error", "Capítulo debe ser un número mayor a 0"); return; }
            finished = finishedCheck != null && finishedCheck.isSelected();
        } else if (type.contains("Serie")) {
            try { season = Integer.parseInt(seasonTextField.getText().trim()); episode = Integer.parseInt(episodeTextField.getText().trim()); }
            catch (NumberFormatException e) { showAlert("Error", "Temporada y capítulo deben ser números"); return; }
            seasonFin = seasonFinishedCheck != null && seasonFinishedCheck.isSelected();
            seriesFin = seriesFinishedCheck != null && seriesFinishedCheck.isSelected();
        } else if (type.contains("Teatro")) {
            venue = venueTextField != null ? venueTextField.getText().trim() : null;
        } else if (type.contains("Pel")) {
            director     = directorTextField    != null ? directorTextField.getText().trim()   : null;
            seenInCinema = seenInCinemaCheckBox != null && seenInCinemaCheckBox.isSelected();
        } else if (type.contains("Cómic")) {
            isSingleVolume = isSingleVolumeCheckBox != null && isSingleVolumeCheckBox.isSelected();
            if (!Boolean.TRUE.equals(isSingleVolume)) {
                try { comicVolume = Integer.parseInt(comicVolumeTextField.getText().trim()); }
                catch (NumberFormatException e) { showAlert("Error", "Número de tomo debe ser un número"); return; }
                try { comicIssue  = Integer.parseInt(comicIssueTextField.getText().trim()); }
                catch (NumberFormatException e) { showAlert("Error", "Número de serie debe ser un número"); return; }
            }
            finished  = finishedCheck       != null && finishedCheck.isSelected();
            seriesFin = seriesFinishedCheck != null && seriesFinishedCheck.isSelected();
        }

        Entry entry = new Entry(getTitleFieldValue(), type, descriptionArea.getText(),
            datePicker.getValue(), selectedCoverPath, chapters, season, episode);
        entry.setAuthor(author); entry.setVenue(venue); entry.setIsSingleVolume(isSingleVolume);
        entry.setComicVolume(comicVolume); entry.setComicIssue(comicIssue);
        entry.setDirector(director); entry.setSeenInCinema(seenInCinema);
        entry.setRating(currentRating > 0 ? currentRating : null);
        entry.setFinished(finished); entry.setSeasonFinished(seasonFin); entry.setSeriesFinished(seriesFin);
        db.addEntry(entry);
        clearForm(); loadEntries();
        showAlert("Éxito", "Entrada registrada correctamente ✨");
    }

    // ═ TARJETAS ════════════════════════════════════════════════════════
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
        VBox card = new VBox(8); card.setPadding(new Insets(14)); card.setStyle(Theme.cardStyle());
        HBox head = new HBox(14); head.setAlignment(Pos.TOP_LEFT);
        if (entry.getCoverPath() != null) {
            try {
                ImageView img = new ImageView(new Image(
                    new File(entry.getCoverPath()).toURI().toString(), 75, 110, true, true));
                img.setStyle("-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),6,0,0,2);");
                head.getChildren().add(img);
            } catch (Exception ex) { head.getChildren().add(placeholderIcon()); }
        } else head.getChildren().add(placeholderIcon());

        VBox info = new VBox(5);
        Label typeL  = new Label(entry.getType()); typeL.setStyle("-fx-font-size:11; -fx-text-fill:" + Theme.C_ACCENT2 + "; -fx-font-weight:bold;");
        Label titleL = new Label(entry.getTitle()); titleL.setStyle("-fx-font-size:15; -fx-font-weight:bold; -fx-text-fill:" + Theme.text() + ";");
        info.getChildren().addAll(typeL, titleL);
        String t = entry.getType();
        if (t.contains("Libro")) {
            if (entry.getAuthor()!=null && !entry.getAuthor().isBlank()) info.getChildren().add(chip("✍️ " + entry.getAuthor()));
            if (entry.getChapters()!=null) info.getChildren().add(chip("📖 Cap. " + entry.getChapters()));
            if (Boolean.TRUE.equals(entry.getFinished()))                info.getChildren().add(chipGreen("✅ Libro terminado"));
        } else if (t.contains("Serie")) {
            if (entry.getSeason()!=null) info.getChildren().add(chip("T" + entry.getSeason() + " E" + entry.getEpisode()));
            if (Boolean.TRUE.equals(entry.getSeasonFinished()))          info.getChildren().add(chipGreen("🌟 Fin temporada"));
            if (Boolean.TRUE.equals(entry.getSeriesFinished()))          info.getChildren().add(chipGreen("🏆 Serie terminada"));
        } else if (t.contains("Pel")) {
            if (entry.getDirector()!=null && !entry.getDirector().isBlank()) info.getChildren().add(chip("🎬 " + entry.getDirector()));
            if (Boolean.TRUE.equals(entry.getSeenInCinema()))               info.getChildren().add(chip("🎫 Vista en cine"));
        } else if (t.contains("Teatro") && entry.getVenue()!=null) {
            info.getChildren().add(chip("🎭 " + entry.getVenue()));
        } else if (t.contains("Cómic")) {
            if (Boolean.TRUE.equals(entry.getIsSingleVolume())) info.getChildren().add(chip("Tomo único"));
            else {
                String ci = "";
                if (entry.getComicVolume()!=null) ci += "Tomo " + entry.getComicVolume();
                if (entry.getComicIssue() !=null) ci += (ci.isEmpty()?"":"  ") + "#" + entry.getComicIssue();
                if (!ci.isEmpty()) info.getChildren().add(chip("📕 " + ci));
            }
            if (Boolean.TRUE.equals(entry.getFinished()))       info.getChildren().add(chipGreen("✅ Tomo terminado"));
            if (Boolean.TRUE.equals(entry.getSeriesFinished())) info.getChildren().add(chipGreen("🏆 Serie terminada"));
        }
        info.getChildren().add(chip("📅 " + entry.getDate()));
        if (entry.getRating()!=null && entry.getRating()>0) {
            HBox stars = new HBox(1); stars.setAlignment(Pos.CENTER_LEFT);
            for (int i = 1; i <= 10; i++) {
                Label s = new Label(i<=entry.getRating()?"★":"☆");
                s.setStyle("-fx-font-size:13; -fx-text-fill:" + (i<=entry.getRating()?"#f5c518":"#cccccc") + ";");
                stars.getChildren().add(s);
            }
            Label ratingTxt = new Label(" " + entry.getRating() + "/10");
            ratingTxt.setStyle("-fx-font-size:11; -fx-text-fill:" + Theme.muted() + "; -fx-font-weight:bold;");
            stars.getChildren().add(ratingTxt);
            info.getChildren().add(stars);
        }
        HBox.setHgrow(info, Priority.ALWAYS); head.getChildren().add(info);
        VBox btns = new VBox(6); btns.setAlignment(Pos.TOP_RIGHT);
        Button editBtn = iconBtn("✏️"); editBtn.setOnAction(e -> openEditDialog(entry));
        Button delBtn  = iconBtn("🗑️");  delBtn.setOnAction(e -> { db.deleteEntry(entry.getId()); loadEntries(); });
        btns.getChildren().addAll(editBtn, delBtn); head.getChildren().add(btns);
        card.getChildren().add(head);
        if (entry.getDescription()!=null && !entry.getDescription().isBlank()) {
            TextArea desc = new TextArea(entry.getDescription());
            desc.setWrapText(true); desc.setPrefRowCount(2); desc.setEditable(false);
            desc.setStyle("-fx-control-inner-background:" + Theme.input() +
                "; -fx-text-fill:" + Theme.muted() + "; -fx-font-size:11; -fx-background-radius:6;");
            card.getChildren().add(desc);
        }
        return card;
    }

    // ═ EDICIÓN ═════════════════════════════════════════════════════════
    private void openEditDialog(Entry entry) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("✏️ Editar Registro");
        dialog.setHeaderText("Modifica los datos del registro");
        dialog.getDialogPane().setStyle("-fx-background-color:" + Theme.surface() + ";");
        VBox form = new VBox(10); form.setPadding(new Insets(18));
        form.setStyle("-fx-background-color:" + Theme.surface() + ";");
        String fs = Theme.fieldStyle();
        String cbStyle = "-fx-text-fill:" + Theme.text() + "; -fx-font-size:12;";
        TextField eTitleF = styledField(entry.getTitle());
        ComboBox<String> eTypeC = new ComboBox<>();
        eTypeC.getItems().addAll("📚 Libro","🎬 Serie","🎥 Película","🎭 Teatro","💭 Cómic");
        eTypeC.setValue(entry.getType()); eTypeC.setStyle(fs);
        TextArea eDescA = new TextArea(entry.getDescription()!=null?entry.getDescription():"");
        eDescA.setWrapText(true); eDescA.setPrefRowCount(3); eDescA.setStyle(fs);
        DatePicker eDateP = new DatePicker(entry.getDate()); eDateP.setStyle(fs);
        int[] editRating = { entry.getRating()!=null ? entry.getRating() : 0 };
        Label[] editStars = new Label[10];
        HBox editStarsRow = new HBox(3); editStarsRow.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < 10; i++) {
            final int idx = i + 1;
            Label star = new Label("☆"); star.setStyle("-fx-font-size:20; -fx-cursor:hand; -fx-text-fill:#cccccc;");
            star.setOnMouseEntered(e -> highlightStars(editStars, idx));
            star.setOnMouseExited(e  -> highlightStars(editStars, editRating[0]));
            star.setOnMouseClicked(e -> { editRating[0] = idx; highlightStars(editStars, editRating[0]); });
            editStars[i] = star; editStarsRow.getChildren().add(star);
        }
        highlightStars(editStars, editRating[0]);
        TextField eChapF = styledField(entry.getChapters()    !=null?entry.getChapters().toString()   :"");
        TextField eAuthF = styledField(entry.getAuthor()      !=null?entry.getAuthor()                 :"");
        TextField eSeaF  = styledField(entry.getSeason()      !=null?entry.getSeason().toString()      :"");
        TextField eEpF   = styledField(entry.getEpisode()     !=null?entry.getEpisode().toString()      :"");
        TextField eVenF  = styledField(entry.getVenue()       !=null?entry.getVenue()                  :"");
        TextField eDirF  = styledField(entry.getDirector()    !=null?entry.getDirector()               :"");
        CheckBox eCinC   = new CheckBox("🎫 Vista en el cine"); eCinC.setStyle(cbStyle); eCinC.setSelected(Boolean.TRUE.equals(entry.getSeenInCinema()));
        CheckBox eSingC  = new CheckBox("¿Es tomo único?");       eSingC.setStyle(cbStyle); eSingC.setSelected(Boolean.TRUE.equals(entry.getIsSingleVolume()));
        TextField eVolF  = styledField(entry.getComicVolume() !=null?entry.getComicVolume().toString() :"");
        TextField eIssF  = styledField(entry.getComicIssue()  !=null?entry.getComicIssue().toString()  :"");
        CheckBox eFinC   = new CheckBox("✅ Terminado");       eFinC.setStyle(cbStyle);    eFinC.setSelected(Boolean.TRUE.equals(entry.getFinished()));
        CheckBox eSeaFinC= new CheckBox("🌟 Fin temporada");  eSeaFinC.setStyle(cbStyle); eSeaFinC.setSelected(Boolean.TRUE.equals(entry.getSeasonFinished()));
        CheckBox eSerFinC= new CheckBox("🏆 Serie terminada"); eSerFinC.setStyle(cbStyle); eSerFinC.setSelected(Boolean.TRUE.equals(entry.getSeriesFinished()));
        VBox dynEdit = new VBox(8);
        Runnable refreshDyn = () -> {
            dynEdit.getChildren().clear();
            String ty = eTypeC.getValue(); if (ty==null) return;
            if (ty.contains("Libro"))       dynEdit.getChildren().addAll(styledLabel("✍️ Autor:"),eAuthF,styledLabel("📖 Capítulo:"),eChapF,eFinC);
            else if (ty.contains("Serie"))  dynEdit.getChildren().addAll(styledLabel("Temporada:"),eSeaF,styledLabel("Capítulo:"),eEpF,new HBox(20,eSeaFinC,eSerFinC));
            else if (ty.contains("Pel"))    dynEdit.getChildren().addAll(styledLabel("Director:"),eDirF,eCinC);
            else if (ty.contains("Teatro")) dynEdit.getChildren().addAll(styledLabel("Lugar:"),eVenF);
            else if (ty.contains("Cómic"))  dynEdit.getChildren().addAll(eSingC,styledLabel("Nº tomo:"),eVolF,styledLabel("Nº serie:"),eIssF,new HBox(20,eFinC,eSerFinC));
        };
        refreshDyn.run(); eTypeC.setOnAction(e -> refreshDyn.run());
        form.getChildren().addAll(styledLabel("Título:"),eTitleF,styledLabel("Tipo:"),eTypeC,
            styledLabel("Descripción:"),eDescA,styledLabel("Fecha:"),eDateP,
            styledLabel("⭐ Valoración:"),editStarsRow,dynEdit);
        ScrollPane sp = new ScrollPane(form); sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:" + Theme.surface() + "; -fx-background:" + Theme.surface() + ";");
        sp.setPrefHeight(520);
        dialog.getDialogPane().setContent(sp);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get()==ButtonType.OK) {
            entry.setTitle(eTitleF.getText()); entry.setType(eTypeC.getValue());
            entry.setDescription(eDescA.getText()); entry.setDate(eDateP.getValue());
            entry.setRating(editRating[0]>0 ? editRating[0] : null);
            String ty = eTypeC.getValue();
            try {
                if (ty.contains("Libro"))       { entry.setAuthor(eAuthF.getText()); entry.setChapters(Integer.parseInt(eChapF.getText().trim())); entry.setFinished(eFinC.isSelected()); }
                else if (ty.contains("Serie"))  { entry.setSeason(Integer.parseInt(eSeaF.getText().trim())); entry.setEpisode(Integer.parseInt(eEpF.getText().trim())); entry.setSeasonFinished(eSeaFinC.isSelected()); entry.setSeriesFinished(eSerFinC.isSelected()); }
                else if (ty.contains("Teatro")) { entry.setVenue(eVenF.getText()); }
                else if (ty.contains("Pel"))    { entry.setDirector(eDirF.getText()); entry.setSeenInCinema(eCinC.isSelected()); }
                else if (ty.contains("Cómic"))  { entry.setIsSingleVolume(eSingC.isSelected()); if (!eSingC.isSelected()) { entry.setComicVolume(Integer.parseInt(eVolF.getText().trim())); entry.setComicIssue(Integer.parseInt(eIssF.getText().trim())); } entry.setFinished(eFinC.isSelected()); entry.setSeriesFinished(eSerFinC.isSelected()); }
            } catch (NumberFormatException ex) { showAlert("Error", "Revisa los campos numéricos"); return; }
            db.updateEntry(entry); loadEntries();
            showAlert("Éxito", "Registro actualizado ✨");
        }
    }

    // ═ BÚSQUEDA ═════════════════════════════════════════════════════════
    private void onSearchByTitle() {
        String term = searchTitleField.getText().trim();
        if (term.isEmpty()) { showAlert("Info", "Introduce un término de búsqueda"); return; }
        String sel = searchTypeCombo.getValue();
        displayResults("Todo".equals(sel)?db.searchByTitle(term):db.searchByTitleAndType(term,sel),"búsqueda '"+term+"'");
    }
    private void onSearchByDate() {
        LocalDate d = searchDatePicker.getValue();
        if (d==null) { showAlert("Info", "Selecciona una fecha"); return; }
        displayResults(db.searchByDate(d),"fecha "+d);
    }
    private void displayResults(List<Entry> entries, String desc) {
        entriesContainer.getChildren().clear();
        if (entries.isEmpty()) {
            Label l = new Label("📭 Sin resultados para "+desc);
            l.setStyle("-fx-font-size:14; -fx-text-fill:"+Theme.muted()+";");
            entriesContainer.getChildren().add(l); return;
        }
        Label l = new Label("📊 "+entries.size()+" resultado(s) para "+desc);
        l.setStyle("-fx-font-size:13; -fx-font-weight:bold; -fx-text-fill:"+Theme.C_SUCCESS+";");
        entriesContainer.getChildren().add(l);
        for (Entry e : entries) entriesContainer.getChildren().add(createEntryCard(e));
    }

    // ═ HELPERS ═════════════════════════════════════════════════════════
    private void updateTitleSuggestions() {
        if (typeCombo==null || titleField==null) return;
        List<String> s = db.getTitleSuggestions(typeCombo.getValue()!=null?typeCombo.getValue():"");
        titleField.setItems(FXCollections.observableArrayList(s));
    }
    private void clearForm() {
        titleField.setValue(null); titleTextField.clear(); descriptionArea.clear();
        typeCombo.setValue("📚 Libro"); datePicker.setValue(LocalDate.now());
        coverPreview.setImage(null); selectedCoverPath = null;
        currentRating = 0;
        if (starLabels != null) highlightStars(starLabels, 0);
        updateDynamicFields();
    }
    private String getTitleFieldValue() {
        String v = titleField.getValue();
        return v!=null?v.trim():(titleTextField!=null?titleTextField.getText().trim():"");
    }
    private Label styledLabel(String text) { Label l=new Label(text); l.setStyle(Theme.labelBold()); return l; }
    private TextField styledField(String text) { TextField f=new TextField(text); f.setStyle(Theme.fieldStyle()); return f; }
    private Label chip(String text) { Label l=new Label(text); l.setStyle(Theme.chipStyle()); return l; }
    private Label chipGreen(String text) {
        Label l=new Label(text);
        l.setStyle("-fx-background-color:#d4edda; -fx-text-fill:#1a7a3a; -fx-font-size:11; -fx-padding:2 8; -fx-background-radius:10;");
        return l;
    }
    private Label placeholderIcon() { Label l=new Label("📷"); l.setStyle("-fx-font-size:36; -fx-text-fill:"+Theme.muted()+";"); return l; }
    private Button iconBtn(String icon) {
        Button b=new Button(icon); b.setStyle("-fx-background-color:transparent; -fx-font-size:15; -fx-cursor:hand; -fx-padding:4 8;"); return b;
    }
    private void showAlert(String title, String msg) {
        Alert a=new Alert(Alert.AlertType.INFORMATION); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.getDialogPane().setStyle("-fx-background-color:"+Theme.surface()+"; -fx-font-size:13;"); a.showAndWait();
    }
    public static void main(String[] args) { launch(args); }
}
