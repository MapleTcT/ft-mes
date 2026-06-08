/*
 * @Author: DWP
 * @Date: 2020-07-27 16:59:27
 * @LastEditors: DWP
 * @LastEditTime: 2021-02-02 14:33:00
 */
/**
 * ==============================================================================
 * 树控件功能
 * ==============================================================================
 */
import _ from 'lodash';

// 树结构平铺数组
const treeToArray = (data = [], parentKey, list = []) => {
  if (!data || data.length === 0) return list;

  data.forEach((item) => {
    list.push({
      key: `${item.key}`,
      title: item.title,
      name: item.name,
      parentId: parentKey ? `${parentKey}` : ''
    });

    if (item.children) {
      return treeToArray(item.children, `${item.key}`, list);
    }
  });
  return list;
};

/**
  * 数组转树
  * @param {array} list 数组
  * @param {string} key 唯一值字段
  * @param {string} parentKey 父元素字段
  */
const listToTree = (list, parentKeys) => {
  const tree = [];
  if (!Array.isArray(list)) return tree;

  list.forEach((item) => {
    delete item.children;
  });

  const filterList = [];
  const map = {};
  list.forEach((item) => {
    const key = `${item.key}`;
    if (!parentKeys.includes(key)) return;
    map[key] = item;
    filterList.push(item);
  });

  filterList.forEach((item) => {
    const parentId = map[`${item.parentId}`];
    if (parentId) {
      if (!parentId.children) {
        parentId.children = [];
      }
      parentId.children.push(item);
    } else {
      tree.push(item);
    }
  });

  return tree;
};

/**
 * 获取所有父节点
 * @param {*} keys 当次查询的节点
 * @param {*} dataList 需查询的数据集合
 * @param {*} result 输出的父节点
 */
const getParentKeys = (keys, dataList, result = []) => {
  const newKeys = [];
  const residualDataList = [];
  // 对当前需要查询的节点进行遍历，获取这些节点的父节点集合
  keys.forEach((key) => {
    dataList.forEach((item) => {
      if (!item.parentId || newKeys.includes(item.parentId)) return;

      if (item.key !== key) {
        return residualDataList.push(item);
      } else {
        newKeys.push(item.parentId);
      }
    });
  });

  // 输出的父节点整合当前节点集合
  result = [...new Set(result.concat(keys))];

  if (newKeys.length) {
    result = [...new Set(result.concat(newKeys))];
    return getParentKeys(newKeys, residualDataList, result);
  }

  return result;
};

const tableFn = (api) => (superClass) => class extends superClass {
  getTree = (isInit) => {
    const { entityCode } = this.props;

    api.getTree({
      entityCode
    }).then((res) => {
      // 默认选中第一个节点
      const obj = isInit ? { selectedTreeKeys: [`${res[0].key}`], selectedTreeName: res[0].title } : {};

      this.treeData = res;
      this.treeList = treeToArray(res);

      this.setState({
        treeData: res,
        ...obj
      }, () => {
        if (!isInit) return;
        this.getList();
      });
    });
  }

  handleSearchTree = ({ title, id }, type) => {
    const key = `${id}`;
    let selectedTreeKeys = [];
    let selectedTreeName = '';
    let selectedTreeI18nKey = '';
    let treeData = [];

    let parentKeys = [];
    const keys = [];
    // 模糊搜索
    if (type === 'fuzzy') {
      this.treeList.forEach((item) => {
        if (item.title.includes(title)) {
          keys.push(`${item.key}`);
        }
      });
    }

    // 精确匹配
    if (type === 'advanced') {
      this.treeList.forEach((item) => {
        if (item.key === key) {
          selectedTreeKeys = [key];
          selectedTreeName = item.title;
          selectedTreeI18nKey = item.name;
          this.setState({
            selectedTreeKeys: [key]
          });
        }
      });
    }

    parentKeys = getParentKeys(type === 'fuzzy' ? keys : [key], this.treeList);
    treeData = listToTree(this.treeList, parentKeys);

    // 选择左侧树
    this.handleSelectTree(selectedTreeKeys, { node: { props: { item: { title: selectedTreeName, name: selectedTreeI18nKey } } } });
    this.setState({
      treeData: type === 'clear' ? _.cloneDeep(this.treeData) : treeData
    });
  }

  // 选择左侧树
  handleSelectTree = (selectedKeys, info) => {
    this.setState({
      selectedTreeKeys: selectedKeys,
      selectedTreeName: selectedKeys.length ? _.get(info, 'node.props.item.title') : '',
      selectedTreeI18nKey: selectedKeys.length ? _.get(info, 'node.props.item.name') : '',
      selectedRowKeys: [],
      indeterminate: false,
      checkAll: false,
      isSearch: false
    }, () => {
      this.getList('reload');
    });
  }

  // 拖拽树节点
  dragTreeNode = (parentId, prevId, nextId, currentId, parentName) => {
    api.sort({
      parentId,
      prevId,
      nextId,
      currentId,
      parentName
    }).then(() => {
      this.getTree();
      this.getList('reload');
    });
  }
};

export default tableFn;
