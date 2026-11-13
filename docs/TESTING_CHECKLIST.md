# Desktop manual verification checklist

This list is provided for the recipient to verify against their Oracle development schema. No end-to-end Oracle workflow was exercised while packaging this project.

- [ ] Java 17 starts the desktop sign-in window.
- [ ] An active database user can sign in and receives the menu allowed by their permissions.
- [ ] Customer Worklist shows customers assigned to the signed-in collector.
- [ ] Follow-up save appears in follow-up history.
- [ ] Confirmed payment posts and appears in payment history.
- [ ] Communication entry is saved and visible in communications history.
- [ ] Admin-only screens reject access when the user lacks the required permission.
- [ ] Appearance settings persist and Log4j 2 writes to the configured log file.

If a database operation fails, check `Application.config` and `logs/cms-application.log`.
