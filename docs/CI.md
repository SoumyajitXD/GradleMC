# GradleMC CI guide

GradleMC maintains eight CI targets across Forge, Fabric, and NeoForge on Minecraft 1.20.1, 1.21.11 and 26.1.2, as applicable. Quilt is discontinued.

## Triggers and coverage

- Every push to main, every weekly schedule, and manual dispatch: all eight targets.
- Pull requests: changed targets only, unless CI/unknown files change or the diff cannot be determined; in those cases all eight run.
- Documentation-only PRs can legitimately skip compilation. They are not proof of a successful full build.
- Merge queue events run all eight targets if a merge queue is enabled.

The source of truth for commands, operating systems and Java versions is [.github/ci/targets.json](../.github/ci/targets.json).
Selection is tested using:

    python3 -m unittest discover -s .github/scripts/tests -p 'test_*.py' -v

The stable Required checks job enforces the expected test/build results and refuses unexpected full-matrix skips.

## Security and maintenance

All third-party Actions are SHA pinned, checkout does not retain credentials, permissions are read-only, and setup-gradle checks Gradle wrapper JARs against known official hashes. A machine-specific org.gradle.java.home is a CI error. CI does not alter gradlew or gradle.properties before compiling. Only basic open-source Gradle caching is configured; it is not combined with setup-java Gradle caching.

Release candidates must pass [the release checklist](RELEASE_CHECKLIST.md). CI compilation, self-tests, metadata checks, and SHA-256 hashes do not certify runtime GUI behavior or a dedicated Minecraft server. That requires separate runtime evidence using the final JAR with matching loader dependencies.

To enforce this policy for merges, require the Required checks status in repository branch protection/rulesets; a repository administrator must configure that separately.
