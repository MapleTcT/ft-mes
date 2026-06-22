# 最新基础模块准入预检报告

本报告记录 `/Users/zhangchu/Documents/MES包/最新基础模块` 的真实准入预检结果。机器可读账本见
`metadata/module-intake-latest-basic-modules.json`，校验命令：

```bash
make module-intake-candidate-report-check
```

原始扫描命令：

```bash
python3 scripts/precheck-module-intake.py "/Users/zhangchu/Documents/MES包/最新基础模块" \
  --report /tmp/adp-intake-latest-basic-modules.json \
  --report-only
```

该扫描是只读的，不解包到仓库，也不把运行包二进制提升为源码模块。`--report-only` 允许记录阻断项并继续输出报告；它不表示这些包已经可以进入 `backend/source-modules`。

## 当前结论

| 项目 | 结果 |
| --- | --- |
| 状态 | `BLOCKED_FOR_SOURCE_PROMOTION` |
| 候选文件 | 13996 |
| POM | 68 |
| Java 文件 | 5084 |
| SQL 文件 | 49 |
| Mapper/SQL 文件 | 250 |
| 二进制/压缩包告警 | 27 |
| 阻断项 | 3 |
| 告警项 | 303 |

这些包可以继续作为业务包考古、页面/API/表映射和 PostgreSQL 迁移分析证据，但在阻断项处理前，不能直接复制到默认可编译源码区。

## 扫描覆盖

| 项目 | 结果 |
| --- | --- |
| 文本候选 / 已读取 | 9806 / 9784 |
| 超过 2MB 未读取文本 | 22 |
| 嵌套文件数 | 13990 |
| 最大嵌套深度 | 2 |
| zip/jar/war/ear 数 | 26 |
| 不可检查压缩包 | 1 |
| 嵌套路径保留 | true |

本次扫描会递归检查 zip/jar/war/ear，最大嵌套深度为 2，单个文本文件读取上限为 2MB，单个嵌套压缩包递归上限为 64MB。`textCoverageComplete=false` 的原因是 22 个文本候选超过单文件读取上限；`hasUnsupportedArchives=true` 的原因是仍存在一个 7z 包。后续要把这些包提升为源码模块前，必须先处理不可检查压缩包，并对超大文本文件补单独审计或拆分扫描证据。

## 阻断项

| 类型 | 文件 | 说明 | 处理要求 |
| --- | --- | --- | --- |
| 不可检查压缩包 | `XTYsupPlant-WOM V6.1.3.4-220722-C(α版).7z` | 7z 不能被当前准入工具安全递归检查 | 重新打包为 zip/jar/war/ear，或提供单独签字的准入报告后再考虑提升 |
| Oracle 默认连接 | `supPlant-Base V6.1.2.5-220222-C.zip!supPlant-Base V6.1.2.5-220222-C/DataSet_6.1.2.2.zip!service/src/main/custom/com/supcon/orchid/DataSet/util/DbUtils.java:27` | 存在 `jdbc:oracle:thin` URL 拼接 | 提升 DataSet 前必须改为 PostgreSQL-first 数据源处理，或隔离到显式 legacy 迁移路径 |
| Oracle 默认连接 | `supPlant-Base V6.1.2.5-220222-C.zip!supPlant-Base V6.1.2.5-220222-C/DataSet_6.1.2.2.zip!service/src/main/custom/com/supcon/orchid/DataSet/util/DbUtils.java:30` | 存在 `jdbc:oracle:thin` URL 拼接 | 同上，不能进入默认 source module 路径 |

## 主要告警

| Pattern | 数量 | 说明 |
| --- | ---: | --- |
| `from-dual` | 146 | Oracle 风格 `dual` 语句，需要改写为 PostgreSQL 兼容 SQL 或保留为 legacy 模板 |
| `sysdate` | 91 | 日期函数需要确认 PostgreSQL 语义 |
| `zip` | 26 | 压缩/运行包不能直接复制进源码模块 |
| `rownum` | 20 | Oracle 分页或行号语义需要改写 |
| `mysql-only` | 13 | MySQL 特有函数或反引号语法需要处理 |
| `decode` | 6 | 需要区分 Java decode 误报和 Oracle `decode(...)` 语义风险 |
| `7z` | 1 | 同阻断项，不可检查压缩包 |

## 后续接入规则

- 先解决 3 个阻断项，再考虑把子模块提升到 `backend/source-modules`。
- 所有 SQL 方言告警必须进入 PostgreSQL 迁移脚本、模块 backlog 或 legacy-template-only 说明。
- 不允许把 `jdbc:oracle:thin`、Oracle driver、Oracle dialect 或 `mapper/oracle` 带入默认源码路径。
- 运行包 zip/7z/jar 只能作为外部证据或部署来源，不能直接提交到可编译源码区。
- 如果这些包用于补齐 `material-service`、`ProcessAnalysis` 或生产导出缺口，需要先重新运行 `make module-intake-check INTAKE=/path/to/package-or-dir`，再补真实前端/API/PostgreSQL marker 验收。
