# WOM 工具栏动作覆盖账本

本文件和 `metadata/wom-toolbar-action-coverage.json` 配套，用来回答制造指令单列表工具栏“看得到、点得动、是否真实落库”的边界问题。它只汇总已有证据，不把按钮可见性冒充业务验收。

可复验入口：

```bash
make wom-toolbar-action-coverage-check
```

## 汇总

| Field | Value |
| --- | --- |
| 路由 | `/msService/WOM/produceTask/produceTask/makeTaskList` |
| 数据库目标 | `PostgreSQL` |
| 工具栏动作数 | `8` |
| 已通过业务验收 | `6` |
| 外部/运行包阻断 | `2` |
| 尚未验收 | `0` |
| 真实普通点击证据 | `3` |
| 前端页面上下文触发证据 | `3` |

最新真实点击复验：2026-06-21 17:49 左右使用公网业务入口
`http://222.88.185.146:18080` 打开同一测试环境页面。证据文件：
`/tmp/adp-wom-toolbar-clicks-20260621094954/evidence.json`。
本次复验确认 `layoutJson` 的 8 个按钮均为
`isPublished=true`、`isconfirm=false(boolean)`、`isSignatureConfig=false`；
页面运行时 `ReactAPI.international.getText('WOM.custom.randon1575958246066')`
返回中文，`开始/重启/提前放料/请检` 的负向业务提示不再显示
`WOM.custom...` key。

2026-06-21 18:19 在远端 Nginx 应用 WOM 页面和 i18n `no-store` 缓存头后，
通过 Make 目标复跑 `start -> hold -> restart`。证据文件：
`/tmp/adp-wom-hold-restart-make-target-browser-base.json`；截图：
`/tmp/adp-wom-start-hold-restart-persistence-20260621101926/wom-start-hold-restart-after.png`。
本轮通过真实 `.sup-datagrid-row` 行点击选中 marker
`ADP_E2E_20260621101926_WOMSTART_HOLD_RESTART`，再用普通鼠标 click
触发 `开始/保持/重启` 三个按钮；三次 `updateTaskState` 均返回
`HTTP 200/dealSuccessFlag=true`，PostgreSQL 回查最终
`wom_produce_tasks.task_run_state=WOM_runState/runing/version=3`。

2026-06-21 18:46 追加整排普通点击复验。证据文件：
`/tmp/adp-wom-toolbar-row-clicks-202606211843.json`；截图：
`/tmp/adp-wom-toolbar-row-clicks-202606211843.png`。本轮选中同一
执行中 marker `9000005371668587` 后依次点击查询、仅查待办、清空、
开始、保持、重启、结束、提前放料、请检、生产过程追溯、生成二维码。
页面未再出现 `WOM.custom...` key；开始、提前放料、请检均显示中文业务
提示；保持/重启均 `HTTP 200`，复验 SQL 后最终
`task_run_state=WOM_runState/runing/version=7`。

2026-06-21 19:50 针对工具栏外部依赖按钮追加前端兜底：
`deploy/docker/assets/module-static/WOM/produceTask/produceTask/makeTaskList/body.js`
和 `body-es5.js` 通过 `deploy/docker/nginx/adp.conf` 精确映射到测试机。
当前 `生产过程追溯`、`生成二维码` 在依赖服务缺失时会显示中文提示，不再
打开空白弹窗或无反馈。证据截图：`/tmp/adp-wom-toolbar-guard-current.png`。
这只是交互兜底，不改变两个动作的业务验收状态。

2026-06-21 20:31 追加连续点击状态同步修复。此前复验发现 `开始`
接口和 PostgreSQL 落库已成功，但页面下一次读取选中行时仍可能拿到旧的
`waitForRun` 状态，导致马上点击 `保持/重启` 时只弹业务校验提示而不发
`updateTaskState`。当前 `body.js/body-es5.js` 会在
`updateTaskState` 成功后记住后端返回的 `exeState`，并在下一次工具栏点击前
修正当前选中行状态，同时延迟刷新表格。证据文件：
`/tmp/adp-wom-hold-restart-after-state-sync.json`。marker
`ADP_E2E_20260621123054_WOMSTART_HOLD_RESTART` 已通过真实普通点击完成
`开始 -> 保持 -> 重启`，三次接口均 `HTTP 200/dealSuccessFlag=true`；
PostgreSQL 回查最终 `wom_produce_tasks.task_run_state=WOM_runState/runing`。

2026-06-21 20:34 重新做整排工具栏点击复验。证据文件：
`/tmp/adp-wom-toolbar-row-after-state-sync.json`；截图：
`/tmp/adp-wom-toolbar-row-after-state-sync.png`。本轮确认 `查询` 和
`仅查待办` 均 `HTTP 200`，`清空` 可点击且无 console/page error；
页面运行时 `ReactAPI.international.getText('WOM.custom.randon1575958246066')`
返回中文，表头 `ec.common.tableNo` 已显示为 `单据编号`。选中 marker
`9000005450549258` 后，`开始` 显示中文负向提示，`保持/重启` 均
`HTTP 200` 并显示“指令单已保持！”、“指令单已重启！”。`生产过程追溯`
和 `生成二维码` 继续显示中文兜底提示，后端阻断仍分别是 ProcessAnalysis
`503` 和 WOM `printManage/generateCode` `404`。

2026-06-21 20:56 将整排工具栏普通点击复验固化为可复跑 Make 目标：
`make smoke-wom-toolbar-row`。该目标先复用
`acceptance-wom-hold-restart-persistence` 种出独立 marker，再打开真实
`makeTaskList` 页面点击查询、仅查待办、清空、开始、保持、重启、生产过程
追溯和生成二维码，并查 PostgreSQL。最新证据：
`metadata/wom-toolbar-row-smoke.json`；种子落库证据：
`/tmp/adp-wom-toolbar-row-smoke-seed.json`；截图：
`/tmp/adp-wom-toolbar-row-smoke.png`。marker
`ADP_E2E_20260621125514_WOMSTART_HOLD_RESTART` 通过复验：`查询/仅查待办`
均 `HTTP 200`，`清空` 可点击，`WOM.custom.randon1575958246066` 与
`ec.common.tableNo` 均已翻译，`保持/重启` 均 `HTTP 200`，PostgreSQL 回读
`wom_produce_tasks.id=9000005465147155` 最终
`task_run_state=WOM_runState/runing/version=5`。`生产过程追溯` 继续捕获
ProcessAnalysis `503`，`生成二维码` 继续捕获 WOM `printManage/generateCode`
`404`，二者均显示中文兜底提示，业务验收状态仍是 `BLOCKED`。本轮也修正了
验收脚本中的行选择抖动：老前端 DOM 行点击偶发反选时，脚本会记录真实点击
并恢复同一 marker 行选择，避免把“未选中行”误判为业务按钮失败。

2026-06-21 21:36 追加刷新后选中行恢复修复。复验发现 `开始/保持/重启`
等状态动作会触发列表刷新，老前端刷新后可能清掉当前选中行，导致后续点击
`生产过程追溯/生成二维码` 时误报“请先选择一条指令单！”。当前
`body.js/body-es5.js` 会在 `updateTaskState` 成功后的短窗口内记住最近
任务 ID，并在列表刷新后恢复同一行选中。证据：
`metadata/wom-toolbar-row-smoke.json`；截图：
`/tmp/adp-wom-toolbar-row-smoke-after-selection-fix.png`。marker
`ADP_E2E_20260621131208_WOMSTART_HOLD_RESTART` 通过复验：页面无
`WOM.custom...`/`ec.common.tableNo` 泄漏，`开始` 显示中文业务提示，
`保持/重启` 均 `HTTP 200`，PostgreSQL 回读
`wom_produce_tasks.id=9000005475286863` 最终
`task_run_state=WOM_runState/runing/version=5`。同一轮点击
`生产过程追溯` 已能保持选中并捕获 ProcessAnalysis `503`，点击
`生成二维码` 捕获 WOM `printManage/generateCode` `404`，两者均显示中文兜底。

2026-06-21 22:29 追加外出办公地址复验：直接用
`http://100.99.133.43:18080` 打开本页面时，浏览器在 180 秒内无法等到
`SupDataGrid` 初始化，核心大包 `vendors.echarts.js`、`vendors.antdicons.js`、
`vendors.sesgis.js`、`vendors.commons.js` 均出现
`net::ERR_CONTENT_LENGTH_MISMATCH`。测试机本机访问 `127.0.0.1:18080`
同一批 gzip 静态文件 0.01 秒完整；公网入口 `222.88.185.146:18080`
下载同一批 gzip 文件 0-1 秒完整；Tailscale 诊断显示本机到
`100.99.133.43` 走 DERP relay `lax`，未建立直连。因此该现象记录为
`100.99` 浏览器入口链路阻断，不归因为 WOM 工具栏业务代码失败。机器记录：
`metadata/test-environment-static-bundle-link-smoke.json`。

2026-06-21 22:50 使用同一测试环境公网入口重新执行
`make smoke-wom-toolbar-row`。新 marker
`ADP_E2E_20260621144811_WOMSTART_HOLD_RESTART` 先通过
`start -> hold -> restart`，再完成整排点击复验；页面无 `WOM.custom...`
和 `ec.common.tableNo` 泄漏，`查询/仅查待办` 均 HTTP 200，`清空` 可点击，
`开始` 显示中文业务提示，`保持/重启` 均 HTTP 200。PostgreSQL 回查
`wom_produce_tasks.id=9000005532917120` 最终
`task_run_state=WOM_runState/runing/version=5`，`wom_wait_put_records`
同步为执行中。`生产过程追溯` 仍是 ProcessAnalysis 503，`生成二维码`
仍是 WOM `printManage/generateCode` 404，二者均显示中文兜底。最新证据：
`metadata/wom-toolbar-row-smoke.json`；截图：
`/tmp/adp-wom-toolbar-row-smoke-public-fresh.png`。

2026-06-21 23:20 使用同一测试环境公网入口再次执行
`ADP_BASE_URL=http://100.99.133.43:18080 ADP_BROWSER_BASE_URL=http://222.88.185.146:18080 WOM_TOOLBAR_PAGE_TIMEOUT_MS=240000 make smoke-wom-toolbar-row`。
新 marker `ADP_E2E_20260621151918_WOMSTART_HOLD_RESTART`
先完成 `start -> hold -> restart` 种子落库，再打开真实 `makeTaskList`
页面完成整排点击复验。`查询/仅查待办` 均 `HTTP 200`，`清空` 可点击，
`开始` 显示中文业务提示，`保持/重启` 均 `HTTP 200`；PostgreSQL 回查
`wom_produce_tasks.id=9000005551587839`
最终 `task_run_state=WOM_runState/runing/version=5`，
`wom_wait_put_records.proc_report_id=757677987427584`。`生产过程追溯`
仍是 ProcessAnalysis `503`，`生成二维码` 仍是 WOM
`printManage/generateCode` `404`。最新证据：
`metadata/wom-toolbar-row-smoke.json`；截图：
`/tmp/adp-wom-toolbar-row-smoke.png`。

2026-06-21 23:54 将 `make smoke-wom-toolbar-row` 补强为完整首行交互复验。
新 marker `ADP_E2E_20260621155239_WOMSTART_HOLD_RESTART` 先完成
`start -> hold -> restart` 种子落库，再打开真实 `makeTaskList` 页面检查
左侧下拉、查询、仅查待办、清空、开始、保持、重启、结束、提前放料、请检、
生产过程追溯和生成二维码。左侧下拉可展开，但当前数据源返回 `暂无数据`；
`查询/仅查待办` 均 `HTTP 200`，`清空` 可点击。页面无 `WOM.custom...`
和 `ec.common.tableNo` 泄漏；`开始/提前放料/请检` 均显示中文业务提示；
`保持/重启` 均 `HTTP 200`；`结束` 普通点击后
`findProcReportIdByTaskId=200` 且 `outPutCommonTaskEdit=200`。PostgreSQL 回查
`wom_produce_tasks.id=9000005571592916`
最终 `task_run_state=WOM_runState/runing/version=5`，
`wom_wait_put_records.proc_report_id=757686174524672`。`生产过程追溯`
仍是 ProcessAnalysis `503`，`生成二维码` 仍是 WOM
`printManage/generateCode` `404`，二者显示中文兜底且无 request/page error。

2026-06-22 00:12 针对“这一排交互都有问题”的反馈再次执行
`ADP_BASE_URL=http://100.99.133.43:18080 ADP_BROWSER_BASE_URL=http://222.88.185.146:18080 WOM_TOOLBAR_PAGE_TIMEOUT_MS=240000 make smoke-wom-toolbar-row`。
新 marker `ADP_E2E_20260621160953_WOMSTART_HOLD_RESTART` 先完成
`start -> hold -> restart` 种子落库，再打开真实 `makeTaskList` 页面点击
左侧下拉、查询、仅查待办、清空、开始、保持、重启、结束、提前放料、请检、
生产过程追溯和生成二维码。复验结果为 `PASS_WITH_KNOWN_BLOCKERS`：
左侧下拉可展开并显示 `暂无数据`；`查询/仅查待办` 均 `HTTP 200`；
`清空` 可点击；`开始/提前放料/请检` 均显示中文业务提示，没有
`WOM.custom...` 或 `ec.common.tableNo` 泄漏；`保持/重启` 均 `HTTP 200`；
`结束` 打开真实“指令单完工报工”入口，`findProcReportIdByTaskId=200` 且
`outPutCommonTaskEdit=200`。PostgreSQL 回查
`wom_produce_tasks.id=9000005581932827`
最终 `task_run_state=WOM_runState/runing/version=5`，
`wom_wait_put_records.proc_report_id=757690414802176`。

2026-06-22 01:51 针对同一排按钮继续修复“依赖缺失按钮仍发起坏请求”的问题。
这次发现远端 Nginx 对 `makeTaskList/body-es5.js` 仍命中了旧 `.gz` 静态文件，
导致浏览器拿到的不是最新守卫脚本；已在 `deploy/docker/nginx/adp.conf` 对
`body.js/body-es5.js` 精确关闭 `gzip_static`，并重新加载 Nginx。复验文件：
`/tmp/adp-wom-toolbar-guard-final2.json`；截图：
`/tmp/adp-wom-toolbar-guard-final2.png`。当前点击 `生产过程追溯` 会直接显示
`生产过程追溯服务未部署或暂不可用！`，不会再发起 `ProcessAnalysis` 请求；
点击 `生成二维码` 会直接显示 `二维码生成页面未部署或暂不可用！`，不会再发起
`/WOM/printManage` 请求。两者业务验收仍是 `BLOCKED`：前者缺
ProcessAnalysis 服务、运行元数据、菜单和 schema；后者的
`metadata/wom-qrcode-route-probe.json` 仍证明 WOM `printManage` 端点和运行包实现缺失。

2026-06-22 03:20 针对截图中 `WOM.custom.randon...` 提示继续复验，定位到
老页面加载慢导致 `ReactAPI` 晚于原 20 秒补丁窗口初始化。当前
`i18n-value.js` 改为持续安装 fallback，`ReactAPI` 晚加载或被替换也会补上
`getText/showMessage/confirm`。专项浏览器探针证据：
`/tmp/adp-wom-toolbar-focused-probe.json`；截图：
`/tmp/adp-wom-toolbar-focused-probe.png`。复验确认
`ReactAPI.international.getText('WOM.custom.randon1575958246066')`
返回 `只有【待执行】的指令单可以开始！`，`getText('ec.common.tableNo')`
返回 `单据编号`，页面 body 没有 raw i18n key；真实点击 `开始` 显示
`指令单已开始！`，并通过 PostgreSQL 回读
`wom_produce_tasks.id=9000005688468704`、
`wom_wait_put_records.exe_state=WOM_runState/runing` 和
`proc_report_id=757737548141824`。同一轮点击 `生产过程追溯` 和 `生成二维码`
仍只显示中文依赖提示，不再发起缺失 `ProcessAnalysis` 或 `/WOM/printManage`
请求。

2026-06-22 04:32 为消除静态链路账本漂移，重新执行
`ADP_BASE_URL=http://100.99.133.43:18080 ADP_BROWSER_BASE_URL=http://222.88.185.146:18080 WOM_TOOLBAR_PAGE_TIMEOUT_MS=240000 make smoke-wom-toolbar-row`。
新 marker `ADP_E2E_20260621203121_WOMSTART_HOLD_RESTART` 完成
`start -> hold -> restart` 种子落库，再打开真实 `makeTaskList` 页面完成
整排点击复验。`metadata/wom-toolbar-row-smoke.json` 当前记录
`PASS_WITH_KNOWN_BLOCKERS`，查询/仅查待办均 `HTTP 200`，清空可点击，
开始/提前放料/请检均显示中文业务提示且无 raw i18n key，保持/重启均
`HTTP 200`，结束打开真实“指令单完工报工”入口。PostgreSQL 回查
`wom_produce_tasks.id=9000005738810447` 最终
`task_run_state=WOM_runState/runing/version=5`，
`wom_wait_put_records.proc_report_id=757754647041280`。同一报告也把
`生产过程追溯` 和 `生成二维码` 的依赖缺失记录为 `observed=true`、
`guardedWithoutRequest=true`、`dependencyStatus=BLOCKED`，继续与
`metadata/test-environment-static-bundle-link-smoke.json` 的 DERP/
`net::ERR_CONTENT_LENGTH_MISMATCH` 链路证据对齐。

2026-06-22 05:32 针对用户截图中“开始”等按钮仍偶发显示
`WOM.custom.randon...` 的问题，进一步把消息翻译兜底复制到每次带随机参数加载的
`body.js/body-es5.js`，包住 `ReactAPI.international.getText`、
`ReactAPI.showMessage` 和确认框。测试机挂载源
`/home/v6/adp-mes-docker-newbase-20260611-181921/deploy/docker/assets/module-static`
已同步并 reload Nginx；宿主机文件 SHA256 为
`8937c407d4652c80fc7c5666ffb8a980a9e2e04b5862022a50e4dfbf4b42ea1c`。
复验证据 `/tmp/adp-wom-toolbar-row-smoke-after-body-fallback.json`、
截图 `/tmp/adp-wom-toolbar-row-smoke-after-body-fallback.png`：
`hasRawWomCustom=false`、`hasTableNoKey=false`，真实点击 `开始` 显示
`只有【待执行】的指令单可以开始！`；`保持/重启` 均为普通点击触发
`updateTaskState HTTP 200/dealSuccessFlag=true`，PostgreSQL 回查
`wom_produce_tasks.id=9000005738810447` 最终
`task_run_state=WOM_runState/runing/version=9`。

2026-06-22 06:02 针对“这一排交互都有问题”继续做可复跑真实浏览器复验。
证据已同步到 `metadata/wom-toolbar-row-smoke.json`；截图：
`/tmp/adp-wom-toolbar-row-smoke-report-work-render.png`，完工报工页面截图：
`/tmp/adp-wom-toolbar-report-work-render.png`。本轮直接访问
`http://100.99.133.43:18080`，打开真实 `makeTaskList` 页面后依次复验
左侧下拉、查询、仅查待办、清空、开始、保持、重启、结束、提前放料、请检、
生产过程追溯、生成二维码。结果为 `PASS_WITH_KNOWN_BLOCKERS`：页面无
`WOM.custom...` 或 `ec.common.tableNo` 泄漏；左侧下拉可展开但数据源返回
`暂无数据`；`查询/仅查待办` 均为 `HTTP 200`；`清空` 可点击；`开始`
显示中文业务提示；`保持` 返回 `WOM_runState/iskeep`，`重启` 返回
`WOM_runState/runing`。`结束` 不只验证 HTTP 200，还重新打开
`outPutCommonTaskEdit` 页面并确认可见字段包含 `生产批号`、`产品编码`、
`产品名称`、`计划数量` 和批次
`ADP_E2E_20260621203121_WOMSTART_HOLD_RESTART_BATCH`。PostgreSQL 回查
`wom_produce_tasks.id=9000005738810447` 最终
`task_run_state=WOM_runState/runing/version=17`，
`wom_wait_put_records.proc_report_id=757754647041280` 且状态同步为执行中。
本轮也固化了脚本的重跑前置状态处理：如果上一次中断把 marker 留在保持状态，
脚本会先用正常页面“重启”按钮恢复到执行中，再正式验收保持/重启。

2026-06-22 06:27 继续针对当前截图中的整排控件复跑真实浏览器验收。
本轮 marker 为 `ADP_E2E_20260621222639_WOMSTART_HOLD_RESTART`，
taskId 为 `9000005807992985`，直接访问
`http://100.99.133.43:18080`。结果仍为 `PASS_WITH_KNOWN_BLOCKERS`：
左侧下拉可展开并显示 `暂无数据`；`查询/仅查待办` 均为 `HTTP 200`；
`清空` 可点击；`开始/提前放料/请检` 均显示中文业务提示且无
`WOM.custom...` 或 `ec.common.tableNo` 泄漏；`保持` 返回
`WOM_runState/iskeep`，`重启` 返回 `WOM_runState/runing`；`结束` 打开真实
`指令单完工报工` 页面并确认 `生产批号/产品编码/产品名称/计划数量` 以及
本轮批次可见。PostgreSQL 回查 `wom_produce_tasks.id=9000005807992985`
最终为 `task_run_state=WOM_runState/runing/version=5`，
`wom_wait_put_records.proc_report_id=757783033844992`。
`生产过程追溯/生成二维码` 当前仍是依赖缺失 blocker，但已由前端守卫显示
中文兜底提示，不再发起缺失服务请求。

2026-06-22 06:45 继续针对截图里 `WOM.custom.randon...` 提示做兜底修复。
本轮在 `i18n-value.js`、`body.js` 和 `body-es5.js` 增加 DOM 级可见文本
fallback：即使消息组件绕过 `ReactAPI.showMessage`，新出现的
`WOM.custom.*`、`ec.common.tableNo` 等 key 也会被替换成中文。已同步到
测试机挂载目录并 reload Nginx。复验命令仍为
`ADP_BASE_URL=http://100.99.133.43:18080 ADP_BROWSER_BASE_URL=http://222.88.185.146:18080 WOM_TOOLBAR_PAGE_TIMEOUT_MS=240000 make smoke-wom-toolbar-row`。
本轮 marker 为 `ADP_E2E_20260621224453_WOMSTART_HOLD_RESTART`，
taskId 为 `9000005818934762`；`metadata/wom-toolbar-row-smoke.json`
记录 `PASS_WITH_KNOWN_BLOCKERS`。左侧下拉可展开并显示 `暂无数据`；
`查询/仅查待办` 均为 `HTTP 200`；`清空` 可点击；`开始/提前放料/请检`
均显示中文业务提示，页面 body 无 `WOM.custom...` 或 `ec.common.tableNo`
残留；`保持` 返回 `WOM_runState/iskeep`，`重启` 返回
`WOM_runState/runing`；`结束` 打开真实 `指令单完工报工` 页面。PostgreSQL
回查 `wom_produce_tasks.id=9000005818934762` 最终为
`task_run_state=WOM_runState/runing/version=5`，
`wom_wait_put_records.proc_report_id=757787465209088`。`生产过程追溯` 和
`生成二维码` 仍是业务包/服务缺失 blocker，但现在只显示中文依赖提示，
不再发起缺失服务请求。

2026-06-22 07:17 针对用户最新截图里右上角 toast 仍显示
`WOM.custom.randon1575958246066`，把 `i18n-value.js` 的翻译兜底从当前
iframe 扩到可访问的 `parent/top` 窗口，并把 WOM 工具栏相关
`supfusion_i18n_resource` 同步补齐到 `zh_CN/zh_HK`。测试机已覆盖
`makeTaskList/i18n-value.js`、执行
`168-wom-maketasklist-toolbar-interaction-compat.sql` 并 reload Nginx /
restart `baseService`。HTTP 复验确认公网与 Tailscale 静态文件均包含
`accessibleWindows`、`patchWindow` 和 `WOM.custom.randon1575958246066`
翻译表。
随后重新执行 `make smoke-wom-toolbar-row`，本轮 marker 为
`ADP_E2E_20260621231718_WOMSTART_HOLD_RESTART`，taskId 为
`9000005838388864`；`metadata/wom-toolbar-row-smoke.json` 记录
`generatedAt=2026-06-22T01:02:18.756Z`、`PASS_WITH_KNOWN_BLOCKERS` 且
`failures=[]`。真实浏览器复验结果：左侧下拉可展开并显示
`暂无数据`；`查询/仅查待办` 均为 `HTTP 200`；`清空` 可点击；
`开始` 的右上角提示显示中文 `只有【待执行】的指令单可以开始！`，没有 raw key；
`保持` 返回 `WOM_runState/iskeep`，`重启` 返回 `WOM_runState/runing`；
`提前放料` 显示 `该批次不能提前放料！`，`请检` 显示
`该指令单产品无需质检！`；`结束` 打开真实 `指令单完工报工` 页面并确认
`生产批号/产品编码/产品名称/计划数量` 和本轮批次可见。PostgreSQL 回查
`wom_produce_tasks.id=9000005838388864` 最终为
`task_run_state=WOM_runState/runing/version=9`，
`wom_wait_put_records.proc_report_id=757795467318528`。`生产过程追溯` 和
`生成二维码` 仍是业务包/服务缺失 blocker，但已由前端守卫显示中文依赖提示，
且 `guardedWithoutRequest=true`，没有再发起缺失服务请求。

2026-06-22 09:27 复跑 `make probe-wom-qrcode-route`。结果仍为
`BLOCKED`：`GET /msService/WOM/printManage/printDate/generateCode`、
`POST /msService/WOM/printManage/generateQrCode` 和
`POST /msService/WOM/printManage/backfill-printInfo` 均返回
`HTTP 404/code 404`；远端 WOMMs service jar 对
`printManage/generateQrCode/backfill-printInfo/QrCode` 的类名和字符串扫描均为 0；
PostgreSQL 中 `baseset_printers`、`baseset_qr_code_types`、
`baseset_qr_detail_infos` 仍为 0 行，`printer_register` 为 8 行。结论不变：
当前缺 WOM `printManage` 二维码运行包接口实现，不是新的 PostgreSQL 兼容缺口。

2026-06-22 12:38 针对当前外出办公入口再次复验。先用
`ADP_BROWSER_BASE_URL=http://100.99.133.43:18080` 跑
`make smoke-wom-toolbar-row`，种子页面阻断在大静态包链路：`vendors.sesgis.js`
和 `vendors.antd.js` 出现 `net::ERR_CONTENT_LENGTH_MISMATCH`，同机
`tailscale ping` 显示 `DERP(lax)` 且 `direct connection not established`；
手工 `curl` 复核 `100.99` 上 `vendors.sesgis.js.gz`、`vendors.antd.js.gz`、
`vendors.commons.js.gz` 均在 25 秒内超时或只收到部分字节，而同一环境公网入口
`http://222.88.185.146:18080` 能完整下载三个 gzip 包。因此浏览器验收切回
公网入口，API/DB/SSH 仍走 `100.99.133.43`。复跑命令：
`ADP_BASE_URL=http://100.99.133.43:18080 ADP_BROWSER_BASE_URL=http://222.88.185.146:18080 WOM_TOOLBAR_PAGE_TIMEOUT_MS=240000 make smoke-wom-toolbar-row`。
本轮 marker `ADP_E2E_20260622043703_WOMSTART_HOLD_RESTART` / taskId
`9000006030233192` 完成 `start -> hold -> restart` 种子落库，再打开真实
`makeTaskList` 页面完成左侧下拉、查询、仅查待办、清空、开始、保持、重启、
结束、提前放料、请检、生产过程追溯和生成二维码整排点击。结果为
`PASS_WITH_KNOWN_BLOCKERS`：页面无 `WOM.custom...` 或 `ec.common.tableNo`
泄漏，查询/仅查待办均 `HTTP 200`，清空可点击，开始/提前放料/请检显示中文
业务提示，保持/重启均 `HTTP 200`，结束打开真实完工报工入口。PostgreSQL 回查
`wom_produce_tasks.id=9000006030233192` 最终
`task_run_state=WOM_runState/runing/version=5`，
`wom_wait_put_records.proc_report_id=757874014926080`。`生产过程追溯` 和
`生成二维码` 仍记录为 `guardedWithoutRequest=true`、`dependencyStatus=BLOCKED`，
对应缺 `ProcessAnalysis` 服务和 WOM `printManage` 二维码实现。证据：
`metadata/wom-toolbar-row-smoke.json`、
`metadata/test-environment-static-bundle-link-smoke.json`。
同轮补查确认左侧下拉的 `暂无数据` 不是点击事件失效：当前
`runtime_data_classific/ec_data_classific` 均只有 1 条 EAM 数据分类记录，
WOM `makeTaskList` 的 `runtime_extra_view.view_json` 中没有
`dataclassify/dataGroupProperty` 配置，因此该控件现阶段只能作为空分类过滤器
展开，不能算完整分类配置已恢复。

2026-06-22 16:32 针对同一截图中的整排控件再次重跑现场 smoke，API/DB/SSH
仍走 `http://100.99.133.43:18080`，浏览器仍走同环境公网入口
`http://222.88.185.146:18080`。本轮 marker
`ADP_E2E_20260622083104_WOMSTART_HOLD_RESTART` / taskId `9000006170646946`
真实点击左侧下拉、查询、仅查待办、清空、开始、保持、重启、结束、提前放料、
请检、生产过程追溯和生成二维码；`metadata/wom-toolbar-row-smoke.json`
记录 `generatedAt=2026-06-22T08:32:01.573Z`、`PASS_WITH_KNOWN_BLOCKERS`、
`failures=[]`。页面无 `WOM.custom...` 或 `ec.common.tableNo` 泄漏；`查询/仅查待办`
均 `HTTP 200`；`开始/提前放料/请检` 均显示中文业务提示；`保持` 返回
`WOM_runState/iskeep`，`重启` 返回 `WOM_runState/runing`；`结束` 打开真实
`指令单完工报工` 入口。PostgreSQL 回查 `wom_produce_tasks.id=9000006170646946`
最终为 `task_run_state=WOM_runState/runing/version=5`，
`wom_wait_put_records.proc_report_id=757931527595264`。`生产过程追溯` 与
`生成二维码` 仍是业务包/服务缺失 blocker，但本轮继续证明它们只显示中文兜底提示，
不再发起缺失服务请求。

2026-06-22 17:50 针对用户截图中的顶部筛选和整排工具栏再次修复复验。修复点：
`makeTaskList/body.js` 与 `body-es5.js` 在 `search_panel_selectField` 无真实选项时
显示 `全部`，并在捕获 `mousedown/click/keydown` 时立即修补和拦截，避免用户点出
空的 `暂无数据` 菜单；`adp-wom-toolbar-row-smoke.js` 同步收紧判定，空下拉必须有
明确兜底，否则失败。本轮先复用
`/tmp/adp-wom-toolbar-row-smoke-seed.json`，再执行：
`ADP_BASE_URL=http://222.88.185.146:18080 ADP_BROWSER_BASE_URL=http://222.88.185.146:18080 ADP_WOM_TOOLBAR_SEED_EVIDENCE=/tmp/adp-wom-toolbar-row-smoke-seed.json ADP_WOM_TOOLBAR_ROW_SMOKE_OUTPUT=metadata/wom-toolbar-row-smoke.json ADP_WOM_TOOLBAR_SCREENSHOT=metadata/wom-toolbar-row-smoke.png node deploy/docker/scripts/adp-wom-toolbar-row-smoke.js`。
结果 `PASS_WITH_KNOWN_BLOCKERS`，marker
`ADP_E2E_20260622094817_WOMSTART_HOLD_RESTART` / taskId `9000006216972059`。
`metadata/wom-toolbar-row-smoke.json` 记录
`filterDropdown.displayText=全部`、`dropdownVisible=false`、
`emptySelectFallbackApplied=true`、`failures=[]`；`开始/提前放料/请检` 均为中文
业务提示，`保持/重启` 返回 `HTTP 200/dealSuccessFlag=true`。PostgreSQL 回读
`wom_produce_tasks.task_run_state=WOM_runState/runing/status=99/version=5`，
`wom_wait_put_records.proc_report_id=757950510773504`。

2026-06-22 18:09 针对截图中“这一排交互都有问题”的无选中路径追加前置守卫：
`makeTaskList/body.js` 与 `body-es5.js` 在整排行操作按钮点击前统一检查
SupDataGrid 选择状态。无选中时，`开始/保持/重启/结束/提前放料/请检/生产过程追溯/生成二维码`
都会先显示 `请先选择一条指令单！`，并阻止后续业务请求，避免继续出现
`WOM.custom.randon...` 或底层不一致提示。测试机静态卷已同步到
`100.99.133.43`，并重新执行：
`ADP_BASE_URL=http://100.99.133.43:18080 ADP_BROWSER_BASE_URL=http://222.88.185.146:18080 ADP_USERNAME=admin ADP_PASSWORD=123456 WOM_TOOLBAR_PAGE_TIMEOUT_MS=240000 make smoke-wom-toolbar-row`。
结果 `PASS_WITH_KNOWN_BLOCKERS`，marker
`ADP_E2E_20260622100835_WOMSTART_HOLD_RESTART` / taskId `9000006229150923`。
`metadata/wom-toolbar-row-smoke.json` 记录 8 个按钮无选中均
`selectedCount=0`、提示 `请先选择一条指令单！`、`raw=false`；选中 marker 后
`查询/仅查待办` 均 `HTTP 200`，`清空` 可点击，`保持/重启` 均
`HTTP 200/dealSuccessFlag=true`，`结束` 打开真实“指令单完工报工”页面。
PostgreSQL 回读 `wom_produce_tasks.id=9000006229150923`
最终 `task_run_state=WOM_runState/runing/status=99/version=5`，
`wom_wait_put_records.proc_report_id=757955491001600`。

## 动作矩阵

| 动作 | API | 验收状态 | 点击证据 | 生产用例 | 落库/后端结论 | 问题 |
| --- | --- | --- | --- | --- | --- | --- |
| 开始 | `POST /msService/WOM/produceTask/produceTask/updateTaskState` | PASS | NORMAL_MOUSE_CLICK | `PROD-003` | `ADP_E2E_20260622094817_WOMSTART_HOLD_RESTART` 已通过可复跑目标验证；17:34 复验在运行中行上点击开始显示中文业务提示，无 raw i18n key。 | 无 |
| 保持 | `POST /msService/WOM/produceTask/produceTask/updateTaskState` | PASS | NORMAL_MOUSE_CLICK | `PROD-024` | 状态同步、选中行恢复和 parent/top 窗口 toast 翻译兜底已覆盖连续点击链路；17:34 复验 `保持` 返回 `WOM_runState/iskeep`。 | 无 |
| 重启 | `POST /msService/WOM/produceTask/produceTask/updateTaskState` | PASS | NORMAL_MOUSE_CLICK | `PROD-024` | 状态同步、选中行恢复和 parent/top 窗口 toast 翻译兜底已覆盖连续点击链路；17:34 复验 `重启` 返回 `WOM_runState/runing`，最终 `version=7`、待办状态为执行中。 | 无 |
| 结束 | `updateTaskState`、`findProcReportIdByTaskId`、`addOutputByOutPutDetails` | PASS | PAGE_CONTEXT_RUNTIME_EVENT | `PROD-025`、`PROD-026` | 最小结束和产出明细报工均已查 PostgreSQL；17:34 普通点击已能打开真实“指令单完工报工”入口，`findProcReportIdByTaskId/outPutCommonTaskEdit` 均为 200，并确认页面可见 `生产批号/产品编码/产品名称/计划数量` 和当前 marker 批次。 | 最终“完成/保存”落库仍以专项 marker 报告为准。 |
| 提前放料 | `GET /msService/WOM/produceTask/produceTask/setAdvanceTrue/{taskId}` | PASS | PAGE_CONTEXT_RUNTIME_EVENT | `PROD-027` | `advance_charge/is_advanced/feed_condition` 已查 PostgreSQL；17:34 普通点击在不允许提前放料的执行中行上显示中文提示“该批次不能提前放料！”，没有 raw key。 | 有效提前放料落库仍以专项 marker 报告为准。 |
| 请检 | `POST /msService/WOM/produceTask/produceTask/createManuInspect` | PASS | PAGE_CONTEXT_RUNTIME_EVENT | `PROD-031`、`PROD-032`、`PROD-035`、`PROD-036` | WOM/QCS 请检、报告、合格/不合格回写和不合格处理单均已有 marker 验证；17:34 普通点击在无需质检产品上显示中文提示“该指令单产品无需质检！”。 | 有效请检创建仍以 WOM/QCS 专项 marker 报告为准。 |
| 生产过程追溯 | `ProcessAnalysis/traceability` 系列接口 | BLOCKED | DEPENDENCY_BLOCKED | `PROD-020` | 17:34 复验已由前端守卫拦截：点击后显示 `生产过程追溯服务未部署或暂不可用！`，不再发起缺失 `ProcessAnalysis` 请求。当前不是 PostgreSQL 小修能闭合，缺 ProcessAnalysis 服务、运行元数据、菜单和 schema。 | 需要补业务包和服务注册；前端兜底不能替代业务验收。 |
| 生成二维码 | `GET /msService/WOM/printManage/printDate/generateCode`；`POST /msService/WOM/printManage/generateQrCode`；`POST /msService/WOM/printManage/backfill-printInfo` | BLOCKED | DEPENDENCY_BLOCKED | 尚未纳入生产矩阵 | 17:34 复验已由前端守卫拦截：点击后显示 `二维码生成页面未部署或暂不可用！`，不再发起缺失 `/WOM/printManage` 请求。09:27 `make probe-wom-qrcode-route` 仍复验三个 WOM `printManage` 端点均为 404，WOM service jar 无匹配实现。 | 当前运行包缺 WOM 二维码生成接口实现；补业务包/控制器后仍需补打印机/包装配置并做 marker 落库和可选打印回填验收。 |

## 不能替代的证据

- `078-wom-list-button-runtime-json.sql` 和 i18n 静态覆盖只能证明按钮可见、文字正常。
- `168-wom-maketasklist-toolbar-interaction-compat.sql` 证明保持/重启等按钮配置修正，但仍需要动作级请求和查库。
- `body.js/body-es5.js` 的状态同步和短窗口选中行恢复兜底只解决连续点击时的前端旧状态/丢选择问题；最终通过仍以 `updateTaskState` 响应和 PostgreSQL 回查为准。
- 生产过程追溯依赖 ProcessAnalysis，不能用 WOM 自身表补 SQL 来冒充通过。
- 生成二维码当前不是简单前端/i18n 问题：`metadata/wom-qrcode-route-probe.json` 证明按钮目标端点在现运行包中 404 且 WOM service jar 无匹配实现。补齐 WOM `printManage` 业务包/控制器后，还必须单独做 `generateQrCode`、可选打印、`backfill-printInfo` 的 marker 验收。
