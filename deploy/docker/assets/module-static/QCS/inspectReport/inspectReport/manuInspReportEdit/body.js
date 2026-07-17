//检验结果回调判断
function sampleComRefCallback(data, event) {
	var reportComDg = ReactAPI.getComponentAPI("SupDataGrid").APIs("QCS_5.0.0.0_inspectReport_manuInspReportEditdg1591145511105");

	var selData = reportComDg.getSelecteds()[0];

	reportComDg.setRowData(selData.rowIndex, {dispValue: data[0].dispValue, valueSrcId: data[0].id, procedureNo: data[0].sampleTestId.testMethodId.procedureNo});

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
			var values = (reference && reference.getValue && reference.getValue()) || [];
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
