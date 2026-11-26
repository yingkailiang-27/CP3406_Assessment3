# AlgoSteps

AlgoSteps is an Android learning app for first-year computing students. It teaches search and array algorithms through worked examples, prediction questions and Python coding exercises.

- **Browse algorithms** to trace linear search, count occurrences, binary search and fixed-size sliding window. Each built-in lesson asks students to predict the next step on a new input and explains the answer.
- **Coding practice** lets students run sample tests, submit a solution and replay the input from a failed sample. The built-in exercises also offer an independent exam mode.
- **Search lessons** from the home screen or choose **Import a lesson file** to add a supported JSON lesson. **View statistics** shows saved results and lets students reopen an exercise with its draft.

For marking, use the [setup instructions](#open-and-run) and follow the [binary search demo](#quick-demo).

## Open and run

1. Clone this repository or extract the project ZIP. In Android Studio, open the project root containing `settings.gradle.kts`.
2. Install **Android SDK Platform 35** and the Android SDK Build Tools requested by Gradle. Use Gradle JDK 17 (used by CI) or JDK 21, accept the SDK licences and sync with the included Gradle 8.13 wrapper. First sync needs internet access to Google Maven, Maven Central, the Gradle plugin portal and the distribution host.
3. Start an API 35 virtual device in Device Manager, or connect an Android 8.0+ phone with USB debugging. Select the device and run the **app** configuration. An API 33+ device is needed to exercise the notification permission prompt.
4. The lessons, editor, judge and statistics run on the device. Open **Browse algorithms → a lesson → Further reading** to fetch the live article. A successful fetch leaves a cached copy for offline use.

From the project root on Windows:
```powershell
.\gradlew.bat assembleDebug testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
```
Use `bash ./gradlew` on macOS and Linux. The second command needs a running emulator or connected device. The APK built by the first command is written to `app/build/outputs/apk/debug/app-debug.apk`. No API keys, cloud database, or deployed backend are needed.

## Quick demo

1. **Trace and practise.** Open **Browse algorithms → Binary search**. In **Example 1**, step through the search for 16 in `[2, 4, 7, 11, 16, 23]` until it reaches index 4. Under **Predict the next step**, try **Search right**, read the hint, then choose **Search left** to see why the sorted order rules out the right half. Choose **Open coding exercise → Run samples** with the starter code on a fresh practice draft. Example 1 reports `expected 4, got -1`. Choose **Review example 1 steps** to replay that input inside the results card. Close the trace and check that the editor still contains the draft.
2. **Save and reopen progress.** Choose **Submit solution**, then go to **View statistics** from home and find the new practice entry. Stop and relaunch the app to check that the entry persists. Choose **Open practice** on that entry to reopen the matching lesson and current practice draft. From the lesson-to-practice route, Android Back returns to the existing trace at its previous step.
## Import a sample lesson

1. Copy [maximum-subarray-lesson.json](examples/maximum-subarray-lesson.json) to the Android device or emulator so it is accessible in the file picker.
2. Open **Home → Import a lesson file**, select the JSON file, and confirm **Import lessons** in the preview.
3. The app opens the course library after import. Tap the **Maximum subarray sum** course card, switch between its two examples, and try **Predict the next step**.
4. Choose **Open coding exercise → Run samples**. With the starter code on a fresh draft, Example 1 should report `expected 8, got 4`. Select **Review example 1 steps** to replay the same input.
5. Choose **Submit solution** to save a practice result. Restart the app, open **View statistics**, and choose **Open practice** on the saved entry. The imported lesson and its history remain on the device. Selecting the same package again displays a duplicate ID error.

The preview checks the package structure. Before opening an imported exercise, the app runs the provided reference solution against its example and grading cases. Imported lessons support study and practice through **Algorithms**. The built-in lessons also offer exam mode and Wikipedia reading.

## Built-in lessons

| Lesson | Algorithm activity | Prediction | Coding objective |
| --- | --- | --- | --- |
| Linear search | Scan from left to right | Decide whether to stop at the first match | Return the first matching index, or −1 |
| Count occurrences | Track a running count | Update the count when a repeated value matches | Return how often the target appears |
| Binary search | Narrow the interval in a sorted list | Choose which half to search next | Return the matching index, or −1 |
| Fixed-size sliding window | Track values entering and leaving the window | Choose which values to subtract and add | Return the largest fixed-width window sum |

**Algorithms.** Step forward or back through two worked inputs. Each step names the active index or window and explains the decision. A prediction question, a correctness explanation, and a time and space analysis follow the walkthrough.

**Coding practice.** Practice offers an editable starter, a topic-specific hint and a numbered worked solution with explanations. A single-problem exam keeps the worked solution behind the first recorded submission. Practice and exam drafts are separate. **Run samples** evaluates two public cases. **Submit solution** grades those cases plus the lesson’s additional checks and saves the result. A sample run never changes statistics. The acceptance rate is the fraction of saved submissions that passed every check. The solved-problem count uses each distinct lesson that has ever passed.

The editor runs a small, bounded Python teaching subset on the device. Implement `def solve(nums, target):` and return one integer. It supports integer variables, comparisons, indexed reads, `len(nums)`, conditionals, loops and integer arithmetic. It accepts neither imports nor arbitrary packages. Source length, integer range and execution steps are bounded. A passing outcome means the implementation passed the test cases; the judge does not measure asymptotic complexity. The exam is for individual self-assessment on a personal device. The four built-in exercises and their assessment cases are bundled in the source.

## Core screens

| Assignment screen | Where to find it | Implementation |
| --- | --- | --- |
| Landing Page | **Home** | Compose dashboard with lesson search, course import, practice shortcuts and a progress summary |
| Activity Screen | **Browse algorithms → Binary search** | Interactive step trace and prediction feedback, with navigation to the matching Python exercise |
| Settings Screen | **Home → Settings** (gear icon) | Preferences for hints and optional reminders, plus a confirmation dialog for clearing progress |
| User Statistics Screen | **Home → View statistics** | Room-backed progress and submission history, with mode filters and links to reopen saved drafts |

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

The app uses Kotlin, Jetpack Compose and Material 3. Navigation Compose connects the dashboard, lessons, practice, statistics and settings. ViewModels handle screen state, and the application container supplies the repository and data dependencies. **Further reading** fetches relevant Wikipedia content over HTTPS and shows a saved excerpt when an offline refresh fails. Room persists submissions, drafts, preferences and cached readings.

## Privacy and user control

Code is evaluated on the device, where Room stores drafts and scores. The app disables Android backup. The lesson explains the Wikipedia request before opening **Further reading**. The request includes a curated public article title and ordinary connection metadata. Excerpts link to the source and licence. Study reminders are opt-in and can be declined while continuing to use the learning features.

## Verification

The app revision `6ec1785` passed the local Debug build and all 13 JVM tests. Android Lint reported zero errors and 20 warnings. The GitHub Actions build and API 35 device tests also passed. The automated suites cover grading, curriculum, Compose interactions, navigation, Room persistence and Wikipedia response parsing. [View the CI run](https://github.com/yingkailiang-27/CP3406_Assessment3/actions/runs/45776491342)

Use an API 35 emulator to reproduce the device tests. An earlier local Espresso run on API 37.2 stopped during initialization. See the [verification notes](docs/verification/README_ZH.md) and [fix history](docs/verification/FIX_HISTORY_ZH.md) for earlier checks.

## Optional demo checks

1. **Read online and offline.** In the lesson, read the disclosure above **Further reading**, then open it with an internet connection. Wait for **Reading updated from Wikipedia**. Disconnect the device and choose **Refresh article**. The page reports **Refresh unavailable. Showing your saved copy.** and keeps the dated excerpt visible. Reconnect after checking.
2. **Check progress controls.** With disposable test data, choose **Settings → Clear progress → Cancel** and verify that the history remains. Repeat and choose **Delete progress**. Room removes saved submissions and drafts in a single transaction and retains settings and cached readings.

## Sources

Lesson prompts, worked examples, traces and solutions were created for AlgoSteps. Internet readings are fetched from English Wikipedia and link to the corresponding article and its contributors. Platform references:

- [Android architecture](https://developer.android.com/topic/architecture)
- [Room](https://developer.android.com/training/data-storage/room)
- [WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent)
- [Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)
- [MediaWiki TextExtracts](https://www.mediawiki.org/wiki/Extension:TextExtracts)
- [Wikipedia text licence](https://creativecommons.org/licenses/by-sa/4.0/)
