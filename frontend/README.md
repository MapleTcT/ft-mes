# Frontend

前端源码从 `../bap-server/bap-workspace/bap-static/**/*.map` 的 `sourcesContent` 恢复。

## 已恢复应用

```text
auth                 57 files
bap                  185 files
greenDill            226 files
i18n                 16 files
menu                 21 files
notification         43 files
organization         47 files
print                78 files
signature-static     19 files
supplant             97 files
supplant-test        87 files
systemcode           26 files
systemconfig         48 files
taskscheduler        22 files
theme                19 files
```

源码位置：

```text
frontend/apps/<app>/src/
```

## 注意事项

- 当前是 source map 级恢复，不保证包含原始 `package.json`、锁文件、webpack 配置和构建脚本。
- 已排除大部分 `node_modules`、webpack runtime、DLL reference 和第三方库噪声。
- 个别 `.vue?hash` 形式的 source map 片段会以安全文件名保存，需要后续人工合并成标准 `.vue` 单文件组件。
- 原始静态入口仍在 `../bap-server/bap-workspace/bap-static`，本仓库只保存恢复源码。
