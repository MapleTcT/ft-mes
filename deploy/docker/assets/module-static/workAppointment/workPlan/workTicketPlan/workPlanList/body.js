(function () {
  var operateCode = "workPlanList_add_add_workAppointment_6.1.6.1_workPlan_workPlanList";
  var buttonCode = "workAppointment_6.1.6.1_workPlan_workPlanList_BUTTON_add";
  var dialogName = "wapsWorkPlanEditDialog";
  var deploymentId = "6579649925021696";
  var activityName = "start_glvw15o";

  function text(key, fallback) {
    if (window.ReactAPI && ReactAPI.international && ReactAPI.international.getText) {
      var value = ReactAPI.international.getText(key);
      return value || fallback;
    }
    return fallback;
  }

  function refreshList() {
    try {
      var dg = ReactAPI.getComponentAPI("SupDataGrid").APIs(
        "workAppointment_6.1.6.1_workPlan_workPlanList_workTicketPlan_sdg"
      );
      if (dg && dg.refreshDataByRequst) {
        dg.refreshDataByRequst({
          type: "post",
          url: "/msService/workAppointment/workPlan/workTicketPlan/workPlanList-query",
          param: {
            classifyCodes: "",
            customCondition: {},
            datagridCode: "workAppointment_6.1.6.1_workPlan_workPlanList",
            permissionCode: "workAppointment_6.1.6.1_workPlan_workPlanList",
            pageNo: 1,
            paging: true,
            pageSize: 20,
            crossCompanyFlag: "true"
          }
        });
      }
    } catch (error) {
      if (window.console) {
        console.warn("WorkAppointment work plan list refresh skipped", error);
      }
    }
  }

  function openAddDialog(powerCode) {
    if (!window.ReactAPI || !ReactAPI.createDialog) {
      return;
    }

    ReactAPI.createDialog(dialogName, {
      title: "作业计划编辑",
      size: 5,
      url:
        "/msService/workAppointment/workPlan/workTicketPlan/workPlanEdit?__pc__=" +
        encodeURIComponent(powerCode || "") +
        "&viewCode=workAppointment_6.1.6.1_workPlan_workPlanList" +
        "&entityCode=workAppointment_6.1.6.1_workPlan" +
        "&deploymentId=" +
        encodeURIComponent(deploymentId) +
        "&activityName=" +
        encodeURIComponent(activityName) +
        "&iscrosscompany=false&openType=dialog&buttonCode=" +
        buttonCode +
        "&iscallback=true",
      onOk: function (event) {
        event.ReactAPI.submitFormData("save", function (result) {
          if (!result || !result.data || result.data.dealSuccessFlag !== false) {
            ReactAPI.destroyDialog(dialogName);
            refreshList();
          }
        });
      },
      okText: "保存",
      onCancel: function () {
        ReactAPI.destroyDialog(dialogName);
      }
    });
  }

  function handleAddClick(event) {
    event.preventDefault();
    event.stopPropagation();
    if (event.stopImmediatePropagation) {
      event.stopImmediatePropagation();
    }

    if (window.ReactAPI && ReactAPI.getPowerCode) {
      ReactAPI.getPowerCode(operateCode, function (result) {
        openAddDialog(result && result[operateCode]);
      });
    } else {
      openAddDialog("");
    }
  }

  function bindAddButton() {
    var button = document.querySelector("#btn-add .sup-datagrid-button-item");
    if (!button || button.getAttribute("data-adp-waps-add-bound") === "true") {
      return Boolean(button);
    }

    button.setAttribute("data-adp-waps-add-bound", "true");
    button.onclick = null;
    button.addEventListener("click", handleAddClick, true);
    return true;
  }

  function bindWhenReady() {
    if (!bindAddButton()) {
      setTimeout(bindWhenReady, 500);
    }
  }

  bindWhenReady();
})();
