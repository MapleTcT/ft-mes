import React, { PureComponent } from 'react';
import { InputNumber } from 'sup-ui';
import * as _ from 'lodash';

export default class NumberInput extends PureComponent {
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

  inputChange = (value) => {
    const { basicOperate, config: { fatherEvent, event, funcName }, edit } = this.props;
    this.setState({ value });
    if (_.isNumber(value) && value >= 0) {
      if (edit) {
        // edit(value);
        basicOperate({ opt: `set${funcName}`, options: { value } });
      } else {
        // updataConfig(fatherEvent, event, value);
        basicOperate({ opt: 'updataConfig', options: { fatherEvent, event, value } });
      }
    }
  }

  render() {
    return (
      <InputNumber onChange={this.inputChange} value={this.state.value} size="small" />
    );
  }
}
