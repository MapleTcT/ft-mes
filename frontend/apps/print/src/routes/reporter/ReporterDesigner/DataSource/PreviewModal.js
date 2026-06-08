import React, { Component } from 'react';
import { Row, Col, Input, Button, Table, Icon, notification, Tooltip } from 'sup-ui';
// import { exec } from 'root/services/datatableApi';
import _ from 'lodash';
import commonMessage from '../commonMessages';
import styles from './DataSource.less';
import message from '../messages';

export default class PreviewModal extends Component {
  constructor() {
    super();
    this.state = {
      previewDataVisiable: false,
      dataSource: [],
      columns: [],
      tableLoading: false,
      modalStyleHeight: 244,
      total: 0
    };
  }

  componentDidMount() {
    if (!(this.props.params && this.props.params.length)) {
      this.exec({ modalStyleHeight: 580 });
    }
  }

  getParams = () => {
    const { intl } = this.props;
    const params = [];
    let canExec = true;
    _.map(this.props.params, (item) => {
      let { value } = document.getElementById(`${item}Input`);
      if (value) {
        if (/^\[.*\]$/.test(value)) {
          value = value.slice(1, -1).split(',');
        }
        params.push({
          name: item,
          value
        });
      } else {
        canExec = false;
        notification.warning({ message: intl.formatMessage(message.previewTip, { item }) });
      }
    });
    if (canExec) return params;
    else return false;
  }

  exec = ({ modalStyleHeight = 751 }) => {
    const params = this.getParams();
    if (!params) {
      return;
    }
    this.setState({
      modalStyleHeight,
      previewDataVisiable: true,
      tableLoading: true,
      dataSource: [],
      columns: []
    }, () => {
      this.execSql(params);
    });
  }

  execSql = (params) => {
    const { dataSourceKey: id } = this.props;
    // exec({
    //   id,
    //   sql: this.props.script,
    //   params
    // }).then((res) => {
    //   if (res && +res.code === 200) {
    //     this.setState({ tableLoading: false });
    //     if (res.error) {
    //       this.props.handleErr(res.msg);
    //     } else {
    //       const { dataSource, columns, total } = this.parseData(res);
    //       if (dataSource && dataSource.length) {
    //         this.setState({
    //           dataSource,
    //           columns,
    //           total
    //         });
    //       }
    //     }
    //   } else {
    //     this.props.handleErr();
    //   }
    // });
  }

  parseData = (res) => {
    const { dataSource, columnNames } = res.data;
    const columns = [];
    const num = columnNames.length - 1;
    const width = columnNames.length > 9 ? 100 : 876 / columnNames.length;
    const style = {
      width: `${width - 16}px`,
      overflow: 'hidden',
      textOverflow: 'ellipsis'
    };
    _.map(columnNames, (col, index) => {
      const item = {
        title: (
          <div style={{ ...style, cursor: 'pointer' }}>
            <Tooltip title={col}>{col}</Tooltip>
          </div>
        ),
        dataIndex: col,
        className: 'table-cell-item',
        width,
        render: (text) => <div style={style} title={text}>{text}</div>
      };
      if (index === num) {
        delete item.width;
      }
      columns.push(item);
    });
    _.map(dataSource, (record, index) => {
      record.key = index;
    });
    return { dataSource, columns, total: res.total };
  }

  showPreviewParams = (params) => {
    const { intl } = this.props;
    if (params.length) {
      return (
        <div>
          <div className={styles.previewTxt}>{intl.formatMessage(commonMessage.parameter)}</div>
          <div className={styles.previewParams}>
            <div className={styles.content}>
              {
                _.map(params, (item, index) => {
                  return (
                    <div
                      className={styles.flexRow}
                      style={{ height: 40 }}
                      key={index}
                    >
                      <label>
                        {item}
                        :
                        <Input id={`${item}Input`} />
                      </label>
                    </div>
                  );
                })
              }
            </div>
            <Row type="flex" justify="center" align="bottom">
              <Button type="primary" onClick={this.exec} style={{ width: 80 }}>{intl.formatMessage(message.exec)}</Button>
            </Row>
          </div>
        </div>
      );
    }
  }

  showPreviewData = () => {
    const {
      previewDataVisiable,
      dataSource,
      columns,
      total,
      tableLoading
    } = this.state;
    const { params, intl } = this.props;
    const width = columns.length < 9 ? 876 : columns.length * 100;
    if (previewDataVisiable) {
      const style = params && params.length ? { paddingTop: 10, borderTop: '1px solid #DDE1EB' } : {};
      return (
        <div>
          <div className={styles.previewData}>
            <Row style={style}>
              <Col span={4}>{intl.formatMessage(commonMessage.data)}</Col>
              <Col span={20}>
                <Row gutter={16} type="flex" align="middle" justify="end" style={{ color: 'rgba(0, 0, 0, 0.45)' }}>
                  <Col>
                    {intl.formatMessage(message.previewRows)}
                    ：
                  </Col>
                  <Col>{dataSource.length || 0}</Col>
                  <Col>
                    {intl.formatMessage(message.totalRows)}
                    ：
                  </Col>
                  <Col>{total}</Col>
                </Row>
              </Col>
            </Row>
            <div className={styles['table-box']}>
              <Table
                style={{ width }}
                dataSource={dataSource}
                columns={columns}
                pagination={false}
                loading={tableLoading}
                size="small"
                scroll={{ y: 400 }}
              />
            </div>
          </div>
        </div>
      );
    }
  }

  handleCancel = () => {
    this.props.showOrHideModal({ previewVisiable: false });
  }

  render() {
    const { params, intl } = this.props;
    return (
      <div className={styles.modalMask}>
        <div className={styles.previewModal} style={{ height: this.state.modalStyleHeight }}>
          <Row type="flex" justify="space-between" className={styles.closeHeader}>
            <Col span={23}>{intl.formatMessage(message.preview)}</Col>
            <Col>
              <Icon type="close" theme="outlined" onClick={this.handleCancel} className={styles.closeCursor} />
            </Col>
          </Row>
          <div style={{ padding: 20 }}>
            {this.showPreviewParams(params)}
            {this.showPreviewData()}
          </div>
        </div>
      </div>
    );
  }
}
