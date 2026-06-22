// PostgreSQL/runtime compatibility: the recovered WorkAppointment work-plan
// list backend needs datagridCode on list query/pending requests.
(function () {
    var listCode = "workAppointment_6.1.6.1_workPlan_workPlanList";
    var targetPattern = /\/workAppointment\/workPlan\/workTicketPlan\/workPlanList-(query|pending)$/;

    if (window.__adpWapsWorkPlanListCompat) {
        return;
    }
    window.__adpWapsWorkPlanListCompat = true;

    function shouldPatch(method, url) {
        return String(method || "").toUpperCase() === "POST" && targetPattern.test(String(url || "").split("?")[0]);
    }

    function patchBody(body) {
        if (typeof body !== "string" || body.indexOf("{") !== 0 || body.indexOf("datagridCode") !== -1) {
            return body;
        }
        try {
            var data = JSON.parse(body);
            data.datagridCode = listCode;
            data.permissionCode = data.permissionCode || listCode;
            return JSON.stringify(data);
        } catch (error) {
            return body;
        }
    }

    if (window.XMLHttpRequest && !window.XMLHttpRequest.__adpWapsWorkPlanListCompat) {
        var originalOpen = window.XMLHttpRequest.prototype.open;
        var originalSend = window.XMLHttpRequest.prototype.send;
        window.XMLHttpRequest.prototype.open = function (method, url) {
            this.__adpWapsMethod = method;
            this.__adpWapsUrl = url;
            return originalOpen.apply(this, arguments);
        };
        window.XMLHttpRequest.prototype.send = function (body) {
            if (shouldPatch(this.__adpWapsMethod, this.__adpWapsUrl)) {
                body = patchBody(body);
            }
            return originalSend.call(this, body);
        };
        window.XMLHttpRequest.__adpWapsWorkPlanListCompat = true;
    }

    if (window.fetch && !window.fetch.__adpWapsWorkPlanListCompat) {
        var originalFetch = window.fetch;
        var patchedFetch = function (input, init) {
            var url = typeof input === "string" ? input : input && input.url;
            var method = (init && init.method) || (input && input.method) || "GET";
            if (init && shouldPatch(method, url)) {
                init = Object.assign({}, init, { body: patchBody(init.body) });
            }
            return originalFetch.call(this, input, init);
        };
        patchedFetch.__adpWapsWorkPlanListCompat = true;
        window.fetch = patchedFetch;
    }
})();
