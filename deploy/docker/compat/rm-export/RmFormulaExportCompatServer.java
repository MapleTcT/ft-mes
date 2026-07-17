import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class RmFormulaExportCompatServer {
    private static final String EXPORT_PATH = "/msService/RM/formula/formula/batchFormulaList-query";
    private static final int MAX_REQUEST_BODY_BYTES = intEnv("RM_EXPORT_MAX_REQUEST_BYTES", 1_048_576);
    private static final Pattern EXPORT_FLAG = Pattern.compile(
            "\"exportFlag\"\\s*:\\s*true",
            Pattern.CASE_INSENSITIVE
    );
    private static final String[] HEADERS = {
            "单据编号",
            "配方编码",
            "配方名称",
            "配方版本",
            "产品编码",
            "批控配方ID",
            "批控配方编码",
            "批控版本",
            "批控配方标准产量",
            "描述"
    };

    private RmFormulaExportCompatServer() {
    }

    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        int port = intEnv("RM_EXPORT_PORT", 18090);
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/health", RmFormulaExportCompatServer::health);
        server.createContext(EXPORT_PATH, RmFormulaExportCompatServer::export);
        server.setExecutor(Executors.newFixedThreadPool(Math.max(2, intEnv("RM_EXPORT_THREADS", 4))));
        server.start();
        System.out.println("RM export compatibility server listening on " + port);
    }

    private static void health(HttpExchange exchange) throws IOException {
        writeJson(exchange, 200, "{\"status\":\"UP\"}");
    }

    private static void export(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())
                && !"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"code\":405,\"message\":\"Method Not Allowed\"}");
            return;
        }
        if (!hasAuth(exchange)) {
            writeJson(exchange, 401, "{\"code\":401,\"message\":\"Missing ADP auth proof\"}");
            return;
        }

        String requestBody;
        try {
            requestBody = readRequestBody(exchange);
        } catch (RequestBodyTooLargeException error) {
            writeJson(exchange, 413, "{\"code\":413,\"message\":\"Request body too large\"}");
            return;
        }

        try {
            if (!isExportRequest(requestBody)) {
                writeJson(exchange, 200, buildListJson(requestBody));
                return;
            }
            int maxRows = Math.max(1, Math.min(100_000, intEnv("RM_EXPORT_MAX_ROWS", 5000)));
            int totalRows = countRows();
            if (totalRows > maxRows) {
                writeJson(exchange, 422, "{\"code\":422,\"message\":\"Export row limit exceeded\",\"totalRows\":"
                        + totalRows + ",\"maxRows\":" + maxRows + "}");
                return;
            }
            byte[] workbook = buildWorkbook(rowValues(queryRows(0, Math.max(1, totalRows))));
            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            headers.set("Content-Disposition", "attachment; filename=\"RM_batchFormulaList.xlsx\"");
            headers.set("Cache-Control", "no-store");
            headers.set("X-Content-Type-Options", "nosniff");
            headers.set("X-ADP-Compat-Export", "rm-formula-postgres");
            exchange.sendResponseHeaders(200, workbook.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(workbook);
            }
        } catch (Exception error) {
            System.err.println("RM export compatibility failed: " + error.getClass().getSimpleName()
                    + ": " + String.valueOf(error.getMessage()));
            writeJson(exchange, 500, "{\"code\":500,\"message\":\"RM export compatibility failed\"}");
        }
    }

    private static boolean hasAuth(HttpExchange exchange) {
        return "1".equals(exchange.getRequestHeaders().getFirst("X-ADP-Auth-Checked"));
    }

    private static List<String[]> rowValues(List<FormulaRow> rows) {
        List<String[]> values = new ArrayList<String[]>();
        for (FormulaRow row : rows) {
            values.add(row.values);
        }
        return values;
    }

    private static List<FormulaRow> queryRows(int offset, int limit) throws Exception {
        String sql =
                "select cast(f.id as text), coalesce(f.table_no, ''), coalesce(f.formual_code, ''), "
                        + "coalesce(f.formula_name, ''), coalesce(f.formula_edtion, ''), "
                        + "coalesce(cast(f.product_id as text), ''), coalesce(m.code, ''), coalesce(m.name, ''), "
                        + "coalesce(f.batch_formulaid, ''), coalesce(f.batch_formula_code, ''), "
                        + "coalesce(f.batch_formula_edition, ''), coalesce(f.nor_size, ''), "
                        + "coalesce(f.description, ''), coalesce(cast(f.status as text), ''), "
                        + "coalesce(cast(f.valid as text), '') "
                        + "from public.rm_formulas f "
                        + "left join public.baseset_materials m on m.id = f.product_id "
                        + "where coalesce(f.valid, true) is true "
                        + "order by f.id desc limit ? offset ?";
        List<FormulaRow> rows = new ArrayList<FormulaRow>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(intEnv("RM_EXPORT_QUERY_TIMEOUT_SECONDS", 30));
            statement.setInt(1, limit);
            statement.setInt(2, Math.max(0, offset));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String[] row = new String[HEADERS.length];
                    row[0] = resultSet.getString(2);
                    row[1] = resultSet.getString(3);
                    row[2] = resultSet.getString(4);
                    row[3] = resultSet.getString(5);
                    row[4] = firstNonEmpty(resultSet.getString(7), resultSet.getString(6));
                    row[5] = resultSet.getString(9);
                    row[6] = resultSet.getString(10);
                    row[7] = resultSet.getString(11);
                    row[8] = resultSet.getString(12);
                    row[9] = resultSet.getString(13);
                    rows.add(new FormulaRow(
                            resultSet.getString(1),
                            row,
                            resultSet.getString(6),
                            resultSet.getString(7),
                            resultSet.getString(8),
                            resultSet.getString(14),
                            resultSet.getString(15)
                    ));
                }
            }
        }
        return rows;
    }

    private static int countRows() throws Exception {
        String sql = "select count(*) from public.rm_formulas where coalesce(valid, true) is true";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(intEnv("RM_EXPORT_QUERY_TIMEOUT_SECONDS", 30));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private static Connection openConnection() throws Exception {
        Connection connection = DriverManager.getConnection(
                jdbcUrl(),
                env("SUPOS_SYSTEM_DB_USERNAME", "adp"),
                env("SUPOS_SYSTEM_DB_PASSWORD", "adp123456")
        );
        connection.setReadOnly(true);
        return connection;
    }

    private static String buildListJson(String requestBody) throws Exception {
        int pageNo = Math.max(1, Math.min(1_000_000, extractInt(requestBody, "pageNo", 1)));
        int pageSize = Math.max(1, Math.min(500, extractInt(requestBody, "pageSize", 20)));
        int offset = (int) Math.min(Integer.MAX_VALUE, ((long) pageNo - 1L) * pageSize);
        int totalCount = countRows();
        int totalPages = totalCount == 0 ? 0 : ((totalCount + pageSize - 1) / pageSize);
        List<FormulaRow> rows = queryRows(offset, pageSize);

        StringBuilder json = new StringBuilder();
        json.append("{\"code\":200,\"data\":{");
        json.append("\"first\":1,");
        json.append("\"hasNext\":").append(pageNo < totalPages).append(',');
        json.append("\"hasPre\":").append(pageNo > 1).append(',');
        json.append("\"nextPage\":").append(pageNo < totalPages ? pageNo + 1 : pageNo).append(',');
        json.append("\"pageNo\":").append(pageNo).append(',');
        json.append("\"pageSize\":").append(pageSize).append(',');
        json.append("\"prePage\":").append(pageNo > 1 ? pageNo - 1 : 1).append(',');
        json.append("\"result\":[");
        for (int index = 0; index < rows.size(); index += 1) {
            if (index > 0) {
                json.append(',');
            }
            appendRowJson(json, rows.get(index));
        }
        json.append("],");
        json.append("\"totalCount\":").append(totalCount).append(',');
        json.append("\"totalPages\":").append(totalPages).append(',');
        json.append("\"treeToSurfaceMode\":false");
        json.append("},\"message\":\"操作成功\"}");
        return json.toString();
    }

    private static void appendRowJson(StringBuilder json, FormulaRow row) {
        json.append('{');
        appendJsonString(json, "id", row.id);
        json.append(',');
        appendJsonString(json, "tableNo", row.values[0]);
        json.append(',');
        appendJsonString(json, "formualCode", row.values[1]);
        json.append(',');
        appendJsonString(json, "formulaName", row.values[2]);
        json.append(',');
        appendJsonString(json, "formulaEdtion", row.values[3]);
        json.append(',');
        json.append("\"productId\":{");
        appendJsonString(json, "id", row.productId);
        json.append(',');
        appendJsonString(json, "code", row.productCode);
        json.append(',');
        appendJsonString(json, "name", row.productName);
        json.append("},");
        appendJsonString(json, "batchFormulaID", row.values[5]);
        json.append(',');
        appendJsonString(json, "batchFormulaCode", row.values[6]);
        json.append(',');
        appendJsonString(json, "batchFormulaEdition", row.values[7]);
        json.append(',');
        appendJsonString(json, "norSize", row.values[8]);
        json.append(',');
        appendJsonString(json, "description", row.values[9]);
        json.append(',');
        appendJsonScalar(json, "status", row.status);
        json.append(',');
        appendJsonScalar(json, "valid", row.valid);
        json.append('}');
    }

    static String firstNonEmpty(String preferred, String fallback) {
        return preferred == null || preferred.isEmpty() ? (fallback == null ? "" : fallback) : preferred;
    }

    private static void appendJsonString(StringBuilder json, String key, String value) {
        json.append('"').append(key).append("\":\"").append(jsonEscape(value)).append('"');
    }

    private static void appendJsonScalar(StringBuilder json, String key, String value) {
        String normalized = value == null ? "" : value.trim();
        json.append('"').append(key).append("\":");
        if ("true".equalsIgnoreCase(normalized) || "false".equalsIgnoreCase(normalized)) {
            json.append(Boolean.parseBoolean(normalized));
        } else if (normalized.matches("-?\\d+(\\.\\d+)?")) {
            json.append(normalized);
        } else {
            json.append('"').append(jsonEscape(value)).append('"');
        }
    }

    private static String jdbcUrl() {
        String host = env("SUPOS_SYSTEM_DB_HOST", "postgres");
        String port = env("SUPOS_SYSTEM_DB_PORT", "5432");
        String db = env("SUPOS_SYSTEM_DB_NAME", "adp");
        return "jdbc:postgresql://" + host + ":" + port + "/" + db
                + "?connectTimeout=5&socketTimeout=30&ApplicationName=adp-rm-export-compat";
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        try (InputStream input = exchange.getRequestBody()) {
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (bytes.size() + read > MAX_REQUEST_BODY_BYTES) {
                    throw new RequestBodyTooLargeException();
                }
                bytes.write(buffer, 0, read);
            }
        }
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }

    private static boolean isExportRequest(String requestBody) {
        return EXPORT_FLAG.matcher(requestBody == null ? "" : requestBody).find();
    }

    private static int extractInt(String requestBody, String key, int defaultValue) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)")
                .matcher(requestBody == null ? "" : requestBody);
        if (!matcher.find()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    static byte[] buildWorkbook(List<String[]> dataRows) throws IOException {
        List<String[]> rows = new ArrayList<String[]>();
        rows.add(HEADERS);
        rows.addAll(dataRows);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            put(zip, "[Content_Types].xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                            + "<Override PartName=\"/docProps/core.xml\" ContentType=\"application/vnd.openxmlformats-package.core-properties+xml\"/>"
                            + "<Override PartName=\"/docProps/app.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.extended-properties+xml\"/>"
                            + "</Types>");
            put(zip, "_rels/.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                            + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties\" Target=\"docProps/core.xml\"/>"
                            + "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties\" Target=\"docProps/app.xml\"/>"
                            + "</Relationships>");
            put(zip, "docProps/app.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<Properties xmlns=\"http://schemas.openxmlformats.org/officeDocument/2006/extended-properties\">"
                            + "<Application>ADP RM Export Compat</Application></Properties>");
            put(zip, "docProps/core.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" "
                            + "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" "
                            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                            + "<dc:creator>ADP RM Export Compat</dc:creator>"
                            + "<dcterms:created xsi:type=\"dcterms:W3CDTF\">" + xmlEscape(Instant.now().toString())
                            + "</dcterms:created></cp:coreProperties>");
            put(zip, "xl/workbook.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                            + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                            + "<sheets><sheet name=\"RM_batchFormulaList\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            put(zip, "xl/_rels/workbook.xml.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" "
                            + "Target=\"worksheets/sheet1.xml\"/></Relationships>");
            put(zip, "xl/worksheets/sheet1.xml", sheetXml(rows));
        }
        return bytes.toByteArray();
    }

    private static String sheetXml(List<String[]> rows) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex += 1) {
            int excelRow = rowIndex + 1;
            xml.append("<row r=\"").append(excelRow).append("\">");
            String[] row = rows.get(rowIndex);
            for (int colIndex = 0; colIndex < row.length; colIndex += 1) {
                xml.append("<c r=\"").append(columnName(colIndex)).append(excelRow)
                        .append("\" t=\"inlineStr\"><is><t>")
                        .append(xmlEscape(row[colIndex]))
                        .append("</t></is></c>");
            }
            xml.append("</row>");
        }
        xml.append("</sheetData></worksheet>");
        return xml.toString();
    }

    private static String columnName(int index) {
        StringBuilder name = new StringBuilder();
        int value = index + 1;
        while (value > 0) {
            int remainder = (value - 1) % 26;
            name.insert(0, (char) ('A' + remainder));
            value = (value - 1) / 26;
        }
        return name.toString();
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void writeJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private static int intEnv(String key, int defaultValue) {
        try {
            return Integer.parseInt(env(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    static String xmlEscape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length();) {
            int current = value.codePointAt(index);
            index += Character.charCount(current);
            if (current == '&') {
                escaped.append("&amp;");
            } else if (current == '<') {
                escaped.append("&lt;");
            } else if (current == '>') {
                escaped.append("&gt;");
            } else if (current == '"') {
                escaped.append("&quot;");
            } else if (current == '\'') {
                escaped.append("&apos;");
            } else if (isValidXmlCodePoint(current)) {
                escaped.appendCodePoint(current);
            }
        }
        return escaped.toString();
    }

    private static boolean isValidXmlCodePoint(int value) {
        return value == 0x9 || value == 0xA || value == 0xD
                || (value >= 0x20 && value <= 0xD7FF)
                || (value >= 0xE000 && value <= 0xFFFD)
                || (value >= 0x10000 && value <= 0x10FFFF);
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index += 1) {
            char current = value.charAt(index);
            switch (current) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (current < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) current));
                    } else {
                        escaped.append(current);
                    }
            }
        }
        return escaped.toString();
    }

    private static final class FormulaRow {
        private final String id;
        private final String[] values;
        private final String productId;
        private final String productCode;
        private final String productName;
        private final String status;
        private final String valid;

        private FormulaRow(
                String id,
                String[] values,
                String productId,
                String productCode,
                String productName,
                String status,
                String valid
        ) {
            this.id = id;
            this.values = values;
            this.productId = productId;
            this.productCode = productCode;
            this.productName = productName;
            this.status = status;
            this.valid = valid;
        }
    }

    private static final class RequestBodyTooLargeException extends IOException {
        private static final long serialVersionUID = 1L;
    }
}
