import React from 'react';
import { Form, Input, Row, Col, Checkbox, Select, InputNumber } from 'sup-ui';
import { injectIntl } from 'react-intl';
import { SupI18nSelect } from 'sup-rc-i18n';
import style from '../style.less';
import BaseManageForm from '../component/BaseForm';
import { PROPERTY_TYPE, VIEW_TYPE } from '../constant';
import { getI18nLanguage } from '../utils';

const { EDIT, EXTRA, DATAGRID, VIEW } = VIEW_TYPE;
const { SYSTEMCODE, DECIMAL, TEXT } = PROPERTY_TYPE;
const { Option } = Select;

class ModelManageForm extends BaseManageForm {
  constructor(props) {
    super(props);
    this.hiddenFields = [
      'property.code',
      'property.type',
      'associatedCode',
      'propertyLayRec',
      'textareaRow'
    ];
  }

  getAlginOptions() {
    return ['left', 'center', 'right'].map((d) => {
      return (
        <Option key={d} val={d}>
          {this.intl(`align.${d}`)}
        </Option>
      );
    });
  }

  showColSpan(item) {
    const { viewType } = item;
    return !!~[EDIT, VIEW].indexOf(viewType);
  }

  showNullableReadonly(item) {
    const { viewType, propertyLayRec } = item;
    const pIndex = (propertyLayRec || '').split('||')[0].indexOf('.');
    // 编辑或者增强显示, datagrid显示
    return !!(
      // eslint-disable-next-line operator-linebreak
      (~[EXTRA, EDIT].indexOf(viewType) || (viewType === DATAGRID && !~pIndex))
    );
  }

  renderFields() {
    const { formData } = this.props;
    const { property, textareaRow } = formData;
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
              {getFieldDecorator('propertyType', {
                initialValue: this.getPropertyTypeI18n()
              })(<Input readOnly />)}
            </Form.Item>
          </Col>
          <Col span={3} offset={2}>
            <Form.Item>
              {getFieldDecorator('showCustom', {
                valuePropName: 'checked',
                initialValue: formData.showCustom
              })(<Checkbox>{this.intl('showCustom')}</Checkbox>)}
            </Form.Item>
          </Col>
          {this.showNullableReadonly(formData) && (
            <>
              <Col span={3}>
                <Form.Item>
                  {getFieldDecorator('nullable', {
                    valuePropName: 'checked',
                    initialValue: formData.nullable
                  })(<Checkbox>{this.intl('nullable')}</Checkbox>)}
                </Form.Item>
              </Col>
              <Col span={3}>
                <Form.Item>
                  {getFieldDecorator('readonly', {
                    valuePropName: 'checked',
                    initialValue: formData.readonly
                  })(<Checkbox>{this.intl('readonly')}</Checkbox>)}
                </Form.Item>
              </Col>
            </>
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
                  disabled
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
                  disabled
                  style={{ width: '100%' }}
                  onChange={this.handleSelectChange}
                >
                  {this.getFormatOptions()}
                </Select>
              )}
            </Form.Item>
          </Col>
        </Row>

        <Row>
          {property.type !== SYSTEMCODE && (
            <>
              <Col span={4}>{this.renderLabel('align')}</Col>
              <Col span={8}>
                <Form.Item>
                  {getFieldDecorator('align', {
                    initialValue: formData.align
                  })(
                    <Select
                      style={{ width: '100%' }}
                      onChange={this.handleSelectChange}
                    >
                      {this.getAlginOptions()}
                    </Select>
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
          {/* 添加长度 */}
          {property.type === TEXT && (
            <>
              <Col span={4}>{this.renderLabel('fieldLength')}</Col>
              <Col span={8}>
                <Form.Item>
                  {getFieldDecorator('length', {
                    initialValue: formData.length
                  })(<InputNumber style={{ width: '100%' }} />)}
                </Form.Item>
              </Col>
            </>
          )}
        </Row>
        {this.showColSpan(formData) && (
          <Row className={style.viewFormColSpanWrap}>
            <Col span={4} />
            <Col span={20}>
              <span>{this.intl('colSpanLeft', { textareaRow })}</span>
              <Form.Item className={style.viewFormColSpan}>
                {getFieldDecorator('colspan', {
                  initialValue: formData.colspan
                })(<InputNumber style={{ width: 50 }} />)}
              </Form.Item>
              <span>{this.intl('colSpanRight')}</span>
            </Col>
          </Row>
        )}
      </>
    );
  }
}

export default Form.create({
  name: 'ModelManageForm'
})(injectIntl(ModelManageForm));
