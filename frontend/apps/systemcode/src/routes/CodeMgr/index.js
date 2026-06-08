import React, { Component } from 'react';
import { injectIntl } from 'react-intl';
import codeHoc from 'root/hoc/codeHoc';
import { Layout, Divider, Button, Icon, message, Spin } from 'sup-ui';
import SupTree from 'sup-rc-tree';
import SupResize from 'sup-rc-resize';
import SupSearch from 'sup-rc-search';
import Modal from 'root/components/Modal';
import CodeForm from 'root/components/CodeForm';
import CustomTable from 'root/components/CustomTable';
import * as codeApi from 'root/services/codeApi';
import * as commonApi from 'root/services/commonApi';
import messages from 'root/common/messages';
import CodeValueMgr from '../CodeValueMgr';

import styles from './index.less';

const { Content, Header } = Layout;

async function getTree() {
  const { data: { list } } = await codeApi.getTree();
  const treeData = [
    {
      key: 'default',
      title: '系统编码管理',
      disabled: true,
      children: []
    }
  ];

  list.forEach((item) => {
    treeData[0].children.push({
      key: item.moduleId,
      title: item.moduleName,
      parentId: 'default'
    });
  });

  return treeData;
}

async function getList(params) {
  const { data } = await codeApi.getList(params);

  if (data.list && data.list.length !== 0) {
    data.list.forEach((item) => {
      if (item.sysDefault) {
        item.disabled = true;
      }
    });
  }

  return data;
}

@injectIntl
@codeHoc({
  ...codeApi,
  getList,
  getTree
})
class CodeMgr extends Component {
  constructor(props) {
    super(props);

    const { intl } = props;

    this.columns = [
      {
        title: intl.formatMessage(messages.code),
        dataIndex: 'code',
        key: 'code',
        width: '20%',
        showTitle: true
      },
      {
        title: intl.formatMessage(messages.name),
        dataIndex: 'displayName',
        key: 'displayName',
        width: '20%',
        showTitle: true
      },
      {
        title: intl.formatMessage(messages.showType),
        dataIndex: 'type',
        key: 'type',
        width: '10%',
        render: (text) => {
          return (
            <span>{intl.formatMessage(messages[text])}</span>
          );
        }
      },
      {
        title: intl.formatMessage(messages.memo),
        dataIndex: 'memo',
        key: 'memo',
        showTitle: true
      },
      {
        title: intl.formatMessage(messages.operation),
        key: 'operation',
        align: 'center',
        width: 200,
        authority: () => {
          return this.props.source === 'lcdp' || this.state.authorityList.includes('editEntity') || this.state.authorityList.includes('addEntity') || this.state.authorityList.includes('queryValue') || this.state.authorityList.includes('deleteEntity');
        },
        render: (text, record) => {
          return (
            <div>
              {
                (this.props.source === 'lcdp' || this.state.authorityList.includes('editEntity'))
                && (
                  <>
                    <span
                      className={styles.operation}
                      onClick={() => { this.toggleVisible('edit', record); }}
                    >
                      {intl.formatMessage(messages.edit)}
                    </span>
                    <Divider type="vertical" />
                  </>
                )
              }
              {
                (this.props.source === 'lcdp' || this.state.authorityList.includes('queryValue') || this.state.authorityList.includes('addEntity') || this.state.authorityList.includes('editEntity')) && (
                  <span
                    className={styles.operation}
                    onClick={() => { this.toggleVisible('codeValueMgr', record); }}
                  >
                    {intl.formatMessage(messages.codeValueMgr)}
                  </span>
                )
              }
              {
                !record.sysDefault && (this.props.source === 'lcdp' || this.state.authorityList.includes('deleteEntity'))
                && (
                  <>
                    <Divider type="vertical" />
                    <span
                      className={styles.operation}
                      onClick={() => { this.props.clickBtn('singleDelete', record); }}
                    >
                      {intl.formatMessage(messages.delete)}
                    </span>
                  </>
                )
              }
            </div>
          );
        }
      }
    ];

    if (this.props.source !== 'lcdp') {
      this.columns.splice(2, 0, {
        title: intl.formatMessage(messages.owningCompany),
        dataIndex: 'companyName',
        key: 'companyName',
        width: '15%'
      });
    }

    this.state = {
      visible: false,
      btnDisabled: false,
      authorityList: []
    };
  }

  componentDidMount() {
    if (this.props.source !== 'lcdp') {
      this.getAuthority();
    }

    window.codeSearchRef = this.codeSearch;
  }

  componentWillUnmount() {
    window.codeSearchRef = null;
  }

  // 获取权限接口
  getAuthority = () => {
    commonApi.getAuthority({
      code: 'systemcode'
    }).then((res) => {
      const { data: { list } } = res;
      this.setState({
        authorityList: list
      });
    });
  }

  handleClickBtn = (type, data) => {
    if (['add', 'edit'].includes(type)) {
      this.toggleVisible(type, data);
    } else {
      this.props.clickBtn(type, data);
    }
  }

  handleChangeCodeValueType = (type) => {
    this.setState({
      modalType: type
    });
  }

  toggleVisible = (type, data) => {
    const { modalType } = this.state;
    if (['addCodeValue', 'editCodeValue'].includes(modalType)) {
      this.handleChangeCodeValueType('codeValueMgr');
    } else {
      this.setState({
        visible: !this.state.visible,
        modalType: type,
        modalData: data
      });
    }
  }

  handleSelectTree = (selectedKeys, info) => {
    const { onselecttree } = this.props;

    if (onselecttree) {
      onselecttree(selectedKeys, info);
    }

    // 列表返回顶部
    this.table.resetScrollTop();
  }

  // 点击Modal确认按钮
  submit = () => {
    const { modalType, modalData: { displayName } = {} } = this.state;
    const { intl } = this.props;

    if (!this[modalType]) return;

    this[modalType].validateFieldsAndScroll((err, values) => {
      if (err) return;

      const language = localStorage.getItem('language') || 'zh_CN';

      let newLanguage = '';
      const lan = language.split('-');
      if (lan.length === 2) {
        newLanguage = `${lan[0]}_${lan[1].toUpperCase()}`;
      }

      // 保存国际化
      const i18nParams = {
        moduleCode: values.name.moduleCode,
        i18n_key: values.name.i18nKey,
        i18n_value: values.name.i18nValue
      };

      this[modalType].instances.name.onSave(i18nParams, (res) => {
        if (JSON.stringify(res) !== '{}') return;

        values.displayName = values.name.i18nValue ? values.name.i18nValue[newLanguage] : displayName;
        values.name = values.name.i18nKey;
        values.code = modalType === 'add' ? `${values.moduleId}_${values.code}` : values.code;

        this.setState({
          btnDisabled: true
        }, () => {
          codeApi[`${modalType}Item`]({
            ...values
          }).then(() => {
            message.success(intl.formatMessage(messages.successSave));
            // 列表返回顶部
            if (modalType === 'add') {
              this.table.resetScrollTop();
            }
            this.props.submitCallback(modalType, values);
            this.toggleVisible();
            this.setState({
              btnDisabled: false
            });
          }).catch(() => {
            setTimeout(() => {
              this.setState({
                btnDisabled: false
              });
            }, 1000);
          });
        });
      });
    });
  }

  render() {
    const {
      intl,
      keyword,
      selectedRowKeys,
      selectedTreeKeys,
      selectedTreeName,
      loading,
      hasMore,
      dataSource,
      indeterminate,
      checkAll,
      onloadmore,
      oncheckall,
      onselectchange,
      onchange,
      onsearchtree,
      treeData,
      pagination,
      onsearch,
      source,
      spinning,
      className
    } = this.props;
    const { visible, modalType, modalData, btnDisabled, authorityList } = this.state;

    const isCodeValueModal = ['codeValueMgr', 'addCodeValue', 'editCodeValue'].includes(modalType);
    const width = isCodeValueModal ? modalData.type === 'tree' ? 1000 : 720 : 520;
    const height = isCodeValueModal ? 618 : 460;
    let title = '';

    if (isCodeValueModal) {
      title = (
        <>
          {
            ['addCodeValue', 'editCodeValue'].includes(modalType) && (
              <>
                <Icon type="back" onClick={() => { this.handleChangeCodeValueType('codeValueMgr'); }} />
                <Divider type="vertical" style={{ margin: '0 15px' }} />
              </>
            )
          }
          <span>{intl.formatMessage(messages[`${modalType}`])}</span>
        </>
      );
    } else if (modalType) {
      title = intl.formatMessage(modalType === 'add' ? messages.addCode : messages.editCode);
    }

    const footer = !isCodeValueModal ? (
      <div
        className={styles.modalFooter}
        style={{
          padding: '0 24px'
        }}
      >
        <Button
          className={styles.sureBtn}
          type="primary"
          onClick={this.submit}
          disabled={btnDisabled}
        >
          {intl.formatMessage(messages.sure)}
        </Button>
        <Button
          className={styles.cancelBtn}
          onClick={() => this.toggleVisible()}
        >
          {intl.formatMessage(messages.cancel)}
        </Button>
      </div>
    ) : null;

    const table = (
      <Content className={styles.content}>
        <CustomTable
          ref={(ref) => { this.table = ref; }}
          intl={intl}
          showAdd={source === 'lcdp' || authorityList.includes('addEntity')}
          showDelete={source === 'lcdp' || authorityList.includes('deleteEntity')}
          placeholder={intl.formatMessage(messages.pleaseEnterCodeName)}
          total={pagination.total}
          rowKey="code" // 唯一值
          value={keyword} // 搜索内容
          onchange={onchange} // 修改搜索内容
          noLoad
          loading={loading} // 加载更多loading
          hasMore={hasMore} // 是否可加载更多
          onloadmore={onloadmore} // 加载更多事件
          columns={this.columns} // 表头数据
          dataSource={dataSource} // 表身数据
          addDisabled={source === 'lcdp' ? false : !selectedTreeKeys || selectedTreeKeys.length === 0} // 选中左侧树
          selectedRowKeys={selectedRowKeys} // 选中数据
          indeterminate={indeterminate} // 全选的半选状态
          checkAll={checkAll} // 全选状态
          oncheckall={oncheckall} // 全选事件
          onselectchange={onselectchange} // 选中事件
          clickBtn={this.handleClickBtn}
        />
      </Content>
    );

    return (
      <Layout className={`${styles.container} ${className || ''}`}>
        {
          spinning && (
            <Spin
              className={styles.spin}
              spinning={spinning}
            />
          )
        }
        <Header className={styles.header}>
          {intl.formatMessage(messages.systemCode)}
          {
            this.props.source !== 'lcdp' && (
            <SupSearch
              ref={(ref) => { this.codeSearch = ref; }}
              className={styles.codeSearch}
              size="small"
              placeholder={intl.formatMessage(messages.pleaseEnterCodeName)}
              onSearch={onsearch}
            />
            )
          }
        </Header>
        {
          source === 'lcdp'
            ? table : (
              <SupResize>
                <SupTree
                  autoExpandRoot
                  rootId="default"
                  dataSource={treeData}
                  placeholder={intl.formatMessage(messages.appNameSearch)}
                  selectedKeys={selectedTreeKeys}
                  fuzzyParams={{
                    url: '/inter-api/module-registry/v1/modules',
                    param: 'fuzzyName',
                    callback: (res) => {
                      const { list = [] } = res;
                      return list.map((item) => {
                        return {
                          key: item.moduleId,
                          id: item.moduleId,
                          title: item.moduleName
                        };
                      });
                    }
                  }}
                  onSearch={onsearchtree}
                  onSelect={this.handleSelectTree}
                />
                { table }
              </SupResize>
            )
        }
        <Modal
          maskClosable={false}
          destroyOnClose
          title={title}
          visible={visible}
          width={width}
          bodyStyle={{
            height,
            overflowY: 'auto',
            overflowX: 'hidden'
          }}
          onCancel={() => this.toggleVisible()}
          footer={footer}
        >
          {
            isCodeValueModal
              ? (
                <CodeValueMgr
                  source={source}
                  moduleId={modalData.moduleId}
                  modalType={modalType}
                  authorityList={authorityList}
                  entityCode={modalData.code}
                  isTree={modalData.type === 'tree'}
                  changeType={this.handleChangeCodeValueType}
                />
              ) : (
                <CodeForm
                  ref={(ref) => { this[modalType] = ref; }}
                  data={{
                    ...modalData,
                    moduleId: (modalData && modalData.moduleId) || (selectedTreeKeys && selectedTreeKeys[0]),
                    moduleName: (modalData && modalData.moduleName) || selectedTreeName
                  }}
                  type={modalType}
                  intl={intl}
                  source={source}
                />
              )
          }
        </Modal>
      </Layout>
    );
  }
}

export default CodeMgr;
