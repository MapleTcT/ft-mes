(function () {
  "use strict";

  var productTitle = "飞天生物制造执行系统";
  var styleHref = "/bap/static/adp-custom/homepage/style/index.css?v=20260729";

  function ensureStyle() {
    if (document.querySelector('link[data-feitian-homepage-branding="true"]')) return;
    var link = document.createElement("link");
    link.rel = "stylesheet";
    link.href = styleHref;
    link.setAttribute("data-feitian-homepage-branding", "true");
    document.head.appendChild(link);
  }

  function applyFeitianHomepageBranding() {
    document.title = productTitle;
    ensureStyle();

    var logo = document.getElementById("v3_logo");
    if (logo) {
      logo.setAttribute("role", "img");
      logo.setAttribute("aria-label", "飞天生物");
      logo.title = "飞天生物";
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", applyFeitianHomepageBranding);
  } else {
    applyFeitianHomepageBranding();
  }

  window.setTimeout(applyFeitianHomepageBranding, 1000);
})();
