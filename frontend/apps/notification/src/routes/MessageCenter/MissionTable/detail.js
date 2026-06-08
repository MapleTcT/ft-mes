import React from 'react';
import {
  Form,
  Modal,
  Tag,
  message
} from 'sup-ui';
import moment from 'moment';
import { injectIntl } from 'react-intl';
import commonMessage from 'root/common/messages';
import SupTable from 'sup-rc-table';
import Content from 'root/components/Content';
import { getReceive, getNotice } from 'root/services/messageCenter';
import styles from './styles.less';

@injectIntl
@Form.create()
export default class Detail extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.state = {
      pageNo: 1,
      pageSize: 20,
      total: 0,
      visibleContent: false,
      data: [],
      recordData: {},
      searchColumns: [],
      filteredInfo: {},
      filterParams: {
        staffName: {
          url: '/inter-api/notification-admin/v1/notice/message/keyword',
          customParmas: this.customParmas,
          param: 'staffName',
          callback: (result) => {
            const data = [];
            result.data.list.forEach((item) => {
              data.push(item.staffName);
            });
            return data;
          }
        }
      },
      columns: [
        {
          title: intl.formatMessage(commonMessage.receiver),
          dataIndex: 'staffName',
          filterType: 'search',
          width: 120
        },
        { title: intl.formatMessage(commonMessage.sendStatus),
          dataIndex: 'sendStatus',
          filterType: 'checkbox',
          width: 120,
          filterOptions: [
            { label: intl.formatMessage(commonMessage.unknow), value: '2' },
            { label: intl.formatMessage(commonMessage.success), value: '1' },
            { label: intl.formatMessage(commonMessage.fail), value: '0' }
          ],
          render: (record, obj) => {
            let tagStr = null;
            if (record === 2) {
              tagStr = (<Tag color="default" title={intl.formatMessage(commonMessage.unknow)}>{intl.formatMessage(commonMessage.unknow)}</Tag>);
            } else if (record === 1) {
              tagStr = (<Tag color="green" title={intl.formatMessage(commonMessage.success)}>{intl.formatMessage(commonMessage.success)}</Tag>);
            } else {
              tagStr = (<Tag color="red" title={obj.errorResult}>{intl.formatMessage(commonMessage.fail)}</Tag>);
            }
            return tagStr;
          } },
        // {
        //   title: intl.formatMessage(commonMessage.result),
        //   dataIndex: 'errorResult'
        // },
        {
          title: intl.formatMessage(commonMessage.readStatus),
          dataIndex: 'readStatus',
          width: 120,
          filterType: 'checkbox',
          filterOptions: [
            { label: intl.formatMessage(commonMessage.unknow), value: '2' },
            { label: intl.formatMessage(commonMessage.read), value: '1' },
            { label: intl.formatMessage(commonMessage.unread), value: '0' }
          ],
          render: (record) => {
            let text = '';
            if (record === 1) {
              text = intl.formatMessage(commonMessage.read);
            } else if (record === 0) {
              text = intl.formatMessage(commonMessage.unread);
            } else {
              text = intl.formatMessage(commonMessage.unknow);
            }
            return <span title={text}>{text}</span>;
          }
        },
        {
          title: intl.formatMessage(commonMessage.sendTime),
          dataIndex: 'shardingTime',
          width: 180,
          render: (v) => {
            const text = moment(v).format('YYYY.MM.DD HH:mm:ss');
            return <span title={text}>{text}</span>;
          }
        },
        {
          title: intl.formatMessage(commonMessage.operate),
          dataIndex: 'operation',
          type: 'operation',
          width: 80,
          fixed: false,
          render: (text, record) => {
            return (
              <a
                onClick={() => { this.content(record); }}
              >
                {intl.formatMessage(commonMessage.content)}
              </a>
            );
          }
        }
      ]
    };
  }

  async componentWillMount() {
    const { receive, date, intl } = this.props;
    const { pageNo, pageSize } = this.state;
    const { id } = receive;
    this.fiterColumn();
    const notices = await getNotice();
    const defaultId = _.get(notices, 'data.list[0].id', '');
    const retData = await getReceive({
      noticeTaskId: id,
      startTime: date[0].valueOf(),
      endTime: date[1].valueOf(),
      noticeProtocolId: defaultId,
      pageNo,
      pageSize
    });
    this.setState({
      data: retData.data.list,
      total: retData.data.pagination.total,
      chooseNotice: defaultId,
      searchColumns: [
        {
          key: 'noticeProtocolId',
          title: intl.formatMessage(commonMessage.notice),
          type: 'select',
          span: 6,
          defaultValue: defaultId,
          options: notices.data.list.map((item) => {
            return {
              label: item.name,
              value: item.id
            };
          })
        }
      ]
    });
  }

  componentDidMount() {
    if (this.table && this.table.changeOperationFixedStatus) {
      this.table.changeOperationFixedStatus();
    }
  }

  fiterColumn = () => {
    const { columns } = this.state;
    columns.forEach((item) => {
      item.filteredValue = this.state.filteredInfo[item.dataIndex] || [];
    });
  }

  customParmas = () => {
    const startTIme = this.props.date[0].valueOf();
    const endTime = this.props.date[1].valueOf();
    return `startTime=${startTIme}&endTime=${endTime}&noticeProtocolId=${this.state.chooseNotice}`;
  }

  content = (record) => {
    this.setState({
      visibleContent: true,
      recordData: record
    });
  }

  closeContent = () => {
    this.setState({
      visibleContent: false
    });
  }

  refreshTable = (search = {}) => {
    const { pageNo, pageSize } = this.state;
    const { receive } = this.props;
    getReceive({
      pageNo,
      pageSize,
      ...search,
      noticeTaskId: receive.id
    }).then((res) => {
      this.setState({
        data: res.data.list,
        total: res.data.pagination.total,
        pageSize: res.data.pagination.pageSize,
        pageNo: res.data.pagination.current
      });
    });
  }

  handleTableChange = (params, type) => {
    const { date, intl } = this.props;
    const { pagination, filters, search } = params;
    const { current, pageSize } = pagination;
    if (type === 'clear') {
      return;
    }
    if (date.length === 0) {
      message.warning(intl.formatMessage(commonMessage.noCreateTime));
      return;
    }
    if (!search.noticeProtocolId) {
      message.warning(intl.formatMessage(commonMessage.noNotice));
      return;
    }
    const useFilter = type === 'search' ? {} : _.cloneDeep(filters);
    const pageNo = type === 'search' ? 1 : current;
    this.setState({
      filteredInfo: useFilter,
      chooseNotice: search.noticeProtocolId
    }, () => {
      this.fiterColumn();
    });
    const searcha = Object.assign(
      useFilter,
      search,
      {
        startTime: date[0].valueOf(),
        endTime: date[1].valueOf(),
        pageNo,
        pageSize
      }
    );
    this.refreshTable(searcha);
  };

  updateColumns = (columns) => {
    this.setState({
      columns
    });
  }

  render() {
    const { visible, closeReceive } = this.props;
    const { columns, data, pageSize, pageNo, visibleContent, recordData, searchColumns, filterParams } = this.state;
    // let scroll = {
    //   y: null,
    //   x: null
    // };
    // if (data.length * 44 > 400) {
    //   scroll = {
    //     y: 400,
    //     x: null
    //   };
    // }
    return (
      <Modal
        title={this.props.intl.formatMessage(commonMessage.receiveInfo)}
        destroyOnClose
        visible={visible}
        width={1200}
        onCancel={() => { closeReceive(); }}
        footer={null}
        maskClosable={false}
        bodyStyle={{
          padding: 0
        }}
      >
        <div className={`${styles.tableBox} wrapTable`}>
          <SupTable
            ref={(ref) => { this.table = ref; }}
            searchColumns={searchColumns}
            searchBtnPosition="normal"
            filterParams={filterParams}
            rowKey={(record) => record.id}
            showSelection={false}
            columns={columns}
            dataSource={data}
            size="middle"
            onSearch={this.handleTableChange}
            showSearchIcon={false}
            showColumnsFilter={false}
            updateColumns={this.updateColumns}
            style={{
              height: 500
            }}
            onDoubleClick={(re) => {
              this.content(re);
            }}
            pagination={{
              total: this.state.total,
              current: pageNo,
              pageSize
            }}
          />
          {
            visibleContent ? (
              <Content visible={visibleContent} closeContent={this.closeContent} recordData={recordData} />
            ) : null
          }
        </div>
      </Modal>
    );
  }
}
