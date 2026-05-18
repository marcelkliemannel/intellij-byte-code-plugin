# AGENTS.md

Guidance for code agents working in this repository.

## Project Snapshot

This is a single-module Kotlin IntelliJ Platform plugin named `Byte Code Analyzer`.
It opens JVM `.class` files, parses them with a shaded ASM dependency, and shows
multiple tool-window views such as structure, plain text, ASMified source,
decompiled source, and constant pool.

The package root is:

`src/main/kotlin/dev/turingcomplete/intellijbytecodeplugin`

Resources and IntelliJ plugin registration live in:

`src/main/resources/META-INF/plugin.xml`

## Tooling

- Build system: Gradle Kotlin DSL via `./gradlew`.
- JVM target/toolchain: Java 21.
- Language: Kotlin.
- IntelliJ Platform Gradle plugin is configured in `build.gradle.kts`.
- Dependency versions are in `gradle/libs.versions.toml`.
- Formatting: Spotless with `ktfmt().googleStyle()`.
- Tests: JUnit 5 plus some JUnit 4 parameterized tests on IntelliJ test
  framework fixtures.

Useful commands:

```bash
./gradlew test
./gradlew check
./gradlew spotlessApply
./gradlew runIde
./gradlew shadowAsmJar
./gradlew verifyPlugin
```

Use `./gradlew test --tests "fully.qualified.TestClass"` for focused test runs.
Some class-file consumer tests intentionally parse many classes and can be slow.

## Repository Map

- `src/main/kotlin/.../_ui`: Tool-window UI, tabs, data-provider plumbing,
  shared Swing helpers, notifications, and icons.
- `src/main/kotlin/.../common`: Public-ish model and service entry points such
  as `ClassFile`, `ClassFileContext`, `SourceFile`, `CommonDataKeys`, and
  `ByteCodeAnalyserOpenClassFileService`.
- `src/main/kotlin/.../openclassfiles`: Entry points and services that resolve
  source files, PSI elements, virtual files, dragged files, and chosen files to
  actual `.class` files.
- `src/main/kotlin/.../bytecode`: ASM-backed byte-code utilities for access
  flags, class versions, method declarations, frames, traces, types, and
  constant-pool parsing.
- `src/main/kotlin/.../view`: Byte-code views and per-class actions.
- `src/main/kotlin/.../tool`: General tool-window tools that are not tied to a
  currently opened class file.
- `src/main/resources`: `plugin.xml`, plugin icons, action icons, and
  `byte-code-instructions.csv`.
- `src/test/kotlin`: IntelliJ platform tests and byte-code parsing tests.
- `testProject`: Java/Kotlin fixture project used by tests.
- `screenshots`: README/plugin marketplace screenshots.

Packages whose name starts with `_` are internal. Do not treat them as stable
extension API.

## Main Code Flows

Opening a class file:

1. UI/action entry points call `ByteCodeAnalyserOpenClassFileService`.
2. `ClassFilesFinderService` maps source files, PSI elements, virtual files, or
   existing `ClassFile` instances to class files or preparation tasks.
3. `ClassFilesPreparatorService` compiles/prepares class files when the source
   file must be compiled first.
4. `ByteCodeToolWindowFactory.openClassFile(...)` creates a `ClassFileTab`.
5. `ClassFileTab` builds a `DefaultClassFileContext`, then creates all
   registered `ByteCodeView` implementations as tabs.

Parsing and rendering:

- `DefaultClassFileContext` owns the ASM `ClassReader`, ASM `ClassNode`, and
  related nested/outer class-file discovery.
- Views use `ClassFileContext` instead of reopening files directly.
- Structure rendering starts in `view/_internal/_structure/StructureView.kt`
  and `StructureTree.kt`, then fans out into `_class` and `_common` nodes.
- Constant-pool parsing starts in `bytecode/_internal/constantpool` and is
  rendered by `view/_internal/_constantpool`.

Toolbar actions:

- `ByteCodeAction` implementations are per-opened-class actions shown in view
  toolbars. Current examples include reparse and verify.
- `ByteCodeTool` implementations are general tools shown in the tool-window
  title menu. Current examples include access converter, signature parser,
  instructions overview, and class versions overview.

## Extension Points

All local extension points are declared and wired in `plugin.xml`.

- `dev.turingcomplete.intellijbytecodeplugin.openClassFilesAction`
  - Interface: `openclassfiles.OpenClassFilesToolWindowAction`
  - Add actions that open class files from the tool window.
- `dev.turingcomplete.intellijbytecodeplugin.byteCodeTool`
  - Interface: `tool.ByteCodeTool`
  - Add standalone byte-code tools.
- `dev.turingcomplete.intellijbytecodeplugin.byteCodeView`
  - Interface: `view.ByteCodeView.Creator`
  - Add a tab/view for a parsed class file.
- `dev.turingcomplete.intellijbytecodeplugin.byteCodeAction`
  - Interface: `view.ByteCodeAction`
  - Add actions that operate on the selected class file/view context.

When adding a new implementation, update both the Kotlin class and the matching
`plugin.xml` extension registration.

## Coding Conventions

- Keep source formatting compatible with `.editorconfig`: 2-space indentation,
  LF line endings, UTF-8, and no required final newline.
- Prefer existing IntelliJ Platform APIs, Swing helpers, and local UI utilities
  over adding new abstractions.
- UI code should be EDT-aware. Background work generally goes through
  `ApplicationManager`, `ReadAction`, or `AsyncUtils`; UI updates should return
  to `invokeLater`.
- Use `DumbService` checks when PSI/index-dependent logic may run during
  indexing.
- Preserve `Disposable` ownership when adding UI components or listeners.
- Use project services via `project.getService(...)` where the codebase already
  follows that pattern.
- Use the shaded ASM package:
  `dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm`.
  Do not import unshaded `org.objectweb.asm` from production plugin code.
- Avoid changing public behavior of non-underscore packages casually; they are
  the closest thing this project has to extension API.

## Testing Guidance

Start with focused tests around the touched area:

- Open-class-file resolution:
  `src/test/kotlin/.../openclassfiles/_internal/ClassFilesFinderServiceTest.kt`
  and `ClassFilesPreparatorServiceTest.kt`.
- Structure rendering:
  `src/test/kotlin/.../view/_internal/_structure/StructureTreeTest.kt`.
- Constant-pool parsing:
  `src/test/kotlin/.../_internal/constantpool/ConstantPoolTest.kt`.
- Class version helpers:
  `src/test/kotlin/.../bytecode/ClassVersionUtilsTest.kt`.

`ClassFileConsumerTestCase.LIMIT_CLASSES` limits expensive library-wide parsing
tests to 800 classes per library. Increase it for release confidence runs, not
for routine local iterations.

Run `./gradlew check` before handing off larger changes. For formatting-only
failures, run `./gradlew spotlessApply`.

## Common Tasks

Add a byte-code view:

1. Implement `ByteCodeView` and nested/adjacent `Creator`.
2. Register the creator in `plugin.xml` under `byteCodeView`.
3. Use `ClassFileContext` for ASM data and project/file access.
4. Add focused tests if parsing, rendering, or state restore behavior changes.

Add a tool-window tool:

1. Implement `ByteCodeTool`.
2. Register it in `plugin.xml` under `byteCodeTool`.
3. Keep it independent from a currently selected class file unless it should be
   a `ByteCodeAction` instead.

Add a per-class action:

1. Implement `ByteCodeAction`.
2. Register it in `plugin.xml` under `byteCodeAction`.
3. Use `CommonDataKeys`/`ClassFileContext` data-provider plumbing instead of
   manually reaching into UI internals.

Change class-file discovery:

1. Start in `ClassFilesFinderService`.
2. Check `ClassFileCandidates`, `ClassNameProviderConfigurations`, and
   `ClassFilesPreparatorService` before adding new resolution logic.
3. Cover Java, Kotlin, nested classes, enum entries, module descriptors, source
   roots, compiled output, libraries, and dumb mode where relevant.

Update ASM or add support for a new Java class-file version:

1. Update `asm` in `gradle/libs.versions.toml`.
2. Run or rely on `shadowAsmJar` to rebuild the shaded ASM JAR.
3. Update `DefaultClassFileContext.ASM_API` if ASM exposes a newer API constant.
4. Update `ClassVersionUtils.CLASS_VERSIONS`.
5. Update README and `plugin.xml` description text for the ASM and Java support
   versions.
6. Run the relevant parsing tests and at least one full `./gradlew check`.

## Release And Metadata Notes

- Plugin id: `dev.turingcomplete.intellijbytecodeplugin`.
- Plugin version, since-build, and target platform are in `gradle.properties`.
- Changelog rendering is configured through the Gradle changelog plugin.
- Signing and publishing expect local JetBrains credentials/properties; do not
  try to publish from an agent session unless explicitly asked.
- Marketplace-facing description is duplicated between README and `plugin.xml`;
  keep them consistent when changing advertised features or supported versions.

