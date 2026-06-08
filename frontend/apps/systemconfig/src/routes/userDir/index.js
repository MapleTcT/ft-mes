import React from 'react';
import { injectIntl } from 'react-intl';
import SupTable from 'sup-rc-table';
import { getCodeValueList } from 'sup-rc-syscode';
import SpinModal from 'root/components/SpinModal/index.js';
import { Divider, Switch, message, Icon, Modal, Layout } from 'sup-ui';
import messages from './messages';
import style from './style.less';
import commonStyle from '../sysconfig/style.less';
import {
  fetchUserDirs,
  createUserDir,
  removeUserDir,
  enableUserDir,
  updateUserDirSort,
  connectUserDir,
  updateUserDir,
  getUserDir
} from '../../services/userDir';
import UserDirModal from './EditModal';
import { USER_DIR_SYSCODE, USER_DIR_VALUE_KEY, SUG_REC_URL } from './constant';

// import MockJS from 'mockjs';
// const { Random } = MockJS;

// const fakeAdd = async (nums) => {
//   while (nums--) {
//     createUserDir({
//       directoryName: Random.name(),
//       directoryType: Random.pick(['ldap', 'msad']),
//       hostname: Random.name(),
//       port: Random.integer(1, 9000)
//     });
//   }
// };

// fakeAdd(100);

const genFilter = (data) => {
  const filter = {};
  for (const key in data) {
    if ({}.hasOwnProperty.call(data, key)) {
      const firstUpperKey = key[0].toUpperCase() + key.slice(1);
      filter[`screen${firstUpperKey}s`] = data[key];
    }
  }
  return filter;
};

class UserDir extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      pagination: {
        pageSize: 20,
        current: 1,
        total: 0
      },
      tableData: [],
      modalVisible: false,
      initData: null,
      testConnLoading: false
    };

    this.selectedRows = [];

    this.columns = [
      {
        title: this.intl('columnDirectoryName'),
        dataIndex: 'directoryName',
        key: 'directoryName',
        width: 300,
        filterType: 'search'
      },
      {
        title: this.intl('columnDirectoryType'),
        dataIndex: 'directoryType',
        key: 'directoryType',
        width: 300,
        render: this.renderDirType,
        filterType: 'search'
      },
      {
        title: this.intl('columnSort'),
        dataIndex: 'sort',
        key: 'sort',
        width: 200,
        render: this.renderSort
      },
      {
        title: this.intl('columnEnabled'),
        dataIndex: 'enabled',
        key: 'enabled',
        width: 100,
        render: this.renderEnableOperate
      },
      {
        title: this.intl('columnOperate'),
        render: this.renderOperateColumn,
        width: 220,
        type: 'operation'
      }
    ];

    this.btnColumns = [
      {
        key: 'add',
        content: this.intl('tableBtnAdd'),
        callback: () => {
          this.handleAdd();
        }
      },
      {
        key: 'delete',
        disabled: true,
        callback: () => {
          this.handleRemove();
        }
      }
    ];

    this.filterParams = ['directoryName', 'directoryType'].reduce((a, d) => {
      a[d] = {
        param: 'fieldValue',
        url: SUG_REC_URL,
        customParmas() {
          return `fieldName=${d}`;
        },
        callback({ data: { data } }) {
          return data;
        }
      };
      return a;
    }, {});
  }

  handleRemove(id, userDir) {
    const { selectedRows } = this;
    let ids;
    if (!id) {
      ids = selectedRows.map((d) => d.id).join(',');
      userDir = this.intl('allSelectUserDir');
    } else {
      ids = id;
    }
    Modal.confirm({
      title: this.intl('removeTitle', { title: userDir }),
      content: this.intl('removeContent'),
      onOk: () => {
        removeUserDir({ ids }).then(() => {
          message.success(this.intl('removeSuccess'));
          if (id) {
            const index = this.selectedRows.findIndex((d) => d.id === id);
            if (index > -1) {
              this.selectedRows.splice(index, 1);
            }
          } else {
            this.selectedRows = null;
          }
          this.refreshDeleteBtnState();
          this.refreshUserDirs();
        });
      }
    });
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

  renderDirType = (data) => {
    const displayName = this.userDirMap[data.code][USER_DIR_VALUE_KEY];
    return <span title={displayName}>{displayName}</span>;
  };

  renderEnableOperate = (used, row) => {
    return (
      <div>
        <Switch
          checked={!!used}
          onChange={(checked) => {
            this.handleEnableUserDir(row.id, checked);
          }}
        />
      </div>
    );
  };

  handleEnableUserDir(id, checked) {
    enableUserDir(id, checked).then(() => {
      const msg = checked ? 'enableSuccess' : 'disableSuccess';
      message.success(this.intl(msg));
      this.refreshUserDirs();
    });
  }

  handleSort = (id, direction) => {
    updateUserDirSort(id, direction).then(() => {
      message.success(this.intl('changeSortSuccess'));
      this.refreshUserDirs();
    });
  };

  renderSort = (_, row, index) => {
    const { tableData } = this.state;
    const len = tableData.length;
    return (
      <span>
        {this.renderSortIcon('up', index === 0, () => {
          this.handleSort(row.id, 0);
        })}
        {this.renderSortIcon('down', index === len - 1, () => {
          this.handleSort(row.id, 1);
        })}
      </span>
    );
  };

  renderSortIcon(type, disabled, onClick) {
    const iconStyle = {
      fontSize: 16,
      color: disabled ? '#7F8FA4' : '#0F71E2',
      marginLeft: type === 'down' ? '16px' : 0
    };
    return (
      <Icon
        type={`${type}-circle`}
        style={iconStyle}
        onClick={() => {
          if (!disabled && onClick) {
            onClick();
          }
        }}
      />
    );
  }

  showTestLoading(data) {
    const nextState = { testConnLoading: true };
    if (data) {
      // 修复测试连接点击后用户数据被清空
      nextState.initData = data;
    }
    this.setState(nextState);
  }

  hideTestLoading() {
    this.setState({
      testConnLoading: false
    });
  }

  handleTestConn = (data) => {
    const { baseDn, enableSsl, hostname, password, port, userName } = data;
    const { testConnLoading } = this.state;
    if (testConnLoading) return;
    this.showTestLoading(data);

    return connectUserDir({
      baseDn,
      enableSsl,
      hostname,
      password,
      port,
      userName
    })
      .then(() => {
        this.hideTestLoading();
        message.success(this.intl('connectSuccess'));
        return true;
      })
      .catch((err) => {
        this.hideTestLoading();
        message.error(
          (err.data && err.data.message) || this.intl('connectFail')
        );
        return false;
      });
  };

  handleEdit(data) {
    // TODO refres data
    getUserDir(data.id).then(({ data: { data: initData } }) => {
      // 用户目录类型值需转换为可识别的系统编码值
      const {
        directoryType: { entityCode, code }
      } = initData;
      initData.directoryType = `${entityCode}/${code}`;
      this.setState({
        modalVisible: true,
        initData
      });
    });
  }

  renderOperateColumn = (_, row) => {
    return (
      <div className={style.operateCell}>
        <div>
          <a
            href="#"
            onClick={(e) => {
              e.preventDefault();
              this.handleEdit(row);
            }}
          >
            {this.intl('operateColumnBtnEdit')}
          </a>
          <Divider type="vertical" />
          <a
            href="#"
            onClick={(e) => {
              e.preventDefault();
              this.handleTestConn(row);
            }}
          >
            {this.intl('operateColumnBtnTest')}
          </a>
          <Divider type="vertical" />
          <a
            href="#"
            onClick={(e) => {
              e.preventDefault();
              this.handleRemove(row.id, row.directoryName);
            }}
          >
            {this.intl('opereateColumnBtnRemove')}
          </a>
        </div>
      </div>
    );
  };

  componentDidMount() {
    getCodeValueList({
      entityCode: USER_DIR_SYSCODE
    }).then((res) => {
      const {
        data: { list }
      } = res;
      this.userDirMap = list.reduce((acc, item) => {
        acc[item.code] = item;
        return acc;
      }, {});
      this.refreshUserDirs();
    });
    // 判断表格操作栏是否固定
    if (this.table && this.table.changeOperationFixedStatus) {
      this.table.changeOperationFixedStatus();
    }
  }

  handleAdd() {
    this.setState({
      modalVisible: true,
      initData: null
    });
  }

  refreshUserDirs() {
    const {
      pagination: { current, pageSize },
      filter
    } = this.state;

    fetchUserDirs(
      {
        current,
        pageSize
      },
      { screenDirectoryNames: [], screenDirectoryTypes: [], ...filter }
    ).then(({ data: { list, pagination } }) => {
      this.setState({
        pagination,
        tableData: list
      });
    });
  }

  intl(key, data) {
    const { intl } = this.props;
    return intl.formatMessage(messages[key], data);
  }

  handleSave = (values, cb = () => {}) => {
    if (values.id) {
      updateUserDir(values.id, values)
        .then(() => {
          cb();
          this.handleSaveSuccess(this.intl('editSuccess'));
        })
        .catch(() => {
          cb();
        });
    } else {
      createUserDir(values)
        .then(() => {
          cb();
          this.handleSaveSuccess(this.intl('addSuccess'));
        })
        .catch(() => {
          cb();
        });
    }
  };

  handleSaveSuccess(msg) {
    this.refreshUserDirs();
    this.handleCloseModal();
    message.success(msg);
  }

  handleCloseModal = () => {
    this.setState({
      modalVisible: false
    });
  };

  handleOnSearch = (data) => {
    // FIXME 翻页是否取消选中
    this.selectedRows = [];
    this.refreshDeleteBtnState();

    const { pagination, keyword, filters } = data;
    const nextData = {
      filter: genFilter(filters),
      pagination: { ...pagination }
    };
    // 手动组织过滤数据
    nextData.keyword = '';
    if (keyword && keyword.keyword) {
      nextData.keyword = keyword;
    }
    this.setState(nextData, () => {
      this.refreshUserDirs();
    });
  };

  handleChangeSelectItem = (_, rows) => {
    this.selectedRows = rows;
    this.refreshDeleteBtnState();
  };

  updateColumns = (columns) => {
    this.columns = columns;
    this.setState({});
  };

  render() {
    const {
      pagination,
      tableData,
      modalVisible,
      initData,
      testConnLoading
    } = this.state;

    return (
      <div className={style.wrap}>
        <SpinModal
          visible={testConnLoading}
          tip={this.intl('connTestingTip')}
        />
        <UserDirModal
          visible={modalVisible}
          handleCancel={this.handleCloseModal}
          handleSave={this.handleSave}
          initData={initData}
          handleTest={this.handleTestConn}
        />
        <Layout.Header className={commonStyle.header}>
          {this.intl('userDirTitle')}
        </Layout.Header>
        <SupTable
          ref={(ref) => {
            this.table = ref;
          }}
          size="small"
          btnColumns={this.btnColumns}
          showColumnsFilter={false}
          tableKey="userDirList"
          columns={this.columns}
          updateColumns={this.updateColumns}
          showSelection
          showSearchIcon={false}
          onSelectItem={this.handleChangeSelectItem}
          pagination={pagination}
          rowKey="id"
          dataSource={tableData}
          onSearch={this.handleOnSearch}
          filterParams={this.filterParams}
        />
      </div>
    );
  }
}

export default injectIntl(UserDir);
