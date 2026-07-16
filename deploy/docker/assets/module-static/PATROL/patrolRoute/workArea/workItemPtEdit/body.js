"use strict";

(function installPatrolItemEditorActions(window, document) {
  if (window.__ADP_PATROL_ITEM_EDITOR_ACTIONS_INSTALLED__) {
    return;
  }
  window.__ADP_PATROL_ITEM_EDITOR_ACTIONS_INSTALLED__ = true;

  var ITEM_GRID = "PATROL_1.0.0_patrolRoute_workItemPtEditdg1575632686507";
  var AREA_GRID = "PATROL_1.0.0_patrolRoute_workGroupListdg1575506226708";
  var DELETED_KEY = "dg1575632686507";

  function itemGrid() {
    return ReactAPI.getComponentAPI("SupDataGrid").APIs(ITEM_GRID);
  }

  function message(type, text) {
    ReactAPI.showMessage(type, text);
  }

  function selectedRows() {
    return itemGrid().getSelecteds();
  }

  function requireSelectedRows(single) {
    var rows = selectedRows();
    if (!rows.length) {
      message("w", "请选择一条巡检项进行操作");
      return null;
    }
    if (single && rows.length !== 1) {
      message("w", "该操作只能选择一条巡检项");
      return null;
    }
    return rows;
  }

  function requestParams() {
    return ReactAPI.getParamsInRequestUrl(window.location.href) || {};
  }

  function parentAreaContext() {
    var context = {
      areaId: requestParams().id || null,
      routeId: null,
      device: null,
    };
    try {
      var parentGrid = parent.ReactAPI.getComponentAPI("SupDataGrid").APIs(AREA_GRID);
      var rows = parentGrid.getSelecteds();
      if (rows.length) {
        var row = rows[0];
        context.areaId = row.id || context.areaId;
        context.routeId = row.workGroupId && row.workGroupId.id;
        if (row.device && row.device.id) {
          context.device = { id: row.device.id, name: row.device.name };
        }
      }
    } catch (_error) {
      // A direct editor URL has no parent grid; derive what we can from the loaded rows.
    }

    var existing = itemGrid().getDatagridData()[0];
    if (!context.routeId && existing && existing.routeId) {
      context.routeId = existing.routeId.id;
    }
    return context;
  }

  function setRowContext(rowIndex) {
    var table = itemGrid();
    var context = parentAreaContext();
    if (context.areaId) {
      table.setValueByKey(rowIndex, "workId.id", context.areaId);
    }
    if (context.routeId) {
      table.setValueByKey(rowIndex, "routeId.id", context.routeId);
    }
    if (context.device) {
      table.setValueByKey(rowIndex, "eamId", context.device);
    }
    table.setValueByKey(rowIndex, "isRun", true);
    table.setValueByKey(rowIndex, "valid", true);
    table.setValueByKey(rowIndex, "isAutoJudge", false);
    table.setValueByKey(rowIndex, "isConclusionModify", false);
    table.setValueByKey(rowIndex, "isErrorBench", false);
    table.setValueByKey(rowIndex, "isPass", false);
    table.setValueByKey(rowIndex, "isPhone", false);
    table.setValueByKey(rowIndex, "isSeismic", false);
    table.setValueByKey(rowIndex, "isThermometric", false);
  }

  function addItemRow() {
    var table = itemGrid();
    table.addLine();
    var rows = table.getDatagridData();
    var row = rows[rows.length - 1];
    setRowContext(row.rowIndex);
    return row.rowIndex;
  }

  function insertItemRow() {
    var rows = requireSelectedRows(true);
    if (!rows) {
      return;
    }
    var rowIndex = rows[0].rowIndex;
    itemGrid().insertLine(rowIndex);
    setRowContext(rowIndex);
  }

  function deleteItemRows() {
    var rows = requireSelectedRows(false);
    if (!rows) {
      return;
    }
    var indexes = rows.map(function mapRowIndex(row) {
      return row.rowIndex;
    });
    itemGrid().deleteLine(indexes.join(","));
  }

  function moveItemRow(direction) {
    if (!requireSelectedRows(true)) {
      return;
    }
    if (direction === "up") {
      itemGrid().moveUpLine();
    } else {
      itemGrid().moveDownLine();
    }
  }

  function closeMoreMenu(button) {
    var moreButton = button.closest("#btn-more");
    if (!moreButton) {
      return;
    }
    var menuTrigger = moreButton.querySelector(".sup-datagrid-button-item");
    window.setTimeout(function closeMenuAfterCurrentClick() {
      (menuTrigger || moreButton).click();
    }, 0);
  }

  function inputStandardOf(row) {
    return row && row.inputStandardId ? row.inputStandardId : null;
  }

  function setInputStandardAttributes(valueType, editType, rowIndex) {
    var table = itemGrid();
    var isChoice =
      editType === "PATROL_editType/singleSelect" || editType === "PATROL_editType/whether";
    var isNumberInput =
      editType === "PATROL_editType/input" && valueType === "PATROL_valueType/number";
    var supportsAutomaticJudge = isChoice || isNumberInput;

    table.setDatagridCellAttr(rowIndex, "defaultVal", { readonly: !supportsAutomaticJudge });
    table.setDatagridCellAttr(rowIndex, "isAutoJudge", { readonly: !supportsAutomaticJudge });
    table.setDatagridCellAttr(rowIndex, "normalRange", { readonly: !supportsAutomaticJudge });
    table.setDatagridCellAttr(rowIndex, "lowerLimit", { readonly: !isNumberInput });
    table.setDatagridCellAttr(rowIndex, "upperLimit", { readonly: !isNumberInput });
    table.setDatagridCellAttr(rowIndex, "isThermometric", { readonly: !isNumberInput });
    table.setDatagridCellAttr(rowIndex, "isSeismic", { readonly: !isNumberInput });
    table.setDatagridCellAttr(rowIndex, "isErrorBench", { readonly: !isNumberInput });
    table.setDatagridCellAttr(rowIndex, "errorBenchUpper", { readonly: !isNumberInput });

    if (!supportsAutomaticJudge) {
      table.setValueByKey(rowIndex, "isAutoJudge", false);
      table.setValueByKey(rowIndex, "normalRange", null);
      table.setValueByKey(rowIndex, "defaultVal", null);
    }
    if (!isNumberInput) {
      table.setValueByKey(rowIndex, "lowerLimit", null);
      table.setValueByKey(rowIndex, "upperLimit", null);
      table.setValueByKey(rowIndex, "isThermometric", false);
      table.setValueByKey(rowIndex, "isSeismic", false);
      table.setValueByKey(rowIndex, "isErrorBench", false);
      table.setValueByKey(rowIndex, "errorBenchUpper", null);
    }
  }

  function setInputStandard(objects, rowIndex) {
    var standard = objects && objects.length ? objects[0] : null;
    if (!standard) {
      setInputStandardAttributes(null, null, rowIndex);
      return;
    }
    setInputStandardAttributes(
      standard.valType && standard.valType.id,
      standard.editType && standard.editType.id,
      rowIndex
    );
  }

  function normalizeNumber(value, rowIndex) {
    if (value === null || value === undefined || value === "") {
      itemGrid().setValueByKey(rowIndex, "defaultVal", null);
      return;
    }
    var standard = inputStandardOf(itemGrid().getDatagridData()[rowIndex]);
    if (
      !standard ||
      !standard.valType ||
      standard.valType.id !== "PATROL_valueType/number" ||
      !standard.editType ||
      standard.editType.id !== "PATROL_editType/input"
    ) {
      return;
    }
    var number = Number(value);
    itemGrid().setValueByKey(
      rowIndex,
      "defaultVal",
      isFinite(number) ? number.toFixed(Number(standard.decimalPlace || 0)) : null
    );
  }

  function applyReferenceItems(items) {
    (items || []).forEach(function applyItem(source) {
      var rowIndex = addItemRow();
      var table = itemGrid();
      [
        "claim",
        "content",
        "part",
        "valueName",
        "defaultVal",
        "remark",
        "errorBenchUpper",
        "isAutoJudge",
        "normalRange",
        "upperLimit",
        "lowerLimit",
        "isThermometric",
        "isSeismic",
        "isErrorBench",
        "isPhone",
        "isPass",
        "isConclusionModify",
        "itemNumber",
      ].forEach(function copyField(key) {
        if (source[key] !== undefined) {
          table.setValueByKey(rowIndex, key, source[key]);
        }
      });
      if (source.eamId && source.eamId.id) {
        table.setValueByKey(rowIndex, "eamId", source.eamId);
      }
      if (source.publicItemId && source.publicItemId.id) {
        table.setValueByKey(rowIndex, "publicItemId", source.publicItemId);
      }
      if (source.inputStandardId && source.inputStandardId.id) {
        table.setValueByKey(rowIndex, "inputStandardId", source.inputStandardId);
        setInputStandard([source.inputStandardId], rowIndex);
      }
    });
  }

  function openItemReference() {
    ReactAPI.createDialog("workItemRef", {
      title: "巡检项参照",
      url: "/msService/PATROL/patrolRoute/workItem/workItemRef",
      size: 5,
      callback: function onReferenced(data) {
        applyReferenceItems(data);
        ReactAPI.destroyDialog("workItemRef");
      },
      isRef: true,
      buttons: [
        {
          text: "选择",
          type: "primary",
          onClick: function selectReference(data) {
            applyReferenceItems(data);
            ReactAPI.destroyDialog("workItemRef");
          },
        },
        {
          text: "取消",
          onClick: function cancelReference() {
            ReactAPI.destroyDialog("workItemRef");
          },
        },
      ],
    });
  }

  function openExemptionRule() {
    var rows = requireSelectedRows(true);
    if (!rows) {
      return;
    }
    var row = rows[0];
    if (!row.id) {
      message("w", "请先保存巡检项，再设置免检规则");
      return;
    }
    var standard = inputStandardOf(row);
    var editType = standard && standard.editType && standard.editType.id;
    if (editType !== "PATROL_editType/singleSelect" && editType !== "PATROL_editType/whether") {
      message("w", "只有单选或是否类型的巡检项可以设置免检规则");
      return;
    }
    ReactAPI.createDialog("exemptionRuleDialog", {
      title: "免检规则",
      size: 4,
      url:
        "/msService/PATROL/patrolRoute/workItem/exeRuleEdit?id=" +
        row.id +
        "&sortNum=" +
        (row.sort || 0) +
        "&areaId=" +
        requestParams().id +
        "&view.code=PATROL_1.0.0_patrolRoute_exeRuleEdit&entity.code=PATROL_1.0.0_patrolRoute",
      isRef: false,
      buttons: [
        {
          text: "保存",
          type: "primary",
          onClick: function saveExemptionRule(event) {
            event.ReactAPI.submitFormData("save", function onSaved(response) {
              if (response && response.code === 200) {
                ReactAPI.destroyDialog("exemptionRuleDialog");
              }
            });
          },
        },
        {
          text: "取消",
          onClick: function cancelExemptionRule() {
            ReactAPI.destroyDialog("exemptionRuleDialog");
          },
        },
      ],
    });
  }

  function openCandidateReference(gridObject, field) {
    var rows = gridObject.getSelecteds();
    if (!rows.length || !rows[0].inputStandardId || !rows[0].inputStandardId.id) {
      message("w", "请先选择录入标准");
      return;
    }
    var rowIndex = rows[0].rowIndex;
    ReactAPI.createDialog("candidateRef", {
      title: "候选值",
      url:
        "/msService/PATROL/inputStandard/candidateValue/candidateRef?inputId=" +
        rows[0].inputStandardId.id +
        "&customConditionKey=inputId&openType=frame",
      size: 4,
      isRef: true,
      callback: function onCandidateSelected(data) {
        var values = (data || []).map(function candidateValue(item) {
          return item.valueName;
        });
        gridObject.setValueByKey(rowIndex, field, values.join(","));
        ReactAPI.destroyDialog("candidateRef");
      },
      buttons: [
        {
          text: "选择",
          type: "primary",
          onClick: function selectCandidates(data) {
            var values = (data || []).map(function candidateValue(item) {
              return item.valueName;
            });
            gridObject.setValueByKey(rowIndex, field, values.join(","));
            ReactAPI.destroyDialog("candidateRef");
          },
        },
        {
          text: "取消",
          onClick: function cancelCandidates() {
            ReactAPI.destroyDialog("candidateRef");
          },
        },
      ],
    });
  }

  function isNumericRange(value) {
    var range = String(value || "").replace(/\s/g, "");
    var comparison = /^(?:>=|<=|>|<|=|≥|≤|＞|＜)-?\d+(?:\.\d+)?$/;
    if (!range) {
      return false;
    }
    if (range.indexOf("|") >= 0) {
      var conditions = range.split("|");
      return conditions.length === 2 && conditions.every(function validCondition(item) {
        return comparison.test(item);
      });
    }
    if (range.indexOf("~") >= 0) {
      var bounds = range.replace(/[()]/g, "").split("~");
      return (
        bounds.length === 2 &&
        bounds[0] !== "" &&
        bounds[1] !== "" &&
        isFinite(Number(bounds[0])) &&
        isFinite(Number(bounds[1])) &&
        Number(bounds[0]) <= Number(bounds[1])
      );
    }
    return comparison.test(range);
  }

  function validateRows() {
    var errors = [];
    itemGrid()
      .getDatagridData()
      .forEach(function validateRow(row, index) {
        var standard = inputStandardOf(row);
        var isNumericInput =
          standard &&
          standard.valType &&
          standard.valType.id === "PATROL_valueType/number" &&
          standard.editType &&
          standard.editType.id === "PATROL_editType/input";
        if (row.isAutoJudge && !row.normalRange) {
          errors.push("第 " + (index + 1) + " 行启用自动判定后，正常范围不能为空");
        } else if (row.isAutoJudge && isNumericInput && !isNumericRange(row.normalRange)) {
          errors.push("第 " + (index + 1) + " 行正常范围格式不正确");
        }
        if (
          row.upperLimit !== null &&
          row.upperLimit !== undefined &&
          row.lowerLimit !== null &&
          row.lowerLimit !== undefined &&
          Number(row.upperLimit) < Number(row.lowerLimit)
        ) {
          errors.push("第 " + (index + 1) + " 行上限值不能小于下限值");
        }
      });
    if (errors.length) {
      message("f", errors.join("<br/>"));
      return false;
    }
    return true;
  }

  function normalizeDeletedIds(value) {
    if (!value) {
      return [];
    }
    return (Array.isArray(value) ? value : String(value).split(","))
      .map(function trimId(item) {
        return String(item).trim();
      })
      .filter(Boolean);
  }

  function softDeleteRowsBeforeSave(saveData) {
    var deleted = normalizeDeletedIds(
      saveData.dgDeletedIds && saveData.dgDeletedIds[DELETED_KEY]
    );
    if (!deleted.length) {
      ReactAPI.setSaveData(saveData);
      return true;
    }

    saveData.workArea = saveData.workArea || {};
    var tracked = normalizeDeletedIds(saveData.workArea.itemDeleteIds);
    var uniqueTracked = [];
    tracked.concat(deleted).forEach(function addUniqueId(id) {
      if (uniqueTracked.indexOf(id) < 0) {
        uniqueTracked.push(id);
      }
    });
    saveData.workArea.itemDeleteIds = uniqueTracked.join(",");

    var deletedSuccessfully = false;
    ReactAPI.request(
      {
        type: "get",
        async: false,
        url:
          "/msService/PATROL/patrolRoute/workItem/deleteWorkItems?workItemIds=" +
          deleted.join(","),
      },
      function onRowsDeleted(response) {
        deletedSuccessfully = Boolean(response && response.code === 200);
      }
    );
    if (!deletedSuccessfully) {
      message("f", "巡检项删除失败，保存已取消");
      return false;
    }

    saveData.dgDeletedIds[DELETED_KEY] = null;
    ReactAPI.setSaveData(saveData);
    return true;
  }

  function beforeSave() {
    if (!validateRows()) {
      return false;
    }
    return softDeleteRowsBeforeSave(ReactAPI.getSaveData() || {});
  }

  function addComparisonMark(element) {
    var rows = requireSelectedRows(true);
    if (!rows) {
      return;
    }
    var row = rows[0];
    var standard = inputStandardOf(row);
    if (
      !row.isAutoJudge ||
      !standard ||
      !standard.valType ||
      standard.valType.id !== "PATROL_valueType/number" ||
      !standard.editType ||
      standard.editType.id !== "PATROL_editType/input"
    ) {
      message("w", "只有启用自动判定的数字录入项可以追加范围符号");
      return;
    }
    itemGrid().setValueByKey(
      row.rowIndex,
      "normalRange",
      String(row.normalRange || "") + String(element.textContent || "").trim()
    );
  }

  function handleButton(button) {
    if (button.id === "btn-add") {
      addItemRow();
    } else if (button.id === "btn-delete") {
      deleteItemRows();
    } else if (button.id === "btn-insertLine") {
      insertItemRow();
    } else if (button.id === "btn-moveUpward") {
      moveItemRow("up");
    } else if (button.id === "btn-moveDown") {
      moveItemRow("down");
    } else if (button.id === "btn-workItemRef") {
      openItemReference();
    } else if (button.id === "btn-exeRuleSet") {
      openExemptionRule();
    }
  }

  document.addEventListener(
    "click",
    function onItemEditorAction(event) {
      var target = event.target;
      var button =
        target && target.closest
          ? target.closest(
              "#btn-add, #btn-delete, #btn-insertLine, #btn-moveUpward, #btn-moveDown, #btn-workItemRef, #btn-exeRuleSet"
            )
          : null;
      if (!button) {
        return;
      }
      event.preventDefault();
      event.stopImmediatePropagation();
      closeMoreMenu(button);
      handleButton(button);
    },
    true
  );

  window.addNewRow = addItemRow;
  window.insertLineClick = insertItemRow;
  window.workItemRef = openItemReference;
  window.exeRuleSet = openExemptionRule;
  window.callBackItem = applyReferenceItems;
  window.openCandidateRef = openCandidateReference;
  window.clearCandidateValue = function clearCandidateValue(gridCode, rowIndex, field) {
    ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode).setValueByKey(rowIndex, field, "");
  };
  window.addMarks = addComparisonMark;
  window.inputStarnderItemAttribute = setInputStandardAttributes;
  window.setInputStarnder = setInputStandard;
  window.setInputStarnderOnchange = function onInputStandardChanged(value, rowIndex) {
    var rows = itemGrid().getDatagridData();
    var standard = rows[rowIndex] && rows[rowIndex].inputStandardId;
    setInputStandard(standard ? [standard] : value, rowIndex);
  };
  window.valueOnchange = normalizeNumber;
  window.autoJChange = function onAutomaticJudgeChanged(value, rowIndex) {
    itemGrid().setDatagridCellAttr(rowIndex, "normalRange", { readonly: !value });
    if (!value) {
      itemGrid().setValueByKey(rowIndex, "normalRange", null);
    }
  };
  window.setContentValue = function setContentValue(objects, rowIndex) {
    if (objects && objects.length) {
      itemGrid().setValueByKey(rowIndex, "claim", objects[0].claim);
      itemGrid().setValueByKey(rowIndex, "content", objects[0].content);
    }
  };
  window.eamCallBack = function eamCallBack() {};
  window.valueOnclick = function valueOnclick() {};
  window.normalValueOnclick = function normalValueOnclick() {};
  window.PATROL_patrolRoute_workArea_onload = function onLoad() {};
  window.PATROL_patrolRoute_workArea_onsave = beforeSave;
})(window, document);
