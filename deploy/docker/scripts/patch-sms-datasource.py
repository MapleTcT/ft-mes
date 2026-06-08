#!/usr/bin/env python3
from __future__ import annotations

import argparse
import io
import shutil
import subprocess
import tempfile
import zipfile
from pathlib import Path


COMPILED_CLASS = "com/supcon/supfusion/notification/sms/config/DataSourceConfig.class"
JAR_CLASS_ENTRY = f"BOOT-INF/classes/{COMPILED_CLASS}"
SOURCE = r"""
package com.supcon.supfusion.notification.sms.config;

import com.alibaba.druid.pool.DruidDataSource;
import javax.sql.DataSource;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(value = {"com.supcon.supfusion.notification.sms.dao.mappers"})
public class DataSourceConfig {
    private static String setting(String property, String env, String defaultValue) {
        String value = System.getProperty(property);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(env);
        }
        return (value == null || value.trim().isEmpty()) ? defaultValue : value;
    }

    @Bean
    public DataSource getDataSource() {
        String url = setting("sms.datasource.url", "SMS_DB_URL", "jdbc:postgresql://postgres:5432/adp");
        String user = setting("sms.datasource.username", "SMS_DB_USERNAME", "adp");
        String password = setting("sms.datasource.password", "SMS_DB_PASSWORD", "adp");
        String driver = setting("sms.datasource.driver-class-name", "SMS_DB_DRIVER", "org.postgresql.Driver");

        DruidDataSource source = new DruidDataSource();
        source.setUrl(url);
        source.setUsername(user);
        source.setPassword(password);
        source.setDriverClassName(driver);
        source.setValidationQuery("SELECT 1");
        source.setTestWhileIdle(true);
        source.setTestOnBorrow(false);
        source.setTestOnReturn(false);
        return source;
    }
}
""".strip()


def read_entries(path: Path) -> dict[str, tuple[zipfile.ZipInfo, bytes]]:
    with zipfile.ZipFile(path, "r") as zf:
        return {info.filename: (info, zf.read(info.filename)) for info in zf.infolist()}


def write_entries(path: Path, entries: dict[str, tuple[zipfile.ZipInfo, bytes]]) -> None:
    out = io.BytesIO()
    with zipfile.ZipFile(out, "w") as zf:
        for name, (info, data) in entries.items():
            new_info = zipfile.ZipInfo(name, date_time=info.date_time)
            new_info.compress_type = info.compress_type
            new_info.comment = info.comment
            new_info.extra = info.extra
            new_info.internal_attr = info.internal_attr
            new_info.external_attr = info.external_attr
            new_info.create_system = info.create_system
            zf.writestr(new_info, data)
    path.write_bytes(out.getvalue())


def main() -> None:
    parser = argparse.ArgumentParser(description="Patch notification-sms-jincang DataSourceConfig for Docker PostgreSQL.")
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--backup-suffix", default=".pre-sms-datasource.bak")
    args = parser.parse_args()

    entries = read_entries(args.jar)
    if JAR_CLASS_ENTRY not in entries:
        raise SystemExit(f"{JAR_CLASS_ENTRY} was not found in {args.jar}")

    with tempfile.TemporaryDirectory(prefix="adp-sms-datasource-") as tmp:
        tmp_path = Path(tmp)
        lib_dir = tmp_path / "lib"
        src_dir = tmp_path / "src/com/supcon/supfusion/notification/sms/config"
        classes_dir = tmp_path / "classes"
        lib_dir.mkdir()
        src_dir.mkdir(parents=True)
        classes_dir.mkdir()

        classpath_parts: list[str] = []
        for name, (_, data) in entries.items():
            if name.startswith("BOOT-INF/lib/") and name.endswith(".jar"):
                lib_path = lib_dir / Path(name).name
                lib_path.write_bytes(data)
                classpath_parts.append(str(lib_path))

        source_path = src_dir / "DataSourceConfig.java"
        source_path.write_text(SOURCE + "\n", encoding="utf-8")
        subprocess.run(
            [
                "javac",
                "--release",
                "8",
                "-proc:none",
                "-encoding",
                "UTF-8",
                "-cp",
                ":".join(classpath_parts),
                "-d",
                str(classes_dir),
                str(source_path),
            ],
            check=True,
        )
        class_bytes = (classes_dir / COMPILED_CLASS).read_bytes()

    backup = args.jar.with_name(args.jar.name + args.backup_suffix)
    if not backup.exists():
        shutil.copy2(args.jar, backup)
    info = entries[JAR_CLASS_ENTRY][0]
    entries[JAR_CLASS_ENTRY] = (info, class_bytes)
    write_entries(args.jar, entries)
    print(f"patched {args.jar}")


if __name__ == "__main__":
    main()
