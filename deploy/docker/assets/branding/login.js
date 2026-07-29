(function () {
  "use strict";

  var productTitle = "飞天生物制造执行系统";

  function ensureViewport() {
    var viewport = document.querySelector('meta[name="viewport"]');
    if (!viewport) {
      viewport = document.createElement("meta");
      viewport.name = "viewport";
      document.head.appendChild(viewport);
    }
    viewport.content = "width=device-width, initial-scale=1";
  }

  function createElement(className, text) {
    var element = document.createElement("div");
    element.className = className;
    if (text) element.textContent = text;
    return element;
  }

  function applyFeitianLoginBranding() {
    document.title = productTitle;

    var copyright = document.getElementById("login-copyright");
    if (copyright) {
      copyright.textContent = "河南飞天生物科技股份有限公司 版权所有";
    }

    var logo = document.querySelector("#logo img");
    if (logo) {
      logo.alt = "飞天生物";
      logo.setAttribute("aria-label", "飞天生物");
    }

    var slogan = document.getElementById("slogan");
    if (!slogan) return;

    var sloganLogo = slogan.querySelector("img");
    if (sloganLogo) {
      sloganLogo.alt = "飞天生物";
      sloganLogo.setAttribute("aria-label", "飞天生物");
    }

    if (!slogan.querySelector(".feitian-login-system-title")) {
      var accent = createElement("feitian-login-accent");
      accent.setAttribute("aria-hidden", "true");
      accent.appendChild(document.createElement("span"));
      accent.appendChild(document.createElement("span"));
      accent.appendChild(document.createElement("span"));
      slogan.appendChild(accent);
      slogan.appendChild(createElement("feitian-login-system-title", "智能制造执行系统"));
      slogan.appendChild(
        createElement("feitian-login-system-subtitle", "MANUFACTURING EXECUTION SYSTEM")
      );
    }
  }

  ensureViewport();

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", applyFeitianLoginBranding);
  } else {
    applyFeitianLoginBranding();
  }
})();
