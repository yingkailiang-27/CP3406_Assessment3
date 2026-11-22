# AlgoSteps

An Android study app for first-year computing students who can read a loop but need practice turning an algorithm into working code. An algorithm lesson and a coding exercise share the same two examples, so a student can compare a handwritten trace with their program's output.

## Open and run

1. Extract the ZIP into a writable directory. In Android Studio, open the **AlgoSteps** folder containing `settings.gradle.kts`.
2. Install **Android SDK Platform 35** and the Android SDK Build Tools requested by Gradle. Use Gradle JDK 17 or a compatible bundled JDK, accept the SDK licences and let the included Gradle wrapper sync. First sync needs internet access to Google Maven, Maven Central, the Gradle plugin portal and the distribution host.
3. Start an API 35 virtual device in Device Manager, or connect an Android 8.0+ phone with USB debugging. Select the device and run the **app** configuration. An API 33+ device is needed to exercise the notification permission prompt.
4. The lessons, editor, judge and statistics run on the device. Open **Algorithms → a lesson → Further reading** to fetch the live article. A successful fetch leaves a cached copy for offline use.

From the project root on Windows:
```powershell
.\gradlew.bat assembleDebug testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
```
Use `./gradlew` on macOS and Linux. The second command needs a running emulator or connected device. The APK built by the first command is written to `app/build/outputs/apk/debug/app-debug.apk`. No API keys, cloud database, or deployed backend are needed.

## Two study paths

| Lesson | Algorithm activity | Coding objective |
| --- | --- | --- |
| Find the first match | Inspect an unsorted list from left to right | Return the first matching index, or −1 |
| Count every match | Track a counter through an entire list | Return the frequency of a target value |
| Halve the search interval | Inspect the midpoint and discard part of a sorted list | Return the target index, including at either endpoint |
| Slide a fixed window | Trace the value leaving and entering a fixed-size window | Return the largest window sum, including when all values are negative |

**Algorithms.** Step forward or back through two worked inputs. Each step names the active index or window, explains the decision, and leads into a correctness argument and time/space analysis.

**Coding.** Practice offers an editable starter, a topic-specific hint and a numbered worked solution with explanations. A single-problem exam keeps the worked solution behind the first recorded submission. Practice and exam drafts are separate. **Run samples** evaluates two public cases. **Submit solution** grades those cases plus six additional checks and saves the result. A sample run never changes statistics. The acceptance rate is the fraction of saved submissions that passed every check. The solved-problem count uses each distinct lesson that has ever passed.

The editor runs a small, bounded Python teaching subset on the device. Implement `def solve(nums, target):` and return one integer. It supports integer variables, comparisons, indexed reads, `len(nums)`, conditionals, loops and integer arithmetic. It accepts neither imports nor arbitrary packages. Source length, integer range and execution steps are bounded. A passing outcome means the implementation passed the test cases; the judge does not measure asymptotic complexity. The exam is for individual self-assessment on a personal device. The four exercises and assessment cases are bundled in the source.

## Android architecture

| Component | Responsibility |
| --- | --- |
| `AlgoNavigation.kt`, `MainActivity.kt` | Single activity, Navigation Compose destinations and system Back |
| `content/Curriculum.kt` | Original questions, examples, step traces, code notes and assessment cases |
| `judge/` | Kotlin interface and JVM evaluator. Students' programs are interpreted locally |
| `ui/` | Material 3 Compose screens. Lifecycle-aware ViewModels handle grading, reading refresh and settings |
| `data/StudyRepository.kt` | Repository and manual dependency injection through Application → AppContainer → Repository → ViewModel |
| `data/StudyDatabase.kt` | Room tables for submissions, drafts, preferences and readings |
| `data/WikipediaSource.kt` | MediaWiki HTTPS API request for a curated subject, attribution URL, response validation and cache |
| `data/ReviewWorker.kt` | Unique periodic WorkManager job. Posts an opt-in reminder if a previously attempted problem has never been solved |

## Demonstrate the app

1. Open **Algorithms → Halve the search interval**. For target 16 in `[2, 4, 7, 11, 16, 23]`, trace index 2 (value 7) and index 4 (value 16). Switch examples to see the case where the target is absent.
2. Select **Open coding exercise**. Run the unchanged starter and compare its output against the public cases. Open the code walkthrough and examine why `low <= high` includes a one-element interval. Edit and resubmit a solution. **View statistics** displays the new outcome, test count and date.
3. Enter the same problem from **Coding → Exam**. Check that the practice draft and walkthrough are kept separate. Review the answer after submitting.
4. In an algorithm lesson, load **Further reading** while connected, wait for **Reading updated from Wikipedia**, then disconnect and refresh. The dated saved article remains available. On a first offline visit, the page explains how to retry.
5. Under **Settings**, switch hints, test permission denial/grant on Android 13+, and confirm that learning remains available. With explicit opt-in, the app schedules a daily review check, first eligible roughly 24 hours later. It only posts if there is a never-solved attempt. Android schedules the actual delivery time. Use Android Studio's Background Task Inspector to inspect the scheduled worker.
6. Under **Settings → Clear progress**, cancel once and verify your history remains. Confirm to remove submissions and drafts. The public reading cache and preferences remain available.

## Ethics, testing and course submission

The app stores learners' drafts and results locally and disables Android backup for this project. Article requests send a curated public title to Wikipedia, along with normal HTTP connection metadata. The user can decline reminders or delete progress. Labels accompany colour feedback and the layout supports larger fonts and system dark mode. Wikipedia excerpts show the article link and CC BY-SA licence.

JVM tests cover grading behavior and curriculum consistency. Instrumentation tests exercise Compose interactions, Room persistence and parsing. A GitHub Actions workflow runs the project checks on pushes or pull requests once the repository is created. The Chinese guide provides first-run steps and a course rubric crosswalk.

Version 1.1.1 passed 20 host tests. In earlier live checks, the four curated MediaWiki requests returned an introductory extract and HTTPS article URL. To test the full suite on a computer, run `./gradlew -I gradle/host-tests.gradle :app:testDebugUnitTest` or use `.\gradlew.bat` on Windows. Physical installation and scheduled notification delivery can be checked on the demo phone.

For the individual assessment, maintain your own GitHub repository, document actual changes in regular commits, and grant the lecturer and subject coordinator access. Submit the Android Studio project export, the repository link, the personal two-page / 1,000-word Gibbs reflection about **your Assessment 2** and your AI-generated material declaration. Disclose the generation of this starting project, including the code, original practice material, tests and documentation you retain. Document what you personally changed and verified. Only describe observations you actually made.

## Sources

Lesson prompts, worked examples, traces and solutions were created for AlgoSteps. Internet readings are fetched from English Wikipedia and link to the corresponding article and its contributors. Platform references:

- [Android architecture](https://developer.android.com/topic/architecture)
- [Room](https://developer.android.com/training/data-storage/room)
- [WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent)
- [Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)
- [MediaWiki TextExtracts](https://www.mediawiki.org/wiki/Extension:TextExtracts)
- [Wikipedia text licence](https://creativecommons.org/licenses/by-sa/4.0/)

## Version 1.1

Syntax and unsupported source errors are shown before grading. A rejected submission does not affect statistics or unlock an exam answer. The last saved score and exam walkthrough access are restored when an exercise is reopened. Submitting the same code twice in a row for the same lesson and mode counts once. Statistics can filter by practice or exam. Scores from version 1.0 appear under Previous version because they did not record a mode.

## Version 1.1.1

Explicit legacy full-backup configuration clears the remaining Android Lint warning. See [the verification record](docs/verification/README_ZH.md) for the before/after output and the 20 recorded test results.


## Version 1.1.2

Exam submissions require a parsed return statement before an unfinished draft can unlock the answer. An incorrect integer answer still receives a score. Re-running samples keeps the extra checks for the last submission visible until the draft changes. Opening an attempt from history restores the matching exam or practice draft. The judge now handles keyword punctuation, reserved identifiers, augmented assignments and decimal literals more accurately.

The [Chinese verification record](docs/verification/FIX_HISTORY_ZH.md) documents all 11 fixes in this release and five earlier fixes, with reproducible cases and actual test output.
