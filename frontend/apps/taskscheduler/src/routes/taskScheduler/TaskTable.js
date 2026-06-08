import React from 'react';
import { Modal, Divider, message } from 'sup-ui';
import SupTable from 'sup-rc-table';
import { Control } from 'react-keeper';
import { injectIntl } from 'react-intl';
import Highlighter from 'react-highlight-words';
import GlobalSearch from './GlobalSearch';
import WrappedAddForm from './addForm';
import style from './style.less';
import messages from './messages';
import {
  taskDetail,
  addJob,
  updateJop,
  updateTrigger,
  removeTask,
  startTask,
  stopTask,
  immediateExcutionTask
} from '../../services/taskServer';
import { EMPTY_SELECT_NODE } from './constant';

class TaskTable extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.taskForm = React.createRef();
    this.state = {
      isSubmit: false, // 标识不能重复提交
      modalVisible: false,
      dataSource: [],
      total: 50,
      current: 1,
      pageSize: 20,
      btnColumns: [
        {
          key: 'add',
          content: intl.formatMessage(messages.addTask),
          disabled: false,
          callback: this.handleAddModal
        },
        {
          key: 'menu',
          content: intl.formatMessage(messages.batchOpetation),
          menu: [
            {
              key: 'batchStart',
              content: intl.formatMessage(messages.batchStart),
              disabled: true,
              callback: this.handleBatchStart
            },
            {
              key: 'batchStop',
              content: intl.formatMessage(messages.batchStop),
              disabled: true,
              callback: this.handlebatchStop
            },
            {
              key: 'delete',
              content: intl.formatMessage(messages.batchDel),
              disabled: true,
              callback: this.handleDelTask
            }
          ],
          callback: (param) => {
            if (param.callback) {
              param.callback();
            }
          }
        }
      ],
      modalTitle: '',
      selectedRowKeys: [],
      modalType: false,
      searchConditions: {},
      columns: [
        {
          title: intl.formatMessage(messages.taskName),
          width: 180,
          dataIndex: 'jobName'
        },
        {
          title: intl.formatMessage(messages.taskCode),
          width: 180,
          dataIndex: 'code'
        },
        {
          title: intl.formatMessage(messages.modalName),
          dataIndex: 'modelName',
          sorter: true,
          width: 180
        },
        {
          title: intl.formatMessage(messages.interfaceUrl),
          dataIndex: 'serviceApi',
          width: 180
        },
        {
          title: intl.formatMessage(messages.croExpression),
          dataIndex: 'jobCron',
          width: 180
        },
        {
          title: intl.formatMessage(messages.taskParams),
          dataIndex: 'serviceParams',
          width: 180
        },
        {
          title: intl.formatMessage(messages.taskDetail),
          dataIndex: 'jobDesc',
          width: 180
        },
        {
          title: intl.formatMessage(messages.taskStatus),
          dataIndex: 'jobStatus',
          width: 100,
          render: this.renderJonStatus
        },
        {
          title: intl.formatMessage(messages.callNumber),
          sorter: true,
          dataIndex: 'callNo',
          width: 100
        },
        {
          title: intl.formatMessage(messages.lastCallTime),
          sorter: true,
          dataIndex: 'lastTime',
          width: 280
        },
        {
          title: intl.formatMessage(messages.nextCallTime),
          sorter: true,
          dataIndex: 'nextTime',
          width: 280
        },
        {
          title: intl.formatMessage(messages.operation),
          dataIndex: 'operation',
          width: 220,
          type: 'operation',
          render: this.renderOperateCol
        }
      ]
    };
    this.addCustomHighlightRender();
  }

  addCustomHighlightRender() {
    this.state.columns.forEach((c) => {
      if (!c.render) {
        const colKey = c.dataIndex;
        c.render = (text) => {
          return this.customHighlightRender(colKey, text);
        };
      }
    });
  }

  customHighlightRender = (dataIndex, text) => {
    const { searchConditions = {} } = this.state;
    const searchVal = searchConditions[dataIndex];
    return searchVal ? (
      <Highlighter
        // FIXME 样式和sup-rc-table同步
        highlightStyle={{ color: '#fa6400', backgroundColor: 'inherit' }}
        searchWords={[searchVal]}
        autoEscape
        textToHighlight={(text || '').toString()}
      />
    ) : (
      text
    );
  };

  componentDidMount() {
    this.props.onRefs(this);
  }

  refreshAddBtnState() {
    const { activeId } = this.props;
    let disabled = true;
    if (activeId && activeId !== EMPTY_SELECT_NODE) {
      disabled = false;
    }
    if (this.state.btnColumns[0].disabled !== disabled) {
      this.state.btnColumns[0].disabled = disabled;
      this.setState({});
    }
  }

  initBtn = () => {
    const { intl } = this.props;
    const { selectedRowKeys } = this.state;
    this.setState({
      btnColumns: [
        {
          key: 'add',
          content: intl.formatMessage(messages.addTask),
          disabled: false,
          callback: this.handleAddModal
        },
        {
          key: 'menu',
          content: intl.formatMessage(messages.batchOpetation),
          menu: [
            {
              key: 'batchStart',
              content: intl.formatMessage(messages.batchStart),
              disabled: selectedRowKeys.length === 0,
              callback: this.handleBatchStart
            },
            {
              key: 'batchStop',
              content: intl.formatMessage(messages.batchStop),
              disabled: selectedRowKeys.length === 0,
              callback: this.handlebatchStop
            },
            {
              key: 'delete',
              content: intl.formatMessage(messages.batchDel),
              disabled: selectedRowKeys.length === 0,
              callback: this.handleDelTask
            }
          ],
          callback: (param) => {
            if (param.callback) {
              param.callback();
            }
          }
        }
      ]
    });
  };

  initTable = (props, inputSearch = {}) => {
    const { moduleCode } = this.props;
    const { current, pageSize, searchConditions } = this.state;
    let search = {};
    search = {
      current,
      pageSize,
      ...inputSearch,
      moduleCode,
      ...searchConditions
    };
    this.handleTaskDetail(search);
  };

  handleTaskDetail = (search) => {
    const { intl } = this.props;
    taskDetail(search).then(
      (res) => {
        const { list, pagination } = res.data;
        this.setState({
          total: pagination.total,
          current: pagination.current,
          pageSize: pagination.pageSize,
          dataSource: list
        });
      },
      () => {
        message.error(intl.formatMessage(messages.errorNetwork));
      }
    );
  };

  updateColumns = (columns) => {
    this.setState({
      columns
    });
  };

  handleTableSearch = (params) => {
    const { sorter, pagination } = params;
    const { current, pageSize } = pagination;
    const { searchConditions } = this.state;
    let inputSorter;
    if (Object.keys(sorter).length > 0) {
      inputSorter = `${Object.keys(sorter)[0]}_${
        sorter[Object.keys(sorter)[0]]
      }`;
    }
    const inputSearch = Object.assign(searchConditions, {
      current,
      pageSize,
      sorter: inputSorter
    });
    this.initTable(this.props, inputSearch);
  };

  resetSubmitState = () => {
    this.setState({
      isSubmit: false
    });
  };

  handleSelectModule() {
    // 重置搜索条件
    this.GlobalSearch.handleReset();
    // this.setState({
    //   searchConditions: {}
    // }, () => {
    // });
    this.refreshList({});
  }

  handleAddForm = () => {
    this.setState({
      isSubmit: true
    });
    const taskForm = this.taskForm.current;
    let editModFunction = null;
    taskForm.validateFields().then((data) => {
      let i18nValue;
      let i18nKey;
      if (data.jobName) {
        i18nValue = data.jobName.i18nValue;
        i18nKey = data.jobName.i18nKey;
      }
      const arr = [];
      let str = '';
      if (data.jobName) {
        for (const key in i18nValue) {
          if (Object.prototype.hasOwnProperty.call(i18nValue, key)) {
            str = `${key}=${i18nValue[key]}`;
            if (key === 'zh_CN') {
              data.jobName = i18nValue[key];
            }
            arr.push(str);
          }
        }
      }
      if (this.state.modalType) {
        editModFunction = addJob;
        data.jobKey = i18nKey;
        data.jobNameInternational = arr.join('$&#');
      } else if (this.state.bol) {
        editModFunction = updateTrigger;
        data.id = this.state.taskId;
      } else {
        editModFunction = updateJop;
        data.id = this.state.taskId;
        data.jobKey = i18nKey;
        data.jobNameInternational = arr.join('$&#');
      }
      editModFunction(data).then(() => {
        this.setState({
          modalVisible: false
        });
        message.success(this.state.editTips);
        this.initTable(this.props);
      }, this.resetSubmitState);
    }, this.resetSubmitState);
  };

  handleAddModal = (...args) => {
    const { intl } = this.props;
    if (args[1] && args[1].id) {
      this.setState({
        taskId: args[1].id
      });
    }
    this.setState({
      // taskId: args[1].id,
      modalVisible: true,
      isSubmit: false,
      modalTitle: args[0].content || args[0],
      bol: args[2],
      record: args[1] || [],
      modalType: args[0].key === 'add',
      editTips:
        args[0].key === 'add'
          ? `${intl.formatMessage(messages.addSuccess)}`
          : args[2]
            ? `${intl.formatMessage(messages.modifySuccess)}`
            : `${intl.formatMessage(messages.configSuccess)}`
    });
  };

  handleCloseModal = () => {
    this.setState({
      modalVisible: false
    });
  };

  handleTaskLog = (record) => {
    const { jobName, modelName, serviceApi, code } = record;
    const state = {
      jobName,
      code,
      modelName,
      serviceApi
    };
    Control.go('/tasklog', state);
    // const w = window.open('about:black');
    // w.location.href=`/#/tasklog?jobName=${jobName}&modelName=${modelName}&serviceApi=${serviceApi}`
  };

  renderOperateCol = (_, record) => {
    const { intl } = this.props;
    const title = `${intl.formatMessage(messages.modifyTask)}`;
    const croTitle = `${intl.formatMessage(messages.croExpression)}`;
    const bol = true;
    return (
      <div>
        <a
          onClick={() => {
            this.handleAddModal(title, record);
          }}
        >
          {intl.formatMessage(messages.modify)}
        </a>
        <Divider type="vertical" />
        <a
          onClick={() => {
            this.handleAddModal(croTitle, record, bol);
          }}
        >
          {intl.formatMessage(messages.trigger)}
        </a>
        <Divider type="vertical" />
        <a
          onClick={() => {
            this.handleTaskLog(record);
          }}
        >
          {intl.formatMessage(messages.log)}
        </a>
        <Divider type="vertical" />
        <a
          onClick={() => {
            this.handleImmediateExcutionTask(record);
          }}
        >
          {intl.formatMessage(messages.oneClickExecution)}
        </a>
      </div>
    );
  };

  renderJonStatus = (_, record) => {
    const { intl } = this.props;
    const jobStatusArr = [
      { label: intl.formatMessage(messages.normalStatus), value: 0 },
      { label: intl.formatMessage(messages.stopStatus), value: 1 },
      { label: intl.formatMessage(messages.abnormalStatus), value: 2 },
      { label: intl.formatMessage(messages.awaitStatus), value: 3 }
    ];
    return (
      <div>
        {jobStatusArr.find((item) => item.value === record.jobStatus).label}
      </div>
    );
  };

  handleBatchOperation = (params) => {
    const { intl } = this.props;
    let data = {};
    data.key = this.state.selectedRowKeys;
    let batchFunction = null;
    if (params === 'stop') {
      batchFunction = stopTask;
    } else if (params === 'start') {
      batchFunction = startTask;
    } else if (params === 'del') {
      batchFunction = removeTask;
    } else {
      batchFunction = immediateExcutionTask;
      data = params;
    }

    this.setState(
      {
        confirmTitleText:
          params === 'stop'
            ? intl.formatMessage(messages.stopTask)
            : params === 'start'
              ? intl.formatMessage(messages.implementTask)
              : params === 'del'
                ? intl.formatMessage(messages.removeTask)
                : intl.formatMessage(messages.oneClickExecutionTask),
        successTip:
          params === 'stop'
            ? intl.formatMessage(messages.stopSuccess)
            : params === 'start'
              ? intl.formatMessage(messages.openSuccess)
              : params === 'del'
                ? intl.formatMessage(messages.removeSuccess)
                : intl.formatMessage(messages.immidiateSuccess)
      },
      () => {
        Modal.confirm({
          cancelText: intl.formatMessage(messages.cancel),
          okText: intl.formatMessage(messages.ok),
          title: this.state.confirmTitleText,
          onOk: () => {
            batchFunction(data).then((res) => {
              if (params === 'start' || params === 'stop') {
                if (res.data === '') {
                  message.success(this.state.successTip);
                }
              } else if (params === 'del') {
                message.success(this.state.successTip);
                this.setState({
                  selectedRowKeys: []
                });
                this.reRenderDelBtn();
              } else {
                message.success(this.state.successTip);
              }
              this.initTable(this.props);
            });
          }
        });
      }
    );
  };

  handleDelTask = () => {
    this.handleBatchOperation('del');
  };

  handleBatchStart = () => {
    this.handleBatchOperation('start');
  };

  handlebatchStop = () => {
    this.handleBatchOperation('stop');
  };

  handleImmediateExcutionTask = (record) => {
    this.handleBatchOperation(record);
  };

  tableSelectItem = (selectedRowKeys) => {
    this.setState(
      {
        selectedRowKeys
      },
      () => {
        this.reRenderDelBtn();
      }
    );
  };

  /**
   *
   * @param {boolean} clear 用于标识清空
   */
  reRenderDelBtn = (clear) => {
    const { selectedRowKeys } = this.state;
    const disabled = !!clear || selectedRowKeys.length === 0;
    this.state.btnColumns[1].menu.find(
      (item) => item.key === 'batchStart'
    ).disabled = disabled;
    this.state.btnColumns[1].menu.find(
      (item) => item.key === 'batchStop'
    ).disabled = disabled;
    this.state.btnColumns[1].menu.find(
      (item) => item.key === 'delete'
    ).disabled = disabled;
    if (!clear) {
      this.forceUpdate();
    }
  };

  refreshList(searchConditions) {
    const { intl, moduleCode } = this.props;
    const { current, pageSize, searchConditions: lastSearchConditions } = this.state;
    let params = {};
    params = {
      current,
      pageSize,
      moduleCode,
      ...(searchConditions || lastSearchConditions)
    };

    taskDetail(params).then(
      (res) => {
        const { list, pagination } = res.data;
        const nextState = {
          total: pagination.total,
          current: pagination.current,
          pageSize: pagination.pageSize,
          dataSource: list,
          selectedRowKeys: []// 重置选中项
        };
        if (searchConditions) {
          nextState.searchConditions = { ...searchConditions };
        }
        this.refreshAddBtnState();
        this.reRenderDelBtn(true);
        this.setState(nextState);
      },
      () => {
        message.error(intl.formatMessage(messages.errorNetwork));
      }
    );
  }

  handleFormSearch = (searchConditions) => {
    // 取消选择树module
    // TODO当前已取消选择不重复调用
    const { activeId } = this.props;
    if (activeId !== EMPTY_SELECT_NODE) {
      this.props.handleDeselect(() => {
        this.refreshList(searchConditions);
      });
    } else {
      this.refreshList(searchConditions);
    }
  }

  render() {
    const {
      columns,
      dataSource,
      total,
      current,
      pageSize,
      modalVisible,
      btnColumns,
      record,
      modalTitle,
      bol,
      isSubmit,
      selectedRowKeys
    } = this.state;
    const { activeName, moduleCode } = this.props;
    return (
      <>
        <Modal
          title={modalTitle}
          visible={modalVisible}
          onOk={this.handleAddForm}
          onCancel={this.handleCloseModal}
          destroyOnClose
          maskClosable={false}
          wrapClassName={style.addFormWrap}
          okButtonProps={{ disabled: isSubmit }}
        >
          <WrappedAddForm
            ref={this.taskForm}
            record={record}
            bol={bol}
            activeName={activeName}
            code={moduleCode}
            isEdit
          />
        </Modal>
        <div className={style.contentBox}>
          <GlobalSearch
            handleSearch={this.handleFormSearch}
            onRef={(ref) => {
              this.GlobalSearch = ref;
            }}
          />
          <SupTable
            style={{ height: 'calc(100% - 214px)' }}
            rowKey="id"
            columns={columns}
            selectedRowKeys={selectedRowKeys}
            updateColumns={this.updateColumns}
            showSearchIcon={false}
            onSearch={this.handleTableSearch}
            dataSource={dataSource}
            searchColumns={this.searchColumns}
            btnColumns={btnColumns}
            pagination={{
              total,
              current,
              pageSize
            }}
            onSelectItem={this.tableSelectItem}
          />
        </div>
      </>
    );
  }
}

export default injectIntl(TaskTable);
