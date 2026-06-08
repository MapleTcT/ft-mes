import React from 'react';
import { Form, Input, Icon } from 'sup-ui';
import { injectIntl } from 'react-intl';
import messages from './messages';
import { getCurrentUserName } from '../../utils';
import styles from './style.less';

const userName = getCurrentUserName();
class EditForm extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  handleCheckPaw = (rule, value, callback) => {
    const { intl, form } = this.props;
    const { field } =rule;
    const newPaw = form.getFieldValue('password');
    const oldPaw = form.getFieldValue('prepassword')
    if (field === 'password') {
      if(value && oldPaw === value) {
        callback(intl.formatMessage(messages.comparePaw));
      }
    } else if (field === 'repassword') {
      if (value && newPaw !== value) {
        callback(intl.formatMessage(messages.differentPaw));
      }
    }
    callback();
  }

  passwordInput = (txt) => {
    const passInput = (
      <span className={styles.passSpan} >
        <Icon className={styles.passIcon} type="lock" theme="filled" />
        <Input placeholder={txt} type='password' className={styles.passInput} />
      </span>
    );
    return passInput;
  }

  render() {
    const {
      intl,
      form
    } = this.props;
    const { getFieldDecorator } = form;
    return (
      <div className={styles.formBox}>
        <Form>
          <Form.Item label={intl.formatMessage(messages.userName)}>
            {getFieldDecorator('userName', {
              initialValue: userName
            })(<Input disabled />)}
          </Form.Item>
          <Form.Item label={intl.formatMessage(messages.oldPaw)}>
            {getFieldDecorator('prepassword', {
              rules: [
                {
                  required: true,
                  message: intl.formatMessage(messages.pleaseEnterOldPaw)
                }
              ],
              validateTrigger: 'onBlur'
            })(
              this.passwordInput(intl.formatMessage(messages.pleaseEnterOldPaw))
            )}
          </Form.Item>
          <Form.Item
            label={intl.formatMessage(messages.newPaw)}
            // extra={intl.formatMessage(messages.pawValidate)}
          >
            {getFieldDecorator('password', {
                  rules: [
                    {
                      required: true,
                      message: intl.formatMessage(messages.pleaseEnterNewPas)
                    },
                    {
                      validator:this.handleCheckPaw
                    }
                  ],
                  validateTrigger: 'onBlur'
            })(
              this.passwordInput(intl.formatMessage(messages.pleaseEnterNewPas))
            )}
          </Form.Item>
          <Form.Item label={intl.formatMessage(messages.confirmPaw)}>
            {getFieldDecorator('repassword', {
              rules: [
                {
                  required: true,
                  message: intl.formatMessage(messages.pleaseAgainEnterPaw)
                },
                {
                  validator: this.handleCheckPaw
                }
              ],
              validateTrigger: 'onBlur'
            })(
              this.passwordInput(intl.formatMessage(messages.pleaseAgainEnterPaw))
            )}
          </Form.Item>
        </Form>
      </div>
    );
  }
}

const WrappedEditForm = Form.create({ name: 'editForm' })(injectIntl(EditForm));

export default WrappedEditForm;
