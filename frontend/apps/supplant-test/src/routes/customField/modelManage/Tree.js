import React from 'react';
import { injectIntl } from 'react-intl';
import SupTree from 'sup-rc-tree';
import { fetchTree } from 'root/services/customProperty';

const TREE_KEY = 'code';
const TREE_NAME = 'name';
const NODE_TYPE_MODULE = 'module';
const NODE_TYPE_ENTITY = 'entity';
const NODE_TYPE_MODEL = 'model';

const NODE_TYPE_TREE = [NODE_TYPE_MODULE, NODE_TYPE_ENTITY, NODE_TYPE_MODEL];

class ModelManageTree extends React.Component {
  state = {
    treeData: [],
    selectedKeys: []
  };

  handleSelect = (selectedKeys, event) => {
    const { isParent } = event.node.props.item;
    this.setState({
      selectedKeys
    });
    if (!isParent) {
      const { handleClickModel } = this.props;
      handleClickModel(selectedKeys[0]);
    }
  };

  componentDidMount() {
    fetchTree({
      type: NODE_TYPE_MODULE
    }).then(({ data }) => {
      const { list } = data;
      list.forEach((element) => {
        element.isLeaf = false;
        element.type = NODE_TYPE_MODULE;
      });
      // const treeData = [
      //   {
      //     [TREE_NAME]: '模型管理',
      //     [TREE_KEY]: '$root',
      //     children: list,
      //     isLeaf: false
      //   }
      // ];
      this.setState({
        treeData: list
      });
    });
  }

  addListType(list, type) {
    list.forEach((element) => {
      const { isParent } = element;
      element.isLeaf = !isParent;
      if (isParent) {
        element.type = type;
      }
    });
  }

  onLoadData = (treeNode) => {
    // eslint-disable-next-line compat/compat
    return new Promise((resolve) => {
      const {
        props: { item }
      } = treeNode;
      const { type, code } = item;
      const index = NODE_TYPE_TREE.indexOf(type);
      const nextLevelType = NODE_TYPE_TREE[index + 1];
      fetchTree({
        type: nextLevelType,
        code
      }).then(({ data }) => {
        const { list } = data;
        this.addListType(list, nextLevelType);
        item.children = list;
        const { treeData } = this.state;
        this.setState({
          treeData: [...treeData]
        });
        resolve();
      });
    });
  };

  render() {
    const { treeData, selectedKeys } = this.state;
    return (
      <SupTree
        autoExpandRoot
        draggable={false}
        addDisabled
        showSearch={false}
        dataSource={treeData}
        onSelect={this.handleSelect}
        treeKey={TREE_KEY}
        treeTitle={TREE_NAME}
        loadData={this.onLoadData}
        selectedKeys={selectedKeys}
      />
    );
  }
}

export default injectIntl(ModelManageTree);
