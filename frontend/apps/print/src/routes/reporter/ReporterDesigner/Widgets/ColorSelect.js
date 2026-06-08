import React, { PureComponent } from 'react';
import ColorPicker from '../ColorPicker';

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
    if (this.colorPicker && this.colorPicker.colorPickerIsOpened()) return;
    this.setState({ value });
  }

  colorChange = (value) => {
    const { basicOperate, config: { fatherEvent, event, funcName }, edit } = this.props;
    if (edit) {
      basicOperate({ opt: `set${funcName}`, options: { value } });
    } else {
      basicOperate({ opt: 'updataConfig', options: { fatherEvent, event, value } });
    }
  }

  render() {
    const { value = '' } = this.state;
    const isColor = (value.indexOf('rgb') !== -1 || value.indexOf('#') !== -1);
    const color = isColor ? value : this.props.config.defaultValue;
    return (
      <ColorPicker
        ref={(node) => { this.colorPicker = node; }}
        value={color}
        edit={this.colorChange}
        name={Math.random()}
        left="5px"
      />
    );
  }
}
