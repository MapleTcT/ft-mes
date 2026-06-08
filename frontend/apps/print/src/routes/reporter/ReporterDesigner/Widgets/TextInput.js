import React, { PureComponent } from 'react';
import { Input } from 'sup-ui';

export default class TextInput extends PureComponent {
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

  inputChange = (e) => {
    const { basicOperate, config: { fatherEvent, event, funcName }, edit } = this.props;
    const { value } = e.target;
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
    return (
      <Input onChange={this.inputChange} value={this.state.value} size="small" />
    );
  }
}
