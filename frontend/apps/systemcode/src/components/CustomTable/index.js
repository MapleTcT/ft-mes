import React, { Component } from 'react';
import { Button, List, Spin, Checkbox, Divider } from 'sup-ui';
import { DndProvider } from 'react-dnd';
import HTML5Backend from 'react-dnd-html5-backend';
import InfiniteScroll from 'react-infinite-scroller';
import SupSearch from 'sup-rc-search';
import messages from 'root/common/messages';
import BodyRow from './BodyRow';
import styles from './index.less';

class CustomTable extends Component {
  constructor(props) {
    super(props);

    const { columns } = props;

    this.columns = columns.map((item) => {
      // 操作栏鼠标移入才显示
      if (item.key === 'operation') {
        const newItem = { ...item };
        newItem.render = (text, record) => {
          if (this.state.showRow !== record.code) {
            return (
              <span>...</span>
            );
          }

          return item.render(text, record);
        };
        return newItem;
      }
      return item;
    });

    this.onRow = (record, index) => {
      const obj = props.drag ? {
        moveRow: this.moveRow
      } : {};

      return {
        index,
        onMouseEnter: () => { this.handleEnter(record); },
        onMouseLeave: this.handleLeave,
        ...obj
      };
    };

    this.state = {
      showRow: '',
      showScrollTop: false
    };
  }

  componentDidMount() {
    this.checkScrollbar();

    window.tableSearchRef = this.search;
  }

  componentDidUpdate() {
    this.checkScrollbar();
  }

  componentWillUnmount() {
    window.tableSearchRef = null;
  }

  // 监测表身是否出现滚动条，目的：表头表身宽度对齐
  checkScrollbar = () => {
    if (!this.tBody || !this.tHeader) return;

    const scrollbarWidth = `${this.tBody.offsetWidth - this.tBody.clientWidth}px`;

    if (this.tHeader.style.paddingRight !== scrollbarWidth) {
      this.tHeader.style.paddingRight = scrollbarWidth;
    }
  }

  // 鼠标移入行，显示编辑
  handleEnter = (record) => {
    this.setState({
      showRow: record.code
    });
  }

  // 鼠标移出行，隐藏编辑
  handleLeave = () => {
    this.setState({
      showRow: ''
    });
  }

  // 删除
  toggleVisibleDelete = () => {
    const { toggleVisibleDelete } = this.props;

    if (toggleVisibleDelete) {
      toggleVisibleDelete('', 'batchDelete');
    }
  }

  handleSearch = (value) => {
    const { onsearch } = this.props;

    if (onsearch) {
      onsearch(value);
    }

    this.resetScrollTop();
  }

  handleScroll = () => {
    this.setState({
      showScrollTop: this.tBody.scrollTop > 0
    });
  }

  // 返回顶部
  scrollTop = () => {
    this.tBody.scrollTop = 0;
  }

  // 重置返回顶部
  resetScrollTop = () => {
    this.scrollTop();
    this.handleScroll();
  }

  renderItem = (data) => {
    const { rowKey, size } = this.props;
    return (
      <List.Item key={data[rowKey]} onMouseEnter={() => this.handleEnter(data)} onMouseLeave={this.handleLeave}>
        <ul
          className={styles.tableItem}
          style={{
            height: size === 'small' ? 40 : 44,
            fontSize: 12
          }}
        >
          <li className={styles.checkbox}>
            <Checkbox value={data[rowKey]} disabled={data.disabled} />
          </li>
          {
            this.columns && this.columns.map((item) => {
              const title = item.showTitle ? data[item.key] : '';

              if (item.authority && item.authority() === false) return null;

              return (
                <li
                  className={styles.td}
                  key={item.key}
                  style={{
                    flexBasis: item.width || '0%',
                    flexShrink: item.width ? 0 : 1,
                    flexGrow: item.width ? 0 : 1,
                    textAlign: item.align || 'left'
                  }}
                  title={title}
                >
                  {
                    item.render ? item.render(data[item.key], data) : data[item.key]
                  }
                </li>
              );
            })
          }
        </ul>
      </List.Item>
    );
  }

  renderTBody = () => {
    const {
      dataSource,
      selectedRowKeys,
      onselectchange,
      loading,
      hasMore,
      onloadmore,
      drag,
      moveRow,
      fixeFirstItem,
      noLoad
    } = this.props;

    const item = (data, i) => {
      // 固定首项
      if ((fixeFirstItem && i === 0) || !drag) {
        return this.renderItem(data);
      }

      return (
        <BodyRow
          className={styles.bodyRow}
          index={i}
          moveRow={moveRow}
        >
          {this.renderItem(data)}
        </BodyRow>
      );
    };

    return (
      <div
        style={{ width: '100%', height: '100%', overflow: 'hidden' }}
        className={styles.tableBody}
      >
        <Checkbox.Group
          value={selectedRowKeys}
          style={{ width: '100%', height: '100%', overflow: 'hidden' }}
          onChange={onselectchange}
        >
          <div
            ref={(ref) => { this.tBody = ref; }}
            style={{ width: '100%', height: '100%', overflowX: 'hidden', overflowY: 'auto' }}
            className={styles.tableBody}
            onScroll={this.handleScroll}
          >
            <InfiniteScroll
              initialLoad={false}
              pageStart={1}
              loadMore={onloadmore}
              hasMore={!loading && hasMore}
              useWindow={false}
            >
              <List
                dataSource={dataSource}
                renderItem={item}
              >
                {
                  !noLoad && loading && hasMore && (
                    <div className={styles.loadingBox}>
                      <Spin />
                    </div>
                  )
                }
              </List>
            </InfiniteScroll>
          </div>
        </Checkbox.Group>
      </div>
    );
  }

  render() {
    const { showScrollTop } = this.state;
    const {
      onsearch,
      selectedRowKeys = [],
      addDisabled,
      className = '',
      size,
      drag,
      indeterminate,
      checkAll,
      oncheckall,
      clickBtn,
      intl,
      total,
      placeholder,
      showAdd,
      showDelete
    } = this.props;
    const disabled = selectedRowKeys.length === 0;

    return (
      <div className={`${styles.container} ${className}`}>
        <div className={styles.header}>
          <div className={styles.btnGroup}>
            {
              showAdd && (
              <Button
                className={`${styles.addBtn} ${addDisabled ? styles.disabledBtn : ''}`}
                icon="plus"
                disabled={addDisabled}
                onClick={() => clickBtn('add')}
              >
                {intl.formatMessage(messages.add)}
              </Button>
              )
            }
            {
              showAdd && showDelete
              && <Divider className={styles.divider} type="vertical" />
            }
            {
              showDelete && (
              <Button
                className={styles.deleteBtn}
                icon="delete"
                disabled={disabled}
                onClick={() => clickBtn('batchDelete')}
              />
              )
            }
          </div>
          {
            onsearch && (
              <SupSearch
                ref={(ref) => { this.search = ref; }}
                size="small"
                style={{
                  marginRight: 10
                }}
                placeholder={placeholder}
                onSearch={this.handleSearch}
              />
            )
          }
        </div>
        <div className={styles.tableBox}>
          <ul
            ref={(ref) => { this.tHeader = ref; }}
            className={styles.tableHeader}
            style={{ height: size === 'small' ? 26 : 30 }}
          >
            <li className={styles.checkbox}>
              <Checkbox
                indeterminate={indeterminate}
                checked={checkAll}
                onChange={oncheckall}
              />
            </li>
            {
              this.columns && this.columns.map((item) => {
                return (
                  <li
                    className={styles.th}
                    key={item.key}
                    style={{
                      flexBasis: item.width || '0%',
                      flexShrink: item.width ? 0 : 1,
                      flexGrow: item.width ? 0 : 1,
                      textAlign: item.align || 'left'
                    }}
                  >
                    {item.title}
                  </li>
                );
              })
            }
          </ul>
          {
            drag ? (
              <DndProvider backend={HTML5Backend}>
                {this.renderTBody()}
              </DndProvider>
            ) : this.renderTBody()
          }
        </div>
        <div className={styles.totalPage}>
          <span>{intl.formatMessage(messages.totalPage, { total })}</span>
          {
            showScrollTop && (
              <div
                className={styles.scrollTop}
                onClick={this.scrollTop}
              >
                {intl.formatMessage(messages.backTop)}
              </div>
            )
          }
        </div>
      </div>
    );
  }
}

export default CustomTable;
