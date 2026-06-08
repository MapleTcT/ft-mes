import React, { Component } from 'react';
import { Row, Col, Icon, Button } from 'sup-ui';
import styles from '../Reporter.less';
import messages from '../messages';

export default class CloseDesignerModal extends Component {
  onCancel = () => {
    this.props.showOrHideModal({ closeDesignerVisible: false });
  }

  save = () => {
    this.props.onSaveClose();
    this.onCancel();
  }

  closeWithoutSave = () => {
    this.props.onClose();
    this.onCancel();
  }

  render() {
    const { intl } = this.props;
    return (
      <div className={styles.modalMask}>
        <div className={styles.modalContent}>
          <Row type="flex" justify="space-between" className={styles.closeHeader}>
            <Col span={8}>{intl.formatMessage(messages.Tip)}</Col>
            <Col span={1}>
              <Icon type="close" theme="outlined" onClick={this.onCancel} className={styles.closeCursor} />
            </Col>
          </Row>
          <Row>
            <Col offset={2}>{intl.formatMessage(messages.isSaveReport)}</Col>
          </Row>
          <Row>
            <Col offset={5} span={6}>
              <Button type="primary" onClick={this.save}>{intl.formatMessage(messages.Save)}</Button>
            </Col>
            <Col span={6}>
              <Button onClick={this.closeWithoutSave}>{intl.formatMessage(messages.noSave)}</Button>
            </Col>
            <Col span={6}>
              <Button onClick={this.onCancel}>{intl.formatMessage(messages.cancel)}</Button>
            </Col>
          </Row>
        </div>
      </div>
    );
  }
}
