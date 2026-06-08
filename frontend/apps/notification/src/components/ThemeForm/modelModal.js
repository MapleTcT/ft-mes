import React from 'react';
import { Button, Modal, Tooltip, Layout, Menu, Tag, Radio, Icon } from 'sup-ui';
import SupTable from 'sup-rc-table';
import { getNotice, getContent } from 'root/services/messageCenter';
import { injectIntl } from 'react-intl';
import commonMessage from 'root/common/messages';
import AddModel from 'root/components/AddModel';
import ContentDetail from 'root/components/Content';
import styles from './styles.less';

const { Content, Sider, Header } = Layout;
@injectIntl
export default class ModelModal extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.state = {
      tableVisible: false,
      total: 0,
      okButtonDisabled: true,
      visibleContent: false,
      selectedRows: [],
      menuDefault: [],
      recordData: {},
      filteredInfo: {},
      defaultOpen: '',
      selectMenu: '',
      pageSize: 20,
      pageNo: 1,
      list: [],
      menu: [],
      filterParams: {
        code: {
          url: '/inter-api/notification-admin/v1/notice/template/keyword',
          param: 'code',
          callback: (result) => {
            const data = [];
            result.data.list.forEach((item) => {
              data.push(item.code);
            });
            return data;
          }
        },
        name: {
          url: '/inter-api/notification-admin/v1/notice/template/keyword',
          param: 'name',
          callback: (result) => {
            const data = [];
            result.data.list.forEach((item) => {
              data.push(item.name);
            });
            return data;
          }
        }
      },
      columns: [
        {
          title: '',
          dataIndex: 'checkbox',
          width: 60,
          align: 'center',
          render: (text, record) => {
            const { selectedRows, defaultOpen } = this.state;
            const id = _.get(selectedRows.find((x) => x.protocol_id.toString() === defaultOpen), 'id', '');
            return (
              <Radio key={record.id} checked={record.id === id} onChange={() => { this.chooseData(record); }} />
            );
          }
        },
        {
          title: intl.formatMessage(commonMessage.modelCode),
          dataIndex: 'code',
          filterType: 'search',
          width: 120
        },
        {
          title: intl.formatMessage(commonMessage.modelName),
          dataIndex: 'name',
          filterType: 'search',
          width: 120
        },
        {
          title: intl.formatMessage(commonMessage.desc),
          dataIndex: 'memo',
          width: 150
        },
        {
          title: intl.formatMessage(commonMessage.operate),
          dataIndex: 'operation',
          type: 'operation',
          width: 120,
          fixed: false,
          render: (text, record) => {
            return (
              <span>
                <a onClick={() => { this.content(record); }}>
                  {intl.formatMessage(commonMessage.content)}
                </a>
              </span>
            );
          }
        }
      ]
    };
  }

  componentWillMount() {
    this.setState({
      selectedRows: this.props.formTableData
    });
    this.fiterColumn();
    getNotice().then((res) => {
      this.setState({
        defaultOpen: _.get(res, 'data.list[0].id', '').toString(),
        menuDefault: [_.get(res, 'data.list[0].id', '').toString()],
        menu: res.data.list.map((x) => {
          return {
            key: x.id,
            name: x.name,
            protocol: x.protocol
          };
        })
      }, () => {
        this.menuChoose({
          key: _.get(res, 'data.list[0].id', ''),
          item: {
            props: {
              value: _.get(res, 'data.list[0]', {})
            }
          }
        });
      });
    });
  }

  componentDidMount() {
    // if (this.table && this.table.changeOperationFixedStatus) {
    //   this.table.changeOperationFixedStatus();
    // }
  }

  fiterColumn = () => {
    const { columns } = this.state;
    columns.forEach((item) => {
      item.filteredValue = this.state.filteredInfo[item.dataIndex] || [];
    });
  }

  menuChoose = (value) => {
    this.setState({
      filteredInfo: {}
    }, () => {
      this.fiterColumn();
    });
    getContent({ pageNo: 1, pageSize: 20, noticeTypeIds: value.key })
      .then((res) => {
        this.setState({
          pageNo: 1,
          total: res.data.pagination.total,
          list: res.data.list,
          selectMenu: value.key,
          selectprotocol: value.item.props.value.protocol
        });
      });
  }

  handleTableChange = ({ pagination, filters }) => {
    const { current, pageSize } = pagination;
    const { selectMenu } = this.state;
    this.setState({
      filteredInfo: filters
    }, () => {
      this.fiterColumn();
    });
    const search = Object.assign(filters, {
      noticeTypeIds: selectMenu,
      pageNo: current,
      pageSize
    });
    getContent(search).then((res) => {
      this.setState({
        list: res.data.list,
        total: res.data.pagination.total,
        pageNo: res.data.pagination.current,
        pageSize: res.data.pagination.pageSize
      });
    });
  };

  setModal1Visible = (visible) => {
    this.setState({
      okButtonDisabled: true,
      tableVisible: visible
    });
  }

  submitAddModel = (e) => {
    this.addmodel.handleSubmit(e);
  }

  chooseData = (record) => {
    const { selectedRows, defaultOpen } = this.state;
    const copyList = selectedRows.concat([]);
    const isExit = copyList.filter((x) => x.protocol_id.toString() === defaultOpen).length;
    if (isExit > 0) {
      const index = copyList.findIndex((x) => x.protocol_id.toString() === defaultOpen);
      copyList.splice(index, 1);
    }
    copyList.push(record);
    this.setState({
      selectedRows: copyList
    });
  }

  menuSelect = ({ key }) => {
    this.setState({
      defaultOpen: key
    });
  }

  closeTag = (index) => {
    const { selectedRows } = this.state;
    const copyList = _.cloneDeep(selectedRows);
    copyList.splice(index, 1);
    this.setState({
      selectedRows: copyList
    });
  }

  submitData = () => {
    const { selectedRows } = this.state;
    this.props.okModal(selectedRows);
  }

  content = (recordData) => {
    this.setState({
      visibleContent: true,
      recordData
    });
  }

  closeContent = () => {
    this.setState({
      visibleContent: false
    });
  }

  updateColumns = (columns) => {
    this.setState({
      columns
    });
  }

  render() {
    const {
      tableVisible,
      columns,
      list,
      selectedRows,
      pageNo,
      pageSize,
      visibleContent,
      recordData,
      menuDefault,
      filterParams
    } = this.state;
    const { visible, intl } = this.props;
    let scroll = {};
    if (list.length * 44.77 > 180) {
      scroll = {
        y: 180
      };
    }
    return (
      <Modal
        title={
          <div>
            <span>{intl.formatMessage(commonMessage.chooseContentTemplate)}</span>
            <Tooltip placement="bottomLeft" title={intl.formatMessage(commonMessage.chooseSingleModel)} arrowPointAtCenter>
              <Icon
                type="question-circle"
                style={{
                  marginLeft: 6,
                  cursor: 'pointer',
                  fontSize: 12
                }}
              />
            </Tooltip>
          </div>
        }
        destroyOnClose
        maskClosable={false}
        style={{ padding: 0 }}
        width={900}
        visible={visible}
        okText={intl.formatMessage(commonMessage.confirm)}
        onOk={this.submitData}
        onCancel={this.props.closeModal}
        bodyStyle={{
          padding: 0
        }}
      >
        <Layout className="modelLayout" style={{ height: '100%' }}>
          <Header className={selectedRows.length > 0 ? styles.tagContent : null}>
            {
              selectedRows.map((x, index) => {
                return (
                  <Tag closable onClose={(e) => { e.preventDefault(); this.closeTag(index); }}>
                    {`${x.protocol_name}(${x.code})`}
                  </Tag>
                );
              })
            }
          </Header>
          <Content>
            <Layout style={{ height: '100%' }}>
              <Sider width={140} className={styles.modelMenu}>
                {
                  menuDefault.length > 0 ? (
                    <Menu
                      mode="inline"
                      defaultSelectedKeys={menuDefault}
                      onSelect={this.menuSelect}
                      style={{ height: '100%', borderRight: 0 }}
                    >
                      {
                        this.state.menu.map((x) => {
                          return (
                            <Menu.Item key={x.key} value={x} onClick={this.menuChoose}>
                              {x.name}
                            </Menu.Item>
                          );
                        })
                      }
                    </Menu>
                  ) : null
                }
              </Sider>
              <Content className={styles.modelContent}>
                <div style={{ height: '100%' }}>
                  <div className={styles.screen}>
                    <span>{intl.formatMessage(commonMessage.chooseContentTemplate)}</span>
                    <Button
                      icon="plus"
                      type="link"
                      style={{ float: 'right' }}
                      disabled={!this.state.defaultOpen}
                      onClick={() => this.setModal1Visible(true)}
                    >
                      {intl.formatMessage(commonMessage.modelAdd)}
                    </Button>
                  </div>
                  <div style={{ height: 350 }}>
                    <SupTable
                      ref={(ref) => { this.table = ref; }}
                      filterParams={filterParams}
                      rowKey={(record) => record.id}
                      className={styles.tableBody}
                      columns={columns}
                      dataSource={list}
                      size="middle"
                      showSearchIcon={false}
                      showColumnsFilter={false}
                      showSelection={false}
                      updateColumns={this.updateColumns}
                      onSearch={this.handleTableChange}
                      scroll={scroll}
                      onDoubleClick={(re) => {
                        this.content(re);
                      }}
                      pagination={{
                        total: this.state.total,
                        current: pageNo,
                        pageSize
                      }}
                    />
                  </div>
                  <Modal
                    title={intl.formatMessage(commonMessage.modelAdd)}
                    destroyOnClose
                    maskClosable={false}
                    visible={tableVisible}
                    width={720}
                    okButtonProps={{ disabled: this.state.okButtonDisabled }}
                    okText={intl.formatMessage(commonMessage.confirm)}
                    onOk={this.submitAddModel}
                    onCancel={() => this.setModal1Visible(false)}
                  >
                    <div
                      style={{
                        height: 360,
                        overflowY: 'auto'
                      }}
                    >
                      <AddModel
                        status="add"
                        wrappedComponentRef={(node) => { this.addmodel = node; }}
                        record={{
                          noticeType: this.state.defaultOpen,
                          protocol: {
                            protocol: this.state.selectprotocol
                          }
                        }}
                        setModal1Visible={() => this.setModal1Visible(false)}
                        refreshTable={() => {
                          this.setState({
                            tableVisible: false
                          });
                          this.menuChoose({
                            key: this.state.selectMenu,
                            item: {
                              props: {
                                value: {
                                  protocol: this.state.selectprotocol
                                }
                              }
                            }
                          });
                        }}
                        renderButton={(value) => {
                          if (this.state.okButtonDisabled) {
                            this.setState({
                              okButtonDisabled: value
                            });
                          }
                        }}
                      />
                    </div>
                  </Modal>
                  {
                    visibleContent ? (
                      <ContentDetail visible={visibleContent} closeContent={this.closeContent} recordData={recordData} />
                    ) : null
                  }
                </div>
              </Content>
            </Layout>
          </Content>
        </Layout>
      </Modal>
    );
  }
}
