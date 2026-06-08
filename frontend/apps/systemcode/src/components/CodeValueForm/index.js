import React, { Component } from 'react';
import { Input, Form, Row, Col, TreeSelect, Radio } from 'sup-ui';
import { SupI18nSelect } from 'sup-rc-i18n';
import messages from 'root/common/messages';

const { TextArea } = Input;
const { TreeNode } = TreeSelect;

@Form.create()
class CodeForm extends Component {
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
      callback(intl.formatMessage(messages.pleaseEnterValue));
    } else if (!isInLength) {
      callback(intl.formatMessage(messages.codeMaxLengthTip, { count: 500 }));
    } else {
      callback();
    }
  }

  handleChangeParentNode = (value, label) => {
    this.props.form.setFieldsValue({ parentName: label[0] });
  }

  renderTreeNode = (data = []) => {
    return data.map((item) => {
      const { key, title, children } = item;
      return (
        <TreeNode value={key} title={title} key={key}>
          {children && this.renderTreeNode(children)}
        </TreeNode>
      );
    });
  }

  render() {
    const { intl, data = {}, type, isTree } = this.props;
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

    return (
      <Form layout="vertical" style={{ padding: '20px 64px', height: '100%', overflow: 'auto' }}>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item style={{ display: 'none' }}>
              {
                getFieldDecorator('entityCode', {
                  initialValue: data.entityCode
                })(
                  <Input />
                )
              }
            </Form.Item>
            <Form.Item style={{ display: 'none' }}>
              {
                getFieldDecorator('cid', {
                  initialValue: data.cid || cid
                })(
                  <Input />
                )
              }
            </Form.Item>
            <Form.Item label={intl.formatMessage(messages.code)} style={{ marginBottom: 7 }}>
              {
                getFieldDecorator('code', {
                  initialValue: data.code,
                  rules: [
                    {
                      required: true,
                      message: intl.formatMessage(messages.pleaseEnterCode)
                    },
                    { pattern: /^[A-Za-z0-9_]{0,100}$/, message: intl.formatMessage(messages.codeRuleTip) }
                  ]
                })(
                  <Input placeholder={intl.formatMessage(messages.pleaseEnter)} disabled={type === 'editCodeValue'} />
                )
              }
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item label={intl.formatMessage(messages.value)} style={{ marginBottom: 7 }}>
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
            <Form.Item label={intl.formatMessage(messages.applicationRange)} style={{ marginBottom: 7 }}>
              {
                getFieldDecorator('companyName', {
                  initialValue: data.companyName || companyName
                })(
                  <Input disabled />
                )
              }
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item label={intl.formatMessage(messages.default)} style={{ marginBottom: 7 }}>
              {
                getFieldDecorator('defaultFlag', {
                  initialValue: data.defaultFlag || 0
                })(
                  <Radio.Group>
                    <Radio value={1}>{intl.formatMessage(messages.yes)}</Radio>
                    <Radio value={0}>{intl.formatMessage(messages.no)}</Radio>
                  </Radio.Group>
                )
              }
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={12} style={{ display: isTree ? 'block' : 'none' }}>
            <Form.Item style={{ display: 'none' }}>
              {
                getFieldDecorator('parentName', {
                  initialValue: data.parentName
                })(
                  <Input />
                )
              }
            </Form.Item>
            <Form.Item label={intl.formatMessage(messages.parentNode)} style={{ marginBottom: 7 }}>
              {
                getFieldDecorator('parentDisplayName', {
                  initialValue: data.parentDisplayName
                })(
                  <Input disabled />
                )
              }
            </Form.Item>
            <Form.Item style={{ display: 'none' }}>
              {
                getFieldDecorator('parentId', {
                  initialValue: data.parentId
                })(
                  <Input disabled />
                  // <TreeSelect
                  //   disabled
                  //   showSearch
                  //   style={{ width: '100%' }}
                  //   dropdownStyle={{ maxHeight: 400, overflow: 'auto' }}
                  //   allowClear
                  //   treeDefaultExpandAll
                  //   onChange={this.handleChangeParentNode}
                  // >
                  //   {
                  //     this.renderTreeNode(treeData)
                  //   }
                  // </TreeSelect>
                )
              }
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item label={intl.formatMessage(messages.descriptionA)} style={{ marginBottom: 7 }}>
              {
                getFieldDecorator('desA', {
                  initialValue: data.desA,
                  rules: [
                    {
                      max: 255,
                      message: intl.formatMessage(messages.codeMaxLengthTip, { count: 255 })
                    }
                  ]
                })(
                  <Input />
                )
              }
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item label={intl.formatMessage(messages.descriptionB)} style={{ marginBottom: 7 }}>
              {
                getFieldDecorator('desB', {
                  initialValue: data.desB,
                  rules: [
                    {
                      max: 255,
                      message: intl.formatMessage(messages.codeMaxLengthTip, { count: 255 })
                    }
                  ]
                })(
                  <Input />
                )
              }
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item label={intl.formatMessage(messages.descriptionC)} style={{ marginBottom: 7 }}>
              {
                getFieldDecorator('desC', {
                  initialValue: data.desC,
                  rules: [
                    {
                      max: 255,
                      message: intl.formatMessage(messages.codeMaxLengthTip, { count: 255 })
                    }
                  ]
                })(
                  <Input />
                )
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
                })(
                  <TextArea rows={3} />
                )
              }
            </Form.Item>
          </Col>
        </Row>
      </Form>
    );
  }
}

export default CodeForm;
