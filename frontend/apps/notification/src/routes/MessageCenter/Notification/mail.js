import React from 'react';
import {
  Form,
  Input,
  Button,
  message,
  Checkbox,
  Row,
  Col,
  Modal,
  Spin,
  Tabs
} from 'sup-ui';
import { Prompt } from 'react-router-dom';
import { getConfig, saveEmail, testEmail } from 'root/services/messageCenter';
import { injectIntl } from 'react-intl';
import SysModel from 'root/components/SysModel';
import commonMessage from 'root/common/messages';
import styles from './notification.less';

const FormItem = Form.Item;
const { TabPane } = Tabs;
let winThis = null;
@injectIntl
@Form.create({
  onValuesChange: () => {
    winThis.setState({
      isPrompt: true
    });
  }
})
export default class Mail extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      // emailProtocol: 'imap',
      enableSSL: true,
      outhToken: '',
      senderEmail: '',
      smtpHost: '',
      sslPort: '465',
      spin: false,
      isPrompt: false
    };
    winThis = this;
  }

  componentWillMount() {
    const { id } = this.props;
    getConfig(id).then((res) => {
      const configValue = _.get(res, 'data.data.configValue', '{}');
      this.setState({
        ...JSON.parse(configValue)
      });
    });
  }

  alarmContent = (value, key) => {
    this.setState({
      [key]: value
    });
  }

  handleSubmit = (e) => {
    e.preventDefault();
    const { intl } = this.props;
    this.setState({
      isPrompt: false
    });
    this.props.form.validateFields((err, values) => {
      if (!err) {
        this.setState({
          lastSave: values
        });
        saveEmail(Object.assign(values, {
          smtpHost: values.smtpHost.trim(),
          sslPort: values.sslPort.trim()
        })).then(() => {
          message.success(intl.formatMessage(commonMessage.updateEmail));
        });
      }
    });
  }

  SSLchange = (e) => {
    const { checked } = e.target;
    const { setFieldsValue } = this.props.form;
    if (checked === true) {
      setFieldsValue({
        sslPort: '465'
      });
    } else if (checked === false) {
      setFieldsValue({
        sslPort: '25'
      });
    }
  }

  connectTest = (e) => {
    e.preventDefault();
    const { intl } = this.props;
    this.props.form.validateFields((err, values) => {
      if (!err) {
        this.setState({
          spin: true
        });
        testEmail({
          host: values.smtpHost.trim(),
          port: values.sslPort.trim(),
          username: values.senderEmail.trim(),
          password: values.outhToken,
          enableSSL: values.enableSSL
        })
          .then((res) => {
            if (res.data.data) {
              message.success(intl.formatMessage(commonMessage.connectSuccess));
            } else {
              message.error(intl.formatMessage(commonMessage.connectFail));
            }
            this.setState({ spin: false });
          })
          .catch((error) => {
            this.setState({ spin: false });
            if (error.message) {
              message.error(error.message);
              return;
            }
            message.error(error.data.message);
          });
      }
    });
  }

  resetForm = () => {
    const { lastSave } = this.state;
    if (!lastSave) {
      this.props.form.resetFields();
    } else {
      this.props.form.setFieldsValue(lastSave);
    }
  }

  render() {
    const { getFieldDecorator } = this.props.form;
    const {
      outhToken,
      senderEmail,
      smtpHost,
      sslPort,
      // emailProtocol,
      enableSSL,
      spin,
      isPrompt
    } = this.state;
    const { intl } = this.props;
    return (
      <div className={`${styles.wrap} tabWrap`}>
        <Tabs
          defaultActiveKey="1"
          style={{
            height: '100%'
          }}
          tabBarStyle={{
            height: 57,
            margin: '21px 0 0 0',
            background: 'transparent',
            textAlign: 'center',
            borderBottom: 0
          }}
          animated={false}
        >
          <TabPane
            className={styles.setTabpane}
            tab={intl.formatMessage(commonMessage.baseSet)}
            key="1"
          >
            <Spin
              spinning={spin}
              tip={`${intl.formatMessage(commonMessage.connectTest)}...`}
            >
              <Form
                onSubmit={this.handleSubmit}
                layout="vertical"
                colon={false}
                style={{ width: 600, margin: '0 auto', transform: 'translate(100px, 0)' }}
              >
                <Row gutter={24}>
                  <Col span={12}>
                    <FormItem
                      label={intl.formatMessage(commonMessage.SMTPserver)}
                    >
                      {
                        getFieldDecorator('smtpHost', {
                          initialValue: smtpHost,
                          rules: [
                            {
                              required: true,
                              whitespace: true,
                              message: intl.formatMessage(commonMessage.enterSMTPserver)
                            }, {
                              max: 50,
                              message: intl.formatMessage(commonMessage.maxWord, { num: 50 })
                            }
                          ]
                        })(
                          <Input />
                        )
                      }
                    </FormItem>
                  </Col>
                  <Col span={4}>
                    <FormItem
                      label={intl.formatMessage(commonMessage.SSLport)}
                    >
                      {
                        getFieldDecorator('sslPort', {
                          initialValue: sslPort,
                          rules: [{
                            required: true,
                            message: intl.formatMessage(commonMessage.enterSSLport)
                          }, {
                            max: 50,
                            message: intl.formatMessage(commonMessage.maxWord, { num: 50 })
                          }]
                        })(
                          <Input />
                        )
                      }
                    </FormItem>
                  </Col>
                  <Col span={4}>
                    <FormItem>
                      {
                        getFieldDecorator('enableSSL', {
                          initialValue: enableSSL,
                          valuePropName: 'checked'
                        })(
                          <Checkbox
                            style={{ marginTop: 33 }}
                            onChange={this.SSLchange}
                          >
                            SSL
                          </Checkbox>
                        )
                      }
                    </FormItem>
                  </Col>
                </Row>
                <FormItem
                  label={intl.formatMessage(commonMessage.senderEmail)}
                >
                  {
                    getFieldDecorator('senderEmail', {
                      initialValue: senderEmail,
                      rules: [
                        {
                          type: 'email',
                          message: intl.formatMessage(commonMessage.rightEmail)
                        },
                        {
                          required: true,
                          whitespace: true,
                          message: intl.formatMessage(commonMessage.enterSenderEmail)
                        }, {
                          max: 50,
                          message: intl.formatMessage(commonMessage.maxWord, { num: 50 })
                        }
                      ]
                    })(
                      <Input style={{ width: 393 }} />
                    )
                  }
                </FormItem>
                <FormItem
                  label={intl.formatMessage(commonMessage.senderPassword)}
                >
                  {
                    getFieldDecorator('outhToken', {
                      initialValue: outhToken,
                      rules: [{
                        required: true,
                        whitespace: true,
                        message: intl.formatMessage(commonMessage.enterSenderPassword)
                      }]
                    })(
                      <Input type="password" style={{ width: 393 }} />
                    )
                  }
                </FormItem>
                <FormItem>
                  <Button onClick={this.connectTest}>
                    {intl.formatMessage(commonMessage.connectTest)}
                  </Button>
                </FormItem>
              </Form>
            </Spin>
            <div className={styles.buttonArea}>
              <Button
                type="primary"
                disabled={!this.props.form.isFieldsTouched()}
                onClick={this.handleSubmit}
                style={{ marginRight: 12, width: 110 }}
              >
                {intl.formatMessage(commonMessage.confirm)}
              </Button>
              <Button
                disabled={!this.props.form.isFieldsTouched()}
                onClick={this.resetForm}
              >
                {intl.formatMessage(commonMessage.cancel)}
              </Button>
            </div>
          </TabPane>
          <TabPane
            className={styles.modelSet}
            tab={intl.formatMessage(commonMessage.defaultModel)}
            key="2"
          >
            <SysModel {...this.props} />
          </TabPane>
        </Tabs>
        <Prompt
          message={(location) => {
            if (location.pathname === '/messageCenter' && this.props.form.isFieldsTouched() && isPrompt) {
              const _self = this;
              Modal.confirm({
                title: intl.formatMessage(commonMessage.saveTip),
                content: intl.formatMessage(commonMessage.confirmTip),
                okText: intl.formatMessage(commonMessage.confirm),
                onOk() {
                  _self.props.form.validateFields((err, values) => {
                    if (!err) {
                      saveEmail(Object.assign(values, {
                        smtpHost: values.smtpHost.trim(),
                        sslPort: values.sslPort.trim()
                      })).then(() => {
                        message.success(intl.formatMessage(commonMessage.updateEmail));
                      });
                    }
                    window.history.go(-1);
                    _self.setState({
                      isPrompt: false
                    });
                  });
                },
                onCancel() {
                  window.location.reload();
                }
              });
              return false;
            } else {
              return true;
            }
          }}
        />
      </div>
    );
  }
}
