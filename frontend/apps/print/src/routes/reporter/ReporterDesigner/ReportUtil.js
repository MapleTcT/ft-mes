import * as _ from 'lodash';
import GC from '@grapecity/spread-sheets';

const spreadNS = GC.Spread.Sheets;

export function getCellPositionFromString(str) {
  const [, colStr, rowStr] = str.match(/^([a-zA-Z]*)(\d*)$/);
  let col = 0;
  for (let i = 0; i < colStr.length; i += 1) {
    col += ((colStr.charCodeAt(i) - 64) * (26 ** (colStr.length - 1 - i)));
  }
  return { row: Number(rowStr) - 1, col: col - 1 };
}

export function getColString(column) {
  if (column < 1) return null;
  let letters = '';
  while (column > 0) {
    const num = column % 26;
    if (num === 0) {
      letters = `Z${letters}`;
      column -= 1;
    } else {
      letters = String.fromCharCode('A'.charCodeAt(0) + (num - 1)) + letters;
    }
    column = parseInt((column / 26).toString(), 10);
  }
  return letters;
}

export function getCellPositionString(row, column, style = 0) {
  if (row < 1 || column < 1) {
    return null;
  } else {
    let letters = '';
    switch (style) {
      case spreadNS.ReferenceStyle.a1: // 0
        letters = `${getColString(column)}${row.toString()}`;
        break;
      case spreadNS.ReferenceStyle.r1c1: // 1
        letters = `R${row.toString()}C${column.toString()}`;
        break;
      default:
        break;
    }
    return letters;
  }
}

export function setCellTags(sheet, { row, col, key, value }) {
  if (col === undefined) {
    for (let c = 0; c < sheet.getColumnCount(); c += 1) {
      setCellTagsValue(sheet, { row, col: c, key, value });
    }
  } else {
    setCellTagsValue(sheet, { row, col, key, value });
  }
}

export function setCellTagsValue(sheet, { row, col, key, value }) {
  const tags = sheet.getTag(row, col) || {};
  tags[key] = value;
  sheet.setTag(row, col, tags);
}

export function getCellTagValue(sheet, { row, col = 0, key }) {
  return sheet.getTag(row, col) ? sheet.getTag(row, col)[key] : null;
}

export function editHISData(sheet, { cell, dataSource, utcTimestamp, type = 'HIS' }) {
  if (!dataSource) return;
  const key = 'hisInfo';
  const { row, col } = cell;
  if (!getCellTagValue(sheet, { row, col, key })) {
    const [templateNamespace, templateName, objName, propNamespace, propName, dimension] = dataSource.split(':');
    setCellTags(sheet, {
      row,
      col,
      key,
      value: { templateNamespace, templateName, objName, propNamespace, propName, dimension, utcTimestamp, type }
    });
  }
  if (cell.locked()) cell.locked(false);
}

export function editDTData(sheet, { cell, tableName, columnName, primaryKeyObj, typeTransfer }) {
  if (!tableName || !columnName || ['_key_', '_table_'].includes(columnName)) return;
  const key = 'tableInfo';
  const { row, col } = cell;
  setCellTags(sheet, {
    row,
    col,
    key,
    value: { row, col, tableName, columnName, primaryKeyObj, typeTransfer, curValue: cell.value() }
  });

  if (cell.locked()) cell.locked(false);
}

export function getDataSource(formula) {
  const [, dataType, params] = (formula && formula.match(/(RT|HIS|DT|SER|DD)\((.*)\)$/)) || [];
  const [selectedObject] = (params && params.replace(/"/g, '').split(',')) || [];
  if (!selectedObject || ~selectedObject.indexOf('_key_') || ~selectedObject.indexOf('_table_')) return {};
  const [instance] = (selectedObject && selectedObject.replace(/:/g, '.').split('.')) || [];
  return { dataType, dataSource: instance };
}

export function isDTSerFormula(formula) {
  const { dataType } = getDataSource(formula);
  return dataType && ['DT', 'SER'].includes(dataType);
}

export function getCell(spread) {
  const sheet = spread.getActiveSheet();
  const selections = sheet ? sheet.getSelections() : [];
  return selections[0] || {};
}

export function getCellValue(spread) {
  const sheet = spread.getActiveSheet();
  const { row, col } = getCell(spread);
  return row !== undefined && col !== undefined ? sheet.getCell(row, col).value() : null;
}

export function numberTransform(value) {
  if (value && value !== true) {
    return _.isNaN(Number(value)) ? value : Number(value);
  } else {
    return value;
  }
}

export function changeCondFormat(sheet, { parentType, type, rangeValue, newState, selections }) {
  if (parentType === 'formatCells') {
    condFormatByCellsRule(sheet, selections, type, rangeValue, newState);
  } else {
    condFormatByProRule(sheet, selections, type, rangeValue, newState);
  }
}

// 将26进制转10进制
export function ConvertNum(str) {
  let n = 0;
  const s = str.match(/./g); // 求出字符数组
  const len = str.length - 1;
  for (let i = len, j = 1; i >= 0; i -= 1, j *= 26) {
    const c = s[i].toUpperCase();
    if (c < 'A' || c > 'Z') {
      return 0;
    }
    n += (c.charCodeAt(0) - 64) * j;
  }
  n -= 1;
  return n;
}

// 将10进制转26进制
export function Convert26(num) {
  num += 1;
  let str = '';
  while (num > 0) {
    let m = num % 26;
    if (m === 0) {
      m = 26;
    }
    str = String.fromCharCode(m + 64) + str;
    num = (num - m) / 26;
  }
  return str;
}

export function getOriDataSource(dataSource, dataType) {
  if (['RT', 'HIS'].includes(dataType)) {
    const [tempNS, tempName, instance, propNS, propName] = dataSource.replace(/[., #]/g, ':').split(':');
    return {
      templateTab: 'template',
      key: 'instance',
      tab: 'instance',
      selectedTemplate: {
        name: tempName,
        namespace: tempNS
      },
      selectedInstance: {
        name: instance
      },
      selectedProp: {
        propertyName: propName,
        namespace: propNS,
        propertyType: 'property'
      }
    };
  } else if (['SER'].includes(dataType)) {
    if (/服务$/.test(dataSource)) {
      const [, service] = dataSource.match(/(.*)服务/);
      dataSource = service;
    }
    const arr = dataSource.replace(/[., #]/g, ':').split(':');
    let [tempNS, tempName, instance, propNS, propName] = [];
    let [key, tab] = ['service', 'instance'];
    if (arr.length === 4) {
      [tempNS, tempName, propNS, propName] = arr;
      [key, tab] = ['template', 'template'];
    } else {
      [tempNS, tempName, instance, propNS, propName] = arr;
    }
    return {
      templateTab: 'template',
      key,
      tab,
      subTab: 'service',
      selectedTemplate: {
        name: tempName,
        namespace: tempNS
      },
      selectedInstance: {
        name: instance
      },
      selectedProp: {
        name: propName,
        namespace: propNS,
        propertyType: 'service'
      }
    };
  }
}

function condFormatByCellsRule(sheet, selections, type, rangeValue, newState) {
  const { interValue, minValue, maxValue, optionValue } = rangeValue;
  const style = setCellStyleByCondFormat(newState);
  const { conditionalFormats } = sheet;
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

function condFormatByProRule(sheet, selections, type, rangeValue, newState) {
  const { conditionalFormats } = sheet;
  const style = setCellStyleByCondFormat(newState);
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

function setCellStyleByCondFormat({ fillColor, textColor, borderColor }) {
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
