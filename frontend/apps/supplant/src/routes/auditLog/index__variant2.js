import React from 'react';
import { injectIntl } from 'react-intl';
import './index.less';
import mockData from '../../mock/authLog.js';
import TreeTable from './TreeTable.js';
import Detail from './Detail.js';
import Export from './Export.js';

@injectIntl
export default class AuditLog extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      columns: [
        {
          width: 150,
          title: '所属模块',
          dataIndex: 'moduleName',
          key: 'moduleName',
          filterType: 'search'
        },
        {
          width: 50,
          title: '序号',
          dataIndex: 'rowIndex',
          key: 'rowIndex',
          fixed: true,
          filterType: 'search',
          render: (text, record) => {
            return <span>{record.sort}</span>;
          },
          className: 'indexColumn'
        },
        {
          width: 150,
          title: '表单名称',
          dataIndex: 'formName',
          key: 'formName',
          filterType: 'search'
        },

        {
          width: 150,
          title: '操作用户',
          dataIndex: 'operator',
          key: 'operator',
          filterType: 'search'
        },
        {
          width: 150,
          title: '操作时间',
          dataIndex: 'operateTime',
          key: 'operateTime',
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
          title: 'IP地址',
          dataIndex: 'ip',
          key: 'ip',
          filterType: 'search'
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
          title: '操作异常描述',
          dataIndex: 'errorDesc',
          key: 'errorDesc',
          filterType: 'search'
        },
        {
          width: 150,
          title: '导入/导出文件',
          dataIndex: 'importFile',
          key: 'importFile'
        },
        {
          width: 150,
          title: '操作',
          type: 'operation',
          dataIndex: 'operation',
          key: 'operation',
          fixed: true,
          render: () => {
            return <a onClick={this.handleLink}>查看详情</a>;
          }
        }
      ],
      dataSource: mockData.list,
      currentPage: 1,
      pageSize: 20
    };
  }

  handleLink = () => {
    this.setState({ showDetail: true });
  };

  // 导出
  handleExport = (value) => {
    this.setState({ showExport: false });
    console.log(value);
  };

  render() {
    const {
      dataSource,
      columns,
      total,
      currentPage,
      pageSize,
      showDetail,
      showExport
    } = this.state;
    return (
      <>
        <TreeTable
          rowKey={(item) => item.rowIndex}
          dataSource={dataSource}
          columns={columns}
          showSearch={false}
          showSearchIcon={false}
          showColumnsFilter={false}
          showSelection={false}
          pagination={{
            total,
            current: currentPage,
            pageSize
          }}
          onSearch={this.onSearch}
          btnColumns={[
            {
              key: 'export',
              content: '导出',
              callback: () => this.setState({ showExport: true })
            }
          ]}
        />
        {showDetail && (
          <Detail
            onCancel={() => this.setState({ showDetail: false })}
            onOk={() => this.setState({ showDetail: false })}
          />
        )}
        {showExport && (
          <Export
            onCancel={() => this.setState({ showExport: false })}
            onOk={this.handleExport}
          />
        )}
      </>
    );
  }
}
