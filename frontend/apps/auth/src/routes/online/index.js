import React, { Component } from 'react';
import { injectIntl } from 'react-intl';
import moment from 'moment';
import { Layout, message, Modal } from 'sup-ui';
import SupTable from 'sup-rc-table';
import * as commonApi from '../../services/commonApi';
import * as onlineUsersApi from '../../services/onlineApi';
import messages from './messages.js';
import styles from './index.less';

const { Header } = Layout;
const { confirm } = Modal;

@injectIntl
class OnlineManage extends Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    moment.defaultFormat = 'YYYY-MM-DDTHH:mm:ss.SSSZZ';

    this.searchColumns = [
      {
        key: 'userName',
        title: intl.formatMessage(messages.userName),
        type: 'input',
        span: 8
      },
      {
        key: 'dateRange',
        title: intl.formatMessage(messages.loginTime),
        type: 'date',
        dateType: 'rangeDateTime',
        format: 'YYYY-MM-DD HH:mm:ss',
        span: 8
      }
    ];

    const columns = [
      {
        title: intl.formatMessage(messages.no),
        dataIndex: 'no',
        width: 60
      },
      {
        title: intl.formatMessage(messages.userName),
        dataIndex: 'userName',
        width: 150
      },
      {
        title: intl.formatMessage(messages.name),
        dataIndex: 'personName',
        width: 150
      },
      {
        title: intl.formatMessage(messages.staffCode),
        dataIndex: 'personCode',
        width: 150
      },
      {
        title: intl.formatMessage(messages.ip),
        dataIndex: 'loginIp',
        width: 200
      },
      {
        title: intl.formatMessage(messages.loginTime),
        dataIndex: 'loginTime',
        width: 200,
        render: (text) => {
          return (
            <span>{text ? moment(text).format('YYYY-MM-DD HH:mm:ss') : ''}</span>
          );
        }
      },
      {
        title: intl.formatMessage(messages.operate),
        dataIndex: 'operation',
        type: 'operation',
        width: 100,
        fixed: true,
        authority: () => { return this.state.authorityList.includes('onlineUser'); },
        render: (text, record) => {
          return (
            <a onClick={() => this.logout(record)}>
              {intl.formatMessage(messages.logout)}
            </a>
          );
        }
      }
    ];

    this.state = {
      columns,
      userName: '',
      dateRange: [],
      pagination: {
        total: 0,
        current: 1,
        pageSize: 20
      },
      authorityList: []
    };
  }

  componentDidMount() {
    this.getData();
    this.getAuthority();
  }

  // 获取权限接口
  getAuthority = () => {
    commonApi.getAuthority({
      code: 'online'
    }).then((res) => {
      const { data: { list } } = res;
      this.setState({
        authorityList: list
      });
    });
  }

  // 获取列表数据
  getData = (params = {}) => {
    const { pagination: { current, pageSize }, userName, dateRange } = this.state;

    let startLoginTime;
    let endLoginTime;

    if (params.dateRange && params.dateRange[0] && params.dateRange[1]) {
      startLoginTime = moment(params.dateRange[0]).format();
      endLoginTime = moment(params.dateRange[1]).format();
    } else if (dateRange && dateRange[0] && dateRange[1]) {
      startLoginTime = moment(dateRange[0]).format();
      endLoginTime = moment(dateRange[1]).format();
    }

    onlineUsersApi.getList({
      username: params.userName || userName,
      startLoginTime,
      endLoginTime,
      current: params.current || current,
      pageSize: params.pageSize || pageSize
    }).then((res) => {
      const { data: { list, pagination } } = res;

      list.map((item, i) => {
        item.no = i + 1;
        return item;
      });

      this.setState({
        dataSource: list,
        pagination
      });
    });
  }

  // 查询
  handleSearch = (params = {}) => {
    this.getData({ ...params.search, ...params.pagination });
  }

  updateColumns = (columns) => {
    this.setState({
      columns
    });
  }

  // 注销
  logout = (params) => {
    const { intl } = this.props;
    const { pagination: { current, pageSize, total } } = this.state;

    confirm({
      title: intl.formatMessage(messages.confirmLogout, { name: params.userName }),
      content: intl.formatMessage(messages.confirmLogoutTip),
      onOk: () => {
        onlineUsersApi.logout({
          ids: params.id
        }).then(() => {
          const newCurrent = total - ((current - 1) * pageSize) === 1 ? current - 1 : current;
          this.getData({ current: newCurrent });
          message.success(intl.formatMessage(messages.logoutSuccess));
        });
      }
    });
  }

  render() {
    const { intl } = this.props;
    const { columns, dataSource, pagination } = this.state;

    return (
      <Layout className={styles.layout}>
        <Header className={styles.header}>
          <span>{intl.formatMessage(messages.onlineUserMgr)}</span>
        </Header>
        <div className={styles.content}>
          <SupTable
            rowKey="id"
            controlColumns
            showSelection={false}
            columns={columns}
            dataSource={dataSource}
            searchColumns={this.searchColumns}
            pagination={pagination}
            onSearch={this.handleSearch}
            updateColumns={this.updateColumns}
          />
        </div>
      </Layout>
    );
  }
}

export default OnlineManage;
