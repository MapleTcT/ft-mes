import React from 'react';
import {
  Modal,
  Table,
  DatePicker,
  Checkbox,
  Button,
  message
} from 'sup-ui';
import moment from 'moment';
import { transferInPost } from 'root/services/personManage';
import { injectIntl } from 'react-intl';
import { SupReferenceView } from 'sup-rc-reference';
import styles from './styles.less';
import commonMessage from './messages';

@injectIntl
export default class PostTransfer extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.state = {
      personVisible: false,
      retData: [],
      columns: [
        {
          title: intl.formatMessage(commonMessage.name),
          dataIndex: 'name',
          width: 80,
          render: (text) => {
            return <span title={text} className={styles.tdWidth} style={{ width: 66 }}>{text}</span>;
          }
        }, {
          title: intl.formatMessage(commonMessage.code),
          dataIndex: 'code',
          width: 100,
          render: (text) => {
            return <span title={text} className={styles.tdWidth} style={{ width: 84 }}>{text}</span>;
          }
        }, {
          title: intl.formatMessage(commonMessage.transferIn),
          dataIndex: 'post',
          ellipsis: true,
          width: 228,
          render: (text) => {
            return this.transformPath(text);
          }
        }, {
          title: intl.formatMessage(commonMessage.transferInTime),
          dataIndex: 'time',
          width: 180,
          render: (text, record) => {
            return <DatePicker allowClear={false} value={text} onChange={(date) => { record.time = date; this.forceUpdate(); }} />;
          }
        }, {
          title: intl.formatMessage(commonMessage.mainPost),
          dataIndex: 'mainPost',
          width: 60,
          render: (text, record) => {
            return <Checkbox checked={text} onChange={(e) => { record.mainPost = e.target.checked; this.forceUpdate(); }} />;
          }
        }, {
          title: intl.formatMessage(commonMessage.operate),
          key: 'action',
          width: 60,
          render: (record) => (
            <span className={styles.operateBox}>
              <span className={styles.moreOperate}>···</span>
              <a className={styles.operate} onClick={() => { this.delete(record); }}>
                {intl.formatMessage(commonMessage.cancel)}
              </a>
            </span>
          )
        }
      ],
      data: []
    };
  }

  transformPath(text) {
    if (text) {
      return text.map((fullPath) => {
        let use = '';
        const fullPathArr = fullPath.split('/');
        if (fullPathArr.length > 2) {
          use = `${fullPathArr[0]}...${fullPathArr[fullPathArr.length - 1]}`;
        } else {
          use = fullPath;
        }
        return <span title={fullPath} className={styles.tdPathWidth} key={fullPath}>{use}</span>;
      });
    }
  }

  delete = (record) => {
    const { data, retData } = this.state;
    const index1 = data.findIndex((item) => item.id === record.id);
    const index2 = retData.findIndex((item) => item.id === record.id);
    const copy1 = [].concat(data);
    const copy2 = [].concat(retData);
    copy1.splice(index1, 1);
    copy2.splice(index2, 1);
    this.setState({
      data: copy1,
      retData: copy2
    });
  }

  onOk = () => {
    const { data } = this.state;
    const persons = data.map((item) => {
      return {
        id: item.id,
        mainPosition: item.mainPost,
        workTime: moment(item.time).format('YYYY-MM-DD')
      };
    });
    const transferIn = {
      persons,
      positionId: this.props.treeData.id
    };
    transferInPost(transferIn)
      .then(() => {
        message.success(this.props.intl.formatMessage(commonMessage.postInSuccess));
        this.props.closePostModal(true);
      })
      .catch((error) => {
        message.error(error.data.message);
      });
  }

  onCancel = () => {
    this.props.closePostModal();
  }

  tableRows = (selectedRowKeys, selectedRows) => {
    console.log(selectedRowKeys, selectedRows);
  }

  choosePerson = () => {
    this.setState({
      personVisible: true
    });
  }

  renderNone = () => {
    const { intl } = this.props;
    return (
      <div className={styles.nonePost}>
        <span>
          {intl.formatMessage(commonMessage.hasChoosePost, { name: this.props.treeData.name })}
        </span>
        <div className={styles.opPost}>
          <p>{intl.formatMessage(commonMessage.noPerson)}</p>
          <Button onClick={this.choosePerson}>{intl.formatMessage(commonMessage.choosePerson)}</Button>
        </div>
      </div>
    );
  }

  personPay = (retData) => {
    const { data } = this.state;
    const retIds = retData.map((item) => item.id);
    // 保留上次选择的角色
    const holdData = data.filter((item) => retIds.includes(item.id));
    const ids = holdData.map((item) => item.id);
    const tableData = retData
      .filter((item) => !ids.includes(item.id))
      .map((item) => {
        const { id, name, code } = item;
        return {
          id,
          name,
          code,
          post: [this.props.treeData.fullPath],
          time: moment(),
          mainPost: false
        };
      });
    this.setState({
      retData,
      data: holdData.concat(tableData),
      personVisible: false
    });
  }

  render() {
    const { visible, intl } = this.props;
    const { columns, data, personVisible, retData } = this.state;
    const selectedRowKeys = retData.map((item) => {
      return item.code;
    });
    const rowSelection = {
      onChange: this.tableRows
    };
    let scroll = {};
    if (56 * data.length > 300) {
      scroll = {
        y: 300
      };
    }
    return (
      <Modal
        visible={visible}
        maskClosable={false}
        destroyOnClose
        title={intl.formatMessage(commonMessage.transferIn)}
        onOk={this.onOk}
        onCancel={this.onCancel}
        okButtonProps={{ disabled: data.length === 0 }}
        width={800}
        bodyStyle={{
          padding: 12
        }}
      >
        {
          data.length > 0 ? (
            <div className={`${styles.postContentBox} normalTable`}>
              <div className={styles.postOpBox}>
                {/* <Button className={styles.opButton}>{intl.formatMessage(commonMessage.transferIn)}</Button> */}
                <Button
                  className={styles.opButton}
                  onClick={this.choosePerson}
                >
                  {intl.formatMessage(commonMessage.choosePerson)}
                </Button>
              </div>
              <Table
                rowKey={(item) => item.id}
                rowSelection={rowSelection}
                columns={columns}
                dataSource={data}
                scroll={scroll}
                size="middle"
                pagination={false}
              />
            </div>
          ) : this.renderNone()
        }
        <SupReferenceView
          title={this.props.intl.formatMessage(commonMessage.staff)}
          type="staff"
          height="600px"
          destroyOnClose
          onCancel={() => {
            this.setState({ personVisible: false });
          }}
          visible={personVisible}
          multiple
          onOk={this.personPay}
          selectedRowKeys={selectedRowKeys}
          selectedRows={retData}
        />
      </Modal>
    );
  }
}
