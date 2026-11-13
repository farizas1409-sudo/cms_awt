package com.cms.desktop;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Panel;
import java.awt.TextComponent;
import java.util.Map;
import com.cms.config.SystemParameters;

/** Shared visual language for every CMS desktop screen. */
final class DesktopTheme {
  static Color NAVY = new Color(12, 31, 58);
  static Color NAVY_HOVER = new Color(24, 48, 81);
  static Color BLUE = new Color(18, 104, 224);
  static Color BLUE_DARK = new Color(8, 77, 179);
  static Color PALE = new Color(239, 245, 252);
  static final Color WHITE = Color.WHITE;
  static Color SURFACE = Color.WHITE;
  static Color INK = new Color(22, 39, 62);
  static Color MUTED = new Color(101, 119, 143);
  static Color BORDER = new Color(218, 229, 242);
  static Color HEADER = new Color(239, 245, 252);
  static Color ACCENT_RED = new Color(232, 42, 57);
  static Color SOFT_BLUE = new Color(230, 241, 255);
  static Color SHADOW = new Color(28, 59, 99, 24);
  static final Color SUCCESS = new Color(0, 143, 103);
  static final Color WARNING = new Color(220, 126, 18);
  static final Color DANGER = new Color(218, 58, 77);
  static Font BODY = new Font("Segoe UI", Font.PLAIN, 12);
  static Font MEDIUM = new Font("Segoe UI", Font.BOLD, 12);
  static Font TITLE = new Font("Segoe UI", Font.BOLD, 19);
  private static String fontFamily = "Segoe UI";
  private static int fontSize = 12;
  private static boolean darkMode;

  private DesktopTheme() {}

  static void apply(Map<String, Object> preferences) {
    if (preferences == null) return;
    apply(String.valueOf(preferences.getOrDefault("theme", "SYSTEM")),
        String.valueOf(preferences.getOrDefault("accent", SystemParameters.get("ui.default.accent"))),
        String.valueOf(preferences.getOrDefault("fontSize", "12")),
        String.valueOf(preferences.getOrDefault("fontFamily", "SEGOE_UI")));
  }

  static void apply(String theme, String accent, String size, String family) {
    try { BLUE = Color.decode(accent); } catch (RuntimeException invalidColor) { BLUE = new Color(37,99,235); }
    String requestedFont = SystemParameters.getCsvMap("ui.font-family.options").getOrDefault(family, "Segoe UI");
    fontFamily = requestedFont;
    try { fontSize = Math.max(10, Math.min(22, Integer.parseInt(size))); } catch (RuntimeException invalidSize) { fontSize = 12; }
    String selectedTheme = theme == null ? "SYSTEM" : theme.toUpperCase(java.util.Locale.ROOT);
    darkMode = "DARK".equals(selectedTheme) || "SLATE".equals(selectedTheme);
    switch (selectedTheme) {
      case "DARK" -> palette("#0B1220", "#172338", "#101B2D", "#EAF1FC", "#AAB9CF", "#273750", "#1A2A41", "#1A2C48");
      case "OCEAN" -> palette("#0C3440", "#15505D", "#F8FEFF", "#173C47", "#5C7580", "#CDE4E9", "#E8F5F7", "#E0F0F3");
      case "EMERALD" -> palette("#12392F", "#1D5947", "#FAFFFC", "#193E32", "#62786E", "#D6E8DF", "#EEF7F1", "#E7F3EC");
      case "VIOLET" -> palette("#30204F", "#49316F", "#FFFDFF", "#35244F", "#766B86", "#E5DDF0", "#F6F2FB", "#F0EAF8");
      case "ROSE" -> palette("#4B1D2C", "#6D2C42", "#FFFCFD", "#492433", "#846A73", "#EEDBE1", "#FFF2F5", "#F9E8ED");
      case "AMBER" -> palette("#49340F", "#674B18", "#FFFEFB", "#493815", "#83785F", "#EFE5CF", "#FFF8EB", "#F8EFDC");
      case "NORDIC" -> palette("#253746", "#354C5C", "#FBFEFF", "#2D4352", "#687A86", "#DCE6EB", "#F0F5F8", "#E8F0F4");
      case "SLATE" -> palette("#202B36", "#344351", "#18222D", "#E8EFF6", "#A1B0BE", "#3A4A58", "#273744", "#293B4A");
      case "LIGHT", "SYSTEM" -> palette("#142D4B", "#1E4168", "#FFFFFF", "#183653", "#677A91", "#DCE6F1", "#F3F7FC", "#EAF1FA");
      default -> palette("#142D4B", "#1E4168", "#FFFFFF", "#183653", "#677A91", "#DCE6F1", "#F3F7FC", "#EAF1FA");
    }
    BLUE_DARK = BLUE.darker();
    BODY = new Font(fontFamily, Font.PLAIN, fontSize);
    MEDIUM = new Font(fontFamily, Font.BOLD, fontSize);
    TITLE = new Font(fontFamily, Font.BOLD, fontSize + 4);
  }

  private static void palette(String navy, String hover, String surface, String ink,
      String muted, String border, String pale, String soft) {
    NAVY = Color.decode(navy);
    NAVY_HOVER = Color.decode(hover);
    SURFACE = Color.decode(surface);
    INK = Color.decode(ink);
    MUTED = Color.decode(muted);
    BORDER = Color.decode(border);
    PALE = Color.decode(pale);
    HEADER = darkMode ? Color.decode(hover) : Color.decode(soft);
    SOFT_BLUE = Color.decode(soft);
  }

  static Font font(int requestedSize, boolean bold) {
    int scaled = Math.max(10, Math.round(requestedSize * (fontSize / 12.0f)));
    return new Font(fontFamily, bold ? Font.BOLD : Font.PLAIN, scaled);
  }

  static Panel surface(Color color) {
    Panel panel = new Panel();
    panel.setBackground(color.equals(Color.WHITE) ? SURFACE : color.equals(new Color(241,246,252)) ? PALE : color);
    return panel;
  }

  static Panel card() { return new SurfacePanel(); }

  static void styleText(Component component) {
    component.setFont(BODY);
    if (component instanceof TextComponent) {
      component.setForeground(INK);
      component.setBackground(SURFACE);
    }
  }
}

