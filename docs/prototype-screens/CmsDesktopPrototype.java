import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Standalone AWT visual prototype generator. Captures realistic desktop-window mock screens. */
public final class CmsDesktopPrototype {
  private static final Color NAVY = new Color(16, 43, 78);
  private static final Color NAVY2 = new Color(24, 58, 100);
  private static final Color BLUE = new Color(24, 105, 221);
  private static final Color BLUE2 = new Color(7, 82, 186);
  private static final Color BG = new Color(242, 246, 252);
  private static final Color BORDER = new Color(215, 226, 240);
  private static final Color MUTED = new Color(105, 123, 146);
  private static final Color INK = new Color(26, 43, 65);
  private static final Font REG = new Font("Segoe UI", Font.PLAIN, 13);
  private static final Font MED = new Font("Segoe UI", Font.BOLD, 13);
  private static final Font TITLE = new Font("Segoe UI", Font.BOLD, 21);
  private static final Font SMALL = new Font("Segoe UI", Font.PLAIN, 11);
  private static final String[] SCREENS = {"Login", "Worklist", "Follow-up", "Payment", "Administration", "Reports"};
  private static final String OUT = "docs/prototype-screens/";

  public static void main(String[] args) throws Exception {
    System.setProperty("java.awt.headless", "false");
    for (String screen : SCREENS) capture(screen);
    makeContactSheet();
    System.exit(0);
  }

  private static void capture(String screen) throws Exception {
    int width=1360, titleHeight=38, height=titleHeight+690;
    BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
    Graphics2D g=image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    g.setColor(new Color(247,249,252));g.fillRect(0,0,width,titleHeight);
    g.setColor(new Color(223,228,235));g.drawLine(0,titleHeight-1,width,titleHeight-1);
    g.setColor(BLUE);g.fillRoundRect(12,10,18,18,4,4);
    g.setColor(Color.WHITE);g.setFont(new Font("Segoe UI",Font.BOLD,12));g.drawString("₹",16,24);
    g.setColor(new Color(49,58,70));g.setFont(new Font("Segoe UI",Font.PLAIN,12));g.drawString("CMS — Collections Management System",38,24);
    g.setColor(new Color(88,98,110));g.drawString("—",width-68,23);g.drawRect(width-43,11,12,12);g.drawString("×",width-19,24);
    PrototypeCanvas canvas=new PrototypeCanvas(screen);canvas.setSize(width,690);
    Graphics2D content=(Graphics2D)g.create();content.translate(0,titleHeight);canvas.paint(content);content.dispose();g.dispose();
    ImageIO.write(image,"png",new File(OUT+fileName(screen)+".png"));
  }

  private static String fileName(String name) { return name.toLowerCase().replace(' ', '-'); }
  private static void makeContactSheet() throws Exception {
    int w = 1320, h = 1235, gap = 20, tileW = 630, tileH = 365;
    BufferedImage sheet = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = sheet.createGraphics();
    g.setColor(new Color(232, 238, 247)); g.fillRect(0,0,w,h);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setFont(new Font("Segoe UI",Font.BOLD,23)); g.setColor(NAVY); g.drawString("CMS Desktop Prototype",24,36);
    g.setFont(new Font("Segoe UI",Font.PLAIN,12)); g.setColor(MUTED); g.drawString("Native AWT desktop concepts · fictional sample data · for design review",24,56);
    for (int i=0;i<SCREENS.length;i++) {
      int x=24+(i%2)*(tileW+gap), y=76+(i/2)*(tileH+gap);
      BufferedImage image=ImageIO.read(new File(OUT+fileName(SCREENS[i])+".png"));
      int titleBar=28;
      g.setColor(Color.WHITE); g.fillRoundRect(x,y,tileW,tileH,12,12);
      g.setColor(BORDER); g.drawRoundRect(x,y,tileW,tileH,12,12);
      g.setFont(new Font("Segoe UI",Font.BOLD,12)); g.setColor(INK); g.drawString(SCREENS[i],x+12,y+18);
      g.drawImage(image,x+5,y+titleBar,tileW-10,tileH-titleBar-5,null);
    }
    g.dispose(); ImageIO.write(sheet,"png",new File(OUT+"prototype-contact-sheet.png"));
  }

  private static final class PrototypeCanvas extends Canvas {
    private final String screen;
    PrototypeCanvas(String screen) { this.screen=screen; setBackground(BG); }
    public void paint(Graphics raw) {
      Graphics2D g=(Graphics2D)raw.create();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      double sx=getWidth()/1360.0, sy=getHeight()/690.0; g.scale(sx,sy);
      if(screen.equals("Login")) login(g); else appShell(g,screen);
      g.dispose();
    }
    private void login(Graphics2D g) {
      g.setPaint(new GradientPaint(0,0,new Color(9,55,108),1360,690,new Color(24,119,220)));g.fillRect(0,0,1360,690);
      g.setColor(new Color(255,255,255,25));g.fillOval(-190,420,480,480);g.fillOval(1120,-190,420,420);
      card(g,420,94,520,500,Color.WHITE,18);
      round(g,628,130,104,104,new Color(255,39,53),52);text(g,"₹",Font.BOLD,58,Color.WHITE,658,202);
      text(g,"Bharat Small Finance Company",Font.BOLD,23,NAVY,490,272);
      text(g,"Collections Management System",Font.PLAIN,15,MUTED,526,301);
      line(g,480,325,880,325,BORDER);
      text(g,"Welcome back",Font.BOLD,21,INK,480,367);
      text(g,"Sign in with your company account to continue",Font.PLAIN,13,MUTED,480,391);
      field(g,"Username","Collector ID or username",480,416,400);
      field(g,"Password","Enter password",480,486,400);
      button(g,"Sign in",480,548,400,44,BLUE);
      text(g,"Authorized company users only",Font.PLAIN,11,MUTED,608,616);
    }
    private void appShell(Graphics2D g,String page) {
      g.setColor(BG);g.fillRect(0,0,1360,690);
      g.setColor(NAVY);g.fillRect(0,0,226,690);
      round(g,19,18,42,42,new Color(255,41,54),12);text(g,"₹",Font.BOLD,25,Color.WHITE,29,48);
      text(g,"CMS",Font.BOLD,19,Color.WHITE,73,38);text(g,"COLLECTIONS MANAGEMENT",Font.BOLD,9,new Color(187,207,230),73,53);
      line(g,0,76,226,76,new Color(48,78,112));
      String[] items=page.equals("Administration")?new String[]{"Administration","User Management","Roles & Permissions","Application Masters","Risk Rules","Customer Allocation","Audit Logs","Change Password"}:page.equals("Reports")?new String[]{"Owner Workspace","Performance Report","Collector Performance","Collector Worklist","Change Password"}:new String[]{"Collector Workspace","Dashboard","Customer Worklist","Collector Reports","Change Password"};
      int y=104;for(int i=0;i<items.length;i++){boolean active=(page.equals("Administration")&&i==1)||(page.equals("Reports")&&i==1)||(page.equals("Worklist")&&i==2)||((page.equals("Follow-up")||page.equals("Payment"))&&i==2);if(active){round(g,12,y-22,202,40,new Color(35,101,183),10);}text(g,active?"●":"○",Font.BOLD,11,active?Color.WHITE:new Color(141,171,205),24,y);text(g,items[i],Font.PLAIN,13,active?Color.WHITE:new Color(216,228,243),46,y);y+=48;}
      line(g,0,637,226,637,new Color(48,78,112));text(g,"Bharat Small Finance Company",Font.PLAIN,10,new Color(185,205,228),18,661);
      g.setColor(Color.WHITE);g.fillRect(226,0,1134,65);line(g,226,64,1360,64,BORDER);
      text(g,"CMS",Font.BOLD,16,NAVY,250,28);text(g,"›",Font.PLAIN,18,MUTED,299,29);text(g,pageTitle(page),Font.BOLD,14,INK,318,28);
      text(g,"●  Connected",Font.PLAIN,11,new Color(12,139,92),1017,28);
      round(g,1097,13,164,38,new Color(239,245,253),19);round(g,1106,20,25,25,BLUE,13);text(g,"SR",Font.BOLD,10,Color.WHITE,1111,37);text(g,"Suresh Reddy",Font.BOLD,11,INK,1139,29);text(g,"COLLECTOR  ▾",Font.PLAIN,9,MUTED,1139,42);
      round(g,1275,16,62,32,Color.WHITE,8);g.setColor(BORDER);g.drawRoundRect(1275,16,62,32,8,8);text(g,"Sign out",Font.BOLD,10,NAVY,1283,36);
      text(g,pageTitle(page),Font.BOLD,23,INK,250,103);text(g,pageSubtitle(page),Font.PLAIN,12,MUTED,250,124);
      if(page.equals("Worklist")) worklist(g); else if(page.equals("Follow-up")) followup(g); else if(page.equals("Payment")) payment(g); else if(page.equals("Administration")) administration(g); else if(page.equals("Reports")) reports(g);
    }
    private String pageTitle(String p){return switch(p){case "Worklist"->"Customer Worklist";case "Follow-up"->"Customer Follow-up";case "Payment"->"Payment Posting";case "Administration"->"User Management";case "Reports"->"Performance Report";default->"Sign in";};}
    private String pageSubtitle(String p){return switch(p){case "Worklist"->"Review assigned customers and open a case to take action.";case "Follow-up"->"Record a customer contact, promise, and next action.";case "Payment"->"Post confirmed payments and review payment posting history.";case "Administration"->"Manage user accounts, roles, and branch assignments.";case "Reports"->"Portfolio performance across districts, branches, and risk levels.";default->"";};}

    private void worklist(Graphics2D g) {
      stat(g,"Assigned customers","167","across your branches",250,146,254,75,BLUE);
      stat(g,"High risk","28","needs attention",518,146,254,75,new Color(239,79,91));
      stat(g,"Overdue balance","₹ 42.8 L","assigned portfolio",786,146,254,75,new Color(225,123,14));
      stat(g,"Due today","14","next actions",1054,146,282,75,new Color(13,151,111));
      card(g,250,236,1086,91,Color.WHITE,12);text(g,"FILTER WORKLIST",Font.BOLD,10,MUTED,270,258);
      smallField(g,"Customer / phone",270,269,270);smallField(g,"Risk",554,269,174);smallField(g,"Portfolio",742,269,190);smallField(g,"Next action",946,269,184);button(g,"Search",1143,278,82,34,BLUE);outlineButton(g,"Reset",1233,278,82,34);
      card(g,250,342,1086,324,Color.WHITE,12);text(g,"Assigned customers",Font.BOLD,15,INK,270,370);text(g,"167 customers",Font.PLAIN,11,MUTED,1233,369);
      tableHeader(g,270,388,new String[]{"Customer","Customer name","Mobile","Loans","Outstanding","Overdue","DPD","Risk","Next action"},new int[]{220,190,130,70,130,120,65,90,100});
      String[][] rows={{"CMS22-MVP-100-00001","Anusha Reddy","98••••••21","2","₹ 2,18,500","₹ 18,500","42","MEDIUM","Today"},{"CMS22-MVP-100-00002","Kiran Naidu","98••••••54","1","₹ 84,200","₹ 12,100","28","LOW","Today"},{"CMS22-MVP-100-00003","Sravani Varma","99••••••17","2","₹ 3,54,800","₹ 64,200","67","HIGH","Overdue"},{"CMS22-MVP-100-00004","Ramesh Kumar","97••••••63","1","₹ 1,42,000","₹ 9,700","19","LOW","Tomorrow"},{"CMS22-MVP-100-00005","Meena Devi","96••••••09","3","₹ 4,20,600","₹ 1,02,300","91","HIGH","Today"}};
      tableRows(g,270,416,rows,new int[]{220,190,130,70,130,120,65,90,100},44);
      text(g,"Showing 1–5 of 167 customers",Font.PLAIN,11,MUTED,270,652);outlineButton(g,"‹ Previous",1164,634,75,29);button(g,"Next ›",1247,634,70,29,BLUE);
    }

    private void followup(Graphics2D g) {
      customerSummary(g,250,145,1086,118);
      tabs(g,250,272,"Follow-up");
      card(g,250,324,663,337,Color.WHITE,12);text(g,"Record follow-up",Font.BOLD,15,INK,270,352);text(g,"Capture the result of a call or visit and schedule the next step.",Font.PLAIN,11,MUTED,270,371);
      smallField(g,"Loan account",270,393,197);smallField(g,"Contact mode",479,393,197);smallField(g,"Contact date",688,393,204);
      smallField(g,"Outcome",270,454,197);smallField(g,"Promise amount (₹)",479,454,197);smallField(g,"Promise date",688,454,204);
      smallField(g,"Next action date",270,515,197);text(g,"Remarks",Font.BOLD,10,MUTED,479,535);round(g,479,543,413,48,new Color(250,252,255),8);g.setColor(BORDER);g.drawRoundRect(479,543,413,48,8,8);text(g,"Add a short note about the conversation…",Font.PLAIN,11,MUTED,492,564);
      button(g,"Save follow-up",708,611,184,34,BLUE);
      card(g,929,324,407,337,Color.WHITE,12);text(g,"Follow-up history",Font.BOLD,15,INK,949,352);text(g,"Last 5 activities",Font.PLAIN,11,MUTED,1237,352);
      tableHeader(g,949,371,new String[]{"Date","Account","Mode","Outcome","Next action"},new int[]{90,105,66,72,96});
      String[][] rows={{"Nov 10","…00001-1","Call","Promise to pay","Nov 14"},{"Nov 08","…00001-2","Visit","Reached","Nov 12"},{"Nov 06","…00001-1","SMS","No answer","Nov 10"},{"Nov 03","…00001-1","Call","Promise to pay","Nov 08"},{"Oct 29","…00001-2","Call","Reached","Nov 03"}};
      tableRows(g,949,399,rows,new int[]{90,105,66,72,96},42);text(g,"Page 1 of 4",Font.PLAIN,10,MUTED,949,635);outlineButton(g,"‹",1265,617,28,27);outlineButton(g,"›",1300,617,28,27);
    }

    private void payment(Graphics2D g) {
      customerSummary(g,250,145,1086,118);tabs(g,250,272,"Payment Posting");
      card(g,250,324,527,337,Color.WHITE,12);text(g,"Post a payment",Font.BOLD,15,INK,270,352);text(g,"Enter a confirmed transaction to update account balances.",Font.PLAIN,11,MUTED,270,371);
      smallField(g,"Loan account",270,398,231);smallField(g,"Amount (₹)",514,398,241);smallField(g,"Payment mode",270,464,231);smallField(g,"Transaction reference",514,464,241);
      round(g,270,526,485,39,new Color(239,248,255),8);text(g,"✓  Payment is applied to overdue first, then outstanding.",Font.PLAIN,11,new Color(40,92,145),284,550);
      button(g,"Post confirmed payment",270,584,485,42,BLUE);
      card(g,793,324,543,337,Color.WHITE,12);text(g,"Payment posting history",Font.BOLD,15,INK,813,352);text(g,"3 records",Font.PLAIN,11,MUTED,1281,352);
      tableHeader(g,813,374,new String[]{"Posted","Account","Reference","Mode","Amount","Status"},new int[]{90,105,122,62,75,55});
      String[][] rows={{"Nov 10","…00001-1","TXN-812304","UPI","₹ 5,000","SUCCESS"},{"Nov 08","…00001-2","TXN-812172","Cash","₹ 2,500","SUCCESS"},{"Nov 05","…00001-1","TXN-811894","UPI","₹ 1,000","SUCCESS"}};
      tableRows(g,813,402,rows,new int[]{90,105,122,62,75,55},48);text(g,"History is limited to the selected customer and account.",Font.PLAIN,10,MUTED,813,584);
    }

    private void administration(Graphics2D g) {
      card(g,250,145,1086,109,Color.WHITE,12);text(g,"Create user",Font.BOLD,15,INK,270,173);
      smallField(g,"Username",270,188,210);smallField(g,"Display name",492,188,220);smallField(g,"Role",724,188,170);smallField(g,"District",906,188,170);smallField(g,"Branch",1088,188,150);button(g,"Create user",1245,198,73,32,BLUE);
      card(g,250,270,1086,395,Color.WHITE,12);text(g,"Application users",Font.BOLD,15,INK,270,298);text(g,"33 active users",Font.PLAIN,11,MUTED,1240,298);
      text(g,"Search users",Font.BOLD,10,MUTED,270,326);round(g,350,309,260,30,new Color(250,252,255),7);g.setColor(BORDER);g.drawRoundRect(350,309,260,30,7,7);text(g,"Name, username, or branch",Font.PLAIN,11,MUTED,362,328);button(g,"Search",620,309,70,30,BLUE);outlineButton(g,"Export",1243,309,75,30);
      tableHeader(g,270,356,new String[]{"Username","Display name","Role","District","Branch","Status","Actions"},new int[]{175,210,145,170,190,100,230});
      String[][] rows={{"OWNER","Company Owner","OWNER","All","All","Active","View"},{"ADMIN","System Admin","ADMIN","All","All","Active","Edit   Disable"},{"MVP_100_C01","Suresh Reddy","COLLECTOR","Visakhapatnam","MVP Colony","Active","Edit   Disable"},{"GJW_101_C02","Ramesh Naidu","COLLECTOR","Visakhapatnam","Dwaraka Nagar","Active","Edit   Disable"},{"GNT_103_C03","Lakshmi Devi","COLLECTOR","Guntur","Brodipet","Active","Edit   Disable"},{"VJA_102_C04","Venkatesh Rao","COLLECTOR","NTR","Benz Circle","Active","Edit   Disable"}};
      tableRows(g,270,384,rows,new int[]{175,210,145,170,190,100,230},43);
      text(g,"Role changes, user creation, and access updates are recorded in Audit Logs.",Font.PLAIN,11,MUTED,270,655);
    }

    private void reports(Graphics2D g) {
      card(g,250,145,1086,67,Color.WHITE,11);smallField(g,"District",268,157,180);smallField(g,"Branch",458,157,180);smallField(g,"Period",648,157,180);smallField(g,"Risk",838,157,180);button(g,"Run report",1030,165,110,32,BLUE);outlineButton(g,"Export CSV",1150,165,166,32);
      stat(g,"Customers","1,002","in selected scope",250,226,254,74,BLUE);stat(g,"Loan accounts","1,140","active accounts",518,226,254,74,new Color(122,78,200));stat(g,"Outstanding","₹ 3.12 Cr","including overdue",786,226,254,74,new Color(226,123,14));stat(g,"Collections","₹ 5.35 L","this period",1054,226,282,74,new Color(13,151,111));
      card(g,250,316,525,194,Color.WHITE,12);text(g,"Risk distribution",Font.BOLD,14,INK,270,342);pie(g,360,416);text(g,"LOW        422 customers",Font.PLAIN,12,INK,450,392);text(g,"MEDIUM  341 customers",Font.PLAIN,12,INK,450,419);text(g,"HIGH      239 customers",Font.PLAIN,12,INK,450,446);
      card(g,793,316,543,194,Color.WHITE,12);text(g,"Portfolio outstanding",Font.BOLD,14,INK,813,342);bar(g,"Gold loan",813,373,455,0.86,"₹ 1.82 Cr");bar(g,"Microfinance",813,410,455,0.65,"₹ 0.91 Cr");bar(g,"Business loan",813,447,455,0.43,"₹ 0.39 Cr");
      card(g,250,526,1086,139,Color.WHITE,12);text(g,"Branch performance",Font.BOLD,14,INK,270,552);tableHeader(g,270,564,new String[]{"District","Branch","Customers","Accounts","Outstanding","Overdue","Collections","Efficiency"},new int[]{190,180,105,110,155,135,145,145});
      String[][] rows={{"Visakhapatnam","MVP Colony","167","190","₹ 51.72 L","₹ 7.69 L","₹ 97,143","71.4%"},{"Guntur","Brodipet","151","172","₹ 43.28 L","₹ 6.14 L","₹ 81,388","68.2%"}};tableRows(g,270,592,rows,new int[]{190,180,105,110,155,135,145,145},34);
    }

    private void customerSummary(Graphics2D g,int x,int y,int w,int h){card(g,x,y,w,h,Color.WHITE,12);text(g,"CUSTOMER",Font.BOLD,9,MUTED,x+18,y+22);text(g,"Anusha Reddy",Font.BOLD,16,INK,x+18,y+46);text(g,"CMS22-MVP-100-00001  ·  98••••••21  ·  Visakhapatnam, Andhra Pradesh",Font.PLAIN,11,MUTED,x+18,y+67);
      int sx=x+w-438;stat(g,"Loans","2","",sx,y+18,96,78,BLUE);stat(g,"Outstanding","₹ 2.18 L","",sx+103,y+18,107,78,new Color(13,151,111));stat(g,"Overdue","₹ 18,500","",sx+217,y+18,103,78,new Color(232,62,79));stat(g,"Risk","MEDIUM","",sx+327,y+18,96,78,new Color(230,126,14));}
    private void tabs(Graphics2D g,int x,int y,String active){String[] vals={"Follow-up","Payment Posting","Follow-up History","Payment History","Communications"};int dx=x;for(String v:vals){int width=v.length()*7+28;round(g,dx,y,width,31,v.equals(active)?BLUE:Color.WHITE,8);if(!v.equals(active)){g.setColor(BORDER);g.drawRoundRect(dx,y,width,31,8,8);}text(g,v,Font.BOLD,10,v.equals(active)?Color.WHITE:BLUE,dx+13,y+20);dx+=width+7;}}
    private void stat(Graphics2D g,String title,String value,String note,int x,int y,int w,int h,Color accent){card(g,x,y,w,h,Color.WHITE,11);round(g,x,y,5,h,accent,3);text(g,title,Font.PLAIN,10,MUTED,x+15,y+19);text(g,value,Font.BOLD,20,INK,x+15,y+45);if(!note.isEmpty())text(g,note,Font.PLAIN,10,MUTED,x+15,y+62);}
    private void smallField(Graphics2D g,String label,int x,int y,int w){text(g,label,Font.BOLD,10,MUTED,x,y+10);round(g,x,y+15,w,30,new Color(250,252,255),7);g.setColor(BORDER);g.drawRoundRect(x,y+15,w,30,7,7);text(g,"Choose or enter…",Font.PLAIN,11,new Color(141,155,173),x+10,y+34);}
    private void field(Graphics2D g,String label,String placeholder,int x,int y,int w){text(g,label,Font.BOLD,11,INK,x,y);round(g,x,y+10,w,38,new Color(250,252,255),8);g.setColor(BORDER);g.drawRoundRect(x,y+10,w,38,8,8);text(g,placeholder,Font.PLAIN,12,new Color(139,154,172),x+12,y+34);}
    private void button(Graphics2D g,String label,int x,int y,int w,int h,Color c){round(g,x,y,w,h,c,8);text(g,label,Font.BOLD,11,Color.WHITE,x+Math.max(8,(w-textWidth(g,label,MED))/2),y+h/2+4);}
    private void outlineButton(Graphics2D g,String label,int x,int y,int w,int h){round(g,x,y,w,h,Color.WHITE,8);g.setColor(BORDER);g.drawRoundRect(x,y,w,h,8,8);text(g,label,Font.BOLD,10,BLUE,x+Math.max(7,(w-textWidth(g,label,MED))/2),y+h/2+4);}
    private void card(Graphics2D g,int x,int y,int w,int h,Color color,int radius){g.setColor(new Color(20,54,94,12));g.fillRoundRect(x,y+2,w,h,radius,radius);g.setColor(color);g.fillRoundRect(x,y,w,h,radius,radius);g.setColor(BORDER);g.drawRoundRect(x,y,w,h,radius,radius);}
    private void round(Graphics2D g,int x,int y,int w,int h,Color c,int r){g.setColor(c);g.fill(new RoundRectangle2D.Float(x,y,w,h,r,r));}
    private void line(Graphics2D g,int x1,int y1,int x2,int y2,Color c){g.setColor(c);g.drawLine(x1,y1,x2,y2);}
    private void text(Graphics2D g,String s,int style,int size,Color c,int x,int y){g.setFont(new Font("Segoe UI",style,size));g.setColor(c);g.drawString(s,x,y);}
    private int textWidth(Graphics2D g,String s,Font f){g.setFont(f);return g.getFontMetrics().stringWidth(s);}
    private void tableHeader(Graphics2D g,int x,int y,String[] cols,int[] widths){int total=0;for(int w:widths)total+=w;round(g,x,y,total,27,new Color(231,240,251),7);int cx=x;for(int i=0;i<cols.length;i++){text(g,cols[i],Font.BOLD,10,new Color(53,77,106),cx+8,y+18);cx+=widths[i];}}
    private void tableRows(Graphics2D g,int x,int y,String[][] rows,int[] widths,int rh){for(int r=0;r<rows.length;r++){g.setColor(r%2==0?Color.WHITE:new Color(248,251,255));g.fillRect(x,y+r*rh,ArraysSum(widths),rh);line(g,x,y+(r+1)*rh,x+ArraysSum(widths),y+(r+1)*rh,BORDER);int cx=x;for(int c=0;c<rows[r].length;c++){String v=rows[r][c];Color color=INK;if(c==7&&v.equals("HIGH"))color=new Color(202,55,65);else if(c==7&&v.equals("LOW"))color=new Color(8,135,92);text(g,v,Font.PLAIN,10,color,cx+8,y+r*rh+18);cx+=widths[c];}}}
    private int ArraysSum(int[] values){int n=0;for(int v:values)n+=v;return n;}
    private void pie(Graphics2D g,int cx,int cy){g.setColor(new Color(30,142,96));g.fillArc(cx-40,cy-40,80,80,90,150);g.setColor(new Color(236,156,18));g.fillArc(cx-40,cy-40,80,80,240,60);g.setColor(new Color(224,62,77));g.fillArc(cx-40,cy-40,80,80,300,150);}
    private void bar(Graphics2D g,String label,int x,int y,int width,double ratio,String value){text(g,label,Font.PLAIN,11,INK,x,y+13);round(g,x+116,y,width-225,10,new Color(232,239,249),5);round(g,x+116,y,(int)((width-225)*ratio),10,new Color(130,91,219),5);text(g,value,Font.BOLD,10,INK,x+width-95,y+10);}
  }
}
