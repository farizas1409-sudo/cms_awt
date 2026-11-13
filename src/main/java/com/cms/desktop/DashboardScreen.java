package com.cms.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Dashboard screen layout, separated from the application frame/controller. */
final class DashboardScreen {
  private DashboardScreen() {}

  static void render(Panel target, Map<String, Object> data,
      Consumer<Map<String, Object>> openCustomer) {
    Map<String, Object> summary = asMap(data.get("summary"));
    Panel root = new Panel(new BorderLayout(0, 14));
    root.setBackground(DesktopTheme.PALE);
    Panel metrics = new Panel(new GridLayout(1, 5, 12, 0));
    metrics.setBackground(DesktopTheme.PALE);
    metrics.add(metric("CUSTOMERS", summary.get("CUSTOMERS"), "Assigned portfolio", DesktopTheme.BLUE));
    metrics.add(metric("LOAN ACCOUNTS", summary.get("ACCOUNTS"), "Active accounts", new Color(112, 79, 190)));
    metrics.add(metric("OUTSTANDING", money(summary.get("OUTSTANDING")), "Balance due", DesktopTheme.WARNING));
    metrics.add(metric("OVERDUE", money(summary.get("OVERDUE")), "Past due balance", DesktopTheme.DANGER));
    metrics.add(metric("COLLECTIONS", money(summary.get("COLLECTIONS")), "Payments received", DesktopTheme.SUCCESS));
    root.add(metrics, BorderLayout.NORTH);

    Panel cards = new Panel(new GridLayout(2, 2, 14, 14));
    cards.setBackground(DesktopTheme.PALE);
    cards.add(CmsGrid.section("Risk distribution", rows(data.get("riskRows")),
        List.of("RISK", "TOTAL"), false, null));
    cards.add(CmsGrid.section("Portfolio balances", rows(data.get("portfolioRows")),
        List.of("PORTFOLIO_NAME", "ACCOUNTS", "OUTSTANDING", "OVERDUE"), false, null));
    cards.add(CmsGrid.section("Latest customer activity", rows(data.get("recentRows")),
        List.of("CREATED_AT", "CUSTOMER_NO", "NAME", "ACCOUNT_NO", "CHANNEL", "OUTCOME"), true, openCustomer));
    cards.add(CmsGrid.section("Follow-up outcomes", rows(data.get("promiseRows")),
        List.of("OUTCOME", "ACTIONS", "PROMISED"), false, null));
    root.add(cards, BorderLayout.CENTER);
    target.add(root, BorderLayout.CENTER);
  }

  private static Panel metric(String title, Object amount, String note, Color accent) {
    Panel card = DesktopTheme.card();
    card.setLayout(new BorderLayout(0, 0));
    card.setBackground(DesktopTheme.SURFACE);
    Panel rail = new Panel();
    rail.setBackground(accent);
    rail.setPreferredSize(new Dimension(5, 1));
    card.add(rail, BorderLayout.WEST);
    Panel text = new Panel(new GridLayout(3, 1, 0, 2));
    text.setBackground(DesktopTheme.SURFACE);
    text.add(label(title, 10, DesktopTheme.MUTED));
    text.add(label(String.valueOf(amount == null ? "0" : amount), 16, DesktopTheme.INK, true));
    text.add(label(note, 10, DesktopTheme.MUTED));
    card.add(text, BorderLayout.CENTER);
    card.setPreferredSize(new Dimension(150, 78));
    return card;
  }

  private static Label label(String value, int size, Color color) { return label(value, size, color, false); }
  private static Label label(String value, int size, Color color, boolean bold) {
    Label label = new Label("  " + value);
    label.setFont(DesktopTheme.font(size, bold));
    label.setForeground(color);
    return label;
  }

  private static String money(Object value) {
    try { return "INR " + new DecimalFormat("#,##,##0.00").format(new BigDecimal(String.valueOf(value))); }
    catch (Exception ignored) { return "INR 0.00"; }
  }

  private static Map<String, Object> asMap(Object value) {
    if (!(value instanceof Map<?, ?> map)) return Map.of();
    Map<String, Object> result = new LinkedHashMap<>();
    map.forEach((key, item) -> result.put(String.valueOf(key), item));
    return result;
  }

  private static List<Map<String, Object>> rows(Object value) {
    if (!(value instanceof List<?> list)) return List.of();
    List<Map<String, Object>> result = new ArrayList<>();
    for (Object item : list) result.add(asMap(item));
    return result;
  }
}

