import React from 'react';
import { Layout } from 'sup-ui';
import { injectIntl } from 'react-intl';
import SupResize from 'sup-rc-resize';
import SupTree from 'sup-rc-tree';
import SupIcon from 'sup-rc-icon';
import { cloneDeep } from 'lodash';
import messages from './messages.js';
import style from './style.less';
import DetailTable from './DetailTable.js';
import { queryApp } from '../../services/printManage.js';

@injectIntl
export default class PrintManage extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      treeData: [],
      selectedKeys: [],
      selectedNode: null
    };
    this.formatMessage = props.intl.formatMessage;
  }

  componentDidMount() {
    this.refreshTree();
  }

  refreshTree = (params = {}) => {
    queryApp(params).then((res) => {
      const {
        data: { data: list }
      } = res;
      if (list) {
        this.initTreeData = cloneDeep(list);
        this.setState({ treeData: list, selectedKeys: [], selectedNode: null });
      }
    });
  };

  handleSelect = (node, event) => {
    const {
      node: {
        props: { item }
      }
    } = event;
    const { level } = item || {};
    this.setState({ selectedKeys: node, selectedNode: item });
    if (level === 1) this.setState({ selectedNode: null });
  };

  renderEmpty = () => {
    return (
      <div className={style.emptyTit}>
        <SupIcon className={style.emptyIcon} type="iconpoint" />
        <span>{this.formatMessage(messages.leftEmpty)}</span>
      </div>
    );
  };

  filterData = (data, query, isFuzzy) => {
    const predicate = (node) => {
      const condition = isFuzzy
        ? node.name.indexOf(query) > -1
        : node.name === query;
      return !!condition;
    };
    if (!(data && data.length)) {
      return [];
    }
    const newChildren = [];
    for (const node of data) {
      const subs = this.filterData(node.children, query);
      if (predicate(node)) {
        newChildren.push(node);
      } else if (subs && subs.length) {
        node.children = subs;
        newChildren.push(node);
      }
    }
    return newChildren.length ? newChildren : [];
  };

  render() {
    const { treeData, selectedKeys, selectedNode } = this.state;
    return (
      <Layout className={style.layout}>
        <Layout.Header className={style.topHeader}>
          {this.formatMessage(messages.printManageTitle)}
        </Layout.Header>
        <Layout className={style.content}>
          <SupResize min={220}>
            <SupTree
              treeKey="code"
              treeTitle="name"
              placeholder={this.formatMessage(messages.placeholder_tree)}
              dataSource={treeData}
              selectedKeys={selectedKeys}
              onSelect={this.handleSelect}
              fuzzyParams={{
                url: '/inter-api/printer/v1/apps',
                param: 'name',
                // eslint-disable-next-line no-undef
                otherParams: `source=${SOURCE}`,
                callback: (data) => {
                  const { data: list } = data || {};
                  if (list) {
                    return list.map((d) => {
                      return { key: d.code, title: d.name, id: d.code, ...d };
                    });
                  }
                }
              }}
              onSearch={(params, type) => {
                let newData = treeData;
                let stdKeys = [];
                let stdNode = null;
                const cloneData = cloneDeep(this.initTreeData);
                if (type === 'clear') {
                  newData = cloneData;
                } else if (type === 'fuzzy') {
                  newData = this.filterData(cloneData, params.title, true);
                } else if (type === 'advanced') {
                  if (params.level !== 1) {
                    stdNode = params;
                    stdKeys = [params.id];
                  }
                  newData = this.filterData(cloneData, params.title, false);
                }
                this.setState({
                  treeData: newData,
                  selectedKeys: stdKeys,
                  selectedNode: stdNode
                });
              }}
            />
            {!selectedNode ? (
              this.renderEmpty()
            ) : (
              <DetailTable selectedNode={selectedNode} />
            )}
          </SupResize>
        </Layout>
      </Layout>
    );
  }
}
