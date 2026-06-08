import React from 'react';
import { Button, message, Modal } from 'sup-ui';
import SupTable from 'sup-rc-table';
import { injectIntl } from 'react-intl';
import commonMessage from 'root/common/messages';
import mainStyle from 'root/routes/MessageCenter/styles.less';
import { getTheme, getNotice, delTheme } from 'root/services/messageCenter';
import ThemeForm from 'root/components/ThemeForm';
import SelfModal from 'root/components/SelfModal';
import styles from './styles.less';

const confirmModal = Modal.confirm;
@injectIntl
export default class ThemeTable extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.state = {
      tableVisible: false,
      isExit: false,
      okButtonDisabled: true,
      title: '',
      total: 50,
      pageNo: 1,
      pageSize: 20,
      list: [],
      selectedRows: [],
      filterValue: {},
      filterParams: {
        code: {
          url: '/inter-api/notification-admin/v1/notice/topic/keyword',
          param: 'code',
          customParmas: this.customParmas,
          callback: (result) => {
            const data = [];
            result.data.list.forEach((item) => {
              data.push(item.code);
            });
            return data;
          }
        },
        name: {
          url: '/inter-api/notification-admin/v1/notice/topic/keyword',
          param: 'name',
          customParmas: this.customParmas,
          callback: (result) => {
            const data = [];
            result.data.list.forEach((item) => {
              data.push(item.name);
            });
            return data;
          }
        },
        templateName: {
          url: '/inter-api/notification-admin/v1/notice/topic/keyword',
          param: 'templateName',
          customParmas: this.customParmas,
          callback: (result) => {
            const data = [];
            result.data.list.forEach((item) => {
              data.push(item.templateName);
            });
            return data;
          }
        }
      },
      btnColumns: [
        {
          key: 'add',
          content: intl.formatMessage(commonMessage.add),
          callback: () => {
            return this.setModal1Visible(true);
          }
        },
        {
          key: 'delete',
          disabled: () => {
            return this.state.selectedRows.length === 0;
          },
          callback: () => {
            if (this.state.selectedRows.length === 0) {
              return;
            }
            this.delete(null);
          }
        }
      ],
      columns: [
        {
          title: intl.formatMessage(commonMessage.themeName),
          dataIndex: 'name',
          filterType: 'search',
          width: 200
        },
        {
          title: intl.formatMessage(commonMessage.themeCode),
          dataIndex: 'code',
          filterType: 'search',
          width: 200
        },
        {
          title: intl.formatMessage(commonMessage.notice),
          dataIndex: 'protocolName',
          filterType: 'checkbox',
          filterOptions: [],
          width: 120
        },
        {
          title: intl.formatMessage(commonMessage.modelName),
          dataIndex: 'templateName',
          width: 250,
          filterType: 'search'
        },
        {
          title: intl.formatMessage(commonMessage.receiver),
          dataIndex: 'receiver',
          width: 120
        },
        {
          title: intl.formatMessage(commonMessage.operate),
          dataIndex: 'operation',
          type: 'operation',
          width: 150,
          fixed: true,
          render: (text, record) => (
            <span>
              <a onClick={() => { this.edit(record); }}>
                {intl.formatMessage(commonMessage.edit)}
              </a>
              <span className={styles.operateUnit}>|</span>
              <a onClick={() => { this.delete(record); }}>
                {intl.formatMessage(commonMessage.delete)}
              </a>
            </span>
          )
        }
      ]
    };
  }

  componentWillMount() {
    const { id } = this.props;
    const { pageNo, pageSize } = this.state;
    Promise.all([getTheme({ topicTreeId: id, pageNo, pageSize }), getNotice()]).then((res) => {
      this.props.dataHas(res[0].data.list);
      this.state.columns
        .find((item) => item.dataIndex === 'protocolName')
        .filterOptions = res[1].data.list.map((x) => {
          return {
            label: x.name,
            value: x.id
          };
        });
      this.setState({
        isExit: res[0].data.list.length > 0,
        list: res[0].data.list,
        total: res[0].data.pagination.total
      });
    });
  }

  componentDidMount() {
    if (this.table && this.table.changeOperationFixedStatus) {
      this.table.changeOperationFixedStatus();
    }
  }

  componentWillReceiveProps(nextProps) {
    const { pageNo, pageSize } = this.state;
    getTheme({ topicTreeId: nextProps.id, pageNo, pageSize }).then((res) => {
      this.setState({
        isExit: res.data.list.length > 0
      }, () => {
        this.checkCallBack(res);
      });
    });
  }

  customParmas = () => {
    return `topicTreeId=${this.props.id}`;
  }

  edit = (record) => {
    this.setModal1Visible(true, record, this.props.intl.formatMessage(commonMessage.themeEdit));
  }

  delete = (record = null) => {
    const { selectedRows, list, pageNo } = this.state;
    const { intl } = this.props;
    const _self = this;
    confirmModal({
      title: record
        ? intl.formatMessage(commonMessage.deleteSome, { name: `${record.id}${record.name}` })
        : intl.formatMessage(commonMessage.deleteAll),
      content: intl.formatMessage(commonMessage.deleteConfirm),
      okText: intl.formatMessage(commonMessage.confirm),
      onOk() {
        // message.error('数据被引用，不可删除！');
        let delList = [];
        if (record) {
          delList = [record.id];
        } else {
          delList = selectedRows.map((x) => {
            return x.id;
          });
        }
        delTheme(delList).then(() => {
          message.success(intl.formatMessage(commonMessage.deleteData));
          if (list.length === delList.length && pageNo !== 1) {
            _self.setState({
              pageNo: pageNo - 1
            }, () => {
              _self.refreshTable();
            });
          } else {
            _self.refreshTable();
          }
        });
      },
      onCancel() {
      }
    });
  }

  refreshTable = (search = {}, add) => {
    const { id } = this.props;
    const { pageNo, pageSize, filterValue } = this.state;
    getTheme({ topicTreeId: id, pageNo, pageSize, ...filterValue, ...search }).then((res) => {
      this.checkCallBack(res, add);
    });
  }

  checkCallBack = (res, add) => {
    this.props.dataHas(res.data.list);
    const st = {
      list: res.data.list,
      total: res.data.pagination.total,
      pageNo: res.data.pagination.current,
      pageSize: res.data.pagination.pageSize
    };
    if (add) {
      st.isExit = res.data.list.length > 0;
    }
    this.setState(st);
  }

  setModal1Visible = (visible, record = {}, title = this.props.intl.formatMessage(commonMessage.themeAdd)) => {
    this.setState({
      okButtonDisabled: true,
      tableVisible: visible,
      record,
      title
    });
  }

  submitAddModel = (e) => {
    // this.setState({
    //   tableVisible: false
    // });
    this.addmodel.handleSubmit(e);
  }

  renderNone = () => {
    const { intl } = this.props;
    return (
      <div className={mainStyle.nomission}>
        <div className={styles.tipBox}>
          <i className={mainStyle.missionIcon} />
          <p className={mainStyle.tip1}>{intl.formatMessage(commonMessage.noTheme)}</p>
          <Button icon="plus" onClick={() => this.setModal1Visible(true)}>{intl.formatMessage(commonMessage.addNow)}</Button>
        </div>
      </div>
    );
  }

  handleTableChange = ({ pagination, filters }) => {
    const { current, pageSize } = pagination;
    let obj = {};
    if (filters.protocolName) {
      obj = {
        protocolName: '',
        noticeTypeIds: filters.protocolName
      };
    }
    this.setState({
      filterValue: {
        ...filters,
        ...obj
      }
    });
    const search = Object.assign(filters, {
      pageNo: current,
      pageSize
    }, obj);
    // delete filters.protocolName;
    this.refreshTable(search);
  };

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
      record,
      title,
      pageSize,
      pageNo,
      btnColumns,
      isExit,
      okButtonDisabled,
      filterParams
    } = this.state;
    return (
      <div className="contentBox" style={{ height: '100%', width: list.length > 0 ? 'auto' : '100%' }}>
        {
          isExit > 0
            ? (
              <div style={{ height: '100%' }}>
                <SupTable
                  ref={(ref) => { this.table = ref; }}
                  rowKey={(x) => x.code}
                  filterParams={filterParams}
                  onSelectItem={(selectedRowKeys, selectedRows) => {
                    this.setState({
                      selectedRows
                    });
                  }}
                  columns={columns}
                  dataSource={list}
                  size="middle"
                  showSearchIcon={false}
                  onSearch={this.handleTableChange}
                  btnColumns={btnColumns}
                  updateColumns={this.updateColumns}
                  onDoubleClick={(re) => {
                    this.edit(re);
                  }}
                  pagination={{
                    total: this.state.total,
                    current: pageNo,
                    pageSize
                  }}
                />
              </div>
            )
            : this.renderNone()
        }
        <SelfModal
          title={title}
          visible={tableVisible}
          okButtonDisabled={okButtonDisabled}
          onOk={this.submitAddModel}
          onCancel={() => this.setModal1Visible(false)}
        >
          <ThemeForm
            wrappedComponentRef={(node) => { this.addmodel = node; }}
            type={this.props.type}
            record={record}
            refreshTable={this.refreshTable}
            setModal1Visible={this.setModal1Visible}
            renderButton={(value) => {
              if (this.state.okButtonDisabled) {
                this.setState({
                  okButtonDisabled: value
                });
              }
            }}
          />
        </SelfModal>
      </div>
    );
  }
}
