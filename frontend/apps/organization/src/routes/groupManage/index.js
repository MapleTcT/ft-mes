import React from 'react';
import { Layout, Modal, message } from 'sup-ui';
import SupIcon from 'sup-rc-icon';
import { injectIntl } from 'react-intl';
import SupResize from 'sup-rc-resize';
import style from './style.less';
import GroupManageSider from './Sider';
import GroupManageContent from './Content';
import {
  fetchGroups,
  removeGroup,
  addGroup,
  fetchGroupDetail,
  editGroup,
  companyTree as companyTreeApi
} from '../../services/groupManage';
import { getAuthority } from '../../services/personManage';

import { PAGECOUNT } from './constants';
import messages from './messages';
import WrappedGroupEditForm from './GroupEditForm';

const { Content, Header } = Layout;

// import MockJS from 'mockjs';
// const { Random } = MockJS;

// const fakeAddGroup = async (nums) => {
//   while (nums--) {
//     addGroup({
//       code: Random.string(),
//       companyId: 1000,
//       description: Random.sentence(),
//       managerName: Random.name(),
//       name: nums + Random.pick(['中控集团', '深蓝数智', 'Apple'])
//     });
//   }
// };

// fakeAddGroup(100);

class GroupManage extends React.Component {
  constructor(props) {
    super(props);
    this.groupManagerSider = React.createRef();
    this.groupEditForm = React.createRef();
    // 后期获取公司信息赋值
    this.selectCompanyValue = {};
  }

  state = {
    groupSearchValue: '',
    groupListData: [],
    activeGroupId: null,
    activeGroupData: {},
    selectCompanyValue: [],
    groupListPager: {
      total: 0,
      hasMore: false,
      pageSize: PAGECOUNT
    },
    editGroupModalVisible: false,
    editGroupData: {},
    editGroupId: null,
    btnAuths: null
  };

  getGroupAuth() {
    getAuthority('groupmanage').then(({ data: { list } }) => {
      this.setState({
        btnAuths: list
      });
    });
  }

  hasAuth = (code) => {
    const { btnAuths } = this.state;
    if (!code) return btnAuths;
    if (typeof code === 'string') {
      return btnAuths.includes(code);
    }
    return code.some((d) => btnAuths.includes(d));
  };

  componentDidMount() {
    this.getGroupAuth();
    this.loadCompanyTree().then((companyTree) => {
      const data = { companyTree };
      const [tree] = companyTree;
      if (tree) {
        data.selectCompanyTitle = tree.fullName;
        data.selectCompanyValue = [String(tree.id)];
        // 使用真实公司信息
        this.selectCompanyValue = {
          id: String(tree.id),
          shortName: tree.shortName
        };
      }
      this.setState(data);
      this.loadGroupList(true);
    });
  }

  loadCompanyTree() {
    return companyTreeApi().then((res) => {
      const {
        data: { list: data }
      } = res;
      // 生成公司树
      const companyTree = data.reduce((acc, tree, _, treeArr) => {
        const { parentId } = tree;
        // 根节点可能为0，或者为null
        if (parentId === 0 || parentId === null) {
          acc.push(tree);
        } else {
          const parentTree = treeArr.find((t) => t.id === parentId);
          // FIXME 默认父节点找不到做为根节点
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

      return companyTree;
    });
  }

  siderBackTop() {
    this.groupManagerSider.current.handleTreeScroll();
  }

  treeBackTop() {
    this.groupManagerSider.current.treeBackTop();
  }

  appendFakeCompanyNode(data) {
    const { selectCompanyValue } = this;
    return [
      {
        ...this.selectCompanyValue,
        name: selectCompanyValue.shortName,
        children: data
      }
    ];
  }

  loadGroupList(refresh, cb) {
    const {
      groupListPager: { current, pageSize },
      groupListData,
      selectCompanyValue,
      groupSearchValue
    } = this.state;

    fetchGroups({
      current: refresh ? 1 : current + 1,
      pageSize,
      companyId: selectCompanyValue[0],
      keyword: groupSearchValue
    }).then((res) => {
      if (refresh) {
        this.siderBackTop();
      }
      const {
        data: {
          list,
          pagination: {
            total,
            hasMore,
            current: currentPage,
            pageSize: currentPageSize
          }
        }
      } = res;
      const newGroupListData = refresh ? list : groupListData.concat(list);
      // FIXME 兼容后台返回没有hasMore字段
      let newHasMore = hasMore;
      if (typeof newHasMore === 'undefined') {
        newHasMore = newGroupListData.length < total;
      }

      this.setState(
        {
          groupListData: this.appendFakeCompanyNode(newGroupListData),
          groupListPager: {
            current: currentPage,
            pageSize: currentPageSize,
            total,
            hasMore: newHasMore
          }
        },
        cb
      );
    });
  }

  searchGroupItem = (param, type) => {
    // type
    // fuzzy: 模糊回车
    // advanced: 点击单个搜索结果
    // clear: 清除搜索条件

    if (type === 'advanced') {
      this.setState({
        groupSearchValue: ''
      });
      this.setState({
        groupListData: this.appendFakeCompanyNode([
          {
            id: param.id,
            name: param.title
          }
        ]),
        groupListPager: {
          current: 1,
          hasMore: false,
          pageSize: PAGECOUNT,
          total: 1
        }
      });
      // 选中该项
      this.handleSelectGroup([`${param.id}`]);
    } else {
      this.setState(
        {
          groupSearchValue: param.title || ''
        },
        () => {
          this.loadGroupList(true);
        }
      );
    }
  };

  refreshGroupDetail = (id) => {
    fetchGroupDetail(id).then((res) => {
      const {
        data: { data: activeGroupData }
      } = res;
      const { activeGroupId } = this.state;
      // 当前选中key为string
      if (activeGroupId === id.toString()) {
        this.setState({ activeGroupData });
      }
    });
  };

  handleSelectGroup = ([id]) => {
    // 及时响应用户点击
    this.setState({
      activeGroupId: id
    });
    // 点击公司不处理选择组逻辑
    if (`${id}` === `${this.selectCompanyValue.id}`) {
      // 点击公司时需要重置当前选中组
      this.setState({
        activeGroupData: null
      });
      return;
    }
    // 强制刷新
    // TODO 延迟请求
    this.refreshGroupDetail(id);
  };

  refreshGroupList = (cb) => {
    const { groupListPager } = this.state;
    this.setState(
      {
        groupListPager: {
          ...groupListPager,
          current: 1
        }
      },
      () => {
        this.loadGroupList(true, cb);
      }
    );
  };

  addCompanyIdParams(data) {
    const { selectCompanyValue } = this.state;
    data.companyId = selectCompanyValue[0] - 0;
  }

  formatManagers = (postData) => {
    // 处理负责人对象
    if (postData.managerPerson) {
      postData.managerIds = postData.managerPerson.map((d) => d.id);
      delete postData.managerPerson;
    }
  };

  handleAddGroup = (data, cb) => {
    const { intl } = this.props;
    const postData = { ...data };
    this.formatManagers(postData);
    this.addCompanyIdParams(postData);
    addGroup(postData).then(() => {
      this.refreshGroupList(cb);
      message.success(intl.formatMessage(messages.addGroupSuccess));
    });
  };

  handleEditGroup = (data, cb) => {
    const { editGroupId, activeGroupId } = this.state;
    const postData = { ...data };
    this.formatManagers(postData);
    this.addCompanyIdParams(postData);
    postData.id = editGroupId;
    editGroup(postData).then(() => {
      this.refreshGroupList(cb);
      // 如果编辑的是当前选中的需要刷新详情
      // FIXME 后台返回id为number, 设值key为string
      if (activeGroupId === postData.id.toString()) {
        this.refreshGroupDetail(postData.id);
      }
    });
  };

  handleRemoveGroup = (groupId) => {
    // set active group id when equals selected
    // eslint-disable-next-line compat/compat
    return new Promise((resolve) => {
      removeGroup(groupId).then(() => {
        const { intl } = this.props;
        this.treeBackTop();
        this.refreshGroupList(resolve);
        this.setState({
          activeGroupData: null,
          activeGroupId: null
        });
        message.success(intl.formatMessage(messages.removeGroupSuccess));
      });
    });
  };

  handleSelectCompany = (selectCompanyValue) => {
    this.selectCompanyValue = selectCompanyValue;
    this.setState(
      {
        selectCompanyValue: [selectCompanyValue.id]
      },
      () => {
        this.setState({
          activeGroupData: null,
          activeGroupId: null
        });
        this.refreshGroupList();
      }
    );
  };

  handleLoadGroupListData = (cb) => {
    this.loadGroupList(false, cb);
  };

  toggleEditGroupModal = (visible) => {
    this.setState((state) => {
      state.editGroupModalVisible = !!visible;
      return state;
    });
  };

  handleEditGroupClose = () => {
    this.toggleEditGroupModal(false);
  };

  handleEditGroupClick = (id) => {
    fetchGroupDetail(id).then((res) => {
      const {
        data: { data: editGroupData }
      } = res;
      this.setState({ editGroupId: id, editGroupData });
      this.toggleEditGroupModal(true);
    });
  };

  handleEditGroupSubmit = () => {
    const { intl } = this.props;
    const groupEditForm = this.groupEditForm.current;
    groupEditForm.validateFields().then(
      (editData) => {
        this.handleEditGroup(editData, () => {
          this.handleEditGroupClose();
          message.success(intl.formatMessage(messages.editGroupSuccess));
        });
      },
      (err) => {
        console.error(err);
      }
    );
  };

  render() {
    const {
      activeGroupId,
      activeGroupData,
      groupListData,
      groupListPager,
      editGroupModalVisible,
      editGroupData,
      selectCompanyValue,
      btnAuths
    } = this.state;

    if (!btnAuths) return null;

    const { intl } = this.props;

    return (
      <div className={style.groupManage}>
        <Modal
          title={intl.formatMessage(messages.basicInfo)}
          visible={editGroupModalVisible}
          onOk={this.handleEditGroupSubmit}
          onCancel={this.handleEditGroupClose}
          destroyOnClose
          maskClosable={false}
          wrapClassName={style.groupEditFormWrap}
        >
          <div>
            <WrappedGroupEditForm
              initialValueObj={editGroupData}
              ref={this.groupEditForm}
              isEdit
            />
          </div>
        </Modal>

        <Layout className={style.layout}>
          <Header className={style.topHeader}>
            {intl.formatMessage(messages.groupManageTitle)}
          </Header>
          <Content className={style.mainContent}>
            <Layout className={style.layout}>
              <SupResize min={220}>
                <GroupManageSider
                  activeGroupId={activeGroupId}
                  selectGroup={this.handleSelectGroup}
                  addGroup={this.handleAddGroup}
                  removeGroup={this.handleRemoveGroup}
                  selectCompany={this.handleSelectCompany}
                  groupListPager={groupListPager}
                  groupListData={groupListData}
                  loadGroupListData={this.handleLoadGroupListData}
                  siderRef={this.groupManagerSider}
                  handleEditGroupClick={this.handleEditGroupClick}
                  companyId={selectCompanyValue[0]}
                  onSearchValueChange={this.searchGroupItem}
                  hasAuth={this.hasAuth}
                />
                {activeGroupId && activeGroupData && activeGroupData.code ? (
                  <GroupManageContent
                    key={activeGroupData.code}
                    activeGroupId={activeGroupId}
                    activeGroupData={activeGroupData}
                    handleEditGroupClick={this.handleEditGroupClick}
                    hasAuth={this.hasAuth}
                  />
                ) : (
                  <Layout style={{ height: '100%' }}>
                    <div className={style.emptyContent}>
                      <SupIcon className={style.backIcon} type="iconpoint" />
                      <span>{intl.formatMessage(messages.chooseLeftItem)}</span>
                    </div>
                  </Layout>
                )}
              </SupResize>
            </Layout>
          </Content>
        </Layout>
      </div>
    );
  }
}

export default injectIntl(GroupManage);
