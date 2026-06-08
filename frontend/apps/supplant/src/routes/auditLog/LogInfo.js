import React from 'react';
import { injectIntl } from 'react-intl';
import SupTable from 'sup-rc-table';
import { Modal, Row, Col } from 'sup-ui';
import {
  fetchDataLog,
  fetchtDataLogModelWithCode
} from '../../services/auditLog';
import './index.less';
import messages from './messages';

@injectIntl
export default class LogInfo extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      columns1: [
        {
          width: 60,
          title: this.intl('col.rowIndex'),
          dataIndex: 'rowIndex',
          key: 'rowIndex',
          render: (_, data, rowIndex) => rowIndex + 1
        },
        {
          width: 100,
          title: this.intl('col.modelName'),
          dataIndex: 'modelName',
          key: 'modelName'
        },
        {
          width: 100,
          title: this.intl('col.modelObjPk'),
          dataIndex: 'modelObjPk',
          key: 'modelObjPk'
        },
        {
          width: 100,
          title: this.intl('col.operateType'),
          dataIndex: 'operateType.displayName',
          key: 'operateType.displayName'
        }
      ],
      columns2: [
        {
          width: 60,
          title: this.intl('col.rowIndex'),
          dataIndex: 'rowIndex',
          key: 'rowIndex',
          render: (_, data, rowIndex) => rowIndex + 1
        },
        {
          width: 100,
          title: this.intl('col.propertyName'),
          dataIndex: 'propertyName',
          key: 'propertyName'
        },
        {
          width: 100,
          title: this.intl('col.currentValue'),
          dataIndex: 'currentValue',
          key: 'currentValue'
        },
        {
          width: 100,
          title: this.intl('col.historyValue'),
          dataIndex: 'historyValue',
          key: 'historyValue'
        }
      ],
      dataSource1: [],
      dataSource2: []
    };
  }

  intl(key, data = {}) {
    const { intl } = this.props;
    return intl.formatMessage(messages[key], data);
  }

  fetchDataLogList() {
    const { traceId, selectModelObjPk } = this.props;
    fetchDataLog(traceId).then((res) => {
      const { list } = res.data;
      this.setState(
        {
          dataSource1: list
        },
        () => {
          if (list.length) {
            // 没有指定哪一条辅模的时候默认取第一条
            let [{ modelObjPk, modelCode }] = list;
            if (selectModelObjPk) {// fix145244
              const data = list.find(d=>d.modelObjPk === selectModelObjPk);
              if (data) {
                modelCode = data.modelCode;
                modelObjPk = selectModelObjPk;
              }
            }
            this.fetchtDataLogModelList(modelCode, modelObjPk);
          }
        }
      );
    });
  }

  fetchtDataLogModelList(modelCode, modelObjPk) {
    const { traceId } = this.props;
    fetchtDataLogModelWithCode(traceId, modelCode, modelObjPk).then((res) => {
      const { list } = res.data;
      this.setState({
        dataSource2: list
      });
    });
  }

  componentDidMount() {
    const { importLogInfo } = this.props;
    if (importLogInfo) {
      this.setState({
        dataSource1: [importLogInfo]
      });
      const { modelCode, modelObjPk } = importLogInfo;
      this.fetchtDataLogModelList(modelCode, modelObjPk);
    } else {
      this.fetchDataLogList();
    }
  }

  handleCancel = () => {
    const { onCancel } = this.props;
    onCancel();
  };

  handleOk = () => {
    const { onOk } = this.props;
    onOk();
  };

  showChangeDetail = (record) => {
    this.fetchtDataLogModelList(record.modelCode, record.modelObjPk);
  };

  render() {
    const { columns1, columns2, dataSource1, dataSource2 } = this.state;
    return (
      <Modal
        title={this.intl('modal.title.viewLog')}
        visible
        onOk={this.handleOk}
        onCancel={this.handleCancel}
        width={1000}
        className="sup-log-detail"
        cancelButtonProps={{ style: { display: 'none' } }}
        okText={this.intl('modal.btn.close')}
      >
        <Row className="sup-log-info">
          <Col className="sup-info-tab" span={12}>
            <h3 className="m-title">
              {this.intl('modal.bodyTitle.basicInfo')}
            </h3>
            <SupTable
              className="m-table"
              columns={columns1}
              dataSource={dataSource1}
              pagination={false}
              showSearch={false}
              showColumnsFilter={false}
              showSelection={false}
              onRowClick={this.showChangeDetail}
            />
          </Col>
          <Col className="sup-info-tab" span={12}>
            <h3 className="m-title">
              {this.intl('modal.bodyTitle.detailInfo')}
            </h3>
            <SupTable
              rowKey={(d) => d.__index}
              className="m-table"
              columns={columns2}
              dataSource={dataSource2}
              pagination={false}
              showSearch={false}
              showColumnsFilter={false}
              showSelection={false}
            />
          </Col>
        </Row>
      </Modal>
    );
  }
}
