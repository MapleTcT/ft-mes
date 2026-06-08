import React from 'react';
import { Form, Input, Select } from 'sup-ui';
import messages from '../messages';
import { getFormatOptions, getFieldTypeOptions, getLanguage } from '../utils';
import FormLabel from './Label';

const { Option } = Select;

class BaseManageForm extends React.Component {
  constructor(props) {
    super(props);
    this.getLanguage = getLanguage;
  }

  getPropertyType() {
    const { formData } = this.props;
    return formData.property.type;
  }

  // 显示类型
  getFieldTypeOptions() {
    const propertyType = this.getPropertyType();
    return getFieldTypeOptions(propertyType).map((d) => {
      return (
        <Option value={d.val} key={d.val}>
          {d.text || this.intl([`type.${d.i18n}`])}
        </Option>
      );
    });
  }

  getPropertyTypeI18n() {
    const propertyType = this.getPropertyType();
    return this.intl([`type.${propertyType}`]);
  }

  // 显示格式
  getFormatOptions() {
    const propertyType = this.getPropertyType();
    return getFormatOptions(propertyType).map((d) => {
      return (
        <Option value={d.val} key={d.val}>
          {d.text || this.intl([`format.${d.i18n}`])}
        </Option>
      );
    });
  }

  handleI18nValueChange = (value) => {
    const { setFieldsValue } = this.props.form;
    setFieldsValue({
      i18n: value
    });
  };

  intl(id, obj = {}) {
    const { intl } = this.props;
    return intl.formatMessage(messages[id], obj);
  }

  getI18nValue() {
    const { formData } = this.props;
    const moduleCode = (formData.displayName || '').split('.')[0];
    const language = this.getLanguage();
    return {
      moduleCode: moduleCode || 'customProperty',
      i18nKey: formData.displayName,
      i18nValue: {
        [language]: formData.displayNameInternational
      }
    };
  }

  renderLabel(id, required) {
    const data = { id, required };
    return <FormLabel {...data} />;
  }

  renderHiddenFields() {
    const { formData } = this.props;
    const { getFieldDecorator } = this.props.form;
    const { hiddenFields } = this;
    return hiddenFields.map((field) => {
      const keys = field.split('.');
      const data = keys.reduce((d, key) => {
        d = d[key];
        return d;
      }, formData);
      return (
        <Form.Item key={field} style={{ display: 'none' }}>
          {getFieldDecorator(field, {
            initialValue: data
          })(<Input readOnly />)}
        </Form.Item>
      );
    });
  }

  render() {
    return (
      <Form>
        {this.renderHiddenFields()}
        {this.renderFields()}
      </Form>
    );
  }
}

export default BaseManageForm;
