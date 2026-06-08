import React from 'react';
import { Form, Input, Checkbox, Select } from 'sup-ui';
import { injectIntl } from 'react-intl';
import { SupReference } from 'sup-rc-reference';
import messages from './messages';
import { getCurrentCompanyId } from '../../utils';

const companyId = getCurrentCompanyId();
const { Option } = Select;
class EditForm extends React.Component {
  constructor(props) {
    super(props);
    const { intl, row } = this.props;
    this.state = {
      checked: row.signatureEnabled,
      signTypeValue: 'singleSign',
      authTypeValue: 'staff',
      selectTypeClick: false,
      supReferenceTitle: row.powerType === 'role' ? `${intl.formatMessage(messages.role)}`
        : row.powerType === 'position' ? `${intl.formatMessage(messages.post)}`
          : `${intl.formatMessage(messages.person)}`,
      supReferenceType: row.powerType || 'staff',
      supReferenceItem: row.positions || row.persons || row.roles || [],
      selectTypeMsg: row.powerType === 'position' ? `${intl.formatMessage(messages.pleaseSelect)}${intl.formatMessage(messages.post)}`
        : row.powerType === 'role' ? `${intl.formatMessage(messages.pleaseSelect)}${intl.formatMessage(messages.role)}`
          : `${intl.formatMessage(messages.pleaseSelect)}${intl.formatMessage(messages.person)}`,
      selectAuthTypeClick: false
    };
  }

  handleChange = (params) => {
    this.setState({
      checked: params.target.checked
    });
  }

  handleCutSignType = (value) => {
    this.setState({
      selectTypeClick: true,
      signTypeValue: value
    });
  }

  handleCutAuthType = (value) => {
    const { intl, row } = this.props;
    if (value === 'staff') {
      this.setState({
        supReferenceTitle: `${intl.formatMessage(messages.person)}`,
        selectTypeMsg: `${intl.formatMessage(messages.pleaseSelect)}${intl.formatMessage(messages.person)}`,
        supReferenceType: 'staff'
      }, () => {
        this.referenceRef.handelChange((row.signerNames || []).map((item) => ({ name: item })));
      });
    } else if (value === 'position') {
      this.setState({
        supReferenceTitle: `${intl.formatMessage(messages.post)}`,
        selectTypeMsg: `${intl.formatMessage(messages.pleaseSelect)}${intl.formatMessage(messages.post)}`,
        supReferenceType: 'position'
      }, () => {
        this.referenceRef.handelChange((row.positionNames || []).map((item) => ({ name: item })));
      });
    } else if (value === 'role') {
      this.setState({
        supReferenceTitle: `${intl.formatMessage(messages.role)}`,
        selectTypeMsg: `${intl.formatMessage(messages.pleaseSelect)}${intl.formatMessage(messages.role)}`,
        supReferenceType: 'role'
      }, () => {
        this.referenceRef.handelChange((row.roleNames || []).map((item) => ({ name: item })));
      });
    }
    this.setState({
      selectAuthTypeClick: true,
      authTypeValue: value
    });
  }

  render() {
    const {
      intl,
      form,
      row,
      isEdit
    } = this.props;
    const { checked, signTypeValue, authTypeValue, selectTypeClick, selectAuthTypeClick, supReferenceTitle, supReferenceType, supReferenceItem, selectTypeMsg } = this.state;
    const { getFieldDecorator } = form;
    return (
      <>
        <Form>
          <Form.Item label={intl.formatMessage(messages.btnName)} colon={false}>
            {getFieldDecorator('buttonName', { initialValue: row.name })(<Input disabled={!!isEdit} />)}
          </Form.Item>
          <Form.Item>
            <span style={{ marginRight: '10px', color: '#7f8fa4' }}>
              {intl.formatMessage(messages.enableSigning)}
            </span>
            {
              getFieldDecorator('signatureEnabled', { initialValue: checked })(
                <Checkbox
                  defaultChecked={checked}
                  onChange={this.handleChange}
                  style={{ borderRadius: '8px' }}
                />
              )
            }
          </Form.Item>
          {
            checked ? (
              <div>
                <Form.Item label={intl.formatMessage(messages.signType)} style={{ display: checked ? 'block' : 'none' }} colon={false}>
                  {
                    getFieldDecorator('signatureType', {
                      initialValue: row.signatureType || signTypeValue,
                      rules: [
                        {
                          required: true,
                          message: `${intl.formatMessage(messages.pleaseSelect)}${intl.formatMessage(messages.signType)}`
                        }
                      ]
                    })(
                      <Select onChange={this.handleCutSignType}>
                        <Option value="singleSign">
                          {intl.formatMessage(messages.singleSign)}
                        </Option>
                        <Option value="doubleSign">
                          {intl.formatMessage(messages.doubleSign)}
                        </Option>
                      </Select>
                    )
                  }
                </Form.Item>
                {
                  (selectTypeClick ? signTypeValue === 'doubleSign' : row.signatureType === 'doubleSign') ? (
                    <div style={{ display: checked ? 'block' : 'none' }}>
                      <Form.Item label={intl.formatMessage(messages.authType)} colon={false}>
                        {
                          getFieldDecorator('powerType', {
                            initialValue: row.powerType || authTypeValue,
                            rules: [
                              {
                                required: true,
                                message: `${intl.formatMessage(messages.pleaseSelect)}${intl.formatMessage(messages.authType)}`
                              }
                            ]
                          })(
                            <Select onChange={this.handleCutAuthType}>
                              <Option value="staff">
                                {intl.formatMessage(messages.person)}
                              </Option>
                              <Option value="position">
                                {intl.formatMessage(messages.post)}
                              </Option>
                              <Option value="role">
                                {intl.formatMessage(messages.role)}
                              </Option>
                            </Select>
                          )
                        }
                      </Form.Item>
                      <Form.Item
                        label={
                          row.powerType ? (
                            (selectAuthTypeClick ? authTypeValue === 'staff' : row.powerType === 'staff') ? intl.formatMessage(messages.manager)
                              : (selectAuthTypeClick ? authTypeValue === 'position' : row.powerType === 'position') ? intl.formatMessage(messages.post)
                                : intl.formatMessage(messages.role)
                          ) : (
                            authTypeValue === 'staff' ? intl.formatMessage(messages.manager)
                              : authTypeValue === 'position' ? intl.formatMessage(messages.post)
                                : intl.formatMessage(messages.role)
                          )
                        }
                        colon={false}
                      >
                        {
                          getFieldDecorator('selectType', {
                            initialValue: supReferenceItem.map((item) => ({
                              name: item.name,
                              code: item.code,
                              id: item.id
                            })) || [],
                            rules: [
                              {
                                required: true,
                                message: `${selectTypeMsg}`
                              }
                            ]
                          })(
                            <SupReference
                              multiple
                              ref={(res) => { this.referenceRef = res; }}
                              referenceView={{
                                title: supReferenceTitle,
                                type: `${supReferenceType}`,
                                companyConfig: {
                                  disabled: true,
                                  parentId: companyId
                                }
                              }}
                            />
                          )
                        }
                      </Form.Item>
                    </div>
                  ) : null
                }
                <Form.Item label={intl.formatMessage(messages.describle)} style={{ display: checked ? 'block' : 'none' }} colon={false}>
                  {getFieldDecorator('signatureDescrible', {
                    initialValue: row.signatureDescrible,
                    rules: [
                      {
                        max: 255,
                        message: intl.formatMessage(messages.desLength)
                      }
                    ]
                  })(<Input.TextArea style={{ height: '81px', resize: 'none' }} />)}
                </Form.Item>
              </div>
            ) : null
          }
        </Form>
      </>
    );
  }
}

const WrappedEditForm = Form.create({ name: 'editForm' })(injectIntl(EditForm));

export default WrappedEditForm;
