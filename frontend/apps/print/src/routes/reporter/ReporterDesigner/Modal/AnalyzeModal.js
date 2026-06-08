import React, { Component } from 'react';
import { Row, Col, Input, Button, Icon, notification } from 'sup-ui';
import Rnd from 'react-rnd';
import GC from '@grapecity/spread-sheets';
// import GC from 'root/dependencies/spreadjs/gc.spread.sheets.all.12.0.7.min.js';
import * as Util from '../ReportUtil.js';
import styles from '../Reporter.less';
import messages from '../messages';

const spreadNS = GC.Spread.Sheets;
export default class AnalyzeModal extends Component {
  componentDidMount() {
    this.init();
  }

  setSingleCellValue = (cellText, value) => {
    const { index } = this.getRowStrColStr(cellText);
    const { row, col } = this.getCell(cellText);
    this.props.basicOperate({ opt: 'setCellValue', options: { row, col, value, index } });
  }

  getCell = (cellText) => {
    const { rowStr, colStr, index } = this.getRowStrColStr(cellText);
    const { row, col } = Util.getCellPositionFromString(`${colStr}${rowStr}`);
    return this.props.spread.getSheet(index).getCell(row, col);
  }

  getRowStrColStr = (cellText) => {
    const [, colStr, rowStr] = cellText.match(/!([a-zA-Z]+)(\d+)$/);
    return { rowStr, colStr, index: this.getActiveSheetIndex(cellText) };
  }

  getActiveSheetIndex = (text) => {
    return this.props.sheets[this.matchSheetName(text)];
  }

  getParamValuesByRange = ({ rowStart, colStart, rowEnd, colEnd, sheetName }, { row, col }) => {
    const values = [];
    for (let r = rowStart; r <= rowEnd; r += 1) {
      for (let c = colStart; c <= colEnd; c += 1) {
        let value = this.props.spread.sheets[this.props.sheets[sheetName]].getCell(r, c).value();
        if (row === r && col === c) {
          value = Number(this.targetValue);
        }
        values.push(value);
      }
    }
    return values;
  }

  getTargetCellFormula = (cellText, targetText) => {
    const { intl } = this.props;
    const targetCellInfo = this.parseFormula(targetText);
    const { colStart, rowStart, colEnd, rowEnd, func, value } = targetCellInfo;
    if (!func) {
      notification.warning({ message: intl.formatMessage(messages.cellFormula) });
      return null;
    } else {
      if (!this.verifyCellText(cellText, targetCellInfo)) return;
      const count = ((colEnd - colStart) + 1) * ((rowEnd - rowStart) + 1);
      const paramValues = this.getParamValuesByRange(targetCellInfo, this.getCell(cellText));
      return { func, count, targetCellCurrVal: value, paramValues };
    }
  }

  getCaculatedValue = ({ func, count, targetCellCurrVal, paramValues }, cellText) => {
    const { intl } = this.props;
    if (this.targetValue === targetCellCurrVal) {
      this.handleCancel();
      return false;
    }
    const singleCellValue = Number(this.getCell(cellText).value());
    const diff = this.targetValue - targetCellCurrVal;
    let canCaculate = true;
    switch (func) {
      case 'SUM': return singleCellValue + diff;
      case 'AVERAGE': return singleCellValue + (diff * count);
      case 'COUNT': {
        if (this.targetValue <= count && diff === 1 && !singleCellValue) {
          return 0;
        } else {
          canCaculate = false;
          break;
        }
      }
      case 'MAX': {
        if (this.targetValue === Math.max(...paramValues)) {
          return this.targetValue;
        } else {
          canCaculate = false;
          break;
        }
      }
      case 'MIN': {
        if (this.targetValue === Math.min(...paramValues)) {
          return this.targetValue;
        } else {
          canCaculate = false;
          break;
        }
      }
      default: break;
    }
    if (!canCaculate) {
      notification.warning({ message: intl.formatMessage(messages.cantGain) });
      return false;
    }
  }

  init = () => {
    this.targetSelectBox = new spreadNS.FormulaTextBox.FormulaTextBox(this.targetSelect,
      { rangeSelectMode: true }
    );
    this.targetSelectBox.workbook(this.props.spread);
    this.singleCellSelectBox = new spreadNS.FormulaTextBox.FormulaTextBox(this.singleCellSelect,
      { rangeSelectMode: true }
    );
    this.singleCellSelectBox.workbook(this.props.spread);
  }

  verifyCellText = (cellText, targetCellInfo) => {
    const { intl } = this.props;
    const { colStart, rowStart, colEnd, rowEnd, sheetName: targetSheetName } = targetCellInfo;
    let isValid = false;
    if (targetSheetName || this.matchSheetName(cellText) === targetSheetName) {
      const { row, col } = this.getCell(cellText);
      if (row <= rowEnd && row >= rowStart && col >= colStart && col <= colEnd) isValid = true;
    }
    if (!isValid) {
      notification.warning({ message: intl.formatMessage(messages.cantGain) });
      return false;
    }
    return true;
  }

  parseFormula = (text) => {
    const cell = this.getCell(text);
    if (cell.formula()) {
      const parseFormulaRegex = /!?([a-zA-Z]+)(\d+):([a-zA-Z]+)(\d+)/;
      const [, colStartStr, rowStartStr, colEndStr, rowEndStr] = cell.formula().match(parseFormulaRegex) || [];
      const startCell = Util.getCellPositionFromString(`${colStartStr}${rowStartStr}`);
      const endCell = Util.getCellPositionFromString(`${colEndStr}${rowEndStr}`);
      return {
        colStart: startCell.col,
        rowStart: startCell.row,
        colEnd: endCell.col,
        rowEnd: endCell.row,
        func: this.matchFunc(cell.formula()),
        sheetName: this.matchSheetName(text),
        value: Number(cell.value())
      };
    } else {
      return {};
    }
  }

  matchSheetName = (text) => {
    return Object.keys(this.props.sheets).find((name) => {
      return RegExp(name).test(text);
    });
  }

  matchFunc = (text) => {
    return ['SUM', 'AVERAGE', 'COUNT', 'MAX', 'MIN'].find((func) => {
      return RegExp(func).test(text);
    });
  }

  basicValidation = (text) => {
    const { intl } = this.props;
    let message;
    if (/![a-zA-Z]+\d+:[a-zA-Z]+\d+$/.test(text)) {
      message = intl.formatMessage(messages.mustCell);
    } else if (!text || !/![a-zA-Z]+\d+$/.test(text) || !this.matchSheetName(text)) {
      message = intl.formatMessage(messages.InvalidQuote);
    } else {
      return true;
    }
    notification.warning({ message });
  }

  numberValidation = (value) => {
    const { intl } = this.props;
    if (!/^-?([1-9]\d*|0)(\.\d{1,2})?$/.test(value)) {
      notification.warning({ message: intl.formatMessage(messages.enterFigure) });
    } else {
      return true;
    }
  }

  handleOk = () => {
    const targetText = this.targetSelectBox.text();
    const cellText = this.singleCellSelectBox.text();
    const targetValue = document.getElementById('targetValue').value;
    if (this.basicValidation(targetText) && this.numberValidation(targetValue) && this.basicValidation(cellText)) {
      this.targetValue = Number(targetValue);
      const formula = this.getTargetCellFormula(cellText, targetText);
      if (!formula) return;
      const value = this.getCaculatedValue(formula, cellText);
      if (value !== false) {
        this.setSingleCellValue(cellText, value);
        this.handleCancel();
      }
    }
  }

  handleCancel = () => {
    this.props.showOrHideModal(false);
  }

  render() {
    const { intl } = this.props;
    return (
      <Rnd
        default={{
          x: 500,
          y: 300,
          width: 330,
          height: 300
        }}
        cancel="input"
        enableResizing={false}
      >
        <Row type="flex" justify="space-between" className={styles.analyzeHeader}>
          <Col span={8}>{intl.formatMessage(messages.Solve)}</Col>
          <Col span={1}>
            <Icon type="close" theme="outlined" onClick={this.handleCancel} className={styles.closeCursor} />
          </Col>
        </Row>
        <div className={styles.analyze}>
          <Row>
            <Col span={10}>
              {intl.formatMessage(messages.targetCell)}
              ：
            </Col>
            <Col span={14}>
              <div className={styles.rangeSelectContainer}>
                <div ref={(node) => { this.targetSelect = node; }} />
              </div>
            </Col>
          </Row>
          <Row>
            <Col span={10}>
              {intl.formatMessage(messages.targetValue)}
              ：
            </Col>
            <Col span={14}>
              <Input id="targetValue" />
            </Col>
          </Row>
          <Row>
            <Col span={10}>
              {intl.formatMessage(messages.changeCell)}
              ：
            </Col>
            <Col span={14}>
              <div className={styles.rangeSelectContainer}>
                <div ref={(node) => { this.singleCellSelect = node; }} />
              </div>
            </Col>
          </Row>
          <Row style={{ marginTop: 30 }}>
            <Col span={12}>
              <Button type="primary" onClick={this.handleOk}>{intl.formatMessage(messages.ok)}</Button>
            </Col>
            <Col span={12}>
              <Button onClick={this.handleCancel}>{intl.formatMessage(messages.cancel)}</Button>
            </Col>
          </Row>
        </div>
      </Rnd>
    );
  }
}
