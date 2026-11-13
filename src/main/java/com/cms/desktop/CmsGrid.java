package com.cms.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.Dimension;
import java.awt.Adjustable;
import java.awt.event.AdjustmentListener;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Reusable compact data grid used by desktop screens. */
final class CmsGrid {
  private CmsGrid() {}

  static Panel section(String title, List<Map<String, Object>> rows, List<String> preferred,
      boolean customerRows, Consumer<Map<String, Object>> openCustomer) {
    Panel section = DesktopTheme.card();
    section.setLayout(new BorderLayout(0, 2));
    section.setBackground(DesktopTheme.SURFACE);
    Panel header = new Panel(new BorderLayout(10, 0));
    header.setBackground(DesktopTheme.SURFACE);
    Panel marker = new Panel();
    marker.setBackground(DesktopTheme.BLUE);
    marker.setPreferredSize(new Dimension(4, 1));
    Label heading = new Label("  " + title);
    heading.setFont(DesktopTheme.font(13, true));
    heading.setForeground(DesktopTheme.INK);
    Label count = new Label(rows.size() + (rows.size() == 1 ? " record  " : " records  "));
    count.setFont(DesktopTheme.font(10, false));
    count.setForeground(DesktopTheme.MUTED);
    header.add(marker, BorderLayout.WEST);
    header.add(heading, BorderLayout.CENTER);
    header.add(count, BorderLayout.EAST);
    header.setPreferredSize(new Dimension(10, 28));
    section.add(header, BorderLayout.NORTH);
    Panel tableArea = new Panel(new BorderLayout());
    tableArea.setBackground(DesktopTheme.SURFACE);
    tableArea.add(table(rows, preferred, customerRows, openCustomer), BorderLayout.CENTER);
    section.add(tableArea, BorderLayout.CENTER);
    return section;
  }

  static Panel table(List<Map<String, Object>> rows, List<String> preferred,
      boolean customerRows, Consumer<Map<String, Object>> openCustomer) {
    Panel outer = new Panel(new BorderLayout());
    outer.setBackground(DesktopTheme.SURFACE);
    if (rows == null || rows.isEmpty()) {
      Label empty = new Label("  No matching records for this view.");
      empty.setFont(DesktopTheme.BODY);
      empty.setForeground(DesktopTheme.MUTED);
      empty.setBackground(DesktopTheme.SURFACE);
      outer.add(empty, BorderLayout.NORTH);
      return outer;
    }
    LinkedHashSet<String> columns = new LinkedHashSet<>();
    if (preferred != null) for (String key : preferred) if (rows.get(0).containsKey(key)) columns.add(key);
    if (columns.isEmpty()) columns.addAll(rows.get(0).keySet());
    Panel headerGrid = new Panel(new GridBagLayout());
    headerGrid.setBackground(DesktopTheme.NAVY);
    Panel bodyGrid = new Panel(new GridBagLayout());
    bodyGrid.setBackground(DesktopTheme.SURFACE);
    GridBagConstraints cell = new GridBagConstraints();
    cell.fill = GridBagConstraints.BOTH;
    cell.insets = new Insets(0, 0, 0, 0);
    int x = 0;
    for (String column : columns) {
      cell.gridx = x++;
      cell.gridy = 0;
      cell.weightx = weight(column);
      headerGrid.add(value(human(column), true, 0), cell);
    }
    if (customerRows) {
      cell.gridx = x;
      cell.gridy = 0;
      cell.weightx = 0.9;
      headerGrid.add(value("Action", true, 0), cell);
    }
    int y = 0;
    for (Map<String, Object> row : rows) {
      x = 0;
      cell.gridy = y;
      cell.weighty = 0;
      int stripe = y++;
      for (String column : columns) {
        cell.gridx = x++;
        cell.weightx = weight(column);
        Object rawValue = row.get(column);
        String displayValue = rawValue == null ? "" : String.valueOf(rawValue);
        if ("ACTIVE".equalsIgnoreCase(column) && rawValue instanceof Number status) {
          if (status.doubleValue() == 1) displayValue = "Y";
          else if (status.doubleValue() == 0) displayValue = "N";
        }
        bodyGrid.add(value(displayValue, false, stripe), cell);
      }
      if (customerRows && openCustomer != null) {
        cell.gridx = x;
        cell.weightx = 0.9;
        Panel action = new Panel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        action.setBackground((stripe % 2 == 0) ? DesktopTheme.SURFACE : DesktopTheme.SOFT_BLUE);
        ActionButton open = new ActionButton("Open customer");
        open.setBackground(DesktopTheme.BLUE);
        action.add(open);
        open.addActionListener(e -> openCustomer.accept(row));
        bodyGrid.add(action, cell);
      }
    }
    // Keep unused viewport height below the last record. Without a weighted
    // trailing row, GridBagLayout centers the records vertically in tall grids,
    // which looks like a blank first row on screens with only a few results.
    GridBagConstraints filler = new GridBagConstraints();
    filler.gridx = 0;
    filler.gridy = y;
    filler.gridwidth = columns.size() + (customerRows ? 1 : 0);
    filler.weightx = 1;
    filler.weighty = 1;
    filler.fill = GridBagConstraints.BOTH;
    filler.anchor = GridBagConstraints.NORTHWEST;
    Panel remainingSpace = new Panel();
    remainingSpace.setBackground(DesktopTheme.SURFACE);
    bodyGrid.add(remainingSpace, filler);
    Panel gridPanel = new Panel(new BorderLayout());
    gridPanel.setBackground(DesktopTheme.SURFACE);
    ScrollPane headerScroller = new ScrollPane(ScrollPane.SCROLLBARS_NEVER);
    headerScroller.setBackground(DesktopTheme.NAVY);
    headerScroller.setPreferredSize(new Dimension(100, 28));
    headerScroller.add(headerGrid);
    ScrollPane bodyScroller = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
    bodyScroller.setBackground(DesktopTheme.SURFACE);
    bodyScroller.add(bodyGrid);
    AdjustmentListener syncHeader = event -> {
      Adjustable headerBar = headerScroller.getHAdjustable();
      Adjustable bodyBar = bodyScroller.getHAdjustable();
      if (headerBar.getValue() != bodyBar.getValue()) headerBar.setValue(bodyBar.getValue());
    };
    bodyScroller.getHAdjustable().addAdjustmentListener(syncHeader);
    gridPanel.add(headerScroller, BorderLayout.NORTH);
    gridPanel.add(bodyScroller, BorderLayout.CENTER);
    outer.add(gridPanel, BorderLayout.CENTER);
    return outer;
  }

  private static Panel value(String text, boolean header, int row) {
    Panel cell = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 3));
    cell.setBackground(header ? DesktopTheme.NAVY : row % 2 == 0 ? DesktopTheme.SURFACE : DesktopTheme.SOFT_BLUE);
    cell.setPreferredSize(new Dimension(118, header ? 27 : 26));
    Label label = new Label(text == null || "null".equals(text) ? "" : text);
    label.setFont(header ? DesktopTheme.font(11, true) : DesktopTheme.font(11, false));
    label.setForeground(header ? DesktopTheme.WHITE : DesktopTheme.INK);
    cell.add(label);
    return cell;
  }

  private static double weight(String column) {
    return switch (column) {
      case "CUSTOMER_NO", "ACCOUNT_NO", "USERNAME", "EMAIL", "ADDRESS", "DETAILS", "SUMMARY" -> 2.0;
      case "NAME", "DISPLAY_NAME", "BRANCH_NAME", "DISTRICT_NAME" -> 1.5;
      default -> 1.0;
    };
  }

  private static String human(String value) {
    String result = value.replace('_', ' ').toLowerCase();
    return result.isEmpty() ? result : Character.toUpperCase(result.charAt(0)) + result.substring(1);
  }
}

