import React from 'react';
import {
  Form,
  message,
  Tag
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
export default class ReceiveTable extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.state = {
      pageNo: 1,
      pageSize: 20,
      chooseNotice: '',
      visibleContent: false,
      recordData: {},
      filteredInfo: {},
      date: [moment().startOf('month'), moment().endOf('month')],
      // chooseMonth: null,
      total: 50,
      searchColumns: [],
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
        },
        noticeTaskId: {
          url: '/inter-api/notification-admin/v1/notice/message/keyword',
          customParmas: this.customParmas,
          param: 'noticeTaskId',
          callback: (result) => {
            const data = [];
            result.data.list.forEach((item) => {
              data.push(item.noticeTaskId.toString());
            });
            return data;
          }
        }
      },
      columns: [
        { title: intl.formatMessage(commonMessage.receiver),
          dataIndex: 'staffName',
          filterType: 'search',
          width: 120
        },
        {
          title: intl.formatMessage(commonMessage.missionId),
          dataIndex: 'noticeTaskId',
          filterType: 'search',
          width: 180
        },
        {
          title: intl.formatMessage(commonMessage.sendStatus),
          dataIndex: 'sendStatus',
          filterType: 'checkbox',
          width: 120,
          filterOptions: [
            { label: intl.formatMessage(commonMessage.unknow), value: 2 },
            { label: intl.formatMessage(commonMessage.success), value: 1 },
            { label: intl.formatMessage(commonMessage.fail), value: 0 }
          ],
          render: (record, obj) => {
            let tagStr = null;
            if (record === 2) {
              tagStr = (<Tag title={intl.formatMessage(commonMessage.unknow)}>{intl.formatMessage(commonMessage.unknow)}</Tag>);
            } else if (record === 1) {
              tagStr = (<Tag color="green" title={intl.formatMessage(commonMessage.success)}>{intl.formatMessage(commonMessage.success)}</Tag>);
            } else {
              tagStr = (<Tag color="red" title={obj.errorResult}>{intl.formatMessage(commonMessage.fail)}</Tag>);
            }
            return tagStr;
          } },
        // {
        //   title: intl.formatMessage(commonMessage.result),
        //   dataIndex: 'errorResult',
        //   render: (text) => {
        //     return <span title={text} className={styles.errorResult}>{text}</span>;
        //   }
        // },
        {
          title: intl.formatMessage(commonMessage.readStatus),
          dataIndex: 'readStatus',
          width: 120,
          filterType: 'checkbox',
          filterOptions: [
            { label: intl.formatMessage(commonMessage.read), value: 1 },
            { label: intl.formatMessage(commonMessage.unread), value: 0 },
            { label: intl.formatMessage(commonMessage.unknow), value: 2 }
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
          width: 100,
          fixed: true,
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
    const { pageNo, pageSize, date } = this.state;
    const { intl } = this.props;
    const _self = this;
    this.fiterColumn();
    const res = await getNotice();
    const retData = await getReceive({
      startTime: date[0].valueOf(),
      endTime: date[1].valueOf(),
      pageNo,
      pageSize,
      noticeProtocolId: _.get(res, 'data.list[0].id', '')
    });
    this.setState({
      data: retData.data.list,
      total: retData.data.pagination.total,
      chooseNotice: _.get(res, 'data.list[0].id', ''),
      searchColumns: [
        {
          key: 'createTime',
          title: intl.formatMessage(commonMessage.createTime),
          type: 'date',
          dateType: 'rangeDateTime',
          span: 8,
          format: 'YYYY-MM-DD HH:mm:ss',
          defaultValue: [moment().startOf('month'), moment().endOf('month')],
          disabledDate: (current) => {
            if (current && _self.state.chooseMonth) {
              return _self.state.chooseMonth !== current.get('month');
            }
            return false;
          },
          onChange: () => {
            _self.setState({
              chooseMonth: null
            });
          },
          onCalendarChange: (dates) => {
            _self.setState({
              chooseMonth: dates[0].get('month')
            });
          }
        },
        {
          key: 'protocolId',
          title: intl.formatMessage(commonMessage.notice),
          type: 'select',
          span: 4,
          defaultValue: _.get(res, 'data.list[0].id', ''),
          options: res.data.list.map((item) => {
            return { label: item.name, value: item.id };
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
    const startTIme = this.state.date[0].valueOf();
    const endTime = this.state.date[1].valueOf();
    return `startTime=${startTIme}&endTime=${endTime}&noticeProtocolId=${this.state.chooseNotice}`;
  }

  content = (record) => {
    this.setState({
      visibleContent: true,
      recordData: record
    });
  }

  refreshTable = (search = {}) => {
    const { pageNo, pageSize, date, chooseNotice } = this.state;
    getReceive({
      pageNo,
      pageSize,
      startTime: date[0].valueOf(),
      endTime: date[1].valueOf(),
      noticeProtocolId: chooseNotice,
      ...search
    }).then((res) => {
      this.setState({
        data: res.data.list,
        total: res.data.pagination.total,
        pageNo: res.data.pagination.current,
        pageSize: res.data.pagination.pageSize
      });
    });
  }

  handleTableChange = (params, type) => {
    const { pagination, filters, search } = params;
    const { current, pageSize } = pagination;
    const { intl } = this.props;
    if (type === 'search') {
      if (search.createTime.length === 0) {
        message.warning(intl.formatMessage(commonMessage.noCreateTime));
        return;
      }
      if (!search.protocolId) {
        message.warning(intl.formatMessage(commonMessage.noNotice));
        return;
      }
      this.setState({
        filteredInfo: {},
        date: [moment(search.createTime[0]), moment(search.createTime[1])],
        chooseNotice: search.protocolId,
        pageSize,
        pageNo: 1
      }, () => {
        this.fiterColumn();
        this.refreshTable();
      });
    } else if (type === 'filters' || type === 'pagination') {
      this.setState({
        filteredInfo: filters
      }, () => {
        this.fiterColumn();
      });
      const searchObj = Object.assign(filters, {
        pageNo: current,
        pageSize
      });
      this.refreshTable(searchObj);
    }
  };

  updateColumns = (columns) => {
    this.setState({
      columns
    });
  }

  closeContent = () => {
    this.setState({
      visibleContent: false
    });
  }

  render() {
    const { columns, data, pageNo, pageSize, visibleContent, recordData, searchColumns, filterParams } = this.state;
    return (
      <div className={`${styles.tableBox} wrapTable`}>
        <SupTable
          ref={(ref) => { this.table = ref; }}
          filterParams={filterParams}
          searchColumns={searchColumns}
          rowKey={(record) => record.id}
          showSelection={false}
          className={styles.tableBody}
          columns={columns}
          dataSource={data}
          size="middle"
          onSearch={this.handleTableChange}
          updateColumns={this.updateColumns}
          showSearchIcon={false}
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
    );
  }
}
