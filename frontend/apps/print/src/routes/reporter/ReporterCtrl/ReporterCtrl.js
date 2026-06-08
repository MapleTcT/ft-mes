import React, { Component } from 'react';
import * as _ from 'lodash';

import Reporter from '../ReporterDesigner/Reporter';
import { queryCustomService, queryEntityObjects } from '../../../services/templateDesigner';

export default class ReporterCtrl extends Component {
  constructor(props) {
    super(props);
    const { config = {} } = this.props;
    this.state = this.initial(config);
    this.currentConfig = this.state.config;
    this.dataSource = (config && config.dataSourceInfo && config.dataSourceInfo.dataSource) || {};
    this.state.cellAuthorityArr = _.get(config, 'cellAuthorityArr') || [];
    if (props.registerChart) {
      props.registerChart(this);
    }
  }

  componentDidMount() {
    // 根据配置判断报表初始化是否加载数据
    // const { initLoadData = false } = this.state.config;
    // if (initLoadData) {
    // const formData = this.getInitAssociateParameter();
    // 获取datatable数据
    // this.props.getDataTable({ formData, firstQuery: true });
    // 获取实时统计值
    // this.props.getStatisticInfo({ formData, firstQuery: true });
    // }
    const { config, prePrint } = this.props;
    if (prePrint) {
      this.getData(config);
    }
  }

  componentWillReceiveProps(nextProps) {
    const { config, prePrint } = nextProps || {};
    if (!config) return;
    const configs = this.initial(config || {});
    this.setState({
      ...configs
    });
    if (prePrint) {
      this.getData(config);
    }
  }

  componentWillUnmount() {
    this.currentConfig = null;
  }

  getData = (config) => {
    const customServices = {};
    const entityObjects = {};
    const customPromise = this.fetchCustomService(config, customServices);
    const entityPromise = this.fetchEntityObjects(config, entityObjects);
    const allRequestList = [...customPromise, entityPromise];
    Promise.all(allRequestList).then(() => {
      this.setState({
        customServices,
        entityObjects
      });
    });
  }

  fetchCustomService = (config, customServices) => {
    const { customService } = config;
    const serviceList = [];
    _.map(customService, (item, name) => {
      serviceList.push(new Promise((resolve) => {
        queryCustomService(item.sourceUrl, true).then((res) => {
          const { status, data: { data = [] } } = res;
          if (+status === 200) {
            const list = _.map(data, ((obj) => {
              return _.mapValues(obj, 'value');
            }));
            customServices[name] = {
              list
            };
            resolve();
          }
          resolve();
        }).catch(() => {
          resolve();
        });
      }));
    });
    return serviceList;
  }

  fetchEntityObjects = (config, objects) => {
    const { entityObjects } = config;
    const { entityCode, formId, pageId } = this.props;
    const entityList = [];
    _.map(entityObjects, (item) => {
      const { propCode, paramCode } = item;
      entityList.push({ modelCode: propCode, propertyCode: paramCode });
    });
    const entityPromise = new Promise((resolve) => {
      queryEntityObjects(entityCode, formId, entityList, pageId).then((res) => {
        const { status, data: { data } } = res;
        if (+status === 200) {
          if (data && data.length) {
            const dataModelCode = data.map(d => d.propertyCode);
            entityList.forEach(entity => {
              const { propertyCode } = entity;
              if (!dataModelCode.includes(propertyCode)) {
                objects[`${entity.modelCode}@:@${propertyCode}`] = {
                  list: [null]
                };
              }
            })
            _.map(data, (obj) => {
              objects[`${obj.modelCode}@:@${obj.propertyCode}`] = {
                list: obj.propertyValue
              };
            });
          }
        }
        resolve();
      }).catch(() => {
        resolve();
      });
    });
    return entityPromise;
  }

  getInitAssociateParameter() {
    const { config: { initValues = [] } } = this.props;
    const params = {};
    _.map(initValues, (item) => {
      if (!params[item.dataSource]) {
        params[item.dataSource] = {};
      }
      params[item.dataSource][`${item.key}`] = item.value;
    });
    return params;
  }

  setJsonData = (json, objects) => {
    this.currentConfig.json = json;
    this.setReportInfo(objects);
  }

  setReportInfo = (objects) => {
    const array = [];
    const serviceArr = [];
    const dataTable = {};
    const statisticTask = {};
    const { serviceInput } = objects;
    _.map(objects, (value, key) => {
      if (key === 'cellInfo') {
        _.map(value, (item) => {
          const { dataSource, type } = item;
          if (['HIS', 'SER'].includes(type)) {
            const [instanceName, propertyDesc] = dataSource.split('.');
            if (instanceName !== undefined && propertyDesc !== undefined) {
              item.selectedInstance = { instanceName };
              item.selectedProp = { propertyDesc };
            }
          }
          switch (type) {
            case 'DT': {
              if (item.dataTable) {
                if (dataTable[item.dataTable]) {
                  const { pageSize } = dataTable[item.dataTable];
                  item.pageSize = Math.max(pageSize || 200, item.pageSize || 200);
                }
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
            case 'HIS': {
              array.push(item);
              break;
            }
            case 'SER': {
              const newItem = _.cloneDeep(item);
              newItem.type = 'service';
              newItem.inputs = _.get(serviceInput, dataSource);
              serviceArr.push(newItem);
              break;
            }
            default: break;
          }
        });
      } else if (key === 'dataSourceInfo') {
        this.dataSource = value;
      } else {
        this.currentConfig[key] = value;
      }
    });
    this.currentConfig.dataTable = dataTable;
    this.currentConfig.statisticTask = statisticTask;
    this.setObject([...array, ..._.uniqBy(serviceArr, 'dataSource')]);
  }

  // 初始化控件
  initial = (config = {}) => {
    const result = {
      config
    };
    if (!config.bindedCtrls) {
      result.config.bindedCtrls = [];
    }
    if (!config.associatedObject) {
      result.config.associatedObject = [];
    }
    if (!config.parameterAssociate) {
      result.config.parameterAssociate = {};
    }
    if (!config.colsWidth) {
      result.colsWidth = '100%';
    }
    if (config.runTimeEdit === undefined) {
      result.config.runTimeEdit = false;
    }
    if (config.runTimeShowSheet === undefined) {
      result.config.runTimeShowSheet = true;
    }
    if (config.runTimeZoomSheet === undefined) {
      result.config.runTimeZoomSheet = true;
    }
    if (config.runTimeZoomLargeSheet === undefined) {
      result.config.runTimeZoomLargeSheet = false;
    }
    if (config.initLoadData === undefined) {
      result.config.initLoadData = true;
    }
    if (!config.fillDataType) {
      result.config.fillDataType = 'insert';
    }
    if (config.scrollbarVisible === undefined) {
      result.config.scrollbarVisible = 'auto';
    }
    if (config.positionSetting === undefined) {
      result.config.positionSetting = {};
    }
    if (config.horizontalAdaptive === undefined) {
      result.config.horizontalAdaptive = 'horizontal';
    }
    if (config.verticalAdaptive === undefined) {
      result.config.verticalAdaptive = 'horizontal';
    }
    if (config.backgroundColor === undefined) {
      result.backgroundColor = '#ffffff';
    }
    return result;
  }

  getAdaptiveType = (orientation) => {
    const { horizontalAdaptive = 'horizontal', verticalAdaptive = 'horizontal' } = this.state.config || {};
    switch (orientation) {
      case 'portrait': return verticalAdaptive;
      case 'landscape': return horizontalAdaptive;
      default: return verticalAdaptive;
    }
  }

  repositionNode = (currentWidth, currentHeight, originNodeInfo, scaleX, scaleY, orientation) => {
    // const { originWidth, originHeight, positionX, positionY } = originNodeInfo || {};
    // const [newWidth, newHeight] = [originWidth * scaleX, originHeight * scaleY];
    // const adaptive = this.getAdaptiveType(orientation);
    // const { data } = this.props;
    // let [width, height] = [newWidth, newHeight];
  }

  getNewXY = (currentWidth, currentHeight, originNodeInfo) => {
    const { originWidth, originHeight } = originNodeInfo || {};
    const [newX, newY] = [currentWidth / originWidth, currentHeight / originHeight];
    return { newX, newY };
  }

  scale = (scaleX, newXY, orientation) => {
    const { newX, newY } = newXY || {};
    if (this.props.isPreview) {
      const { runTimeZoomSheet = true, runTimeZoomLargeSheet = false } = this.state.config || {};
      const { spread } = this.report;
      const originJson = this.state.config.json || spread.toJSON();
      spread.suspendPaint();
      for (let index = 0; index < spread.getSheetCount(); index += 1) {
        const sheet = spread.getSheet(index);
        const adaptive = this.getAdaptiveType(orientation);
        const json = originJson.sheets[sheet.name()];
        const { zoomFactor = 1 } = json;
        if (scaleX > 1 && runTimeZoomLargeSheet) {
          // 缩放
          const zoom = this.getZoom(zoomFactor * scaleX);
          sheet.zoom(zoom);
          this.adaptive(json, sheet, false, zoom);
        } else if (scaleX > 1 || !runTimeZoomSheet) {
          // 拉伸
          sheet.zoom(zoomFactor);
          this.adaptive(json, sheet, true, scaleX, newX, newY, adaptive);
        } else if (scaleX < 1) {
          // 缩放
          const zoom = this.getZoom(zoomFactor * scaleX);
          sheet.zoom(zoom);
          this.adaptive(json, sheet, false, zoom);
        }
      }
      spread.resumePaint();
      spread.refresh();
    }
  }

  getZoom = (zoom) => {
    return (Math.ceil(zoom * 100) / 100);
  }

  adaptive = (json, sheet, shouldUpdate, scaleX, newX, newY, adaptive) => {
    const { data } = this.props;
    const { columns, rows, data: { dataTable } } = json;
    const width = data.getWidth();
    const height = data.getHeight();
    const columnCount = sheet.getColumnCount() || 20;
    const rowCount = sheet.getRowCount() || 200;

    // 默认列宽为62 行高20
    if (this.isMobile || this.isZhizhi) {
      switch (adaptive) {
        case 'horizontal': {
          this.scaleFont(sheet, Math.min(newX, newY), columnCount, rowCount, dataTable);
          this.updateWidth(columns, newX, sheet, true, 62, width, columnCount, 'ColumnWidth');
          this.restoreWidth(rows, rowCount, 20, sheet);
          break;
        }
        case 'vertical': {
          this.scaleFont(sheet, Math.min(newX, newY), columnCount, rowCount, dataTable);
          this.updateWidth(columns, scaleX, sheet, true, 62, width, columnCount, 'ColumnWidth');
          this.updateWidth(rows, newY, sheet, shouldUpdate, 20, height, rowCount, 'RowHeight');
          break;
        }
        case 'biAdaptive': {
          this.scaleFont(sheet, Math.min(newX, newY), columnCount, rowCount, dataTable);
          this.updateWidth(columns, newX, sheet, true, 62, width, columnCount, 'ColumnWidth');
          this.updateWidth(rows, newY, sheet, shouldUpdate, 20, height, rowCount, 'RowHeight');
          break;
        }
        case 'none': {
          this.updateWidth(columns, scaleX, sheet, true, 62, width, columnCount, 'ColumnWidth');
          this.restoreWidth(rows, rowCount, 20, sheet);
          break;
        }
        default: break;
      }
    } else {
      this.updateWidth(columns, scaleX, sheet, shouldUpdate, 62, width, columnCount, 'ColumnWidth');
    }
  }

  restoreWidth = (arr, count, initSize, sheet) => {
    const newArr = arr && arr.length === count ? arr : this.initSize(arr, count, initSize);
    _.map(newArr, (item, index) => {
      const { size } = item || { size: initSize };
      sheet.setRowHeight(index, size);
    });
  }

  updateWidth = (arr, scale, sheet, shouldUpdate, initSize, curSize, count, type) => {
    const newArr = arr && arr.length === count ? arr : this.initSize(arr, count, initSize);
    let total = 0;
    window.sheet = sheet;
    _.map(newArr, (item) => {
      const { size } = item || { size: initSize };
      const target = shouldUpdate ? Math.round(size * scale) : size;
      // 隐藏列宽度只修改，不记录
      if (_.get(item, 'visible') !== false) total += target;
    });
    let finalTotal = 0;
    _.map(newArr, (item, index) => {
      const { size } = item || { size: initSize };
      const isLastIndex = index === count - 1;
      let target = size;
      if (curSize >= total || shouldUpdate) {
        if (!shouldUpdate) {
          const gap = curSize / (total * scale);
          target = Math.floor(gap * size);
          finalTotal += target;
          if (isLastIndex) target += (curSize - (finalTotal * scale));
        } else {
          const gap = curSize / total;
          target = Math.floor(gap * Math.round(size * scale));
          finalTotal += target;
          if (isLastIndex) target += (curSize - finalTotal);
        }
      } else {
        target = shouldUpdate ? Math.round(size * scale) : size;
      }
      sheet[`set${type}`](index, target);
    });
  }

  scaleFont = (sheet, scale, columnCount, rowCount, dataTable) => {
    const newScale = this.generateFontScale(scale);
    for (let i = 0; i < rowCount; i += 1) {
      for (let j = 0; j < columnCount; j += 1) {
        if (sheet.getCell(i, j).value() !== null) {
          const style = sheet.getStyle(i, j);
          const font = _.get(dataTable[i][j], 'style.font', '11pt Calibri');
          const [, fontSize] = font.match(/(\d+)pt/);
          const newFont = Math.round(scale < 1 ? Math.max(fontSize * newScale, 8) : Math.min(fontSize * newScale, 72));
          style.font = font.replace(/\d+pt/, `${newFont}pt`);
          sheet.setStyle(i, j, style);
        }
      }
    }
  }

  generateFontScale = (scale) => {
    if (scale < 1) {
      return Math.min(1, scale + 0.3);
    } else {
      return Math.max(1, scale - 0.3);
    }
  }

  resetLastColumns = (sheet, initSize, type, count) => {
    let total = 0;
    const lastIndex = count - 1;
    for (let i = lastIndex; i >= 0; i -= 1) {
      const size = sheet[`get${type}`](i);
      total += size;
      if (total > initSize) {
        const diff = initSize - (total - size) - 1;
        const lastSize = sheet[`get${type}`](lastIndex);
        sheet[`set${type}`](lastIndex, lastSize + diff);
        break;
      }
    }
  }

  initSize = (arr, count, initSize) => {
    const sizeArray = [];
    for (let i = 0; i < count; i += 1) {
      const item = _.get(arr, i) || {};
      if (!item.size) item.size = initSize;
      sizeArray.push(item);
    }
    return sizeArray;
  }

  reRender = () => {
    this.report.reRender();
  }

  render() {
    const {
      actions = '',
      fillDataType = 'replace',
      scrollbarVisible = 'auto',
      runTimeEdit = false,
      runTimeShowSheet = true,
      backgroundColor = '#ffffff',
      cellAuthorityArr,
      sqlInfo = {},
      json = {}
    } = this.state.config || {};
    return (
      <div style={{ width: '100%', height: '100%' }}>
        <Reporter
          ref={(node) => { this.report = node; }}
          pageConfig={this.props.pageConfig}
          config={this.props.config}
          isPreview={this.props.isPreview}
          isDesign={this.props.isDesign}
          formData={this.props.formData}
          prePrint={this.props.prePrint}
          changePrintStatus={this.props.changePrintStatus}
          scrollbarVisible={scrollbarVisible}
          runTimeEdit={runTimeEdit}
          runTimeShowSheet={runTimeShowSheet}
          backgroundColor={backgroundColor}
          fillDataType={fillDataType}
          json={_.cloneDeep(json)}
          setJsonData={this.setJsonData}
          historyData={this.props.historyData}
          showOrHideModal={this.props.showOrHideModal}
          getDataTable={this.props.getDataTable}
          dataTableInfo={this.props.dataTableInfo}
          statisticInfo={this.props.statisticInfo}
          customServices={this.state.customServices}
          entityObjects={this.state.entityObjects}
          sqlInfo={sqlInfo}
          cellAuthorityArr={cellAuthorityArr}
          requestEnd={this.props.requestEnd}
          resetRequestState={this.props.resetRequestState}
          rtDataSource={this.dataSource}
          intl={this.props.intl}
          // 事件
          actions={actions}
          getActionHandle={this.props.getActionHandle}
        />
      </div>
    );
  }
}
