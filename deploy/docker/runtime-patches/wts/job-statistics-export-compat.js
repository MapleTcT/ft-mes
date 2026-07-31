(function installWtsJobStatisticsExport() {
  "use strict";

  var latestStatistics = null;
  var endpointMarker = "/WTS/jobStatistics/jobStatistics/getAllQueryData";
  var workTypeLabels = {
    "WTS_workType/heightWork": "高处安全作业",
    "WTS_workType/limitSpaceWork": "受限空间安全作业",
    "WTS_workType/electricityWork": "临时用电安全作业",
    "WTS_workType/fireWork": "动火安全作业",
    "WTS_workType/breakWork": "断路安全作业",
    "WTS_workType/liftWork": "吊装安全作业",
    "WTS_workType/blockWork": "盲板抽堵安全作业",
    "WTS_workType/soilWork": "动土安全作业"
  };

  function rememberPayload(payload) {
    if (payload && payload.code === 200 && payload.data) {
      latestStatistics = payload.data;
    }
  }

  function rememberResponse(xhr) {
    if (!xhr.__wtsStatisticsRequest || !xhr.responseText) {
      return;
    }
    try {
      rememberPayload(JSON.parse(xhr.responseText));
    } catch (ignore) {}
  }

  var originalOpen = XMLHttpRequest.prototype.open;
  XMLHttpRequest.prototype.open = function(method, url) {
    this.__wtsStatisticsRequest = String(url || "").indexOf(endpointMarker) >= 0;
    if (this.__wtsStatisticsRequest) {
      this.addEventListener("load", function() { rememberResponse(this); });
    }
    return originalOpen.apply(this, arguments);
  };

  if (window.fetch) {
    var originalFetch = window.fetch;
    window.fetch = function(input) {
      var url = typeof input === "string" ? input : input && input.url;
      return originalFetch.apply(this, arguments).then(function(response) {
        if (String(url || "").indexOf(endpointMarker) >= 0) {
          response.clone().json().then(rememberPayload).catch(function() {});
        }
        return response;
      });
    };
  }

  function csvCell(value) {
    var text = value === null || value === undefined ? "" : String(value);
    return '"' + text.replace(/"/g, '""') + '"';
  }

  function appendObjectRows(rows, title, value) {
    rows.push([]);
    rows.push([title]);
    if (!value || typeof value !== "object") {
      rows.push(["暂无数据"]);
      return;
    }
    if (Array.isArray(value)) {
      if (!value.length) {
        rows.push(["暂无数据"]);
        return;
      }
      var keys = [];
      value.forEach(function(item) {
        if (item && typeof item === "object") {
          Object.keys(item).forEach(function(key) {
            if (keys.indexOf(key) < 0) { keys.push(key); }
          });
        }
      });
      rows.push(keys);
      value.forEach(function(item) {
        rows.push(keys.map(function(key) { return item && item[key]; }));
      });
      return;
    }
    var objectKeys = Object.keys(value);
    if (!objectKeys.length) {
      rows.push(["暂无数据"]);
      return;
    }
    rows.push(["项目", "数值"]);
    objectKeys.forEach(function(key) {
      var item = value[key];
      rows.push([
        workTypeLabels[key] || key,
        item && typeof item === "object" ? JSON.stringify(item) : item
      ]);
    });
  }

  function buildCsv(data) {
    var rows = [["作业统计"], ["导出时间", new Date().toLocaleString("zh-CN")]];
    var total = data.totalWorkNum || {};
    rows.push([]);
    rows.push(["作业总览"]);
    rows.push(["本时段作业总数", "新增作业总数", "异常封票", "异常占比"]);
    rows.push([total.totalNum, total.newNum, total.abnormalNum, total.abnormalRatio]);

    appendObjectRows(rows, "作业类型", data.difWorkNum);

    var daily = data.everyDayWorkNum || {};
    rows.push([]);
    rows.push(["每日开票/封票"]);
    rows.push(["日期", "开票量", "封票量"]);
    var dates = daily.xAxisData || [];
    var series = daily.seriesList || [];
    var opened = series[0] && series[0].data || [];
    var closed = series[1] && series[1].data || [];
    dates.forEach(function(date, index) {
      rows.push([date, opened[index], closed[index]]);
    });

    rows.push([]);
    rows.push(["部门作业一览表"]);
    rows.push(["部门", "开票", "封票", "异常", "执行中"]);
    (data.dataByDept || []).forEach(function(item) {
      rows.push([
        item.name,
        item.timeNum,
        item.effectNum,
        item.abnormalNum,
        item.inExecutionNum
      ]);
    });

    appendObjectRows(rows, "作业区域排行", data.dataByWorkArea);
    appendObjectRows(rows, "外来承包商统计", data.dataContractor);
    appendObjectRows(rows, "承包商作业人员统计", data.dataContractorPeople);
    return "\ufeff" + rows.map(function(row) {
      return row.map(csvCell).join(",");
    }).join("\r\n");
  }

  function downloadStatistics() {
    if (!latestStatistics) {
      window.alert("统计数据尚未加载，请先点击查询后再导出");
      return;
    }
    var blob = new Blob([buildCsv(latestStatistics)], {
      type: "text/csv;charset=utf-8"
    });
    var href = window.URL.createObjectURL(blob);
    var link = document.createElement("a");
    link.href = href;
    link.download = "WTS_job_statistics_" + new Date().toISOString().slice(0, 10) + ".csv";
    link.style.display = "none";
    document.body.appendChild(link);
    link.click();
    window.setTimeout(function() {
      window.URL.revokeObjectURL(href);
      if (link.parentNode) { link.parentNode.removeChild(link); }
    }, 1000);
  }

  function normalizedText(element) {
    return String(element && (element.innerText || element.textContent) || "")
      .replace(/\s+/g, "")
      .trim();
  }

  function installButton() {
    if (document.querySelector("[data-wts-statistics-export='true']")) {
      return true;
    }
    var buttons = Array.prototype.slice.call(document.querySelectorAll("button"));
    var clearButton = buttons.filter(function(button) {
      return normalizedText(button) === "清空";
    })[0];
    if (!clearButton || !clearButton.parentNode) {
      return false;
    }
    var exportButton = document.createElement("button");
    exportButton.type = "button";
    exportButton.className = clearButton.className;
    exportButton.setAttribute("data-wts-statistics-export", "true");
    exportButton.style.marginLeft = "8px";
    exportButton.textContent = "导出";
    exportButton.addEventListener("click", downloadStatistics);
    clearButton.parentNode.insertBefore(exportButton, clearButton.nextSibling);
    return true;
  }

  var observer = new MutationObserver(function() {
    if (installButton()) { observer.disconnect(); }
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  window.addEventListener("load", installButton);
  installButton();
})();
