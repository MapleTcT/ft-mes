// import React from 'react';
// import { Select } from 'sup-ui';
import CreateBaseForm from 'root/components/FormModal/index.js';
import messages from '../messages';

const field = ({ fmt }) => {
  return [
    {
      label: fmt(messages.modalFieldName),
      key: 'name',
      type: 'text',
      rules: [
        {
          required: true,
          message: fmt(messages.modalFieldNameRequired)
        },
        {
          max: 50,
          message: fmt(messages.modalFieldNameMaxLength)
        }
      ]
    },
    {
      label: fmt(messages.modalFieldCode),
      key: 'code',
      type: 'text',
      formItemProps: {
        disabled: true
      },
      rules: [
        {
          required: true,
          message: fmt(messages.modalFieldCodeRequired)
        }
      ]
    },
    // {
    //   label: fmt(messages.modalFieldTag),
    //   key: 'tags',
    //   renderFormItem: ({ tags }) => {
    //     return (
    //       <Select mode="tags" style={{ width: '100%' }}>
    //         {tags.map((item) => {
    //           return <Select.Option key={item}>{item}</Select.Option>;
    //         })}
    //       </Select>
    //     );
    //   }
    // },
    {
      label: fmt(messages.modalFieldDesc),
      key: 'description',
      type: 'textarea',
      formItemProps: {
        rows: 3
      },
      rules: [
        {
          max: 255,
          message: fmt(messages.modalFieldDescMaxLength)
        }
      ]
    }
  ];
};

export default CreateBaseForm({
  formProps: { name: 'editRoleFormModal' },
  messages
})(field);
