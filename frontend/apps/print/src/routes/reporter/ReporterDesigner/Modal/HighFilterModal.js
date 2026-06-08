import React, { Component } from 'react';
import { Radio, Icon, Menu, Checkbox, Dropdown, Row, Col, InputNumber, DatePicker, notification, Input } from 'sup-ui';
import moment from 'moment';
import ModalDelete from './ModalDelete';
import Modal from './CommonModal';
import * as Util from '../ReportUtil.js';
import styles from '../Reporter.less';
import messages from '../messages';
import DataTree from '../DataTree';

export default class RowColSettingModal extends Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.state = {
      optionalCol: { icon: 'iconColName', key: 'ColName', name: intl.formatMessage(messages.colName) }, // ColName列名， ColSerial列序号
      colName: {},
      parentPaneChecked: _.get(props, 'data.paneFilter', true),
      optionalType: 'custom',
      operatorType: {},
      valType: {
        key: 'String',
        String: '',
        Date: moment().format('YYYY-MM-DD HH:mm:ss'),
        Col: 'A|1',
        Data: '',
        Boolean: true
      },
      andOrVal: 'and',
      nodesTree: _.get(props, 'data.list', []) || [],
      showModalDelete: false
    };
    this.initDataList();
  }

  componentDidMount() {

  }

  initDataList = () => {
    const { intl } = this.props;
    this.optionalColList = [
      { icon: 'iconColName', key: 'ColName', name: intl.formatMessage(messages.colName) },
      { icon: 'iconColSerial', key: 'ColSerial', name: intl.formatMessage(messages.colSerial) }
    ];

    this.initColNameList();
  }

  initColNameList = () => {
    const { dataSource, curFormula, sqlInfo } = this.props;
    const [, , params] = (curFormula && curFormula.match(/(RT|HIS|DT|SER|RTS)\((.*)\)$/)) || [];
    const [selectedObject] = (params && params.replace(/"/g, '').split(',')) || [];
    if (selectedObject && selectedObject.split('.').length >= 2) {
      const [, obj] = selectedObject.match(/(.+)\.(.+)$/);
      this.colNameList = [];
      this.dataSource = [];
      this.dataInstance = [];
      let dataIndex = 0;
      _.map(dataSource, (data, key) => {
        let index = 0;
        const instances = [];
        const dataList = _.get(data, 'list', []);
        _.forEach(dataList, (item) => {
          let properName = '';
          if (sqlInfo[key]) {
            properName = item;
          } else {
            properName = item.propertyName;
          }
          if (['_table_', '_key_'].includes(properName)) return;
          instances.push({ name: properName, key: `${dataIndex}|${index}` });
          index += 1;
          if (obj !== key) return;
          this.colNameList.push({ name: properName, key: `${index}` });
        });
        let [instance, propName] = [];
        const arr = key.split(':');
        let newKey = key;
        if (/服务$/.test(key)) {
          if (arr.length === 4) {
            [, , , propName] = arr;
            newKey = propName;
          } else {
            [, , instance, , propName] = arr;
            newKey = `${instance}:${propName}`;
          }
        } else if (sqlInfo[key]) {
          newKey = key;
        } else {
          [, , instance] = arr;
          newKey = instance;
        }
        this.dataSource.push({ name: newKey, key: `${dataIndex}`, instance: instances });
        dataIndex += 1;
      });
    }
  }

  initOperatorTypeList = (valType = '') => {
    const { intl } = this.props;
    let tmpList = [];
    if (!valType || !valType.key) return;
    const groupArr1 = [
      { key: 'equal', name: intl.formatMessage(messages.equal) },
      { key: 'unequal', name: intl.formatMessage(messages.unequal) }
    ];
    const groupArr2 = [
      { key: 'greater', name: intl.formatMessage(messages.greater) },
      { key: 'greaterOrEqual', name: intl.formatMessage(messages.greaterOrEqual) },
      { key: 'less', name: intl.formatMessage(messages.less) },
      { key: 'lessOrEqual', name: intl.formatMessage(messages.lessOrEqual) }
    ];
    const groupArr3 = [
      { key: 'begin', name: intl.formatMessage(messages.begin) },
      { key: 'noBegin', name: intl.formatMessage(messages.noBegin) },
      { key: 'end', name: intl.formatMessage(messages.end) },
      { key: 'noEnd', name: intl.formatMessage(messages.noEnd) },
      { key: 'include', name: intl.formatMessage(messages.include) },
      { key: 'noInclude', name: intl.formatMessage(messages.noInclude) },
      { key: 'contained', name: intl.formatMessage(messages.contained) },
      { key: 'noContained', name: intl.formatMessage(messages.noContained) }
    ];
    tmpList = tmpList.concat(groupArr1);
    if (['String', 'Int', 'Float', 'Date', 'Col', 'Data'].includes(valType.key)) tmpList = tmpList.concat(groupArr2);
    if (['String', 'Col', 'Data'].includes(valType.key)) tmpList = tmpList.concat(groupArr3);
    this.operatorTypeList = tmpList;
  }

  initValTypeList = (operatorType = '') => {
    const { intl } = this.props;
    let tmpList = [];
    if (!operatorType) return;
    const groupArr1 = [
      { icon: 'iconTypeString', key: 'String', name: intl.formatMessage(messages.typeString) }
    ];
    const groupArr2 = [
      { icon: 'iconTypeInt', key: 'Int', name: intl.formatMessage(messages.typeInt) },
      { icon: 'iconTypeFloat', key: 'Float', name: intl.formatMessage(messages.typeFloat) }
    ];
    const groupArr3 = [
      { icon: 'iconTypeBoolean', key: 'Boolean', name: intl.formatMessage(messages.typeBoolean) }
    ];
    const groupArr4 = [
      { icon: 'iconTypeDate', key: 'Date', name: intl.formatMessage(messages.typeDate) }
    ];
    const groupArr5 = [
      { icon: 'iconTypeCol', key: 'Col', name: intl.formatMessage(messages.typeCol) },
      { icon: 'iconTypeData', key: 'Data', name: intl.formatMessage(messages.typeData) }
    ];
    tmpList = tmpList.concat(groupArr1);
    if (['equal', 'unequal', 'greater', 'greaterOrEqual', 'less', 'lessOrEqual'].includes(operatorType.key) || !operatorType.key) tmpList = tmpList.concat(groupArr2);
    if (['equal', 'unequal'].includes(operatorType.key) || !operatorType.key) tmpList = tmpList.concat(groupArr3);
    if (['equal', 'unequal', 'greater', 'greaterOrEqual', 'less', 'lessOrEqual'].includes(operatorType.key) || !operatorType.key) tmpList = tmpList.concat(groupArr4);
    tmpList = tmpList.concat(groupArr5);
    this.valTypeList = tmpList;
  }

  handleOk = () => {
    const { parentPaneChecked } = this.state;
    this.props.basicOperate({ opt: 'highFilter', options: { value: { list: this.state.nodesTree, paneFilter: parentPaneChecked } } });

    this.handleCancal();
  }

  handleCancal = () => {
    this.props.showOrHideModal({ highFilterVisible: false });
  }

  resetTreeNodes(treeNodes, changeNode, onlyMove) {
    let hasRepetition = false;
    const reloopTreeNodes = (nodes, parentKey = '') => {
      let parsentTitle = '';
      let lastConditionList = _.drop(_.cloneDeep(nodes)) || [];
      const nodeList = _.map(nodes, (node, idx) => {
        let tmpNode = node;
        let curCondition = null;
        if (changeNode && changeNode.key === node.key && !onlyMove) {
          tmpNode = changeNode;
        }
        if (parentKey === '') {
          tmpNode.key = `${idx}`;
        } else {
          tmpNode.key = `${parentKey}-${idx}`;
        }
        // 重置选中值
        if (changeNode && changeNode.key === node.key) {
          this.selectedKeys = [tmpNode.key];
        }
        const { data } = tmpNode;
        const optionVal = data.optionalCol.type.key === 'ColName' ? data.optionalCol.val.name : data.optionalCol.val.key;
        let val = data.valType[`${data.valType.key}`];
        const showQuot = data.valType.key === 'String' ? '"' : '';
        if (data.valType.key === 'Col' && val) val = val.split('|').join('');
        tmpNode.title = optionVal ? `(${data.optionalCol.type.name}:${optionVal}) ${data.operator.name} ${showQuot}${val}${showQuot}` : '()';
        curCondition = tmpNode;
        if (idx !== 0) {
          tmpNode.title = `${data.andOr} ${tmpNode.title}`;
        }
        if (tmpNode.children.length > 0) {
          const obj = reloopTreeNodes(tmpNode.children, tmpNode.key);
          tmpNode.children = obj.list;
          tmpNode.title = `${idx !== 0 ? data.andOr : ''} (${obj.title})`;
        }
        parsentTitle += `${tmpNode.title} `;
        // 判断是否有重复条件
        _.forEach(lastConditionList, ((item) => {
          if (item && curCondition) {
            const lastData = _.get(item, 'data');
            const curData = _.get(curCondition, 'data');
            if ((_.get(lastData, 'operator.key') === _.get(curData, 'operator.key'))
              && (_.get(lastData, 'optionalCol.type.key') === _.get(curData, 'optionalCol.type.key'))
              && (_.get(lastData, 'optionalCol.val.name') === _.get(curData, 'optionalCol.val.name'))
              && (_.get(lastData, 'valType.key') === _.get(curData, 'valType.key'))
              && (_.get(lastData, 'valType')[`${_.get(lastData, 'valType.key')}`] === _.get(curData, 'valType')[`${_.get(curData, 'valType.key')}`])) {
              hasRepetition = true;
            }
          }
        }));
        lastConditionList = _.drop(lastConditionList);
        return tmpNode;
      });
      return { list: nodeList, title: parsentTitle };
    };
    const nodesTreeList = reloopTreeNodes(treeNodes).list;
    if (nodesTreeList.length === 0) this.selectedKeys = [];
    if (hasRepetition) {
      const { intl } = this.props;
      notification.warning({ message: intl.formatMessage(messages.conditionRepetition) });
      return false;
    }
    this.setState({
      nodesTree: nodesTreeList
    });
    return true;
  }

  recursionList = (list, keys, newList) => {
    return _.map(list, (item) => {
      if (item.key === _.take(keys, keys.length - 1).join('-')) {
        item.children = newList;
        return item;
      } else {
        item.children = this.recursionList(item.children, keys, newList);
        return item;
      }
    });
  }

  moveTreeIndex = (keys, type = 'up') => {
    const { nodesTree } = this.state;
    const nodesTreeList = _.cloneDeep(nodesTree);
    let changeNode = null;
    _.forEach(keys, (key) => {
      const ks = key.split('-');
      let list = nodesTreeList;
      _.forEach(ks, (item, idx) => {
        if (idx === ks.length - 1) {
          const index = parseInt(item, 10);
          if (type === 'up') {
            const targetIndex = index - 1;
            list.splice(targetIndex, 0, list[index]);
            list.splice(index + 1, 1);
            changeNode = list[targetIndex];
          } else {
            const targetIndex = index + 1;
            list.splice(targetIndex + 1, 0, list[index]);
            list.splice(index, 1);
            changeNode = list[targetIndex];
          }
        } else {
          list = list[parseInt(item, 10)].children;
        }
      });
    });
    return { list: nodesTreeList, changeNode };
  }

  /**
   * DataTree回调事件
   */
  treeSelectChanged = (keys, nodes) => {
    if (!nodes.length) return;
    const { operator, valType, optionalCol, andOr } = nodes[0].data;
    this.setState({
      operatorType: operator,
      valType,
      optionalCol: optionalCol.type,
      colName: optionalCol.val,
      andOrVal: andOr
    });
    if (valType.key === 'Data' && valType[`${valType.key}`] && valType[`${valType.key}`].split('.').length >= 2) {
      const [, dataSource] = valType[`${valType.key}`].match(/(.+)\.(.+)$/);
      _.forEach(this.dataSource, (data) => {
        if (data.name === dataSource) {
          this.dataInstance = data.instance;
        }
      });
    }
    this.selectedKeys = keys;
    // TODO 待完善
  }

  // 新增节点方法
  addTreeNodeAction = (selectKey) => {
    const { nodesTree, valType, optionalCol, operatorType, colName, andOrVal } = this.state;
    const nodesTreeList = _.cloneDeep(nodesTree);
    const { intl } = this.props;
    // 判空
    if (this.conditionIsEmpty()) {
      notification.warning({ message: intl.formatMessage(messages.noCondition) });
      return;
    }
    const node = {
      title: '',
      key: '',
      children: [],
      data: {
        andOr: andOrVal,
        optionalCol: {
          type: optionalCol,
          val: colName
        },
        operator: operatorType,
        valType: _.cloneDeep(valType)
      }
    };
    if (selectKey === '' || selectKey.split('-').length === 1) {
      if (nodesTreeList.length === 0) {
        node.key = '0';
      } else {
        const keys = nodesTreeList[nodesTreeList.length - 1].key.split('-');
        let newKey = '';
        _.forEach(keys, (item, idx) => {
          newKey = idx === keys.length - 1 ? `${newKey}${parseInt(item, 10) + 1}` : `${newKey}${item}-`;
        });
        node.key = newKey;
      }
      nodesTreeList.push(node);
      this.resetTreeNodes(nodesTreeList, node);
    } else {
      const keys = selectKey.split('-');
      let newKey = '';
      let list = _.cloneDeep(nodesTreeList);
      let curNode = null;
      _.forEach(keys, (item, idx) => {
        if (idx === keys.length - 1) {
          const ks = list[list.length - 1].key.split('-');
          newKey = `${newKey}${parseInt(ks[ks.length - 1], 10) + 1}`;
          node.key = newKey;
          curNode = node;
          list.push(node);
        } else {
          list = list[parseInt(item, 10)].children;
          newKey = `${newKey}${item}-`;
        }
      });
      const tmpList = this.recursionList(nodesTreeList, keys, list);
      this.resetTreeNodes(tmpList, curNode);
    }
  }

  // 编辑节点方法
  editTreeNodeAction = (key, node) => {
    const { nodesTree, andOrVal } = this.state;
    let { valType, optionalCol, operatorType, colName } = this.state;
    const { intl } = this.props;
    // 判空
    if (this.conditionIsEmpty() && (!node.children || node.children.length < 1)) {
      notification.warning({ message: intl.formatMessage(messages.noCondition) });
      return;
    }
    const nodesTreeList = _.cloneDeep(nodesTree);
    const { title, children } = node;
    if (node.children && node.children.length >= 1) {
      operatorType = {};
      optionalCol = {
        icon: 'iconColName',
        key: 'ColName',
        name: '列名'
      };
      colName = {};
      valType = { key: 'String' };
    }
    const editNode = {
      title,
      key,
      children,
      data: {
        andOr: andOrVal,
        optionalCol: {
          type: optionalCol,
          val: colName
        },
        operator: operatorType,
        valType: _.cloneDeep(valType)
      }
    };
    this.resetTreeNodes(nodesTreeList, editNode);
  }

  /**
   * 删除节点
   */
  deleteTreeNodeAction = (keys) => {
    this.selectedDeleteKeys = keys;
    this.setState({
      showModalDelete: true
    });
  }

  /**
   * 上移
   */
  moveUpAction = (key) => {
    const obj = this.moveTreeIndex([key], 'up');
    this.resetTreeNodes(obj.list, obj.changeNode, true);
  }

  /**
   * 下移
   */
  moveDownAction = (key) => {
    const obj = this.moveTreeIndex([key], 'down');
    this.resetTreeNodes(obj.list, obj.changeNode, true);
  }

  /**
   * 添加括号
   */
  addBracketAction = (keys) => {
    const { nodesTree } = this.state;
    const nodesTreeList = _.cloneDeep(nodesTree);
    let minIndex;
    const parentNode = {
      title: '',
      key: '',
      children: [],
      data: {
        andOr: 'and',
        optionalCol: {
          type: {
            icon: 'iconColName',
            key: 'ColName',
            name: '列名'
          },
          val: {}
        },
        operator: {},
        valType: { key: 'String' }
      }
    };
    _.forEach(keys, (key, index) => {
      const ks = key.split('-');
      let list = nodesTreeList;
      _.forEach(ks, (item, idx) => {
        if (idx === ks.length - 1) {
          minIndex = (parseInt(item, 10) < minIndex || !minIndex) ? parseInt(item, 10) : minIndex;
          if (index === keys.length - 1) {
            const nodes = list.splice(minIndex, keys.length);
            parentNode.children = nodes;
            list.splice(minIndex, 0, parentNode);
          }
        } else {
          list = list[parseInt(item, 10)].children;
        }
      });
    });
    this.resetTreeNodes(nodesTreeList);
  }

  /**
   * 移除括号
   */
  removeBracketAction = (key) => {
    const { nodesTree } = this.state;
    const nodesTreeList = _.cloneDeep(nodesTree);
    if (!key) return;
    const ks = key.split('-');
    let list = nodesTreeList;
    _.forEach(ks, (item, idx) => {
      if (idx === ks.length - 1) {
        const nodes = list[parseInt(item, 10)].children;
        if (!nodes || nodes.length === 0) return;
        list.splice(parseInt(item, 10), 1, ...nodes);
      } else {
        list = list[parseInt(item, 10)].children;
      }
    });
    this.resetTreeNodes(nodesTreeList);
  }

  /**
   * Form表单控件事件
   */
  checkboxChanged = (e) => {
    this.setState({
      parentPaneChecked: e.target.checked
    });
  }

  inputChanged = (key, e) => {
    e.persist();
    let val = e.target.value;
    if (key === 'Int') {
      val = parseInt(val, 10);
    } else if (key === 'Float') {
      val = parseFloat(val);
    }
    this.setFormData(val);
  }

  radioChange = (e) => {
    this.setFormData(e.target.value);
  }

  dateChanged = (date, dateString) => {
    this.setFormData(dateString);
  }

  inputNumberChanged = (type, val) => {
    const { Col: valTypeCol } = this.state.valType;
    let tmpArr = [];
    const [rowVal, colVal = '1'] = (valTypeCol && valTypeCol.split('|')) || [];
    if (type === 'row') {
      tmpArr = [Util.Convert26(val), valTypeCol && valTypeCol.split('|').length === 2 ? colVal : '1'];
    } else {
      tmpArr = [valTypeCol && valTypeCol.split('|').length === 2 ? rowVal : 'A', val];
    }
    this.setFormData(tmpArr.join('|'));
  }

  dataSourceItemClick = (key, obj) => {
    const { Data } = this.state.valType;
    const tmpArr = [];
    if (key === 'dataSource') {
      _.forEach(this.dataSource, (item) => {
        if (item.key === obj.key) {
          this.dataInstance = item.instance;
          tmpArr[0] = item.name;
          tmpArr[1] = (this.dataInstance[0] && this.dataInstance[0].name) || '';
        }
      });
    } else {
      _.forEach(this.dataInstance, (item) => {
        if (item.key === obj.key) {
          tmpArr[1] = item.name;
          if (Data && Data.match(/(.+)\.(.+)$/).length >= 2) {
            const [, source] = Data.match(/(.+)\.(.+)$/);
            tmpArr[0] = source;
          }
        }
      });
    }
    this.setFormData(tmpArr.join('.'));
  }

  // 私有方法
  operatorDidChanged = (e) => {
    this.setState({
      andOrVal: e.target.value
    });
  }

  conditionIsEmpty = () => {
    const { valType, operatorType, colName } = this.state;
    if (!_.keys(operatorType).length || !_.keys(colName).length) return true;
    if (valType.key === 'String' && (!valType[`${valType.key}`] || valType[`${valType.key}`] === '')) {
      return true;
    } else if (['Int', 'Float'].includes(valType.key) && (_.isNil(valType[`${valType.key}`]) || _.isNaN(valType[`${valType.key}`]))) {
      return true;
    } else if (valType.key === 'Col' && _.indexOf(valType[`${valType.key}`].split('|'), '') !== -1) {
      return true;
    } else if (valType.key === 'Data' && _.indexOf(valType[`${valType.key}`].split('.'), '') !== -1) {
      return true;
    }
    return false;
  }

  setFormData = (val) => {
    const { valType } = this.state;
    const data = Object.assign({}, valType, {
      [`${valType.key}`]: val
    });
    this.setState({
      valType: data
    });
  }

  menuItemClick = (key, obj) => {
    const clickObj = _.filter(this[`${key}List`], (item) => { return item.key === obj.key; });
    if (!clickObj || clickObj.length === 0) return;
    const curObj = this.state[`${key}`];
    const tmpObj = Object.assign(curObj, clickObj[0]) || {};
    this.setState({
      [key]: tmpObj
    });
  }

  showModalDelete = () => {
    const { showModalDelete } = this.state;
    if (this.selectedDeleteKeys[0]) {
      this.setState({
        showModalDelete: !showModalDelete
      });
    }
  }

  deleteItem = () => {
    const { nodesTree } = this.state;
    const nodesTreeList = _.cloneDeep(nodesTree);
    if (this.selectedDeleteKeys.length > 0) {
      let curNode = null;
      _.forEach(this.selectedDeleteKeys, (key) => {
        let list = nodesTreeList;
        const ks = key.split('-');
        let parentNode = null;
        _.forEach(ks, (item, idx) => {
          const nodeKey = parseInt(item, 10);
          if (idx === ks.length - 1) {
            list.splice(nodeKey, 1);
            if (list[nodeKey]) {
              curNode = list[nodeKey];
            } else if (list[nodeKey - 1]) {
              curNode = list[nodeKey - 1];
            } else {
              curNode = parentNode;
            }
          } else {
            parentNode = list[nodeKey];
            list = list[nodeKey].children;
          }
        });
      });
      this.resetTreeNodes(nodesTreeList, curNode);
    }
    this.setState({
      showModalDelete: false
    });
  }

  renderMenu(menuList, key, param = 'name') {
    return (
      <Menu
        className={styles.menuItem}
        onClick={this.menuItemClick.bind(this, key)}
      >
        {
          _.map(menuList, (item, index) => {
            const { icon } = item;
            return (
              <Menu.Item
                key={item.key || index}
                title={item[`${param}`]}
              >
                {icon ? (<i className={icon ? `${styles[icon]} ${styles.iconSmall}` : ''} />) : null}
                {item[`${param}`]}
              </Menu.Item>
            );
          })
        }
      </Menu>
    );
  }

  renderDataSourceMenu(list, key) {
    return (
      <Menu
        className={styles.menuItem}
        onClick={this.dataSourceItemClick.bind(this, key)}
      >
        {
          _.map(list, (item, index) => {
            return (
              <Menu.Item
                key={item.key || index}
                title={item.name}
              >
                {item.name}
              </Menu.Item>
            );
          })
        }
      </Menu>
    );
  }

  modalDelete = () => {
    const { intl } = this.props;
    return (
      <ModalDelete
        isShowModal={this.state.showModalDelete}
        content={intl.formatMessage(messages.IsDelete)}
        intl={intl}
        onOk={this.deleteItem}
        onCancel={this.showModalDelete}
      />
    );
  }

  renderValType() {
    const { valType } = this.state;
    const inputDom = (type, key) => {
      return (
        <React.Fragment>
          <div className={`${styles.inputBox} ${styles.flexRow}`}>
            <Input type={type} value={valType[`${key}`] || ''} onChange={this.inputChanged.bind(this, key)} />
          </div>
        </React.Fragment>
      );
    };
    switch (valType.key) {
      case 'Int': {
        return inputDom('number', valType.key);
      }
      case 'Float': {
        return inputDom('number', valType.key);
      }
      case 'String': {
        return inputDom('text', valType.key);
      }
      case 'Boolean': {
        return (
          <Radio.Group onChange={this.radioChange} value={valType.Boolean}>
            <Radio value>TRUE</Radio>
            <Radio value={false}>FALSE</Radio>
          </Radio.Group>
        );
      }
      case 'Date': {
        const dateFormat = 'YYYY-MM-DD HH:mm:ss';
        const defaultDate = valType.Date ? moment(valType.Date, dateFormat) : moment();
        return (
          <React.Fragment>
            <DatePicker showTime value={defaultDate} format={dateFormat} allowClear={false} onChange={this.dateChanged} />
          </React.Fragment>
        );
      }
      case 'Col': {
        const rowVal = (valType.Col && Util.ConvertNum(valType.Col.split('|')[0])) || 0;
        const colVal = (valType.Col && valType.Col.split('|')[1]) || 1;
        return (
          <React.Fragment>
            <Row style={{ flex: 1 }}>
              <Col span={11} className={`${styles.inputBox} ${styles.flexRow}`}>
                <InputNumber
                  min={0}
                  style={{ width: '100%' }}
                  defaultValue={rowVal}
                  formatter={value => Util.Convert26(parseInt(value, 10))}
                  parser={(value) => {
                    if (value.match(/^[a-zA-Z]+/)) {
                      return Util.ConvertNum(value.toUpperCase());
                    } else {
                      return value;
                    }
                  }}
                  onChange={this.inputNumberChanged.bind(this, 'row')}
                />
              </Col>
              <Col span={2} />
              <Col span={11} className={`${styles.inputBox} ${styles.flexRow}`}>
                <InputNumber
                  min={1}
                  style={{ width: '100%' }}
                  defaultValue={colVal}
                  onChange={this.inputNumberChanged.bind(this, 'col')}
                />
              </Col>
            </Row>
          </React.Fragment>
        );
      }
      case 'Data': {
        const [, data, instance] = (valType.Data && valType.Data.match(/(.+)\.(.+)$/)) || ['', ''];
        return (
          <React.Fragment>
            <Row style={{ flex: 1, width: 0, overflow: 'hidden' }}>
              <Col span={11}>
                <Dropdown
                  overlay={this.renderDataSourceMenu(this.dataSource, 'dataSource')}
                  trigger={['click']}
                >
                  <div className={`${styles.inputBox} ${styles.flexRow}`}>
                    <span className={`${styles.textOverflow}`} title={data}>{data}</span>
                    <Icon type="down" />
                  </div>
                </Dropdown>
              </Col>
              <Col span={2} />
              <Col span={11}>
                <Dropdown
                  overlay={this.renderDataSourceMenu(this.dataInstance, 'dataInstance')}
                  trigger={['click']}
                >
                  <div className={`${styles.inputBox} ${styles.flexRow}`}>
                    <span className={`${styles.textOverflow}`} title={instance}>{instance}</span>
                    <Icon type="down" />
                  </div>
                </Dropdown>
              </Col>
            </Row>
          </React.Fragment>
        );
      }
      default:
    }
  }

  render() {
    const { intl } = this.props;
    const { operatorType, optionalCol, colName, parentPaneChecked, optionalType, valType, nodesTree, andOrVal } = this.state;
    this.initOperatorTypeList(valType);
    this.initValTypeList(operatorType);
    return (
      <div>
        <Modal
          visible
          title={intl.formatMessage(messages.HighFilter)}
          width="40%"
          onOk={this.handleOk}
          onCancel={this.handleCancal}
          okText={intl.formatMessage(messages.ok)}
          cancelText={intl.formatMessage(messages.cancel)}
        >
          <div className={`${styles.flexColumn} ${styles.filterModal}`}>
            <span>{intl.formatMessage(messages.parentPaneCondition)}</span>
            <div className={styles.modalCheckBox}>
              <Checkbox
                checked={parentPaneChecked}
                disabled={this.state.disabled}
                onChange={this.checkboxChanged}
              >
                {intl.formatMessage(messages.parentPaneFilterMsg)}
              </Checkbox>
            </div>
            <span style={{ marginTop: '24px', display: 'inline-block' }}>{intl.formatMessage(messages.customConfition)}</span>
            <div className={styles.modalCheckBox} >
              <div className={styles.flexRow}>
                <span style={{ paddingRight: '11px' }}>{intl.formatMessage(messages.optionalType)}</span>
                <Radio.Group onChange={this.onChange} value={optionalType}>
                  <Radio value="custom">{intl.formatMessage(messages.custom)}</Radio>
                  <Radio value="formula" disabled>{intl.formatMessage(messages.Formulas)}</Radio>
                </Radio.Group>
              </div>
              <Row className={styles.optionColInputs} style={{ marginTop: '14px' }}>
                <Col className={styles.flexColumn} span={7}>
                  <span>{intl.formatMessage(messages.optionalCol)}</span>
                  <div className={styles.flexRow} style={{ paddingTop: '4px' }}>
                    <Dropdown
                      overlay={this.renderMenu(this.optionalColList, 'optionalCol')}
                      trigger={['click']}
                    >
                      <i className={`${styles[`${optionalCol.icon}`]} ${styles.iconSmall}`} />
                    </Dropdown>
                    <Dropdown
                      overlay={this.renderMenu(this.colNameList, 'colName', optionalCol.key === 'ColName' ? 'name' : 'key')}
                      trigger={['click']}
                    >
                      <div className={`${styles.inputBox} ${styles.flexRow}`}>
                        <span className={`${styles.textOverflow}`} title={optionalCol.key === 'ColName' ? colName.name : colName.key}>{optionalCol.key === 'ColName' ? colName.name : colName.key}</span>
                        <Icon type="down" />
                      </div>
                    </Dropdown>
                  </div>
                </Col>
                <Col className={styles.flexColumn} span={6}>
                  <span>{intl.formatMessage(messages.operator)}</span>
                  <div className={styles.flexRow} style={{ paddingTop: '4px' }}>
                    <Dropdown
                      overlay={this.renderMenu(this.operatorTypeList, 'operatorType')}
                      trigger={['click']}
                    >
                      <div className={`${styles.inputBox} ${styles.flexRow}`}>
                        <span>{operatorType.name}</span>
                        <Icon type="down" />
                      </div>
                    </Dropdown>
                  </div>
                </Col>
                <Col className={styles.flexColumn} span={11}>
                  <span>{intl.formatMessage(messages.valueType)}</span>
                  <div className={styles.flexRow} style={{ paddingTop: '4px', alignItems: 'center' }}>
                    <Dropdown
                      overlay={this.renderMenu(this.valTypeList, 'valType')}
                      trigger={['click']}
                    >
                      <i className={`${styles[`iconType${valType.key}`]} ${styles.iconSmall}`} style={{ flexShrink: 0 }} />
                    </Dropdown>
                    {this.renderValType()}
                  </div>
                </Col>
              </Row>
              <div className={styles.andOr}>
                <Radio.Group onChange={this.operatorDidChanged} value={andOrVal}>
                  <Radio value="and">{intl.formatMessage(messages.and)}</Radio>
                  <Radio value="or">{intl.formatMessage(messages.or)}</Radio>
                </Radio.Group>
              </div>
              <DataTree
                intl={intl}
                data={nodesTree}
                selectedKeys={this.selectedKeys}
                selectChanged={this.treeSelectChanged}
                addTreeNodeAction={this.addTreeNodeAction}
                editTreeNodeAction={this.editTreeNodeAction}
                deleteTreeNodeAction={this.deleteTreeNodeAction}
                moveUpAction={this.moveUpAction}
                moveDownAction={this.moveDownAction}
                addBracketAction={this.addBracketAction}
                removeBracketAction={this.removeBracketAction}
              />
            </div>
          </div>
        </Modal>
        {this.modalDelete()}
      </div>
    );
  }
}
