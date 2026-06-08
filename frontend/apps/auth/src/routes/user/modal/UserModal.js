import React from 'react';
import { Select, Radio, Modal, Form, Input } from 'sup-ui';
import { injectIntl } from 'react-intl';
import { SupReference } from 'sup-rc-reference';
import messages from '../messages';

const { Option } = Select;

const USER_TYPE = {
  normal: 0,
  admin: 1
};

class UserFormModal extends React.Component {
  state = {
    userType: USER_TYPE.normal
  };

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

  handleUserTypeChange = (e) => {
    const {
      target: { value: userType }
    } = e;
    this.setState({
      userType
    });
  };

  // 关闭对话框重置用户类型
  resetUserType = () => {
    this.setState({
      userType: USER_TYPE.normal
    });
  };

  render() {
    const {
      modalProps,
      initData = {},
      form,
      intl,
      isEdit,
      currentCompanyId
    } = this.props;
    const { getFieldDecorator } = form;
    const fmt = intl.formatMessage;
    let { userType } = this.state;
    // 编辑用户使用固定类型
    if (isEdit) {
      userType = initData.userType;
    }

    return (
      <Modal
        maskClosable={false}
        destroyOnClose
        {...modalProps}
        onOk={this.handleOk}
        afterClose={this.resetUserType}
      >
        <Form>
          <Form.Item
            style={{ display: isEdit ? 'none' : '' }}
            label={fmt(messages.modalFieldRoleType)}
            key="userType"
          >
            {getFieldDecorator('userType', {
              initialValue: userType
            })(
              <Radio.Group name="userType" onChange={this.handleUserTypeChange}>
                <Radio value={0}>{fmt(messages.userTypeNormal)}</Radio>
                <Radio value={1}>{fmt(messages.userTypeAdmin)}</Radio>
              </Radio.Group>
            )}
          </Form.Item>

          <Form.Item label={fmt(messages.modalFieldUsername)} key="userName">
            {getFieldDecorator('userName', {
              initialValue: initData.userName,
              rules: [
                {
                  required: true,
                  message: fmt(messages.modalFieldUsernameRequired)
                },
                {
                  pattern: /^[\w]+$/,
                  message: fmt(messages.modalFieldUsernamePattern)
                },
                {
                  max: 50,
                  message: fmt(messages.modalFieldUsernameMaxLength)
                }
              ]
            })(<Input disabled={isEdit} maxLength={50} />)}
          </Form.Item>

          {isEdit ? null : (
            <Form.Item label={fmt(messages.modalFieldPwd)} key="password">
              {getFieldDecorator('password', {
                initialValue: initData.password,
                rules: [
                  {
                    required: true,
                    message: fmt(messages.modalFieldPwdRequired)
                  }
                ]
              })(<Input.Password autocomplete="new-password" />)}
            </Form.Item>
          )}

          {userType === USER_TYPE.normal ? (
            <Form.Item label={fmt(messages.modalFieldStaffname)} key="staff">
              {getFieldDecorator('staff', {
                initialValue: initData.staff,
                rules: [
                  {
                    required: true,
                    message: fmt(messages.modalFieldStaffnameRequired)
                  }
                ]
              })(
                <SupReference
                  disabled={isEdit}
                  referenceView={{
                    title: fmt(messages.modalFieldStaffname),
                    type: 'staff',
                    companyConfig: {
                      parentId: currentCompanyId,
                      disabled: true
                    }
                  }}
                />
              )}
            </Form.Item>
          ) : null}

          {userType === USER_TYPE.normal ? (
            <Form.Item label={fmt(messages.modalFieldRole)} key="role">
              {getFieldDecorator('role', {
                initialValue: initData.role
              })(
                <SupReference
                  bindKey="id"
                  multiple
                  referenceView={{
                    title: fmt(messages.modalFieldRole),
                    type: 'role',
                    companyConfig: {
                      parentId: currentCompanyId
                    }
                  }}
                />
              )}
            </Form.Item>
          ) : null}

          <Form.Item label={fmt(messages.modalFieldTimezone)} key="timeZone">
            {getFieldDecorator('timeZone', {
              initialValue: initData.timeZone
            })(
              <Select>
                <Option value="CST+08:00">
                  (UTC +08:00)北京，重庆，香港特别行政区，乌鲁木齐
                </Option>
                <Option value="JST+09:00">(UTC +09:00)大阪，札幌，东京</Option>
                <Option value="PST-08:00">
                  (UTC -05:00)太平洋时间（美国和加拿大）
                </Option>
              </Select>
            )}
          </Form.Item>

          <Form.Item label={fmt(messages.modalFieldDesc)} key="description">
            {getFieldDecorator('description', {
              initialValue: initData.description,
              rules: [
                {
                  max: 255,
                  message: fmt(messages.modalFieldDescMaxLength)
                }
              ]
            })(<Input.TextArea rows={3} maxLength={255} />)}
          </Form.Item>
        </Form>
      </Modal>
    );
  }
}

export default Form.create({ name: 'userFormModal' })(
  injectIntl(UserFormModal)
);
