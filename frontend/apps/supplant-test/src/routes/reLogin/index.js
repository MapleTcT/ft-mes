import React from 'react';
import axios from 'axios';
import { injectIntl } from 'react-intl';
import { Form, Input, Button } from 'sup-ui';
import { getQueryString } from '../../utils';
import message from './message';
import './index.less';

@injectIntl
export default class SupLogin extends React.PureComponent {
  constructor(props) {
    super(props);
    this.userName = getQueryString('username');
  }

  state = {};

  onCancel = () => {
    window.top.postMessage('reLoginCancel', '*');
  };

  onOk = () => {
    const { intl } = this.props;
    const { password } = this.state;
    if (!password) {
      this.setState({
        msg: `${intl.formatMessage(message.password)}${intl.formatMessage(
          message.notnull
        )}`
      });
      return;
    }
    axios
      .post(window.top.CUI.fusionAPI.loginUrl || '/inter-api/auth/login', {
        //   .post('http://192.168.91.64:8080/inter-api/auth/login', {
        userName: this.userName,
        password
      })
      .then(
        (res) => {
          this.setState({ msg: '' });
          window.top.CUI.setLoginInfo(res.data);
          window.top.postMessage('reLoginOk', '*');
        },
        (err) => {
          if (err.response) {
            this.setState({
              msg: (err.response.data || {}).message
            });
          }
        }
      );
  };

  onChange = (e) => {
    this.setState({
      password: e.target.value
    });
  };

  render() {
    const { intl } = this.props;
    const { msg } = this.state;

    return (
      <>
        <div
          className="supplant-login"
          style={{ width: '410px', height: '264px' }}
        >
          <div className="sup-modal-content">
            <Button
              onClick={this.onCancel}
              className="sup-modal-close"
              style={{ background: 'none' }}
            >
              <span className="sup-modal-close-x">
                <i
                  aria-label="图标: close"
                  className="anticon anticon-close sup-modal-close-icon"
                >
                  <svg
                    viewBox="64 64 896 896"
                    className=""
                    data-icon="close"
                    width="1em"
                    height="1em"
                    fill="currentColor"
                    aria-hidden="true"
                    focusable="false"
                  >
                    <path d="M563.8 512l262.5-312.9c4.4-5.2.7-13.1-6.1-13.1h-79.8c-4.7 0-9.2 2.1-12.3 5.7L511.6 449.8 295.1 191.7c-3-3.6-7.5-5.7-12.3-5.7H203c-6.8 0-10.5 7.9-6.1 13.1L459.4 512 196.9 824.9A7.95 7.95 0 0 0 203 838h79.8c4.7 0 9.2-2.1 12.3-5.7l216.5-258.1 216.5 258.1c3 3.6 7.5 5.7 12.3 5.7h79.8c6.8 0 10.5-7.9 6.1-13.1L563.8 512z" />
                  </svg>
                </i>
              </span>
            </Button>
            <div className="sup-modal-header">
              <div className="sup-modal-title" id="rcDialogTitle0">
                {intl.formatMessage(message.title)}
              </div>
            </div>
            <div className="sup-modal-body" style={{ padding: 0 }}>
              <Form layout="vertical" style={{ height: '124px' }}>
                <Form.Item label={intl.formatMessage(message.username)}>
                  <Input
                    value={this.userName}
                    readOnly
                    style={{ background: '#F4F6F8' }}
                  />
                </Form.Item>
                <Form.Item label={intl.formatMessage(message.password)}>
                  <Input.Password
                    id="password"
                    visibilityToggle={false}
                    onChange={this.onChange}
                    onPressEnter={this.onOk}
                  />
                </Form.Item>
                {msg ? (
                  <div
                    className="ant-form-explain"
                    style={{ marginTop: '-12px', color: '#e64c66' }}
                  >
                    {msg}
                  </div>
                ) : null}
              </Form>
            </div>
            <div className="sup-modal-footer">
              <Button
                type="primary"
                style={{
                  background:
                    'linear-gradient(0deg, rgb(15, 113, 226), rgb(25, 145, 235))',
                  height: '30px'
                }}
                onClick={this.onOk}
              >
                {intl.formatMessage(message.login)}
              </Button>
            </div>
          </div>
        </div>
      </>
    );
  }
}
