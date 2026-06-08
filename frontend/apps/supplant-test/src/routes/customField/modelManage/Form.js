import React from 'react';
import { Form, Input, Row, Col, Checkbox, Select, InputNumber } from 'sup-ui';
import { injectIntl } from 'react-intl';
import { SupI18nSelect } from 'sup-rc-i18n';
import { getRefViews } from 'root/services/customProperty';
import BaseManageForm from '../component/BaseForm';
import SelectComp from '../component/SelectComp';
import Systemcode from '../component/Systemcode';
import {
  PROPERTY_TYPE,
  REFVIEW_FIELD,
  REFVIEW_CODE,
  FILL_CONTENT_CODE,
  ASSOC_CODE,
  PROPERTYTYPE
} from '../constant';
import { getI18nLanguage } from '../utils';

const { DECIMAL, OBJECT, SYSTEMCODE } = PROPERTY_TYPE;
const { Option } = Select;
const { TextArea } = Input;

class ModelManageForm extends BaseManageForm {
  constructor(props) {
    super(props);
    this.hiddenFields = ['property.code', 'property.type', REFVIEW_FIELD];
  }

  getRefViews() {
    const { getFieldValue } = this.props.form;
    return getFieldValue(REFVIEW_FIELD) || [];
  }

  changeId = (value) => {
    const { setFieldsValue } = this.props.form;
    setFieldsValue({
      [ASSOC_CODE]: value
    });
  };

  handleSystemcodeChange = (code) => {
    const { setFieldsValue } = this.props.form;
    setFieldsValue({
      [FILL_CONTENT_CODE]: code
    });
  };

  changeModel = (modelCode, isDefault) => {
    const { setFieldsValue } = this.props.form;
    const lastRefView = this.getRefViews();
    // 清空参照视图选择
    if (!modelCode && lastRefView && lastRefView.length) {
      setFieldsValue({
        [REFVIEW_CODE]: '',
        [REFVIEW_FIELD]: []
      });
    } else if (modelCode) {
      // 获取参照视图
      getRefViews({ modelCode }).then(({ data: { list } }) => {
        const refs = list.map((d) => {
          const { code, displayNameInternational: name } = d;
          return {
            code,
            name
          };
        });
        const fieldData = {
          [REFVIEW_FIELD]: refs
        };
        if (!isDefault) {
          // 非默认值情况才重置参照视图值
          fieldData[REFVIEW_CODE] = '';
        }
        setFieldsValue(fieldData);
      });
    }
  };

  renderRefViewOptions() {
    const views = this.getRefViews();
    return views.map((view) => {
      return (
        <Option key={view.code} value={view.code}>
          {view.name}
        </Option>
      );
    });
  }

  getRefViewDisableState() {
    // 参照视图未全部获取前, 不允许选择参照视图
    const [ref = {}] = this.getRefViews();
    return ref.isFake;
  }

  renderFields() {
    const { formData } = this.props;
    const { property } = formData;
    const { getFieldDecorator } = this.props.form;

    return (
      <>
        <Row>
          <Col span={4}>{this.renderLabel('fieldName')}</Col>
          <Col span={8}>
            <Form.Item>
              {getFieldDecorator('property.name', {
                initialValue: property.name
              })(<Input readOnly />)}
            </Form.Item>
          </Col>
          <Col span={4}>{this.renderLabel('displayName', true)}</Col>
          <Col span={8}>
            <Form.Item>
              {getFieldDecorator('i18n', {
                initialValue: this.getI18nValue(),
                rules: [
                  {
                    validator: (_, value, callback) => {
                      const { i18nValue } = value;
                      if (!i18nValue[getI18nLanguage()]) {
                        callback(this.intl('requiredDisplayName'));
                      } else {
                        callback();
                      }
                    }
                  }
                ]
              })(<SupI18nSelect callback={this.handleI18nValueChange} />)}
            </Form.Item>
          </Col>
        </Row>
        <Row>
          <Col span={4}>{this.renderLabel('fieldType')}</Col>
          <Col span={8}>
            <Form.Item>
              {getFieldDecorator(PROPERTYTYPE, {
                initialValue: this.getPropertyTypeI18n()
              })(<Input readOnly />)}
            </Form.Item>
          </Col>
          <Col span={3} offset={2}>
            <Form.Item>
              {getFieldDecorator('enableCustom', {
                valuePropName: 'checked',
                initialValue: formData.enableCustom
              })(<Checkbox>{this.intl('enable')}</Checkbox>)}
            </Form.Item>
          </Col>
          <Col span={3}>
            <Form.Item>
              {getFieldDecorator('nullable', {
                valuePropName: 'checked',
                initialValue: formData.nullable
              })(<Checkbox>{this.intl('nullable')}</Checkbox>)}
            </Form.Item>
          </Col>
          {property.type === SYSTEMCODE && (
            <Col span={3}>
              <Form.Item>
                {getFieldDecorator('multable', {
                  valuePropName: 'checked',
                  initialValue: formData.multable
                })(<Checkbox>{this.intl('multable')}</Checkbox>)}
              </Form.Item>
            </Col>
          )}
        </Row>
        <Row>
          <Col span={4}>{this.renderLabel('displayType')}</Col>
          <Col span={8}>
            <Form.Item>
              {getFieldDecorator('fieldType', {
                initialValue: formData.fieldType
              })(
                <Select
                  style={{ width: '100%' }}
                  onChange={this.handleSelectChange}
                >
                  {this.getFieldTypeOptions()}
                </Select>
              )}
            </Form.Item>
          </Col>
          <Col span={4}>{this.renderLabel('format')}</Col>
          <Col span={8}>
            <Form.Item>
              {getFieldDecorator('format', {
                initialValue: formData.format
              })(
                <Select
                  style={{ width: '100%' }}
                  onChange={this.handleSelectChange}
                >
                  {this.getFormatOptions()}
                </Select>
              )}
            </Form.Item>
          </Col>
        </Row>
        {property.type === OBJECT && (
          <>
            <Row>
              <Col span={4}>{this.renderLabel('selectCompLabel')}</Col>
              <Col span={20}>
                <Form.Item>
                  {getFieldDecorator(ASSOC_CODE, {
                    initialValue: formData[ASSOC_CODE],
                    rules: [
                      {
                        required: true,
                        message: this.intl('requiredAssocCode')
                      }
                    ]
                  })(
                    <SelectComp
                      moduleCode={formData.moduleCode}
                      onChangeModel={this.changeModel}
                      onChangeValue={this.changeId}
                      associatedProperty={formData.associatedProperty}
                    />
                  )}
                </Form.Item>
              </Col>
            </Row>
            <Row>
              <Col span={4}>{this.renderLabel('associatedType')}</Col>
              <Col span={8}>
                <Form.Item>
                  {getFieldDecorator('associatedType', {
                    initialValue: formData.associatedType,
                    rules: [
                      {
                        required: true,
                        message: this.intl('requiredAssocType')
                      }
                    ]
                  })(
                    <Select style={{ width: '100%' }}>
                      <Option value={1}>{this.intl('oneToOne')}</Option>
                      <Option value={2}>{this.intl('manyToOne')}</Option>
                    </Select>
                  )}
                </Form.Item>
              </Col>
              <Col span={4}>{this.renderLabel('refView')}</Col>
              <Col span={8}>
                <Form.Item>
                  {getFieldDecorator(REFVIEW_CODE, {
                    initialValue: formData[REFVIEW_CODE],
                    rules: [
                      {
                        required: true,
                        message: this.intl('requiredRefView')
                      }
                    ]
                  })(
                    <Select
                      style={{ width: '100%' }}
                      disabled={this.getRefViewDisableState()}
                    >
                      {this.renderRefViewOptions()}
                    </Select>
                  )}
                </Form.Item>
              </Col>
            </Row>
          </>
        )}
        <Row>
          <Col span={4}>{this.renderLabel('associatedProperty')}</Col>
          <Col span={8}>
            <Form.Item>
              {getFieldDecorator('relatedKey', {
                initialValue: formData.relatedKey
              })(<Input />)}
            </Form.Item>
          </Col>
          {property.type === SYSTEMCODE && (
            <>
              <Col span={4}>{this.renderLabel('systemcode')}</Col>
              <Col span={8}>
                <Form.Item>
                  {getFieldDecorator(FILL_CONTENT_CODE, {
                    initialValue: formData.fillContent
                      ? formData.fillContent.fillContent
                      : '',
                    rules: [
                      {
                        required: true,
                        message: this.intl('requiredSystemcode')
                      }
                    ]
                  })(
                    <Systemcode
                      handleSelectChange={this.handleSystemcodeChange}
                      moduleCode={formData.moduleCode}
                    />
                  )}
                </Form.Item>
              </Col>
            </>
          )}
          {/* 添加小数位数 */}
          {property.type === DECIMAL && (
            <>
              <Col span={4}>{this.renderLabel('precision')}</Col>
              <Col span={8}>
                <Form.Item>
                  {getFieldDecorator('precision', {
                    initialValue: formData.precision
                  })(<InputNumber style={{ width: '100%' }} />)}
                </Form.Item>
              </Col>
            </>
          )}
        </Row>
        <Row>
          <Col span={4}>{this.renderLabel('desc')}</Col>
          <Col span={20}>
            <Form.Item>
              {getFieldDecorator('description', {
                initialValue: formData.description
              })(<TextArea rows={3} />)}
            </Form.Item>
          </Col>
        </Row>
      </>
    );
  }
}

export default Form.create({
  name: 'ModelManageForm'
})(injectIntl(ModelManageForm));
