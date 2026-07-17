import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class RmFormulaExportCompatServerTest {
    private RmFormulaExportCompatServerTest() {
    }

    public static void main(String[] args) throws Exception {
        assertEquals("preferred", RmFormulaExportCompatServer.firstNonEmpty("preferred", "fallback"));
        assertEquals("fallback", RmFormulaExportCompatServer.firstNonEmpty("", "fallback"));
        assertEquals("&lt;&amp;&gt;&quot;&apos;AB",
                RmFormulaExportCompatServer.xmlEscape("<&>\"'\u0000A\uD800B"));

        List<String[]> rows = new ArrayList<String[]>();
        rows.add(new String[]{
                "FORM-1",
                "FC-1",
                "=2+2",
                "V1",
                "MAT<&>",
                "BATCH-1",
                "BC-1",
                "BV1",
                "100",
                "line 1\nline 2"
        });
        byte[] workbook = RmFormulaExportCompatServer.buildWorkbook(rows);
        assertTrue(workbook.length > 4, "workbook must not be empty");
        assertTrue(workbook[0] == 'P' && workbook[1] == 'K', "workbook must be a ZIP/XLSX file");

        String sheet = zipEntry(workbook, "xl/worksheets/sheet1.xml");
        assertTrue(sheet.contains("t=\"inlineStr\""), "cells must remain inline strings");
        assertTrue(sheet.contains("=2+2"), "formula-looking input must remain text content");
        assertTrue(sheet.contains("MAT&lt;&amp;&gt;"), "XML-sensitive material codes must be escaped");
        assertTrue(sheet.contains("产品编码"), "workbook must expose the product code column");
        assertTrue(!sheet.contains("产品ID"), "workbook must not substitute product IDs for product codes");

        System.out.println("RM formula export compatibility helpers: PASS");
    }

    private static String zipEntry(byte[] zipBytes, String expectedName) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!expectedName.equals(entry.getName())) {
                    continue;
                }
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int read;
                while ((read = zip.read(buffer)) >= 0) {
                    bytes.write(buffer, 0, read);
                }
                return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("missing ZIP entry: " + expectedName);
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
