import React from 'react';
import {
  Layout,
  Button,
  Form,
  Divider,
  message,
  Modal,
  Icon,
  Tabs,
  Spin
} from 'sup-ui';
import { injectIntl } from 'react-intl';
import _ from 'lodash';
import {
  getMenuTree,
  getUserAuthority,
  getRoleAuthority,
  postUserAuthority,
  postRoleAuthority,
  getAllUserAssign,
  getAllRoleAssign,
  getSearchTree,
  getFromRole,
  getAssignFromRole,
  loadBap
} from 'root/services/authority';
import SupIcon from 'sup-rc-icon';
import SupResize from 'sup-rc-resize';
import SupTree from 'sup-rc-tree';
import AuthorTable from './authorityTable';
import ExtendAuthority from './extendAuthority';
import SourceAuth from './sourceAuth';
// import Edit from './edit';
import commonMessage from './messages';
import styles from './styles.less';

const { Content } = Layout;
const { TabPane } = Tabs;

@injectIntl
@Form.create()
export default class Authority extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    const { id, status, name } = this.getUrlParams();
    this.state = {
      name,
      gData: [],
      selectedKeys: [],
      up: true,
      down: true,
      refreshButton: true,
      status,
      id,
      all: false,
      Unallocated: [],
      Allocated: [],
      allAssign: [],
      fromRoleArr: [],
      // 标识已分配权限加载状态
      allAssignLoading: false,
      loadSource: false,
      treeLoading: false,
      tableLoading: false
    };
    this.updateList = [];
    this.treeCustomParams = [
      {
        align: 'middle',
        record: () => {
          return (
            <div className={styles.haveAuthority} onClick={this.getAllAssign}>
              {intl.formatMessage(commonMessage.treeDistribution)}
            </div>
          );
        }
      }
    ];
  }

  componentWillMount() {
    this.initTree();
    loadBap().then((res) => {
      this.setState({
        loadSource: res.data.install
      });
    });
  }

  componentDidMount() {
    const { status } = this.state;
    const { intl } = this.props;
    if (status === 'user') {
      setTimeout(() => {
        window.top.document.title = intl.formatMessage(
          commonMessage.userAuthority
        );
      }, 100);
    } else if (status === 'role') {
      setTimeout(() => {
        window.top.document.title = intl.formatMessage(
          commonMessage.roleAuthority
        );
      }, 100);
    }
  }

  getUrlParams = () => {
    const params = window.location.hash.split('?')[1];
    const ret = params.split('&');
    const obj = {};
    ret.forEach((item) => {
      const i = item.split('=');
      const key = i[0];
      const value = i[1];
      obj[key] = value;
    });
    return obj;
  };

  initTree = () => {
    this.setState({ treeLoading: true });
    getMenuTree().then((res) => {
      this.setState({
        treeLoading: false,
        gData: res.data.list
      });
    });
  };

  onSelect = (selectedKeys, obj) => {
    if (!obj.selected) {
      this.setState({
        selectedKeys: []
      });
      return;
    }
    this.setState(
      {
        selectedKeys,
        up: true,
        down: true,
        Unallocated: [],
        Allocated: [],
        all: false
      },
      () => {
        this.initTableData();
      }
    );
  };

  initTableData = () => {
    const { id, status, selectedKeys } = this.state;
    let func = null;
    let params = {};
    this.setState({
      tableLoading: true
    });
    if (status === 'user') {
      func = getUserAuthority;
      params = {
        menuId: selectedKeys[0],
        userId: id
      };
      getFromRole(params).then((res) => {
        const { data } = res;
        this.setState({
          fromRoleArr: _.get(data, 'data', [])
        });
      });
    } else {
      func = getRoleAuthority;
      params = {
        menuId: selectedKeys[0],
        roleId: id
      };
    }
    func(params).then((res) => {
      const { data } = res;
      this.updateList = [];
      this.setState({
        Unallocated: _.get(data, 'data.unassign', []),
        Allocated: _.get(data, 'data.assign', []),
        refreshButton: true,
        tableLoading: false
      });
    });
  };

  addAuthority = () => {
    const { Unallocated, status, id } = this.state;
    const nameKey = status === 'user' ? 'userPermission' : 'rolePermission';
    let func = null;
    let params = {};
    const addList = Unallocated.filter((item) => item.choose).map((item) => {
      const tipName = _.get(item, 'op.nameDisplay', '-') === '-'
        ? _.get(item, 'nameDisplay', '')
        : `${_.get(item, 'nameDisplay', '')}-${_.get(
          item,
          'op.nameDisplay',
          ''
        )}`;
      return {
        tipName,
        ..._.get(item, `op.${nameKey}`, {})
      };
    });
    const validError = this.validAuthority(addList);
    if (validError) {
      message.error(validError.errorName);
      return;
    }
    if (status === 'user') {
      func = postUserAuthority;
      params = {
        list: [
          {
            userId: id,
            addList,
            deleteList: []
          }
        ]
      };
    } else {
      func = postRoleAuthority;
      params = {
        list: [
          {
            roleId: id,
            addList,
            deleteList: []
          }
        ]
      };
    }
    func(params).then(() => {
      message.success(
        this.props.intl.formatMessage(commonMessage.authoritySuccess)
      );
      this.initTableData();
    });
  };

  validAuthority = (validArr) => {
    const { intl } = this.props;
    const retObj = validArr.find(
      (item) => (item.assignPosFlag && !item.positions)
        || (item.assignDeptFlag && !item.departments)
        || (item.assignStaffFlag && !item.staffs)
    );
    if (retObj) {
      const errorName = retObj.tipName;
      if (retObj.assignPosFlag && !retObj.positions) {
        retObj.errorName = intl.formatMessage(commonMessage.assignPosition, {
          errorName
        });
      } else if (retObj.assignDeptFlag && !retObj.departments) {
        retObj.errorName = intl.formatMessage(commonMessage.assignDepartment, {
          errorName
        });
      } else if (retObj.assignStaffFlag && !retObj.staffs) {
        retObj.errorName = intl.formatMessage(commonMessage.assignStaff, {
          errorName
        });
      }
    }
    return retObj;
  };

  validHasItem = (validArr) => {
    const retObj = validArr.find((item) => {
      return (
        !item.positionFlag
        && !item.departmentFlag
        && !item.assignPosFlag
        && !item.assignDeptFlag
        && !item.assignStaffFlag
        && !item.dealerPermissionFlag
        && !item.noRestrictFlag
      );
    });
    return retObj;
  };

  storageUpdateList = (node) => {
    if (this.updateList.filter((item) => item.id === node.id).length === 0) {
      this.updateList.push(node);
    } else {
      Object.assign(
        this.updateList.find((item) => item.id === node.id),
        node
      );
    }
  };

  saveAuthority = () => {
    const { status, id, all } = this.state;
    const { intl } = this.props;
    let func = null;
    let params = {};
    const validError = this.validAuthority(this.updateList);
    const opValidObj = this.validHasItem(this.updateList);
    if (opValidObj) {
      message.error(
        intl.formatMessage(commonMessage.noSet, { name: opValidObj.tipName })
      );
      return;
    }
    if (validError) {
      message.error(validError.errorName);
      return;
    }
    if (status === 'user') {
      func = postUserAuthority;
      params = {
        list: [
          {
            userId: id,
            addList: this.updateList,
            deleteList: []
          }
        ]
      };
    } else {
      func = postRoleAuthority;
      params = {
        list: [
          {
            roleId: id,
            addList: this.updateList,
            deleteList: []
          }
        ]
      };
    }
    func(params).then(() => {
      message.success(intl.formatMessage(commonMessage.updateSuccess));
      if (all) {
        this.initAllTable();
      } else {
        this.initTableData();
      }
    });
  };

  deleteAuthority = () => {
    const { intl } = this.props;
    const { Allocated, allAssign, status, id, all } = this.state;
    const nameKey = status === 'user' ? 'userPermission' : 'rolePermission';
    let func = null;
    let params = {};
    let deleteList = [];
    const _self = this;
    if (all) {
      deleteList = allAssign
        .filter((item) => item.choose)
        .map((item) => {
          return _.get(item, `op.${nameKey}`, {});
        });
    } else {
      deleteList = Allocated.filter((item) => item.choose).map((item) => {
        return _.get(item, `op.${nameKey}`, {});
      });
    }
    if (status === 'user') {
      func = postUserAuthority;
      params = {
        list: [
          {
            userId: id,
            addList: [],
            deleteList
          }
        ]
      };
    } else {
      func = postRoleAuthority;
      params = {
        list: [
          {
            roleId: id,
            addList: [],
            deleteList
          }
        ]
      };
    }
    Modal.confirm({
      title: intl.formatMessage(commonMessage.confirmDeleteAuthority),
      content: intl.formatMessage(commonMessage.confirmDelete),
      okText: intl.formatMessage(commonMessage.confirm),
      onOk() {
        func(params).then(() => {
          if (all) {
            _self.initAllTable();
          } else {
            _self.initTableData();
          }
          message.success(intl.formatMessage(commonMessage.deleteSucess));
        });
      },
      onCancel() {}
    });
  };

  initAllTable = () => {
    const { id, status } = this.state;
    let func = null;
    let params = {};
    this.setState({
      allAssignLoading: true,
      tableLoading: true
    });
    if (status === 'user') {
      func = getAllUserAssign;
      params = {
        userId: id
      };
      getAssignFromRole(params).then((res) => {
        const { data } = res;
        this.setState({
          fromRoleArr: _.get(data, 'list', [])
        });
      });
    } else {
      func = getAllRoleAssign;
      params = {
        roleId: id
      };
    }
    func(params).then((res) => {
      this.setState({
        allAssign: res.data.list,
        refreshButton: true,
        allAssignLoading: false,
        tableLoading: false
      });
    });
  };

  getAllAssign = () => {
    this.setState(
      {
        all: true,
        selectedKeys: [],
        up: true,
        down: true,
        Unallocated: [],
        Allocated: []
      },
      () => {
        this.initAllTable();
      }
    );
  };

  up = () => {
    this.setState({
      up: false,
      down: true
    });
  };

  center = () => {
    this.setState({
      up: true,
      down: true
    });
  };

  down = () => {
    this.setState({
      down: false,
      up: true
    });
  };

  getClientHeight = (tableHeight = true) => {
    // 可是区域整体浏览器高度
    const { Allocated, up, Unallocated, down, status } = this.state;
    const { clientHeight } = document.documentElement;
    const opHead = 50;
    const tableHead = tableHeight ? 72 : 0;
    const padding = 20;
    const header = 56;
    const tabHeight = status === 'user' ? 50 : 0;
    let useHeight = clientHeight - tableHead - opHead - padding - header - tabHeight;
    if (Allocated.length > 0 && Unallocated.length !== 0) {
      // 分割线高度
      const divider = tableHeight ? 56 : 0;
      if (up && down) {
        useHeight = (useHeight - divider - opHead - tableHead - header - tabHeight) / 2;
      } else {
        useHeight -= divider;
      }
    }
    return useHeight;
  };

  getExtendHeight = () => {
    // 可是区域整体浏览器高度
    const { clientHeight } = document.documentElement;
    const opHead = 50;
    const tableHead = 30;
    const padding = 20;
    const header = 56;
    const tabsHeight = 50;
    const useHeight = clientHeight - tableHead - opHead - padding - header - tabsHeight;
    return useHeight;
  };

  vagueSearch = (value) => {
    getSearchTree({ keyword: value, restrict: false }).then((res) => {
      this.setState({
        selectedKeys: [],
        gData: res.data.list
      });
    });
  };

  accurateSearch = (item) => {
    getSearchTree({ id: item.id }).then((res) => {
      this.setState(
        {
          selectedKeys: [],
          gData: res.data.list
        },
        () => {
          this.onSelect([item.id.toString()], { selected: true });
        }
      );
    });
  };

  renderNoAuthority = (msg) => {
    const { allAssignLoading } = this.state;
    return (
      <div className={styles.noData}>
        {allAssignLoading ? (
          <Spin />
        ) : (
          <>
            <Icon type="zanwushuju" />
            <span>{msg}</span>
          </>
        )}
      </div>
    );
  };

  renderCommon = () => {
    const { Unallocated, Allocated, status, all, allAssign, tableLoading } = this.state;
    const { intl } = this.props;
    return (
      <div className={styles.tableWrapper}>
        {!all ? (
          <div className={styles.singleTableWrapper}>
            {Allocated.length > 0 ? (
              <div
                className={styles.tableBox}
                style={{
                  display: this.state.up ? 'block' : 'none',
                  height:
                    this.state.up && !this.state.down
                      ? this.getClientHeight(false)
                      : 'calc((100% - 64px)/2)'
                }}
              >
                <div className={styles.tableTitle}>
                  <span>{intl.formatMessage(commonMessage.distribution)}</span>
                  <div className={styles.rightButton}>
                    <Button
                      type="primary"
                      ghost
                      style={{ marginRight: 10 }}
                      disabled={this.state.refreshButton}
                      onClick={_.debounce(this.saveAuthority, 200)}
                    >
                      {intl.formatMessage(commonMessage.save)}
                    </Button>
                    <Button
                      icon="delete"
                      disabled={!Allocated.some((item) => item.choose)}
                      onClick={this.deleteAuthority}
                    />
                  </div>
                </div>
                <AuthorTable
                  ref={(node) => {
                    this.assign = node;
                  }}
                  status={
                    status === 'user' ? 'userPermission' : 'rolePermission'
                  }
                  data={Allocated}
                  type="assign"
                  storageUpdateList={this.storageUpdateList}
                  contentHeight={this.getClientHeight()}
                  refreshDelete={() => {
                    this.forceUpdate();
                  }}
                  refreshButton={() => {
                    this.setState({
                      refreshButton: false
                    });
                  }}
                />
              </div>
            ) : null}
            {Allocated.length > 0 && Unallocated.length !== 0 ? (
              <Divider style={{ padding: '0 20px' }}>
                <Button
                  icon="double-up"
                  className={styles.layoutButton}
                  onClick={this.up}
                  title="向上"
                />
                <Button
                  icon="minus"
                  className={styles.layoutButton}
                  style={{ margin: '0 8px' }}
                  title="居中"
                  onClick={this.center}
                />
                <Button
                  icon="double-down"
                  className={styles.layoutButton}
                  onClick={this.down}
                  title="向下"
                />
                {/* <Icon type="double-up" onClick={this.up} title="向上" />
                    <Icon type="minus" style={{ margin: '0 8px' }} title="居中" onClick={this.center} />
                    <Icon type="double-down" onClick={this.down} title="向下" /> */}
              </Divider>
            ) : null}
            {Unallocated.length > 0 ? (
              <div
                className={styles.tableBox}
                style={{
                  display: this.state.down ? 'block' : 'none',
                  height:
                    !this.state.up && this.state.down
                      ? this.getClientHeight(false) - 56
                      : 'calc((100% - 64px)/2)'
                }}
              >
                <div className={styles.tableTitle}>
                  <span>{intl.formatMessage(commonMessage.unallocated)}</span>
                  <Button
                    type="primary"
                    ghost
                    className={styles.rightButton}
                    onClick={this.addAuthority}
                    disabled={!Unallocated.some((item) => item.choose)}
                  >
                    {intl.formatMessage(commonMessage.authority)}
                  </Button>
                </div>
                <AuthorTable
                  ref={(node) => {
                    this.unassign = node;
                  }}
                  status={
                    status === 'user' ? 'userPermission' : 'rolePermission'
                  }
                  data={Unallocated}
                  type="unassign"
                  contentHeight={this.getClientHeight()}
                  refreshDelete={() => {
                    this.forceUpdate();
                  }}
                />
              </div>
            ) : null}
          </div>
        ) : (
          <div className={styles.singleTableWrapper}>
            {allAssign.length > 0 ? (
              <div>
                <div className={styles.allOp}>
                  <Button
                    type="primary"
                    ghost
                    style={{ marginRight: 10 }}
                    disabled={this.state.refreshButton}
                    onClick={_.debounce(this.saveAuthority, 200)}
                  >
                    {intl.formatMessage(commonMessage.save)}
                  </Button>
                  <Button style={{ marginRight: 10, display: 'none' }}>
                    {intl.formatMessage(commonMessage.export)}
                  </Button>
                  <Button
                    icon="delete"
                    disabled={!allAssign.some((item) => item.choose)}
                    onClick={this.deleteAuthority}
                  />
                </div>
                <div className={styles.allTitle}>
                  {intl.formatMessage(commonMessage.treeDistribution)}
                </div>
                <div style={{ padding: '0 20px' }}>
                  <AuthorTable
                    ref={(node) => {
                      this.assign = node;
                    }}
                    status={
                      status === 'user' ? 'userPermission' : 'rolePermission'
                    }
                    data={allAssign}
                    type="assign"
                    storageUpdateList={this.storageUpdateList}
                    contentHeight={this.getClientHeight() - 50}
                    refreshDelete={() => {
                      this.forceUpdate();
                    }}
                    refreshButton={() => {
                      this.setState({
                        refreshButton: false
                      });
                    }}
                  />
                </div>
              </div>
            ) : (
              this.renderNoAuthority(
                this.props.intl.formatMessage(commonMessage.noData)
              )
            )}
          </div>
        )}
        <Spin
          spinning={tableLoading}
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            height: '100%',
            width: '100%',
            background: 'rgba(240, 242, 245 , 0.3)',
            zIndex: 999
          }}
        />
      </div>
    );
  };

  render() {
    const {
      gData,
      selectedKeys,
      name,
      status,
      fromRoleArr,
      all,
      id,
      treeLoading,
      tableLoading
    } = this.state;
    const { intl } = this.props;
    return (
      <Layout className={`${styles.wrap} authority`}>
        <span className={styles.authorityTitle}>
          {intl.formatMessage(commonMessage.authorityTitle, {
            name: decodeURI(name)
          })}
        </span>
        <Tabs
          animated={false}
          defaultActiveKey="1"
          className={styles.authTabs}
          tabBarStyle={{
            height: '50px',
            margin: '0 0 5px 0',
            display: 'flex',
            background: '#fff',
            justifyContent: 'center',
            boxShadow: '3px 4px 4px rgba(9,9,9,.05)'
          }}
        >
          <TabPane
            tab={intl.formatMessage(commonMessage.functionAuth)}
            key="1"
            className={styles.singleTabPane}
          >
            <Content className={styles.content}>
              <SupResize min={220} max={320}>
                <div className={styles.themeTree}>
                  <SupTree
                    treeKey="id"
                    treeTitle="nameDisplay"
                    placeholder={intl.formatMessage(commonMessage.menuSearch)}
                    dataSource={gData}
                    onSelect={this.onSelect}
                    selectedKeys={selectedKeys}
                    customParams={this.treeCustomParams}
                    onSearch={(param, type) => {
                      if (type === 'fuzzy') {
                        this.vagueSearch(param.title);
                      } else if (type === 'advanced') {
                        this.accurateSearch(param);
                      } else {
                        this.setState(
                          {
                            gData: [],
                            selectedKeys: []
                          },
                          () => {
                            this.initTree();
                          }
                        );
                      }
                    }}
                    fuzzyParams={
                      {
                        url: '/inter-api/rbac/v1/menus/associate/ref',
                        param: 'keyword',
                        otherParams: 'restrict=false&size=10000',
                        callback: (data) => {
                          return data.list.map((item) => {
                            return {
                              key: item.code,
                              id: item.id,
                              title: item.nameDisplay
                            };
                          });
                        }
                      }
                    }
                  />
                  <Spin
                    spinning={treeLoading}
                    style={{
                      position: 'absolute',
                      top: 0,
                      left: 0,
                      height: '100%',
                      width: '100%',
                      background: 'rgba(240, 242, 245 , 0.3)',
                      zIndex: 999
                    }}
                  />
                </div>
                {selectedKeys.length > 0 || all ? (
                  <div style={{ height: '100%' }}>
                    {status === 'user' ? (
                      <Tabs
                        animated={false}
                        defaultActiveKey="1"
                        className={styles.authTabs}
                        tabBarStyle={{
                          height: '50px',
                          margin: '0 19px',
                          background: '#fff'
                        }}
                      >
                        <TabPane
                          tab={intl.formatMessage(commonMessage.userAuth)}
                          key="1"
                          className={styles.singleTabPane}
                        >
                          {this.renderCommon()}
                        </TabPane>
                        <TabPane
                          tab={intl.formatMessage(
                            commonMessage.extendRoleAuthority
                          )}
                          key="3"
                          className={styles.singleTabPane}
                        >
                          <div style={{ padding: '10px 20px 0' }}>
                            {fromRoleArr.length > 0 ? (
                              <ExtendAuthority
                                status={
                                  status === 'user'
                                    ? 'userPermission'
                                    : 'rolePermission'
                                }
                                data={fromRoleArr}
                                contentHeight={this.getExtendHeight()}
                                loading={tableLoading}
                              />
                            ) : (
                              this.renderNoAuthority(
                                intl.formatMessage(
                                  commonMessage.noRoleAuthority
                                )
                              )
                            )}
                          </div>
                        </TabPane>
                      </Tabs>
                    ) : (
                      this.renderCommon()
                    )}
                  </div>
                ) : (
                  <div className={styles.noneChoose}>
                    <SupIcon className={styles.backIcon} type="iconpoint" />
                    {intl.formatMessage(commonMessage.leftMenu)}
                  </div>
                )}
              </SupResize>
            </Content>
          </TabPane>
          {this.state.loadSource ? (
            <TabPane
              tab={intl.formatMessage(commonMessage.sourceAuth)}
              key="2"
              className={styles.singleTabPane}
            >
              <iframe
                title="sourceAuthority"
                style={{
                  width: '100%',
                  height: '100%',
                  outline: 'none',
                  border: 'none'
                }}
                src={`/sourceAuth/sourceAuthority.html?${
                  status === 'user' ? 'userId' : 'roleId'
                }=${id}&type=${status}`}
              />
            </TabPane>
          ) : null}
          <TabPane
            tab={intl.formatMessage(commonMessage.dataGroup)}
            key="3"
            className={styles.singleTabPane}
          >
            <SourceAuth status={status} id={id} />
          </TabPane>
        </Tabs>
      </Layout>
    );
  }
}
