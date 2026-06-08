import React from 'react';
import {
  Modal
} from 'sup-ui';

import styles from './styles.less';

export default function CustomModal(props) {
  return (
    <Modal
      title="附件上传"
      wrapClassName={styles.wrapClassName}
      okText="确认"
      cancelText="取消"
      {...props}
    >
      {props.children}
    </Modal>
  );
}
