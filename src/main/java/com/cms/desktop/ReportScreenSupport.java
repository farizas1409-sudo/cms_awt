package com.cms.desktop;

import java.awt.*;
import java.util.List;
import java.util.Map;

/** Shared report controls and layout pieces used by dedicated report screens. */
final class ReportScreenSupport {
  private ReportScreenSupport() {}

  static Panel controls(CmsFrame frame, List<Map<String,Object>> rows, String screen) {
    Panel toolbar = DesktopTheme.card(); toolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));
    TextField query = frame.field("Customer, name, or account");
    Choice risk = new Choice(); for (String item : List.of("All risks", "LOW", "MEDIUM", "HIGH")) risk.add(item);
    ActionButton search = frame.button("Run report", DesktopTheme.BLUE);
    search.addActionListener(e -> frame.updateReport(query.getText(), risk.getSelectedItem()));
    ActionButton export = frame.button("Export CSV", DesktopTheme.NAVY_HOVER);
    export.addActionListener(e -> frame.exportReport(rows));
    toolbar.add(frame.label("Search scope", 12, true, DesktopTheme.INK)); toolbar.add(query);
    toolbar.add(risk); toolbar.add(search); toolbar.add(export);
    return toolbar;
  }

  static Panel reportContent(CmsFrame frame, Map<String,Object> data, String screen, String[] keys) {
    Panel detail = frame.panel(DesktopTheme.PALE); detail.setLayout(new BorderLayout(0, 12));
    Panel metrics = frame.metricPanel(frame.mapOf(data.get("summary")));
    if (metrics.getComponentCount() > 0) detail.add(metrics, BorderLayout.NORTH);
    Panel reports = frame.panel(DesktopTheme.PALE); reports.setLayout(new GridLayout(0, 1, 0, 10));
    boolean hasRows = false;
    for (String key : keys) {
      List<Map<String,Object>> records = frame.listOfMaps(data.get(key));
      if (!records.isEmpty()) { reports.add(frame.tableSection(frame.reportTitle(key), records)); hasRows = true; }
    }
    if (!hasRows) reports.add(frame.tableSection(frame.pageHeading(screen), frame.listOfMaps(data.get("rows"))));
    ScrollPane scroll = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED); scroll.add(reports);
    detail.add(scroll, BorderLayout.CENTER);
    return detail;
  }
}

