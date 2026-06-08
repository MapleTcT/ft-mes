import React from 'react';
import { injectIntl } from 'react-intl';
import SupTable from 'sup-rc-table';
import {
  getAuthorityList,
  getLicenseByModule
} from '../../services/authorityList.js';
import defaultMessages from './messages.js';

@injectIntl
export default class AuthorityList extends React.PureComponent {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.state = {
      columns: [
        {
          width: 200,
          title: intl.formatMessage(defaultMessages.name),
          dataIndex: 'applicationName',
          key: 'applicationName'
        },
        {
          width: 200,
          title: intl.formatMessage(defaultMessages.type),
          dataIndex: 'applicationType',
          key: 'applicationType'
        },
        {
          width: 200,
          title: intl.formatMessage(defaultMessages.code),
          dataIndex: 'moduleCode',
          key: 'moduleCode'
        },
        {
          width: 300,
          title: intl.formatMessage(defaultMessages.status),
          dataIndex: 'description',
          key: 'description'
        },
        {
          width: 200,
          title: intl.formatMessage(defaultMessages.operation),
          dataIndex: 'operation',
          key: 'operation',
          type: 'operation',
          fixed: false,
          render: (_, record, index) => (
            <a
              style={{ textDecoration: 'underline' }}
              // eslint-disable-next-line no-script-url
              href="javascript:;"
              onClick={() => this.refresh(record, index)}
            >
              {intl.formatMessage(defaultMessages.refresh)}
            </a>
          )
        }
      ],
      dataSource: [],
      currentPage: 1,
      pageSize: 20
    };
  }

  componentWillMount() {
    this.getAuthorityList();
  }

  getAuthorityList() {
    const { currentPage, pageSize } = this.state;
    getAuthorityList({ current: currentPage, size: pageSize }).then((res) => {
      if (res.data) {
        this.setState({
          dataSource: res.data.list,
          total: (res.data.pagination || {}).total
        });
      }
    });
  }

  refresh = (record, index) => {
    getLicenseByModule(record.moduleCode).then((res) => {
      if (res.data) {
        const { dataSource } = this.state;
        const prevData = [...dataSource];
        prevData[index] = res.data.data;
        this.setState({
          dataSource: prevData
        });
      }
    });
  };

  onSearch = (param, type) => {
    if (type === 'pagination') {
      const { pageSize, current } = param.pagination || {};
      this.setState(
        {
          pageSize,
          currentPage: current
        },
        () => this.getAuthorityList()
      );
    }
  };

  render() {
    const { dataSource, columns, total, currentPage, pageSize } = this.state;
    return (
      <SupTable
        rowKey={(item) => item.moduleCode}
        dataSource={dataSource}
        columns={columns}
        showSearch={false}
        showColumnsFilter={false}
        // showSelection={false}
        pagination={{
          total,
          current: currentPage,
          pageSize
        }}
        onSearch={this.onSearch}
      />
    );
  }
}
