package com.readingtracker;

import com.readingtracker.models.Database;
import com.readingtracker.models.Entry;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class StatsPanel {

    private static final String C_BG      = "#1e1e2e";
    private static final String C_SURFACE = "#2a2a3e";
    private static final String C_CARD    = "#313145";
    private static final String C_ACCENT  = "#7c6af7";
    private static final String C_ACCENT2 = "#56c8d8";
    private static final String C_TEXT    = "#e0e0f0";
    private static final String C_MUTED   = "#9090aa";
    private static final String C_SUCCESS = "#4caf6e";

    private static final String CHART_STYLE =
            "-fx-background-color:" + C_CARD + "; -fx-background-radius:12;" +
            " -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.4),10,0,0,3);" +
            " -fx-padding:12;";

    private final Database db;
    private VBox root;
    private ComboBox<String> periodCombo;

    public StatsPanel(Database db) {
        this.db = db;
        this.root = build();
    }

    public VBox getRoot() { return root; }

    // -----------------------------------------------------------------------
    private VBox build() {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color:" + C_BG + ";");

        // Header con selector de periodo
        HBox header = new HBox(16);
        header.setPadding(new Insets(20, 24, 14, 24));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color:" + C_SURFACE + ";" +
                " -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),6,0,0,2);");

        Label title = new Label("📊 Estadísticas");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setTextFill(Color.web(C_ACCENT));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label periodLabel = new Label("Periodo:");
        periodLabel.setStyle("-fx-text-fill:" + C_MUTED + "; -fx-font-size:13;");

        periodCombo = new ComboBox<>();
        periodCombo.getItems().addAll("Esta semana", "Este mes", "Este año", "Todo el tiempo");
        periodCombo.setValue("Este mes");
        periodCombo.setStyle("-fx-background-color:#3b3b55; -fx-text-fill:" + C_TEXT +
                "; -fx-border-color:#5050aa; -fx-border-radius:6; -fx-background-radius:6;");

        Button refreshBtn = new Button("🔄 Actualizar");
        refreshBtn.setStyle("-fx-background-color:" + C_ACCENT +
                "; -fx-text-fill:white; -fx-padding:8 18; -fx-background-radius:8; -fx-cursor:hand;");
        refreshBtn.setOnAction(e -> refresh());
        periodCombo.setOnAction(e -> refresh());

        header.getChildren().addAll(title, spacer, periodLabel, periodCombo, refreshBtn);
        panel.getChildren().add(header);

        // Scroll con contenido
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:" + C_BG + "; -fx-background:" + C_BG + ";");

        VBox content = new VBox(24);
        content.setPadding(new Insets(22));
        content.setStyle("-fx-background-color:" + C_BG + ";");
        content.setId("stats-content");
        scroll.setContent(content);

        panel.getChildren().add(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        refresh();
        return panel;
    }

    // -----------------------------------------------------------------------
    public void refresh() {
        VBox content = (VBox) ((ScrollPane) root.getChildren().get(1)).getContent();
        content.getChildren().clear();

        List<Entry> all = db.getAllEntries();
        LocalDate now  = LocalDate.now();
        LocalDate from = switch (periodCombo.getValue()) {
            case "Esta semana" -> now.minusDays(now.getDayOfWeek().getValue() - 1);
            case "Este mes"    -> now.withDayOfMonth(1);
            case "Este año"   -> now.withDayOfYear(1);
            default            -> LocalDate.of(2000, 1, 1);
        };

        List<Entry> filtered = all.stream()
                .filter(e -> !e.getDate().isBefore(from))
                .collect(Collectors.toList());

        // ── KPI cards ─────────────────────────────────────────────────
        HBox kpiRow = new HBox(16);
        kpiRow.setAlignment(Pos.CENTER_LEFT);

        long totalPeriod = filtered.size();
        long totalAll    = all.size();
        long libros   = filtered.stream().filter(e -> e.getType().contains("Libro")).count();
        long peliculas = filtered.stream().filter(e -> e.getType().contains("Pel")).count();
        long series   = filtered.stream().filter(e -> e.getType().contains("Serie")).count();
        long comics   = filtered.stream().filter(e -> e.getType().contains("mic")).count();

        kpiRow.getChildren().addAll(
                kpiCard("📚", String.valueOf(totalPeriod), "registros en periodo", C_ACCENT),
                kpiCard("📋", String.valueOf(totalAll),    "registros totales",   C_ACCENT2),
                kpiCard("📖", String.valueOf(libros),      "libros",              C_SUCCESS),
                kpiCard("🎥", String.valueOf(series + peliculas), "series + películas", "#f0a050"),
                kpiCard("💭", String.valueOf(comics),      "cómics",             "#d060d0")
        );
        content.getChildren().add(kpiRow);

        if (filtered.isEmpty()) {
            Label empty = new Label("📭 Sin registros en el periodo seleccionado");
            empty.setStyle("-fx-font-size:14; -fx-text-fill:" + C_MUTED + ";");
            content.getChildren().add(empty);
            return;
        }

        // ── Fila 1: Barras por tipo | Dona distribución ──────────────────
        HBox row1 = new HBox(20);
        row1.getChildren().addAll(
                wrapChart("Registros por tipo", buildTypeBarChart(filtered)),
                wrapChart("Distribución (%)",   buildPieChart(filtered))
        );
        content.getChildren().add(row1);

        // ── Fila 2: Línea actividad temporal ───────────────────────────
        content.getChildren().add(
                wrapChart("Actividad en el tiempo", buildActivityLineChart(filtered, from, now)));

        // ── Fila 3: Barras por día de semana | Por mes (si > 1 mes) ──────
        HBox row3 = new HBox(20);
        row3.getChildren().add(wrapChart("Actividad por día de la semana", buildWeekdayChart(filtered)));
        if (!"Esta semana".equals(periodCombo.getValue()) && !"Este mes".equals(periodCombo.getValue())) {
            row3.getChildren().add(wrapChart("Registros por mes", buildMonthlyBarChart(filtered)));
        }
        content.getChildren().add(row3);
    }

    // -----------------------------------------------------------------------
    // CHARTS
    // -----------------------------------------------------------------------

    private BarChart<String, Number> buildTypeBarChart(List<Entry> entries) {
        CategoryAxis xAxis = styledCategoryAxis();
        NumberAxis   yAxis = styledNumberAxis("Registros");
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setStyle("-fx-background-color:transparent;");
        chart.setBarGap(4); chart.setCategoryGap(12);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<String, Long> byType = entries.stream().collect(
                Collectors.groupingBy(e -> typeEmoji(e.getType()), Collectors.counting()));
        byType.entrySet().stream()
                .sorted(Map.Entry.<String,Long>comparingByValue().reversed())
                .forEach(en -> series.getData().add(new XYChart.Data<>(en.getKey(), en.getValue())));
        chart.getData().add(series);
        styleBarChart(chart);
        return chart;
    }

    private PieChart buildPieChart(List<Entry> entries) {
        Map<String, Long> byType = entries.stream().collect(
                Collectors.groupingBy(e -> typeEmoji(e.getType()), Collectors.counting()));
        var data = FXCollections.observableArrayList(
                byType.entrySet().stream()
                        .map(en -> new PieChart.Data(en.getKey() + " (" + en.getValue() + ")", en.getValue()))
                        .collect(Collectors.toList()));
        PieChart pie = new PieChart(data);
        pie.setLabelsVisible(true);
        pie.setLegendVisible(false);
        pie.setStyle("-fx-background-color:transparent;");
        pie.setClockwise(true);
        return pie;
    }

    private LineChart<String, Number> buildActivityLineChart(List<Entry> entries, LocalDate from, LocalDate to) {
        CategoryAxis xAxis = styledCategoryAxis();
        NumberAxis   yAxis = styledNumberAxis("Entradas");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setStyle("-fx-background-color:transparent;");
        chart.setLegendVisible(false);
        chart.setCreateSymbols(true);
        chart.setAnimated(false);

        boolean byDay = !to.minusDays(60).isAfter(from);
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        if (byDay) {
            Map<LocalDate, Long> map = entries.stream()
                    .collect(Collectors.groupingBy(Entry::getDate, Collectors.counting()));
            from.datesUntil(to.plusDays(1)).forEach(d -> {
                if (map.containsKey(d)) s.getData().add(new XYChart.Data<>(d.toString(), map.getOrDefault(d, 0L)));
            });
        } else {
            Map<String, Long> map = entries.stream().collect(
                    Collectors.groupingBy(e -> e.getDate().getYear() + "-" +
                            String.format("%02d", e.getDate().getMonthValue()), Collectors.counting()));
            new TreeMap<>(map).forEach((k, v) -> s.getData().add(new XYChart.Data<>(k, v)));
        }
        chart.getData().add(s);
        return chart;
    }

    private BarChart<String, Number> buildWeekdayChart(List<Entry> entries) {
        CategoryAxis xAxis = styledCategoryAxis();
        NumberAxis   yAxis = styledNumberAxis("Entradas");
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false); chart.setStyle("-fx-background-color:transparent;");
        chart.setBarGap(3); chart.setCategoryGap(10);

        String[] days = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
        Map<Integer, Long> map = entries.stream().collect(
                Collectors.groupingBy(e -> e.getDate().getDayOfWeek().getValue(), Collectors.counting()));
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        for (int i = 1; i <= 7; i++) s.getData().add(new XYChart.Data<>(days[i-1], map.getOrDefault(i, 0L)));
        chart.getData().add(s);
        styleBarChart(chart);
        return chart;
    }

    private BarChart<String, Number> buildMonthlyBarChart(List<Entry> entries) {
        CategoryAxis xAxis = styledCategoryAxis();
        NumberAxis   yAxis = styledNumberAxis("Entradas");
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false); chart.setStyle("-fx-background-color:transparent;");
        chart.setBarGap(3); chart.setCategoryGap(8);

        Map<String, Long> map = new TreeMap<>(entries.stream().collect(
                Collectors.groupingBy(e -> {
                    java.time.Month m = e.getDate().getMonth();
                    return e.getDate().getYear() + " " + m.getDisplayName(TextStyle.SHORT, new Locale("es"));
                }, Collectors.counting())));
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        map.forEach((k, v) -> s.getData().add(new XYChart.Data<>(k, v)));
        chart.getData().add(s);
        styleBarChart(chart);
        return chart;
    }

    // -----------------------------------------------------------------------
    // HELPERS UI
    // -----------------------------------------------------------------------

    private VBox wrapChart(String title, javafx.scene.Node chart) {
        VBox box = new VBox(8);
        box.setStyle(CHART_STYLE);
        box.setPrefWidth(500);
        HBox.setHgrow(box, Priority.ALWAYS);

        Label lbl = new Label(title);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 13));
        lbl.setTextFill(Color.web(C_ACCENT2));

        box.getChildren().addAll(lbl, chart);
        return box;
    }

    private VBox kpiCard(String icon, String value, String label, String color) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16, 24, 16, 24));
        card.setStyle("-fx-background-color:" + C_CARD +
                "; -fx-background-radius:14;" +
                " -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.35),8,0,0,2);" +
                " -fx-border-color:" + color + "; -fx-border-radius:14; -fx-border-width:1.5;");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label ico = new Label(icon);
        ico.setFont(Font.font(28));
        Label val = new Label(value);
        val.setFont(Font.font("System", FontWeight.BOLD, 26));
        val.setTextFill(Color.web(color));
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill:" + C_MUTED + "; -fx-font-size:11;");

        card.getChildren().addAll(ico, val, lbl);
        return card;
    }

    private CategoryAxis styledCategoryAxis() {
        CategoryAxis a = new CategoryAxis();
        a.setTickLabelFill(Color.web(C_MUTED));
        a.setStyle("-fx-tick-label-fill:" + C_MUTED + ";");
        return a;
    }

    private NumberAxis styledNumberAxis(String label) {
        NumberAxis a = new NumberAxis();
        a.setLabel(label);
        a.setTickLabelFill(Color.web(C_MUTED));
        a.setMinorTickVisible(false);
        a.setStyle("-fx-tick-label-fill:" + C_MUTED + ";");
        return a;
    }

    private void styleBarChart(BarChart<String, Number> chart) {
        chart.setAnimated(false);
        chart.applyCss();
    }

    private String typeEmoji(String type) {
        if (type == null) return "Otro";
        if (type.contains("Libro"))  return "📚 Libro";
        if (type.contains("Serie"))  return "🎬 Serie";
        if (type.contains("Pel"))    return "🎥 Película";
        if (type.contains("Teatro")) return "🎭 Teatro";
        if (type.contains("mic"))    return "💭 Cómic";
        return type;
    }
}
