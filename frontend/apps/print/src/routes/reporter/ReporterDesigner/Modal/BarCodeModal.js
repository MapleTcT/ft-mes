import React, { Component } from 'react';
import { Row, Col, Input, InputNumber, Select, Checkbox } from 'sup-ui';
import Modal from './CommonModal';
import ColorPicker from '../ColorPicker';
import { barCodeConfig, baseConfigKeys, quietZoneConfig, barCodeOption } from './BarCodeConfig';
import messages from '../messages';
import styles from './BarCode.less';

const { Option } = Select;
const typeToComponent = {
  'input': Input,
  'select': Select,
  'checkbox': Checkbox,
  'inputNumber': InputNumber
}
export default class BarCodeModal extends Component {
  constructor(props) {
    super(props);
    this.state = {
      barCodeObject: props.barCodeObject
    };
    if (!props.barCodeObject.type) {
      // eslint-disable-next-line prefer-destructuring
      this.state.barCodeObject.type = barCodeOption[0];
    }
  }

  handleCancel = () => {
    this.props.showOrHideModal({ barCodeVisiable: false });
  }

  handleOk = () => {
    const { barCodeObject } = this.state;
    this.props.createBarCode(barCodeObject);
  }

  seniorOperate = (item, e) => {
    const { key, type } = item;
    const { barCodeObject } = this.state;
    let value = '';
    if (type === 'input') {
      value = e.target.value;
    } else if (type === 'checkbox') {
      value = e.target.checked;
    } else {
      value = e;
    }
    if (item.parent === 'font') {
      this.setState({
        barCodeObject: {
          ...barCodeObject,
          font: {
            ...barCodeObject.font,
            [key]: value
          }
        }
      })
    } else if (item.parent === 'quietZone') {
      this.setState({
        barCodeObject: {
          ...barCodeObject,
          quietZone: {
            ...barCodeObject.quietZone,
            [key]: value
          }
        }
      })
    } else {
      this.setState({
        barCodeObject: {
          ...barCodeObject,
          [key]: value
        }
      });
    }
  }

  changeType = (value) => {
    const { barCodeObject } = this.state;
    this.setState({
      barCodeObject: { ...barCodeObject, type: value, }
    });
  }

  renderSenior = () => {
    const { intl } = this.props;
    const { barCodeObject } = this.state;
    const { type, showLabel, font = {} } = barCodeObject;
    const configLength = barCodeConfig[type].length;
    return <div className={styles.barCodeSenior}>
      {
        (barCodeConfig[type] || []).map((item, index) => {
          const C = typeToComponent[item.type];
          const CProps = {};
          if (!showLabel && baseConfigKeys.includes(item.key)) {
            CProps.disabled = true
          } else delete CProps.disabled;
          if (item.type === 'checkbox') CProps.checked = barCodeObject[item.key];
          else {
            if (item.parent) {
              CProps.value = font[item.key] || item.default;
            } else CProps.value = barCodeObject[item.key] || item.default;
          }
          return <div>
            <span
              className="barCodeSpan"
              style={index >= configLength / 2 ? { paddingLeft: 10 } : {}}
              title={intl.formatMessage(item.name)}>
              {intl.formatMessage(item.name)}
            </span>
            <C  {...CProps} onChange={this.seniorOperate.bind(null, item)}>
              {
                item.type === 'select' ? item.option.map(opt => <Option key={opt}>{opt}</Option>) : null
              }
            </C>
          </div>
        })
      }
    </div>
  }


  render() {
    const { intl } = this.props;
    const { barCodeObject: { type, color = 'rgb(0, 0, 0)', backgroundColor = 'rgb(255, 255, 255)', realValue, quietZone = {} } } = this.state;

    return (
      <Modal
        destroyOnClose
        visible
        width="660px"
        title={intl.formatMessage(messages.barCodeTitle)}
        onOk={this.handleOk}
        onCancel={this.handleCancel}
        okText={intl.formatMessage(messages.ok)}
        cancelText={intl.formatMessage(messages.cancel)}
        key="barCodeModal"
      >
        <div className={styles.barCodeWrapper}>
          <Row>
            <Col span={12}>
              <span className="barCodeSpan">{intl.formatMessage(messages.barCodeValue)}</span>
              <Input onChange={this.seniorOperate.bind(null, { key: 'realValue', type: 'input' })} value={realValue} />
            </Col>
            <Col span={12}>
              <span className="barCodeSpan">{intl.formatMessage(messages.barCodeType)}</span>
              <Select value={type} onChange={this.changeType}>
                {
                  barCodeOption.map((type) => <Option value={type} key={type}>{type}</Option>)
                }
              </Select>
            </Col>
          </Row>
          <Row>
            <Col span={12}>
              <span className="barCodeSpan" style={{ lineHeight: '32px' }}>{intl.formatMessage(messages.barCodeFontColor)}</span>
              <div className="barCodeColor">
                <ColorPicker
                  ifTriangle={false}
                  name="type"
                  floatType="right"
                  ref={(color) => { this.fontColor = color; }}
                  borderRadius={0}
                  colorWidth="100%"
                  colorHeight={20}
                  edit={this.seniorOperate.bind(null, { key: 'color' })}
                  value={color}
                />
              </div>
            </Col>
            <Col span={12}>
              <span className="barCodeSpan" style={{ lineHeight: '32px' }}>{intl.formatMessage(messages.barCodeBgColor)}</span>
              <div className="barCodeColor">
                <ColorPicker
                  ifTriangle={false}
                  name="type"
                  floatType="right"
                  ref={(color) => { this.bgColor = color; }}
                  borderRadius={0}
                  colorWidth="100%"
                  colorHeight={20}
                  edit={this.seniorOperate.bind(null, { key: 'backgroundColor' })}
                  value={backgroundColor}
                />
              </div>
            </Col>
          </Row>
          {
            quietZoneConfig.map((zone, index) => {
              return <Row key={index}>
                {
                  zone.map(z => {
                    return <Col span={12} key={z.key}>
                      <span className="barCodeSpan">{intl.formatMessage(z.name)}</span>
                      <InputNumber onChange={this.seniorOperate.bind(null, z)} value={quietZone[z.key]} />
                    </Col>
                  })
                }
              </Row>
            })
          }
          {this.renderSenior()}
        </div>
      </Modal >
    );
  }
}
