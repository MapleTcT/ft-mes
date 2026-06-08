import React, { Component } from 'react';
import * as _ from 'lodash';
import { Menu, Dropdown, Icon, Tree, Spin, Input, notification, Tooltip, message } from 'sup-ui';
import classnames from 'classnames';
import Modal from '../Modal/CommonModal';
// import { getProps, getServiceInfo } from 'root/services/objectApi';
// import { checkDataSource } from 'root/services/datatableApi';
// import config from 'root/config';
// import ObjectSelector from '@supos/object-selector/dist';
import ModalDelete from '../Modal/ModalDelete';
import InsertFormula from './InsertFormula';
import OperateFormula from './OperateFormula';
import DataTableSearchModal from './DataTableSearchModal';
import CellRelevance from './CellRelevance';
import styles from './DataSource.less';
import messages from '../messages';
import { queryCustomService, queryEntityModelList, queryModelPropertys } from '../../../../services/templateDesigner';

const { TreeNode } = Tree;
const { Search } = Input;

export default class DataSourceSetDrawer extends Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.menu = (
      <Menu>
        {/* <Menu.Item>
          <span onClick={this.showOrHideModal.bind(this, { objectSelectorVisiable: true })}>{props.intl.formatMessage(messages.Instance)}</span>
        </Menu.Item>
        <Menu.Item>
          <span onClick={this.showDataSearchModal}>{props.intl.formatMessage(messages.DataSearch)}</span>
        </Menu.Item> */}
        {/* <Menu.Item>
          <span onClick={this.showOrHideModal.bind(this, { serviceSelectorVisiable: true })}>{props.intl.formatMessage(messages.AddServer)}</span>
        </Menu.Item> */}
        {/* <Menu.Item>
          <span onClick={this.showOrHideModal.bind(this, { objectsModalVisiable: true })}>{props.intl.formatMessage(messages.EntityObjects)}</span>
        </Menu.Item> */}
        <Menu.Item>
          <span onClick={this.showOrHideModal.bind(this, { customServiceVisiable: true })}>{props.intl.formatMessage(messages.CustomService)}</span>
          <Tooltip title={intl.formatMessage(messages.Rule14)}>
            <Icon type="question-circle" style={{ paddingLeft: '10px' }} />
          </Tooltip>
        </Menu.Item>
      </Menu>
    );

    this.state = {
      objectSelectorVisiable: false,
      dataTableVisiable: false,
      allDataSource: props.allDataSource || {},
      sqlInfo: props.sqlInfo,
      loading: false,
      selectedKeys: [],
      showModalDelete: false,
      searchValue: '',
      editName: '',
      settingTab: props.intl.formatMessage(messages.AttrSet),
      objectsModalVisiable: false,
      customServiceVisiable: false,
      customSerName: '',
      customSerUrl: ''
    };

    // this.checkDataSource(props.sqlInfo);
    // this.checkService(props.allDataSource, props.serviceInput);
    // this.checkProperty(props.allDataSource);
  }

  componentWillMount() {
    // this.setSystemBasicInfo();
    this.fetchEntityObjects();
  }

  componentWillReceiveProps(nextProps) {
    if (_.difference(_.keys(this.state.allDataSource), _.keys(nextProps.allDataSource)).length !== 0) {
      this.setState({
        allDataSource: nextProps.allDataSource
      });
    }
    this.props.backfillConfig();
  }

  shouldComponentUpdate(nextProps, nextState) {
    if (nextProps.unfoldDataSource !== this.props.unfoldDataSource) {
      return false;
    } else if (_.isEqual(this.props, nextProps) && _.isEqual(this.state, nextState)) {
      return false;
    }
    return true;
  }

  setObject = (arr) => {
    if (!_.isArray(arr)) {
      arr = [arr];
    }
    if (arr.length) {
      if (_.get(arr, '[0].key') === 'instance') {
        this.setState({ loading: true });
        this.getObjectProperties(arr);
      } else if (_.get(arr, '[0].subTab') === 'service') {
        this.setService(arr);
      }
    }
  }

  setService = (arr) => {
    const { intl } = this.props;
    const serviceOutput = {};
    const serviceInput = _.merge({}, this.props.serviceInput);
    const notMatchedArr = [];
    const outputNotMatchedArr = [];
    _.map(arr, (object) => {
      const propertyType = object.key === 'template' ? 'template_service' : 'service';
      const { selectedInstance: { name: instanceName, id: instanceId }, selectedProp, selectedTemplate: { id: templateId, name: tempName, namespace: tempNS } } = object;
      _.map(selectedProp, (property) => {
        const { name, output, inputs, id: serviceId, namespace: serviceNS } = property;
        const oriServiceName = instanceName ? `${tempNS}:${tempName}:${instanceName}:${serviceNS}:${name}` : `${tempNS}:${tempName}:${serviceNS}:${name}`;
        const serviceName = `${oriServiceName}服务`;
        if (_.get(output, 'primitiveType') === 'JSON') {
          const jsonStr = _.get(output, 'jsonDesc');
          const jsonObject = JSON.parse(jsonStr);
          if (!_.get(jsonObject, 'list[0]')) {
            outputNotMatchedArr.push(serviceName);
          } else {
            const list = _.map(jsonObject.list[0], (v, k) => {
              return {
                propertyType,
                propertyName: k,
                primitiveType: 'STRING'
              };
            });
            const obj = { ...object, selectedProp: property };
            serviceOutput[serviceName] = {
              list,
              instanceId,
              templateId,
              serviceId,
              originalSelectedObject: obj
            };
            if (serviceOutput[serviceName].list.length) {
              serviceOutput[serviceName].list.unshift({
                propertyType,
                propertyName: '_key_',
                primitiveType: 'INTEGER'
              });
              serviceOutput[serviceName].list.unshift({
                propertyType,
                propertyName: '_table_'
              });
            }
            if (inputs) {
              serviceInput[oriServiceName] = _.map(inputs, (item) => {
                return {
                  name: item.name,
                  type: item.primitiveType
                };
              });
            }
          }
        } else {
          notMatchedArr.push(serviceName);
        }
      });
    });
    if (notMatchedArr.length) {
      const msg = intl.formatMessage(messages.Rule1, { name: notMatchedArr.join(',') });
      notification.warning({
        message: intl.formatMessage(messages.ServiceAddFailed),
        description: (<div dangerouslySetInnerHTML={{ __html: msg }} />),
        className: styles.notification
      });
    }
    if (outputNotMatchedArr.length) {
      const msg = intl.formatMessage(messages.Rule2, { name: outputNotMatchedArr.join(',') });
      notification.warning({
        message: intl.formatMessage(messages.ServiceAddFailed),
        description: (<div dangerouslySetInnerHTML={{ __html: msg }} />),
        className: styles.notification
      });
    }
    const allDataSource = {
      ...this.state.allDataSource,
      ...serviceOutput
    };

    this.setState({
      allDataSource,
      searchValue: ''
    }, () => {
      this.props.updateDataSource({ allDataSource, serviceInput });
    });
  }

  setDataTable = (tableName, columns, sqlInfo) => {
    const { editName, searchValue } = this.state;
    const allDataSource = {
      ...this.state.allDataSource,
      [tableName]: { list: columns || [] }
    };
    if (editName !== tableName) {
      delete allDataSource[editName];
    }
    this.setState({
      allDataSource,
      sqlInfo: {
        ...this.state.sqlInfo,
        [tableName]: sqlInfo
      },
      selectedKeys: [],
      searchValue: editName === tableName ? searchValue : ''
    }, () => {
      this.props.updateDataSource({ allDataSource, sqlInfo: { tableName, sqlInfo }, serviceInput: this.props.serviceInput });
    });
  }

  setCustomServiceObject = (callback = () => { }) => {
    const { customSerUrl, customSerName } = this.state;
    queryCustomService(customSerUrl, false).then((res) => {
      const { data, message: msg = '' } = res.data;
      if (!data) {
        message.error(msg);
        return;
      }
      try {
        callback();
        const finalRes = data[0] && _.map(_.get(data, '[0]', []), (item, key) => {
          return { propertyType: 'custom', propertyName: item.name, propertyCode: key };
        });
        const { editName, searchValue } = this.state;
        const allDataSource = {
          ...this.state.allDataSource,
          [customSerName]: { list: finalRes || [], sourceUrl: customSerUrl }
        };
        this.setState({
          allDataSource,
          selectedKeys: [],
          searchValue: editName === customSerName ? searchValue : ''
        }, () => {
          this.props.updateDataSource({ allDataSource });
        });
      } catch (err) {
        console.log(err);
      }
    });
  }

  fetchEntityObjects = () => {
    const { entityCode } = this.props;
    queryEntityModelList(entityCode).then((res) => {
      const { status, data: { data } } = res;
      const entityObjs = {};
      if (+status !== 200) return;
      _.map(data, (item) => {
        const { modelName, modelCode } = item;
        item.propertyType = 'entity';
        entityObjs[`${modelName}@#@${modelCode}`] = item;
      });
      const allDataSource = {
        ...this.props.allDataSource,
        ...entityObjs
      };
      this.setState({
        allDataSource,
        searchValue: ''
      }, () => {
        this.props.updateDataSource({ allDataSource });
      });
    });
  }

  onLoadData = ({ props: { entityKey, children, propertyCode } }) => {
    // console.log("startload:  ", params);
    return new Promise((resolve) => {
      if (children) {
        resolve();
        return;
      }
      queryModelPropertys(entityKey, propertyCode).then((res) => {
        const { data } = res.data;
        const allDataSource = this.updateAllDataSource(entityKey, propertyCode, data);
        this.setState({
          allDataSource
        });
        resolve();
      });
    });
  }

  updateAllDataSource = (entityKey, propertyCode, children) => {
    const { allDataSource } = this.state;
    children = _.map(children, (item) => {
      return { ...item, propertyType: 'entity' };
    });
    function resetSubChildren(list) {
      return _.map(list, (item) => {
        if (item.propertyCode === propertyCode) {
          return { ...item, children };
        } else if (item.children) {
          return { ...item, children: resetSubChildren(item.children) };
        }
        return item;
      });
    }

    return _.mapValues(allDataSource, (item) => {
      if (item.propertyType === 'entity') {
        if (item.modelCode === entityKey && !propertyCode) {
          return { ...item, list: children };
        } else if (item.list) {
          return { ...item, list: resetSubChildren(item.list) };
        }
      }
      return item;
    });
  }

  getObjectProperties = (arr) => {
    const { intl, serviceInput } = this.props;
    const propsPromiseList = [];
    this.tmpDataSource = this.state.allDataSource;
    _.map(arr, (item) => {
      const { selectedTemplate: { name: tempName, namespace: tempNamespace }, selectedInstance: { name: instanceName } } = item;
      const params = {
        name: `${tempNamespace}:${tempName}:${instanceName}`,
        templateId: _.get(item, 'selectedTemplate.id'),
        instanceId: _.get(item, 'selectedInstance.id'),
        queryAll: false,
        pageSize: 200,
        originalSelectedObject: item
      };
      const tmpPromise = new Promise((resolve) => {
        // getObjectProps(params).then((res) => {
        //   if (+res.code === 200) {
        //     const { data: { list: properties = [] } } = res;
        //     const allowType = ['INTEGER', 'DOUBLE', 'LONG', 'FLOAT', 'BOOLEAN', 'STRING', 'DATE', 'DATETIME', 'DECIMAL'];
        //     const data = _.map(_.filter(properties, p => allowType.includes(p.primitiveType)), (obj) => {
        //       obj.propertyType = 'property';
        //       return obj;
        //     });
        //     this.updateDataSource(data, params);
        //     if (!data.length) {
        //       const msg = intl.formatMessage(messages.Rule4, { name1: params.name });
        //       notification.warning({
        //         message: intl.formatMessage(messages.ObjectAddFailed),
        //         description: (<div dangerouslySetInnerHTML={{ __html: msg }} />),
        //         className: styles.notification
        //       });
        //     }
        //   }
        //   resolve();
        // }).catch(() => {
        //   resolve();
        // });
      });
      propsPromiseList.push(tmpPromise);
    });
    Promise.all(propsPromiseList).then(() => {
      this.setState({
        loading: false,
        allDataSource: this.tmpDataSource || {},
        searchValue: ''
      }, () => {
        this.props.updateDataSource({ allDataSource: this.tmpDataSource, serviceInput });
        delete this.tmpDataSource;
      });
    });
  }

  updateDataSource = (data, { name, templateId, instanceId, originalSelectedObject }) => {
    data.unshift({
      propertyType: 'property',
      propertyName: '_key_',
      primitiveType: 'INTEGER'
    });
    this.tmpDataSource = {
      ...this.tmpDataSource,
      [name]: {
        list: data,
        templateId,
        instanceId,
        originalSelectedObject
      }
    };
  }

  getDataType = () => {
    const [, dataType] = (this.props.formula && this.props.formula.match(/(RT|HIS|DT|SER|RTS|CTS|ENT)\((.*)\)$/)) || [];
    return dataType;
  }

  checkDataSource = (sqlInfo) => {
    const { intl } = this.props;
    const tableMap = {};
    const sourceMap = {};
    _.map(sqlInfo, (item, tableName) => {
      const { sourceName, id } = item;
      if (id) {
        if (!tableMap[id]) tableMap[id] = [];
        tableMap[id].push(tableName);
        sourceMap[id] = sourceName;
      }
    });
    this.unavailableSourceNames = [];
    _.map(tableMap, (names, id) => {
      if (id && names.length) {
        // checkDataSource({ id }).then((res) => {
        //   if (!_.get(res, 'isConnect')) {
        //     this.unavailableSourceNames.push(...names);
        //     const msg = intl.formatMessage(messages.Rule3, { name1: sourceMap[id] || id, name2: names.join(',') });
        //     notification.warning({
        //       message: intl.formatMessage(messages.DataSourceConnectionFailed),
        //       description: (<div dangerouslySetInnerHTML={{ __html: msg }} />),
        //       className: styles.notification
        //     });
        //   }
        // });
      }
    });
  }

  checkService = (allDataSource, serviceInput) => {
    const { intl } = this.props;
    const filtered = {};
    const inputChangedArr = [];
    const outputChangedArr = [];
    const deletedArr = [];

    _.map(allDataSource, (value, key) => {
      if (/服务$/.test(key)) {
        filtered[key] = value;
      }
    });
    const count = 0;

    _.map(filtered, (outputs, key) => {
      const [, instance, service] = key.match(/(.*):(.*)服务$/);
      if (instance && service) {
        // const param = {
        //   ...outputs
        // };
        // getServiceInfo(param).then((res) => {
        //   if (res) {
        //     count += 1;
        //     if (+res.code === 400 || res.message === '找不到该对象') {
        //       deletedArr.push(key);
        //     } else if (+res.code === 200) {
        //       const { inputs: resInputs, output: resOutputs } = res.data;
        //       const inputs = serviceInput[`${instance}:${service}`] || [];
        //       const intersectionInputs = _.intersectionBy(resInputs, inputs, 'name');
        //       if (intersectionInputs.length !== resInputs.length || intersectionInputs.length !== inputs.length) {
        //         inputChangedArr.push(key);
        //       }

        //       if (_.get(resOutputs, 'primitiveType') === 'JSON') {
        //         const jsonStr = _.get(resOutputs, 'jsonDesc');
        //         const jsonObject = _.get(JSON.parse(jsonStr), 'list[0]', {});
        //         const filteredArr = [];
        //         const outputList = _.get(outputs, 'list', []);
        //         _.map(outputList, (item) => {
        //           if (item.propertyName !== '_key_' && item.propertyName !== '_table_') {
        //             filteredArr.push(item.propertyName);
        //           }
        //         });

        //         const newOutputs = _.keys(jsonObject);
        //         const intersectionOutput = _.intersection(newOutputs, filteredArr);
        //         if (intersectionOutput.length !== newOutputs.length || intersectionOutput.length !== filteredArr.length) {
        //           outputChangedArr.push(key);
        //         }
        //       }
        //     }
        //     if (_.keys(filtered).length === count) {
        //       const msgArr = [];
        //       if (deletedArr.length) {
        //         msgArr.push(intl.formatMessage(messages.Rule12, { name1: deletedArr.join(', ') }));
        //       }
        //       if (inputChangedArr.length) {
        //         msgArr.push(intl.formatMessage(messages.Rule5, { name1: inputChangedArr.join(', ') }));
        //       }
        //       if (outputChangedArr.length) {
        //         msgArr.push(intl.formatMessage(messages.Rule6, { name1: outputChangedArr.join(', ') }));
        //       }

        //       if (msgArr.length) {
        //         notification.warning({
        //           message: intl.formatMessage(messages.Server),
        //           description: (<div dangerouslySetInnerHTML={{ __html: msgArr.join('<br/><br/>') }} />),
        //           className: styles.notification
        //         });
        //       }
        //     }
        //   }
        // });
      }
    });
  }

  checkProperty = (dataSource) => {
    const { intl, serviceInput } = this.props;
    const { allDataSource } = this.state;
    const checkPropertyObj = {
      updateArr: [],
      removedArr: []
    };
    const filtered = {};
    const count = 0;
    _.map(dataSource, (value, key) => {
      if (_.get(value, 'list[0].propertyType') === 'property') {
        filtered[key] = value;
      }
    });
    const propsPromiseList = [];
    this.tmpDataSource = allDataSource;
    _.map(filtered, (value, key) => {
      const param = {
        name: key,
        queryAll: false,
        pageSize: 200,
        ...value
      };
      const tmpPromise = new Promise((resolve) => {
        //   getObjectProps(param).then((res) => {
        //     count += 1;
        //     if (+res.code === 200) {
        //       const { data: { list: properties = [] } } = res;
        //       const allowType = ['INTEGER', 'DOUBLE', 'LONG', 'FLOAT', 'BOOLEAN', 'STRING', 'DATE', 'DATETIME', 'DECIMAL'];
        //       const data = _.map(_.filter(properties, p => allowType.includes(p.primitiveType)), (item) => {
        //         item.propertyType = 'property';
        //         return item;
        //       });
        //       const tmpData = _.clone(data);
        //       this.updateDataSource(data, param);
        //       if (tmpData.length) {
        //         // 判断是否发生变化
        //         const oldArr = _.get(this.tmpDataSource, `${param.name}.list`);
        //         const intersectionArr = _.intersectionBy(oldArr, data, 'propertyName');
        //         if (intersectionArr.length !== data.length || intersectionArr.length !== oldArr.length) {
        //           _.get(checkPropertyObj, 'updateArr', []).push(param.name);
        //         }
        //       } else {
        //         _.get(checkPropertyObj, 'removedArr', []).push(param.name);
        //       }
        //       if (_.keys(filtered).length === count) {
        //         const { updateArr, removedArr } = checkPropertyObj;
        //         const msgArr = [];
        //         if (updateArr.length) msgArr.push(intl.formatMessage(messages.Rule11, { name1: updateArr.join(', ') }));
        //         if (removedArr.length) msgArr.push(intl.formatMessage(messages.Rule13, { name1: removedArr.join(', ') }));
        //         if (msgArr.length) {
        //           notification.info({
        //             message: intl.formatMessage(messages.Instance),
        //             description: (<div dangerouslySetInnerHTML={{ __html: msgArr.join('<br/>') }} />),
        //             className: styles.notification
        //           });
        //         }
        //       }
        //     }
        //     resolve();
        //   }).catch(() => {
        //     resolve();
        //   });
      });
      propsPromiseList.push(tmpPromise);
    });
    Promise.all(propsPromiseList).then(() => {
      this.setState({
        loading: false,
        allDataSource: this.tmpDataSource || {},
        searchValue: ''
      }, () => {
        this.props.updateDataSource({ allDataSource: this.tmpDataSource, serviceInput });
        delete this.tmpDataSource;
      });
    });
  }

  showDataSearchModal = () => {
    this.setState({
      dataTableVisiable: true,
      editName: ''
    });
  }

  chooseItem = (item, key, isEntity, e) => {
    if (isEntity && !item.isLeaf) return;
    this.props.chooseDataSourceProperties(item, key, e);
  }

  treeSelect = (selectedKeys) => {
    this.setState({
      selectedKeys
    });
  }

  deleteItem = () => {
    const { intl } = this.props;
    const { allDataSource, selectedKeys } = this.state;
    delete allDataSource[selectedKeys[0]];
    const serviceInput = _.merge({}, this.props.serviceInput);
    const index = selectedKeys[0].indexOf(intl.formatMessage(messages.Server));
    if (~index) {
      const key = selectedKeys[0].replace('-', '.').slice(0, index);
      delete serviceInput[key];
    }
    this.setState({
      allDataSource,
      selectedKeys: [],
      showModalDelete: false
    }, () => {
      this.props.updateDataSource({ allDataSource, serviceInput });
    });
  }

  searchItem = (value) => {
    this.setState({
      searchValue: value.trim()
    });
  }

  editSql = (key) => {
    this.setState({
      dataTableVisiable: true,
      editName: key
    });
  }

  editCustomService = (key) => {
    const { allDataSource } = this.state;
    const obj = {};
    if (allDataSource[key]) {
      obj.customSerName = key;
      obj.customSerUrl = _.get(allDataSource[key], 'sourceUrl', '');
    }
    this.isEdit = true;
    this.setState({
      ...obj,
      customServiceVisiable: true
    });
  }

  editDataSource = (key) => {
    const { sqlInfo } = this.state;
    if (sqlInfo[key]) {
      this.editSql(key);
    } else {
      this.editCustomService(key);
    }
  }

  dataSourceTree = () => {
    const { allDataSource, searchValue, sqlInfo = {} } = this.state;
    const { intl } = this.props;
    const showTreeData = {};
    _.map(allDataSource, (value, key) => {
      const list = _.get(value, 'list', []);
      if (~key.toUpperCase().indexOf(searchValue.toUpperCase())) {
        showTreeData[key] = list;
      } else {
        const labArr = _.filter(list, (item) => {
          const name = item.propertyName || item;
          return ~name.toUpperCase().indexOf(searchValue.toUpperCase());
        });
        if (labArr.length > 0) {
          showTreeData[key] = labArr;
        }
      }
    });

    return (
      <Tree onSelect={this.treeSelect} loadData={this.onLoadData}>
        {
          _.map(_.keys(showTreeData), (key) => {
            let title = key;
            if (_.get(showTreeData, `[${key}][0].propertyType`) === 'property') {
              const [, , instance] = key.split(':');
              title = instance;
            } else if (_.get(showTreeData, `[${key}][0].propertyType`) === 'service') {
              const [, , instance, , propName] = key.split(':');
              title = `${instance}:${propName}`;
            } else if (_.get(showTreeData, `[${key}][0].propertyType`) === 'template_service') {
              const [, , , propName] = key.split(':');
              title = `${propName}`;
            } else if (_.get(showTreeData, `["${key}"][0].propertyType`) === 'entity' || (key.split('@#@').length >= 2 && _.get(showTreeData, `["${key}"]`).length === 0)) {
              const [modelName] = key.split('@#@');
              title = `${modelName}`;
            }
            const parentNodeTitle = (
              <div className={styles['flex-center']}>
                <span className={styles['flex-center']}>
                  <div className={sqlInfo[key] ? styles['table-icon'] : styles['his-icon']} />
                  <div className={styles['tree-leaf']} title={key}>{title}</div>
                </span>
                {sqlInfo[key] || _.get(showTreeData, `[${key}][0].propertyType`) === 'custom' ? (
                  <i
                    className={`${styles['icon-edit']} ${styles['icon-mini']}`}
                    onClick={this.editDataSource.bind(this, key)}
                    style={{ marginLeft: 2 }}
                  />
                ) : null}
                {(_.get(showTreeData, `[${key}][0].propertyType`) === 'property' && showTreeData[key].length > 200) ? (
                  <Tooltip title={intl.formatMessage(messages.Rule8)}>
                    <Icon type="info-circle" style={{ paddingLeft: '10px' }} />
                  </Tooltip>
                ) : null}
              </div>
            );
            let entityKey = '';
            const isEntity = _.get(allDataSource, `["${key}"].propertyType`) === 'entity';
            if (isEntity) {
              const { modelCode } = allDataSource[key];
              entityKey = `${modelCode}`;
            }
            return (
              <TreeNode key={key} title={parentNodeTitle} entityKey={`${entityKey}`}>
                {
                  this.renderSubTreeNode(showTreeData[key], key, entityKey, isEntity)
                }
              </TreeNode>
            );
          })
        }
      </Tree>
    );
  }

  renderSubTreeNode = (list, key, entityKey, isEntity, inheritTree) => {
    if (!list || !list.length) return;
    return _.map(list, (item) => {
      const { namespace, propertyName, propertyCode } = item;
      const propName = namespace ? `${namespace}:${propertyName}` : propertyName;
      let inheritStr = '';
      if (isEntity) {
        if (!inheritTree) {
          inheritStr = `${propertyName}@#@${propertyCode}`;
        } else {
          inheritStr = `${inheritTree}@:@${propertyName}@#@${propertyCode}`;
        }
      }
      item.inheritTree = inheritStr;
      if (propName) {
        return (
          <TreeNode
            key={propName}
            isLeaf={!isEntity || item.isLeaf}
            selectable={false}
            entityKey={entityKey}
            propertyCode={propertyCode}
            title={
              <span title={propertyName} onMouseDown={this.chooseItem.bind(this, item, key, isEntity)}>
                {propertyName}
              </span>
            }
          >
            {this.renderSubTreeNode(item.children, key, entityKey, isEntity, inheritStr)}
          </TreeNode>
        );
      } else {
        return (
          <TreeNode
            key={item}
            isLeaf
            selectable={false}
            title={
              <span title={item} onMouseDown={this.chooseItem.bind(this, item, key, isEntity)}>
                {item}
              </span>
            }
          />
        );
      }
    });
  }

  updateSelect = (objects) => {
    this.selectedObjects = objects;
  }

  handleOk = (objects) => {
    this.updateSelect(objects);
    this.setObject(this.selectedObjects);
    this.handleCancel();
  }

  showObjectSelectorModal = () => {
    const { objectSelectorVisiable: object } = this.state;
    if (object) {
      // return (
      //   <ObjectSelectorModal
      //     visible={object}
      //     handleOk={this.handleOk}
      //     handleCancel={this.handleCancel}
      //     tabs={[
      //       {
      //         key: 'instance',
      //         tabKey: 'instance',
      //         tabName: '对象实例'
      //       },
      //       {
      //         key: 'template',
      //         tabKey: 'template',
      //         tabName: '对象模板-服务'
      //       },
      //       {
      //         key: 'service',
      //         tabKey: 'instance',
      //         tabName: '对象实例-服务'
      //       }
      //     ]}
      //     subTabs={{
      //       service: [{
      //         key: 'service',
      //         multiSelect: true
      //       }],
      //       template: [{
      //         key: 'service',
      //         multiSelect: true
      //       }]
      //     }}
      //     multiSelect
      //     domain={config.domainObjectSelect}
      //     namespace={this.props.appId}
      //     scope={7}
      //   />
      // );
    }
  }

  showDataTableSearchModal = () => {
    const { editName, dataTableVisiable, sqlInfo, allDataSource } = this.state;
    if (dataTableVisiable) {
      return (
        <DataTableSearchModal
          intl={this.props.intl}
          showOrHideModal={this.showOrHideModal}
          setDataTable={this.setDataTable}
          editName={editName}
          sqlInfo={sqlInfo}
          allDataSource={allDataSource}
          unavailableSourceNames={this.unavailableSourceNames}
        />
      );
    }
  }

  showCustomServiceModal = () => {
    const { customServiceVisiable, customSerName, customSerUrl } = this.state;
    const { intl } = this.props;
    if (customServiceVisiable) {
      return (
        <Modal
          visible
          title={intl.formatMessage(messages.CustomService)}
          width={480}
          bodyStyle={{ padding: '35px 100px' }}
          onOk={this.objectHandleOk}
          onCancel={this.objectHandleCancal}
          okText={intl.formatMessage(messages.ok)}
          cancelText={intl.formatMessage(messages.cancel)}
        >
          <div>
            <span className={styles.inputLabel}>
              <span style={{ color: 'red' }}>*</span>
              {intl.formatMessage(messages.Name)}
            </span>
            <Input value={customSerName} disabled={this.isEdit} onChange={this.inputChange.bind(this, 'customSerName')} />
          </div>
          <div style={{ marginTop: 18 }}>
            <span className={styles.inputLabel}>
              <span style={{ color: 'red' }}>*</span>
              URL
            </span>
            <Input value={customSerUrl} onChange={this.inputChange.bind(this, 'customSerUrl')} />
          </div>
        </Modal>
      );
    }
  }

  inputChange = (key, e) => {
    const { value } = e.target;
    this.setState({
      [key]: value
    });
  }

  objectHandleOk = () => {
    const { intl } = this.props;
    const { customSerName, customSerUrl } = this.state;
    if (customSerName === '' || customSerUrl === '') {
      message.error(intl.formatMessage(messages.Rule15));
      return;
    } else if (!/^[A-Za-z0-9\u4e00-\u9fa5]+$/g.test(customSerName)) {
      message.error(intl.formatMessage(messages.Rule16));
      return;
    }
    this.setCustomServiceObject(this.objectHandleCancal);
    // this.objectHandleCancal();
  }

  objectHandleCancal = () => {
    this.supPlantObjects = [];
    this.plantObjectChanged = false;
    this.setState({
      objectsModalVisiable: false,
      customServiceVisiable: false,
      customSerUrl: '',
      customSerName: ''
    });
  }

  tabClick = (value) => {
    this.setState({
      settingTab: value
    }, () => {
      this.props.backfillConfig();
    });
  }

  backfill = ({
    paneValue = this.state.paneValue,
    paneUpValue = this.state.paneUpValue,
    formulaValue = this.state.formulaValue
  }) => {
    const { intl } = this.props;
    const { settingTab } = this.state;
    switch (settingTab) {
      case intl.formatMessage(messages.parentPane): {
        this.setState({
          paneValue,
          paneUpValue
        });
        break;
      }
      case intl.formatMessage(messages.Formula): {
        this.setState({
          formulaValue
        });
        break;
      }
      default: break;
    }
  }

  showFormulaConfig = () => {
    const { intl } = this.props;
    const { settingTab } = this.state;
    return (
      <div className={styles['cell-setting-box']}>
        <div className={styles['cell-setting-tab']}>
          <Tooltip placement="right" title={intl.formatMessage(messages.AttrSet)}>
            <div
              onClick={this.tabClick.bind(this, intl.formatMessage(messages.AttrSet))}
              className={classnames(
                styles['setting-tab-icon'],
                styles['config-setting-icon'],
                { [styles['config-setting-selected']]: settingTab === intl.formatMessage(messages.AttrSet) }
              )}
            />
          </Tooltip>
          <Tooltip placement="right" title={intl.formatMessage(messages.parentPane)}>
            <div
              onClick={this.tabClick.bind(this, intl.formatMessage(messages.parentPane))}
              className={classnames(
                styles['setting-tab-icon'],
                styles['pane-setting-icon'],
                { [styles['pane-setting-selected']]: settingTab === intl.formatMessage(messages.parentPane) }
              )}
            />
          </Tooltip>
          <Tooltip placement="right" title={intl.formatMessage(messages.Formula)}>
            <div
              onClick={this.tabClick.bind(this, intl.formatMessage(messages.Formula))}
              className={classnames(
                styles['setting-tab-icon'],
                styles['formula-blur-icon'],
                { [styles['formula-focus-icon']]: settingTab === intl.formatMessage(messages.Formula) }
              )}
            />
          </Tooltip>
          <Tooltip placement="right" title={intl.formatMessage(messages.operate)}>
            <div
              onClick={this.tabClick.bind(this, intl.formatMessage(messages.operate))}
              className={classnames(
                styles['setting-tab-icon'],
                styles['opt-blur-icon'],
                { [styles['opt-focus-icon']]: settingTab === intl.formatMessage(messages.operate) }
              )}
            />
          </Tooltip>
        </div>
        <div className={styles['cell-setting-content']}>
          {
            settingTab && (
              <div className={styles.head}>
                <span>{settingTab}</span>
                <span />
              </div>
            )
          }
          <div className={styles['content-box']}>
            {this.renderConfig()}
            {this.renderCellRelevance()}
            {this.renderOperateConfig()}
          </div>
        </div>
      </div>
    );
  }

  saveValue = ({ tagName, row, col, action }) => {
    this.props.settingConfig({ tagName, row, col, action });
    this.props.backfillConfig();
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

  showModalDelete = () => {
    const { intl } = this.props;
    const { showModalDelete, selectedKeys, allDataSource } = this.state;
    if (['entity', 'systemInfo'].includes(_.get(allDataSource, `["${selectedKeys[0]}"].propertyType`))) {
      message.warning(intl.formatMessage(messages.Rule17));
      return;
    }
    if (selectedKeys[0]) {
      this.setState({
        showModalDelete: !showModalDelete
      });
    }
  }

  showOrHideModal = (modal) => {
    this.isEdit = false;
    this.setState(modal);
  }

  handleCancel = () => {
    this.showOrHideModal({ objectSelectorVisiable: false });
  }

  renderOperateConfig() {
    const { intl, formula, selections, textVal, sqlInfo, basicOperate } = this.props;
    const { settingTab } = this.state;
    if (settingTab === intl.formatMessage(messages.operate)) {
      return (
        <OperateFormula
          formula={formula}
          selections={selections}
          textVal={textVal}
          sqlInfo={sqlInfo}
          intl={intl}
          basicOperate={basicOperate}
        />
      );
    }
  }

  renderConfig() {
    const { intl, appId, formula, selections, updateFlag, basicOperate, updatePrimaryKeys, sqlInfo } = this.props;
    const { settingTab } = this.state;
    if (settingTab === intl.formatMessage(messages.AttrSet) && this.getDataType()) {
      return (
        <InsertFormula
          appId={appId}
          allDataSource={this.state.allDataSource}
          formula={formula}
          selections={selections}
          updateFlag={updateFlag}
          basicOperate={basicOperate}
          updatePrimaryKeys={updatePrimaryKeys}
          sqlInfo={sqlInfo}
          intl={intl}
          type="dataSourceInfo"
        />
      );
    } else {
      return null;
    }
  }

  renderCellRelevanceDetail = (label, tagName, value) => {
    const { selections, intl } = this.props;
    return (
      <CellRelevance
        label={label}
        tagName={tagName}
        value={value}
        saveValue={this.saveValue}
        selections={selections}
        intl={intl}
      />
    );
  }

  renderCellRelevance() {
    const { settingTab, paneValue, paneUpValue, formulaValue } = this.state;
    const { intl } = this.props;
    switch (settingTab) {
      case intl.formatMessage(messages.parentPane): {
        return (
          <>
            {this.renderCellRelevanceDetail(intl.formatMessage(messages.leftPane), 'paneSettingConf', paneValue)}
            {this.renderCellRelevanceDetail(intl.formatMessage(messages.upPane), 'paneSettingUpConf', paneUpValue)}
          </>
        );
      }
      case intl.formatMessage(messages.Formula): return this.renderCellRelevanceDetail(intl.formatMessage(messages.Formula), 'formulaConf', formulaValue);
      default: return null;
    }
  }

  render() {
    const { loading, selectedKeys } = this.state;
    const { unfoldDataSourceSet, intl } = this.props;
    return (
      <div
        className={styles['data-source-set-box']}
        style={{ display: 'flex', flexDirection: 'column' }}
      >
        <div style={{ height: '50%' }}>
          <div className={styles.head}>
            <span>{intl.formatMessage(messages.DataManage)}</span>
            <Icon type="close-circle" theme="filled" onClick={unfoldDataSourceSet} style={{ cursor: 'pointer' }} />
          </div>
          <div className={styles.select}>
            <div className={styles['select-left']}>
              <Dropdown overlay={this.menu}>
                <span>
                  {/* <Icon type="plus" theme="filled" style={{ verticalAlign: 'middle' }} /> */}
                  <i className={styles['btn-add']} />
                  <Icon type="down" style={{ verticalAlign: 'middle' }} />
                </span>
              </Dropdown>
              <span style={{ marginRight: '6px' }}> | </span>
              <div
                className={classnames(
                  styles.delete,
                  {
                    [styles['btn-delete']]: selectedKeys[0],
                    [styles['btn-dis-delete']]: !selectedKeys[0]
                  }
                )}
                onClick={this.showModalDelete}
              />
            </div>
            <Search
              size="small"
              onSearch={this.searchItem}
            />
          </div>
          <div className={styles.content}>
            <Spin spinning={loading} style={{ width: '100%' }}>
              {this.dataSourceTree()}
            </Spin>
          </div>
        </div>
        {this.showFormulaConfig()}
        {this.showObjectSelectorModal()}
        {this.showDataTableSearchModal()}
        {this.modalDelete()}
        {this.showCustomServiceModal()}
      </div>
    );
  }
}
