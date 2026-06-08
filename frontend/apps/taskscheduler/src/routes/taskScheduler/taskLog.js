import React from 'react';
import { Control } from 'react-keeper';
import SupTable from 'sup-rc-table';
import { injectIntl } from 'react-intl';
import { Button } from 'sup-ui';
import style from './style.less';
import messages from './messages';
import { queryTaskLog } from '../../services/taskServer';

class TaskLog extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.state = {
      dataSource: [],
      total: 50,
      current: 1,
      pageSize: 20,
      columns: [
        {
          title: intl.formatMessage(messages.taskName),
          width: 100,
          dataIndex: 'jobName'
        },
        {
          title: intl.formatMessage(messages.taskCode),
          width: 100,
          dataIndex: 'code'
        },
        {
          title: intl.formatMessage(messages.modalName),
          dataIndex: 'modelName',
          width: 100
        },
        {
          title: intl.formatMessage(messages.interfaceUrl),
          dataIndex: 'serviceApi',
          width: 180
        },
        {
          title: intl.formatMessage(messages.interfaceStatus),
          dataIndex: 'jobStatus',
          width: 100,
          render: this.renderJonStatus
        },
        {
          title: intl.formatMessage(messages.taskParams),
          dataIndex: 'serviceParams',
          width: 180
        },
        {
          title: intl.formatMessage(messages.taskInfo),
          dataIndex: 'jobMessage',
          width: 180
        },
        {
          title: intl.formatMessage(messages.abnormalInfo),
          dataIndex: 'exceptionInfo',
          width: 180
        },
        {
          title: intl.formatMessage(messages.createTime),
          sorter: true,
          dataIndex: 'createTime',
          width: 100
        }
      ]
    };
    this.searchColumns = [
      {
        key: 'jobName',
        title: intl.formatMessage(messages.taskName),
        type: 'input',
        defaultValue: Control.state ? Control.state.jobName : '',
        placeholder: `${intl.formatMessage(messages.pleaseEnter)}${intl.formatMessage(messages.taskName)}`,
        span: 6
      },
      {
        key: 'code',
        title: intl.formatMessage(messages.taskCode),
        type: 'input',
        defaultValue: Control.state ? Control.state.code : '',
        placeholder: `${intl.formatMessage(messages.pleaseEnter)}${intl.formatMessage(messages.taskCode)}`,
        span: 6
      },
      {
        key: 'modelName',
        title: intl.formatMessage(messages.modalName),
        type: 'input',
        defaultValue: Control.state ? Control.state.modelName : '',
        placeholder: `${intl.formatMessage(messages.pleaseEnter)}${intl.formatMessage(messages.modalName)}`,
        span: 6
      },
      {
        key: 'serviceApi',
        title: intl.formatMessage(messages.interfaceUrl),
        type: 'input',
        defaultValue: Control.state ? Control.state.serviceApi : '',
        placeholder: `${intl.formatMessage(messages.pleaseEnter)}${intl.formatMessage(messages.interfaceUrl)}`,
        span: 6
      },
      {
        key: 'jobStatus',
        title: intl.formatMessage(messages.interfaceStatus),
        type: 'select',
        placeholder: `${intl.formatMessage(messages.pleaseSelect)}`,
        span: 6,
        options: [
          { label: '成功', value: 2 },
          { label: '失败', value: 3 }
        ]
      }
    ];
  }

  componentWillReceiveProps(nextProps) {
    this.initTable(nextProps);
  }

  componentWillMount() {
    if (Control.state) {
      const { jobName, modelName, serviceApi, code } = Control.state;
      const search = {
        jobName,
        modelName,
        serviceApi,
        code
      };
      this.initTable(search);
    } else this.initTable();
  }

  initTable = (inputSearch = {}) => {
    const { current, pageSize } = this.state;
    let search = {};
    search = {
      current,
      pageSize,
      ...inputSearch
    };
    if (!Control.state) {
      search.fuzzySearch = true;
    }
    queryTaskLog(search).then((res) => {
      const { list, pagination } = res.data;
      this.setState({
        total: pagination.total,
        current: pagination.current,
        pageSize: pagination.pageSize,
        dataSource: list
      });
    });
  }

  updateColumns = (columns) => {
    this.setState({
      columns
    });
  }

  renderJonStatus = (_, record) => {
    const jobStatusArr = [
      { label: '成功', value: 2 },
      { label: '失败', value: 3 }
    ];
    return (
      <div>
        {jobStatusArr.find((item) => item.value === record.jobStatus).label}
      </div>
    );
  }

  handleTableSearch = (params) => {
    const { sorter, search, pagination } = params;
    const { current, pageSize } = pagination;
    let inputSorter;
    if (Object.keys(sorter).length > 0) {
      inputSorter = `${Object.keys(sorter)[0]}_${sorter[Object.keys(sorter)[0]]}`;
    }
    const searchCondition = {};
    Object.entries(search).map((item) => {
      if (item[1] && item[1] !== '') {
        const item0 = item[0];
        const item1 = item[1];
        searchCondition[item0] = item1;
      }
      return searchCondition;
    });
    const inputSearch = Object.assign(searchCondition, {
      current,
      pageSize,
      sorter: inputSorter
    });
    this.initTable(inputSearch);
  }

  render() {
    const { columns, dataSource, total, current, pageSize } = this.state;
    // const { intl } = this.props;
    return (
      <>
        {
          Control.state ? (
            <Button
              onClick={() => { Control.go(-1); }}
              className={style.backBtn}
            >
              返回
            </Button>
          ) : null
        }
        <div className={style.logContent}>
          <SupTable
            ref={(ref) => { this.table = ref; }}
            // rowKey="key"
            columns={columns}
            updateColumns={this.updateColumns}
            showColumnsFilter={false}
            showSelection={false}
            onSearch={this.handleTableSearch}
            dataSource={dataSource}
            searchColumns={this.searchColumns}
            pagination={{
              total,
              current,
              pageSize
            }}
          />
        </div>
      </>
    );
  }
}

export default injectIntl(TaskLog);
