# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### CI
- Upgrade from JDK 8 to JDK 11 to fix the problem encountered by Spotless when formatting Kotlin.
- Automation of the right to execute (x) shell scripts.
### Upgraded examples
- "Keyple Service Library" to version `2.1.0`
- "Keyple Service Resource Library" to version `2.0.2`

## [2.0.1] - 2022-06-10
### Added
- `CHANGELOG.md` file (issue [#7]).
- CI: Forbid the publication of a version already released (issue [#5]).
### Fixed
- Removal of the unused Jacoco plugin for compiling Android applications that had an unwanted side effect when the application was launched (stacktrace with warnings).
### Upgraded
- "Keyple Util Library" to version `2.1.0` by removing the use of deprecated methods.
### Upgraded examples
- "Calypsonet Terminal Calypso API" to version `1.2.+`
- "Keyple Service Resource Library" to version `2.0.1`
- "Keyple Card Calypso Library" to version `2.2.0`

## [2.0.0] - 2021-10-06
### Changed
- Upgrade the component to comply with the new internal APIs of Keyple 2.0

## [1.1.0] - 2021-01-29
This is the initial release.

[unreleased]: https://github.com/calypsonet/keyple-plugin-cna-flowbird-android-java-lib/compare/2.0.1...HEAD
[2.0.1]: https://github.com/calypsonet/keyple-plugin-cna-flowbird-android-java-lib/compare/2.0.0...2.0.1
[2.0.0]: https://github.com/calypsonet/keyple-plugin-cna-flowbird-android-java-lib/compare/1.1.0...2.0.0
[1.1.0]: https://github.com/calypsonet/keyple-plugin-cna-flowbird-android-java-lib/releases/tag/1.1.0

[#7]: https://github.com/calypsonet/keyple-plugin-cna-flowbird-android-java-lib/issues/7
[#5]: https://github.com/calypsonet/keyple-plugin-cna-flowbird-android-java-lib/issues/5