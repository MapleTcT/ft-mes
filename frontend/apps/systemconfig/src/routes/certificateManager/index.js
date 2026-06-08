/*
 * @Author: DWP
 * @Date: 2020-11-04 13:35:24
 * @LastEditors: DWP
 * @LastEditTime: 2021-03-02 11:11:09
 */

import React, { Component } from 'react';
import { Layout, Modal, Form, Input, message, Button } from 'sup-ui';
import { injectIntl } from 'react-intl';
import moment from 'moment';
import { downFile } from 'root/utils';
import SupTable from 'sup-rc-table';
import * as akskApi from 'root/services/aksk';
import messages from './messages';
import styles from './index.less';

const { Header } = Layout;
const { TextArea } = Input;
const { confirm } = Modal;

@injectIntl
@Form.create()
class CertificateManager extends Component {
  constructor(props) {
    super(props);

    const { intl } = props;

    this.btnColumns = [
      {
        key: 'add',
        content: intl.formatMessage(messages.add),
        callback: () => this.toggleVisible({}, 'add')
      },
      {
        key: 'delete',
        disabled: () => { return this.state.selectedRowKeys.length === 0; },
        callback: () => this.handleDelete()
      }
    ];

    const columns = [
      {
        title: 'APP ID',
        width: 300,
        dataIndex: 'appId'
      },
      {
        title: intl.formatMessage(messages.createTime),
        width: 300,
        dataIndex: 'createTime',
        render: (text) => {
          return (
            <span title={moment(text).format('YYYY-MM-DD HH:mm:ss')}>{moment(text).format('YYYY-MM-DD HH:mm:ss')}</span>
          );
        }
      },
      {
        title: intl.formatMessage(messages.desc),
        width: 200,
        dataIndex: 'description'
      },
      {
        title: intl.formatMessage(messages.operation),
        dataIndex: 'operation',
        type: 'operation',
        width: 200,
        render: (text, record) => {
          return (
            <div>
              <a onClick={() => this.handleDelete(record)}>{intl.formatMessage(messages.delete)}</a>
            </div>
          );
        }
      }
    ];

    this.state = {
      columns,
      dataSource: [],
      pagination: {
        current: 1,
        pageSize: 20,
        total: 0
      },
      filters: {},
      selectedRowKeys: [],
      modalData: {},
      modalType: '',
      downloadId: '',
      downloadVisible: false
    };
  }

  componentDidMount() {
    // 首次加载根据是否显示横向滚动条来控制操作栏是否固定
    if (this.table && this.table.changeOperationFixedStatus) {
      this.table.changeOperationFixedStatus();
    }

    this.getList();
  }

  getList = () => {
    const { filters, pagination } = this.state;

    akskApi.getList({
      ...pagination,
      ...filters
    }).then((res) => {
      const { data: { list, pagination: newPage } } = res;
      this.setState({
        dataSource: list,
        pagination: newPage
      });
    });
  }

  // 筛选
  handleTableSearch = (params) => {
    this.setState({
      pagination: {
        current: 1,
        pageSize: 20,
        total: 0
      },
      ...params,
      selectedRowKeys: []
    }, () => {
      this.getList();
    });
  }

  // 拖拽
  updateColumns = (params) => {
    this.setState({
      columns: params
    });
  }

  // 选择条目
  handleSelectItems = (selectedRowKeys) => {
    this.setState({
      selectedRowKeys
    });
  }

  toggleVisible = (modalData = {}, modalType = '') => {
    const { visible } = this.state;
    this.setState({
      modalData,
      visible: !visible
    }, () => {
      this.setState({
        modalType
      });
    });
  }

  // 显隐下载Modal
  toggleDownloadVisible = (id) => {
    const { downloadVisible } = this.state;
    this.setState({
      downloadId: id,
      downloadVisible: !downloadVisible
    });
  }

  handleOk = () => {
    const { intl } = this.props;
    const { modalType } = this.state;
    const { validateFieldsAndScroll } = this.props.form;

    validateFieldsAndScroll((err, values) => {
      if (err) return;

      akskApi[`${modalType}Aksk`]({
        ...values
      }).then((res) => {
        const { data: { data: { id } } = {} } = res;
        message.success(intl.formatMessage(messages[`${modalType}Success`]));
        this.toggleVisible();
        this.getList();
        // 新增时显示下载窗口
        if (modalType === 'add') {
          this.toggleDownloadVisible(id);
        }
      });
    });
  }

  // 删除
  handleDelete = (record) => {
    const { intl } = this.props;
    const { selectedRowKeys, dataSource, pagination } = this.state;
    let name = '';
    let ids = [];

    // 单个删除
    if (record) {
      name = record.appId;
      ids = [record.id];
    } else {
      // 批量删除
      ids = selectedRowKeys;
    }

    confirm({
      title: `${record ? intl.formatMessage(messages.deleteTip, { name }) : intl.formatMessage(messages.batchDeleteTip)}?`,
      content: intl.formatMessage(messages.deleteTipContentTip),
      onOk: () => {
        akskApi.deleteAksk({
          ids
        }).then(() => {
          message.success(intl.formatMessage(messages.deleteSuccess));

          // 检测当前页是否还存在数据,不存在则往前一页搜索
          const newPagination = _.cloneDeep(pagination);
          if (dataSource.length === ids.length) {
            newPagination.current = newPagination.current === 1 ? 1 : newPagination.current - 1;
          }

          // 单个删除时,如果已选中,则取消该选中项
          let newSelectedRowKeys = _.cloneDeep(selectedRowKeys);
          if (record && newSelectedRowKeys.includes(record.id)) {
            const index = newSelectedRowKeys.indexOf(record.id);
            newSelectedRowKeys.splice(index, 1);
          } else if (!record) {
            // 批量删除.清空选中项
            newSelectedRowKeys = [];
          }

          this.setState({
            selectedRowKeys: newSelectedRowKeys,
            pagination: newPagination
          }, () => {
            this.getList();
          });
        });
      }
    });
  }

  // 下载
  handleDownload = () => {
    const { downloadId } = this.state;
    akskApi.downfile({
      id: downloadId
    }).then((res) => {
      const { headers, data } = res;
      downFile(headers, data);
      this.toggleDownloadVisible();
    });
  }

  render() {
    const { intl } = this.props;
    const { columns, dataSource, pagination, modalType, visible, modalData, downloadVisible } = this.state;
    const { getFieldDecorator } = this.props.form;

    return (
      <Layout className={styles.layout}>
        <Header className={styles.header}>
          {intl.formatMessage(messages.certificateManager)}
        </Header>
        <div className={styles.content}>
          <SupTable
            ref={(ref) => { this.table = ref; }}
            rowKey="id"
            btnColumns={this.btnColumns}
            showSearchIcon={false}
            showColumnsFilter={false}
            columns={columns}
            dataSource={dataSource}
            pagination={pagination}
            updateColumns={this.updateColumns}
            onSearch={this.handleTableSearch}
            onSelectItem={this.handleSelectItems}
            onDoubleClick={(record) => this.handleDoubleClick(record)}
          />
        </div>
        <Modal
          destroyOnClose
          visible={visible}
          title={modalType ? intl.formatMessage(messages[modalType]) : ''}
          onOk={this.handleOk}
          onCancel={() => { this.toggleVisible(); }}
        >
          <Form>
            <Form.Item style={{ display: 'none' }}>
              {
                getFieldDecorator('id', {
                  initialValue: modalData.id
                })(
                  <Input />
                )
              }
            </Form.Item>
            <Form.Item label="APP ID">
              {
                getFieldDecorator('appId', {
                  initialValue: modalData.appId,
                  rules: [
                    {
                      required: true,
                      message: intl.formatMessage(messages.enterAppIdTip)
                    }
                  ]
                })(
                  <Input
                    disabled={modalType === 'edit'}
                  />
                )
              }
            </Form.Item>
            <Form.Item label={intl.formatMessage(messages.desc)}>
              {
                getFieldDecorator('description', {
                  initialValue: modalData.description
                })(
                  <TextArea />
                )
              }
            </Form.Item>
          </Form>
        </Modal>
        <Modal
          title={intl.formatMessage(messages.download)}
          visible={downloadVisible}
          onCancel={() => this.toggleDownloadVisible()}
          footer={
            <Button type="primary" onClick={this.handleDownload}>
              {intl.formatMessage(messages.download)}
            </Button>
          }
        >
          <span>{intl.formatMessage(messages.downloadTip)}</span>
        </Modal>
      </Layout>
    );
  }
}

export default CertificateManager;
