import React from 'react';
import { message, Modal, Tooltip } from 'sup-ui';
import { injectIntl } from 'react-intl';
import commonMessage from 'root/common/messages';
import { getContent, delContent, getNotice } from 'root/services/messageCenter';
import AddModel from 'root/components/AddModel';
import SelfModal from 'root/components/SelfModal';
import SupTable from 'sup-rc-table';
import styles from './content.less';

const confirmModal = Modal.confirm;

@injectIntl
export default class Content extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.state = {
      tableVisible: false,
      okButtonDisabled: true,
      pageNo: 1,
      pageSize: 20,
      record: null,
      title: '',
      total: 50,
      selectedRows: [],
      filterValue: {},
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
          title: intl.formatMessage(commonMessage.modelCode),
          dataIndex: 'code',
          filterType: 'search',
          width: 150
        },
        {
          title: intl.formatMessage(commonMessage.modelName),
          dataIndex: 'name',
          filterType: 'search',
          width: 200
        },
        {
          title: intl.formatMessage(commonMessage.desc),
          dataIndex: 'memo',
          width: 300,
          render: (text) => {
            return (
              <Tooltip title={text} placement="topLeft">
                <div className={styles.overlength}>{text}</div>
              </Tooltip>
            );
          }
        },
        {
          title: intl.formatMessage(commonMessage.notice),
          dataIndex: 'protocol_name',
          filterType: 'checkbox',
          filterOptions: [],
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
              <a onClick={() => { this.edit(record); }}>{intl.formatMessage(commonMessage.edit)}</a>
              <span className={styles.operateUnit}>|</span>
              <a onClick={() => { this.delete(record); }}>{intl.formatMessage(commonMessage.delete)}</a>
            </span>
          )
        }
      ]
    };
  }

  componentWillMount() {
    Promise.all([getContent({ pageNo: 1, pageSize: 20 }), getNotice()]).then((res) => {
      this.state.columns.find((item) => item.dataIndex === 'protocol_name').filterOptions = res[1].data.list.map((x) => {
        return {
          label: x.name,
          value: x.id
        };
      });
      this.setState({
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

  edit = (record) => {
    this.setModal1Visible(true, record, this.props.intl.formatMessage(commonMessage.modelEdit));
  }

  delete = (record = null) => {
    const { intl } = this.props;
    const { selectedRows, pageNo, pageSize, filterValue, list } = this.state;
    const _self = this;
    confirmModal({
      title: record
        ? intl.formatMessage(commonMessage.deleteSome, { name: ` ${record.code} ${record.name} ` })
        : intl.formatMessage(commonMessage.deleteAll),
      content: intl.formatMessage(commonMessage.deleteConfirm),
      // icon: <Icon type="exclamation-circle" />,
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
        delContent(delList).then(() => {
          message.success(intl.formatMessage(commonMessage.deleteData));
          let nowPage;
          if (list.length === delList.length && pageNo !== 1) {
            nowPage = pageNo - 1;
          } else {
            nowPage = pageNo;
          }
          getContent({ pageNo: nowPage, pageSize, ...filterValue }).then((res) => {
            _self.setState({
              list: res.data.list,
              total: res.data.pagination.total
            });
          });
        }).catch((err) => {
          const errList = err.data.message.split(/\n/);
          if (errList.length === 1) {
            message.warning(err.data.message);
          } else {
            Modal.error({
              width: 500,
              title: intl.formatMessage(commonMessage.cannotDelete),
              content: (<pre style={{ whiteSpace: 'pre-wrap', maxHeight: 300, overflowY: 'auto' }}>{err.data.message}</pre>)
            });
          }
          // message.error(<pre>{err.data.message}</pre>);
        });
      },
      onCancel() {
      }
    });
  }

  refreshTable = (search = {}) => {
    const { pageNo, pageSize, filterValue } = this.state;
    getContent({ pageNo, pageSize, ...filterValue, ...search }).then((res) => {
      this.setState({
        list: res.data.list,
        pageSize: res.data.pagination.pageSize,
        pageNo: res.data.pagination.current,
        total: res.data.pagination.total
      });
    });
  }

  setModal1Visible = (visible, record = {}, title = this.props.intl.formatMessage(commonMessage.modelAdd)) => {
    this.setState({
      okButtonDisabled: true,
      tableVisible: visible,
      record,
      title
    });
  }

  submitAddModel = (e) => {
    this.addmodel.handleSubmit(e);
  }

  handleTableChange = ({ pagination, filters }) => {
    const { current, pageSize } = pagination;
    let obj = {};
    if (filters.protocol_name) {
      obj = {
        protocol_name: '',
        noticeTypeIds: filters.protocol_name
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
      okButtonDisabled,
      filterParams
    } = this.state;
    return (
      <div className="contentBox" style={{ height: '100%', width: '100%', position: 'absolute' }}>
        <SupTable
          ref={(ref) => { this.table = ref; }}
          rowKey={(x) => x.id}
          filterParams={filterParams}
          onSelectItem={(selectedRowKeys, selectedRows) => {
            this.setState({
              selectedRows
            });
          }}
          columns={columns}
          dataSource={list}
          size="middle"
          onSearch={this.handleTableChange}
          showSearchIcon={false}
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
        <SelfModal
          title={title}
          visible={tableVisible}
          okButtonDisabled={okButtonDisabled}
          onOk={this.submitAddModel}
          onCancel={() => this.setModal1Visible(false)}
        >
          <AddModel
            ref={(node) => { this.form = node; }}
            wrappedComponentRef={(node) => { this.addmodel = node; }}
            setModal1Visible={this.setModal1Visible}
            refreshTable={this.refreshTable}
            record={record}
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
