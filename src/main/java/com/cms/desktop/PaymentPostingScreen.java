package com.cms.desktop;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.TextField;
import java.util.List;
import java.util.Map;

/** Confirmed payment entry and posting history screen. */
final class PaymentPostingScreen {
  private PaymentPostingScreen() {}

  static void render(CmsFrame frame, Panel content, Map<String, Object> data) {
    content.add(frame.customerHeader(data), BorderLayout.NORTH);
    List<Map<String, Object>> loans = frame.listOfMaps(data.get("loans"));
    var account = frame.loanChoice(loans);
    Panel split = new Panel(new GridLayout(1, 2, 14, 0));
    split.setBackground(DesktopTheme.PALE);
    Panel form = frame.gridPanel("Post a payment");
    Panel fields = new Panel(new GridLayout(0, 2, 12, 12));
    fields.setBackground(DesktopTheme.SURFACE);
    TextField amount = frame.field("");
    TextField mode = frame.field("UPI");
    TextField reference = frame.field("");
    frame.addField(fields, "Loan account", account);
    frame.addField(fields, "Amount (INR)", amount);
    frame.addField(fields, "Payment mode", mode);
    frame.addField(fields, "Unique transaction reference", reference);
    ActionButton post = frame.button("Post confirmed payment", DesktopTheme.BLUE);
    post.addActionListener(e -> frame.savePayment(loans, account, amount, mode, reference));
    Panel actions = new Panel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
    actions.setBackground(DesktopTheme.SURFACE);
    actions.add(post);
    Panel entry = new Panel(new BorderLayout(0, 4));
    entry.setBackground(DesktopTheme.SURFACE);
    entry.add(fields, BorderLayout.NORTH);
    entry.add(actions, BorderLayout.CENTER);
    form.add(entry, BorderLayout.CENTER);
    split.add(form);
    Panel history = frame.gridPanel("Payment posting history");
    history.add(CmsGrid.table(frame.listOfMaps(data.get("rows")), List.of(), false, null), BorderLayout.CENTER);
    split.add(history);
    content.add(split, BorderLayout.CENTER);
  }
}

