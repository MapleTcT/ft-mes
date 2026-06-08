import React, { Component } from 'react';
import { Form, Input, TreeSelect, Select, Radio, Divider, Checkbox, Row, Col } from 'sup-ui';
import { SupI18nSelect } from 'sup-rc-i18n';
import { EmpowerBtn } from 'sup-rc-empower';
import { getRangeValue } from 'root/services/api.js';
import messages from 'root/common/messages';
import 'sup-rc-empower/dist/index.less';
import styles from './index.less';

const { TreeNode } = TreeSelect;
const { Option } = Select;

@Form.create()
class MenuForm extends Component {
  state = {
    rangeDisabled: null
  }

  // 初始化国际化
  onChangeInitValue = (value, key) => {
    const { setFieldsValue } = this.props.form;
    setFieldsValue({
      [key]: value
    });
  }

  // 菜单名称校验
  handleCheckName = (rule, value, callback) => {
    const { intl } = this.props;

    const hasValue = this.i18nName.getValidate({ required: true });
    const isInLength = this.i18nName.getValidate({ maxLength: 500 });

    if (!hasValue) {
      callback(intl.formatMessage(messages.enterMenuNameTip));
    } else if (!isInLength) {
      callback(intl.formatMessage(messages.maxLengthTip, { count: 500 }));
    } else {
      callback();
    }
  }

  // 修改所属模块时同步修改国际化模块
  handleChangeModule = async (value, node) => {
    const { setFieldsValue, getFieldValue } = this.props.form;
    const { i18nValue } = getFieldValue('name');
    const { moduleType, moduleId } = node.props;
    let companyIds = [];
    let rangeDisabled = false;

    setFieldsValue({
      moduleCode: node.props.moduleCode,
      name: {
        moduleCode: value,
        i18nValue
      }
    });
    // 业务模块请求适用范围
    if (moduleType !== 'SYSTEM') {
      await getRangeValue(moduleId).then((res = {}) => {
        companyIds = res.data;
        rangeDisabled = true;
      });
    }
    setFieldsValue({ companyIds });
    this.setState({ rangeDisabled });
  }

  render() {
    const { intl, data, optType, onChangeItem, companyName, companyId, moduleList } = this.props;
    const { getFieldValue, getFieldDecorator } = this.props.form;
    const { rangeDisabled } = this.state;

    const linkType = getFieldValue('showType') !== undefined ? getFieldValue('showType') : data.showType;

    const language = localStorage.getItem('language');
    const i18nParam = data.nameDisplay ? {
      i18nValue: {
        [language]: data.nameDisplay
      }
    } : {};

    const codeForm = (
      <Form.Item label={intl.formatMessage(messages.menuCode)}>
        {
          getFieldDecorator('code', {
            initialValue: data.code,
            rules: [
              {
                required: true,
                message: intl.formatMessage(messages.enterCodeTip)
              },
              {
                max: 200,
                message: intl.formatMessage(messages.maxLengthTip, { count: 200 })
              },
              {
                pattern: /^[A-Za-z0-9_]([A-Za-z0-9_.]*[A-Za-z0-9_]){0,200}?$/,
                message: intl.formatMessage(messages.codeRuleTip)
              }
            ]
          })(
            <Input
              size="small"
              disabled={optType === 'editMenu'}
            />
          )
        }
      </Form.Item>
    );

    const nameForm = (
      <Form.Item label={intl.formatMessage(messages.menuName)}>
        {
          getFieldDecorator('name', {
            initialValue: {
              moduleCode: data.app || 'rbac',
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
              size="small"
              callback={(value) => this.onChangeInitValue(value, 'name')}
            />
          )
        }
      </Form.Item>
    );

    const companyNameForm = (
      <Form.Item label={intl.formatMessage(messages.useCompany)}>
        {
          getFieldDecorator('cname', {
            initialValue: companyName
          })(
            <Input
              size="small"
              disabled
            />
          )
        }
      </Form.Item>
    );

    const urlInputForm = (
      <Form.Item label={window.menuSource === 'supplant' ? intl.formatMessage(messages.linkUrl) : ''}>
        {
          getFieldDecorator('url', {
            initialValue: data.url
          })(
            <Input
              size="small"
              placeholder={intl.formatMessage(messages.pleaseInputURL)}
            />
          )
        }
      </Form.Item>
    );

    return (
      <Form layout="vertical" className={styles.form}>
        {
          window.menuSource === 'supos' && (
            <Form.Item style={{ display: 'none' }}>
              {
                getFieldDecorator('status', {
                  initialValue: 1
                })(
                  <Input
                    disabled
                  />
                )
              }
            </Form.Item>
          )
        }
        <Form.Item style={{ display: 'none' }}>
          {
            getFieldDecorator('type', {
              initialValue: 'menu'
            })(
              <Input
                disabled
              />
            )
          }
        </Form.Item>
        <Form.Item style={{ display: 'none' }}>
          {
            getFieldDecorator('id', {
              initialValue: data.id
            })(
              <Input
                disabled
              />
            )
          }
        </Form.Item>
        <Form.Item style={{ display: 'none' }}>
          {
            getFieldDecorator('cid', {
              initialValue: companyId
            })(
              <Input
                disabled
              />
            )
          }
        </Form.Item>
        {
          window.menuSource === 'supplant' && (
            <>
              <Row gutter={16}>
                <Col span={12}>{codeForm}</Col>
                <Col span={12}>{nameForm}</Col>
              </Row>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item label={intl.formatMessage(messages.menuStyle)}>
                    {
                      getFieldDecorator('cssClass', {
                        initialValue: data.cssClass
                      })(
                        <Input size="small" />
                      )
                    }
                  </Form.Item>
                </Col>
                <Col span={12}>{companyNameForm}</Col>
              </Row>
            </>
          )
        }
        {
          window.menuSource !== 'supplant' && (
            <>
              {codeForm}
              {nameForm}
            </>
          )
        }
        <Form.Item label={intl.formatMessage(messages.openType)}>
          {
            getFieldDecorator('target', {
              initialValue: data.target || 'SELF'
            })(
              <Radio.Group size="small">
                <Radio value="SELF">{intl.formatMessage(messages.currentTab)}</Radio>
                <Radio value="BLANK">{intl.formatMessage(messages.newTab)}</Radio>
              </Radio.Group>
            )
          }
        </Form.Item>
        {
          window.menuSource === 'supplant' && (
            <Form.Item
              label={intl.formatMessage(messages.hide)}
              style={{
                display: 'flex',
                marginBottom: 0
              }}
            >
              {
                getFieldDecorator('isHide', {
                  initialValue: data.isHide,
                  valuePropName: 'checked'
                })(
                  <Checkbox
                    size="small"
                    style={{
                      marginLeft: 5
                    }}
                  />
                )
              }
            </Form.Item>
          )
        }
        {
          window.menuSource === 'supplant' ? urlInputForm : (
            <>
              <Form.Item label={intl.formatMessage(messages.showType)}>
                {
                  getFieldDecorator('showType', {
                    initialValue: data.showType
                  })(
                    <Radio.Group
                      size="small"
                      onChange={(e) => onChangeItem(e.target.value, 'showType')}
                    >
                      <Radio value={0}>{intl.formatMessage(messages.linkPage)}</Radio>
                      <Radio value={1}>{intl.formatMessage(messages.linkUrl)}</Radio>
                    </Radio.Group>
                  )
                }
              </Form.Item>
              {
                linkType === 0 && (
                  <Form.Item>
                    {
                      getFieldDecorator('url', {
                        initialValue: data.url
                      })(
                        <TreeSelect
                          size="small"
                          showSearch
                          placeholder={`-${intl.formatMessage(messages.pleaseSelect)}-`}
                        >
                          <TreeNode />
                        </TreeSelect>
                      )
                    }
                  </Form.Item>
                )
              }
              {
                linkType === 1 && urlInputForm
              }
              <Divider />
            </>
          )
        }
        {
          window.menuSource === 'supplant' && (
            <>
              <Form.Item style={{ display: 'none' }}>
                {
                  getFieldDecorator('moduleCode', {
                    initialValue: data.moduleCode
                  })(
                    <Input
                      disabled
                    />
                  )
                }
              </Form.Item>
              <Form.Item label={intl.formatMessage(messages.moduleCode)}>
                {
                  getFieldDecorator('app', {
                    initialValue: data.app,
                    rules: [
                      { required: true, message: intl.formatMessage(messages.enterModuleCodeTip) }
                    ]
                  })(
                    <Select
                      showSearch
                      size="small"
                      disabled={optType === 'editMenu'}
                      optionFilterProp="children"
                      placeholder={`-${intl.formatMessage(messages.pleaseSelect)}-`}
                      getPopupContainer={(trigger) => (trigger.parentNode)}
                      onChange={this.handleChangeModule}
                    >
                      {
                        moduleList && moduleList.length !== 0
                        && moduleList.map((item) => {
                          return (
                            <Option value={item.moduleId} moduleCode={item.moduleCode} moduleId={item.moduleId} moduleType={item.moduleType}>{item.moduleName}</Option>
                          );
                        })
                      }
                    </Select>
                  )
                }
              </Form.Item>
            </>
          )
        }
        <Form.Item label={intl.formatMessage(messages.useRange)}>
          {
            getFieldDecorator('companyIds', {
              initialValue: data.companyIds,
              rules: [
                {
                  required: true,
                  message: intl.formatMessage(messages.enterUseRangeTip)
                }
              ]
            })(
              <EmpowerBtn
                type="input"
                disabled={rangeDisabled || data.company_readOnly}
                placeholder={`-${intl.formatMessage(messages.pleaseSelect)}-`}
              />
            )
          }
        </Form.Item>
        {
          window.menuSource !== 'supplant' && companyNameForm
        }
        <Form.Item label={intl.formatMessage(messages.memo)}>
          {
            getFieldDecorator('memo', {
              initialValue: data.memo,
              rules: [
                {
                  max: 250,
                  message: intl.formatMessage(messages.maxLengthTip, { count: 250 })
                }
              ]
            })(
              <Input.TextArea size="small" />
            )
          }
        </Form.Item>
      </Form>
    );
  }
}

export default MenuForm;
