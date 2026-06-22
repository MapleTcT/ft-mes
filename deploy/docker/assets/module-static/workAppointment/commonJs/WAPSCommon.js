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

/**
 * 获取页面的保存数据（可直接用于保存接口）
 */
 function getRegularSaveData(win) {

    if (!win) win = window;
    var API = win.ReactAPI;
    var saveData = API.getSaveData();
    var multiSysCodes = __getMultiSelectSystemCodeKeys(API);
    multiSysCodes.forEach(key => {
        var comp = API.getComponentAPI("SystemCode")[key];
        var keys = key.split(".");
        var obj = saveData;
        var i = 0;
        for (; i < keys.length - 1; i++) {
            obj = obj[keys[i]];
        }
        obj[keys[i]] = comp.props.value.join(",");
    });

    return saveData;
}


/**
 * 获取多选系统编码的key
 */
function __getMultiSelectSystemCodeKeys(API) {
    if (!API) API = ReactAPI;
    var keys = [];
    Object.keys(API.getComponentAPI("SystemCode")).forEach(key => {
        var comp = API.getComponentAPI("SystemCode")[key];
        if (typeof comp == "object" && comp.props) {
            if (typeof comp.props.value == "object") {
                //说明值为多选系统编码，需要转换成逗号隔开的字符串
                keys.push(key);
            }
        }
    });
    return keys;
}


    //回显数据
    function actionEcho(actions) {
        var tabIds=[];
        actions.forEach(element => {
            var tabId = ticketAddTab(actionSrc + "&id=" + element.id);
            tabIds.push(tabId);//保存第一个页签id
        });
        //用于 选中第一个页签--
        return tabIds;
    }


    /**
     *
     * @param id
     * @param title
     * @param url
     */
     function ticketAddTab(url) {
        //添加页签
        let id = "action-" + tabIndex;
        ReactAPI.Layout.addTab(parentTabId, {
            id: id,
            title: tabName_action,
            url: url,
        });
        //隐藏默认删除按钮
        $('#' + id + ' > i ').remove();
        //记录页签的id 与iframe的序号
        tab$iframe.set(id, tabIndex);
        //序号++
        tabIndex++;
        return id;
    }
