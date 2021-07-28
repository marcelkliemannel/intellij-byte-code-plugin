# Changelog

## [Unreleased]
### Added

### Changed

### Removed

### Fixed
## [2.0.2] - 2021-07-28
### Fixed
- Fix compatibility with IntelliJ 2021.2

## [2.0.1] - 2021-07-20
### Fixed
- Opening a class file while indexing could lead to a stack overflow error (GitHub issue #6)

## [2.0.0] - 2021-07-05
### Added
- Update ASM to 9.2 to support Java 18
- Add search functionality to structure tree and constant pool (GitHub issue #3)

### Changed
- [Internally] Migrate 'open class file' topic to a light service

### Fixed
- Fix 'Analyze Byte Code' action does not open tool window if the window wasn't opened before
- Fix 'Analyze Byte Code' action does not find a class file if selected editor element is outside a class
- Fix 'Analyze Byte Code' action can't open class file of Kotlin source file that does not contain a class

## [1.0.1] - 2021-06-14
### Fixed
- Fix "Analyze Byte Code" does not show tool window (GitHub issue #1)

## [1.0.0] - 2021-06-08
### Added
- Initial release.