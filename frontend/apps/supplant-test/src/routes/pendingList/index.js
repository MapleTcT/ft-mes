import React from 'react';
import { injectIntl } from 'react-intl';
import SupTable from 'sup-rc-table';
import { message } from 'sup-ui';
import { getPendingList } from '../../services/pending.js';
import SearchBar from './SearchBar.js';
import { getQueryString } from '../../utils/index.js';
import defaultMessages from './messages.js';
import './index.less';

// const pendingData = [{ flowName: '流程名', activityName: '活动名', number: 1.,linkUrl:'' }];
// 支持查询条件按活动/流程，时间，类型
@injectIntl
export default class PendingList extends React.PureComponent {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.state = {
      columns: [
        {
          width: 60,
          title: intl.formatMessage(defaultMessages.rowIndex),
          dataIndex: 'rowIndex',
          key: 'rowIndex'
        },
        {
          width: 350,
          title: intl.formatMessage(defaultMessages.processName),
          dataIndex: 'processName',
          key: 'processName'
        },
        {
          width: 350,
          title: intl.formatMessage(defaultMessages.taskName),
          dataIndex: 'taskName',
          key: 'taskName'
        },
        {
          width: 200,
          title: intl.formatMessage(defaultMessages.count),
          dataIndex: 'count',
          key: 'count'
        }
      ],
      dataSource: [],
      currentPage: 1,
      pageSize: 20,
      searchParam: this.getDftParam()
    };
  }

  componentWillMount() {
    this.fetchData();
  }

  fetchData() {
    const { currentPage, pageSize, searchParam } = this.state;
    const param = {
      current: currentPage,
      size: pageSize,
      ...searchParam
    };
    if (param.taskType === 'all') delete param.taskType;
    if (param.timeType === 'all') delete param.timeType;
    getPendingList(param).then((res) => {
      if (res.data) {
        this.setState({
          dataSource: this.fmtData(res.data.data.list),
          total: (res.data.data.pagination || {}).total
        });
      }
    });
  }

  // 获取初始值
  getDftParam = () => {
    const keyMap = { today: 'TODAY', week: 'SEVEN_DAYS', total: 'all' };
    const time = getQueryString('time');
    const timeType = time ? keyMap[time] : 'all';
    return { category: 'task', taskType: 'all', timeType };
  };

  fmtData = (data) => {
    return data.map((item, index) => {
      return { rowIndex: index + 1, ...item };
    });
  };

  handleSearch = (param) => {
    this.setState({ searchParam: param, currentPage: 1 }, () =>
      this.fetchData()
    );
  };

  getColumns = () => {
    const { columns, searchParam } = this.state;
    if (searchParam.category === 'process') {
      return columns.filter((item) => item.key !== 'taskName');
    }
    return columns;
  };

  onSearch = (param, type) => {
    if (type === 'pagination') {
      const { pageSize, current } = param.pagination || {};
      this.setState(
        {
          pageSize,
          currentPage: current
        },
        () => this.fetchData()
      );
    }
  };

  handleClick = (rowData) => {
    const { intl } = this.props;
    const { url, menuCode, menuName } = rowData;
    if (!url) {
      message.error(intl.formatMessage(defaultMessages.noFlow));
      return;
    }
    try {
      window.parent.CUI.loadPage({
        menuName,
        code: menuCode,
        url,
        root: 'system',
        target: 'SELF'
      });
    } catch (e) {
      window.open(url);
    }
  };

  render() {
    const {
      dataSource,
      total,
      currentPage,
      pageSize,
      searchParam
    } = this.state;
    return (
      <div className="sup-pendingList-wrap">
        <div className="pl-header">
          <SearchBar onSearch={this.handleSearch} value={searchParam} />
        </div>
        <div className="pl-content">
          <SupTable
            rowKey={(item) => item.rowIndex}
            dataSource={dataSource}
            columns={this.getColumns()}
            controlColumns={this.getColumns()}
            updateColumns={(update) => this.setState({ columns: update })}
            showSearch={false}
            showColumnsFilter={false}
            showSelection={false}
            pagination={{
              total,
              current: currentPage,
              pageSize
            }}
            onSearch={this.onSearch}
            onRowClick={this.handleClick}
            ref={(dom) => {
              this.tableRef = dom;
            }}
          />
        </div>
      </div>
    );
  }
}
