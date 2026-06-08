import React, { Component } from 'react';
import { Row, Col, Select, Radio } from 'sup-ui';
import * as _ from 'lodash';
import styles from '../Reporter.less';
import messages from '../messages';
import Modal from './CommonModal';

const { Group } = Radio;
const { Option } = Select;

export default class PrintSettingModal extends Component {
  constructor(props) {
    super(props);
    this.state = {
      scope: 'all'
    };
    this.sheetsName = [];
    _.map(props.sheets, (index, name) => {
      this.sheetsName.push(<Option key={name}>{name}</Option>);
    });
  }

  onStateChange = (type, value) => {
    const state = {};
    state[type] = value;
    this.setState(state);
  }

  onRadioChange = (type, e) => {
    this.onStateChange(type, e.target.value);
  }

  onSelectChange = (names) => {
    this.selectedSheets = names;
  }

  handleOk = () => {
    const spread = _.merge({}, this.props.spread);
    if (this.state.scope !== 'all') {
      if (!(this.selectedSheets && this.selectedSheets.length)) return;
      const willRemoveSheetsIndex = [];
      _.map(this.props.sheets, (index, name) => {
        if (!this.selectedSheets.includes(name)) {
          willRemoveSheetsIndex.unshift(index);
        }
      });
      _.map(willRemoveSheetsIndex, (index) => {
        spread.removeSheet(index);
      });
    }
    spread.print();
    this.handleCancel();
  }

  handleCancel = () => {
    this.props.showOrHideModal({ printModalVisiable: false });
  }

  render() {
    const { intl } = this.props;
    return (
      <Modal
        visible
        width="500px"
        bodyStyle={{ height: '300px' }}
        title={intl.formatMessage(messages.PrintContentSet)}
        onCancel={this.handleCancel}
        onOk={this.handleOk}
        okText={intl.formatMessage(messages.ok)}
        cancelText={intl.formatMessage(messages.cancel)}
      >
        <div className={styles.print}>
          <Row>
            {intl.formatMessage(messages.PrintContent)}
            ：
          </Row>
          <Group
            onChange={this.onRadioChange.bind(this, 'scope')}
            value={this.state.scope}
          >
            <Row type="flex" justify="start" align="middle">
              <Col offset={1} span={7}>
                <Radio value="all">{intl.formatMessage(messages.Book)}</Radio>
              </Col>
            </Row>
            <Row type="flex" justify="start" align="middle">
              <Col offset={1} span={7}>
                <Radio value="part">{intl.formatMessage(messages.Sheet)}</Radio>
              </Col>
              <Col span={16}>
                <Select
                  mode="multiple"
                  size="default"
                  placeholder={intl.formatMessage(messages.chooseSheet)}
                  onChange={this.onSelectChange}
                  disabled={this.state.scope === 'all'}
                >
                  {this.sheetsName}
                </Select>
              </Col>
            </Row>
          </Group>

        </div>
      </Modal>
    );
  }
}
