import React, { PureComponent } from 'react';
import PropTypes from 'prop-types';
import { Button } from 'sup-ui';
import Modal from './CommonModal';
import commonMessage from '../commonMessages';

class ConfirmModal extends PureComponent {
  render() {
    const { intl, content } = this.props;
    return (
      <Modal
        title={this.props.title || intl.formatMessage(commonMessage.tip)}
        visible
        width={450}
        bodyStyle={{ minHeight: 60 }}
        maskClosable={false}
        wrapClassName="vertical-center-modal"
        maskStyle={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
        onCancel={this.props.onCancel}
        onOk={this.props.onOk}
        footer={[
          <Button key="submit" size="large" type={this.props.isInversion ? '' : 'primary'} onClick={this.props.onOk}>{intl.formatMessage(commonMessage.sure)}</Button>,
          <Button key="back" size="large" type={!this.props.isInversion ? '' : 'primary'} onClick={this.props.onCancel}>{intl.formatMessage(commonMessage.cancel)}</Button>
        ]}
      >
        <div dangerouslySetInnerHTML={{ __html: content }} />
      </Modal>
    );
  }
}

ConfirmModal.propTypes = {
  isInversion: PropTypes.bool,
  onOk: PropTypes.func,
  onCancel: PropTypes.func
};

ConfirmModal.defaultProps = {
  isInversion: false,
  onOk: () => { },
  onCancel: () => { }
};

export default ConfirmModal;
