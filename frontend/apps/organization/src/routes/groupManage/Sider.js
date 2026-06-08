import React from 'react';
import { injectIntl } from 'react-intl';
import SupTree from 'sup-rc-tree';
import { Icon, Modal } from 'sup-ui';
import style from './style.less';
import WrappedGroupEditForm from './GroupEditForm';
import messages from './messages';
import { getFetchGroupsUrl } from '../../services/groupManage';

class GroupManageSider extends React.Component {
  constructor(props) {
    super(props);
    this.treeRef = React.createRef();
    this.groupAddForm = React.createRef();
    this.backTopRef = React.createRef();
    this.treeCustomParams = [
      {
        align: 'bottom',
        record: this.renderFooter
      }
    ];
  }

  state = {
    addGroupModalVisible: false
  };

  componentDidMount() {
    const {
      current: { treeDOM }
    } = this.treeRef;
    treeDOM.addEventListener('scroll', this.handleTreeScroll);
    this.props.siderRef.current = this;
  }

  componentWillUnmount() {
    const {
      current: { treeDOM }
    } = this.treeRef;
    treeDOM.removeEventListener('scroll', this.handleTreeScroll);
    this.props.siderRef.current = null;
  }

  handleTreeScroll = () => {
    const {
      current: { treeDOM }
    } = this.treeRef;
    const { current: backtop } = this.backTopRef;

    const { scrollTop } = treeDOM;
    let display = 'none';
    if (scrollTop > 0) {
      display = '';
    }
    backtop.style.display = display;
  };

  treeBackTop() {
    const {
      current: { treeDOM }
    } = this.treeRef;
    treeDOM.scrollTop = 0;
  }

  renderFooter = () => {
    const { groupListPager } = this.props;
    return (
      <div style={{}} className={style.groupItemFooter}>
        <span className={style.groupItemFooterTotal}>
          {this.intl(messages.totalGroupNumber, {
            total: groupListPager.total
          })}
        </span>
        <a
          ref={this.backTopRef}
          href="#"
          style={{
            textDecoration: 'underline',
            display: 'none'
          }}
          onClick={(e) => {
            e.preventDefault();
            this.treeBackTop();
          }}
        >
          {this.intl(messages.backTop)}
        </a>
      </div>
    );
  };

  toggleAddGroupModal = (visible) => {
    this.setState((state) => {
      state.addGroupModalVisible = !!visible;
      return state;
    });
  };

  intl(...args) {
    const { intl } = this.props;
    return intl.formatMessage(...args);
  }

  handleEditGroupClick = (e, activeGroupId) => {
    e.preventDefault();
    e.stopPropagation();
    this.props.handleEditGroupClick(activeGroupId);
  };

  handleRemoveGroupBtnClick = (e, activeGroupId) => {
    e.preventDefault();
    e.stopPropagation();
    Modal.confirm({
      cancelText: this.intl(messages.modalCancel),
      okText: this.intl(messages.modalok),
      title: this.intl(messages.removeModalTitle),
      content: this.intl(messages.removeModalContent),
      onOk: () => this.props.removeGroup(activeGroupId)
    });
  };

  handleAddGroupButtonClick = () => {
    this.toggleAddGroupModal(true);
  };

  handleAddGroupSubmit = () => {
    const groupAddForm = this.groupAddForm.current;
    groupAddForm.validateFields().then(
      (data) => {
        this.props.addGroup(data, () => {
          this.toggleAddGroupModal(false);
        });
      },
      (err) => {
        console.error(err);
      }
    );
  };

  handleAddGroupCancel = () => {
    this.toggleAddGroupModal(false);
  };

  handleCompanyListTreeSelect = (selectCompanyValue) => {
    if (selectCompanyValue) {
      this.props.selectCompany(selectCompanyValue);
    }
  };

  optRender = (item) => {
    const enableEdit = this.props.hasAuth('updateGroup');
    const enableDelete = this.props.hasAuth('deletGroup');
    return (
      <div className={style.actionGroup}>
        {enableEdit ? (
          <a
            href="#"
            onClick={(e) => {
              this.handleEditGroupClick(e, item.id);
            }}
          >
            <Icon type="edit" />
          </a>
        ) : null}
        {enableDelete ? (
          <a
            style={{ marginLeft: '5px' }}
            href="#"
            onClick={(e) => {
              this.handleRemoveGroupBtnClick(e, item.id);
            }}
          >
            <Icon type="delete" />
          </a>
        ) : null}
      </div>
    );
  };

  render() {
    const {
      groupListData = [],
      activeGroupId,
      groupListPager,
      companyId
    } = this.props;
    const { pageSize } = groupListPager;
    const createAuth = this.props.hasAuth('createGroup');
    const treeCls = createAuth ? '' : `${style.noTreeBorder}`;
    const selectedKeys = activeGroupId ? [activeGroupId] : [];
    return (
      <>
        <Modal
          title={this.intl(messages.addGroup)}
          visible={this.state.addGroupModalVisible}
          onOk={this.handleAddGroupSubmit}
          onCancel={this.handleAddGroupCancel}
          destroyOnClose
          maskClosable={false}
          wrapClassName={style.groupEditFormWrap}
        >
          <div>
            <WrappedGroupEditForm ref={this.groupAddForm} />
          </div>
        </Modal>
        <SupTree
          placeholder={this.intl(messages.groupTreeSearchPlaceholder)}
          treeKey="id"
          treeTitle="name"
          className={treeCls}
          selectedKeys={selectedKeys}
          showSearch
          showAdd={createAuth}
          autoExpandRoot
          switchCompany
          ref={this.treeRef}
          onAdd={this.handleAddGroupButtonClick}
          onSelectCompany={this.handleCompanyListTreeSelect}
          dataSource={groupListData}
          onSelect={this.props.selectGroup}
          customParams={this.treeCustomParams}
          optRender={this.optRender}
          onSearch={this.props.onSearchValueChange}
          fuzzyParams={{
            url: getFetchGroupsUrl(),
            otherParams: `companyId=${companyId}&current=1&pageSize=${pageSize}`,
            param: 'keyword',
            callback: ({ list }) => {
              return list.map(({ name, id }) => {
                return {
                  title: name,
                  id
                };
              });
            }
          }}
          scrollParams={{
            hasMore: groupListPager.hasMore,
            loadMore: this.props.loadGroupListData
          }}
        />
      </>
    );
  }
}

export default injectIntl(GroupManageSider);
