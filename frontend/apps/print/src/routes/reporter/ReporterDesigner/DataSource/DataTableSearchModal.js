import React, { Component } from 'react';
import { Row, Col, Input, notification, Tree, Spin, Select, Form, Button } from 'sup-ui';
import _ from 'lodash';
// import { queryDataSource, queryDatatable, test } from 'root/services/datatableApi';
// import MyCodeMirror from 'root/components/common/CodeMirrorEditorComponent/CodeMirrorEditor';
// import iconPreview from 'root/assets/img/icons/report/icon_preview.svg';
import PreviewModal from './PreviewModal';
import styles from './DataSource.less';
import messages from '../messages';
import Modal from '../Modal/CommonModal';

const { Search } = Input;
const { TreeNode } = Tree;
const { Option } = Select;
const { Item: FormItem } = Form;

@Form.create()
export default class DataTableSearchModal extends Component {
  constructor(props) {
    super();
    const { editName, sqlInfo, unavailableSourceNames: names } = props;
    let dataSourceKey = 0;
    if (editName) {
      const { id } = sqlInfo[editName];
      dataSourceKey = names.length && names.includes(editName) ? null : id;
    }

    const { script, sql } = sqlInfo[editName] || {};

    this.state = {
      previewVisiable: false,
      previewDataVisiable: true,
      loading: false,
      script: editName ? (script || sql) : '',
      tableName: editName,
      tables: [],
      datasource: [],
      dataSourceKey
    };
  }

  componentDidMount() {
    this.fetchDataSource();
    const { dataSourceKey } = this.state;
    if (dataSourceKey !== null) {
      // this.fetchAllTables(dataSourceKey);
    }
  }

  onStateChange = (type, value) => {
    this.setState({ [type]: value });
  }

  onInputChange = (type, e) => {
    this.onStateChange(type, e.target.value);
  }

  onSearchTableName = (value) => {
    const tables = value ? _.filter(this.tables, (item) => ~item.tableName.toUpperCase().indexOf(value.toUpperCase())) : this.tables;
    this.onStateChange('tables', tables);
  }

  onSelectTreeNode = (selectedKey) => {
    this.setState({ selectedKey });
  }

  onChangeDataSource = (e) => {
    this.setState({ dataSourceKey: e });
    this.getAllTables(e);
  }

  getAllTables = (sourceKey) => {
    this.fetchAllTables(sourceKey);
    if (this.editor) {
      this.editor.clearAutoComplete();
    }
  }

  fetchDataSource = () => {
    // const { dataSourceKey } = this.state;
    // queryDataSource().then((res) => {
    //   if (res && +res.code === 200) {
    //     this.dataSourceMap = {};
    //     _.map(res.list, (item) => {
    //       this.dataSourceMap[item.id] = item.sourceName;
    //     });
    //     if (this.props.editName && this.state.dataSourceKey) {
    //       if (!_.find(res.list, item => item.id === this.state.dataSourceKey)) {
    //         this.setState({ dataSourceKey: null });
    //       } else {
    //         this.getAllTables(dataSourceKey);
    //       }
    //     }
    //     this.setState({ datasource: res.list });
    //   } else {
    //     this.handleErr();
    //   }
    // });
  }

  fetchAllTables = (id) => {
    // this.setState({
    //   loading: true
    // }, () => {
    //   queryDatatable({ id }).then((res) => {
    //     if (res && +res.code === 200) {
    //       this.tables = res.data;
    //       this.setState({
    //         tables: res.data
    //       });
    //     } else {
    //       this.handleErr();
    //     }
    //   }).finally(() => {
    //     this.setState({
    //       loading: false
    //     });
    //   });
    // });
  }

  updateCode=(editor, data, value) => {
    this.setState({
      script: value
    });
  }

  getNewScript = () => {
    const { script } = this.state;
    const rows = script.split('\n');
    let newScript = '';
    _.map(rows, (row) => {
      const index = row.indexOf('--');
      if (!~index) {
        newScript = `${newScript}${row} `;
      } else {
        newScript = `${newScript}${row.slice(0, index)} `;
      }
    });
    return newScript;
  }

  testSql = (isPreview) => {
    const { intl, sqlInfo } = this.props;
    const { script, tableName, dataSourceKey: id } = this.state;
    const { allDataSource, editName } = this.props;
    if (!script) {
      notification.warning({ message: intl.formatMessage(messages.enterSql) });
      return;
    }
    if (editName !== tableName && allDataSource[tableName]) {
      notification.warning({ message: intl.formatMessage(messages.SourceRule) });
      return;
    }
    this.newScript = this.getNewScript();
    this.params = this.parseSql();
    // test({ sql: this.newScript, id }).then((res) => {
    //   if (res && +res.code === 200) {
    //     if (res.error) {
    //       this.handleErr(res.msg);
    //     } else if (isPreview) {
    //       this.setState({ previewVisiable: true });
    //     } else {
    //       const columnNames = res.data;
    //       columnNames.unshift('_key_');
    //       columnNames.unshift('_table_');
    //       this.props.setDataTable(tableName, columnNames, {
    //         sql: this.newScript,
    //         script,
    //         id,
    //         params: this.params,
    //         sourceName: this.dataSourceMap[id],
    //         primaryKeys: sqlInfo ? _.get(sqlInfo[tableName], 'primaryKeys') : null,
    //         pkTypeTransfer: sqlInfo ? _.get(sqlInfo[tableName], 'pkTypeTransfer') : null
    //       });
    //       this.handleCancel();
    //     }
    //   } else {
    //     this.handleErr();
    //   }
    // });
  }

  parseSql = () => {
    const arr = this.newScript.match(/\${.*?}+/g) || [];
    const paramSet = new Set();
    _.map(arr, (item) => {
      const param = item.slice(2, -1).trim();
      if (param) paramSet.add(param.split(':')[0].trim());
    });
    return Array.from(paramSet);
  }

  showPreview = () => {
    if (this.state.previewVisiable) {
      return (
        <PreviewModal
          previewDataVisiable={this.state.previewDataVisiable}
          params={this.params}
          script={this.newScript}
          dataSourceKey={this.state.dataSourceKey}
          showOrHideModal={this.showOrHideModal}
          handleErr={this.handleErr}
          intl={this.props.intl}
        />
      );
    } else {
      return null;
    }
  }

  showOrHideModal = (modal) => {
    this.setState(modal);
  }

  showCodeMirror = () => {
    const { intl } = this.props;
    const tip = `${intl.formatMessage(messages.sheetTip1)} "\${abc}" ${intl.formatMessage(messages.sheetTip2)}
    ${intl.formatMessage(messages.eg)} select * from table where id = \${abc}`;
    // return (
    //   <React.Fragment>
    //     <MyCodeMirror
    //       ref={(node) => {
    //         this.editor = node ? node.getWrappedInstance() : {};
    //       }}
    //       value={this.state.script}
    //       onBeforeChange={this.updateCode}
    //       height={300}
    //       width="100%"
    //       language="sql"
    //       hideHeader
    //       useWrapMode
    //       tables={this.tables}
    //     />
    //     <div style={{ padding: '10px 10px 0 25px', marginTop: -125 }}>{tip}</div>
    //   </React.Fragment>
    // );
  }

  handleErr = (message) => {
    const { intl } = this.props;
    const msgObj = {};
    if (message === '连接超时') {
      msgObj.message = intl.formatMessage(messages.connectionTimeout);
    } else {
      msgObj.message = intl.formatMessage(messages.grammarError);
      msgObj.description = message;
    }
    msgObj.className = styles.notification;
    notification.warning(msgObj);
  }

  handleOk = () => {
    this.props.form.validateFields(
      (err) => {
        if (!err) {
          this.testSql(false);
        }
      },
    );
  }

  handleCancel = () => {
    if (this.editor) {
      this.editor.clearAutoComplete();
    }
    this.props.showOrHideModal({ dataTableVisiable: false });
  }

  addToSqlEditor = (selectedKey) => {
    this.setState({ selectedKey });
    if (this.editor) {
      this.editor.insert(selectedKey);
      this.editor.focus();
    }
  }

  renderTreeNodes = (data) => _.map(data, (item) => {
    const { tableName, columns = [] } = item;
    return (
      <TreeNode
        key={tableName}
        title={
          <span
            onDoubleClick={this.addToSqlEditor.bind(this, tableName)}
            className={styles.treeNode}
          >
            {tableName}
          </span>
        }
      >
        {
          _.map(columns, (col) => (
            <TreeNode
              className={styles.treeLeafNode}
              key={col.name}
              title={
                <span
                  onDoubleClick={this.addToSqlEditor.bind(this, col.name)}
                  className={styles.treeLeaf}
                >
                  {col.name}
                </span>
              }
            />
          ))
        }
      </TreeNode>
    );
  });

  render() {
    const formItemLayout = {
      labelCol: { span: 6 },
      wrapperCol: { span: 18 }
    };
    const formTailLayout = {
      wrapperCol: { span: 17, offset: 7 }
    };
    const { intl } = this.props;
    return (
      <Modal
        title={intl.formatMessage(messages.SqlSearch)}
        visible
        width={750}
        bodyStyle={{ padding: 10, height: 570 }}
        onCancel={this.handleCancel}
        footer={null}
      >
        <div className={styles.dataTableSearch}>
          <Row type="flex" align="middle" className="source-name">
            <FormItem {...formItemLayout} label={intl.formatMessage(messages.SourceName)}>
              {this.props.form.getFieldDecorator('name', {
                rules: [
                  { required: true, message: intl.formatMessage(messages.enterSourceName) },
                  { pattern: /^[A-Za-z][A-Za-z0-9_]{0,199}$/, message: '只允许输入以字母开头，字母数字和_的组合名称' },
                  { pattern: /\S/, message: '不能只输入空格' }
                ],
                initialValue: this.state.tableName
              })(
                <Input
                  maxLength="200"
                  autoComplete="off"
                  onChange={this.onInputChange.bind(this, 'tableName')}
                  disabled={!!this.props.editName}
                />
              )}
            </FormItem>
          </Row>
          <Row>
            <Col span={6}>
              <div className={styles.tableList}>
                <Select
                  showArrow
                  style={{ width: 171, margin: '2px 4px' }}
                  onChange={this.onChangeDataSource}
                  value={this.state.dataSourceKey || ''}
                >
                  {/* <Option value={0}>{intl.formatMessage(messages.InSideSource)}</Option> */}
                  {
                    _.map(this.state.datasource, (item) => {
                      return (<Option value={item.id} key={item.id}>{item.sourceName}</Option>);
                    })
                  }
                </Select>
                <Search
                  className={styles.tableSearch}
                  placeholder={intl.formatMessage(messages.enterName)}
                  onSearch={this.onSearchTableName}
                  style={{ width: '100%' }}
                />
                <Spin spinning={this.state.loading}>
                  <Tree
                    className={styles.list}
                    selectedKey={[this.state.selectedKey]}
                    onSelect={this.onSelectTreeNode}
                  >
                    {this.renderTreeNodes(this.state.tables)}
                  </Tree>
                </Spin>
              </div>

            </Col>
            <Col span={18}>
              <div className={styles.sqlEditor}>
                <div className={styles.header}>
                  <img
                    // src={iconPreview}
                    alt={intl.formatMessage(messages.preview)}
                    onClick={this.testSql.bind(this, true)}
                    style={{ cursor: 'pointer' }}
                  />
                </div>
                {this.showCodeMirror()}
              </div>
            </Col>
          </Row>
          <Row style={{ marginTop: 10 }}>
            <FormItem {...formTailLayout}>
              <Button type="primary" style={{ width: 140 }} onClick={this.handleOk}>{intl.formatMessage(messages.ok)}</Button>
              <Button htmlType="button" style={{ width: 140, marginLeft: 10 }} onClick={this.handleCancel}>{intl.formatMessage(messages.cancel)}</Button>
            </FormItem>
          </Row>
        </div>
        {this.showPreview()}
      </Modal>
    );
  }
}
