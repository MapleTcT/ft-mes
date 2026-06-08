import React from 'react';
import { injectIntl } from 'react-intl';
import SupTable from 'sup-rc-table';
import { Modal } from 'sup-ui';
import mockData from '../../mock/authLog.js';
import LogInfo from './LogInfo.js';

import './index.less';

@injectIntl
export default class Detail extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      columns: [
        {
          width: 150,
          title: '表单名称',
          dataIndex: 'formName',
          key: 'formName',
          filterType: 'search'
        },
        {
          width: 150,
          title: '被操作对象名称',
          dataIndex: 'beOperated',
          key: 'beOperated',
          filterType: 'search'
        },
        {
          width: 150,
          title: '被操作对象编码',
          dataIndex: 'beOperatedCode',
          key: 'beOperatedCode',
          filterType: 'search'
        },
        {
          width: 150,
          title: '操作类型',
          dataIndex: 'operateType',
          key: 'operateType',
          filterType: 'checkbox',
          filterOptions: [
            { label: '新增', value: 'add' },
            { label: '修改', value: 'modify' },
            { label: '删除', value: 'delete' },
            { label: '其他', value: 'other' },
            { label: '导入', value: 'import' },
            { label: '导出', value: 'export' },
            { label: '作废', value: 'cancel' },
            { label: '驳回', value: 'reject' },
            { label: '打印', value: 'print' },
            { label: '批量打印', value: 'batchPrint' },
            { label: '还原', value: 'revoke' }
          ]
        },
        {
          width: 150,
          title: '操作描述',
          dataIndex: 'operateDesc',
          key: 'operateDesc',
          filterType: 'search'
        },
        {
          width: 150,
          title: '操作',
          type: 'operation',
          dataIndex: 'operation',
          key: 'operation',
          render: () => {
            return <a onClick={this.handleLink}>查看详情</a>;
          }
        }
      ],
      currentPage: 1,
      pageSize: 20,
      dataSource: mockData.list
    };
  }

  handleCancel = () => {
    const { onCancel } = this.props;
    onCancel();
  };

  handleOk = () => {
    const { onOk } = this.props;
    onOk();
  };

  handleLink = () => {
    this.setState({ showLogInfo: true });
  };

  render() {
    const {
      columns,
      dataSource,
      total,
      currentPage,
      pageSize,
      showLogInfo
    } = this.state;
    return (
      <>
        <Modal
          title="导入明细"
          visible
          onOk={this.handleOk}
          onCancel={this.handleCancel}
          width={800}
          className="sup-log-detail"
        >
          <SupTable
            rowKey={(item) => item.rowIndex}
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
          />
        </Modal>
        {showLogInfo && (
          <LogInfo
            onOk={() => this.setState({ showLogInfo: false })}
            onCancel={() => this.setState({ showLogInfo: false })}
          />
        )}
      </>
    );
  }
}
