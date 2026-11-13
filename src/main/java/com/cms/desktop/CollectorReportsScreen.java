package com.cms.desktop;

import java.awt.*;
import java.util.List;
import java.util.Map;

/** Collector's assigned-customer report screen layout. */
final class CollectorReportsScreen {
  private CollectorReportsScreen() {}
  static void render(CmsFrame frame, Panel content, Map<String,Object> data) {
    Panel root = frame.panel(DesktopTheme.PALE); root.setLayout(new BorderLayout(0, 12));
    List<Map<String,Object>> rows = frame.listOfMaps(data.get("rows"));
    root.add(ReportScreenSupport.controls(frame, rows, "Collector Reports"), BorderLayout.NORTH);
    root.add(ReportScreenSupport.reportContent(frame, data, "Collector Reports", new String[]{"rows"}), BorderLayout.CENTER);
    content.add(root, BorderLayout.CENTER);
  }
}

