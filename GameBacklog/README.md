# My Game Backlog (JavaFX + SQLite)

## Requirements
- JDK 17 or newer
- Maven (bundled with IntelliJ IDEA, NetBeans and Eclipse)

## Run in an IDE
1. **IntelliJ IDEA:** File > Open > select the `pom.xml` > "Open as Project". Wait for Maven to download dependencies,
   then open the Maven tool window > Plugins > javafx > `javafx:run`.
   **NetBeans / Eclipse:** import as a Maven project, then run the `javafx:run` goal.
2. Or from a terminal in this folder: `mvn javafx:run`

(Running `Main` directly with the green arrow can fail with "JavaFX runtime components are missing"; use `javafx:run`.)

## Notes
- Data is stored in `games.db` (SQLite), created next to where you launch the app. It is seeded with 8 sample games on first run only.
- Cover images are optional; games without one get a generated colour placeholder.
- Settings page lets you reload the sample games or wipe the library.

## Structure (src/main/java/gamebacklog)
Main, Game, DatabaseManager, GameService, MainController, DashboardView, GameLibraryView, GameFormView,
GameDetailsView, UI (shared helpers). Styling: src/main/resources/styles.css

## Play Game and automatic playtime
- Edit a game, choose its `.exe`, then click **Play Game** on its card or details page. Tracking starts automatically.
- Games that start through a launcher (Steam, Epic…) can exit the launched process immediately. Fill in **Process name**
  (e.g. `eldenring.exe`) in Edit Game and the app will watch that process instead.
- **End Session** stops tracking manually. Sessions are stored in the `sessions` table and recovered after a restart.
- Console games have no `.exe`: use **+ Log time** on the details page.
- Quick test: set a game's exe to `C:\Windows\System32\notepad.exe`, press Play, then close Notepad.

## Cover art (RAWG) and themes
1. Create a free account and API key at https://rawg.io/apidocs.
2. In the app open **Settings**, paste the key and press **Save key** (or set the `RAWG_API_KEY` environment variable, which wins).
   The key is stored in `settings.properties` next to `games.db`; nothing is hard-coded in the source.
3. Adding a game: type the title and leave the field. Matches appear in a list, so pick the right one (exact matches are auto-selected)
   and see a preview. Pressing Save with no cover chosen auto-attaches only an EXACT title match.
4. Games without a cover (like the samples) are looked up once automatically and the card updates by itself.
5. Images are cached in the `covers` folder and remembered in the database (RAWG id, image URL, local path), so they work offline.
   Use **Browse…** in Edit Game to pick your own image instead.
- Light/dark mode: use the single sun/moon button in the top bar. The choice is saved in `settings.properties`.
- RAWG requires attribution: the sidebar shows a "Game data & covers: RAWG" link. Please read RAWG's current API terms.

## Game executable (.exe)
In Add/Edit Game, use **Browse…** to pick the game's `.exe`, or paste a path (quotes from "Copy as path" are removed automatically).
Click **?** for a step-by-step guide to finding the file. The field is optional; if filled, the file must exist and end in `.exe`.

## Packaging as a Windows app
1. Maven panel > Lifecycle > `clean`, then `package` (creates target\game-backlog-1.0.jar).
2. Double-click `package.bat` (needs jpackage from JDK 17+ on PATH). The app appears in `dist\Playora`.
3. Zip the `dist\Playora` folder and share it. Data is stored in `%APPDATA%\Playora`.
