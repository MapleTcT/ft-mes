import React from 'react';
import { injectIntl } from 'react-intl';
import { Layout, message } from 'sup-ui';
import SupIcon from 'sup-rc-icon';
import SupResize from 'sup-rc-resize';
import SupSearch from 'sup-rc-search';
import {
  createExportTask,
  queryExportStatus,
  downloadExcel
} from 'root/services/role';
import { getAuthority } from 'root/services/auth';

import AllUserTable from './AllUserTable';
import RoleUserTable from './RoleUserTable';
import RoleSearchTable from './SearchTable';
import SiderWithoutTag from './SiderWithoutTag';
import style from './style.less';
import messages from './messages';
import ExportModal from '../../components/ExportModal';
import SpinModal from '../../components/SpinModal';
import { fetchDownloadFile } from './utils';

const { Content } = Layout;

class Role extends React.Component {
  state = {
    currentSelectRole: null,
    isSelectAllRole: false,
    exportLoading: false,
    searchKeyword: null,
    companyId: null,
    exportModalVisible: false,
    btnAuth: []
  };

  constructor(props) {
    super(props);
    this.siderRef = React.createRef();
  }

  componentDidMount() {
    getAuthority('role').then(({ data: { list } }) => {
      this.setState({
        btnAuth: list
      });
    });
  }

  handleSelectTag = (selectedRole) => {
    this.setState({
      isSelectAllRole: false,
      currentSelectRole: { ...selectedRole },
      searchKeyword: null
    });
  };

  handleRemoveRole = (code) => {
    const { currentSelectRole } = this.state;
    if (currentSelectRole && String(currentSelectRole.code) === String(code)) {
      this.setState({
        currentSelectRole: null
      });
    }
  };

  handleUpdateRole = (newCurrentSelectRole) => {
    const { currentSelectRole } = this.state;
    // 提前判断当前是否有选择角色
    if (currentSelectRole) {
      if (
        String(currentSelectRole.code) === String(newCurrentSelectRole.code)
      ) {
        // 更新当前编辑角色
        this.setState({
          currentSelectRole: newCurrentSelectRole
        });
      }
    }
  };

  handleSelectAllRole = () => {
    this.setState({
      isSelectAllRole: true,
      searchKeyword: null
    });
  };

  handleExport = (data, cb) => {
    if (!this.lastCheckTime) {
      this.setExportState();
      this.exportCb = cb;
      this.lastCheckTime = Date.now();
      // 生成临时id
      data.id = `${Date.now()}_${Math.random()
        .toString(16)
        .slice(2)}`;
      createExportTask(data).then(() => {
        // 轮询查询excel状态
        this.checkExportStatus(data.id);
      });
    }
  };

  cancelCheckExportStatus() {
    this.lastCheckTime = null;
    this.setState({
      exportLoading: false
    });
    const { exportCb } = this;
    if (exportCb) {
      exportCb();
      this.exportCb = null;
    }
  }

  setExportState() {
    const { exportLoading } = this.state;
    if (!exportLoading) {
      this.setState({
        exportLoading: true
      });
    }
  }

  checkExportStatus(id) {
    queryExportStatus({ id })
      .then((res) => {
        const {
          data: {
            data: { status }
          }
        } = res;

        const { intl } = this.props;

        // 错误
        if (status === 0) {
          this.cancelCheckExportStatus();
          message.error(intl.formatMessage(messages.exportFail));
        } else if (status === 1) {
          // 成功
          fetchDownloadFile(downloadExcel(id))
            .then(() => {
              this.cancelCheckExportStatus();
              message.success(intl.formatMessage(messages.exportSuccess));
              this.exportTable = null;
            })
            .catch(() => {
              this.cancelCheckExportStatus();
              message.error(intl.formatMessage(messages.exportFail));
            });
        } else if (status === 2) {
          // 未结束 轮询
          setTimeout(() => {
            this.checkExportStatus(id);
          }, 2000);
        }
        return false;
      })
      .catch(() => {
        this.cancelCheckExportStatus();
      });
  }

  handleCompanyChange = (companyId) => {
    this.setState({
      companyId
    });
  };

  handleSearchChange = (searchKeyword) => {
    if (searchKeyword) {
      this.setState({
        currentSelectRole: null
      });
      this.siderRef.current.setSelectedKeys([]);
    }
    this.setState({
      searchKeyword
    });
  };

  hanldeCancelExport = () => {
    this.setState({
      exportModalVisible: false
    });
  };

  openExportModal = (table) => {
    this.exportTable = table;
    this.setState({
      exportModalVisible: true
    });
  };

  handleExportOk = (downall) => {
    // 调用表格方法
    const params = this.exportTable.handleExport(downall);
    this.handleExport(params);
    this.hanldeCancelExport();
  };

  hasAuth = (code) => {
    const { btnAuth } = this.state;
    if (!code) return btnAuth;
    if (typeof code === 'string') {
      return btnAuth.includes(code);
    }
    return code.some((d) => btnAuth.includes(d));
  };

  render() {
    const { intl } = this.props;
    const {
      currentSelectRole,
      isSelectAllRole,
      exportLoading,
      searchKeyword,
      companyId,
      exportModalVisible
    } = this.state;

    return (
      <Layout style={{ height: '100%' }}>
        <SpinModal
          tip={intl.formatMessage(messages.exportTip)}
          visible={exportLoading}
        />
        <ExportModal
          visible={exportModalVisible}
          onOk={this.handleExportOk}
          onCancel={this.hanldeCancelExport}
        />
        <Layout.Header className={style.header}>
          {intl.formatMessage(messages.roleManageHeader)}
          <SupSearch
            placeholder={intl.formatMessage(messages.headerSearchPlaceholder)}
            className={style.headerSearch}
            onSearch={this.handleSearchChange}
          />
        </Layout.Header>
        <Layout style={{ height: '100%', backgroundColor: '#fff' }}>
          <SupResize min={220}>
            <SiderWithoutTag
              hasAuth={this.hasAuth}
              siderRef={this.siderRef}
              handleSelectTag={this.handleSelectTag}
              handleRemoveRole={this.handleRemoveRole}
              handleUpdateRole={this.handleUpdateRole}
              handleSelectAllRole={this.handleSelectAllRole}
              handleCompanyChange={this.handleCompanyChange}
            />
            <Content className={style.content}>
              {searchKeyword ? (
                <RoleSearchTable
                  hasAuth={this.hasAuth}
                  searchKeyword={searchKeyword}
                  key={searchKeyword}
                  openExportModal={this.openExportModal}
                />
              ) : isSelectAllRole || searchKeyword === '' ? ( // 搜索条件为空时显示所有角色
                <AllUserTable
                  hasAuth={this.hasAuth}
                  companyId={companyId}
                  key={companyId}
                  openExportModal={this.openExportModal}
                />
              ) : currentSelectRole ? (
                <RoleUserTable
                  hasAuth={this.hasAuth}
                  key={currentSelectRole.code}
                  currentSelectRole={currentSelectRole}
                  openExportModal={this.openExportModal}
                />
              ) : (
                <div className={style.emptyContent}>
                  <SupIcon className={style.backIcon} type="iconpoint" />
                  {intl.formatMessage(messages.emptyContentText)}
                </div>
              )}
            </Content>
          </SupResize>
        </Layout>
      </Layout>
    );
  }
}

export default injectIntl(Role);
