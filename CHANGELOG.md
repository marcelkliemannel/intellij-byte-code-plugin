# Changelog

## Unreleased

### Added

### Changed

### Removed

### Fixed

## 4.3.0 - 2025-01-15

### Added

- Support for Java 24 using ASM 9.7.1

## 4.2.1 - 2024-08-07

### Added

- Add support for Kotlin K2 compiler beta mode 

### Changed

- The "Analyze Byte Code" action in the context menu is now only visible if the file is analysable
- Set minimum IntelliJ Version to 2024.1

## 4.2.0 - 2024-04-11

### Added

- Support for Java 23 with ASM 9.7

### Changed

- Lower minimum IntelliJ version to 2023.2 to support Android Studio

## 4.1.0 - 2023-12-25

### Changed

- For lambda classes, a fallback to the parent class is now made if the lambda class file is not found. This is necessary because, for example, the Kotlin compiler often optimises lambda code and does not create an extra class.
- The Open Class Files mechanism has been optimised for performance by using read access to the PSI data model (IntelliJ's source code model) only when it is actually needed.

## 4.0.0 - 2023-12-17

### Added

- New decompiled tab (this replaces the decompile toolbar action)
- Add Kotlin metadata overview to the structure view
- List relates class files (e.g., nested classes for lambdas) to the structure view
- New option to build the whole project if a class file is outdated or missing
- Add option to re-compile the class file in the "Re-Parse Class File" action if the class file is outdated or missing
- Add source file information to the structure view

### Changed

- Complete revision of the open class file mechanism. This results in better support for nested, local and anonymous classes and Kotlin files

### Fixed

- Fix "Re-Parse Class File" action may work on an outdated cached version

## 3.2.0 - 2023-11-07

### Added

- Add Java 22 support by updating ASM to 9.6
- Add persistent storage for configuration of the structure view and parsing options

## 3.1.0 - 2023-06-24

### Added

- ASM 9.5 with Java 21 support

### Changed

- Adapt tool window icon to the new IntelliJ UI design philosophy

### Fixed

- Fix wrong usage of EDT thread for the context action

## 3.0.0 - 2023-03-12

### Added

- Automatically run code style formatting in the ASM view
- Add Java class versions overview

### Changed

- Compatibility improvements for IntelliJ 2023.1
- Compatibility improvements for IntelliJ's "new UI"
- UI improvements in the access converter
- Change order of tabs, for better accessibility

### Fixed

- Fix parameters are not showing for abstract methods in the tree structure
- Fix misleading icon for abstract methods in the tree structure
- Fix display of class version in the structure view if the ASM ClassNode version is represented as a negative

## 2.5.0 - 2022-10-13

### Added

- Update to ASM 9.4 to support Java 20

## 2.4.0 - 2022-09-18

### Added

- Add a built-in overview of all byte code instructions
- Add class file version mapping for Java 19

### Fixed

- Prevent potential NPE in the drag and drop mechanism

## 2.3.0 - 2022-05-21

### Changed

- Update bundled ASM to 9.3

## 2.2.1 - 2022-02-16

### Fixed

- Fix display of enum access flags

## 2.2.0 - 2021-10-15

### Added

- New byte code action to re-parse the opened class file

### Changed

- Improve icons of the byte code actions

## 2.1.0 - 2021-10-09

### Changed

- Move method frames to a non-modal dialog
- Move byte code actions to the left to keep them at a fixed position
- Move byte code related links to a separate group
- Add report issue link

### Fixed

- Fix TooManyListenersException (GitHub issue #12)
- Fix removed ToolWindow#getEmptyText() in 2021.3 (GitHub issue #13)

## 2.0.3 - 2021-08-29

### Fixed

- Fix swapped column names "Locals" and "Stack" in method frames dialog (GitHub issue #9)

## 2.0.2 - 2021-07-28

### Fixed

- Fix compatibility with IntelliJ 2021.2

## 2.0.1 - 2021-07-20

### Fixed

- Opening a class file while indexing could lead to a stack overflow error (GitHub issue #6)

## 2.0.0 - 2021-07-05

### Added

- Update ASM to 9.2 to support Java 18
- Add search functionality to structure tree and constant pool (GitHub issue #3)

### Changed

- [Internally] Migrate 'open class file' topic to a light service

### Fixed

- Fix 'Analyze Byte Code' action does not open tool window if the window wasn't opened before
- Fix 'Analyze Byte Code' action does not find a class file if selected editor element is outside a class
- Fix 'Analyze Byte Code' action can't open class file of Kotlin source file that does not contain a class

## 1.0.1 - 2021-06-14

### Fixed

- Fix "Analyze Byte Code" does not show tool window (GitHub issue #1)

## 1.0.0 - 2021-06-08

### Added

- Initial release.
