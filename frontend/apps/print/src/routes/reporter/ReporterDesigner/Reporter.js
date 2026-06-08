import React, { Component } from 'react';
import GC from '@grapecity/spread-sheets';
import Excel from '@grapecity/spread-excelio';
import '@grapecity/spread-sheets-print';
import '@grapecity/spread-sheets-charts';
import '@grapecity/spread-sheets-resources-zh';
import '@grapecity/spread-sheets-barcode';
import '@grapecity/spread-sheets/styles/gc.spread.sheets.excel2013white.css';
// import { editHistoryData } from 'root/services/objectApi';
// import { update, exec } from 'root/services/datatableApi';
// import commonMessage from 'root/common/messages';
import { notification } from 'sup-ui';
import { saveAs } from 'file-saver/FileSaver';
import * as _ from 'lodash';
import * as CustomFunction from './CustomFunction';
import * as Util from './ReportUtil.js';
import { unzip } from '../utils/utils';
import { fillData } from './FillData';
import { CustomFormat } from './CustomFormat';
import ConfirmModal from './Modal/ConfirmModal';
import PrintSettingModal from './Modal/PrintSettingModal';
import ExportTypeSelectModal from './Modal/ExportTypeSelectModal';
import styles from './Reporter.less';
import messages from './messages';
import TagTriangleCell from './Cell/TagTriangleCell';

const spreadNS = GC.Spread.Sheets;
const oldFun = spreadNS.getTypeFromString;
let isShowCustomCell = false;
spreadNS.getTypeFromString = function (typeString, ...args) {
  if (isShowCustomCell) {
    switch (typeString) {
      case 'TagTriangleCell':
        return TagTriangleCell;
      default:
        return oldFun.apply(this, args);
    }
  }
  return oldFun.apply(this, args);
};
export default class Reporter extends Component {
  constructor(props) {
    super();
    this.state = {
      printModalVisiable: false,
      exportTypeSelectVisiable: false
    };
    isShowCustomCell = !props.isPreview;
  }

  componentDidMount() {
    GC.Spread.Common.CultureManager.culture(localStorage.getItem('language') || 'zh-cn');
    this.spread = new spreadNS.Workbook(this.spreadContainer, { sheetCount: 1 });
    /**
     * TODO
     * 临时方案，等官方除解决方案
     */
    if (this.props.isPreview) {
      _.map(['keydown', 'mousedown', 'mousewheel', 'wheel'], (event) => {
        this.spreadContainer.addEventListener(event, (e) => {
          e.stopPropagation();
        });
      });
    }
    // end
    this.excelIO = new Excel.IO();
    this.initCustomFunction();

    if (!this.props.isEdit) {
      this.unzipJson = unzip(this.props.config.json);
      this.init(this.unzipJson);
    }
    this.formatterRecords = {}; // 记录formatter;
  }

  componentDidUpdate(prevProp) {
    const { isPreview, json } = this.props;
    const preJson = _.get(prevProp, 'json');
    if (isPreview) {
      if (!json) return;
      // 预览期 展示数据
      const { historyInfo, serviceInfo, dataTableInfo, statisticInfo, minDate, maxDate, customServices, entityObjects } = this.props;
      if (prevProp.historyInfo !== historyInfo
        || prevProp.serviceInfo !== serviceInfo
        || prevProp.dataTableInfo !== dataTableInfo
        || prevProp.statisticInfo !== statisticInfo
        || prevProp.customServices !== customServices
        || prevProp.entityObjects !== entityObjects
        || prevProp.minDate !== minDate
        || prevProp.maxDate !== maxDate) {
        this.initForFillData(true);
      }
    } else if (json && preJson !== json) {
      // 设计期获取json
      this.unzipJson = unzip(json);
      this.init(this.unzipJson);
    }
  }

  init = (json) => {
    const newJson = _.keys(json).length ? json : this.spread.toJSON();
    this.spread.fromJSON(this.updateInitJson(newJson));
    if (this.props.isEdit) {
      this.sheets = this.getSheetsInfo(this.spread);
    } else {
      this.initPaneTag();
      this.bindEvent();
      this.initChartDataRange();
      this.authorityObj = this.getAuthorityObj();
      this.initForPreview();
    }
  }

  getSheetsInfo = (spread) => {
    const sheets = {};
    for (let index = 0; index < spread.getSheetCount(); index += 1) {
      const name = spread.sheets[index].name();
      sheets[name] = index;
    }
    return sheets;
  }

  getFileName = () => {
    const to2DigitsString = (num) => {
      return (`0${num}`).substr(-2);
    };

    const date = new Date();
    return [
      'export',
      date.getFullYear(), to2DigitsString(date.getMonth() + 1), to2DigitsString(date.getDate()),
      to2DigitsString(date.getHours()), to2DigitsString(date.getMinutes()), to2DigitsString(date.getSeconds())
    ].join('');
  }

  setData = (dataPoint) => {
    this.spread.suspendPaint();
    this.objectSource = dataPoint;
    this.setRtData(dataPoint);
    this.spread.resumePaint();
  }

  setRtData = (dataPoint) => {
    _.map(this.rtData || this.allRecords, (cellInfo, id) => {
      const { dataSource, sheetName, curRow, curCol, dataSourceType } = cellInfo;
      const fDataSource = dataSource.replace(/\./g, ':');
      cellInfo.dataSource = fDataSource;
      if (dataSourceType === 'RT' && dataPoint[fDataSource]) {
        let { value } = dataPoint[fDataSource][0];
        const posString = Util.getCellPositionString(curRow + 1, curCol + 1);
        const key = `${sheetName}!${posString}`;
        const newValue = _.get(this.newRtValue, key);
        if (newValue) {
          value = _.isNaN(Number(newValue)) ? newValue : Number(newValue);
        }
        const authorityMap = this.authorityObj[sheetName];
        const authority = _.get(authorityMap, id) || _.get(authorityMap, posString) || 2;
        this.setRtValue(value, cellInfo, authority);
      }
    });
  }

  setRtValue = (value, cellInfo, authority) => {
    const { dataSource, sheetName, curRow, curCol } = cellInfo;
    if (this.editingObj) {
      const { row, col } = this.editingObj;
      // 编辑过的单元格，不更新推上来的实时值
      if (row !== undefined && col !== undefined && row === curRow && col === curCol) return;
    }
    const sheet = this.spread.getSheetFromName(sheetName);
    const cell = sheet.getCell(curRow, curCol);
    const preValue = cell.value();
    // 读写/只读权限时，推上来的值与当前值相同时，不更新
    if (preValue === value && [1, 2].includes(authority)) return;

    cell.formula(null);
    switch (authority) {
      case 1: {
        cell.value(value);
        if (_.get(this.props, 'config.runTimeEdit')) {
          Util.editHISData(sheet, { cell, dataSource, type: 'RT' });
        }
        break;
      }
      case 2: cell.value(value); break;
      case 3: {
        if (cell.value() !== null) {
          cell.value(null);
          cell.formatter(new CustomFormat());
        }
        break;
      }
      default: break;
    }
    const posString = Util.getCellPositionString(curRow + 1, curCol + 1);
    if (value === null) {
      if (!this.formatterRecords[posString]) this.formatterRecords[posString] = cell.formatter();
      cell.formatter(new CustomFormat());
    } else if (this.formatterRecords[posString]) {
      cell.formatter(this.formatterRecords[posString]);
    }
    if (cell.wordWrap()) sheet.autoFitRow(curRow);
  }

  setHorizontalScrollbarVisiable = (sheet = this.spread.getActiveSheet()) => {
    let visible = false;
    if (this.props.isPreview) {
      const data = sheet.toJSON().data.dataTable;
      const colCount = _.max(_.map(_.values(data), (item) => Number(_.keys(item)[_.keys(item).length - 1])));
      let colWidth = 0;
      for (let i = 0; i <= colCount; i += 1) {
        colWidth += sheet.getColumnWidth(i);
      }
      visible = colWidth > this.props.data._width;
    }
    this.spread.options.showHorizontalScrollbar = visible;
  }

  getAllData = () => {
    const { historyInfo = {}, serviceInfo = {}, dataTableInfo, statisticInfo, customServices, entityObjects } = this.props;
    const data = {
      historyInfo: this.filterHisSerData(historyInfo),
      serviceInfo: this.filterHisSerData(serviceInfo),
      dataTableInfo,
      statisticInfo,
      customServices,
      entityObjects
    };
    return data;
  }

  filterHisSerData = (dataInfo) => {
    const newDataInfo = {};
    _.map(dataInfo, (value, key) => {
      if (['code', 'noNext', 'message'].includes(key)) return;
      newDataInfo[key] = _.get(value, 'list') ? value : [];
    });
    return newDataInfo;
  }

  initForFillData = (hasChange = false) => {
    this.initCustomFunction(); // bugfix SUP-5698 重新定义自定义函数-性能不佳
    fillData(this);
    this.bindRuntimeEditEvent();
    if (this.props.prePrint && hasChange) {
      this.print(false);
      this.props.changePrintStatus();
    }
  }

  initForPreview = () => {
    const { isPreview, config: { json } = {} } = this.props;
    if (isPreview && json) {
      this.spreadJson = this.spread.toJSON();
      this.allRecords = _.merge({}, this.records);
      this.allOptRecords = _.merge({}, this.optRecords);
      this.records = null;
      this.optRecords = null;
      this.initForFillData();
    }
  }

  initPaneTag = () => {
    const { isPreview, config: { fillDataType } = {} } = this.props;
    if (!isPreview || fillDataType === 'replace') return;
    const count = this.spread.getSheetCount();
    for (let i = 0; i < count; i += 1) {
      const sheet = this.spread.getSheet(i);
      this.resetDefaultPaneTag(sheet, 'paneSettingConf');
      this.resetDefaultPaneTag(sheet, 'paneSettingUpConf');
    }
  }

  resetDefaultPaneTag = (sheet, tagName) => {
    const tags = sheet.tag() || {};
    let paneTag = _.get(tags, tagName, {});
    paneTag = this.clearEmptyPaneTag(sheet, paneTag);
    _.map(this.records, (cell) => {
      const { cellId: subId, curRow, curCol } = cell;
      const exist = _.find(_.values(paneTag), (arr) => arr.includes(subId));
      if (exist) return;
      if (curCol && tagName === 'paneSettingConf') {
        const leftCellId = Util.getCellPositionString(curRow + 1, curCol);
        this.setPaneTag(sheet, paneTag, tagName, subId, leftCellId);
      }
      if (curRow && tagName === 'paneSettingUpConf') {
        const upCellId = Util.getCellPositionString(curRow, curCol + 1);
        this.setPaneTag(sheet, paneTag, tagName, subId, upCellId);
      }
    });
    sheet.tag({ ...tags, [tagName]: paneTag });
  }

  /**
   * 清除不存在单元格的窗格
   */
  clearEmptyPaneTag = (sheet, paneTag) => {
    const tempTag = {};
    _.map(_.keys(paneTag), (subId) => {
      let paneTagArr = paneTag[subId] || [];
      const { row: subRow, col: subCol } = Util.getCellPositionFromString(subId);
      const maxRows = sheet.getRowCount();
      const maxCols = sheet.getColumnCount();
      if (subRow >= maxRows || subCol >= maxCols) return;
      paneTagArr = _.filter(paneTagArr, (subPaneId) => {
        const { row: subPaneRow, col: subPaneCol } = Util.getCellPositionFromString(subPaneId);
        if (subPaneRow >= maxRows || subPaneCol >= maxCols) return false;
        return true;
      });
      if (!paneTagArr || !~(paneTagArr.length - 1)) return;
      tempTag[subId] = paneTagArr;
    });
    return tempTag;
  }

  setPaneTag = (sheet, paneTag, tagName, subId, id) => {
    // 默认父格id, 子单元格subId
    if (this.isRelativeDataSource(sheet, id, subId)) {
      const key = `${sheet.name()}!${id}`;
      const { cellId } = this.records[key];
      const removedConfig = _.get(sheet.tag(), `${tagName}-removed`, []);
      if (removedConfig.length && removedConfig.includes(subId)) return;
      if (!paneTag[cellId]) {
        paneTag[cellId] = [subId];
      } else {
        paneTag[cellId].push(subId);
      }
    }
  }

  isRelativeDataSource = (sheet, id, subId) => {
    const key = `${sheet.name()}!${id}`;
    const subKey = `${sheet.name()}!${subId}`;
    if (!this.records[key] || !this.records[subKey]) return false;
    const { dataSourceType, param, dataSource, tableName } = this.records[key];
    const { dataSourceType: subDataSourceType, param: subParam, dataSource: subDataSource, tableName: subTableName } = this.records[subKey];
    if (!['SER', 'DT'].includes(dataSourceType) || ['_key_', '_table_'].includes(param)) return false;
    if (!['SER', 'DT'].includes(subDataSourceType) || ['_key_', '_table_'].includes(subParam)) return false;
    if (dataSourceType !== subDataSourceType) return false;
    if ((tableName || dataSource) !== (subTableName || subDataSource)) return false;
    return true;
  }

  initCustomFunction = () => {
    const { isPreview, isEdit } = this.props;
    this.dataSource = { cellInfo: {}, dataSourceInfo: {} };
    if (isPreview) {
      this.records = {};
      this.optRecords = {};
    }
    _.map(_.keys(CustomFunction), (func) => {
      CustomFunction[func]({ isPreview, isEdit, report: this });
    });
  }

  initChartDataRange = () => {
    this.chartDataRange = {};
    for (let index = 0; index < this.spread.getSheetCount(); index += 1) {
      const sheet = this.spread.getSheet(index);
      const name = sheet.name();
      this.chartDataRange[name] = {};
      _.map(sheet.charts.all(), (chart) => {
        if (!chart.dataRange()) return;
        const [, formulaParam] = chart.getFormulas()[0].match(/^=SERIES\((.*)\)$/);
        const paramArr = formulaParam.split(',');
        // paramArr: [左表头，上表头，数据区域]
        if (!paramArr[0] || !paramArr[2]) return;
        const [startPos, endPos] = chart.dataRange().match(/([a-zA-Z]+\$\d+)/g);
        // 图表数据源区域中的数据位置上（endPos）必须含有填充公式
        if (!this.checkPosIsFormula(endPos.replace('$', ''))) return;
        const { row: sRow, col: sCol } = Util.getCellPositionFromString(startPos.replace('$', ''));
        const { row: eRow, col: eCol } = Util.getCellPositionFromString(endPos.replace('$', ''));
        const chartInfo = { sRow, sCol, eRow, eCol };
        this.chartDataRange[name][chart.name()] = chartInfo;
      });
    }
  }

  checkPosIsFormula = (pos) => {
    return _.find(this.records, ({ cellId }) => cellId === pos);
  }

  updateInitJson = (json) => {
    const { isPreview, isDesign, config: { runTimeEdit, runTimeShowSheet, scrollbarVisible, backgroundColor } = {} } = this.props;
    json.scrollbarMaxAlign = true;
    json.backColor = backgroundColor;
    json.grayAreaBackColor = backgroundColor;
    if (!isPreview && !isDesign) {
      delete json.allowContextMenu;
      delete json.allowUserZoom;
      delete json.tabStripVisible;
      delete json.newTabVisible;
      delete json.showVerticalScrollbar;
      delete json.showHorizontalScrollbar;
      _.map(json.sheets, (sheet) => {
        sheet.frozenlineColor = 'green';
        if (!sheet.gridline) sheet.gridline = {};
        sheet.gridline.color = '#D4D4D4';
        delete sheet.rowHeaderVisible;
        delete sheet.colHeaderVisible;
        delete sheet.isProtected;
      });
    } else {
      json.allowContextMenu = false;
      json.allowUserZoom = (this.isMobile || this.isZhizhi);
      json.tabEditable = isPreview && runTimeEdit;
      json.tabStripVisible = runTimeShowSheet;
      json.newTabVisible = false;
      json.showVerticalScrollbar = false;
      json.showHorizontalScrollbar = scrollbarVisible !== 'hidden';
      delete json.activeSheetIndex;
      _.map(json.sheets, (sheet) => {
        sheet.sheetAreaOffset = { left: 1, top: 1 };
        sheet.frozenlineColor = 'transparent';
        if (!sheet.gridline) sheet.gridline = {};
        sheet.gridline.color = isPreview ? 'transparent' : '#D4D4D4';
        sheet.rowHeaderVisible = false;
        sheet.colHeaderVisible = false;
        sheet.isProtected = true;
        sheet.selections = { activeSelectedRangeIndex: -1, length: 0 };
        sheet.protectionOptions = {
          allowSelectLockedCells: this.newRtValue,
          allowSelectUnlockedCells: true
        };
      });
    }
    return json;
  }

  updateZoom = (json = {}, zoom) => {
    _.map(json.sheets, (sheet) => {
      sheet.zoomFactor = zoom;
    });
  }

  updateJson = () => {
    const zoom = this.spread.getActiveSheet().zoom();
    this.updateZoom(this.spreadJson, zoom);
    this.spread.fromJSON(this.spreadJson);
    this.bindCellClick();
    this.bindSelectionChanged();
    this.authorityObj = this.getAuthorityObj();
  }

  bindEvent = () => {
    if (this.props.isPreview) {
      this.spread.bind(spreadNS.Events.ActiveSheetChanged, (sender, args) => {
        const sheet = args.newSheet;
        sheet.clearSelection();
      });
      this.bindSelectionChanged();
      this.bindCellClick();
    }
  }

  bindSelectionChanged = () => {
    const { ctrl } = this.props || {};
    for (let index = 0; index < this.spread.getSheetCount(); index += 1) {
      const sheet = this.spread.getSheet(index);
      sheet.bind(spreadNS.Events.SelectionChanged, (e, info) => {
        if (!info.newSelections[0]) return;
        const { row, col, rowCount, colCount } = info.newSelections[0];
        const records = [];
        for (let r = row; r < row + rowCount; r += 1) {
          const record = [];
          for (let c = col; c < col + colCount; c += 1) {
            record.push(sheet.getCell(r, c).value() || '');
          }
          records.push(record);
        }
        ctrl.value = {
          event: { type: 'select' }, // 此处event 代表动作，
          value: records
          // value: { row, col, rowCount, colCount, sheet, records }// 传递的参数
        };
      });
    }
  }

  bindCellClick = () => {
    const { config: { runTimeEdit } = {}, ctrl } = this.props || {};
    const hasDT = this.checkDataSource('DT');
    const linkTargets = _.get(ctrl, 'targets', []);
    if (runTimeEdit || linkTargets.length > 0) {
      for (let index = 0; index < this.spread.getSheetCount(); index += 1) {
        const sheet = this.spread.getSheet(index);
        sheet.bind(spreadNS.Events.CellClick, (sender, args) => {
          const { row, col } = args;
          // 报表点击事件获取信息
          this.clickedCell = { row, col, sheet };

          // 报表联动（单元格点击）传递信息
          // record  this.getCellRecord(row, col, sheet)
          ctrl.value = {
            event: { type: 'click' }, // 此处event 代表动作，
            value: sheet.getValue(row, col)
            // value: { row, col, sheet, record: sheet.getValue(row, col) } // 传递的参数
          };

          if (runTimeEdit) {
            if (hasDT) this.handleEditChangeDT(sheet, args);
            this.handleCellClick();
          }
        });
      }
    }
  }

  checkDataSource = (type) => {
    const { dataTableInfo, historyInfo } = this.getAllData();
    const { config: { dataSourceInfo: { addDataSource = {} } = {} } = {} } = this.props;
    switch (type) {
      case 'DT': return _.keys(dataTableInfo).length > 0;
      case 'HIS': return _.keys(historyInfo).length > 0;
      case 'RT': return _.keys(addDataSource).length > 0;
      default: return false;
    }
  }

  handleCellClick = () => {
    const { config: { fillDataType } = {}, intl } = this.props;
    const { row, col, sheet } = this.clickedCell;
    const records = _.filter(this.allOptRecords, (item) => item.type !== 'null' && item.sheetName === sheet.name() && item.text === sheet.getCell(row, col).value());
    _.map(records, (item) => {
      const { dataSource, optType: type, optScope, tableName, curRow, curCol } = item;
      if (fillDataType !== 'replace' || (curRow === row && curCol === col)) {
        if (!dataSource || !tableName) {
          notification.warning({ message: intl.formatMessage(messages.noDataSourceOrTableName) });
        } else {
          switch (optScope) {
            case 'all': this.sqlUpdateAllData({ dataSource, tableName }); break;
            case 'row': this.sqlUpdateData({ dataSource, row, tableName, type }); break;
            default: break;
          }
        }
      }
    });
  }

  bindRuntimeEditEvent = () => {
    if (this.props.isPreview && _.get(this.props, 'config.runTimeEdit')) {
      if (!this.modifiedHISData) this.modifiedHISData = {};
      if (!this.modifiedDTData) this.modifiedDTData = {};
      if (!this.modifiedAllDTData) this.modifiedAllDTData = {};
      if (!this.newRtValue) this.newRtValue = {};
      for (let index = 0; index < this.spread.getSheetCount(); index += 1) {
        this.bindEditChangeEvent(this.spread.getSheet(index));
      }
    }
  }

  bindEditChangeEvent = (sheet) => {
    sheet.bind(spreadNS.Events.ClipboardPasting, (sender, args) => {
      const { cellRange: { row, col, rowCount, colCount }, pasteData, sheetName } = args;
      const rowList = pasteData.text.split('\r\n');
      for (let r = row; r < row + rowCount; r += 1) {
        const colList = rowList[r - row].split('\t');
        for (let c = col; c < col + colCount; c += 1) {
          const editText = colList[c - col];
          this.handleEditChangeEvent(sheet, { editingText: editText, row: r, col: c, sheetName });
        }
      }
    });
    sheet.bind(spreadNS.Events.EditChange, (sender, args) => {
      this.handleEditChangeEvent(sheet, args);
    });
  }

  handleEditChangeEvent = (sheet, args) => {
    if (this.checkDataSource('DT')) this.handleEditChangeDT(sheet, args);
    if (this.checkDataSource('RT') || this.checkDataSource('HIS')) this.handleEditChangeHIS(sheet, args);
  }

  handleEditChangeHIS = (sheet, args) => {
    const { row, col, editingText } = args;
    const key = `${sheet.name()}!${Util.getCellPositionString(row + 1, col + 1)}`;
    const hisInfo = Util.getCellTagValue(sheet, { row, col, key: 'hisInfo' });
    if (hisInfo) {
      this.editingObj = { row, col, editingText };
      if (hisInfo.type === 'RT') {
        this.newRtValue[key] = editingText;
      }
      hisInfo.value = editingText;
      this.modifiedHISData[key] = hisInfo;
    }
  }

  handleEditChangeDT = (sheet, args) => {
    this.handleEditChangeDTData({ sheet, args, modifiedDTData: this.modifiedAllDTData });
    for (let colIndex = 0; colIndex < sheet.getColumnCount(); colIndex += 1) {
      this.handleEditChangeDTData({ sheet, args, colIndex, modifiedDTData: this.modifiedDTData });
    }
  }

  handleEditChangeDTData = ({ sheet, args, colIndex = args.col, modifiedDTData }) => {
    const { row, col, editingText, sheetName } = args;
    const key = `${sheetName}!${Util.getCellPositionString(row + 1, colIndex + 1)}`;
    const tableInfo = (modifiedDTData && modifiedDTData[key]) || Util.getCellTagValue(sheet, { row, col: colIndex, key: 'tableInfo' });
    if (tableInfo) {
      if (col === colIndex && editingText !== undefined) {
        this.editingObj = { row, col, editingText };
        tableInfo.value = editingText;
      } else if (!modifiedDTData[key]) {
        tableInfo.value = undefined;
      }
      modifiedDTData[key] = tableInfo;
    }
  }

  handleMouseUp = () => {
    if (this.spread.getActiveSheet().isEditing()) {
      document.dispatchEvent(new Event('mouseup'));
    } else {
      document.dispatchEvent(new Event('mousedown'));
    }
  }

  handleKeyUp = (e) => {
    // del
    if (this.props.isPreview && e.keyCode === 46) {
      const sheet = this.spread.getActiveSheet();
      const args = Util.getCell(this.spread);
      args.editingText = null;
      this.handleEditChangeDT(sheet, args);
    }
  }

  getAuthorityObj = () => {
    const authorityObj = {};
    this.spread.suspendPaint();
    for (let index = 0; index < this.spread.getSheetCount(); index += 1) {
      const sheet = this.spread.getSheet(index);
      _.map(_.get(sheet.tag(), 'authorityConf'), (id) => {
        const { row, col } = Util.getCellPositionFromString(id);
        const { authority: tag } = sheet.getTag(row, col) || {};
        if (tag) {
          const { roleId = [] } = JSON.parse(localStorage.getItem('loginMsg'));
          const authorityArr = roleId.map((item) => tag[item.roleName]);
          const authority = _.min(authorityArr);
          _.set(authorityObj, `${sheet.name()}.${id}`, _.min(authorityArr));
          this.changeCellState(sheet.getCell(row, col), authority);
        }
      });
    }
    this.spread.resumePaint();
    return authorityObj;
  }

  changeCellState = (cell, authority) => {
    // 权限状态码， 1：读写  2：只读  3：隐藏
    switch (authority) {
      // case 1: cell.locked(false); break;
      // case 2:  break;
      case 3: cell.value(null); break;
      default: break;
    }
  }

  addFromDataSource = ({ tableName, row, col, dataSource, isShowHeader = true }) => {
    const table = this.spread.getActiveSheet().tables.addFromDataSource(tableName, row, col, dataSource);
    table.showHeader(isShowHeader);
  }

  updateFormula = () => {
    _.map(this.spread.sheets, (sheet, index) => {
      const rowCount = sheet.getRowCount();
      const colCount = sheet.getColumnCount();
      for (let row = 0; row < rowCount; row += 1) {
        for (let col = 0; col < colCount; col += 1) {
          const formula = sheet.getFormula(row, col);
          if (formula && formula.match(/^BC_(\w+)\((\w+)\((.*?)\)(.*?)\)/)) {
            const formulaArr = formula.match(/^BC_(\w+)\(BAR\((.*?)\)\),/);
            if (formulaArr) {
              const newFormula = formula.replace(`BAR(${formulaArr[2]})`, `${formulaArr[2]}`)
              sheet.setFormula(row, col, newFormula);
            }
          }
        }
      }
    })
  }

  initPrintInfo = () => {
    this.spread.suspendPaint();
    this.updateFormula();
    this.spread.fromJSON(this.spread.toJSON());
    this.sheets = this.getSheetsInfo(this.spread);
    _.map(this.sheets, (index) => {
      const sheet = this.spread.sheets[index];
      const printInfo = sheet.printInfo();
      // const zoom = this.spread.sheets[index].zoom() || 1;
      // 打印设置纸张设置为A4
      printInfo.paperSize(new GC.Spread.Sheets.Print.PaperSize(GC.Spread.Sheets.Print.PaperKind.a4));
      // 纸张设置为其他尺寸
      // printInfo.paperSize(new GC.Spread.Sheets.Print.PaperSize(950,1350));
      // 设置页边距
      printInfo.margin({ top: 0, bottom: 0, left: 0, right: 0, header: 0, footer: 0 });
      // 是否打印控件的外边框
      printInfo.showBorder(false);
      // 是否打印网格线（默认打印）
      printInfo.showGridLine(false);
      //  printInfo.headerCenter("&\ "Comic Sans MS"System Information ");
      //  printInfo.footerCenter("&P/&N/&F ");
      // 缩放
      // printInfo.zoomFactor(zoom);
      // 打印时是否每一列都自适应宽度
      // printInfo.bestFitColumns(true);
      // 打印时是否每一行都自适应高度
      // printInfo.bestFitRows(true);
      // 打印的质量因子
      printInfo.qualityFactor(5);
      // 打印的页面方向。portrait纵 landscape横
      printInfo.orientation(GC.Spread.Sheets.Print.PrintPageOrientation.portrait);
      // 打印页面的居中方式。
      printInfo.centering(GC.Spread.Sheets.Print.PrintCentering.none);
      // 获取或设置是否打印列标题。
      printInfo.showColumnHeader(GC.Spread.Sheets.Print.PrintVisibilityType.hide);
      printInfo.showRowHeader(GC.Spread.Sheets.Print.PrintVisibilityType.hide);
      // 设置是否以黑白打印
      printInfo.blackAndWhite(false);
      // 打印截止
      let maxCol = 0;
      let maxRow = 0;
      const rowCount = sheet.getRowCount();
      const colCount = sheet.getColumnCount();
      for (let r = 0; r < rowCount; r += 1) {
        for (let c = 0; c < colCount; c += 1) {
          const style = sheet.getStyle(r, c);
          const value = sheet.getValue(r, c);
          if (style || value) {
            if (r > maxRow) maxRow = r;
            if (c > maxCol) maxCol = c;
          }
        }
      }
      _.map(sheet.pictures.all(), (item) => {
        const endRow = item.endRow();
        const endCol = item.endColumn();
        if (endRow > maxRow) maxRow = endRow;
        if (endCol > maxCol) maxCol = endCol;
      });
      printInfo.columnEnd(maxCol);
      printInfo.rowEnd(maxRow);
    });
    this.spread.resumePaint();
  }

  print = (showModal = true) => {
    this.initPrintInfo();
    if (showModal) {
      this.showOrHideModal({ printModalVisiable: true });
    } else {
      const spread = _.merge({}, this.spread);
      const willRemoveSheetsIndex = [];
      _.map(this.sheets, (index) => {
        if (index !== 0) {
          willRemoveSheetsIndex.unshift(index);
        }
      });
      _.map(willRemoveSheetsIndex, (index) => {
        spread.removeSheet(index);
      });
      spread.print();
    }
  }

  doPrint = () => {
    this.spread.print();
  }

  showExportTypeSelect = () => {
    if (this.state.exportTypeSelectVisiable) {
      return (
        <ExportTypeSelectModal
          showOrHideModal={this.showOrHideModal}
          export={this.doExport}
          intl={this.props.intl}
        />
      );
    }
  }

  showOrHideModal = (modelName) => {
    this.setState(modelName);
  }

  showPrintSetting = () => {
    if (this.state.printModalVisiable) {
      return (
        <PrintSettingModal
          sheets={this.sheets}
          spread={this.spread}
          showOrHideModal={this.showOrHideModal}
          intl={this.props.intl}
        />
      );
    }
  }

  restoreEditedContent = () => {
    const sheet = this.spread.getActiveSheet();
    _.map(this.modifiedDTData, (item) => {
      const { row, col, value } = item;
      if (value !== undefined) {
        sheet.getCell(row, col).value(value);
      }
    });
  }

  setEditingTextValue = () => {
    const sheet = this.spread.getActiveSheet();
    if (sheet.getSelections().length) {
      const { row, col } = sheet.getSelections()[0];
      const { row: editRow, col: editCol, editingText } = this.editingObj || {};
      if (editingText !== undefined && row === editRow && col === editCol) {
        const value = Util.numberTransform(editingText);
        const cell = sheet.getCell(row, col);
        cell.value(value);
        this.editingObj = null;
      }
    }
    this.spread.getActiveSheet().clearSelection();
  }

  /**
   * 关系型数据 修改存储所有内容api
   */
  sqlUpdateAllData = ({ dataSource, tableName, condition }) => {
    const { intl, sqlInfo } = this.props;
    // 过滤得到当前数据源的修改记录
    const records = _.filter(this.modifiedAllDTData, (item) => item.tableName === dataSource);
    if (records.length) {
      this.setEditingTextValue(records);
      const list = [];
      const columnSet = new Set();
      const missingPKArr = [];
      let dataSourceId;
      let isPKChanged = false;
      const nfPrimaryKeys = {};
      _.map(records, (record) => {
        const obj = {};
        obj.params = [];
        obj.name = tableName || dataSource;
        const { columnName, curValue, value, primaryKeyObj, typeTransfer, tableName: tbName } = record;
        const { id, primaryKeys = [], pkTypeTransfer } = _.get(sqlInfo, dataSource, {});
        const key = `${obj.name}-${columnName}-${_.values(primaryKeyObj).join('-')}`;
        if (columnSet.has(key)) return;
        dataSourceId = id;
        if (!primaryKeys.length) {
          missingPKArr.push(`${columnName}(${curValue})`);
        } else if (value !== undefined) {
          // 修改列
          columnSet.add(key);
          obj.column = intl.formatMessage(messages.changedTo, { name: columnName, value });
          obj.params.push({ name: columnName, value });
          const columnStr = this.generateColumn(typeTransfer, columnName);
          this.checkDataTablePrimaryKeys(primaryKeyObj, nfPrimaryKeys, tbName);

          // 条件子句
          const primaryKeyArr = this.generatePKArr({ obj: primaryKeyObj, params: obj.params, transfer: pkTypeTransfer });
          obj.primaryKeyObj = primaryKeyObj;
          obj.conditionDetail = condition || primaryKeyArr.join(' and ');
          obj.sql = `update ${obj.name} set ${columnStr} where ${obj.conditionDetail}`;
          list.push(obj);
          if (!isPKChanged) isPKChanged = primaryKeyObj[columnName];
        }
      });

      if (_.keys(nfPrimaryKeys).length) {
        let notiMsg = '';
        _.map(nfPrimaryKeys, (obj, key) => {
          notiMsg += `${intl.formatMessage(messages.sqlParamsNoKeys, { datasource: key, keys: obj.join(', ') })}`;
        });
        notification.warning({ message: `${intl.formatMessage(messages.sqlParamsNotFound)} ${notiMsg}` });
        return;
      }

      if (missingPKArr.length) {
        notification.warning({ message: `${intl.formatMessage(messages.field)}：${missingPKArr.join('，')}， ${intl.formatMessage(messages.noPKMsg)}` });
      } else {
        this.generateConfirmContent(dataSourceId, list);
      }
    }
  }

  checkDataTablePrimaryKeys = (primaryKeyObj, nfPrimaryKeys, tableName) => {
    _.map(primaryKeyObj, (value, key) => {
      if (value === 'params_key_null') {
        if (nfPrimaryKeys[tableName]) {
          if (!nfPrimaryKeys[tableName].includes(key)) nfPrimaryKeys[tableName].push(key);
        } else {
          nfPrimaryKeys[tableName] = [key];
        }
      }
    });
  }

  // eslint-disable-next-line
  generateConfirmContent = (id, list) => {
    // const { intl } = this.props;
    // const newList = [];
    // _.map(list, (obj) => {
    //   exec({
    //     id,
    //     params: obj.params,
    //     sql: `select count(*) COUNT from ${obj.name} where ${obj.conditionDetail}`
    //   }).then((res) => {
    //     if (res && +res.code === 200) {
    //       obj.count = _.get(res.data, 'dataSource[0].COUNT', 0);
    //       newList.push(this.generateSingleContent(obj));
    //       if (newList.length === list.length) {
    //         this.confirmContent = `${newList.join(';<br />')}. <br />${intl.formatMessage(messages.whetherToContinue)}`;
    //         this.showOrHideConfirmModal('updateConfirmModal', true);
    //         this.id = id;
    //         this.list = list;
    //         this.type = 'update';
    //       }
    //     }
    //   });
    // });
  }

  generateSingleContent = (obj) => {
    const { intl } = this.props;
    const pkStr = [];
    _.map(obj.primaryKeyObj, (v, k) => {
      pkStr.push(`${k}: ${v}`);
    });
    return `${obj.name}(${pkStr.join(', ')}), ${intl.formatMessage(messages.updatedRows)}：${obj.count}`;
  }

  /**
   * 关系型数据 修改/删除内容api
   * 参数：
   * dataSource(必填)：数据源名称
   * row（必填）： 行号、
   * tableName（选填，默认数据源名称）： 修改的单表名称、
   * condition（选填，默认为指定的主键）： 修改的条件，
   * sql（选填)
   * type: update/delete
   */
  sqlUpdateData = ({ dataSource, row: editRow, tableName, condition, sql, type = 'update' }) => {
    const { intl, sqlInfo } = this.props;
    // 过滤得到当前行和当前数据源的修改记录
    const records = _.filter(this.modifiedDTData, (item) => item.row === editRow && item.tableName === dataSource);
    if (records.length) {
      this.setEditingTextValue(records);
      const { primaryKeyObj } = records[0];
      const { id, primaryKeys = [], pkTypeTransfer } = _.get(sqlInfo, dataSource, {});
      if (!primaryKeys.length) {
        notification.warning({ message: intl.formatMessage(messages.noPKMsg) });
      } else {
        let shouldUpdate = true;
        const columnArr = [];
        const columnSet = new Set();
        const obj = {};
        obj.sql = sql;
        obj.params = [];
        const nfPrimaryKeys = {};
        if (!obj.sql) {
          // 修改列
          if (type === 'update') {
            _.map(records, (item) => {
              const { value, columnName, typeTransfer, tableName: tbName } = item;
              this.checkDataTablePrimaryKeys(primaryKeyObj, nfPrimaryKeys, tbName);
              if (value !== undefined && !columnSet.has(columnName)) {
                columnSet.add(columnName);
                obj.params.push({ name: columnName, value });
                obj.column = obj.column
                  ? `${obj.column}, ${intl.formatMessage(messages.changedTo, { name: columnName, value })}`
                  : intl.formatMessage(messages.changedTo, { name: columnName, value });
                // 列转换
                columnArr.push(this.generateColumn(typeTransfer, columnName));
              }
            });
            if (!columnArr.length) shouldUpdate = false;
          }

          if (_.keys(nfPrimaryKeys).length) {
            let notiMsg = '';
            _.map(nfPrimaryKeys, (item, key) => {
              notiMsg += `${intl.formatMessage(messages.sqlParamsNoKeys, { datasource: key, keys: item.join(', ') })}`;
            });
            notification.warning({ message: `${intl.formatMessage(messages.sqlParamsNotFound)} ${notiMsg}` });
            return;
          }

          // 条件子句
          const primaryKeyArr = this.generatePKArr({ obj: primaryKeyObj, params: obj.params, transfer: pkTypeTransfer });
          obj.primaryKeyObj = primaryKeyObj;
          obj.name = tableName || dataSource;
          obj.conditionDetail = condition || primaryKeyArr.join(' and ');

          if (type === 'update') {
            if (!shouldUpdate) return;
            obj.sql = `update ${obj.name} set ${columnArr.join(', ')} where ${obj.conditionDetail}`;
            this.isPKChanged = _.find(primaryKeyObj, (v, k) => columnSet.has(k));
          } else if (type === 'delete') {
            obj.sql = `delete from ${obj.name} where ${obj.conditionDetail}`;
          }
          // 校验查询
          this.sqlQueryCount(id, obj);
          this.type = type;
          this.id = id;
          this.list = [obj];
          this.editRow = editRow;
          this.tableName = dataSource;
        }
      }
    }
  }

  modalUpdateConfirm = () => {
    const { intl } = this.props;
    const { updateConfirmModal = false } = this.state;
    if (updateConfirmModal) {
      return (
        <ConfirmModal
          content={this.confirmContent}
          intl={intl}
          onOk={this.sqlUpdate}
          onCancel={this.showOrHideConfirmModal.bind(this, 'updateConfirmModal', false)}
        />
      );
    } else {
      return null;
    }
  }

  modalDelete = () => {
    const { intl } = this.props;
    const { deleteModal = false } = this.state;
    if (deleteModal) {
      return (
        <ConfirmModal
          content={this.confirmContent}
          intl={intl}
          onOk={this.sqlDelete}
          onCancel={this.showOrHideConfirmModal.bind(this, 'deleteModal', false)}
        />
      );
    } else {
      return null;
    }
  }

  showOrHideConfirmModal = (modal, value) => {
    this.setState({ [modal]: value }, () => {
      this.restoreEditedContent();
    });
  }

  // eslint-disable-next-line
  sqlQueryCount = (id, obj) => {
    // const { intl } = this.props;
    // exec({
    //   id,
    //   params: obj.params,
    //   sql: `select count(*) COUNT from ${obj.name} where ${obj.conditionDetail}`
    // }).then((res) => {
    //   if (res && +res.code === 200) {
    //     obj.count = _.get(res.data, 'dataSource[0].COUNT', 0);
    //     if (this.type === 'update') {
    //       this.confirmContent = `${this.generateSingleContent(obj)}. <br />${intl.formatMessage(messages.whetherToContinue)}`;
    //       this.showOrHideConfirmModal('updateConfirmModal', true);
    //     } else if (this.type === 'delete') {
    //       this.confirmContent = intl.formatMessage(messages.Rule10, { count: obj.count });
    //       this.showOrHideConfirmModal('deleteModal', true);
    //     }
    //   }
    // });
    // this.clearParam();
  }

  sqlUpdate = () => {
    // this.showOrHideConfirmModal('updateConfirmModal', false);
    // const { intl } = this.props;
    // update({ id: this.id, list: this.list }).then((res) => {
    //   if (res && !res.error) {
    //     const success = intl.formatMessage(commonMessage.editSuccess);
    //     const pkChangedMsg = intl.formatMessage(messages.pkChangedMsg);
    //     notification.success({ message: `${success}! ${this.isPKChanged ? pkChangedMsg : ''}` });
    //   } else if (res && res.msg) {
    //     notification.warning({
    //       message: intl.formatMessage(messages.updateFailed),
    //       description: res.msg,
    //       className: styles.notification
    //     });
    //   }
    // });
    // this.clearParam(true);
  }

  sqlDelete = () => {
    // this.showOrHideConfirmModal('deleteModal', false);
    // const { intl, fetchDatatableData } = this.props;
    // update({ id: this.id, list: this.list }).then((res) => {
    //   if (res && !res.error) {
    //     notification.success({ message: intl.formatMessage(commonMessage.deleteSuccess) });
    //     if (fetchDatatableData) {
    //       fetchDatatableData({ firstQuery: false });
    //     }
    //   } else if (res && res.msg) {
    //     notification.warning({
    //       message: intl.formatMessage(messages.deleteFailed),
    //       description: res.msg,
    //       className: styles.notification
    //     });
    //   }
    // });
    // this.clearParam(true);
  }

  clearParam = (isReset) => {
    if (isReset) {
      if (this.editRow !== null) {
        this.modifiedDTData = _.pickBy(this.modifiedDTData, (item) => item.tableName === this.tableName && item.row !== this.editRow);
        this.modifiedAllDTData = _.pickBy(this.modifiedAllDTData, (item) => item.tableName === this.tableName && item.row !== this.editRow);
      } else {
        this.modifiedAllDTData = {};
        this.modifiedDTData = {};
      }
    }
    this.id = null;
    this.list = null;
    this.type = null;
    this.isPKChanged = null;
    this.count = null;
    this.confirmContent = null;
    this.tableName = null;
    this.editRow = null;
  }

  generateColumn = (typeTransfer, columnName) => {
    if (typeTransfer && !/cast/.test(typeTransfer.toLowerCase())) {
      const col = `\${${columnName}}`;
      if (/[$]{3}/.test(typeTransfer)) {
        // 值转换
        return `${columnName} = ${typeTransfer.replace('$$$', col)}`;
      } else if (/[$]{2}/.test(typeTransfer)) {
        // 名称转换
        return `${typeTransfer.replace('$$', columnName)} = \${${columnName}}`;
      }
    } else {
      return `${columnName} = \${${columnName}}`;
    }
  }

  generatePKArr = ({ obj, params, transfer }) => {
    const result = [];
    _.map(obj, (v, k) => {
      const param = `\${param-${k}}`;
      params.push({ name: `param-${k}`, value: v });
      if (v === null) {
        result.push(`${k} is ${param}`);
      } else if (transfer && transfer[k]) {
        if (/[$]{3}/.test(transfer[k])) {
          result.push(`${k} = ${transfer[k].replace('$$$', param)}`);
        } else if (/[$]{2}/.test(transfer[k])) {
          result.push(`${transfer[k].replace('$$', k)} = ${param}`);
        }
      } else {
        result.push(`${k} = ${param}`);
      }
    });
    return result;
  }

  reported = () => {
    // eslint-disable-next-line no-unused-vars
    const { intl } = this.props;
    const result = [];
    _.map(_.values(this.modifiedHISData), (item) => {
      delete item.type;
      const newItem = _.merge({}, item);
      result.push(newItem);
    });
    if (result.length) {
      // editHistoryData(result).then((res) => {
      //   if (res && !res.error) {
      //     notification.success({ message: intl.formatMessage(commonMessage.editSuccess) });
      //   } else {
      //     notification.warning({
      //       message: intl.formatMessage(messages.updateFailed),
      //       description: res.msg,
      //       className: styles.notification
      //     });
      //     console.error(res);
      //   }
      // });
    }
  }

  designInit = () => {
    return this.spread.toJSON();
  }

  setReportInfo = (config, objects = this.dataSource) => {
    const propArr = [];
    const serviceArr = [];
    const customService = {};
    const dataTable = {};
    const statisticTask = {};
    const dynamicDataSource = {};
    const systemInfo = {};
    const entityObjects = {};
    _.map(objects, (value, key) => {
      if (key === 'cellInfo') {
        _.map(value, (item) => {
          const { dataSource, type } = item;
          switch (type) {
            case 'DT': {
              if (item.dataTable) {
                if (dataTable[item.dataTable]) {
                  const { pageSize } = dataTable[item.dataTable];
                  item.pageSize = Math.max(pageSize || 200, item.pageSize || 200);
                }
                this.getDynamicDataSource(type, item.tableName, dynamicDataSource);
                dataTable[item.dataTable] = item;
              }
              break;
            }
            case 'RTS': {
              const { taskName, source, statisticalType, limit } = item;
              if (dataSource && taskName && source && statisticalType) {
                statisticTask[dataSource] = { taskName, source, statisticalType, limit };
              }
              break;
            }
            case 'Property': {
              item.originalSelectedObject = Util.getOriDataSource(dataSource, 'HIS');
              propArr.push(item);
              break;
            }
            case 'Service': {
              const newItem = _.cloneDeep(item);
              const data = item.dataSource.replace(/\./g, ':');
              newItem.originalSelectedObject = Util.getOriDataSource(dataSource, 'SER');
              newItem.inputs = _.get(objects.serviceInput, data);
              serviceArr.push(newItem);
              this.getDynamicDataSource(type, data, dynamicDataSource, newItem.originalSelectedObject);
              break;
            }
            case 'System': {
              const arr = _.filter(_.get(objects, `allDataSource.${this.props.intl.formatMessage(messages.systemBasic)}.list`), (o) => o.propertyName === dataSource);
              systemInfo[dataSource] = _.get(arr, '[0]', []);
              break;
            }
            case 'CustomService': {
              const [propName] = item.dataSource.replace(/\./g, ':').split(':');
              const newItem = _.cloneDeep(item);
              newItem.sourceUrl = _.get(objects, `allDataSource.${propName}.sourceUrl`);
              customService[propName] = newItem;
              break;
            }
            case 'EntityObjects': {
              const [, propCode, ...paramArr] = item.dataSource.replace(/@#@/g, '@:@').split('@:@');
              const newItem = _.cloneDeep(item);
              // eslint-disable-next-line no-shadow
              const paramCode = _.filter(paramArr, (item, idx) => (idx % 2)).join('#');
              // newItem.paramName = paramName;
              newItem.paramCode = paramCode;
              newItem.propCode = propCode;
              entityObjects[dataSource] = newItem;
              break;
            }
            default: break;
          }
        });
      } else if (key === 'dataSourceInfo') {
        config.dataSourceInfo = { addDataSource: _.keys(value) };
      } else {
        config[key] = value;
      }
    });
    config.dataTable = dataTable;
    config.statisticTask = statisticTask;
    config.object = [...propArr, ..._.uniqBy(serviceArr, 'dataSource')];
    // 存储表格查询和服务的联动入参
    this.saveDynamicDataSource(dynamicDataSource);
    config.dataSource = this.dataSource.dataSource;
    config.systemInfo = systemInfo;
    config.customService = customService;
    config.entityObjects = entityObjects;
    return config;
  }

  saveDynamicDataSource = (dynamicDataSource) => {
    const list = _.values(dynamicDataSource);
    list.unshift({
      id: 'link_date',
      selectedProp: {
        name: '_date_',
        inputs: ['minDate', 'maxDate']
      },
      subType: 'other'
    });
    this.dataSource.dataSource = { dynamicDataSource: list };
  }

  getDynamicDataSource = (dataType, dataSource, dynamicDataSource, originalSelectedObject) => {
    const { sqlInfo, serviceInput } = this.dataSource;
    if (dataType === 'Service' && serviceInput[dataSource]) {
      const array = serviceInput[dataSource] || [];
      if (array.length) {
        _.set(originalSelectedObject, 'selectedProp.inputs', array);
        _.set(originalSelectedObject, 'selectedInstance.showName', _.get(originalSelectedObject, 'selectedInstance.name'));
        _.set(originalSelectedObject, 'selectedProp.showName', _.get(originalSelectedObject, 'selectedProp.name'));
        _.set(originalSelectedObject, 'selectedTemplate.showName', _.get(originalSelectedObject, 'selectedTemplate.name'));
        dynamicDataSource[dataSource] = originalSelectedObject;
      }
    } else if (dataType === 'DT' && sqlInfo[dataSource]) {
      const info = sqlInfo[dataSource];
      if (info.params && info.params.length) {
        dynamicDataSource[dataSource] = {
          id: `link_sqlInfo_${dataSource}`,
          selectedProp: {
            name: dataSource.replace(/\./g, ':'),
            inputs: info.params
          },
          subType: 'other'
        };
      }
    }
  }

  saveReport = (json, dataSource) => {
    const { isDesign, isPreview } = this.props;
    if (!isPreview && isDesign) {
      this.spread.fromJSON(this.updateInitJson(json));
    }
    this.props.setJsonData(json, dataSource);
  }

  designComplete = (json, dataSource) => {
    this.saveReport(json, dataSource);
    this.designAbort();
  }

  designAbort = () => {
    this.spread.getSheet(0).clearSelection();
    this.props.showOrHideModal({
      designerVisible: false
    });
  }

  exportSheet = () => {
    this.showOrHideModal({ exportTypeSelectVisiable: true });
  }

  doExport = (type) => {
    const fileName = this.getFileName();
    if (type === 'csv') {
      this.exportCSV(fileName);
    } else if (type === 'xlsx') {
      this.exportExcel(fileName);
    }
  }

  exportCSV = (fileName) => {
    const sheet = this.spread.getActiveSheet();
    const csvString = sheet.getCsv(0, 0, sheet.getRowCount(), sheet.getColumnCount(), '', ',');
    const csvData = new Blob([`\uFEFF${csvString}`], { type: 'text/csv' });
    saveAs(csvData, `${fileName}.csv`);
  }

  exportExcel = (fileName) => {
    const json = this.getExportJson();
    this.excelIO.save(json, (blob) => {
      saveAs(blob, `${fileName}.xlsx`);
    }, (e) => {
      console.error(e);
    });
  }

  getExportJson = () => {
    const json = this.spread.toJSON({ includeBindingSource: true });
    delete json.newTabVisible;
    delete json.showHorizontalScrollbar;
    delete json.showVerticalScrollbar;
    delete json.tabEditable;
    delete json.scrollbarMaxAlign;
    delete json.allowContextMenu;
    _.map(json.sheets, (sheet) => {
      // 去自定义公式
      // if (this.props.isPreview) {
      _.map(sheet.data.dataTable, (row, rowNum) => {
        _.map(row, (obj, colNum) => {
          const { formula } = obj;
          if (formula && /DT|HIS|DD|RT|SER|OPT|RTS/.test(formula)) delete sheet.data.dataTable[rowNum][colNum].formula;
        });
      });
      // }

      delete sheet.isProtected;
      delete sheet.protectionOptions;
      delete sheet.rowHeaderVisible;
      delete sheet.colHeaderVisible;
      delete sheet.gridline;
      delete sheet.sheetAreaOffset;
      delete sheet.selections;
    });
    return json;
  }

  reRender = () => {
    this.spread.refresh();
  }

  dropItem = (e) => {
    if (this.props.allowMove) {
      const client = e.target.getBoundingClientRect();
      const x = e.pageX - client.left;
      const y = e.pageY - client.top;
      const sheet = this.spread.getActiveSheet();
      const { row, col } = sheet.hitTest(x, y);
      const { value = {} } = sheet.getValue(row, col) || {};
      const formula = sheet.getFormula(row, col) || "";
      if (_.isNumber(x) && _.isNumber(y)) {
        const { upperName, moveItem } = this.props;
        const { propertyName, propertyType, primitiveType, namespace, propertyCode, inheritTree } = moveItem;
        let formulaValue;
        if (propertyName) {
          switch (propertyType) {
            case 'service':
            case 'template_service': formulaValue = `=SER("${upperName}.${propertyName}")`; break;
            case 'systemInfo': formulaValue = `=SYS("${propertyName}")`; break;
            case 'custom': formulaValue = `CTS("${upperName}.${propertyCode}")`; break;
            case 'entity': formulaValue = `ENT("${upperName}@:@${inheritTree}")`; break;
            case 'property':
            default: {
              if (propertyName === '_key_') {
                formulaValue = `=HIS("${upperName}.${propertyName}")`;
              } else if (['BOOLEAN', 'STRING', 'DATE', 'DATETIME'].includes(primitiveType)) {
                formulaValue = `=RT("${upperName}.${namespace}:${propertyName}#${primitiveType}")`;
              } else {
                formulaValue = `=RT("${upperName}.${namespace}:${propertyName}")`;
              }
              break;
            }
          }
        } else {
          formulaValue = `=DT("${upperName}.${moveItem}")`;
        }
        if (/^BC_(EAN8|EAN13|CODE39|CODE93|CODE49|CODE128|CODABAR|GS1_128|DATAMATRIX|QRCODE)\(/.test(formula) && value.type) {
          this.props.updateBarCodeFormula(formulaValue);
        } else
          this.props.basicOperate({
            opt: 'generateFormula',
            options: {
              formula: formulaValue,
              updatePane: true,
              selections: [new spreadNS.CellRange(sheet, row, col, 1, 1)]
            },
          });
        sheet.setActiveCell(row, col);
        this.props.updateCellFormula({ row, col });
      }
    }
  }

  itemMove = (e) => {
    if (this.props.allowMove) {
      const client = e.target.getBoundingClientRect();
      const x = e.pageX - client.left;
      const y = e.pageY - client.top;
      if (_.isNumber(x) && _.isNumber(y)) {
        const sheet = this.spread.getActiveSheet();
        const target = sheet.hitTest(x, y);
        sheet.setActiveCell(target.row, target.col);
      }
    }
  }

  render() {
    // eslint-disable-next-line no-unused-vars
    const { isPreview, isEdit, config } = this.props;
    return (
      <div
        className={styles.container}
        onKeyUp={this.handleKeyUp}
        onMouseUp={this.handleMouseUp}
      >
        {isEdit || isPreview ? null : <div className={styles.mask} />}
        <div
          ref={(node) => { this.spreadContainer = node; }}
          style={{ width: '100%', height: '100%' }}
          onMouseUp={this.dropItem}
          onMouseMove={this.itemMove}
        // {...(isPreview ? scriptUtil.getActionHandle(_.get(config, 'actions', ''), this) : {})}
        />
        {this.showPrintSetting()}
        {this.showExportTypeSelect()}
        {this.modalUpdateConfirm()}
        {this.modalDelete()}
      </div>
    );
  }
}
