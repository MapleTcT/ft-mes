import React from 'react';
import {
  searchGroupPerson,
  groupConnectPerson,
  groupDisconnectPerson
} from 'root/services/groupManage.js';
import { SupReferenceView } from 'sup-rc-reference';
import {
  Layout,
  Modal,
  Tooltip,
  message,
  Icon,
  Button,
  Row,
  Col
} from 'sup-ui';
import { injectIntl } from 'react-intl';
import style from './style.less';
import messages from './messages';
import { LIST_PAGECOUNT } from './constants';
import GroupContentTable from './ContentTable';
import IconConnect from './IconConnect';

const { Header, Content } = Layout;
// const { Group: ButtonGroup } = Button;

const PAGINATION = {
  pageSize: LIST_PAGECOUNT,
  current: 1,
  total: 0
};
class GroupManageContent extends React.Component {
  state = {
    dataSource: [],
    pagination: PAGINATION,
    refUserVisible: false,
    keyword: ''
  };

  constructor(props) {
    super(props);
    this._loadPerson = false;
    this.groupEditForm = React.createRef();
    const { intl } = props;
    this.state.columns = [
      {
        title: intl.formatMessage(messages.tableColumnPersonName),
        dataIndex: 'name',
        width: 300
      },
      {
        title: intl.formatMessage(messages.tableColumnPersonCode),
        dataIndex: 'code',
        width: 300
      },
      {
        title: intl.formatMessage(messages.tableColumnSex),
        dataIndex: 'gender',
        width: 100
      },
      {
        title: intl.formatMessage(messages.tableColumnOperate),
        type: 'operation',
        authority: () => {
          return this.props.hasAuth(['createGroup', 'updateGroup']);
        },
        width: 150,
        render: (_, row) => {
          return (
            <a
              href="#"
              onClick={(e) => {
                e.preventDefault();
                Modal.confirm({
                  title: intl.formatMessage(messages.disconnectModalTitle, {
                    name: row.name
                  }),
                  content: intl.formatMessage(messages.disconnectModalCotent),
                  onOk: () => {
                    this.disConnectUser(row.id, () => {
                      if (this.selecteRows && this.selecteRows.length) {
                        const index = this.selecteRows.findIndex(
                          (d) => d === row.id
                        );
                        if (index > -1) {
                          this.selecteRows.splice(index, 1);
                          this.refreshDeleteBtnState();
                        }
                      }
                    });
                  }
                });
              }}
            >
              {intl.formatMessage(messages.disconnectOperateBtn)}
            </a>
          );
        }
      }
    ];

    this.btnColumns = [
      {
        key: 'connectUser',
        className: 'sup-btn-primary sup-btn-background-ghost',
        authority: () => {
          return this.props.hasAuth(['createGroup', 'updateGroup']);
        },
        content: () => (
          <div>
            <IconConnect style={{ marginRight: 5 }} />
            {intl.formatMessage(messages.connectBtn)}
          </div>
        ),
        callback: this.showConnectModal
      },
      // {
      //   key: 'exportBtn',
      //   content: intl.formatMessage(messages.exportBtn),
      //   callback: () => {
      //     // params.callback();
      //     this.underWork();
      //   }
      //   // menu: [
      //   //   {
      //   //     key: 'exportAll',
      //   //     callback: this.underWork,
      //   //     content: intl.formatMessage(messages.exportAllBtn)
      //   //   },
      //   //   {
      //   //     key: 'exportSelect',
      //   //     content: intl.formatMessage(messages.exportSelectBtn),
      //   //     callback: this.underWork
      //   //   }
      //   // ]
      // },
      {
        key: 'delete',
        disabled: true,
        authority: () => {
          return this.props.hasAuth(['createGroup', 'updateGroup']);
        },
        content: intl.formatMessage(messages.disConnectUserBtn),
        callback: () => {
          const { selecteRows } = this;
          Modal.confirm({
            title: intl.formatMessage(messages.disconnectModalTitleAll),
            content: intl.formatMessage(messages.disconnectModalCotent),
            onOk: () => {
              const ids = selecteRows.join(',');
              this.disConnectUser(ids, () => {
                this.selecteRows = [];
                this.refreshDeleteBtnState();
              });
            }
          });
        }
      }
      // {
      //   key: 'more',
      //   callback: (params) => {
      //     params.callback();
      //   },
      //   menu: [
      //     {
      //       key: 'importBtn',
      //       content: intl.formatMessage(messages.importBtn),
      //       callback: this.underWork,
      //       authority: () => {
      //         return this.props.hasAuth(['createGroup', 'updateGroup']);
      //       }
      //     },
      //     {
      //       key: 'exportTemplate',
      //       content: intl.formatMessage(messages.exportTemplateBtn),
      //       callback: this.underWork,
      //       authority: () => {
      //         return this.props.hasAuth(['createGroup', 'updateGroup']);
      //       }
      //     }
      //   ]
      // }
    ];
  }

  refreshDeleteBtnState() {
    const { selecteRows } = this;
    let disabled = true;
    if (selecteRows && selecteRows.length) {
      disabled = false;
    }
    const deleteBtn = this.btnColumns.find((d) => d.key === 'delete');
    deleteBtn.disabled = disabled;
    this.setState({});
  }

  loadGroupPerson() {
    const {
      pagination: { current, pageSize },
      keyword
    } = this.state;
    const { activeGroupId } = this.props;

    searchGroupPerson({
      groupId: activeGroupId,
      current,
      // 兼容后台分页返回为0的情况
      pageSize: pageSize <= 0 ? LIST_PAGECOUNT : pageSize,
      keyword
    }).then((res) => {
      this._loadPerson = true;
      const {
        data: { list, pagination }
      } = res;

      this.setState({
        dataSource: list,
        pagination
      });
    });
  }

  componentDidMount() {
    this.loadGroupPerson();
  }

  underWork = () => {
    message.info('功能开发中');
  };

  handleEditGroupButtonClick = (e) => {
    e.preventDefault();
    this.props.handleEditGroupClick(this.props.activeGroupId);
  };

  showConnectModal = () => {
    this.setState({
      refUserVisible: true
    });
  };

  connectUser = (val) => {
    const { activeGroupId, intl } = this.props;
    const postData = {
      groupId: activeGroupId,
      persons: val.map((d) => d.id)
    };
    groupConnectPerson(postData).then(() => {
      message.success(intl.formatMessage(messages.connectUserSuccess));
      this.loadGroupPerson();
    });
  };

  disConnectUser = (ids, cb) => {
    const { activeGroupId, intl } = this.props;
    groupDisconnectPerson(activeGroupId, ids).then(() => {
      message.success(intl.formatMessage(messages.disconnectUserSuccess));
      this.loadGroupPerson();
      if (cb) {
        cb();
      }
    });
  };

  handleChangeSelectItem = (rows) => {
    this.selecteRows = rows;
    this.refreshDeleteBtnState();
  };

  handleOnSearch = ({ pagination, keyword }) => {
    this.setState(
      {
        pagination,
        keyword: keyword || ''
      },
      () => {
        this.loadGroupPerson();
      }
    );
  };

  updateColumns = (columns) => {
    this.setState({ columns });
  };

  renderEmptyChooseConnect() {
    const { intl, hasAuth } = this.props;
    const enableConnect = hasAuth(['createGroup', 'updateGroup']);
    return (
      <div className={style.emptyTableWrap}>
        <p style={{ marginBottom: 10 }}>
          {intl.formatMessage(messages.chooseConnectFirst)}
        </p>
        {enableConnect ? (
          <div>
            <Button ghost type="primary" onClick={this.showConnectModal}>
              <div>
                <IconConnect style={{ marginRight: 5 }} />
                {intl.formatMessage(messages.connectBtn)}
              </div>
            </Button>

            {/* <ButtonGroup style={{ marginLeft: 10 }}>
              <Button onClick={this.underWork}>
                {intl.formatMessage(messages.importBtn)}
              </Button>
              <Button onClick={this.underWork}>
                {intl.formatMessage(messages.exportTemplateBtn)}
              </Button>
            </ButtonGroup> */}
          </div>
        ) : null}
      </div>
    );
  }

  render() {
    const { activeGroupData, intl } = this.props;
    const {
      dataSource,
      pagination,
      refUserVisible,
      columns,
      keyword
    } = this.state;
    const enableEdit = this.props.hasAuth('updateGroup');

    const tableProps = {
      rowKey: 'id',
      onSelectItem: this.handleChangeSelectItem,
      onSearch: this.handleOnSearch,
      showSelection: true,
      pagination,
      btnColumns: this.btnColumns,
      columns,
      updateColumns: this.updateColumns,
      tableKey: 'groupUserTable',
      dataSource,
      showColumnsFilter: false,
      searchPlaceholder: intl.formatMessage(
        messages.groupTablesearchPlaceholder
      )
    };

    return (
      <Layout style={{ height: '100%' }}>
        <SupReferenceView
          type="staff"
          title={intl.formatMessage(messages.userReferenceTitle)}
          height="600px"
          destroyOnClose
          onCancel={() => {
            this.setState({ refUserVisible: false });
          }}
          visible={refUserVisible}
          multiple
          onOk={(val) => {
            this.connectUser(val);
            this.setState({
              refUserVisible: false
            });
          }}
        />
        <Header className={`${style.contentHeader} ${style.shadow}`}>
          {activeGroupData.name}
        </Header>
        <Content className={style.content}>
          <div className={style.contentWrap}>
            <h1 className={style.contentTitle}>
              <span>{intl.formatMessage(messages.contentInfoBasic)}</span>
              {enableEdit ? (
                <Tooltip title={intl.formatMessage(messages.editGroup)}>
                  <a
                    href="#"
                    onClick={this.handleEditGroupButtonClick}
                    className={style.contentEditBtn}
                  >
                    <Icon type="edit" className={style.editIcon} />
                  </a>
                </Tooltip>
              ) : null}
            </h1>
            <Row className={style.contentBody}>
              <Col span={12}>
                <div className={style.contentBodyItem}>
                  <span className={style.contentBodyItemProp}>
                    {`${intl.formatMessage(messages.contentInfoBasicName)}：`}
                  </span>
                  <span className={style.contentBodyItemValue}>
                    {activeGroupData.name}
                  </span>
                </div>
              </Col>
              <Col span={12}>
                {' '}
                <div className={style.contentBodyItem}>
                  <span className={style.contentBodyItemProp}>
                    {`${intl.formatMessage(messages.contentInfoBasicCode)}：`}
                  </span>
                  <span className={style.contentBodyItemValue}>
                    {activeGroupData.code}
                  </span>
                </div>
              </Col>
              <Col span={12}>
                <div className={style.contentBodyItem}>
                  <span className={style.contentBodyItemProp}>
                    {`${intl.formatMessage(
                      messages.contentInfoBasicManagerName
                    )}：`}
                  </span>
                  <span className={style.contentBodyItemValue}>
                    {(activeGroupData.managers || [])
                      .map((d) => d.managerName)
                      .join(' / ')}
                  </span>
                </div>
              </Col>
              <Col span={12}>
                <div className={style.contentBodyItem}>
                  <span className={style.contentBodyItemProp}>
                    {`${intl.formatMessage(
                      messages.contentInfoBasicDescription
                    )}：`}
                  </span>
                  <span
                    className={style.contentBodyItemValue}
                    title={activeGroupData.description}
                  >
                    {activeGroupData.description}
                  </span>
                </div>
              </Col>
            </Row>

            <h1 className={style.contentTitle}>
              <span>{intl.formatMessage(messages.connectUserTitle)}</span>
            </h1>
            <div className={style.contentTableBody}>
              {(dataSource && dataSource.length) || keyword ? (
                <GroupContentTable {...tableProps} />
              ) : this._loadPerson ? (
                this.renderEmptyChooseConnect()
              ) : null}
            </div>
          </div>
        </Content>
      </Layout>
    );
  }
}

export default injectIntl(GroupManageContent);
