/*
 * @Author: DWP
 * @Date: 2020-07-21 11:24:56
 * @LastEditors: DWP
 * @LastEditTime: 2021-01-21 09:18:16
 */
import React, { Component } from 'react';
import { Icon } from 'sup-ui';
import SupTree from 'sup-rc-tree';
import messages from 'root/common/messages';
import styles from './index.less';

class MenuTree extends Component {
  // 选中
  handleSelect = (keys, event, status) => {
    const { onSelect } = this.props;

    if (keys.length === 0) return;

    if (onSelect) {
      onSelect(keys[0], event, status);
    }
  }

  // 新增
  handleClickAdd = () => {
    const { selectedKeys, onClickBtn } = this.props;

    if (!selectedKeys || !onClickBtn) return;

    onClickBtn('addMenu');
  }

  // 启用停用
  toggleDisabled = (e, id, name, type) => {
    const { onClickBtn } = this.props;

    if (e) {
      e.stopPropagation();
    }

    onClickBtn(type, id, name);
  }

  // 删除
  handleDelete = (e, code, name) => {
    const { onClickBtn } = this.props;

    if (e) {
      e.stopPropagation();
    }

    onClickBtn('delete', code, name);
  }

  // 拖拽
  handleDrag = (info) => {
    const { dragTreeNode } = this.props;
    const dropId = info.node.props.eventKey;
    const currentId = info.dragNode.props.eventKey;
    const { dataSource } = this.props;
    const dropPos = info.node.props.pos.split('-');
    const dropPosition = info.dropPosition - Number(dropPos[dropPos.length - 1]);
    let parentId;
    let prevId;
    let nextId;

    const loop = (data, key, callback) => {
      data.forEach((item, index) => {
        if (item.id === key) {
          parentId = item.parentId;

          // 拖至节点前
          if (dropPosition === -1) {
            prevId = index === 0 ? '' : data[index - 1].id;
            nextId = item.id;
          } else if (dropPosition === 1) {
            // 拖至节点后
            prevId = item.id;
            nextId = index === data.length - 1 ? '' : data[index + 1].id;
          } else {
            // 拖拽至节点内部
            parentId = item.id;
          }
        }
        if (item.children) {
          return loop(item.children, key, callback);
        }
      });
    };

    loop(dataSource, dropId - 0);

    if (dragTreeNode) {
      dragTreeNode(parentId, prevId, nextId, currentId);
    }
  }

  render() {
    const {
      intl,
      dataSource,
      visibleDisabledMenu,
      disabledDataSource,
      toggleVisibleDisabledMenu,
      isEnable,
      selectedKeys,
      showAdd,
      authorityList,
      onSearch,
      loading
    } = this.props;
    const optBtns = (record) => {
      if (window.menuSource === 'supplant') {
        // 根节点或没权限不可删除
        if (record.id === '-1' || !authorityList.includes('deleteMenuConfigure')) return null;

        return (
          <Icon
            title={intl.formatMessage(messages.delete)}
            type="delete"
            onClick={(e) => this.handleDelete(e, record.code, record.nameDisplay)}
          />
        );
      }

      if (!authorityList.includes('enableMenu')) return null;

      return (
        <Icon
          title={intl.formatMessage(messages.disabledMenu)}
          type="pause-circle"
          theme="twoTone"
          onClick={(e) => this.toggleDisabled(e, record.id, record.nameDisplay, 'stopMenu')}
        />
      );
    };

    const blockOptBtns = (record) => {
      if (!authorityList.includes('enableMenu')) return null;
      return (
        <Icon
          title={intl.formatMessage(messages.startMenu)}
          type="play-circle"
          theme="twoTone"
          onClick={(e) => this.toggleDisabled(e, record.id, record.nameDisplay, 'startMenu')}
        />
      );
    };

    return (
      <div className={styles.container}>
        <div className={`${styles.tree} ${!showAdd ? styles.noAddTree : ''}`}>
          <SupTree
            ref={(ref) => { this.treeRef = ref; }}
            showAdd={showAdd}
            loading={loading}
            draggable={authorityList.includes(window.menuSource === 'supplant' ? 'editMenuConfigure' : 'editMenu')}
            treeKey="id"
            treeTitle="nameDisplay"
            autoExpandRoot
            type={window.menuSource === 'supplant' ? 'singleRoot' : 'multiRoot'}
            rootId="-1"
            fuzzyParams={{
              url: window.menuSource === 'supos' ? '/inter-api/rbac/v1/resources/runtime/associate' : '/inter-api/rbac/v1/menus/associate',
              param: 'keyword',
              otherParams: 'size=10000&enable=true',
              callback: (data) => {
                const { list } = data;
                const options = list.map((item) => { return { key: item.code, title: item.nameDisplay, id: item.id }; });

                return options;
              }
            }}
            addDisabled={selectedKeys.length === 0}
            dataSource={dataSource}
            placeholder={intl.formatMessage(messages.menuSearch)}
            selectedKeys={isEnable ? selectedKeys : []}
            optRender={optBtns}
            onSearch={onSearch}
            onSelect={(keys, event) => this.handleSelect(keys, event, true)}
            onAdd={this.handleClickAdd}
            onDrag={this.handleDrag}
          />
        </div>
        {
          window.menuSource !== 'supplant' && (
            <div
              className={styles.disabledMenu}
              style={{ height: visibleDisabledMenu ? 400 : 30 }}
            >
              <header
                className={styles.header}
                onClick={toggleVisibleDisabledMenu}
              >
                <span>{intl.formatMessage(messages.disabledMenu)}</span>
                <Icon
                  type="down"
                  className={styles.downIcon}
                  style={{
                    transform: visibleDisabledMenu ? 'rotate(180deg)' : 'none'
                  }}
                />
              </header>
              <div className={styles.blockTree}>
                <SupTree
                  loading={loading}
                  showSearch={false}
                  treeKey="id"
                  treeTitle="nameDisplay"
                  dataSource={disabledDataSource}
                  selectedKeys={!isEnable ? selectedKeys : []}
                  optRender={blockOptBtns}
                  onSelect={(keys, event) => this.handleSelect(keys, event, false)}
                />
              </div>
            </div>
          )
        }
      </div>
    );
  }
}

export default MenuTree;
