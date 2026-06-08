import React, { Component } from 'react';
import { Button, Tree } from 'sup-ui';
import _ from 'lodash';
import styles from './Reporter.less';

const { TreeNode } = Tree;

let isPressCtrl = false;

export default class DataTree extends Component {
  constructor(props) {
    super(props);
    this.state = {
      moveUpAble: false,
      moveDownAble: false,
      bracketAble: false,
      caBracketAble: false,
      editAble: false,
      deleteAble: false
    };
    this.selectedTree = [];
  }

  componentDidMount() {
    // 监听Ctrl按压
    window.onkeydown = function (e) {
      const keyNum = window.event ? e.keyCode : e.which;
      if (keyNum === 17) isPressCtrl = true;
    };
    window.onkeyup = function (e) {
      const keyNum = window.event ? e.keyCode : e.which;
      if (keyNum === 17) isPressCtrl = false;
    };
  }

  componentWillReceiveProps(nextProps) {
    const { data, selectedKeys = [] } = nextProps;
    this.selectedTree = selectedKeys;
    if (data && data.length > 0 && this.selectedTree.length === 0) {
      this.selectedTree = data[0].key;
    }
    const nodes = this.getNodesFromKeys(this.selectedTree, data);
    if (this.selectedTree) {
      this.checkBtnDisabled(nodes, data);
    }
  }

  // 暴露props方法
  onSelectTree = (selectedKeys) => {
    const { data } = this.props;
    const key = selectedKeys[0];
    if (!key) return;
    if (isPressCtrl && this.selectedTree.length > 0) {
      if (_.indexOf(this.selectedTree, key) !== -1) {
        this.selectedTree = _.pull(this.selectedTree, key);
      } else {
        let inBound = false;
        const keys = key.split('-');
        _.map(this.selectedTree, (item) => {
          // 是否并排
          const items = item.split('-');
          if ((parseInt(keys[keys.length - 1], 10) + 1) === parseInt(items[items.length - 1], 10) || (parseInt(keys[keys.length - 1], 10) - 1) === parseInt(items[items.length - 1], 10)) inBound = true;
        });
        if (this.selectedTree[0].split('-').length !== keys.length || !inBound) return;
        this.selectedTree.push(key);
      }
    } else {
      this.selectedTree = [key];
    }
    const nodes = this.getNodesFromKeys(this.selectedTree, data);
    this.checkBtnDisabled(nodes, data);
    this.props.selectChanged(this.selectedTree, nodes);
  }

  /**
   * 添加节点
   */
  addTreeNodeAction = () => {
    this.props.addTreeNodeAction(this.selectedTree[0] || '');
  }

  /**
   * 编辑节点
   */
  editTreeNodeAction = () => {
    if (this.selectedTree.length <= 0) return;
    const { data } = this.props;
    const nodes = this.getNodesFromKeys(this.selectedTree, data);
    this.props.editTreeNodeAction(this.selectedTree[0], nodes[0]);
  }

  /**
   * 删除节点
   */
  deleteTreeNodeAction = () => {
    if (this.selectedTree.length <= 0) return;
    const { data } = this.props;
    const nodes = this.getNodesFromKeys(this.selectedTree, data);
    this.props.deleteTreeNodeAction(this.selectedTree, nodes);
  }

  /**
   * 上移
   */
  moveUpAction = () => {
    if (this.selectedTree.length <= 0) return;
    const keys = this.selectedTree[0].split('-');
    if (parseInt(keys[keys.length - 1], 10) === 0) return;
    this.props.moveUpAction(this.selectedTree[0]);
  }

  /**
   * 下移
   */
  moveDownAction = () => {
    if (this.selectedTree.length <= 0) return;
    this.props.moveDownAction(this.selectedTree[0]);
  }

  /**
   * 添加括号
   */
  addBracketAction = () => {
    if (this.selectedTree.length <= 1) return;
    this.props.addBracketAction(this.selectedTree);
  }

  /**
   * 去除括号
   */
  removeBracketAction = () => {
    this.props.removeBracketAction(this.selectedTree[0]);
  }

  checkBtnDisabled = (nodes, nodesTreeList) => {
    const keys = this.selectedTree && this.selectedTree.length > 0 ? this.selectedTree[0].split('-') : [];
    // edit able
    const editAble = this.selectedTree.length === 1;
    //  delete able
    const deleteAble = this.selectedTree.length >= 1;
    // up able
    const moveUpAble = this.selectedTree.length === 1 && _.last(keys) !== '0';
    // addBrack able
    const bracketAble = this.selectedTree.length >= 2;
    // removeBrack able
    const caBracketAble = this.selectedTree.length === 1 && nodes[0] && nodes[0].children.length > 0;
    // down able
    let list = nodesTreeList;
    let curNodes = [];
    _.forEach(keys, (key, idx) => {
      if (idx === keys.length - 1) {
        // const index = parseInt(key, 10);
        curNodes = _.cloneDeep(list);
      } else {
        list = list[parseInt(key, 10)].children;
      }
    });
    const moveDownAble = this.selectedTree.length === 1 && curNodes.length !== (parseInt(_.last(keys), 10) + 1);
    this.setState({
      moveUpAble,
      moveDownAble,
      bracketAble,
      caBracketAble,
      editAble,
      deleteAble
    });
  }

  getNodesFromKeys = (keys, nodesTree) => {
    if (!nodesTree || nodesTree.length === 0) return [];
    const nodes = [];
    _.forEach(keys, (key) => {
      const arr = key.split('-');
      let list = _.cloneDeep(nodesTree);
      _.forEach(arr, (item, idx) => {
        if (idx + 1 === arr.length) {
          nodes.push(list[parseInt(item, 10)]);
        } else {
          list = list[parseInt(item, 10)].children;
        }
      });
    });
    return nodes;
  }

  // 渲染函数
  renderTreeNodes = (data) => {
    const { intl } = this.props;
    return data.map((item) => {
      const title = Object.prototype.toString.call(item.title) !== '[object Object]' ? item.title : intl.formatMessage(item.title);
      const nodeClass = _.indexOf(this.selectedTree, item.key) !== -1 ? styles.nodeSelected : '';
      if (item.children) {
        return (
          <TreeNode title={title} key={item.key} dataRef={item} className={`${nodeClass}`}>
            {this.renderTreeNodes(item.children)}
          </TreeNode>
        );
      }
      return <TreeNode {...Object.assign(item, { title })} dataRef={item} className={`${nodeClass}`} />;
    });
  }

  render() {
    const { data } = this.props;
    const { moveUpAble, moveDownAble, bracketAble, caBracketAble, editAble, deleteAble } = this.state;
    return (
      <div className={styles.dataTreeContainer}>
        <div className={styles.mainContainer}>
          <div className={styles.treeEditContainer}>
            <Tree
              showLine
              showIcon
              defaultExpandAll
              onSelect={(select) => this.onSelectTree(select)}
            >
              {this.renderTreeNodes(data)}
            </Tree>
          </div>
        </div>
        <div className={styles.toolBarsContainer}>
          <Button type="primary" onClick={this.addTreeNodeAction}>
            <i className={`${styles.iconAddBtn} ${styles.iconSmall}`} />
            新增
          </Button>
          <Button type="primary" onClick={this.editTreeNodeAction} disabled={!editAble}>
            <i className={`${styles.iconEditBtn} ${styles.iconSmall} ${editAble ? '' : styles.disabled}`} />
            修改
          </Button>
          <Button onClick={this.deleteTreeNodeAction} disabled={!deleteAble}>删除</Button>
          <Button disabled={!moveUpAble} onClick={this.moveUpAction}>上移</Button>
          <Button disabled={!moveDownAble} onClick={this.moveDownAction}>下移</Button>
          <Button disabled={!bracketAble} onClick={this.addBracketAction}>
            <i className={`${styles.iconBracketBtn} ${styles.iconSmall} ${bracketAble ? '' : styles.disabled}`} />
            添加括号
          </Button>
          <Button disabled={!caBracketAble} onClick={this.removeBracketAction}>
            <i className={`${styles.iconCaBracketBtn} ${styles.iconSmall} ${caBracketAble ? '' : styles.disabled}`} />
            去掉括号
          </Button>
        </div>
      </div>
    );
  }
}
