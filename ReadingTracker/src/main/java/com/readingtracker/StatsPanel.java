package com.readingtracker;

import com.readingtracker.models.Database;
import com.readingtracker.models.Entry;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class StatsPanel {

    // Colores de acento fijos
    private static final Color ACCENT  = Color.web(Theme.C_ACCENT);
    private static final Color ACCENT2 = Color.web(Theme.C_ACCENT2);
    private static final Color SUCCESS = Color.web(Theme.C_SUCCESS);
    private static final Color WARN    = Color.web(Theme.C_WARN);
    private static final Color PURPLE  = Color.web(Theme.C_PURPLE);

    private static final Color[] BAR_COLORS = {
        Color.web("#6c5ce7"), Color.web("#00b4cc"), Color.web("#27ae60"),
        Color.web("#e67e22"), Color.web("#9b59b6"), Color.web("#e74c3c")
    };

    private final Database db;
    private VBox root;
    private ComboBox<String> periodCombo;
    private VBox chartsArea;

    public StatsPanel(Database db) {
        this.db  = db;
        this.root = build();
    }

    public VBox getRoot() { return root; }

    private VBox build() {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color:" + Theme.bg() + ";");

        // Header
        HBox header = new HBox(16);
        header.setPadding(new Insets(18, 24, 14, 24));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color:" + Theme.surface() +
                "; -fx-effect:dropshadow(gaussian," + Theme.shadow() + ",6,0,0,2);");

        Label title = new Label("📊  Estadísticas");
        title.setStyle("-fx-font-size:17; -fx-font-weight:bold; -fx-text-fill:" + Theme.C_ACCENT + ";");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Label pLabel = new Label("Periodo:");
        pLabel.setStyle("-fx-text-fill:" + Theme.muted() + "; -fx-font-size:13;");

        periodCombo = new ComboBox<>();
        periodCombo.getItems().addAll("Esta semana", "Este mes", "Este año", "Todo el tiempo");
        periodCombo.setValue("Este mes");
        periodCombo.setStyle(Theme.fieldStyle());
        periodCombo.setOnAction(e -> refresh());

        Button refreshBtn = new Button("🔄  Actualizar");
        refreshBtn.setStyle(Theme.btnPrimary());
        refreshBtn.setOnAction(e -> refresh());

        header.getChildren().addAll(title, spacer, pLabel, periodCombo, refreshBtn);
        panel.getChildren().add(header);

        chartsArea = new VBox(20);
        chartsArea.setPadding(new Insets(20));
        chartsArea.setStyle("-fx-background-color:" + Theme.bg() + ";");

        ScrollPane scroll = new ScrollPane(chartsArea);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:" + Theme.bg() +
                "; -fx-background:" + Theme.bg() + ";");
        panel.getChildren().add(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Reestilizar automáticamente al cambiar tema
        Theme.darkMode.addListener((obs, old, nw) -> {
            panel.setStyle("-fx-background-color:" + Theme.bg() + ";");
            header.setStyle("-fx-background-color:" + Theme.surface() +
                    "; -fx-effect:dropshadow(gaussian," + Theme.shadow() + ",6,0,0,2);");
            chartsArea.setStyle("-fx-background-color:" + Theme.bg() + ";");
            scroll.setStyle("-fx-background-color:" + Theme.bg() +
                    "; -fx-background:" + Theme.bg() + ";");
            pLabel.setStyle("-fx-text-fill:" + Theme.muted() + "; -fx-font-size:13;");
            periodCombo.setStyle(Theme.fieldStyle());
            refreshBtn.setStyle(Theme.btnPrimary());
        });

        return panel;
    }

    public void refresh() {
        chartsArea.getChildren().clear();

        List<Entry> all      = db.getAllEntries();
        LocalDate   now      = LocalDate.now();
        LocalDate   from     = switch (periodCombo.getValue()) {
            case "Esta semana" -> now.minusDays(now.getDayOfWeek().getValue() - 1);
            case "Este mes"    -> now.withDayOfMonth(1);
            case "Este año"    -> now.withDayOfYear(1);
            default            -> LocalDate.of(2000, 1, 1);
        };
        List<Entry> filtered = all.stream()
                .filter(e -> !e.getDate().isBefore(from)).collect(Collectors.toList());

        // KPI cards
        long totalP   = filtered.size();
        long totalAll = all.size();
        long libros   = filtered.stream().filter(e -> e.getType().contains("Libro")).count();
        long avVideo  = filtered.stream().filter(e -> e.getType().contains("Serie") || e.getType().contains("Pel")).count();
        long comics   = filtered.stream().filter(e -> e.getType().contains("mic")).count();

        HBox kpiRow = new HBox(14);
        kpiRow.setAlignment(Pos.CENTER_LEFT);
        kpiRow.getChildren().addAll(
            kpi("📚", String.valueOf(totalP),   "en periodo",      ACCENT),
            kpi("📋", String.valueOf(totalAll), "total histórico", ACCENT2),
            kpi("📖", String.valueOf(libros),   "libros",          SUCCESS),
            kpi("🎥", String.valueOf(avVideo),  "series/pelís",    WARN),
            kpi("💭", String.valueOf(comics),   "cómics",          PURPLE)
        );
        chartsArea.getChildren().add(kpiRow);

        if (filtered.isEmpty()) {
            Label empty = new Label("📭 Sin registros en el periodo seleccionado");
            empty.setStyle("-fx-font-size:14; -fx-text-fill:" + Theme.muted() + ";");
            chartsArea.getChildren().add(empty);
            return;
        }

        // Conteos por tipo
        String[] typeKeys   = {"📚 Libro", "🎬 Serie", "🎥 Película", "🎭 Teatro", "💭 Cómic"};
        String[] typeLabels = {"Libro", "Serie", "Película", "Teatro", "Cómic"};
        long[]   typeCounts = new long[typeKeys.length];
        for (int i = 0; i < typeKeys.length; i++) {
            final String k = typeKeys[i];
            typeCounts[i] = filtered.stream().filter(e -> e.getType().contains(k.substring(3))).count();
        }

        // Fila 1: barras + tarta
        HBox row1 = new HBox(20);
        VBox barBox = chartCard("Registros por tipo",
                drawBarChart(typeLabels, typeCounts, BAR_COLORS, 460, 220));
        VBox pieBox = chartCard("Distribución por tipo",
                drawPieChart(typeLabels, typeCounts, 280, 230));
        HBox.setHgrow(barBox, Priority.ALWAYS);
        row1.getChildren().addAll(barBox, pieBox);
        chartsArea.getChildren().add(row1);

        // Actividad temporal
        boolean byDay = !"Este año".equals(periodCombo.getValue()) && !"Todo el tiempo".equals(periodCombo.getValue());
        if (byDay) {
            Map<LocalDate, Long> dayMap = filtered.stream()
                    .collect(Collectors.groupingBy(Entry::getDate, Collectors.counting()));
            List<LocalDate> dates = from.datesUntil(now.plusDays(1)).collect(Collectors.toList());
            String[] labels = dates.stream().map(d -> d.getDayOfMonth() + "/" + d.getMonthValue()).toArray(String[]::new);
            long[]   vals   = dates.stream().mapToLong(d -> dayMap.getOrDefault(d, 0L)).toArray();
            chartsArea.getChildren().add(chartCard("Actividad diaria", drawLineChart(labels, vals, 880, 200)));
        } else {
            Map<String, Long> monthMap = new TreeMap<>(filtered.stream().collect(
                    Collectors.groupingBy(e ->
                        e.getDate().getYear() + "-" + String.format("%02d", e.getDate().getMonthValue()),
                    Collectors.counting())));
            String[] labels = monthMap.keySet().toArray(new String[0]);
            long[]   vals   = monthMap.values().stream().mapToLong(Long::longValue).toArray();
            chartsArea.getChildren().add(chartCard("Actividad mensual",
                    drawBarChart(labels, vals, new Color[]{ACCENT2}, 880, 200)));
        }

        // Por día de la semana
        String[] days = {"Lun","Mar","Mié","Jue","Vie","Sáb","Dom"};
        long[] dayCounts = new long[7];
        for (Entry e : filtered) dayCounts[e.getDate().getDayOfWeek().getValue() - 1]++;
        chartsArea.getChildren().add(chartCard("Actividad por día de la semana",
                drawBarChart(days, dayCounts, BAR_COLORS, 880, 180)));
    }

    // ── CANVAS CHARTS ────────────────────────────────────────────────────

    private Canvas drawBarChart(String[] labels, long[] values, Color[] colors, double w, double h) {
        Canvas c = new Canvas(w, h);
        GraphicsContext g = c.getGraphicsContext2D();
        Color cardBg = Theme.isDark() ? Color.web("#313145") : Color.web("#ffffff");
        Color gridC  = Theme.isDark() ? Color.web("#3b3b55") : Color.web("#e8e8f0");
        Color mutedC = Theme.isDark() ? Color.web("#9090aa") : Color.web("#8888aa");
        Color textC  = Theme.isDark() ? Color.web("#e0e0f0") : Color.web("#2a2a3e");

        g.setFill(cardBg); g.fillRect(0, 0, w, h);
        int n = labels.length;
        if (n == 0) return c;
        long max = Arrays.stream(values).max().orElse(1);
        if (max == 0) max = 1;

        double padL = 36, padR = 10, padT = 14, padB = 34;
        double chartW = w - padL - padR;
        double chartH = h - padT - padB;
        double barW   = Math.min(chartW / n * 0.6, 50);
        double gap    = chartW / n;

        g.setStroke(gridC); g.setLineWidth(1);
        for (int i = 0; i <= 4; i++) {
            double y = padT + chartH - (chartH * i / 4);
            g.strokeLine(padL, y, w - padR, y);
            g.setFill(mutedC); g.setFont(Font.font(9)); g.setTextAlign(TextAlignment.RIGHT);
            g.fillText(String.valueOf(max * i / 4), padL - 4, y + 3);
        }
        for (int i = 0; i < n; i++) {
            double barH = (values[i] * chartH) / max;
            double x    = padL + gap * i + (gap - barW) / 2;
            double y    = padT + chartH - barH;
            g.setFill(colors[i % colors.length]);
            g.fillRoundRect(x, y, barW, barH, 6, 6);
            if (values[i] > 0) {
                g.setFill(textC); g.setFont(Font.font("System", FontWeight.BOLD, 10));
                g.setTextAlign(TextAlignment.CENTER);
                g.fillText(String.valueOf(values[i]), x + barW / 2, y - 3);
            }
            g.setFill(mutedC); g.setFont(Font.font(9)); g.setTextAlign(TextAlignment.CENTER);
            String lbl = labels[i].length() > 8 ? labels[i].substring(0, 7) + "…" : labels[i];
            g.fillText(lbl, x + barW / 2, padT + chartH + 13);
        }
        g.setStroke(mutedC); g.setLineWidth(1);
        g.strokeLine(padL, padT + chartH, w - padR, padT + chartH);
        return c;
    }

    private Canvas drawLineChart(String[] labels, long[] values, double w, double h) {
        Canvas c = new Canvas(w, h);
        GraphicsContext g = c.getGraphicsContext2D();
        Color cardBg = Theme.isDark() ? Color.web("#313145") : Color.web("#ffffff");
        Color gridC  = Theme.isDark() ? Color.web("#3b3b55") : Color.web("#e8e8f0");
        Color mutedC = Theme.isDark() ? Color.web("#9090aa") : Color.web("#8888aa");

        g.setFill(cardBg); g.fillRect(0, 0, w, h);
        int n = labels.length;
        if (n < 2) return c;
        long max = Arrays.stream(values).max().orElse(1);
        if (max == 0) max = 1;

        double padL = 36, padR = 10, padT = 14, padB = 30;
        double chartW = w - padL - padR;
        double chartH = h - padT - padB;

        g.setStroke(gridC); g.setLineWidth(1);
        for (int i = 0; i <= 4; i++) {
            double y = padT + chartH - chartH * i / 4;
            g.strokeLine(padL, y, w - padR, y);
            g.setFill(mutedC); g.setFont(Font.font(9)); g.setTextAlign(TextAlignment.RIGHT);
            g.fillText(String.valueOf(max * i / 4), padL - 4, y + 3);
        }
        double[] px = new double[n + 2];
        double[] py = new double[n + 2];
        for (int i = 0; i < n; i++) {
            px[i] = padL + chartW * i / (n - 1);
            py[i] = padT + chartH - (values[i] * chartH) / max;
        }
        px[n] = px[n-1]; py[n] = padT + chartH;
        px[n+1] = px[0]; py[n+1] = padT + chartH;
        g.setFill(Color.web(Theme.C_ACCENT, 0.15));
        g.fillPolygon(px, py, n + 2);
        g.setStroke(ACCENT); g.setLineWidth(2.5);
        g.beginPath(); g.moveTo(px[0], py[0]);
        for (int i = 1; i < n; i++) g.lineTo(px[i], py[i]);
        g.stroke();
        int step = Math.max(1, n / 15);
        for (int i = 0; i < n; i++) {
            if (values[i] > 0) { g.setFill(ACCENT); g.fillOval(px[i] - 3, py[i] - 3, 6, 6); }
            if (i % step == 0) {
                g.setFill(mutedC); g.setFont(Font.font(8)); g.setTextAlign(TextAlignment.CENTER);
                g.fillText(labels[i], px[i], padT + chartH + 12);
            }
        }
        g.setStroke(mutedC); g.setLineWidth(1);
        g.strokeLine(padL, padT + chartH, w - padR, padT + chartH);
        return c;
    }

    private Canvas drawPieChart(String[] labels, long[] values, double w, double h) {
        Canvas c = new Canvas(w, h);
        GraphicsContext g = c.getGraphicsContext2D();
        Color cardBg = Theme.isDark() ? Color.web("#313145") : Color.web("#ffffff");
        Color mutedC = Theme.isDark() ? Color.web("#9090aa") : Color.web("#8888aa");

        g.setFill(cardBg); g.fillRect(0, 0, w, h);
        long total = Arrays.stream(values).sum();
        if (total == 0) return c;

        double cx = w / 2, cy = h * 0.44;
        double r  = Math.min(w, h) * 0.34;
        double start = -Math.PI / 2;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == 0) continue;
            double angle = 2 * Math.PI * values[i] / total;
            g.setFill(BAR_COLORS[i % BAR_COLORS.length]);
            g.fillArc(cx - r, cy - r, r * 2, r * 2,
                    Math.toDegrees(-start), -Math.toDegrees(angle),
                    javafx.scene.shape.ArcType.ROUND);
            double mid = start + angle / 2;
            double lx  = cx + (r * 0.66) * Math.cos(mid);
            double ly  = cy + (r * 0.66) * Math.sin(mid);
            int pct = (int) Math.round(100.0 * values[i] / total);
            if (pct >= 5) {
                g.setFill(Color.WHITE); g.setFont(Font.font("System", FontWeight.BOLD, 10));
                g.setTextAlign(TextAlignment.CENTER);
                g.fillText(pct + "%", lx, ly + 4);
            }
            start += angle;
        }
        double ly = cy + r + 14; double lx = 10;
        g.setFont(Font.font(9));
        for (int i = 0; i < labels.length; i++) {
            if (values[i] == 0) continue;
            g.setFill(BAR_COLORS[i % BAR_COLORS.length]);
            g.fillRoundRect(lx, ly, 10, 10, 3, 3);
            g.setFill(mutedC); g.setTextAlign(TextAlignment.LEFT);
            g.fillText(labels[i] + " (" + values[i] + ")", lx + 13, ly + 9);
            lx += 75;
            if (lx > w - 60) { lx = 10; ly += 14; }
        }
        return c;
    }

    // ── HELPERS UI ───────────────────────────────────────────────────────

    private VBox chartCard(String title, javafx.scene.Node chart) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(14));
        String cardBg  = Theme.isDark() ? "#313145" : "#ffffff";
        String borderC = Theme.isDark() ? "#5050aa" : "#d0cce8";
        box.setStyle("-fx-background-color:" + cardBg +
                "; -fx-background-radius:12; -fx-border-color:" + borderC +
                "; -fx-border-radius:12; -fx-border-width:1;" +
                " -fx-effect:dropshadow(gaussian," + Theme.shadow() + ",10,0,0,3);");
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size:13; -fx-font-weight:bold; -fx-text-fill:" + Theme.C_ACCENT2 + ";");
        box.getChildren().addAll(lbl, chart);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox kpi(String icon, String value, String label, Color color) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14, 20, 14, 20));
        String hex    = String.format("#%02x%02x%02x",
                (int)(color.getRed()*255), (int)(color.getGreen()*255), (int)(color.getBlue()*255));
        String cardBg = Theme.isDark() ? "#313145" : "#ffffff";
        card.setStyle("-fx-background-color:" + cardBg +
                "; -fx-background-radius:14; -fx-border-color:" + hex +
                "; -fx-border-radius:14; -fx-border-width:2;" +
                " -fx-effect:dropshadow(gaussian," + Theme.shadow() + ",8,0,0,2);");
        HBox.setHgrow(card, Priority.ALWAYS);
        Label ico = new Label(icon); ico.setFont(Font.font(26));
        Label val = new Label(value);
        val.setFont(Font.font("System", FontWeight.BOLD, 24)); val.setTextFill(color);
        String mutedHex = Theme.isDark() ? "#9090aa" : "#7070aa";
        Label lbl = new Label(label); lbl.setStyle("-fx-text-fill:" + mutedHex + "; -fx-font-size:11;");
        card.getChildren().addAll(ico, val, lbl);
        return card;
    }
}
