import React, { Component } from 'react';
import * as _ from 'lodash';
import classnames from 'classnames';
import { Row, Col, Select, InputNumber } from 'sup-ui';
import Modal from '../Modal/CommonModal';
import styles from './DataSource.less';
import messages from '../messages';

const { Option } = Select;

export default class InsertDateFormulaModal extends Component {
  constructor(props) {
    super(props);
    this.templateTypes = {
      h: {
        h: '1',
        'h:00:00': '1:00:00',
        'y年M月d日h:00:00': '2018年12月12日12:00:00'
      },
      d: {
        d: '12',
        第d天: '第12天',
        M月d日: '12月12日',
        y年M月d日: '2018年12月12日'
      },
      M: {
        M: '12',
        M月: '12月',
        y年M月: '2018年12月'
      },
      y: {
        y: '2018',
        y年: '2018年'
      }
    };
    this.parseFormula(props.formula);
  }

  onStateChange = (type, value) => {
    if (type === 'dateType') {
      this.setState({
        dateType: value,
        templateType: value
      });
    } else {
      this.setState({ [type]: value });
    }
  }

  parseFormula = (formula) => {
    const [, params] = (formula && formula.match(/DD\((.*)\)$/)) || [];
    const [dateType, templateType, direction, dataLength] = (params && params.replace(/"/g, '').split(',')) || [];
    this.state = {
      interval: 1,
      dateType: 'd',
      templateType: templateType ? _.get(templateType.match(/(y|M|d|h).*/), [0], 'd') : 'd',
      direction: direction ? _.get(direction.match(/(H|V)/), [0], 'V') : 'V',
      dataLength: dataLength && !_.isNaN(Number(dataLength)) ? Number(dataLength) : 0
    };
    if (dateType) {
      const [, interval, type] = dateType.match(/(\d*)(y|M|d|h)/);
      if (interval && !_.isNaN(Number(dataLength))) this.state.interval = interval;
      this.state.dateType = type;
    }
  }

  generateFormula = () => {
    const { interval, dateType, templateType, direction, dataLength = 0 } = this.state;
    return `=DD("${interval}${dateType}", "${templateType}", "${direction}", "${dataLength}")`;
  }

  handleCancel = () => {
    this.props.showOrHideModal({ formulaDateModalVisiable: false });
  }

  handleOk = () => {
    const formula = this.generateFormula();
    this.props.basicOperate({ opt: 'generateFormula', options: { formula } });
    this.handleCancel();
  }

  renderTemplate() {
    const { dateType } = this.state;
    return _.map(this.templateTypes[dateType], (v, k) => <Option value={k} key={k}>{v}</Option>);
  }

  render() {
    const { intl } = this.props;
    const { interval, dateType, templateType, direction, dataLength } = this.state;
    return (
      <Modal
        visible
        width="500px"
        bodyStyle={{ height: 200 }}
        title={intl.formatMessage(messages.BasicFormula)}
        onCancel={this.handleCancel}
        onOk={this.handleOk}
      >
        <div className={classnames(styles.insertFormula, styles.insertDateFormula)}>
          <Row type="flex" justify="space-around" align="middle">
            <Col span={5}>
              {intl.formatMessage(messages.Time)}
              :
            </Col>
            <Col span={3}>
              <InputNumber
                style={{ width: '100%' }}
                min={1}
                precision={0}
                value={Number(interval)}
                onChange={this.onStateChange.bind(this, 'interval')}
              />
            </Col>
            <Col span={15} offset={1}>
              <Select
                style={{ width: '95%' }}
                value={dateType}
                onChange={this.onStateChange.bind(this, 'dateType')}
                size="default"
              >
                <Option value="h">{intl.formatMessage(messages.hourTip)}</Option>
                <Option value="d">{intl.formatMessage(messages.dayTip)}</Option>
                <Option value="M">{intl.formatMessage(messages.monthTip)}</Option>
                <Option value="y">{intl.formatMessage(messages.yearTip)}</Option>
              </Select>
            </Col>
          </Row>
          <Row type="flex" justify="space-around" align="middle">
            <Col span={5}>
              {intl.formatMessage(messages.dateFormat)}
              :
            </Col>
            <Col span={19}>
              <Select
                style={{ width: '96%' }}
                value={templateType}
                onChange={this.onStateChange.bind(this, 'templateType')}
                size="default"
              >
                {this.renderTemplate()}
              </Select>
            </Col>
          </Row>
          <Row type="flex" justify="space-around" align="middle">
            <Col span={5}>
              {intl.formatMessage(messages.fillOrien)}
              ：
            </Col>
            <Col span={19}>
              <Select
                style={{ width: '96%' }}
                value={direction}
                onChange={this.onStateChange.bind(this, 'direction')}
                size="default"
              >
                <Option value="H">{intl.formatMessage(messages.HorizontalFill)}</Option>
                <Option value="V">{intl.formatMessage(messages.VerticalFill)}</Option>
              </Select>
            </Col>
          </Row>
          <Row type="flex" justify="space-around" align="middle">
            <Col span={5}>
              {intl.formatMessage(messages.fillLength)}
              ：
            </Col>
            <Col span={19}>
              <InputNumber
                min={0}
                precision={0}
                value={dataLength}
                size="default"
                onChange={this.onStateChange.bind(this, 'dataLength')}
                onBlur={this.updateDataSourceFormula}
              />
            </Col>
          </Row>
        </div>
      </Modal>
    );
  }
}
