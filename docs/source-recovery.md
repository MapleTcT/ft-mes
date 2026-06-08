# 源码恢复流程

## 前端

```bash
node scripts/recover-frontend-sourcemaps.mjs
```

输入：

- `../bap-server/bap-workspace/bap-static/**/*.map`

输出：

- `frontend/apps/<app>/`
- `metadata/frontend-sourcemap-summary.json`
- `metadata/frontend-recovered-files.json`

## 后端 sources.jar

```bash
python3 scripts/extract-backend-sources.py
```

输入：

- `../bap-server/assembly/repository/maven/**/*-sources.jar`

输出：

- `backend/modules/`
- `metadata/backend-source-summary.json`

## 后端服务清单

```bash
python3 scripts/generate-service-manifest.py
```

输入：

- `../bap-server/base-Server/*/*.jar`
- `../bap-server/config/configgroup/*.properties`

输出：

- `backend/services/<service>/README.md`
- `metadata/backend-service-manifest.json`

## 服务类反编译

先准备 CFR：

```bash
mkdir -p metadata/tools
curl -L --fail -o metadata/tools/cfr-0.152.jar \
  https://repo1.maven.org/maven2/org/benf/cfr/0.152/cfr-0.152.jar
```

执行：

```bash
python3 scripts/decompile-service-classes.py
```

输出：

- `backend/decompiled-services/<service>/src/main/java`
- `backend/decompiled-services/<service>/src/main/resources`
- `metadata/backend-decompile-summary.json`

`metadata/tools/` 已被 `.gitignore` 排除。
