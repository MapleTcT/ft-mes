import React from 'react';
import {
  Layout,
  Radio,
  Row,
  Col,
  Input,
  Divider,
  Button,
  Table,
  message
} from 'sup-ui';
import { injectIntl } from 'react-intl';
// import { SupReference } from 'sup-rc-reference';
import SupIcon from 'sup-rc-icon';
import SupResize from 'sup-rc-resize';
import SupTree from 'sup-rc-tree';
import {
  getSourceGroup,
  getRoleSourceAuth,
  saveRoleSourceAuth,
  getUserSourceAuth,
  saveUserSourceAuth,
  getSourceTable
} from 'root/services/authority';
import styles from './styles.less';
import commonMessage from './messages';

const { Content } = Layout;

@injectIntl
export default class sourceAuth extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.state = {
      radioValue: true,
      selectedKeys: [],
      // distribution: [],
      selectedRowKeys: [],
      total: 200,
      current: 1,
      pageSize: 20,
      resKey: '',
      resName: '',
      tableurl: '',
      columns: [
        {
          title: intl.formatMessage(commonMessage.code),
          dataIndex: 'code',
          key: 'code',
          width: 200
        },
        {
          title: intl.formatMessage(commonMessage.name),
          dataIndex: 'name',
          key: 'name',
          width: 200
        },
        {
          title: intl.formatMessage(commonMessage.type),
          dataIndex: 'resType',
          key: 'resType',
          width: 200
        },
        {
          title: intl.formatMessage(commonMessage.sourceDetail),
          dataIndex: 'hasInfo',
          key: 'hasInfo',
          render: () => {
            return (<span />);
          }
        }
      ],
      gData: [
        {
          id: 'root',
          name: intl.formatMessage(commonMessage.sourceGroup),
          nameDisplay: intl.formatMessage(commonMessage.sourceGroup),
          children: []
        }
      ],
      tData: [],
      companyId: ''
    };
  }

  componentWillMount() {
    const { intl } = this.props;
    const companyJson = localStorage.getItem('loginMsg');
    if (companyJson) {
      this.setState({
        companyId: _.get(JSON.parse(companyJson), 'currentCompany.id', null)
      });
    }
    getSourceGroup().then((res) => {
      this.setState({
        gData: [
          {
            groupCode: 'root',
            groupName: intl.formatMessage(commonMessage.sourceGroup),
            resourceUrl: '',
            children: res.data.list
          }
        ]
      });
    });
  }

  generateTree = (data) => {
    const result = [];
    if (!Array.isArray(data)) {
      return result;
    }
    data.forEach((item) => {
      delete item.children;
    });
    const map = {};
    data.forEach((item) => {
      map[item.id] = item;
    });
    data.forEach((item) => {
      const parent = map[item.parentId];
      if (parent) {
        (parent.children || (parent.children = [])).push(item);
      } else {
        result.push(item);
      }
    });
    return result;
  }

  flat = (nodes, parentId, resultArr = []) => {
    if (!nodes || nodes.length === 0) return [];
    nodes.forEach((node) => {
      resultArr.push({ ...node });
      return this.flat(node.children, node.id, resultArr);
    });
  }

  onSelect = (key, node) => {
    // 点击根节点,不渲染页面,页面置空
    if (key[0] === 'root') {
      this.setState({ selectedKeys: [] });
      return;
    }
    const { resourceUrl, groupCode } = _.get(node, 'node.props.item', {});
    const { id, status } = this.props;
    const getSourceData = status === 'user' ? getUserSourceAuth : getRoleSourceAuth;
    getSourceData(id, groupCode).then((res) => {
      this.setState({
        selectedKeys: key,
        tableurl: resourceUrl,
        selectedRowKeys: res.data.data.dataResouceVOS.map((x) => x.resourceCode),
        selectRowRecord: res.data.data.dataResouceVOS.map((x) => {
          return {
            code: x.resourceCode,
            name: x.resourceName,
            resType: x.resourceType
          };
        }),
        radioValue: res.data.data.controlled
      }, () => {
        this.searchTable();
      });
    });
  }

  searchTable = () => {
    const {
      tableurl,
      companyId,
      current,
      pageSize,
      resKey,
      resName
    } = this.state;
    const { intl } = this.props;
    getSourceTable(tableurl, {
      current,
      pageSize,
      cid: companyId,
      resKey,
      resName
    }).then((res) => {
      const { list, pagination } = res.data;
      this.setState({
        tData: list,
        current: pagination.current,
        pageSize: pagination.pageSize,
        total: pagination.total
      });
    }).catch((err) => {
      if (err.status >= 400) {
        message.error(intl.formatMessage(commonMessage.serviceError));
      }
    });
  }

  radioChange = (e) => {
    this.setState({
      radioValue: e.target.value
    });
  }

  showTotal = () => {
    const { total } = this.state;
    const { intl } = this.props;
    return intl.formatMessage(commonMessage.total, { total });
  }

  pageChange = (current, pageSize) => {
    this.setState({
      current,
      pageSize
    });
  }

  onShowSizeChange = (current, pageSize) => {
    this.setState({
      current,
      pageSize
    }, () => {
      this.searchTable();
    });
  }

  saveSetting = () => {
    const { status, id, intl } = this.props;
    const { selectRowRecord, radioValue, selectedKeys } = this.state;
    const saveSourceData = status === 'user' ? saveUserSourceAuth : saveRoleSourceAuth;
    saveSourceData(id, selectedKeys[0], {
      controlled: radioValue,
      dataResouceVOS: radioValue ? selectRowRecord.map((x) => {
        return {
          resourceCode: x.code,
          resourceName: x.name,
          resourceType: x.resType
        };
      }) : []
    }).then((res) => {
      if (res.status === 200) {
        message.success(intl.formatMessage(commonMessage.sourceSuccess));
      }
    });
  }

  condition = (value, key) => {
    this.setState({
      [key]: value
    });
  }

  reset = () => {
    this.setState({
      resKey: '',
      resName: ''
    });
  }

  render() {
    const {
      selectedKeys,
      gData,
      radioValue,
      // distribution,
      tData,
      columns,
      total,
      current,
      pageSize,
      selectedRowKeys
    } = this.state;
    const {
      // status,
      intl
    } = this.props;
    const rowSelection = {
      selectedRowKeys,
      onSelect: (record, selected, selectedRows) => {
        const children = [];
        this.flat(record.children, record.id, children);
        let sk = selectedRows.map((x) => x.code);
        if (selected) {
          children.forEach((x) => {
            if (!sk.includes(x.code)) {
              sk.push(x.code);
            }
          });
        } else {
          const map = children.map((x) => x.code);
          sk = sk.filter((x) => !map.includes(x));
        }
        this.setState({
          selectedRowKeys: sk,
          selectRowRecord: selectedRows
        });
      },
      onSelectAll: (selected, selectedRows) => {
        this.setState({
          selectedRowKeys: selectedRows.map((x) => x.code),
          selectRowRecord: selectedRows
        });
      }
    };
    const height = document.documentElement.clientHeight - 320;
    return (
      <Content className={`${styles.content} sourceAuth`}>
        <SupResize
          min={220}
          max={320}
        >
          <div className={styles.themeTree}>
            <SupTree
              treeKey="groupCode"
              treeTitle="groupName"
              autoExpandRoot
              placeholder={intl.formatMessage(commonMessage.menuSearch)}
              dataSource={gData}
              showSearch={false}
              defaultExpandKeys={['root']}
              onSelect={this.onSelect}
              selectedKeys={selectedKeys}
            />
          </div>
          {
            selectedKeys.length > 0 ? (
              <div style={{ height: '100%', padding: '20px 15px 0' }}>
                <Row className={styles.gutRow}>
                  <Radio.Group onChange={this.radioChange} defaultValue={radioValue}>
                    <Radio value>{intl.formatMessage(commonMessage.sourceCtrl)}</Radio>
                    <Radio value={false}>{intl.formatMessage(commonMessage.sourceNoCtrl)}</Radio>
                  </Radio.Group>
                </Row>
                {
                  this.state.radioValue ? (
                    <Row>
                      <Row className={styles.gutRow}>
                        <Col style={{ width: 300 }} span={8}>
                          <span style={{ marginRight: 8 }}>{intl.formatMessage(commonMessage.sourceCode)}</span>
                          <Input
                            style={{ width: 200 }}
                            onChange={(e) => { this.condition(e.target.value, 'resKey'); }}
                            value={this.state.resKey}
                          />
                        </Col>
                        <Col style={{ width: 300 }} span={8}>
                          <span style={{ marginRight: 8 }}>{intl.formatMessage(commonMessage.sourceName)}</span>
                          <Input
                            style={{ width: 200 }}
                            onChange={(e) => { this.condition(e.target.value, 'resName'); }}
                            value={this.state.resName}
                          />
                        </Col>
                        <Button onClick={this.searchTable} style={{ marginRight: 8 }}>
                          {intl.formatMessage(commonMessage.tablesearch)}
                        </Button>
                        <Button onClick={this.reset}>
                          {intl.formatMessage(commonMessage.reset)}
                        </Button>
                      </Row>
                      <Divider style={{ margin: '16px 0' }} />
                    </Row>
                  ) : null
                }
                <Row className={styles.gutRow}>
                  <Button type="primary" ghost onClick={this.saveSetting}>
                    {intl.formatMessage(commonMessage.authority)}
                  </Button>
                  {/* <span style={{ margin: '0 8px' }}>同时分配给</span>
                  <SupReference
                    defaultValue={distribution || []}
                    multiple
                    placeholder={`请选择${status === 'user' ? '用户' : '角色'}`}
                    style={{
                      width: 330,
                      display: 'inline-block',
                      verticalAlign: 'middle'
                    }}
                    referenceView={{
                      title: `请选择${status === 'user' ? '用户' : '角色'}`,
                      type: status
                    }}
                    onChange={(data) => {
                      this.setState({
                        distribution: data
                      });
                    }}
                  /> */}
                </Row>
                {
                  this.state.radioValue ? (
                    <Table
                      rowKey="code"
                      size="middle"
                      columns={columns}
                      rowSelection={rowSelection}
                      dataSource={this.generateTree(tData)}
                      scroll={{
                        y: height
                      }}
                      style={{
                        height: 'calc(100% - 155px)'
                      }}
                      pagination={{
                        defaultCurrent: 1,
                        defaultPageSize: 20,
                        current,
                        pageSize,
                        showSizeChanger: true,
                        showQuickJumper: true,
                        total,
                        showTotal: this.showTotal,
                        onChange: this.pageChange,
                        onShowSizeChange: this.onShowSizeChange,
                        pageSizeOptions: ['10', '20', '50', '100', '200']
                      }}
                    />
                  ) : null
                }
              </div>
            ) : (
              <div className={styles.noneChoose}>
                <SupIcon className={styles.backIcon} type="iconpoint" />
                {intl.formatMessage(commonMessage.leftMenu)}
              </div>
            )
          }
        </SupResize>
      </Content>
    );
  }
}
