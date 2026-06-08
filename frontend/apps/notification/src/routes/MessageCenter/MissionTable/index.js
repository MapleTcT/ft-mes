import React from 'react';
import { Form, message } from 'sup-ui';
import moment from 'moment';
import { getMission, getTheme, getNotice } from 'root/services/messageCenter';
import { injectIntl } from 'react-intl';
import commonMessage from 'root/common/messages';
import SupTable from 'sup-rc-table';
import Detail from './detail';
import styles from './styles.less';

@injectIntl
@Form.create()
export default class MissionTable extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.state = {
      detailVisible: false,
      noticeTopicId: '',
      filteredInfo: {},
      receive: {},
      total: 50,
      pageNo: 1,
      pageSize: 20,
      data: [],
      date: [moment().startOf('day'), moment().endOf('day')],
      searchColumns: [],
      filterParams: {
        id: {
          url: '/inter-api/notification-admin/v1/notice/task/keyword',
          customParmas: this.customParmas,
          param: 'id',
          callback: (result) => {
            const data = [];
            result.data.list.forEach((item) => {
              data.push(item.id.toString());
            });
            return data;
          }
        },
        bsmodCode: {
          url: '/inter-api/notification-admin/v1/notice/task/keyword',
          customParmas: this.customParmas,
          param: 'bsmodCode',
          callback: (result) => {
            const data = [];
            result.data.list.forEach((item) => {
              data.push(item.bsmodCode);
            });
            return data;
          }
        },
        bsmodName: {
          url: '/inter-api/notification-admin/v1/notice/task/keyword',
          customParmas: this.customParmas,
          param: 'bsmodName',
          callback: (result) => {
            const data = [];
            result.data.list.forEach((item) => {
              data.push(item.bsmodName);
            });
            return data;
          }
        }
      },
      columns: [
        {
          title: intl.formatMessage(commonMessage.missionId),
          dataIndex: 'id',
          filterType: 'search',
          width: 200
        },
        {
          title: intl.formatMessage(commonMessage.senderCode),
          dataIndex: 'bsmodCode',
          filterType: 'search',
          width: 180
        },
        {
          title: intl.formatMessage(commonMessage.serviceName),
          dataIndex: 'bsmodName',
          filterType: 'search',
          width: 200
        },
        {
          title: intl.formatMessage(commonMessage.createTime),
          dataIndex: 'shardingTime',
          width: 200,
          render: (v) => {
            return moment(v).format('YYYY.MM.DD HH:mm:ss');
          }
        },
        {
          title: intl.formatMessage(commonMessage.notice),
          dataIndex: 'protocolNames',
          width: 200,
          // filterType: 'checkbox',
          // filterOptions: [],
          hide: true
        },
        {
          title: intl.formatMessage(commonMessage.operate),
          dataIndex: 'operation',
          type: 'operation',
          width: 100,
          fixed: true,
          render: (text, record) => (
            <a onClick={() => { this.detail(record); }}>
              {intl.formatMessage(commonMessage.check)}
            </a>
          )
        }
      ]
    };
  }

  async componentWillMount() {
    // this.renderHead(this.state.defaultColumns);
    const { intl } = this.props;
    const { pageNo, pageSize, date } = this.state;
    this.fiterColumn();
    const _self = this;
    const res = await getTheme({});
    const res1 = await getMission({
      pageNo,
      pageSize,
      startTime: date[0].valueOf(),
      endTime: date[1].valueOf()
    });
    const res2 = await getNotice();
    this.state.columns
      .find((item) => item.dataIndex === 'protocolNames')
      .filterOptions = res2.data.list.map((x) => {
        return {
          label: x.name,
          value: x.id
        };
      });
    this.setState({
      data: res1.data.list,
      total: res1.data.pagination.total,
      searchColumns: [
        {
          key: 'createTime',
          title: intl.formatMessage(commonMessage.createTime),
          type: 'date',
          dateType: 'rangeDateTime',
          span: 8,
          format: 'YYYY-MM-DD HH:mm:ss',
          defaultValue: [moment().startOf('day'), moment().endOf('day')],
          onPanelChange: (value, mode) => {
            console.log(value, mode);
          },
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
          key: 'theme',
          title: intl.formatMessage(commonMessage.theme),
          type: 'select',
          span: 4,
          defaultValue: undefined,
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
    if (this.state.noticeTopicId) {
      return `startTime=${startTIme}&endTime=${endTime}&noticeTopicId=${this.state.noticeTopicId}`;
    } else {
      return `startTime=${startTIme}&endTime=${endTime}`;
    }
    // return `startTime=${startTIme}&endTime=${endTime}&noticeTopicId=${this.state.noticeTopicId}`;
  }

  keywordCallback = (result) => {
    const data = [];
    result.forEach((item) => {
      data.push(item.valueZhCn);
    });
    return data;
  }

  initTable = () => {
    const { pageNo, pageSize, date, noticeTopicId } = this.state;
    getMission({
      pageNo,
      pageSize,
      noticeTopicId,
      startTime: date[0].valueOf(),
      endTime: date[1].valueOf()
    }).then((res) => {
      this.setState({
        data: res.data.list,
        total: res.data.pagination.total
      });
    });
  }

  detail = (item) => {
    this.setState({
      detailVisible: true,
      receive: item
    });
  }

  closeReceive = () => {
    this.setState({
      detailVisible: false
    });
  }

  handleTableChange = (params, type) => {
    const { filters, pagination, search } = params;
    const { current, pageSize } = pagination;
    const { intl } = this.props;
    if (type === 'search') {
      if (search.createTime.length === 0) {
        message.warning(intl.formatMessage(commonMessage.noCreateTime));
        return;
      }
      this.setState({
        filteredInfo: {},
        date: [moment(search.createTime[0]), moment(search.createTime[1])],
        noticeTopicId: search.theme,
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
      }, filters.userName && {
        receiverName: filters.userName
      }, filters.protocolNames && {
        noticeTypeIds: filters.protocolNames
      });
      this.refreshTable(searchObj);
    }
  };

  refreshTable = (search = {}) => {
    const { pageNo, pageSize, date, noticeTopicId } = this.state;
    getMission({
      pageNo,
      pageSize,
      noticeTopicId,
      startTime: date[0].valueOf(),
      endTime: date[1].valueOf(),
      ...search
    }).then((res) => {
      this.setState({
        data: res.data.list,
        total: res.data.pagination.total,
        pageSize: res.data.pagination.pageSize,
        pageNo: res.data.pagination.current
      });
    });
  }

  updateColumns = (columns) => {
    this.setState({
      columns
    });
  }

  render() {
    const {
      detailVisible,
      receive,
      columns,
      date,
      data,
      pageNo,
      pageSize,
      searchColumns,
      filterParams
    } = this.state;
    return (
      <div className={`${styles.tableBox} wrapTable`}>
        <SupTable
          ref={(ref) => { this.table = ref; }}
          searchColumns={searchColumns}
          rowKey={(record) => record.id}
          showSelection={false}
          columns={columns}
          dataSource={data}
          size="middle"
          onSearch={this.handleTableChange}
          showSearchIcon={false}
          updateColumns={this.updateColumns}
          filterParams={filterParams}
          onDoubleClick={(re) => {
            this.detail(re);
          }}
          pagination={{
            total: this.state.total,
            current: pageNo,
            pageSize
          }}
        />
        {
          detailVisible ? (
            <Detail
              receive={receive}
              visible={detailVisible}
              closeReceive={this.closeReceive}
              date={date}
            />
          ) : null
        }
      </div>
    );
  }
}
