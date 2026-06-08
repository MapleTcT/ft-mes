import React from 'react';
import { Modal, Radio } from 'sup-ui';
import { injectIntl } from 'react-intl';
import style from './style.less';
import messages from './messages';
import { DOWNLOAD_SELECTED, DOWNLOAD_ALL } from './constants';

function ExportModal(props) {
  const { intl, visible, onCancel, onOk } = props;
  const [exportType, setExportType] = React.useState(DOWNLOAD_SELECTED);
  return (
    <Modal
      maskClosable={false}
      title={intl.formatMessage(messages.exportModalTitle)}
      width={400}
      visible={visible}
      onOk={() => {
        onOk(exportType !== DOWNLOAD_SELECTED);
      }}
      onCancel={onCancel}
      destroyOnClose
      wrapClassName={style.exportModal}
    >
      <Radio.Group
        onChange={({ target: { value } }) => {
          setExportType(value);
        }}
        value={exportType}
      >
        <Radio value={DOWNLOAD_SELECTED} className={style.exportRadio}>
          {intl.formatMessage(messages.exportRadioSelected)}
        </Radio>
        <Radio value={DOWNLOAD_ALL}>
          {intl.formatMessage(messages.exportRadioAll)}
        </Radio>
      </Radio.Group>
    </Modal>
  );
}

export default injectIntl(ExportModal);
