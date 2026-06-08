import React from 'react';
import { Modal, Icon } from 'sup-ui';
import style from './style.less';

export default function SpinModal(props) {
  const { tip, visible } = props;
  return (
    <Modal
      wrapClassName={style.spinModal}
      visible={visible}
      width={310}
      closable={false}
      centered
      footer={null}
    >
      <Icon type="loading-3-quarters" />
      <span>{tip}</span>
    </Modal>
  );
}
