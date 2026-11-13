package com.cms.desktop;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Panel;
import java.util.List;
import java.util.Map;

/** Shared loan-account filter and history grid layout. */
final class CustomerHistoryLayout {
  private CustomerHistoryLayout() {}

  static void render(CmsFrame frame, Panel content, Map<String, Object> data,
      String heading, String targetScreen, String rowsKey, String countKey) {
    content.add(frame.customerHeader(data), BorderLayout.NORTH);
    List<Map<String, Object>> loans = frame.listOfMaps(data.get("loans"));
    List<Map<String, Object>> rows = frame.listOfMaps(data.get(rowsKey));
    if (rows.isEmpty() && "Follow-up History".equals(targetScreen)) rows = frame.listOfMaps(data.get("secondary"));
    Panel history = frame.gridPanel(heading);
    Panel filter = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 3));
    filter.setBackground(DesktopTheme.SURFACE);
    var account = frame.loanChoice(loans);
    ActionButton search = frame.button("Search", DesktopTheme.BLUE);
    ActionButton clear = frame.button("Clear", DesktopTheme.BLUE);
    filter.add(frame.label("Filter by loan account", 11, true, DesktopTheme.MUTED));
    filter.add(account);
    filter.add(search);
    filter.add(clear);
    search.addActionListener(e -> frame.filterByLoan(frame.selectedLoanId(loans, account), targetScreen));
    clear.addActionListener(e -> frame.filterByLoan(null, targetScreen));
    Panel filterBar = new Panel(new BorderLayout(0, 8)); filterBar.setBackground(DesktopTheme.SURFACE);
    filterBar.add(filter, BorderLayout.CENTER);
    filterBar.setPreferredSize(new java.awt.Dimension(10, 46));
    history.add(filterBar, BorderLayout.NORTH);
    Panel grid = CmsGrid.table(rows, List.of(), false, null);
    history.add(grid, BorderLayout.CENTER);
    content.add(history, BorderLayout.CENTER);
  }
}

