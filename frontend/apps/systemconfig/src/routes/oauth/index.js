import React from 'react';
import { injectIntl } from 'react-intl';
import SupTable from 'sup-rc-table';
import { Divider, Switch, message, Modal, Layout } from 'sup-ui';
import messages from './messages';
import style from './style.less';
import commonStyle from '../sysconfig/style.less';
import {
  fetchAuthList,
  searchAuth,
  addAuthApp,
  removeAuthApp,
  enableAuthApp,
  updateAuthApp,
  getAuthDetailInfo
} from '../../services/auth';
import AuthModal from './EditModal';

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

class AuthList extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      tableData: [],
      modalVisible: false,
      initData: null
    };

    this.initTable();
  }

  componentDidMount() {
    this.refreshAuthList();
    // 判断表格操作栏是否固定
    if (this.table && this.table.changeOperationFixedStatus) {
      this.table.changeOperationFixedStatus();
    }
  }

  intl(key, data) {
    const { intl } = this.props;
    return intl.formatMessage(messages[key], data);
  }

  initTable() {
    this.selectedRows = [];

    this.columns = [
      {
        title: this.intl('columnThirdApp'),
        dataIndex: 'systemName',
        key: 'systemName',
        width: 200
      },
      {
        title: this.intl('columnAuthorityCertificate'),
        dataIndex: 'oauthName',
        key: 'oauthName',
        width: 200
      },
      {
        title: this.intl('columnType'),
        dataIndex: 'systemFlag',
        key: 'systemFlag',
        width: 100,
        render: this.renderSystemFlag
      },
      {
        title: this.intl('columnAuthAddr'),
        dataIndex: 'oauthUrl',
        key: 'oauthUrl',
        width: 500
      },
      {
        title: this.intl('columnDescription'),
        dataIndex: 'description',
        key: 'description',
        width: 200
      },
      {
        title: this.intl('columnEnabled'),
        dataIndex: 'enable',
        key: 'enable',
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
        url: '',
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

  refreshAuthList() {
    const { keyword } = this.state;
    let getList = fetchAuthList;
    let params = {};
    if (keyword) {
      getList = searchAuth;
      params = { keyword };
    }
    getList(params).then(({ data: { list } }) => {
      this.setState({
        tableData: list
      });
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

  changeAppEnableStatus(id, name, checked) {
    enableAuthApp(id, checked).then(() => {
      const msg = checked ? 'enableSuccess' : 'disableSuccess';
      message.success(this.intl(msg, { name }));
      this.refreshAuthList();
    });
  }

  handleAdd() {
    this.setState({
      modalVisible: true,
      initData: null
    });
  }

  handleEdit(data) {
    getAuthDetailInfo(data.id).then(({ data: { data: initData } }) => {
      this.setState({
        modalVisible: true,
        initData
      });
    });
  }

  handleEnableAuthApp(id, name, flag, checked) {
    if (!flag && checked) {
      Modal.confirm({
        title: this.intl('enableModalTitle', { title: name }),
        content: this.intl('enableMsg'),
        onOk: () => {
          this.changeAppEnableStatus(id, name, checked);
        }
      });
    } else {
      this.changeAppEnableStatus(id, name, checked);
    }
  }

  handleSave = (values, cb = () => {}) => {
    if (values.id) {
      updateAuthApp(values.id, values)
        .then(() => {
          cb();
          this.handleSaveSuccess(this.intl('editSuccess'));
        })
        .catch(() => {
          cb();
        });
    } else {
      addAuthApp(values)
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
    this.refreshAuthList();
    this.handleCloseModal();
    message.success(msg);
  }

  handleCloseModal = () => {
    this.setState({
      modalVisible: false
    });
  };

  handleOnSearch = (data) => {
    this.selectedRows = [];
    this.refreshDeleteBtnState();

    const { pagination, keyword, filters } = data;
    const nextData = {
      filter: genFilter(filters),
      pagination: { ...pagination }
    };
    // 手动组织过滤数据
    nextData.keyword = '';
    if (keyword) {
      nextData.keyword = keyword;
    }
    this.setState(nextData, () => {
      this.refreshAuthList();
    });
  };

  handleChangeSelectItem = (_, rows) => {
    this.selectedRows = rows;
    this.refreshDeleteBtnState();
  };

  handleRemove(id, authName) {
    const { selectedRows } = this;
    let ids;
    if (!id) {
      ids = selectedRows.map((d) => d.id).join(',');
      authName = this.intl('allSelectOuth');
    } else {
      ids = id;
    }
    Modal.confirm({
      title: this.intl('removeTitle', { title: authName }),
      content: this.intl('removeContent'),
      onOk: () => {
        removeAuthApp({ ids }).then(({ data: { data, message: msg } }) => {
          if (data) {
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
            this.refreshAuthList();
          } else {
            message.error(this.intl('removeFail', { msg }));
          }
        });
      }
    });
  }

  updateColumns = (columns) => {
    this.columns = columns;
    this.setState({});
  };

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
              this.handleRemove(row.id, row.systemName);
            }}
          >
            {this.intl('opereateColumnBtnRemove')}
          </a>
        </div>
      </div>
    );
  };

  renderSystemFlag = (flag) => {
    return flag ? this.intl('builtIn') : this.intl('external');
  }

  renderEnableOperate = (used, row) => {
    return (
      <div>
        <Switch
          checked={!!used}
          onChange={(checked) => {
            this.handleEnableAuthApp(row.id, row.systemName, row.systemFlag, checked);
          }}
        />
      </div>
    );
  };

  render() {
    const {
      tableData,
      modalVisible,
      initData
    } = this.state;

    return (
      <div className={style.wrap}>
        <AuthModal
          visible={modalVisible}
          handleCancel={this.handleCloseModal}
          handleSave={this.handleSave}
          initData={initData}
        />
        <Layout.Header className={commonStyle.header}>
          {this.intl('oauthTitle')}
        </Layout.Header>
        <SupTable
          ref={(ref) => {
            this.table = ref;
          }}
          size="small"
          btnColumns={this.btnColumns}
          tableKey="authList"
          columns={this.columns}
          updateColumns={this.updateColumns}
          showSelection
          onSelectItem={this.handleChangeSelectItem}
          rowKey="id"
          dataSource={tableData}
          onSearch={this.handleOnSearch}
          filterParams={this.filterParams}
        />
      </div>
    );
  }
}

export default injectIntl(AuthList);
