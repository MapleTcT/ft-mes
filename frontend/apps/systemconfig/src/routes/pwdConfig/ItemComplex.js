import React from 'react';
import { Checkbox } from 'sup-ui';
import { injectIntl } from 'react-intl';
import messages from './messages';

class ItemComplex extends React.Component {
  handleCheck = (checked) => {
    const { onChange, value = {} } = this.props;
    const checkedValue = Object.keys(value).reduce((t, c) => {
      t[c] = true;
      if (!~checked.indexOf(c)) {
        t[c] = false;
      }
      return t;
    }, {});
    onChange(checkedValue);
  };

  render() {
    const { intl, value = {} } = this.props;
    const checkedFields = Object.keys(value).filter((d) => !!value[d]);
    const options = [
      {
        label: intl.formatMessage(messages.complexCaseInsensitive),
        value: 'containLetterCase'
      },
      { label: intl.formatMessage(messages.complexNumeric), value: 'containNumbers' },
      {
        label: intl.formatMessage(messages.complexSpecial),
        value: 'containSpecialChar'
      }
    ];

    return (
      <Checkbox.Group
        onChange={this.handleCheck}
        value={checkedFields}
        style={{ width: '100%' }}
        options={options}
      />
    );
  }
}

export default injectIntl(ItemComplex);
