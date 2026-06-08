import React, { PureComponent } from 'react';
import { Checkbox } from 'sup-ui';

export default class SelfCheckBox extends PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      checked: false
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
    const checked = fetch ? fetch() : getValue(fatherEvent, event);
    if (checked === this.state.checked) return;
    this.setState({ checked });
  }

  inputChange = (e) => {
    const { basicOperate, config: { fatherEvent, event, funcName }, edit } = this.props;
    const { checked } = e.target;
    this.setState({ checked });
    if (edit) {
      // edit(checked);
      basicOperate({ opt: `set${funcName}`, options: { value: checked } });
    } else {
      // updataConfig(fatherEvent, event, checked);
      basicOperate({ opt: 'updataConfig', options: { fatherEvent, event, value: checked } });
    }
  }

  render() {
    return (
      <Checkbox onChange={this.inputChange} checked={this.state.checked} size="small" />
    );
  }
}
