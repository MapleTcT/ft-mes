// Generated from platform-common-zh_CN.properties; do not edit by hand.
window.InternationalResource = window.InternationalResource || {};
window.InternationalResource["QCS.Inspect.operate.warn.selectProduct"] = "请先选择物料！";
window.InternationalResource["QCS.Inspect.operate.warn.productIsNotCited"] = "物料未被样品模板关联，无法参照！";
window.InternationalResource["LIMSBasic.viewtitle.randon1584520303249"] = "质量标准参照";
window.InternationalResource["Button.text.select"] = "选择";
window.InternationalResource["Reference.confirm.tip.message"] = "请至少选中一行！";
window.InternationalResource["Button.text.close"] = "关闭";
window.InternationalResource["QCS.Inspect.operate.success.addSuccessWithRepeatDataFilted"] = "添加成功，已过滤重复数据！";
window.InternationalResource["Reference.select.tip.success"] = "添加成功";
window.InternationalResource["QCS.inspect.operate.warn.selectOneStdData"] = "请先选择一条质量标准！";
window.InternationalResource["QCS.Inspect.operate.warn.inspStdDataNull"] = "质量标准表体不允许为空，请添加！";
window.InternationalResource["QCS.Inspect.operate.warn.inspStdVerComDataNull"] = "质量标准第[{0}]行没有检验项目数据，请至少勾选一条检验项目！";
window.InternationalResource["EditView.notice.operate.failure"] = "处理失败！";
window.InternationalResource["LIMSSample.sample.sampleCom.notAllowedToSubmit"] = "检测分项【{0}】多项结果未录入完成，不允许提交！";
window.InternationalResource["QCS.custom.random1622532409498"] = "未选择检验项目，是否确认跳过本次检验？";
window.InternationalResource["Button.text.ok"] = "确定";
window.InternationalResource["Button.text.cancel"] = "取消";
window.InternationalResource["EditView.notice.operate.success"] = "处理成功！";
window.InternationalResource["EditView.notice.processing"] = "处理中...";
window.InternationalResource["QCS.inspect.stdVerEdit"] = "标准维护";
window.InternationalResource["QCS.inspect.stdVerCopy"] = "标准复制";

(function installQCS_MANU_INSPECT_EDITI18nCompatibility(resources) {
  var patchFlag = "__adpModuleI18nPatched_QCS_MANU_INSPECT_EDIT";
  var observerFlag = "__adpModuleI18nObserver_QCS_MANU_INSPECT_EDIT";
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
