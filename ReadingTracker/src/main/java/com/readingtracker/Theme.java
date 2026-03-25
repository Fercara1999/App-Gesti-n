package com.readingtracker;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Singleton de tema. Observa darkMode para reaccionar en tiempo real.
 */
public class Theme {

    public static final BooleanProperty darkMode = new SimpleBooleanProperty(false);

    // ── Colores modo oscuro ──────────────────────────────────────────────
    private static final String D_BG      = "#1e1e2e";
    private static final String D_SURFACE = "#2a2a3e";
    private static final String D_CARD    = "#313145";
    private static final String D_TEXT    = "#e0e0f0";
    private static final String D_MUTED   = "#9090aa";
    private static final String D_INPUT   = "#3b3b55";
    private static final String D_BORDER  = "#5050aa";
    private static final String D_CHIP    = "#3b3b55";

    // ── Colores modo claro ───────────────────────────────────────────────
    private static final String L_BG      = "#f0f2f8";
    private static final String L_SURFACE = "#ffffff";
    private static final String L_CARD    = "#ffffff";
    private static final String L_TEXT    = "#1a1a2e";
    private static final String L_MUTED   = "#606080";
    private static final String L_INPUT   = "#f4f4fb";
    private static final String L_BORDER  = "#c0b8f0";
    private static final String L_CHIP    = "#ebebf8";

    // ── Colores de acento (iguales en ambos temas) ───────────────────────
    public static final String C_ACCENT  = "#6c5ce7";
    public static final String C_ACCENT2 = "#00b4cc";
    public static final String C_SUCCESS = "#27ae60";
    public static final String C_WARN    = "#e67e22";
    public static final String C_PURPLE  = "#9b59b6";

    // ── Getters dinámicos ────────────────────────────────────────────────
    public static boolean isDark()     { return darkMode.get(); }

    public static String bg()      { return isDark() ? D_BG      : L_BG;      }
    public static String surface() { return isDark() ? D_SURFACE : L_SURFACE; }
    public static String card()    { return isDark() ? D_CARD    : L_CARD;    }
    public static String text()    { return isDark() ? D_TEXT    : L_TEXT;    }
    public static String muted()   { return isDark() ? D_MUTED   : L_MUTED;   }
    public static String input()   { return isDark() ? D_INPUT   : L_INPUT;   }
    public static String border()  { return isDark() ? D_BORDER  : L_BORDER;  }
    public static String chip()    { return isDark() ? D_CHIP    : L_CHIP;    }
    public static String shadow()  { return isDark() ? "rgba(0,0,0,0.4)" : "rgba(100,100,180,0.15)"; }

    // ── Estilos reutilizables ────────────────────────────────────────────
    public static String fieldStyle() {
        return "-fx-background-color:" + input() +
               "; -fx-text-fill:" + text() +
               "; -fx-prompt-text-fill:" + muted() +
               "; -fx-border-color:" + border() +
               "; -fx-border-radius:6; -fx-background-radius:6; -fx-padding:6 10;";
    }
    public static String labelBold() {
        return "-fx-font-weight:bold; -fx-text-fill:" + text() + "; -fx-font-size:12;";
    }
    public static String btnPrimary() {
        return "-fx-background-color:" + C_ACCENT +
               "; -fx-text-fill:white; -fx-font-size:13;" +
               " -fx-padding:9 28; -fx-background-radius:8; -fx-cursor:hand;";
    }
    public static String btnSecondary() {
        return "-fx-background-color:" + (isDark() ? "#3b3b55" : "#ebebf8") +
               "; -fx-text-fill:" + text() +
               "; -fx-font-size:12; -fx-padding:8 20; -fx-background-radius:8; -fx-cursor:hand;";
    }
    public static String cardStyle() {
        return "-fx-background-color:" + card() +
               "; -fx-background-radius:12;" +
               " -fx-border-color:" + border() +
               "; -fx-border-radius:12; -fx-border-width:1;" +
               " -fx-effect:dropshadow(gaussian," + shadow() + ",10,0,0,3);";
    }
    public static String chipStyle() {
        return "-fx-background-color:" + chip() +
               "; -fx-text-fill:" + muted() +
               "; -fx-font-size:11; -fx-padding:2 8; -fx-background-radius:10;";
    }
    public static String panelStyle() {
        return "-fx-background-color:" + surface() + ";";
    }
    public static String cardAreaStyle() {
        return "-fx-background-color:" + bg() + ";";
    }
}
