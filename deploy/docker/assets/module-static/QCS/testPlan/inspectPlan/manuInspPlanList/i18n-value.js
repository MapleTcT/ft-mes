// Generated from platform-common-zh_CN.properties; do not edit by hand.
window.InternationalResource = window.InternationalResource || {};
window.InternationalResource["SupDatagrid.button.error"] = "请选择一条记录进行操作！";
window.InternationalResource["SupDatagrid.button.tit"] = "提示";
window.InternationalResource["foundation.data.cross.company"] = "第{0}行数据为非本公司数据，无法操作！";
window.InternationalResource["LIMSBasic.viewdisplayName.randon1614081910119"] = "检测频率参照";
window.InternationalResource["Button.text.select"] = "选择";
window.InternationalResource["Reference.confirm.tip.message"] = "请至少选中一行！";
window.InternationalResource["Button.text.close"] = "关闭";
window.InternationalResource["Button.text.cancel"] = "取消";
window.InternationalResource["Button.text.save"] = "保存";
window.InternationalResource["LIMSBasic.viewtitle.randon1614163932466"] = "设置检测日期";
window.InternationalResource["EditView.notice.operate.success"] = "处理成功！";
window.InternationalResource["Notification.message.title.info"] = "提示";
window.InternationalResource["LIMSBasic.viewtitle.randon1615774477306"] = "设置已跳批";
window.InternationalResource["LIMSBasic.custom.countNumberRangeError"] = "{0}值不正确，最小值为{1}";
window.InternationalResource["LIMSBasic.selectedRowsAreNotBatch"] = "请选择频率为按批的记录进行操作！";
window.InternationalResource["QCS.custom.random1624424081281"] = "请选择频率类型为按间隔且按批的记录进行操作！";

(function installQCS_MANU_INSP_PLAN_LISTI18nCompatibility(resources) {
  var patchFlag = "__adpModuleI18nPatched_QCS_MANU_INSP_PLAN_LIST";
  var observerFlag = "__adpModuleI18nObserver_QCS_MANU_INSP_PLAN_LIST";
  var attempts = 0;

  function accessibleWindows() {
    var windows = [];
    function add(candidate) {
      try {
        if (candidate && candidate.document && windows.indexOf(candidate) === -1) {
          windows.push(candidate);
        }
      } catch (_error) {}
    }
    add(window);
    add(window.parent);
    add(window.top);
    return windows;
  }

  function patchInternational(targetWindow) {
    var reactApi = targetWindow.ReactAPI;
    var international = reactApi && reactApi.international;
    if (!international || typeof international.getText !== "function") {
      return false;
    }
    if (typeof international.getLanguageObjData === "function") {
      var languageData = international.getLanguageObjData();
      if (languageData && typeof languageData === "object") {
        Object.keys(resources).forEach(function copyResource(key) {
          languageData[key] = resources[key];
        });
      }
    }
    if (!international[patchFlag]) {
      var originalGetText = international.getText.bind(international);
      international.getText = function getTextWithModuleFallback(key) {
        var value = originalGetText.apply(null, arguments);
        if ((value === key || value === "" || value == null) && resources[key] != null) {
          return resources[key];
        }
        return value;
      };
      international[patchFlag] = true;
    }
    return true;
  }

  function translateTextNode(node) {
    var original = node && node.nodeValue;
    if (typeof original !== "string") return;
    var trimmed = original.trim();
    if (resources[trimmed] == null) return;
    node.nodeValue = original.replace(trimmed, resources[trimmed]);
  }

  function translateDocumentTitle(targetWindow) {
    var doc = targetWindow && targetWindow.document;
    if (!doc || resources[doc.title] == null) return;
    doc.title = resources[doc.title];
  }

  function translateSubtree(root) {
    if (!root || !root.ownerDocument || !root.ownerDocument.createTreeWalker) return;
    var doc = root.ownerDocument;
    var targetWindow = doc.defaultView || window;
    var nodeFilter = targetWindow.NodeFilter || window.NodeFilter;
    if (!nodeFilter) return;
    var walker = doc.createTreeWalker(root, nodeFilter.SHOW_TEXT, null, false);
    var node;
    while ((node = walker.nextNode())) {
      var parent = node.parentNode;
      if (!parent || /^(SCRIPT|STYLE|TEXTAREA|INPUT)$/i.test(parent.nodeName || "")) continue;
      translateTextNode(node);
    }
  }

  function installObserver(targetWindow) {
    var doc = targetWindow.document;
    if (!doc) return;
    translateDocumentTitle(targetWindow);
    if (doc.body) translateSubtree(doc.body);
    var observerRoot = doc.documentElement || doc.body;
    if (!observerRoot) return;
    if (!targetWindow.MutationObserver || targetWindow[observerFlag]) return;
    targetWindow[observerFlag] = true;
    var observer = new targetWindow.MutationObserver(function translateMutations(mutations) {
      translateDocumentTitle(targetWindow);
      mutations.forEach(function translateMutation(mutation) {
        if (mutation.type === "characterData") {
          translateTextNode(mutation.target);
          return;
        }
        Array.prototype.forEach.call(mutation.addedNodes || [], function translateAdded(node) {
          if (node.nodeType === 3) translateTextNode(node);
          if (node.nodeType === 1) translateSubtree(node);
        });
      });
    });
    observer.observe(observerRoot, { childList: true, subtree: true, characterData: true });
  }

  function install() {
    var patched = false;
    accessibleWindows().forEach(function installWindow(targetWindow) {
      patched = patchInternational(targetWindow) || patched;
      installObserver(targetWindow);
    });
    attempts += 1;
    return patched || attempts >= 100;
  }

  if (!install()) {
    var timer = window.setInterval(function retryInstall() {
      if (install()) window.clearInterval(timer);
    }, 100);
  }
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", install);
  }
})(window.InternationalResource);
