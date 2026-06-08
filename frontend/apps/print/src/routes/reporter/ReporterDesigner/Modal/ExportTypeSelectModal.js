import React, { Component } from 'react';
import { Radio } from 'sup-ui';
import Modal from './CommonModal';
import messages from '../messages';
import styles from '../Reporter.less';

const { Group } = Radio;

export default class ExportTypeSelectModal extends Component {
  constructor() {
    super();
    this.state = {
      exportFileType: 'xlsx'
    };
  }

  exportFileTypeChange= (e) => {
    this.setState({
      exportFileType: e.target.value
    });
  }

  handleOk = () => {
    this.props.export(this.state.exportFileType);
    this.handleCancel();
  }

  handleCancel = () => {
    this.props.showOrHideModal({ exportTypeSelectVisiable: false });
  }

  render() {
    const { intl } = this.props;
    const xlsxType = intl.formatMessage(messages.exportType, { type: 'xlsx' });
    const csvType = intl.formatMessage(messages.exportType, { type: 'csv' });
    return (
      <Modal
        title={intl.formatMessage(messages.Export)}
        visible
        onOk={this.handleOk}
        onCancel={this.handleCancel}
        okText={intl.formatMessage(messages.ok)}
        cancelText={intl.formatMessage(messages.cancel)}
      >
        <div style={{ paddingLeft: 20 }}>
          <div className={styles.exportTitle}>{intl.formatMessage(messages.exportTip)}</div>
          <Group onChange={this.exportFileTypeChange} value={this.state.exportFileType}>
            <Radio value="xlsx">{xlsxType}</Radio>
            <Radio value="csv">{csvType}</Radio>
          </Group>
        </div>
      </Modal>
    );
  }
}
