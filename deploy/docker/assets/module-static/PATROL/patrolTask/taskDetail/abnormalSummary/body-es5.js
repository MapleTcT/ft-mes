"use strict";

(function loadPatrolHiddenDangerAction(document) {
  var script = document.createElement("script");
  script.src =
    "/greenDill/static/PATROL/patrolTask/taskDetail/abnormalSummary/body.js?v=" +
    Date.now();
  script.async = false;
  document.head.appendChild(script);
})(document);
