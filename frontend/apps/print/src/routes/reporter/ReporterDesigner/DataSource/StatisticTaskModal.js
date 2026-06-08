import React, { Component } from 'react';
import * as _ from 'lodash';
import { Row, Col, Table, Select, Input, AutoComplete } from 'sup-ui';
import Modal from '../Modal/CommonModal';
// import { getStatisticsMng } from 'root/services/objectApi';
import messages from '../messages';
import styles from './DataSource.less';

const { Option } = Select;
const { Search } = Input;

export default class StatisticTaskModal extends Component {
  constructor() {
    super();
    this.state = {
      dataSource: [],
      loading: false,
      pagination: {},
      selectedTask: {}
    };
  }

  componentDidMount() {
    const { selectedTaskName } = this.props;
    this.fetchTaskInfo({
      pageNum: 1,
      selectedTaskName
    });
  }

  fetchTaskInfo = ({ pageNum, selectedTaskName }) => {
    const { taskName = '' } = this.state;
    this.setState({
      loading: true,
      taskName: selectedTaskName || taskName
    });
    // getStatisticsMng({
    //   pageNum,
    //   taskName: selectedTaskName || taskName,
    //   enableExactMatch: !!selectedTaskName
    // }).then((res) => {
    //   const { list } = res || {};
    //   const stateObj = {
    //     loading: false,
    //     dataSource: [],
    //     pagination: _.get(res, 'pagination')
    //   };
    //   if (list && list.length) {
    //     const { selectedType, selectedSource } = this.props;
    //     const selectedTask = _.find(list, { taskName: selectedTaskName });
    //     stateObj.dataSource = list;

    //     if (selectedTask) {
    //       stateObj.selectedTask = { taskName: selectedTask.taskName, type: selectedType, sourceItem: selectedSource };
    //       stateObj.selectedRowKeys = [selectedTask.taskName];
    //     }
    //   }
    //   this.setState(stateObj);
    // });
  }

  setTaskState = ({ taskName, type, sourceItem }) => {
    this.setState({
      selectedTask: { taskName, type, sourceItem },
      selectedRowKeys: [taskName]
    });
  }

  onSearch = () => {
    this.fetchTaskInfo({ pageNum: 1 });
  };

  handleSearch = (e) => {
    this.setState({ taskName: e.target.value });
  };

  handleTableChange = (pagination) => {
    const pager = { ...this.state.pagination };
    pager.current = pagination.current;
    this.setState({
      taskName: '',
      pagination: pager
    }, () => {
      this.fetchTaskInfo({
        pageNum: pagination.current
      });
    });
  };

  handleTypeChange = (record, type) => {
    const { taskName, source } = record;
    const { selectedTask: { sourceItem, taskName: selectedTaskName } = {} } = this.state;
    this.setTaskState({
      taskName,
      type,
      sourceItem: taskName === selectedTaskName ? sourceItem : source[0]
    });
  }

  handleSourceChange = (record, sourceItem) => {
    const { taskName, statisticalType } = record;
    const { selectedTask: { type, taskName: selectedTaskName } = {} } = this.state;
    this.setTaskState({
      taskName,
      type: taskName === selectedTaskName ? type : statisticalType[0],
      sourceItem
    });
  }

  handleCancel = () => {
    this.props.showOrHideModal(false);
  }

  handleOk = () => {
    const { selectedTask, selectedRowKeys, dataSource } = this.state;
    if (selectedRowKeys && selectedRowKeys.length) {
      const object = _.find(dataSource, item => item.taskName === selectedRowKeys[0]);
      object.type = selectedTask.taskName === selectedRowKeys[0] ? selectedTask.type : object.statisticalType[0];
      object.sourceItem = selectedTask.taskName === selectedRowKeys[0] ? selectedTask.sourceItem : object.source[0];
      this.props.setObject(object);
    } else {
      this.props.setObject();
    }
    this.handleCancel();
  }

  getStatisticalType = (key) => {
    switch (key) {
      case 'INS': return '瞬时值';
      case 'INT': return '瞬时时间';
      case 'AVG': return '平均值';
      case 'INV': return '积分值';
      case 'DIF': return '差值';
      case 'MAX': return '最大值';
      case 'MIN': return '最小值';
      case 'MAT': return '最大值出现时间';
      case 'MIT': return '最小值出现时间';
      case 'DOS': return '状态持续时间';
      case 'COS': return '状态发生次数';
      case 'POS': return '状态发生率';
      default: break;
    }
  }

  renderTaskTable() {
    const { selectedTaskName, selectedSource, selectedType } = this.props;
    const { dataSource, selectedRowKeys } = this.state;
    const columns = [
      {
        title: '任务名称',
        key: 'taskName',
        dataIndex: 'taskName',
        width: '20%',
        render: (text, { taskName }) => (
          <span title={taskName}>{taskName}</span>
        )
      },
      {
        title: '统计源',
        key: 'source',
        dataIndex: 'source',
        width: '35%',
        render: (text, record) => (
          <AutoComplete
            style={{ width: '100%' }}
            size="small"
            dataSource={record.source}
            placeholder={record.taskName === selectedTaskName ? selectedSource : record.source[0]}
            // defaultValue={record.taskName === selectedTaskName ? selectedSource : record.source[0]}
            onChange={this.handleSourceChange.bind(this, record)}
            filterOption={(inputValue, option) =>
              option.props.children.toUpperCase().indexOf(inputValue.toUpperCase()) !== -1
            }
          />
        )
      },
      {
        title: '统计类型',
        key: 'statisticalType',
        dataIndex: 'statisticalType',
        width: '35%',
        render: (text, record) => (
          <Select
            tyle={{ width: '50%' }}
            size="small"
            defaultValue={record.taskName === selectedTaskName ? selectedType : record.statisticalType[0]}
            onChange={this.handleTypeChange.bind(this, record)}
          >
            {
              _.map(record.statisticalType, (tag) => {
                return (
                  <Option value={tag} key={tag}>{this.getStatisticalType(tag)}</Option>
                );
              })
            }
          </Select>
        )
      }
    ];
    const rowSelection = {
      type: 'radio',
      selectedRowKeys,
      onChange: this.onSelectChange
    };

    return (
      <Table
        className={styles.statistic}
        rowSelection={rowSelection}
        rowKey={(record) => record.taskName}
        size="middle"
        columns={columns}
        dataSource={dataSource}
        pagination={this.state.pagination}
        loading={this.state.loading}
        onChange={this.handleTableChange}
        onRow={(record) => {
          return {
            onClick: () => {
              if (record) {
                this.onSelectChange([record.taskName]);
              }
            }
          };
        }}
      />
    );
  }

  onSelectChange = (selectedRowKeys) => {
    this.setState({ selectedRowKeys });
  };

  renderSearchTask = () => {
    const { taskName = '' } = this.state;
    return (
      <Row type="flex" align="middle" style={{ marginBottom: 10 }}>
        <Col span={10}>
          <Search
            placeholder="请输入任务名称"
            style={{ width: 200 }}
            onSearch={this.onSearch}
            onChange={this.handleSearch}
            value={taskName}
          />
        </Col>
      </Row>
    );
  }

  render() {
    const { intl } = this.props;
    return (
      <Modal
        visible
        width="670px"
        bodyStyle={{ height: 400 }}
        title="实时数据统计任务"
        onCancel={this.handleCancel}
        onOk={this.handleOk}
        okText={intl.formatMessage(messages.ok)}
        cancelText={intl.formatMessage(messages.cancel)}
      >
        {this.renderSearchTask()}
        {this.renderTaskTable()}
      </Modal>
    );
  }
}
