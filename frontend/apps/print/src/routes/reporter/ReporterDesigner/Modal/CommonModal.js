import React from 'react';
import * as _ from 'lodash';
import { Button, Modal } from 'sup-ui';
import styles from './CommonModal.less';

class CommonModal extends Modal {
  defaultFooter = [
    <Button
      key="submit"
      type="primary"
      style={this.props.isNewStyle ? { width: 80, height: 34 } : { width: 140 }}
      onClick={this.props.onOk}
    >
      {this.props.okText || 'OK'}
    </Button>,
    <Button
      key="back"
      style={this.props.isNewStyle ? { width: 80, height: 34 } : { width: 140 }}
      onClick={this.props.onCancel}
    >
      {this.props.cancelText || 'Cancel'}
    </Button>
  ]

  render() {
    const { title, modalMarginTop, modalMarginLeft, isNewStyle } = this.props; // moveable 是否可拖动，默认可以
    const modalIsCenter = this.props.modalIsCenter || this.props.modalIsCenter === undefined;
    const wrapClassName = modalIsCenter ? this.props.wrapClassName : 'modal-left-position';
    const className = `${typeof this.props.modalIsCenter === 'boolean' ? 'modal-padding-zero' : ''} ${wrapClassName || ''} vertical-center-modal`;
    // const renderTitle = title ? (moveable === true || moveable === undefined ? <ModalTitle title={title} /> : title) : null; // 兼容无 title 的 modal 样式问题
    const containerElement = document.fullscreenElement || document.webkitFullscreenElement || document.mozFullScreenElement;
    const getContainer = this.props.getContainer ? this.props.getContainer : () => containerElement || document.body;

    return (
      <Modal
        {...this.props}
        className={`${this.props.className ? this.props.className : ''} ${isNewStyle ? styles.modal : ''}`}
        title={title}
        wrapClassName={className}
        centered
        maskStyle={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
        getContainer={getContainer}
        footer={this.props.footer !== undefined ? this.props.footer : this.defaultFooter}
        maskClosable={false}
        style={modalIsCenter ? { ...this.props.style } : { top: modalMarginTop, left: modalMarginLeft, position: 'absolute' }}
      />
    );
  }
}

export default CommonModal;
