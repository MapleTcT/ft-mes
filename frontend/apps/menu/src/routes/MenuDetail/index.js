import React, { Component } from 'react';
import { Button, Icon, Modal, Divider, Row, Col, message } from 'sup-ui';
import SupIcon from 'sup-rc-icon';
import OptForm from 'root/components/OptForm';
import messages from 'root/common/messages';
import SupTable from 'sup-rc-table';
import * as api from 'root/services/api';
import 'sup-rc-table/dist/index.less';
import styles from './index.less';

const restrictData = ['enablePosrestrict', 'enableAssignpos', 'enableAssignstaff', 'enableAssignDept', 'enableDeptrict', 'enableDealerpermission'];
const methodTypes = ['GET', 'POST', 'PUT', 'DELETE'];

const { confirm } = Modal;

class MenuDetail extends Component {
  constructor(props) {
    super(props);

    const { intl } = props;

    // 只有本公司创建的菜单才需修改
    this.cid = localStorage.getItem('loginMsg') ? _.get(JSON.parse(localStorage.getItem('loginMsg')), 'currentCompany.id', '') : '';

    this.btnColumns = [
      {
        key: 'add',
        disabled: () => { return this.state.globalSearch; },
        content: intl.formatMessage(messages.add),
        callback: () => this.toggleVisible({}, 'addOpt')
      },
      {
        key: 'delete',
        disabled: () => { return this.state.selectedRowKeys.length === 0; },
        callback: this.batchDelete
      }
    ];

    const columns = [
      {
        title: intl.formatMessage(messages.optName),
        width: 150,
        dataIndex: 'nameDisplay'
      },
      {
        title: intl.formatMessage(messages.optCode),
        width: 300,
        dataIndex: 'code'
      },
      {
        title: 'URL',
        width: 300,
        dataIndex: 'urls',
        render: (text) => {
          const label = [];
          text.forEach((item) => {
            label.push(`${methodTypes[item.methodType]}_${item.url}`);
          });
          const newLabel = label.join('\n').replace('\\n', ' \n ');
          return (
            <div title={newLabel}>
              {
                label.map((txt) => <div className={styles.urlTxt}>{txt}</div>)
              }
            </div>
          );
        }
      },
      {
        title: intl.formatMessage(messages.menuPath),
        width: 300,
        dataIndex: 'fullPathName'
      },
      {
        title: intl.formatMessage(messages.operation),
        dataIndex: 'operation',
        type: 'operation',
        fixed: true,
        width: 160,
        authority: () => {
          // 选中禁用菜单时，取消操作列
          return this.props.isEnable && this.getOptAuthority();
        },
        render: (text, record) => {
          return (
            <div>
              <a onClick={() => this.toggleVisible(record, 'updateOpt')}>{intl.formatMessage(messages.edit)}</a>
              {
                !record.isDefault && (
                  <>
                    <Divider type="vertical" />
                    <a onClick={() => this.toggleDelete(record)}>{intl.formatMessage(messages.delete)}</a>
                  </>
                )
              }
            </div>
          );
        }
      }
    ];

    this.state = {
      btnDisabled: false,
      globalSearch: false,
      optType: '',
      visible: false,
      pagination: {
        current: 1,
        pageSize: 20,
        total: 0
      },
      columns,
      selectedRowKeys: [],
      selectedRows: [],
      dataSource: [],
      optDetail: {}
    };
  }

  componentDidMount() {
    // 首次加载根据是否显示横向滚动条来控制操作栏是否固定
    if (this.table && this.table.changeOperationFixedStatus) {
      this.table.changeOperationFixedStatus();
    }
  }

  shouldComponentUpdate(nextProps) {
    // 选择根节点，详情页不变
    return !nextProps.isSelectedRoot;
  }

  // 获取操作列表
  getOptList = () => {
    const { menuinfoId } = this.props;
    const { pagination } = this.state;

    api.getOptList({
      menuinfoId,
      keyword: this.keyword,
      current: pagination.current,
      pageSize: pagination.pageSize
    }).then((res) => {
      const { data } = res;
      this.setState({
        selectedRowKeys: [],
        selectedRows: [],
        dataSource: data.list,
        pagination: data.pagination
      });
    });
  }

  // 获取操作权限
  getOptAuthority = () => {
    const { authorityList = [] } = this.props;
    const addKey = window.menuSource === 'supplant' ? 'addMenuConfigure' : 'addMenu';
    const editKey = window.menuSource === 'supplant' ? 'editMenuConfigure' : 'editMenu';
    return authorityList.includes(addKey) || authorityList.includes(editKey);
  }

  // 修改全局搜索状态，外部用
  toggleGolbalSearchState = (state) => {
    const { columns } = this.state;

    // 全局搜索显示菜单路径
    columns[3].hide = !state;

    this.keyword = !state ? '' : this.keyword;

    this.setState({
      columns,
      globalSearch: state
    });
  }

  // 表格操作
  handleTableSearch = ({ pagination }) => {
    this.setState({
      pagination
    }, () => {
      this.getOptList();
    });
  }

  // 搜索，外部用
  handleSearch = (keyword) => {
    const { globalSearch, pagination, columns } = this.state;

    // 显示菜单路径
    columns[3].hide = false;

    this.keyword = keyword;

    this.setState({
      columns,
      globalSearch: true,
      pagination: {
        current: 1,
        pageSize: 20,
        total: globalSearch ? pagination.total : 0
      }
    }, () => {
      this.getOptList();
    });
  }

  // 拖拽
  updateColumns = (params) => {
    this.setState({
      columns: params
    });
  }

  // 显隐操作modal
  toggleVisible = (data = {}, type) => {
    // 处理数据，form表单用
    if (data && JSON.stringify(data) !== '{}') {
      data.restrictedCone = [];
      restrictData.forEach((k) => {
        if (data[k]) {
          data.restrictedCone.push(k);
        }
      });
    }

    this.setState({
      optDetail: _.cloneDeep(data),
      visible: !this.state.visible,
      optType: type
    });
  }

  // 双击显示编辑Modal
  handleDoubleClick = (record) => {
    if (!this.getOptAuthority()) return;

    this.toggleVisible(record, 'updateOpt');
  }

  // 选中checkbox
  handleSelectItems = (selectedRowKeys, selectedRows) => {
    this.setState({
      selectedRowKeys,
      selectedRows
    });
  }

  // 确认
  submit = (e) => {
    const { optType, optDetail: { nameDisplay } } = this.state;
    const { intl } = this.props;

    e.preventDefault();

    // 校验操作URL（导入重复数据，提交时校验）
    this.formRef.validTableForm();

    this.form.validateFieldsAndScroll((err, values) => {
      if (err) return;

      // 判断类型+url组合是否重复
      if (values.urls && values.urls.length > 0) {
        const result = [];
        let isRepeat = false;

        values.urls.forEach((item) => {
          if (result.includes(`${item.methodType}_${item.url}`)) {
            isRepeat = true;
            return;
          }
          result.push(`${item.methodType}_${item.url}`);
        });

        if (isRepeat) return;
      }

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

        values.nameDisplay = values.name.i18nValue ? values.name.i18nValue[newLanguage] : nameDisplay;
        values.name = values.name.i18nKey;

        restrictData.forEach((k) => {
          values[k] = values.restrictedCone.includes(k);
        });

        values.restrictedCone = undefined;

        this.setState({
          btnDisabled: true
        }, () => {
          api[optType]({
            ...values
          }).then(() => {
            message.success(intl.formatMessage(messages[optType === 'addOpt' ? 'successAdd' : 'successUpdate']));
            this.getOptList();
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

  // 单个删除
  toggleDelete = (record) => {
    const { intl } = this.props;

    confirm({
      title: `${intl.formatMessage(messages.confirmDeleteOpt, { title: record.nameDisplay })}?`,
      content: intl.formatMessage(messages.pleaseConfirmDelete),
      okText: intl.formatMessage(messages.sure),
      cancelText: intl.formatMessage(messages.cancel),
      onOk: () => {
        this.delete([record.code]);
      }
    });
  }

  // 批量删除
  batchDelete = () => {
    const { intl } = this.props;
    const { selectedRows } = this.state;

    const selectedRowKeys = selectedRows && selectedRows.length !== 0 ? selectedRows.map((o) => o.code) : [];

    confirm({
      title: `${intl.formatMessage(messages.confirmBatchDeleteOpt)}?`,
      content: intl.formatMessage(messages.pleaseConfirmBatchDelete),
      okText: intl.formatMessage(messages.sure),
      cancelText: intl.formatMessage(messages.cancel),
      onOk: () => {
        this.delete(selectedRowKeys);
      }
    });
  }

  // 删除
  delete = (codes) => {
    const { intl } = this.props;
    const { dataSource, pagination } = this.state;

    api.deleteOpt({
      codes: codes.join(',')
    }).then(() => {
      message.success(intl.formatMessage(messages.successDelete));

      this.getOptList();

      if (dataSource.length === codes.length) {
        pagination.current = pagination.current === 1 ? 1 : pagination.current - 1;
        this.setState({
          pagination
        });
      }

      if (codes.length > 1) {
        this.setState({
          selectedRowKeys: [],
          selectedRows: []
        });
      }
    });
  }

  // 添加删除url
  updateUrl = (type, index) => {
    const { optDetail } = this.state;

    // 添加
    if (type === 'add') {
      if (!optDetail.urls) {
        optDetail.urls = [];
      }
      optDetail.urls.push({ key: new Date().getTime().toString() });
    } else {
      // 删除
      optDetail.urls.splice(index, 1);
    }

    this.setState({
      optDetail
    });
  }

  render() {
    const {
      intl,
      data = {},
      menuinfoId,
      isEnable,
      companyName,
      currentCompanyNames,
      authorityList,
      onEdit,
      moduleCode
    } = this.props;
    const {
      visible,
      columns,
      dataSource,
      pagination,
      optDetail,
      optType,
      globalSearch,
      selectedRowKeys,
      btnDisabled
    } = this.state;

    const footer = (
      <div
        className={styles.modalFooter}
        style={{
          padding: '0 22px'
        }}
      >
        <Button
          className={styles.sureBtn}
          type="primary"
          disabled={btnDisabled}
          onClick={this.submit}
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
    );

    const getCheckboxProps = (record) => ({
      disabled: record.isDefault
    });

    // 未选择树节点,且不是全局搜索时，提示选择左侧对象
    if (!menuinfoId && !globalSearch) {
      return (
        <div className={styles.noSelectBox}>
          <SupIcon className={styles.backIcon} type="iconpoint" />
          <span>{intl.formatMessage(messages.selectLeftObject)}</span>
        </div>
      );
    }

    // 有新增和修改权限时允许新增操作
    const optAuthority = this.getOptAuthority();

    return (
      <div className={styles.container}>
        <header className={styles.header}>
          <span className={styles.title}>{data.nameDisplay}</span>
        </header>
        <div className={styles.content}>
          {
            !globalSearch && (
              <article className={styles.article}>
                <p className={styles.subTitle}>
                  {`1. ${intl.formatMessage(messages.baseMsg)}`}
                  {
                    this.cid === data.cid && isEnable && authorityList.includes(window.menuSource === 'supplant' ? 'editMenuConfigure' : 'editMenu') && (
                      <Icon
                        className={styles.editIcon}
                        type="edit"
                        onClick={() => onEdit('editMenu')}
                      />
                    )
                  }
                </p>
                <Row>
                  <Col span={8} className={styles.li}>
                    <span className={styles.label}>
                      {intl.formatMessage(messages.code)}
                      ：
                    </span>
                    <span className={styles.value} title={data.code}>{data.code}</span>
                  </Col>
                  <Col span={8} className={styles.li}>
                    <span className={styles.label}>
                      {intl.formatMessage(messages.openType)}
                      ：
                    </span>
                    <span className={styles.value}>
                      {JSON.stringify(data) !== '{}' && intl.formatMessage(messages[data.target === 'BLANK' ? 'newTab' : 'currentTab'])}
                    </span>
                  </Col>
                  <Col span={8} className={styles.li}>
                    <span className={styles.label}>
                      {intl.formatMessage(messages.useCompany)}
                      ：
                    </span>
                    <span className={styles.value}>{companyName}</span>
                  </Col>
                  <Col span={8} className={styles.li}>
                    <span className={styles.label}>URL：</span>
                    <span className={styles.value} title={data.url}>{data.url}</span>
                  </Col>
                  <Col span={8} className={styles.li}>
                    <span className={styles.label}>
                      {intl.formatMessage(messages.useRange)}
                      ：
                    </span>
                    <span className={styles.value} title={currentCompanyNames}>{currentCompanyNames}</span>
                  </Col>
                  {
                    window.menuSource === 'supplant' && (
                      <>
                        <Col span={8} className={styles.li}>
                          <span className={styles.label}>
                            {intl.formatMessage(messages.hide)}
                            ：
                          </span>
                          <span className={styles.value}>{intl.formatMessage(messages[`${data.isHide ? 'yes' : 'no'}`])}</span>
                        </Col>
                        <Col span={8} className={styles.li}>
                          <span className={styles.label}>
                            {intl.formatMessage(messages.menuStyle)}
                            ：
                          </span>
                          <span className={styles.value}>{data.cssClass}</span>
                        </Col>
                        <Col span={8} className={styles.li}>
                          <span className={styles.label}>
                            {intl.formatMessage(messages.moduleCode)}
                            ：
                          </span>
                          <span className={styles.value}>{data.moduleName}</span>
                        </Col>
                        <Col span={8} className={styles.li}>
                          <span className={styles.label}>
                            {intl.formatMessage(messages.memo)}
                            ：
                          </span>
                          <span className={styles.value} title={data.memo}>{data.memo}</span>
                        </Col>
                      </>

                    )
                  }
                </Row>
              </article>
            )
          }
          <article className={`${styles.article} ${styles.optBox}`}>
            <p className={styles.subTitle}>
              {`${globalSearch ? '1. ' : '2. '}${intl.formatMessage(messages.optList)}`}
            </p>
            <div className={styles.table}>
              {
                !dataSource || dataSource.length === 0 ? (
                  <div className={styles.emptyBox}>
                    {
                      optAuthority && isEnable && (
                        <>
                          <span>{intl.formatMessage(messages.emptyLabel)}</span>
                          <Button
                            size="small"
                            ghost
                            type="primary"
                            icon="plus"
                            disabled={globalSearch}
                            onClick={() => this.toggleVisible({}, 'addOpt')}
                          >
                            {intl.formatMessage(messages.add)}
                          </Button>
                        </>
                      )
                    }
                  </div>
                ) : (
                  <SupTable
                    ref={(ref) => { this.table = ref; }}
                    rowKey="id"
                    showSelection={isEnable && optAuthority}
                    getCheckboxProps={getCheckboxProps}
                    selectedRowKeys={selectedRowKeys}
                    btnColumns={(isEnable || globalSearch) && optAuthority ? this.btnColumns : []}
                    showColumnsFilter={isEnable || globalSearch}
                    showSearchIcon={false}
                    columns={columns}
                    dataSource={dataSource}
                    pagination={pagination}
                    updateColumns={this.updateColumns}
                    onSearch={this.handleTableSearch}
                    onSelectItem={this.handleSelectItems}
                    onDoubleClick={(record) => this.handleDoubleClick(record)}
                  />
                )
              }
            </div>
          </article>
        </div>
        <Modal
          className={styles.modal}
          maskClosable={false}
          destroyOnClose
          title={optType ? intl.formatMessage(messages[optType]) : ''}
          visible={visible}
          width={640}
          bodyStyle={{
            padding: '24px 40px',
            height: 615
          }}
          onCancel={() => this.toggleVisible()}
          footer={footer}
        >
          <OptForm
            ref={(ref) => { this.form = ref; }}
            wrappedComponentRef={(form) => { this.formRef = form; }}
            intl={intl}
            data={optDetail}
            optType={optType}
            moduleCode={moduleCode}
            menuinfoId={data.id}
            restrictData={restrictData}
            updateUrl={this.updateUrl}
          />
        </Modal>
      </div>
    );
  }
}

export default MenuDetail;
