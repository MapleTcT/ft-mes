// 无标签树
import React from 'react';
import { injectIntl } from 'react-intl';
import { Icon, Menu, Dropdown, Modal, message } from 'sup-ui';
import {
  fetchNoTagRoleTree,
  addRole,
  updateRole,
  removeRole,
  fetchTagsList,
  fetchRolDetail,
  getSearchRoleByKeywordAPI,
  searchNoTagRoleByKeyword
} from 'root/services/role';
import { getUserSessionInfo } from 'root/services/user';
import SupTree from 'sup-rc-tree';
import style from './style.less';
import messages from './messages.js';
import AddRoleModal from './modal/AddRole';
import EditRoleModal from './modal/EditRole';

const ALL_ROLE_KEY = '__ALL_ROLE_KEY__';

const extractRoleListData = (treeData) => {
  return treeData.map(({ code, id, name: title }) => {
    return {
      key: code,
      code,
      title,
      id
    };
  });
};

class RoleSider extends React.Component {
  state = {
    selectRoleKeys: [],
    addRoleModalVisible: false,
    editRoleModalVisible: false,
    editRole: {},
    treeData: [],
    selectAll: false,
    tagsList: []
  };

  renderAllRoleAction = () => {
    const { selectAll } = this.state;
    const { intl } = this.props;

    return (
      <div
        className={style.roleManage + (!selectAll ? '' : ` ${style.selected}`)}
        onClick={this.handleClickRoleManage}
      >
        {intl.formatMessage(messages.roleManageTitle)}
      </div>
    );
  };

  loadRoleTree = () => {
    return fetchNoTagRoleTree().then((res) => {
      const {
        data: { list }
      } = res;
      const childTreeData = extractRoleListData(list);
      // 虚拟公司节点
      const treeData = this.addFakeCompany(childTreeData);
      this.setState({
        treeData,
        selectRoleKeys: []
      });
      // 刷新标签, 用于新增编辑角色
      this.loadTagsList();
    });
  };

  loadTagsList() {
    return fetchTagsList().then((res) => {
      const {
        data: { list: tagsList }
      } = res;
      this.setState({
        tagsList
      });
    });
  }

  componentDidMount() {
    this.props.siderRef.current = this;
    // 获取当前用户公司信息
    this.getCurrentCompanyInfo().then((company) => {
      this.currentCompanyInfo = company;
      this.loadRoleTree();
    });
  }

  getCurrentCompanyInfo() {
    return getUserSessionInfo().then(({ data }) => {
      return data.userSessionInfo;
    });
  }

  handleAddRoleButtonClick = () => {
    this.showAddRoleModal();
  };

  showAddRoleModal = () => {
    this.setState({
      addRoleModalVisible: true
    });
  };

  hideAddRoleModal = () => {
    this.setState({
      addRoleModalVisible: false
    });
  };

  hideEditRoleModal = () => {
    this.setState({
      editRoleModalVisible: false
    });
  };

  handleAddRoleSave = (data) => {
    const { intl } = this.props;
    addRole(data).then(() => {
      this.loadRoleTree();
      this.hideAddRoleModal();
      message.success(intl.formatMessage(messages.roleCreateSuccess));
    });
  };

  handleEditRoleSave = (data) => {
    const {
      editRole: { rawTags }
    } = this.state;
    const { tags = [] } = data;
    // 匹配删除的tagsid
    const deleteIds = rawTags.reduce((acc, { id, name }) => {
      const existed = tags.indexOf(name) > -1;
      if (!existed) {
        acc.push(id);
      }
      return acc;
    }, []);
    data.deleteIds = deleteIds;
    updateRole(data).then(() => {
      this.props.handleUpdateRole(data);
      const { intl } = this.props;
      const { selectRoleKeys } = this.state;
      message.success(intl.formatMessage(messages.roleEditSuccess));
      this.loadRoleTree().then(() => {
        // 重新设置上次选中角色
        this.setState({
          selectRoleKeys
        });
      });
      this.hideEditRoleModal();
    });
  };

  handleEditRole = ({ code }) => {
    this.loadRoleDetail(code).then((editRole) => {
      // 编辑提交删除时用
      editRole.rawTags = editRole.tags;
      editRole.tags = (editRole.tags || []).map((d) => d.name);
      this.setState({
        editRole,
        editRoleModalVisible: true
      });
    });
  };

  handleClickRoleManage = () => {
    this.props.handleSelectAllRole();
    this.setState({
      selectRoleKeys: [ALL_ROLE_KEY]
    });
  };

  handleRemoveRole = ({ code, title: roleName }) => {
    const { intl } = this.props;
    Modal.confirm({
      cancelText: intl.formatMessage(messages.modalConfirmCancel),
      okText: intl.formatMessage(messages.modalConfirmOk),
      title: intl.formatMessage(messages.removeRoleTitle, { roleName }),
      content: intl.formatMessage(messages.removeRoleContent),
      onOk: () => {
        removeRole(code).then(() => {
          message.success(intl.formatMessage(messages.roleRemoveSuccess));
          this.props.handleRemoveRole(code);
          const { selectRoleKeys } = this.state;
          this.loadRoleTree().then(() => {
            // 重新恢复上次选中角色
            if (selectRoleKeys && selectRoleKeys[0] !== code) {
              this.setState({
                selectRoleKeys
              });
            }
          });
        });
      }
    });
  };

  loadRoleDetail(code) {
    return fetchRolDetail({ code }).then((res) => res.data.data);
  }

  handleSelectTag(roleKey) {
    const { handleSelectTag } = this.props;
    this.loadRoleDetail(roleKey).then((data) => {
      handleSelectTag(data);
    });
  }

  setSelectedKeys(selectedKeys) {
    this.setState({
      selectRoleKeys: selectedKeys,
      selectAll: false
    });
  }

  handleTreeSelect = (selectedKeys, { selected }) => {
    if (selectedKeys[0] === ALL_ROLE_KEY) {
      return this.handleClickRoleManage();
    }
    if (selected) {
      this.setSelectedKeys(selectedKeys);
      const [roleKey] = selectedKeys;
      this.handleSelectTag(roleKey);
    }
  };

  openAuthority = (row) => {
    const { intl } = this.props;
    if (process.env.NODE_ENV === 'development') {
      const url = `http://${window.location.host}/#/authority?id=${row.id}&status=role&name=${row.title}`;
      const w = window.open(url);
      w.document.title = intl.formatMessage(messages.authorityTitle, {
        name: row.name
      });
    } else {
      // const url = `http://${window.location.host}/auth/#/authority?id=${row.id}&status=role&name=${row.title}`;
      const url = `http://${window.location.host}/#/runtime-fullscreen/runtime-fullscreen/authority-set?id=${row.id}&status=role&name=${row.title}`;
      const w = window.open(url);
      w.document.title = intl.formatMessage(messages.authorityTitle, {
        name: row.name
      });
    }
  };

  optRender = (tree) => {
    const { intl, hasAuth } = this.props;
    const { name: title, id } = tree;
    if (!id) return title;
    const editAuth = hasAuth('editRole');
    const queryRoleAuth = hasAuth('queryRolePermissions');
    const deleteRoleAuth = hasAuth('deleteRole');
    const hasSomeAuth = editAuth || queryRoleAuth || deleteRoleAuth;
    if (!hasSomeAuth) return null;
    const hasTopAuth = (editAuth || queryRoleAuth) && deleteRoleAuth;
    const menu = (
      <Menu style={{ width: 100 }}>
        {editAuth && (
          <Menu.Item
            key="edit"
            onClick={(e) => {
              e.domEvent.stopPropagation();
              this.handleEditRole(tree);
            }}
          >
            {intl.formatMessage(messages.roleMenuEdit)}
          </Menu.Item>
        )}
        {queryRoleAuth && (
          <Menu.Item
            key="auth"
            disabled={!this.props.hasAuth('queryRolePermissions')}
            onClick={(e) => {
              e.domEvent.stopPropagation();
              this.openAuthority(tree);
            }}
          >
            {intl.formatMessage(messages.roleMenuAuth)}
          </Menu.Item>
        )}
        {hasTopAuth && <Menu.Divider />}
        {deleteRoleAuth && (
          <Menu.Item
            key="remove"
            disabled={!this.props.hasAuth('deleteRole')}
            onClick={(e) => {
              e.domEvent.stopPropagation();
              this.handleRemoveRole(tree);
            }}
          >
            {intl.formatMessage(messages.roleMenuRemove)}
          </Menu.Item>
        )}
      </Menu>
    );

    return (
      <span className="tree-title-wrap">
        <span className="tree-title-content">{title}</span>
        <Dropdown
          onClick={(e) => {
            e.stopPropagation();
          }}
          overlay={menu}
          overlayClassName={style.roleMenuOverlay}
          trigger={['click']}
          placement="bottomRight"
        >
          <span className="tree-action">
            <Icon type="ellipsis" theme="filled" />
          </span>
        </Dropdown>
      </span>
    );
  };

  handleSearch = (param, type) => {
    // type
    // fuzzy: 模糊回车
    // advanced: 点击单个搜索结果
    // clear: 清除搜索条件
    // TODO add company id

    if (type === 'advanced') {
      fetchRolDetail({ code: param.key }).then(({ data }) => {
        this.buildAdvSearchData(data.data);
      });
    } else if (type === 'clear') {
      this.setState({
        selectRoleKeys: []
      });
      this.loadRoleTree();
    } else if (type === 'fuzzy') {
      searchNoTagRoleByKeyword({ keyword: param.title }).then(({ data }) => {
        this.buildFuzzySearchData(data);
      });
    }
  };

  buildFuzzySearchData(data) {
    const childTreeData = extractRoleListData(data.list);
    // 虚拟公司节点
    // TODO 无结果时是否展示空
    const treeData = this.addFakeCompany(childTreeData);
    this.setState({
      treeData,
      selectRoleKeys: []
    });
  }

  addFakeCompany(childTreeData) {
    // FIXME 无结果时是否展示空
    // if (!childTreeData || !childTreeData.length) return [];
    const {
      currentCompanyInfo: { companyName }
    } = this;
    // 虚拟公司节点
    return [
      {
        key: ALL_ROLE_KEY,
        children: childTreeData,
        title: companyName
      }
    ];
  }

  buildAdvSearchData(data) {
    const { code: roleCode, id: roleId, name: roleName } = data;
    // 虚拟公司节点
    const treeData = this.addFakeCompany([
      {
        title: roleName,
        key: roleCode,
        id: roleId,
        code: roleCode
      }
    ]);
    this.setState({
      treeData,
      selectRoleKeys: [roleCode]
    });
    this.handleSelectTag(roleCode);
  }

  render() {
    const { intl } = this.props;

    const {
      addRoleModalVisible,
      editRoleModalVisible,
      editRole,
      tagsList,
      treeData,
      selectRoleKeys
    } = this.state;

    const addRoleModalProps = {
      onSubmit: this.handleAddRoleSave,
      modalProps: {
        visible: addRoleModalVisible,
        wrapClassName: style.addRoleModal,
        title: intl.formatMessage(messages.addRoleModalTitle),
        onCancel: this.hideAddRoleModal,
        width: 580
      },
      compData: {
        tags: tagsList
      }
    };

    const editRoleModalProps = {
      modalProps: {
        visible: editRoleModalVisible,
        wrapClassName: style.addRoleModal,
        title: intl.formatMessage(messages.editRoleModalTitle),
        onCancel: this.hideEditRoleModal,
        width: 580
      },
      initData: editRole,
      onSubmit: this.handleEditRoleSave,
      compData: {
        tags: tagsList
      }
    };

    const enableAddRole = this.props.hasAuth('addRole');
    const noBorderCls = enableAddRole ? '' : ` ${style.disabledAddRole}`;

    return (
      <>
        <SupTree
          placeholder={intl.formatMessage(messages.roleTreeSearchPlaceholder)}
          className={style.supTree + noBorderCls}
          selectedKeys={selectRoleKeys}
          showSearch
          switchCompany={false}
          showAdd={enableAddRole}
          onAdd={this.handleAddRoleButtonClick}
          dataSource={treeData}
          onSelect={this.handleTreeSelect}
          autoExpandRoot
          optRender={this.optRender}
          onSearch={this.handleSearch}
          fuzzyParams={{
            url: getSearchRoleByKeywordAPI(),
            param: 'keyword',
            callback: ({ list }) => {
              return list.map(({ code, name }) => {
                return {
                  key: code,
                  title: name
                };
              });
            }
          }}
        />
        <AddRoleModal {...addRoleModalProps} />
        <EditRoleModal {...editRoleModalProps} />
      </>
    );
  }
}

export default injectIntl(RoleSider);
