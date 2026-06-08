import React from 'react';
import { injectIntl } from 'react-intl';
import { Layout, Form, Button, Radio, message, Input } from 'sup-ui';

import style from '../sysconfig/style.less';
import messages from './messages';
import ItemComplex from './ItemComplex';
import ItemLength from './ItemLength';
import {
  fetchPwdConfig,
  updatePwdConfig,
  resetPWdConfig
} from '../../services/pwdConfig';
import pwdStyle from './style.less';

const { Header, Content } = Layout;

const COMPLEX_TYPE = {
  group: 0,
  regular: 1
};

const isInteger = (value) => {
  if (isNaN(value)) {
    return false;
  }
  return typeof value === 'number' && parseInt(value, 10) === value;
};

class PwdConfig extends React.Component {
  state = {
    ruleType: COMPLEX_TYPE.group
  };

  componentDidMount() {
    this.initValue();
  }

  handleFormSubmit = (e) => {
    e.preventDefault();
    const {
      form: { validateFields },
      intl
    } = this.props;
    validateFields().then((data) => {
      const { pwdComplex, pwdRange, ruleType, findPwdSwitch, regularExpression, hint } = data;
      const pwdConfig = ruleType === COMPLEX_TYPE.group ? {
        ...pwdComplex
      } : {
        regularExpression,
        hint
      };
      updatePwdConfig({
        ...pwdConfig,
        ...pwdRange,
        ruleType,
        findPwdSwitch,
        id: this.formId
      }).then(() => {
        message.success(intl.formatMessage(messages.saveSuccess));
      });
    });
  };

  handleReset = () => {
    resetPWdConfig().then(() => {
      this.initValue().then(() => {
        const { intl } = this.props;
        message.success(intl.formatMessage(messages.resetSuccess));
      });
    });
  };

  initValue() {
    return fetchPwdConfig().then(({ data: { data } }) => {
      const { containLetterCase, maxLength, minLength, containNumbers, containSpecialChar, id, ruleType } = data;
      const {
        form: { setFieldsValue }
      } = this.props;
      // 更新值
      const pwdComplex = { containLetterCase, containNumbers, containSpecialChar };
      const pwdRange = { minLength, maxLength };
      this.formId = id;
      setFieldsValue({
        pwdComplex,
        pwdRange,
        ...data
      });
      this.setState({
        ruleType
      });
    });
  }

  pwdRangeValidator = (_, data = {}, cb) => {
    const { intl } = this.props;
    const { minLength, maxLength } = data;

    if (minLength == null || maxLength == null) {
      return cb(intl.formatMessage(messages.pwdLengthRequired));
    }

    if (!isInteger(minLength) || !isInteger(maxLength)) {
      return cb(intl.formatMessage(messages.pwdLengthIntegerRequired));
    }
    if (minLength === 0 || maxLength === 0) {
      return cb(intl.formatMessage(messages.pwdLengthGreateThanZero));
    }
    if (minLength < 8) {
      return cb(intl.formatMessage(messages.pwdMinLengthRequired));
    }
    if (maxLength > 32) {
      return cb(intl.formatMessage(messages.pwdMaxLengthRequired));
    }
    if (minLength >= maxLength) {
      return cb(intl.formatMessage(messages.pwdLengthRanged));
    }
    cb();
  };

  handleRuleTypeChange = (e) => {
    this.setState({
      ruleType: e.target.value
    });
  }

  render() {
    const {
      intl,
      form: { getFieldDecorator }
    } = this.props;
    const { ruleType } = this.state;
    return (
      <>
        <Header className={style.header}>
          {intl.formatMessage(messages.pwdConfigHeader)}
        </Header>
        <Content className={[style.content, pwdStyle.pwdConfigContent]}>
          <Form onSubmit={this.handleFormSubmit} colon={false}>
            <Form.Item
              className={pwdStyle.complexBox}
              label={intl.formatMessage(messages.pwdConfigItemPwdComplex)}
              required
            >
              {getFieldDecorator('ruleType', {
                initialValue: ruleType
              })(
                <Radio.Group name="ruleType" onChange={this.handleRuleTypeChange}>
                  <Radio value={COMPLEX_TYPE.group}>
                    {intl.formatMessage(messages.groupRule)}
                    <Form.Item style={{ paddingLeft: 24, marginBottom: 0, display: ruleType !== COMPLEX_TYPE.group ? 'none' : 'block' }}>
                      {getFieldDecorator('pwdComplex')(<ItemComplex />)}
                    </Form.Item>
                  </Radio>
                  {/* <Radio value={COMPLEX_TYPE.regular}>
                    {intl.formatMessage(messages.expression)}
                    <div style={{ paddingLeft: 24, display: ruleType !== COMPLEX_TYPE.regular ? 'none' : 'block' }}>
                      <Form.Item
                        style={{ marginBottom: 0 }}
                        label={intl.formatMessage(messages.regular)}
                        required
                      >
                        {getFieldDecorator('regularExpression')(<Input className={pwdStyle.inputEle} />)}
                      </Form.Item>
                      <Form.Item
                        style={{ marginBottom: 0 }}
                        label={intl.formatMessage(messages.verifyHintMsg)}
                        required
                      >
                        {getFieldDecorator('hint')(<Input className={pwdStyle.inputEle} />)}
                      </Form.Item>
                    </div>
                  </Radio> */}
                </Radio.Group>
              )}
            </Form.Item>
            <Form.Item
              label={intl.formatMessage(messages.pwdConfigItemPwdLength)}
              style={{ marginTop: 20 }}
              required
            >
              {getFieldDecorator('pwdRange', {
                rules: [
                  {
                    validator: this.pwdRangeValidator
                  }
                ]
              })(<ItemLength />)}
            </Form.Item>

            {/* <Form.Item
              label={intl.formatMessage(messages.forgetPassword)}
            >
              {getFieldDecorator('findPwdSwitch')(
                <Radio.Group name="findPwdSwitch">
                  <Radio value>{intl.formatMessage(messages.open)}</Radio>
                  <Radio value={false}>{intl.formatMessage(messages.close)}</Radio>
                </Radio.Group>
              )}
            </Form.Item> */}

            <Form.Item>
              <Button type="primary" htmlType="submit">
                {intl.formatMessage(messages.save)}
              </Button>
              <Button onClick={this.handleReset} style={{ marginLeft: '12px' }}>
                {intl.formatMessage(messages.reset)}
              </Button>
            </Form.Item>
          </Form>
        </Content>
      </>
    );
  }
}

const PwdConfigForm = Form.create({ name: 'pwdConfigForm' })(PwdConfig);

export default injectIntl(PwdConfigForm);
