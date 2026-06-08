import React from 'react';
import { Modal, Icon } from 'sup-ui';
import style from './style.less';

export default function SpinModal(props) {
  const { tip, visible, timeout = 2e3 } = props;
  const [loading, setLoading] = React.useState(false);

  React.useEffect(() => {
    if (visible) {
      const d = setTimeout(() => {
        setLoading(true);
      }, timeout);

      return () => {
        setLoading(false);
        clearTimeout(d);
      };
    }
  }, [visible]);

  return (
    <Modal
      wrapClassName={style.spinModal}
      visible={loading}
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
