import React, { Component } from 'react';
import { notification, message, Button } from 'sup-ui';
import _ from 'lodash';
import { injectIntl } from 'react-intl';
import '@grapecity/spread-sheets-resources-zh';
import '@grapecity/spread-sheets/styles/gc.spread.sheets.excel2013white.css';
import GC from '@grapecity/spread-sheets';
import Excel from '@grapecity/spread-excelio';
import { zip } from '../utils/utils';
import commonMessage from './commonMessages';
import * as Util from './ReportUtil.js';
import Reporter from './Reporter';
import ReportMenuBar from './ReportMenuBar';
import CellSettingModal from './Modal/CellSettingModal';
import BarCodeModal from './Modal/BarCodeModal';
import QrBarCodeModal from './Modal/QrBarCodeModal';
import CondFormatModal from './Modal/CondFormatModal';
import RowColSettingModal from './Modal/RowColSettingModal';
import AnalyzeModal from './Modal/AnalyzeModal';
import InsertFormulaModal from './DataSource/InsertFormulaModal';
import InsertDateFormulaModal from './DataSource/InsertDateFormulaModal';
import HighFilterModal from './Modal/HighFilterModal';
import DataSourceSetDrawer from './DataSource/DataSourceSetDrawer';
import InsertChartModal from './Modal/InsertChartModal';
import CloseDesignerModal from './Modal/CloseDesignerModal';
import ReportChartSet from './ReportChartSet';
import TagTriangleCell from './Cell/TagTriangleCell';
import styles from './Reporter.less';
import messages from './messages';
import Modal from './Modal/CommonModal';
import { systemInfo } from './DataSource/SystemInfoConfig';
import { qrBarCodeOption } from './Modal/BarCodeConfig';

window.GC = GC;
const spreadNS = GC.Spread.Sheets;

class ReportDesigner extends Component {
  constructor(props) {
    super(props);
    // GC.Spread.Common.CultureManager.culture('zh-cn');
    // this.json = props.spread.report.designInit();
    this.state = {
      curCellName: 'A1',
      unfoldDataSource: true,
      formula: null,
      allowMove: false,
      moveItem: null,
      moveX: 0,
      moveY: 0,
      upperName: '',
      showInsertChartModal: false,
      closeDesignerVisible: false,
      chartConfigArea: false,
      highFilterVisible: false,
      json: {},
      barCodeObject: {
        font: {},
        quietZone: {}
      },
      realValue: 1234567,
      qrBarCodeObject: {
        isQrCode: true
      }
    };
    const { empty, thin, dotted, dashed, double } = spreadNS.LineStyle;
    this.borderStyleMap = {
      none: empty,
      solid: thin,
      dotted,
      dashed,
      double
    };
  }

  componentDidMount() {
    GC.Spread.Common.CultureManager.culture(localStorage.getItem('language'));
    this.spread = this.reporter.spread;
    this.fbx = new spreadNS.FormulaTextBox.FormulaTextBox(this.formulabox);
    this.fbx.workbook(this.spread);
    this.registerCommands();
    // this.sheets = this.props.spread.report.getSheetsInfo(this.spread);
    // // bind event
    // this.bindSpreadEvent();
    // this.bindSheets = [];
    // for (let index = 0; index < this.spread.getSheetCount(); index += 1) {
    //   this.bindSheetEvent(this.spread.getSheet(index));
    // }
    setTimeout(() => {
      this.reporter.reRender();
    }, 100);
    // 格式刷
    document.onmousedown = (e) => {
      e = e || window.event;
      if (e.button === 2) {
        this.doubleClickBrush = false;
      }
    };
    this.initReporter(this.props);
  }

  componentWillReceiveProps(nextProps) {
    this.initReporter(nextProps);
  }

  initReporter = (props) => {
    const { config } = props;
    this.getConfigs(config);
    this.setState({
      unfoldDataSource: false,
      json: _.get(config, 'json')
    }, () => {
      this.init();
    });
  }

  getReportInfo = (props = this.props) => {
    const { data } = props;
    return {
      reportInfo: _.get(data, data.id, {})
    };
  }

  init = () => {
    this.bindSpreadEvent();
    this.sheets = this.reporter.getSheetsInfo(this.spread);
    // bind event
    this.bindSheets = [];
    for (let index = 0; index < this.spread.getSheetCount(); index += 1) {
      this.bindSheetEvent(this.spread.getSheet(index));
    }
    const activeSheet = this.spread.getActiveSheet();
    activeSheet.setActiveCell(0, 0);
    this.initStatus({ sheet: activeSheet });
    this.setFrozenStatus(activeSheet);
  }

  initStatus = ({ sheet, info }) => {
    if (sheet && !info) [info] = sheet.getSelections();
    if (info) {
      this.setMenubarStatus(info);
      this.updateCellFormula(info);
      this.backfillConfig(sheet, info);
    }
  }

  onSplitterDragEnd() {
    this.spread.refresh();
  }

  onMerge = ({ selections }) => {
    const sheet = this.spread.getActiveSheet();
    sheet.suspendPaint();
    selections.forEach((range) => {
      const ranges = sheet.getSpans(range);
      const spanCount = ranges.length;
      let mergable = false;

      if (spanCount > 1 || (spanCount === 0 && (range.rowCount > 1 || range.colCount > 1))) {
        mergable = true;
      } else if (spanCount === 1) {
        const [range2] = ranges;
        if (range2.row !== range.row || range2.col !== range.col
          || range2.colCount !== range.colCount) {
          mergable = true;
        }
      }
      if (spanCount > 0) {
        this.unmergeCells(range, sheet);
      } else if (mergable) {
        this.mergeCells(range, sheet);
      }
      this.updatePositionBox();
    });
    sheet.resumePaint();
  }

  onSave = (isEnable = false, callback) => {
    // eslint-disable-next-line no-unused-vars
    const { config, intl } = this.props;
    if (config) {
      this.beforeSave();
      const json = this.reporter.updateInitJson(this.toJsonForSave());
      const zipJson = zip(json);
      this.spread.fromJSON(json);
      const newConfig = this.reporter.setReportInfo(config);
      newConfig.json = zipJson;
      // console.log(newConfig);
      this.props.saveTemplate(newConfig, isEnable, callback);
      this.init();

      // 检测错误数据源
      // const { cellInfo } = this.reporter.dataSource;
      // const { keySet } = this.analyzeFunction(cellInfo);
      // const noHasList = this.fetchAllDataSource(config, Array.from(keySet), cellInfo) || [];
      // if (noHasList && noHasList.length) {
      //   const msg = intl.formatMessage(commonMessage.saveSqlFail, { cells: noHasList.join(',') });
      //   notification.warning({
      //     message: intl.formatMessage(messages.DataSourceConnectionFailed),
      //     description: (<div dangerouslySetInnerHTML={{ __html: msg }} />),
      //     className: styles.notification
      //   });
      // }
    } else {
      message.error(this.props.intl.formatMessage(commonMessage.saveFail));
    }
  }

  onExit = () => {
    this.showOrHideModal({ closeDesignerVisible: true });
  }

  onExport = () => {
    this.reporter.exportSheet();
  }

  closeDesignerVisible = () => {
    this.setState({
      closeDesignerVisible: true
    });
  }

  onSaveClose = () => {
    this.onSave(false, () => {
      this.onClose();
    });
  }

  onClose = () => {
    window.close();
    // 更新列表
    window.opener.postMessage({ action: 'refresh' }, '*');
    // this.props.goBackToPrevPage();
  }

  onPrint = () => {
    this.reporter.print();
    this.init();
  }

  onOpen = (param) => {
    this.isPicture = param === 'picture';
    this.fileSelector.click();
  }

  setCodeFormat = (key, value) => {
    let val = value;
    if (key === 'color') {
      if (!value) val = ',';
      else val = `"${value}",`;
    } else if (key === 'position') {
      if (value !== null && !isNaN(value)) val = `${value},`;
      else val = ',';
    }
    return val;
  }

  createBarCode = (barCodeObject) => {
    const { isQrCode, type, color, backgroundColor, quietZone = {},
      realValue: value = "", showLabel, labelPosition, font = {}, isFormula } = barCodeObject;
    const { left, right, top, bottom } = quietZone;
    const { fontFamily, fontStyle, fontWeight, textDecoration, textAlign, fontSize } = font;
    if (!(String(value).split('"').length % 2)) {
      message.info(this.props.intl.formatMessage(messages.barCodeValueErrTip));
      return;
    }
    this.spread.suspendPaint();
    const sheet = this.spread.getActiveSheet();
    const { row, col } = this.getCellRange(sheet) || {};
    const fontColor = this.setCodeFormat('color', color);
    const bgColor = this.setCodeFormat('color', backgroundColor);
    const l = this.setCodeFormat('position', left);
    const r = this.setCodeFormat('position', right);
    const t = this.setCodeFormat('position', top);
    const b = this.setCodeFormat('position', bottom);
    let realValue = value == "" ? 1234567 : `"${value}"`;
    const isBindData = /^(ENT|SYS)\(/.test(value);
    let formula = ''
    if (isQrCode) {
      // 二维码
      let comma = ',,,,,,,,';
      if (type === 'DATAMATRIX') {
        comma = ',,,,,,';
      }
      if (isFormula) {
        realValue = value;
        if (isBindData && value.indexOf('@_@') > -1) {
          const formulaArr = value.match(/^(.*?)@_@(.*?)\"\)/);
          if (formulaArr) realValue = value.replace(`@_@${formulaArr[2]}`, '');
        }
      }
      formula = `BC_${type}(${realValue},${fontColor}${bgColor}${comma}${l}${r}${t}${b})`;
      sheet.setFormula(row, col, formula);
    } else {
      // 条形码
      const sl = `${showLabel || false},`;
      const lp = this.setCodeFormat('color', labelPosition);
      const ff = this.setCodeFormat('color', fontFamily);
      const fstyle = this.setCodeFormat('color', fontStyle);
      const fw = this.setCodeFormat('color', fontWeight);
      const td = this.setCodeFormat('color', textDecoration);
      const ta = this.setCodeFormat('color', textAlign);
      const fsize = this.setCodeFormat('color', fontSize);
      const bt = bottom || '';
      const { labelWithStartAndStopCharacter, checkDigit, nwRatio, fullASCII, addOn, addOnLabelPosition, grouping, groupNo, codeSet } = barCodeObject;
      let centerFormula = `${sl}${lp}`;
      if (type === 'EAN13') {
        realValue = value == "" ? 6920312296219 : `"${value}"`;
        centerFormula += `${this.setCodeFormat('color', addOn)}${this.setCodeFormat('color', addOnLabelPosition)}`
      } else if (type === 'CODE39') {
        centerFormula += `${labelWithStartAndStopCharacter || false},`;
        centerFormula += `${checkDigit || false},`;
        centerFormula += this.setCodeFormat('color', nwRatio);
        centerFormula += `${fullASCII || false},`;
      } else if (type === 'CODE93') {
        centerFormula += `${checkDigit || false},`;
        centerFormula += `${fullASCII || false},`;
      } else if (type === 'CODE49') {
        centerFormula += `${grouping || false},`;
        centerFormula += this.setCodeFormat('color', groupNo);
      } else if (type === 'CODE128') {
        realValue = value == "" ? 225869 : `"${value}"`;
        centerFormula += this.setCodeFormat('color', codeSet);
      } else if (type === 'CODABAR') {
        centerFormula += `${checkDigit || false},`;
        centerFormula += this.setCodeFormat('color', nwRatio);
      }
      if (isFormula) {
        if (isBindData) {
          realValue = value;
          if (value.indexOf('@_@') > -1) {
            const formulaArr = value.match(/^(.*?)@_@(.*?)\"\)/);
            if (formulaArr) realValue = value.replace(`@_@${formulaArr[2]}`, '');
          }
        } else if (!/^BAR\(/.test(value)) realValue = `BAR(${value})`;
        else realValue = value;
      }
      formula = `BC_${type}(${realValue},${fontColor}${bgColor}${centerFormula}${ff}${fstyle}${fw}${td}${ta}${fsize}${l}${r}${t}${bt})`;
      sheet.setFormula(row, col, formula);
    }
    this.spread.resumePaint();
    this.setState({
      qrBarCodeVisiable: false,
      barCodeVisiable: false,
      realValue: value,
      formula
    });
    if (value == "") {
      message.success(isQrCode ? this.props.intl.formatMessage(messages.qrBarCodeBindTip) : this.props.intl.formatMessage(messages.barCodeBindTip));
    }
    const v = sheet.getValue(row, col) || '';
    if (v && typeof v === 'object' && v._error === "#VALUE!") {
      message.info(this.props.intl.formatMessage(messages.barCodeErrorTip));
    }
  }

  onOpenBarCodeByType = (isQrCode) => {
    const sheet = this.spread.getActiveSheet();
    const { row, col } = this.getCellRange(sheet) || {};
    const cellValue = sheet.getValue(row, col) || '';
    const formula = sheet.getFormula(row, col) || "";
    const showType = isQrCode ? 'QRCODE' : 'EAN8';
    const barCodeKey = isQrCode ? 'qrBarCodeObject' : 'barCodeObject';
    const newState = {
      qrBarCodeVisiable: isQrCode,
      barCodeVisiable: !isQrCode,
      [barCodeKey]: {
        realValue: cellValue,
        type: showType,
        isQrCode
      }
    };
    if (
      cellValue &&
      typeof cellValue === 'object' &&
      /^BC_(EAN8|EAN13|CODE39|CODE93|CODE49|CODE128|CODABAR|GS1_128|DATAMATRIX|QRCODE)/.test(formula)
    ) {
      const { value } = cellValue;
      if (value && typeof value === 'object' && value.type) {
        let realType = value.type || '';
        if (value.type) realType = String(realType).toUpperCase();
        if (
          (!isQrCode && qrBarCodeOption.includes(realType)) ||
          (isQrCode && !qrBarCodeOption.includes(realType))
        ) {
          realType = showType;
        }
        let showValue = value.text;
        let isFormula = false;
        const isTrueFormula = formula.match(/^BC_(\w+)\((\w+)\((.*?)\)(.*?)\)/);
        const formulaArr = formula.match(/^BC_(\w+)\((\w+)\((.*?)\),/);
        if (isTrueFormula && formulaArr[2]) {
          isFormula = true;
          showValue = `${formula.match(/^BC_(\w+)\((.*?)\),/)[2]})`;
        }
        newState[barCodeKey] = {
          ...value,
          isFormula,
          realValue: showValue,
          type: realType,
          isQrCode
        }
      } else {
        const formulaArr = formula.match(/^BC_(\w+)\(/);
        newState[barCodeKey] = {
          realValue: '',
          isFormula: false,
          type: formulaArr && !isQrCode ? formulaArr[1] : showType,
          isQrCode
        }
      }
    } else {
      if (formula) {
        newState[barCodeKey] = {
          realValue: formula,
          isFormula: true,
          type: showType,
          isQrCode
        }
      }
    }
    this.setState(newState);
  }

  updateBarCodeFormula = (formulaValue = '') => {
    const sheet = this.spread.getActiveSheet();
    const { row, col } = this.getCellRange(sheet) || {};
    const { value = {} } = sheet.getValue(row, col) || {};
    const formula = sheet.getFormula(row, col) || "";
    let isFormula = false;
    const formulaArr = formula.match(/^BC_(\w+)\((.*?)\)/);
    if (formula && formulaArr && formulaArr[2]) {
      isFormula = true;
    }
    this.createBarCode({
      ...value,
      isFormula,
      isQrCode: /^BC_(DATAMATRIX|QRCODE)/.test(formula),
      realValue: formulaValue,
      type: value.type ? String(value.type).toUpperCase() : ''
    })
  }

  onSheetLoad = (spread) => {
    const sheet = spread.getActiveSheet();
    this.cell = sheet.getCell(0, 0);
  }

  onFileSelected = (e) => {
    const file = e.target.files[0];
    if (this.isPicture) {
      if (!/image\/\w+/.test(file.type)) {
        notification.warning({ message: this.props.intl.formatMessage(messages.ChooseImage) });
        return false;
      }
      this.processPictureAdded(file);
    } else {
      const fileName = file.name;
      const index = fileName.lastIndexOf('.');
      const fileExt = fileName.substr(index + 1).toLowerCase();
      if (fileExt === 'json' || fileExt === 'ssjson') {
        this.importSpreadFromJSON(file);
      } else if (fileExt === 'xlsx') {
        this.importSpreadFromExcel(file);
      } else if (fileExt === 'csv') {
        this.importSpreadFromCsv(file);
      } else {
        notification.warning({ message: this.props.intl.formatMessage(messages.SupportFormat) });
      }
    }
    e.target.value = null;
  }

  onOpenChartModal = () => {
    this.setState({
      // eslint-disable-next-line react/no-access-state-in-setstate
      showInsertChartModal: !this.state.showInsertChartModal
    });
  }

  getConfigs = (config) => {
    if (!this.reporter.dataSource) return;
    this.reporter.dataSource = {
      ...this.reporter.dataSource,
      allDataSource: this.setSystemBasicInfo(config),
      serviceInput: config.serviceInput || {},
      sqlInfo: config.sqlInfo || {},
      customServices: config.customServices || {}
    };
  }

  setSystemBasicInfo = (config) => {
    const { intl } = this.props;
    const allDataSource = {
      ...config.allDataSource,
      [`${intl.formatMessage(messages.systemBasic)}`]: {
        list: systemInfo(intl),
        propertyType: 'systemInfo'
      }
    };
    return allDataSource;
  }

  setCellValue = ({ row, col, value, index }) => {
    this.spread.getSheet(index).setValue(row, col, value);
  }

  getCellRange = (sheet = this.spread.getActiveSheet()) => {
    if (sheet.getSelections().length) {
      const { row, col, rowCount, colCount } = sheet.getSelections()[0];
      return new spreadNS.CellRange(sheet, row, col, rowCount, colCount);
    }
  }

  getMenubarStatus = ({ row, col }) => {
    const info = {};
    if (row !== undefined && col !== undefined) {
      info.fontStyles = this.getFontStyles({ row, col });
      info.alignment = this.getAlignmentInfo({ row, col });
      info.bgColor = this.getColor({ row, col }, 'backColor');
      this.isMerged = this.isMergedCell({ row, col });
    }
    this.info = info;
    return info;
  }

  setMenubarStatus = ({ row, col }) => {
    this.menuBar.updateStatus(this.getMenubarStatus({ row, col }));
    this.menuBar.updateTransaction();
  }

  setCellSettings = ({ changedTabs, format, fontStyles, borderStyles, alignment, bgColor, cellTypes, selections, authority }) => {
    const { numberType, font, border, align, background, cellType } = changedTabs;

    // 类型
    if (cellType) {
      this.changeCellType(cellTypes.type, cellTypes.comboItems);
    }

    // 图案
    if (background) {
      this.setColor({ prop: 'backColor', value: bgColor, selections });
    }

    // 设置对齐方式
    if (align) {
      const { vAlign, hAlign, isWrap, isVerticalText } = alignment;
      this.setAlignment({ prop: 'vAlign', value: vAlign, selections });
      this.setAlignment({ prop: 'hAlign', value: hAlign, selections });
      this.changeWritingMode(isVerticalText);
      this.wordWrap({ isWrap, selections });
    }

    // 设置边框
    if (border) {
      const { borderColor, borderStyle, borderOptions } = borderStyles;
      _.map(borderOptions, (value, key) => {
        if (value) {
          this.setBorderLines({ lineStyle: this.borderStyleMap[borderStyle], borderColor, borderType: key, selections });
        } else {
          this.setBorderLines({ lineStyle: this.borderStyleMap.none, borderType: key, selections });
        }
      });
    }

    // 设置字体
    if (font) {
      const { fontColor, fontFamily, fontSize, fontWeight, fontStyle, underline, lineThrough } = fontStyles;
      this.setColor({ prop: 'foreColor', value: fontColor, selections });
      const styleObj = [
        { prop: 'font-weight', value: fontWeight },
        { prop: 'font-style', value: fontStyle },
        { prop: 'font-family', value: fontFamily },
        { prop: 'font-size', value: fontSize }
      ];
      styleObj.forEach((item) => {
        const { prop, value } = item;
        if ((item.prop === 'font-weight' && !fontWeight) || (item.prop === 'font-style' && !fontStyle)) return;
        this.setFontStyle({ prop, value, selections });
      });
      this.setTextDecoration({ underline, lineThrough, settingType: 'cellSetting', selections });
    }

    // 设置数字
    if (numberType) {
      this.setNumberFormat(numberType, format, selections);
    }

    //  设置权限
    if (!_.isEmpty(authority)) {
      this.setAuthority(authority);
    }
  }

  setAuthority = (authority) => {
    const sheet = this.spread.getActiveSheet();
    const tags = sheet.tag();
    const oldAuthority = _.get(tags, 'authorityConf') || [];
    const newAuthority = [];
    _.map(sheet.getSelections(), (range) => {
      const { row, col, rowCount, colCount } = range;
      for (let r = row; r < row + rowCount; r += 1) {
        for (let c = col; c < col + colCount; c += 1) {
          this.setCellTags({ row: r, col: c, key: 'authority', value: authority });
          newAuthority.push(Util.getCellPositionString(r + 1, c + 1));
        }
      }
    });

    sheet.tag({
      ...tags,
      authorityConf: _.uniq(oldAuthority.concat(newAuthority))
    });
  }

  setBorderLines = ({ borderType, lineStyle = 1, borderColor = 'rgb(0, 0, 0)', selections }) => {
    const sheet = this.spread.getActiveSheet();
    const settings = this.getBorderSettings(borderType, lineStyle, borderColor);

    const rowCount = sheet.getRowCount();
    const columnCount = sheet.getColumnCount();

    sheet.suspendPaint();

    _.map(selections, (range) => {
      const sel = this.getActualCellRange(sheet, range, rowCount, columnCount);
      _.map(settings, (setting) => this.setSheetBorder(sel, setting));
    });

    sheet.resumePaint();
  }

  getFontStyle = (row, col) => {
    const font = _.get(this.spread.getActiveSheet().getStyle(row, col), 'font', null);
    if (!font) return;
    return this.parseFont(font);
  }

  setFontStyle = ({ prop, value, selections }) => {
    const dom = this.styleStorage;
    const updateStyleFont = (style) => {
      const cloneDom = dom.cloneNode(true);
      if (style.font) cloneDom.style.font = style.font;

      switch (prop) {
        case 'font-size': cloneDom.style[prop] = `${value}pt`; break;
        case 'font-weight': cloneDom.style[prop] = value || (~cloneDom.style.font.indexOf('700') ? 'normal' : '700'); break;
        case 'font-style': cloneDom.style[prop] = value || (~cloneDom.style.font.indexOf('italic') ? 'normal' : 'italic'); break;
        default: cloneDom.style[prop] = value; break;
      }

      style.font = cloneDom.style.font;
    };

    selections.forEach((range) => {
      const sheet = this.spread.getActiveSheet();
      sheet.suspendPaint();
      const { row, col, rowCount, colCount } = range;
      for (let r = row; r < row + rowCount; r += 1) {
        for (let c = col; c < col + colCount; c += 1) {
          let style = sheet.getStyle(r, c);
          if (!style) style = new spreadNS.Style();
          updateStyleFont(style);
          sheet.setStyle(r, c, style);
        }
      }
      sheet.resumePaint();
    });
  }

  getColor = ({ row, col }, type) => {
    return this.spread.getActiveSheet().getActualStyle(row, col)[type] || undefined;
  }

  setColor = ({ prop, value, selections }) => {
    if (!value) return;
    if (prop === 'backColor' && value === 'rgba(255,255,255,0)') value = undefined;
    const sheet = this.spread.getActiveSheet();
    const rowCount = sheet.getRowCount();
    const columnCount = sheet.getColumnCount();
    sheet.suspendPaint();
    _.map(selections, (range) => {
      const sel = this.getActualCellRange(sheet, range, rowCount, columnCount);
      sheet.getRange(sel.row, sel.col, sel.rowCount, sel.colCount)[prop](value);
    });
    sheet.resumePaint();
  };

  getTextDecoration = (cell) => {
    const { underline, lineThrough } = spreadNS.TextDecorationType;
    const textDecoration = cell.textDecoration();
    if (textDecoration) {
      return {
        underline: (textDecoration & underline) === underline,
        lineThrough: (textDecoration & lineThrough) === lineThrough
      };
    } else {
      return 'none';
    }
  }

  setTextDecoration = ({ underline = false, lineThrough = false, settingType, selections }) => {
    const sheet = this.spread.getActiveSheet();
    const rowCount = sheet.getRowCount();
    const columnCount = sheet.getColumnCount();
    sheet.suspendPaint();
    const { underline: undefinedType, lineThrough: lineThroughType } = spreadNS.TextDecorationType;
    for (let n = 0; n < selections.length; n += 1) {
      const sel = this.getActualCellRange(sheet, selections[n], rowCount, columnCount);
      let textDecoration = sheet.getCell(sel.row, sel.col).textDecoration();
      if (settingType === 'cellSetting') {
        textDecoration = (underline && undefinedType) + (lineThrough && lineThroughType);
      } else if ((textDecoration & undefinedType) === undefinedType) {
        textDecoration -= undefinedType;
      } else {
        textDecoration |= undefinedType;
      }

      sheet.getRange(sel.row, sel.col, sel.rowCount, sel.colCount).textDecoration(textDecoration);
    }
    sheet.resumePaint();
  }

  setAlignment = ({ prop, value, selections }) => {
    const sheet = this.spread.getActiveSheet();
    const align = prop === 'hAlign' ? spreadNS.HorizontalAlign[value] : spreadNS.VerticalAlign[value];
    sheet.suspendPaint();
    selections.forEach((range) => {
      sheet.getRange(range.row, range.col, range.rowCount, range.colCount)[prop](align);
    });
    sheet.resumePaint();
  }

  setAlignCenter = (range) => {
    const selections = [range];
    this.setAlignment({ prop: 'vAlign', value: 'center', selections });
    this.setAlignment({ prop: 'hAlign', value: 'center', selections });
  }

  setAlignNormal = (range) => {
    const selections = [range];
    this.setAlignment({ prop: 'vAlign', value: 'top', selections });
    this.setAlignment({ prop: 'hAlign', value: 'geneal', selections });
  }

  setRowCol = ({ value, selections }) => {
    const sheet = this.spread.getActiveSheet();
    selections.forEach((range) => {
      const { row, rowCount, colCount } = range;
      const count = this.isCol ? colCount : rowCount;
      for (let i = 0; i < count; i += 1) {
        sheet[`set${this.rowColType}`](range[this.isCol ? 'col' : 'row'] + i, value);
      }
      if (!this.isCol) {
        this.setCellTags({ row, key: 'autoFitRow', value: false });
      }
    });
  }

  setAutoFitRowCol = ({ type, selections }) => {
    const sheet = this.spread.getActiveSheet();
    switch (type) {
      case 'autoFitRow': {
        selections.forEach((range) => {
          for (let i = 0; i < range.rowCount; i += 1) {
            this.setCellTags({ row: range.row + i, key: 'autoFitRow', value: true });
            sheet.autoFitRow(range.row + i);
          }
        });
        break;
      }
      case 'autoFitColumn': {
        selections.forEach((range) => {
          for (let i = 0; i < range.colCount; i += 1) {
            sheet.autoFitColumn(range.col + i);
          }
        });
        break;
      }
      default: break;
    }
  }

  setRowColOptions = (type) => {
    const sheet = this.spread.getActiveSheet();
    const selections = sheet.getSelections();
    if (!selections.length) return;
    const { row, col } = selections[0];
    if (['autoFitRow', 'autoFitColumn'].includes(type)) {
      this.basicOperate({ opt: 'setAutoFitRowCol', options: { type } });
    } else {
      this.rowColType = type;
      switch (type) {
        case 'ColumnWidth': {
          this.isCol = true;
          this.setState({
            rowColSettingVisible: true
          });
          this.rowColSettingType = this.props.intl.formatMessage(messages.Width);
          this.rowColValue = sheet.getColumnWidth(col);
          break;
        }
        case 'RowHeight': {
          this.isCol = false;
          this.setState({
            rowColSettingVisible: true
          });
          this.rowColSettingType = this.props.intl.formatMessage(messages.LineHeight);
          this.rowColValue = sheet.getRowHeight(row);
          break;
        }
        default: this.basicOperate({ opt: 'insertDelRowCol', options: { type } }); break;
      }
    }
  }

  setSheetBorder = (sel, setting) => {
    const lineBorder = new spreadNS.LineBorder(setting.borderColor, setting.lineStyle);
    const { options } = setting;
    if (options.up) {
      sel.diagonalUp(lineBorder);
    } else if (options.down) {
      sel.diagonalDown(lineBorder);
    } else {
      sel.setBorder(lineBorder, setting.options);
      // 导致插入填充时样式复制不全
      // this.setRangeBorder(this.spread.getActiveSheet(), sel, setting.options, lineBorder);
    }
  }

  setRangeBorder = (sheet, range, options) => {
    const outline = options.all || options.outline;
    const rowCount = sheet.getRowCount();
    const columnCount = sheet.getColumnCount();
    const startRow = range.row;
    const endRow = (startRow + range.rowCount) - 1;
    const startCol = range.col;
    const endCol = (startCol + range.colCount) - 1;

    if ((startCol > 0) && (outline || options.left)) {
      sheet.getRange(startRow, startCol - 1, range.rowCount, 1).borderRight(undefined);
    }

    if ((startRow > 0) && (outline || options.top)) {
      sheet.getRange(startRow - 1, startCol, 1, range.colCount).borderBottom(undefined);
    }

    if ((endCol < columnCount - 1) && (outline || options.right)) {
      sheet.getRange(startRow, endCol + 1, range.rowCount, 1).borderLeft(undefined);
    }

    if ((endRow < rowCount - 1) && (outline || options.bottom)) {
      sheet.getRange(endRow + 1, startCol, 1, range.colCount).borderTop(undefined);
    }
  }

  getBorderSettings = (borderType, lineStyle, borderColor) => {
    const result = [];
    switch (borderType) {
      case 'outside':
        result.push({ lineStyle, options: { outline: true }, borderColor });
        break;

      case 'inside':
        result.push({ lineStyle, options: { innerHorizontal: true }, borderColor });
        result.push({ lineStyle, options: { innerVertical: true }, borderColor });
        break;

      case 'all':
        result.push({ lineStyle, options: { all: true }, borderColor });
        break;

      case 'none':
        result.push({ lineStyle: 0, options: { all: true }, borderColor });
        result.push({ lineStyle: 0, options: { up: true }, borderColor });
        result.push({ lineStyle: 0, options: { down: true }, borderColor });
        break;

      case 'left':
        result.push({ lineStyle, options: { left: true }, borderColor });
        break;

      case 'innerVertical':
        result.push({ lineStyle, options: { innerVertical: true }, borderColor });
        break;

      case 'right':
        result.push({ lineStyle, options: { right: true }, borderColor });
        break;

      case 'top':
        result.push({ lineStyle, options: { top: true }, borderColor });
        break;

      case 'innerHorizontal':
        result.push({ lineStyle, options: { innerHorizontal: true }, borderColor });
        break;

      case 'bottom':
        result.push({ lineStyle, options: { bottom: true }, borderColor });
        break;

      case 'diagonalUp':
        result.push({ lineStyle, options: { up: true }, borderColor });
        break;

      case 'diagonalDown':
        result.push({ lineStyle, options: { down: true }, borderColor });
        break;

      default: break;
    }

    return result;
  }

  getActualCellRange = (sheet, cellRange, rowCount, columnCount) => {
    if (!cellRange) return;
    if (cellRange.row === -1 && cellRange.col === -1) {
      return new spreadNS.CellRange(sheet, 0, 0, rowCount, columnCount);
    } else if (cellRange.row === -1) {
      return new spreadNS.CellRange(sheet, 0, cellRange.col, rowCount, cellRange.colCount);
    } else if (cellRange.col === -1) {
      return (
        new spreadNS.CellRange(sheet, cellRange.row, 0, cellRange.rowCount, columnCount)
      );
    }
    return (
      new spreadNS.CellRange(sheet,
        cellRange.row, cellRange.col,
        cellRange.rowCount,
        cellRange.colCount)
    );
  }

  isNullCell = (sheet, row, col) => {
    if (row < 0 || col < 0 || row >= sheet.getRowCount() || col >= sheet.getColumnCount()) return true;
    return sheet.getCell(row, col).value() === null && !this.isMergedCell({ row, col });
  }

  getFilterRange = (sheet, range) => {
    const { row, col, rowCount, colCount } = range;
    const isMerged = this.isMergedCell(range);
    if (colCount > 1) {
      if (!isMerged || _.filter(sheet.getSpans(range), (item) => item.row === row).length > 1) {
        return {
          row: row + 1,
          col,
          rowCount: rowCount - 1,
          colCount
        };
      }
    }

    let [rowStart, rowEnd] = [row, row + rowCount];
    if (rowCount === 1 || isMerged) {
      if (this.isNullCell(sheet, row, col)
        && this.isNullCell(sheet, row + 1, col)
        && this.isNullCell(sheet, row - 1, col)) {
        return;
      }
      for (let r = row - 1; r >= 0; r -= 1) {
        if (this.isNullCell(sheet, r, col) || r === 0) {
          rowStart = r;
          break;
        }
      }
      if (rowStart) rowStart += 1;
    }
    for (let r = row + 1; r <= sheet.getRowCount(); r += 1) {
      if (this.isNullCell(sheet, r, col) || r === sheet.getRowCount()) {
        rowEnd = r;
        break;
      }
    }

    return {
      row: rowStart + 1,
      col,
      rowCount: rowEnd - rowStart - 1,
      colCount: 1
    };
  }

  getSelectedRangeString = (sheet, range) => {
    let selectionInfo = '';
    let { rowCount, colCount } = range;
    if (this.isMergedCell(range)) {
      rowCount = 1;
      colCount = 1;
    }

    if (rowCount === 1 && colCount === 1) {
      selectionInfo = Util.getCellPositionString(range.row + 1, range.col + 1);
    } else if (rowCount < 0 && colCount > 0) {
      selectionInfo = `${colCount}C`;
    } else if (colCount < 0 && rowCount > 0) {
      selectionInfo = `${rowCount}R`;
    } else if (rowCount < 0 && colCount < 0) {
      selectionInfo = `${sheet.getRowCount()}R x ${sheet.getColumnCount()}C`;
    } else {
      selectionInfo = `${rowCount}R x ${colCount}C`;
    }
    return selectionInfo;
  }

  setNumberFormat = (numberType, format, selections) => {
    const sheet = this.spread.getActiveSheet();
    const { numberFormat } = format;
    sheet.suspendPaint();
    selections.forEach((range) => {
      const { row, col, rowCount, colCount } = range;
      for (let r = row; r < row + rowCount; r += 1) {
        for (let c = col; c < col + colCount; c += 1) {
          const cell = this.spread.getActiveSheet().getCell(r, c);
          format.numberType = numberType;
          this.setCellTags({ row: r, col: c, key: 'numberFormat', value: format });
          cell.formatter(numberFormat);
        }
      }
    });
    sheet.resumePaint();
  }

  getNumberFormat = ({ row, col }) => {
    const sheet = this.spread.getActiveSheet();
    const formatter = this.getCellTagValue({ row, col, key: 'numberFormat' }) || {};
    const numberFormat = this.getFormatterDetails(sheet.getFormatter(row, col));

    if (formatter.negativeItem) numberFormat.negativeItem = formatter.negativeItem;
    numberFormat.example = sheet.getValue(row, col) || '';
    const isDate = numberFormat.example instanceof Date;
    if (isDate) {
      const { value } = numberFormat.example && sheet.toJSON().data.dataTable[row][col];
      numberFormat.example = value.replace(/[^0-9]/ig, '');
    }
    return numberFormat;
  }

  getFormatterDetails = (formatter) => {
    if (!formatter) return {};
    const { intl } = this.props;
    const numberFormat = { numberFormat: formatter };
    const formatterArr = [
      { type: intl.formatMessage(messages.SelfDefined), formatter: '([a-zA-Z]*)##([a-zA-Z]*)' },
      { type: intl.formatMessage(messages.Text), formatter: '@' },
      { type: intl.formatMessage(messages.Percentage), formatter: '%' },
      { type: intl.formatMessage(messages.Money), formatter: '\\$' },
      { type: intl.formatMessage(messages.Money), formatter: '￥' },
      { type: intl.formatMessage(messages.Money), formatter: '€' },
      { type: intl.formatMessage(messages.Dates), formatter: 'yyyy-MM-dd' },
      { type: intl.formatMessage(messages.Dates), formatter: 'HH:mm:ss' },
      { type: intl.formatMessage(messages.Values), formatter: '0' }
    ];
    numberFormat.numberType = (_.find(formatterArr, (o) => RegExp(o.formatter).test(formatter)) || {}).type;
    if (numberFormat.numberType === intl.formatMessage(messages.Dates)) {
      numberFormat.dateFormat = formatter;
    } else if ([intl.formatMessage(messages.Values), intl.formatMessage(messages.Money), intl.formatMessage(messages.Percentage)].includes(numberFormat.numberType)) {
      if (/(\$|￥|€)/.test(formatter)) {
        [, numberFormat.currency] = formatter.match(/(\$|￥|€)/);
      }
      if (/##,##0/.test(formatter)) {
        numberFormat.useThousandSeparator = true;
      }
      const decimalArr = formatter.split(';')[0].split('.');
      if (decimalArr.length > 1) {
        numberFormat.decimal = decimalArr[1].slice(-1) === '%' ? decimalArr[1].length - 1 : decimalArr[1].length;
      } else {
        numberFormat.decimal = 0;
      }
    }
    return numberFormat;
  }

  getFontStyles = ({ row, col }) => {
    const fontStyles = { ...this.getFontStyle(row, col) };
    fontStyles.fontColor = this.getColor({ row, col }, 'foreColor');
    fontStyles.textDecoration = this.getTextDecoration(this.spread.getActiveSheet().getCell(row, col));
    return fontStyles;
  }

  getBorderSyles = ({ rowCount, colCount }) => {
    // 暂时只返回行数，列数
    return { rowCount, colCount };
  }

  getAlignmentInfo = ({ row, col }) => {
    const alignments = {
      hAlign: ['left', 'center', 'right', 'general'],
      vAlign: ['top', 'center', 'bottom']
    };
    const sheet = this.spread.getActiveSheet();
    const range = new spreadNS.CellRange(sheet, row, col, 1, 1);
    const alignment = {
      hAlign: alignments.hAlign[range.hAlign()],
      vAlign: alignments.vAlign[range.vAlign()]
    };
    alignment.wordWrap = this.isWordWrap(row, col);
    alignment.isVerticalText = sheet.getCell(row, col).isVerticalText();
    return alignment;
  }

  setCellStyleByCondFormat = ({ fillColor, textColor, borderColor }) => {
    const style = new spreadNS.Style();
    const { LineBorder, LineStyle } = spreadNS;
    if (fillColor) {
      style.backColor = fillColor;
    }
    if (textColor) {
      style.foreColor = textColor;
    }
    if (borderColor) {
      style.borderLeft = new LineBorder(borderColor, LineStyle.medium);
      style.borderTop = new LineBorder(borderColor, LineStyle.medium);
      style.borderRight = new LineBorder(borderColor, LineStyle.medium);
      style.borderBottom = new LineBorder(borderColor, LineStyle.medium);
    }
    return style;
  }

  getMergeCellInfo = ({ row, col, rowCount, colCount }) => {
    for (let r = row; r < row + rowCount; r += 1) {
      for (let c = col; c < col + colCount; c += 1) {
        const sheet = this.spread.getActiveSheet();
        const value = sheet.getCell(r, c).value();
        const formula = sheet.getFormula(r, c);
        if (value || formula) {
          return { value, formula };
        }
      }
    }
    return { value: null };
  }

  getBlankCellInRows = ({ row, col }) => {
    const sheet = this.spread.getActiveSheet();
    for (let r = row; r < sheet.getRowCount(); r += 1) {
      if (sheet.getCell(r, col).value() === null) return r;
    }
  }

  getBlankCellInCols = ({ row, col }) => {
    const sheet = this.spread.getActiveSheet();
    for (let c = col; c < sheet.getColumnCount(); c += 1) {
      if (sheet.getCell(row, c).value() === null) return c;
    }
  }

  setCellTags = ({ row, col, key, value }) => {
    if (col === undefined) {
      for (let c = 0; c < this.spread.getActiveSheet().getColumnCount(); c += 1) {
        this.setCellTagsValue({ row, col: c, key, value });
      }
    } else {
      this.setCellTagsValue({ row, col, key, value });
    }
  }

  setCellTagsValue = ({ row, col, key, value }) => {
    const sheet = this.spread.getActiveSheet();
    const tags = sheet.getTag(row, col) || {};
    tags[key] = value;
    sheet.setTag(row, col, tags);
  }

  getCellTagValue = ({ row, col = 0, key }) => {
    const sheet = this.spread.getActiveSheet();
    return sheet.getTag(row, col) ? sheet.getTag(row, col)[key] : null;
  }

  setFrozenStatus = (sheet) => {
    if (!sheet.getSelections().length) return;
    const selectionCell = sheet.getSelections()[0];
    const isfrozenPane = sheet.frozenRowCount() !== 0
      || sheet.frozenColumnCount() !== 0
      || sheet.frozenTrailingRowCount() !== 0
      || sheet.frozenTrailingColumnCount() !== 0;
    const showRowFrozen = sheet.frozenRowCount() !== 1;
    const showColFrozen = sheet.frozenColumnCount() !== 1;
    const showFrozenPane = !(sheet.frozenColumnCount() === selectionCell.col && sheet.frozenRowCount() === selectionCell.row);
    if (isfrozenPane !== this.state.isfrozenPane || showRowFrozen !== this.state.showRowFrozen || showColFrozen !== this.state.showColFrozen || showFrozenPane !== this.state.showFrozenPane) {
      this.setState({ isfrozenPane, showRowFrozen, showColFrozen, showFrozenPane });
    }
  }

  getActiveChart = () => {
    const sheet = this.spread.getActiveSheet();
    let activeChart = null;
    _.map(sheet.charts.all(), (chart) => {
      if (chart.isSelected()) {
        activeChart = chart;
      }
    });
    return activeChart;
  }

  getSelectionsPositionString = (sheet) => {
    const arr = [];
    _.map(sheet.getSelections(), (range) => {
      const { rowCount, colCount } = range;
      let { row, col } = range;
      if (row < 0) row = 0;
      if (col < 0) col = 0;
      for (let r = row; r < row + rowCount; r += 1) {
        for (let c = col; c < col + colCount; c += 1) {
          arr.push(Util.getCellPositionString(r + 1, c + 1));
        }
      }
    });
    return arr;
  }

  getBackfillValue = (config, cellString) => {
    const arr = _.keys(config);
    let value = '';
    for (let i = 0; i < arr.length; i += 1) {
      if (config[arr[i]].includes(cellString)) {
        value = arr[i];
        break;
      }
    }
    return value;
  }

  getActiveCellFormula = () => {
    const sheet = this.spread.getActiveSheet();
    if (!sheet.getSelections().length) return;
    const { row, col } = sheet.getSelections()[0];
    return sheet.getFormula(row, col);
  }

  toJsonForSave = () => {
    const json = this.spread.toJSON();
    json.activeSheetIndex = 0;
    _.map(json.sheets, (v, k) => {
      json.sheets[k].selections = [{ row: 0, rowCount: 1, col: 0, colCount: 1 }];
      delete json.sheets[k].activeCol;
      delete json.sheets[k].activeRow;
    });
    return json;
  }

  registerCommands = () => {
    const { intl } = this.props;
    // 添加右键菜单项
    const contextMenuArr = [{
      text: intl.formatMessage(messages.CellSet),
      name: 'cellSettings',
      command: 'cellSettings',
      func: 'showOrHideModal',
      param: 'cellSettingVisiable'
    }, {
      text: intl.formatMessage(messages.InsertFormula),
      name: 'insertFormula',
      subMenu: [{
        text: intl.formatMessage(messages.DataSource),
        name: 'RT/HIS',
        command: 'insertDataSource',
        func: 'showOrHideFormulaModal'
        // TODO 2.8.0暂时去除实时统计
        // }, {
        //   text: intl.formatMessage(messages.RTStatistics),
        //   name: 'RTS',
        //   command: 'insertRTStatistics',
        //   func: 'showOrHideFormulaModal'
      }, {
        text: intl.formatMessage(messages.BasicFormula),
        name: 'insertDateFormula',
        command: 'insertDateFormula',
        func: 'showOrHideModal',
        param: 'formulaDateModalVisiable'
      }]
    }];

    // 注册命令
    const register = ({ name, text, command, func, param }) => {
      if (!name) return;
      this.spread.commandManager().register(command, {
        canUndo: false,
        execute: this[func].bind(this, param ? { [param]: true } : true, name, text)
      });
    };

    _.map(contextMenuArr, (item) => {
      this.spread.contextMenu.menuData.push({ workArea: 'viewport', ...item });
      if (item.subMenu) {
        _.map(item.subMenu, (subItem) => {
          register(subItem);
        });
      } else {
        register(item);
      }
    });

    const cmdRegisterArr = [
      'onMerge', 'wordWrap', 'setAlignment', 'setFontStyle', 'setColor',
      'setBorderLines', 'setTextDecoration', 'setCellSettings', 'doClear',
      'clearRules', 'changeReportByCondFormat', 'basicFunc', 'sort', 'setRowCol',
      'filter', 'highFilter', 'setAutoFitRowCol', 'setCellValue', 'generateFormula', 'insertDelRowCol'
    ];
    _.map(cmdRegisterArr, (cmd) => {
      this.spread.commandManager().register(cmd, {
        canUndo: true,
        execute: (context, options, isUndo) => {
          const commands = spreadNS.Commands;
          if (isUndo) {
            commands.undoTransaction(context, options);
            return true;
          } else {
            commands.startTransaction(context, options);
            const params = _.merge({}, options);
            delete params.cmd;
            delete params.sheetName;
            this[cmd](params);
            commands.endTransaction(context, options);
            return true;
          }
        }
      });
    });
  }

  insertDelRowCol = ({ type, selections }) => {
    const sheet = this.spread.getActiveSheet();
    const { row, col, rowCount, colCount } = selections[0];
    switch (type) {
      case 'insertRow': sheet.addRows(row, rowCount); break;
      case 'insertColumn': sheet.addColumns(col, colCount); break;
      case 'deleteRows': sheet.deleteRows(row, rowCount); break;
      case 'deleteColumns': sheet.deleteColumns(col, colCount); break;
      default: break;
    }
  }

  bindSpreadEvent = () => {
    this.spread.bind(spreadNS.Events.ActiveSheetChanged, (sender, args) => {
      this.setFrozenStatus(args.newSheet);
      this.bindSheetEvent(args.newSheet);
      this.initStatus({ sheet: args.newSheet });
      this.sheets = this.reporter.getSheetsInfo(this.spread);
      this.hasChanged = true;
    });
    this.spread.bind(spreadNS.Events.SheetNameChanged, () => {
      this.hasChanged = true;
    });
  }

  bindSheetEvent = (sheet) => {
    if (this.bindSheets.includes(sheet.name())) return;
    this.bindSheets.push(sheet.name());

    sheet.options.allowCellOverflow = true;
    sheet.options.frozenlineColor = 'green';

    sheet.bind(spreadNS.Events.SelectionChanged, (e, info) => {
      if (!info.newSelections[0]) return;
      const { row, col, rowCount, colCount } = info.newSelections[0];
      if (!~row && !~col) { // 全选
        info.sheet.setSelection(0, 0, rowCount, colCount);
      }
      this.updatePositionBox();
    });
    sheet.bind(spreadNS.Events.CellClick, () => {
      this.executeFormat(); // 格式刷
      this.setState({ textVal: Util.getCellValue(this.spread) });
    });
    sheet.bind(spreadNS.Events.CellChanged, () => {
      const { row, col } = this.getCellRange(sheet);
      this.setMenubarStatus({ row, col });
    });
    // 浮动对象
    sheet.bind(spreadNS.Events.FloatingObjectChanged, (e, args) => {
      const { floatingObject, sheet: activeSheet } = args;
      if (floatingObject && floatingObject instanceof spreadNS.Charts.Chart) {
        this.showChartPanel(floatingObject);
        setTimeout(() => {
          window.Yl.activeElement = activeSheet;
        }, 10);
      }
    });
    sheet.bind(spreadNS.Events.PictureChanged, (e, args) => {
      const { picture, sheet: activeSheet } = args;
      if (picture && picture.isSelected()) {
        setTimeout(() => {
          window.Yl.activeElement = activeSheet;
        }, 100);
      }
    });
    sheet.bind(spreadNS.Events.EnterCell, (e, info) => {
      this.executeFormat(); // 格式刷
      this.initStatus({ sheet, info });
      // 操作配置更新文本内容
      this.setState({ textVal: Util.getCellValue(this.spread) });
    });
    sheet.bind(spreadNS.Events.RangeChanged, () => {
      // const { row, rowCount, sheet: changedSheet, action } = args;
      // if (action === spreadNS.RangeChangedAction.paste) {
      //   for (let r = row; r < row + rowCount; r += 1) {
      //     changedSheet.autoFitRow(r);
      //   }
      // }
      setTimeout(() => {
        if (this.menuBar) this.menuBar.updateTransaction();
        // this.setState({ updateMenuBar: true });
      }, 0);
    });
    sheet.bind(spreadNS.Events.EditEnded, () => {
      const cellRange = this.getCellRange(sheet);
      if (cellRange) {
        // 操作配置更新文本内容
        this.setState({ textVal: Util.getCellValue(this.spread) });

        // 文字自动换行
        const { row, col } = cellRange;
        const verticalTextNoWrap = sheet.getCell(row, col).isVerticalText() && !this.isWordWrap(row, col);
        const hasAutoFitRow = this.getCellTagValue({ row, col, key: 'autoFitRow' });
        const wordWrapAutoFit = this.isWordWrap(row, col) && !sheet.getCell(row, col).isVerticalText();
        if (hasAutoFitRow !== false && (wordWrapAutoFit || verticalTextNoWrap)) {
          sheet.autoFitRow(row);
        }
      }
    });

    // copy && paste
    const oldFun = spreadNS.Commands.clipboardPaste.execute;
    spreadNS.Commands.clipboardPaste.execute = (context, options, isUndo) => {
      if (Object.keys(options).length) {
        this.isCutting = options.isCutting;
        return oldFun.call(this, context, options, isUndo);
      }
    };
    sheet.bind(spreadNS.Events.ClipboardChanged, () => {
      this.setState({ canPaste: true });
    });
    sheet.bind(spreadNS.Events.ClipboardPasted, () => {
      this.initStatus({ sheet });
      if (this.isCutting) {
        this.setState({ canPaste: false });
      }
    });
    // 拖动单元格， 重置源和目标单元格上的窗格
    sheet.bind(spreadNS.Events.DragDropBlock, (e, info) => {
      const { fromRow, fromCol, toRow, toCol } = info;
      const formula = sheet.getFormula(fromRow, fromCol);
      this.generateDefaultPane(toRow, toCol, formula, sheet);
      const selections = [Util.getCellPositionString(fromRow + 1, fromCol + 1)];
      this.settingConfig({ tagName: 'paneSettingUpConf', selections, action: 'delete' });
      this.settingConfig({ tagName: 'paneSettingConf', selections, action: 'delete' });
    });

    _.map(['RowHeightChanged', 'ColumnWidthChanged'], (e) => {
      sheet.bind(spreadNS.Events[e], () => {
        this.hasChanged = true;
        this.menuBar.updateTransaction();
      });
    });

    sheet.bind(spreadNS.Events.RowChanged, (e, info) => {
      const { propertyName, row, count, oldValue } = info;
      let isUndo = false;
      // 撤销触发时的判断
      if (propertyName === 'addRows') {
        isUndo = sheet.getRowCount() !== oldValue + count;
      } else if (propertyName === 'deleteRows') {
        isUndo = sheet.getRowCount() !== oldValue - count;
      }
      if (propertyName === 'deleteRows' || propertyName === 'addRows') {
        this.resetPaneConf({ sheet, tagName: 'paneSettingUpConf', type: propertyName, row, count, isUndo });
        this.resetPaneConf({ sheet, tagName: 'paneSettingConf', type: propertyName, row, count, isUndo });
      }
    });
    sheet.bind(spreadNS.Events.ColumnChanged, (e, info) => {
      const { propertyName, col, count, oldValue } = info;
      let isUndo = false;
      // 撤销触发时的判断
      if (propertyName === 'addColumns') {
        isUndo = info.sheet.getColumnCount() !== oldValue + count;
      } else if (propertyName === 'deleteColumns') {
        isUndo = info.sheet.getColumnCount() !== oldValue - count;
      }
      if (propertyName === 'deleteColumns' || propertyName === 'addColumns') {
        this.resetPaneConf({ sheet, tagName: 'paneSettingUpConf', type: propertyName, col, count, isUndo, isRow: false });
        this.resetPaneConf({ sheet, tagName: 'paneSettingConf', type: propertyName, col, count, isUndo, isRow: false });
      }
    });
  }

  reportHasChanged = () => {
    this.hasChanged = true;
  }

  showChartPanel = (chart, isAdd = false) => {
    if ((chart && chart.isSelected()) || isAdd) {
      const { unfoldDataSource, chartConfigArea } = this.state;
      this.editChart = chart;
      if (!chartConfigArea) {
        this.unfoldDataSource = unfoldDataSource;
        this.setState({
          unfoldDataSource: true,
          chartConfigArea: true
        });
      }
    } else {
      this.editChart = null;
      this.setState({
        unfoldDataSource: this.unfoldDataSource,
        chartConfigArea: false
      });
    }
  }

  backfillConfig = (sheet = this.spread.getActiveSheet(), info) => {
    if (sheet && !info) [info] = sheet.getSelections();
    if (!this.state.unfoldDataSource && info) {
      const cellString = Util.getCellPositionString(info.row + 1, info.col + 1);
      const paneConf = _.get(sheet.tag(), 'paneSettingConf') || {};
      const paneUpConf = _.get(sheet.tag(), 'paneSettingUpConf') || {};
      const formulaConf = _.get(sheet.tag(), 'formulaConf') || {};
      if (this.dataSourceSetDrawer) {
        this.dataSourceSetDrawer.backfill({
          paneValue: this.getBackfillValue(paneConf, cellString),
          paneUpValue: this.getBackfillValue(paneUpConf, cellString),
          formulaValue: this.getBackfillValue(formulaConf, cellString)
        });
      }
    }
  }

  isMergedCell = ({ row, col }) => {
    return !!this.spread.getActiveSheet().getSpans({ row, col, rowCount: 1, colCount: 1 }).length;
  }

  executeFormat = () => {
    if (this.isFormatPainting) {
      this.basicOperate({ opt: 'paste', type: 'formatting' });
      if (!this.doubleClickBrush) {
        this.resetFormatPainting();
      }
    }
  }

  format = ({ opt }) => {
    if (this.state.canBrush) {
      this.doubleClickBrush = false;
      this.resetFormatPainting();
    } else {
      this.setState({
        canBrush: true
      });
      this.doubleClickBrush = opt === 'double';
      this.isFormatPainting = true;
      this.basicOperate({ opt: 'copy' });
    }
  }

  resetFormatPainting = () => {
    this.setState({
      canBrush: false
    });
    this.isFormatPainting = false;
    this.basicOperate({ opt: 'cancelInput' });
  }

  processPictureAdded = (file) => {
    const reader = new FileReader();
    reader.onload = () => {
      this.addPicture(reader.result);
    };
    reader.readAsDataURL(file);
  }

  importSpreadFromExcel = (file, options) => {
    const excelIO = new Excel.IO();
    excelIO.open(file, (json) => {
      this.spread.suspendPaint();
      this.spread.fromJSON(json);
      this.init();
      this.spread.resumePaint();
    }, () => {
    }, options);
  }

  importSpreadFromJSON = (file) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      this.spread.suspendPaint();
      const json = JSON.parse(e.target.result);
      this.spread.fromJSON(json);
      this.init();
      this.spread.resumePaint();
    };
    reader.readAsText(file);
  }

  importSpreadFromCsv = (file) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      this.spread.suspendPaint();
      this.spread.fromJSON(this.reporter.json);
      this.spread.getActiveSheet().setCsv(0, 0, e.target.result, '', ',');
      this.init();
      this.spread.resumePaint();
    };
    reader.readAsText(file);
  }

  mergeCells = (range, sheet) => {
    const { row, col, rowCount, colCount } = range;
    const { value, formula } = this.getMergeCellInfo(range);

    sheet.addSpan(row, col, rowCount, colCount);
    sheet.clear(row, col, rowCount, colCount, spreadNS.SheetArea.viewport, spreadNS.StorageType.data);
    if (formula) {
      sheet.setFormula(row, col, formula);
      this.updateCellFormula({ row, col, formula });
    } else if (value) {
      sheet.getCell(row, col).value(value);
    }
    this.setAlignCenter(range);
  }

  unmergeCells = (range, sheet) => {
    const { row, col, rowCount, colCount } = range;
    const value = sheet.getCell(row, col).value();
    const formula = sheet.getFormula(row, col);
    for (let r = row; r < row + rowCount; r += 1) {
      for (let c = col; c < col + colCount; c += 1) {
        if (this.isMergedCell({ row: r, col: c })) {
          sheet.removeSpan(r, c);
        }
      }
    }
    if (formula) {
      sheet.setFormula(row, col, formula);
    } else if (value) {
      sheet.getCell(row, col).value(value);
    }
    this.setAlignNormal(range);
  }

  doClear = ({ type, selections }) => {
    const sheet = this.spread.getActiveSheet();
    selections.forEach((range) => {
      const { row, col, rowCount, colCount } = range;
      switch (type) {
        case 'clearAll': {
          sheet.clear(row, col, rowCount, colCount, spreadNS.SheetArea.viewport, 255);
          this.clearSpansInSelection(sheet, range);
          break;
        }
        case 'clearFormat': {
          sheet.clear(row, col, rowCount, colCount, spreadNS.SheetArea.viewport, spreadNS.StorageType.style);
          sheet.clear(row, col, rowCount, colCount, spreadNS.SheetArea.viewport, spreadNS.StorageType.tag);
          this.clearSpansInSelection(sheet, range);
          break;
        }
        default: {
          sheet.clear(row, col, rowCount, colCount, spreadNS.SheetArea.viewport, spreadNS.StorageType.data);
          break;
        }
      }
    });
  }

  clearSpansInSelection = (sheet, selection) => {
    if (sheet && selection) {
      const ranges = [];
      sheet.getSpans().forEach((range) => {
        if (range.intersect(selection.row, selection.col, selection.rowCount, selection.colCount)) {
          ranges.push(range);
        }
      });
      ranges.forEach((range) => {
        sheet.removeSpan(range.row, range.col);
      });
    }
  }

  basicOperate = ({ opt, type, options = {} }) => {
    this.hasChanged = true;
    const sheet = this.spread.getActiveSheet();
    if (type) {
      sheet.options.clipBoardOptions = spreadNS.ClipboardPasteOptions[type];
    }

    if (!options.selections) options.selections = sheet.getSelections();
    this.spread.options.allowUndo = true;
    this.spread.commandManager().execute({ cmd: opt, sheetName: sheet.name(), ...options });
  }

  frozenPaneCheck = () => {
    const sheet = this.spread.getActiveSheet();
    this.setFrozenStatus(sheet);
  }

  frozenPane = (type) => {
    const sheet = this.spread.getActiveSheet();
    if (!sheet.getSelections().length) return;
    const range = _.merge({}, sheet.getSelections()[0]);
    const { row, col } = range;
    this.setState({
      isfrozenPane: type !== 'cancelFrozenPane'
    });
    this.spread.suspendPaint();
    switch (type) {
      case 'curRowCol': {
        if (row === undefined || col === undefined) return;
        sheet.frozenRowCount(row);
        sheet.frozenColumnCount(col);
        break;
      }
      case 'curRowColTrailing': {
        if (row === undefined || col === undefined) return;
        const rowCount = sheet.getRowCount();
        const colCount = sheet.getColumnCount();
        const spans = sheet.getSpans(range);
        let [rowDiff, colDiff] = [0, 0];
        if (spans.length) {
          [rowDiff, colDiff] = [spans[0].rowCount - 1, spans[0].colCount - 1];
        }
        sheet.frozenTrailingRowCount(rowCount - row - rowDiff - 1);
        sheet.frozenTrailingColumnCount(colCount - col - colDiff - 1);
        break;
      }
      case 'firstRow': sheet.frozenRowCount(1); break;
      case 'firstColumn': sheet.frozenColumnCount(1); break;
      case 'cancelFrozenPane': {
        sheet.frozenRowCount(0);
        sheet.frozenColumnCount(0);
        sheet.frozenTrailingRowCount(0);
        sheet.frozenTrailingColumnCount(0);
        break;
      }
      default: break;
    }
    this.spread.resumePaint();
  }

  sort = ({ type, selections }) => {
    const ascending = type === 'asc';
    const { intl } = this.props;
    if (selections && selections.length > 1) {
      this.warning({ content: intl.formatMessage(messages.ChooseRule) });
    } else {
      const { row, col, rowCount, colCount } = selections[0];
      const sheet = this.spread.getActiveSheet();
      const spans = sheet.getSpans(selections[0]);
      if (spans && spans.length) {
        this.warning({ content: intl.formatMessage(messages.MergeRule) });
      } else {
        sheet.sortRange(row, col, rowCount, colCount, true,
          [
            { index: col, ascending }
          ]);
      }
    }
  }

  warning = ({ title = this.props.intl.formatMessage(messages.SetFail), content }) => {
    Modal.warning({ title, content, width: 520 });
  }

  filter = ({ type, selections }) => {
    if (type === 'filter') {
      this.showFilter({ selections });
    } else {
      // 高级过滤
      if (!selections || selections.length <= 0) return;
      this.setState({
        highFilterVisible: true
      });
    }
  }

  highFilter = ({ value, selections }) => {
    if (!selections) return;
    const sheet = this.spread.getActiveSheet();
    const sel = selections.shift();
    sheet.suspendPaint();
    const { row, col, rowCount, colCount } = sel;
    for (let r = row; r < row + rowCount; r += 1) {
      for (let c = col; c < col + colCount; c += 1) {
        this.setCellTags({ row: r, col: c, key: 'highFilter', value });
        if (value && value.list && value.list.length > 0) {
          sheet.setCellType(r, c, new TagTriangleCell());
        } else {
          sheet.setCellType(r, c, null);
        }
      }
    }
    sheet.resumePaint();
  }

  showFilter = ({ selections }) => {
    if (!selections) return;
    const { intl } = this.props;
    const sheet = this.spread.getActiveSheet();
    const isFilted = sheet.rowFilter() !== null;
    const sel = selections.shift();
    let range = this.getFilterRange(sheet, sel);
    if (!range) {
      if (isFilted) {
        range = sel;
      } else {
        this.warning({ content: intl.formatMessage(messages.ApplyError) });
        return;
      }
    }

    const rowFilter = new spreadNS.Filter.HideRowFilter(range);
    sheet.rowFilter(rowFilter);
    if (isFilted) {
      rowFilter.filterButtonVisible(false);
      sheet.rowFilter(null);
    } else {
      rowFilter.filterButtonVisible(true);
    }
  }

  isSingleCell = ({ row, col, rowCount, colCount }) => {
    if (rowCount === 1 && colCount === 1) {
      return true;
    } else {
      const sheet = this.spread.getActiveSheet();
      const spans = sheet.getSpans({ row, col, rowCount: 1, colCount: 1 });
      const range = spans.length && spans[0];
      if (range && range.rowCount === rowCount && range.colCount === colCount) {
        return true;
      }
    }
    return false;
  }

  basicFunc = ({ func, selections }) => {
    const sheet = this.spread.getActiveSheet();
    selections.forEach((range) => {
      const { row, col, rowCount, colCount } = range;
      if (this.isSingleCell(range)) {
        // 选中单个cell计算区域
        const formula = `=${func}(`;
        this.fbx.text(formula);
        sheet.getCell(row, col).text(formula);
        sheet.startEdit(true);
      } else if (rowCount > 1) {
        // 计算列
        for (let c = col; c < col + colCount; c += 1) {
          if (this.isBlankRange({ row, col: c, rowCount })) continue;
          const column = Util.getColString(c + 1);
          let resultRow = (row + rowCount) - 1;
          let endRow = resultRow;
          if (sheet.getCell(resultRow, c).value() !== null) {
            resultRow = this.getBlankCellInRows({ row: row + rowCount, col: c });
            endRow += 1;
          }
          if (this.isMergedCell({ row: resultRow, col: c })) return;
          sheet.setFormula(resultRow, c, `=${func}(${sheet.name()}!${column}${row + 1}:${column}${endRow})`);
        }
      }
      if (colCount > 1) {
        // 计算行
        for (let r = row; r < row + rowCount; r += 1) {
          if (this.isBlankRange({ row: r, col, colCount })) continue;
          const startColumn = Util.getColString(col + 1);
          let resultCol = (col + colCount) - 1;
          let endCol = resultCol - 1;
          if (sheet.getCell(r, resultCol).value() !== null) {
            resultCol = this.getBlankCellInCols({ row: r, col: col + colCount });
            endCol += 1;
          }
          const endColumn = Util.getColString(endCol + 1);
          if (this.isMergedCell({ row: r, col: resultCol })) return;
          sheet.setFormula(r, resultCol, `=${func}(${sheet.name()}!${startColumn}${r + 1}:${endColumn}${r + 1})`);
        }
      }
    });
  }

  isBlankRange = ({ row, col, rowCount = 1, colCount = 1 }) => {
    const sheet = this.spread.getActiveSheet();
    for (let r = row; r < row + rowCount; r += 1) {
      for (let c = col; c < col + colCount; c += 1) {
        if (sheet.getCell(r, c).value()) return false;
      }
    }
    return true;
  }

  showCondFormatModal = () => {
    const { condFormatVisible = false } = this.state;
    if (condFormatVisible) {
      return (
        <CondFormatModal
          intl={this.props.intl}
          spread={this.spread}
          condFormat={this.condFormat}
          setTags={this.setCellTagsValue}
          getTags={this.getCellTagValue}
          showOrHideModal={this.showOrHideModal}
          basicOperate={this.basicOperate}
        />
      );
    } else {
      return null;
    }
  }

  showOrHideModal = (state) => {
    this.setState(state);
  }

  showRowColSettingModal = () => {
    const { rowColSettingVisible = false } = this.state;
    if (rowColSettingVisible) {
      return (
        <RowColSettingModal
          type={this.rowColSettingType}
          value={this.rowColValue}
          basicOperate={this.basicOperate}
          showOrHideModal={this.showOrHideModal}
          intl={this.props.intl}
        />
      );
    } else {
      return null;
    }
  }

  showDateFormulaModal = () => {
    if (this.state.formulaDateModalVisiable) {
      return (
        <InsertDateFormulaModal
          formula={this.getActiveCellFormula()}
          showOrHideModal={this.showOrHideModal}
          basicOperate={this.basicOperate}
          intl={this.props.intl}
        />
      );
    } else {
      return null;
    }
  }

  showOrHideFormulaModal = (boolean, name, text) => {
    this.formulaType = name;
    this.formulaTitle = text;
    this.setState({
      formulaModalVisiable: boolean
    });
  }

  showFormulaModal = () => {
    if (this.state.formulaModalVisiable) {
      const { allDataSource } = _.get(this.reporter, 'dataSource', {});
      return (
        <InsertFormulaModal
          appId={this.props.appId}
          formula={this.getActiveCellFormula()}
          allDataSource={allDataSource}
          showOrHideModal={this.showOrHideFormulaModal}
          basicOperate={this.basicOperate}
          intl={this.props.intl}
          formulaType={this.formulaType}
          formulaTitle={this.formulaTitle}
        />
      );
    } else {
      return null;
    }
  }

  showOrHideAnalyzeModal = (boolean) => {
    if (!boolean) {
      this.spread.options.allowContextMenu = true;
      this.spread.options.newTabVisible = true;
    }
    this.setState({
      analyzeModalVisiable: boolean
    });
  }

  showAnalyzeModal = () => {
    if (this.state.analyzeModalVisiable) {
      this.spread.options.allowContextMenu = false;
      this.spread.options.newTabVisible = false;
      this.sheets = this.reporter.getSheetsInfo(this.spread);
      return (
        <AnalyzeModal
          spread={this.spread}
          sheets={this.sheets}
          report={this.reporter}
          showOrHideModal={this.showOrHideAnalyzeModal}
          basicOperate={this.basicOperate}
          intl={this.props.intl}
        />
      );
    } else {
      return null;
    }
  }

  showCellSettingModal = () => {
    const { cellSettingVisiable = false } = this.state;
    if (cellSettingVisiable) {
      const cellRange = this.getCellRange();
      const info = {};
      if (cellRange) {
        const { row, col } = cellRange;
        info.numberFormat = this.getNumberFormat(cellRange);
        info.fontStyles = this.getFontStyles(cellRange);
        info.borderStyles = this.getBorderSyles(cellRange);
        info.alignment = this.getAlignmentInfo(cellRange);
        info.bgColor = this.getColor(cellRange, 'backColor') || 'rgba(255,255,255,0)';
        info.authority = this.getCellTagValue({ row, col, key: 'authority' });
      }
      return (
        <CellSettingModal
          showOrHideModal={this.showOrHideModal}
          basicOperate={this.basicOperate}
          numberFormat={info.numberFormat || {}}
          fontStyles={info.fontStyles}
          borderStyles={info.borderStyles}
          alignment={info.alignment}
          bgColor={info.bgColor}
          authority={info.authority}
          intl={this.props.intl}
        />
      );
    } else {
      return null;
    }
  }

  showBarCodeModal = () => {
    const { barCodeVisiable, qrBarCodeVisiable, barCodeObject, qrBarCodeObject } = this.state;
    if (barCodeVisiable) {
      return (
        <BarCodeModal
          intl={this.props.intl}
          showOrHideModal={this.showOrHideModal}
          barCodeObject={barCodeObject}
          createBarCode={this.createBarCode}
          basicOperate={this.basicOperate}
        />
      );
    } else if (qrBarCodeVisiable) {
      return (
        <QrBarCodeModal
          intl={this.props.intl}
          showOrHideModal={this.showOrHideModal}
          barCodeObject={qrBarCodeObject}
          createBarCode={this.createBarCode}
          basicOperate={this.basicOperate}
        />
      );
    }
    return null;
  }

  showHighFilterModal = () => {
    const { highFilterVisible = false } = this.state;
    const { allDataSource, sqlInfo } = _.get(this.reporter, 'dataSource', {});
    if (highFilterVisible) {
      const sheet = this.spread.getActiveSheet();
      const { row, col } = sheet.getSelections()[0];
      const currentFilterRule = this.getCellTagValue({ row, col, key: 'highFilter' });
      const curFormula = sheet.getFormula(row, col);
      return (
        <HighFilterModal
          data={_.cloneDeep(currentFilterRule)}
          dataSource={allDataSource}
          sqlInfo={sqlInfo}
          curFormula={curFormula}
          basicOperate={this.basicOperate}
          showOrHideModal={this.showOrHideModal}
          intl={this.props.intl}
        />
      );
    } else {
      return null;
    }
  }

  wordWrap = ({ isWrap, selections }) => {
    const sheet = this.spread.getActiveSheet();
    sheet.suspendPaint();
    selections.forEach((range) => {
      const { row, col, rowCount, colCount } = range;
      if (isWrap !== undefined) {
        sheet.getRange(row, col, rowCount, colCount).wordWrap(isWrap);
      } else {
        let canWrap = false;
        for (let r = row; r < row + rowCount; r += 1) {
          for (let c = col; c < col + colCount; c += 1) {
            if (!this.isWordWrap(r, c)) {
              canWrap = true;
              break;
            }
          }
        }
        sheet.getRange(row, col, rowCount, colCount).wordWrap(canWrap);
        for (let r = row; r < row + rowCount; r += 1) {
          const hasAutoFitRow = this.getCellTagValue({ row, key: 'autoFitRow' });
          if (hasAutoFitRow !== false) {
            sheet.autoFitRow(r);
          }
        }
      }
    });
    sheet.resumePaint();
  }

  isWordWrap = (row, col) => {
    return !!this.spread.getActiveSheet().getCell(row, col).wordWrap();
  }

  changeWritingMode = (isVerticalText) => {
    if (isVerticalText === undefined) return;
    const sheet = this.spread.getActiveSheet();
    sheet.suspendPaint();
    sheet.getSelections().forEach((range) => {
      const { row, col, rowCount, colCount } = range;
      for (let r = row; r < row + rowCount; r += 1) {
        for (let c = col; c < col + colCount; c += 1) {
          sheet.getCell(r, c).isVerticalText(isVerticalText);
        }
        sheet.autoFitRow(r);
      }
    });
    sheet.resumePaint();
  }

  updatePositionBox = () => {
    const sheet = this.spread.getActiveSheet();
    const selection = sheet.getSelections().slice(-1)[0];
    if (selection) {
      this.setState({
        curCellName: this.getSelectedRangeString(sheet, selection)
      });
    }
  }

  updateCellFormula = ({ row, col, formula }) => {
    if (formula || (row !== undefined && col !== undefined)) {
      const newFormula = formula || this.spread.getActiveSheet().getFormula(row, col);
      if (this.state.formula !== newFormula) {
        this.setState({ formula: newFormula });
        this.fbx.refresh();
        this.spread.focus(false);
      }
    }
  }

  formatCells = ([key, parentKey]) => {
    if (parentKey === 'clearRules') {
      this.basicOperate({ opt: 'clearRules', options: { key } });
    } else {
      this.condFormat = _.merge({}, this.condFormat, { parentType: parentKey, type: key });
      this.setState({ condFormatVisible: true });
    }
  }

  clearRules = ({ key, selections }) => {
    const sheet = this.spread.getActiveSheet();
    if (key === 'clearAll') {
      sheet.conditionalFormats.clearRule();
      this.clearRuleTag({ sheet });
    } else if (key === 'removeRuleByRange') {
      _.map(selections, (range) => {
        const { row, col, rowCount, colCount } = range;
        sheet.conditionalFormats.removeRuleByRange(row, col, rowCount, colCount);
        this.clearRuleTag({ range, sheet });
      });
    }
  }

  clearRuleTag = ({ range = {}, sheet }) => {
    const { row = 0, col = 0, rowCount = sheet.getRowCount(), colCount = sheet.getColumnCount() } = range;
    for (let r = row; r < row + rowCount; r += 1) {
      for (let c = col; c < col + colCount; c += 1) {
        const tag = sheet.getTag(r, c);
        if (tag && tag.conditionalFormats) {
          delete tag.conditionalFormats;
          sheet.setTag(r, c, tag);
        }
      }
    }
  }

  changeToComboBox = (cell, item) => {
    const combo = new spreadNS.CellTypes.ComboBox();
    combo.items(item);
    cell.cellType(combo);
  }

  changeCellType = (type, item) => {
    const sheet = this.spread.getActiveSheet();
    sheet.suspendPaint();
    sheet.getSelections().forEach((range) => {
      const { row, col, rowCount, colCount } = range;
      for (let c = col; c < colCount + col; c += 1) {
        for (let r = row; r < rowCount + row; r += 1) {
          const cell = sheet.getCell(r, c);
          switch (type) {
            case 'list': this.changeToComboBox(cell, item); break;
            case 'text': cell.cellType(new spreadNS.CellTypes.Text()); break;
            default: break;
          }
        }
      }
    });
    sheet.resumePaint();
  }

  condFormatByCellsRule = (selections, type, rangeValue, newState) => {
    const { interValue, minValue, maxValue, optionValue } = rangeValue;
    const style = this.setCellStyleByCondFormat(newState);
    const { conditionalFormats } = this.spread.getActiveSheet();
    const {
      ComparisonOperators: operators,
      TextComparisonOperators: textOperators,
      DateOccurringType
    } = spreadNS.ConditionalFormatting;
    switch (type) {
      case 'greaterThan': {
        conditionalFormats.addCellValueRule(operators.greaterThan, interValue, interValue, style, selections);
        break;
      }
      case 'smaller': {
        conditionalFormats.addCellValueRule(operators.lessThan, interValue, interValue, style, selections);
        break;
      }
      case 'between': {
        conditionalFormats.addCellValueRule(operators.between, minValue, maxValue, style, selections);
        break;
      }
      case 'equal': {
        conditionalFormats.addCellValueRule(operators.equalsTo, interValue, interValue, style, selections);
        break;
      }
      case 'contains': {
        conditionalFormats.addSpecificTextRule(textOperators.contains, interValue, style, selections);
        break;
      }
      case 'date': {
        conditionalFormats.addDateOccurringRule(DateOccurringType[optionValue], style, selections);
        break;
      }
      case 'duplicate': {
        conditionalFormats[optionValue === 'duplicate' ? 'addDuplicateRule' : 'addUniqueRule'](style, selections);
        break;
      }
      default:
        break;
    }
  }

  condFormatByProRule = (selections, type, rangeValue, newState) => {
    const sheet = this.spread.getActiveSheet();
    const { conditionalFormats } = sheet;
    const style = this.setCellStyleByCondFormat(newState);
    const { Top10ConditionType: topType, AverageConditionType: avgType } = spreadNS.ConditionalFormatting;
    const countNumber = () => {
      let totalNumber = 0;
      _.map(selections, (range) => {
        const { row, col, rowCount, colCount } = range;
        for (let c = col; c < colCount + col; c += 1) {
          for (let r = row; r < rowCount + row; r += 1) {
            if (sheet.getValue(r, c) !== null) totalNumber += 1;
          }
        }
      });

      let numberVal = Math.floor(rangeValue.numberValue * totalNumber * 0.01);
      numberVal = numberVal || 1;
      return numberVal;
    };
    switch (type) {
      case 'maxTen': {
        conditionalFormats.addTop10Rule(topType.top, rangeValue.numberValue, style, selections);
        break;
      }
      case 'maxTenPct': {
        conditionalFormats.addTop10Rule(topType.top, countNumber(), style, selections);
        break;
      }
      case 'minTen': {
        conditionalFormats.addTop10Rule(topType.bottom, rangeValue.numberValue, style, selections);
        break;
      }
      case 'minTenPct': {
        conditionalFormats.addTop10Rule(topType.bottom, countNumber(), style, selections);
        break;
      }
      case 'aboveAverage': {
        conditionalFormats.addAverageRule(avgType.above, style, selections);
        break;
      }
      case 'belowAverage': {
        conditionalFormats.addAverageRule(avgType.below, style, selections);
        break;
      }
      default:
        break;
    }
  }

  changeReportByCondFormat = ({ parentType, type, rangeValue, newState, selections }) => {
    if (parentType === 'formatCells') {
      this.condFormatByCellsRule(selections, type, rangeValue, newState);
    } else {
      this.condFormatByProRule(selections, type, rangeValue, newState);
    }
  }

  parseFont = (font) => {
    let [fontSize, fontStyle, fontWeight] = [null, 'normal', 'normal'];

    const fontStyleArr = font.split(/"/);
    const elements = fontStyleArr[0].split(/\s+/);
    const fontFamily = fontStyleArr.length > 1 ? fontStyleArr[1] : elements.slice(-1)[0];
    const px2pt = (pxValue) => Math.round(pxValue * 0.75);

    let element;
    while ((element = elements.shift())) {
      switch (element) {
        case 'normal': break;
        case 'italic': fontStyle = element; break;
        case '700': fontWeight = element; break;
        default:
          if (!fontSize) {
            fontSize = element.split('/').shift();
            if (fontSize && fontSize.match(/px/)) {
              fontSize = `${px2pt(parseFloat(fontSize))}`;
            } else if (fontSize.match(/pt/)) {
              fontSize = `${fontSize.split('pt').shift()}`;
            }
            break;
          }
      }
    }

    return {
      fontStyle,
      fontWeight,
      fontSize,
      fontFamily
    };
  }

  generateDefaultPane = (row, col, formula, sheet) => {
    if (!Util.isDTSerFormula(formula)) return;
    const { dataSource } = Util.getDataSource(formula);
    const selections = [Util.getCellPositionString(row + 1, col + 1)];
    if (row && dataSource) {
      const upFormula = sheet.getFormula(row - 1, col);
      if (upFormula && Util.isDTSerFormula(upFormula) && dataSource === Util.getDataSource(upFormula).dataSource) {
        this.settingConfig({ tagName: 'paneSettingUpConf', row, col: Util.getColString(col + 1), selections });
      }
    }
    if (col && dataSource) {
      const leftFormula = sheet.getFormula(row, col - 1);
      if (leftFormula && Util.isDTSerFormula(leftFormula) && dataSource === Util.getDataSource(leftFormula).dataSource) {
        this.settingConfig({ tagName: 'paneSettingConf', row: row + 1, col: Util.getColString(col), selections });
      }
    }
  }

  generateFormula = ({ selectedObject, formula, selections, updatePane }) => {
    const { allDataSource } = this.reporter.dataSource;
    if (this.state.formulaModalVisiable && selectedObject && !allDataSource[selectedObject]) {
      this.dataSourceSetDrawer.setObject(selectedObject);
    }
    const sheet = this.spread.getActiveSheet();
    _.map(selections, (range) => {
      const { row, col, rowCount, colCount } = range;
      if (this.isMergedCell({ row, col })) {
        const span = sheet.getSpans(range);
        const { row: r, col: c } = span[0];
        sheet.setFormula(r, c, formula);
      } else {
        for (let r = row; r < row + rowCount; r += 1) {
          for (let c = col; c < col + colCount; c += 1) {
            sheet.setFormula(r, c, formula);
            if (updatePane) this.generateDefaultPane(r, c, formula, sheet);
          }
        }
      }
    });

    this.updateCellFormula({ formula });
  }

  hasPendingChanges = () => {
    for (let i = 0; i < this.spread.getSheetCount(); i += 1) {
      if (this.spread.getSheet(i).hasPendingChanges()) {
        this.hasChanged = true;
        break;
      }
    }
    return this.hasChanged;
  }

  exitDesigner = () => {
    if (this.state.closeDesignerVisible) {
      if (this.hasPendingChanges()) {
        return (
          <CloseDesignerModal
            showOrHideModal={this.showOrHideModal}
            onSaveClose={this.onSaveClose}
            onClose={this.onClose}
            intl={this.props.intl}
          />
        );
      } else {
        this.setState({ closeDesignerVisible: false });
        this.onClose();
      }
    } else {
      return null;
    }
  }

  unfoldDataSourceSet = () => {
    const { unfoldDataSource } = this.state;
    if (unfoldDataSource && this.editChart) {
      this.editChart.isSelected(false);
    }
    this.unfoldDataSource = !unfoldDataSource;
    this.setState({
      unfoldDataSource: !unfoldDataSource,
      chartConfigArea: unfoldDataSource ? false : !!this.editChart
    });
  }

  chooseDataSourceProperties = (item, key, e) => {
    const client = this.designerBox.getBoundingClientRect();
    this.setState({
      allowMove: true,
      moveItem: item,
      upperName: key,
      moveX: (e.pageX - client.left) + 30,
      moveY: (e.pageY - client.top) + 30
    });
  }

  abolishMoveItem = () => {
    if (this.state.allowMove) {
      this.setState({
        allowMove: false,
        moveItem: null
      });
    }
  }

  updateFlag = (state) => {
    this.setState(state);
  }

  move = (e) => {
    if (this.state.allowMove) {
      const client = this.designerBox.getBoundingClientRect();
      this.setState({
        moveX: (e.pageX - client.left) + 34,
        moveY: (e.pageY - client.top) + 34
      });
    }
  }

  updateDataSource = ({ allDataSource, serviceInput, sqlInfo }) => {
    const { dataSource } = this.reporter;
    this.hasChanged = true;
    if (allDataSource) {
      dataSource.allDataSource = allDataSource;
    }
    if (serviceInput) {
      dataSource.serviceInput = serviceInput;
    }
    if (sqlInfo) {
      this.updateSqlInfo(sqlInfo);
    }
  }

  updateSqlInfo = (newSqlInfo = {}) => {
    this.hasChanged = true;
    const { tableName, sqlInfo } = newSqlInfo;
    if (tableName) {
      this.reporter.dataSource.sqlInfo[tableName] = sqlInfo;
    }
  }

  updatePrimaryKeys = (tableName, primaryKeys, { name, typeTransfer } = {}) => {
    const { sqlInfo } = this.reporter.dataSource;
    if (tableName && sqlInfo[tableName]) {
      const info = sqlInfo[tableName];
      info.primaryKeys = primaryKeys;
      if (!info.pkTypeTransfer) info.pkTypeTransfer = {};
      if (!typeTransfer || !/[$]{2}/.test(typeTransfer)) {
        delete info.pkTypeTransfer[name];
      } else {
        info.pkTypeTransfer[name] = typeTransfer;
      }
    }
  }

  addPicture = (pictureUrl) => {
    const sheet = this.spread.getActiveSheet();
    if (pictureUrl) {
      sheet.suspendPaint();
      _.map(sheet.pictures.all(), (item) => {
        if (item.isSelected()) {
          item.isSelected(false);
        }
      });
      const pic = sheet.pictures.add(`picture${new Date().getTime()}`, pictureUrl, 0, 0);
      pic.isSelected(true);
      sheet.resumePaint();
    }
  }

  addChart = (type) => {
    const { intl } = this.props;
    const sheet = this.spread.getActiveSheet();
    const { row, col, colCount, rowCount } = this.getCellRange(sheet) || {};
    if (colCount >= 2 || rowCount >= 2) {
      this.hasChanged = true;
      const { getCellPositionString: getCell } = Util;
      const area = `${getCell(row + 1, col + 1)}:${getCell(row + rowCount, col + colCount)}`;
      const chart = sheet.charts.add(
        `${type}_${area}_${Math.random()}`, // name
        spreadNS.Charts.ChartType[type], // type
        30, 85, 800, 350, // x, y, w, h
        area
      );
      chart.isSelected(true);
      // sheet.setActiveCell(row, col);
      this.showChartPanel(chart, true);
    } else {
      this.warning({ title: intl.formatMessage(messages.Tip), content: intl.formatMessage(messages.ChooseWarning) });
    }
  }

  beforeSave = () => {
    this.hasChanged = false;
    this.spread.suspendPaint();
    for (let index = 0; index < this.spread.getSheetCount(); index += 1) {
      const sheet = this.spread.getSheet(index);
      sheet.clearPendingChanges();
      const floatArray = [...sheet.charts.all(), ...sheet.pictures.all()];
      _.map(floatArray, (item) => {
        if (item.isSelected()) {
          item.isSelected(false);
        }
      });
    }
    this.spread.resumePaint();

    const { sqlInfo, allDataSource } = this.reporter.dataSource;
    const tmp = {};
    _.map(sqlInfo, (item, key) => {
      if (_.keys(allDataSource).includes(key)) tmp[key] = item;
    });
    this.reporter.dataSource.sqlInfo = tmp;
    this.reporter.dataSource = { ...this.reporter.dataSource, cellInfo: {}, dataSourceInfo: {} };
  }

  analyzeFunction = (records) => {
    const keySet = new Set();
    _.map(records, (cell) => {
      const { dataSourceType, param } = cell;
      if (dataSourceType !== 'RT' && dataSourceType !== 'DATE' && param !== '_key_') {
        const { dataSource, tableName } = cell;
        keySet.add(tableName || dataSource);
      }
    });
    return { keySet };
  };

  fetchAllDataSource = (config, keys, cellInfo) => {
    const { object, statisticTask, sqlInfo } = config;
    const tmp = {};
    const noHasList = [];
    const objectData = {};
    _.forEach(object, (obj) => { objectData[obj.dataSource] = obj; });
    _.map(_.merge({}, statisticTask, objectData, sqlInfo), (item, key) => {
      if (tmp[key]) return;
      if (keys.includes(key)) {
        tmp[key] = item;
      }
    });
    _.map(cellInfo, (cell, key) => {
      const { dataSource, tableName } = cell;
      if (!tmp[dataSource || (tableName && tableName.split('.')[0])]) noHasList.push(key);
    });
    return noHasList;
  };

  removeDuplicateSubPane = (selections, paneInfo) => {
    const map = {};
    // map[子格] = 父格
    _.map(paneInfo, (arr, key) => {
      _.map(arr, (subId) => {
        map[subId] = key;
      });
    });
    _.map(selections, (subId) => {
      const key = map[subId];
      if (key) {
        const set = new Set(paneInfo[key]);
        set.delete(subId);
        paneInfo[key] = Array.from(set);
      }
    });
    _.map(paneInfo, (arr, key) => {
      if (!arr.length) delete paneInfo[key];
    });
    return paneInfo;
  }

  beforeSetting = () => {
    this.hasChanged = true;
    const sheet = this.spread.getActiveSheet();
    return {
      sheet,
      selections: this.getSelectionsPositionString(sheet),
      tags: sheet.tag() || {}
    };
  }

  settingConfig = ({ tagName, row, col, action = 'add', selections: singleSelection }) => {
    const { sheet, selections, tags } = this.beforeSetting();
    const config = this.removeDuplicateSubPane(selections, tags[tagName] || {});

    const id = `${col}${row}`;
    const oldCof = new Set(config[id]);

    _.map(singleSelection || selections, (item) => {
      oldCof[action](item);
    });
    if (!oldCof.size) {
      delete config[id];
    } else {
      config[id] = Array.from(oldCof);
    }
    sheet.tag({
      ...tags,
      [tagName]: config
    });

    // 删除窗格
    if (['paneSettingConf', 'paneSettingUpConf'].includes(tagName)) {
      const newTags = sheet.tag() || {};
      const removedConfig = newTags[`${tagName}-removed`] || [];
      _.map(selections, (item) => {
        const { row: r, col: c } = Util.getCellPositionFromString(item);
        const formula = sheet.getFormula(r, c);
        if (action === 'delete' && Util.isDTSerFormula(formula)) {
          // 添加item到删除列表中
          removedConfig.push(item);
        } else if (action === 'add' && removedConfig.includes(item)) {
          // 从删除列表中除去item
          removedConfig.splice(removedConfig.indexOf(item), 1);
        }
      });
      sheet.tag({
        ...newTags,
        [`${tagName}-removed`]: _.uniq(removedConfig)
      });
    }
  }

  resetPaneConf = (args) => {
    const { sheet, tagName, type, count, isRow = true, isUndo = false } = args;
    if (isUndo) return;
    const tags = sheet.tag();
    const paneConf = _.get(tags, tagName) || {};
    const clonePane = _.cloneDeep(paneConf);
    const rowCol = isRow ? 'Row' : 'Column';
    const rowColType = isRow ? 'row' : 'col';
    _.map(_.keys(paneConf), (cell) => {
      const cellObj = Util.getCellPositionFromString(cell);
      const { row, col } = cellObj;
      const subPane = clonePane[cell];
      const tempSubPane = [];
      _.map(subPane, (subCell) => {
        const subCellObj = Util.getCellPositionFromString(subCell);
        const { row: subRow, col: subCol } = subCellObj;
        if (type === `delete${rowCol}s`) {
          if (subCellObj[`${rowColType}`] > args[`${rowColType}`]) {
            tempSubPane.push(Util.getCellPositionString((subRow + 1) - (isRow ? count : 0), (subCol + 1) - (isRow ? 0 : count)));
          } else if (subCellObj[`${rowColType}`] < args[`${rowColType}`]) {
            tempSubPane.push(Util.getCellPositionString(subRow + 1, subCol + 1));
          }
        } else if (type === `add${rowCol}s`) {
          if (subCellObj[`${rowColType}`] >= args[`${rowColType}`]) {
            tempSubPane.push(Util.getCellPositionString(subRow + (isRow ? count + 1 : 1), subCol + (isRow ? 1 : count + 1)));
          } else {
            tempSubPane.push(Util.getCellPositionString(subRow + 1, subCol + 1));
          }
        }
      });
      if (type === `delete${rowCol}s`) {
        if (cellObj[`${rowColType}`] === args[`${rowColType}`]) {
          delete clonePane[cell];
        } else if (cellObj[`${rowColType}`] > args[`${rowColType}`]) {
          delete clonePane[cell];
          clonePane[Util.getCellPositionString((row + 1) - (isRow ? count : 0), (col + 1) - (isRow ? 0 : count))] = tempSubPane;
        } else {
          clonePane[cell] = tempSubPane;
        }
      } else if (type === `add${rowCol}s`) {
        if (cellObj[`${rowColType}`] >= args[`${rowColType}`]) {
          delete clonePane[cell];
          clonePane[Util.getCellPositionString(row + (isRow ? count + 1 : 1), col + (isRow ? 1 : count + 1))] = tempSubPane;
        } else {
          clonePane[cell] = tempSubPane;
        }
      }
    });
    sheet.tag({
      ...tags,
      [tagName]: clonePane
    });
  }

  insertChartModal = () => {
    const { showInsertChartModal } = this.state;
    if (showInsertChartModal) {
      return (
        <InsertChartModal
          showInsertChartModal={showInsertChartModal}
          onOpenChartModal={this.onOpenChartModal}
          addChart={this.addChart}
          intl={this.props.intl}
        />
      );
    }
  }

  renderMenuBar = () => {
    const {
      canPaste = false,
      canBrush = false,
      isfrozenPane = false,
      showRowFrozen = true,
      showColFrozen = true,
      updateMenuBar = true,
      showFrozenPane = true

    } = this.state;
    return (
      <ReportMenuBar
        ref={(node) => { this.menuBar = node; }}
        spread={this.spread}
        // 属性
        canPaste={canPaste}
        canBrush={canBrush}
        isMerged={this.isMerged}
        showFrozenPane={showFrozenPane}
        cancelFrozenPane={isfrozenPane}
        showRowFrozen={showRowFrozen}
        showColFrozen={showColFrozen}
        // 方法
        format={this.format}
        onOpen={this.onOpen}
        onSave={this.onSave}
        onExport={this.onExport}
        onPrint={this.onPrint}
        basicOperate={this.basicOperate}
        frozenPane={this.frozenPane}
        frozenPaneCheck={this.frozenPaneCheck}
        setRowColOptions={this.setRowColOptions}
        formatCells={this.formatCells}
        showAnalyzeModal={this.showOrHideAnalyzeModal}
        unfoldDataSourceSet={this.unfoldDataSourceSet}
        onOpenBarCodeByType={this.onOpenBarCodeByType}
        showOrHideModal={this.showOrHideModal}
        addChart={this.addChart}
        onOpenChartModal={this.onOpenChartModal}
        updateMenuBar={updateMenuBar}
        updateFlag={this.updateFlag}
        intl={this.props.intl}
      />
    );
  }

  renderChartConfigArea = () => {
    if (this.state.chartConfigArea) {
      return (
        <ReportChartSet
          spread={this.spread}
          editChart={this.editChart}
          intl={this.props.intl}
          reportHasChanged={this.reportHasChanged}
        />
      );
    } else {
      return null;
    }
  }

  renderDataSource = () => {
    const { unfoldDataSource, formula, formulaModalVisiable, textVal = null } = this.state;
    const sheet = this.spread ? this.spread.getActiveSheet() : null;
    const selections = sheet ? sheet.getSelections() : [];
    const { allDataSource, sqlInfo, serviceInput } = _.get(this.reporter, 'dataSource', {});
    const { entityCode, modelCode } = this.props;
    if (!unfoldDataSource) {
      return (
        <div
          className={styles['data-source-set-box']}
          style={{ display: unfoldDataSource ? 'none' : 'flex' }}
        >
          <DataSourceSetDrawer
            ref={(node) => { this.dataSourceSetDrawer = node; }}
            appId={this.props.appId}
            allDataSource={allDataSource}
            unfoldDataSource={unfoldDataSource}
            formula={formula}
            selections={selections}
            textVal={textVal}
            updateFlag={this.updateFlag}
            basicOperate={this.basicOperate}
            chooseDataSourceProperties={this.chooseDataSourceProperties}
            unfoldDataSourceSet={this.unfoldDataSourceSet}
            updateDataSource={this.updateDataSource}
            sqlInfo={sqlInfo}
            serviceInput={serviceInput}
            settingConfig={this.settingConfig}
            formulaModalVisiable={formulaModalVisiable}
            backfillConfig={this.backfillConfig}
            updatePrimaryKeys={this.updatePrimaryKeys}
            intl={this.props.intl}
            entityCode={entityCode}
            modelCode={modelCode}
          />
        </div>
      );
    } else {
      return null;
    }
  }

  render() {
    const { allowMove, moveItem, moveX, moveY, upperName, json } = this.state;
    const { intl, templateName } = this.props;
    const config = _.get(this.props, 'config', {});
    return (
      <div className={styles.printContainer}>
        <div className={styles.toolBox}>
          <div>{decodeURIComponent(templateName)}</div>
          <div>
            <Button
              key="save"
              onClick={this.onSave.bind(this, true)}
            >
              {intl.formatMessage(messages.publish)}
            </Button>
            <Button
              key="exit"
              type="primary"
              onClick={this.onSaveClose}
            >
              {intl.formatMessage(messages.saveAndQuit)}
            </Button>
          </div>
        </div>
        <div
          className={styles.spreadDesigner}
          ref={(node) => { this.designerBox = node; }}
        >
          <div className={styles.header}>
            {this.renderMenuBar()}
            <div
              ref={(styleStorage) => { this.styleStorage = styleStorage; }}
              style={{ display: 'none', font: '11pt Calibri' }}
            />
            <div className={styles.hidden}>
              <input
                ref={(node) => { this.fileSelector = node; }}
                type="file"
                onChange={this.onFileSelected}
              />
            </div>
          </div>
          <div className={styles['reporter-box']}>
            <div className={styles['reporter-container']}>
              <div className={styles.valueContainer}>
                <div className={styles.cellName}>
                  <div>{this.state.curCellName}</div>
                </div>
                <div className={styles.cellValue}>
                  <input type="text" ref={(node) => { this.formulabox = node; }} />
                </div>
              </div>
              <div
                onMouseUp={this.abolishMoveItem}
                onMouseLeave={this.abolishMoveItem}
                onMouseMove={this.move}
                style={{ cursor: allowMove ? 'pointer' : 'auto', height: 'calc(100% - 24px)', width: '100%' }}
              >
                <Reporter
                  ref={(node) => { this.reporter = node; }}
                  intl={this.props.intl}
                  spread={this.spread}
                  json={json}
                  config={config}
                  onSheetLoad={this.onSheetLoad}
                  allowMove={allowMove}
                  moveItem={moveItem}
                  upperName={upperName}
                  updateCellFormula={this.updateCellFormula}
                  updateFlag={this.updateFlag}
                  basicOperate={this.basicOperate}
                  updateBarCodeFormula={this.updateBarCodeFormula}
                  isEdit
                />
              </div>
            </div>
            <div>
              {this.renderDataSource()}
              {this.renderChartConfigArea()}
            </div>
          </div>
          {this.showCondFormatModal()}
          {this.showRowColSettingModal()}
          {this.showAnalyzeModal()}
          {this.showFormulaModal()}
          {this.showDateFormulaModal()}
          {this.showCellSettingModal()}
          {this.insertChartModal()}
          {this.showHighFilterModal()}
          {this.showBarCodeModal()}
          {this.exitDesigner()}
          {
            allowMove ? (
              <span
                className={styles['move-item']}
                style={{ left: moveX, top: moveY, pointerEvents: 'none' }}
              >
                {moveItem.propertyName || moveItem}
              </span>
            ) : null
          }
        </div>
      </div>
    );
  }
}

export default injectIntl(ReportDesigner, { withRef: true });
