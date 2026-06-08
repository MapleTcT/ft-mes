import * as _ from 'lodash';
import moment from 'moment';
import GC from '@grapecity/spread-sheets';
import * as Util from './ReportUtil.js';

function generateFunc({ funcName, func }) {
  const AsyncFunc = () => { };
  AsyncFunc.prototype = new GC.Spread.CalcEngine.Functions.AsyncFunction(funcName, 1, 255);
  AsyncFunc.prototype.evaluateMode = () => 0;
  AsyncFunc.prototype.evaluateAsync = func;
  GC.Spread.CalcEngine.Functions.defineGlobalCustomFunction(funcName, new AsyncFunc());
}

export function getHisFunc({ isPreview, isEdit, report }) {
  const func = function (context, ...args) {
    const { row, col } = context;
    const maxLength = getMaxLength('HIS') - 1;
    const [text, startIndex = 0, endIndex = maxLength, direction = 'V', group = '1s', aggrType = 'sum', displaySelect = 'value'] = args;
    const dataSource = getDataSource(text);
    const info = getCellInfo(context);
    const { cellId, id, sheetName, formatter } = info;
    try {
      if (isPreview) {
        const auth = getAuth({ info, report });
        if (auth === 3) return '';
        const tempDataSource = dataSource.replace(/\./g, ':');
        const object = `${tempDataSource}:${aggrType}:time(${group})`;
        const param = ~tempDataSource.indexOf('_key_') ? '_key_' : aggrType;
        if (report.records) {
          report.records[id] = {
            cellId,
            dataSourceType: 'HIS',
            sheetName,
            startIndex,
            endIndex,
            curRow: row,
            curCol: col,
            dataSource: object,
            direction,
            param,
            group,
            formatter,
            displaySelect
          };
        }

        const { historyInfo } = report.props;
        const { length } = _.keys(historyInfo);
        if (length) {
          if (param === '_key_') {
            const [, instance] = tempDataSource.match(/(.*):_key_/);
            return _.find(historyInfo, (v, k) => ~k.indexOf(instance) && _.get(v, 'list', false)) ? 1 : '';
          } else {
            const list = _.get(_.get(historyInfo, object), 'list');
            if (auth === 1) {
              const utcTimestamp = _.get(list, `${startIndex}.time`, null);
              Util.editHISData(info.sheet, { cell: info.cell, dataSource: tempDataSource, utcTimestamp, type: 'HIS' });
            }
            if (displaySelect === 'time') {
              const value = numberTransform(_.get(list, `${startIndex}.time`, null));
              return value ? moment(value).utcOffset(moment().utcOffset()).format('YYYY-MM-DD HH:mm:ss') : '';
            } else {
              return numberTransform(_.get(list, `${startIndex}.${aggrType}`, null));
            }
          }
        }
        return '';
      } else {
        if (isEdit && !~dataSource.indexOf('_key_')) {
          report.dataSource.cellInfo[id] = {
            dataSource,
            filters: {
              group: `time(${group})`,
              aggrType,
              limit: (Number(endIndex) - Number(startIndex)) + 1,
              requestType: 'report'
            },
            type: 'Property'
          };
        }
        const [, , instance, , propName] = dataSource.replace(/\./g, ':').split(':');
        const propParam = ~dataSource.indexOf('_key_') ? '_key_' : propName;
        return `#${instance}.${propParam}`;
      }
    } catch (e) {
      console.log(e);
      return 'Error data';
    }
  };
  generateFunc({ funcName: 'HIS', func });
}

export function getRTFunc({ isPreview, isEdit, report }) {
  const func = function (context, ...args) {
    const { row, col } = context;
    const dataSource = getDataSource(args[0], true);
    const param = 'value';
    const info = getCellInfo(context);
    const { cellId, id, sheetName, formatter } = info;
    try {
      if (isPreview) {
        const auth = getAuth({ info, report });
        if (auth === 3) return '';
        if (report.records) {
          report.records[id] = {
            cellId,
            dataSourceType: 'RT',
            sheetName,
            curRow: row,
            curCol: col,
            dataSource,
            param,
            formatter
          };
        }
        const object = _.get(report.objectSource, dataSource);
        return object ? numberTransform(object[0][param], true) : '';
      } else {
        if (isEdit && !~dataSource.indexOf('_key_')) {
          report.dataSource.dataSourceInfo[dataSource.replace(/\./g, ':')] = {};
        }
        const [, , instance, , propName] = dataSource.replace(/\./g, ':').split(':');
        return `#${instance}.${propName}`;
      }
    } catch (e) {
      console.log(e);
      return 'Error data';
    }
  };
  generateFunc({ funcName: 'RT', func });
}

export function getDTFunc({ isPreview, isEdit, report }) {
  const func = function (context, ...args) {
    const { row, col } = context;
    const maxLength = getMaxLength('DT') - 1;
    const [dataSource, startIndex = 0, endIndex = maxLength, direction = 'V', canMerge = true, theme = 'null'] = args;
    const [tableName, param] = dataSource.split('.');
    const info = getCellInfo(context);
    const { cellId, id, sheetName, formatter } = info;
    try {
      if (isPreview) {
        const auth = getAuth({ info, report });
        if (auth === 3) return '';
        if (report.records) {
          report.records[id] = {
            cellId,
            dataSourceType: 'DT',
            sheetName,
            startIndex,
            endIndex,
            curRow: row,
            curCol: col,
            dataSource,
            direction,
            param,
            tableName
          };
          if (param === '_table_') {
            report.records[id].showHeader = canMerge;
            report.records[id].theme = theme;
          } else {
            report.records[id].formatter = formatter;
            report.records[id].canMerge = canMerge;
            if (param !== '_key_' && theme && theme !== 'null') report.records[id].typeTransfer = theme;
          }
        }
        const tableInfo = _.get(report.props.dataTableInfo, `${tableName}.data.dataSource`);
        if (tableInfo && tableInfo.length) {
          if (param === '_key_') return 1;
          return numberTransform(_.get(report.sheetInfo, `${sheetName}.${cellId}.firstValue`));
        } else {
          return '';
        }
      } else {
        if (isEdit && param !== '_key_') {
          report.dataSource.cellInfo[id] = {
            dataTable: dataSource,
            tableName,
            columnName: param,
            pageSize: (endIndex - startIndex) + 1,
            type: 'DT'
          };
        }
        return `#${dataSource}`;
      }
    } catch (e) {
      console.log(e);
      return 'Error data';
    }
  };
  generateFunc({ funcName: 'DT', func });
}

export function getServiceFunc({ isPreview, isEdit, report }) {
  const func = function (context, ...args) {
    const { row, col } = context;
    const maxLength = getMaxLength('SER') - 1;
    const [info, startIndex = 0, endIndex = maxLength, direction = 'V', canMerge = true, theme = 'null'] = args;
    const arr = info.replace(/\./g, ':').split(':');
    let [dataSource, newParam, newInstance, newPropName] = [];
    if (arr.length === 5) {
      const [tempNS, tempName, propNS, propName, param] = arr;
      dataSource = `${tempNS}:${tempName}.${propNS}:${propName.slice(0, -2)}`;
      [newParam, newPropName] = [param, propName];
    } else {
      const [tempNS, tempName, instance, propNS, propName, param] = arr;
      dataSource = `${tempNS}:${tempName}:${instance}.${propNS}:${propName.slice(0, -2)}`;
      [newParam, newPropName, newInstance] = [param, propName, instance];
    }
    const sheetInfo = getCellInfo(context);
    const { cellId, id, sheetName, formatter } = sheetInfo;
    try {
      if (isPreview) {
        const auth = getAuth({ info: sheetInfo, report });
        if (auth === 3) return '';
        if (report.records) {
          report.records[id] = {
            cellId,
            dataSourceType: 'SER',
            sheetName,
            startIndex,
            endIndex,
            curRow: row,
            curCol: col,
            dataSource,
            direction,
            param: newParam
          };
          if (newParam === '_table_') {
            report.records[id].showHeader = canMerge;
            report.records[id].theme = theme;
          } else {
            report.records[id].formatter = formatter;
            report.records[id].canMerge = canMerge;
          }
        }
        const { serviceInfo } = report.props;
        const { length } = _.keys(serviceInfo);
        if (length) {
          if (newParam === '_key_') {
            return _.get(serviceInfo[dataSource.replace(/\./g, ':')], 'list', []).length ? 1 : '';
          } else {
            const firstValue = _.get(report.sheetInfo, `${sheetName}.${cellId}.firstValue`);
            return numberTransform(firstValue);
          }
        }
        return '';
      } else {
        if (isEdit && !~dataSource.indexOf('_key_')) {
          report.dataSource.cellInfo[id] = {
            dataSource,
            filters: {},
            type: 'Service'
          };
        }
        return newInstance ? `#${newInstance}.${newPropName}.${newParam}` : `#${newPropName}.${newParam}`;
      }
    } catch (e) {
      console.log(e);
      return 'Error data';
    }
  };
  generateFunc({ funcName: 'SER', func });
}

export function getRTSFunc({ isPreview, isEdit, report }) {
  const AsyncFunc = () => { };
  AsyncFunc.prototype = new GC.Spread.CalcEngine.Functions.AsyncFunction('RTS', 1, 255);
  AsyncFunc.prototype.evaluateMode = () => 0;
  AsyncFunc.prototype.defaultValue = () => '';
  AsyncFunc.prototype.evaluateAsync = (context, ...args) => {
    const { row, col } = context;
    const maxLength = getMaxLength('RTS') - 1;
    const [text, startIndex = 0, endIndex = maxLength, direction = 'V'] = args;
    const [taskName, source, statisticalType] = text.split('-');
    const dataSource = text.replace(/-/g, '.');
    const sheetInfo = getCellInfo(context);
    const { cellId, id, sheetName, formatter } = getCellInfo(context, report);
    try {
      if (isPreview) {
        const auth = getAuth({ info: sheetInfo, report });
        if (auth === 3) return '';
        if (report.records) {
          report.records[id] = {
            cellId,
            dataSourceType: 'RTS',
            sheetName,
            startIndex,
            endIndex,
            curRow: row,
            curCol: col,
            dataSource,
            direction,
            param: 'value',
            formatter,
            taskName,
            source,
            statisticalType
          };
        }
        const { statisticInfo } = report.props;
        const { length } = _.keys(statisticInfo);
        if (length) {
          const list = _.get(_.get(statisticInfo, dataSource), 'list');
          const value = numberTransform(_.get(list, `${startIndex}.value`, null));
          if (['MAT', 'MIT', 'INT'].includes(statisticalType) && _.isInteger(value)) {
            return moment(value).utcOffset(moment().utcOffset()).format('YYYY-MM-DD HH:mm:ss');
          }
          return value;
        }
        return '';
      } else {
        if (isEdit) {
          report.dataSource.cellInfo[id] = {
            dataSource,
            taskName,
            source,
            statisticalType,
            type: 'RTS',
            limit: (endIndex - startIndex) + 1
          };
        }
        return `#${text}`;
      }
    } catch (e) {
      console.log(e);
      return 'Error data';
    }
  };
  GC.Spread.CalcEngine.Functions.defineGlobalCustomFunction('RTS', new AsyncFunc());
}

export function getDateFunc({ isPreview, report }) {
  const func = function (context, ...args) {
    const { row, col } = context;
    const [type = '1d', templateType, direction = 'V', dataLength = 0] = args;
    const [, interval = 1, dateType = 'd'] = type.match(/(\d*)(y|M|d|h)/);
    const info = getCellInfo(context);
    const { cellId, id, sheetName, formatter } = info;
    try {
      if (isPreview) {
        const auth = getAuth({ info, report });
        if (auth === 3) return '';
        if (report.records) {
          report.records[id] = {
            cellId,
            dataSourceType: 'DATE',
            dataSource: 'date',
            sheetName,
            startIndex: 0,
            curRow: row,
            curCol: col,
            interval: Number(interval),
            dateType,
            templateType,
            direction,
            param: 'date',
            formatter,
            dataLength: Number(dataLength)
          };
        }
        const value = getFirstDateItem(dateType, templateType);
        return numberTransform(value);
      } else {
        return `#${templateType}`;
      }
    } catch (e) {
      console.log(e);
      return 'Error data';
    }
  };

  const getFirstDateItem = (dateType, templateType) => {
    let { minDate: min, maxDate: max } = report.props;
    min = min ? moment(min) : moment().subtract(1, 'd');
    max = max ? moment(max) : moment();

    if (min.diff(moment(max)) > 0) return '';

    let template = templateType;
    switch (dateType) {
      case 'h': {
        template = templateType.replace('y', min.year()).replace('M', min.month() + 1).replace('d', min.date());
        min = min.hour();
        break;
      }
      case 'd': {
        template = templateType.replace('y', min.year()).replace('M', min.month() + 1);
        min = min.date();
        break;
      }
      case 'M': {
        template = templateType.replace('y', min.year());
        min = min.month() + 1;
        break;
      }
      case 'y': {
        min = min.year();
        break;
      }
      default: break;
    }
    return template.replace(RegExp(dateType), min);
  };

  generateFunc({ funcName: 'DD', func });
}

export function getOperateFunc({ isPreview, report }) {
  const func = function (context, ...args) {
    const { row, col } = context;
    const [dataSource, optType, optScope, text, tableName] = args;
    const sheetInfo = getCellInfo(context);
    const { cellId, id, sheetName } = sheetInfo;
    try {
      if (isPreview) {
        const auth = getAuth({ info: sheetInfo, report });
        if (auth === 3) return '';
        if (report.optRecords) {
          report.optRecords[id] = {
            cellId,
            dataSource,
            dataSourceType: 'OPT',
            sheetName,
            curRow: row,
            curCol: col,
            optType,
            optScope,
            tableName,
            text
          };
        }
      }
      return text;
    } catch (e) {
      console.log(e);
      return 'Error data';
    }
  };
  generateFunc({ funcName: 'OPT', func });
}

export function getSystemInfoFunc({ isPreview, isEdit, report }) {
  const func = function (context, ...args) {
    const [value] = args;
    const dataSource = value.split("@_@")[0];
    const { row, col } = context;
    const info = getCellInfo(context);
    const { cellId, id, sheetName, formatter, formula } = info;
    try {
      if (isPreview) {
        const auth = getAuth({ info, report });
        if (auth === 3) return '';
        if (report.records) {
          report.records[id] = {
            cellId,
            dataSourceType: 'SYS',
            dataSource: 'system',
            sheetName,
            startIndex: 0,
            curRow: row,
            curCol: col,
            param: 'date'
          };
        }
        let firstValue = '';
        try {
          const data = _.get(report, `props.config.systemInfo.${dataSource}.data`, '');
          const result = new Function(data)();
          if (_.isArray(result)) {
            [firstValue] = result;
          } else {
            firstValue = result;
          }
        } catch (err) {
          console.log(err);
        }
        return firstValue;
      } else {
        if (isEdit) {
          report.dataSource.cellInfo[id] = {
            dataSource,
            type: 'System'
          };
        }
        if (formula && formula.match(/^BC_(\w+)\(/)) {
          return setBarCodeDefaultValue(formula)
        }
        return `#${dataSource}`;
      }
    } catch (e) {
      console.log(e);
      return 'Error data';
    }
  };

  generateFunc({ funcName: 'SYS', func });
}

export function getCustomServiceFunc({ isPreview, isEdit, report }) {
  const func = function (context, ...args) {
    const { row, col } = context;
    const maxLength = getMaxLength('CTS') - 1;
    const [info, startIndex = 0, endIndex = maxLength, direction = 'V', canMerge = false, theme = 'null'] = args;
    const arr = info.replace(/\./g, ':').split(':');
    const [newPropName, newParam] = arr;
    const sheetInfo = getCellInfo(context);
    const { cellId, id, sheetName, formatter } = sheetInfo;
    try {
      if (isPreview) {
        const auth = getAuth({ info: sheetInfo, report });
        if (auth === 3) return '';
        if (report.records) {
          report.records[id] = {
            cellId,
            dataSourceType: 'CTS',
            sheetName,
            startIndex,
            endIndex,
            curRow: row,
            curCol: col,
            dataSource: newPropName,
            direction,
            param: newParam
          };
          if (newParam === '_table_') {
            report.records[id].showHeader = canMerge;
            report.records[id].theme = theme;
          } else {
            report.records[id].formatter = formatter;
            report.records[id].canMerge = canMerge;
          }
        }
        const { customServices } = report.props;
        const { length } = _.keys(customServices);
        if (length) {
          if (newParam === '_key_') {
            return _.get(customServices[info.replace(/\./g, ':')], 'list', []).length ? 1 : '';
          } else {
            const firstValue = _.get(report.sheetInfo, `${sheetName}.${cellId}.firstValue`);
            return numberTransform(firstValue);
          }
        }
        return '';
      } else {
        if (isEdit && !~info.indexOf('_key_')) {
          report.dataSource.cellInfo[id] = {
            dataSource: newPropName,
            filters: {},
            type: 'CustomService'
          };
        }
        return `#${newPropName}.${newParam}`;
      }
    } catch (e) {
      console.log(e);
      return 'Error data';
    }
  };
  generateFunc({ funcName: 'CTS', func });
}

export function getEntityFunc({ isPreview, isEdit, report }) {
  const func = function (context, ...args) {
    const { row, col } = context;
    const maxLength = getMaxLength('ENT') - 1;
    const [value, startIndex = 0, endIndex = maxLength, direction = 'V', canMerge = false] = args;
    const info = value.split("@_@")[0];
    const arr = info.split('@:@');
    const [model, ...propertyArr] = arr;
    const [modelName, modelCode] = model.split('@#@');
    const sheetInfo = getCellInfo(context);
    const { cellId, id, sheetName, formatter, formula } = sheetInfo;
    const codeArr = [];
    let propName = '';
    _.forEach(propertyArr, (item, idx) => {
      const [name, code] = item.split('@#@');
      codeArr.push(code);
      if (idx === propertyArr.length - 1) propName = name;
    });
    const propCode = codeArr.join('#');
    try {
      if (isPreview) {
        const auth = getAuth({ info: sheetInfo, report });
        if (auth === 3) return '';
        if (report.records) {
          report.records[id] = {
            cellId,
            dataSourceType: 'ENT',
            sheetName,
            startIndex,
            endIndex,
            curRow: row,
            curCol: col,
            canMerge,
            dataSource: `${modelCode}@:@${propCode}`,
            param: propCode,
            direction,
            formatter
          };
        }
        const { entityObjects } = report.props;
        const { length } = _.keys(entityObjects);
        if (length) {
          const values = _.filter(entityObjects, (item, key) => {
            const [newModelCode, newPropCode] = key.split('@:@');
            return modelCode === newModelCode && propCode === newPropCode;
          });
          const list = _.get(values, '[0].list', []);
          return values ? numberTransform(_.get(list, `${startIndex}`), true) : '';
        }
        return '';
      } else {
        if (isEdit && !~info.indexOf('_key_')) {
          report.dataSource.cellInfo[id] = {
            dataSource: info,
            type: 'EntityObjects'
          };
        }
        if (formula && formula.match(/^BC_(\w+)\(/)) {
          return setBarCodeDefaultValue(formula)
        }
        return `#${modelName}@:@${propName}`;
      }
    } catch (e) {
      console.log(e);
      return 'Error data';
    }
  };
  return generateFunc({ funcName: 'ENT', func });
}

export function getBarFunc({ isPreview, isEdit, report }) {
  const func = function (context, ...args) {
    const { row, col } = context;
    const [info] = args;
    const sheetInfo = getCellInfo(context);
    const { cellId, id, sheetName, formatter, formula } = sheetInfo;
    try {
      // if (isPreview) {
      //   const auth = getAuth({ info: sheetInfo, report });
      //   if (auth === 3) return '';
      //   if (report.records) {
      //     report.records[id] = {
      //       cellId,
      //       dataSourceType: 'ENT',
      //       sheetName,
      //       curRow: row,
      //       curCol: col,
      //       formatter,
      //       dataSource: info
      //     };
      //   }
      // }
      if (isEdit) {
        if (!~info.indexOf('_key_')) {
          report.dataSource.cellInfo[id] = {
            dataSource: info,
            type: 'EntityObjects'
          };
        }
        return setBarCodeDefaultValue(formula);
      }
    } catch (e) {
      console.log(e);
      return 'Error data';
    }
  };
  return generateFunc({ funcName: 'BAR', func });
}

function setBarCodeDefaultValue(formula) {
  if (formula) {
    const formulaArr = formula.match(/^BC_(\w+)\(/);
    if (formulaArr) {
      if (formulaArr[1] === 'EAN13') return 6920312296219;
      if (formulaArr[1] === 'CODE128') return 225869;
    }
  }
  return 1234567;
}

function getAuth({ info, report }) {
  const { sheetName, cellId } = info;
  return _.get(report.authorityObj, `${sheetName}.${cellId}`);
}

function getCellInfo(context) {
  const { row, col } = context;
  const sheet = context.ctx.source.getSheet();
  const sheetName = sheet.name();
  const cellId = Util.getCellPositionString(row + 1, col + 1);
  const formatter = sheet.getFormatter(row, col);
  const formula = sheet.getFormula(row, col);
  return {
    sheet,
    sheetName,
    id: `${sheetName}!${cellId}`,
    cellId,
    formatter,
    formula,
    cell: sheet.getCell(row, col)
  };
}

function getDataSource(initValue = '', isRT = false) {
  let dataSource = initValue;
  const array = initValue.replace(/#/g, '.').split('.') || [];
  if (array.length > 2) {
    dataSource = isRT ? `${array[0]}.${array[1]}` : `${array[0]}.${array[1]}`;
  }
  return dataSource;
}

function numberTransform(value, isRT = false) {
  if (isRT) {
    if (value === true) return 1;
    else if (value === false) return 0;
  }
  // if (value && value !== true) {
  //   return _.isNaN(Number(value)) ? value : Number(value);
  // }
  return value || '';
}

function getMaxLength(dataType) {
  let maxLength = 500;
  switch (dataType) {
    case 'DT':
      maxLength = 200;
      break;
    case 'RTS':
      maxLength = 720;
      break;
    case 'HIS':
      maxLength = 500;
      break;
    default:
      maxLength = 300;
      break;
  }
  return maxLength;
}
