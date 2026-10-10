# 恢复 1.1.8 中英文自动切换逻辑与资源结构实现方案

根据您的反馈与确认，之前定位中将原因归咎于“缺少 values-zh 导致系统退化为英文”确实是不成立的。在 1.1.8 版本中，Android 采用的是标准的“默认 `values/` 目录为中文，`values-en/` 目录为英文”的架构，Android 资源加载机制会自然回退到默认中文资源。之前在 1.1.9 出现简体中文环境下显示英文的实际原因，在于代码中对 `LocaleList` 的遍历检索或语言判断逻辑出现了偏差，导致系统语言列表中次优级的英文干扰了主语言判断。

我们将完全恢复 1.1.8 的资源目录结构与语言判断策略。

## 用户审核与确认事项

- **资源目录处理**：清理此前生成的 `values-zh`、`values-zh-rCN`、`layout-zh`、`layout-zh-rCN`、`drawable-zh`、`drawable-zh-night` 等多余目录，仅保留基础默认中文资源（`values/` 等）与英文资源（`values-en/` 等）。
- **语言判断规则**：采用 1.1.8 原有规则，严格以系统第一顺位主语言为准；只要第一主语言以 `zh` 开头（无论是 `zh-CN`、`zh-TW`、`zh-HK` 还是其他中文变体），均显示中文；其他非中文主语言均显示英文。

---

## 计划变更内容

### 1. 清理多余资源目录
- 删除 `app/src/main/res/values-zh`
- 删除 `app/src/main/res/values-zh-rCN`
- 删除 `app/src/main/res/layout-zh`
- 删除 `app/src/main/res/layout-zh-rCN`
- 删除 `app/src/main/res/drawable-zh`
- 删除 `app/src/main/res/drawable-zh-night`
- 保留 `values`、`values-en`、`drawable-en` 等 1.1.8 原生资源结构。

### 2. 还原 `TranslationHelper.kt` 判断逻辑
- 修改 `app/src/main/java/com/sunplace/calendar/util/TranslationHelper.kt` 中的 `isEnglish` 方法：
  - 获取当前环境第一位主语言 Locale（优先通过传入的 `Context` 获取当前配置的 `locales.get(0)`，回退使用 `Locale.getDefault()`）。
  - 判断逻辑恢复为标准 1.1.8 规则：
    ```kotlin
    val lang = primaryLocale.language.lowercase()
    val isChinese = lang.startsWith("zh")
    return !isChinese
    ```
  - 彻底移除对繁体字（tw/hk/mo/hant）的额外排除逻辑，以及对完整语言列表的误判循环，使任何中文主语言系统均稳定显示中文，非中文主语言均显示英文。

---

## 验证计划

1. **资源完整性检查**：
   - 验证 `values/strings.xml` 与 `values-en/strings.xml` 的键值对照完整，无缺失键。
2. **逻辑单元测试验证**：
   - 运行 Robolectric 测试，在不同的主语言环境下（`zh-CN`, `zh-TW`, `en-US` 等）验证 `isEnglish()` 的返回值以及界面字符串显示是否符合 1.1.8 表现。
3. **整体编译与打包**：
   - 执行 `compile_applet`，确保工程无冗余文件、编译正常通过。
