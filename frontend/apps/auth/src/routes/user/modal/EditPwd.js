import CreateBaseForm from 'root/components/FormModal/index.js';
import messages from '../messages';

const field = ({ fmt }) => {
  return [
    {
      label: fmt(messages.modalFieldUsername),
      key: 'userName',
      type: 'text',
      formItemProps: {
        disabled: true
      }
    },
    {
      label: fmt(messages.modalFieldNewPwd),
      key: 'password',
      type: 'password',
      rules: [
        {
          required: true,
          message: fmt(messages.modalFieldPwdRequired)
        }
      ]
    },
    {
      label: fmt(messages.modalFieldConfirmPwd),
      key: 'confirmPassword',
      type: 'password',
      rules: [
        {
          required: true,
          message: fmt(messages.modalFieldPwdRequired)
        },
        (form) => ({
          validator: (_, value, callback) => {
            if (value && value !== form.getFieldValue('password')) {
              callback(fmt(messages.modalFieldPwdNotMatch));
            } else {
              callback();
            }
          }
        })
      ]
    }
  ];
};

export default CreateBaseForm({
  formProps: { name: 'editUserPwdFormModal' },
  messages
})(field);
