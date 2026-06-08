import React from 'react';
import { injectIntl } from 'react-intl';
import PropTypes from 'prop-types';
import moment from 'moment';
import SupTable from 'sup-rc-table';
import SearchBar from './SearchBar.js';
import { getMyProcess, getOpenURL } from '../../services/process.js';
import { getUrlConcat } from '../../utils/index.js';
import defaultMessages from './messages.js';

@injectIntl
class Panel extends React.PureComponent {
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
          width: 200,
          title: intl.formatMessage(defaultMessages.tableNo),
          dataIndex: 'tableNo',
          key: 'tableNo',
          render: (text, data) => (
            <a className="table-link" onClick={() => this.handleLink(data)}>
              {text}
            </a>
          )
        },
        {
          width: 200,
          title: intl.formatMessage(defaultMessages.summary),
          dataIndex: 'summary',
          key: 'summary'
        },
        {
          width: 150,
          title: intl.formatMessage(defaultMessages.flowName),
          dataIndex: 'name',
          key: 'name'
        },
        {
          width: 150,
          title: intl.formatMessage(defaultMessages.status),
          dataIndex: 'status',
          key: 'status'
        },
        {
          width: 150,
          title: intl.formatMessage(defaultMessages.sname),
          dataIndex: 'sname',
          key: 'sname'
        },
        {
          width: 150,
          title: intl.formatMessage(defaultMessages.dname),
          dataIndex: 'dname',
          key: 'dname'
        },
        {
          width: 150,
          title: intl.formatMessage(defaultMessages.createTime),
          dataIndex: 'createTime',
          key: 'createTime'
        }
      ],
      dataSource: [],
      currentPage: 1,
      pageSize: 20,
      queryParam: {}
    };
  }

  componentWillMount() {
    this.getMyProcess();
  }

  getMyProcess() {
    const { currentPage, pageSize, queryParam } = this.state;
    const { type } = this.props;
    const queryBody = {
      type,
      page: {
        pageNo: currentPage,
        pageSize
      },
      ...queryParam
    };
    getMyProcess(queryBody).then((res) => {
      if (res.data) {
        this.setState({
          dataSource: this.fmtData(res.data.data.result),
          total: res.data.data.totalCount
        });
      }
    });
  }

  fmtData = (data) => {
    const { type } = this.props;
    return data.map((item, index) => {
      const { createTime, taskDescription } = item;
      if (type === 'pending') item.status = taskDescription;
      item.createTime = moment(createTime).format('YYYY-MM-DD HH:mm:ss');
      item.rowIndex = index + 1;
      return item;
    });
  };

  handleLink = (data) => {
    const {
      url,
      tableInfoId,
      statusValue,
      targetTableName,
      targetEntityCode
    } = data;
    const { type } = this.props;
    const urlParam = getUrlConcat({
      tableInfoId,
      entityCode: targetEntityCode,
      status: statusValue,
      targetTablename: targetTableName
    });
    if (type === 'pending') {
      window.open(url);
    } else {
      getOpenURL(urlParam).then((res) => {
        if (res.data) {
          window.open(res.data.data);
        }
      });
    }
  };

  handleSearch = (param) => {
    this.setState({ queryParam: param, currentPage: 1 }, () =>
      this.getMyProcess()
    );
  };

  onSearch = (param, type) => {
    if (type === 'pagination') {
      const { pageSize, current } = param.pagination || {};
      this.setState(
        {
          pageSize,
          currentPage: current
        },
        () => this.getMyProcess()
      );
    }
  };

  render() {
    const { type } = this.props;
    const { dataSource, columns, total, currentPage, pageSize } = this.state;
    return (
      <>
        <div className="pl-header">
          <SearchBar onSearch={this.handleSearch} type={type} />
        </div>
        <div className="pl-content">
          <SupTable
            rowKey={(item) => item.id}
            dataSource={dataSource}
            columns={columns}
            showSearch={false}
            showColumnsFilter={false}
            showSelection={false}
            pagination={{
              total,
              current: currentPage,
              pageSize
            }}
            onSearch={this.onSearch}
            onDoubleClick={this.handleLink}
          />
        </div>
      </>
    );
  }
}

export default Panel;
Panel.defaultProps = { type: 'pending' };
Panel.propTypes = { type: PropTypes.string };
