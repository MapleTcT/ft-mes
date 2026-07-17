#!/usr/bin/env python3
"""Regression checks for the legacy Foundation simulated-login runtime patch."""

from __future__ import annotations

import unittest
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parent
PATCH_SOURCE = (
    SCRIPT_DIR.parent
    / "patches/foundation-simulated-login-serverport/src/com/supcon/orchid/"
    "foundation/internal/services/SimulatedLoginService.java"
)
BUILD_SCRIPT = SCRIPT_DIR / "build-foundation-simulated-login-serverport-patch.sh"


class FoundationSimulatedLoginPatchTest(unittest.TestCase):
    def test_auth_response_logging_never_serializes_token_values(self):
        source = PATCH_SOURCE.read_text(encoding="utf-8")

        self.assertIn('"response fields=" + body.keySet()', source)
        self.assertNotIn("body.toJSONString()", source)
        self.assertNotIn('logger.error("Simulated login failed: " + accessToken)', source)

    def test_jwt_parser_keeps_configurable_fallback(self):
        source = PATCH_SOURCE.read_text(encoding="utf-8")

        self.assertIn("adp.simulated-login.jwt.fallback-enabled", source)
        self.assertIn("adp.simulated-login.jwt.public-key", source)
        self.assertIn("JwtUser jwtUser = parseJwtUser(accessToken);", source)
        self.assertIn("fallbackJwtTokenUtil.getJwtUserFromToken(accessToken)", source)

    def test_builder_supports_generic_boot_jars_and_validates_nested_zip(self):
        source = BUILD_SCRIPT.read_text(encoding="utf-8")

        self.assertIn("--input-boot-jar|--input-lims-jar", source)
        self.assertIn("--output-boot-jar|--output-lims-jar", source)
        self.assertIn("zipfile.ZIP_STORED", source)
        self.assertIn("len(names) != len(set(names))", source)
        self.assertIn("outer.testzip()", source)
        self.assertIn("nested.testzip()", source)


if __name__ == "__main__":
    unittest.main()
