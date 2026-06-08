import React from 'react';
import { injectIntl } from 'react-intl';
import { Layout, Modal, Radio, Button, message, Spin, Icon } from 'sup-ui';
import SupTable from 'sup-rc-table';
import messages from './messages';
import style from './style.less';
import DetailsForm from './EsignLogDetails';
import { querySignLog, exportSignLOg, pollingStatus, downfile } from '../../services/eSign';

const { Header } = Layout;

class ESignLog extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = this.props;
    this.state = {
      btnColumns: [],
      exportVisible: false,
      value: 1,
      dataSource: [],
      total: 50,
      current: 1,
      pageSize: 20,
      modalVisible: false,
      spinVisible: false,
      selectedRows: [],
      columns: [
        {
          title: intl.formatMessage(messages.businessMod),
          filterType: 'search',
          dataIndex: 'moduleName',
          width: 100
        },
        {
          title: intl.formatMessage(messages.entityName),
          filterType: 'search',
          dataIndex: 'entityName',
          width: 100
        },
        {
          title: intl.formatMessage(messages.modelName),
          filterType: 'search',
          dataIndex: 'modelName',
          width: 100
        },
        {
          title: intl.formatMessage(messages.businessKey),
          filterType: 'search',
          dataIndex: 'businessKey',
          width: 100
        },
        {
          title: intl.formatMessage(messages.firstSigner),
          filterType: 'search',
          dataIndex: 'firstUserName',
          width: 100
        },
        {
          title: intl.formatMessage(messages.firstSignReasons),
          filterType: 'search',
          dataIndex: 'firstReason',
          width: 180
        },
        {
          title: intl.formatMessage(messages.firstSignTime),
          filterType: 'date',
          dataIndex: 'firstSignTimeStr',
          filterOptions: [
            { label: `${intl.formatMessage(messages.withinADay)}`, value: '1' },
            { label: `${intl.formatMessage(messages.withinThreeDays)}`, value: '2' },
            { label: `${intl.formatMessage(messages.withinAWeek)}`, value: '3' },
            { label: `${intl.formatMessage(messages.withinAMonth)}`, value: '4' },
            { label: `${intl.formatMessage(messages.withinThreeMonths)}`, value: '5' },
            { label: `${intl.formatMessage(messages.withinHalfYear)}`, value: '6' },
            { label: `${intl.formatMessage(messages.withinAYear)}`, value: '7' }
          ],
          width: 200
        },
        {
          title: intl.formatMessage(messages.secondSigner),
          filterType: 'search',
          dataIndex: 'secondUserName',
          width: 100
        },
        {
          title: intl.formatMessage(messages.secondSignReasons),
          filterType: 'search',
          dataIndex: 'secondReason',
          width: 180
        },
        {
          title: intl.formatMessage(messages.secondSignTime),
          filterType: 'date',
          dataIndex: 'secondSignTimeStr',
          filterOptions: [
            { label: `${intl.formatMessage(messages.withinADay)}`, value: '1' },
            { label: `${intl.formatMessage(messages.withinThreeDays)}`, value: '2' },
            { label: `${intl.formatMessage(messages.withinAWeek)}`, value: '3' },
            { label: `${intl.formatMessage(messages.withinAMonth)}`, value: '4' },
            { label: `${intl.formatMessage(messages.withinThreeMonths)}`, value: '5' },
            { label: `${intl.formatMessage(messages.withinHalfYear)}`, value: '6' },
            { label: `${intl.formatMessage(messages.withinAYear)}`, value: '7' }
          ],
          width: 200
        },
        {
          title: intl.formatMessage(messages.signType),
          filterType: 'checkbox',
          dataIndex: 'signatureType',
          width: 100,
          align: 'center',
          filterOptions: [
            { label: `${intl.formatMessage(messages.singleSign)}`, value: 'singleSign' },
            { label: `${intl.formatMessage(messages.doubleSign)}`, value: 'doubleSign' }
          ]
        },
        {
          title: intl.formatMessage(messages.operation),
          dataIndex: 'operation',
          width: 150,
          type: 'operation',
          render: this.renderOperateCol
        }
      ]
    };
  }

  componentWillReceiveProps(nextProps) {
    this.initSignLog(nextProps);
  }

  componentWillMount() {
    this.initBtn(this.props);
    this.initSignLog(this.props);
  }

  // componentDidMount() {
  //   if (this.table && this.table.changeOperationFixedStatus) {
  //     this.table.changeOperationFixedStatus();
  //   }
  // }

  initBtn = (props) => {
    const { intl } = props;
    this.setState({
      btnColumns: [
        {
          key: 'export',
          content: intl.formatMessage(messages.export),
          callback: () => {
            this.setState((prev) => ({ exportVisible: !prev.exportVisible }));
          }
        }
      ]
    });
  }

  initSignLog = (props, inputSearch = {}) => {
    let search = {};
    const { current, pageSize } = this.state;
    search = {
      current,
      pageSize,
      ...inputSearch
    };
    querySignLog(search).then((res) => {
      const { list, pagination } = res.data;
      this.setState({
        total: pagination.total,
        current: pagination.current,
        pageSize: pagination.pageSize,
        dataSource: list
      });
    });
  }

  renderOperateCol = (a, row) => {
    const { intl } = this.props;
    return (
      <div style={{ display: 'inline-block', width: '100%' }}>
        <a onClick={() => { this.handleModal(row); }}>
          {intl.formatMessage(messages.vieDetails)}
        </a>
      </div>
    );
  }

  handleDoubleClick = (record) => {
    this.handleModal(record);
  }

  handleModal = (row) => {
    this.setState({
      modalVisible: true,
      row
    });
  }

  handleExport = () => {
    const { selectedRows, dataSource } = this.state;
    let idArr = [];
    const data = {};
    if (this.state.value === 1) {
      idArr = selectedRows.length > 0
        ? selectedRows
        : dataSource.map((item) => item.uuid);
      data.isAll = false;
      data.ids = idArr;
      exportSignLOg(data).then((res) => {
        this.setState({
          taskStatusId: res.data.data,
          exportVisible: false
        }, () => {
          this.exportStatusInterval();
        });
      });
    } else if (this.state.value === 2) {
      exportSignLOg({
        isAll: true,
        ...this.state.exportFilters
      }).then((res) => {
        this.setState({
          taskStatusId: res.data.data,
          exportVisible: false
        }, () => {
          this.exportStatusInterval();
        });
      });
    }
  }

  exportStatusInterval = () => {
    this.exportStatus();
    this.searchStatus = setInterval(this.exportStatus, 1000);
  }

  exportStatus = () => {
    const { taskStatusId } = this.state;
    pollingStatus(taskStatusId).then((res) => {
      const { data: { data } } = res;
      if (data.status === 1) {
        this.setState({
          spinVisible: true
        });
      } else if (data.status === 2) {
        this.setState({
          spinVisible: false
        });
        this.downloadFileBolb();
        clearInterval(this.searchStatus);
      } else if (data.status === 3) {
        this.setState({
          spinVisible: false
        });
        clearInterval(this.searchStatus);
        message.error(this.props.intl.formatMessage(messages.exportFailed));
      }
    });
  }

  downloadFileBolb = () => {
    const { taskStatusId } = this.state;
    downfile(taskStatusId).then(({ headers, data }) => {
      const [, file] = decodeURIComponent(headers['content-disposition']).split(
        'filename='
      );
      if (window.navigator.msSaveOrOpenBlob) {
        // Internet Explorer
        window.navigator.msSaveOrOpenBlob(
          new Blob([data]),
          file
        );
      } else {
        const blob = new Blob([data]);
        const blobURL = window.URL.createObjectURL(blob);
        const tempLink = document.createElement('a');
        tempLink.style.display = 'none';
        tempLink.href = blobURL;
        tempLink.setAttribute('download', file);
        document.body.appendChild(tempLink);
        tempLink.click();
        setTimeout(() => {
          document.body.removeChild(tempLink);
          window.URL.revokeObjectURL(blobURL);
        }, 200);
      }
      message.success(this.props.intl.formatMessage(messages.exportSuccess));
    });
  }

  handleExportCancle = () => {
    this.setState({
      exportVisible: false
    });
  }

  updateColumns = (columns) => {
    this.setState({
      columns
    });
  }

  handleTableSearch = (params) => {
    const { filters, pagination } = params;
    const { current, pageSize } = pagination;
    this.setState({
      exportFilters: filters
    });
    const search = Object.assign(filters, {
      current,
      pageSize
    });
    this.initSignLog(this.props, search);
  }

  onChange = (e) => {
    this.setState({
      value: e.target.value
    });
  }

  tableSelectItem = (selectedRows) => {
    this.setState({
      selectedRows
    }, () => {
      this.initBtn(this.props);
    });
  }

  handleModalCancel = () => {
    this.setState({
      modalVisible: false
    });
  }

  render() {
    const { intl } = this.props;
    const { columns, btnColumns, exportVisible, dataSource, total, pageSize, current, modalVisible, row, spinVisible } = this.state;
    const radioStyle = {
      display: 'block',
      height: '40px',
      lineHeight: '40px'
    };
    const spinIcon = <Icon type="loading-3-quarters" style={{ fontSize: 30 }} spin />;
    return (
      <>
        <Modal
          className={style.editFormWrap}
          title={intl.formatMessage(messages.esignDetails)}
          visible={modalVisible}
          onCancel={this.handleModalCancel}
          destroyOnClose
          maskClosable={false}
          footer={[
            <Button
              key="close"
              onClick={this.handleModalCancel}
              style={{ width: '100px', padding: '0px 15px', backgroundImage: 'linear-gradient(1deg, #0F71E2 2%, #1991EB 98%)', color: '#fff' }}
            >
              {`${intl.formatMessage(messages.close)}`}
            </Button>
          ]}
        >
          <DetailsForm
            ref={this.signEditForm}
            row={row}
          />
        </Modal>
        <Modal
          title={intl.formatMessage(messages.export)}
          visible={exportVisible}
          onOk={this.handleExport}
          onCancel={this.handleExportCancle}
          className={style.exportModal}
        >
          <Radio.Group onChange={this.onChange} value={this.state.value}>
            <Radio style={radioStyle} value={1}>
              {intl.formatMessage(messages.exportSelect)}
            </Radio>
            <Radio style={radioStyle} value={2}>
              {intl.formatMessage(messages.exportAll)}
            </Radio>
          </Radio.Group>
        </Modal>
        <Modal
          title=""
          visible={spinVisible}
          footer={null}
          className={style.importing}
        >
          <Spin indicator={spinIcon} />
          <h3>{intl.formatMessage(messages.exporting)}</h3>
        </Modal>
        <Layout style={{ height: '100%' }}>
          <Header className={style.head}>{intl.formatMessage(messages.signLog)}</Header>
          <SupTable
            // ref={(ref) => { this.table = ref; }}
            rowKey="uuid"
            columns={columns}
            dataSource={dataSource}
            btnColumns={btnColumns}
            // updateColumns={this.updateColumns}
            showSearchIcon={false}
            onSearch={this.handleTableSearch}
            pagination={{
              total,
              current,
              pageSize
            }}
            onSelectItem={this.tableSelectItem}
            onDoubleClick={this.handleDoubleClick}
          />
        </Layout>
      </>
    );
  }
}

export default injectIntl(ESignLog);
