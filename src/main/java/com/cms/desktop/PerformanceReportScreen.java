package com.cms.desktop;

import java.awt.*;
import java.util.List;
import java.util.Map;

/** Owner-wide performance report screen layout. */
final class PerformanceReportScreen {
  private PerformanceReportScreen() {}
  static void render(CmsFrame frame, Panel content, Map<String,Object> data) {
    Panel root = frame.panel(DesktopTheme.PALE); root.setLayout(new BorderLayout(0, 12));
    List<Map<String,Object>> rows = frame.listOfMaps(data.get("rows"));
    root.add(ReportScreenSupport.controls(frame, rows, "Performance Report"), BorderLayout.NORTH);
    root.add(ReportScreenSupport.reportContent(frame, data, "Performance Report", new String[]{"riskRows", "portfolioRows", "rows"}), BorderLayout.CENTER);
    content.add(root, BorderLayout.CENTER);
  }
}

