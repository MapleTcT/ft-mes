import React, { Component } from 'react';
import { injectIntl } from 'react-intl';
import _ from 'lodash';
import { Layout, Button, Modal, message } from 'sup-ui';
import * as api from 'root/services/api';
import * as commonApi from 'root/services/commonApi';
import messages from 'root/common/messages';
import SupResize from 'sup-rc-resize';
import SupSearch from 'sup-rc-search';
import MenuTree from '../MenuTree';
import MenuDetail from '../MenuDetail';
import MenuForm from '../../components/MenuForm';
import 'sup-rc-search/dist/index.less';
import styles from './index.less';

const { Header } = Layout;
const { confirm } = Modal;

@injectIntl
class Menu extends Component {
  constructor(props) {
    super(props);

    this.companyList = [];
    this.companyId = _.get(JSON.parse(localStorage.getItem('loginMsg')), 'currentCompany.id', '');
    this.companyName = _.get(JSON.parse(localStorage.getItem('loginMsg')), 'currentCompany.name', '');

    // 区分supplant和supfusion
    window.menuSource = window.location.hash.includes('#/design') ? 'supplant' : window.location.hash.includes('#/supos') ? 'supos' : 'supfusion';

    const { intl } = props;
    const headerTitle = window.menuSource === 'supplant' ? 'menuManage' : 'menuConfig';
    document.title = intl.formatMessage(messages[headerTitle]);

    this.state = {
      btnDisabled: false,
      isSelectedRoot: false, // 是否根节点
      optType: '',
      visible: false,
      selectedKey: '',
      menuDetail: {},
      menuForm: {},
      showType: '',
      moduleCode: 'rbac',
      isResable: false,
      treeData: [],
      disabledTreeData: [],
      isEnable: true, // 是否启用
      authorityList: [],
      moduleList: [], // 模块数据
      loading: false
    };
  }

  componentDidMount() {
    this.getAuthority();
    this.getMenuTree();
    this.getDisabledMenuTree();
    this.getCompanyTree();
    this.getApps();
  }

  // 获取所有模块
  getApps = () => {
    commonApi.getApps().then((res) => {
      const { data: { list = [] } = {} } = res;

      this.setState({
        moduleList: list
      });
    });
  }

  // 获取权限接口
  getAuthority = () => {
    commonApi.getAuthority({
      code: window.menuSource === 'supplant' ? 'menuManageConfigure' : 'menumanage'
    }).then((res) => {
      const { data: { list } } = res;
      this.setState({
        authorityList: list
      });
    });
  }

  // 获取公司树
  getCompanyTree = () => {
    const { intl } = this.props;

    commonApi.getCompanyTree().then((res) => {
      const { data: { list } } = res;

      // 新增所有公司
      list.unshift({
        id: -1,
        shortName: intl.formatMessage(messages.allCompanys)
      });

      this.companyList = list;
    });
  }

  // 获取菜单数据
  getMenuTree = () => {
    const { selectedKey } = this.state;
    const { intl } = this.props;
    const url = window.menuSource === 'supos' ? 'getSuposMenuTree' : 'getMenuTree';

    this.setState({
      loading: true
    }, () => {
      api[url]({
        enableStatus: true
      }).then((res) => {
        const { data: { list } } = res;

        // 增加根节点
        const newList = [{
          id: '-1',
          nameDisplay: intl.formatMessage(messages.menuRoot),
          children: list
        }];

        if (selectedKey) {
          this.getDetail(newList, true);
        }

        this.setState({
          treeData: newList
        });
      }).finally(() => {
        this.setState({
          loading: false
        });
      });
    });
  }

  // 获取停用菜单数据
  getDisabledMenuTree = () => {
    if (window.menuSource === 'supplant') return;

    api.getMenuTree({
      enableStatus: false
    }).then((res) => {
      const { data: { list } } = res;

      this.setState({
        disabledTreeData: list
      });
    });
  }

  // 搜索树
  handleTreeSearch = (params, type) => {
    const { intl } = this.props;
    const param = type === 'advanced' ? { id: params.id } : type === 'fuzzy' ? { keyword: params.title } : '';

    // 清空搜索，取消选中
    if (type === 'clear') {
      // 取消全局搜索
      this.menuDetail.toggleGolbalSearchState(false);
      this.setState({
        selectedKey: ''
      }, () => {
        this.getMenuTree();
      });
    } else {
      const url = window.menuSource === 'supos' ? 'querySuposMenus' : 'queryMenus';
      api[url]({
        ...param,
        enable: true
      }).then((res) => {
        const { data: { list } } = res;

        // 增加根节点
        const newList = [{
          id: '-1',
          nameDisplay: intl.formatMessage(messages.menuRoot),
          children: list
        }];

        // 精确搜索，选中该搜索项
        this.setState({
          treeData: newList,
          selectedKey: type === 'advanced' ? params.id : ''
        }, () => {
          if (this.state.selectedKey) {
            this.menuDetail.getOptList();
            this.getDetail(newList, true);
          }
        });
      });
    }
  }

  // 获取选中详情
  getDetail = (data, isEnable) => {
    const { selectedKey } = this.state;

    for (let i = 0; i < data.length; i += 1) {
      if (data[i].id === selectedKey) {
        return this.setState({
          isEnable,
          menuDetail: _.cloneDeep(data[i])
        });
      }

      if (data[i].children && data[i].children.length !== 0) {
        this.getDetail(data[i].children, isEnable);
      }
    }
  }

  // 点击树按钮回调
  handleClickBtn = (type, id, name) => {
    const { intl } = this.props;

    // 启用停用
    if (['startMenu', 'stopMenu'].includes(type)) {
      confirm({
        title: `${intl.formatMessage(messages[`${type}Tip`], { name })}?`,
        content: intl.formatMessage(messages[`${type}ContentTip`]),
        onOk: () => {
          api.modifyEnableStatus({
            id,
            enable: type === 'startMenu'
          }).then(() => {
            message.success(intl.formatMessage(messages[`${type}Success`], { name }));
            this.setState({
              selectedKey: ''
            }, () => {
              this.getMenuTree();
              this.getDisabledMenuTree();
            });
          });
        }
      });
    } else if (type === 'delete') {
      // 删除
      confirm({
        title: `${intl.formatMessage(messages.confirmDeleteOpt, { title: name })}?`,
        content: intl.formatMessage(messages.deleteComfirmDelete),
        onOk: () => {
          api.deleteMenu({
            codes: id
          }).then(() => {
            message.success(intl.formatMessage(messages.successDelete));
            this.setState({
              selectedKey: ''
            }, () => {
              this.getMenuTree();
              this.getDisabledMenuTree();
            });
          });
        }
      });
    } else {
      // 新增修改菜单
      const { menuDetail } = this.state;
      const menuForm = type === 'addMenu' ? {} : _.cloneDeep(menuDetail);

      if (!menuForm.companyIds) {
        menuForm.companyIds = [];
      }

      // id:-1 所有公司,如果不是所有公司,则适用范围需包含本公司
      // 补充: 仅新增时添加默认值
      if (type === 'addMenu' && (!menuDetail.companyIds || (!menuDetail.companyIds.includes(-1) && !menuForm.companyIds.includes(this.companyId)))) {
        menuForm.companyIds.push(this.companyId);
      }

      this.setState({
        optType: type,
        visible: true,
        menuForm
      });
    }
  }

  // 选择树
  handleSelectTree = (id, event, isEnable) => {
    if (id === '-1') {
      this.setState({
        isSelectedRoot: true,
        selectedKey: id
      });
    } else {
      // 清空搜索内容
      this.supSearch.resetSearch();

      this.setState({
        isEnable,
        isSelectedRoot: false,
        selectedKey: id,
        menuDetail: _.cloneDeep(event.node.props.item),
        showType: event.node.props.item.showType,
        moduleCode: event.node.props.item.moduleCode
      }, () => {
        this.menuDetail.toggleGolbalSearchState(false);
      });
    }
  }

  // 显隐modal
  toggleVisible = () => {
    const { visible, showType } = this.state;

    this.setState({
      visible: !visible,
      showType: visible ? '' : showType
    });
  }

  // 修改菜单form
  handleChangeItem = (value, type) => {
    this.setState({
      [`${type}`]: value
    });
  }

  callback = (msg, result) => {
    const { intl } = this.props;
    const { data: { data } } = result;

    message.success(intl.formatMessage(messages[msg]));

    this.setState({
      btnDisabled: false,
      visible: false,
      selectedKey: data.id
    }, () => {
      this.getMenuTree();

      if (msg === 'successAdd') {
        this.setState({
          isSelectedRoot: false
        });
        this.menuDetail.toggleGolbalSearchState(false);
        this.menuDetail.getOptList();
      }
    });
  }

  // 新增修改
  submit = (e) => {
    const { selectedKey, optType, menuDetail: { nameDisplay } } = this.state;

    e.preventDefault();
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

        values.nameDisplay = values.name.i18nValue ? values.name.i18nValue[newLanguage] : nameDisplay;
        values.name = values.name.i18nKey;

        // 适用范围取id
        if (values.companyIds && values.companyIds.length !== 0) {
          values.companyIds = values.companyIds.map((item) => item.id || item);
        }

        this.setState({
          btnDisabled: true
        }, () => {
          if (optType === 'addMenu') {
            api.addMenu({
              ...values,
              parentId: selectedKey
            }).then((result) => {
              // 重置树的搜索内容
              if (this.treeRef && this.treeRef.treeRef && this.treeRef.treeRef.resetValue) {
                this.treeRef.treeRef.resetValue();
              }

              this.callback('successAdd', result);
            }).catch(() => {
              setTimeout(() => {
                this.setState({
                  btnDisabled: false
                });
              }, 1000);
            });
          } else {
            api.updateMenu({
              ...values
            }).then((result) => {
              this.callback('successUpdate', result);
            }).catch(() => {
              setTimeout(() => {
                this.setState({
                  btnDisabled: false
                });
              }, 1000);
            });
          }
        });
      });
    });
  }

  // 全局搜索操作
  handleOptSearch = (keyword) => {
    // 取消选中树
    this.setState({
      isEnable: true,
      selectedKey: '',
      menuDetail: {},
      showType: '',
      moduleCode: 'rbac'
    }, () => {
      this.menuDetail.handleSearch(keyword);
    });
  }

  // 显隐已停用树
  toggleVisibleDisabledMenu = () => {
    this.setState({
      visibleDisabledMenu: !this.state.visibleDisabledMenu
    });
  }

  // 拖拽
  dragTreeNode = (parentId, prevId, nextId, currentId) => {
    const params = window.menuSource !== 'supplant' ? { supfusion: true } : {};

    // 拖至根节点平级，不请求接口
    if (!parentId && !prevId && !nextId) return;

    api.sort({
      parentId,
      prevId,
      nextId,
      currentId,
      ...params
    }).then(() => {
      this.getMenuTree();
    });
  }

  // 根据id获取对应的companyIds
  getCompanyIds = (data, id) => {
    let companyIds = [];
    data.forEach((item) => {
      if (item.id === id && item.companyIds) {
        companyIds = _.cloneDeep(item.companyIds);
      }
    });

    return companyIds;
  }

  render() {
    const { intl } = this.props;
    const {
      treeData,
      disabledTreeData,
      optType,
      visible,
      isResable,
      menuDetail = {},
      menuForm,
      showType,
      moduleCode,
      isEnable,
      selectedKey,
      visibleDisabledMenu,
      authorityList,
      isSelectedRoot,
      moduleList,
      btnDisabled,
      loading
    } = this.state;

    const currentCompanyNames = [];
    let companyName = '';
    let companyId = '';

    // 新增时，所属公司为当前登录公司
    if (optType === 'addMenu') {
      companyName = this.companyName;
      companyId = this.companyId;
    } else if (menuDetail.cid) {
      companyName = _.find(this.companyList, (o) => { return o.id === menuDetail.cid; }).shortName;
      companyId = menuDetail.cid;
    }

    this.companyList.forEach((item) => {
      if (menuDetail.companyIds && menuDetail.companyIds.includes(item.id)) {
        currentCompanyNames.push(item.shortName);
      }
    });

    const modalTitle = optType ? intl.formatMessage(messages[optType]) : '';
    const footer = (
      <div
        className={styles.modalFooter}
        style={{
          padding: '0 24px'
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
          onClick={this.toggleVisible}
        >
          {intl.formatMessage(messages.cancel)}
        </Button>
      </div>
    );
    let headerTitle = 'menuConfig';
    if (window.menuSource === 'supplant') {
      headerTitle = 'menuManage';
    }

    return (
      <Layout className={`${styles.layout} ${isResable ? styles.noUserSelect : ''}`}>
        <Header className={styles.header}>
          <span>{intl.formatMessage(messages[headerTitle])}</span>
          <SupSearch
            ref={(ref) => { this.supSearch = ref; }}
            placeholder={intl.formatMessage(messages.searchTip)}
            style={{
              width: 220
            }}
            onSearch={this.handleOptSearch}
          />
        </Header>
        <div className={styles.content}>
          <SupResize>
            <MenuTree
              ref={(ref) => { this.treeRef = ref; }}
              intl={intl}
              isEnable={isEnable}
              loading={loading}
              showAdd={authorityList.includes(window.menuSource === 'supplant' ? 'addMenuConfigure' : 'addMenu')}
              selectedKeys={selectedKey ? [selectedKey.toString()] : []}
              authorityList={authorityList}
              dataSource={treeData}
              disabledDataSource={disabledTreeData}
              onClickBtn={this.handleClickBtn}
              onSelect={this.handleSelectTree}
              changeUserSelect={this.changeUserSelect}
              visibleDisabledMenu={visibleDisabledMenu}
              toggleVisibleDisabledMenu={this.toggleVisibleDisabledMenu}
              dragTreeNode={this.dragTreeNode}
              onSearch={this.handleTreeSearch}
            />
            <MenuDetail
              ref={(ref) => { this.menuDetail = ref; }}
              intl={intl}
              data={menuDetail}
              moduleCode={menuDetail.app}
              companyName={companyName}
              isSelectedRoot={isSelectedRoot}
              isEnable={isEnable}
              menuinfoId={selectedKey}
              authorityList={authorityList}
              currentCompanyNames={currentCompanyNames.join(',')}
              onEdit={this.handleClickBtn}
            />
          </SupResize>
        </div>
        <Modal
          className={styles.modal}
          maskClosable={false}
          destroyOnClose
          title={modalTitle}
          visible={visible}
          width={600}
          bodyStyle={{
            padding: '30px 40px',
            height: 615
          }}
          onCancel={this.toggleVisible}
          footer={footer}
        >
          <MenuForm
            ref={(ref) => { this.form = ref; }}
            intl={intl}
            data={menuForm}
            moduleList={moduleList}
            showType={showType}
            moduleCode={moduleCode}
            optType={optType}
            companyId={companyId}
            companyName={companyName}
            onChangeItem={this.handleChangeItem}
          />
        </Modal>
      </Layout>
    );
  }
}

export default Menu;
