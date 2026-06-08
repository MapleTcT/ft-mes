import React from 'react';
import { Form, Input } from 'sup-ui';
import { injectIntl } from 'react-intl';
import { SupReference } from 'sup-rc-reference';
import { Select } from 'sup-rc-syscode';
import messages from './messages';
import style from './style.less';

class EditForm extends React.Component {
  handleSubmit = () => {};

  render() {
    const {
      intl,
      initialValueObj = {},
      form,
      isEdit,
      modalEditForm,
      modalDepartmentEditForm,
      companyId
    } = this.props;
    const { getFieldDecorator } = form;
    const { name, code, managers = [], description, depName, depId } = initialValueObj;
    return (
      <>
        {modalDepartmentEditForm || modalEditForm ? (
          <Form>
            <Form.Item
              label={intl.formatMessage(messages.departmentName)}
              colon={false}
            >
              {getFieldDecorator('name', {
                initialValue: name,
                rules: [
                  {
                    required: true,
                    message: intl.formatMessage(
                      messages.departmentEditFormName,
                    )
                  },
                  {
                    max: 200,
                    message: intl.formatMessage(
                      messages.departmentEditFormNameMaxLength,
                    )
                  }
                ]
              })(<Input />)}
            </Form.Item>
            <Form.Item label={intl.formatMessage(messages.code)} colon={false}>
              {getFieldDecorator('code', {
                rules: [
                  {
                    required: true,
                    message: intl.formatMessage(messages.editFormCodeRequired)
                  },
                  {
                    max: 50,
                    message: intl.formatMessage(messages.editFormCodeMaxLength)
                  }
                ],
                initialValue: code
              })(<Input disabled={!!isEdit} />)}
            </Form.Item>
            <Form.Item label={intl.formatMessage(messages.modl)} colon={false}>
              {getFieldDecorator('type', {
                initialValue: initialValueObj.type,
                rules: [
                  {
                    required: true,
                    message: intl.formatMessage(messages.editFormModl)
                  }
                ]
              })(
                <Select entityCode="sys_department_type">
                  {/* {this.getCodeValueList()} */}
                </Select>,
              )}
            </Form.Item>
            <Form.Item label={intl.formatMessage(messages.managerName)} colon={false}>
              {getFieldDecorator('managers', {
                initialValue: managers.map((d) => ({
                  id: d.managerId,
                  name: d.managerName,
                  code: d.managerCode
                }))
              })(
                <SupReference
                  multiple
                  referenceView={{
                    title: intl.formatMessage(messages.managerName),
                    type: 'staff',
                    companyConfig: {
                      disabled: true,
                      parentId: Number(companyId)
                    }
                  }}
                />,
              )}
            </Form.Item>

            <Form.Item label={intl.formatMessage(messages.description)} colon={false}>
              {getFieldDecorator('description', {
                rules: [
                  {
                    max: 500,
                    message: intl.formatMessage(messages.editFormDescMaxLength)
                  }
                ],
                initialValue: description
              })(<Input.TextArea className={style.textarea} />)}
            </Form.Item>
          </Form>
        ) : (
          <Form>
            <Form.Item label={intl.formatMessage(messages.PositionName)} colon={false}>
              {getFieldDecorator('name', {
                initialValue: name,
                rules: [
                  {
                    required: true,
                    message: intl.formatMessage(messages.positionEditFormName)
                  },
                  {
                    max: 200,
                    message: intl.formatMessage(
                      messages.positionEditFormNameMaxLenth,
                    )
                  }
                ]
              })(<Input />)}
            </Form.Item>
            <Form.Item label={intl.formatMessage(messages.code)} colon={false}>
              {getFieldDecorator('code', {
                rules: [
                  {
                    required: true,
                    message: intl.formatMessage(messages.editFormCodeRequired)
                  },
                  {
                    max: 50,
                    message: intl.formatMessage(messages.editFormCodeMaxLength)
                  }
                ],
                initialValue: code
              })(<Input disabled={!!isEdit} />)}
            </Form.Item>
            <Form.Item
              label={intl.formatMessage(messages.editFormRelatedDepartment)}
              colon={false}
            >
              {getFieldDecorator('relatedDepartment', {
                rules: [
                  {
                    required: true,
                    message: intl.formatMessage(messages.chooseRelatedDepartment)
                  }
                ],
                initialValue: depId ? [{
                  id: depId,
                  name: depName
                }] : []
              })(
                <SupReference
                  // multiple
                  companyConfig={{ disabled: true }}
                  referenceView={{
                    title: intl.formatMessage(
                      messages.editFormRelatedDepartment,
                    ),
                    type: 'department',
                    companyConfig: {
                      disabled: true,
                      parentId: Number(companyId)
                    }
                  }}
                />,
              )}
            </Form.Item>
            <Form.Item label={intl.formatMessage(messages.description)} colon={false}>
              {getFieldDecorator('description', {
                rules: [
                  {
                    max: 500,
                    message: intl.formatMessage(messages.editFormDescMaxLength)
                  }
                ],
                initialValue: description
              })(<Input.TextArea className={style.textarea} />)}
            </Form.Item>
          </Form>
        )}
      </>
    );
  }
}

const WrappedEditForm = Form.create({ name: 'editForm' })(injectIntl(EditForm));

export default WrappedEditForm;
