package com.cms.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.TextField;
import java.util.List;
import java.util.Map;

/** Customer search and assigned worklist layout. */
final class CustomerWorklistScreen {
  private CustomerWorklistScreen() {}

  static void render(CmsFrame frame, Panel content, Map<String, Object> data) {
    Panel filters = frame.gridPanel("Find a customer");
    Panel fields = new Panel(new GridBagLayout()); fields.setBackground(DesktopTheme.SURFACE);
    TextField query = frame.field("");
    query.setColumns(30);
    ActionButton search = frame.button("Search", DesktopTheme.BLUE);
    ActionButton clear = frame.button("Clear", DesktopTheme.BLUE);
    GridBagConstraints c = new GridBagConstraints(); c.gridy = 0; c.insets = new Insets(4, 6, 4, 8); c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx = 0; fields.add(frame.label("Customer, loan account, or phone", 11, true, DesktopTheme.MUTED), c);
    c.gridx = 1; c.weightx = 1; fields.add(query, c);
    c.gridx = 2; c.weightx = 0; fields.add(search, c);
    c.gridx = 3; fields.add(clear, c);
    filters.add(fields, BorderLayout.CENTER);
    search.addActionListener(e -> frame.reloadCustomers(query.getText()));
    clear.addActionListener(e -> frame.reloadCustomers(""));
    content.add(filters, BorderLayout.NORTH);

    List<Map<String, Object>> rows = frame.listOfMaps(data.get("rows"));
    Panel worklist = frame.gridPanel("Assigned customers");
    worklist.add(CmsGrid.table(rows,
        List.of("CUSTOMER_NO", "NAME", "PHONE", "LOAN_COUNT", "OUTSTANDING", "OVERDUE", "DPD", "RISK"),
        true, row -> frame.openCustomer(row)), BorderLayout.CENTER);
    content.add(worklist, BorderLayout.CENTER);
  }
}

