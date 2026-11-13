package com.cms.desktop;

import java.awt.*;
import java.util.List;
import java.util.Map;

/** Branch customer allocation screen layout. */
final class CustomerAllocationScreen {
  private CustomerAllocationScreen() {}

  static void render(CmsFrame frame, Panel content, Map<String,Object> data) {
    List<Map<String,Object>> branches = frame.listOfMaps(data.get("rows"));
    Panel root = frame.panel(DesktopTheme.PALE); root.setLayout(new BorderLayout(0, 12));
    Panel form = frame.gridPanel("Allocate new customers");
    Panel controls = frame.panel(Color.WHITE); controls.setLayout(new GridLayout(1, 5, 8, 6));
    Choice scope = new Choice(); scope.add("All branches"); scope.add("Selected branch");
    Choice branch = new Choice(); branch.add("Choose a branch");
    java.util.Set<String> branchOptions = new java.util.LinkedHashSet<>();
    branches.forEach(row -> {
      String option = frame.text(row.get("NAME")) + " · " + frame.text(row.get("ID"));
      if (branchOptions.add(option)) branch.add(option);
    });
    scope.addItemListener(e -> branch.setEnabled(scope.getSelectedIndex() != 0)); branch.setEnabled(false);
    ActionButton run = frame.button("Allocate customers", DesktopTheme.BLUE);
    run.addActionListener(e -> frame.runAllocation(scope.getSelectedIndex() == 0,
        scope.getSelectedIndex() == 0 || branch.getSelectedIndex() == 0 ? null : branches.get(branch.getSelectedIndex() - 1)));
    controls.add(frame.label("Allocation scope", 12, true, DesktopTheme.INK)); controls.add(scope);
    controls.add(frame.label("Branch", 12, true, DesktopTheme.INK)); controls.add(branch); controls.add(run);
    form.add(controls, BorderLayout.CENTER); root.add(form, BorderLayout.NORTH);
    root.add(frame.tableSection("Branch allocation details", branches,
        List.of("REGION_NAME", "DISTRICT_NAME", "NAME", "COLLECTOR_NAME", "ASSIGNED_CASES", "CUSTOMERS", "ALLOCATED", "UNASSIGNED")), BorderLayout.CENTER);
    content.add(root, BorderLayout.CENTER);
  }
}

