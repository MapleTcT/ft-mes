// Generated from platform-common-zh_CN.properties; do not edit by hand.
window.InternationalResource = window.InternationalResource || {};
window.InternationalResource["LIMSBasic.qualityStd.stdGrade.range"] = "范围";

(function installQCS_MANU_INSP_REPORT_VIEWI18nCompatibility(resources) {
  var patchFlag = "__adpModuleI18nPatched_QCS_MANU_INSP_REPORT_VIEW";
  var observerFlag = "__adpModuleI18nObserver_QCS_MANU_INSP_REPORT_VIEW";
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
