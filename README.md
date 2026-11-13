# CMS AWT Desktop — Java 17

This is the Java 17 desktop edition of the CMS Collections Management System. The web presentation (Struts actions, JSP pages, servlet deployment, and JBoss-specific runtime) has been replaced with a role-aware AWT application shell, task-focused screens, compact customer overview cards, aligned forms, and dedicated data grids. The existing application model, service workflows, JDBC repositories, Oracle schema, and Log4j 2 application logging remain in use.

## What is included

- Java AWT desktop sign-in and role-based application shell.
- Dashboard and Customer Worklist with customer selection.
- Customer follow-up, payment posting, follow-up history, payment history, and communications screens.
- User, role and permission, master data, risk, allocation, audit, owner report, collector performance, and collector report screens backed by the existing service layer.
- District-filtered branch selection when creating or editing users, searchable master types, and branch allocation by scope.
- Consistent screen headings, selected navigation states, compact customer summaries, and non-generic screen layouts.
- Shared `Application.config` configuration and file-based Log4j 2 logging.
- The single Oracle setup script: `database/CMS_Complete_Database_Setup.sql`.

## Requirements

- 64-bit JDK 17 or later (verify with `java -version`).
- Oracle 19c or a compatible Oracle database with the CMS schema created.
- Windows for the included `run-cms.bat`; on other operating systems use the Java command below.

## Configure the database

1. Edit the root `Application.config` file and set `database.jdbc.url`, `database.jdbc.username`, and `database.jdbc.password` for your database. The AWT app uses direct JDBC; it does not require JBoss or an application-server datasource.
2. In Oracle SQL Developer, connect as the dedicated CMS schema owner, open `database/CMS_Complete_Database_Setup.sql`, and run it with **F5**. The setup script drops and recreates CMS objects and demo data, so use a development schema.
3. The database user needs permission to create the objects in the setup script. Consult your Oracle administrator if the script reports privilege errors.

## Run the desktop application

Double-click `run-cms.bat`, or run from PowerShell in this project folder:

```powershell
java -Dcms.config.path="Application.config" -cp "cms-awt.jar;lib/*" com.cms.desktop.CmsDesktopApplication
```

The launcher expects `cms-awt.jar` and the dependency jars under `lib/`, both included in this delivery. Logs are written under `logs/cms-application.log` (with rollover archives under `logs/archive`).

## Build from source

The Maven project targets Java 17. With Maven installed and dependency access available:

```powershell
mvn clean package -DskipTests
```

This creates `target/cms-awt.jar`. For a local build in an environment that already has the dependencies cached, use the standard Maven offline option `mvn -o clean package -DskipTests`. The prebuilt jar and runtime libraries are included so the application can be launched without Maven.

If Maven plugins are not available locally, compile directly with the included dependencies from PowerShell:

```powershell
$sourceFiles = Get-ChildItem src/main/java -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 --release 17 -cp "lib/*" -d "target/classes" $sourceFiles
Copy-Item src/main/resources/* target/classes/ -Force
jar --create --file cms-awt.jar --main-class com.cms.desktop.CmsDesktopApplication -C target/classes .
```

## Project structure

| Area | Location | Purpose |
|---|---|---|
| AWT screens | `src/main/java/com/cms/desktop` | Desktop window, navigation, forms, and screen rendering |
| Configuration | `src/main/java/com/cms/config` and `Application.config` | Load application and database settings before opening screens |
| Models | `src/main/java/com/cms/model` | Application identity and data objects |
| Services | `src/main/java/com/cms/service` | Permission checks and business workflows |
| Repositories | `src/main/java/com/cms/repository` | Oracle JDBC reads and updates |
| Database setup | `database/CMS_Complete_Database_Setup.sql` | Single schema reset and demo-data script |

## Notes

- The complete desktop source is also copied to `C:\Users\HomePC\Documents\ChatGPT\CMS-AWT` as requested. The separate web project remains unchanged.
- The UI now runs as a desktop process; there is no `http://localhost` URL and no JBoss deployment step.
- Oracle connectivity and login depend on your local Oracle service and schema configuration. They could not be exercised during packaging here.
- Never distribute a development `Application.config` with production credentials. Set production secrets on the destination machine and protect file access.
