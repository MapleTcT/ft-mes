import React from 'react';
import { Modal, Switch } from 'sup-ui';
import { injectIntl } from 'react-intl';
import SupTable from 'sup-rc-table';
import style from '../style.less';
import { LAN_USED } from '../constant';
import messages from '../messages';

class IntlSetting extends React.Component {
  constructor(props) {
    super(props);

    this.columns = [
      {
        title: this.intl('columnNo'),
        dataIndex: 'no',
        key: 'no',
        width: 70,
        render: (v, row, i) => {
          return i + 1;
        }
      },
      {
        title: this.intl('columnKey'),
        dataIndex: 'languCode',
        key: 'languCode',
        width: 130
      },
      {
        title: this.intl('columnLanguage'),
        dataIndex: 'languType',
        width: 130,
        key: 'languType'
      },
      {
        title: this.intl('columnEnable'),
        dataIndex: LAN_USED,
        key: LAN_USED,
        render: this.renderEnableOperate
      }
    ];
  }

  intl(key, data) {
    const { intl } = this.props;
    return intl.formatMessage(messages[key], data);
  }

  renderEnableOperate = (used, row) => {
    return (
      <div>
        <Switch
          checked={!!used}
          onChange={(checked) => {
            this.props.handleSwitchChange(checked, row);
          }}
        />
      </div>
    );
  };

  render() {
    const { modalProps, lanList } = this.props;
    return (
      <Modal
        maskClosable={false}
        width={490}
        title={this.intl('languageSettingsModalTitle')}
        {...modalProps}
        destroyOnClose
        wrapClassName={style.settingModal}
      >
        <div style={{ height: 310 }}>
          <SupTable
            btnColumns={[]}
            showSearchIcon={false}
            showColumnsFilter={false}
            tableKey="intlSetting"
            columns={this.columns}
            showSelection={false}
            pagination={null}
            rowKey="id"
            dataSource={lanList}
            operationBarTitle=""
          />
        </div>
      </Modal>
    );
  }
}

export default injectIntl(IntlSetting);
