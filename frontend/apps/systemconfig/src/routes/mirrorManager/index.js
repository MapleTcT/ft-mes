/*
 * @Author: DWP
 * @Date: 2020-11-12 09:55:07
 * @LastEditors: DWP
 * @LastEditTime: 2021-03-02 11:13:02
 */
import React, { Component } from 'react';
import { Layout, Divider, Modal, Form, Input, Upload, message, Button, Icon, Spin } from 'sup-ui';
import { injectIntl } from 'react-intl';
import { downFile } from 'root/utils';
import moment from 'moment';
import SupTable from 'sup-rc-table';
import * as registryApi from 'root/services/registry';
import messages from './messages';
import styles from './index.less';

const { Header } = Layout;
const { confirm } = Modal;

@injectIntl
@Form.create()
class MirrorManager extends Component {
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
        title: intl.formatMessage(messages.mirrorName),
        width: 200,
        dataIndex: 'name'
      },
      {
        title: intl.formatMessage(messages.version),
        width: 200,
        dataIndex: 'tag'
      },
      {
        title: intl.formatMessage(messages.uploadTime),
        width: 300,
        dataIndex: 'createTime',
        render: (text) => {
          return (
            <span>{text ? moment(text).format('YYYY-MM-DD HH:mm:ss') : ''}</span>
          );
        }
      },
      {
        title: intl.formatMessage(messages.operation),
        dataIndex: 'operation',
        type: 'operation',
        width: 200,
        render: (text, record) => {
          return (
            <div>
              <a onClick={() => this.handleDownload(record)}>{intl.formatMessage(messages.download)}</a>
              <Divider type="vertical" />
              <a onClick={() => this.handleDelete(record)}>{intl.formatMessage(messages.delete)}</a>
            </div>
          );
        }
      }
    ];

    this.state = {
      loadingDownLoad: false,
      loading: false,
      visible: false,
      columns,
      dataSource: [],
      pagination: {
        current: 1,
        pageSize: 20,
        total: 0
      },
      modalData: '',
      selectedRowKeys: [],
      filters: {}
    };
  }

  componentDidMount() {
    this.getList();
  }

  getList = () => {
    const { filters, pagination } = this.state;

    registryApi.getList({
      ...pagination,
      ...filters
    }).then((res) => {
      const { data: { list, pagination: newPage } } = res;

      list.forEach((item) => {
        item.id = `${item.name}__${item.tag}`;
      });

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

  toggleVisible = (modalData = {}) => {
    const { visible } = this.state;
    this.setState({
      modalData,
      visible: !visible
    });
  }

  normFile = (e) => {
    if (Array.isArray(e)) {
      return e;
    }
    return e && [e.file];
  };

  // 轮寻检验状态
  checkStatus = (taskId, callback) => {
    if (this.statusTime) {
      clearTimeout(this.statusTime);
      this.statusTime = null;
    }

    registryApi.checkStatus({
      taskId
    }).then((res) => {
      // 1 处理中  2 成功  3 失败
      if (res.data.message === '1') {
        this.statusTime = setTimeout(() => {
          this.checkStatus(taskId, callback);
        }, 1000);
      } else {
        if (res.data.message === '2' && callback) {
          callback();
        }
        clearTimeout(this.statusTime);
        this.statusTime = null;
        this.setState({
          loading: false,
          loadingDownLoad: false
        });
      }
    });
  }

  // 上传文件
  uploadMirror = (info) => {
    if (info.file.status !== 'uploading') {
      console.log(info.file, info.fileList);
    }
    if (info.file.status === 'done') {
      const taskId = _.get(info, 'file.response.data.taskId');
      this.checkStatus(taskId);
      this.setState({
        loading: true
      });
    } else if (info.file.status === 'error') {
      message.error(`${info.file.name} file upload failed.`);
    }
  }

  // 新增
  handleOk = () => {
    const { intl } = this.props;
    const { validateFieldsAndScroll } = this.props.form;

    validateFieldsAndScroll((err, values) => {
      if (err) return;

      registryApi.addMirror({
        ...values,
        path: values.path[0].response.data.path
      }).then((res) => {
        this.checkStatus(res.data.message, () => {
          message.success(intl.formatMessage(messages.addSuccess));
          this.toggleVisible();
          this.getList();
        });
        this.setState({
          loading: true
        });
      });
    });
  }

  // 删除
  handleDelete = (record) => {
    const { intl } = this.props;
    const { selectedRowKeys, dataSource, pagination } = this.state;
    const name = record ? record.name : '';
    const params = [];

    // 单个删除
    if (record) {
      const param = record.id.split('__');
      params.push({
        name: param[0],
        tag: param[1]
      });
    } else {
      // 批量删除
      selectedRowKeys.forEach((id) => {
        const param = id.split('__');
        params.push({
          name: param[0],
          tag: param[1]
        });
      });
    }

    confirm({
      title: `${record ? intl.formatMessage(messages.deleteTip, { name }) : intl.formatMessage(messages.batchDeleteTip)}?`,
      content: intl.formatMessage(messages.deleteTipContentTip),
      onOk: () => {
        registryApi.deleteMirror({
          params
        }).then(() => {
          message.success(intl.formatMessage(messages.deleteSuccess));

          // 检测当前页是否还存在数据,不存在则往前一页搜索
          const newPagination = _.cloneDeep(pagination);
          if (dataSource.length === params.length) {
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
  handleDownload = (params) => {
    this.setState({
      loadingDownLoad: true
    });
    registryApi.startDownfile({
      name: params.name,
      tag: params.tag
    }).then((res) => {
      const { data: { message: taskId } } = res;
      this.checkStatus(taskId, () => {
        registryApi.downfile({
          taskId
        }).then((result) => {
          const { headers, data } = result;
          downFile(headers, data);
        });
        this.setState({
          loadingDownLoad: true
        });
      });
    });
  }

  render() {
    const { intl } = this.props;
    const { visible, columns, dataSource, pagination, modalData, loading, loadingDownLoad } = this.state;
    const { getFieldDecorator } = this.props.form;

    const props = {
      name: 'file',
      accept: '.tar',
      action: `${window.location.origin}/inter-api/installer/v1/registry/upload`,
      headers: {
        'Accept-Language': localStorage.getItem('language'),
        Authorization: `Bearer ${localStorage.getItem('ticket')}`
      },
      onChange: this.uploadMirror
    };

    return (
      <Layout className={styles.layout}>
        <Spin className={styles.spinBox} spinning={loadingDownLoad} />
        <Header className={styles.header}>
          {intl.formatMessage(messages.mirrorManager)}
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
          className={styles.modal}
          visible={visible}
          title={intl.formatMessage(messages.addMirror)}
          onOk={this.handleOk}
          onCancel={() => { this.toggleVisible(); }}
        >
          <Spin className={styles.spinBox} spinning={loading} />
          <Form>
            <Form.Item label={intl.formatMessage(messages.uploadFile)}>
              {
                getFieldDecorator('path', {
                  valuePropName: 'fileList',
                  getValueFromEvent: this.normFile,
                  rules: [
                    {
                      required: true,
                      message: intl.formatMessage(messages.uploadFileTip)
                    }
                  ]
                })(
                  <Upload {...props}>
                    <Button>
                      <Icon type="export" />
                      <span>{intl.formatMessage(messages.upload)}</span>
                    </Button>
                  </Upload>
                )
              }
            </Form.Item>
            <Form.Item label={intl.formatMessage(messages.mirrorName)}>
              {
                getFieldDecorator('name', {
                  initialValue: modalData.name,
                  rules: [
                    {
                      required: true,
                      message: intl.formatMessage(messages.enterTip)
                    },
                    {
                      pattern: /^[A-Za-z0-9-]*$/,
                      message: intl.formatMessage(messages.nameRuleTip)
                    }
                  ]
                })(
                  <Input placeholder={intl.formatMessage(messages.enterTip)} />
                )
              }
            </Form.Item>
            <Form.Item label={intl.formatMessage(messages.version)}>
              {
                getFieldDecorator('tag', {
                  initialValue: modalData.tag,
                  rules: [
                    {
                      required: true,
                      message: intl.formatMessage(messages.enterTip)
                    }
                  ]
                })(
                  <Input placeholder={intl.formatMessage(messages.enterTip)} />
                )
              }
            </Form.Item>
          </Form>
        </Modal>
      </Layout>
    );
  }
}

export default MirrorManager;
