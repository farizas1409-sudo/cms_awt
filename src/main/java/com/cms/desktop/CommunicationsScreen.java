package com.cms.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.TextField;
import java.util.List;
import java.util.Map;

/** Communications contact entry and searchable history screen. */
final class CommunicationsScreen {
  private CommunicationsScreen() {}

  static void render(CmsFrame frame, Panel content, Map<String, Object> data) {
    content.add(frame.customerHeader(data), BorderLayout.NORTH);
    List<Map<String, Object>> loans = frame.listOfMaps(data.get("loans"));
    Panel split = new Panel(new GridLayout(1, 2, 14, 0));
    split.setBackground(DesktopTheme.PALE);

    Panel form = frame.gridPanel("Log a completed contact");
    Panel fields = new Panel(new GridLayout(0, 2, 12, 12));
    fields.setBackground(DesktopTheme.SURFACE);
    var loan = frame.loanChoice(loans);
    Choice channel = new Choice();
    for (String option : List.of("Email", "SMS", "Telephone", "WhatsApp", "Other")) channel.add(option);
    channel.setFont(DesktopTheme.BODY);
    channel.setForeground(DesktopTheme.INK);
    channel.setBackground(DesktopTheme.SURFACE);
    TextField contact = frame.field("");
    TextField notes = frame.field("");
    Map<String, Object> customer = frame.mapOf(data.get("customer"));
    Runnable populateContact = () -> contact.setText(switch (channel.getSelectedItem()) {
      case "Email" -> frame.text(customer.get("EMAIL"));
      case "SMS" -> frame.text(customer.get("PHONE"));
      case "Telephone" -> frame.text(customer.get("TELEPHONE_NO"));
      case "WhatsApp" -> frame.text(customer.get("WHATSAPP_NO"));
      default -> "";
    });
    channel.addItemListener(event -> populateContact.run());
    populateContact.run();
    frame.addField(fields, "Loan account", loan);
    frame.addField(fields, "Channel", channel);
    frame.addField(fields, "Contact used", contact);
    frame.addField(fields, "Subject / summary", notes);
    ActionButton save = frame.button("Save contact log", DesktopTheme.BLUE);
    save.addActionListener(e -> frame.saveCommunication(loans, loan, channel, contact, notes));
    Panel actions = new Panel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
    actions.setBackground(DesktopTheme.SURFACE);
    actions.add(save);
    Panel entry = new Panel(new BorderLayout(0, 4));
    entry.setBackground(DesktopTheme.SURFACE);
    entry.add(fields, BorderLayout.NORTH);
    entry.add(actions, BorderLayout.CENTER);
    form.add(entry, BorderLayout.CENTER);

    Panel history = frame.gridPanel("Communication history");
    Panel filter = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 3));
    filter.setBackground(DesktopTheme.SURFACE);
    var account = frame.loanChoice(loans);
    ActionButton search = frame.button("Search", DesktopTheme.BLUE);
    ActionButton clear = frame.button("Clear", DesktopTheme.BLUE);
    filter.add(frame.label("Filter by loan account", 11, true, DesktopTheme.MUTED));
    filter.add(account);
    filter.add(search);
    filter.add(clear);
    search.addActionListener(e -> frame.filterByLoan(frame.selectedLoanId(loans, account), "Communications"));
    clear.addActionListener(e -> frame.filterByLoan(null, "Communications"));
    Panel historyBody = new Panel(new BorderLayout(0, 10)); historyBody.setBackground(DesktopTheme.SURFACE);
    filter.setPreferredSize(new java.awt.Dimension(10, 42));
    historyBody.add(filter, BorderLayout.NORTH);
    historyBody.add(CmsGrid.section("Recent contacts", frame.listOfMaps(data.get("tertiary")), List.of(), false, null), BorderLayout.CENTER);
    history.add(historyBody, BorderLayout.CENTER);
    split.add(form);
    split.add(history);
    content.add(split, BorderLayout.CENTER);
  }
}

