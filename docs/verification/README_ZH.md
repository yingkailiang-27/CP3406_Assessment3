# AlgoSteps 1.1 与 1.1.1 修复与验证记录

[查看包含 1.1.2 的完整修复记录](FIX_HISTORY_ZH.md)。

本记录由 AI 根据这次项目的源码、Gradle 日志和测试报告整理。下文区分测试代码编译失败、应用功能验证和静态检查警告。可以按末尾的命令在自己的设备上复测。

## 1.1 开发中遇到的真实编译错误

新增数据库升级测试时，测试用 `Room.databaseBuilder(...).build().use { ... }` 打开迁移后的数据库。这一版依赖下，返回类型无法匹配 Kotlin 的 `Closeable.use`。Gradle 在编译测试代码时停止，这次执行尚未进入测试断言。改成在 `try` 中读取成绩与草稿，并在 `finally` 中显式调用 `migrated.close()`，随后构建成功。前后两次构建的原始输出摘录见 `migration-compile-excerpts.txt`。

| 功能 | 测试如何触发 | 检查了什么 |
| --- | --- | --- |
| 考试与草稿恢复 | 同一道题先保存练习草稿，再打开考试。正式提交后销毁旧 ViewModel，用新的 ViewModel 重进 | 考试独立起步。保存后重进可查看解析，同一答案的提交按钮禁用，数据库仍只有一次提交 |
| 无效代码与成绩 | 提交缺少冒号的函数定义，修正后依次运行样例、正式提交 | 语法错误显示提示、成绩为空、解析锁定。运行样例后仍不记分，正式提交才记录考试成绩 |
| 模式统计 | 放入一次通过的考试和一次未通过的练习，分别选择全部、考试、练习 | 三种视图分别显示 50%、100%、0% 的通过率 |
| 旧版数据升级 | 创建版本 1 的数据库，写入一条成绩与一份草稿，再用版本 2 打开 | 原成绩仍为 2/8，模式标为旧版，草稿文本保留 |

## 1.1.1 备份配置检查

旧版支持 Android 8.0 起的设备。Manifest 已设置 `android:allowBackup="false"`，并通过 `android:dataExtractionRules` 配置 Android 12 及以上的备份规则。Lint 提醒旧系统缺少 `android:fullBackupContent` 声明。此次在 `<application>` 增加 `android:fullBackupContent="false"`，显式关闭旧版系统的完整备份。警告原文见 `lint-before.txt`，复测输出见 `lint-after.txt`。

`test-results.csv` 逐项列出本次 JUnit 报告中的测试类、方法名、状态和执行时间。JVM 测试直接运行，Android 相关测试使用 Robolectric 的 API 35 环境。此次没有进行实体手机备份或换机恢复实验。

## 在 Windows 复测

在包含 `gradlew.bat` 的项目目录运行以下命令。前两行可在电脑上运行，第三行需要已启动的模拟器或已连接的安卓设备。

```powershell
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

Lint 的本地报告为 `app/build/reports/lint-results-debug.html`。第二行运行项目自带的 JVM 测试，第三行在设备上执行 Android 相关测试。做个人开发记录时，可以填写自己的设备型号、执行命令、实际输出和修改说明，再关联自己实际创建的 Git 提交。
