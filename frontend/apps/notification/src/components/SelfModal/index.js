import React from 'react';
import { Modal, Icon } from 'sup-ui';
import { injectIntl } from 'react-intl';
import commonMessage from 'root/common/messages';
import styles from './styles.less';

@injectIntl
export default class SelfModal extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
    };
  }

  title = () => {
    const { title, onCancel } = this.props;
    return (
      <div>
        <span className={styles.closeIcon}>
          <Icon className={styles.backIcon} type="back" onClick={onCancel} />
        </span>
        {title}
      </div>
    );
  }

  render() {
    const { visible, onOk, onCancel, children, okButtonDisabled, intl } = this.props;
    return (
      <Modal
        className="selfModal"
        id="selfModal"
        maskClosable={false}
        title={this.title()}
        closable={false}
        moveable={false}
        destroyOnClose
        style={{ top: 0, left: 0, padding: 0, height: '100%' }}
        width="100%"
        visible={visible}
        okButtonProps={{ disabled: okButtonDisabled }}
        okText={intl.formatMessage(commonMessage.confirm)}
        onOk={(e) => { onOk(e); }}
        onCancel={() => { onCancel(); }}
      >
        <div
          style={{
            height: '100%',
            overflowY: 'auto'
          }}
        >
          {children}
        </div>
      </Modal>
    );
  }
}
