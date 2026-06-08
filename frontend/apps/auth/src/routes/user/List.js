import React from 'react';
import { injectIntl } from 'react-intl';
import SupTable from 'sup-rc-table';
import { message, Divider } from 'sup-ui';
import unlockIcon from 'root/assets/img/unlock.svg';
import lockIcon from 'root/assets/img/lock.svg';
import messages from './messages';
import style from './style.less';
import { fetchDownloadFile } from '../role/utils';

class UserList extends React.Component {
  constructor(props) {
    super(props);
    const { intl, tableBtnAuth } = this.props;
    this.state = {};

    this.btnColumns = [
      {
        key: 'add',
        content: intl.formatMessage(messages.tableBtnAdd),
        authority: tableBtnAuth.includes('createUser'),
        callback: () => {
          this.props.showAddUserModal();
        }
      },
      {
        key: 'exportBtn',
        content: intl.formatMessage(messages.exportBtn),
        callback: this.handleExportClick
      },
      {
        key: 'delete',
        authority: tableBtnAuth.includes('deleteUser'),
        disabled: true,
        callback: () => {
          this.props.handleRemoveUser(this.selectedRows, false, () => {
            this.selectedRows = null;
            this.refreshDeleteBtnState();
          });
        }
      }
    ];

    if (tableBtnAuth.includes('importUser')) {
      this.btnColumns.push({
        key: 'more',
        callback: (params) => {
          if (params.callback) {
            params.callback();
          }
        },
        menu: [
          {
            key: 'import',
            content: intl.formatMessage(messages.importBtn),
            importParams: {
              action: '/inter-api/auth/v1/importExcel',
              accept: '.xlsx',
              headers: {
                Authorization: `Bearer ${localStorage.getItem('ticket')}`
              },
              beforeUpload: this.props.handleBeforeUpload,
              onChange: this.props.handleImportChange,
              onError: this.props.handleImportError
            }
          },
          {
            key: 'exportTemplate',
            content: intl.formatMessage(messages.exportTemplate),
            callback: this.handleExportTemplate
          }
        ]
      });
    }

    this.state.columns = [
      {
        title: intl.formatMessage(messages.columnTitleUsername),
        dataIndex: 'userName',
        key: 'userName',
        width: 200
      },
      {
        title: intl.formatMessage(messages.columnTitleUerType),
        dataIndex: 'userType',
        key: 'userType',
        width: 200,
        render: this.renderUserType
      },
      {
        title: intl.formatMessage(messages.columnTitleStaffname),
        dataIndex: 'personName',
        width: 200,
        key: 'personName'
      },
      {
        title: intl.formatMessage(messages.columnTitleRole),
        dataIndex: 'role',
        key: 'role',
        width: 300,
        render: this.renderRoleColumn
      },
      {
        width: 300,
        title: intl.formatMessage(messages.columnTitleUserDesc),
        dataIndex: 'description',
        key: 'description',
        render: this.renderDescColumn
      },
      {
        title: intl.formatMessage(messages.columnTitleLock),
        dataIndex: 'hasLock',
        key: 'hasLock',
        width: 100,
        render: this.renderLockColumn
      },
      {
        title: intl.formatMessage(messages.columnTitleOperate),
        render: this.renderOperateColumn,
        type: 'operation',
        authority: () => {
          return ['updateUser', 'queryUserPermission', 'deleteUser'].some(
            (d) => {
              return tableBtnAuth.includes(d);
            }
          );
        },
        width: 227
      }
    ];
  }

  handleExportTemplate = () => {
    const EXPORT_TEMPLATE_URL = '/inter-api/auth/v1/excel/template';
    const { intl } = this.props;
    fetchDownloadFile(EXPORT_TEMPLATE_URL, null, null, () => {
      message.error(intl('downloadTemplateErr'));
    });
  };

  componentDidMount() {
    // 判断表格操作栏是否固定
    if (this.table && this.table.changeOperationFixedStatus) {
      this.table.changeOperationFixedStatus();
    }
  }

  handleExportClick = () => {
    this.props.handleExportClick(this);
  };

  handleExport(all) {
    const params = {};
    params.all = !!all;
    if (!all) {
      // 当前页或选择导出接口只支持当前选中项参数
      const { selectedRows } = this;
      let { tableData = [] } = this.props;
      if (selectedRows && selectedRows.length) {
        tableData = selectedRows;
      }
      params.ids = tableData.map((u) => u.id).join(',');
    }
    params.companyId = 1000;
    return params;
  }

  refreshDeleteBtnState() {
    const { selectedRows } = this;
    let disabled = true;
    if (selectedRows && selectedRows.length) {
      disabled = false;
    }
    const deleteBtn = this.btnColumns.find((d) => d.key === 'delete');
    deleteBtn.disabled = disabled;
    this.setState({});
  }

  renderUserType = (userType) => {
    const { intl } = this.props;
    return userType === 0
      ? intl.formatMessage(messages.userTypeNormal)
      : intl.formatMessage(messages.userTypeAdmin);
  };

  renderDescColumn = (desc) => {
    return <div className={style.columnDesc}>{desc}</div>;
  };

  renderLockColumn = (locked, row) => {
    const { tableBtnAuth } = this.props;

    const lock = locked ? (
      <img src={lockIcon} alt="" />
    ) : (
      <img src={unlockIcon} alt="" />
    );

    return tableBtnAuth.includes('lockUser') ? (
      <a
        href="#"
        onClick={(e) => {
          this.props.handleClickLock(e, row);
        }}
      >
        {lock}
      </a>
    ) : (
      lock
    );
  };

  renderOperateColumn = (_, row) => {
    const { intl, tableBtnAuth } = this.props;
    const btns = [
      {
        onClick: () => {
          this.props.handleEditUser(row);
        },
        text: intl.formatMessage(messages.operateEdit),
        key: 'updateUser',
        authCode: 'updateUser'
      },
      {
        onClick: () => {
          this.openAuthority(row);
        },
        text: intl.formatMessage(messages.operateAuth),
        key: 'queryUserPermission',
        authCode: 'queryUserPermission'
      },
      {
        onClick: (e) => {
          e.preventDefault();
          this.props.handleRemoveUser([row], true, () => {
            if (this.selectedRows && this.selectedRows.length) {
              const index = this.selectedRows.findIndex((d) => d.id === row.id);
              if (index > -1) {
                this.selectedRows.splice(index, 1);
                this.refreshDeleteBtnState();
              }
            }
          });
        },
        text: intl.formatMessage(messages.operateRemove),
        key: 'deleteUser',
        authCode: 'deleteUser'
      },
      {
        onClick: (e) => {
          this.props.handleResetPwd(e, row);
        },
        text: intl.formatMessage(messages.operateResetPwd),
        key: 'changePassword',
        authCode: 'updateUser'
      }
    ];

    const authBtns = btns.filter((btn) => {
      const { authCode } = btn;
      return !authCode || tableBtnAuth.includes(authCode);
    });

    return (
      <div className={style.operateCell}>
        <div>
          {authBtns.map((btn, i) => {
            return (
              <>
                <a onClick={btn.onClick}>{btn.text}</a>
                {i < authBtns.length - 1 ? <Divider type="vertical" /> : null}
              </>
            );
          })}
        </div>
      </div>
    );
  };

  renderRoleColumn = (value = []) => {
    return value.map((d) => d.name).join(' / ');
  };

  handleChangeSelectItem = (_, rows) => {
    this.selectedRows = rows;
    this.refreshDeleteBtnState();
  };

  updateColumns = (columns) => {
    this.setState({ columns });
  };

  openAuthority = (row) => {
    if (process.env.NODE_ENV === 'development') {
      const url = `http://${window.location.host}/#/authority?id=${row.id}&status=user&name=${row.userName}`;
      window.open(url);
    } else {
      // const url = `http://${window.location.host}/auth/#/authority?id=${row.id}&status=user&name=${row.userName}`;
      const url = `http://${window.location.host}/#/runtime-fullscreen/runtime-fullscreen/authority-set?id=${row.id}&status=user&name=${row.userName}`;
      window.open(url);
    }
  };

  handleOnDblClick = (row) => {
    const { tableBtnAuth } = this.props;
    if (tableBtnAuth.includes('updateUser')) {
      this.props.handleEditUser(row);
    }
  };

  render() {
    const { tableData, pagination, intl } = this.props;
    const { columns } = this.state;
    return (
      <div style={{ height: '100%' }}>
        <SupTable
          ref={(ref) => {
            this.table = ref;
          }}
          searchPlaceholder={intl.formatMessage(
            messages.tableSearchPlaceholder
          )}
          onDoubleClick={this.handleOnDblClick}
          updateColumns={this.updateColumns}
          btnColumns={this.btnColumns}
          tableKey="userManager"
          columns={columns}
          showSelection
          onSelectItem={this.handleChangeSelectItem}
          pagination={pagination}
          rowKey="id"
          dataSource={tableData}
          operationBarTitle={false}
          onSearch={this.props.handleOnSearch}
        />
      </div>
    );
  }
}

export default injectIntl(UserList);
