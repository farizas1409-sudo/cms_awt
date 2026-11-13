package com.cms.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.List;
import java.util.Map;

/** Follow-up entry screen. */
final class CustomerFollowupScreen {
  private CustomerFollowupScreen() {}

  static void render(CmsFrame frame, Panel content, Map<String, Object> data) {
    content.add(frame.customerHeader(data), BorderLayout.NORTH);
    List<Map<String, Object>> loans = frame.listOfMaps(data.get("loans"));
    var loan = frame.loanChoice(loans);
    Panel form = frame.gridPanel("Record a customer follow-up");
    Panel fields = new Panel(new GridLayout(0, 2, 10, 8));
    fields.setBackground(DesktopTheme.SURFACE);
    TextField contactDate = frame.field("");
    TextField channel = frame.field("Telephone");
    TextField outcome = frame.field("Reached");
    TextField promiseAmount = frame.field("");
    TextField promiseDate = frame.field("");
    TextField nextAction = frame.field("");
    TextField remarks = frame.field("");
    frame.addField(fields, "Loan account", loan);
    frame.addField(fields, "Contact date (YYYY-MM-DD)", frame.dateField(contactDate, "contact date"));
    frame.addField(fields, "Contact method", channel);
    frame.addField(fields, "Outcome", outcome);
    frame.addField(fields, "Promise amount (INR)", promiseAmount);
    frame.addField(fields, "Promise date (YYYY-MM-DD)", frame.dateField(promiseDate, "promise date"));
    frame.addField(fields, "Next action (YYYY-MM-DD)", frame.dateField(nextAction, "next action date"));
    frame.addField(fields, "Remarks", remarks);
    ActionButton save = frame.button("Save follow-up", DesktopTheme.BLUE);
    save.addActionListener(e -> frame.saveFollowup(loan, loans, contactDate, channel, outcome,
        promiseAmount, promiseDate, nextAction, remarks));
    fields.add(new Label(""));
    fields.add(save);
    form.add(fields, BorderLayout.CENTER);
    Panel split = new Panel(new GridLayout(1, 2, 12, 0));
    split.setBackground(DesktopTheme.PALE);
    split.add(form);
    split.add(frame.tableCard("Recent follow-up activity", frame.listOfMaps(data.get("secondary"))));
    content.add(split, BorderLayout.CENTER);
  }
}

