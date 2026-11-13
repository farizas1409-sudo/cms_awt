package com.cms.desktop;

import java.awt.*;
import java.util.List;
import java.util.Map;

/** Risk thresholds and assessment screen layout. */
final class RiskRulesScreen {
  private RiskRulesScreen() {}

  static void render(CmsFrame frame, Panel content, Map<String,Object> data) {
    List<Map<String,Object>> rules = frame.listOfMaps(data.get("rows"));
    List<Map<String,Object>> distribution = frame.listOfMaps(data.get("riskDistribution"));
    Panel root = frame.panel(DesktopTheme.PALE); root.setLayout(new BorderLayout(0, 12));
    Panel toolbar = DesktopTheme.card(); toolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));
    ActionButton add = frame.button("+ Add threshold", DesktopTheme.BLUE); add.addActionListener(e -> frame.createRiskRule());
    Choice selected = new Choice(); rules.forEach(row -> selected.add(frame.text(row.get("RISK")) + " · " + frame.text(row.get("MIN_DPD")) + " days"));
    ActionButton edit = frame.button("Edit", DesktopTheme.BLUE); edit.setEnabled(!rules.isEmpty());
    edit.addActionListener(e -> { if (!rules.isEmpty()) frame.editRiskRule(rules.get(selected.getSelectedIndex())); });
    ActionButton delete = frame.button("Delete", DesktopTheme.DANGER); delete.setEnabled(!rules.isEmpty());
    delete.addActionListener(e -> { if (!rules.isEmpty()) frame.deleteRisk(rules.get(selected.getSelectedIndex())); });
    ActionButton run = frame.button("Run risk assessment", DesktopTheme.BLUE); run.addActionListener(e -> frame.runRiskAssessment());
    toolbar.add(add); toolbar.add(selected); toolbar.add(edit); toolbar.add(delete); toolbar.add(run);
    root.add(toolbar, BorderLayout.NORTH);
    Panel panels = frame.panel(DesktopTheme.PALE);
    panels.setLayout(new BorderLayout(0, 12));
    Panel thresholds = frame.tableSection("Configured risk thresholds", rules,
        List.of("ID", "RISK", "PORTFOLIO_NAME", "MIN_DPD", "MIN_OVERDUE_PAISE", "ACTIVE"));
    int visibleRows = Math.max(1, Math.min(rules.size(), 3));
    thresholds.setPreferredSize(new Dimension(0, 72 + 26 * visibleRows));
    panels.add(thresholds, BorderLayout.NORTH);
    panels.add(frame.tableSection("Current loan risk distribution", distribution), BorderLayout.CENTER);
    root.add(panels, BorderLayout.CENTER); content.add(root, BorderLayout.CENTER);
  }
}

