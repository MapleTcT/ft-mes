import React from 'react';
import { injectIntl } from 'react-intl';
import { Layout, Modal, message } from 'sup-ui';
import {
  fetchUserList,
  addNewUser,
  removeUser,
  editUser,
  updatePassword,
  lockUser,
  searchUsers,
  companyTree,
  fetchUserDetail,
  createExportTask,
  queryExportStatus,
  downloadExcel,
  checkUploadStatus,
  getUserSessionInfo
} from 'root/services/user';
import { getAuthority } from 'root/services/auth';
import { getCurrentCompanyId } from 'root/utils/index';

import style from './style.less';
import messages from './messages';

import EditPwdModal from './modal/EditPwd';
import UserModal from './modal/UserModal';
import UserList from './List';
import ExportModal from '../../components/ExportModal';
import SpinModal from '../../components/SpinModal';
import { fetchDownloadFile } from '../role/utils';

// const { TreeNode } = Tree;
const { Header, Content } = Layout;

const defaultUserData = { timeZone: 'CST+08:00' };
const DEFAULT_ROLE_TYPE = 1;

class User extends React.Component {
  state = {
    tableBtnAuth: null,
    addUserVisible: false,
    editUserVisible: false,
    pwdModalVisible: false,
    currentEditUser: {},
    companyTreeValue: '', // 用户控制公司查询按钮
    selectCompanyTreeValue: '', // 用于查询用户传入公司参数
    pagination: {
      current: 1,
      pageSize: 20,
      total: 0
    },
    spinTip: '',
    spinVisible: false,
    tableLoading: false
    // companyTreeList: []
  };

  searchUserList(keyword, nextPage, nextPageSize) {
    const {
      pagination: { current, pageSize },
      selectCompanyTreeValue
    } = this.state;
    searchUsers({
      keyword,
      current: nextPage || current,
      pageSize: nextPageSize || pageSize,
      companyId: selectCompanyTreeValue
    }).then((res) => {
      const {
        data: { list, pagination }
      } = res;
      this.setState({
        tableData: list,
        pagination,
        tableLoading: false
      });
    });
  }

  loadUserList(nextPage, nextPageSize) {
    const {
      pagination: { current, pageSize },
      selectCompanyTreeValue,
      keyword
    } = this.state;
    this.setState({ tableLoading: true });
    // spi5888 兼容当前列表处于搜索情况
    if (keyword) {
      return this.searchUserList(keyword, nextPage, nextPageSize);
    }

    if (!nextPage) {
      nextPage = current;
    }
    if (!nextPageSize) {
      nextPageSize = pageSize;
    }
    fetchUserList({
      current: nextPage,
      pageSize: nextPageSize,
      companyId: selectCompanyTreeValue
    }).then((res) => {
      const {
        data: { list, pagination }
      } = res;
      this.setState({
        tableData: list,
        pagination,
        tableLoading: false
      });
    });
  }

  loaderCompanyList() {
    return companyTree().then((res) => {
      const {
        data: { list }
      } = res;

      const companyTreeList = list.reduce((acc, tree, _, treeArr) => {
        const { parentId } = tree;
        // 根节点可能为0，或者为null
        if (parentId === 0 || parentId === null) {
          acc.push(tree);
        } else {
          const parentTree = treeArr.find((t) => t.id === parentId);
          if (!parentTree) {
            acc.push(tree);
          } else if (!parentTree.children) {
            parentTree.children = [tree];
          } else {
            parentTree.children.push(tree);
          }
        }
        return acc;
      }, []);

      return companyTreeList;
    });
  }

  componentDidMount() {
    getAuthority('auth').then(({ data: { list } }) => {
      this.setState({
        tableBtnAuth: list
      });
    });
    // this.loaderCompanyList().then((companyTreeList) => {
    //   this.setState({
    //     companyTreeList
    //   });
    //   this.loadUserList();
    // });
    // 获取用户所属公司信息
    this.currentCompanyId = getCurrentCompanyId();
    if (!this.currentCompanyId) {
      // 考虑缓存公司获取不到的情况
      getUserSessionInfo().then(({ data }) => {
        this.currentCompanyId = data.userSessionInfo.companyId;
        this.loadUserList();
      });
    } else {
      this.loadUserList();
    }
  }

  handleClickLock = (e, row) => {
    e.preventDefault();
    const { intl } = this.props;
    const { hasLock, userName } = row;
    const successMsg = hasLock
      ? intl.formatMessage(messages.unLockSuccess)
      : intl.formatMessage(messages.lockSuccess);
    const title = hasLock
      ? intl.formatMessage(messages.unLockTitle, { userName })
      : intl.formatMessage(messages.lockTitle, { userName });
    const content = hasLock
      ? intl.formatMessage(messages.unLockContent)
      : intl.formatMessage(messages.lockContent);

    Modal.confirm({
      title,
      content,
      onOk: () => {
        lockUser({ lock: !hasLock, id: row.id }).then(() => {
          this.loadUserList();
          message.success(successMsg);
        });
      }
    });
  };

  handleAddUserSubmit = (data) => {
    const { intl } = this.props;
    const postData = { ...data };
    if (postData.staff) {
      // 系统管理员不需要关联人员
      postData.personId = postData.staff[0].id;
      delete postData.staff;
    }
    if (postData.role) {
      postData.role = postData.role.map(({ id, name }) => ({
        id,
        name,
        type: DEFAULT_ROLE_TYPE
      }));
    } else {
      // 后台数据格式要求
      postData.role = [];
    }
    addNewUser(postData).then(() => {
      message.success(intl.formatMessage(messages.addUserSuccess));
      this.hideAddUserModal();
      this.loadUserList();
    });
  };

  hideAddUserModal = () => {
    this.setState({
      addUserVisible: false
    });
  };

  hideEditUserModal = () => {
    this.setState({
      editUserVisible: false
    });
  };

  showAddUserModal = () => {
    this.setState({
      addUserVisible: true
    });
  };

  showEditUserModal = (cb) => {
    this.setState(
      {
        editUserVisible: true
      },
      cb
    );
  };

  handleRemoveUser = (users, isSingle, cb) => {
    const { intl } = this.props;
    let title;
    const ids = users.map((d) => d.id);
    if (isSingle) {
      const [{ userName }] = users;
      title = intl.formatMessage(messages.removeUserModalTitle, {
        userName
      });
    } else {
      title = intl.formatMessage(messages.bulkRemoveUserModalTitle);
    }

    Modal.confirm({
      title,
      content: intl.formatMessage(messages.removeUserModalContent),
      onOk: () => {
        removeUser({ ids }).then(() => {
          message.success(intl.formatMessage(messages.removeUserSuccess));
          this.loadUserList();
          if (cb) {
            cb();
          }
        });
      }
    });
  };

  handleEditUserSubmit = (data) => {
    const { intl } = this.props;
    const postData = { ...data };
    if (postData.staff) {
      // 系统管理员没有staff
      postData.personId = postData.staff[0].id;
      delete postData.staff;
    }
    if (postData.role) {
      postData.role = postData.role.map(
        ({ id, name, type = DEFAULT_ROLE_TYPE }) => ({
          id,
          name,
          type
        })
      );
    } else {
      postData.role = [];
    }
    const {
      currentEditUser: { id }
    } = this.state;
    postData.id = id;
    editUser(postData).then(() => {
      message.success(intl.formatMessage(messages.editUserSuccess));
      this.hideEditUserModal();
      this.loadUserList();
    });
  };

  handleEditUser = (currentEditUser) => {
    // 解决修改完马上点击数据不同步
    fetchUserDetail(currentEditUser.id).then((res) => {
      const {
        data: { data: user }
      } = res;
      this.setState(
        {
          currentEditUser: {
            ...user,
            staff: [
              {
                id: currentEditUser.personId,
                name: currentEditUser.personName
              }
            ]
          }
        },
        this.showEditUserModal
      );
    });
  };

  handleResetPwd = (e, row) => {
    e.preventDefault();
    this.setState({
      pwdModalVisible: true,
      currentEditUser: row
    });
  };

  handleEditPwdSubmit = (data) => {
    const {
      currentEditUser: { id }
    } = this.state;
    updatePassword({
      ...data,
      id
    }).then(() => {
      const { intl } = this.props;
      message.success(intl.formatMessage(messages.msgPwdResetSuccess));
      this.hideEditPwdModal();
    });
  };

  hideEditPwdModal = () => {
    this.setState({
      pwdModalVisible: false
    });
  };

  handlePageOnChange = (page, pageSize) => {
    this.loadUserList(page, pageSize);
  };

  handleOnSearch = (params) => {
    const {
      pagination: { current, pageSize },
      keyword
    } = params;
    this.setState(
      {
        keyword
      },
      () => {
        this.loadUserList(current, pageSize);
      }
    );
  };

  handleCompanyTreeChange = (companyTreeValue) => {
    this.setState({
      companyTreeValue
    });
  };

  handleSearchCompany = () => {
    const { companyTreeValue } = this.state;

    this.setState(
      {
        selectCompanyTreeValue: companyTreeValue
      },
      () => {
        this.loadUserList(1);
      }
    );
  };

  // renderCompanyTreeSelect() {
  //   const { companyTreeList, companyTreeValue } = this.state;
  //   const { intl } = this.props;

  //   const loopTreeNode = (data) => {
  //     return data.map((tree) => {
  //       let treeChildren = null;
  //       if (tree.children && tree.children.length) {
  //         treeChildren = loopTreeNode(tree.children);
  //       }
  //       return (
  //         <TreeNode key={tree.id} value={tree.id} title={tree.shortName}>
  //           {treeChildren}
  //         </TreeNode>
  //       );
  //     });
  //   };

  //   return (
  //     <TreeSelect
  //       value={companyTreeValue}
  //       placeholder={intl.formatMessage(messages.chooseCompany)}
  //       allowClear
  //       treeDefaultExpandAll
  //       onChange={this.handleCompanyTreeChange}
  //     >
  //       {loopTreeNode(companyTreeList)}
  //     </TreeSelect>
  //   );
  // }

  handleExportOk = (downall) => {
    // 调用表格方法
    const params = this.exportTable.handleExport(downall);
    this.handleExport(params);
    this.hanldeCancelExport();
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

  handleExport = (data, cb) => {
    if (!this.lastCheckTime) {
      this.setExportState();
      this.exportCb = cb;
      this.lastCheckTime = Date.now();
      // 拼接keyword
      const { keyword } = this.state;
      data.keyword = keyword;
      createExportTask(data)
        .then((res) => {
          // 轮询查询excel状态
          this.checkExportStatus(res.data.data.id);
        })
        .catch((err) => {
          this.handleExportFail(err);
        });
    }
  };

  handleExportFail(err) {
    const { intl } = this.props;
    this.cancelCheckExportStatus();
    const errMsg = err.data && err.data.errorMessage;
    message.error(errMsg || intl.formatMessage(messages.exportFail));
  }

  cancelCheckExportStatus() {
    this.lastCheckTime = null;
    this.setSpinState('', false);
    const { exportCb } = this;
    if (exportCb) {
      exportCb();
      this.exportCb = null;
    }
  }

  setExportState() {
    const { spinVisible } = this.state;
    if (!spinVisible) {
      const { intl } = this.props;
      this.setSpinState(intl.formatMessage(messages.exportTip), true);
    }
  }

  setSpinState(spinTip, spinVisible) {
    this.setState({
      spinTip,
      spinVisible
    });
  }

  checkExportStatus(id) {
    queryExportStatus({ id })
      .then((res) => {
        const {
          data: {
            data: { status, addNum }
          }
        } = res;
        const { intl } = this.props;
        // 1进行中  2成功  3失败
        if (status === 1) {
          setTimeout(() => {
            this.checkExportStatus(id);
          }, 2000);
        } else if (status === 2) {
          fetchDownloadFile(downloadExcel(id))
            .then(() => {
              this.cancelCheckExportStatus();
              message.success(
                intl.formatMessage(messages.exportSuccess, { num: addNum })
              );
            })
            .catch((errRes) => {
              this.handleExportFail(errRes);
            });
        } else if (status === 3) {
          this.cancelCheckExportStatus();
          message.error(intl.formatMessage(messages.exportFail));
        }
      })
      .catch((err) => {
        this.handleExportFail(err);
      });
  }

  refreshUploadStatus(id) {
    const { intl } = this.props;
    checkUploadStatus({
      id
    })
      .then((res) => {
        const {
          data: {
            data: { status, hasErrorFile, errorMessage, addNum, updateNum }
          }
        } = res;
        // 1 解析进行中 2 导入结束 3 导入失败
        if (status === 1) {
          setTimeout(() => {
            this.refreshUploadStatus(id);
          }, 2000);
        } else if (status === 2) {
          this.resetUploadState();
          message.success(
            intl.formatMessage(messages.importSuccessMsg, {
              add: addNum,
              update: updateNum
            })
          );
          this.loadUserList();
        } else if (status === 3) {
          // 判断数据为空和是否有错误文件下载情况
          this.resetUploadState();
          const content = hasErrorFile
            ? this.createImportFailFileContent(id)
            : errorMessage;
          Modal.error({
            title: intl.formatMessage(messages.importFailMsg),
            content
          });
        }
      })
      .catch(({ data }) => {
        this.resetUploadState();
        message.error(
          (data && data.message) || intl.formatMessage(messages.importFailMsg)
        );
      });
  }

  createImportFailFileContent = (id) => {
    const { intl } = this.props;
    // FIXME 其他语言
    const msg = intl.formatMessage(messages.downloadImportFailFile).split('@');
    return (
      <span>
        {msg[0]}
        <a
          style={{ textDecoration: 'underline' }}
          onClick={(e) => {
            e.preventDefault();
            this.downloadImportErrorFile(id);
          }}
        >
          {msg[1]}
        </a>
        {msg[2]}
      </span>
    );
  };

  downloadImportErrorFile(id) {
    const url = downloadExcel(id);
    fetchDownloadFile(url, null, null, () => {
      message.error(this.intl('downloadErrorFileErr'));
    });
  }

  handleBeforeUpload = (file) => {
    const { intl } = this.props;
    const fileName = (file.name || '').toLowerCase();
    const isXLSX = fileName && fileName.endsWith('.xlsx');
    if (!isXLSX) {
      message.error(intl.formatMessage(messages.importTypeErrMsg));
      return false;
    }
    const { spinVisible } = this.state;
    // 进入加载等待状态
    if (!spinVisible) {
      this.setSpinState(intl.formatMessage(messages.importTip), true);
    }
  };

  handleImportChange = ({ file }) => {
    if (file.status === 'done') {
      const {
        response: { data }
      } = file;
      this.refreshUploadStatus(data.id);
    }
  };

  resetUploadState() {
    this.setSpinState('', false);
  }

  handleImportError = (res, data) => {
    this.resetUploadState();
    const { intl } = this.props;
    message.error(
      (data && data.errorMessage) || intl.formatMessage(messages.importFailMsg)
    );
  };

  render() {
    const { currentCompanyId } = this;
    const { intl } = this.props;
    const {
      tableData,
      addUserVisible,
      editUserVisible,
      currentEditUser,
      pwdModalVisible,
      pagination,
      tableBtnAuth,
      spinVisible,
      spinTip,
      exportModalVisible,
      tableLoading
    } = this.state;

    const editUserModalProps = {
      visible: editUserVisible,
      wrapClassName: style.userModal,
      title: intl.formatMessage(messages.editUserModalTitle),
      onCancel: this.hideEditUserModal,
      width: 580
    };

    const addUserModalProps = {
      visible: addUserVisible,
      wrapClassName: style.userModal,
      title: intl.formatMessage(messages.addUserModalTitle),
      onCancel: this.hideAddUserModal,
      width: 580
    };

    const pwdModalProps = {
      visible: pwdModalVisible,
      wrapClassName: style.userModal,
      title: intl.formatMessage(messages.editPwdModalTitle),
      onCancel: this.hideEditPwdModal,
      width: 580
    };

    const listProps = {
      tableData,
      tableLoading,
      handleClickLock: this.handleClickLock,
      handleEditUser: this.handleEditUser,
      handleRemoveUser: this.handleRemoveUser,
      handleResetPwd: this.handleResetPwd,
      showAddUserModal: this.showAddUserModal,
      pagination: {
        ...pagination
      },
      handleOnSearch: this.handleOnSearch,
      tableBtnAuth,
      handleExportClick: this.openExportModal,
      handleImportError: this.handleImportError,
      handleImportChange: this.handleImportChange,
      handleBeforeUpload: this.handleBeforeUpload
    };

    return (
      <Layout style={{ height: '100%', backgroundColor: '#fff' }}>
        <SpinModal tip={spinTip} visible={spinVisible} />
        <ExportModal
          visible={exportModalVisible}
          onOk={this.handleExportOk}
          onCancel={this.hanldeCancelExport}
        />
        <EditPwdModal
          initData={currentEditUser}
          modalProps={pwdModalProps}
          onSubmit={this.handleEditPwdSubmit}
        />

        <UserModal
          initData={defaultUserData}
          onSubmit={this.handleAddUserSubmit}
          modalProps={addUserModalProps}
          currentCompanyId={currentCompanyId}
        />

        <UserModal
          modalProps={editUserModalProps}
          initData={{ ...defaultUserData, ...currentEditUser }}
          currentCompanyId={currentCompanyId}
          onSubmit={this.handleEditUserSubmit}
          isEdit
        />

        <Header className={style.header}>
          {intl.formatMessage(messages.headerUserManage)}
        </Header>
        {/* <div className={style.companySelect}>
          {this.renderCompanyTreeSelect()}
          <Button
            disabled={!companyTreeValue}
            onClick={this.handleSearchCompany}
          >
            {intl.formatMessage(messages.searchCompanyBtn)}
          </Button>
        </div> */}
        <Content className={style.content}>
          {tableBtnAuth ? <UserList {...listProps} /> : null}
        </Content>
      </Layout>
    );
  }
}

export default injectIntl(User);
