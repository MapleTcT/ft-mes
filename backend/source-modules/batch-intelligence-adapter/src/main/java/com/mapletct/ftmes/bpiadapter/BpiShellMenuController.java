package com.mapletct.ftmes.bpiadapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
public class BpiShellMenuController {

    static final String SHELL_MENU_PATH = "/bpi-shell/menus/currentUser";
    static final String UI_GATE_HEADER = "X-BPI-UI-Gate";
    private static final String LEGACY_MENU_PATH = "/inter-api/rbac/v1/menus/currentUser";
    private static final long BPI_MENU_ID = 899_000_000_001L;
    private static final long BPI_CONSOLE_MENU_ID = 899_000_000_002L;
    private static final Pattern TRACE = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final List<String> GATEWAY_REQUEST_HEADERS = Arrays.asList(
            HttpHeaders.ACCEPT,
            HttpHeaders.ACCEPT_LANGUAGE,
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.COOKIE,
            "X-Tenant-Id",
            "langu_code",
            "X-Trace-Id");
    private static final List<String> GATEWAY_RESPONSE_HEADERS = Arrays.asList(
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.ETAG,
            "X-Trace-Id");

    private final BpiAdapterProperties properties;
    private final BpiClaimsMapper claimsMapper;
    private final InternalJwtIssuer jwtIssuer;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public BpiShellMenuController(
            BpiAdapterProperties properties,
            BpiClaimsMapper claimsMapper,
            InternalJwtIssuer jwtIssuer,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.claimsMapper = claimsMapper;
        this.jwtIssuer = jwtIssuer;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @GetMapping(SHELL_MENU_PATH)
    public ResponseEntity<byte[]> currentUserMenus(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        String traceId = traceId(request.getHeader("X-Trace-Id"));
        ResponseEntity<byte[]> gateway = fetchGatewayMenus(request, traceId);
        if (!properties.isShellMenuEnabled()) {
            return response(gateway, gateway.getBody(), "BYPASSED_DISABLED", traceId);
        }
        if (!gateway.getStatusCode().is2xxSuccessful() || gateway.getBody() == null) {
            return response(gateway, gateway.getBody(), "BYPASSED_GATEWAY_STATUS", traceId);
        }

        ObjectNode root;
        ArrayNode menus;
        try {
            JsonNode parsed = objectMapper.readTree(gateway.getBody());
            if (!(parsed instanceof ObjectNode) || !(parsed.get("list") instanceof ArrayNode)) {
                return response(gateway, gateway.getBody(), "BYPASSED_INVALID_MENU", traceId);
            }
            root = (ObjectNode) parsed;
            menus = (ArrayNode) root.get("list");
        } catch (Exception error) {
            return response(gateway, gateway.getBody(), "BYPASSED_INVALID_MENU", traceId);
        }

        GateDecision decision = resolveUiGate(jwt, traceId);
        String gateStatus = decision.headerValue();
        if (decision == GateDecision.ENABLED) {
            if (containsBpiMenu(menus)) {
                gateStatus = "VISIBLE_EXISTING";
            } else {
                insertBpiMenu(menus);
                gateStatus = "VISIBLE_INJECTED";
            }
        } else {
            removeBpiMenus(menus);
        }
        try {
            return response(gateway, objectMapper.writeValueAsBytes(root), gateStatus, traceId);
        } catch (Exception error) {
            return response(gateway, gateway.getBody(), "BYPASSED_SERIALIZATION", traceId);
        }
    }

    private ResponseEntity<byte[]> fetchGatewayMenus(HttpServletRequest request, String traceId) {
        HttpHeaders headers = new HttpHeaders();
        for (String name : GATEWAY_REQUEST_HEADERS) {
            String value = request.getHeader(name);
            if (StringUtils.hasText(value)) headers.set(name, value);
        }
        headers.set("X-Trace-Id", traceId);
        UriComponentsBuilder uri = UriComponentsBuilder
                .fromHttpUrl(trimSlash(properties.getLegacyGatewayBaseUrl()))
                .path(LEGACY_MENU_PATH);
        if (StringUtils.hasText(request.getQueryString())) uri.query(request.getQueryString());
        return restTemplate.exchange(
                uri.build(true).toUri(),
                HttpMethod.GET,
                new HttpEntity<byte[]>(headers),
                byte[].class);
    }

    private GateDecision resolveUiGate(Jwt jwt, String traceId) {
        try {
            BpiActor actor = claimsMapper.map(jwt);
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(jwtIssuer.issue(actor));
            headers.set("X-Trace-Id", traceId);
            URI target = UriComponentsBuilder
                    .fromHttpUrl(trimSlash(properties.getUpstreamBaseUrl()))
                    .path("/bpi/v1/feature-flags")
                    .queryParam("plantId", properties.getShellPlantId())
                    .queryParam("lineId", properties.getShellLineId())
                    .queryParam("scopeType", "LINE")
                    .build(true)
                    .toUri();
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    target,
                    HttpMethod.GET,
                    new HttpEntity<byte[]>(headers),
                    byte[].class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return GateDecision.UPSTREAM_UNAVAILABLE;
            }
            JsonNode data = objectMapper.readTree(response.getBody()).path("data");
            if (!data.isArray()) return GateDecision.UPSTREAM_UNAVAILABLE;
            for (JsonNode flag : data) {
                if ("bpi.ui".equals(flag.path("flagKey").asText())) {
                    return flag.path("effectiveEnabled").asBoolean(false)
                            ? GateDecision.ENABLED : GateDecision.DISABLED;
                }
            }
            return GateDecision.FLAG_MISSING;
        } catch (AdapterAccessDeniedException error) {
            return GateDecision.SCOPE_DENIED;
        } catch (RuntimeException error) {
            return GateDecision.UPSTREAM_UNAVAILABLE;
        } catch (Exception error) {
            return GateDecision.UPSTREAM_UNAVAILABLE;
        }
    }

    private boolean containsBpiMenu(ArrayNode menus) {
        for (JsonNode menu : menus) {
            if ("BPI".equals(menu.path("code").asText())) return true;
        }
        return false;
    }

    private void removeBpiMenus(ArrayNode menus) {
        for (int index = menus.size() - 1; index >= 0; index--) {
            if ("BPI".equals(menus.get(index).path("code").asText())) menus.remove(index);
        }
    }

    private void insertBpiMenu(ArrayNode menus) {
        JsonNode cid = menus.size() == 0 ? null : menus.get(0).get("cid");
        ObjectNode top = objectMapper.createObjectNode();
        commonMenuFields(top, BPI_MENU_ID, "BPI.menu.name", "智能批次", "BPI", -1L, cid);
        top.put("cssClass", "icon-folder");
        top.putNull("url");
        top.put("menuType", 1);
        top.putNull("status");
        top.putNull("route");
        top.put("sort", 68);

        ObjectNode console = objectMapper.createObjectNode();
        commonMenuFields(
                console,
                BPI_CONSOLE_MENU_ID,
                "BPI.menu.console",
                "智能批次工作台",
                "BPI_1.0.0_console",
                BPI_MENU_ID,
                cid);
        console.put("cssClass", "icon-set");
        console.put("url", "/bpi/#/overview");
        console.put("menuType", 2);
        console.putNull("children");
        console.put("status", 0);
        console.put("route", "/bpi/#/overview");
        console.put("sort", 1);
        top.set("children", objectMapper.createArrayNode().add(console));

        int insertionIndex = menus.size();
        for (int index = 0; index < menus.size(); index++) {
            if ("makeTask".equals(menus.get(index).path("code").asText())) {
                insertionIndex = index + 1;
                break;
            }
        }
        menus.insert(insertionIndex, top);
    }

    private void commonMenuFields(
            ObjectNode menu,
            long id,
            String name,
            String nameDisplay,
            String code,
            long parentId,
            JsonNode cid) {
        menu.put("id", id);
        menu.put("name", name);
        menu.put("nameDisplay", nameDisplay);
        menu.put("isHide", false);
        menu.putNull("moduleCode");
        menu.put("code", code);
        if (cid == null) menu.putNull("cid");
        else menu.set("cid", cid.deepCopy());
        menu.put("parentId", parentId);
        menu.put("showType", 1);
        menu.putNull("source");
        menu.putNull("target");
        menu.putNull("memo");
        menu.put("type", "menu");
        menu.putNull("moduleName");
        menu.put("app", "BPI");
        menu.putNull("extra");
        menu.putNull("company_readOnly");
        menu.putNull("systemMenu");
        menu.putNull("copyMenu");
    }

    private ResponseEntity<byte[]> response(
            ResponseEntity<byte[]> gateway,
            byte[] body,
            String gateStatus,
            String traceId) {
        HttpHeaders headers = new HttpHeaders();
        for (String name : GATEWAY_RESPONSE_HEADERS) {
            if (gateway.getHeaders().containsKey(name)) {
                headers.put(name, gateway.getHeaders().get(name));
            }
        }
        if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        headers.setCacheControl("no-store, no-cache, must-revalidate");
        headers.set(UI_GATE_HEADER, gateStatus);
        headers.set("X-Trace-Id", traceId);
        return new ResponseEntity<byte[]>(body, headers, gateway.getStatusCode());
    }

    private String traceId(String requested) {
        return requested != null && TRACE.matcher(requested).matches()
                ? requested : UUID.randomUUID().toString();
    }

    private String trimSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private enum GateDecision {
        ENABLED("VISIBLE"),
        DISABLED("HIDDEN_DISABLED"),
        FLAG_MISSING("HIDDEN_FLAG_MISSING"),
        SCOPE_DENIED("HIDDEN_SCOPE_DENIED"),
        UPSTREAM_UNAVAILABLE("HIDDEN_UPSTREAM_UNAVAILABLE");

        private final String headerValue;

        GateDecision(String headerValue) {
            this.headerValue = headerValue;
        }

        String headerValue() {
            return headerValue;
        }
    }
}
