# AlgoSteps 缺陷修复与验证记录

记录包含 16 项不同的问题。1.1.2 新修复 11 项（06–16），此前版本修复 5 项（01–05）。编号对应缺陷，Git 提交次数另计。此记录由 AI 协助整理，测试结果来自实际执行。

本轮新增的 11 项都在改动前触发了对应的失败，修复后的全套测试通过 33/33 项。其中 13 项直接在 JVM 上执行，20 项在 Robolectric 的 Android API 35 环境执行，失败、错误、跳过均为 0。Android Lint 输出 `No issues found.`，调试 APK 和设备测试 APK 均构建成功。

## 之前的版本

| 编号 | 版本与触发条件 | 原行为 → 修复行为 | 验证依据 |
| --- | --- | --- | --- |
| 01 | 1.1，提交考试后退出，再次打开同一题 | 原来只恢复草稿，已解锁的解析会重新锁住，同一份代码还能再次计分。现在从 Room 恢复本题、本模式的最后一次提交，并按源码摘要防止连续重复计分。 | 旧版 CodingViewModel 的状态仅在内存中；回归测试 `sampleAndSubmissionUseRealJudgeAndRecordOnlyOneAttempt` 重建 ViewModel，检查解析和记录数。 |
| 02 | 1.1，提交缺少冒号的函数定义 | 原来逐例捕获解析错误并计为失败，可能产生 0/8 成绩。现在先解析整个函数，语法报错时保留草稿，成绩与考试解锁状态不变。 | 同一端到端测试也检查无效输入、修正后运行样例、正式提交的三种结果。 |
| 03 | 1.1，首次编译数据库升级测试 | 迁移测试对 Room 对象调用不适用的 `.use`，测试尚未执行就编译失败。随后改用 `try/finally` 关闭数据库。 | `migration-compile-excerpts.txt` 保存失败和下一次成功的 Gradle 摘录；`migrationPreservesOldScoresAndDrafts` 验证旧成绩和草稿。 |
| 04 | 1.1，针对最低 API 26 执行 Android Lint | 基础主题中包含需要 API 27 的 `windowLightNavigationBar`。移除该无条件引用，使用 Activity 的系统栏配置。 | `theme-compatibility-excerpt.txt` 保存 NewApi 原始诊断；后续 Lint 不再报告。此处记录的是静态检查错误。 |
| 05 | 1.1.1，检查旧系统完整备份声明 | Manifest 设置了 Android 12+ 的数据提取规则，缺少旧系统的显式完整备份声明；现已添加 `android:fullBackupContent="false"`。 | 1.1.1 的 `lint-before.txt` 和 `lint-after.txt` 记录 1 条警告到无问题的变化。没有做真机备份泄漏实验。 |

## 1.1.2 的修复

| 编号 | 复现条件 | 之前的表现 → 修复行为 | 对应测试 |
| --- | --- | --- | --- |
| 06 | 在判题器提交 `while(i < len(nums)):` 等合法写法 | 解析器强制关键字后出现空格，报“不支持的语句”。现在按词法边界识别关键字，也能识别 `return-1`。 | `statementKeywordsAcceptParenthesesAndAdjacentOperators` |
| 07 | 以 Python 保留字作变量，如 `class = 4` | 部分保留字可用作变量，短路分支也可能藏住非法标识符。现在解析阶段即报错，并给出行号。 | `pythonKeywordsCannotBecomeNamesEvenInSkippedExpressions` |
| 08 | `count` 未初始化，执行 `count += nums[99]` | 原来先读取右侧列表元素，首先提示下标越界。现在按增强赋值的求值顺序先读取左侧变量，首先提示 `count` 未定义。 | `augmentedAssignmentChecksCurrentValueBeforeEvaluatingItsRightSide` |
| 09 | 在新考试中直接提交默认的 `pass` 模板 | 该模板语法合法，但没有任何返回语句，旧版本会记 0/8 并开放解析。现在要求考试作答至少包含一条解析器识别的 `return`。返回错误整数的真实作答仍正常评分，即使是 0/8。 | `unfinishedExamCannotRevealAnswerButAnIncorrectIntegerStillCounts` |
| 10 | 提交代码后，未编辑，立即再点 Run samples | 原来仅保留新样例报告，刚提交的额外测试明细消失。现在同一草稿可以同时查看样例结果与最近一次提交的额外测试明细；编辑后清除过时明细。 | `rerunningPublicSamplesKeepsTheSubmittedAdditionalCheckBreakdown` |
| 11 | 同一道题分别有练习草稿和考试草稿，从统计历史打开考试提交 | 旧导航只传题目 ID，固定打开练习草稿。现在打开对应模式。旧记录无模式时明确选择练习入口。 | `openingExamFromHistoryRestoresExamDraftWhenPracticeDraftAlsoExists` |
| 12 | 只有练习成绩，切换到 Exam 统计 | 旧界面同时显示无记录及 0% 通过率，容易误解为参加过考试但全部失败。现显示该模式没有提交；有真实失败提交时仍显示 0%。 | `emptyExamHistoryDoesNotReportAZeroPercentResult` |
| 13 | 首次保存成绩，然后用相同源码摘要重试 | 首次调用返回插入前的临时 ID 0，而重试返回 Room 分配的真实 ID。现在首次返回值、重试返回值、数据库行一致。问题涉及返回值，原成绩没有丢失。 | `firstSubmissionAndIdenticalRetryReturnTheSamePersistedRow` |
| 14 | 后台提醒已读取资格，但在发送前用户关闭提醒 | 旧顺序允许先取消通知、后被运行中的任务重新发出。现在最终资格检查与发送、关闭与撤销在同一把锁下排序。 | `disablingRemindersDuringDeliveryLeavesNoNotification` 控制两段协程执行顺序，并检查最终不可见。这是仓库层并发测试。 |
| 15 | 用辅助功能浏览算法演示的数组格子 | 旧语义树把单元格描述与视觉下标暴露为分开的节点，索引信息重复。现在每格只有一条含索引、数值和状态的描述。 | `oneLabelPerCellTracksTheShrinkingInterval` 检查 Compose 语义树。这次没有录制真机 TalkBack 音频。 |
| 16 | 提交 `return 010` 或提交 `return True or -007` | 旧判题器接受了带前导零的非零十进制整数，Python 3 在解析时会报语法错误。现已在数字解析阶段给出行号；合法的 `000` 和 `10` 仍可运行。 | `nonzeroDecimalLiteralsRejectLeadingZerosDuringParsing`，附 CPython 实测对照。 |

## 核对证据

- `baseline-regressions.csv` 记录保留的 1.1.1 源码运行选定回归用例所得的结果。复现时沿用旧版按钮标签、回调签名及无障碍描述。提醒测试按旧后台任务的顺序先查询、再发送；前后均用相同的受控协程顺序验证最终是否仍显示通知。
- `judge-before.txt`、`judge-after.txt` 是同一套判题源码检查的前后 JUnit 摘录。修复后另加了一项对“源码中是否存在真实返回语句”的测试，因此两次测试方法总数不同。
- `decimal-before.txt` 和 `decimal-after.txt` 记录第 16 项的单独复现与修复，前者也记录了 CPython 处理相同整数的结果。
- `gradle-summary.txt` 保留旧版 7 个失败用例与新版最终构建的任务结果摘录。
- `test-results-1.1.2.csv` 来自本次完整复测的 JUnit XML，每行对应一个测试方法。判题单独运行的结果与这里有重叠，不另行叠加。
- `lint-1.1.2.txt` 是本次 Android Lint 的输出。自动化覆盖的范围是源码、模型、Room 和模拟的 Android 环境，未在实体设备上进行系统备份或通知投递实验。

## Windows 上重复运行

在包含 `gradlew.bat` 的项目目录打开 PowerShell。第一条使用随项目附带的配置运行 JVM 与 Robolectric Android 测试，首次需要联网下载依赖。第二条执行 Lint 并构建 APK。

```powershell
.\gradlew.bat -I gradle/host-tests.gradle :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug :app:assembleDebug
```

测试报告在 `app/build/reports/tests/testDebugUnitTest/index.html`。如需用手机验证，在 Android Studio 选择真机后执行 `.\gradlew.bat :app:connectedDebugAndroidTest`。可用实际设备、原始输出和修改的文件补充个人复测记录。
