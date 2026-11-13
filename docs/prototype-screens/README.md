# CMS AWT desktop prototype set

These files are AWT/Java2D concept renders for design review. They are not screenshots of a running application. They replace the browser frame with a native desktop-style window and illustrate the main workflows and role navigation before full conversion work begins.

## Screens

- `login.png` — company sign-in.
- `worklist.png` — collector worklist with filters, risk indicators, customer grid, and paging.
- `follow-up.png` — customer summary, follow-up entry, and recent follow-up history.
- `payment.png` — payment posting and posting history.
- `administration.png` — user management and admin navigation for roles, masters, risk rules, allocation, audit logs, and password management.
- `reports.png` — portfolio performance with risk distribution, product balances, and branch metrics.
- `prototype-contact-sheet.png` — overview of the six concepts.

All displayed customer and transaction details are fictional sample data. The prototype does not connect to the CMS database or implement interactions.

Regenerate the PNGs from the project directory with:

```powershell
javac -encoding UTF-8 -d docs\prototype-screens docs\prototype-screens\CmsDesktopPrototype.java
java -cp docs\prototype-screens CmsDesktopPrototype
```
