import React from 'react';
import { injectIntl } from 'react-intl';
import SupTable from 'sup-rc-table';
import { message, Modal } from 'sup-ui';
import {
  fetchModelManageList,
  batcheUpdateEnabledStatus
} from 'root/services/customProperty';
import messages from '../messages.js';
import {
  MODEL_ROOT,
  PROPERTY_TYPE,
  ASSOC_CODE,
  FILL_CONTENT,
  REFVIEW_CODE
} from '../constant';

class ModelManageList extends React.Component {
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
        key: 'enable',
        content: intl.formatMessage(messages.btnEnable),
        disabled: true,
        callback: () => {
          this.handleEnable();
        }
      },
      {
        key: 'disable',
        content: intl.formatMessage(messages.btnDisable),
        disabled: true,
        callback: () => {
          this.handleDisable();
        }
      }
    ];
    this.state = {
      selectedItems: [],
      selectedRowKeys: [],
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
          title: intl.formatMessage(messages.enable),
          width: 60,
          dataIndex: 'enableCustom',
          render: (v) => {
            return v ? '✔' : '';
          }
        },
        {
          title: intl.formatMessage(messages.fieldName),
          width: 200,
          dataIndex: 'property.name'
        },
        {
          title: intl.formatMessage(messages.displayName),
          width: 200,
          dataIndex: 'displayNameInternational'
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
        },
        {
          title: intl.formatMessage(messages.desc),
          width: 100,
          dataIndex: 'description'
        }
      ],
      dataSource: [],
      total: 0,
      current: 0,
      pageSize: 20
    };
  }

  onSelectItem = (selectedRowKeys, selectedItems) => {
    const btns = this.btnColumns;
    if (selectedRowKeys.length > 0) {
      btns[2].disabled = false; // 启用
      btns[3].disabled = false; // 停用
      if (selectedRowKeys.length === 1) {
        btns[0].disabled = false; // 修改
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
    return data.some((d) => d.enableCustom);
  }

  handleModify() {
    const {
      selectedItems: [editItem]
    } = this.state;
    this.editModal(editItem);
  }

  handleSort() {
    const { dataSource } = this.state;
    const enabledFields = dataSource.filter((d) => d.enableCustom);
    this.props.sortModal(enabledFields);
  }

  warning(msgs) {
    Modal.info({
      title: '提示',
      content: (
        <div>
          <span>
            {msgs.map((l) => {
              return <p key={l}>{l}</p>;
            })}
          </span>
        </div>
      )
    });
  }

  checkEnableInValidItems(disableItems) {
    const { intl } = this.props;
    return disableItems
      .filter((d) => {
        const { type } = d.property;
        const { OBJECT, SYSTEMCODE } = PROPERTY_TYPE;
        return (
          // 对象需开启目标字段, 参照视图
          // 系统编码字段需选择字段
          // eslint-disable-next-line operator-linebreak
          (type === OBJECT && (!d[REFVIEW_CODE] || !d[ASSOC_CODE])) ||
          (type === SYSTEMCODE && !d[FILL_CONTENT])
        );
      })
      .map((d) => {
        const { type } = d.property;
        return intl.formatMessage(messages[`warningConfig${type}`], {
          name: d.displayNameInternational
        });
      });
  }

  genEnabledStatusData(items, enabled) {
    return items.reduce(
      (d, item) => {
        const {
          property: { code },
          id
        } = item;
        if (id) {
          d.ids.push(id);
        } else {
          d.codes.push(code);
        }
        return d;
      },
      {
        codes: [],
        enabled,
        ids: []
      }
    );
  }

  // 启用
  handleEnable() {
    const { intl } = this.props;
    const { selectedItems } = this.state;
    // 过滤未启用字段
    const disableItems = selectedItems.filter((d) => !d.enableCustom);
    if (disableItems.length === 0) {
      message.info(intl.formatMessage(messages.warningFieldEnable));
    } else {
      // 检查缺少配置项的字段
      const inValidItems = this.checkEnableInValidItems(disableItems);
      if (inValidItems.length) {
        this.warning(inValidItems);
      } else {
        const data = this.genEnabledStatusData(disableItems, true);
        batcheUpdateEnabledStatus(data).then(() => {
          message.success(intl.formatMessage(messages.enableSuccess));
          this.refreshList();
        });
      }
    }
  }

  // 禁用
  handleDisable() {
    const { intl } = this.props;
    const { selectedItems } = this.state;
    // 过滤未启用字段
    const enableItems = selectedItems.filter((d) => d.enableCustom);
    if (enableItems.length === 0) {
      message.info(intl.formatMessage(messages.warningFieldDisable));
    } else {
      const data = this.genEnabledStatusData(enableItems, false);
      batcheUpdateEnabledStatus(data).then(() => {
        message.success(intl.formatMessage(messages.disableSuccess));
        this.refreshList();
      });
    }
  }

  editModal(editItem) {
    this.props.editModal(editItem);
  }

  handleDblClick = (data) => {
    this.editModal(data);
  };

  componentDidMount() {
    this.refreshList();
  }

  refreshList() {
    const { selectModel } = this.props;
    if (MODEL_ROOT !== selectModel) {
      return fetchModelManageList({ modelCode: selectModel }).then(({ data }) => {
        const { list: dataSource } = data;
        this.resetBtnDisableState(dataSource);
        this.setState({
          dataSource,
          selectedRowKeys: [],
          selectedItems: []
        });
      });
    }
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
        selectedRowKeys={selectedRowKeys}
        tableKey="modelMangeList"
        rowKey={getRowKey}
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

const getRowKey = (row) => row.property.code;

export default injectIntl(ModelManageList, { forwardRef: true });
