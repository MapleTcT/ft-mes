import React from 'react';
import { injectIntl } from 'react-intl';
import { Modal, Radio } from 'sup-ui';
import { EXPORT_ALL, EXPORT_SELECTED } from './constant';
import messages from './messages';

@injectIntl
export default class Export extends React.PureComponent {
  state = { value: EXPORT_SELECTED };

  handleChange = (e) => {
    const { value } = e.target;
    this.setState({ value });
  };

  handleCancel = () => {
    const { onCancel } = this.props;
    onCancel();
  };

  handleOk = () => {
    const { onOk } = this.props;
    const { value } = this.state;
    onOk(value);
  };

  intl(key, data = {}) {
    const { intl } = this.props;
    return intl.formatMessage(messages[key], data);
  }

  render() {
    const { value } = this.state;
    const radioStyle = {
      display: 'block',
      height: '40px',
      lineHeight: '40px'
    };
    return (
      <>
        <Modal
          title={this.intl('modal.title.export')}
          visible
          onOk={this.handleOk}
          onCancel={this.handleCancel}
          width={400}
        >
          <Radio.Group onChange={this.handleChange} value={value}>
            <Radio value={EXPORT_SELECTED} style={radioStyle}>
              {this.intl('modal.selectOption.exportSelected')}
            </Radio>
            <Radio value={EXPORT_ALL} style={radioStyle}>
              {this.intl('modal.selectOption.exportAll')}
            </Radio>
          </Radio.Group>
        </Modal>
      </>
    );
  }
}
