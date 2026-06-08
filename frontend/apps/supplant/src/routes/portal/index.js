import React from 'react';
import { injectIntl } from 'react-intl';
import { Layout, Checkbox, Divider, Icon } from 'sup-ui';
import SupTable from 'sup-rc-table';
import SupResize from 'sup-rc-resize';
import {
  getAuthorityList,
  getLicenseByModule
} from '../../services/authorityList.js';
import Sider from './Sider.js';
import messages from './messages.js';
import PortalEditForm from './PortalEditForm.js';

const { Content } = Layout;

@injectIntl
export default class Portal extends React.PureComponent {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.formatMessage = intl.formatMessage;
    this.state = {
      columns: [
        {
          width: 100,
          title: intl.formatMessage(messages.portalCode),
          dataIndex: 'portalCode',
          align: 'center',
          key: 'portalCode'
        },
        {
          width: 100,
          title: intl.formatMessage(messages.portalTitle),
          dataIndex: 'portalTitle',
          align: 'center',
          key: 'portalTitle'
        },
        {
          width: 200,
          title: intl.formatMessage(messages.portalModel),
          dataIndex: 'portalModel',
          align: 'center',
          key: 'portalModel'
        },
        {
          width: 300,
          title: intl.formatMessage(messages.portalUrl),
          dataIndex: 'portalUrl',
          align: 'center',
          key: 'portalUrl'
        },
        {
          width: 100,
          title: intl.formatMessage(messages.portalApplyAuth),
          dataIndex: 'portalApplyAuth',
          key: 'portalApplyAuth',
          align: 'center',
          render: () => (
            <span>
              <Checkbox />
            </span>
          )
        },
        {
          width: 100,
          title: intl.formatMessage(messages.portalApplyIframe),
          dataIndex: 'portalApplyIframe',
          key: 'portalApplyIframe',
          align: 'center',
          render: () => <Checkbox />
        },
        {
          width: 200,
          title: intl.formatMessage(messages.portalRelatedMenu),
          dataIndex: 'portalRelatedMenu',
          align: 'center',
          key: 'portalRelatedMenu'
        },
        {
          width: 100,
          title: intl.formatMessage(messages.portalHide),
          dataIndex: 'portalHide',
          key: 'portalHide',
          align: 'center',
          render: () => <Checkbox />
        },
        {
          width: 200,
          title: intl.formatMessage(messages.portalOperation),
          dataIndex: 'operation',
          key: 'operation',
          type: 'operation',
          align: 'center',
          fixed: false,
          render: (_, record, index) => (
            <span>
              <a
                style={{ textDecoration: 'underline' }}
                onClick={() => this.editRow(record, index)}
              >
                {intl.formatMessage(messages.portalOperationEdit)}
              </a>
              <Divider type="vertical" />
              <a
                style={{ textDecoration: 'underline' }}
                onClick={() => this.deleteRow(record, index)}
              >
                {intl.formatMessage(messages.portalOperationDel)}
              </a>
            </span>
          )
        }
      ],
      dataSource: [],
      currentPage: 1,
      pageSize: 20
    };
    this.btnColumns = [
      {
        key: 'addUser',
        // authority: this.props.hasAuth('addRoleUser'),
        className: 'ant-btn-primary ant-btn-background-ghost',
        content: () => (
          <div>
            <Icon type="plus" style={{ marginRight: 5, fontSize: '13px' }} />
            {intl.formatMessage(messages.portalBtnAdd)}
          </div>
        ),
        callback: () => {
          this.setState({ visible: true });
        }
      },
      {
        key: 'deleteUser',
        className: 'ant-btn-primary ant-btn-background-ghost',
        content: intl.formatMessage(messages.portalBtnMutiDel),
        callback: () => {}
      }
    ];
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

  editRow = (record, index) => {
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

  deleteRow = (record, index) => {
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

  onCancel = () => {
    this.setState({ visible: false });
  };

  render() {
    const {
      dataSource,
      columns,
      total,
      currentPage,
      pageSize,
      visible
    } = this.state;

    return (
      <>
        <SupResize>
          <Sider />
          <Content style={{ height: '100%' }}>
            <SupTable
              rowKey={(item) => item.id}
              dataSource={dataSource}
              columns={columns}
              showSearch
              showColumnsFilter
              showSelection
              btnColumns={this.btnColumns}
              pagination={{
                total,
                current: currentPage,
                pageSize
              }}
              onSearch={this.onSearch}
            />
          </Content>
        </SupResize>
        {visible && (
          <PortalEditForm onCancel={this.onCancel} key="PortalEditForm" />
        )}
      </>
    );
  }
}
