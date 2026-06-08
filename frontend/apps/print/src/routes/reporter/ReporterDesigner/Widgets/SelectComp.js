import React, { PureComponent } from 'react';
import { Select } from 'sup-ui';

const { Option } = Select;

export default class SelectComp extends PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      value: ''
    };
  }

  componentWillMount() {
    this.setInfo(this.props);
  }

  componentWillReceiveProps(nextProps) {
    this.setInfo(nextProps);
  }

  setInfo = (props) => {
    const { getValue, config: { fatherEvent, event }, fetch } = props;
    const value = fetch ? fetch() : getValue(fatherEvent, event);
    if (value === this.state.value) return;
    this.setState({ value });
  }

  selectChange = (value) => {
    const { basicOperate, config: { fatherEvent, event, funcName }, edit } = this.props;
    this.setState({ value });
    if (edit) {
      // edit(value);
      basicOperate({ opt: `set${funcName}`, options: { value } });
    } else {
      // updataConfig(fatherEvent, event, value);
      basicOperate({ opt: 'updataConfig', options: { fatherEvent, event, value } });
    }
  }

  render() {
    let { value } = this.state;
    const { intl } = this.props;
    const { options, defaultValue } = this.props.config;
    if (!value && value !== 0) {
      value = defaultValue;
    }
    return (
      <Select onChange={this.selectChange} value={value} size="small" style={{ width: '100%' }}>
        {
          options.map((item) => (
            <Option value={item.value} key={item.value}>
              {Object.prototype.toString.call(item.label) !== '[object Object]' ? item.label : intl.formatMessage(item.label)}
            </Option>
          ))
        }
      </Select>
    );
  }
}
