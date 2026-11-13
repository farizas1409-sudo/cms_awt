package com.cms.desktop;

import java.awt.*;
import java.util.List;
import java.util.Map;

/** Application master-data screen layout. */
final class ApplicationMastersScreen {
  private ApplicationMastersScreen() {}

  static void render(CmsFrame frame, Panel content, Map<String,Object> data) {
    Panel root = frame.panel(DesktopTheme.PALE); root.setLayout(new BorderLayout(0, 12));
    Panel toolbar = DesktopTheme.card(); toolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));
    Choice type = new Choice(); for (String value : List.of("District", "Branch", "Region", "Portfolio")) type.add(value);
    String current = frame.text(data.getOrDefault("masterType", "district"));
    for (int index = 0; index < type.getItemCount(); index++) if (type.getItem(index).equalsIgnoreCase(current)) type.select(index);
    type.addItemListener(event -> frame.selectMasterType(type.getSelectedItem()));
    ActionButton fetch = frame.button("Fetch details", DesktopTheme.BLUE); fetch.addActionListener(e -> frame.fetchMaster(type.getSelectedItem()));
    ActionButton add = frame.button("+ New master", DesktopTheme.BLUE); add.addActionListener(e -> frame.createMaster(type.getSelectedItem()));
    toolbar.add(frame.label("Master type", 12, true, DesktopTheme.INK)); toolbar.add(type); toolbar.add(fetch); toolbar.add(add);
    root.add(toolbar, BorderLayout.NORTH);
    List<Map<String,Object>> rows = frame.listOfMaps(data.get("rows"));
    if (Boolean.TRUE.equals(data.get("fetched"))) root.add(frame.tableSection(capitalize(current) + " details", rows), BorderLayout.CENTER);
    else root.add(frame.emptySection("Choose a master type, then select Fetch details to view its records."), BorderLayout.CENTER);
    content.add(root, BorderLayout.CENTER);
  }

  private static String capitalize(String value) {
    if (value == null || value.isBlank()) return "";
    return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase(java.util.Locale.ROOT);
  }
}

