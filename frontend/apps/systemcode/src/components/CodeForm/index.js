import React, { PureComponent } from 'react';
import { Input, Form, Select, Row, Col } from 'sup-ui';
import { SupI18nSelect } from 'sup-rc-i18n';
import messages from 'root/common/messages';

const { Option } = Select;
const { TextArea } = Input;

@Form.create()
class CodeForm extends PureComponent {
  // 初始化国际化
  onChangeInitValue = (value, key) => {
    const { setFieldsValue } = this.props.form;
    setFieldsValue({
      [key]: value
    });
  }

  // 名称校验
  handleCheckName = (rule, value, callback) => {
    const { intl } = this.props;
    const hasValue = this.i18nName.getValidate({ required: true });
    const isInLength = this.i18nName.getValidate({ maxLength: 500 });

    if (!hasValue) {
      callback(intl.formatMessage(messages.pleaseEnterName));
    } else if (!isInLength) {
      callback(intl.formatMessage(messages.codeMaxLengthTip, { count: 500 }));
    } else {
      callback();
    }
  }

  handleCheckCount = (rule, value, callback) => {
    const { intl, data = {}, type } = this.props;
    const addonBefore = type === 'add' ? `${data.moduleId || 'sys'}_` : '';
    const prevCount = addonBefore.length;
    const reg = new RegExp(/^[A-Za-z0-9_]*$/);

    if (!value) {
      callback(intl.formatMessage(messages.pleaseEnterCode));
    } else if (value.length > 100 - prevCount || !reg.test(value)) {
      callback(intl.formatMessage(messages.codeRuleTip));
    } else {
      callback();
    }
  }

  render() {
    const { intl, data = {}, type, source } = this.props;
    const { getFieldDecorator } = this.props.form;

    const loginMsg = localStorage.getItem('loginMsg') ? JSON.parse(localStorage.getItem('loginMsg')) : {};
    const companyName = _.get(loginMsg, 'currentCompany.name', '');
    const cid = _.get(loginMsg, 'currentCompany.id', '');

    const language = localStorage.getItem('language');
    const i18nParam = data.displayName ? {
      i18nValue: {
        [language]: data.displayName
      }
    } : {};

    const addonBefore = type === 'add' ? `${data.moduleId || 'sys'}_` : '';
    const beforeDom = addonBefore ? (
      <span
        title={addonBefore}
        style={{
          maxWidth: 100,
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap'
        }}
      >
        {addonBefore}
      </span>
    ) : null;

    return (
      <Form layout="vertical" style={{ padding: '0 16px' }}>
        <Row gutter={16}>
          <Col span={12}>
            {
              source === 'lcdp' && (
                <Form.Item style={{ display: 'none' }}>
                  {
                    getFieldDecorator('source', {
                      initialValue: 'supide'
                    })(<Input />)
                  }
                </Form.Item>
              )
            }
            <Form.Item style={{ display: 'none' }}>
              {
                getFieldDecorator('moduleId', {
                  initialValue: data.moduleId
                })(<Input />)
              }
            </Form.Item>
            <Form.Item style={{ display: 'none' }}>
              {
                getFieldDecorator('cid', {
                  initialValue: data.cid || cid
                })(<Input />)
              }
            </Form.Item>
            <Form.Item label={intl.formatMessage(messages.code)} style={{ marginBottom: 16 }}>
              {
                getFieldDecorator('code', {
                  initialValue: data.code,
                  rules: [
                    {
                      required: true,
                      validator: this.handleCheckCount
                    }
                  ]
                })(
                  <Input
                    addonBefore={beforeDom}
                    placeholder={intl.formatMessage(messages.pleaseEnter)}
                    disabled={type === 'edit'}
                  />
                )
              }
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item label={intl.formatMessage(messages.name)} style={{ marginBottom: 16 }}>
              {
                getFieldDecorator('name', {
                  initialValue: {
                    moduleCode: data.moduleId || 'systemCode',
                    i18nKey: data.name,
                    ...i18nParam
                  },
                  rules: [
                    {
                      required: true,
                      validator: this.handleCheckName
                    }
                  ]
                })(
                  <SupI18nSelect
                    ref={(ref) => { this.i18nName = ref; }}
                    placeholder={intl.formatMessage(messages.pleaseEnter)}
                    callback={(value) => this.onChangeInitValue(value, 'name')}
                  />
                )
              }
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item label={intl.formatMessage(messages.showType)} style={{ marginBottom: 16 }}>
              {
                getFieldDecorator('type', {
                  initialValue: data.type || 'list'
                })(
                  <Select disabled={type === 'edit'}>
                    <Option value="list">{intl.formatMessage(messages.list)}</Option>
                    <Option value="tree">{intl.formatMessage(messages.tree)}</Option>
                  </Select>
                )
              }
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              label={intl.formatMessage(messages.owningModule)}
              style={{
                marginBottom: 16,
                display: source === 'lcdp' ? 'none' : 'block'
              }}
            >
              {
                getFieldDecorator('moduleName', {
                  initialValue: data.moduleName
                })(<Input disabled />)
              }
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              label={intl.formatMessage(messages.applicationRange)}
              style={{
                marginBottom: 16,
                display: source === 'lcdp' ? 'none' : 'block'
              }}
            >
              {
                getFieldDecorator('companyName', {
                  initialValue: data.companyName || companyName
                })(<Input disabled />)
              }
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={24}>
            <Form.Item label={intl.formatMessage(messages.memo)} style={{ marginBottom: 0 }}>
              {
                getFieldDecorator('memo', {
                  initialValue: data.memo,
                  rules: [
                    {
                      max: 255,
                      message: intl.formatMessage(messages.codeMaxLengthTip, { count: 255 })
                    }
                  ]
                })(<TextArea rows={3} />)
              }
            </Form.Item>
          </Col>
        </Row>
      </Form>
    );
  }
}

export default CodeForm;
