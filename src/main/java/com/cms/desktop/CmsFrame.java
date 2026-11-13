package com.cms.desktop;

import com.cms.config.SystemParameters;
import com.cms.model.Actor;
import com.cms.model.Identity;
import com.cms.service.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Main AWT window and service-backed CMS screens. */
final class CmsFrame extends Frame {
  private static final Logger LOG = LogManager.getLogger(CmsFrame.class);
  private static Color NAVY = DesktopTheme.NAVY;
  private static Color NAVY_HOVER = DesktopTheme.NAVY_HOVER;
  private static Color BLUE = DesktopTheme.BLUE;
  private static Color PALE = DesktopTheme.PALE;
  private static Color INK = DesktopTheme.INK;
  private static Color MUTED = DesktopTheme.MUTED;
  private static Color BORDER = DesktopTheme.BORDER;

  private final IdentityService identityService = new IdentityService();
  private final DashboardService dashboardService = new DashboardService();
  private final CustomersService customersService = new CustomersService();
  private final FollowupService followupService = new FollowupService();
  private final PaymentService paymentService = new PaymentService();
  private final HistoryService historyService = new HistoryService();
  private final CommunicationsService communicationsService = new CommunicationsService();
  private final UsersService usersService = new UsersService();
  private final RolesService rolesService = new RolesService();
  private final MastersService mastersService = new MastersService();
  private final RiskService riskService = new RiskService();
  private final AllocationService allocationService = new AllocationService();
  private final OwnerService ownerService = new OwnerService();
  private final CollectorPerformanceService performanceService = new CollectorPerformanceService();
  private final ReportsService reportsService = new ReportsService();
  private final AuditService auditService = new AuditService();
  private final ProfileService profileService = new ProfileService();
  private final AppearanceService appearanceService = new AppearanceService();

  private Identity identity;
  private Actor actor;
  private Panel workspace;
  private Panel navigation;
  private final Map<String,ActionButton> navigationButtons = new LinkedHashMap<>();
  private Long customerId;
  private Long historyFilterId;
  private String currentScreen = "Dashboard";
  private String masterType = "district";
  private boolean masterFetched;
  private int userPage = 1;
  private String customerQuery = "";
  private String auditFrom = "";
  private String auditTo = "";
  private String reportQuery = "";
  private String performanceDistrict;
  private String performanceRisk = "";
  private String reportRisk = "All risks";

  CmsFrame() {
    super(SystemParameters.get("application.name"));
    DesktopTheme.apply(
        SystemParameters.get("ui.default.theme"),
        SystemParameters.get("ui.default.accent"),
        SystemParameters.get("ui.default.font-size"),
        SystemParameters.get("ui.default.font-family"));
    syncThemeColors();
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    setSize(Math.min(1380, Math.max(1040, screen.width - 120)),
        Math.min(860, Math.max(660, screen.height - 140)));
    setMinimumSize(new Dimension(980, 620));
    setLocationRelativeTo(null);
    setBackground(PALE);
    addWindowListener(new WindowAdapter() {
      @Override public void windowClosing(WindowEvent e) { dispose(); }
    });
  }

  void showLogin() {
    removeAll();
    setLayout(new BorderLayout());
    Panel login = LoginScreen.render(this::authenticate);
    add(login, BorderLayout.CENTER);
    validate();
    setVisible(true);
  }

  private void authenticate(String username, String password, Label status) {
    try {
      OptionalLongResult result = authenticateUser(username, password);
      if (result.id < 0) { status.setText("Sign-in failed. Check username and password."); return; }
      identity = identityService.findActiveIdentity(result.id).orElseThrow();
      actor = new Actor(identity.id(), identity.username(), identity.displayName(),
          identity.role(), identity.permissions());
      DesktopTheme.apply(appearanceService.load(actor));
      syncThemeColors();
      LOG.info("Desktop sign-in succeeded username={} userId={} role={}", identity.username(), identity.id(), identity.role());
      showApplication();
    } catch (Exception failure) {
      LOG.error("Desktop sign-in failed username={}", username, failure);
      status.setText(shortMessage(failure));
    }
  }

  private OptionalLongResult authenticateUser(String username, String password) throws SQLException {
    var authenticated = identityService.authenticate(username == null ? "" : username.trim(), password == null ? "" : password);
    return new OptionalLongResult(authenticated.isPresent() ? authenticated.getAsLong() : -1);
  }

  private record OptionalLongResult(long id) {}

  private void showApplication() {
    removeAll();
    setLayout(new BorderLayout());
    Panel appHeader = new Panel(new BorderLayout());
    appHeader.setBackground(DesktopTheme.SURFACE);
    Panel header = new Panel(new BorderLayout(18, 0));
    header.setBackground(DesktopTheme.SURFACE);
    header.setPreferredSize(new Dimension(10, 64));
    Panel brand = new Panel(new FlowLayout(FlowLayout.LEFT, 12, 10));
    brand.setBackground(DesktopTheme.SURFACE);
    LogoMark mark = new LogoMark();
    brand.add(mark);
    Panel brandText = new Panel(new GridLayout(2, 1, 0, 1)); brandText.setBackground(DesktopTheme.SURFACE);
    brandText.add(label(SystemParameters.get("application.short-name"), 21, true, INK));
    brandText.add(label("COLLECTIONS MANAGEMENT", 9, true, MUTED));
    brand.add(brandText);
    header.add(brand, BorderLayout.WEST);
    Panel userBar = new Panel(new FlowLayout(FlowLayout.RIGHT, 8, 15)); userBar.setBackground(DesktopTheme.SURFACE);
    userBar.add(label(SystemParameters.get("application.company-name"), 11, true, MUTED));
    String identityText = "COLLECTOR".equalsIgnoreCase(identity.role())
        ? "  Collector ID: " + identity.username() + "  ·  " + identity.displayName() + "  ·  " + identity.role() + "  "
        : "  " + identity.displayName() + "  ·  " + identity.role() + "  ";
    Label identityChip = label(identityText, 12, true, INK);
    identityChip.setBackground(DesktopTheme.SOFT_BLUE);
    userBar.add(identityChip);
    ActionButton preferences = button("Display settings", DesktopTheme.SOFT_BLUE);
    preferences.setPreferredSize(new Dimension(126, 30));
    preferences.setForeground(BLUE);
    preferences.addActionListener(e -> openScreen("Appearance Settings")); userBar.add(preferences);
    ActionButton logout = button("Sign out", new Color(255, 239, 240));
    logout.setPreferredSize(new Dimension(82, 30));
    logout.setForeground(DesktopTheme.DANGER);
    logout.addActionListener(e -> { identity = null; actor = null; showLogin(); }); userBar.add(logout);
    header.add(userBar, BorderLayout.EAST);
    Panel headerRule = new Panel(); headerRule.setBackground(DesktopTheme.BORDER);
    headerRule.setPreferredSize(new Dimension(1, 1));
    appHeader.add(header, BorderLayout.CENTER); appHeader.add(headerRule, BorderLayout.SOUTH);
    add(appHeader, BorderLayout.NORTH);

    Panel side = new Panel(new BorderLayout()); side.setBackground(NAVY); side.setPreferredSize(new Dimension(216, 10));
    navigation = new Panel(new GridLayout(0, 1, 0, 3)); navigation.setBackground(NAVY);
    navigationButtons.clear();
    Panel navRows = new Panel(new BorderLayout()); navRows.setBackground(NAVY); navRows.add(navigation, BorderLayout.NORTH);
    ScrollPane navScroll = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED); navScroll.add(navRows); side.add(navScroll, BorderLayout.CENTER);
    Panel navFooter = new Panel(new BorderLayout()); navFooter.setBackground(NAVY); navFooter.setPreferredSize(new Dimension(216, 46));
    navFooter.add(label("  Bharat Small Finance Company", 10, false, new Color(190, 208, 231)), BorderLayout.CENTER); side.add(navFooter, BorderLayout.SOUTH);
    add(side, BorderLayout.WEST);
    workspace = new Panel(new BorderLayout()); workspace.setBackground(PALE); add(workspace, BorderLayout.CENTER);
    for (String item : availableScreens()) {
      ActionButton b = button("   " + item, NAVY);
      b.setForeground(new Color(225, 235, 249)); b.setPreferredSize(new Dimension(202, 32)); b.setFont(DesktopTheme.font(11, true));
      b.addActionListener(e -> openScreen(item)); navigation.add(b); navigationButtons.put(item,b);
    }
    setLocationRelativeTo(null);
    setVisible(true);
    openScreen(identity.permissions().contains("COLLECT") ? "Dashboard" : availableScreens().get(0));
  }

  private List<String> availableScreens() {
    List<String> names = new ArrayList<>();
    if (has("COLLECT")) { names.add("Dashboard"); names.add("Customer Worklist"); names.add("Collector Reports"); }
    if (has("OWNER_REPORT")) { names.add("Performance Report"); names.add("Collector Performance"); }
    if (has("USER_MANAGE")) names.add("User Management");
    if (has("ROLE_MANAGE")) names.add("Roles & Permissions");
    if (has("MASTER_MANAGE")) names.add("Application Masters");
    if (has("RISK_EXECUTE")) names.add("Risk Rules");
    if (has("ALLOCATE")) names.add("Customer Allocation");
    if (has("ROLE_MANAGE")) names.add("Audit Logs");
    names.add("Change Password");
    return names;
  }

  String currentScreenName() { return currentScreen; }
  String auditFromValue() { return auditFrom; }
  String auditToValue() { return auditTo; }
  void refreshScreen() { openScreen(currentScreen); }
  void setUserPage(int page) { userPage = Math.max(1, page); openScreen(currentScreen); }
  void createRole(String name) {
    try { rolesService.save(actor, "CREATE", null, name, List.of()); message("Role created."); openScreen(currentScreen); }
    catch (Exception failure) { showError(failure); }
  }
  void deleteRisk(Map<String,Object> row) {
    try { message(riskService.delete(actor, number(row.get("ID")))); openScreen(currentScreen); }
    catch (Exception failure) { showError(failure); }
  }
  void runRiskAssessment() {
    try { riskService.run(actor); message("Risk rules executed."); openScreen(currentScreen); }
    catch (Exception failure) { showError(failure); }
  }
  void runAllocation(boolean allBranches, Map<String,Object> branch) {
    try {
      Long branchId = allBranches || branch == null ? null : number(branch.get("ID"));
      String outcome = allocationService.run(actor, allBranches ? "ALL" : "BRANCH", branchId);
      String scope = allBranches ? "All branches" : text(branch.get("NAME"));
      var rows = listOfMaps(allocationService.load(actor).get("rows")).stream()
          .filter(row -> allBranches || branchId.equals(number(row.get("ID"))))
          .toList();
      long customers = rows.stream().mapToLong(row -> ((Number) row.get("CUSTOMERS")).longValue()).sum();
      long allocated = rows.stream().mapToLong(row -> ((Number) row.get("ALLOCATED")).longValue()).sum();
      long unassigned = rows.stream().mapToLong(row -> ((Number) row.get("UNASSIGNED")).longValue()).sum();
      openScreen(currentScreen);
      message("Scope: " + scope + "\nCustomers: " + customers
          + "\nCurrently allocated: " + allocated + "\nCurrently unassigned: " + unassigned
          + "\n" + outcome);
    } catch (Exception failure) { showError(failure); }
  }
  void filterAudit(String from, String to) {
    auditFrom=from==null?"":from.trim(); auditTo=to==null?"":to.trim(); openScreen("Audit Logs");
  }
  Panel dateField(TextField target, String label) {
    Panel control = new Panel(new BorderLayout(6, 0));
    ActionButton picker = button("CALENDAR_ICON", NAVY_HOVER);
    picker.getAccessibleContext().setAccessibleName("Choose " + label);
    picker.addActionListener(e -> pickDate(target));
    control.add(target, BorderLayout.CENTER);
    Panel action = new Panel(new GridBagLayout());
    action.add(picker);
    control.add(action, BorderLayout.EAST);
    return control;
  }

  private void pickDate(TextField target) {
    java.time.LocalDate initial;
    try { initial = target.getText().isBlank() ? java.time.LocalDate.now() : java.time.LocalDate.parse(target.getText().trim()); }
    catch (java.time.DateTimeException invalid) { initial = java.time.LocalDate.now(); }
    Dialog picker = new ClosableDialog(this, "Choose date", true);
    picker.setLayout(new BorderLayout(6, 6));
    picker.setBackground(PALE);
    java.time.YearMonth[] visibleMonth = {java.time.YearMonth.from(initial)};
    int[] selectedDay = {initial.getDayOfMonth()};
    Panel calendar = new Panel(new BorderLayout(4, 6));
    calendar.setBackground(Color.WHITE);
    Panel monthBar = new Panel(new BorderLayout(8, 0));
    monthBar.setBackground(Color.WHITE);
    Label monthTitle = label("", 13, true, INK);
    ActionButton previous = button("‹", NAVY_HOVER);
    ActionButton next = button("›", NAVY_HOVER);
    previous.addActionListener(e -> {
      visibleMonth[0] = visibleMonth[0].minusMonths(1);
      selectedDay[0] = Math.min(selectedDay[0], visibleMonth[0].lengthOfMonth());
      refreshCalendar(calendar, monthTitle, visibleMonth, selectedDay);
    });
    next.addActionListener(e -> {
      visibleMonth[0] = visibleMonth[0].plusMonths(1);
      selectedDay[0] = Math.min(selectedDay[0], visibleMonth[0].lengthOfMonth());
      refreshCalendar(calendar, monthTitle, visibleMonth, selectedDay);
    });
    monthBar.add(previous, BorderLayout.WEST);
    Choice year = new Choice();
    year.getAccessibleContext().setAccessibleName("Calendar year");
    int currentYear = java.time.Year.now().getValue();
    for (int option = currentYear - 10; option <= currentYear + 10; option++) {
      year.add(Integer.toString(option));
    }
    Runnable syncYear = () -> {
      String displayedYear = Integer.toString(visibleMonth[0].getYear());
      boolean listed = false;
      for (int index = 0; index < year.getItemCount(); index++) {
        if (displayedYear.equals(year.getItem(index))) {
          listed = true;
          break;
        }
      }
      if (!listed) year.add(displayedYear);
      year.select(displayedYear);
    };
    syncYear.run();
    previous.addActionListener(e -> syncYear.run());
    next.addActionListener(e -> syncYear.run());
    year.addItemListener(e -> {
      visibleMonth[0] = visibleMonth[0].withYear(Integer.parseInt(year.getSelectedItem()));
      selectedDay[0] = Math.min(selectedDay[0], visibleMonth[0].lengthOfMonth());
      refreshCalendar(calendar, monthTitle, visibleMonth, selectedDay);
    });
    Panel monthControls = new Panel(new FlowLayout(FlowLayout.CENTER, 4, 0));
    monthControls.add(monthTitle);
    monthControls.add(year);
    monthBar.add(monthControls, BorderLayout.CENTER);
    monthBar.add(next, BorderLayout.EAST);
    calendar.add(monthBar, BorderLayout.NORTH);
    picker.add(calendar, BorderLayout.CENTER);
    refreshCalendar(calendar, monthTitle, visibleMonth, selectedDay);
    Panel actions = new Panel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
    actions.setBackground(PALE);
    ActionButton clear = button("Clear date", DesktopTheme.NAVY_HOVER);
    clear.addActionListener(e -> { target.setText(""); picker.dispose(); });
    ActionButton today = button("Today", DesktopTheme.NAVY_HOVER);
    today.addActionListener(e -> {
      java.time.LocalDate todayDate = java.time.LocalDate.now();
      target.setText(todayDate.toString()); picker.dispose();
    });
    ActionButton use = button("Use date", BLUE);
    use.addActionListener(e -> {
      try {
        target.setText(visibleMonth[0].atDay(selectedDay[0]).toString());
        picker.dispose();
      } catch (RuntimeException invalid) { showError(new IllegalArgumentException("Choose a valid date.")); }
    });
    actions.add(clear); actions.add(today); actions.add(use);
    picker.add(actions, BorderLayout.SOUTH);
    picker.setResizable(false);
    picker.setSize(450, 310); picker.setLocationRelativeTo(this); picker.setVisible(true);
  }

  private void refreshCalendar(Panel calendar, Label monthTitle,
      java.time.YearMonth[] visibleMonth, int[] selectedDay) {
    monthTitle.setText(visibleMonth[0].getMonth().getDisplayName(
        java.time.format.TextStyle.FULL, Locale.ENGLISH) + " " + visibleMonth[0].getYear());
    Panel days = new Panel(new GridLayout(0, 7, 3, 3));
    days.setBackground(Color.WHITE);
    for (String weekday : List.of("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")) {
      Label label = label(weekday, 11, true, MUTED);
      label.setAlignment(Label.CENTER);
      days.add(label);
    }
    int firstDay = visibleMonth[0].atDay(1).getDayOfWeek().getValue() % 7;
    for (int blank = 0; blank < firstDay; blank++) days.add(new Label(""));
    for (int day = 1; day <= visibleMonth[0].lengthOfMonth(); day++) {
      int chosen = day;
      ActionButton date = button(Integer.toString(day),
          chosen == selectedDay[0] ? BLUE : DesktopTheme.SURFACE);
      if (chosen != selectedDay[0]) date.setForeground(INK);
      date.addActionListener(e -> {
        selectedDay[0] = chosen;
        refreshCalendar(calendar, monthTitle, visibleMonth, selectedDay);
      });
      days.add(date);
    }
    calendar.add(days, BorderLayout.CENTER);
    calendar.validate();
    calendar.repaint();
  }
  void runCollectorPerformance(String district, String risk) {
    performanceDistrict = district;
    performanceRisk = risk;
    openScreen("Collector Performance");
  }

  void updateReport(String query, String risk) { reportQuery=query==null?"":query.trim(); reportRisk=risk==null?"All risks":risk; openScreen(currentScreen); }
  void exportReport(List<Map<String,Object>> rows) { exportRows(rows); }
  void createMaster(String type) { showMasterForm(type); }
  void fetchMaster(String type) { loadMaster(type, true); }
  void createRiskRule() { showRiskForm(null); }
  void editRiskRule(Map<String,Object> row) { showRiskForm(row); }
  void createRoleDialog() {
    formDialog("Create role", Map.of("Role name", ""), values -> createRole(values.get("Role name")));
  }
  void applyAppearance(String theme, String accent, String size, String family) {
    try {
      appearanceService.save(actor, theme, accent, size, family);
      DesktopTheme.apply(theme, accent, size, family);
      syncThemeColors();
      message("Settings saved for the application.");
      showApplication();
    }
    catch (Exception failure) { showError(failure); }
  }
  private void syncThemeColors() {
    NAVY=DesktopTheme.NAVY; NAVY_HOVER=DesktopTheme.NAVY_HOVER; BLUE=DesktopTheme.BLUE;
    PALE=DesktopTheme.PALE; INK=DesktopTheme.INK; MUTED=DesktopTheme.MUTED; BORDER=DesktopTheme.BORDER;
  }
  void selectMasterType(String type) { masterType=normalizeMasterType(type); masterFetched=false; }
  void updatePassword(String current, String next) {
    try { profileService.save(actor, current, next); message("Password updated."); }
    catch (Exception failure) { showError(failure); }
  }

  void openScreen(String screen) {
    currentScreen = screen;
    for (var entry : navigationButtons.entrySet()) {
      ActionButton nav = entry.getValue();
      nav.setBackground(entry.getKey().equals(screen) ? BLUE : NAVY);
      nav.setForeground(Color.WHITE);
    }
    LOG.info("Opening desktop screen={} userId={} username={}", screen, identity.id(), identity.username());
    workspace.removeAll();
    Panel body = new Panel(new BorderLayout(0, 0)); body.setBackground(PALE);
    Panel title = new Panel(new BorderLayout(10, 0)); title.setBackground(DesktopTheme.SURFACE); title.setPreferredSize(new Dimension(10, 62));
    Panel titleRail = new Panel(); titleRail.setBackground(BLUE); titleRail.setPreferredSize(new Dimension(4, 1));
    Panel titleLeft = new Panel(new GridLayout(2, 1, 0, 2)); titleLeft.setBackground(DesktopTheme.SURFACE);
    titleLeft.add(label("  " + screenGroup(screen).toUpperCase(Locale.ROOT), 10, true, BLUE));
    titleLeft.add(label("  " + screen, 20, true, INK));
    Panel titleText = new Panel(new BorderLayout(0, 3)); titleText.setBackground(DesktopTheme.SURFACE);
    titleText.add(titleLeft, BorderLayout.CENTER);
    titleText.add(label("  " + screenSubtitle(screen), 11, false, MUTED), BorderLayout.SOUTH);
    title.add(titleRail, BorderLayout.WEST);
    title.add(titleText, BorderLayout.CENTER);
    if (customerId != null && screen.startsWith("Follow-up") || customerId != null && screen.startsWith("Payment") || customerId != null && screen.equals("Communications")) {
      ActionButton back = button("Back to Worklist", BLUE);
      back.addActionListener(e -> openScreen("Customer Worklist"));
      Panel actions = new Panel(new GridBagLayout());
      GridBagConstraints position = new GridBagConstraints();
      position.insets = new Insets(0, 8, 0, 12);
      actions.add(back, position);
      title.add(actions, BorderLayout.EAST);
    }
    body.add(title, BorderLayout.NORTH);
    Panel outer = panel(PALE); outer.setLayout(new BorderLayout());
    Panel content = panel(PALE); content.setLayout(new BorderLayout(8, 8));
    outer.add(spacer(PALE, 8, 8), BorderLayout.NORTH);
    outer.add(spacer(PALE, 8, 8), BorderLayout.WEST);
    outer.add(spacer(PALE, 8, 8), BorderLayout.EAST);
    outer.add(spacer(PALE, 8, 8), BorderLayout.SOUTH);
    outer.add(content, BorderLayout.CENTER);
    try {
      switch (screen) {
        case "Dashboard" -> DashboardScreen.render(content, dashboardService.load(actor), row -> openCustomer(row));
        case "Customer Worklist" -> CustomerWorklistScreen.render(this, content, customersService.load(actor, 1, customerQuery, "", "", "", "", ""));
        case "Follow-up" -> CustomerFollowupScreen.render(this, content, followupService.load(actor, customerId, 1, null));
        case "Payment Posting" -> PaymentPostingScreen.render(this, content, paymentService.load(actor, customerId, null, null, 1));
        case "Follow-up History" -> FollowupHistoryScreen.render(this, content, followupService.load(actor, customerId, 1, historyFilterId));
        case "Payment History" -> PaymentHistoryScreen.render(this, content, historyService.load(actor, customerId, 1, historyFilterId, null));
        case "Communications" -> CommunicationsScreen.render(this, content, communicationsService.load(actor, customerId, 1, historyFilterId, null));
        case "User Management" -> UserManagementScreen.render(this, content, usersService.load(actor, null, userPage));
        case "Roles & Permissions" -> RolesPermissionsScreen.render(this, content, rolesService.load(actor, null));
        case "Application Masters" -> ApplicationMastersScreen.render(this, content, mastersService.load(actor, masterType, masterFetched));
        case "Risk Rules" -> RiskRulesScreen.render(this, content, riskService.load(actor, null, null, false, null));
        case "Customer Allocation" -> CustomerAllocationScreen.render(this, content, allocationService.load(actor));
        case "Performance Report" -> PerformanceReportScreen.render(this, content, ownerService.load(actor, null, null, null, null, true));
        case "Collector Performance" -> CollectorPerformanceScreen.render(this, content, performanceService.load(actor, null, null, performanceDistrict, null, null, null, performanceRisk, 1, performanceDistrict != null));
        case "Collector Reports" -> CollectorReportsScreen.render(this, content, reportsService.load(actor, 1, reportQuery, reportRisk.equals("All risks") ? "" : reportRisk, ""));
        case "Audit Logs" -> AuditLogsScreen.render(this, content, auditService.load(actor, blank(auditFrom), blank(auditTo), null, 1));
        case "Change Password" -> ChangePasswordScreen.render(this, content);
        case "Appearance Settings" -> AppearanceSettingsScreen.render(this, content, appearanceService.load(actor));
        default -> content.add(label("Choose a screen from the navigation.", 14, false, INK));
      }
    } catch (Exception failure) { LOG.error("Unable to load desktop screen={} userId={}", screen, identity.id(), failure); content.add(error(shortMessage(failure)), BorderLayout.NORTH); }
    body.add(outer, BorderLayout.CENTER);
    workspace.add(body, BorderLayout.CENTER); workspace.validate(); workspace.repaint();
  }

  void openCustomer(Map<String,Object> row) {
    customerId=number(row.get("ID"));historyFilterId=null;showApplication();openScreen("Follow-up");
  }

  Panel customerHeader(Map<String,Object> data) { return CustomerOverviewScreen.render(this, data); }

  private String navIcon(String screen) {
    return "";
  }

  private String screenGroup(String screen) {
    if (List.of("Dashboard", "Customer Worklist", "Follow-up", "Payment Posting", "Follow-up History", "Payment History", "Communications").contains(screen)) return "Collector Workspace";
    if (List.of("Performance Report", "Collector Performance", "Collector Reports").contains(screen)) return "Reports";
    if (List.of("User Management", "Roles & Permissions", "Application Masters", "Risk Rules", "Customer Allocation", "Audit Logs").contains(screen)) return "Administration";
    if (screen.equals("Appearance Settings")) return "Preferences";
    return "Account";
  }

  private String screenSubtitle(String screen) {
    return switch (screen) {
      case "Dashboard" -> "A summary of your assigned collection portfolio and today's activity.";
      case "Customer Worklist" -> "Search assigned customers and open an account to take action.";
      case "Follow-up" -> "Record a customer contact, payment promise, and next action.";
      case "Payment Posting" -> "Record confirmed customer payments and review posting history.";
      case "Follow-up History" -> "Review the recorded customer contact history by loan account.";
      case "Payment History" -> "Review customer receipts and payment outcomes by loan account.";
      case "Communications" -> "Record completed calls, messages, and other customer contacts.";
      case "User Management" -> "Create and maintain staff accounts and branch assignments.";
      case "Roles & Permissions" -> "Control which screens and operations each role can access.";
      case "Application Masters" -> "Fetch, review, and add the application reference data.";
      case "Risk Rules" -> "Set delinquency thresholds and run loan risk assessment.";
      case "Customer Allocation" -> "Assign eligible customers to collectors branch by branch.";
      case "Audit Logs" -> "Review administrative changes and operational events.";
      case "Performance Report" -> "View portfolio results across districts, branches, and risk.";
      case "Collector Performance" -> "Compare collector activity and collections for a period.";
      case "Collector Reports" -> "Review customers and loans assigned to your worklist.";
      case "Appearance Settings" -> "Set the appearance used by your desktop application.";
      case "Change Password" -> "Update your sign-in password.";
      default -> "Collections Management System";
    };
  }

  String pageHeading(String screen) { return screen.equals("Collector Reports") ? "Assigned customer loans" : screen; }
  String reportTitle(String key) {
    return switch (key) { case "riskRows" -> "Risk by category"; case "portfolioRows" -> "Outstanding by loan product";
      case "summaryRows" -> "Collector summary"; case "activityRows" -> "Collector action details"; default -> "Report details"; };
  }

  private Panel spacer(Color color, int width, int height) {
    Panel space = new Panel(); space.setBackground(color); space.setPreferredSize(new Dimension(width, height)); return space;
  }

  Panel tableSection(String title, List<Map<String,Object>> rows) { return tableSection(title, rows, List.of()); }
  Panel tableSection(String title, List<Map<String,Object>> rows, List<String> columns) {
    Panel section = gridPanel(title + "  ·  " + rows.size() + (rows.size() == 1 ? " record" : " records"));
    section.add(table(rows, columns, false), BorderLayout.CENTER);
    return section;
  }

  Panel emptySection(String message) {
    Panel section = gridPanel("Application masters");
    Label note = label(message, 13, false, MUTED); note.setBackground(DesktopTheme.SURFACE); section.add(note, BorderLayout.CENTER); return section;
  }

  Panel metricPanel(Map<String,Object> summary) {
    Panel metrics = panel(PALE); metrics.setLayout(new GridLayout(1, Math.max(1, summary.size()), 10, 0));
    for (var entry : summary.entrySet()) metrics.add(metricCard(human(entry.getKey()), entry.getValue(), "Current scope", BLUE));
    return metrics;
  }

  private Panel metricCard(String title, Object value, String note, Color accent) {
    Panel card = panel(Color.WHITE); card.setLayout(new BorderLayout(6, 4)); card.setBackground(DesktopTheme.SURFACE);
    Panel stripe = panel(accent); stripe.setPreferredSize(new Dimension(5, 1)); card.add(stripe, BorderLayout.WEST);
    Panel text = new Panel(new GridLayout(3, 1, 0, 1)); text.setBackground(DesktopTheme.SURFACE);
    text.add(label("  " + title, 11, false, MUTED)); text.add(label("  " + (value == null ? "—" : value), 19, true, INK)); text.add(label("  " + note, 10, false, MUTED)); card.add(text, BorderLayout.CENTER);
    card.setPreferredSize(new Dimension(160, 76)); return card;
  }

  String rupees(Object amount) {
    if (amount == null) return "INR 0.00";
    try { return "INR " + new java.text.DecimalFormat("#,##,##0.00").format(new BigDecimal(amount.toString())); }
    catch (NumberFormatException invalid) { return "INR " + amount; }
  }

  private void exportRows(List<Map<String,Object>> rows) {
    if (rows.isEmpty()) { message("There are no records to export."); return; }
    FileDialog dialog = new FileDialog(this, "Export report as CSV", FileDialog.SAVE); dialog.setFile(currentScreen.toLowerCase(Locale.ROOT).replace(' ', '-') + ".csv"); dialog.setVisible(true);
    if (dialog.getFile() == null) return;
    java.nio.file.Path path = java.nio.file.Path.of(dialog.getDirectory(), dialog.getFile());
    try (var writer = java.nio.file.Files.newBufferedWriter(path, java.nio.charset.StandardCharsets.UTF_8)) {
      List<String> keys = new ArrayList<>(rows.get(0).keySet());
      writer.write(String.join(",", keys.stream().map(this::csv).toList())); writer.newLine();
      for (Map<String,Object> row : rows) { writer.write(String.join(",", keys.stream().map(key -> csv(text(row.get(key)))).toList())); writer.newLine(); }
      message("CSV exported to " + path.toAbsolutePath());
    } catch (Exception ex) { showError(ex); }
  }

  private String csv(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }

  void showUserForm(Map<String,Object> existing){
    try {
      Map<String,Object> data = usersService.load(actor, existing == null ? null : number(existing.get("ID")), 1);
      Map<String,Object> selectedUser = mapOf(data.get("customer"));
      Map<String,Object> user = new LinkedHashMap<>(existing == null ? Map.of() : existing);
      user.putAll(selectedUser);
      List<Map<String,Object>> roleRows = listOfMaps(data.get("roleOptions"));
      List<Map<String,Object>> districtRows = listOfMaps(data.get("districtOptions"));
      List<Map<String,Object>> branchRows = listOfMaps(data.get("branchOptions"));
      Dialog dialog = new ClosableDialog(this, existing == null ? "Create CMS user" : "Edit CMS user", true);
      dialog.setLayout(new BorderLayout(10, 8)); dialog.setBackground(PALE);
      Panel fields = new Panel(new GridBagLayout()); fields.setBackground(DesktopTheme.SURFACE);
      TextField username = field(text(user.get("USERNAME")));
      username.setColumns(30);
      if (existing != null) username.setEnabled(false);
      TextField display = field(text(user.get("DISPLAY_NAME")));
      display.setColumns(30);
      TextField password = field(existing == null ? SystemParameters.get("security.default-user-password") : "");
      password.setColumns(30);
      password.setEchoChar('\u2022');
      Choice role = optionChoice(roleRows, "NAME");
      TextField district = field("");
      district.setEditable(false);
      district.setBackground(new Color(239, 243, 249));
      Choice branch = new Choice();
      branch.add("Company-wide access");
      for (Map<String,Object> branchRow : branchRows) {
        branch.add(text(branchRow.get("NAME")) + "  ·  " + text(branchRow.get("ID")));
      }
      Checkbox active = new Checkbox("User can sign in", existing == null || !"0".equals(text(user.get("ACTIVE"))));
      active.setFont(DesktopTheme.BODY);
      active.setForeground(INK);
      active.setBackground(DesktopTheme.SURFACE);
      selectOption(role, roleRows, number(user.get("ROLE_ID")));
      selectBranch(branch, branchRows, number(user.get("BRANCH_ID")));
      updateDistrictFromBranch(district, branch, branchRows, districtRows);
      branch.addItemListener(event -> updateDistrictFromBranch(district, branch, branchRows, districtRows));
      addDialogField(fields, 0, "Username", username);
      addDialogField(fields, 1, "Display name", display);
      addDialogField(fields, 2, "Initial password", password);
      addDialogField(fields, 3, "Role", role);
      addDialogField(fields, 4, "Branch", branch);
      addDialogField(fields, 5, "District (auto-filled)", district);
      addDialogField(fields, 6, "Account status", active);
      fields.setPreferredSize(new Dimension(620, 300));
      Panel formSurface = new Panel(new BorderLayout(8, 6));
      formSurface.setBackground(DesktopTheme.SURFACE);
      formSurface.add(fields, BorderLayout.CENTER);
      dialog.add(formSurface, BorderLayout.CENTER);
      Panel buttons = new Panel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
      buttons.setBackground(PALE);
      ActionButton save = button(existing == null ? "Create user" : "Save changes",BLUE);
      save.addActionListener(event -> {
        try {
          if(roleRows.isEmpty()) throw new IllegalArgumentException("Create a role before adding users.");
          Long roleId = selectedOptionId(roleRows, role);
          int selectedBranch = branch.getSelectedIndex();
          Map<String,Object> branchRow = selectedBranch > 0 && selectedBranch <= branchRows.size()
              ? branchRows.get(selectedBranch - 1) : null;
          Long branchId = branchRow == null ? null : number(branchRow.get("ID"));
          Long districtId = branchRow == null ? null : number(branchRow.get("DISTRICT_ID"));
          usersService.save(actor,active.getState()?"1":"0",existing==null?null:number(existing.get("ID")),districtId,branchId,roleId,
              display.getText(),existing==null?password.getText():null,username.getText());
          dialog.dispose(); message(existing==null?"User created.":"User updated."); openScreen(currentScreen);
        } catch(Exception failure) { showError(failure); }
      });
      ActionButton cancel = button("Cancel",DesktopTheme.NAVY_HOVER); cancel.addActionListener(event -> dialog.dispose());
      buttons.add(save); buttons.add(cancel); dialog.add(buttons,BorderLayout.SOUTH);
      dialog.setResizable(false);
      dialog.setSize(690, 390);
      dialog.setLocationRelativeTo(this);
      dialog.setVisible(true);
    } catch (Exception failure) { showError(failure); }
  }

  private Choice optionChoice(List<Map<String,Object>> rows, String labelKey) {
    Choice choice = new Choice();
    for (Map<String,Object> row : rows) choice.add(text(row.get(labelKey)) + "  ·  " + text(row.get("ID")));
    if (rows.isEmpty()) choice.add("No options available");
    return choice;
  }

  private void addDialogField(Panel form, int row, String title, Component input) {
    GridBagConstraints labelCell = new GridBagConstraints();
    labelCell.gridx = 0;
    labelCell.gridy = row;
    labelCell.weightx = 0;
    labelCell.fill = GridBagConstraints.HORIZONTAL;
    labelCell.anchor = GridBagConstraints.WEST;
    labelCell.insets = new Insets(5, 14, 5, 18);
    form.add(label(title, 12, true, INK), labelCell);

    GridBagConstraints inputCell = new GridBagConstraints();
    inputCell.gridx = 1;
    inputCell.gridy = row;
    inputCell.weightx = 1;
    inputCell.fill = GridBagConstraints.HORIZONTAL;
    inputCell.insets = new Insets(5, 0, 5, 14);
    input.setFont(DesktopTheme.BODY);
    if (!(input instanceof Checkbox)) {
      input.setPreferredSize(new Dimension(320, 30));
      if (input instanceof Choice) {
        input.setForeground(INK);
        input.setBackground(DesktopTheme.SURFACE);
      }
    }
    form.add(input, inputCell);
  }

  private void selectOption(Choice choice, List<Map<String,Object>> rows, Long id) {
    if (id == null) return;
    for (int index=0; index<rows.size(); index++) if (Objects.equals(number(rows.get(index).get("ID")),id)) { choice.select(index); return; }
  }

  private void selectBranch(Choice branch, List<Map<String,Object>> branches, Long branchId) {
    if (branchId == null) {
      branch.select(0);
      return;
    }
    for (int index = 0; index < branches.size(); index++) {
      if (Objects.equals(number(branches.get(index).get("ID")), branchId)) {
        branch.select(index + 1);
        return;
      }
    }
    branch.select(0);
  }

  private void updateDistrictFromBranch(TextField district, Choice branch,
      List<Map<String,Object>> branches, List<Map<String,Object>> districts) {
    int selectedIndex = branch.getSelectedIndex();
    if (selectedIndex <= 0 || selectedIndex > branches.size()) {
      district.setText("");
      return;
    }
    Long districtId = number(branches.get(selectedIndex - 1).get("DISTRICT_ID"));
    String districtName = districts.stream()
        .filter(row -> Objects.equals(number(row.get("ID")), districtId))
        .map(row -> text(row.get("NAME")))
        .findFirst()
        .orElse("");
    district.setText(districtName);
  }

  private Long selectedOptionId(List<Map<String,Object>> rows, Choice choice) {
    return rows.isEmpty() ? null : number(rows.get(Math.min(choice.getSelectedIndex(),rows.size()-1)).get("ID"));
  }

  private void populateBranches(Choice choice, List<Map<String,Object>> branches,
      List<Map<String,Object>> districts, int districtIndex, Long selectedBranchId) {
    choice.removeAll();
    if (districts.isEmpty() || districtIndex < 0 || districtIndex >= districts.size()) {
      choice.add("Select a district first"); return;
    }
    Long districtId = number(districts.get(districtIndex).get("ID"));
    List<Map<String,Object>> options = branches.stream()
        .filter(row -> Objects.equals(number(row.get("DISTRICT_ID")), districtId)).toList();
    for (Map<String,Object> row : options) choice.add(text(row.get("NAME")) + "  ·  " + text(row.get("ID")));
    if (options.isEmpty()) choice.add("No branches in this district");
    else selectOption(choice, options, selectedBranchId);
  }

  void showRolePermissions(Map<String,Object> data){
    List<Map<String,Object>> roles=listOfMaps(data.get("roles"));List<Map<String,Object>> permissions=listOfMaps(data.get("permissions"));
    if(roles.isEmpty()||permissions.isEmpty()){message("No roles or permissions are available.");return;}
    Dialog dialog=new ClosableDialog(this,"Update role access",true);
    dialog.setLayout(new BorderLayout(10,8));
    dialog.setBackground(PALE);
    Panel form=new Panel(new GridBagLayout());
    form.setBackground(PALE);
    GridBagConstraints row=new GridBagConstraints();
    row.gridx=0;row.gridy=0;row.weightx=1;row.fill=GridBagConstraints.HORIZONTAL;
    row.anchor=GridBagConstraints.NORTHWEST;row.insets=new Insets(8,10,10,10);
    Choice role=new Choice();
    role.setFont(DesktopTheme.BODY);
    role.setForeground(INK);
    role.setBackground(DesktopTheme.SURFACE);
    for(Map<String,Object> roleData:roles)role.add(text(roleData.get("NAME"))+" · "+text(roleData.get("ID")));
    Panel roleRow=new Panel(new BorderLayout(12,0));
    roleRow.setBackground(PALE);
    Label roleLabel=label("Role",12,true,INK);
    roleRow.add(roleLabel,BorderLayout.WEST);
    roleRow.add(role,BorderLayout.CENTER);
    form.add(roleRow,row);

    row.gridy=1;row.weighty=0;row.insets=new Insets(2,10,4,10);
    Label permissionsLabel=label("Permissions",12,true,INK);
    form.add(permissionsLabel,row);

    row.gridy=2;row.weighty=0;row.fill=GridBagConstraints.HORIZONTAL;
    row.insets=new Insets(0,10,8,10);
    Panel checks=DesktopTheme.card();
    checks.setLayout(new GridLayout(0,2,12,6));
    Map<String,Checkbox> boxes=new LinkedHashMap<>();
    for(Map<String,Object> permission:permissions){
      String code=text(permission.get("CODE"));
      Checkbox box=new Checkbox(text(permission.get("LABEL")),false);
      box.setFont(DesktopTheme.BODY);box.setForeground(INK);box.setBackground(DesktopTheme.SURFACE);
      boxes.put(code,box);checks.add(box);
    }
    form.add(checks,row);

    dialog.add(form,BorderLayout.CENTER);
    Runnable selectPermissions=()->{Map<String,Object> selectedRole=roles.get(role.getSelectedIndex());Map<String,Object> allMatrices=mapOf(data.get("permissionMatrix"));Map<String,Object> matrix=mapOf(allMatrices.get(text(selectedRole.get("ID"))));for(String code:boxes.keySet())boxes.get(code).setState(Boolean.TRUE.equals(matrix.get(code)));};role.addItemListener(e->selectPermissions.run());selectPermissions.run();
    Panel buttons=new Panel(new FlowLayout(FlowLayout.RIGHT,8,5));
    buttons.setBackground(PALE);
    ActionButton save=button("Save access",BLUE);save.addActionListener(e->{try{List<String> selected=new ArrayList<>();for(var entry:boxes.entrySet())if(entry.getValue().getState())selected.add(entry.getKey());rolesService.save(actor,"UPDATE",number(roles.get(role.getSelectedIndex()).get("ID")),null,selected);dialog.dispose();message("Role permissions updated.");openScreen(currentScreen);}catch(Exception ex){showError(ex);}});
    ActionButton cancel=button("Cancel",DesktopTheme.NAVY_HOVER);cancel.addActionListener(e->dialog.dispose());
    buttons.add(save);buttons.add(cancel);dialog.add(buttons,BorderLayout.SOUTH);dialog.setResizable(false);dialog.setSize(740,340);dialog.setLocationRelativeTo(this);dialog.setVisible(true);
  }

  private String normalizeMasterType(String entity) {
    return entity == null ? "district" : entity.trim().toLowerCase(Locale.ROOT);
  }

  void showMasterForm(String entity){
    final String masterEntity = normalizeMasterType(entity);
    Map<String,String> fields=new LinkedHashMap<>();if("district".equals(masterEntity)){fields.put("State ID","");fields.put("Name","");}else if("branch".equals(masterEntity)){fields.put("District ID","");fields.put("Region ID","");fields.put("Branch code","");fields.put("Name","");}else fields.put("Name","");
    formDialog("Add "+human(masterEntity),fields,form->{try{mastersService.save(actor,masterEntity,parseLong(form.get("District ID")),parseLong(form.get("Region ID")),parseLong(form.get("State ID")),form.get("Branch code"),form.get("Name"));message(human(masterEntity)+" master created.");loadMaster(masterEntity,true);}catch(Exception ex){showError(ex);}});
  }

  void loadMaster(String entity,boolean fetch){
    masterType = normalizeMasterType(entity);
    masterFetched = fetch;
    openScreen("Application Masters");
  }

  void showRiskForm(Map<String,Object> existing){
    Map<String,String> fields=new LinkedHashMap<>();fields.put("Portfolio ID (blank for all)",existing==null?"":text(existing.get("PORTFOLIO_ID")));fields.put("Risk category (LOW/MEDIUM/HIGH)",existing==null?"LOW":text(existing.get("RISK")));fields.put("Minimum days past due",existing==null?"0":text(existing.get("MIN_DPD")));Object paise=existing==null?null:existing.get("MIN_OVERDUE_PAISE");fields.put("Minimum overdue amount (INR)",paise==null?"0":new BigDecimal(paise.toString()).movePointLeft(2).toPlainString());
    formDialog(existing==null?"Add risk threshold":"Edit risk threshold",fields,form->{try{riskService.save(actor,existing==null?null:number(existing.get("ID")),form.get("Risk category (LOW/MEDIUM/HIGH)").trim().toUpperCase(Locale.ROOT),parseLong(form.get("Portfolio ID (blank for all)")),form.get("Minimum days past due"),form.get("Minimum overdue amount (INR)"));riskService.run(actor);message("Risk threshold saved and loan risk categories recalculated.");openScreen(currentScreen);}catch(Exception ex){showError(ex);}});
  }

  void formDialog(String title,Map<String,String> initial,java.util.function.Consumer<Map<String,String>> submit){
    Dialog dialog=new ClosableDialog(this,title,true);
    dialog.setLayout(new BorderLayout(12, 10));
    dialog.setBackground(PALE);
    Panel fields=DesktopTheme.card();
    fields.setLayout(new GridBagLayout());
    Map<String,TextField> inputs=new LinkedHashMap<>();
    int row = 0;
    for(var entry:initial.entrySet()){
      TextField input=field(entry.getValue());
      input.setColumns(22);
      addDialogField(fields, row++, entry.getKey(), input);
      inputs.put(entry.getKey(),input);
    }
    fields.setPreferredSize(new Dimension(600, Math.max(76, initial.size() * 42 + 16)));
    dialog.add(fields,BorderLayout.CENTER);
    Panel buttons=new Panel(new FlowLayout(FlowLayout.RIGHT,8,6));
    buttons.setBackground(PALE);
    ActionButton save=button("Save",BLUE);
    save.addActionListener(e->{Map<String,String> values=new LinkedHashMap<>();for(var entry:inputs.entrySet())values.put(entry.getKey(),entry.getValue().getText());try{submit.accept(values);dialog.dispose();}catch(Exception ex){showError(ex);}});
    ActionButton cancel=button("Cancel",DesktopTheme.NAVY_HOVER);
    cancel.addActionListener(e->dialog.dispose());
    buttons.add(save);buttons.add(cancel);dialog.add(buttons,BorderLayout.SOUTH);
    dialog.setResizable(false);
    dialog.setSize(660, Math.max(185, initial.size() * 46 + 105));
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);
  }

  Panel tableCard(String title,List<Map<String,Object>> rows){Panel card=gridPanel(title+"  ·  "+rows.size()+" records");card.add(table(rows));return card;}

  private Panel table(List<Map<String,Object>> rows) { return table(rows,List.of(),false); }
  private Panel table(List<Map<String,Object>> rows,List<String> preferred,boolean customerRows) {
    return CmsGrid.table(rows, preferred, customerRows, row -> {
      customerId=number(row.get("ID"));historyFilterId=null;showApplication();openScreen("Follow-up");
    });
  }

  Panel gridPanel(String title) {
    Panel card = DesktopTheme.card();
    card.setLayout(new BorderLayout(0, 5));
    Panel heading = new Panel(new BorderLayout(10, 0));
    heading.setBackground(DesktopTheme.SURFACE);
    Panel marker = new Panel(); marker.setBackground(BLUE); marker.setPreferredSize(new Dimension(4, 1));
    Label titleLabel = label("  " + title, 13, true, INK);
    heading.add(marker, BorderLayout.WEST); heading.add(titleLabel, BorderLayout.CENTER);
    heading.setPreferredSize(new Dimension(10, 32));
    card.add(heading, BorderLayout.NORTH);
    return card;
  }
  Panel panel(Color color){return DesktopTheme.surface(color);}
  Label label(String value,int size,boolean bold,Color color){Label l=new Label(value==null?"":value);l.setFont(DesktopTheme.font(size,bold));l.setForeground(color);return l;}
  ActionButton button(String name, Color color) {
    ActionButton button = new ActionButton(name);
    button.setBackground(color);
    button.setForeground(contrastText(color));
    button.setFont(DesktopTheme.MEDIUM);
    return button;
  }

  private Color contrastText(Color background) {
    double luminance = (0.2126 * background.getRed()
        + 0.7152 * background.getGreen()
        + 0.0722 * background.getBlue()) / 255.0;
    return luminance > 0.62 ? DesktopTheme.BLUE_DARK : Color.WHITE;
  }
  TextField field(String value){TextField input=new TextField(value==null?"":value,18);DesktopTheme.styleText(input);input.setFont(DesktopTheme.BODY);return input;}
  private GridBagConstraints constraints(){GridBagConstraints c=new GridBagConstraints();c.fill=GridBagConstraints.HORIZONTAL;c.weightx=1;return c;}
  void addField(Panel form,String title,Component input){
    form.add(label(title,11,true,MUTED));
    input.setFont(DesktopTheme.BODY);
    if (input instanceof TextField textField) { textField.setForeground(INK); textField.setBackground(DesktopTheme.SURFACE); }
    if (input instanceof Choice choice) { choice.setForeground(INK); choice.setBackground(DesktopTheme.SURFACE); }
    if (input instanceof Checkbox checkbox) { checkbox.setForeground(INK); checkbox.setBackground(DesktopTheme.SURFACE); }
    form.add(input);
  }
  private Panel error(String text){Panel p=panel(new Color(255,230,230));p.add(label(text,13,true,new Color(170,34,34)));return p;}
  private boolean has(String permission){return identity!=null&&identity.permissions().contains(permission);}

  void reloadCustomers(String q){customerQuery=q==null?"":q.trim();openScreen("Customer Worklist");}

  Choice loanChoice(List<Map<String,Object>> loans){Choice choice=new Choice();for(Map<String,Object> loan:loans)choice.add(text(loan.get("ACCOUNT_NO"))+"  ·  "+text(loan.get("PORTFOLIO_NAME")));if(loans.isEmpty())choice.add("No loan accounts");choice.setFont(DesktopTheme.BODY);choice.setForeground(INK);choice.setBackground(DesktopTheme.SURFACE);return choice;}
  Long selectedLoanId(List<Map<String,Object>> loans,Choice choice){return loans.isEmpty()?null:number(loans.get(Math.min(choice.getSelectedIndex(),loans.size()-1)).get("ID"));}
  void filterByLoan(Long accountId,String screen){historyFilterId=accountId;openScreen(screen);}

  void saveFollowup(Choice choice,List<Map<String,Object>> loans,TextField contactDate,TextField channel,TextField outcome,TextField amount,TextField promiseDate,TextField nextAction,TextField remarks){try{followupService.save(actor,blank(contactDate.getText()),blank(promiseDate.getText()),blank(amount.getText()),customerId,selectedLoanId(loans,choice),channel.getText(),outcome.getText(),remarks.getText(),blank(nextAction.getText()));message("Follow-up saved.");openScreen("Follow-up");}catch(Exception e){showError(e);}}
  void savePayment(List<Map<String,Object>> loans,Choice choice,TextField amount,TextField mode,TextField reference){try{paymentService.save(actor,customerId,selectedLoanId(loans,choice),mode.getText(),reference.getText(),new BigDecimal(amount.getText()));message("Payment posted.");openScreen("Payment Posting");}catch(Exception e){showError(e);}}
  void saveCommunication(List<Map<String,Object>> loans,Choice choice,Choice channel,TextField contact,TextField notes){try{communicationsService.save(actor,customerId,selectedLoanId(loans,choice),channel.getSelectedItem(),notes.getText(),contact.getText());message("Contact log saved.");openScreen("Communications");}catch(Exception e){showError(e);}}
  private String blank(String value){return value==null||value.isBlank()?null:value.trim();}
  private Long parseLong(String value){return value==null||value.isBlank()?null:Long.valueOf(value.trim());}
  private Long number(Object value){return value instanceof Number n?n.longValue():null;}
  String text(Object value){return value==null?"":String.valueOf(value);}
  private String shortMessage(Throwable e){Throwable t=e;while(t.getCause()!=null)t=t.getCause();return t.getMessage()==null?t.getClass().getSimpleName():t.getMessage();}
  Map<String,Object> mapOf(Object o){if(!(o instanceof Map<?,?> m))return Map.of();Map<String,Object> out=new LinkedHashMap<>();for(var e:m.entrySet())out.put(String.valueOf(e.getKey()),e.getValue());return out;}
  List<Map<String,Object>> listOfMaps(Object value){if(!(value instanceof List<?> list))return List.of();List<Map<String,Object>> out=new ArrayList<>();for(Object item:list)if(item instanceof Map<?,?>)out.add(mapOf(item));return out;}
  Map<String,String> mapOfStrings(Object value){Map<String,String> out=new LinkedHashMap<>();for(var e:mapOf(value).entrySet())out.put(e.getKey(),text(e.getValue()));return out;}
  private String human(String s){String value=s.replace('_',' ').toLowerCase(Locale.ROOT);return value.isEmpty()?value:Character.toUpperCase(value.charAt(0))+value.substring(1);}
  private String choiceKey(Choice choice){String selected=choice.getSelectedItem();return selected==null?"":selected.split(" · ",2)[0];}
  void showError(Exception e){LOG.error("Desktop operation failed userId={} screen={}",identity==null?null:identity.id(),currentScreen,e);new MessageDialog(this,"CMS",shortMessage(e),true).setVisible(true);}
  void message(String value){new MessageDialog(this,"CMS",value,false).setVisible(true);}

  private static class ClosableDialog extends Dialog {
    ClosableDialog(Frame owner, String title, boolean modal) {
      super(owner, title, modal);
      addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent event) {
          dispose();
        }
      });
    }
  }

  private static final class MessageDialog extends ClosableDialog {
    MessageDialog(Frame owner,String title,String message,boolean error){super(owner,title,true);setLayout(new BorderLayout(12,12));setBackground(error?new Color(255,241,241):Color.WHITE);Panel lines = new Panel(new GridLayout(0, 1)); lines.setBackground(getBackground()); for (String line : message.split("\\R")) lines.add(new Label("  " + line)); add(lines,BorderLayout.CENTER);ActionButton close=new ActionButton("OK");close.addActionListener(e->dispose());Panel p=new Panel();p.add(close);add(p,BorderLayout.SOUTH);pack();setSize(Math.max(440,getWidth()+20),Math.max(145,getHeight()+20));setLocationRelativeTo(owner);}
  }
}



