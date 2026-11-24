# AlgoSteps

**AlgoSteps is an Android learning app that helps first-year computing students practise search and array algorithms by tracing examples and writing Python code.**

1. **Algorithms** shows how an algorithm processes an example, one step at a time. In **Binary search**, students also predict which half to search on a new input and get feedback before moving on to code.
2. **Coding** lets students write and run a solution on the same inputs. When a practice sample fails, they can compare the expected and actual results and replay that exact example beside the feedback.
3. **View statistics** shows saved results and lets students reopen an exercise with its draft. **Settings** lets them adjust hints and clear their learning progress.

For a quick demo, follow [the binary search walkthrough](#demonstrate-the-app). It shows the complete learning flow in one exercise.

## Open and run

1. Clone this repository or extract the project ZIP. In Android Studio, open the project root containing `settings.gradle.kts`.
2. Install **Android SDK Platform 35** and the Android SDK Build Tools requested by Gradle. Use Gradle JDK 17 (used by CI) or JDK 21, accept the SDK licences and sync with the included Gradle 8.13 wrapper. First sync needs internet access to Google Maven, Maven Central, the Gradle plugin portal and the distribution host.
3. Start an API 35 virtual device in Device Manager, or connect an Android 8.0+ phone with USB debugging. Select the device and run the **app** configuration. An API 33+ device is needed to exercise the notification permission prompt.
4. The lessons, editor, judge and statistics run on the device. Open **Algorithms → a lesson → Further reading** to fetch the live article. A successful fetch leaves a cached copy for offline use.

From the project root on Windows:
```powershell
.\gradlew.bat assembleDebug testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
```
Use `./gradlew` on macOS and Linux. The second command needs a running emulator or connected device. The APK built by the first command is written to `app/build/outputs/apk/debug/app-debug.apk`. No API keys, cloud database, or deployed backend are needed.

## Demonstrate the app

1. **Trace and practise.** Open **Algorithms → Binary search**. In **Example 1**, step through the search for 16 in `[2, 4, 7, 11, 16, 23]` until it reaches index 4. Under **Predict the next step**, try **Search right**, read the hint, then choose **Search left** to see why the sorted order rules out the right half. Choose **Open coding exercise → Run samples** with the default starter. Example 1 reports `expected 4, got -1`. Choose **Review example 1 steps** to replay that input inside the results card. Close the trace and check that the editor still contains the draft.
2. **Save and reopen progress.** Choose **Submit solution**, then go to **View statistics** from home and find the new practice entry. Stop and relaunch the app to check that the entry persists. Choose **Open practice** on that entry to reopen the matching lesson and current practice draft. From the lesson-to-practice route, Android Back returns to the existing trace at its previous step.
3. **Read online and offline.** In the lesson, read the disclosure above **Further reading**, then open it with an internet connection. Wait for **Reading updated from Wikipedia**. Disconnect the device and choose **Refresh article**. The page reports **Refresh unavailable. Showing your saved copy.** and keeps the dated excerpt visible. Reconnect after checking.
4. **Check progress controls.** With disposable test data, choose **Settings → Clear progress → Cancel** and verify that the history remains. Repeat and choose **Delete progress**. Room removes saved submissions and drafts in a single transaction and retains settings and cached readings.

## Two study paths

| Lesson | Algorithm activity | Coding objective |
| --- | --- | --- |
| Linear search | Inspect an unsorted list from left to right | Return the first matching index, or −1 |
| Count occurrences | Track a counter through an entire list | Return the frequency of a target value |
| Binary search | Inspect the midpoint and discard part of a sorted list | Return the target index, including at either endpoint |
| Fixed-size sliding window | Trace the value leaving and entering a fixed-size window | Return the largest window sum, including when all values are negative |

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

## Privacy and user control

Code is evaluated on the device, where Room stores drafts and scores. The app disables Android backup. The lesson explains the Wikipedia request before opening **Further reading**. The request includes a curated public article title and ordinary connection metadata. Excerpts link to the source and licence. Study reminders are opt-in and can be declined while continuing to use the learning features.

## Verification

JVM tests cover grading and curriculum logic. Android tests cover Compose interactions, navigation, Room persistence and Wikipedia response parsing.

| Check | Recorded result |
| --- | --- |
| GitHub Actions on commit `9f55eb4` | `build` and `device-tests` passed. The workflow uses JDK 17 and an API 35 emulator for the device suite. |
| Commit `c42e6e3` | The GitHub Actions `build` job passed. The API 35 `device-tests` job passed on rerun after the first attempt could not resolve the KSP plugin. On a clean local installation, the binary search walkthrough, prediction feedback and retry, and failed-sample review passed a manual check. |
| Binary search on the local emulator | Verified the failed-sample trace, return to the previous lesson step, and reopening the matching practice draft from statistics after restarting. |
| Wikipedia and Room on the local emulator | Verified live loading, offline refresh with the same saved excerpt, and submission persistence after restarting. Eight repository and parser tests passed using the Android test runner. |
| Clear progress on the local emulator | Cancel kept the test submission. Confirming deletion removed the submission from statistics. |

Use an **API 35** emulator to reproduce the CI device tests. On the local API 37.2 emulator, Espresso failed to initialize during the automated exam-history test, before its navigation assertions ran. See [GitHub Actions](https://github.com/yingkailiang-27/CP3406_Assessment3/actions) for the workflow results.

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
