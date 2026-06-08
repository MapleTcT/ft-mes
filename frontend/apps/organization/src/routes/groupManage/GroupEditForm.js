import React from 'react';
import { Form, Input } from 'sup-ui';
import { injectIntl } from 'react-intl';
import { SupReference } from 'sup-rc-reference';
import messages from './messages';

class GroupEditForm extends React.Component {
  handleSubmit = () => {};

  render() {
    const { intl, initialValueObj = {}, form, isEdit } = this.props;
    const { getFieldDecorator } = form;
    const { name, code, managers = [], description } = initialValueObj;

    return (
      <Form>
        <Form.Item label={intl.formatMessage(messages.groupEditFormFieldName)}>
          {getFieldDecorator('name', {
            initialValue: name,
            rules: [
              {
                required: true,
                message: intl.formatMessage(
                  messages.groupEditFormFieldNameRequired
                )
              },
              {
                max: 50,
                message: intl.formatMessage(
                  messages.groupEditFormFieldNameMaxLength
                )
              }
            ]
          })(<Input />)}
        </Form.Item>
        <Form.Item label={intl.formatMessage(messages.groupEditFormFieldCode)}>
          {getFieldDecorator('code', {
            rules: [
              {
                required: true,
                message: intl.formatMessage(
                  messages.groupEditFormFieldCodeRequired
                )
              },
              {
                max: 50,
                message: intl.formatMessage(
                  messages.groupEditFormFieldCodeMaxLength
                )
              }
            ],
            initialValue: code
          })(<Input disabled={!!isEdit} />)}
        </Form.Item>
        <Form.Item
          label={intl.formatMessage(messages.groupEditFormFieldManagerName)}
        >
          {getFieldDecorator('managerPerson', {
            initialValue: managers.map((d) => ({
              id: d.managerId,
              name: d.managerName
            }))
          })(
            <SupReference
              multiple
              bindKey="id"
              referenceView={{
                title: intl.formatMessage(
                  messages.groupEditFormFieldManagerName
                ),
                type: 'staff'
              }}
            />
          )}
        </Form.Item>

        <Form.Item
          label={intl.formatMessage(messages.groupEditFormFieldDescription)}
        >
          {getFieldDecorator('description', {
            rules: [
              {
                max: 255,
                message: intl.formatMessage(
                  messages.groupEditFormFieldDescMaxLength
                )
              }
            ],
            initialValue: description
          })(<Input.TextArea rows={4} />)}
        </Form.Item>
      </Form>
    );
  }
}

const WrappedGroupEditForm = Form.create({ name: 'groupEditForm' })(
  injectIntl(GroupEditForm)
);

export default WrappedGroupEditForm;
