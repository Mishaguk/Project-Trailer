# ProjectTrailer

![Build](https://github.com/Mishaguk/ProjectTrailer/workflows/Build/badge.svg)

IntelliJ IDEA plugin by team **Who_knows**.

<!-- Plugin description -->
ProjectTrailer is an IntelliJ Platform plugin that adds AI-assisted helpers to the IDE.

The plugin currently ships a scaffold tool window registered under the **ProjectTrailer** id. Real features will land here as the team builds them out.
<!-- Plugin description end -->

## Development

This project is built with Gradle and the [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html). See [`CLAUDE.md`](./CLAUDE.md) for an architecture overview and common commands.

### Quick plugin development boot

1. **Install prerequisites**
   - JDK 17+ (IntelliJ Platform 2025.2 requires JDK 21 at runtime — the Gradle toolchain resolver will fetch it automatically via `foojay-resolver-convention`).
   - IntelliJ IDEA 2025.2+ (Community or Ultimate). The Kotlin and Gradle plugins ship bundled.
   - Git.

2. **Clone and open**
   ```bash
   git clone https://github.com/Mishaguk/ProjectTrailer.git
   cd ProjectTrailer
   ```
   Open the folder in IntelliJ IDEA — the IDE will auto-import the Gradle project and download dependencies on first sync (this can take several minutes; platform artifacts are large).

3. **Configure the OpenAI key (optional, only if testing AI features)**
   Create `src/main/resources/env.local` with:
   ```
   OPENAI_API_KEY=sk-your-key-here
   ```
   This file is bundled into the plugin classpath, so keep it out of version control.

4. **Launch a sandbox IDE with the plugin loaded**
   ```bash
   ./gradlew runIde
   ```
   Or use the pre-made `Run Plugin` configuration in `.run/` (dropdown in the IDE toolbar → **Run Plugin**). A second IntelliJ instance will boot with ProjectTrailer installed. Find the tool window via **View → Tool Windows → ProjectTrailer**.

5. **Run tests**
   ```bash
   ./gradlew test
   ```
   Or use the `Run Tests` configuration in `.run/`.

6. **Verify the plugin against target IDE versions before publishing**
   ```bash
   ./gradlew verifyPlugin
   ```

7. **Build a distributable zip**
   ```bash
   ./gradlew build
   ```
   Output lands in `build/distributions/*.zip` — this is what you upload to JetBrains Marketplace or install manually.

### Common Gradle commands

```bash
./gradlew runIde         # launch sandbox IDE with the plugin loaded
./gradlew test           # run unit tests
./gradlew build          # produce a distributable plugin zip in build/distributions/
./gradlew verifyPlugin   # verify compatibility with target IDE versions
./gradlew patchChangelog # roll [Unreleased] into the current version
```

## Installation

Once published to JetBrains Marketplace:

- <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > search for `ProjectTrailer`.

Manual install from a built zip:

- <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd> and pick the zip from `build/distributions/`.

---
Plugin based on the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template).
