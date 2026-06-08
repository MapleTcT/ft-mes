import React from 'react';
import { Modal, Form, Input } from 'sup-ui';
import { injectIntl } from 'react-intl';

const customCmpMap = {
  text: Input,
  textarea: Input.TextArea,
  password: Input.Password
};

// modal props 所以modal属性
// onSubmit 提交事件
export default function createBaseForm(options) {
  const { formProps } = options;
  return (fields) => {
    class BaseFormModal extends React.Component {
      handleOk = () => {
        const { form } = this.props;
        const { onSubmit, onValidateErr } = this.props;

        form.validateFields().then(
          (data) => {
            if (onSubmit) {
              onSubmit(data, form);
            }
          },
          (err) => {
            console.error(err);
            if (onValidateErr) {
              onValidateErr(err);
            }
          }
        );
      };

      // 返回固定组件
      renderCustomCmp(type, formItemProps) {
        // TODO consider formItemProps is a function
        const Component = customCmpMap[type];
        return <Component {...formItemProps} />;
      }

      render() {
        const {
          modalProps,
          initData = {},
          form,
          intl,
          compData
        } = this.props;
        const { getFieldDecorator } = form;
        // FIXME optimize unnecessary execute
        const intlFields = fields({ fmt: intl.formatMessage });
        return (
          <Modal maskClosable={false} destroyOnClose {...modalProps} onOk={this.handleOk}>
            <Form>
              {intlFields.map((field) => {
                const {
                  key,
                  label,
                  renderFormItem,
                  rules = [],
                  type,
                  formItemProps = {}
                } = field;

                // consider func rule need form context
                const formatedRules = rules.map((rule) => {
                  return typeof rule === 'function' ? rule(form) : rule;
                });

                return (
                  <Form.Item label={label} key={key}>
                    {getFieldDecorator(key, {
                      // TODO extra decorator option
                      initialValue: initData[key],
                      rules: formatedRules
                    })(
                      renderFormItem
                        ? renderFormItem(compData)
                        : this.renderCustomCmp(type, formItemProps)
                    )}
                  </Form.Item>
                );
              })}
            </Form>
          </Modal>
        );
      }
    }

    return Form.create({ ...formProps })(injectIntl(BaseFormModal));
  };
}
