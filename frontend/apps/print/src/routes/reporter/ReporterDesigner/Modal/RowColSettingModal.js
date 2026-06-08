import React, { Component } from 'react';
import { InputNumber } from 'sup-ui';
import Modal from './CommonModal';
import styles from '../Reporter.less';
import messages from '../messages';

export default class RowColSettingModal extends Component {
  constructor(props) {
    super(props);
    this.state = {
      rowColValue: this.props.value
    };
  }

  onChange = (e) => {
    this.setState({ rowColValue: e });
  }

  handleOk = () => {
    this.props.basicOperate({ opt: 'setRowCol', options: { value: this.state.rowColValue } });
    this.handleCancal();
  }

  handleCancal = () => {
    this.props.showOrHideModal({ rowColSettingVisible: false });
  }

  render() {
    const { type, intl } = this.props;
    return (
      <Modal
        visible
        title={type}
        onOk={this.handleOk}
        onCancel={this.handleCancal}
        okText={intl.formatMessage(messages.ok)}
        cancelText={intl.formatMessage(messages.cancel)}
      >
        <div className={styles.flexRow} style={{ alignItems: 'center', padding: '0 80px' }}>
          <span style={{ width: 100 }}>
            {type}
            :
          </span>
          <InputNumber
            style={{ width: '25%' }}
            min={0}
            max={500}
            value={this.state.rowColValue}
            onChange={this.onChange}
            autoFocus
          />
          <span style={{ paddingLeft: 20 }}>{intl.formatMessage(messages.Px)}</span>
        </div>
      </Modal>
    );
  }
}
