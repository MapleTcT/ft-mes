// 单个角色下的关联用户列表
// 所有角色下的关联用户列表

import React from 'react';
import SupTable from 'sup-rc-table';
import { injectIntl } from 'react-intl';
import { Icon, message, Modal } from 'sup-ui';
import { SupReferenceView } from 'sup-rc-reference';
import { getCurrentCompanyId } from 'root/utils/index';
import {
  fetchRoleUser,
  removeUserRoleConnection,
  addUserRoleConnection
} from 'root/services/role';

import messages from './messages';
import {
  disableDisconnectRole,
  getRoleSourceText,
  checkDisabledConnectedRoles
} from './utils';
import style from './style.less';

const currentCompanyId = getCurrentCompanyId();

class RoleUserTable extends React.Component {
  state = {
    dataSource: [],
    refUserVisible: false,
    keyword: '',
    pagination: {
      current: 1,
      pageSize: 20,
      total: 0
    }
  };

  constructor(props) {
    super(props);

    const { intl } = props;

    this.state.columns = [
      {
        title: intl.formatMessage(messages.roleColumnUserName),
        width: 200,
        dataIndex: 'user.userName'
      },
      {
        title: intl.formatMessage(messages.roleColumnStaffName),
        width: 200,
        dataIndex: 'user.personName'
      },
      {
        title: intl.formatMessage(messages.roleColumnPersonCode),
        width: 200,
        dataIndex: 'user.personCode'
      },
      {
        title: intl.formatMessage(messages.roleColumnRoleSource),
        dataIndex: 'fromPosition',
        align: 'center',
        width: 200,
        render: (v) => {
          const value = messages[getRoleSourceText(v)];
          return <span>{intl.formatMessage(value)}</span>;
        }
      },
      {
        title: intl.formatMessage(messages.roleColumnOperate),
        width: 200,
        authority: () => {
          return this.props.hasAuth(['deleteRoleUser']);
        },
        type: 'operation',
        render: (_, row) => {
          const hasAuth = this.props.hasAuth('deleteRoleUser');
          if (!hasAuth) return null;
          const disabled = disableDisconnectRole(row);
          const title = disabled
            ? intl.formatMessage(messages.disableDisConnectRole)
            : null;
          return (
            <div className={style.roleColumnOperate}>
              <span className="role-operate-actions" title={title}>
                <a
                  disabled={disabled}
                  onClick={(e) => {
                    e.preventDefault();
                    if (disableDisconnectRole(row)) return;
                    Modal.confirm({
                      title: intl.formatMessage(
                        messages.disConnectRoleConfirmTitle,
                        { personName: row.user.personName }
                      ),
                      content: intl.formatMessage(
                        messages.disConnectRoleConfirmContent
                      ),
                      onOk: () => {
                        this.disConnectRole([row.id], () => {
                          if (this.selecteRows && this.selecteRows.length) {
                            const index = this.selecteRows.findIndex(
                              (d) => d.id === row.id
                            );
                            if (index > -1) {
                              this.selecteRows.splice(index, 1);
                              this.refreshDisConnectBtnState();
                            }
                          }
                        });
                      }
                    });
                  }}
                >
                  {intl.formatMessage(messages.roleColumnOperateRemoveConnect)}
                </a>
              </span>
            </div>
          );
        }
      }
    ];

    this.btnColumns = [
      {
        key: 'connectUser',
        authority: this.props.hasAuth('addRoleUser'),
        className: 'ant-btn-primary ant-btn-background-ghost',
        content: () => (
          <div>
            <Icon type="plus" style={{ marginRight: 5, fontSize: '13px' }} />
            {intl.formatMessage(messages.connectRoleBtn)}
          </div>
        ),
        callback: () => {
          this.setState({
            refUserVisible: true
          });
        }
      },
      {
        key: 'removeConnect',
        disabled: true,
        authority: this.props.hasAuth('deleteRoleUser'),
        content: intl.formatMessage(messages.disConnectRoleBtn),
        callback: () => {
          if (this.selecteRows && this.selecteRows.length) {
            Modal.confirm({
              title: intl.formatMessage(
                messages.disConnectRoleConfirmBulkTitle
              ),
              content: intl.formatMessage(
                messages.disConnectRoleConfirmContent
              ),
              onOk: () => {
                // 先校验不允许解除的岗位
                const errs = checkDisabledConnectedRoles(
                  this.selecteRows,
                  intl
                );
                if (errs) {
                  message.error(
                    intl.formatMessage(messages.userDisableDisConnectRole, {
                      roles: errs
                    })
                  );
                  return false;
                }
                const roleUserIds = this.selecteRows.map((d) => d.id);

                this.disConnectRole(roleUserIds, () => {
                  this.selecteRows = [];
                  this.refreshDisConnectBtnState();
                });
              }
            });
          } else {
            message.info(intl.formatMessage(messages.chooseRoleFirst));
          }
        }
      },
      {
        key: 'exportBtn',
        content: intl.formatMessage(messages.exportBtn),
        callback: () => {
          this.props.openExportModal(this);
        }
      }
    ];
  }

  refreshDisConnectBtnState() {
    const { selecteRows } = this;
    let disabled = true;
    if (selecteRows && selecteRows.length) {
      disabled = false;
    }
    const removeBtnObj = this.btnColumns[1];
    if (!!removeBtnObj.disabled !== disabled) {
      removeBtnObj.disabled = disabled;
      this.setState({});
    }
  }

  handleExport(all) {
    const {
      currentSelectRole: { id: roleId }
    } = this.props;
    // 当前关联角色id
    const params = { roleId };
    // 当前页需附加选中项或者分页信息
    if (!all) {
      const { selecteRows } = this;
      if (selecteRows && selecteRows.length) {
        const roleUserIds = selecteRows.map((d) => d.id);
        if (roleUserIds) {
          params.roleUserIds = roleUserIds;
        }
      } else {
        const {
          pagination: { pageSize, current }
        } = this.state;
        params.current = current;
        params.pageSize = pageSize;
      }
    }
    return params;
  }

  // 解除用户角色关联
  disConnectRole = (id, cb) => {
    const { intl } = this.props;
    removeUserRoleConnection(id.join(',')).then(() => {
      message.success(intl.formatMessage(messages.disConnectRoleSucceess));
      this.loadAllUserList();
      if (cb) {
        cb();
      }
    });
  };

  loadAllUserList() {
    const {
      currentSelectRole: { code: roleCode }
    } = this.props;
    const {
      pagination: { current, pageSize },
      keyword
    } = this.state;
    fetchRoleUser({
      current,
      pageSize,
      roleCode,
      keyword
    }).then((res) => {
      const {
        data: { list, pagination: pager }
      } = res;

      this.setState({
        pagination: pager,
        dataSource: list
      });
      this.selecteRows = [];
      this.refreshDisConnectBtnState();
    });
  }

  handleOnSearch = ({ pagination, keyword }) => {
    let nextKeyword = '';
    if (keyword && keyword.keyword) {
      nextKeyword = keyword.keyword;
    }
    this.setState(
      {
        pagination,
        keyword: nextKeyword
      },
      () => {
        this.loadAllUserList();
      }
    );
  };

  componentDidMount() {
    this.loadAllUserList();
    // 判断表格操作栏是否固定
    if (this.table && this.table.changeOperationFixedStatus) {
      this.table.changeOperationFixedStatus();
    }
  }

  handleChangeSelectItem = (keys, rows) => {
    this.selecteRows = rows;
    this.refreshDisConnectBtnState();
  };

  updateColumns = (columns) => {
    this.setState({ columns });
  };

  render() {
    const { dataSource, pagination, refUserVisible, columns } = this.state;
    const {
      currentSelectRole: { name: roleName },
      intl
    } = this.props;

    return (
      <>
        <SupReferenceView
          type="user"
          title={intl.formatMessage(messages.userRefTitle)}
          height="600px"
          onCancel={() => {
            this.setState({ refUserVisible: false });
          }}
          companyConfig={{
            parentId: currentCompanyId,
            disabled: true
          }}
          maskClosable={false}
          destroyOnClose
          visible={refUserVisible}
          multiple
          onOk={(usersArr) => {
            const users = usersArr.map((user) => {
              const { name, code, userId } = user;
              return {
                ...user,
                id: userId,
                // 角色需要personName, personCode字段供冗余显示
                personName: name,
                personCode: code
              };
            });
            this.setState({
              refUserVisible: false
            });

            const {
              currentSelectRole: { id: roleId }
            } = this.props;

            addUserRoleConnection({ roleId, users }).then(() => {
              message.success(intl.formatMessage(messages.connectRoleSuccess));
              this.loadAllUserList();
            });
          }}
        />
        <SupTable
          ref={(ref) => {
            this.table = ref;
          }}
          rowKey="id"
          updateColumns={this.updateColumns}
          onSelectItem={this.handleChangeSelectItem}
          onSearch={this.handleOnSearch}
          showSelection
          pagination={pagination}
          btnColumns={this.btnColumns}
          tableKey="roleUserTable"
          operationBarTitle={roleName}
          dataSource={dataSource}
          columns={columns}
          showSearchIcon={false}
        />
      </>
    );
  }
}

export default injectIntl(RoleUserTable);
