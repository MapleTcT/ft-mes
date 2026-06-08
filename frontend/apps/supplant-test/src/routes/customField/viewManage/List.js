import React from 'react';
import { injectIntl } from 'react-intl';
import SupTable from 'sup-rc-table';
import { Icon, message } from 'sup-ui';
import {
  fetchViewManageList,
  showOrHiddenViewField
} from 'root/services/customProperty';
import messages from '../messages.js';
import { VIEW_ROOT } from '../constant';

class ViewManageList extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.btnColumns = [
      {
        key: 'modify',
        content: intl.formatMessage(messages.btnModify),
        disabled: true,
        callback: () => {
          this.handleModify();
        }
      },
      {
        key: 'sort',
        content: intl.formatMessage(messages.btnSort),
        disabled: true, // 只在有字段启用时才允许操作
        callback: () => {
          this.handleSort();
        }
      },
      {
        key: 'show',
        content: intl.formatMessage(messages.btnShow),
        disabled: true,
        callback: () => {
          this.handleShow();
        }
      },
      {
        key: 'hide',
        content: intl.formatMessage(messages.btnHide),
        disabled: true,
        callback: () => {
          this.handleHide();
        }
      }
    ];
    this.state = {
      selectedItems: [],
      columns: [
        {
          title: intl.formatMessage(messages.number),
          width: 70,
          align: 'center',
          dataIndex: 'index',
          render: (v, r, index) => {
            return <span>{index + 1}</span>;
          }
        },
        {
          title: intl.formatMessage(messages.displayName),
          width: 200,
          dataIndex: 'displayNameInternational',
          render: this.renderDisplayName
        },
        {
          title: intl.formatMessage(messages.showCustom),
          width: 60,
          dataIndex: 'showCustom',
          render: (v) => {
            return v ? '✔' : '';
          }
        },
        {
          title: intl.formatMessage(messages.fieldName),
          width: 200,
          dataIndex: 'property.name',
          render: this.renderText
        },
        {
          title: intl.formatMessage(messages.fieldType),
          width: 100,
          dataIndex: 'property.type',
          render: (v) => {
            const id = messages[`type.${v}`];
            if (id) {
              return intl.formatMessage(id);
            }
            return v;
          }
        },
        {
          title: intl.formatMessage(messages.displayType),
          width: 100,
          dataIndex: 'fieldType',
          render: (v) => {
            const id = messages[`type.${v}`];
            if (id) {
              return intl.formatMessage(id);
            }
            return v;
          }
        },
        {
          title: intl.formatMessage(messages.format),
          width: 100,
          dataIndex: 'format',
          render: (v) => {
            const id = messages[`format.${v}`];
            if (id) {
              return intl.formatMessage(id);
            }
            return v;
          }
        },
        {
          title: intl.formatMessage(messages.nullable),
          width: 60,
          dataIndex: 'nullable',
          render: (v) => {
            // FIXME
            return v ? '✔' : '';
          }
        }
      ],
      dataSource: [],
      total: 0,
      current: 0,
      pageSize: 20
    };
  }

  renderText = (v) => <span title={v}>{v}</span>;

  renderDisplayName = (v, row) => {
    const { isParent, isCollapse } = row;
    const icon = !isCollapse ? 'minus-square' : 'plus-square';
    return isParent ? (
      <span title={v}>
        <Icon
          theme="twoTone"
          type={icon}
          onClick={() => {
            this.toggleModelParent(row);
          }}
          style={{ marginRight: '6px' }}
        />
        {v}
      </span>
    ) : (
      <span title={v}>{v}</span>
    );
  };

  toggleModelParent = (row) => {
    const { code, isCollapse } = row;
    // 过滤不存在的数据
    const { dataSource } = this;
    const parent = dataSource.find((v) => v.code === code);
    // 标识展开状态
    parent.isCollapse = !isCollapse;
    let filteredData = dataSource;
    // 提取isCollapse为true的数据
    const collapsedCodes = dataSource
      .filter((d) => d.isParent && !!d.isCollapse)
      .map((d) => d.code);
    // 不显示收起数据
    if (collapsedCodes.length) {
      filteredData = filteredData.filter(({ parentCode }) => {
        return !~collapsedCodes.indexOf(parentCode);
      });
    }
    this.setState({
      dataSource: filteredData
    });
  };

  getLeafSelectedItem(items) {
    const { selectedItems } = this.state;
    return (items || selectedItems).filter((d) => {
      return !d.isParent;
    });
  }

  resetBtnDisableState(data) {
    this.btnColumns.forEach((b) => {
      // 排除排序
      if (b.key !== 'sort') {
        b.disabled = true;
      } else if (data) {
        b.disabled = !this.hasEnabledFields(data);
      }
    });
  }

  hasEnabledFields(data) {
    return data.some((d) => d.showCustom);
  }

  onSelectItem = (selectedRowKeys, selectedItems) => {
    const btns = this.btnColumns;
    const selectedLeafRowKeys = this.getLeafSelectedItem(selectedItems);
    if (selectedLeafRowKeys.length > 0) {
      btns[2].disabled = false;
      btns[3].disabled = false;
      if (selectedLeafRowKeys.length === 1) {
        btns[0].disabled = false;
      } else {
        btns[0].disabled = true;
      }
    } else {
      this.resetBtnDisableState();
    }
    this.setState({
      selectedItems,
      selectedRowKeys
    });
  };

  handleSort() {
    const { dataSource } = this.state;
    // 统一调整格式为
    /**
     * {
     *  [key]: dataArray
     * }
     */
    const data = dataSource.reduce((d, v) => {
      if (v.isParent) {
        d[v.code] = { ...v, children: [] };
      } else if (v.showCustom) {
        // 只排序显示字段
        d[v.parentCode].children.push(v);
      }
      return d;
    }, {});
    this.props.sortModal(data);
  }

  handleModify() {
    const {
      selectedItems: [editItem]
    } = this.state;
    this.editModal(editItem);
  }

  getShowHiddenParams(flag) {
    const selectedItems = this.getLeafSelectedItem();
    const ids = [];
    const codes = [];
    selectedItems.forEach((row) => {
      if (row.property.code) {
        if (row.showCustom !== flag) {
          if (row.id) {
            ids.push(row.id);
          } else {
            const {
              associatedCode,
              propertyLayRec,
              property: { code }
            } = row;
            const codeObj = {
              associatedCode,
              propertyCode: code,
              propertyLayRec
            };
            codes.push(codeObj);
          }
        }
      }
    });
    return {
      ids,
      codes,
      enabled: flag
    };
  }

  toggleVisible(visible) {
    const { intl } = this.props;
    const data = this.getShowHiddenParams(visible);
    const { ids, codes } = data;
    if (!ids.length && !codes.length) {
      // 不操作相同状态的数据
      message.info(
        intl.formatMessage(
          messages[visible ? 'warningFieldVisible' : 'warningFieldHidden']
        )
      );
    } else {
      showOrHiddenViewField(data).then(() => {
        message.success(intl.formatMessage(messages.operateSuccess));
        this.refreshList();
      });
    }
  }

  handleShow() {
    this.toggleVisible(true);
  }

  handleHide() {
    this.toggleVisible(false);
  }

  editModal(editItem) {
    if (!editItem.isParent) {
      this.props.editModal(editItem);
    }
  }

  handleDblClick = (data) => {
    this.editModal(data);
  };

  componentDidMount() {
    this.refreshList();
  }

  refreshList() {
    const { selectView } = this.props;
    if (VIEW_ROOT !== selectView) {
      return fetchViewManageList({ viewCode: selectView }).then(({ data }) => {
        const { list } = data;
        this.formatViewList(list);
      });
    }
  }

  formatViewList(list) {
    const dataSource = list.reduce((data, model) => {
      data.push({
        ...model,
        list: null,
        isCollapse: false // 标识是否收起, 默认展开
      });
      model.list.reduce((d, modelItem) => {
        modelItem.isCollapse = false;
        d.push(modelItem);
        return d;
      }, data);
      return data;
    }, []);
    // 用于备份实际数据
    this.dataSource = dataSource;
    this.resetBtnDisableState(dataSource);
    this.setState({
      dataSource,
      selectedRowKeys: [],
      selectedItems: [] // 重新加载置空当前选择数据
    });
  }

  updateColumns = (columns) => {
    this.setState({
      columns
    });
  };

  render() {
    const {
      columns,
      dataSource,
      total,
      current,
      pageSize,
      selectedRowKeys
    } = this.state;

    return (
      <SupTable
        onSelectItem={this.onSelectItem}
        tableKey="viewMangeList"
        rowKey="code"
        selectedRowKeys={selectedRowKeys}
        columns={columns}
        updateColumns={this.updateColumns}
        showSearchIcon={false}
        dataSource={dataSource}
        showColumnsFilter={false}
        btnColumns={this.btnColumns}
        onDoubleClick={this.handleDblClick}
        pagination={{
          total,
          current,
          pageSize
        }}
      />
    );
  }
}

export default injectIntl(ViewManageList, { forwardRef: true });
