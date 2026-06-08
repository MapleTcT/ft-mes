import React from 'react';
import { Layout, message } from 'sup-ui';
import SupIcon from 'sup-rc-icon';
import { injectIntl } from 'react-intl';
import SupResize from 'sup-rc-resize';
import style from './style.less';
import DepartmentSider from './Sider';
import DepartmentContent from './Content';
import PositionContent from './PositionContent';
import {
  editDepartment,
  departmentDetail,
  positionDetail,
  editPosition,
  relatedRoles,
  searchPositionRoles,
  getPositionRelatedPerson,
  getDepRelatedPerson,
  getAuthority
} from '../../services/departmentManage';
import { PAGECOUNT } from './constants';
import messages from './messages';
// import 'sup-rc-resize/dist/index.css';

const { Header } = Layout;
const PAGINATION = {
  pageSize: PAGECOUNT,
  current: 1
  // total: 0
};

class DepartmentManage extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      menuKey: 'department',
      buttonAuthority: [],
      activeDepartmentId: null,
      activeDepartmentData: {},
      activePositionId: null,
      activePositionData: {},
      positionRoles: [],
      positionPersonList: [],
      pagination: PAGINATION,
      total: 50,
      relPosList: [],
      depPersonList: [],
      companyId: ''
    };
    this.updateDepState = this.updateDepState.bind(this);
    this.updatePosState = this.updatePosState.bind(this);
  }

  componentWillMount() {
    getAuthority('organizationmanage').then((res) => {
      this.setState({
        buttonAuthority: _.get(res, 'data.list', [])
      });
    });
  }

  getMenuKey = (value) => {
    this.setState({
      menuKey: value
    });
  };

  getFlag = (value) => {
    this.setState({
      showSearch: value
    });
  }

  updatePersonList = (value) => {
    if (this.state.menuKey === 'department') {
      this.setState({
        depPersonList: value
      });
    } else {
      this.setState({
        positionPersonList: value
      });
    }
  }

  getCompanyId = (companyId) => {
    this.setState({
      companyId
    });
  }

  updateDepState = (newData, newId) => {
    this.setState({
      activeDepartmentData: newData,
      activeDepartmentId: newId
    });
  };

  updatePosState = (newData, newId) => {
    this.setState({
      activePositionData: newData,
      activePositionId: newId
    });
  };

  refreshDepartmentDetail(id) {
    departmentDetail(id).then((res) => {
      const {
        data: { data: activeDepartmentData }
      } = res;
      const { relPos } = res.data.data;
      this.setState({
        activeDepartmentId: id,
        activeDepartmentData,
        relPosList: relPos
      });
    });
  }

  handleSelectDepartment = (id) => {
    this.refreshDepartmentDetail(id);
    this.handleGetDepRelatedPerson(id);
    this.setState({
      showSearch: false
    });
  };

  refreshPositionDetail(id) {
    positionDetail(id).then((res) => {
      const {
        data: { data: activePositionData }
      } = res;
      this.setState({
        activePositionId: id,
        activePositionData
      });
    });
  }

  handleSelectPosition = (id) => {
    this.refreshPositionDetail(id);
    this.handleSearchPositionRoles(id);
    this.handleGetPositionRelatedPerson(id);
    this.setState({
      showSearch: false
    });
  };

  handleEditDepartment = (depData, cb) => {
    const { intl } = this.props;
    editDepartment(depData, cb).then(() => {
      this.refreshDepartmentDetail(this.state.activeDepartmentId);
      if (cb) {
        cb();
      }
      this.DepartmentSider.initTree('', this.state.activeDepartmentId);
      message.success(intl.formatMessage(messages.modification));
    });
  };

  handleEditPosition = (postData, cb) => {
    const { intl } = this.props;
    editPosition(postData, cb).then(() => {
      this.refreshPositionDetail(this.state.activePositionId);
      if (cb) {
        cb();
      }
      this.DepartmentSider.initTree('', this.state.activePositionId);
      message.success(intl.formatMessage(messages.modification));
    });
  };

  // handleRemoveDepartment = (departmentId) => {
  //   // return new Promise((resolve) => {
  //   removeDepartment(departmentId).then(() => {
  //     // window.console.log(resolve);

  //   })
  //   // });
  // };

  handleRelatedRoles = (postId, ids) => {
    const roleData = {
      positionId: postId,
      roleIds: ids
    };
    relatedRoles(roleData).then(() => {
      this.handleSearchPositionRoles(postId);
    });
  };

  handleSearchPositionRoles = (id) => {
    searchPositionRoles(id).then((res) => {
      const { list } = res.data;
      this.setState({
        positionRoles: list
      });
    });
  };

  handleGetDepRelatedPerson = (id) => {
    getDepRelatedPerson({
      companyId: this.state.companyId,
      departmentId: id,
      current: this.state.pagination.current,
      pageSize: this.state.pagination.pageSize
    }).then((res) => {
      const { list, pagination } = res.data;
      const { total } = pagination;
      this.setState({
        depPersonList: list,
        pagination,
        total
      });
    });
  };

  handleGetPositionRelatedPerson = (id) => {
    getPositionRelatedPerson({
      companyId: this.state.companyId,
      positionId: id,
      current: this.state.pagination.current,
      pageSize: this.state.pagination.pageSize
    }).then((res) => {
      const { list, pagination } = res.data;
      const { total } = pagination;
      this.setState({
        positionPersonList: list,
        pagination,
        total
      });
    });
  };

  changeTotal = (value) => {
    this.setState({
      total: value
    });
  }

  render() {
    const { intl } = this.props;
    const {
      menuKey,
      buttonAuthority,
      activeDepartmentData,
      activePositionData,
      activeDepartmentId,
      activePositionId,
      positionRoles,
      positionPersonList,
      total,
      relPosList,
      depPersonList,
      showSearch,
      companyId
    } = this.state;
    return (
      <Layout className={style.layout}>
        <Header className={style.head}>{intl.formatMessage(messages.contentHeaderTitle)}</Header>
        <Layout style={{ height: 'calc(100% - 56px)' }}>
          <SupResize
            min={220}
            max={320}
            direction="col"
          >
            <DepartmentSider
              getMenuKey={this.getMenuKey}
              buttonAuthority={buttonAuthority}
              getCompanyId={this.getCompanyId}
              selectDepartment={this.handleSelectDepartment}
              selectPosition={this.handleSelectPosition}
              // removeDepartment={this.handleRemoveDepartment}
              activeDepartmentId={activeDepartmentId}
              activeDepartmentData={activeDepartmentData}
              activePositionId={activePositionId}
              activePositionData={activePositionData}
              updateDepState={this.updateDepState}
              updatePosState={this.updatePosState}
              onRef={(ref) => {
                this.DepartmentSider = ref;
              }}
            />
            {
              menuKey === 'department' ? (
                activeDepartmentId ? (
                  <DepartmentContent
                    editDepartment={this.handleEditDepartment}
                    buttonAuthority={buttonAuthority}
                    activeDepartmentData={activeDepartmentData}
                    activeDepartmentId={activeDepartmentId}
                    menuKey={menuKey}
                    relPosList={relPosList}
                    // pagination={pagination}
                    total={total}
                    changeTotal={this.changeTotal}
                    depPersonList={depPersonList}
                    companyId={companyId}
                    updatePersonList={this.updatePersonList}
                    getFlag={this.getFlag}
                    showSearch={showSearch}
                  />
                ) : (
                  <Layout style={{ height: '100%' }}>
                    <div className={style.emptyContent}>
                      <SupIcon className={style.backIcon} type="iconpoint" />
                      {intl.formatMessage(messages.chooseLeftItem)}
                    </div>
                  </Layout>
                )
              ) : (
                activePositionId ? (
                  <PositionContent
                    editPosition={this.handleEditPosition}
                    buttonAuthority={buttonAuthority}
                    relatedRoles={this.handleRelatedRoles}
                    activePositionId={activePositionId}
                    activePositionData={activePositionData}
                    menuKey={menuKey}
                    searchPositionRoles={this.handleSearchPositionRoles}
                    positionRoles={positionRoles}
                    positionPersonList={positionPersonList}
                    // pagination={pagination}
                    total={total}
                    changeTotal={this.changeTotal}
                    getPositionRelatedPerson={this.handleGetPositionRelatedPerson}
                    companyId={companyId}
                    updatePersonList={this.updatePersonList}
                    getFlag={this.getFlag}
                    showSearch={showSearch}
                  />
                ) : (
                  <Layout style={{ height: '100%' }}>
                    <div className={style.emptyContent}>
                      {/* <Icon type="arrow-left" style={{ marginRight: '5px' }} /> */}
                      <SupIcon className={style.backIcon} type="iconpoint" />
                      {intl.formatMessage(messages.chooseLeftItem)}
                    </div>
                  </Layout>
                )
              )
            }
          </SupResize>
        </Layout>
      </Layout>
    );
  }
}

export default injectIntl(DepartmentManage);
