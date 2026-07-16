"use strict";

(function installPatrolRouteActions(window, document) {
  if (window.__ADP_PATROL_ROUTE_ACTIONS_INSTALLED__) {
    return;
  }
  window.__ADP_PATROL_ROUTE_ACTIONS_INSTALLED__ = true;

  var ROUTE_GRID = "PATROL_1.0.0_patrolRoute_workGroupListdg1575506219664";
  var AREA_GRID = "PATROL_1.0.0_patrolRoute_workGroupListdg1575506226708";
  var ITEM_GRID = "PATROL_1.0.0_patrolRoute_workGroupListdg1575507309041";

  function text(key) {
    return ReactAPI.international.getText.apply(ReactAPI.international, arguments);
  }

  function grid(code) {
    return ReactAPI.getComponentAPI("SupDataGrid").APIs(code);
  }

  function selectedRows(code) {
    return grid(code).getSelecteds();
  }

  function canEdit(rows) {
    if (!rows.length) {
      ReactAPI.showMessage("w", text("ec.common.checkselected"));
      return false;
    }
    return typeof window.checkEditPower !== "function" || window.checkEditPower(rows);
  }

  function showRequestError() {
    ReactAPI.showMessage("f", text("greendill.show.title.500"));
  }

  function refresh(code, request) {
    if (request) {
      grid(code).refreshDataByRequst(request);
    } else {
      grid(code).refreshDataByRequst();
    }
  }

  function updateState(gridCode, tableType, itemState, refreshRequest) {
    var rows = selectedRows(gridCode);
    if (!canEdit(rows)) {
      return;
    }

    var ids = rows.map(function mapId(row) {
      return row.id;
    });
    ReactAPI.request(
      {
        type: "get",
        url:
          "/msService/PATROL/publicItem/publicItem/updateItemState?itemIds=" +
          ids.join(",") +
          "&itemState=" +
          itemState +
          "&tableType=" +
          tableType,
      },
      function onStateUpdated(response) {
        if (response && response.code === 200 && response.data && response.data.dealFlag === true) {
          refresh(gridCode, refreshRequest);
          ReactAPI.showMessage(
            "s",
            text(
              itemState === 1
                ? "foundation.language.enable.success"
                : "foundation.language.disable.success"
            )
          );
          return;
        }
        showRequestError();
      }
    );
  }

  function deleteRoute() {
    var rows = selectedRows(ROUTE_GRID);
    if (!canEdit(rows)) {
      return;
    }

    var routeIds = rows.map(function mapId(row) {
      return row.id;
    });
    var routeList = rows.map(function mapRoute(row, index) {
      return { index: index, routeId: row.id };
    });
    var relatedNames = [];
    var relationCheckPassed = true;

    ReactAPI.request(
      {
        type: "post",
        url: "/msService/PATROL/patrolRoute/workGroup/checkRelationPlan",
        async: false,
        data: routeList,
      },
      function onRelationChecked(response) {
        if (!response || response.code !== 200) {
          relationCheckPassed = false;
          showRequestError();
          return;
        }
        (response.data || []).forEach(function collectRelated(item) {
          if (rows[item.index]) {
            relatedNames.push(rows[item.index].name);
          }
        });
      }
    );

    if (!relationCheckPassed) {
      return;
    }
    if (relatedNames.length) {
      ReactAPI.showMessage(
        "f",
        text("PATROL.custom.randon1611024814055", relatedNames.join(","))
      );
      return;
    }

    ReactAPI.openConfirm({
      message: text("PATROL.custom.randon1578725549579"),
      buttons: [
        {
          operatetype: "yes",
          text: text("calendar.common.check"),
          type: "primary",
          onClick: function confirmDeleteRoute() {
            ReactAPI.closeConfirm();
            ReactAPI.openLoading(text("EditView.notice.processing"));
            ReactAPI.request(
              {
                type: "get",
                url:
                  "/msService/PATROL/patrolRoute/workGroup/deleteWorkGroups?routeIds=" +
                  routeIds.join(","),
              },
              function onRouteDeleted(response) {
                ReactAPI.closeLoading();
                if (response && response.code === 200) {
                  ReactAPI.showMessage("s", text("EditView.notice.operate.success"));
                  refresh(ROUTE_GRID);
                  refresh(AREA_GRID);
                  refresh(ITEM_GRID);
                  return;
                }
                showRequestError();
              }
            );
          },
        },
        {
          operatetype: "no",
          text: text("calendar.common.cancal"),
          type: "primary",
          onClick: function cancelDeleteRoute() {
            ReactAPI.closeConfirm();
          },
        },
      ],
    });
  }

  function deleteArea() {
    var rows = selectedRows(AREA_GRID);
    if (!canEdit(rows)) {
      return;
    }
    var areaIds = rows.map(function mapId(row) {
      return row.id;
    });

    ReactAPI.openConfirm({
      message: text("PATROL.custom.randon1578730253444"),
      buttons: [
        {
          operatetype: "yes",
          text: text("calendar.common.check"),
          type: "primary",
          onClick: function confirmDeleteArea() {
            ReactAPI.closeConfirm();
            ReactAPI.openLoading(text("EditView.notice.processing"));
            ReactAPI.request(
              {
                type: "get",
                url:
                  "/msService/PATROL/patrolRoute/workArea/deleteWorkAreas?workAreaIds=" +
                  areaIds.join(","),
              },
              function onAreaDeleted(response) {
                ReactAPI.closeLoading();
                if (response && response.code === 200) {
                  ReactAPI.showMessage("s", text("EditView.notice.operate.success"));
                  refresh(AREA_GRID);
                  refresh(ITEM_GRID);
                  return;
                }
                showRequestError();
              }
            );
          },
        },
        {
          operatetype: "no",
          text: text("calendar.common.cancal"),
          type: "primary",
          onClick: function cancelDeleteArea() {
            ReactAPI.closeConfirm();
          },
        },
      ],
    });
  }

  function openAreaEditor() {
    var rows = selectedRows(ROUTE_GRID);
    if (!canEdit(rows)) {
      return;
    }
    if (rows.length > 1) {
      ReactAPI.showMessage("w", text("SupDatagrid.button.error"));
      return;
    }

    ReactAPI.getPowerCode(
      "workGroupList_workAreaSet_add_PATROL_1.0.0_patrolRoute_workGroupList",
      function onPowerCode(response) {
        var powerCode =
          response[
            "workGroupList_workAreaSet_add_PATROL_1.0.0_patrolRoute_workGroupList"
          ];
        ReactAPI.createDialog("newDialog", {
          title: text("PATROL.patrolRoute.WorkArea"),
          width: 835,
          height: 550,
          url:
            "/msService/PATROL/patrolRoute/workGroup/workAreaPtEdit?__pc__=" +
            powerCode +
            "&viewCode=PATROL_1.0.0_patrolRoute_workAreaPtEdit&entityCode=PATROL_1.0.0_patrolRoute&iscrosscompany=false&openType=dialog&id=" +
            rows[0].id,
          isRef: false,
          buttons: [
            {
              text: text("calendar.common.save"),
              type: "primary",
              style: { color: "#fff", background: "#58c9cb" },
              onClick: function saveAreas(event) {
                event.ReactAPI.submitFormData("save", function onSaved(result) {
                  if (result.code === 200) {
                    refresh(AREA_GRID, {
                      type: "post",
                      url:
                        "/msService/PATROL/patrolRoute/workArea/data-dg1575506226708?datagridCode=" +
                        AREA_GRID,
                      param: { customCondition: { ruoteId: rows[0].id } },
                    });
                    ReactAPI.destroyDialog("newDialog");
                  }
                });
              },
            },
            {
              text: text("ec.common.cancel"),
              onClick: function cancelAreas() {
                ReactAPI.destroyDialog("newDialog");
              },
            },
          ],
        });
      }
    );
  }

  function openItemEditor() {
    var rows = selectedRows(AREA_GRID);
    if (!canEdit(rows)) {
      return;
    }
    if (rows.length > 1) {
      ReactAPI.showMessage("w", text("SupDatagrid.button.error"));
      return;
    }

    ReactAPI.getPowerCode(
      "workGroupList_workItemSet_add_PATROL_1.0.0_patrolRoute_workGroupList",
      function onPowerCode(response) {
        var powerCode =
          response[
            "workGroupList_workItemSet_add_PATROL_1.0.0_patrolRoute_workGroupList"
          ];
        ReactAPI.createDialog("newDialog", {
          title: text("PATROL.patrolRoute.WorkItem"),
          width: 1000,
          height: 650,
          url:
            "/msService/PATROL/patrolRoute/workArea/workItemPtEdit?__pc__=" +
            powerCode +
            "&viewCode=PATROL_1.0.0_patrolRoute_workItemPtEdit&entityCode=PATROL_1.0.0_patrolRoute&iscrosscompany=false&openType=dialog&id=" +
            rows[0].id,
          isRef: false,
          buttons: [
            {
              text: text("calendar.common.save"),
              type: "primary",
              style: { color: "#fff", background: "#58c9cb" },
              onClick: function saveItems(event) {
                event.ReactAPI.submitFormData("save", function onSaved(result) {
                  if (result.code === 200) {
                    refresh(ITEM_GRID, {
                      type: "post",
                      url:
                        "/msService/PATROL/patrolRoute/workItem/data-dg1575507309041?datagridCode=" +
                        ITEM_GRID,
                      param: { customCondition: { workId: rows[0].id } },
                    });
                    ReactAPI.destroyDialog("newDialog");
                  }
                });
              },
            },
            {
              text: text("ec.common.cancel"),
              onClick: function cancelItems() {
                ReactAPI.destroyDialog("newDialog");
              },
            },
          ],
        });
      }
    );
  }

  function toolbarTitle(button) {
    var toolbar = button.closest(".sup-datagrid-button-wrap");
    var titleElement = toolbar && toolbar.querySelector(".sup-datagrid-title");
    return titleElement ? titleElement.textContent.replace(/\s/g, "") : "";
  }

  function routeAction(buttonId) {
    if (buttonId === "btn-delete") {
      deleteRoute();
    } else if (buttonId === "btn-workAreaSet") {
      openAreaEditor();
    } else if (buttonId === "btn-run") {
      updateState(ROUTE_GRID, "workRoute", 1);
    } else if (buttonId === "btn-stop") {
      updateState(ROUTE_GRID, "workRoute", 0);
    }
  }

  function areaAction(buttonId) {
    if (buttonId === "btn-delete") {
      deleteArea();
    } else if (buttonId === "btn-workItemSet") {
      openItemEditor();
    } else if (buttonId === "btn-run") {
      updateState(AREA_GRID, "workArea", 1, {
        type: "post",
        url:
          "/msService/PATROL/patrolRoute/workArea/data-dg1575506226708?datagridCode=" +
          AREA_GRID,
      });
    } else if (buttonId === "btn-stop") {
      updateState(AREA_GRID, "workArea", 0, {
        type: "post",
        url:
          "/msService/PATROL/patrolRoute/workArea/data-dg1575506226708?datagridCode=" +
          AREA_GRID,
      });
    }
  }

  document.addEventListener(
    "click",
    function onPatrolMoreAction(event) {
      var target = event.target;
      var button =
        target && target.closest
          ? target.closest("#btn-delete, #btn-workAreaSet, #btn-workItemSet, #btn-run, #btn-stop")
          : null;
      if (!button) {
        return;
      }

      var title = toolbarTitle(button);
      var routeTitle = text("PATROL.patrolRoute.WorkGroup").replace(/\s/g, "");
      var areaTitle = text("PATROL.patrolRoute.WorkArea").replace(/\s/g, "");
      if (title !== routeTitle && title !== areaTitle) {
        return;
      }

      event.preventDefault();
      if (title === routeTitle) {
        routeAction(button.id);
      } else {
        areaAction(button.id);
      }
    },
    true
  );
})(window, document);
