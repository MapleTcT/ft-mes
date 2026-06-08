import React from 'react';
import {
  Modal,
  Form,
  Input,
  Checkbox,
  Radio,
  InputNumber,
  Button
} from 'sup-ui';
import { injectIntl } from 'react-intl';
import { Select as SupSelect } from 'sup-rc-syscode';
import messages from './messages';
import style from './style.less';
import { USER_DIR_SYSCODE } from './constant';

class UserDirEditModal extends React.Component {
  state = {
    submitLoading: false,
    testLoading: false
  };

  intl(key, data) {
    const { intl } = this.props;
    return intl.formatMessage(messages[key], data);
  }

  handleSave = () => {
    const { form, handleSave } = this.props;
    this.setState({
      submitLoading: true
    });
    form
      .validateFieldsAndScroll()
      .then((values) => {
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

  handleUserDirTypeChange = () => {
    // const { form } = this.props;
    // FIXME
    // form.resetFields();
  };

  renderFooter() {
    const { submitLoading, testLoading } = this.state;

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
        <Button
          ghost
          type="primary"
          key="test"
          loading={testLoading}
          onClick={this.handleTest}
          style={{ float: 'right' }}
        >
          {this.intl('modalBtnTest')}
        </Button>
      </div>
    );
  }

  handleTest = () => {
    // 校验必填项
    const { form } = this.props;
    form.validateFieldsAndScroll().then((values) => {
      this.setState({
        testLoading: true
      });
      this.props.handleTest(values).then(() => {
        this.setState({
          testLoading: false
        });
      });
    });
  };

  resetLoading = () => {
    this.setState({
      testLoading: false,
      submitLoading: false
    });
  };

  handleSslChange = ({ target: { checked } }) => {
    const { form } = this.props;
    form.setFieldsValue({
      enableSsl: checked
    });
    this.setState({});
  };

  render() {
    const { handleCancel, form, visible } = this.props;
    const { getFieldDecorator, getFieldValue } = form;
    const userDirType = getFieldValue('directoryType');
    const enableSsl = getFieldValue('enableSsl');

    const title = (
      <div>
        <a className={style.modalTitleBack} onClick={handleCancel}>
          {this.intl('modalTitleBack')}
        </a>
        <span className={style.modalTitleSep}>/</span>
        <span className={style.modalTitle}>
          {this.intl('addUserDirModalTitle')}
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

            <Form.Item style={{ display: 'none' }}>
              {getFieldDecorator('enableSsl', { valuePropName: 'checked' })(
                <Checkbox />
              )}
            </Form.Item>

            <Form.Item label={this.intl('formItemDirType')}>
              {getFieldDecorator('directoryType', {
                rules: [
                  {
                    required: true,
                    message: this.intl('directoryTypeRequried')
                  }
                ]
              })(
                <SupSelect
                  onChange={this.handleUserDirTypeChange}
                  placeholder={this.intl('formItemDirTypePlaceholder')}
                  style={{ width: 600 }}
                  entityCode={USER_DIR_SYSCODE}
                />
              )}
            </Form.Item>

            <Form.Item label={this.intl('formItemDirectoryName')}>
              {getFieldDecorator('directoryName', {
                rules: [
                  {
                    required: true,
                    message: this.intl('directoryNameRequried')
                  },
                  {
                    max: 50,
                    message: this.intl('formItemDirectoryNameMaxLength')
                  }
                ]
              })(<Input style={{ width: 600 }} />)}
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

            {userDirType ? (
              <div>
                <h4 className={style.formItemH3}>
                  {this.intl('formItemH3BaseSettings')}
                </h4>

                <Form.Item
                  label={this.intl('formItemHostName')}
                  extra={this.intl('formItemHostNameHint')}
                >
                  {getFieldDecorator('hostname', {
                    rules: [
                      {
                        required: true,
                        message: this.intl('formItemHostNameRequired')
                      },
                      {
                        max: 50,
                        message: this.intl('formItemHostNameMaxLength')
                      }
                    ]
                  })(<Input style={{ width: 600 }} />)}
                </Form.Item>

                <Form.Item label={this.intl('formItemPort')}>
                  {getFieldDecorator('port', {
                    rules: [
                      {
                        required: true,
                        message: this.intl('formItemPortRequired')
                      }
                    ]
                  })(<InputNumber min={0} max={65535} style={{ width: 97 }} />)}
                  <Checkbox
                    onChange={this.handleSslChange}
                    checked={enableSsl}
                    style={{ marginLeft: 12 }}
                  >
                    SSL
                  </Checkbox>
                </Form.Item>

                <Form.Item
                  label={this.intl('formItemUsername')}
                  extra={
                    <span>
                      {this.intl('formItemUsernameHintMs')}
                      <br />
                      {this.intl('formItemUsernameHintLdap')}
                    </span>
                  }
                >
                  {getFieldDecorator('userName', {
                    rules: [
                      {
                        required: true,
                        message: this.intl('formItemUserNameRequired')
                      },
                      {
                        max: 255,
                        message: this.intl('formItemUserNameMaxLength')
                      }
                    ]
                  })(<Input style={{ width: 600 }} />)}
                </Form.Item>

                <Form.Item label={this.intl('formItemPassword')}>
                  {getFieldDecorator('password', {
                    rules: [
                      {
                        required: true,
                        message: this.intl('formItemPasswordRequired')
                      }
                    ]
                  })(
                    <Input.Password
                      autoComplete="new-password"
                      style={{ width: 600 }}
                    />
                  )}
                </Form.Item>

                {/* <h4 className={style.formItemH3}>
                  {this.intl('formItemH3LDAPModel')}
                </h4>

                <Form.Item
                  label={this.intl('formItemBaseDN')}
                  extra={this.intl('formItemBaseDNHint')}
                >
                  {getFieldDecorator(
                    'baseDn',
                    {}
                  )(<Input style={{ width: 600 }} />)}
                </Form.Item>

                <Form.Item
                  label={this.intl('formItemAdditionUserDN')}
                  extra={this.intl('formItemAdditionUserDNHint')}
                >
                  {getFieldDecorator(
                    'attachUserDn',
                    {}
                  )(<Input style={{ width: 600 }} />)}
                </Form.Item>

                <Form.Item
                  label={this.intl('formItemAdditionGroupDN')}
                  extra={this.intl('formItemAdditionGroupDNHint')}
                >
                  {getFieldDecorator(
                    'attachGroupDn',
                    {}
                  )(<Input style={{ width: 600 }} />)}
                </Form.Item>

                <h4 className={style.formItemH3}>
                  {this.intl('formItemH3LDAPAuth')}
                </h4>

                <Form.Item label={this.intl('formItemLDAPAuth')}>
                  {getFieldDecorator(
                    'permission',
                    {}
                  )(
                    <Radio.Group className={style.permission}>
                      <Radio value="true">
                        {this.intl('formItemLDAPAuthOptionReadOnly')}
                      </Radio>
                      <span className="ant-form-extra">
                        {this.intl('formItemLDAPAuthOptionReadOnlyHint')}
                      </span>
                      <Radio value={2}>
                        {this.intl(
                          'formItemLDAPAuthOptionReadOnlyOrLocalGroup'
                        )}
                      </Radio>
                      <span className="ant-form-extra">
                        {this.intl(
                          'formItemLDAPAuthOptionReadOnlyOrLocalGroupHint'
                        )}
                      </span>
                    </Radio.Group>
                  )}
                </Form.Item> */}
              </div>
            ) : null}
          </Form>
        </div>
      </Modal>
    );
  }
}

export default Form.create({
  name: 'userDirEditForm',
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
})(injectIntl(UserDirEditModal));
