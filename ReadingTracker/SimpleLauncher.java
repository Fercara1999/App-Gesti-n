import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SimpleLauncher extends Application {
    private Database db;
    private VBox entriesContainer;
    private TextArea descriptionArea;
    private ComboBox<String> typeCombo;
    private TextField titleField;
    private DatePicker datePicker;
    private ImageView coverPreview;
    private String selectedCoverPath = null;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        db = new Database();
        
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        // Panel de entrada
        VBox inputPanel = createInputPanel();
        ScrollPane scrollInput = new ScrollPane(inputPanel);
        scrollInput.setPrefWidth(350);

        // Panel de entradas registradas
        entriesContainer = new VBox(10);
        entriesContainer.setPadding(new Insets(10));
        ScrollPane scrollEntries = new ScrollPane(entriesContainer);

        // Layout principal
        HBox main = new HBox(20);
        main.getChildren().addAll(scrollInput, scrollEntries);
        HBox.setHgrow(scrollEntries, javafx.scene.layout.Priority.ALWAYS);

        root.setCenter(main);

        Scene scene = new Scene(root, 1000, 700);
        stage.setTitle("📚 Registro de Lectura y Visualización");
        stage.setScene(scene);
        stage.show();

        loadEntries();
    }

    private VBox createInputPanel() {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-border-color: #e0e0e0; -fx-padding: 15; -fx-border-radius: 5;");

        Label title = new Label("Nuevo Registro");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        titleField = new TextField();
        titleField.setPromptText("Título del libro/serie");

        typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("📚 Libro", "🎬 Serie");
        typeCombo.setPromptText("Tipo");

        datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now());

        descriptionArea = new TextArea();
        descriptionArea.setPromptText("¿Qué has leído/visto hoy?");
        descriptionArea.setPrefRowCount(5);
        descriptionArea.setWrapText(true);

        coverPreview = new ImageView();
        coverPreview.setFitHeight(150);
        coverPreview.setFitWidth(100);
        coverPreview.setStyle("-fx-border-color: #cccccc; -fx-border-width: 2; -fx-border-style: dashed;");
        
        Label dragLabel = new Label("Arrastra una foto aquí");
        dragLabel.setStyle("-fx-text-alignment: center; -fx-text-fill: #999;");
        dragLabel.setPrefWidth(100);

        VBox coverBox = new VBox(5);
        coverBox.getChildren().addAll(coverPreview, dragLabel);
        coverBox.setStyle("-fx-alignment: center;");

        setupDragAndDrop(coverPreview);

        Button addBtn = new Button("✅ Agregar Entrada");
        addBtn.setStyle("-fx-font-size: 14; -fx-padding: 10;");
        addBtn.setOnAction(e -> addEntry());

        panel.getChildren().addAll(
                title,
                new Separator(),
                new Label("Título:"), titleField,
                new Label("Tipo:"), typeCombo,
                new Label("Fecha:"), datePicker,
                new Label("Notas:"), descriptionArea,
                new Label("Portada:"), coverBox,
                addBtn
        );

        return panel;
    }

    private void setupDragAndDrop(ImageView imageView) {
        imageView.setOnDragOver(event -> {
            event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            event.consume();
        });

        imageView.setOnDragDropped(event -> {
            List<File> files = event.getDragboard().getFiles();
            if (!files.isEmpty()) {
                File file = files.get(0);
                selectedCoverPath = file.getAbsolutePath();
                try {
                    Image image = new Image(new FileInputStream(file), 100, 150, true, true);
                    imageView.setImage(image);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            event.consume();
        });
    }

    private void addEntry() {
        if (titleField.getText().isEmpty() || typeCombo.getValue() == null) {
            showAlert("Error", "Por favor completa título y tipo");
            return;
        }

        Entry entry = new Entry(
                titleField.getText(),
                typeCombo.getValue(),
                descriptionArea.getText(),
                datePicker.getValue(),
                selectedCoverPath
        );

        db.addEntry(entry);
        clearForm();
        loadEntries();
        showAlert("✅ Éxito", "Entrada registrada correctamente");
    }

    private void loadEntries() {
        entriesContainer.getChildren().clear();
        List<Entry> entries = db.getAllEntries();

        if (entries.isEmpty()) {
            Label emptyLabel = new Label("📭 No hay entradas aún");
            emptyLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 14;");
            entriesContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Entry entry : entries) {
            VBox card = createEntryCard(entry);
            entriesContainer.getChildren().add(card);
        }
    }

    private VBox createEntryCard(Entry entry) {
        VBox card = new VBox(10);
        card.setStyle("-fx-border-color: #ddd; -fx-border-width: 1; -fx-padding: 15; -fx-border-radius: 5;");

        HBox header = new HBox(15);

        if (entry.getCoverPath() != null && !entry.getCoverPath().isEmpty()) {
            try {
                ImageView cover = new ImageView(new Image(new FileInputStream(entry.getCoverPath()), 60, 90, true, true));
                header.getChildren().add(cover);
            } catch (Exception e) {
                Label placeholder = new Label("📷");
                placeholder.setStyle("-fx-font-size: 30;");
                header.getChildren().add(placeholder);
            }
        }

        VBox info = new VBox(5);
        Label typeLabel = new Label(entry.getType());
        typeLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold;");
        Label titleLabel = new Label(entry.getTitle());
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        Label dateLabel = new Label("📅 " + entry.getDate());
        dateLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");

        info.getChildren().addAll(typeLabel, titleLabel, dateLabel);
        header.getChildren().add(info);

        card.getChildren().add(header);

        if (entry.getDescription() != null && !entry.getDescription().isEmpty()) {
            Label descLabel = new Label(entry.getDescription());
            descLabel.setWrapText(true);
            descLabel.setStyle("-fx-font-size: 11;");
            card.getChildren().add(descLabel);
        }

        Button deleteBtn = new Button("🗑️ Eliminar");
        deleteBtn.setStyle("-fx-padding: 5; -fx-font-size: 11;");
        deleteBtn.setOnAction(e -> {
            db.deleteEntry(entry.getId());
            loadEntries();
        });
        card.getChildren().add(deleteBtn);

        return card;
    }

    private void clearForm() {
        titleField.clear();
        descriptionArea.clear();
        typeCombo.setValue(null);
        datePicker.setValue(LocalDate.now());
        coverPreview.setImage(null);
        selectedCoverPath = null;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    static class Entry {
        private int id;
        private String title;
        private String type;
        private String description;
        private LocalDate date;
        private String coverPath;

        Entry(String title, String type, String description, LocalDate date, String coverPath) {
            this.title = title;
            this.type = type;
            this.description = description;
            this.date = date;
            this.coverPath = coverPath;
        }

        int getId() { return id; }
        void setId(int id) { this.id = id; }
        String getTitle() { return title; }
        String getType() { return type; }
        String getDescription() { return description; }
        LocalDate getDate() { return date; }
        String getCoverPath() { return coverPath; }
    }

    static class Database {
        private static final String DB_URL = "jdbc:sqlite:reading_tracker.db";

        Database() {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
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
                        "cover_path TEXT)";
                stmt.execute(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        void addEntry(Entry entry) {
            String sql = "INSERT INTO entries(title, type, description, date, cover_path) VALUES(?,?,?,?,?)";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, entry.title);
                pstmt.setString(2, entry.type);
                pstmt.setString(3, entry.description);
                pstmt.setDate(4, Date.valueOf(entry.date));
                pstmt.setString(5, entry.coverPath);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        List<Entry> getAllEntries() {
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
                            rs.getString("cover_path")
                    );
                    entry.setId(rs.getInt("id"));
                    entries.add(entry);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return entries;
        }

        void deleteEntry(int id) {
            String sql = "DELETE FROM entries WHERE id = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
