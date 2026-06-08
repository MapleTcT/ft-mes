import React from 'react';
import { Divider, message, Modal } from 'sup-ui';
import { getBaseModel, deleteSysModel } from 'root/services/messageCenter';
import { injectIntl } from 'react-intl';
import commonMessage from 'root/common/messages';
import SupTable from 'sup-rc-table';
import Add from './add';
// import styles from './styles.less';

@injectIntl
export default class SysModel extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.state = {
      visible: false,
      data: [],
      selectedRowKeys: [],
      record: {},
      columns: [
        {
          title: intl.formatMessage(commonMessage.modelTitle),
          dataIndex: 'name',
          width: 200
        },
        {
          title: intl.formatMessage(commonMessage.content),
          dataIndex: 'template',
          width: 180
        },
        {
          title: intl.formatMessage(commonMessage.operate),
          dataIndex: 'operation',
          type: 'operation',
          width: 100,
          fixed: true,
          render: (text, record) => (
            <span>
              <a onClick={() => { this.detail(record); }}>
                {intl.formatMessage(commonMessage.edit)}
              </a>
              {
                record.system === 0 ? (
                  <span>
                    <Divider type="vertical" />
                    <a onClick={() => { this.delete(record); }}>
                      {intl.formatMessage(commonMessage.delete)}
                    </a>
                  </span>
                ) : null
              }
            </span>
          )
        }
      ],
      btnColumns: [
        {
          key: 'add',
          content: intl.formatMessage(commonMessage.add),
          callback: this.add
        },
        {
          key: 'delete',
          content: intl.formatMessage(commonMessage.delete),
          disabled: () => {
            return this.state.selectedRowKeys.length === 0;
          },
          callback: () => {
            this.delete(null);
          }
        }
      ]
    };
  }

  componentWillMount() {
    this.initTable();
  }

  componentDidMount() {
    if (this.table && this.table.changeOperationFixedStatus) {
      this.table.changeOperationFixedStatus();
    }
  }

  initTable = () => {
    const { id } = this.props.data;
    getBaseModel(id).then((res) => {
      this.setState({
        data: res.data.list
      });
    });
  }

  tableRows = (selectedRowKeys) => {
    this.setState({
      selectedRowKeys
    });
  }

  updateColumns = (columns) => {
    this.setState({
      columns
    });
  }

  detail = (record) => {
    this.setState({
      visible: true,
      record
    });
  }

  add = () => {
    const { data } = this.state;
    const { intl } = this.props;
    if (data.length === 10) {
      Modal.error({
        title: intl.formatMessage(commonMessage.addFail),
        content: intl.formatMessage(commonMessage.addFailRule, { num: 10 })
      });
    } else {
      this.setState({
        visible: true
      });
    }
  }

  delete = (record) => {
    const { selectedRowKeys } = this.state;
    const { intl } = this.props;
    if (!record && selectedRowKeys.length === 0) {
      return;
    }
    Modal.confirm({
      title: record
        ? intl.formatMessage(commonMessage.singleDelete, { name: ` ${record.name} ` })
        : intl.formatMessage(commonMessage.deleteAll),
      content: intl.formatMessage(commonMessage.deleteConfirm),
      okText: intl.formatMessage(commonMessage.confirm),
      onOk: () => {
        let params = {};
        if (record) {
          params = {
            ids: [record.id]
          };
        } else {
          params = {
            ids: selectedRowKeys
          };
        }
        deleteSysModel(params).then(() => {
          this.initTable();
        }).catch((error) => {
          message.error(error.data.message);
        });
      },
      onCancel() {}
    });
  }

  closeAdd = (flag = false) => {
    this.setState({
      visible: false,
      record: {}
    }, () => {
      if (flag) {
        this.initTable();
      }
    });
  }

  render() {
    const {
      columns,
      data,
      btnColumns,
      visible,
      record
    } = this.state;
    const { contentType, id } = this.props.data;
    return (
      <div style={{ height: '100%' }}>
        <SupTable
          ref={(ref) => { this.table = ref; }}
          rowKey={(row) => row.id}
          getCheckboxProps={(re) => {
            return {
              disabled: re.system === 1
            };
          }}
          showColumnsFilter={false}
          onSelectItem={this.tableRows}
          columns={columns}
          btnColumns={btnColumns}
          dataSource={data}
          size="middle"
          showSearchIcon={false}
          updateColumns={this.updateColumns}
          onDoubleClick={(re) => {
            this.detail(re);
          }}
          pagination={false}
        />
        {
          visible ? (
            <Add
              closeAdd={this.closeAdd}
              visible={visible}
              contentType={contentType}
              record={record}
              data={data}
              protocolId={id}
            />
          ) : null
        }
      </div>
    );
  }
}
