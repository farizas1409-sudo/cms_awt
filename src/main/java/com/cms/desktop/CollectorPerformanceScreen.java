package com.cms.desktop;

import java.awt.*;
import java.util.List;
import java.util.Map;

/** Collector productivity and action report screen layout. */
final class CollectorPerformanceScreen {
  private CollectorPerformanceScreen() {}
  static void render(CmsFrame frame, Panel content, Map<String,Object> data) {
    Panel root = frame.panel(DesktopTheme.PALE); root.setLayout(new BorderLayout(0, 12));
    List<Map<String,Object>> rows = frame.listOfMaps(data.get("summaryRows"));
    Panel controls = DesktopTheme.card();
    controls.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));
    List<Map<String,Object>> districts = frame.listOfMaps(data.get("districtOptions"));
    Choice district = new Choice();
    district.add("Choose a district");
    for (Map<String,Object> option : districts) {
      district.add(frame.text(option.get("NAME")));
      if (frame.text(option.get("ID")).equals(data.get("selectedDistrict"))) {
        district.select(district.getItemCount() - 1);
      }
    }
    Choice risk = new Choice();
    for (String option : List.of("All risks", "LOW", "MEDIUM", "HIGH")) {
      risk.add(option);
      if (option.equals(data.get("selectedRisk"))) risk.select(option);
    }
    ActionButton run = frame.button("Run report", DesktopTheme.BLUE);
    run.setEnabled(district.getSelectedIndex() > 0);
    district.addItemListener(e -> run.setEnabled(district.getSelectedIndex() > 0));
    run.addActionListener(e -> {
      int index = district.getSelectedIndex() - 1;
      if (index >= 0) {
        frame.runCollectorPerformance(frame.text(districts.get(index).get("ID")),
            risk.getSelectedIndex() == 0 ? "" : risk.getSelectedItem());
      }
    });
    ActionButton export = frame.button("Export CSV", DesktopTheme.NAVY_HOVER);
    export.setEnabled(!rows.isEmpty());
    export.addActionListener(e -> frame.exportReport(rows));
    controls.add(frame.label("District (required)", 12, true, DesktopTheme.INK));
    controls.add(district);
    controls.add(risk);
    controls.add(run);
    controls.add(export);
    root.add(controls, BorderLayout.NORTH);
    root.add(ReportScreenSupport.reportContent(frame, data, "Collector Performance", new String[]{"summaryRows", "activityRows"}), BorderLayout.CENTER);
    content.add(root, BorderLayout.CENTER);
  }
}

