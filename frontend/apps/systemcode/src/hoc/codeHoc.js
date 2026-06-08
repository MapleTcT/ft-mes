/*
 * @Author: DWP
 * @Date: 2020-08-19 17:58:41
 * @LastEditors: DWP
 * @LastEditTime: 2021-04-12 10:33:46
 */
/**
 * ==============================================================================
 * 高阶组件
 * 作用：提取编码/编码值页面公共方法
 * ==============================================================================
 */

import React from 'react';
import TableFn from './TableFn';
import TreeFn from './TreeFn';

function getDisplayName(WrapperComponent) {
  return WrapperComponent.displayName || WrapperComponent.name || 'Component';
}

export default (api) => (WrapperComponent) => {
  let mixComponent = TableFn(api)(WrapperComponent);
  mixComponent = TreeFn(api)(mixComponent);

  return class extends mixComponent {
    static displayName = `CodeHoc(${getDisplayName(WrapperComponent)})`;

    state = {
      keyword: '',
      loading: false,
      spinning: false,
      hasMore: false,
      selectedRowKeys: [],
      selectedTreeKeys: this.props.modalType !== 'codeValueMgr' && this.props.source === 'lcdp' && this.props.moduleId ? [this.props.moduleId] : [],
      selectedTreeName: this.props.modalType !== 'codeValueMgr' && this.props.source === 'lcdp' && this.props.moduleName ? this.props.moduleName : '',
      treeData: [],
      selectedTreeI18nKey: '',
      dataSource: [],
      pagination: {
        current: 1,
        pageSize: 50,
        total: 0
      }
    }

    componentDidMount() {
      const { isTree } = this.props;
      if (isTree) {
        this.getTree(isTree);
      } else {
        if (isTree === undefined) {
          this.getTree();
        }
        this.getList();
      }
    }

    // 重置选中模块
    initSelectedTreeKeys = () => {
      const { source, moduleId, moduleName, modalType } = this.props;
      this.setState({
        selectedTreeKeys: modalType !== 'codeValueMgr' && source === 'lcdp' && moduleId ? [moduleId] : [],
        selectedTreeName: modalType !== 'codeValueMgr' && source === 'lcdp' && moduleName ? moduleName : ''
      });
    }

    render() {
      const { isSearch, keyword, loading, hasMore, selectedRowKeys, selectedTreeName, selectedTreeI18nKey } = this.state;
      return (
        <WrapperComponent
          {...this.props}
          {...this.state}
          onsearchtree={this.handleSearchTree}
          onselecttree={this.handleSelectTree}
          isSearch={isSearch}
          keyword={keyword}
          loading={loading}
          hasMore={hasMore}
          selectedRowKeys={selectedRowKeys}
          selectedTreeName={selectedTreeName}
          selectedTreeI18nKey={selectedTreeI18nKey}
          onchange={this.changeSearch}
          onsearch={this.handleSearch}
          onloadmore={this.handleLoadMore}
          oncheckall={this.handleCheckAll}
          onselectchange={this.handleSelectChange}
          clickBtn={this.handleClickBtn}
          getList={this.getList}
          getTree={this.getTree}
          submitCallback={this.submitCallback}
          moveRow={this.moveRow}
          dragTreeNode={this.dragTreeNode}
        />
      );
    }
  };
};
