import React, { Component } from 'react';
import { injectIntl } from 'react-intl';
import codeHoc from 'root/hoc/codeHoc';
import { Layout, Divider, Button, Drawer, message } from 'sup-ui';
import SupTree from 'sup-rc-tree';
import SupResize from 'sup-rc-resize';
import CodeValueForm from 'root/components/CodeValueForm';
import CustomTable from 'root/components/CustomTable';
import * as codeValueApi from 'root/services/codeValueApi';
import messages from 'root/common/messages';
import styles from './index.less';

const { Content, Footer } = Layout;

// 获取左侧树
async function getTree(params) {
  const { data: { data } } = await codeValueApi.getTree(params);
  let newData = JSON.stringify(data);

  newData = newData.replace(/"id":/g, '"key":');
  newData = newData.replace(/"displayName":/g, '"title":');
  newData = JSON.parse(newData);
  newData.key = 'null';

  const treeData = [{ ...newData }];
  return treeData;
}

async function getList({ isTree, ...params }) {
  if (params.selectedKey === 'null') {
    params.selectedKey = -1;
  }

  const { data } = await (isTree ? codeValueApi.getListByParent(params) : codeValueApi.getList(params));
  return data;
}

@injectIntl
@codeHoc({
  ...codeValueApi,
  getList,
  getTree
})
class CodeValueMgr extends Component {
  constructor(props) {
    super(props);

    const { intl, isTree } = props;

    this.columns = [
      {
        title: intl.formatMessage(messages.code),
        dataIndex: 'code',
        key: 'code',
        width: 150,
        showTitle: true
      },
      {
        title: intl.formatMessage(messages.value),
        dataIndex: 'displayName',
        key: 'displayName',
        width: 150,
        showTitle: true
      },
      {
        title: intl.formatMessage(messages.default),
        dataIndex: 'defaultFlag',
        key: 'defaultFlag',
        width: 80,
        render: (text) => {
          return (
            <span>{intl.formatMessage(messages[`${text === 1 ? 'yes' : 'no'}`])}</span>
          );
        }
      },
      {
        title: intl.formatMessage(messages.applicationRange),
        dataIndex: 'companyName',
        key: 'companyName',
        showTitle: true
      },
      {
        title: intl.formatMessage(messages.operation),
        key: 'operation',
        align: 'center',
        width: 120,
        authority: () => {
          return this.props.source === 'lcdp' || this.props.authorityList.includes('editEntity') || this.props.authorityList.includes('addEntity');
        },
        render: (text, record) => {
          return (
            <div>
              <span
                className={styles.operation}
                onClick={() => { this.toggleVisible('editCodeValue', record); }}
              >
                {intl.formatMessage(messages.edit)}
              </span>
              <Divider type="vertical" />
              <span
                className={styles.operation}
                onClick={() => { this.props.clickBtn('singleDelete', record); }}
              >
                {intl.formatMessage(messages.delete)}
              </span>
            </div>
          );
        }
      }
    ];

    if (isTree) {
      // 树状编码值展示父节点
      this.columns.splice(2, 0, {
        title: intl.formatMessage(messages.parentName),
        dataIndex: 'parentDisplayName',
        key: 'parentDisplayName',
        width: 100,
        showTitle: true,
        render: (text) => {
          return (
            <span>{text || (this.props.treeData && this.props.treeData[0] && this.props.treeData[0].title)}</span>
          );
        }
      });
    }

    this.state = {
      visible: false,
      btnDisabled: false
    };
  }

  handleClickBtn = (type, data) => {
    if (['add', 'editCodeValue'].includes(type)) {
      this.toggleVisible(type, data);
    } else {
      this.props.clickBtn(type, data);
    }
  }

  toggleVisible = (type, data) => {
    const { changeType } = this.props;
    const modalType = type === 'add' ? 'addCodeValue' : type;

    if (changeType) {
      changeType(modalType);
    }

    if (!data && this.form) {
      this.form.resetFields();
    }

    this.setState({
      visible: !this.state.visible,
      modalData: data || []
    });
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
    const { modalData: { displayName } = {} } = this.state;
    const { intl, modalType } = this.props;

    this.form.validateFieldsAndScroll((err, values) => {
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

      this.form.instances.name.onSave(i18nParams, (res) => {
        if (JSON.stringify(res) !== '{}') return;

        values.displayName = values.name.i18nValue ? values.name.i18nValue[newLanguage] : displayName;
        values.name = values.name.i18nKey;

        this.setState({
          btnDisabled: true
        }, () => {
          codeValueApi[`${modalType}`]({
            ...values
          }).then(() => {
            message.success(intl.formatMessage(messages.successSave));
            this.props.submitCallback(modalType === 'addCodeValue' ? 'add' : 'edit', values, values.defaultFlag === 1);
            this.props.getTree();
            this.toggleVisible('codeValueMgr');
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

  // 拖拽树
  handleDrop = (info) => {
    const { dragTreeNode, intl } = this.props;
    const dropId = info.node.props.eventKey - 0;
    const currentId = info.dragNode.props.eventKey - 0;
    const { treeData } = this.props;
    const dropPos = info.node.props.pos.split('-');
    const dropPosition = info.dropPosition - Number(dropPos[dropPos.length - 1]);
    let parentId;
    let parentName;
    let prevId;
    let nextId;

    // 禁止拖拽至根节点上
    if (treeData[0].key - 0 === dropId) return;

    // 只能在同级间拖拽
    if (info.node.props.item.parentId !== info.dragNode.props.item.parentId || dropPosition === 0) {
      return message.warn(intl.formatMessage(messages.dragErrorTip));
    }

    const loop = (data, key, callback) => {
      data.forEach((item, index) => {
        if (item.key - 0 === key) {
          parentId = item.parentId - 0;
          parentName = item.parentName;

          // 拖至节点前
          if (dropPosition === -1) {
            prevId = index === 0 ? '' : data[index - 1].key - 0;
            nextId = item.key - 0;
          } else if (dropPosition === 1) {
            // 拖至节点后
            prevId = item.key - 0;
            nextId = index === data.length - 1 ? '' : data[index + 1].key - 0;
          } else {
            // 拖拽至节点内部
            parentId = item.key - 0;
            parentName = item.title;
          }
        }
        if (item.children) {
          return loop(item.children, key, callback);
        }
      });
    };

    loop(treeData, dropId);

    if (dragTreeNode) {
      dragTreeNode(parentId, prevId, nextId, currentId, parentName);
    }
  }

  render() {
    const {
      intl,
      isSearch,
      keyword,
      dataSource,
      selectedRowKeys,
      loading,
      hasMore,
      indeterminate,
      checkAll,
      onloadmore,
      oncheckall,
      onselectchange,
      onchange,
      onsearch,
      onsearchtree,
      isTree,
      entityCode,
      moveRow,
      selectedTreeKeys,
      selectedTreeName,
      selectedTreeI18nKey,
      treeData,
      pagination,
      modalType
    } = this.props;

    const { modalData, btnDisabled } = this.state;
    const { source, authorityList, moduleId } = this.props;

    const optValueRule = source === 'lcdp' || authorityList.includes('addEntity') || authorityList.includes('editEntity');

    return (
      <div
        className={styles.container}
      >
        <Layout className={styles.innerLayout}>
          {
            isTree
              ? (
                <SupResize>
                  <SupTree
                    autoExpandRoot
                    draggable={optValueRule}
                    placeholder={intl.formatMessage(messages.codeValueTreeSearch)}
                    selectedKeys={selectedTreeKeys}
                    onSearch={onsearchtree}
                    onSelect={this.handleSelectTree}
                    dataSource={treeData}
                    onDrag={this.handleDrop}
                    fuzzyParams={{
                      url: '/inter-api/systemcode/v1/values/list',
                      param: 'displayName',
                      otherParams: `entityCode=${entityCode}`,
                      callback: (res) => {
                        const { list = [] } = res;
                        return list.map((item) => {
                          return {
                            key: item.code,
                            id: item.id,
                            title: item.displayName
                          };
                        });
                      }
                    }}
                  />
                  <Content className={styles.content}>
                    <CustomTable
                      ref={(ref) => { this.table = ref; }}
                      intl={intl}
                      showAdd={optValueRule}
                      showDelete={optValueRule}
                      total={pagination.total}
                      placeholder={intl.formatMessage(messages.pleaseEnterCodeValue)}
                      size="small"
                      drag={!isSearch && optValueRule}
                      fixeFirstItem={isTree && selectedTreeKeys.length !== 0 && selectedTreeKeys[0] !== 'null'}
                      rowKey="code" // 唯一值
                      value={keyword} // 搜索内容
                      onchange={onchange} // 修改搜索内容
                      onsearch={onsearch} // 搜索事件
                      loading={loading} // 加载更多loading
                      hasMore={hasMore} // 是否可加载更多
                      onloadmore={onloadmore} // 加载更多事件
                      columns={this.columns} // 表头数据
                      dataSource={dataSource} // 表身数据
                      selectedRowKeys={selectedRowKeys} // 选中数据
                      indeterminate={indeterminate} // 全选的半选状态
                      checkAll={checkAll} // 全选状态
                      oncheckall={oncheckall} // 全选事件
                      onselectchange={onselectchange} // 选中事件
                      clickBtn={this.handleClickBtn}
                      moveRow={moveRow}
                    />
                  </Content>
                </SupResize>
              ) : (
                <Content className={styles.content}>
                  <CustomTable
                    ref={(ref) => { this.table = ref; }}
                    intl={intl}
                    showAdd={optValueRule}
                    showDelete={optValueRule}
                    total={pagination.total}
                    placeholder={intl.formatMessage(messages.pleaseEnterCodeValue)}
                    size="small"
                    drag={!isSearch && optValueRule}
                    fixeFirstItem={isTree && selectedTreeKeys.length !== 0 && selectedTreeKeys[0] !== 'null'}
                    rowKey="code" // 唯一值
                    value={keyword} // 搜索内容
                    onchange={onchange} // 修改搜索内容
                    onsearch={onsearch} // 搜索事件
                    loading={loading} // 加载更多loading
                    hasMore={hasMore} // 是否可加载更多
                    onloadmore={onloadmore} // 加载更多事件
                    columns={this.columns} // 表头数据
                    dataSource={dataSource} // 表身数据
                    selectedRowKeys={selectedRowKeys} // 选中数据
                    indeterminate={indeterminate} // 全选的半选状态
                    checkAll={checkAll} // 全选状态
                    oncheckall={oncheckall} // 全选事件
                    onselectchange={onselectchange} // 选中事件
                    clickBtn={this.handleClickBtn}
                    moveRow={moveRow}
                  />
                </Content>
              )
          }
        </Layout>
        <Drawer
          placement="left"
          closable={false}
          mask={false}
          width={isTree ? 1000 : 720}
          visible={['addCodeValue', 'editCodeValue'].includes(modalType)}
          getContainer={false}
          style={{ position: 'absolute' }}
        >
          <Layout>
            <Content className={styles.content}>
              {
                ['addCodeValue', 'editCodeValue'].includes(modalType) && (
                  <CodeValueForm
                    ref={(ref) => { this.form = ref; }}
                    isTree={isTree}
                    data={{
                      moduleId,
                      ...modalData,
                      entityCode,
                      parentId: modalType === 'addCodeValue' && selectedTreeKeys ? selectedTreeKeys[0] : modalData ? modalData.parentId : '',
                      parentName: modalType === 'addCodeValue' ? selectedTreeI18nKey || (treeData && treeData[0] && treeData[0].name) : modalData ? modalData.parentName : '',
                      parentDisplayName: modalType === 'addCodeValue' ? selectedTreeName || (treeData && treeData[0] && treeData[0].title) : modalData ? modalData.parentDisplayName : ''
                    }}
                    type={modalType}
                    intl={intl}
                  />
                )
              }
            </Content>
            <Footer className={styles.footer}>
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
                onClick={() => this.toggleVisible('codeValueMgr')}
              >
                {intl.formatMessage(messages.cancel)}
              </Button>
            </Footer>
          </Layout>
        </Drawer>
      </div>
    );
  }
}

export default CodeValueMgr;
