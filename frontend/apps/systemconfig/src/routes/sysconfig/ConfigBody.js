import React from 'react';
import { Layout } from 'sup-ui';
import { injectIntl } from 'react-intl';
import SupIcon from 'sup-rc-icon';
import ConfigForm from './ConfigForm';
import style from './style.less';
import getModule from './module';
import messages from './messages';

class ConfigBody extends React.Component {
  render() {
    const {
      configItems,
      activeConfig,
      configValueMap,
      handleSaveConfig,
      intl,
      updateAuth
    } = this.props;

    let activeForm = (
      <div className={style.noSelectBox}>
        <SupIcon className={style.backIcon} type="iconpoint" />
        <span>{intl.formatMessage(messages.selectLeftObject)}</span>
      </div>
    );
    if (activeConfig) {
      const configItem = configItems.filter(
        (item) => String(item.catalogId) === activeConfig
      )[0];

      if (configItem) {
        const { catalogId, moduleCode } = configItem;
        const ConfigFormProps = {
          ...configItem,
          config: configValueMap[catalogId],
          handleSaveConfig,
          updateAuth
        };

        activeForm = (
          <Layout className={style.contentWrap}>
            {moduleCode ? (
              getModule(moduleCode)
            ) : (
              <ConfigForm {...ConfigFormProps} />
            )}
          </Layout>
        );
      }
    }

    return <div className={style.mainContent}>{activeForm}</div>;
  }
}

export default injectIntl(ConfigBody);
