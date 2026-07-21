//检验结果回调判断
function sampleComRefCallback(data, event) {
	var reportComDg = ReactAPI.getComponentAPI("SupDataGrid").APIs("QCS_5.0.0.0_inspectReport_manuInspReportEditdg1591145511105");

	var selData = reportComDg.getSelecteds()[0];

	reportComDg.setRowData(selData.rowIndex, { dispValue: data[0].dispValue, valueSrcId: data[0].id, procedureNo: data[0].sampleTestId.testMethodId.procedureNo });

	//调用公用方法
	dispValueChange(reportComDg, data[0].dispValue, selData.rowIndex);
	ReactAPI.destroyDialog("sampleComRefDialog");
}

(function installQcsBadQuantityEntry() {
	if (window.__adpQcsBadQuantityEntry) {
		return;
	}
	window.__adpQcsBadQuantityEntry = true;

	function showWarning(message) {
		if (window.ReactAPI && typeof window.ReactAPI.showMessage === "function") {
			window.ReactAPI.showMessage("w", message);
			return;
		}
		window.alert(message);
	}

	function currentInspectId() {
		try {
			var reference = window.ReactAPI.getComponentAPI("Reference")
				.APIs("inspectReport.inspectId.tableNo");
			var values = reference && reference.getValue && reference.getValue() || [];
			return values[0] && values[0].id ? String(values[0].id) : "";
		} catch (_error) {
			return "";
		}
	}

	function openBadQuantityReport(event) {
		if (event) {
			event.preventDefault();
			event.stopPropagation();
		}
		var inspectId = currentInspectId();
		if (!inspectId) {
			showWarning("请先选择并保存检验申请单！");
			return false;
		}
		var opened = window.open(
			"/msService/WOM/quality-quantity/page?inspectId=" + encodeURIComponent(inspectId),
			"_blank"
		);
		if (!opened) {
			showWarning("不良数量登记页面未部署或暂不可用！");
		}
		return false;
	}

	function installButton() {
		var button = document.getElementById("btn-badQuantityReportQcs");
		if (!button || button.getAttribute("data-adp-qcs-bad-quantity") === "true") {
			return;
		}
		var replacement = button.cloneNode(true);
		replacement.removeAttribute("onclick");
		replacement.setAttribute("data-adp-qcs-bad-quantity", "true");
		replacement.addEventListener("click", openBadQuantityReport, true);
		button.parentNode.replaceChild(replacement, button);
	}

	window.adpOpenQcsBadQuantityReport = openBadQuantityReport;
	installButton();
	window.setInterval(installButton, 500);
}());

(function installQcsPersistedConclusionRestore() {
	if (window.__adpQcsPersistedConclusionRestore) {
		return;
	}
	var queryString = window.location.search || "";
	if (!/(?:^|[?&])id(?:=|&|$)/.test(queryString) || /(?:^|[?&])pendingId(?:=|&|$)/.test(queryString)) {
		return;
	}
	window.__adpQcsPersistedConclusionRestore = true;

	var attempts = 0;
	var maxAttempts = 300;
	var cachedResult = "";
	var cachedGradeId = "";
	var fallbackGradeIds = {
		"合格": "LIMSBasic_standardGrade/Qualified",
		"不合格": "LIMSBasic_standardGrade/Unqualified"
	};

	function componentValue(value) {
		if (!value) {
			return "";
		}
		if (typeof value === "string") {
			return value;
		}
		return value.value || value.id || "";
	}

	function gradeIdFor(result) {
		try {
			if (window.levelMap && typeof window.levelMap.get === "function") {
				var sort = window.levelMap.get(result);
				var gradeId = window.levelMap.get(sort);
				if (gradeId) {
					return gradeId;
				}
			}
		} catch (_error) {
			// The quality-standard map may still be loading.
		}
		return fallbackGradeIds[result] || "";
	}

	function ensureConclusionOption(selector, gradeId, result) {
		if (!selector || !gradeId || !result || typeof selector.addOption !== "function") {
			return false;
		}
		var option = null;
		if (typeof selector.getOptionById === "function") {
			option = selector.getOptionById(gradeId);
		}
		if (option && option.label === result) {
			return false;
		}
		selector.addOption(gradeId, result);
		return true;
	}

	function restorePersistedConclusion() {
		attempts += 1;
		try {
			var input = ReactAPI.getComponentAPI("Input").APIs("inspectReport.checkResult");
			var selector = ReactAPI.getComponentAPI("SystemCode").APIs("inspectReport.checkResOption");
			var result = input && input.getValue ? componentValue(input.getValue()) : "";
			var selected = selector && selector.getValue ? componentValue(selector.getValue()) : "";
			if (result) {
				cachedResult = result;
				cachedGradeId = gradeIdFor(result) || cachedGradeId;
			}
			if (selected) {
				cachedGradeId = selected;
			}
			if (!cachedResult) {
				return false;
			}

			var gradeId = cachedGradeId || gradeIdFor(cachedResult);
			var restoredInput = false;
			var restoredSelector = false;
			var restoredOption = false;
			if (!result && input && typeof input.setValue === "function") {
				input.setValue(cachedResult);
				restoredInput = true;
			}
			if (!selected && gradeId && selector && typeof selector.setValue === "function") {
				restoredOption = ensureConclusionOption(selector, gradeId, cachedResult);
				selector.setValue(gradeId);
				restoredSelector = true;
				if (typeof window.onHeadResultChange === "function") {
					window.onHeadResultChange(gradeId);
				}
			}
			if (input && input.getValue && !componentValue(input.getValue()) && typeof input.setValue === "function") {
				input.setValue(cachedResult);
				restoredInput = true;
			}
			window.__ADP_QCS_CONCLUSION_RESTORED__ = {
				result: cachedResult,
				gradeId: gradeId,
				attempt: attempts,
				restoredInput: restoredInput,
				restoredOption: restoredOption,
				restoredSelector: restoredSelector
			};
			return restoredInput || restoredSelector;
		} catch (_error) {
			return false;
		}
	}

	restorePersistedConclusion();
	var timer = window.setInterval(function retryPersistedConclusion() {
		restorePersistedConclusion();
		if (attempts >= maxAttempts) {
			window.clearInterval(timer);
		}
	}, 100);
}());
