import React, { PureComponent } from 'react';
import PropTypes from 'prop-types';
import { Button } from 'sup-ui';
import Modal from './CommonModal';
import commonMessage from '../commonMessages';

class ModalDelete extends PureComponent {
  render() {
    const { intl } = this.props;
    return (
      <Modal
        title={this.props.title || intl.formatMessage(commonMessage.tip)}
        destroyOnClose
        visible={this.props.isShowModal}
        width={336}
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
        <div>{this.props.content ? this.props.content : intl.formatMessage(commonMessage.sureDelete) }</div>
      </Modal>
    );
  }
}

ModalDelete.propTypes = {
  isInversion: PropTypes.bool,
  isShowModal: PropTypes.bool,
  onOk: PropTypes.func,
  onCancel: PropTypes.func
};

ModalDelete.defaultProps = {
  isInversion: false,
  isShowModal: false,
  onOk: () => {},
  onCancel: () => {}
};

export default ModalDelete;
