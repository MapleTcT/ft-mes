import React from 'react';
import SupTable from 'sup-rc-table';
import commonMessage from 'root/common/messages';
import Content from 'root/components/Content';
import styles from './styles.less';

export default class FormTable extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.state = {
      visibleContent: false,
      selectedRows: [],
      recordData: {},
      columns: [
        {
          title: intl.formatMessage(commonMessage.notice),
          dataIndex: 'protocol_name',
          width: 120
        },
        {
          title: intl.formatMessage(commonMessage.modelName),
          dataIndex: 'name',
          width: 120
        },
        {
          title: intl.formatMessage(commonMessage.modelCode),
          dataIndex: 'code',
          width: 120
        },
        {
          title: intl.formatMessage(commonMessage.desc),
          dataIndex: 'memo',
          width: 150
        },
        {
          title: intl.formatMessage(commonMessage.operate),
          dataIndex: 'operation',
          type: 'operation',
          width: 150,
          fixed: false,
          render: (text, record) => {
            return (
              <span>
                <a onClick={() => { this.content(record); }}>
                  {intl.formatMessage(commonMessage.content)}
                </a>
                <span className={styles.operateUnit}>|</span>
                <a onClick={() => { this.delete(record); }}>
                  {intl.formatMessage(commonMessage.delete)}
                </a>
              </span>
            );
          }
        }
      ]
    };
  }

  componentDidMount() {
    if (this.table && this.table.changeOperationFixedStatus) {
      this.table.changeOperationFixedStatus();
    }
  }

  delete = (record) => {
    this.props.removeTableData([record]);
  }

  content = (recordData) => {
    this.setState({
      visibleContent: true,
      recordData
    });
  }

  closeContent = () => {
    this.setState({
      visibleContent: false
    });
  }

  updateColumns = (columns) => {
    this.setState({
      columns
    });
  }

  render() {
    const { data, intl, selectTable } = this.props;
    const { columns, visibleContent, recordData } = this.state;
    const rowSelection = (selectedRowKeys, selectedRows) => {
      this.setState({
        selectedRows
      }, () => {
        selectTable(this.state.selectedRows);
      });
    };

    return (
      <div className="subtable" style={{ height: data.length > 0 ? 242 : 'auto' }}>
        {
          data.length > 0 ? (
            <SupTable
              ref={(ref) => { this.table = ref; }}
              rowKey={(record) => record.code}
              onSelectItem={rowSelection}
              columns={columns}
              dataSource={data}
              showSearchIcon={false}
              showColumnsFilter={false}
              updateColumns={this.updateColumns}
              size="small"
              onDoubleClick={(re) => {
                this.content(re);
              }}
              pagination={null}
            />
          ) : (
            <div className={styles.noModel}>
              <span>{intl.formatMessage(commonMessage.noChoose)}</span>
            </div>
          )
        }
        {
          visibleContent ? (
            <Content visible={visibleContent} closeContent={this.closeContent} recordData={recordData} />
          ) : null
        }
      </div>
    );
  }
}
