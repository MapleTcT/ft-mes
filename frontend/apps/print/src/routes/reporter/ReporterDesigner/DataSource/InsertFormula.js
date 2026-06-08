import React, { Component } from 'react';
import * as _ from 'lodash';
import { Row, Col, Select, Radio, InputNumber, Input, Switch, notification, Tooltip, Icon } from 'sup-ui';
import Modal from '../Modal/CommonModal';
// import config from 'root/config';
// import ObjectSelector from '@supos/object-selector/dist';
import StatisticTaskModal from './StatisticTaskModal';
import styles from './DataSource.less';
import messages from '../messages';
import * as Util from '../ReportUtil';

const { Group } = Radio;
const { Option, OptGroup } = Select;

export default class InsertFormula extends Component {
  constructor(props) {
    super(props);
    this.parseFormula(props, true);
  }

  componentWillReceiveProps(nextProp) {
    if (this.props.type === 'dataSourceInfo' && nextProp.formula !== this.props.formula) {
      this.parseFormula(nextProp);
    }
  }

  onRadioChange = (type, e) => {
    this.onStateChange(type, e.target.value);
  }

  onStateChange = (type, value) => {
    this.updateState({ [type]: value });
  }

  handleFillType = (e) => {
    const value = _.get(e, 'target.value');
    const { dataType } = this.state;
    if (value) {
      const stateObj = { fillType: e.target.value };
      const maxLength = this.getMaxLength(dataType, value);
      if (value === 'auto') {
        stateObj.dataLength = maxLength;
      }
      this.updateState(stateObj);
    }
  }

  updateState = (stateObj) => {
    this.setState(stateObj, () => {
      this.updateDataSourceFormula();
    });
  }

  updateDataSourceFormula = () => {
    if (this.props.type === 'dataSourceInfo') {
      this.updateFormula();
    }
  }

  changeInputNumber = (type, e) => {
    this.setState({ [type]: e });
  }

  changeInput = (type, e) => {
    this.setState({ [type]: e.target.value });
  }

  setStatisticObject = (item) => {
    if (!item || !_.keys(item).length) {
      notification.warning({ message: '请添加一条任务！' });
      return;
    }
    const { taskName, type, sourceItem } = item;
    this.setState({
      selectedObject: `${taskName}-${sourceItem}-${type}`
    }, () => {
      if (this.props.type === 'dataSourceInfo') {
        this.updateFormula();
      }
    });
  }

  setObject = (dataItem) => {
    const { intl } = this.props;
    if (!_.get(dataItem, 'selectedProp.propertyName', null)) {
      notification.warning({ message: intl.formatMessage(messages.ChooseAttribute) });
      return;
    }
    const allowType = ['INTEGER', 'DOUBLE', 'LONG', 'FLOAT', 'BOOLEAN', 'STRING', 'DATE', 'DATETIME', 'DECIMAL'];
    const primitiveType = _.get(dataItem, 'selectedProp.primitiveType');
    if (!allowType.includes(primitiveType)) {
      notification.warning({ message: intl.formatMessage(messages.Rule7) });
      return;
    }
    const { selectedInstance: { name: instanceName }, selectedProp: { propertyName: propName, namespace: propNS }, selectedTemplate: { name: tempName, namespace: tempNS } } = dataItem;
    const state = {
      selectedObject: `${tempNS}:${tempName}:${instanceName}.${propNS}:${propName}`,
      primitiveType
    };
    if (['BOOLEAN', 'STRING', 'DATE', 'DATETIME'].includes(primitiveType)) {
      state.aggrType = 'first';
    } else if (!this.state.aggrType) {
      state.aggrType = 'sum';
    }
    this.setState(state, () => {
      if (this.props.type === 'dataSourceInfo') {
        this.updateFormula();
      }
    });
  }

  getDefaultState = (props, type) => {
    const formulaTypeArr = (props.formulaType || '').split('/');
    const isMatched = formulaTypeArr.length && formulaTypeArr.includes(type);
    const dataType = isMatched ? type : formulaTypeArr[0] || type;

    return {
      obj: {
        objectSelectorVisible: false,
        selectedObject: undefined,
        primitiveType: undefined,
        dataType,
        fillType: 'auto',
        fixedIndex: 1,
        dataLength: 0,
        direction: 'V',
        groupValue: 1,
        group: 's',
        aggrType: 'sum',
        displaySelect: 'value',
        theme: 'null',
        canMerge: false,
        showHeader: false
      },
      isMatched
    };
  }

  getStateObject = (dataType, params) => {
    const [selectedObject, start, end, direction, ...tailArr] = (params && params.replace(/"/g, '').split(',')) || [];
    const { obj, isMatched } = this.getDefaultState(this.props, dataType);
    if (this.props.formulaType && !isMatched) return obj;

    const startIndex = start && _.isNumber(Number(start)) ? Number(start) : 0;
    const endIndex = end && _.isNumber(Number(end)) ? Number(end) : -1;
    if (startIndex === endIndex) {
      obj.fixedIndex = startIndex + 1;
      obj.fillType = 'fixed';
      obj.dataLength = 1;
    } else {
      obj.fillType = 'auto';
      obj.dataLength = (endIndex - startIndex) + 1;
    }
    if (direction) obj.direction = direction.trim();
    if (selectedObject) {
      if (obj.dataType === 'RTS') {
        obj.selectedObject = selectedObject;
      } else {
        let objArr = [];
        if (['ENT'].includes(obj.dataType)) {
          objArr = selectedObject.replace(/@#@/g, '@:@').split('@:@');
        } else {
          objArr = selectedObject.replace(/#/g, '.').split('.');
        }
        this.selectedOriObject = Util.getOriDataSource(selectedObject, dataType);
        if (['ENT'].includes(obj.dataType)) {
          obj.selectedObject = selectedObject;
          obj.primitiveType = null;
        } else if (objArr.length > 2 && ['STRING', 'BOOLEAN', 'DATE', 'DATETIME'].includes(objArr.slice(-1)[0])) {
          obj.selectedObject = objArr.slice(0, -1).join('.');
          [obj.primitiveType] = objArr.slice(-1);
        } else {
          obj.selectedObject = objArr.join('.');
          obj.primitiveType = null;
        }
      }
    }

    switch (obj.dataType) {
      case 'RT': {
        if (selectedObject && ~selectedObject.indexOf('_key_')) obj.dataType = 'HIS';
        break;
      }
      case 'HIS': {
        if (_.isArray(tailArr) && tailArr.length) {
          [, obj.groupValue, obj.group] = tailArr[0].trim().match(/(\d+)(s|m|h|d|w|M|y)/);
          if (tailArr[1]) obj.aggrType = tailArr[1].trim();
          if (tailArr[2]) obj.displaySelect = tailArr[2].trim();
        }
        break;
      }
      case 'CTS':
      case 'ENT':
      case 'DT':
      case 'SER': {
        const { sqlInfo } = this.props;
        const [tableName] = selectedObject.split('.');
        if (_.get(sqlInfo, tableName)) {
          const { primaryKeys } = sqlInfo[tableName];
          obj.primaryKeys = primaryKeys || [];
        }

        if (_.isArray(tailArr) && tailArr.length) {
          obj.showHeader = tailArr[0] ? (tailArr[0].trim() === 'undefined' ? true : tailArr[0].trim().toLocaleLowerCase() === 'true') : true;
          obj.canMerge = obj.showHeader;
          if (tailArr[1]) {
            const [, ...typeTransfer] = tailArr;
            obj.theme = tailArr[1].trim();
            obj.typeTransfer = typeTransfer.join(',').trim();
          }
        }
        break;
      }
      default: break;
    }

    if (obj.dataLength === 0) {
      obj.dataLength = this.getMaxLength(obj.dataType, obj.fillType);
    }
    return obj;
  }

  updateFormula = () => {
    const formula = this.generateFormula();
    if (formula) {
      if (['RTS'].includes(this.state.dataType)) {
        this.props.basicOperate({
          opt: 'generateFormula',
          options: {
            formula,
            selections: this.props.selections || null
          }
        });
      } else {
        this.props.basicOperate({
          opt: 'generateFormula',
          options: {
            selectedObject: this.selectedOriObject,
            formula,
            selections: this.props.selections || null
          }
        });
      }
    }
  }

  generateFormula = () => {
    if (this.props.type !== 'dataSourceInfo' && !this.state.selectedObject) {
      notification.warning({ message: this.props.intl.formatMessage(messages.AddDatasource) });
      return;
    }
    const { dataType } = this.state;
    let { dataLength, fixedIndex } = this.state;
    if (dataLength && dataLength < 0) dataLength = 0;
    if (fixedIndex && fixedIndex < 0) fixedIndex = 0;
    switch (dataType) {
      case 'RT': return this.getRTFormula();
      case 'CTS':
      case 'ENT':
      case 'DT':
      case 'SER': return this.getDTSerFormula(dataLength);
      case 'HIS': return this.getHisFormula(dataLength, fixedIndex);
      case 'RTS': return this.getRTSFormula(dataLength, fixedIndex);
      default: break;
    }
  }

  getRTSFormula = (dataLength, fixedIndex) => {
    const { selectedObject, fillType, direction } = this.state;
    let startIndex = 0;
    let endIndex = 0;
    const maxLength = this.getMaxLength('RTS', fillType) - 1;
    if (fillType === 'auto') {
      endIndex = !dataLength ? maxLength : dataLength - 1;
    } else if (fixedIndex) {
      startIndex = fixedIndex - 1;
      endIndex = fixedIndex - 1;
    }
    if (startIndex > maxLength) startIndex = maxLength;
    if (endIndex > maxLength) endIndex = maxLength;
    return `=RTS("${selectedObject}", ${startIndex}, ${endIndex}, "${direction}")`;
  }

  getRTFormula = () => {
    const { selectedObject, primitiveType } = this.state;
    if (['BOOLEAN', 'STRING', 'DATE', 'DATETIME'].includes(primitiveType)) {
      return `=RT("${selectedObject}#${primitiveType}")`;
    } else {
      return `=RT("${selectedObject}")`;
    }
  }

  getHisFormula = (dataLength, fixedIndex) => {
    const { selectedObject = '', primitiveType, fillType, direction, groupValue, group, aggrType, displaySelect = 'value' } = this.state;
    let startIndex = 0;
    let endIndex = 0;
    const maxLength = this.getMaxLength('HIS', fillType) - 1;
    if (fillType === 'auto') {
      endIndex = !dataLength ? maxLength : dataLength - 1;
    } else if (fixedIndex) {
      startIndex = fixedIndex - 1;
      endIndex = fixedIndex - 1;
    }
    if (startIndex > maxLength) startIndex = maxLength;
    if (endIndex > maxLength) endIndex = maxLength;

    if (/_key_/.test(selectedObject)) {
      return `=HIS("${selectedObject}", ${startIndex}, ${endIndex}, "${direction}")`;
    } else if (['BOOLEAN', 'STRING', 'DATE', 'DATETIME'].includes(primitiveType)) {
      return `=HIS("${selectedObject}#${primitiveType}", ${startIndex}, ${endIndex}, "${direction}", "${groupValue}${group}", "first", "${displaySelect}")`;
    } else {
      return `=HIS("${selectedObject}", ${startIndex}, ${endIndex}, "${direction}", "${groupValue}${group}", "${aggrType}", "${displaySelect}")`;
    }
  }

  getDTSerFormula = (dataLength) => {
    const { dataType, selectedObject, direction, canMerge, primaryKeys = [], showHeader, theme, typeTransfer } = this.state;
    const [tableName, prop] = selectedObject.split('.');
    const pkTypeTransfer = primaryKeys.includes(prop) ? { name: prop, typeTransfer } : {};
    this.props.updatePrimaryKeys(tableName, primaryKeys, pkTypeTransfer);

    const maxLength = this.getMaxLength(dataType) - 1;
    let endIndex = !dataLength ? maxLength : dataLength - 1;
    if (endIndex > maxLength) endIndex = maxLength;
    if (prop === '_table_') {
      return `=${dataType}("${selectedObject}", 0, ${endIndex}, "${direction}", ${showHeader}, "${theme}")`;
    } else if (typeTransfer && _.isString(typeTransfer)) {
      return `=${dataType}("${selectedObject}", 0, ${endIndex}, "${direction}", ${canMerge}, "${typeTransfer}")`;
    } else {
      return `=${dataType}("${selectedObject}", 0, ${endIndex}, "${direction}", ${canMerge})`;
    }
  }

  parseFormula = (props, isInit) => {
    const [, dataType = 'RT', params] = (props.formula && props.formula.match(/(RT|HIS|DT|SER|RTS|CTS|ENT)\((.*)\)$/)) || [];
    const stateObj = this.getStateObject(dataType, params);
    if (isInit) {
      this.state = stateObj;
    } else {
      this.setState(stateObj);
    }
  }

  showOrHideModal = (boolean) => {
    this.select.blur();
    this.setState({
      objectSelectorVisible: boolean,
      statisticTaskVisible: boolean
    });
  }

  updateSelect = (objects) => {
    this.selectedOriObject = objects;
  }

  handleOk = (objects) => {
    this.updateSelect(objects);
    this.setObject(objects);
    this.showOrHideModal(false);
  }

  getMaxLength = (dataType, fillType) => {
    let maxLength = 500;
    switch (dataType) {
      case 'DT':
        maxLength = 200;
        break;
      case 'RTS':
        maxLength = 720;
        break;
      case 'HIS':
        maxLength = fillType === 'auto' ? 500 : 0;
        break;
      default:
        break;
    }
    return maxLength;
  }

  renderMaxLengthMsg = () => {
    const { intl, type } = this.props;
    const { dataType, fillType } = this.state;
    const dataSourceInfo = type === 'dataSourceInfo';
    const maxLength = this.getMaxLength(dataType, fillType);
    const msg = `${intl.formatMessage(messages.limitedValue)} 1-${maxLength}`;
    let [offset, span] = ['SER', 'DT', 'CTS', 'ENT'].includes(dataType) ? [6, 18] : [8, 16];
    if (dataSourceInfo) {
      offset += 1;
      span -= 1;
    }

    return (
      <div className={styles.tipMsg}>
        <Row className={dataType === 'RT' || fillType !== 'auto' ? styles.disabled : ''}>
          <Col offset={offset} span={span}>{msg}</Col>
        </Row>
      </div>
    );
  }

  getDTDataSource = (object) => {
    return _.filter(_.get(this.props.allDataSource[object], 'list', []), (item) => !['_key_', '_table_'].includes(item)) || [];
  }

  renderTablePrimaryKeys = (object) => {
    const { dataType, selectedObject, primaryKeys = [] } = this.state;
    const primaryKeyList = primaryKeys && primaryKeys.length >= 0 ? primaryKeys : [];
    const { intl } = this.props;
    const data = this.getDTDataSource(object);
    if (dataType === 'DT' && !['_key_'].includes(selectedObject)) {
      return (
        <Row>
          <Col span={7}>
            {intl.formatMessage(messages.primaryKey)}
            :
          </Col>
          <Col span={17}>
            <Select
              allowClear
              autoFocus
              mode="tags"
              size="small"
              maxTagTextLength={10}
              maxTagCount={1}
              maxTagPlaceholder={<span title={primaryKeyList.slice(1).join(', ')}>...</span>}
              value={primaryKeyList}
              onChange={this.onStateChange.bind(this, 'primaryKeys')}
            >
              {_.map(data, (item, index) => <Option value={item} key={index}>{item}</Option>)}
            </Select>
          </Col>
        </Row>
      );
    } else {
      return null;
    }
  }

  renderTableTheme = (prop) => {
    if (prop !== '_table_') return;
    const { intl } = this.props;
    return (
      <Row>
        <Col span={7}>
          {intl.formatMessage(messages.tableTheme)}
          :
        </Col>
        <Col span={17}>
          <Select
            value={this.state.theme}
            onChange={this.onStateChange.bind(this, 'theme')}
            size="small"
          >
            <OptGroup label={intl.formatMessage(messages.noTheme)}>
              <Option value="null">{intl.formatMessage(messages.none)}</Option>
            </OptGroup>
            {
              _.map(['light', 'medium', 'dark'], (item) => {
                return (
                  <OptGroup label={item} key={item}>
                    {
                      _.map([1, 2, 3, 7], (id) => {
                        return <Option value={`${item}${id}`} key={`${item}${id}`}>{`${item}${id}`}</Option>;
                      })
                    }
                  </OptGroup>
                );
              })
            }
          </Select>
        </Col>
      </Row>
    );
  }

  renderTableColumns = (object, prop) => {
    const { selectedObject, dataType } = this.state;
    if (prop === '_table_' || dataType === 'ENT') return;
    const { intl, allDataSource } = this.props;
    const dataList = _.get(allDataSource[object], 'list', []);
    const filteredDataSource = _.filter(dataList, (item) => item.propertyName !== '_table_');
    return (
      <Row>
        <Col span={7}>
          {intl.formatMessage(messages.DataSource)}
          :
        </Col>
        <Col span={17}>
          <Select
            ref={(node) => { this.select = node; }}
            value={selectedObject}
            onChange={this.onStateChange.bind(this, 'selectedObject')}
            size="small"
          >
            {
              _.map(filteredDataSource, (item, index) => {
                switch (dataType) {
                  case 'DT': return <Option value={`${object}.${item}`} key={index}>{item}</Option>;
                  case 'CTS': return <Option value={`${object}.${item.propertyCode}`} key={index}>{item.propertyName}</Option>;
                  case 'SER': return <Option value={`${object}.${item.propertyName}`} key={index}>{item.propertyName}</Option>;
                  default: break;
                }
              })
            }
          </Select>
        </Col>
      </Row>
    );
  }

  renderDataTableConfig = () => {
    const { selectedObject, dataType, dataLength, direction, fillType } = this.state;
    if (!selectedObject) return null;
    const { intl } = this.props;
    const [, object, prop] = selectedObject.match(/(.+)\.(.+)$/) || [];
    const maxLength = this.getMaxLength(dataType, fillType);

    return (
      <div className={styles.insertFormulaSmall}>
        {this.renderTableTheme(prop)}
        {this.renderTableColumns(object, prop)}
        {this.renderTablePrimaryKeys(object)}
        <Row>
          <Col span={7}>
            {intl.formatMessage(messages.fillOrien)}
            :
          </Col>
          <Col span={17}>
            <Select
              value={direction}
              onChange={this.onStateChange.bind(this, 'direction')}
              size="small"
            >
              <Option value="H">{intl.formatMessage(messages.HorizontalFill)}</Option>
              <Option value="V">{intl.formatMessage(messages.VerticalFill)}</Option>
            </Select>
          </Col>
        </Row>
        <Row>
          <Col span={7}>
            {intl.formatMessage(messages.MaxNum)}
            :
          </Col>
          <Col span={17}>
            <InputNumber
              min={0}
              max={maxLength}
              precision={0}
              value={dataLength}
              size="small"
              onChange={this.changeInputNumber.bind(this, 'dataLength')}
              onBlur={this.updateDataSourceFormula}
            />
          </Col>
        </Row>
        {this.renderMaxLengthMsg()}
        <Row />
        {this.renderTableMergeOrHeader(prop)}
        {this.renderTypeTransfer()}
      </div>
    );
  }

  renderTypeTransfer = () => {
    const { intl } = this.props;
    const { dataType, selectedObject, typeTransfer = '' } = this.state;
    const prop = selectedObject.split('.')[1];
    if (dataType === 'DT' && !['_table_', '_key_'].includes(prop)) {
      return (
        <div>
          <Row>
            <Col span={7}>
              {intl.formatMessage(messages.typeTransfer)}
              :
            </Col>
            <Col span={17} title={typeTransfer}>
              <Input
                style={{ width: 180 }}
                size="small"
                value={typeTransfer}
                onChange={this.changeInput.bind(this, 'typeTransfer')}
                onBlur={this.updateDataSourceFormula}
              />
            </Col>
          </Row>
          <Row className={styles.tipMsg}>
            <Col offset={7} span={17}>
              {intl.formatMessage(messages.typeTransferTip)}
              <Tooltip title={intl.formatMessage(messages.typeTransferExample)}>
                <Icon type="question-circle" style={{ paddingLeft: '5px' }} />
              </Tooltip>
            </Col>
          </Row>
        </div>
      );
    } else {
      return null;
    }
  }

  renderTableMergeOrHeader = (prop) => {
    const { intl } = this.props;
    const { showHeader, canMerge } = this.state;
    if (prop === '_table_') {
      return (
        <Row>
          <Col span={7}>
            {intl.formatMessage(messages.showHeader)}
            :
          </Col>
          <Col span={17}>
            <Switch checked={showHeader} onChange={this.onStateChange.bind(this, 'showHeader')} />
          </Col>
        </Row>
      );
    } else {
      return (
        <Row>
          <Col span={7}>
            {intl.formatMessage(messages.FillMerge)}
            :
          </Col>
          <Col span={17}>
            <Switch checked={canMerge} onChange={this.onStateChange.bind(this, 'canMerge')} />
          </Col>
        </Row>
      );
    }
  }

  renderDataTypeOptions() {
    const { dataType, selectedObject } = this.state;
    const { intl } = this.props;
    if (['HIS', 'RT'].includes(dataType) && selectedObject && ~selectedObject.indexOf('_key_')) {
      return (
        <Radio value="HIS">{intl.formatMessage(messages.HistoryData)}</Radio>
      );
    } else {
      return (
        <React.Fragment>
          <Radio value="RT">{intl.formatMessage(messages.RealData)}</Radio>
          <Radio value="HIS">{intl.formatMessage(messages.HistoryData)}</Radio>
        </React.Fragment>
      );
    }
  }

  renderOptions() {
    const { dataType, selectedObject } = this.state;
    if (['HIS', 'RT'].includes(dataType) && selectedObject && ~selectedObject.indexOf('_key_')) {
      return null;
    } else {
      return (
        <React.Fragment>
          {this.renderGroupOptions()}
          {this.renderAggrType()}
        </React.Fragment>
      );
    }
  }

  renderGroupOptions() {
    const { intl, type } = this.props;
    const { dataType, groupValue = 1, group = 's' } = this.state;
    const dataSourceInfo = type === 'dataSourceInfo';
    return (
      <Row>
        <Col span={dataSourceInfo ? 7 : 6}>
          {intl.formatMessage(messages.Time)}
          :
        </Col>
        <Col span={dataSourceInfo ? 7 : 9}>
          <InputNumber
            style={{ width: '100%' }}
            size={dataSourceInfo ? 'small' : 'default'}
            min={1}
            precision={0}
            disabled={dataType === 'RT'}
            value={groupValue}
            onChange={this.changeInputNumber.bind(this, 'groupValue')}
            onBlur={this.updateDataSourceFormula}
          />
        </Col>
        <Col offset={1} span={8}>
          <Select
            style={{ width: dataSourceInfo ? '97%' : '103%' }}
            value={group}
            onChange={this.onStateChange.bind(this, 'group')}
            disabled={dataType === 'RT'}
            size={dataSourceInfo ? 'small' : 'default'}
          >
            <Option value="s">{intl.formatMessage(messages.Second)}</Option>
            <Option value="m">{intl.formatMessage(messages.Minute)}</Option>
            <Option value="h">{intl.formatMessage(messages.Hour)}</Option>
            <Option value="d">{intl.formatMessage(messages.Day)}</Option>
            <Option value="w">{intl.formatMessage(messages.Week)}</Option>
            <Option value="M">{intl.formatMessage(messages.Month)}</Option>
            <Option value="y">{intl.formatMessage(messages.Year)}</Option>
          </Select>
        </Col>
      </Row>
    );
  }

  renderAggrType() {
    const { intl, type } = this.props;
    const dataSourceInfo = type === 'dataSourceInfo';
    const [labelSpan, valSpan, size] = dataSourceInfo ? [7, 17, 'small'] : [6, 18, 'default'];
    return (
      <Row>
        <Col span={labelSpan}>
          {intl.formatMessage(messages.Aggregate)}
          :
        </Col>
        <Col span={valSpan}>
          {this.renderAggrTypeOptions(size)}
        </Col>
      </Row>
    );
  }

  renderAggrTypeOptions(size) {
    const { intl } = this.props;
    if (['STRING', 'BOOLEAN', 'DATE', 'DATETIME'].includes(this.state.primitiveType)) {
      return (
        <Select
          value="first"
          onChange={this.onStateChange.bind(this, 'aggrType')}
          disabled={this.state.dataType === 'RT'}
          size={size}
        >
          <Option value="first">{intl.formatMessage(messages.Moment)}</Option>
        </Select>
      );
    } else {
      return (
        <Select
          value={this.state.aggrType}
          onChange={this.onStateChange.bind(this, 'aggrType')}
          disabled={this.state.dataType === 'RT'}
          size={size}
        >
          <Option value="mean">{intl.formatMessage(messages.Avg)}</Option>
          <Option value="max">{intl.formatMessage(messages.Max)}</Option>
          <Option value="min">{intl.formatMessage(messages.Min)}</Option>
          <Option value="sum">{intl.formatMessage(messages.Sum)}</Option>
          <Option value="first">{intl.formatMessage(messages.Moment)}</Option>
        </Select>
      );
    }
  }

  renderStatisticTask() {
    const { intl } = this.props;
    const { selectedObject = '', statisticTaskVisible = false } = this.state;
    const [taskName, object, statisticType] = selectedObject.split('-');
    if (statisticTaskVisible) {
      return (
        <StatisticTaskModal
          selectedTaskName={taskName}
          selectedSource={object}
          selectedType={statisticType}
          setObject={this.setStatisticObject}
          showOrHideModal={this.showOrHideModal}
          intl={intl}
        />
      );
    } else {
      return null;
    }
  }

  renderObjectSelector() {
    const { objectSelectorVisible } = this.state;
    if (objectSelectorVisible) {
      // return (
        // <ObjectSelectorModal
        //   visible={objectSelectorVisible}
        //   handleOk={this.handleOk}
        //   handleCancel={this.showOrHideModal.bind(this, false)}
        //   tabs={['instance']}
        //   subTabs={['property']}
        //   selectedObject={this.selectedOriObject}
        //   selectedTab="instance"
        //   selectedSubTab="property"
        //   domain={config.domainObjectSelect}
        //   namespace={this.props.appId}
        //   scope={7}
        // />
      // );
    } else {
      return null;
    }
  }

  renderDataSource() {
    const { type, intl } = this.props;
    const dataSourceInfo = type === 'dataSourceInfo';
    const [labelSpan, valSpan, size] = dataSourceInfo ? [7, 17, 'small'] : [6, 18, 'default'];
    const { selectedObject, dataType } = this.state;
    let dataSource = '';
    if (selectedObject) {
      const [, , instance, , propName] = selectedObject.replace(/\./g, ':').split(':');
      dataSource = `${instance}.${propName}`;
    }
    return (
      <Row>
        <Col span={labelSpan}>
          {intl.formatMessage(messages.DataSource)}
          :
        </Col>
        <Col span={valSpan}>
          <Select
            ref={(node) => { this.select = node; }}
            value={dataSource}
            onFocus={this.showOrHideModal.bind(this, true)}
            size={size}
          />
          {dataType === 'RTS' ? this.renderStatisticTask() : this.renderObjectSelector()}
        </Col>
      </Row>
    );
  }

  renderDataType() {
    const dataSourceInfo = this.props.type === 'dataSourceInfo';
    const [labelSpan, valSpan] = dataSourceInfo ? [7, 17] : [6, 18];
    const { dataType } = this.state;
    const { intl } = this.props;
    return (
      <Row>
        <Col span={labelSpan}>
          {intl.formatMessage(messages.DataType)}
          :
        </Col>
        <Col span={valSpan}>
          <Group onChange={this.onRadioChange.bind(this, 'dataType')} value={dataType}>
            {this.renderDataTypeOptions()}
          </Group>
        </Col>
      </Row>
    );
  }

  renderAutoFill() {
    const dataSourceInfo = this.props.type === 'dataSourceInfo';
    const [offsetLabelSpan, offsetValSpan, size] = dataSourceInfo ? [8, 12, 'small'] : [6, 15, 'default'];
    const { dataType, fillType } = this.state;
    const { intl } = this.props;
    const maxLength = this.getMaxLength(dataType, fillType);
    return fillType === 'auto' || !dataSourceInfo ? (
      <React.Fragment>
        <Row>
          <Col span={offsetLabelSpan} offset={2}>{intl.formatMessage(messages.fillOrien)}:</Col>
          <Col span={offsetValSpan}>
            <Select
              value={this.state.direction}
              onChange={this.onStateChange.bind(this, 'direction')}
              disabled={dataType === 'RT' || fillType !== 'auto'}
              size={size}
            >
              <Option value="H">{intl.formatMessage(messages.HorizontalFill)}</Option>
              <Option value="V">{intl.formatMessage(messages.VerticalFill)}</Option>
            </Select>
          </Col>
        </Row>
        <Row>
          <Col span={offsetLabelSpan} offset={2}>{intl.formatMessage(messages.MaxNum)}:</Col>
          <Col span={offsetValSpan}>
            <InputNumber
              min={0}
              max={maxLength}
              precision={0}
              value={this.state.dataLength}
              disabled={dataType === 'RT' || fillType !== 'auto'}
              size={size}
              onChange={this.changeInputNumber.bind(this, 'dataLength')}
              onBlur={this.updateDataSourceFormula}
            />
          </Col>
        </Row>
        {this.renderMaxLengthMsg()}
      </React.Fragment>
    ) : null;
  }

  renderFixedFill() {
    const dataSourceInfo = this.props.type === 'dataSourceInfo';
    const [offsetLabelSpan, offsetValSpan, size] = dataSourceInfo ? [8, 12, 'small'] : [6, 15, 'default'];
    const { dataType, fillType } = this.state;
    const { intl } = this.props;
    const maxLength = this.getMaxLength(dataType, fillType);
    return fillType === 'fixed' || !dataSourceInfo ? (
      <Row>
        <Col span={offsetLabelSpan} offset={2}>
          {intl.formatMessage(messages.Index)}
          :
        </Col>
        <Col span={offsetValSpan}>
          <InputNumber
            min={1}
            max={maxLength}
            precision={0}
            value={this.state.fixedIndex}
            disabled={dataType === 'RT' || fillType !== 'fixed'}
            size={size}
            onChange={this.changeInputNumber.bind(this, 'fixedIndex')}
            onBlur={this.updateDataSourceFormula}
          />
        </Col>
      </Row>
    ) : null;
  }

  renderSmallFill() {
    const { intl } = this.props;
    return (
      <React.Fragment>
        <Row>
          <Col span={8}>填充类型:</Col>
          <Col span={9}>
            <Radio value="auto">{intl.formatMessage(messages.AutoFill)}</Radio>
          </Col>
          <Col span={7}>
            <Radio value="fixed">{intl.formatMessage(messages.Fixed)}</Radio>
          </Col>
        </Row>
        {this.renderAutoFill()}
        {this.renderFixedFill()}
      </React.Fragment>
    );
  }

  renderDefaultFill() {
    const { intl } = this.props;
    return (
      <React.Fragment>
        <Row>
          <Radio value="auto">{intl.formatMessage(messages.AutoFill)}</Radio>
        </Row>
        {this.renderAutoFill()}
        <Row>
          <Radio value="fixed">{intl.formatMessage(messages.Fixed)}</Radio>
        </Row>
        {this.renderFixedFill()}
      </React.Fragment>
    );
  }

  renderFill() {
    const dataSourceInfo = this.props.type === 'dataSourceInfo';
    const size = dataSourceInfo ? 'small' : 'default';
    const { dataType, fillType } = this.state;
    return (
      <Row className={styles.fillType}>
        <Group
          onChange={this.handleFillType}
          value={fillType}
          disabled={dataType === 'RT'}
          size={size}
        >
          {dataSourceInfo ? this.renderSmallFill() : this.renderDefaultFill()}
        </Group>
      </Row>
    );
  }

  renderDisplaySelect() {
    const { intl, type } = this.props;
    const dataSourceInfo = type === 'dataSourceInfo';
    const [labelSpan, valSpan, size] = dataSourceInfo ? [7, 17, 'small'] : [6, 18, 'default'];
    const { displaySelect = 'value', dataType, selectedObject = '' } = this.state;
    const disabled = dataType !== 'HIS' || /_key_/.test(selectedObject);

    return (
      <Row>
        <Col span={labelSpan}>
          {intl.formatMessage(messages.showType)}
          :
        </Col>
        <Col span={valSpan}>
          <Group
            onChange={this.onRadioChange.bind(this, 'displaySelect')}
            value={displaySelect}
            size={size}
            disabled={disabled}
          >
            <Radio value="time">{intl.formatMessage(messages.showTime)}</Radio>
            <Radio value="value">{intl.formatMessage(messages.showValue)}</Radio>
          </Group>
        </Col>
      </Row>
    );
  }

  renderHisConfig() {
    const dataSourceInfo = this.props.type === 'dataSourceInfo';
    return (
      <div className={dataSourceInfo ? styles.insertFormulaSmall : styles.insertFormula}>
        {this.renderDataSource()}
        {this.renderDataType()}
        {this.renderDisplaySelect()}
        {this.renderOptions()}
        {this.renderFill()}
      </div>
    );
  }

  renderStatisticsConfig() {
    const dataSourceInfo = this.props.type === 'dataSourceInfo';
    return (
      <div className={dataSourceInfo ? styles.insertFormulaSmall : styles.insertFormula}>
        {this.renderDataSource()}
        {this.renderFill()}
      </div>
    );
  }

  render() {
    const { dataType } = this.state;
    if (['RTS'].includes(dataType)) {
      return this.renderStatisticsConfig();
    } else if (['DT', 'SER', 'CTS', 'ENT'].includes(dataType)) {
      return this.renderDataTableConfig();
    } else {
      return this.renderHisConfig();
    }
  }
}
