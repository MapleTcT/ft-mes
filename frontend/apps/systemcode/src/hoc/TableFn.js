/**
 * ==============================================================================
 * 表格功能
 * ==============================================================================
 */

import { message, Modal } from 'sup-ui';
import messages from 'root/common/messages';

const { confirm } = Modal;

const tableFn = (api) => (superClass) => class extends superClass {
  getList = (type, data = []) => {
    const { isTree } = this.props;
    const { keyword, pagination, dataSource, selectedTreeKeys, selectedRowKeys } = this.state;

    // 重新请求数据
    if (type === 'reload') {
      // 清空搜索框
      if (window.tableSearchRef) {
        window.tableSearchRef.resetValue(true);
      } else if (window.codeSearchRef) {
        window.codeSearchRef.resetValue(true);
      }

      return this.setState({
        keyword: '',
        selectedRowKeys: [],
        pagination: {
          ...pagination,
          current: 1
        }
      }, () => {
        this.getList();
      });
    }

    this.setState({
      spinning: true
    }, () => {
      api.getList({
        isTree,
        keyword: keyword.replace(/(^\s*)|(\s*$)/g, ''),
        selectedKey: selectedTreeKeys && selectedTreeKeys[0],
        entityCode: this.props.entityCode,
        ...pagination
      }).then((result = {}) => {
        if (!result.list) {
          return this.setState({
            dataSource: [],
            pagination: {
              ...this.state.pagination,
              current: 1,
              total: 0
            },
            indeterminate: false,
            checkAll: false,
            loading: false,
            hasMore: false
          });
        }

        result.list.forEach((item) => {
          item.current = result.pagination.current;
        });

        const newDataSource = type === 'loadMore'
          ? dataSource.concat(result.list)
          : type === 'delete'
            ? data.concat(result.list)
            : result.list;
        const hasMore = result.pagination.total > result.pagination.current * result.pagination.pageSize;

        this.setState({
          dataSource: newDataSource,
          pagination: result.pagination,
          indeterminate: selectedRowKeys.length !== 0 && (selectedRowKeys.length !== newDataSource.length),
          checkAll: selectedRowKeys.length !== 0 && (selectedRowKeys.length === newDataSource.length),
          loading: false,
          hasMore
        });
      }).finally(() => {
        this.setState({
          spinning: false
        });
      });
    });
  }

  // 搜索table数据
  handleSearch = (value = '') => {
    const { pagination } = this.state;

    // 搜索为全局搜索，取消左侧树的选中状态
    this.initSelectedTreeKeys();
    this.setState({
      keyword: value,
      isSearch: true,
      pagination: {
        ...pagination,
        current: 1
      }
    }, () => {
      this.getList();
    });
  }

  // 修改数据项
  updateItem = (params, isSetDefaultFlag) => {
    const { dataSource } = this.state;
    const newDataSource = dataSource.map((item) => {
      if (item.code === params.code) {
        item = { ...item, ...params };
      } else if (isSetDefaultFlag) {
        item.defaultFlag = 0;
      }
      return item;
    });

    this.setState({
      dataSource: newDataSource
    });
  }

  // 新增修改后更新数据
  submitCallback = (type, params, isSetDefaultFlag) => {
    // 新增数据插入至最前面，需要清空所有数据，重新请求
    if (type === 'add') {
      this.getList('reload');
    } else {
    // 修改数据
      this.updateItem({ ...params }, isSetDefaultFlag);
    }
  }

  // 滚动加载
  handleLoadMore = () => {
    const { pagination } = this.state;

    this.setState({
      loading: true,
      pagination: {
        ...pagination,
        current: pagination.current + 1
      }
    }, () => {
      this.getList('loadMore');
    });
  }

  // 多选
  handleSelectChange = (selectedRowKeys) => {
    const { dataSource } = this.state;
    let allCount = 0;
    dataSource.forEach((item) => {
      if (item.disabled) return;
      allCount += 1;
    });

    this.setState({
      selectedRowKeys,
      checkAll: selectedRowKeys && selectedRowKeys.length !== 0 && allCount === selectedRowKeys.length,
      indeterminate: !!selectedRowKeys.length && selectedRowKeys.length < allCount
    });
  }

  // 全选
  handleCheckAll = (e) => {
    const { dataSource } = this.state;

    const allOptions = [];

    dataSource.forEach((item) => {
      if (item.disabled) return;
      allOptions.push(item.code);
    });

    this.setState({
      selectedRowKeys: e.target.checked ? allOptions : [],
      indeterminate: false,
      checkAll: e.target.checked
    });
  }

  // 点击按钮
  handleClickBtn = (type, data) => {
    if (['batchDelete', 'singleDelete'].includes(type)) {
      this.toggleVisibleDelete(type, data);
    }
  }

  // 显隐删除提示
  toggleVisibleDelete = (type, record = {}) => {
    const { intl } = this.props;

    this.visibleDelete = !this.visibleDelete;

    this.setState({
      deleteCode: record.code,
      deletePage: record.current,
      deleteType: type
    }, () => {
      if (!this.visibleDelete) return;

      confirm({
        title: intl.formatMessage(messages.confirmDelete),
        okText: intl.formatMessage(messages.sure),
        cancelText: intl.formatMessage(messages.cancel),
        onOk: () => {
          this.delete();
        },
        onCancel: () => {
          this.toggleVisibleDelete();
        }
      });
    });
  }

  // 删除
  delete = () => {
    const { deleteCode, deleteType, deletePage, selectedRowKeys, dataSource, pagination } = this.state;
    const { intl, entityCode, isTree } = this.props;

    const list = deleteType === 'singleDelete' ? [deleteCode] : selectedRowKeys;

    api.deleteItem({
      entityCode,
      list
    }).then(() => {
      this.toggleVisibleDelete();
      message.success(intl.formatMessage(messages.successDelete));

      let minPage;
      const newDataSource = [];
      // 单个删除，将该页码以及后面的数据清空
      if (deleteType === 'singleDelete') {
        minPage = deletePage;
        dataSource.forEach((item) => {
          if (item.current >= deletePage) return;
          newDataSource.push({ ...item });
        });
      } else {
      // 批量删除，找到被删除的最小的页码，将该页和后面的数据清空
        dataSource.forEach((item) => {
          if (!selectedRowKeys.includes(item.code) || minPage <= item.current) return;
          minPage = item.current;
        });

        dataSource.forEach((item) => {
          if (item.current >= minPage) return;
          newDataSource.push({ ...item });
        });
      }

      if ((isTree && list.includes(dataSource[0].code)) || dataSource.length === 1) {
        this.initSelectedTreeKeys();
      }

      const newSelectedRowKeys = [];
      selectedRowKeys.forEach((iKey) => {
        if (list.includes(iKey)) return;
        newSelectedRowKeys.push(iKey);
      });

      this.setState({
        selectedRowKeys: newSelectedRowKeys,
        indeterminate: newSelectedRowKeys.length !== 0 && (newSelectedRowKeys.length !== newDataSource.length),
        checkAll: newSelectedRowKeys.length !== 0 && (newSelectedRowKeys.length === newDataSource.length),
        pagination: {
          ...pagination,
          current: minPage
        }
      }, () => {
        this.getTree();
        this.getList('delete', newDataSource);
      });
    });
  }

  // 拖拽列表
  moveRow = (dragIndex, hoverIndex) => {
    const { dataSource } = this.state;

    const prevIndex = dragIndex < hoverIndex ? hoverIndex : hoverIndex - 1;
    const nextIndex = dragIndex < hoverIndex ? hoverIndex + 1 : hoverIndex;

    const { parentId } = dataSource[hoverIndex];
    const { id: prevId } = dataSource[prevIndex] || {};
    const { id: nextId } = dataSource[nextIndex] || {};
    const { id: currentId } = dataSource[dragIndex];

    api.sort({
      parentId,
      prevId,
      nextId,
      currentId
    }).then(() => {
      dataSource.splice(hoverIndex, 0, ...dataSource.splice(dragIndex, 1));
      this.getTree();
      this.setState({
        dataSource
      });
    });
  };
};

export default tableFn;
