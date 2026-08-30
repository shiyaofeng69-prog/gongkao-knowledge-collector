# 公考知识采集器（Android）

面向个人备考的本地优先 Android 应用，用来收集刷题时遇到的截图和文字，并逐步整理为：

- 历史文化常识
- 成语积累（包含题目全部选项及辨析）
- 申论好词好句

一期不提供账号和云同步，原始内容与结构化数据默认保存在设备本地。

## 当前进度

项目按模块门禁逐步开发：每个模块先实现，再通过对应测试用例，之后才进入下一模块。

- M00 工程地基：完成
- M01 截图/文字输入接入：完成，自动化门禁通过
- M01.5 收集箱与原始内容详情：完成，自动化门禁通过
- M02 OCR 与结构化解析：待开发
- 后续：自动分类、成语辨析与解析补全、知识浏览检索、题目关联、联网题库搜索

M01 已支持：

- 粘贴文字并保留正文内部换行
- 使用系统 Photo Picker 单选 PNG/JPEG 截图
- 接收 Android 系统分享的图片或纯文字
- 将原图复制到应用私有目录，不持续依赖外部 URI
- 提取图片尺寸、字节数、MIME 类型和 SHA-256
- 对长截图计算有界采样倍率
- 检测完全相同的重复来源
- 明确拒绝空白文字、不支持的文件格式和一期暂不支持的多图导入

## 技术结构

- Kotlin
- Jetpack Compose + Material 3
- Room 3 + bundled SQLite driver
- Coroutines
- Gradle 9 / Android Gradle Plugin 9
- `app`、`domain`、`data`、`ai` 四模块结构

## 构建与测试

环境要求：

- JDK 17
- Android SDK API 37.0
- Android Build Tools 36.0.0

在已经配置 `ANDROID_HOME` 的环境中执行：

```powershell
.\gradlew.bat gate01
```

`gate01` 会串联工程边界、数据库、迁移、输入接入、单元测试、Lint、R8 Release 和 Android 测试包构建等检查。

## 隐私与发布状态

当前仓库处于早期开发阶段。生成的调试 APK 使用本地调试签名，仅用于私人安装验收；签名文件、SDK 路径和构建产物不会提交到 Git。
