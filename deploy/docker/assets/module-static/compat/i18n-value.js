window.InternationalResource = window.InternationalResource || {};
window.InternationalResource["ec.common.tableNo"] = "单据编号";
window.InternationalResource["ec.common.createTime"] = "创建时间";
window.InternationalResource["ec.list.taskDescription"] = "待办说明";
window.InternationalResource["Button.text.cancel"] = "取消";
window.InternationalResource["Button.text.save"] = "保存";
window.InternationalResource["EditView.notice.processing"] = "处理中...";
window.InternationalResource["foundation.language.disable.success"] = "停用成功";
window.InternationalResource["foundation.language.enable.success"] = "启用成功";
window.InternationalResource["RM.buttonPropertyshowName.randon1573641411944.flag"] = "设置检验部门";
window.InternationalResource["RM.custom.dealSuccess"] = "处理成功！";
window.InternationalResource["RM.custom.formulaEdtionNotNull"] = "配方版本不能为空！";
window.InternationalResource["RM.custom.formulaNameNotNull"] = "配方标识不能为空！";
window.InternationalResource["RM.custom.notEffectiveFormulas"] = "配方必须处于生效状态才可设置检验部门！";
window.InternationalResource["RM.custom.onceOnlyOneRow"] = "只允许对一行进行操作！";
window.InternationalResource["RM.custom.random1643017683156"] = "配方未启用，无法设置默认！";
window.InternationalResource["RM.custom.randon1573717880503"] = "请选择一条记录进行操作！";
window.InternationalResource["RM.custom.randon1574755044078"] = "配方必须处于生效状态才可启用！";
window.InternationalResource["RM.custom.randon1574759597334"] = "存在单据已启用，不能重复启用！";
window.InternationalResource["RM.custom.randon1574759822999"] = "存在单据已停用，不能重复停用！";
window.InternationalResource["RM.custom.randon1574766713193"] = "配方必须处于生效状态才可设置默认！";
window.InternationalResource["RM.custom.randon1577945423614"] = "配方必须处于生效状态才可设置默认！";
window.InternationalResource["RM.custom.randon1602291826911"] = "配方必须处于生效状态才可停用！";
window.InternationalResource["RM.custom.randon1624507911528"] = "配方必须处于生效状态才可停用！";
window.InternationalResource["RM.custom.randon1624932897664"] = "配方未启用，不能停用！";
window.InternationalResource["RM.custom.stateNeedEnable"] = "配方必须处于启用状态才可添加适用产线！";
window.InternationalResource["RM.formula.Formula.productId,BaseSet.material.Material.code"] = "产品编码";
window.InternationalResource["RM.viewtitle.randon1573709509366"] = "检验部门设置";
window.InternationalResource["RM.viewtitle.randon1574822696971"] = "配方复制";

(function installCommonI18nCompatibility(resources) {
  var patchFlag = "__adpCommonI18nPatched";
  var observerFlag = "__adpCommonI18nObserver";
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
    var international =
      targetWindow.ReactAPI &&
      targetWindow.ReactAPI.international;
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
      international.getText = function getTextWithCommonFallback(key) {
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
    if (doc.body) translateSubtree(doc.body);
    var observerRoot = doc.documentElement || doc.body;
    if (!observerRoot || !targetWindow.MutationObserver || targetWindow[observerFlag]) return;
    targetWindow[observerFlag] = true;
    var observer = new targetWindow.MutationObserver(function translateMutations(mutations) {
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
