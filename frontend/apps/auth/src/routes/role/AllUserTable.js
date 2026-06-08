// 所有角色下的关联用户列表

import React from 'react';
import SupTable from 'sup-rc-table';
import { injectIntl } from 'react-intl';
import { message, Modal, Divider } from 'sup-ui';
import { fetchRoleUser, removeUserRoleConnection } from 'root/services/role';
import messages from './messages';

import style from './style.less';
import {
  disableDisconnectRole,
  getRoleSourceText,
  checkDisabledConnectedRoles
} from './utils';

class AllUserTable extends React.Component {
  state = {
    dataSource: [],
    pagination: {
      current: 1,
      pageSize: 20
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
        title: intl.formatMessage(messages.roleColumnOwnRole),
        dataIndex: 'role.name',
        align: 'center',
        width: 200,
        render: (value) => {
          return <span className={style.roleTagName}>{value}</span>;
        }
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
        width: 300,
        authority: () => {
          return this.props.hasAuth(['deleteRoleUser', 'addRolePermissions']);
        },
        type: 'operation',
        render: (_, row) => {
          const btns = [
            {
              onClick: (e) => {
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
              },
              text: intl.formatMessage(messages.roleColumnOperateRemoveConnect),
              key: 'deleteRoleUser',
              authCode: 'deleteRoleUser',
              disabled: disableDisconnectRole(row)
            },
            {
              onClick: (e) => {
                e.preventDefault();
                this.openAuthModal(row);
              },
              text: intl.formatMessage(messages.roleColumnOperateRoleAuth),
              key: 'addRolePermissions',
              authCode: 'addRolePermissions'
            }
          ];

          const authBtns = btns.filter((btn) => {
            return this.props.hasAuth(btn.authCode);
          });

          if (!authBtns.length) return null;

          return (
            <div className={style.operateCell}>
              <div>
                {authBtns.map((btn, i) => {
                  // FIXME 通过解除key来判断
                  const { disabled } = btn;
                  const title = disabled
                    ? intl.formatMessage(messages.disableDisConnectRole)
                    : null;
                  return (
                    <>
                      <span title={title}>
                        <a onClick={btn.onClick} disabled={disabled}>
                          {btn.text}
                        </a>
                      </span>
                      {i < authBtns.length - 1 ? (
                        <Divider type="vertical" />
                      ) : null}
                    </>
                  );
                })}
              </div>
            </div>
          );
        }
      }
    ];

    this.btnColumns = [
      {
        key: 'removeConnect',
        authority: this.props.hasAuth('deleteRoleUser'),
        content: intl.formatMessage(messages.disConnectRoleBtn),
        disabled: true,
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
                const roleUserId = this.selecteRows.map((d) => d.id);
                this.disConnectRole(roleUserId, () => {
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

  openAuthModal({ role: row }) {
    if (process.env.NODE_ENV === 'development') {
      const url = `http://${window.location.host}/#/authority?id=${row.id}&status=role&name=${row.name}`;
      window.open(url);
    } else {
      // const url = `http://${window.location.host}/auth/#/authority?id=${row.id}&status=role&name=${row.name}`;
      const url = `http://${window.location.host}/#/runtime-fullscreen/runtime-fullscreen/authority-set?id=${row.id}&status=role&name=${row.name}`;
      window.open(url);
    }
  }

  refreshDisConnectBtnState() {
    const { selecteRows } = this;
    let disabled = true;
    if (selecteRows && selecteRows.length) {
      disabled = false;
    }
    const removeBtnObj = this.btnColumns[0];
    if (!!removeBtnObj.disabled !== disabled) {
      removeBtnObj.disabled = disabled;
      this.setState({});
    }
  }

  handleExport(all) {
    const params = {};
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
    const { pagination } = this.state;
    const { companyId: cid } = this.props;
    fetchRoleUser({ ...pagination, cid }).then((res) => {
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

  componentDidMount() {
    this.loadAllUserList();
    // 判断表格操作栏是否固定
    if (this.table && this.table.changeOperationFixedStatus) {
      this.table.changeOperationFixedStatus();
    }
  }

  handleOnSearch = ({ pagination }) => {
    this.setState(
      {
        pagination
      },
      () => {
        this.loadAllUserList();
      }
    );
  };

  handleChangeSelectItem = (keys, rows) => {
    this.selecteRows = rows;
    this.refreshDisConnectBtnState();
  };

  updateColumns = (columns) => {
    this.setState({ columns });
  };

  render() {
    const { dataSource, pagination, columns } = this.state;
    const { intl } = this.props;

    return (
      <>
        <SupTable
          ref={(ref) => {
            this.table = ref;
          }}
          rowKey="id"
          updateColumns={this.updateColumns}
          onSearch={this.handleOnSearch}
          onSelectItem={this.handleChangeSelectItem}
          showSelection
          pagination={pagination}
          btnColumns={this.btnColumns}
          tableKey="allUserTable"
          operationBarTitle={intl.formatMessage(messages.tableTitleAllUser)}
          dataSource={dataSource}
          columns={columns}
          showSearchIcon={false}
        />
      </>
    );
  }
}

export default injectIntl(AllUserTable);
