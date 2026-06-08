import React from 'react';
import { injectIntl } from 'react-intl';
import style from '../style.less';
import messages from '../messages.js';

class ModelFormLabel extends React.PureComponent {
  render() {
    const { id, intl, required } = this.props;
    return (
      <div className={style.formLabelWrap}>
        <span className={required ? style.formLabelRequired : ''}>
          {intl.formatMessage(messages[id])}
        </span>
      </div>
    );
  }
}

export default injectIntl(ModelFormLabel);
