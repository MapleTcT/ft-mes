import React from 'react';
import { InputNumber } from 'sup-ui';
import { injectIntl } from 'react-intl';
import style from './style.less';

class ItemLength extends React.Component {
  handleChange = (type, v) => {
    const { onChange, value = {} } = this.props;
    onChange({
      ...value,
      [type]: v
    });
  };

  handleMinChange = (v) => {
    this.handleChange('minLength', v);
  };

  handleMaxChange = (v) => {
    this.handleChange('maxLength', v);
  };

  render() {
    const { value = {} } = this.props;
    const { minLength = '', maxLength = '' } = value;
    return (
      <div>
        <InputNumber value={minLength} onChange={this.handleMinChange} />
        <span className={style.pwdLenRange}>-</span>
        <InputNumber value={maxLength} onChange={this.handleMaxChange} />
      </div>
    );
  }
}

export default injectIntl(ItemLength);
