(function installWomMakeTaskListToolbarGuards() {
  if (window.__adpWomMakeTaskListToolbarGuards) {
    return;
  }
  window.__adpWomMakeTaskListToolbarGuards = true;

  var gridId = "WOM_1.0.0_produceTask_makeTaskList_produceTask_sdg";
  var messages = {
    noSelection: "请先选择一条指令单！",
    multiSelection: "只可以进行单批次查看！",
    waitForRun: "未执行的指令单，请重新选择！",
    processUnavailable: "生产过程追溯服务未部署或暂不可用！",
    qrcodeUnavailable: "二维码生成页面未部署或暂不可用！"
  };

  var translationFallbacks = {
    "SupDatagrid.button.error": "请选择一条记录进行操作！",
    "SupDatagrid.button.tit": "提示",
    "ec.common.tableNo": "单据编号",
    "ec.engine.view.dealsuccess": "操作成功",
    "WOM.custom.random1613701402613": "“尾料量”必须小于“产出量”",
    "WOM.custom.random1623129307428": "指令单已保持！",
    "WOM.custom.random1623129401596": "指令单已重启！",
    "WOM.custom.random1633930723936": "配方未配置检验部门和检验人！",
    "WOM.custom.randon1575958091725": "只有非批控的工单才可以操作！",
    "WOM.custom.randon1575958171058": "请先将指令单生效再进行操作！",
    "WOM.custom.randon1575958246066": "只有【待执行】的指令单可以开始！",
    "WOM.custom.randon1575958861853": "只有【执行中】或者【已暂停】的指令单可以结束！",
    "WOM.custom.randon1575959595767": "只有【执行中】的指令单可以保持！",
    "WOM.custom.randon1575968753369": "只有【已保持】的指令单可以重启！",
    "WOM.custom.randon1591583966456": "该批次不能提前放料！",
    "WOM.custom.randon1591599292328": "提前投料失败!",
    "WOM.custom.randon1591755924139": "操作成功！",
    "WOM.custom.randon1592209666643": "【{0}】，是否继续放料？",
    "WOM.custom.randon1596434273127": "是",
    "WOM.custom.randon1596434330969": "否",
    "WOM.custom.randon1597044938864": "只有【执行中】的指令单允许提前放料！",
    "WOM.custom.randon1597055078126": "是否提前放料？",
    "WOM.custom.randon1597211462498": "所选指令单未生效！",
    "WOM.custom.randon1597226087560": "该指令单检验状态为【{0}】",
    "WOM.custom.randon1597227115284": ",检验结论为【{}】",
    "WOM.custom.randon1597231220469": "处理成功！",
    "WOM.custom.randon1597309436125": "是否发起检验申请？",
    "WOM.custom.randon1597996619261": "只有【执行中】的指令单允许请检！",
    "WOM.custom.randon1598424040495": "指令单已生成产品检验申请单，检验状态【待检】，是否发起检验申请？",
    "WOM.custom.randon1598676953828": "该指令单产品无需质检！",
    "WOM.custom.randon1602465957459": "当前指令单不允许该操作！",
    "WOM.custom.randon1602655303748": "指令单已开始！"
  };

  var dependencyStatus = window.ADP_WOM_MAKETASKLIST_DEPENDENCIES || {};

  function accessibleWindows() {
    var result = [];
    function add(candidate) {
      try {
        if (candidate && candidate.document && result.indexOf(candidate) === -1) {
          result.push(candidate);
        }
      } catch (_error) {
        // Cross-origin or detached windows are ignored.
      }
    }
    add(window);
    add(window.parent);
    add(window.top);
    return result;
  }

  function resourceValue(key) {
    var windows = accessibleWindows();
    for (var index = 0; index < windows.length; index += 1) {
      var resources = windows[index].InternationalResource || {};
      if (resources[key]) {
        return resources[key];
      }
    }
    return "";
  }

  function translateKey(key) {
    return resourceValue(key) || translationFallbacks[key] || key;
  }

  function translateMessage(value) {
    if (typeof value !== "string") {
      return value;
    }
    if (resourceValue(value) || translationFallbacks[value]) {
      return translateKey(value);
    }
    if (value.indexOf(".") === -1) {
      return value;
    }
    return value.replace(/(?:WOM\.custom\.(?:random|randon)\d+|ec\.common\.tableNo|SupDatagrid\.button\.(?:error|tit)|ec\.engine\.view\.dealsuccess)/g, function replaceKey(key) {
      return translateKey(key);
    });
  }

  function translateText(text) {
    return translateMessage(text);
  }

  function translateDomText(root) {
    if (!root || !root.ownerDocument || !root.ownerDocument.body || !root.ownerDocument.createTreeWalker) {
      return;
    }
    var doc = root.ownerDocument;
    var win = doc.defaultView || window;
    var nodeFilter = win.NodeFilter || window.NodeFilter;
    if (!nodeFilter) {
      return;
    }
    var walker = doc.createTreeWalker(root, nodeFilter.SHOW_TEXT, {
      acceptNode: function acceptNode(node) {
        var parent = node.parentNode;
        if (!parent || /^(SCRIPT|STYLE|TEXTAREA|INPUT)$/i.test(parent.nodeName || "")) {
          return nodeFilter.FILTER_REJECT;
        }
        return node.nodeValue && node.nodeValue.indexOf(".") !== -1
          ? nodeFilter.FILTER_ACCEPT
          : nodeFilter.FILTER_REJECT;
      }
    });
    var node;
    while ((node = walker.nextNode())) {
      var translated = translateText(node.nodeValue);
      if (translated !== node.nodeValue) {
        node.nodeValue = translated;
      }
    }
  }

  function installVisibleTextFallback() {
    accessibleWindows().forEach(function installWindowVisibleTextFallback(targetWindow) {
      var doc = targetWindow.document;
      if (!doc || !doc.body) {
        return;
      }
      if (!targetWindow.MutationObserver || targetWindow.__ADP_WOM_MAKETASKLIST_BODY_DOM_TRANSLATE__) {
        translateDomText(doc.body);
        return;
      }
      targetWindow.__ADP_WOM_MAKETASKLIST_BODY_DOM_TRANSLATE__ = true;
      var observer = new targetWindow.MutationObserver(function translateMutations(mutations) {
        mutations.forEach(function translateMutation(mutation) {
          if (mutation.type === "characterData") {
            var translated = translateText(mutation.target.nodeValue);
            if (translated !== mutation.target.nodeValue) {
              mutation.target.nodeValue = translated;
            }
            return;
          }
          Array.prototype.forEach.call(mutation.addedNodes || [], function translateNode(node) {
            if (node.nodeType === 3) {
              var text = translateText(node.nodeValue);
              if (text !== node.nodeValue) {
                node.nodeValue = text;
              }
            } else if (node.nodeType === 1) {
              translateDomText(node);
            }
          });
        });
      });
      observer.observe(doc.body, { childList: true, subtree: true, characterData: true });
      translateDomText(doc.body);
    });
  }

  function stopToolbarEvent(event) {
    event.preventDefault();
    event.stopPropagation();
    if (typeof event.stopImmediatePropagation === "function") {
      event.stopImmediatePropagation();
    }
  }

  function installEmptySearchSelectFallback() {
    var searchSelect = document.getElementById("search_panel_selectField");
    if (!searchSelect || searchSelect.getAttribute("data-adp-wom-empty-search-select") === "true") {
      return;
    }

    var selectedValue = searchSelect.querySelector(".ant-select-selection-selected-value");
    var placeholder = searchSelect.querySelector(".ant-select-selection__placeholder");
    var selection = searchSelect.querySelector(".ant-select-selection");
    if (!selectedValue || (selectedValue.textContent || "").trim()) {
      return;
    }

    selectedValue.textContent = "全部";
    selectedValue.setAttribute("title", "全部");
    selectedValue.style.display = "block";
    selectedValue.style.opacity = "1";
    if (placeholder) {
      placeholder.style.display = "none";
    }
    if (selection) {
      selection.setAttribute("aria-disabled", "true");
      selection.setAttribute("title", "当前无可选筛选字段，默认查询全部");
    }
    searchSelect.className += " adp-wom-empty-search-select";
    searchSelect.setAttribute("data-adp-wom-empty-search-select", "true");

    ["mousedown", "click", "keydown"].forEach(function blockEmptyDropdown(eventName) {
      searchSelect.addEventListener(
        eventName,
        function preventEmptyDropdown(event) {
          stopToolbarEvent(event);
        },
        true
      );
    });
  }

  function blockEmptySearchSelectEvent(event) {
    var target = event.target;
    var searchSelect = target && target.closest && target.closest("#search_panel_selectField");
    if (!searchSelect) {
      return;
    }
    installEmptySearchSelectFallback();
    if (searchSelect.getAttribute("data-adp-wom-empty-search-select") === "true") {
      stopToolbarEvent(event);
    }
  }

  function translateOptions(options) {
    if (!options || typeof options !== "object") {
      return options;
    }
    var patched = {};
    Object.keys(options).forEach(function copyOption(key) {
      var optionValue = options[key];
      patched[key] = typeof optionValue === "string" ? translateMessage(optionValue) : optionValue;
    });
    return patched;
  }

  function installMessageTranslationFallback() {
    accessibleWindows().forEach(function installWindowMessageTranslationFallback(targetWindow) {
      var reactApi = targetWindow.ReactAPI;
      var international = reactApi && reactApi.international;

      if (international && typeof international.getText === "function" && !international.__adpWomMakeTaskListBodyI18nPatched) {
        var originalGetText = international.getText.bind(international);
        international.getText = function getTextWithWomBodyFallback(key) {
          var value = originalGetText.apply(null, arguments);
          if (value === key || value === "" || value == null) {
            return translateMessage(key);
          }
          return translateMessage(value);
        };
        international.__adpWomMakeTaskListBodyI18nPatched = true;
      }

      if (!reactApi) {
        return;
      }

      if (
        typeof reactApi.showMessage === "function" &&
        reactApi.showMessage !== reactApi.__adpWomMakeTaskListBodyShowMessageWrapper
      ) {
        var originalShowMessage = reactApi.showMessage;
        var originalShowMessageBound = originalShowMessage.bind(reactApi);
        reactApi.showMessage = function showMessageWithWomBodyFallback(type, message) {
          var args = Array.prototype.slice.call(arguments);
          args = args.map(function translateMessageArg(arg) {
            return typeof arg === "string" ? translateMessage(arg) : translateOptions(arg);
          });
          return originalShowMessageBound.apply(null, args);
        };
        reactApi.__adpWomMakeTaskListBodyShowMessageWrapper = reactApi.showMessage;
      }

      ["openConfirm", "confirm", "showConfirm"].forEach(function patchConfirm(methodName) {
        var wrapperKey = "__adpWomMakeTaskListBody" + methodName + "Wrapper";
        if (
          typeof reactApi[methodName] !== "function" ||
          reactApi[methodName] === reactApi[wrapperKey]
        ) {
          return;
        }
        var original = reactApi[methodName];
        var originalBound = original.bind(reactApi);
        reactApi[methodName] = function confirmWithWomBodyFallback(options) {
          var args = Array.prototype.slice.call(arguments);
          args[0] = translateOptions(args[0]);
          return originalBound.apply(null, args);
        };
        reactApi[wrapperKey] = reactApi[methodName];
      });

      reactApi.__adpWomMakeTaskListBodyMessagePatched = true;
      targetWindow.__ADP_WOM_MAKETASKLIST_BODY_TRANSLATE__ = true;
    });
  }

  function showWarning(message) {
    installMessageTranslationFallback();
    message = translateMessage(message);
    if (window.ReactAPI && typeof window.ReactAPI.showMessage === "function") {
      window.ReactAPI.showMessage("w", message);
      return;
    }
    window.alert(message);
  }

  function systemCodeId(value) {
    if (!value) {
      return "";
    }
    if (typeof value === "string") {
      return value;
    }
    return value.id || value.value || "";
  }

  function syncGet(url, data) {
    var result = { ok: false, status: 0, body: null };
    var ajaxHost = window.jQuery || window.$;
    if (!ajaxHost || !ajaxHost.ajax) {
      result.error = "jQuery.ajax unavailable";
      return result;
    }
    ajaxHost.ajax({
      type: "GET",
      url: url,
      data: data || {},
      traditional: true,
      async: false,
      timeout: 5000,
      success: function onSuccess(body, _textStatus, xhr) {
        result.ok = true;
        result.status = xhr && xhr.status;
        result.body = body;
      },
      error: function onError(xhr) {
        result.ok = false;
        result.status = xhr && xhr.status;
        result.body = xhr && xhr.responseText;
      }
    });
    return result;
  }

  function dependencyEnabled(name) {
    return dependencyStatus[name] === true || dependencyStatus[name] === "true";
  }

  var taskStateById = {};
  var lastSelectedTaskId = "";
  var restoreSelectionUntil = 0;

  function getGrid() {
    var reactApi = window.ReactAPI;
    var factory =
      reactApi &&
      reactApi.getComponentAPI &&
      reactApi.getComponentAPI("SupDataGrid");
    var grid = factory && factory.APIs && factory.APIs(gridId);
    patchGridSelectionApi(grid);
    return grid;
  }

  function asId(value) {
    return value == null ? "" : String(value);
  }

  function extractTaskId(data) {
    if (!data) {
      return "";
    }
    if (typeof data === "string") {
      var match = data.match(/(?:^|&)taskId=([^&]+)/);
      return match ? decodeURIComponent(match[1].replace(/\+/g, " ")) : "";
    }
    if (typeof FormData !== "undefined" && data instanceof FormData) {
      return asId(data.get("taskId"));
    }
    return asId(data.taskId);
  }

  function copyOptions(source) {
    var target = {};
    source = source || {};
    Object.keys(source).forEach(function copyKey(key) {
      target[key] = source[key];
    });
    return target;
  }

  function applyTaskState(row, exeState) {
    if (!row || !exeState || !exeState.id) {
      return;
    }
    row.taskRunState = exeState;
    row.taskRunStateId = exeState.id;
    row.taskRunStateName = exeState.value || exeState.name || exeState.fullPathName || exeState.id;
  }

  function patchGridSelectionApi(grid) {
    if (!grid || grid.__adpWomMakeTaskListSelectionPatched || typeof grid.getSelecteds !== "function") {
      return;
    }
    var originalGetSelecteds = grid.getSelecteds.bind(grid);
    grid.getSelecteds = function getSelectedsWithRememberedTaskState() {
      var rows = originalGetSelecteds.apply(this, arguments) || [];
      rows.forEach(function patchSelectedRow(row) {
        var remembered = row && taskStateById[asId(row.id)];
        if (remembered) {
          applyTaskState(row, remembered);
        }
      });
      return rows;
    };
    grid.__adpWomMakeTaskListSelectionPatched = true;
    window.__ADP_WOM_MAKETASKLIST_SELECTION_SYNC__ = true;
  }

  function gridRows(grid) {
    return (
      (grid && grid.getRows && grid.getRows()) ||
      (grid && grid.getDatagridData && grid.getDatagridData()) ||
      []
    );
  }

  function rememberSelectedRow(row) {
    if (!row || row.id == null) {
      return;
    }
    lastSelectedTaskId = asId(row.id);
  }

  function restoreLastSelection(grid) {
    if (!grid || !lastSelectedTaskId || Date.now() > restoreSelectionUntil) {
      return false;
    }
    var rows = gridRows(grid);
    for (var index = 0; index < rows.length; index += 1) {
      if (asId(rows[index] && rows[index].id) === lastSelectedTaskId) {
        if (typeof grid.setSelecteds === "function") {
          grid.setSelecteds(String(index));
        }
        applyTaskState(rows[index], taskStateById[lastSelectedTaskId]);
        return true;
      }
    }
    return false;
  }

  function patchRememberedRows() {
    var grid = getGrid();
    if (!grid) {
      return;
    }
    var selecteds = (grid.getSelecteds && grid.getSelecteds()) || [];
    var rows = gridRows(grid);
    if (selecteds.length) {
      rememberSelectedRow(selecteds[0]);
    }
    selecteds.concat(rows).forEach(function patchRow(row) {
      var remembered = row && taskStateById[asId(row.id)];
      if (remembered) {
        applyTaskState(row, remembered);
      }
    });
    restoreLastSelection(grid);
  }

  function restoreSelectionLater() {
    [80, 300, 900].forEach(function queueRestore(delay) {
      window.setTimeout(function restoreSelection() {
        patchRememberedRows();
      }, delay);
    });
  }

  function selectedRows() {
    patchRememberedRows();
    var grid = getGrid();
    var rows = (grid && grid.getSelecteds && grid.getSelecteds()) || [];
    if (!rows.length && restoreLastSelection(grid)) {
      rows = (grid && grid.getSelecteds && grid.getSelecteds()) || [];
    }
    if (rows.length) {
      rememberSelectedRow(rows[0]);
    }
    return rows;
  }

  function selectedOne() {
    var rows = selectedRows();
    if (!rows.length) {
      showWarning(messages.noSelection);
      return null;
    }
    if (rows.length > 1) {
      showWarning(messages.multiSelection);
      return null;
    }
    return rows[0];
  }

  function guardToolbarSelection() {
    var rows = selectedRows();
    if (!rows.length) {
      showWarning(messages.noSelection);
      return false;
    }
    if (rows.length > 1) {
      showWarning(messages.multiSelection);
      return false;
    }
    return true;
  }

  function refreshGridLater() {
    var delays = [300, 1200, 2500];
    delays.forEach(function queueRefresh(delay) {
      window.setTimeout(function refreshGrid() {
        patchRememberedRows();
        var grid = getGrid();
        if (!grid || typeof grid.refreshDataByRequst !== "function") {
          return;
        }
        var pagination = (grid.getTablePagination && grid.getTablePagination()) || {};
        grid.refreshDataByRequst({
          type: "POST",
          url: "/msService/WOM/produceTask/produceTask/makeTaskList-pending",
          param: {
            classifyCodes: "",
            customCondition: {},
            permissionCode: "WOM_1.0.0_produceTask_makeTaskList",
            pageNo: pagination.currentPage || 1,
            paging: true,
            pageSize: pagination.pageSize || 20,
            crossCompanyFlag: "true"
          }
        });
        restoreSelectionLater();
      }, delay);
    });
  }

  function rememberUpdateTaskState(requestData, response) {
    var payload = response && response.data;
    if (!payload || payload.dealSuccessFlag !== true || !payload.exeState) {
      return;
    }
    var taskId = extractTaskId(requestData);
    if (!taskId) {
      return;
    }
    lastSelectedTaskId = taskId;
    restoreSelectionUntil = Date.now() + 15000;
    taskStateById[taskId] = payload.exeState;
    patchRememberedRows();
    refreshGridLater();
  }

  function installUpdateTaskStateSync() {
    var ajaxHost = window.jQuery || window.$;
    if (!ajaxHost || !ajaxHost.ajax) {
      window.setTimeout(installUpdateTaskStateSync, 100);
      return;
    }
    if (ajaxHost.ajax.__adpWomMakeTaskStateSyncPatched) {
      return;
    }

    var originalAjax = ajaxHost.ajax;
    var patchedAjax = function updateTaskStateSyncAjax(urlOrOptions, maybeOptions) {
      var options = typeof urlOrOptions === "string" ? copyOptions(maybeOptions) : copyOptions(urlOrOptions);
      if (typeof urlOrOptions === "string") {
        options.url = urlOrOptions;
      }
      if (String(options.url || "").indexOf("/WOM/produceTask/produceTask/updateTaskState") === -1) {
        return originalAjax.apply(this, arguments);
      }

      var originalSuccess = options.success;
      options.success = function updateTaskStateSuccess(response) {
        var returnValue;
        if (typeof originalSuccess === "function") {
          returnValue = originalSuccess.apply(this, arguments);
        }
        rememberUpdateTaskState(options.data, response);
        return returnValue;
      };
      return originalAjax.call(this, options);
    };
    patchedAjax.__adpWomMakeTaskStateSyncPatched = true;
    ajaxHost.ajax = patchedAjax;
  }

  installUpdateTaskStateSync();
  window.__ADP_WOM_MAKETASKLIST_PATCH_SELECTION_SYNC__ = patchRememberedRows;
  installMessageTranslationFallback();
  installVisibleTextFallback();
  installEmptySearchSelectFallback();
  [0, 80, 250, 750, 1500].forEach(function queueSelectionSync(delay) {
    window.setTimeout(patchRememberedRows, delay);
  });
  ["mousedown", "click", "keydown"].forEach(function blockEmptySearchSelect(eventName) {
    document.addEventListener(eventName, blockEmptySearchSelectEvent, true);
  });
  [0, 80, 250, 750, 1500].forEach(function queueEmptySearchSelectFallback(delay) {
    window.setTimeout(installEmptySearchSelectFallback, delay);
  });
  [80, 250, 800, 2000].forEach(function queueVisibleTextFallback(delay) {
    window.setTimeout(installVisibleTextFallback, delay);
  });
  window.setInterval(function installToolbarFallbacks() {
    patchRememberedRows();
    installMessageTranslationFallback();
    installVisibleTextFallback();
    installEmptySearchSelectFallback();
  }, 500);

  function processTraceUnavailable() {
    var row = selectedOne();
    if (!row) {
      return true;
    }
    if (systemCodeId(row.taskRunState) === "WOM_runState/waitForRun" || systemCodeId(row.taskRunState) === "待执行") {
      showWarning(messages.waitForRun);
      return true;
    }
    if (!dependencyEnabled("processTrace")) {
      showWarning(messages.processUnavailable);
      return true;
    }
    return false;
  }

  function qrcodeUnavailable() {
    var row = selectedOne();
    if (!row) {
      return true;
    }
    if (!dependencyEnabled("qrcode")) {
      showWarning(messages.qrcodeUnavailable);
      return true;
    }
    return false;
  }

  function replaceUnavailableDependencyButton(buttonId, guard) {
    var button = document.getElementById(buttonId);
    if (!button || button.getAttribute("data-adp-wom-dependency-guard") === "true") {
      return;
    }

    var replacement = button.cloneNode(true);
    replacement.setAttribute("data-adp-wom-dependency-guard", "true");
    replacement.addEventListener(
      "click",
      function guardedDependencyClick(event) {
        patchRememberedRows();
        guard();
        stopToolbarEvent(event);
      },
      true
    );
    button.parentNode.replaceChild(replacement, button);
  }

  function installUnavailableDependencyButtons() {
    if (!dependencyEnabled("processTrace")) {
      replaceUnavailableDependencyButton("btn-prodprocessView", processTraceUnavailable);
    }
    if (!dependencyEnabled("qrcode")) {
      replaceUnavailableDependencyButton("btn-generateCode", qrcodeUnavailable);
    }
  }

  document.addEventListener(
    "click",
    function guardUnavailableToolbarDependencies(event) {
      var target = event.target;
      var button =
        target &&
        target.closest &&
        target.closest(
          "#btn-startTask, #btn-pauseTask, #btn-recoveryTask, #btn-stopTask, #btn-earlyPutIn, #btn-manuInspect, #btn-prodprocessView, #btn-generateCode"
        );
      if (!button) {
        return;
      }

      patchRememberedRows();

      if (!guardToolbarSelection()) {
        stopToolbarEvent(event);
        return;
      }

      if (button.id !== "btn-prodprocessView" && button.id !== "btn-generateCode") {
        return;
      }

      var unavailable =
        button.id === "btn-prodprocessView"
          ? processTraceUnavailable()
          : qrcodeUnavailable();
      if (!unavailable) {
        return;
      }

      stopToolbarEvent(event);
    },
    true
  );

  installUnavailableDependencyButtons();
  window.setInterval(installUnavailableDependencyButtons, 500);
})();
