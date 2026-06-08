import React from 'react';
import { Form, Input, Row, Col } from 'sup-ui';
import { SupI18nSelect } from 'sup-rc-i18n';
import { injectIntl } from 'react-intl';
import { InputCron } from '../../components/React-Cron';
import messages from './messages';
import style from './style.less';
import HelpWrapper from './helpWrapper';

const isEmptyObj = (o) => o && Object.keys(o).length > 0;

class AddForm extends React.Component {
  // handleCheckUrl = (_, value, callback) => {
  //   const { intl } = this.props;
  //   const regUrl = /(http):\/\/([\w.]+\/?)\S*/;
  //   if (value && !regUrl.test(value)) {
  //     callback(intl.formatMessage(messages.errorUrl));
  //   } else callback();
  // }

  isTouched(value) {
    return value && isEmptyObj(value.i18nValue);
  }

  onChangeInitValue = (value, key) => {
    const { setFields, validateFields, getFieldError } = this.props.form;
    setFields({
      [key]: {
        value,
        errors: getFieldError(key)
      }
    }, () => {
      // bug125002:提交后提示长度超出,删除字符仍然超出但是提示消失
      if (this.isTouched(value)) {
        validateFields([key]);
      }
    });
  }

  handleCheckName = (rule, value, callback) => {
    const { intl } = this.props;
    const hasValue = this.i18nName.getValidate({ required: true });
    const isLength = this.i18nName.getValidate({ maxLength: 255 });

    if (!hasValue) {
      callback(`${intl.formatMessage(messages.pleaseEnter)}${intl.formatMessage(messages.taskName)}`);
    } else if (!isLength) {
      callback(`${intl.formatMessage(messages.taskName)}${intl.formatMessage(messages.maxLength)}`);
    } else {
      callback();
    }
  }

  render() {
    const {
      intl,
      record,
      bol,
      form,
      isEdit,
      code,
      activeName
    } = this.props;
    const { getFieldDecorator } = form;
    const language = localStorage.getItem('language');
    const i18nParams = record.jobName ? {
      i18nValue: {
        [language]: record.jobName
      }
    } : {};
    return (
      <Form>
        {
          bol ? null : (
            <div>
              <Row gutter={24}>
                <Col span={12}>
                  <Form.Item
                    label={intl.formatMessage(messages.taskName)}
                    colon={false}
                  >
                    {getFieldDecorator('jobName', {
                      initialValue: {
                        moduleCode: code,
                        i18nKey: record.jobKey,
                        ...i18nParams
                      },
                      rules: [
                        {
                          required: true,
                          validator: this.handleCheckName
                        }
                      ]
                    })(
                    // <Input placeholder={`${intl.formatMessage(messages.pleaseEnter)}${intl.formatMessage(messages.taskName)}`} />
                      <SupI18nSelect
                        ref={(ref) => { this.i18nName = ref; }}
                        placeholder={intl.formatMessage(messages.pleaseEnterIntl)}
                        callback={(value) => this.onChangeInitValue(value, 'jobName')}
                      />
                    )}
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    label={intl.formatMessage(messages.taskCode)}
                    colon={false}
                  >
                    {getFieldDecorator('code', {
                      initialValue: record.code,
                      rules: [
                        {
                          required: true,
                          message: `${intl.formatMessage(messages.pleaseEnter)}${intl.formatMessage(messages.taskCode)}`
                        },
                        {
                          max: 255,
                          message: `${intl.formatMessage(messages.taskCode)}${intl.formatMessage(messages.maxLength)}`
                        }
                      ]
                    })(<Input disabled={record.length !== 0} />)}
                  </Form.Item>
                </Col>
              </Row>
              <Form.Item key="moduleCode" style={{ display: 'none' }}>
                {getFieldDecorator('moduleCode', {
                  initialValue: record.moduleCode || code
                })(<Input readOnly />)}
              </Form.Item>
              <Form.Item
                label={intl.formatMessage(messages.modalName)}
                colon={false}
              >
                {getFieldDecorator('modelName', {
                  rules: [
                    {
                      required: true,
                      message: `${intl.formatMessage(messages.pleaseEnter)}${intl.formatMessage(messages.modalName)}`
                    }
                  ],
                  initialValue: record.modelName || activeName
                })(
                  <Input
                    placeholder={`${intl.formatMessage(messages.pleaseEnter)}${intl.formatMessage(messages.modalName)}`}
                    disabled={!!isEdit}
                  />
                )}
              </Form.Item>
              <Form.Item
                label={<HelpWrapper tip="url" label={intl.formatMessage(messages.interfaceUrl)} />}
                colon={false}
              >
                {getFieldDecorator('serviceApi', {
                  rules: [
                    {
                      required: true,
                      message: `${intl.formatMessage(messages.pleaseEnter)}${intl.formatMessage(messages.interfaceUrl)}`
                    },
                    {
                      max: 255,
                      message: `${intl.formatMessage(messages.interfaceUrl)}${intl.formatMessage(messages.maxLength)}`
                    }
                  ],
                  initialValue: record.serviceApi
                })(<Input placeholder={`${intl.formatMessage(messages.urlTemplate)}`} />)}
              </Form.Item>
            </div>
          )
        }
        <Form.Item
          label={<HelpWrapper tip="cron" label={intl.formatMessage(messages.croExpression)} />}
          colon={false}
        >
          {getFieldDecorator('jobCron', {
            rules: [
              {
                required: true,
                message: `${intl.formatMessage(messages.pleaseEnter)}${intl.formatMessage(messages.croExpression)}`
              }
            ],
            initialValue: record.jobCron
          })(
            <InputCron
              style={{ width: 576, padding: '0 10px' }}
              type={['second', 'minute', 'hour', 'day', 'month', 'week']}
            />
          )}
        </Form.Item>
        {
          bol ? null : (
            <div>
              <Form.Item
                label={<HelpWrapper tip="param" label={intl.formatMessage(messages.taskParams)} />}
                colon={false}
              >
                {getFieldDecorator('serviceParams', {
                  rules: [
                    {
                      max: 255,
                      message: `${intl.formatMessage(messages.taskParams)}${intl.formatMessage(messages.maxLength)}`
                    }
                  ],
                  initialValue: record.serviceParams
                })(
                  <Input placeholder={`${intl.formatMessage(messages.paramTemplate)}`} />
                )}
              </Form.Item>
            </div>
          )
        }
        <Form.Item
          label={intl.formatMessage(messages.taskDetail)}
          colon={false}
        >
          {getFieldDecorator('jobDesc', {
            rules: [
              {
                max: 255,
                message: `${intl.formatMessage(messages.taskDetail)}${intl.formatMessage(messages.maxLength)}`
              }
            ],
            initialValue: record.jobDesc
          })(
            <Input.TextArea
              className={style.textarea}
              placeholder={intl.formatMessage(messages.pleaseEnterTaskDescribe)}
            />
          )}
        </Form.Item>
      </Form>
    );
  }
}

const WrappedAddForm = Form.create({ name: 'addForm' })(injectIntl(AddForm));

export default WrappedAddForm;
