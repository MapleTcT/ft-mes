import React, { Component } from 'react';
import { injectIntl } from 'react-intl';
import moment from 'moment';
import { Layout, Button, Modal, Input, message, Radio, Form } from 'sup-ui';
import SupTable from 'sup-rc-table';
import * as commonApi from '../../services/commonApi';
import * as blackWhiteApi from '../../services/blackWhiteApi';
import messages from './messages.js';
import styles from './index.less';

const { Header } = Layout;
const { confirm } = Modal;

@injectIntl
@Form.create()
class OnlineManage extends Component {
  constructor(props) {
    super(props);
    const { intl } = props;

    this.searchColumns = [
      {
        key: 'ip',
        type: 'input',
        placeholder: intl.formatMessage(messages.enterIP),
        span: 6
      }
    ];

    this.btnColumns = [
      {
        key: 'add',
        disabled: () => { return this.state.type === ''; },
        callback: () => { this.toggleVisible(); }
      }
    ];

    const columns = [
      {
        title: intl.formatMessage(messages.no),
        dataIndex: 'no',
        width: 60
      },
      {
        title: intl.formatMessage(messages.ip),
        dataIndex: 'ip',
        width: 300
      },
      {
        title: intl.formatMessage(messages.createTime),
        dataIndex: 'createTime',
        width: 300,
        render: (text) => {
          return (
            <span>{moment(text).format('YYYY-MM-DD HH:mm:ss')}</span>
          );
        }
      },
      {
        title: intl.formatMessage(messages.operate),
        dataIndex: 'operate',
        type: 'operation',
        width: 100,
        fixed: true,
        authority: () => { return this.state.authorityList.includes('IpConfig'); },
        render: (text, record) => {
          return (
            <a onClick={() => this.checkoutIP(record, 'delete')}>
              {intl.formatMessage(messages.delete)}
            </a>
          );
        }
      }
    ];

    this.state = {
      columns,
      visible: false,
      ip: '',
      type: '',
      dataSource: [],
      pagination: {
        total: 0,
        current: 1,
        pageSize: 50
      },
      authorityList: []
    };
  }

  componentDidMount() {
    this.getData();
    this.getAuthority();

    // 首次加载根据是否显示横向滚动条来控制操作栏是否固定
    if (this.table) {
      this.table.changeOperationFixedStatus();
    }
  }

  // 获取权限接口
  getAuthority = () => {
    commonApi.getAuthority({
      code: 'ip'
    }).then((res) => {
      const { data: { list } } = res;
      this.setState({
        authorityList: list
      });
    });
  }

  // 获取列表数据
  getData = (params = {}) => {
    const { intl } = this.props;
    const { pagination: { current, pageSize }, ip, type: oldType } = this.state;

    blackWhiteApi.getList({
      ip: params.ip || ip,
      current: params.current || current,
      pageSize: params.pageSize || pageSize
    }).then((res) => {
      const { data: { list, pagination } } = res;

      if (!list || list.length === 0) {
        return this.setState({
          dataSource: []
        });
      }

      const type = `${list[0].controlType}`;
      const dataSize = pagination.total;

      const blackWhiteList = list.map((item, i) => {
        item.no = i + 1;
        return item;
      });

      // 存在数据，则不能切换类型
      if (this.state.type && this.state.type !== type && dataSize !== 0) {
        message.warning(intl.formatMessage(messages.pleaseDeleteAllIPs));
      }

      this.setState({
        ip: params.ip || ip,
        type: type !== undefined ? type : oldType,
        dataSource: blackWhiteList,
        pagination
      });
    });
  }

  // 改变搜索项
  handleChangeValue = (value, type) => {
    const { intl } = this.props;
    const { dataSource } = this.state;
    const obj = {};

    // 切换模式时需要删除所有IP
    if (type === 'type' && dataSource.length) {
      return message.warning(intl.formatMessage(messages.pleaseDeleteAllIPs));
    }

    // 切换管控模式时清空搜索框
    if (type === 'type') {
      obj.ip = '';
    }

    this.setState({
      [type]: value,
      ...obj
    }, () => {
      if (type === 'type') {
        this.getData({ current: 1 });
      }
    });
  }

  // 搜索
  handleSearch = ({ search: { ip }, pagination }) => {
    this.setState({
      ip,
      pagination
    }, () => {
      this.getData();
      this.setState({
        pagination
      });
    });
  }

  checkoutIP = (params, optType) => {
    const { type } = this.state;

    // 黑名单删除
    if (type === '0' && optType === 'delete') {
      this.handleDelete(params, false);
    } else {
      blackWhiteApi.checkIncludeSelf({
        controlType: type - 0,
        ip: params.ip,
        operateType: optType === 'add' ? 0 : 1
      }).then((res) => {
        const { data: { data } } = res;

        // 新增
        if (optType === 'add') {
          // 显示提示框
          if (data) {
            this.addConfirm(params);
          } else {
            this.addIP(params);
          }
        } else {
          // 删除白名单
          this.handleDelete(params, data);
        }
      });
    }
  }

  // 显隐新增Modal
  toggleVisible = () => {
    const { visible } = this.state;
    this.setState({
      visible: !visible
    });
  }

  // 新增按钮
  handleAddIP = () => {
    const { form } = this.props;
    form.validateFieldsAndScroll((err, values) => {
      if (err) return;
      this.checkoutIP({ ...values }, 'add');
    });
  }

  // 新增的提示
  addConfirm = (params) => {
    const { type } = this.state;
    const { intl } = this.props;

    confirm({
      title: intl.formatMessage(messages.addTip, { ip: params.ip }),
      content: intl.formatMessage(messages[type === '1' ? 'whiteTip' : 'blackTip']),
      okText: intl.formatMessage(type === '1' ? messages.allow : messages.sure),
      cancelText: intl.formatMessage(type === '1' ? messages.deny : messages.cancel),
      onOk: () => {
        this.addIP(params, true);
      },
      onCancel: () => {
        if (type === '1') {
          this.addIP(params);
        }
      }
    });
  }

  addIP = (obj = {}, isAddCurrent = false) => {
    const { intl } = this.props;
    const { type } = this.state;

    blackWhiteApi.addIP({
      controlType: type - 0,
      addCurrentIp: isAddCurrent,
      ip: obj.ip
    }).then(() => {
      message.success(intl.formatMessage(messages.addSuccess));
      this.getData({ current: 1, ip: '' });
      this.toggleVisible();
    });
  }

  // 删除的提示
  handleDelete = (params, showAddSelfTip) => {
    const { intl } = this.props;
    const { pagination: { current, pageSize, total } } = this.state;
    const isLastItem = total === 1;

    confirm({
      title: intl.formatMessage(messages.confirmDelete, { ip: params.ip }),
      content: intl.formatMessage(isLastItem ? messages.lastItemTip : showAddSelfTip ? messages.includeSelfTip : messages.ifDelete),
      onOk: () => {
        blackWhiteApi.deleteIP({
          ids: params.id
        }).then(() => {
          const newCurrent = total - ((current - 1) * pageSize) === 1 ? current - 1 : current;
          this.getData({ current: newCurrent });
          if (isLastItem) {
            this.setState({
              type: ''
            });
          }
          message.success(intl.formatMessage(messages[!isLastItem && showAddSelfTip ? 'deleteAndAddSuccess' : 'deleteSuccess'], { ip: params.ip }));
        });
      }
    });
  }

  updateColumns = (columns) => {
    this.setState({
      columns
    });
  }

  render() {
    const { intl } = this.props;
    const { getFieldDecorator } = this.props.form;
    const { columns, dataSource, pagination, type, visible } = this.state;

    const modalTitle = type === '1' ? intl.formatMessage(messages.addWhiteIP) : type === '0' ? intl.formatMessage(messages.addBlackIP) : '';

    const addFooter = (
      <div
        className={styles.modalFooter}
        style={{
          padding: '0 124px'
        }}
      >
        <Button
          className={styles.sureBtn}
          type="primary"
          onClick={this.handleAddIP}
        >
          {intl.formatMessage(messages.sure)}
        </Button>
        <Button
          className={styles.cancelBtn}
          onClick={this.toggleVisible}
        >
          {intl.formatMessage(messages.cancel)}
        </Button>
      </div>
    );

    return (
      <Layout className={styles.layout}>
        <Header className={styles.header}>
          <span>{intl.formatMessage(messages.ipList)}</span>
          <Radio.Group
            className={styles.radioGroup}
            buttonStyle="solid"
            value={type}
            onChange={(e) => this.handleChangeValue(e.target.value, 'type')}
          >
            <Radio.Button value="1">{intl.formatMessage(messages.white)}</Radio.Button>
            <Radio.Button value="0">{intl.formatMessage(messages.black)}</Radio.Button>
          </Radio.Group>
        </Header>
        <div className={styles.content}>
          <SupTable
            ref={(ref) => { this.table = ref; }}
            rowKey="id"
            showSelection={false}
            controlColumns
            columns={columns}
            searchBtnPosition="normal"
            searchColumns={this.searchColumns}
            btnColumns={this.state.authorityList.includes('IpConfig') ? this.btnColumns : []}
            dataSource={dataSource}
            pagination={pagination}
            onSearch={this.handleSearch}
            updateColumns={this.updateColumns}
          />
        </div>
        <Modal
          destroyOnClose
          className={styles.modal}
          title={modalTitle}
          visible={visible}
          onCancel={this.toggleVisible}
          footer={addFooter}
          bodyStyle={{
            padding: '30px 140px'
          }}
        >
          <Form layout="vertical">
            <Form.Item label="IP">
              {
                getFieldDecorator('ip', {
                  rules: [
                    {
                      required: true,
                      message: intl.formatMessage(messages.enterIPTip)
                    }
                  ]
                })(
                  <Input
                    placeholder={intl.formatMessage(messages.addIpTip)}
                    size="small"
                  />
                )
              }
            </Form.Item>
            <span>{intl.formatMessage(messages.addIpEi)}</span>
          </Form>
        </Modal>
      </Layout>
    );
  }
}

export default OnlineManage;
