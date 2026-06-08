import React from 'react';
import { Layout, Modal, Checkbox, message } from 'sup-ui';
import SupTable from 'sup-rc-table';
import { injectIntl } from 'react-intl';
import EditForm from './EditForm';
import style from './style.less';
import messages from './messages';
import { btnDetail, enableSign } from '../../services/eSign';
import { toFormData } from './utils';

const { Header } = Layout;

class EsignContent extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.signEditForm = React.createRef();
    this.state = {
      modalVisible: false,
      dataSource: [],
      total: 50,
      current: 1,
      pageSize: 20,
      columns: [
        {
          title: intl.formatMessage(messages.btnName),
          width: 400,
          dataIndex: 'name'
        },
        {
          title: intl.formatMessage(messages.enableSigning),
          dataIndex: 'code',
          width: 200,
          render: (_, row) => {
            return <Checkbox checked={row.signatureEnabled} />;
          }
        },
        {
          title: intl.formatMessage(messages.operation),
          dataIndex: 'operation',
          width: 300,
          type: 'operation',
          render: this.renderOperateCol
        }
      ]
    };
  }

  componentWillReceiveProps(nextProps) {
    this.initSignBtn(nextProps);
  }

  componentWillMount() {
    this.initSignBtn(this.props);
  }

  // componentDidMount() {
  //   if (this.table && this.table.changeOperationFixedStatus) {
  //     this.table.changeOperationFixedStatus();
  //   }
  // }

  initSignBtn = (props, inputSearch = {}) => {
    const { activeId } = props;
    let search = {};
    search = {
      code: activeId,
      ...inputSearch
    };
    const data = toFormData(search);
    btnDetail(data).then((res) => {
      const { list, pagination: { total, current, pageSize } } = res.data;
      this.setState({
        total,
        current,
        pageSize,
        dataSource: list
      });
    });
  }

  renderOperateCol = (_, row) => {
    const { intl } = this.props;
    return (
      <div style={{ display: 'inline-block', width: '100%' }}>
        <a onClick={() => { this.handleModal(row); }}>
          {intl.formatMessage(messages.signSettings)}
        </a>
      </div>
    );
  }

  handleModal = (row) => {
    this.setState({
      modalVisible: true,
      row
    });
  }

  handleModalCancle = () => {
    this.setState({
      modalVisible: false
    });
  }

  updateColumns = (columns) => {
    this.setState({
      columns
    });
  }

  handleModalSub = () => {
    const signEditForm = this.signEditForm.current;
    signEditForm.validateFields().then(
      (data) => {
        data.buttonCode = this.state.row.code;
        if (data.powerType) {
          if (data.powerType === 'staff') {
            data.staffMultiIDs = data.selectType.map((item) => { return item.id; });
          } else if (data.powerType === 'position') {
            data.positionMultiIDs = data.selectType.map((item) => { return item.id; });
          } else {
            data.roleMultiIDs = data.selectType.map((item) => { return item.id; });
          }
        }
        // const newData = JSON.parse(JSON.stringify(data).replace(/_/g, '.'));
        enableSign(data).then(() => {
          const { intl } = this.props;
          this.setState({
            modalVisible: false
          }, () => {
            this.initSignBtn(this.props);
          });
          message.success(intl.formatMessage(messages.saveSuccess));
        }, (err) => {
          console.error(err);
        });
      }
    );
  }

  handleTableSearch = (params) => {
    const { pagination } = params;
    this.initSignBtn(this.props, pagination);
  }

  render() {
    const { columns, modalVisible, row, dataSource, total, current, pageSize } = this.state;
    const { activeName, intl } = this.props;
    return (
      <>
        <Modal
          className={style.editFormWrap}
          title={intl.formatMessage(messages.signSettings)}
          visible={modalVisible}
          onCancel={this.handleModalCancle}
          onOk={this.handleModalSub}
          okText={intl.formatMessage(messages.ok)}
          destroyOnClose
          maskClosable={false}
        >
          <EditForm
            ref={this.signEditForm}
            row={row}
            isEdit
          />
        </Modal>
        <Header className={`${style.head} ${style.shadow}`}>{activeName}</Header>
        <div className={style.contentBox}>
          <SupTable
            // ref={(ref) => { this.table = ref; }}
            rowKey="code"
            columns={columns}
            // updateColumns={this.updateColumns}
            showSearchIcon={false}
            onSearch={this.handleTableSearch}
            showSelection={false}
            dataSource={dataSource}
            showColumnsFilter={false}
            pagination={{
              total,
              current,
              pageSize
            }}
          />
        </div>
      </>
    );
  }
}

export default injectIntl(EsignContent);
