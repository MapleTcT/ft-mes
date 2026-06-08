import React from 'react';
import { Modal, message } from 'sup-ui';
import { injectIntl } from 'react-intl';
import SupTable from 'sup-rc-table';
import OperateForm from './OperateForm.js';
import {
  tableData,
  deleteData,
  modifyState,
  queryAlignList
} from '../../services/printManage.js';
import messages from './messages.js';
import style from './style.less';

@injectIntl
export default class DetailTable extends React.Component {
  constructor(props) {
    super(props);
    this.formatMessage = props.intl.formatMessage;
    this.initPagination = {
      current: 1,
      pageSize: 20
    };
    this.state = {
      title: '',
      displayModal: false,
      dataSource: [],
      selectedRows: [],
      pagination: this.initPagination,
      columns: this.initColumns()
    };
  }

  // eslint-disable-next-line camelcase
  UNSAFE_componentWillReceiveProps(nextProps) {
    if (nextProps.selectedNode !== this.props.selectedNode) {
      this.setState({ pagination: this.initPagination }, () => {
        this.getTableData(nextProps.selectedNode);
        this.getAlignList(nextProps.selectedNode);
      });
    }
  }

  componentDidMount() {
    this.getTableData();
    this.getAlignList();
    // 监听设计模板关闭
    window.addEventListener('message', (event = {}) => {
      const { data = {} } = event;
      const { action } = data;
      if (action === 'refresh') {
        this.getTableData();
      }
    });
  }

  initBtnColumns = () => {
    const { selectedNode = {} } = this.props;
    const { selectedRows } = this.state;
    return [
      {
        key: 'add',
        content: this.formatMessage(messages.btn_add),
        callback: () => {
          this.getDetail(
            { appId: selectedNode.code },
            this.formatMessage(messages.btn_add),
            'new'
          );
        }
      },
      {
        key: 'start',
        disabled: selectedRows.length === 0,
        content: this.formatMessage(messages.btn_start),
        callback: () => {
          const tit = this.formatMessage(messages.btn_start);
          Modal.confirm({
            title: this.formatMessage(messages.confirm_switch_tit, {
              name: tit
            }),
            content: this.formatMessage(messages.confirm_switch_content),
            onOk: () => {
              this.switchState(
                {
                  enabled: 1,
                  templateIds: selectedRows.map((d) => d.id)
                },
                tit
              );
            }
          });
        }
      },
      {
        key: 'end',
        divider: true,
        disabled: selectedRows.length === 0,
        content: this.formatMessage(messages.btn_stop),
        callback: () => {
          const tit = this.formatMessage(messages.btn_stop);
          Modal.confirm({
            title: this.formatMessage(messages.confirm_switch_tit, {
              name: tit
            }),
            content: this.formatMessage(messages.confirm_switch_content),
            onOk: () => {
              this.switchState(
                {
                  enabled: 4,
                  templateIds: selectedRows.map((d) => d.id)
                },
                tit
              );
            }
          });
        }
      },
      {
        key: 'delete',
        disabled: selectedRows.length === 0,
        callback: () => {
          Modal.confirm({
            title: this.formatMessage(messages.confirm_delete_tit),
            content: this.formatMessage(messages.confirm_delete_content),
            onOk: () => {
              const deleteId = selectedRows.map((d) => d.id).join(',');
              this.handleDelete(deleteId);
            }
          });
        }
      }
    ];
  };

  initColumns = () => {
    return [
      {
        title: this.formatMessage(messages.title_template),
        dataIndex: 'templateName',
        // filterType: 'search',
        width: 200
      },
      {
        title: this.formatMessage(messages.title_code),
        dataIndex: 'templateCode',
        // filterType: 'search',
        width: 150
      },
      {
        title: this.formatMessage(messages.title_enabled),
        dataIndex: 'enabled',
        width: 100,
        render: (text = 0) => {
          const states = [
            {},
            {
              text: messages.enabled_state_Published,
              color: '#0066FF'
            },
            { text: messages.enabled_state_Unpublished, color: '#FF0000' },
            { text: messages.enabled_state_revision, color: '#FF9900' },
            {
              text: messages.enabled_state_stop,
              color: ''
            }
          ];
          return (
            <span style={{ color: states[text].color }}>
              {this.formatMessage(states[text].text)}
            </span>
          );
        }
      },
      {
        title: this.formatMessage(messages.title_label),
        dataIndex: 'labelNames',
        // filterType: 'search',
        width: 150
      },
      {
        title: this.formatMessage(messages.title_descrip),
        dataIndex: 'templateDesc',
        // filterType: 'search',
        width: 250
      },
      {
        title: this.formatMessage(messages.title_operate),
        dataIndex: 'operation',
        type: 'operation',
        width: 220,
        render: (text, record) => (
          <div className={style['table-row-option']}>
            <span
              onClick={() => {
                const { pageDatas = [] } = record;
                const { code } = this.props.selectedNode;
                window.open(
                  `#/reporterDesigner?isRuntime=0&templateId=${
                    record.id
                  }&appcode=${code}&code=${
                    pageDatas[0] ? pageDatas[0].modelCode : ''
                  }&name=${encodeURIComponent(record.templateName)}`
                );
              }}
            >
              {this.formatMessage(messages.btn_template)}
            </span>
            <span
              onClick={() => {
                this.getDetail(
                  record,
                  this.formatMessage(messages.btn_alignPage),
                  'alignPage'
                );
              }}
            >
              {this.formatMessage(messages.btn_alignPage)}
            </span>
            <span
              onClick={() => {
                this.getDetail(record, this.formatMessage(messages.btn_edit));
              }}
            >
              {this.formatMessage(messages.btn_edit)}
            </span>
            <span
              onClick={() => {
                this.getDetail(
                  record,
                  this.formatMessage(messages.btn_copy),
                  'copy'
                );
              }}
            >
              {this.formatMessage(messages.btn_copy)}
            </span>
            <span
              onClick={() => {
                Modal.confirm({
                  title: this.formatMessage(messages.confirm_delete_tip, {
                    name: record.templateName
                  }),
                  content: this.formatMessage(messages.confirm_delete_content),
                  onOk: () => {
                    this.handleDelete(record.id);
                  }
                });
              }}
            >
              {this.formatMessage(messages.btn_delete)}
            </span>
          </div>
        )
      }
    ];
  };

  getTableData = (selectedNode = this.props.selectedNode) => {
    const { code } = selectedNode;
    const { pagination } = this.state;
    tableData({ code, pagination }).then((res) => {
      const {
        data: { list, pagination: page }
      } = res;
      this.setState({
        dataSource: list,
        pagination: page
      });
    });
  };

  // 获取关联页面
  getAlignList = (selectedNode = this.props.selectedNode) => {
    const { code } = selectedNode;
    queryAlignList({ pCode: code }).then((res) => {
      const {
        data: { data }
      } = res;
      if (data) {
        this.setState({ treeData: data });
      }
    });
  };

  // 修改状态
  switchState = (params, name) => {
    modifyState(params).then((res) => {
      if (res.status === 200) {
        message.success(this.formatMessage(messages.success_state, { name }));
        this.getTableData();
      }
    });
  };

  // 获取数据详情
  getDetail = (record = {}, tit = '', type = '') => {
    const temp = {
      displayModal: true,
      title: tit,
      type,
      formData:
        type === 'copy'
          ? { ...record, templateName: '', templateCode: '', i18nKey: '' }
          : record
    };
    this.setState({ ...temp });
  };

  handleDelete = (ids) => {
    deleteData({ id: ids }).then((res) => {
      if (res.status === 200) {
        const { pagination } = this.state;
        message.success(this.formatMessage(messages.success_delete));
        this.setState(
          { selectedRows: [], pagination: { ...pagination, current: 1 } },
          () => {
            this.getTableData();
          }
        );
      }
    });
  };

  render() {
    const { selectedNode } = this.props;
    const {
      columns,
      dataSource,
      displayModal,
      title,
      formData,
      type,
      treeData,
      pagination
    } = this.state;
    const { name } = selectedNode;
    return (
      <div style={{ height: '100%' }}>
        {formData && (
          <OperateForm
            id={formData.id}
            type={type}
            formData={formData}
            title={title}
            treeData={treeData}
            visible={displayModal}
            callback={() => {
              this.setState({ displayModal: false });
              this.getTableData();
            }}
            onCancel={() => {
              this.setState({ displayModal: false });
            }}
          />
        )}
        <SupTable
          className={style['table-content']}
          tableKey="tablePrint"
          rowKey={(record) => {
            return record.id;
          }}
          onSearch={(params) => {
            const { pagination: page } = params;
            this.setState({ pagination: page }, () => {
              this.getTableData();
            });
          }}
          onSelectItem={(keys, rows) => {
            this.setState({ selectedRows: rows });
          }}
          operationBarTitle={name}
          showSearchIcon={false}
          dataSource={dataSource}
          columns={columns}
          btnColumns={this.initBtnColumns()}
          updateColumns={(column) => {
            this.setState(column);
          }}
          pagination={pagination}
        />
      </div>
    );
  }
}
