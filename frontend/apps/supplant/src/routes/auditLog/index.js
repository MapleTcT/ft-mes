import React from 'react';
import { injectIntl } from 'react-intl';
import momentjs from 'moment';
import { Layout } from 'sup-ui';
import SpinModal from 'root/components/SpinModal/index.js';
import style from './index.less';
import TreeTable from './TreeTable.js';
import ImportDetail from './ImportDetail.js';
import LogInfo from './LogInfo.js';
import Export from './Export.js';
import {
  fetchDataLogs,
  exportDataLog,
  downloadDataLog
} from '../../services/auditLog';
import { EXPORT_ALL, EXPORT_SELECTED } from './constant';
import messages from './messages';
import { formatSearchParams, downloadFile } from './utils';

const { Header, Content } = Layout;

const extractRowKey = (row) => {
  const { modelObjPk } = row;
  return modelObjPk;
};

const removeHtmlTag = (s) => s.replace(/<\/?[^>]+>/gi, '');

@injectIntl
// eslint-disable-next-line
export default class AuditLog extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      exportLoading: false,
      showImportDetail: false,
      selectModelObjPk: null, // 用于标识辅模型某一条数据
      showLogInfo: false,
      columns: [
        {
          width: 220,
          title: this.intl('col.moduleName'),
          dataIndex: 'moduleName',
          key: 'moduleName',
          filterType: 'search'
        },
        {
          width: 120,
          title: this.intl('col.formName'),
          dataIndex: 'formName',
          key: 'formName',
          filterType: 'search'
        },

        {
          width: 130,
          title: this.intl('col.operateUserName'),
          dataIndex: 'operateUserName',
          key: 'operateUserName',
          filterType: 'search',
          sorter: true
        },
        {
          width: 150,
          title: this.intl('col.operateTime'),
          dataIndex: 'operateTime',
          key: 'operateTime',
          filterType: 'date',
          render: this.renderColOperateTime,
          filterOptions: [
            { label: '一天内', value: '1' },
            { label: '三天内', value: '2' },
            { label: '一周内', value: '3' },
            { label: '一月内', value: '4' },
            { label: '三月内', value: '5' },
            { label: '半年内', value: '6' },
            { label: '一年内', value: '7' }
          ],
          sorter: true
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
          filterType: 'search',
          sorter: true
        },
        {
          width: 120,
          title: this.intl('col.operateType'),
          dataIndex: 'operateType.displayName',
          key: 'operateType.displayName',
          filterType: 'checkbox',
          filterOptions: [
            { label: this.intl('filterType.add'), value: 'ADD' },
            { label: this.intl('filterType.modify'), value: 'MODIFY' },
            { label: this.intl('filterType.delete'), value: 'DELETE' },
            { label: this.intl('filterType.other'), value: 'OTHER' },
            { label: this.intl('filterType.import'), value: 'IMPORT' },
            { label: this.intl('filterType.export'), value: 'EXPORT' },
            { label: this.intl('filterType.invalid'), value: 'INVALID' },
            { label: this.intl('filterType.reject'), value: 'REJECT' },
            { label: this.intl('filterType.print'), value: 'PRINT' },
            { label: this.intl('filterType.batchprint'), value: 'BATCHPRINT' },
            { label: this.intl('filterType.revoke'), value: 'REVOKE' }
          ],
          sorter: true
        },
        {
          width: 150,
          title: this.intl('col.ipAddress'),
          dataIndex: 'ipAddress',
          key: 'ipAddress',
          filterType: 'search'
        },
        {
          width: 150,
          title: this.intl('col.description'),
          dataIndex: 'description',
          key: 'description'
        },
        {
          width: 170,
          title: this.intl('col.exceptionDescription'),
          dataIndex: 'exceptionDescription',
          key: 'exceptionDescription',
          render: this.renderColExDesc
        },
        {
          width: 150,
          title: this.intl('col.fileName'),
          dataIndex: 'fileName',
          key: 'fileName',
          render: this.renderColFileName
        },
        {
          width: 90,
          title: this.intl('col.operation'),
          type: 'operation',
          dataIndex: 'operation',
          key: 'operation',
          fixed: true,
          render: this.renderColOperation
        }
      ],
      dataSource: [],
      current: 1,
      pageSize: 20,
      total: 0
    };

    this.mainTableButtons = [
      {
        key: 'export',
        content: this.intl('btn.export'),
        disabled: true,
        callback: () => this.setState({ showExport: true })
      }
    ];
  }

  intl(key, data = {}) {
    const { intl } = this.props;
    return intl.formatMessage(messages[key], data);
  }

  renderColOperation = (_, data) => {
    return (
      <a
        onClick={() => {
          this.handleLink(data);
        }}
      >
        {this.intl('btn.viewDetail')}
      </a>
    );
  };

  renderColExDesc = (text) => {
    return <span title={text}>{text}</span>;
  };

  renderColFileName = (fileName, row) => {
    return (
      <a
        onClick={this.handleDownloadImportFile}
        traceId={row.traceId}
        className={style.importFileLink}
      >
        {fileName}
      </a>
    );
  };

  renderColOperateTime = (t) => {
    return momentjs(t).format('YYYY-MM-DD HH:mm:ss');
  };

  handleDownloadImportFile = ({ target }) => {
    const traceId = target.getAttribute('traceid');
    downloadDataLog(traceId).then(({ headers, data }) => {
      downloadFile(data, headers);
    });
  };

  showImportLogInfo = (importLogInfo) => {
    this.setState({
      showLogInfo: true,
      importLogInfo,
      traceId: importLogInfo.traceId
    });
  };

  fetchDataLogsList(params) {
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

    return fetchDataLogs(queryPararms, dataBody).then(({ data }) => {
      const { list, pagination } = data;
      const {
        mainTableButtons: [exportBtn]
      } = this;
      if (list && list.length) {
        exportBtn.disabled = false;
        list.forEach((e) => {
          const errDesc = e.exceptionDescription;
          if (errDesc) {
            e.exceptionDescription = removeHtmlTag(errDesc);
          }
        });
      }
      this.selectedRows = [];
      this.setState({
        dataSource: list,
        ...pagination
      });
    });
  }

  componentDidMount() {
    this.fetchDataLogsList();
  }

  handleLink = (data) => {
    const {
      traceId,
      operateType: { code },
      modelObjPk: selectModelObjPk
    } = data;
    if (code === 'IMPORT') {
      this.setState({ showImportDetail: true, traceId });
    } else {
      this.setState({ showLogInfo: true, traceId, selectModelObjPk });
    }
  };

  // 导出
  handleExport = (value) => {
    const params = { all: false };
    if (value === EXPORT_ALL) {
      params.all = true;
    } else if (value === EXPORT_SELECTED) {
      const { selectedRows } = this;
      if (selectedRows && selectedRows.length) {
        params.traceIds = selectedRows.map((d) => d.traceId);
      } else {
        const { dataSource } = this.state;
        params.traceIds = dataSource.map((d) => d.traceId);
      }
    }

    this.beforeExportAction();
    exportDataLog(params)
      .then(({ headers, data }) => {
        this.afterExportAction();
        downloadFile(data, headers);
      })
      .catch(() => {
        this.afterExportAction();
      });
  };

  beforeExportAction() {
    this.exportTimeout = setTimeout(() => {
      this.changeExportLoadingState(true);
    }, 2e3);
    this.mainTableButtons[0].disabled = true;
    this.setState({ showExport: false });
  }

  afterExportAction() {
    clearTimeout(this.exportTimeout);
    this.exportTimeout = null;
    this.mainTableButtons[0].disabled = false;
    this.changeExportLoadingState(false);
    this.forceUpdate();
  }

  changeExportLoadingState(exportLoading) {
    this.setState({
      exportLoading
    });
  }

  onSearch = (params) => {
    this.fetchDataLogsList(params);
  };

  handleChangeSelectItem = (_, rows) => {
    this.selectedRows = rows;
  };

  hideLogInfo = () => {
    this.setState({ showLogInfo: false, importLogInfo: null });
  };

  render() {
    const {
      dataSource,
      columns,
      total,
      current,
      pageSize,
      showImportDetail,
      showExport,
      traceId,
      showLogInfo,
      importLogInfo,
      exportLoading,
      selectModelObjPk
    } = this.state;

    const exportLoadingTip = this.intl('loading.exportTip');
    return (
      <Layout className={style.layout}>
        <Header className={style.topHeader}>
          {this.intl('businessAuditLogHeader')}
        </Header>
        <Content>
          <TreeTable
            rowKey={extractRowKey}
            dataSource={dataSource}
            columns={columns}
            showSearch={false}
            showSearchIcon={false}
            showSelection={false}
            onSelectItem={this.handleChangeSelectItem}
            pagination={{
              total,
              current,
              pageSize
            }}
            onSearch={this.onSearch}
            btnColumns={this.mainTableButtons}
          />
          {showImportDetail && (
            <ImportDetail
              traceId={traceId}
              showImportLogInfo={this.showImportLogInfo}
              onCancel={() => this.setState({ showImportDetail: false })}
              onOk={() => this.setState({ showImportDetail: false })}
            />
          )}
          {showLogInfo && (
            <LogInfo
              traceId={traceId}
              importLogInfo={importLogInfo}
              onOk={this.hideLogInfo}
              onCancel={this.hideLogInfo}
              selectModelObjPk={selectModelObjPk}
            />
          )}
          {showExport && (
            <Export
              onCancel={() => this.setState({ showExport: false })}
              onOk={this.handleExport}
            />
          )}
        </Content>
        <SpinModal visible={exportLoading} tip={exportLoadingTip} />
      </Layout>
    );
  }
}
