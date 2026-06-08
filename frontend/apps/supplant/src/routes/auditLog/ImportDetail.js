import React from 'react';
import { injectIntl } from 'react-intl';
import SupTable from 'sup-rc-table';
import { Modal } from 'sup-ui';
import { fetchImportDataLog } from '../../services/auditLog';
import './index.less';
import { formatSearchParams } from './utils';
import messages from './messages';

@injectIntl
export default class ExportDetail extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      columns: [
        {
          width: 150,
          title: this.intl('col.formName'),
          dataIndex: 'entityName',
          key: 'entityName'
        },
        {
          width: 150,
          title: this.intl('col.modelObjName'),
          dataIndex: 'modelObjName',
          key: 'modelObjName',
          filterType: 'search'
        },
        {
          width: 150,
          title: this.intl('col.modelObjCode'),
          dataIndex: 'modelObjCode',
          key: 'modelObjCode',
          filterType: 'search'
        },
        {
          width: 100,
          title: this.intl('col.operateType'),
          dataIndex: 'operateType.displayName',
          key: 'operateType.displayName',
          filterType: 'checkbox',
          filterOptions: [
            { label: this.intl('filterType.add'), value: 'ADD' },
            { label: this.intl('filterType.modify'), value: 'MODIFY' },
            { label: this.intl('filterType.import'), value: 'IMPORT' }
          ]
        },
        {
          width: 150,
          title: this.intl('col.operation'),
          type: 'operation',
          dataIndex: 'operation',
          key: 'operation',
          render: (_, data) => {
            return (
              <a
                onClick={() => {
                  this.handleLink(data);
                }}
              >
                {this.intl('btn.viewDetail')}
              </a>
            );
          }
        }
      ],
      dataSource: [],
      current: 1,
      pageSize: 20,
      total: 0
    };
  }

  intl(key, data = {}) {
    const { intl } = this.props;
    return intl.formatMessage(messages[key], data);
  }

  componentDidMount() {
    this.fetchImportDataLogsList();
  }

  fetchImportDataLogsList(params) {
    let dataBody;
    let queryPararms;
    if (!params) {
      const { current, pageSize } = this.state;
      queryPararms = { current, pageSize };
    } else {
      const formatedData = formatSearchParams(params);
      dataBody = formatedData.dataBody;
      queryPararms = formatedData.queryParams;
    }
    const { traceId } = this.props;

    return fetchImportDataLog(traceId, queryPararms, dataBody).then(
      ({ data }) => {
        const { list, pagination } = data;
        this.setState({
          dataSource: list,
          ...pagination
        });
      }
    );
  }

  handleCancel = () => {
    const { onCancel } = this.props;
    onCancel();
  };

  handleOk = () => {
    const { onOk } = this.props;
    onOk();
  };

  handleLink = (data) => {
    this.props.showImportLogInfo(data);
  };

  onSearch = (params) => {
    this.fetchImportDataLogsList(params);
  };

  render() {
    const { columns, dataSource, total, current, pageSize } = this.state;

    return (
      <>
        <Modal
          title={this.intl('modal.title.import')}
          visible
          onOk={this.handleOk}
          onCancel={this.handleCancel}
          width={800}
          className="sup-log-detail"
          cancelButtonProps={{ style: { display: 'none' } }}
          okText={this.intl('modal.btn.close')}
        >
          <SupTable
            rowKey="modelObjCode"
            dataSource={dataSource}
            columns={columns}
            showSearch={false}
            showColumnsFilter={false}
            showSelection={false}
            pagination={{
              total,
              current,
              pageSize
            }}
            onSearch={this.onSearch}
          />
        </Modal>
      </>
    );
  }
}
