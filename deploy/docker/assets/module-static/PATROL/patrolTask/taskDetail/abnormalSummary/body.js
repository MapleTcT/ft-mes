"use strict";

(function installPatrolHiddenDangerAction(root, document) {
  if (root.__ADP_PATROL_HIDDEN_DANGER_ACTION_INSTALLED__) {
    return;
  }
  root.__ADP_PATROL_HIDDEN_DANGER_ACTION_INSTALLED__ = true;

  var GRID_CODE = "PATROL_1.0.0_patrolTask_abnormalSummary_taskDetail_sdg";
  var CREATE_URL = "/msService/PATROL/patrolTask/taskDetail/createHiddenDanger";
  var PAGE_PATH = "/msService/PATROL/patrolTask/taskDetail/abnormalSummary";

  function text(key) {
    return root.ReactAPI.international.getText.apply(root.ReactAPI.international, arguments);
  }

  function grid() {
    return root.ReactAPI.getComponentAPI("SupDataGrid").APIs(GRID_CODE);
  }

  function refreshGrid() {
    var queryButton = document.querySelector("button[data-id='query']");
    if (queryButton) {
      queryButton.click();
      return;
    }
    grid().refreshDataByRequst();
  }

  function showFailure(response) {
    var message =
      response && response.responseJSON && response.responseJSON.message
        ? response.responseJSON.message
        : text("greendill.show.title.500");
    root.ReactAPI.showMessage("f", message);
  }

  function postHiddenDanger(ids) {
    var params = { ids: ids };
    var onSuccess = function onHiddenDangerCreated(response) {
      if (response && response.code === 200) {
        root.ReactAPI.showMessage(
          "s",
          response.message || text("EditView.notice.operate.success")
        );
        refreshGrid();
        return;
      }
      root.ReactAPI.showMessage(
        "f",
        (response && response.message) || text("greendill.show.title.500")
      );
    };

    if (typeof root._postAsyncWithLoading === "function") {
      root._postAsyncWithLoading(
        CREATE_URL,
        params,
        onSuccess,
        showFailure,
        text("PATROL.custom.danger.creating")
      );
      return;
    }

    root.ReactAPI.openLoading(text("PATROL.custom.danger.creating"));
    root
      .fetch(CREATE_URL, {
        method: "POST",
        credentials: "include",
        headers: {
          Accept: "application/json, text/plain, */*",
          "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
          "X-Requested-With": "XMLHttpRequest",
        },
        body: new URLSearchParams(params).toString(),
      })
      .then(function parseResponse(response) {
        if (!response.ok) {
          throw new Error("HTTP " + response.status);
        }
        return response.json();
      })
      .then(onSuccess)
      .catch(showFailure)
      .finally(function closeLoading() {
        root.ReactAPI.closeLoading();
      });
  }

  function createHiddenDanger() {
    var selected = grid().getSelecteds();
    var ids = [];
    var index;
    var row;

    if (!selected.length) {
      root.ReactAPI.showMessage("w", text("Reference.confirm.tip.message"));
      return false;
    }
    if (typeof root.checkEditPower === "function" && !root.checkEditPower(selected)) {
      return false;
    }

    for (index = 0; index < selected.length; index += 1) {
      row = selected[index];
      if (row.isFault === true) {
        root.ReactAPI.showMessage(
          "w",
          text("PATROL.custom.danger.created", Number(row.rowIndex) + 1)
        );
        return false;
      }
      ids.push(row.id);
    }

    root.ReactAPI.openConfirm({
      message: text("PATROL.custom.danger.isCreated"),
      buttons: [
        {
          operatetype: "yes",
          text: text("calendar.common.check"),
          type: "primary",
          onClick: function confirmHiddenDanger() {
            root.ReactAPI.closeConfirm();
            postHiddenDanger(ids.join(",") + ",");
          },
        },
        {
          operatetype: "no",
          text: text("ec.common.cancel"),
          onClick: function cancelHiddenDanger() {
            root.ReactAPI.closeConfirm();
          },
        },
      ],
    });
    return true;
  }

  root.createTask = createHiddenDanger;
  document.addEventListener(
    "click",
    function onCreateHiddenDangerClick(event) {
      var target = event.target;
      var button = target && target.closest ? target.closest("#btn-createTask") : null;
      if (!button || root.location.pathname !== PAGE_PATH) {
        return;
      }
      event.preventDefault();
      event.stopImmediatePropagation();
      createHiddenDanger();
    },
    true
  );
})(window, document);
