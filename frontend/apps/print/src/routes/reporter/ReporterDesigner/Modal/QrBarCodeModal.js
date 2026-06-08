import React, { Component } from 'react';
import { Row, Col, Input, Select, InputNumber } from 'sup-ui';
import Modal from './CommonModal';
import ColorPicker from '../ColorPicker';
import { quietZoneConfig, qrBarCodeOption } from './BarCodeConfig';
import messages from '../messages';
import styles from './BarCode.less';

const { Option } = Select;

export default class QrBarCodeModal extends Component {
  constructor(props) {
    super(props);
    this.state = {
      barCodeObject: props.barCodeObject
    };
    if (!props.barCodeObject.type) {
      // eslint-disable-next-line prefer-destructuring
      this.state.barCodeObject.type = qrBarCodeOption[0];
    }
  }

  handleCancel = () => {
    this.props.showOrHideModal({ qrBarCodeVisiable: false });
  }

  handleOk = () => {
    const { barCodeObject } = this.state;
    this.props.createBarCode(barCodeObject);
  }

  basicOperate = (item, e) => {
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
      console.log(barCodeObject);
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
      barCodeObject: { ...barCodeObject, type: value }
    });
  }

  render() {
    const { intl } = this.props;
    const { barCodeObject: { type, color = 'rgb(0, 0, 0)', backgroundColor = 'rgb(255, 255, 255)', quietZone = {}, realValue } } = this.state;
    return (
      <Modal
        destroyOnClose
        visible
        width="660px"
        title={intl.formatMessage(messages.qrBarCodeTitle)}
        onOk={this.handleOk}
        onCancel={this.handleCancel}
        okText={intl.formatMessage(messages.ok)}
        cancelText={intl.formatMessage(messages.cancel)}
        key="qrBarCodeModal"
      >
        <div className={styles.barCodeWrapper}>
          <Row>
            <Col span={12}>
              <span className="barCodeSpan">{intl.formatMessage(messages.barCodeValue)}</span>
              <Input onChange={this.basicOperate.bind(null, { key: 'realValue', type: 'input' })} value={realValue} />
            </Col>
            <Col span={12}>
              <span className="barCodeSpan">{intl.formatMessage(messages.barCodeType)}</span>
              <Select value={type} onChange={this.changeType}>
                {
                  qrBarCodeOption.map((type) => <Option value={type} key={type}>{type}</Option>)
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
                  edit={this.basicOperate.bind(null, { key: 'color' })}
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
                  edit={this.basicOperate.bind(null, { key: 'backgroundColor' })}
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
                      <InputNumber onChange={this.basicOperate.bind(null, z)} value={quietZone[z.key]} />
                    </Col>
                  })
                }
              </Row>
            })
          }
        </div>
      </Modal>
    );
  }
}
