import moment from 'moment';
import { message } from 'sup-ui';
import * as _ from 'lodash';
import GC from '@grapecity/spread-sheets';
import * as Util from './ReportUtil.js';
import { CustomFormat } from './CustomFormat';
import messages from './messages';

const spreadNS = GC.Spread.Sheets;
let config = {};
let dataInfo = {};
let sheetInfo = {};
let condFormat = {};
let sheet = null;
let originalDataSource = {};
let allDataSource = {};
let authorityObj = null;
let chartDataRange = null;
let minDate = null;
let maxDate = null;
let intl = null;
let autoFitRows = [];

export function fillData(report) {
  ({ authorityObj, chartDataRange, props: { config, minDate, maxDate, intl } } = report);
  sheetInfo = {};
  dataInfo = {};
  report.sheetInfo = sheetInfo;
  autoFitRows = [];
  if (_.get(config, 'fillDataType') === 'replace') {
    prepareDataForReplace(report);
  } else {
    prepareDataForInsert(report);
  }
  // dataInfo: 记录点击位置的内容
  report.dataInfo = dataInfo;
  // if (_.get(report.props, 'config.scrollbarVisible') === 'auto') {
  //   report.setHorizontalScrollbarVisiable();
  // }
}

const analyzeFunction = (records) => {
  const sheetInfoForRt = {};
  const keySet = new Set();
  _.map(records, (cell) => {
    const { cellId: id, sheetName, dataSourceType, param } = cell;
    if (dataSourceType === 'SYS') return;
    if (dataSourceType === 'RT') {
      if (!sheetInfoForRt[sheetName]) sheetInfoForRt[sheetName] = {};
      sheetInfoForRt[sheetName][id] = cell;
    } else {
      if (dataSourceType !== 'DATE' && param !== '_key_') {
        const { dataSource, tableName } = cell;
        keySet.add(tableName || dataSource);
      }
      if (!sheetInfo[sheetName]) sheetInfo[sheetName] = {};
      sheetInfo[sheetName][id] = cell;
    }
  });
  return { keySet, sheetInfo, sheetInfoForRt };
};

const prepareDataForInsert = (report) => {
  const { keySet, sheetInfoForRt } = analyzeFunction(_.cloneDeep(report.allRecords));
  const allData = report.getAllData();

  if (!fetchAllDataSource(allData, Array.from(keySet))) return;

  report.spread.suspendPaint();
  report.updateJson();

  _.map(sheetInfo, (cellInfo, sheetName) => {
    sheet = report.spread.getSheetFromName(sheetName);
    condFormat = {};
    allDataSource = {};
    const rtCellInfo = sheetInfoForRt[sheetName];
    // 获取关联函数信息
    const formulaInfo = getFormulaInfo(cellInfo);
    // 获取左父格信息
    const subPaneInfo = getSubPaneInfo(cellInfo, 'paneSettingConf');
    // 获取上父格信息
    const subPaneUpInfo = getSubPaneInfo(cellInfo, 'paneSettingUpConf');
    // 根据窗格信息重组数据源
    const newSubPaneMap = resetDataSource({ cellInfo, subPaneInfo, subPaneUpInfo });
    // 合并左父格、上父格
    const newSubPane = mergeSubPane(subPaneInfo, subPaneUpInfo, newSubPaneMap);
    // 插入填充
    const lengthMap = fillDataByInsert({ cellInfo, subPaneInfo: newSubPane, formulaInfo });
    // 重设公式位置的值
    resetFirstPosValue({ cellInfo, lengthMap, authority: authorityObj[sheet.name()] });
    // 计算实时数据新位置
    rtData(report, rtCellInfo, lengthMap);
  });
  report.spread.resumePaint();
};

const resetFirstPosValue = ({ cellInfo, lengthMap = { row: {}, col: {} }, authority = {} }) => {
  _.map(cellInfo, (item) => {
    let cell = {};
    let [rowDiff, colDiff] = [0, 0];
    const { dataSourceType, param, firstId, curCol, curRow, cellId, typeTransfer } = item;
    if (firstId && ['HIS', 'DT', 'SER', 'RTS', 'CTS', 'ENT'].includes(dataSourceType) && !['_key_', '_table_'].includes(param)) {
      const { dataSource, tableName } = cellInfo[firstId] || {};
      const key = `${firstId}-${tableName || dataSource}`;
      const firstValue = getFirstPosValue(item, authority[cellId]);
      const { curRow: firstRow, curCol: firstCol, direction } = cellInfo[firstId];

      _.map(lengthMap.row, (len, rowIndex) => {
        const index = Number(rowIndex);
        if (index < curRow && (direction === 'H' || index !== firstRow)) rowDiff += len;
      });
      _.map(lengthMap.col, (len, colIndex) => {
        const index = Number(colIndex);
        if (index < curCol && (direction === 'V' || index !== firstCol)) colDiff += len;
      });

      if (rowDiff || colDiff) {
        const [newRow, newCol] = [curRow + rowDiff, curCol + colDiff];
        const newId = Util.getCellPositionString(newRow + 1, newCol + 1);
        if (!cellInfo[newId]) cellInfo[newId] = {};
        cellInfo[newId].firstValue = firstValue;
        cell = sheet.getCell(newRow, newCol);
        cell.value(firstValue);
        setFillCellRowHeight(newRow, newCol);
      } else {
        item.firstValue = firstValue;
        cell = sheet.getCell(curRow, curCol);
        cell.value(firstValue);
        setFillCellRowHeight(curRow, curCol);
      }

      if (firstValue === null) {
        cell.formatter(new CustomFormat());
      }
      // 删除自定义公式
      cell.formula(null);

      // 设置权限
      if (authority[cellId] === 1 && config.runTimeEdit) {
        switch (dataSourceType) {
          case 'HIS': Util.editHISData(sheet, { cell, dataSource, utcTimestamp: getHisTime({ key, index: 0 }) }); break;
          case 'DT': Util.editDTData(sheet, { cell, tableName, columnName: param, typeTransfer, primaryKeyObj: getPrimaryKeys({ key, tableName, index: 0 }) }); break;
          default: break;
        }
      }
    }
    // 条件格式
    const formats = Util.getCellTagValue(sheet, { row: cell.row, col: cell.col, key: 'conditionalFormats' });
    if (formats && condFormat[cellId]) {
      sheet.conditionalFormats.removeRuleByRange(cell.row, cell.col, 1, 1);
      condFormat[cellId].push(new spreadNS.Range(cell.row, cell.col, 1, 1));
      _.map(formats, (format) => {
        format.selections = condFormat[cellId];
        Util.changeCondFormat(sheet, format);
      });
    }
  });
};

const getFirstPosValue = (cell, authority) => {
  const { dataSourceType, dataSource, tableName, param, newParam: paneParam, group, firstId, cellId, curRow, curCol, displaySelect } = cell;
  if (!['HIS', 'SER', 'DT', 'RTS', 'CTS', 'ENT'].includes(dataSourceType)) return;
  // 有过滤条件时计算自身
  let key = `${firstId}-${tableName || dataSource}`;
  if (getFilterRules(curRow, curCol).length && _.get(allDataSource, `${cellId}-${tableName || dataSource}`)) {
    key = `${cellId}-${tableName || dataSource}`;
  }
  let [firstObjct] = _.get(allDataSource[key], 'list', []);
  let newParam;
  switch (dataSourceType) {
    case 'HIS': newParam = displaySelect === 'time' ? 'time' : `${paneParam || param}-${group}`; break;
    case 'ENT': firstObjct = { [param]: firstObjct };
    default: newParam = paneParam || param; break;
  }
  return authority === 3 ? null : _.get(firstObjct, newParam, null);
};

const mergeSubPane = (subPaneInfo, subPaneUpInfo, newSubPaneMap) => {
  const map = _.merge({}, newSubPaneMap);
  _.map(map, (arr, id) => {
    if (arr.includes(id)) arr.splice(arr.indexOf(id), 1);
  });
  return {
    info: _.merge({}, subPaneInfo.info, subPaneUpInfo.info),
    map
  };
};

const resetDataSource = ({ cellInfo, subPaneInfo = { map: {}, info: {} }, subPaneUpInfo = { map: {}, info: {} }, fillType = 'insert' }) => {
  // 排序,去重, 过滤
  const { paneRelative, paneRelativeParams, bothPaneRelative } = sortDataSource(cellInfo, subPaneInfo, subPaneUpInfo, fillType);
  // 重组生成新List
  generateNewDataSource(paneRelativeParams, cellInfo);
  // 生成双父格数据
  resetBothPaneData(bothPaneRelative, cellInfo);
  // 在子父格集中删除双父格信息
  resetBothPaneCellInfo(cellInfo, bothPaneRelative, subPaneInfo, subPaneUpInfo);

  return paneRelative;
};

const resetBothPaneCellInfo = (cellInfo, bothPaneRelative, subPaneInfo, subPaneUpInfo) => {
  _.map(_.keys(bothPaneRelative), (key) => {
    if (!cellInfo[key]) return;
    const cell = cellInfo[key];
    const { cellId } = cell;
    cell.firstId = cellId;
    cell.param = '_table_';
    cell.showHeader = false;
    cell.theme = 'null';
    clearSubPaneInfo(subPaneInfo, cellId);
    clearSubPaneInfo(subPaneUpInfo, cellId);
  });
};

const clearSubPaneInfo = (subPane, cellId) => {
  const { map, info } = subPane;
  delete info[cellId];
  delete map[cellId];
  _.map(map, (arr, id) => {
    if (arr.includes(cellId)) arr.splice(arr.indexOf(cellId), 1);
    if (!arr.length) delete map[id];
  });
};

const resetBothPaneData = (bothPaneRelative, cellInfo) => {
  _.map(bothPaneRelative, (info, subId) => {
    if (!cellInfo[subId]) return;
    cellInfo[subId].firstId = subId;
    const { dataSource, tableName, param, endIndex, startIndex } = cellInfo[subId];
    const key = `${subId}-${tableName || dataSource}`;
    const list = getAllInitList(cellInfo[subId]);
    const { left: { firstId: firstIdLeft }, up: { firstId: firstIdUp } } = info;
    const [paramsLeft, paramsUp] = [info.left.params, info.up.params];
    const params = [...paramsLeft, ...paramsUp, param];
    if (list) {
      if (params.includes('_table_')) {
        allDataSource[key] = { list, pageSize: list.length };
      } else {
        const initList = _.map(list, row => _.pick(row, params));
        const newList = [];
        // 获取数据源key
        const left = cellInfo[firstIdLeft].tableName || cellInfo[firstIdLeft].dataSource;
        const up = cellInfo[firstIdUp].tableName || cellInfo[firstIdUp].dataSource;
        // 根据key，获取数据源
        const { list: leftList } = allDataSource[`${firstIdLeft}-${left}`] || {};
        const { list: upList } = allDataSource[`${firstIdUp}-${up}`] || {};

        _.map(leftList, (leftRow) => {
          const tmpList = [];

          // 获取与左父格内容相同的所有行
          _.map(initList, (initRow) => {
            const notMatched = _.find(leftRow, (v, k) => v !== initRow[k]);
            if (!notMatched) tmpList.push(initRow);
          });

          // 从结果集中获取与上父格内容相同的所有列
          const newRow = {};
          _.map(upList, (upRow) => {
            const newKey = `Col${_.values(upRow).join('-')}`;
            _.map(tmpList, (tmpRow) => {
              const notMatched = _.find(upRow, (v, k) => v !== tmpRow[k]);
              if (!notMatched) {
                if (newRow[newKey] === undefined) newRow[newKey] = tmpRow[param];
              }
            });
            // 补值
            if (newRow[newKey] === undefined) newRow[newKey] = null;
          });

          newList.push(newRow);
        });

        const pageSize = Math.min((endIndex - startIndex) + 1, newList.length);
        allDataSource[key] = { list: newList.slice(0, pageSize), pageSize };
      }
    }
    const { list: newList } = allDataSource[key];
    [cellInfo[subId].newParam] = _.keys(newList[0]);
  });
};

const handleTableData = (cell, key) => {
  const { param, direction, showHeader } = cell;
  if (param === '_table_' && direction === 'H') {
    const { list } = allDataSource[key];
    allDataSource[key] = switchOrientation(list, showHeader);
  }
};

const generateNewDataSource = (paneRelative, cellInfo) => {
  _.map(allDataSource, (item, key) => {
    const [firstId] = key.split('-');
    const { endIndex, startIndex } = cellInfo[firstId];
    const params = paneRelative[firstId];
    if (!params) return;
    const newList = _.get(allDataSource[key], 'list', []);
    generateNewList(key, newList, endIndex, startIndex);
    handleTableData(cellInfo[firstId], key);
  });
};

const generateNewList = (key, list, endIndex, startIndex) => {
  const length = (endIndex - startIndex) + 1;
  const pageSize = _.isNaN(length) ? list.length : Math.min(length, list.length);
  allDataSource[key].list = list.slice(0, pageSize);
  allDataSource[key].pageSize = pageSize;
};

const getFirstPaneRelative = (id, info, cellInfo) => {
  const cell = info[id];
  // 当cellInfo为子窗格，且父格为DT, SER, HIS
  if (!cell || !['DT', 'SER', 'HIS', 'RTS', 'CTS', 'ENT'].includes(_.get(cellInfo[cell.paneId], 'dataSourceType'))) return id;
  return getFirstPaneRelative(cell.paneId, info, cellInfo);
};

const setPaneRelative = (id, map, mapUp, mapBoth, arr, cellInfo) => {
  const subIds = [].concat(map[id] || []).concat(mapUp[id] || []);

  // 去除双父格的id
  _.map(mapBoth, (bothId) => {
    if (subIds.includes(bothId)) subIds.splice(subIds.indexOf(bothId), 1);
  });

  // 添加DT、SER的参数
  if (['DT', 'SER', 'HIS', 'RTS', 'CTS', 'ENT'].includes(_.get(cellInfo[id], 'dataSourceType')) && subIds.length) {
    arr.push(...subIds);
    _.map(subIds, (subId) => {
      setPaneRelative(subId, map, mapUp, mapBoth, arr, cellInfo);
    });
  }
};

const getBothPaneRelative = (subPaneInfo, subPaneUpInfo, cellInfo) => {
  const { info } = subPaneInfo;
  const { info: infoUp } = subPaneUpInfo;
  const bothPaneRelative = {};
  _.map(info, (subCell, cellId) => {
    if (_.get(infoUp, cellId)) {
      bothPaneRelative[cellId] = { left: {}, up: {} };
      const cell = bothPaneRelative[cellId];
      const firstIdLeft = getFirstPaneRelative(subCell.paneId, info, cellInfo);
      const firstIdUp = getFirstPaneRelative(infoUp[cellId].paneId, infoUp, cellInfo);
      cell.left = { firstId: firstIdLeft, params: [] };
      cell.up = { firstId: firstIdUp, params: [] };
    }
  });
  return bothPaneRelative;
};

const getHisInitList = (firstId, params, cellInfo, cellId) => {
  let newList = [];
  const cell = cellInfo[cellId];
  if (!cell) return;
  const { param, group } = cell;
  const list = getAllInitList(cell);
  if (group) {
    newList = _.map(list, (row) => {
      const newObj = {};
      const item = _.pick(row, [param, 'time']);
      newObj[`${param}-${group}`] = _.get(item, param);
      newObj.time = _.get(item, 'time');
      return newObj;
    });
  }
  return newList;
};

const getRTSInitList = (firstId, params, cellInfo, cellId) => {
  const cell = cellInfo[cellId];
  if (!cell) return;
  const list = getAllInitList(cell);
  return list;
};

const getAllInitList = (cell) => {
  const { dataSourceType, dataSource, tableName } = cell;
  switch (dataSourceType) {
    case 'DT': return _.cloneDeep(_.get(originalDataSource[tableName], 'data.dataSource', []));
    case 'RTS':
    case 'HIS':
    case 'CTS':
    case 'SER':
    case 'ENT':
    default: return _.cloneDeep(_.get(originalDataSource[dataSource], 'list', []));
  }
};

const getInitList = (dataSourceType, cellInfo, firstId, paneRelative, dataSource, tableName, cellId) => {
  let list;
  switch (dataSourceType) {
    case 'HIS': list = getHisInitList(firstId, paneRelative[firstId], cellInfo, cellId); break;
    case 'RTS': list = getRTSInitList(firstId, paneRelative[firstId], cellInfo, cellId); break;
    default: list = getAllInitList(cellInfo[cellId]); break;
  }
  return { list, key: `${firstId}-${tableName || dataSource}` };
};

const bindPaneForSeq = (cellInfo, subPaneInfo, subPaneUpInfo) => {
  const seqInfo = {};
  _.map(cellInfo, (cell, id) => {
    const { param } = cell;
    if (param === '_key_') {
      // 删除序号作为父窗格时的关联关系
      clearSeqAsPane(id, subPaneInfo);
      clearSeqAsPane(id, subPaneUpInfo);

      // 查找未绑定父窗格的序号单元格id
      if (!_.get(subPaneInfo.info, id) && !_.get(subPaneUpInfo.info, id)) {
        seqInfo[id] = cell;
      }
    }
  });

  _.map(seqInfo, (cell) => {
    const { curRow, curCol, direction, cellId } = cell;
    if (direction === 'V') {
      const leftCellId = Util.getCellPositionString(curRow + 1, curCol);
      const rightCellId = Util.getCellPositionString(curRow + 1, curCol + 2);
      let canBind = false;
      if (cellInfo[leftCellId]) {
        canBind = bindPane(leftCellId, cellId, subPaneInfo, cellInfo);
      }
      if (!canBind && cellInfo[rightCellId]) {
        bindPane(rightCellId, cellId, subPaneInfo, cellInfo);
      }
    } else if (direction === 'H') {
      const upCellId = Util.getCellPositionString(curRow, curCol + 1);
      const belowCellId = Util.getCellPositionString(curRow + 2, curCol + 1);
      let canBind = false;
      if (cellInfo[upCellId]) {
        canBind = bindPane(upCellId, cellId, subPaneInfo, cellInfo);
      }
      if (!canBind && cellInfo[belowCellId]) {
        bindPane(belowCellId, cellId, subPaneInfo, cellInfo);
      }
    }
  });
};

const clearSeqAsPane = (id, subPane) => {
  if (_.get(subPane.map, id)) {
    const tmp = [...subPane.map[id]];
    delete subPane.map[id];
    _.map(tmp, subId => delete subPane.info[subId]);
  }
};

const getDataSource = (cell) => {
  const { dataSourceType, tableName, dataSource } = cell;
  if (dataSourceType === 'DT') {
    return tableName;
  } else {
    return dataSource ? dataSource.split('.')[0] : '';
  }
};

const bindPane = (paneId, id, subPaneInfo, cellInfo) => {
  const paneDataSource = getDataSource(cellInfo[paneId]);
  const dataSource = getDataSource(cellInfo[id]);
  if (paneDataSource !== dataSource) return false;
  if (cellInfo[paneId]) {
    subPaneInfo.info[id] = _.merge({}, cellInfo[id]);
    subPaneInfo.info[id].paneId = paneId;
    // 删除绑在序号上的关联关系
    delete subPaneInfo.map[id];
    if (_.get(subPaneInfo.info[paneId], 'paneId') === id) {
      // 删除绑在序号上的子窗格信息
      delete subPaneInfo.info[paneId];
    }
    if (subPaneInfo.map[paneId]) {
      subPaneInfo.map[paneId].push(id);
    } else {
      subPaneInfo.map[paneId] = [id];
    }
  }
  return true;
};

const sortDataSource = (cellInfo, subPaneInfo, subPaneUpInfo, fillType) => {
  // 获取双父格的id
  const bothPaneRelative = getBothPaneRelative(subPaneInfo, subPaneUpInfo, cellInfo);
  const paneRelative = {};
  const paneRelativeParams = {};
  const filterInfo = {};

  // 序号自动绑定父窗格
  bindPaneForSeq(cellInfo, subPaneInfo, subPaneUpInfo);

  // 获取左父格最上级id
  _.map(_.keys(subPaneInfo.info), (id) => {
    const firstId = getFirstPaneRelative(id, subPaneInfo.info, cellInfo);
    paneRelative[firstId] = [firstId];
  });

  // 获取上父格最上级id
  _.map(_.keys(subPaneUpInfo.info), (id) => {
    const firstId = getFirstPaneRelative(id, subPaneUpInfo.info, cellInfo);
    paneRelative[firstId] = [firstId];
  });

  // 获取没有父子窗格的id
  _.map(_.keys(cellInfo), (id) => {
    if (!subPaneInfo.info[id] && !subPaneUpInfo.info[id]) paneRelative[id] = [id];
  });

  // 获取父级子级间的关联关系
  _.map(paneRelative, (arr, firstId) => {
    setPaneRelative(firstId, subPaneInfo.map, subPaneUpInfo.map, _.keys(bothPaneRelative), arr, cellInfo);
  });

  // 匹配关联关系中的字段
  _.map(paneRelative, (ids, firstId) => {
    if (!cellInfo[firstId]) return;
    paneRelativeParams[firstId] = [];
    _.map(ids, (id) => {
      if (!cellInfo[id]) return;
      const { dataSource, dataSourceType, param, group } = cellInfo[id];
      if (!cellInfo[id].firstId) cellInfo[id].firstId = firstId;
      if (['DT', 'SER', 'HIS', 'RTS', 'CTS'].includes(dataSourceType) && !['_key_'].includes(param)) {
        if (dataSourceType === 'RTS') {
          paneRelativeParams[firstId].push(dataSource);
        } else if (dataSourceType === 'HIS') {
          paneRelativeParams[firstId].push(`${param}-${group}`);
        } else {
          paneRelativeParams[firstId].push(param);
        }
      }
    });
  });

  _.map(cellInfo, (cell, id) => {
    const { dataSource: fDataSource, tableName: fTableName } = cellInfo[cell.firstId || id];
    const { dataSource, tableName, dataSourceType, param, group } = cell;
    const highFilterRule = getFilterRules(cell.curRow, cell.curCol);
    if (highFilterRule && highFilterRule.length > 0) {
      filterInfo[id] = highFilterRule;
    }
    // 当单元格有过滤条件时不去重, 或和父格来自不同数据源时
    if (highFilterRule && highFilterRule.length > 0 && cell.firstId !== cell.cellId) {
      if (['DT', 'SER', 'HIS', 'RTS', 'CTS'].includes(dataSourceType) && !['_key_'].includes(param)) {
        if (dataSourceType === 'RTS') {
          paneRelativeParams[id] = [dataSource];
        } else if (dataSourceType === 'HIS') {
          paneRelativeParams[id] = [`${param}-${group}`];
        } else {
          paneRelativeParams[id] = [param];
        }
      }
    } else if (fDataSource !== dataSource || fTableName !== tableName) {
      if (['DT', 'SER', 'HIS', 'RTS', 'CTS'].includes(dataSourceType) && !['_key_'].includes(param)) {
        if (dataSourceType === 'RTS') {
          paneRelativeParams[`${id}-${cell.firstId}`] = [dataSource];
        } else if (dataSourceType === 'HIS') {
          paneRelativeParams[`${id}-${cell.firstId}`] = [`${param}-${group}`];
        } else {
          paneRelativeParams[`${id}-${cell.firstId}`] = [param];
        }
      }
    }
  });

  // 获取双父格间的关联关系
  _.map(bothPaneRelative, (item) => {
    const { left, up } = item;
    item.left.params = paneRelativeParams[left.firstId] || [];
    item.up.params = paneRelativeParams[up.firstId] || [];
  });

  // 生成数据
  _.map(paneRelativeParams, (params, cellId) => {
    let firstId = cellId;
    if (cellId.split('-').length === 2) {
      [cellId, firstId] = cellId.split('-');
    }
    const { dataSourceType, param, dataSource, tableName } = cellInfo[cellId];
    const { list, key } = getInitList(dataSourceType, cellInfo, firstId, paneRelative, dataSource, tableName, cellId);
    if (allDataSource[key]) return;
    if (list) {
      if (fillType === 'replace' || param === '_key_' || params.includes('_table_') || !['DT', 'SER'].includes(dataSourceType)) {
        allDataSource[key] = { list };
      } else if (!_.keys(filterInfo).length) {
        const paramKeys = tableName ? getSqlInfoPrimaryKeys(tableName) : [];
        allDataSource[key] = { list: sortBy(list, _.union(params, paramKeys)) };
      } else {
        allDataSource[key] = { list };
      }
    } else {
      allDataSource[key] = { list: [] };
    }
  });
  resetByFilter(cellInfo, subPaneInfo, subPaneUpInfo, paneRelative, filterInfo);
  return { paneRelative, paneRelativeParams, bothPaneRelative };
};

/**
 * 根据过滤条件重置数据源
 * @param {*} cellInfo
 * @param {*} subPaneInfo
 * @param {*} subPaneUpInfo
 * @param {*} paneRelative
 * @param {*} filterInfo
 */
const resetByFilter = (cellInfo, subPaneInfo, subPaneUpInfo, paneRelative, filterInfo) => {
  const paneRelativeFilters = {};
  const parentPaneFilters = {};
  if (!_.keys(filterInfo).length) return;
  const tmpAllDataSouce = _.clone(allDataSource);
  _.forEach(filterInfo, (highFilterRule, id) => {
    const cell = cellInfo[id];
    const { curRow, curCol, direction } = cell;
    const withPaneFilter = getFilterWithPane(curRow, curCol);
    const parentPaneId = direction === 'V' ? _.get(subPaneInfo, `info.${id}.paneId`) : _.get(subPaneUpInfo, `info.${id}.paneId`);
    const { allDataKey, dataKey, newParam } = getDataKeyParam(cell);
    if (!cell.firstId) return;
    const tmpList = [];
    const paneFilters = [];
    parentPaneFilters[id] = [];
    if (tmpAllDataSouce[allDataKey]) {
      _.forEach(allDataSource[allDataKey].list, (obj, idx) => {
        const isFilter = calculateFilterRule(highFilterRule, dataKey, idx, cell);
        parentPaneFilters[id][idx] = isFilter;
        // 继承父格条件
        if (_.get(parentPaneFilters, `[${parentPaneId}]`, true) && withPaneFilter) {
          parentPaneFilters[id][idx] = _.get(parentPaneFilters, `[${parentPaneId}][${idx}]`, true) && isFilter;
        }
        if (cell.cellId === cell.firstId) {
          if (parentPaneFilters[id][idx]) tmpList.push(obj);
          paneFilters[idx] = parentPaneFilters[id][idx];
        } else if (paneRelative[cell.cellId]) {
          if (!parentPaneFilters[id][idx]) {
            _.set(obj, `${newParam}`, '');
            resetPaneCellData(tmpAllDataSouce, id, subPaneInfo, subPaneUpInfo, cellInfo, idx, paneRelativeFilters);
          }
          tmpList.push(obj);
        } else {
          if (!parentPaneFilters[id][idx]) {
            _.set(obj, `${newParam}`, '');
            resetPaneCellData(tmpAllDataSouce, id, subPaneInfo, subPaneUpInfo, cellInfo, idx, paneRelativeFilters);
          }
          if ((paneRelativeFilters[cell.firstId] && paneRelativeFilters[cell.firstId][idx]) || !paneRelativeFilters[cell.firstId]) tmpList.push(obj);
        }
      });
      tmpAllDataSouce[allDataKey].list = tmpList;
    }
    paneRelativeFilters[id] = paneFilters;
  });
  allDataSource = tmpAllDataSouce;
};

const getDataKeyParam = (cell) => {
  let allDataKey = '';
  let dataKey = '';
  let newParam;
  const { dataSourceType, dataSource, param, displaySelect, group, tableName, cellId } = cell;
  if (['RTS', 'SER', 'CTS'].includes(dataSourceType)) {
    allDataKey = `${cellId}-${dataSource}`;
    dataKey = dataSource;
    newParam = param;
  } else if (['HIS'].includes(dataSourceType)) {
    allDataKey = `${cellId}-${dataSource}`;
    dataKey = dataSource;
    if (displaySelect === 'time') {
      newParam = 'time';
    } else {
      newParam = `${param}-${group}`;
    }
  } else if (['DT'].includes(dataSourceType)) {
    allDataKey = `${cellId}-${tableName}`;
    dataKey = tableName;
    newParam = param;
  }
  return { allDataKey, dataKey, newParam };
};

// 获取父格数据筛选过的下标
const getSubPaneTransIdx = (paneRelativeFilters, cellId, dataIdx) => {
  if (!_.get(paneRelativeFilters, `${cellId}`)) return dataIdx;
  let [tmpIdx, finalIdx] = [0, 0];
  _.map(paneRelativeFilters[cellId], (item, idx) => {
    if (item) {
      if (dataIdx === idx) finalIdx = tmpIdx;
      tmpIdx += 1;
    }
  });
  return finalIdx;
};

/**
 * 如单元格有过滤条件并被过滤掉， 需重置之后单元格的值
 * @param {*} tmpAllDataSouce
 * @param {*} parentId
 * @param {*} subPaneInfo
 * @param {*} subPaneUpInfo
 * @param {*} cellInfo
 * @param {*} dataIdx
 */
const resetPaneCellData = (tmpAllDataSouce, parentId, subPaneInfo, subPaneUpInfo, cellInfo, dataIdx, paneRelativeFilters) => {
  const subPaneIds = subPaneInfo.map[parentId] || [];
  const subPaneUpIds = subPaneUpInfo.map[parentId] || [];
  _.map(subPaneIds, (cellId) => {
    if (!cellInfo[cellId]) return;
    const { firstId, curRow, curCol } = cellInfo[cellId];
    const highFilterRule = getFilterRules(curRow, curCol);
    if (highFilterRule && highFilterRule.length > 0) return;
    const { allDataKey, newParam } = getDataKeyParam(cellInfo[cellId]);
    if (tmpAllDataSouce[allDataKey]) {
      _.set(tmpAllDataSouce[allDataKey], `list[${dataIdx}].${newParam}`, '');
    } else {
      if (!cellInfo[firstId]) return;
      const { allDataKey: fAllDataKey } = getDataKeyParam(cellInfo[firstId]);
      const firstIdx = getSubPaneTransIdx(paneRelativeFilters, firstId, dataIdx);
      _.set(tmpAllDataSouce[fAllDataKey], `list[${firstIdx}].${newParam}`, '');
    }
    resetPaneCellData(tmpAllDataSouce, cellId, subPaneInfo, subPaneUpInfo, cellInfo, dataIdx);
  });
  _.map(subPaneUpIds, (cellId) => {
    if (!cellInfo[cellId]) return;
    const { firstId, curRow, curCol } = cellInfo[cellId];
    const highFilterRule = getFilterRules(curRow, curCol);
    if (highFilterRule && highFilterRule.length > 0) return;
    const { allDataKey, newParam } = getDataKeyParam(cellInfo[cellId]);
    if (tmpAllDataSouce[allDataKey]) {
      _.set(tmpAllDataSouce[allDataKey], `list[${dataIdx}].${newParam}`, '');
    } else {
      if (!cellInfo[firstId]) return;
      const { allDataKey: fAllDataKey } = getDataKeyParam(cellInfo[firstId]);
      const firstIdx = getSubPaneTransIdx(paneRelativeFilters, firstId, dataIdx);
      _.set(tmpAllDataSouce[fAllDataKey], `list[${firstIdx}].${newParam}`, '');
    }
    resetPaneCellData(tmpAllDataSouce, cellId, subPaneInfo, subPaneUpInfo, cellInfo, dataIdx);
  });
};

const getFilterRules = (row, col) => {
  return sheet.getTag(row, col) && sheet.getTag(row, col).highFilter && sheet.getTag(row, col).highFilter.list ? sheet.getTag(row, col).highFilter.list : [];
};

const getFilterWithPane = (row, col) => {
  return sheet.getTag(row, col) && sheet.getTag(row, col).highFilter && sheet.getTag(row, col).highFilter.paneFilter !== undefined ? sheet.getTag(row, col).highFilter.paneFilter : true;
};

const getFilterDataObj = (dataSourceType, dataKey) => {
  let list = [];
  switch (dataSourceType) {
    case 'HIS':
    case 'CTS':
    case 'ENT':
    case 'SER': {
      list = _.get(_.get(originalDataSource, `${dataKey}`), 'list', []);
      break;
    }
    case 'DT': {
      list = _.get(_.get(originalDataSource, `${dataKey}`), 'data.dataSource', []);
      break;
    }
    case 'RTS': {
      list = _.get(_.get(originalDataSource, `${dataKey}`), 'list', []);
      break;
    }
    default: {
      list = [];
      break;
    }
  }
  list = _.clone(list);
  return list;
};

const getDataSourceInstance = (value, dataType = 'HIS') => {
  let dataSource = '';
  let dataInstan = '';
  let list = [];
  const [, dataS, instan] = value.match(/(.+)\.(.+)$/);
  dataInstan = instan;
  dataSource = dataS;
  if (/服务$/.test(dataSource)) {
    const [, service] = dataSource.match(/(.*)服务$/);
    dataSource = service;
  }
  if (originalDataSource[dataSource]) {
    if (_.get(_.get(originalDataSource, `${dataSource}`), 'data.dataSource')) dataType = 'DT';
    list = getFilterDataObj(dataType, dataSource);
  }
  return { instance: dataInstan, list };
};

const getFilterDataVal = (data, optional, cell) => {
  const { dataSourceType, param, displaySelect } = cell;
  switch (dataSourceType) {
    case 'HIS': {
      if (displaySelect === 'time') {
        return _.get(data, 'time', '');
      } else {
        return _.get(data, `${param}`, '');
      }
    }
    case 'RTS':
    case 'DT':
    case 'CTS':
    case 'SER': return _.get(data, `${optional}`, '');
    default:
      break;
  }
};

/**
 * @description 计算总条件判断是否过滤
 * @param {*} rules
 * @param {*} data
 * @param {*} cell
 */
const calculateFilterRule = (rules, dataKey, dataIdx, cell) => {
  let totalCondition = true;
  // if (!data) return false;

  _.forEach(rules, (rule, idx) => {
    let conditionVal = true;
    const optional = _.get(rule, 'data.optionalCol.val.name');
    const operator = _.get(rule, 'data.operator.key');
    const valType = _.get(rule, 'data.valType.key');
    const value = _.get(rule, `data.valType[${valType}]`);
    if (cell.dataSourceType === 'HIS') {
      const [dataSource, instance, param, group] = cell.dataSource.split(':');
      if (instance !== optional) dataKey = `${dataSource}.${optional}.${param}.${group}`;
    }
    const data = _.get(getFilterDataObj(cell.dataSourceType, dataKey), `[${dataIdx}]`, {});
    const optionalVal = getFilterDataVal(data, optional, cell);
    if (rule.children && rule.children.length > 0) {
      conditionVal = calculateFilterRule(rule.children, dataKey, dataIdx, cell);
      totalCondition = getConditionBase({ operator: _.get(rule, 'data.andOr', 'and'), con1: totalCondition, con2: conditionVal });
    } else {
      const currentCondi = getConditionBase({ operator, value, optionalVal, valType, cell });
      if (idx === 0) {
        totalCondition = currentCondi;
      } else {
        totalCondition = getConditionBase({ operator: _.get(rule, 'data.andOr', 'and'), con1: totalCondition, con2: currentCondi });
      }
    }
  });
  return totalCondition;
};

/**
 * @description 根据比较条件计算比较结果
 * @param {*} param0
 */
const getConditionBase = ({ operator, value = '', con1, con2, optionalVal = '', valType, cell }) => {
  let isFilter = false;
  const dateFormat = ['YYYY-MM-DD HH:mm:ss', 'YYYY/MM/DD HH:mm:ss', 'YYYY-MM-DD', 'YYYY/MM/DD', 'YYYY MM DD', 'YYYY-MM', 'MM-DD', 'YYYY/MM', 'YYYY MM', 'MM/DD', 'MM DD'];
  let dataList = [];
  let dataInstan = '';
  let isCustom = false;
  let formatterType = '';
  if (valType === 'Col') {
    const col = Util.ConvertNum(value.split('|')[0]);
    const row = value.split('|')[1] - 1;
    value = sheet.getCell(row, col).value() || '';
    const formula = sheet.getFormula(row, col);
    formatterType = _.get(sheet.getTag(row, col), 'numberFormat.numberType', '');
    if (formula) {
      const [, dataType, params] = (formula && formula.match(/(RT|HIS|DT|SER|RTS|CTS)\((.*)\)$/)) || [];
      if (dataType && params) {
        // 单元格为自定义公式时单独处理
        isCustom = true;
        const [selectedObject] = (params && params.replace(/"/g, '').split(',')) || [];
        const { instance, list } = getDataSourceInstance(selectedObject, dataType);
        dataInstan = instance;
        dataList = list;
      }
      // TODO 当单元格为基础公式的处理
    }
  } else if (valType === 'Data') {
    const { instance, list } = getDataSourceInstance(value);
    dataInstan = instance;
    dataList = list;
  }
  if (['Int', 'Float', 'Date'].includes(valType) && !['equal', 'unequal', 'greater', 'greaterOrEqual', 'less', 'lessOrEqual'].includes(operator)) return false;
  if (['Boolean'].includes(valType) && !['equal', 'unequal'].includes(operator)) return false;
  if (cell && cell.cellId) {
    const sCol = Util.ConvertNum(cell.cellId.match(/^[a-z|A-Z]+/gi)[0]);
    const sRow = cell.cellId.match(/\d+$/gi)[0] - 1;
    const sFormatterType = _.get(sheet.getTag(sRow, sCol), 'numberFormat.numberType', '');
    optionalVal = changeTypeOfFormatter(optionalVal, sFormatterType);
  }
  if (['begin', 'noBegin', 'end', 'noEnd', 'include', 'noInclude', 'contained', 'noContained'].includes(operator) && (typeof optionalVal !== 'string')) return false;

  switch (operator) {
    case 'equal': {
      isFilter = optionalVal === value;
      if (valType === 'Date') {
        isFilter = moment(optionalVal).isSame(value);
      } else if (valType === 'Col') {
        if (moment(optionalVal, dateFormat, true).isValid()) isFilter = moment(optionalVal).isSame(value);
        if (isCustom) {
          isFilter = _.find(dataList, (item) => {
            const finalVal = changeTypeOfFormatter(item[`${dataInstan}`], formatterType);
            return finalVal === optionalVal;
          });
        }
      } else if (valType === 'Data') {
        isFilter = _.find(dataList, (item) => { return item[`${dataInstan}`] === optionalVal; });
      }
      break;
    }
    case 'unequal': {
      isFilter = optionalVal !== value;
      if (valType === 'Date') {
        isFilter = !moment(optionalVal).isSame(value);
      } else if (valType === 'Col') {
        if (moment(optionalVal, dateFormat, true).isValid()) isFilter = !moment(optionalVal).isSame(value);
        if (isCustom) {
          isFilter = _.find(dataList, (item) => {
            const finalVal = changeTypeOfFormatter(item[`${dataInstan}`], formatterType);
            return finalVal !== optionalVal;
          });
        }
      } else if (valType === 'Data') {
        isFilter = _.find(dataList, (item) => { return item[`${dataInstan}`] !== optionalVal; });
      }
      break;
    }
    case 'greater': {
      isFilter = optionalVal > value;
      if (valType === 'Date') {
        isFilter = moment(optionalVal).isAfter(value);
      } else if (valType === 'Col') {
        if (moment(optionalVal, dateFormat, true).isValid()) isFilter = moment(optionalVal).isAfter(value);
        if (isCustom) {
          isFilter = _.find(dataList, (item) => {
            const finalVal = changeTypeOfFormatter(item[`${dataInstan}`], formatterType);
            return optionalVal > finalVal;
          });
        }
      } else if (valType === 'Data') {
        isFilter = _.find(dataList, (item) => { return optionalVal > item[`${dataInstan}`]; });
      }
      break;
    }
    case 'greaterOrEqual': {
      isFilter = optionalVal >= value;
      if (valType === 'Date') {
        isFilter = moment(optionalVal).isAfter(value) || moment(optionalVal).isSame(value);
      } else if (valType === 'Col') {
        if (moment(optionalVal, dateFormat, true).isValid()) isFilter = moment(optionalVal).isAfter(value) || moment(optionalVal).isSame(value);
        if (isCustom) {
          isFilter = _.find(dataList, (item) => {
            const finalVal = changeTypeOfFormatter(item[`${dataInstan}`], formatterType);
            return optionalVal >= finalVal;
          });
        }
      } else if (valType === 'Data') {
        isFilter = _.find(dataList, (item) => { return optionalVal >= item[`${dataInstan}`]; });
      }
      break;
    }
    case 'less': {
      isFilter = optionalVal < value;
      if (valType === 'Date') {
        isFilter = moment(optionalVal).isBefore(value);
      } else if (valType === 'Col') {
        if (moment(optionalVal, dateFormat, true).isValid()) isFilter = moment(optionalVal).isBefore(value);
        if (isCustom) {
          isFilter = _.find(dataList, (item) => {
            const finalVal = formatterType === intl.formatMessage(messages.Values) ? parseFloat(item[`${dataInstan}`]) : item[`${dataInstan}`];
            return optionalVal < finalVal;
          });
        }
      } else if (valType === 'Data') {
        isFilter = _.find(dataList, (item) => { return optionalVal < item[`${dataInstan}`]; });
      }
      break;
    }
    case 'lessOrEqual': {
      isFilter = optionalVal <= value;
      if (valType === 'Date') {
        isFilter = moment(optionalVal).isBefore(value) || moment(optionalVal).isSame(value);
      } else if (valType === 'Col') {
        if (moment(optionalVal, dateFormat, true).isValid()) isFilter = moment(optionalVal).isBefore(value) || moment(optionalVal).isSame(value);
        if (isCustom) {
          isFilter = _.find(dataList, (item) => {
            const finalVal = changeTypeOfFormatter(item[`${dataInstan}`], formatterType);
            return optionalVal <= finalVal;
          });
        }
      } else if (valType === 'Data') {
        isFilter = _.find(dataList, (item) => { return optionalVal <= item[`${dataInstan}`]; });
      }
      break;
    }
    case 'begin': {
      isFilter = optionalVal.match(new RegExp(`^${value}.*$`));
      if (valType === 'Col') {
        if (isCustom) {
          isFilter = _.find(dataList, (item) => {
            const finalVal = changeTypeOfFormatter(item[`${dataInstan}`], formatterType);
            return optionalVal.match(new RegExp(`^${finalVal}.*$`));
          });
        }
      } else if (valType === 'Data') {
        isFilter = _.find(dataList, (item) => { return optionalVal.match(new RegExp(`^${item[`${dataInstan}`]}.*$`)); });
      }
      break;
    }
    case 'noBegin': {
      isFilter = !optionalVal.match(new RegExp(`^${value}.*$`));
      if (valType === 'Col') {
        if (isCustom) {
          isFilter = _.find(dataList, (item) => {
            const finalVal = changeTypeOfFormatter(item[`${dataInstan}`], formatterType);
            return !optionalVal.match(new RegExp(`^${finalVal}.*$`));
          });
        }
      } else if (valType === 'Data') {
        isFilter = _.find(dataList, (item) => { return !optionalVal.match(new RegExp(`^${item[`${dataInstan}`]}.*$`)); });
      }
      break;
    }
    case 'end': {
      isFilter = optionalVal.match(new RegExp(`${value}$`));
      if (valType === 'Col') {
        if (isCustom) {
          isFilter = _.find(dataList, (item) => {
            const finalVal = changeTypeOfFormatter(item[`${dataInstan}`], formatterType);
            return optionalVal.match(new RegExp(`${finalVal}$`));
          });
        }
      } else if (valType === 'Data') {
        isFilter = _.find(dataList, (item) => { return optionalVal.match(new RegExp(`${item[`${dataInstan}`]}$`)); });
      }
      break;
    }
    case 'noEnd': {
      isFilter = !optionalVal.match(new RegExp(`${value}$`));
      if (valType === 'Col') {
        if (isCustom) {
          isFilter = _.find(dataList, (item) => {
            const finalVal = changeTypeOfFormatter(item[`${dataInstan}`], formatterType);
            return !optionalVal.match(new RegExp(`${finalVal}$`));
          });
        }
      } else if (valType === 'Data') {
        isFilter = _.find(dataList, (item) => { return !optionalVal.match(new RegExp(`${item[`${dataInstan}`]}$`)); });
      }
      break;
    }
    case 'include': {
      isFilter = optionalVal.indexOf(value) !== -1;
      if (valType === 'Col') {
        if (isCustom) {
          isFilter = _.find(dataList, (item) => {
            const finalVal = changeTypeOfFormatter(item[`${dataInstan}`], formatterType);
            return optionalVal.indexOf(finalVal) !== -1;
          });
        }
      } else if (valType === 'Data') {
        isFilter = _.find(dataList, (item) => { return optionalVal.indexOf(item[`${dataInstan}`]) !== -1; });
      }
      break;
    }
    case 'noInclude': {
      isFilter = optionalVal.indexOf(value) === -1;
      if (valType === 'Col') {
        if (isCustom) {
          isFilter = _.find(dataList, (item) => {
            const finalVal = changeTypeOfFormatter(item[`${dataInstan}`], formatterType);
            return optionalVal.indexOf(finalVal) === -1;
          });
        }
      } else if (valType === 'Data') {
        isFilter = _.find(dataList, (item) => { return optionalVal.indexOf(item[`${dataInstan}`]) === -1; });
      }
      break;
    }
    case 'contained': {
      isFilter = typeof value === 'string' && value.indexOf(optionalVal) !== -1;
      if (valType === 'Col') {
        if (isCustom) {
          isFilter = _.find(dataList, (item) => {
            const finalVal = changeTypeOfFormatter(item[`${dataInstan}`], formatterType);
            return typeof finalVal === 'string' && finalVal.indexOf(optionalVal) !== -1;
          });
        }
      } else if (valType === 'Data') {
        isFilter = _.find(dataList, (item) => { return typeof item[`${dataInstan}`] === 'string' && item[`${dataInstan}`].indexOf(optionalVal) !== -1; });
      }
      break;
    }
    case 'noContained': {
      isFilter = typeof value === 'string' && value.indexOf(optionalVal) === -1;
      if (valType === 'Col') {
        if (isCustom) {
          isFilter = _.find(dataList, (item) => {
            const finalVal = changeTypeOfFormatter(item[`${dataInstan}`], formatterType);
            return typeof finalVal === 'string' && finalVal.indexOf(optionalVal) === -1;
          });
        }
      } else if (valType === 'Data') {
        isFilter = _.find(dataList, (item) => { return typeof item[`${dataInstan}`] === 'string' && item[`${dataInstan}`].indexOf(optionalVal) === -1; });
      }
      break;
    }
    case 'and': {
      isFilter = con1 && con2;
      break;
    }
    case 'or': {
      isFilter = con1 || con2;
      break;
    }
    default:
      break;
  }
  return isFilter || false;
};

const sortBy = (list, params) => {
  const map = new Map();
  _.map(list, (item) => {
    const keyArr = [];
    _.map(params, (param) => {
      keyArr.push(item[param]);
    });
    map.set(keyArr.join('-'), _.pick(item, params));
  });
  return [...map.values()];
};

const changeTypeOfFormatter = (value, type) => {
  if (type === intl.formatMessage(messages.Values)) {
    value = parseFloat(value);
  } else if (type === intl.formatMessage(messages.Text)) {
    value = `${value}`;
  }
  return value;
};

const switchOrientation = (data, showHeader) => {
  const newData = {};
  _.map(data, (obj, rowNum) => {
    _.map(obj, (v, k) => {
      if (!newData[k]) newData[k] = {};
      newData[k][rowNum + 1] = v;
    });
  });
  if (showHeader) {
    _.map(_.keys(newData), (key) => {
      newData[key][0] = key;
    });
  }
  return { list: _.values(newData), pageSize: _.values(newData).length };
};

const fetchAllDataSource = (data, keys) => {
  const { historyInfo, serviceInfo, dataTableInfo, statisticInfo, customServices, entityObjects } = data;
  const tmp = {};
  const tempKeys = _.map(keys, (key) => { return key.replace(/\./g, ':'); });
  _.map(_.merge({}, historyInfo, serviceInfo, customServices), (item, key) => {
    if (tmp[key]) return;
    const list = _.get(item, 'list', []);
    const idx = _.indexOf(tempKeys, key);
    if (~idx) {
      item.pageSize = list.length;
      tmp[keys[idx]] = item;
    }
  });
  _.map(statisticInfo, (item, key) => {
    if (tmp[key]) return;
    const list = _.get(item, 'list', []);
    if (keys.includes(key)) {
      item.pageSize = list.length;
      tmp[key] = item;
    }
  });
  if (_.keys(dataTableInfo).length) {
    _.map(dataTableInfo, (item, key) => {
      if (tmp[key]) return;
      const dataSource = _.get(item, 'data.dataSource', []);
      if (keys.includes(key)) {
        item.pageSize = dataSource.length;
        tmp[key] = item;
      }
    });
  }
  if (_.keys(entityObjects).length) {
    _.map(entityObjects, (item, key) => {
      if (tmp[key]) return;
      tmp[key] = item;
    });
  }
  originalDataSource = _.merge({}, tmp);
  let hasfetchAllData = true;
  _.map(keys, (item) => {
    if (!originalDataSource[item]) hasfetchAllData = false;
  });
  return hasfetchAllData;
};

const sortKey = (keys) => {
  return _.orderBy(_.map(keys, k => Number(k)));
};

const getFillData = ({ rowColMap, lengthMap, info, type = 'row' }) => {
  const map = rowColMap[`${type}Map`];
  const result = {};
  const mapKeys = _.keys(map);
  if (!mapKeys.length) return;

  // 公式按从下往上，从右往左遍历
  _.map(sortKey(mapKeys).reverse(), (index) => {
    const { newData, length, paneSize, paneFloorSize, spanMap } = generateNewData({ index, data: map[index], ...info });
    if (length > 0) {
      lengthMap[type][index] = length;
      result[index] = {
        data: newData,
        index,
        newIndex: Number(index) + paneFloorSize + 1,
        length,
        paneSize,
        spanMap
      };
    }
  });
  return result;
};

const insertData = ({ fillDataMap, rowColMap, lengthMap, info }) => {
  insertRowsCols(fillDataMap.row, lengthMap, info.cellInfo, 'row');
  insertRowsCols(fillDataMap.col, lengthMap, info.cellInfo, 'col');

  setFillData(fillDataMap.row, lengthMap, rowColMap.rowMap, info, 'row');
  setFillData(fillDataMap.col, lengthMap, rowColMap.colMap, info, 'col');

  // 重置关联了公式的函数的参数
  rowFormulaSetting({ lengthMap, info });
  colFormulaSetting({ lengthMap, info });
};

const insertRowsCols = (fillDataMap, lengthMap, cellInfo, type) => {
  _.map(fillDataMap, (item, index) => {
    const { data, newIndex, length } = item;
    if (type === 'row') {
      insertRows({ data, index, newIndex, length, lengthMap, cellInfo });
    } else {
      insertCols({ index, newIndex, length, lengthMap });
    }
  });
};

const setFillData = (fillDataMap, lengthMap, map, info, type) => {
  const { cellInfo, subPaneInfo } = info;
  _.map(fillDataMap, (item, index) => {
    const { data, newIndex, length, paneSize, spanMap } = item;
    if (type === 'row') {
      const { rowDiff } = setRowsData({ data, index, newIndex, length, lengthMap, info });
      setRowAuthority({ rowIndex: Number(index) + rowDiff, data, length, lengthMap, info });
    } else {
      const { colDiff } = setColsData({ data, index, newIndex, length, lengthMap, info });
      setColAuthority({ colIndex: Number(index) + colDiff, data, length, lengthMap, info });
    }
    if (!paneSize) mergeCells(data, type, lengthMap, info, spanMap);
    brushPaneStyle({ cellInfo, subPane: subPaneInfo, dataLength: map[index], lengthMap });
    brushOtherStyle({ cellInfo, subPane: subPaneInfo, dataLength: map[index], lengthMap, index, type });
  });
};

const fillDataByInsert = (info) => {
  const rowColMap = calculateRowColLength(info);
  const lengthMap = { row: {}, col: {} };
  const fillDataMap = { row: {}, col: {} };

  fillDataMap.row = getFillData({ rowColMap, lengthMap, info });
  fillDataMap.col = getFillData({ rowColMap, lengthMap, info, type: 'col' });
  insertData({ fillDataMap, rowColMap, lengthMap, info });

  addTable(rowColMap, info, lengthMap);
  // 报表联动
  setRecords(fillDataMap, lengthMap, info);
  changeChartDataRange(lengthMap);
  return lengthMap;
};

const rtData = (report, rtCellInfo, lengthMap) => {
  if (!report.rtData) report.rtData = {};
  _.map(rtCellInfo, (cell) => {
    calculateRtDataPosition(report.rtData, cell, lengthMap);
  });
};

const setRecords = (fillDataMap, lengthMap, info) => {
  const { cellInfo, subPaneInfo: subPane } = info;
  _.map(fillDataMap, (rowColMap) => {
    _.map(rowColMap, (data) => {
      _.map(data.data, (rowItem, rowIndex) => {
        _.map(rowItem, (v, k) => {
          const subPaneInfo = _.get(subPane, `info.${k}`, null);
          const newInfo = cellInfo[k] || subPaneInfo;
          if (newInfo) {
            const { sheetName, curRow, curCol, firstId, tableName, dataSource } = newInfo;
            const key = `${firstId}-${tableName || dataSource}`;
            const rowDiff = getDiff(curRow, lengthMap.row);
            const colDiff = getDiff(curCol, lengthMap.col);
            const { cellPos } = getCurPos({ index: rowIndex, cellInfo: newInfo, rowDiff, colDiff });
            dataInfo[sheetName][cellPos] = _.get(allDataSource[key], `list[${rowIndex}]`, null);
          }
        });
      });
    });
  });
};

const getTableNextLineData = (row, col, list) => {
  const tmpData = [];
  if (list.length) {
    for (let c = 0; c < _.keys(list[0]).length; c += 1) {
      const cell = sheet.getCell(row + list.length, col + c);
      tmpData.push(cell.value());
    }
  }
  return tmpData;
};

const setTableNextLineData = (row, col, tmpData) => {
  _.map(tmpData, (value, index) => sheet.getCell(row, col + index).value(value));
};

const addTable = (rowColMap, { cellInfo }, lengthMap) => {
  _.map(rowColMap.rowMap, (map) => {
    _.map(map, (r, id) => {
      const { param, dataSource, tableName, curCol, curRow, theme, showHeader, firstId, direction, sheetName } = cellInfo[id];
      if (param === '_table_') {
        const key = `${firstId}-${tableName || dataSource}`;
        const list = _.get(allDataSource[key], 'list', []);
        if (list.length) {
          cellInfo[id].list = list;
          const rowDiff = getDiff(curRow, lengthMap.row);
          const colDiff = getDiff(curCol, lengthMap.col);
          const [row, col] = [curRow + rowDiff, curCol + colDiff];
          const tableTheme = theme !== 'null' ? spreadNS.Tables.TableThemes[theme] : new spreadNS.Tables.TableTheme();
          // const tmpData = getTableNextLineData(row, col, list);
          const newList = [];
          // 联动：存储内容
          _.map(list, (rowItem, index) => {
            _.map(_.keys(rowItem), (k, i) => {
              const [newRow, newCol] = direction === 'V' ? [curRow, curCol + i] : [curRow + i, curCol];
              const { cellPos } = getCurPos({ index, cellInfo: { sheetName, direction, curRow: newRow, curCol: newCol }, rowDiff, colDiff });
              dataInfo[sheetName][cellPos] = rowItem;
            });
          });
          if (showHeader && direction === 'V') newList.push(_.keys(list[0]));
          _.map(list, (rowItem) => {
            const tempArr = [];
            if (showHeader && direction === 'H') tempArr.push();
            _.map(_.keys(list[0]), (k) => {
              tempArr.push(rowItem[k]);
            });
            newList.push(tempArr);
          });
          try {
            // addFromDataSource配置showHeader false仍需先渲染预留header行， 改为add setArray
            const table = sheet.tables.add(
              `${dataSource}${Math.round(Math.random() * 10000)}`,
              row,
              col,
              newList.length,
              newList[0].length,
              tableTheme
            );
            table.showHeader(false);
            table.filterButtonVisible(false);
            table.cj -= 1;
            table.Xj += 1;
            sheet.setArray(row, col, newList);
            // if (!showHeader || direction === 'H') {
            //   sheet.getCell(row + list.length, col).value(tmpData[0]);
            // }
          } catch (e) {
            console.log(e);
          }
        }
        brushPaneStyle({ cellInfo, dataLength: { [id]: list.length }, lengthMap });
      }
    });
  });
};

const changeChartDataRange = (lengthMap) => {
  const { charts } = sheet;
  if (!chartDataRange[sheet.name()]) return;
  _.map(charts.all(), (chart) => {
    const chartInfo = chartDataRange[sheet.name()][chart.name()];
    if (!chartInfo) return;
    const { sRow, eRow, sCol, eCol } = chartInfo;
    const { row, col } = lengthMap;
    let [sRowDiff, sColDiff] = [0, 0];
    let [rowDiff, colDiff] = [0, 0];
    if (_.keys(row).length) {
      _.map(_.keys(row), (r) => {
        if (Number(r) < sRow) sRowDiff += row[r];
      });
      for (let r = sRow; r <= eRow; r += 1) {
        if (_.keys(row).includes(`${r}`)) rowDiff += row[`${r}`];
      }
    }
    if (_.keys(col).length) {
      _.map(_.keys(col), (c) => {
        if (Number(c) < sCol) sColDiff += col[c];
      });
      for (let c = sCol; c <= eCol; c += 1) {
        if (_.keys(col).includes(`${c}`)) colDiff += col[`${c}`];
      }
    }
    const startCell = Util.getCellPositionString(sRow + sRowDiff + 1, sCol + sColDiff + 1);
    const endCell = Util.getCellPositionString(eRow + sRowDiff + rowDiff + 1, eCol + sColDiff + colDiff + 1);
    chart.dataRange(`${startCell}:${endCell}`);
  });
};

const mergeCells = (data, type, lengthMap, info, spanMap) => {
  const mergeMap = getMergeMap(data, info, spanMap);
  _.map(mergeMap, (mergeInfo, k) => {
    if (_.keys(mergeInfo).length) {
      const { row, col } = Util.getCellPositionFromString(k);
      _.map(mergeInfo, (to, from) => {
        const count = (to - Number(from)) + 1;
        const rowDiff = getDiff(row, lengthMap.row);
        const colDiff = getDiff(col, lengthMap.col);
        if (type === 'row') {
          merge(row + rowDiff + Number(from), col + colDiff, count, 1);
        } else if (type === 'col') {
          merge(row + rowDiff, col + colDiff + Number(from), 1, count);
        }
      });
    }
  });
};

const merge = (row, col, rowCount, colCount) => {
  const value = sheet.getCell(row, col).value();
  // 合并单元格样式覆盖
  const style = sheet.getStyle(row, col);
  for (let r = row; r < row + rowCount; r += 1) {
    for (let c = col; c < col + colCount; c += 1) {
      sheet.setStyle(r, c, style, spreadNS.SheetArea.viewport);
    }
  }
  sheet.addSpan(row, col, rowCount, colCount);
  sheet.clear(row, col, rowCount, colCount, spreadNS.SheetArea.viewport, spreadNS.StorageType.data);
  if (value !== undefined) sheet.getCell(row, col).value(value);
};

const getNewMergeInfo = (cellInfo, subPaneInfo) => {
  const newMergeInfo = {};
  const { info, map } = _.merge({}, subPaneInfo);
  const firstIdArray = _.keys(map);
  _.map(firstIdArray, (firstId) => {
    const subIds = _.uniq(_.concat(..._.values(map[firstId])));
    newMergeInfo[firstId] = _.merge({}, cellInfo[firstId]);
    _.map(subIds, (subId) => {
      newMergeInfo[subId] = _.merge({}, info[subId]);
    });
  });
  return newMergeInfo;
};

const getLengthMapForMerge = (data) => {
  const lengthMap = {};
  _.map(data, (row) => {
    _.map(row, (value, cellId) => {
      if (!lengthMap[cellId]) lengthMap[cellId] = 0;
      lengthMap[cellId] += 1;
    });
  });
  return lengthMap;
};

const getMergeMap = (data, { cellInfo, subPaneInfo }, spanMap) => {
  const newMergeInfo = getNewMergeInfo(cellInfo, subPaneInfo);
  const mergeMap = {};
  const keyValue = {};
  const lengthMap = getLengthMapForMerge(data);
  _.map(data, (row, rowIndex) => {
    _.map(row, (v, k) => {
      const firstId = _.get(cellInfo[k], 'firstId');
      if (spanMap[k] > 1 || (firstId && spanMap[firstId] > 1) || !newMergeInfo[k]) return;
      const { dataSourceType, param, paneId } = newMergeInfo[k];
      const fromIndex = _.max(_.map(_.keys(mergeMap[k]), key => Number(key))) || 0;
      if (!['DT', 'SER'].includes(dataSourceType) || ['_table_'].includes(param)) return;
      if (keyValue[k] === undefined) {
        // 第一行
        keyValue[k] = v;
        mergeMap[k] = { 0: 0 };
      } else if (keyValue[k] !== v) {
        // 子窗格内容不同,记录位置
        mergeMap[k][fromIndex] = rowIndex - 1;
        mergeMap[k][rowIndex] = rowIndex;
      } else if (paneId && mergeMap[paneId] && mergeMap[paneId][rowIndex]) {
        // 子窗格内容相同, 父窗格内容不同，子窗格不合并，记录位置
        mergeMap[k][fromIndex] = rowIndex - 1;
        mergeMap[k][rowIndex] = rowIndex;
      }
      if (rowIndex === lengthMap[k] - 1 && keyValue[k] === v && (!paneId || !(_.get(mergeMap[paneId], rowIndex)))) {
        // 最后一行， 内容相同时记录 (无父格或者父格已合并)
        mergeMap[k][fromIndex] = rowIndex;
      }
      keyValue[k] = v;
    });
  });

  _.map(mergeMap, (v, k) => {
    const { canMerge } = newMergeInfo[k] || {};
    if (!canMerge || !_.keys(v).length) {
      delete mergeMap[k];
    } else {
      _.map(v, (to, from) => {
        if (to === Number(from)) delete mergeMap[k][from];
      });
    }
  });

  return mergeMap;
};

const calculateRtDataPosition = (data, cell, lengthMap) => {
  const { curCol, curRow } = cell;
  const id = Util.getCellPositionString(curRow + 1, curCol + 1);
  const newCell = _.cloneDeep(cell);

  newCell.curRow += getDiff(curRow, lengthMap.row);
  newCell.curCol += getDiff(curCol, lengthMap.col);
  // 运行期 删除实时公式
  sheet.getCell(newCell.curRow, newCell.curCol).formula(null);

  data[id] = newCell;
};

const insertCheck = (data, cellInfo, length) => {
  let count = 0;
  _.map(data[0], (v, k) => {
    if (cellInfo[k]) {
      const { param, showHeader, dataSource, firstId, tableName, direction } = cellInfo[k];
      const key = `${firstId}-${tableName || dataSource}`;
      const tableList = _.get(allDataSource[key], 'list', []);
      if (param === '_table_' && showHeader && direction === 'V' && tableList.length >= length) count += 1;
    }
  });
  return count ? length + 1 : length;
};

const getAuthority = (pos) => {
  if (sheet && pos) {
    return _.get(authorityObj, `${sheet.name()}.${pos}`);
  }
  return 2;
};

const getDiff = (cur, map, first) => {
  let diff = 0;
  if (first !== undefined) {
    _.map(map, (len, num) => {
      if (Number(num) !== first && Number(num) < cur && len) diff += len;
    });
  } else {
    _.map(map, (len, num) => {
      if (Number(num) < cur && len) diff += len;
    });
  }

  return diff;
};

const insertCols = ({ index, newIndex, length, lengthMap }) => {
  const colDiff = getDiff(index, lengthMap.col);
  const col = newIndex + colDiff;
  sheet.addColumns(col, length);
};

const insertRows = ({ data, index, newIndex, length, lengthMap, cellInfo }) => {
  const rowDiff = getDiff(index, lengthMap.row);
  const row = newIndex + rowDiff;

  // 重新计算行数（_table_）
  const newLength = insertCheck(data, cellInfo, length);
  // 更新行数
  lengthMap.row[index] = newLength;
  sheet.addRows(row, newLength);
};

const setColsData = ({ data, index, newIndex, length, lengthMap, info }) => {
  const colDiff = getDiff(index, lengthMap.col);
  const col = newIndex + colDiff;
  const { cellInfo, subPaneInfo: subPane } = info;
  // 公式位置不填充
  const newData = data.slice(data.length - length);
  _.map(newData, (item, colIndex) => {
    _.map(item, (v, k) => {
      const subPaneInfo = _.get(subPane, `info.${k}`, null);
      const newInfo = cellInfo[k] || subPaneInfo;
      if (newInfo) {
        if (newInfo.param === '_table_') return;
        let { curRow } = newInfo;
        const authority = getAuthority(k);
        const rowDiff = getDiff(curRow, lengthMap.row);
        if (cellInfo[k]) {
          cellInfo[k].diffRows = rowDiff;
          curRow += rowDiff;
        } else if (subPaneInfo) {
          curRow += (cellInfo[subPaneInfo.paneId].diffRows || rowDiff);
        }
        setCell({ row: curRow, col: col + colIndex, value: v, authority });
        // 设置二维码类型单元格公式
        // setBarCodeFormula(curRow, col + colIndex, v, sheet.getFormula(curRow, col - 1));
        // 绑了父窗格的单元格中包含公式
        setFormula({ row: curRow, col: col + colIndex, key: k, ...info });
        // 条件公式
        setCondFormatRange(curRow, col + colIndex, k);
      }
    });
  });
  return { colDiff };
};


const setRowsData = ({ data, index, newIndex, length, lengthMap, info }) => {
  const rowDiff = getDiff(index, lengthMap.row);
  const row = newIndex + rowDiff;
  const { cellInfo, subPaneInfo: subPane } = info;

  // 公式位置不填充
  const newData = data.slice(data.length - length);
  _.map(newData, (item, rowIndex) => {
    _.map(item, (v, k) => {
      const subPaneInfo = _.get(subPane, `info.${k}`, null);
      const newInfo = cellInfo[k] || subPaneInfo;
      if (newInfo) {
        let { curCol } = newInfo;
        if (newInfo.param === '_table_') return;
        const authority = getAuthority(k);
        const colDiff = getDiff(curCol, lengthMap.col);
        if (cellInfo[k]) {
          cellInfo[k].diffCols = colDiff;
          curCol += colDiff;
        } else if (subPaneInfo) {
          curCol += (cellInfo[subPaneInfo.paneId].diffCols || colDiff);
        }
        setCell({ row: row + rowIndex, col: curCol, value: v, authority });
        // 设置二维码类型单元格公式
        setBarCodeFormula(row + rowIndex, curCol, v, sheet.getFormula(row - 1, curCol));
        // 绑了父窗格的单元格中包含公式
        setFormula({ row: row + rowIndex, col: curCol, key: k, ...info });
        // 条件公式
        setCondFormatRange(row + rowIndex, curCol, k);
      }
    });
  });
  return { rowDiff };
};

const setColAuthority = ({ colIndex, data, lengthMap, info }) => {
  const { cellInfo, subPaneInfo: subPane } = info;
  _.map(data, (item, index) => {
    _.map(item, (v, k) => {
      const subPaneInfo = _.get(subPane, `info.${k}`, null);
      const newInfo = cellInfo[k] || subPaneInfo;
      if (newInfo) {
        let { curRow } = newInfo;
        const { dataSourceType: type, dataSource, curCol, tableName, param: columnName, typeTransfer } = newInfo;
        const width = sheet.getColumnWidth(curCol);
        sheet.setColumnWidth(colIndex, width);
        if (cellInfo[k]) {
          const rowDiff = getDiff(curRow, lengthMap.row);
          cellInfo[k].diffRows = rowDiff;
          curRow += rowDiff;
        } else if (subPaneInfo) {
          curRow += (cellInfo[subPaneInfo.paneId].diffRows || 0);
        }

        const authority = _.get(authorityObj, `${sheet.name()}.${k}`);
        if (authority === 1 && config.runTimeEdit && !['_key_', '_table_'].includes(columnName)) {
          const cell = sheet.getCell(curRow, colIndex + index);
          switch (type) {
            case 'HIS': Util.editHISData(sheet, { cell, dataSource, utcTimestamp: item[`${k}-time`] }); break;
            case 'DT': Util.editDTData(sheet, { cell, tableName, columnName, typeTransfer, primaryKeyObj: item[`${k}-primaryKey`] }); break;
            default: break;
          }
        }
      }
    });
  });
};


const setRowAuthority = ({ rowIndex, data, info }) => {
  const { cellInfo, subPaneInfo: subPane } = info;
  _.map(data, (item, index) => {
    _.map(item, (v, k) => {
      const subPaneInfo = _.get(subPane, `info.${k}`, null);
      const newInfo = cellInfo[k] || subPaneInfo;
      if (newInfo) {
        const { curCol: col, dataSourceType: type, dataSource, tableName, param: columnName, typeTransfer } = newInfo;
        const authority = _.get(authorityObj, `${sheet.name()}.${k}`);
        if (authority === 1 && config.runTimeEdit && !['_key_', '_table_'].includes(columnName)) {
          const cell = sheet.getCell(rowIndex + index, col);
          switch (type) {
            case 'HIS': Util.editHISData(sheet, { cell, dataSource, utcTimestamp: item[`${k}-time`] }); break;
            case 'DT': Util.editDTData(sheet, { cell, tableName, columnName, typeTransfer, primaryKeyObj: item[`${k}-primaryKey`] }); break;
            default: break;
          }
        }
      }
    });
  });
};

const setCondFormatRange = (row, col, k) => {
  const key = k.split('-')[0];
  if (!condFormat[key]) condFormat[key] = [];
  condFormat[key].push(new spreadNS.Range(row, col, 1, 1));
};

const setFormula = ({ formulaInfo, subPaneInfo, row, col, key }) => {
  const info = _.get(subPaneInfo, `info.${key}`, '');
  if (info && info.formula) {
    const { curRow, curCol, direction } = info;
    let newFormula;
    if (direction === 'V') {
      newFormula = info.formula.replace(RegExp(`([A-Z]${curRow + 1})`, 'g'),
        v1 => v1.replace(RegExp(`(${curRow + 1})`, 'g'),
          v2 => Number(v2) + (row - curRow)));
    } else if (direction === 'H') {
      newFormula = info.formula
        .replace(/([A-Z]\d+)/g, v1 => v1.replace(/(\d+)/g, v2 => Number(v2) + (row - curRow)))
        .replace(RegExp(Util.getColString(curCol + 1), 'g'), Util.getColString(col + 1));
    }
    sheet.setFormula(row, col, newFormula);
  }

  if (formulaInfo.info[key]) {
    // 在公式中添加填充的单元格
    const param = Util.getCellPositionString(row + 1, col + 1);
    formulaInfo.info[key].params.push(param);
  }
};

const setCell = ({ row, col, value, authority }) => {
  const cell = sheet.getCell(row, col);
  if (authority === 3) {
    cell.value(null);
  } else {
    // cell.value(Util.numberTransform(value));
    cell.value(value);
  }
};

const setBarCodeFormula = (row, col, value, formula) => {
  if (formula) {
    const formulaArr = formula.match(/^BC_(\w+)\((.*?)\),/);
    if (formulaArr && formulaArr.length) {
      const v = value ? `"${value}"` : (value || '');
      const newFormula = formula.replace(`${formulaArr[2]})`, v)
      sheet.setFormula(row, col, newFormula);
    }
  }
};

const colFormulaSetting = ({ info: { cellInfo, formulaInfo, subPaneInfo }, lengthMap }) => {
  _.map(formulaInfo.info, (value, cellId) => {
    if (cellInfo[cellId].direction === 'V') return;
    const { params } = value;
    const { paneId = cellId } = subPaneInfo.info[cellId] || {};
    const firstPos = getNewFirstPosForFormula(lengthMap, paneId, cellId);
    params.push(firstPos);

    const firstCell = paneId !== firstPos ? Util.getCellPositionFromString(paneId) : {};
    _.map(formulaInfo.map[cellId], (id) => {
      const { col: firstCol } = Util.getCellPositionFromString(firstPos);
      const { row, col } = Util.getCellPositionFromString(id);
      if (firstCol === col) {
        // 自定义公式与聚合函数同列
        updateFormula({ firstCell, row, col, params, lengthMap });
      } else {
        updateFormula({ row, col, params, lengthMap });
      }
    });
  });
};

const rowFormulaSetting = ({ info: { cellInfo, formulaInfo, subPaneInfo }, lengthMap }) => {
  _.map(formulaInfo.info, (value, cellId) => {
    if (cellInfo[cellId].direction === 'H') return;
    const { params } = value;
    const { paneId = cellId } = subPaneInfo.info[cellId] || {};
    if (!params.length) return;
    const firstPos = getNewFirstPosForFormula(lengthMap, paneId, cellId);
    params.push(firstPos);

    const firstCell = paneId !== firstPos ? Util.getCellPositionFromString(paneId) : {};
    _.map(formulaInfo.map[cellId], (id) => {
      const { row: firstRow } = Util.getCellPositionFromString(firstPos);
      const { row, col } = Util.getCellPositionFromString(id);
      if (firstRow === row) {
        // 自定义公式与聚合函数同行
        updateFormula({ firstCell, row, col, params, lengthMap });
      } else {
        updateFormula({ row, col, params, lengthMap });
      }
    });
  });
};

const updateFormula = ({ firstCell, row, col, params, lengthMap }) => {
  const rowDiff = getDiff(row, lengthMap.row, _.get(firstCell, 'row'));
  const colDiff = getDiff(col, lengthMap.col, _.get(firstCell, 'col'));
  const [newRow, newCol] = [row + rowDiff, col + colDiff];
  const formula = sheet.getFormula(newRow, newCol);
  if (formula) {
    const newFormula = formula.replace(/\(.*\)/, `(${params.join(',')})`);
    sheet.setFormula(newRow, newCol, newFormula);
  }
};

const getNewFirstPosForFormula = (lengthMap, paneId, cellId) => {
  const { row: paneRow, col: paneCol } = Util.getCellPositionFromString(paneId);
  const { row: firstRow, col: firstCol } = Util.getCellPositionFromString(cellId);
  const firstRowDiff = getDiff(paneRow, lengthMap.row);
  const firstColDiff = getDiff(paneCol, lengthMap.col);
  const firstPos = Util.getCellPositionString(firstRow + firstRowDiff + 1, firstCol + firstColDiff + 1);
  return firstPos;
};

const calculatePaneSize = (arr, index) => {
  let floorSize = 0;
  let ceilSize = 0;
  _.map(arr, (i) => {
    if (Number(i) > Number(index)) {
      floorSize += 1;
    } else if (Number(i) < Number(index)) {
      ceilSize += 1;
    }
  });
  return { floorSize, ceilSize };
};

const beforeGenerateNewData = (index, data, subPaneInfo) => {
  const totalLength = Math.max(..._.values(data));
  let paneFloorSize = 0;
  let paneCeilSize = 0;
  const spanMap = {};
  _.map(data, (length, key) => {
    if (!length) return;
    spanMap[key] = (totalLength - (totalLength % length)) / length;
    if (subPaneInfo) {
      const tmp = subPaneInfo.map[key];
      if (!tmp) return;
      const { floorSize, ceilSize } = calculatePaneSize(_.keys(tmp), index);
      paneFloorSize = Math.max(paneFloorSize, floorSize);
      paneCeilSize = Math.max(paneCeilSize, ceilSize);
    }
  });

  return { spanMap, paneFloorSize, paneCeilSize };
};

const existInSubPane = (id, subInfo) => {
  return !!subInfo[id];
};

const generateNewData = ({ index, data, cellInfo, subPaneInfo }) => {
  const totalLength = _.max(_.values(data));
  const newData = [];
  const { spanMap, paneFloorSize, paneCeilSize } = beforeGenerateNewData(index, data, subPaneInfo);

  for (let i = 0; i < totalLength; i += 1) {
    const record = {};
    const newSubData = { prev: [], next: [] };
    _.map(data, (length, key) => {
      if (!length) return;
      const j = i / spanMap[key];
      if (_.isInteger(j) && j < length && !existInSubPane(key, subPaneInfo.info)) {
        generateSingleData(j, cellInfo[key], record, key);
        if (subPaneInfo.map[key] && _.keys(subPaneInfo.map[key]).length) {
          const newPane = { ...subPaneInfo, map: subPaneInfo.map[key] };
          const { prevArr, nextArr } = generateSubPaneData(index, j, newPane, record, cellInfo[key]);
          newSubData.prev = arrayMerge(newSubData.prev, prevArr);
          newSubData.next = arrayMerge(newSubData.next, nextArr);
        }
      }
    });
    pushNewData(newData, newSubData.prev);
    if (_.keys(record).length) newData.push(record);
    pushNewData(newData, newSubData.next);
  }

  const diff = (paneFloorSize + paneCeilSize) + 1;

  return {
    newData,
    length: newData.length - diff,
    paneSize: paneCeilSize + paneFloorSize,
    paneFloorSize,
    spanMap
  };
};

const pushNewData = (data, newData) => {
  if (newData.length) {
    data.push(...newData);
    newData = [];
  }
};

const arrayMerge = (array, newArray) => {
  const oldLen = array.length;
  if (oldLen) {
    _.map(array, (item, id) => {
      if (newArray[id]) {
        array[id] = _.merge({}, item, newArray[id]);
      }
    });
    if (oldLen < newArray.length) {
      array.push(...newArray.slice(oldLen));
    }
  } else {
    array = newArray;
  }
  return array;
};

const generateSubPaneData = (index, j, subPane, record, firstCell) => {
  const prevArr = [];
  const nextArr = [];
  _.map(sortKey(_.keys(subPane.map)), (subIndex) => {
    const diff = Number(subIndex) - Number(index);
    const newRecord = {};
    if (!diff) {
      traverseSubPane(record, j, subIndex, subPane, firstCell);
    } else if (diff > 0) {
      traverseSubPane(newRecord, j, subIndex, subPane, firstCell);
      if (_.keys(newRecord).length) nextArr.push(newRecord);
    } else {
      traverseSubPane(newRecord, j, subIndex, subPane, firstCell);
      if (_.keys(newRecord).length) prevArr.push(newRecord);
    }
  });
  return { prevArr, nextArr };
};

const traverseSubPane = (record, j, index, { map, info }, firstCell) => {
  _.map(map[index], (subId) => {
    if (info[subId]) generateSingleData(j, info[subId], record, subId, firstCell);
  });
};

const getCurPos = ({ index, cellInfo, rowDiff = 0, colDiff = 0 }) => {
  const { sheetName, direction, curRow, curCol } = cellInfo;
  const [row, col] = direction === 'V' ? [curRow + index + rowDiff, curCol + colDiff] : [curRow + rowDiff, curCol + index + colDiff];
  if (!dataInfo[sheetName]) dataInfo[sheetName] = {};
  const cellPos = Util.getCellPositionString(row + 1, col + 1);
  return { cellPos };
};

const generateSingleData = (index, cellInfo, record, id, firstCell) => {
  const { dataSourceType, param, group, dataSource, tableName, firstId, displaySelect = 'value', curRow, curCol } = cellInfo;
  let key = firstCell ? `${firstCell.firstId}-${tableName || dataSource}` : `${firstId}-${tableName || dataSource}`;
  // 有过滤条件时计算自身
  if (getFilterRules(curRow, curCol).length && (_.get(allDataSource, `${id}-${tableName || dataSource}`))) {
    key = `${id}-${tableName || dataSource}`;
  }

  if (record[id]) return;
  switch (param) {
    case '_table_': record[id] = null; break;
    case '_key_': record[id] = index + 1; break;
    default: {
      switch (dataSourceType) {
        case 'HIS': {
          if (displaySelect === 'time') {
            const value = _.get(allDataSource[key], `list[${index}].time`, null);
            record[id] = dateTransform({ value, isHisDate: true });
          } else {
            record[id] = _.get(allDataSource[key], `list[${index}]['${param}-${group}']`, null);
          }
          setHisTime(record, id, key, index);
          break;
        }
        case 'RTS': {
          const value = _.get(allDataSource[key], `list[${index}].value`, null);
          record[id] = dateTransform({ value, type: 'RTS', param: cellInfo.statisticalType });
          break;
        }
        case 'DT': {
          record[id] = _.get(allDataSource[key], `list[${index}].${param}`, null);
          setPrimaryKeyValues(record, id, tableName, index, key);
          break;
        }
        case 'CTS':
        case 'SER': record[id] = _.get(allDataSource[key], `list[${index}].${param}`, null); break;
        case 'ENT': record[id] = _.get(allDataSource[key], `list[${index}]`, null); break;
        case 'DATE': record[id] = cellInfo.list[index]; break;
        default: record[id] = cellInfo.dataSource; break;
      }
      break;
    }
  }
};

const setHisTime = (record, id, key, index) => {
  if (config.runTimeEdit) {
    record[`${id}-time`] = getHisTime({ key, index });
  }
};

const getHisTime = ({ key, index }) => {
  return _.get(allDataSource[key], `list[${index}].time`, null);
};

const setPrimaryKeyValues = (record, id, tableName, index, key) => {
  const primaryKeys = getSqlInfoPrimaryKeys(tableName);
  if (primaryKeys) {
    record[`${id}-primaryKey`] = getPrimaryKeys({ tableName, index, key });
  }
};

const getSqlInfoPrimaryKeys = (tableName) => {
  return _.get(config.sqlInfo[tableName], 'primaryKeys', []);
};

const getPrimaryKeys = ({ key, primaryKeys, tableName, index }) => {
  const keyObj = {};
  const keys = primaryKeys || getSqlInfoPrimaryKeys(tableName);
  _.map(keys, (k) => {
    const tableKeys = _.keys(_.get(allDataSource[key], `list[${index}]`));
    if (tableKeys.includes(k)) {
      keyObj[k] = _.get(allDataSource[key], `list[${index}].${k}`, null);
    } else {
      keyObj[k] = 'params_key_null';
    }
  });
  return keyObj;
};

const calculateDataLength = (map, cells, subPaneInfo, isHorizontal) => {
  if (_.keys(allDataSource).length) {
    _.map(map, (arr, index) => {
      const _key_ = [];
      _.map(arr, (v, k) => {
        const { dataSourceType, startIndex, endIndex, tableName, dataSource, param, firstId } = cells[k];
        const filterRules = getFilterRules(cells[k].curRow, cells[k].curCol);
        // 子格且没有过滤条件时不单独显示
        if (_.get(subPaneInfo.info, k) && !(filterRules && filterRules.length > 0 && cells[k].firstId !== cells[k].cellId)) return;
        if (dataSourceType !== 'DATE') {
          const key = `${firstId}-${tableName || dataSource}`;
          if (param === '_table_' && isHorizontal) {
            const { list } = allDataSource[key];
            map[index][k] = _.keys(_.get(list, [0], null)).length;
          } else {
            if (param === '_key_' && dataSourceType === 'HIS') _key_.push(k);
            const length = _.get(allDataSource[key], 'pageSize', 0);
            const showLength = (endIndex - startIndex) + 1 || length;
            map[index][k] = Math.min(showLength, length);
          }
        }
        _.map(_key_, (key) => {
          map[index][key] = _.max(_.values(map[index]));
        });
      });
    });
  }
  return map;
};

const calculateRowColLength = ({ cellInfo, subPaneInfo }) => {
  const rowColMap = {
    V: { map: {}, key: 'curRow' },
    H: { map: {}, key: 'curCol' }
  };

  _.map(_.keys(cellInfo).reverse(), (id) => {
    initRowColLength(cellInfo[id], id, rowColMap, subPaneInfo);
  });

  return {
    rowMap: calculateDataLength(rowColMap.V.map, cellInfo, subPaneInfo),
    colMap: calculateDataLength(rowColMap.H.map, cellInfo, subPaneInfo, true)
  };
};

const initRowColLength = (cell, id, rowColMap, subPane) => {
  const { map, key } = rowColMap[cell.direction || 'V'];
  const index = cell[key];
  if (!map[index]) map[index] = {};
  if (cell.dataSourceType === 'DATE') {
    generateDateArray(cell);
    map[index][id] = cell.list.length;
    if (_.get(subPane.info, id)) {
      subPane.info[id].list = cell.list;
    }
  } else {
    const { param, direction, curRow, curCol } = cell;
    if (param === '_table_') {
      if (direction === 'V') {
        if (!rowColMap.H.map[curCol]) rowColMap.H.map[curCol] = {};
        rowColMap.H.map[curCol][id] = 0;
      } else {
        if (!rowColMap.V.map[curRow]) rowColMap.V.map[curRow] = {};
        rowColMap.V.map[curRow][id] = 0;
      }
    }
    map[index][id] = 0;
  }
  // map[行/列][父窗格id] = 0

  if (subPane && subPane.map[id]) {
    const subPaneMap = {};
    _.map(subPane.map[id], (subId) => {
      const item = subPane.info[subId];
      if (!item) return;
      const subIndex = item[key];
      if (!subPaneMap[subIndex]) subPaneMap[subIndex] = new Set();
      subPaneMap[subIndex].add(subId);
    });
    _.map(subPaneMap, (set, subIndex) => {
      subPaneMap[subIndex] = Array.from(set);
    });
    subPane.map[id] = subPaneMap;
  }
};

const backfillPane = (ids, subPane, cellInfo) => {
  const tmp = _.merge({}, subPane);
  const newPanes = {};
  _.map(ids, (id) => {
    if (!cellInfo[id]) return;
    // 有父窗格的子窗格
    if (tmp.info[id] && !tmp.map[id]) return;
    // 没有父窗格，没有子窗格
    if (!tmp.map[id]) tmp.map[id] = {};
    const { curCol, curRow, direction } = cellInfo[id];
    const map = tmp.map[id];
    if (direction === 'V') {
      map[curRow] = [...(map[curRow] || []), id];
    } else if (direction === 'H') {
      map[curCol] = [...(map[curCol] || []), id];
    }
    newPanes[id] = map;
  });
  return newPanes;
};

const getNewPane = (cellInfo, subPane, dataLength) => {
  if (!subPane) subPane = { map: {}, info: {} };
  return backfillPane(_.keys(dataLength), subPane, cellInfo);
};

// const getBrushRange = (paneInfo, step, direction) => {
//   let [sRow, sCol, eRow, eCol] = [sheet.getRowCount(), sheet.getColumnCount(), 0, 0];
//   _.map(paneInfo, (array, row) => {
//     if (sRow > Number(row)) sRow = Number(row);
//     if (eRow < Number(row)) eRow = Number(row);
//     _.map(array, (pos) => {
//       const { col } = Util.getCellPositionFromString(pos);
//       if (sCol > col) sCol = col;
//       if (eCol < col) eCol = col;
//     });
//   });
//   const rowCount = (eRow - sRow) + 1;
//   const colCount = (eCol - sCol) + 1;
//   return {
//     rowCount: direction === 'V' ? rowCount * step : rowCount,
//     colCount: direction === 'H' ? colCount * step : colCount
//   };
// };

const brushPaneStyle = ({ cellInfo, subPane, dataLength, lengthMap }) => {
  const newPanes = getNewPane(cellInfo, subPane, dataLength);
  const maxLength = _.max(_.values(dataLength));
  if (!_.keys(newPanes).length) return;
  const maxCol = sheet.getColumnCount();
  const maxRow = sheet.getRowCount();

  _.map(dataLength, (len, id) => {
    const { direction, param, list = [], showHeader, curRow, curCol } = cellInfo[id];
    const map = newPanes[id];
    if (!map) return;
    // const step = Math.floor(maxLength / dataLength[id]);
    // const { rowCount, colCount } = getBrushRange(map, step, direction);
    const rowDiff = getDiff(curRow, lengthMap.row);
    const colDiff = getDiff(curCol, lengthMap.col);
    _.map(_.values(map), (array) => {
      _.map(array, (pos) => {
        let { row, col } = Util.getCellPositionFromString(pos);
        if (pos !== id) {
          // 存在父窗格
          const cell = Util.getCellPositionFromString(id);
          // 纵向插入时除去父窗格插入的行数， 横向填充时除去父窗格插入的列数
          const subRowDiff = getDiff(row, lengthMap.row, direction === 'V' ? cell.row : undefined);
          const subColDiff = getDiff(col, lengthMap.col, direction === 'H' ? cell.col : undefined);
          row += subRowDiff;
          col += subColDiff;
        } else {
          row += rowDiff;
          col += colDiff;
        }

        const rowHeight = sheet.getRowHeight(row);
        const colWidth = sheet.getColumnWidth(col);
        if (row > maxRow - 1 || col > maxCol - 1) return;
        let rowStep = maxLength - 1;
        if (param === '_table_') {
          // 行总和, 当展示表头并且纵向填充时，行数加一
          rowStep = showHeader && direction === 'V' ? maxLength + 1 : maxLength;
        }
        const colStep = _.keys(_.get(list, [0], {})).length;
        for (let i = 0; i < rowStep; i += 1) {
          if (param === '_table_') {
            for (let c = 0; c < colStep; c += 1) {
              setCellStyle(row, col, row + i, col + c, rowHeight, colWidth);
            }
          } else if (direction === 'V') {
            const eRow = (i + 1) + row;
            setCellStyle(row, col, eRow, col, rowHeight, colWidth);
          } else if (direction === 'H') {
            const eCol = (i + 1) + col;
            setCellStyle(row, col, row, eCol, rowHeight, colWidth);
          }
        }
      });
    });
  });
};

const brushOtherStyle = ({ cellInfo, subPane, dataLength, lengthMap, index, type }) => {
  const newPanes = getNewPane(cellInfo, subPane, dataLength);
  dataLength = _.mapValues(dataLength, (len, id) => {
    const { direction, param, showHeader } = cellInfo[id];
    if (param === '_table_') {
      // 行总和, 当展示表头并且纵向填充时，行数加一
      return showHeader && direction === 'V' && type === 'row' ? len + 1 : len;
    }
    return len;
  });
  const maxLength = _.max(_.values(dataLength));
  if (!_.keys(newPanes).length) return;
  const maxCol = sheet.getColumnCount();
  const maxRow = sheet.getRowCount();
  const maxCellLength = type === 'row' ? maxCol : maxRow;
  const cellArr = [];
  _.map(_.keys(dataLength), (id) => {
    const { curRow, curCol } = cellInfo[id];
    if (type === 'row') {
      cellArr.push(curCol);
    } else {
      const diff = getDiff(curRow, lengthMap.row);
      cellArr.push(diff + curRow);
    }
  });
  for (let i = 0; i < maxCellLength; i += 1) {
    let [row, col] = type === 'row' ? [Number(index), i] : [i, Number(index)];
    if (!cellArr.includes(i)) {
      const rowDiff = getDiff(row, lengthMap.row);
      const colDiff = getDiff(col, lengthMap.col);
      if (type === 'row') row += rowDiff;
      col += colDiff;
      const rowHeight = sheet.getRowHeight(row);
      const colWidth = sheet.getColumnWidth(col);
      if (row > maxRow - 1 || col > maxCol - 1) return;
      const rowStep = maxLength - 1;
      for (let j = 0; j < rowStep; j += 1) {
        if (type === 'row') {
          const eRow = (j + 1) + row;
          setCellStyle(row, col, eRow, col, rowHeight, colWidth);
        } else if (type === 'col') {
          const eCol = (j + 1) + col;
          setCellStyle(row, col, row, eCol, rowHeight, colWidth);
        }
      }
    }
  }
};

const setCellStyle = (sRow, sCol, eRow, eCol, rowHeight, colWidth) => {
  const { style } = spreadNS.CopyToOptions;
  try {
    sheet.copyTo(sRow, sCol, eRow, eCol, 1, 1, style);
    sheet.setRowHeight(eRow, rowHeight);
    sheet.setColumnWidth(eCol, colWidth);
    setFillCellRowHeight(eRow, eCol);
  } catch (err) {
    console.log(err);
  }
};

const setFillCellRowHeight = (row, col) => {
  if (autoFitRows.includes(row)) return;
  const cell = sheet.getCell(row, col);
  const rowHeight = sheet.getRowHeight(row);
  if (cell.wordWrap()) {
    sheet.autoFitRow(row);
    autoFitRows.push(row);
    const changedRowHeight = sheet.getRowHeight(row);
    if (changedRowHeight < rowHeight) {
      sheet.setRowHeight(row, rowHeight);
    }
  }
};

const getSubPaneInfo = (cellInfo, tagName) => {
  const paneConf = _.get(sheet.tag(), tagName, null);
  const map = {};
  const info = {};
  if (paneConf && _.keys(paneConf).length) {
    _.map(paneConf, (subIds, id) => {
      if (!cellInfo[id]) return;
      map[id] = new Set();
      _.map(subIds, (subId) => {
        if (!subId || subId === id || (map[subId] && map[subId].includes(id))) return;
        const { row, col } = Util.getCellPositionFromString(subId);
        const cell = sheet.getCell(row, col);
        map[id].add(subId);
        if (!cellInfo[subId]) {
          info[subId] = {
            cellId: subId,
            curCol: col,
            curRow: row,
            // dataSourceType: cellInfo[id].dataSourceType,
            dataSource: cell.value(),
            direction: cellInfo[id].direction,
            formula: sheet.getFormula(row, col)
          };
        } else {
          info[subId] = _.merge({}, cellInfo[subId]);
        }
        info[subId].paneId = id;
      });
      map[id] = Array.from(map[id]);
      if (!map[id].length) delete map[id];
    });
    // 删掉子窗格结点
    // _.map(info, (v, k) => {
    //   delete cellInfo[k];
    // });
  }

  return { map, info };
};

const getFormulaInfo = (cellInfo) => {
  const formulaConf = _.get(sheet.tag(), 'formulaConf', null);
  const map = {}; // 存储与公式关联的聚合函数
  const info = {}; // 存储关联了聚合函数的所有公式信息
  _.map(formulaConf, (formulaIds, id) => {
    if (!cellInfo[id]) return;
    map[id] = new Set();
    _.map(formulaIds, (formulaId) => {
      const { row, col } = Util.getCellPositionFromString(formulaId);
      if (formulaId === id || !sheet.getFormula(row, col)) return;
      map[id].add(formulaId);
    });
    map[id] = Array.from(map[id]);
    if (!map[id].length) delete map[id];
  });
  _.map(map, (v, k) => {
    info[k] = { diff: 0, params: [] };
  });
  return { map, info };
};

const prepareDataForReplace = (report) => {
  const { keySet } = analyzeFunction(report.allRecords);
  const allData = report.getAllData();

  if (!fetchAllDataSource(allData, Array.from(keySet))) return;

  report.spread.suspendPaint();
  report.updateJson();

  _.map(sheetInfo, (cellInfo, sheetName) => {
    allDataSource = {};
    sheet = report.spread.getSheetFromName(sheetName);
    // 重组数据源
    resetDataSource({ cellInfo, fillType: 'replace' });
    // 替换填充
    traverseForReplace(cellInfo);
    // 重设公式位置的值
    resetFirstPosValue({ cellInfo });
  });
  report.spread.resumePaint();
};

const traverseForReplace = (cellInfo) => {
  _.map(_.merge({}, cellInfo), (data) => {
    const { dataSourceType, dataSource, tableName, param, firstId } = data;
    if (dataSourceType === 'RT') return;
    switch (dataSourceType) {
      case 'DATE': {
        generateDateArray(data);
        break;
      }
      case 'DT': {
        const key = `${firstId}-${tableName}`;
        const { list } = allDataSource[key];
        data.key = key;
        if (param === '_key_') {
          data.maxLength = list.length;
        } else {
          data.list = list;
          if (list.length) data.firstValue = _.get(list[0], param);
        }
        break;
      }
      case 'HIS': {
        const key = `${firstId}-${dataSource}`;
        const { list } = allDataSource[key] || {};
        data.key = key;
        if (list) {
          data.list = list;
        } else if (param === '_key_') {
          const instance = dataSource.split('._key_')[0];
          data.maxLength = _.max(_.map(_.values(_.filter(allDataSource, (v, k) => ~k.indexOf(instance))), obj => _.get(obj, 'list', []).length));
        }
        break;
      }
      case 'RTS': {
        const key = `${firstId}-${dataSource}`;
        const { list } = allDataSource[key] || {};
        data.key = key;
        if (list) {
          data.list = list;
        }
        break;
      }
      case 'CTS':
      case 'SER': {
        const key = `${firstId}-${dataSource}`;
        const { list } = allDataSource[key];
        data.key = key;
        if (param === '_key_') {
          const instance = dataSource.split('_key_')[0];
          data.maxLength = _.max(_.map(_.values(_.filter(allDataSource, (v, k) => ~k.indexOf(instance))), obj => _.get(obj, 'list', []).length));
        } else {
          data.list = list;
          if (list.length) data.firstValue = _.get(list[0], param);
        }
        break;
      }
      case 'ENT': {
        const key = `${firstId}-${dataSource}`;
        const { list } = allDataSource[key];
        data.key = key;
        data.list = list;
        if (list.length) data.firstValue = _.get(list, '[0]', null);
        break;
      }
      default: break;
    }
    const tmpLength = Math.max(_.get(data, 'list', []).length - 1, data.maxLength || 0);
    data.endIndex = Math.min(data.endIndex, tmpLength);

    if (param === '_table_') {
      data.list = data.list.slice(0, (data.endIndex - data.startIndex) + 1);
      fillAllDataByReplace(data);
    } else {
      fillDataByReplace(data);
    }
  });
};

const fillAllDataByReplace = ({
  curRow = 0,
  curCol = 0,
  direction = 'V',
  showHeader = false,
  theme = null,
  list,
  dataSource
}) => {
  const tableTheme = theme !== 'null' ? spreadNS.Tables.TableThemes[theme] : new spreadNS.Tables.TableTheme();
  if (list && list.length) {
    const tmpData = getTableNextLineData(curRow, curCol, list);
    try {
      const table = sheet.tables.addFromDataSource(
        `${dataSource.replace(/:/g, '_')}${Math.round(Math.random() * 10000)}`,
        curRow,
        curCol,
        list,
        tableTheme
      );
      table.filterButtonVisible(false);
      if (!showHeader || direction === 'H') {
        table.showHeader(false);
        table.cj -= 1;
        setTableNextLineData(curRow + list.length, curCol, tmpData);
      }
    } catch (e) {
      message.warning(intl.formatMessage(messages.cellNotFoundOfFill));
      console.log(e);
    }
  }
};

const fillDataByReplace = ({
  cellId = '',
  sheetName = 'Sheet1',
  dataSourceType = '',
  startIndex = 0,
  endIndex = startIndex,
  curRow = 0,
  curCol = 0,
  direction = 'V',
  param = 'numberValue',
  group = '',
  list,
  dataSource = '',
  formatter = '',
  maxLength,
  key,
  typeTransfer,
  statisticalType = '',
  displaySelect = 'value'
}) => {
  const isHisDate = dataSourceType === 'HIS' && displaySelect === 'time';
  const authority = _.get(authorityObj, `${sheetName}.${cellId}`) || 2;
  let dataIndex = 0;
  for (let i = startIndex; i <= endIndex; i += 1) {
    const [row, col] = direction === 'V' ? [curRow + i, curCol] : [curRow, curCol + i];
    const utcTimestamp = getHisTime({ key, index: startIndex + i });
    const cell = sheet.getCell(row, col);
    let value;
    if (param === '_key_') {
      if (maxLength === undefined || dataIndex + 1 > maxLength) return;
      value = dataIndex + 1;
    } else {
      let index;
      switch (dataSourceType) {
        case 'DATE': index = [startIndex + dataIndex]; break;
        case 'HIS': index = [startIndex + dataIndex, `${param}-${group}`]; break;
        case 'RTS': index = [startIndex + dataIndex, 'value']; break;
        default: index = [startIndex + dataIndex, param]; break;
      }
      if (isHisDate) index = `[${startIndex + dataIndex}].time`;
      value = _.get(list, index, null);
    }
    if (authority === 3) {
      cell.value(null);
      dataIndex += 1;
    } else {
      // 联动：存储整行的内容
      const { cellPos } = getCurPos({ index: i, cellInfo: { direction, curRow, curCol, sheetName } });
      dataInfo[sheetName][cellPos] = _.get(list, i, null);

      // 填充合并的单元格
      const newDataIndex = handleSpan(row, col, value, dataIndex, dataSourceType, statisticalType, isHisDate);
      if (newDataIndex === dataIndex) {
        endIndex += 1;
      } else {
        dataIndex = newDataIndex;
      }
      cell.formatter(formatter);
      if (cell.wordWrap() && !autoFitRows.includes(row)) {
        sheet.autoFitRow(row);
        autoFitRows.push(row);
      }
    }

    if (authority === 1 && config.runTimeEdit) {
      switch (dataSourceType) {
        case 'HIS': {
          // 未来时间不允许填报
          if (utcTimestamp) Util.editHISData(sheet, { cell, dataSource, utcTimestamp });
          break;
        }
        case 'DT': {
          const [tableName, columnName] = dataSource.split('.');
          const primaryKeyObj = getPrimaryKeys({ key, tableName, index: startIndex + i });
          Util.editDTData(sheet, { cell, tableName, columnName, primaryKeyObj, typeTransfer });
          break;
        }
        default: break;
      }
    }
  }
};

const handleSpan = (row, col, value, dataIndex, type, param, isHisDate) => {
  const range = { row, col, rowCount: 1, colCount: 1 };
  if (sheet.getSpans(range).length) {
    const span = sheet.getSpans(range);
    const { row: r, col: c } = span[0];
    if (r !== row || c !== col) return dataIndex;
  }
  setTransformedValue(row, col, value, type, param, isHisDate);
  return dataIndex + 1;
};

const setTransformedValue = (row, col, value, type, param, isHisDate) => {
  const cellValue = dateTransform({ value, type, param, isHisDate });
  sheet.getCell(row, col).value(Util.numberTransform(cellValue));
};

const dateTransform = ({ value, type, param, isHisDate }) => {
  if (!value) return value;
  if ((type === 'RTS' && ['MAT', 'MIT', 'INT'].includes(param) && _.isInteger(value)) || isHisDate) {
    return moment(value).utcOffset(moment().utcOffset()).format('YYYY-MM-DD HH:mm:ss');
  }
  return value;
};

const generateDateArray = (cellInfo) => {
  const { dateType, templateType, dataLength, interval = 1 } = cellInfo;
  const newInterval = _.isInteger(interval) && interval > 0 ? interval : 1;
  const { minTime, maxTime } = getMinMaxTime();

  const { year: minYear, month: minMonth, date: minDay, hour: minHour } = minTime;
  const { year: maxYear, month: maxMonth, date: maxDay, hour: maxHour } = maxTime;

  const list = [];
  let initFlag = true;
  const diffHours = maxTime.moment.diff(minTime.moment, 'hour');

  for (let y = minYear; y <= maxYear; y += (dateType === 'y' ? newInterval : 1)) {
    if (dateType === 'y') {
      addDateItem({ list, templateType, y, dataLength });
    } else {
      const startMonth = initFlag ? minMonth : 1;
      const monthInYear = maxYear === y ? maxMonth : 12;
      for (let m = startMonth; m <= monthInYear; m += (dateType === 'm' ? newInterval : 1)) {
        if (dateType === 'M') {
          addDateItem({ list, templateType, y, m, dataLength });
          initFlag = false;
        } else {
          let daysInMonth = moment(m, 'M').daysInMonth();
          let startDate = initFlag ? minDay : 1;
          initFlag = false;
          // 最后一个月
          if (maxYear === y && maxMonth === m) {
            daysInMonth = maxDay;
            startDate = maxMonth === minMonth ? minDay : 1;
          }
          for (let d = startDate; d <= daysInMonth; d += (dateType === 'd' ? newInterval : 1)) {
            if (dateType === 'd') {
              addDateItem({ list, templateType, y, m, d, dataLength });
            } else if (dateType === 'h') {
              if (minYear === y && minMonth === m && minDay === d) {
                // 第一天
                const maxLength = minHour + diffHours >= 24 ? 23 : minHour + diffHours;
                for (let h = minHour; h <= maxLength; h += (dateType === 'h' ? newInterval : 1)) {
                  addDateItem({ list, templateType, y, m, d, h, dataLength });
                }
              } else if (maxYear === y && maxMonth === m && maxDay === d) {
                // 最后一天
                for (let h = 0; h <= maxHour; h += (dateType === 'h' ? newInterval : 1)) {
                  addDateItem({ list, templateType, y, m, d, h, dataLength });
                }
              } else {
                for (let h = 0; h <= 23; h += (dateType === 'h' ? newInterval : 1)) {
                  addDateItem({ list, templateType, y, m, d, h, dataLength });
                }
              }
            }
          }
        }
      }
    }
  }
  cellInfo.list = list;
  cellInfo.endIndex = list.length - 1;
};

const addDateItem = ({ list = [], templateType = '', y = '', m = '', d = '', h = '', dataLength = 0 }) => {
  if (list.length <= 500 && (!dataLength || list.length < dataLength)) {
    list.push(templateType.replace('y', y).replace('M', m).replace('d', d).replace('h', h));
  }
};

const getYearMonthDate = (date) => {
  const newDate = date.set({ minute: 0, second: 0 });
  return {
    moment: newDate,
    year: newDate.year(),
    month: newDate.month() + 1,
    date: newDate.date(),
    hour: newDate.hour()
  };
};

const getMinMaxTime = () => {
  return {
    minTime: getYearMonthDate(minDate ? moment(minDate) : moment().subtract(1, 'd')),
    maxTime: getYearMonthDate(maxDate ? moment(maxDate) : moment())
  };
};
