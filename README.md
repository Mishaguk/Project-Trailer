# codeAIHelper

![Build](https://github.com/Mishaguk/codeAIHelper/workflows/Build/badge.svg)

IntelliJ IDEA plugin by team **Who_knows**.

<!-- Plugin description -->
codeAIHelper is an IntelliJ Platform plugin that adds AI-assisted helpers to the IDE.

The plugin currently ships a scaffold tool window registered under the **codeAIHelper** id. Real features will land here as the team builds them out.
<!-- Plugin description end -->

## Development

This project is built with Gradle and the [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html). See [`CLAUDE.md`](./CLAUDE.md) for an architecture overview and common commands.

Quick start:

```bash
./gradlew runIde      # launch a sandbox IDE with the plugin loaded
./gradlew test        # run unit tests
./gradlew build       # produce a distributable plugin zip in build/distributions/
```

## Installation

Once published to JetBrains Marketplace:

- <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > search for `codeAIHelper`.

Manual install from a built zip:

- <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd> and pick the zip from `build/distributions/`.

---
Plugin based on the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template).
