import React from 'react';
import { injectIntl } from 'react-intl';
import { Modal, Radio } from 'sup-ui';

@injectIntl
export default class Export extends React.PureComponent {
  state = { value: 1 };

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
          title="导出"
          visible
          onOk={this.handleOk}
          onCancel={this.handleCancel}
          width={400}
        >
          <Radio.Group onChange={this.handleChange} value={value}>
            <Radio value={1} style={radioStyle}>
              导出所选或当前页
            </Radio>
            <Radio value={2} style={radioStyle}>
              导出全部
            </Radio>
          </Radio.Group>
        </Modal>
      </>
    );
  }
}
