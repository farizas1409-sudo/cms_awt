package com.cms.desktop;

import java.awt.*;
import java.util.*;
import java.util.List;

/** User account management screen layout. */
final class UserManagementScreen {
  private UserManagementScreen() {}

  static void render(CmsFrame frame, Panel content, Map<String,Object> data) {
    List<Map<String,Object>> rows = frame.listOfMaps(data.get("rows"));
    Panel root = frame.panel(DesktopTheme.PALE);
    root.setLayout(new BorderLayout(0, 12));
    Panel toolbar = DesktopTheme.card();
    toolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));
    ActionButton create = frame.button("+ Create user", DesktopTheme.BLUE);
    create.addActionListener(event -> frame.showUserForm(null));
    Choice selected = new Choice();
    rows.forEach(row -> selected.add(frame.text(row.get("USERNAME")) + " · " + frame.text(row.get("DISPLAY_NAME"))));
    ActionButton edit = frame.button("Edit selected", DesktopTheme.BLUE);
    edit.setEnabled(!rows.isEmpty());
    edit.addActionListener(event -> { if (!rows.isEmpty()) frame.showUserForm(rows.get(selected.getSelectedIndex())); });
    int page = Integer.parseInt(frame.text(data.getOrDefault("userPage", 1)));
    int pages = Integer.parseInt(frame.text(data.getOrDefault("userPages", 1)));
    ActionButton previous = frame.button("Previous", DesktopTheme.NAVY_HOVER);
    previous.setEnabled(page > 1); previous.addActionListener(event -> frame.setUserPage(page - 1));
    ActionButton next = frame.button("Next", DesktopTheme.NAVY_HOVER);
    next.setEnabled(page < pages); next.addActionListener(event -> frame.setUserPage(page + 1));
    toolbar.add(create); toolbar.add(selected); toolbar.add(edit);
    toolbar.add(frame.label("Page " + page + " of " + pages, 11, false, DesktopTheme.MUTED));
    toolbar.add(previous); toolbar.add(next);
    root.add(toolbar, BorderLayout.NORTH);
    root.add(frame.tableSection("Company users · " + data.getOrDefault("totalUsers", rows.size()) + " accounts", rows,
        List.of("USERNAME", "DISPLAY_NAME", "ROLE_NAME", "DISTRICT_NAME", "BRANCH_NAME", "ACTIVE")), BorderLayout.CENTER);
    content.add(root, BorderLayout.CENTER);
  }
}

