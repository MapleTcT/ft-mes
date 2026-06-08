import React from 'react';
import { injectIntl } from 'react-intl';
import SupTable from 'sup-rc-table';
import { Modal, Row, Col } from 'sup-ui';
import mockData from '../../mock/authLog.js';

import './index.less';

@injectIntl
export default class LogInfo extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      columns1: [
        {
          width: 60,
          title: '序号',
          dataIndex: 'formName',
          key: 'formName'
        },
        {
          width: 100,
          title: '实体名称',
          dataIndex: 'beOperated',
          key: 'beOperated'
        },
        {
          width: 100,
          title: '实体主键',
          dataIndex: 'beOperatedCode',
          key: 'beOperatedCode'
        },
        {
          width: 100,
          title: '操作类型',
          dataIndex: 'operateType',
          key: 'operateType'
        }
      ],
      columns2: [
        {
          width: 60,
          title: '序号',
          dataIndex: 'formName',
          key: 'formName'
        },
        {
          width: 100,
          title: '属性名',
          dataIndex: 'beOperated',
          key: 'beOperated'
        },
        {
          width: 100,
          title: '当前值',
          dataIndex: 'beOperatedCode',
          key: 'beOperatedCode'
        },
        {
          width: 100,
          title: '历史值',
          dataIndex: 'operateType',
          key: 'operateType'
        }
      ],
      dataSource1: mockData.list,
      dataSource2: mockData.list
    };
  }

  handleCancel = () => {
    const { onCancel } = this.props;
    onCancel();
  };

  handleOk = () => {
    const { onOk } = this.props;
    onOk();
  };

  handleLink = () => {};

  render() {
    const { columns1, columns2, dataSource1, dataSource2 } = this.state;
    return (
      <Modal
        title="查看日志"
        visible
        onOk={this.handleOk}
        onCancel={this.handleCancel}
        width={1000}
        className="sup-log-detail"
      >
        <Row className="sup-log-info">
          <Col className="sup-info-tab" span={12}>
            <h3 className="m-title">基本信息</h3>
            <SupTable
              className="m-table"
              columns={columns1}
              dataSource={dataSource1}
              pagination={false}
              showSearch={false}
              showColumnsFilter={false}
              showSelection={false}
            />
          </Col>
          <Col className="sup-info-tab" span={12}>
            <h3 className="m-title">详细信息</h3>
            <SupTable
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
