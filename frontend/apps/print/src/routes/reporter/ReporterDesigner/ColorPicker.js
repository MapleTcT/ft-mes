import React, { Component } from 'react';
import { SketchPicker } from 'react-color';
import triangle from 'root/assets/img/report/btn_arrow_app.svg';
import styles from './Reporter.less';

// const language = localStorage.getItem('language') === 'zh-cn'
//   ? require('root/css/zh-cn/language.less')
//   : require('root/css/en-us/language.less');

// Object.assign(styles, language);

export default class ColorPickerTemp extends Component {
  constructor(props) {
    super(props);
    this.state = {
      color: props.getInfo ? props.getInfo(props.name) : props.value ? props.value : props.defaultValue,
      colorPicker: false
    };
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.getInfo || nextProps.value) {
      let color = nextProps.defaultValue;
      if (nextProps.getInfo && nextProps.getInfo(nextProps.name)) {
        color = nextProps.getInfo(nextProps.name);
      }
      if (nextProps.value) {
        color = nextProps.value;
      }
      this.setState({ color });
    }
  }

  componentWillUnMount = () => {
    clearTimeout(this.timer);
  }

  colorPickerFn = (event) => {
    event.preventDefault();
    this.colorPicker(false);
  }

  colorPicker = (bol) => {
    if (bol === true) {
      window.addEventListener('click', this.colorPickerFn);
      window.currentColorPicker = this.props.name;
    } else {
      window.removeEventListener('click', this.colorPickerFn);
      window.currentColorPicker = undefined;
    }
    this.setState({
      colorPicker: bol
    });
  }

  colorChange = (color) => {
    const { onChangeColor } = this.props;
    color = color.rgb ? `rgba(${color.rgb.r},${color.rgb.g},${color.rgb.b},${color.rgb.a})` : color;
    this.setState({
      color
    }, () => {
      // Font.js配置用
      if (onChangeColor) {
        onChangeColor(color);
      } else {
        this.props.edit(color);
      }
    });
  }

  colorPickerIsOpened = () => {
    return this.state.colorPicker || this.props.lastingShown;
  }

  triggerClick = (event) => {
    if (this.props.name === window.currentColorPicker) {
      event.stopPropagation();
      event.preventDefault();
    }
    this.timer = setTimeout(() => {
      this.colorPicker(true);
    }, 0);
  }

  render() {
    const { floatType, marginLeft = 0, colorWidth, ifTriangle, border = 'solid 1px #ced3d8', borderRadius, colorHeight: height = 24, lastingShown, left = -44, top = 'auto' } = this.props;
    const presetColors = [{ color: '#FFFFFF00', title: '透明' }, '#D0021B', '#F5A623', '#F8E71C', '#8B572A', '#7ED321', '#417505', '#BD10E0', '#9013FE', '#4A90E2', '#50E3C2', '#B8E986', '#000000', '#4A4A4A', '#9B9B9B', '#FFFFFF'];
    return (
      <div
        className={styles.interTypeBox}
        style={{
          float: floatType || 'inherit',
          marginLeft,
          marginTop: 0,
          width: colorWidth || '100%'
        }}
      >
        <div
          style={{ position: 'relative', width: '100%' }}
          className={styles.interTypeOpera}
        >
          <div
            style={{
              padding: ifTriangle !== false ? '4px 0 4px 4px' : 0,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-around',
              borderRadius: borderRadius || 2,
              border,
              height
            }}
            onClick={this.triggerClick}
          >
            <div
              style={{
                borderRadius: borderRadius || 3,
                height: '100%',
                width: '100%',
                backgroundColor: this.state.color
              }}
            />
            {
              ifTriangle !== false
              && <img src={triangle} style={{ height: 24 }} alt="" />
            }
          </div>
          {
            (this.state.colorPicker || lastingShown) && (
              <div
                style={{
                  position: 'absolute',
                  zIndex: 100,
                  left,
                  top
                }}
                onClick={this.triggerClick}
              >
                <SketchPicker
                  color={this.state.color}
                  onChange={this.colorChange}
                  presetColors={presetColors}
                />
              </div>
            )
          }
        </div>
      </div>
    );
  }
}
