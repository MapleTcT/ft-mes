import React from 'react';
import { injectIntl } from 'react-intl';
import SupTable from 'sup-rc-table';
import { message, Modal, Radio, Divider } from 'sup-ui';
import {
  fetchIntlList,
  downloadExcel,
  createDownloadExcel,
  checkDownloadStatus,
  checkUploadStatus,
  getImportErrorFile,
  sugRec
} from 'root/services/intl';
import SpinModal from 'root/components/SpinModal/index.js';
import { genFilter, downloadFile, fetchDownloadFile } from './utils';
import {
  IMPORT_URL,
  EXPORT_TEMPLATE_URL,
  DOWNLOAD_ALL,
  DOWNLOAD_SELECTED,
  XLSX_TYPE
} from './constant';

import style from './style.less';
import messages from './messages';

class IntlList extends React.Component {
  state = {
    pagination: {
      pageSize: 20,
      current: 1,
      total: 0
    },
    filter: {},
    tableData: [],
    loading: false,
    exportModalVisible: false,
    exportType: 'selected'
  };

  constructor(props) {
    super(props);

    this.btnColumns = [
      {
        key: 'add',
        authority: this.props.hasAuth('i18n'),
        content: this.intl('tableBtnAdd'),
        callback: () => {
          this.props.handleAddIntl();
        }
      },
      {
        key: 'settings',
        authority: this.props.hasAuth('i18n'),
        content: this.intl('tableBtnLanguageSettings'),
        callback: () => {
          this.props.handleIntlSetting();
        }
      },
      {
        key: 'exportExcel',
        content: this.intl('tableBtnExportExcel'),
        callback: this.exportExcel
      },
      {
        key: 'moreBtn',
        callback: (params) => {
          if (params.callback) {
            params.callback();
          }
        },
        menu: [
          {
            key: 'import',
            content: this.intl('tableBtnImport'),
            importParams: {
              action: IMPORT_URL,
              accept: '.xlsx',
              headers: {
                // FIXME 单独封装
                Authorization: `Bearer ${localStorage.getItem('ticket')}`
              },
              beforeUpload: (file) => {
                const fileName = (file.name || '').toLowerCase();
                const isXLSX = fileName && fileName.endsWith('.xlsx');
                if (!isXLSX) {
                  message.error(this.intl('importTypeErrMsg'));
                  return false;
                }
                this.lastUploadTime = Date.now();
              },
              onChange: this.handleImportChange,
              onError: (res, data) => {
                this.resetUploadState();
                message.error(
                  (data && data.message) || this.intl('importFailMsg')
                );
              }
            }
          },
          {
            key: 'exportTempalte',
            callback: () => {
              fetchDownloadFile(EXPORT_TEMPLATE_URL, null, null, () => {
                message.error(this.intl('downloadTemplateErr'));
              });
            },
            content: this.intl('tableBtnExportTemplate')
          }
        ]
      }
    ];

    // 确保冻结属性引用
    this.operateColumn = {
      width: 130,
      title: this.intl('columnOperate'),
      authority: () => {
        return this.props.hasAuth(['i18n']);
      },
      render: this.renderOperateColumn,
      type: 'operation'
    };
  }

  intl(key, data) {
    const { intl } = this.props;
    return intl.formatMessage(messages[key], data);
  }

  downloadImportErrorFile(id) {
    const url = getImportErrorFile(id);
    fetchDownloadFile(url, null, null, () => {
      message.error(this.intl('downloadErrorFileErr'));
    });
  }

  exportExcel = () => {
    if (this.exportTimer) return false;
    this.setState({
      exportModalVisible: true
    });
  };

  resetUploadState() {
    this.lastUploadTime = null;
    this.setState({
      loading: false
    });
  }

  refreshUploadState() {
    const { loading } = this.state;
    const waiting = Date.now() - this.lastUploadTime >= 2e3;
    if (this.lastUploadTime && !loading && waiting) {
      this.setState({
        loading: true,
        loadingTip: this.intl('importingTip')
      });
    }
  }

  createImportFailFileContent = (filename) => {
    // FIXME 其他语言
    const msg = this.intl('downloadImportFailFile').split('@');
    return (
      <span>
        {msg[0]}
        <a
          style={{ textDecoration: 'underline' }}
          onClick={(e) => {
            e.preventDefault();
            this.downloadImportErrorFile(filename);
          }}
        >
          {msg[1]}
        </a>
        {msg[2]}
      </span>
    );
  };

  refreshUploadStatus(id) {
    this.refreshUploadState();
    checkUploadStatus({
      id
    })
      .then((res) => {
        const {
          data: {
            data: { status, errorMessage, errorFile, errorNum, addNum, allNum }
          }
        } = res;
        // 1 解析进行中 2 导入结束 3 导入失败
        if (status === 1) {
          setTimeout(() => {
            this.refreshUploadStatus(id);
          }, 2000);
        } else if (status === 2) {
          this.resetUploadState();
          // 区分内容不正确情况
          if (errorFile) {
            Modal.error({
              title: this.intl('importFailModalTitle'),
              content: this.createImportFailFileContent(id)
            });
          } else {
            const modifiedNum = allNum - addNum - errorNum;
            let addedMsg = '';
            if (addNum) {
              addedMsg = this.intl('importAddedMsg', { num: addNum });
            }
            let modifiedMsg = '';
            if (modifiedNum) {
              modifiedMsg = this.intl('importUpdatedMsg', {
                num: modifiedNum
              });
            }

            message.success(
              this.intl('importSuccessMsg', { addedMsg, modifiedMsg })
            );
          }
          this.refreshIntlList();
        } else if (status === 3) {
          this.resetUploadState();
          Modal.error({
            title: this.intl('importFailModalTitle'),
            content: errorMessage
          });
        }
      })
      .catch(({ data }) => {
        this.resetUploadState();
        message.error((data && data.message) || this.intl('importFailMsg'));
      });
  }

  handleImportChange = ({ file }) => {
    // 处理导入时间过长问题
    this.refreshUploadState();
    if (file.status === 'done') {
      const {
        response: { data }
      } = file;
      this.refreshUploadStatus(data);
    }
  };

  updateColumns = (columns) => {
    this.columns = columns;
    this.setState({});
  };

  getColumns() {
    const { lanList } = this.props;
    if (this.lastLanList !== lanList) {
      this.lastLanList = lanList;
      this.columns = lanList
        .filter((d) => !!d.used)
        .map((d) => {
          return {
            title: d.languType,
            dataIndex: `i18nValues.${d.languCode}`,
            key: `i18nValues.${d.languCode}`,
            width: 200,
            filterType: 'search',
            render: this.renderTextWithTitle
          };
        });

      this.columns.unshift({
        width: 200,
        title: this.intl('columnKey'),
        key: 'i18nKey',
        dataIndex: 'i18nKey',
        filterType: 'search'
      });

      this.columns.push(this.operateColumn);
    }
    return this.columns;
  }

  renderTextWithTitle = (value) => {
    return <span title={value}>{value}</span>;
  };

  // 获取国际化列表
  refreshIntlList = () => {
    const {
      pagination: { current, pageSize },
      filter
    } = this.state;

    fetchIntlList({
      current,
      pageSize,
      ...filter
    }).then((res) => {
      const {
        data: { pagination, list: tableData }
      } = res;
      this.setState({
        pagination,
        tableData
      });
      // FIXME 当前翻页需重置选中状态
      this.selecteRows = [];
    });
  };

  componentDidMount() {
    this.refreshIntlList();
    // 判断表格操作栏是否固定
    if (this.table && this.table.changeOperationFixedStatus) {
      this.table.changeOperationFixedStatus();
    }
  }

  renderOperateColumn = (_, row) => {
    const btns = [
      {
        onClick: (e) => {
          e.preventDefault();
          this.props.handleEditIntl(row);
        },
        text: this.intl('operateColumnBtnEdit'),
        authCode: 'i18n'
      },
      {
        onClick: (e) => {
          e.preventDefault();
          this.props.handleRemoveIntl(row, this.refreshIntlList);
        },
        text: this.intl('operateColumnBtnRemove'),
        authCode: 'i18n'
      }
    ];

    const authBtns = btns.filter((btn) => {
      const { authCode } = btn;
      return !authCode || this.props.hasAuth(authCode);
    });

    return (
      <div className={style.operateCell}>
        <div>
          {authBtns.map((btn, i) => {
            return (
              <>
                <a onClick={btn.onClick}>{btn.text}</a>
                {i < authBtns.length - 1 ? <Divider type="vertical" /> : null}
              </>
            );
          })}
        </div>
      </div>
    );
  };

  handleChangeSelectItem = (_, rows) => {
    this.selecteRows = rows;
  };

  addSelectKeysParam(param) {
    const { selecteRows = [] } = this;
    const i18nKey = selecteRows.map((d) => d.i18nKey).join(',');
    if (i18nKey) {
      param.i18nKeyListStr = i18nKey;
    }
  }

  handleOnSearch = (data) => {
    const { pagination, keyword, filters } = data;
    const nextData = {
      filter: genFilter(filters),
      pagination: { ...pagination }
    };
    // 手动组织过滤数据
    nextData.keyword = '';
    if (keyword && keyword.keyword) {
      nextData.keyword = keyword;
    }
    this.setState(nextData, () => {
      this.refreshIntlList();
    });
  };

  handleExportTypeChange = ({ target: { value } }) => {
    this.setState({
      exportType: value
    });
  };

  handleExportFail = (msg) => {
    this.clearExportTimer();
    message.error(msg || this.intl('exportFailMsg'));
  };

  clearExportTimer = () => {
    clearTimeout(this.exportTimer);
    this.exportTimer = null;
    this.setState({
      loading: false
    });
  };

  startDownloadExcel = (id) => {
    downloadExcel({ id })
      .then((res) => {
        this.clearExportTimer();
        downloadFile(res.data, 'i18n.xlsx', XLSX_TYPE);
        message.success(this.intl('exportSuccessMsg'));
      })
      .catch(() => {
        this.clearExportTimer();
        message.error(this.intl('exportFailMsg'));
      });
  };

  refreshExportStatus = (id) => {
    checkDownloadStatus({ id })
      .then((res) => {
        const {
          data: {
            data: { status }
          }
        } = res;
        // 1 进行中 2 成功 3 失败
        if (status === 1) {
          setTimeout(() => {
            this.refreshExportStatus(id);
          }, 2000);
        } else if (status === 2) {
          this.startDownloadExcel(id);
        } else if (status === 3) {
          this.handleExportFail(res.data.data);
        }
      })
      .catch(({ data }) => {
        this.handleExportFail(data.message);
      });
  };

  handleExportOk = () => {
    if (this.exportTimer) return false;
    this.exportTimer = setTimeout(() => {
      this.setState({
        loading: true,
        loadingTip: this.intl('exportingTip')
      });
    }, 2000);

    this.hideExportModal();

    // 组织参数
    const {
      exportType,
      pagination: { current, pageSize },
      filter
    } = this.state;
    const downAll = exportType === DOWNLOAD_ALL;
    const params = {
      downAll,
      current,
      pageSize,
      ...filter
    };

    // 组织当前选择行
    this.addSelectKeysParam(params);

    createDownloadExcel(params)
      .then((res) => {
        this.refreshExportStatus(res.data.data);
      })
      .catch(() => {
        this.handleExportFail();
      });
  };

  hideExportModal = () => {
    this.setState({
      exportModalVisible: false
    });
  };

  getFilterParams = () => {
    if (!this.filterParams) {
      const { lanList } = this.props;
      const filterPage = 100;
      const obj = {
        // 固定国际化key
        i18nKey: {
          param: 'i18n_key',
          url: sugRec(),
          customParmas() {
            return `current=1&pageSize=${filterPage}`;
          },
          callback({ data: { list } }) {
            return list;
          }
        }
      };

      // 循环语言
      lanList.forEach(({ languCode }) => {
        obj[`i18nValues.${languCode}`] = {
          param: 'i18n_value',
          url: sugRec(),
          customParmas() {
            return `current=1&pageSize=${filterPage}&langu_code=${languCode}`;
          },
          callback({ data: { list } }) {
            return list;
          }
        };
      });

      this.filterParams = obj;
    }
    return this.filterParams;
  };

  handleDblClick = (row) => {
    const editable = this.props.hasAuth('i18n');
    if (editable) {
      this.props.handleEditIntl(row);
    }
  };

  render() {
    const {
      tableData,
      pagination,
      loading,
      loadingTip,
      exportModalVisible,
      exportType
    } = this.state;
    return (
      <>
        <SpinModal visible={loading} tip={loadingTip} />
        <Modal
          maskClosable={false}
          title={this.intl('exportModalTitle')}
          width={400}
          visible={exportModalVisible}
          onOk={this.handleExportOk}
          onCancel={this.hideExportModal}
          wrapClassName={style.exportModal}
        >
          <Radio.Group
            onChange={this.handleExportTypeChange}
            value={exportType}
          >
            <Radio value={DOWNLOAD_SELECTED} className={style.exportRadio}>
              {this.intl('exportRadioSelected')}
            </Radio>
            <Radio value={DOWNLOAD_ALL}>{this.intl('exportRadioAll')}</Radio>
          </Radio.Group>
        </Modal>
        <SupTable
          ref={(ref) => {
            this.table = ref;
          }}
          onDoubleClick={this.handleDblClick}
          size="small"
          btnColumns={this.btnColumns}
          showColumnsFilter={false}
          tableKey="intlList"
          columns={this.getColumns()}
          updateColumns={this.updateColumns}
          showSelection
          showSearchIcon={false}
          onSelectItem={this.handleChangeSelectItem}
          pagination={pagination}
          rowKey="i18nKey"
          dataSource={tableData}
          operationBarTitle=""
          onSearch={this.handleOnSearch}
          filterParams={this.getFilterParams()}
        />
      </>
    );
  }
}

export default injectIntl(IntlList, { forwardRef: true });
