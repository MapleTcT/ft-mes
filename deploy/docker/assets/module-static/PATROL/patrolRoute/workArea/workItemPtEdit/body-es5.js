"use strict";

(function loadPatrolItemEditorActions(document) {
  if (window.__ADP_PATROL_ITEM_EDITOR_ACTIONS_INSTALLED__) {
    return;
  }
  var script = document.createElement("script");
  script.src =
    "/greenDill/static/PATROL/patrolRoute/workArea/workItemPtEdit/body.js?v=" + Date.now();
  script.async = false;
  document.head.appendChild(script);
})(document);
