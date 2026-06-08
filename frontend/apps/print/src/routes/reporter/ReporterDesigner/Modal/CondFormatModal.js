import React from 'react';
import { Select, Row, Col, Input, InputNumber } from 'sup-ui';
import _ from 'lodash';
import styles from './CondFormatModal.less';
import { formatTitleTips } from '../ReportMenuBarConfig';
import messages from '../messages';
import Modal from './CommonModal';

const { Option } = Select;

export default class CondFormatModal extends React.PureComponent {
  constructor(props) {
    super(props);
    const {
      condFormatStyle = 'deepRed',
      interValue = '',
      optionValue = '',
      numberValue = 10,
      minValue = 0,
      maxValue = 1
    } = this.getTag(props);
    this.state = { condFormatStyle, interValue, optionValue, numberValue, minValue, maxValue };
    this.settingOptions = [
      {
        key: 'deepRed',
        name: props.intl.formatMessage(messages.deepRed)
      }, {
        key: 'deepYellow',
        name: props.intl.formatMessage(messages.deepYellow)
      }, {
        key: 'deepGreen',
        name: props.intl.formatMessage(messages.deepGreen)
      }, {
        key: 'lightRedFilling',
        name: props.intl.formatMessage(messages.lightRedFilling)
      }, {
        key: 'redText',
        name: props.intl.formatMessage(messages.redText)
      }, {
        key: 'redBorder',
        name: props.intl.formatMessage(messages.redBorder)
      }
    ];
    this.dateOptions = [
      {
        key: 'today',
        name: props.intl.formatMessage(messages.today)
      }, {
        key: 'yesterday',
        name: props.intl.formatMessage(messages.yesterday)
      }, {
        key: 'tomorrow',
        name: props.intl.formatMessage(messages.tomorrow)
      }, {
        key: 'last7Days',
        name: props.intl.formatMessage(messages.last7Days)
      }, {
        key: 'thisMonth',
        name: props.intl.formatMessage(messages.thisMonth)
      }, {
        key: 'lastMonth',
        name: props.intl.formatMessage(messages.lastMonth)
      }, {
        key: 'nextMonth',
        name: props.intl.formatMessage(messages.nextMonth)
      }, {
        key: 'thisWeek',
        name: props.intl.formatMessage(messages.thisWeek)
      }, {
        key: 'lastWeek',
        name: props.intl.formatMessage(messages.lastWeek)
      }, {
        key: 'nextWeek',
        name: props.intl.formatMessage(messages.nextWeek)
      }
    ];
    this.duplicateOptions = [
      {
        key: 'duplicate',
        name: props.intl.formatMessage(messages.duplicate)
      }, {
        key: 'unique',
        name: props.intl.formatMessage(messages.unique)
      }
    ];
  }

  changeReport = () => {
    const { parentType, type } = this.props.condFormat;
    const rangeValue = Object.assign({}, this.state);
    let fillColor = '';
    let textColor = '';
    let borderColor = '';
    switch (this.state.condFormatStyle) {
      case 'deepRed':
        fillColor = '#ffb6c1';
        textColor = '#dc143c';
        break;
      case 'deepYellow':
        fillColor = '#ff0';
        textColor = '#bdb76b';
        break;
      case 'deepGreen':
        fillColor = 'rgba(201, 226, 184, 0.9)';
        textColor = '#006400';
        break;
      case 'lightRedFilling':
        fillColor = '#ffb6c1';
        break;
      case 'redText':
        textColor = '#dc143c';
        break;
      case 'redBorder':
        borderColor = '#dc143c';
        break;
      default:
        break;
    }

    const options = {
      parentType,
      type,
      rangeValue,
      newState: { fillColor, textColor, borderColor }
    };
    this.props.basicOperate({
      opt: 'changeReportByCondFormat',
      options
    });
    this.setTag(options);
    this.handleCancel();
  }

  setTag = (value) => {
    const { parentType, type } = value;
    const sheet = this.props.spread.getActiveSheet();
    _.map(sheet.getSelections(), (range) => {
      const { row, col, rowCount, colCount } = range;
      for (let r = row; r < row + rowCount; r += 1) {
        for (let c = col; c < col + colCount; c += 1) {
          const tag = this.props.getTags({ row, col, key: 'conditionalFormats' }) || {};
          tag[`${parentType}-${type}-${Math.random()}`] = value;
          this.props.setTags({ row: r, col: c, key: 'conditionalFormats', value: tag });
        }
      }
    });
  }

  getTag = (props) => {
    const { parentType, type } = props.condFormat;
    const sheet = props.spread.getActiveSheet();
    const { row, col } = sheet.getSelections()[0];
    const tag = props.getTags({ row, col, key: 'conditionalFormats' });
    const tags = _.filter(tag, (v, k) => {
      const keyArr = k.split('-');
      return keyArr[0] === parentType && keyArr[1] === type;
    });
    return _.get(tags[tags.length - 1], 'rangeValue', {});
  }

  handleCancel = () => {
    this.props.showOrHideModal({ condFormatVisible: false });
  }

  doubleInterType = () => {
    const { intl } = this.props;
    return (
      <Row>
        <Col span={10}>
          <Input
            value={this.state.minValue}
            onChange={(e) => {
              const { value } = e.target;
              this.setState({
                minValue: value
              });
            }}
          />
        </Col>
        <Col span={4} className={styles.settingText}>{intl.formatMessage(messages.to)}</Col>
        <Col span={10}>
          <Input
            value={this.state.maxValue}
            onChange={(e) => {
              const { value } = e.target;
              this.setState({
                maxValue: value
              });
            }}
          />
        </Col>
      </Row>
    );
  }

  singleInterType = () => {
    return (
      <Input
        value={this.state.interValue}
        onChange={(e) => {
          const { value } = e.target;
          this.setState({
            interValue: value
          });
        }}
      />
    );
  }

  optionInterType = (type) => {
    return (
      <Select
        value={this.state.optionValue}
        style={{ width: '100%' }}
        onChange={(value) => {
          this.setState({
            optionValue: value
          });
        }}
      >
        {this[`${type}Options`].map((optionItem) => {
          return (
            <Option value={optionItem.key} key={optionItem.key}>
              {optionItem.name}
            </Option>
          );
        })}
      </Select>
    );
  }

  numberInterType = (type) => {
    return (
      <div>
        <InputNumber
          value={this.state.numberValue}
          style={{ width: '90%' }}
          min={0}
          max={type === 'minTenpct' || type === 'maxTenPct' ? 100 : Infinity}
          onChange={(value) => {
            value = value ? value.toString().replace(/[^\d]/g, '') : 1;
            this.setState({
              numberValue: value - 0
            });
          }}
        />
        <span className={styles.settingText}>
          {type === 'minTenpct' || type === 'maxTenPct' ? '%' : ''}
        </span>
      </div>
    );
  }

  renderInterType = (parentType, type) => {
    const { intl } = this.props;
    let interNode = '';
    if (type === 'between') {
      interNode = this.doubleInterType();
    }
    if (type === 'date') {
      interNode = this.optionInterType(type);
    }
    if (type === 'duplicate') {
      interNode = this.optionInterType(type);
    }
    if (parentType === 'formatProRule') {
      if (type === 'aboveAverage' || type === 'belowAverage') {
        return (
          <Col span={6}>
            <span className={styles.settingText} style={{ display: 'inline-block' }}>
              {intl.formatMessage(messages.selectedregion)}
            </span>
          </Col>
        );
      } else {
        interNode = this.numberInterType(type);
      }
    }
    if (interNode === '') {
      interNode = this.singleInterType();
    }
    return (<Col span={8}>{interNode}</Col>
    );
  }

  render() {
    const { parentType, type } = this.props.condFormat;
    const { intl } = this.props;
    const { title, tips } = formatTitleTips(type, intl);
    return (
      <Modal
        visible
        title={title}
        onOk={this.changeReport}
        onCancel={this.handleCancel}
        okText={intl.formatMessage(messages.ok)}
        cancelText={intl.formatMessage(messages.cancel)}
      >
        <dl>
          <dt className={styles.tips}>{tips}</dt>
          <Row>
            {this.renderInterType(parentType, type)}
            <Col span={12}>
              <Row>
                <Col span={6} className={styles.settingText}>{intl.formatMessage(messages.Set)}</Col>
                <Col span={18}>
                  <Select
                    value={this.state.condFormatStyle}
                    onChange={(value) => {
                      this.setState({
                        condFormatStyle: value
                      });
                    }}
                  >
                    {this.settingOptions.map((optionItem) => {
                      return (
                        <Option value={optionItem.key} key={optionItem.key}>
                          {optionItem.name}
                        </Option>
                      );
                    })}
                  </Select>
                </Col>
              </Row>
            </Col>
          </Row>
        </dl>
      </Modal>
    );
  }
}
