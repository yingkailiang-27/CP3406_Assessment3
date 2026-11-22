# AlgoSteps 中文运行指南

这是一个给大学一年级编程初学者使用的英文 Android App。手机应用用 Kotlin 和 Jetpack Compose 编写。学生在编辑器里编写 Python 教学子集，两者承担不同的工作。所有判题都在手机上完成。

要查看本次修了哪些问题，打开[中文修复记录](docs/verification/FIX_HISTORY_ZH.md)。每项列出了复现输入或操作、之前的表现、修改和对应的测试。

## 只想先在手机上体验

压缩包中 `install/AlgoSteps-debug.apk` 是已经编译的 Android 调试版。在 Android 8.0 及以上设备上打开 APK 并按系统提示安装，即可进入 App。Android 13 及以上可体验通知运行时授权。要修改、调试或导出作业，从下列步骤打开源码。

## 在 Windows 上第一次运行

1. 解压整个压缩包。启动 Android Studio，点 **Open**，选取同时包含 `settings.gradle.kts` 和 `gradlew.bat` 的 **AlgoSteps** 文件夹。
2. 在 **SDK Manager** 中安装 **Android SDK Platform 35**。让 Gradle 使用 JDK 17 或兼容的 Android Studio 自带 JDK。等待 Gradle Sync 完成，按提示安装缺少的 Android Build Tools 或接受许可。第一次同步需要联网。
3. 打开 **Device Manager**，新建手机模拟器并下载 **API 35** 系统镜像。也可以使用 Android 8.0 及以上真机，开启开发者选项和 USB 调试后连接电脑。
4. 在 Android Studio 的设备下拉框选中模拟器或手机，运行 **app**。看到 **Algorithms** 和 **Coding** 两个入口即已启动成功。真机上的 App 使用手机自己的网络。
5. 在项目根目录打开 PowerShell。运行 `.\gradlew.bat testDebugUnitTest` 验证判题和课程用例。启动模拟器后运行 `.\gradlew.bat connectedDebugAndroidTest` 检查 Compose 界面和 Room。GitHub 仓库中的 CI 工作流也配置了这两个阶段。

如果 Gradle 同步失败，先看错误里是缺少 SDK 35、JDK 不兼容，还是无法连接下载地址。只有延伸阅读需要在 App 内联网，第一次成功加载后会缓存。作业不要求自己租服务器、部署判题 API、配置 Python 环境或发布 Google Play。

## 用三分钟演示算法与代码的区别

在首页选 **Algorithms → Halve the search interval**。第一个样例的有序数组是 `[2, 4, 7, 11, 16, 23]`，目标是 16。先查看中点下标 2，那里的值 7 小于 16，因此保留下标 3 到 5。点击 **Next step** 后查看中点下标 4，命中 16。切换到第二个样例，看找不到目标时搜索区间怎样缩小。这一部分解释“为什么可以丢掉左半边”。

接着选 **Open coding exercise → Show code walkthrough**，展开编号代码和解释。重点看为什么 `while low <= high` 要保留等号，以及为什么用 `low = mid + 1`。先用原始代码点 **Run samples**，观察实际结果与期望值，再在编辑器里补全函数。点 **Submit solution** 后，系统才运行全部 8 个用例并把成绩写入 Room。返回首页选 **View statistics** 查看。

要体验单题自测，从 **Coding** 的题库里点 **Exam**。此时进入独立的考试草稿，首次有效提交前收起参考解法。当前练习和考试均按输出正确性评分；该模式适合自主学习，不提供班级监考、多用户排行或对算法复杂度的评分。

## 对照课程要求

| 作业项 | 本项目中的证据 | 演示方法 |
| --- | --- | --- |
| 针对具体学生群体的教育 App | 面向能读循环的大学编程初学者 | 先演示概念，再将同一输入与程序输出对应 |
| 首页、学习活动、设置、统计页 | 两个首页入口、算法页、编程页、Settings 和 Statistics | 从四类页面分别进入，再返回 |
| Kotlin、Compose、Material 3、导航 | 单 Activity + Navigation Compose，系统与页面返回 | 进入算法→配套练习→返回，恢复上一个页面 |
| ViewModel、Repository、依赖注入 | 学习状态和数据库分层，AppContainer 显式注入 Repository | README 的模块表可定位源码 |
| 外部 API 与 Room | 从 MediaWiki 获取主题简介，缓存文章；保存草稿、设置和成绩 | 联网打开 Further reading，断网刷新显示缓存；重开 App 查看题目草稿和记录 |
| CP5307 的后台任务与权限 | 自愿开启复习提醒，WorkManager 每日检查尚未通过的尝试 | 用 Android 13+ 设备演示通知授权或拒绝；Background Task Inspector 中查看任务 |
| 非 GUI 和 GUI 测试 | JVM 判题和课程用例；Compose、数据库、解析测试 | 使用上面的两条 Gradle 命令 |
| GitHub、反思和 AI 声明 | 源码中附英文 README、工作流，个人材料须反映实际经历 | 自己持续 commit，分享仓库，并按下方说明提交 |

## 最终提交

在自己的 GitHub 仓库中跟踪真实的开发与修改过程，授予 lecturer 和 subject coordinator 访问权限。最终通过 Android Studio 导出项目 ZIP，并在 LearnJCU 提交 ZIP、GitHub 链接、个人反思 PDF 及课程要求的 AI 使用声明。

反思原文要求两页、约 1,000 个英文词，按 Gibbs 的 Description、Feelings、Evaluation、Analysis、Conclusion、Action Plan 六步写。可以选一次真实的技术选择来贯穿，例如你在 Assessment 2 中研究了哪个 ACS 原则、为什么让学生主动开启通知、拒绝后的行为是否符合预期、测试后改了什么。本项目的源码、练习材料、测试和说明使用了生成式 AI；请按实际保留和修改情况如实声明。
