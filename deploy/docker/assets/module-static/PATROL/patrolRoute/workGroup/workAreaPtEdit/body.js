"use strict";

(function installPatrolAreaEditorActions(window, document) {
  if (window.__ADP_PATROL_AREA_EDITOR_ACTIONS_INSTALLED__) {
    return;
  }
  window.__ADP_PATROL_AREA_EDITOR_ACTIONS_INSTALLED__ = true;

  var AREA_GRID = "PATROL_1.0.0_patrolRoute_workAreaPtEditdg1575630363211";

  function areaGrid() {
    return ReactAPI.getComponentAPI("SupDataGrid").APIs(AREA_GRID);
  }

  function selectedRows() {
    return areaGrid().getSelecteds();
  }

  function requireSelectedRow(single) {
    var rows = selectedRows();
    if (!rows.length) {
      ReactAPI.showMessage("w", ReactAPI.international.getText("ec.common.checkselected"));
      return null;
    }
    if (single && rows.length > 1) {
      ReactAPI.showMessage("w", ReactAPI.international.getText("SupDatagrid.button.error"));
      return null;
    }
    return rows;
  }

  function closeMoreMenu(button) {
    var moreButton = button.closest("#btn-more");
    if (moreButton) {
      var menuTrigger = moreButton.querySelector(".sup-datagrid-button-item");
      window.setTimeout(function closeMenuAfterCurrentClick() {
        (menuTrigger || moreButton).click();
      }, 0);
    }
  }

  function addAreaRow() {
    var table = areaGrid();
    table.addLine();
    var rows = table.getDatagridData();
    var rowIndex = rows.length - 1;
    var params = ReactAPI.getParamsInRequestUrl(window.location.href);
    if (params && params.id) {
      table.setValueByKey(rowIndex, "workGroupId.id", params.id);
    }
  }

  function deleteAreaRows() {
    var rows = requireSelectedRow(false);
    if (!rows) {
      return;
    }
    var rowIndexes = rows.map(function mapRowIndex(row) {
      return row.rowIndex;
    });
    areaGrid().deleteLine(rowIndexes.join(","));
  }

  function moveAreaRow(direction) {
    if (!requireSelectedRow(true)) {
      return;
    }
    if (direction === "up") {
      areaGrid().moveUpLine();
    } else {
      areaGrid().moveDownLine();
    }
  }

  document.addEventListener(
    "click",
    function onAreaEditorAction(event) {
      var target = event.target;
      var button =
        target && target.closest
          ? target.closest("#btn-add, #btn-delete, #btn-moveUp, #btn-moveDown")
          : null;
      if (!button) {
        return;
      }

      event.preventDefault();
      event.stopImmediatePropagation();
      closeMoreMenu(button);
      if (button.id === "btn-add") {
        addAreaRow();
      } else if (button.id === "btn-delete") {
        deleteAreaRows();
      } else if (button.id === "btn-moveUp") {
        moveAreaRow("up");
      } else if (button.id === "btn-moveDown") {
        moveAreaRow("down");
      }
    },
    true
  );
})(window, document);
