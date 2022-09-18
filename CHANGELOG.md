# Changelog

## [Unreleased]
### Added

### Changed

### Removed

### Fixed

## [2.4.0] - 2022-09-18
### Added
- Add a built-in overview of all byte code instructions
- Add class file version mapping for Java 19

### Fixed
- Prevent potential NPE in the drag and drop mechanism

## [2.3.0] - 2022-05-21
### Changed
- Update bundled ASM to 9.3

## [2.2.1] - 2022-02-16
### Fixed
- Fix display of enum access flags

## [2.2.0] - 2021-10-15
### Added
- New byte code action to re-parse the opened class file

### Changed
- Improve icons of the byte code actions

## [2.1.0] - 2021-10-09
### Changed
- Move method frames to a non-modal dialog
- Move byte code actions to the left to keep them at a fixed position
- Move byte code related links to a separate group
- Add report issue link

### Fixed
- Fix TooManyListenersException (GitHub issue #12)
- Fix removed ToolWindow#getEmptyText() in 2021.3 (GitHub issue #13)

## [2.0.3] - 2021-08-29
### Fixed
- Fix swapped column names "Locals" and "Stack" in method frames dialog (GitHub issue #9)

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