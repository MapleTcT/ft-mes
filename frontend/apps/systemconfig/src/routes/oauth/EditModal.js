import React from 'react';
import {
  Modal,
  Form,
  Input,
  Divider,
  Button,
  Select
} from 'sup-ui';
import { injectIntl } from 'react-intl';
import _ from 'lodash';
import messages from './messages';
import style from './style.less';

const { Option, OptGroup } = Select;

class OauthEditModal extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      submitLoading: false
    };
    this.protocolList = [
      { type: 'bluetron', name: this.intl('suposAuth'), systemFlag: true },
      { type: 'zhuyun', name: this.intl('zhuyun'), systemFlag: false },
      { type: 'jindieyun', name: this.intl('jindieyun'), systemFlag: false },
      { type: this.intl('openIDAuth'), name: this.intl('openIDAuth'), systemFlag: false }
    ];
  }

  intl(key, data) {
    const { intl } = this.props;
    return intl.formatMessage(messages[key], data);
  }

  getRedirectUrl = () => {
    return `${window.location.origin}/inter-api/auth/v1/third/authorize`;
  }

  handleSave = () => {
    const { form, handleSave } = this.props;
    this.setState({
      submitLoading: true
    });
    form
      .validateFieldsAndScroll()
      .then((values) => {
        const { protocolType } = values;
        const { name, systemFlag } = _.get(_.filter(this.protocolList, { type: protocolType }), '[0]', {});
        values.oauthName = name;
        values.systemFlag = systemFlag;
        values.redirectUrl = this.getRedirectUrl();
        handleSave(values, () => {
          this.setState({
            submitLoading: false
          });
        });
      })
      .catch(() => {
        this.setState({
          submitLoading: false
        });
      });
  };

  resetLoading = () => {
    this.setState({
      submitLoading: false
    });
  };

  renderFooter() {
    const { submitLoading } = this.state;

    return (
      <div>
        <Button
          key="submit"
          type="primary"
          loading={submitLoading}
          onClick={this.handleSave}
          style={{ width: 110 }}
        >
          {this.intl('modalBtnOk')}
        </Button>
        <Button key="back" onClick={this.props.handleCancel}>
          {this.intl('modalBtnCancel')}
        </Button>
      </div>
    );
  }

  render() {
    const { handleCancel, form, visible, initData } = this.props;
    const { getFieldDecorator, getFieldValue } = form;
    const protocolType = getFieldValue('protocolType');
    const isEdit = initData && initData.id;
    const redirectUrl = this.getRedirectUrl();

    const title = (
      <div>
        <a className={style.modalTitleBack} onClick={handleCancel}>
          {this.intl('modalTitleBack')}
        </a>
        <span className={style.modalTitleSep}>/</span>
        <span className={style.modalTitle}>
          {this.intl(isEdit ? 'editAuthModalTitle' : 'addAuthModalTitle')}
        </span>
      </div>
    );

    return (
      <Modal
        wrapClassName={style.editModal}
        title={title}
        closable={false}
        destroyOnClose
        visible={visible}
        onCancel={handleCancel}
        onOk={this.handleSave}
        moveable={false}
        footer={this.renderFooter()}
        afterClose={this.resetLoading}
      >
        <div className={style.editModalContent}>
          <Form>
            <Form.Item style={{ display: 'none' }}>
              {getFieldDecorator('id', {})(<Input readOnly />)}
            </Form.Item>

            <Form.Item label={this.intl('formItemOauthName')}>
              {getFieldDecorator('protocolType', {
                rules: [
                  {
                    required: true,
                    message: this.intl('formItemOauthTypeRequried')
                  }
                ]
              })(
                <Select
                  disabled={isEdit}
                  placeholder={this.intl('formItemOauthTypePlaceholder')}
                  style={{ width: 600 }}
                >
                  <OptGroup label={this.intl('builtIn')}>
                    {
                      _.map(_.filter(this.protocolList, { systemFlag: true }), (item) => {
                        return <Option value={item.type}>{item.name}</Option>;
                      })
                    }
                  </OptGroup>
                  <OptGroup label={this.intl('external')}>
                    {
                      _.map(_.filter(this.protocolList, { systemFlag: false }), (item) => {
                        return <Option value={item.type}>{item.name}</Option>;
                      })
                    }
                  </OptGroup>
                </Select>
              )}
            </Form.Item>
            <Divider />
            {protocolType ? (
              <div>
                <h4 className={style.formItemH3}>
                  {this.intl('formItemH3BaseSettings')}
                </h4>

                <Form.Item
                  label={this.intl('formItemSystemName')}
                >
                  {getFieldDecorator('systemName', {
                    rules: [
                      {
                        required: true,
                        message: this.intl('formItemSystemNameRequired')
                      }
                    ]
                  })(<Input maxLength={50} style={{ width: 600 }} />)}
                </Form.Item>

                <Form.Item
                  label={this.intl('formItemRedirectUrl')}
                >
                  <Input disabled value={redirectUrl} maxLength={225} style={{ width: 600 }} />
                </Form.Item>

                {['zhuyun', 'bluetron'].includes(protocolType) ? (
                  <React.Fragment>
                    <Form.Item
                      label={this.intl('formItemOauthUrl')}
                    >
                      {getFieldDecorator('oauthUrl', {
                        rules: [
                          {
                            required: true,
                            message: this.intl('formItemOauthUrlRequired')
                          },
                          {
                            pattern: /^(?:http(s)?:\/\/)?[\w.-]+(?:\.[\w\\.-]+)+[\w\-\\._~:/?#[\]@!\\$&'\\*\\+,;=.]+$/,
                            message: this.intl('formItemRightUrl')
                          }
                        ]
                      })(<Input placeholder={this.intl('getOauthUrl')} maxLength={225} style={{ width: 600 }} />)}
                    </Form.Item>

                    {
                      protocolType === 'zhuyun' ? (
                        <Form.Item
                          label={this.intl('formItemRefreshUrl')}
                        >
                          {getFieldDecorator('refreshUrl', {
                            rules: [
                              {
                                required: true,
                                message: this.intl('formItemRefreshUrlRequired')
                              },
                              {
                                pattern: /^(?:http(s)?:\/\/)?[\w.-]+(?:\.[\w\\.-]+)+[\w\-\\._~:/?#[\]@!\\$&'\\*\\+,;=.]+$/,
                                message: this.intl('formItemRightUrl')
                              }
                            ]
                          })(<Input placeholder={this.intl('getRefreshUrl')} maxLength={225} style={{ width: 600 }} />)}
                        </Form.Item>
                      ) : null
                    }

                    <Form.Item
                      label={this.intl('formItemLogoutUrl')}
                    >
                      {getFieldDecorator('logoutUrl', {
                        rules: [
                          {
                            required: true,
                            message: this.intl('formItemLogoutUrlRequired')
                          },
                          {
                            pattern: /^(?:http(s)?:\/\/)?[\w.-]+(?:\.[\w\\.-]+)+[\w\-\\._~:/?#[\]@!\\$&'\\*\\+,;=.]+$/,
                            message: this.intl('formItemRightUrl')
                          }
                        ]
                      })(<Input placeholder={this.intl('getLogoutUrl')} maxLength={225} style={{ width: 600 }} />)}
                    </Form.Item>
                  </React.Fragment>
                ) : null}

                {protocolType === 'jindieyun' ? (
                  <Form.Item
                    label={this.intl('formItemQrcodeUrl')}
                  >
                    {getFieldDecorator('qrcodeUrl', {
                      rules: [
                        {
                          required: true,
                          message: this.intl('formItemQrcodeUrlRequired')
                        },
                        {
                          pattern: /^(?:http(s)?:\/\/)?[\w.-]+(?:\.[\w\\.-]+)+[\w\-\\._~:/?#[\]@!\\$&'\\*\\+,;=.]+$/,
                          message: this.intl('formItemRightUrl')
                        }
                      ]
                    })(<Input placeholder={this.intl('getQrcodeUrl')} maxLength={225} style={{ width: 600 }} />)}
                  </Form.Item>
                ) : null}

                <Form.Item
                  label={this.intl('formItemTokenUrl')}
                >
                  {getFieldDecorator('tokenUrl', {
                    rules: [
                      {
                        required: true,
                        message: this.intl('formItemTokenUrlRequired')
                      },
                      {
                        pattern: /^(?:http(s)?:\/\/)?[\w.-]+(?:\.[\w\\.-]+)+[\w\-\\._~:/?#[\]@!\\$&'\\*\\+,;=.]+$/,
                        message: this.intl('formItemRightUrl')
                      }
                    ]
                  })(<Input placeholder={this.intl('getTokenUrl')} maxLength={225} style={{ width: 600 }} />)}
                </Form.Item>

                <Form.Item
                  label={this.intl('formItemUserinfoUrl')}
                >
                  {getFieldDecorator('userinfoUrl', {
                    rules: [
                      {
                        required: true,
                        message: this.intl('formItemUserinfoUrlRequired')
                      },
                      {
                        pattern: /^(?:http(s)?:\/\/)?[\w.-]+(?:\.[\w\\.-]+)+[\w\-\\._~:/?#[\]@!\\$&'\\*\\+,;=.]+$/,
                        message: this.intl('formItemRightUrl')
                      }
                    ]
                  })(<Input placeholder={this.intl('getUserinfoUrl')} maxLength={225} style={{ width: 600 }} />)}
                </Form.Item>

                <Form.Item label={this.intl('formItemDescription')}>
                  {getFieldDecorator('description', {
                    rules: [
                      {
                        max: 255,
                        message: this.intl('formItemDescriptionMaxLength')
                      }
                    ]
                  })(<Input.TextArea rows={3} style={{ width: 600 }} />)}
                </Form.Item>

                <Divider />

                <h4 className={style.formItemH3}>
                  {this.intl('formItemH3ClientSetting')}
                </h4>

                {protocolType === 'jindieyun' ? (
                  <Form.Item label={this.intl('formItemQrcodeAppid')}>
                    {getFieldDecorator('qrcodeAppid', {
                      rules: [
                        {
                          required: true,
                          message: this.intl('formItemQrcodeAppidRequired')
                        }
                      ]
                    })(<Input style={{ width: 600 }} />)}
                  </Form.Item>
                ) : null}

                <Form.Item label={this.intl('formItemAppId')}>
                  {getFieldDecorator('appId', {
                    rules: [
                      {
                        required: true,
                        message: this.intl('formItemAppIdRequired')
                      }
                    ]
                  })(<Input style={{ width: 600 }} />)}
                </Form.Item>

                <Form.Item label={this.intl('formItemAppSecret')}>
                  {getFieldDecorator('appSecret', {
                    rules: [
                      {
                        required: true,
                        message: this.intl('formItemAppSecretRequired')
                      }
                    ]
                  })(
                    <Input.Password
                      autoComplete="new-password"
                      style={{ width: 600 }}
                    />
                  )}
                </Form.Item>
              </div>
            ) : null}
          </Form>
        </div>
      </Modal>
    );
  }
}

export default Form.create({
  name: 'oauthEditForm',
  mapPropsToFields({ initData = {} }) {
    const data = {};
    for (const key in initData) {
      if ({}.hasOwnProperty.call(initData, key)) {
        const value = initData[key];
        data[key] = Form.createFormField({
          value
        });
      }
    }
    return data;
  }
})(injectIntl(OauthEditModal));
