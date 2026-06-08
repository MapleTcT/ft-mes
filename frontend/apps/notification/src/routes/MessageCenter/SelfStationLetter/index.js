import React from 'react';
import {
  Form,
  Button,
  message
} from 'sup-ui';
import moment from 'moment';
import { injectIntl } from 'react-intl';
import commonMessage from 'root/common/messages';
import SupTable from 'sup-rc-table';
import Content from 'root/components/Content';
import { getPersonalStation, readSome, readAll } from 'root/services/messageCenter';
import styles from './styles.less';

@injectIntl
@Form.create()
export default class Detail extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.state = {
      chooseMonth: null,
      selectedRowKeys: [],
      pageNo: 1,
      pageSize: 20,
      total: 50,
      visibleContent: false,
      recordData: {},
      data: [],
      date: [moment().startOf('month'), moment().endOf('month')],
      searchColumns: [
        {
          key: 'date',
          title: '发送时间',
          type: 'date',
          dateType: 'rangeDateTime',
          span: 6,
          format: 'YYYY-MM-DD HH:mm:ss',
          defaultValue: [moment().startOf('month'), moment().endOf('month')],
          disabledDate: (current) => {
            if (current && this.state.chooseMonth) {
              return this.state.chooseMonth !== current.get('month');
            }
            return false;
          },
          onChange: (onChange) => {
            if (!onChange.length) {
              this.setState({
                chooseMonth: null
              });
            }
          },
          onCalendarChange: (dates) => {
            this.setState({
              chooseMonth: dates[0].get('month')
            });
          }
        },
        {
          key: 'readStatus',
          title: '状态',
          type: 'select',
          span: 4,
          defaultValue: '',
          options: [{
            id: '0',
            name: '未读'
          }, {
            id: '1',
            name: '已读'
          }, {
            id: null,
            name: '全部'
          }].map((item) => {
            return {
              label: item.name,
              value: item.id
            };
          })
        }
      ],
      columns: [
        { title: '状态',
          dataIndex: 'readStatus',
          width: 150,
          render: (record) => {
            let tagStr = null;
            if (record === 1) {
              tagStr = '已读';
            } else {
              tagStr = '未读';
            }
            return tagStr;
          } },
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
          fixed: true,
          render: (text, record) => {
            return (
              <a
                onClick={() => { this.content(record); }}
              >
                详情
              </a>
            );
          }
        }
      ]
    };
  }

  async componentWillMount() {
    const { pageNo, pageSize } = this.state;
    const res = await getPersonalStation({
      startTime: moment().startOf('month').valueOf(),
      endTime: moment().endOf('month').valueOf(),
      pageNo,
      pageSize
    });
    this.setState({
      data: res.data.list,
      total: res.data.pagination.total,
      pageNo: res.data.pagination.current,
      pageSize: res.data.pagination.pageSize
    });
    // this.setState({
    //   data: [
    //     {
    //       id: 1042481747853311,
    //       shardingTime: 1596422209959,
    //       readStatus: '0'
    //     },
    //     {
    //       id: 1042481747853312,
    //       shardingTime: 1596422209959,
    //       readStatus: '1'
    //     }
    //   ]
    // });
  }

  componentDidMount() {
    if (this.table && this.table.changeOperationFixedStatus) {
      this.table.changeOperationFixedStatus();
    }
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
    getPersonalStation({
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

  handleTableChange = (params) => {
    const { pagination, search } = params;
    const { current, pageSize } = pagination;
    this.setState({
      date: [moment(search.date[0]), moment(search.date[1])],
      readStatus: search.readStatus
    });
    const searcha = {
      startTime: moment(search.date[0]).valueOf(),
      endTime: moment(search.date[1]).valueOf(),
      readStatus: search.readStatus,
      pageNo: current,
      pageSize
    };
    this.refreshTable(searcha);
  };

  updateColumns = (columns) => {
    this.setState({
      columns
    });
  }

  onSelectItem = (selectedRowKeys) => {
    this.setState({
      selectedRowKeys
    });
  }

  someRead = () => {
    const { date, selectedRowKeys, readStatus, pageSize, pageNo } = this.state;
    readSome({
      messageIds: selectedRowKeys,
      startTime: date[0].valueOf(),
      endTime: date[1].valueOf()
    }).then(() => {
      message.success('状态更新成功');
      window.top.postMessage('unreadnum');
      this.refreshTable({
        startTime: date[0].valueOf(),
        endTime: date[1].valueOf(),
        readStatus,
        pageNo,
        pageSize
      });
    });
  }

  allRead = () => {
    const { date, readStatus, pageSize, pageNo } = this.state;
    readAll({
      startTime: date[0].valueOf(),
      endTime: date[1].valueOf()
    }).then(() => {
      message.success('状态更新成功');
      window.top.postMessage('unreadnum');
      this.refreshTable({
        startTime: date[0].valueOf(),
        endTime: date[1].valueOf(),
        readStatus,
        pageNo,
        pageSize
      });
    });
  }

  render() {
    const { columns, data, pageSize, pageNo, visibleContent, recordData, searchColumns, selectedRowKeys } = this.state;
    return (
      <div className={styles.selfTable}>
        <div className={styles.operate}>
          <Button
            disabled={selectedRowKeys.length === 0}
            onClick={this.someRead}
            style={{ marginRight: 12 }}
          >
            标记已读
          </Button>
          <Button onClick={this.allRead} type="primary">全部已读</Button>
        </div>
        <SupTable
          ref={(ref) => { this.table = ref; }}
          onSelectItem={this.onSelectItem}
          getCheckboxProps={(record) => {
            return {
              disabled: record.status === '1'
            };
          }}
          searchColumns={searchColumns}
          searchBtnPosition="normal"
          rowKey={(record) => record.id}
          columns={columns}
          dataSource={data}
          size="middle"
          onSearch={this.handleTableChange}
          showSearchIcon={false}
          showColumnsFilter={false}
          updateColumns={this.updateColumns}
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
