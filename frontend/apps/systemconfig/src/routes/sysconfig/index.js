import React from 'react';
import { Layout, message } from 'sup-ui';
import { injectIntl } from 'react-intl';
import {
  fetchConfigCatalogs,
  fetchCatalogInfo,
  updateConfigValue,
  getSearchCatalogUrl,
  fetchCatalogParent,
  getAuthority
} from 'root/services/configCatalog';

import 'sup-rc-resize/dist/index.css';
import SupResize from 'sup-rc-resize';
import SupTree from 'sup-rc-tree';

import { extractResData, delay } from './utils';
import ConfigBody from './ConfigBody';
import style from './style.less';
import { FIELD_PREFIX } from './constants';
import messages from './messages';

class Sysconfig extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      configCatalogs: [],
      configItems: [],
      activeMenuKey: null,
      treeMenuLoaded: false,
      configValueMap: {},
      selectedKeys: [],
      btnAuth: []
    };
  }

  componentDidMount() {
    getAuthority('systemconfig').then(({ data: { list } }) => {
      this.setState({
        btnAuth: list
      });
      this.loadCatalogs();
    });
  }

  loadCatalogs(keyword = '', fromSearch) {
    const formatCatToTreeNode = (cat) => {
      const node = {
        key: cat.catalogId,
        title: cat.name
      };
      if (cat.isParent) {
        node.isParent = true;
        if (cat.catalog) {
          node.children = cat.catalog.map(formatCatToTreeNode);
        }
      }
      // 固有模块标志
      if (cat.moduleCode) {
        node.isModule = true;
      }
      return node;
    };

    const genConfigItems = (d) => {
      return d.reduce((items, item) => {
        if (item.catalog) {
          items = items.concat(
            item.catalog.map((child) => {
              const { catalogId, name, moduleCode } = child;
              return { catalogId, name, moduleCode };
            })
          );
        }
        return items;
      }, []);
    };

    fetchConfigCatalogs({ keyword, type: 2 }).then((data) => {
      // const { intl } = this.props;
      let { catalogs } = extractResData(data);
      if (fromSearch) {
        // FIXME spi5739 搜索时不展示固定根节点
        catalogs = catalogs.filter((d) => d && d.catalog && d.catalog.length);
      }
      const configCatalogs = catalogs
        .map((d) => {
          // 处理国际化
          // if (d.code) {
          //   // 返回格式为systemConfig.app, 当前key只匹配后面部分
          //   const id = d.code.slice('systemConfig'.length + 1);
          //   d.name = intl.formatMessage({
          //     id,
          //     defaultMessage: d.name
          //   });
          // }
          // 标记是顶层目录
          d.isParent = true;
          return d;
        })
        .map(formatCatToTreeNode);
      const configItems = genConfigItems(catalogs);
      // 用于自动展开根节点
      const defaultExpandKeys = catalogs.map((d) => d.catalogId);
      delay(() => {
        this.setState({
          configCatalogs,
          treeMenuLoaded: true,
          defaultExpandKeys,
          configItems,
          selectedKeys: [] // 模糊搜索时需清空选中项
        });
      });
    });
  }

  refreshCatalogInfo(selectedKey, isModule) {
    const { configValueMap } = this.state;
    // 为了显示加载状态
    this.setState((state) => {
      state.configValueMap[selectedKey] = null;
      state.activeMenuKey = selectedKey;
      return state;
    });

    // 固有模块不要请求配置项
    if (isModule) return;

    fetchCatalogInfo(selectedKey).then((data) => {
      const { config } = extractResData(data);
      delay(() => {
        this.setState({
          configValueMap: {
            ...configValueMap,
            [selectedKey]: config
          }
        });
      });
    });
  }

  onMenuSelect = (selectedKeys, event) => {
    // 跳过菜单取消选择操作
    // 有值代表切换
    // 没值代表取消，需要跳过，并且刷新菜单
    const {
      node: {
        props: {
          item: { isParent, isModule }
        }
      }
    } = event;

    if (selectedKeys.length) {
      const [selectedKey] = selectedKeys;

      this.setState({
        selectedKeys: [selectedKey]
      });

      if (!isParent) {
        // 父级节点不用显示配置详情
        this.refreshCatalogInfo(selectedKey, isModule);
      }
    }
  };

  handleSaveConfig = (values, configInfos, cb) => {
    const { intl } = this.props;
    const config = [];
    const { activeMenuKey } = this.state;

    for (const configInfo of configInfos) {
      const { configId, type, format } = configInfo;
      let value = values[`${FIELD_PREFIX}${configId}`];
      if (typeof value === 'undefined' || value === null) {
        value = '';
      }
      // TODO 时间类型转换
      if (type === 5 && value) {
        value = value.format(format);
      }
      if (!Array.isArray(value)) {
        value = [value];
      }
      config.push({
        configId,
        value
      });
    }

    updateConfigValue({ config, catalogId: activeMenuKey }).then(() => {
      delay(() => {
        message.success(intl.formatMessage(messages.saveSuccess));
        cb();
      }, 2000);
    }, cb);
  };

  buildAdvSearchResult(data) {
    const { parentId, parentName, id, name, moduleCode } = data;
    // 精确搜索选中当前树需要catalogId,name属性
    const node = { key: id, title: name, catalogId: id, name };
    if (moduleCode) {
      node.moduleCode = moduleCode;
      node.isModule = true;
    }
    const configCatalogs = [
      {
        children: [node],
        key: parentId,
        title: parentName,
        isParent: true
      }
    ];

    // 选择当前项, 修改当前树
    this.setState({
      selectedKeys: [id],
      configCatalogs,
      // 刷新configItems, 修复精确搜索无法自动选中
      configItems: [node]
    });

    this.refreshCatalogInfo(id, !!moduleCode);
  }

  onSearchValueChange = (param, type) => {
    if (type === 'advanced') {
      // 获取接口
      fetchCatalogParent(param.catalogId).then(({ data: { data } }) => {
        this.buildAdvSearchResult(data);
      });
    } else {
      let keyword = '';
      if (type !== 'clear') {
        keyword = param.title;
      }
      this.loadCatalogs(keyword, type !== 'clear');
    }
  };

  render() {
    const {
      configItems,
      activeMenuKey,
      configCatalogs,
      treeMenuLoaded,
      configValueMap,
      selectedKeys,
      btnAuth,
      defaultExpandKeys
    } = this.state;

    const configBody = {
      configItems,
      activeConfig: activeMenuKey,
      configValueMap,
      handleSaveConfig: this.handleSaveConfig,
      updateAuth: btnAuth.includes('configCatalog')
    };

    const { intl } = this.props;

    return (
      <div className={style.sysconfig}>
        <Layout className={style.layout}>
          <Layout.Header className={style.topHeader}>
            {intl.formatMessage(messages.topHeader)}
          </Layout.Header>
          <Layout className={style.layout}>
            <SupResize min={220}>
              {treeMenuLoaded ? (
                <SupTree
                  defaultExpandKeys={defaultExpandKeys}
                  draggable={false}
                  addDisabled
                  placeholder={intl.formatMessage(messages.searchPlaceholder)}
                  selectedKeys={selectedKeys}
                  dataSource={configCatalogs}
                  onSelect={this.onMenuSelect}
                  onSearch={this.onSearchValueChange}
                  fuzzyParams={{
                    url: getSearchCatalogUrl(),
                    param: 'keyword',
                    otherParams: 'type=1',
                    callback: ({ data }) => {
                      return data.catalogs.map(({ name, catalogId }) => {
                        return {
                          title: name,
                          catalogId
                        };
                      });
                    }
                  }}
                />
              ) : null}
              <ConfigBody {...configBody} />
            </SupResize>
          </Layout>
        </Layout>
      </div>
    );
  }
}

export default injectIntl(Sysconfig);
