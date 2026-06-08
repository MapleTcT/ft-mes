import React from 'react';
import { Select, Form, Modal, Input } from 'sup-ui';
import { injectIntl } from 'react-intl';
import { createI18nKey, i18nKeyExist } from 'root/services/intl';
import { LAN_KEY, LAN_VALUE, MODULE_CODE } from '../constant';
import style from '../style.less';
import messages from '../messages';

const { Option } = Select;
const I18NRE = /^[\w.]+$/g;

class IntlEditForm extends React.Component {
  intl(key, data) {
    const { intl } = this.props;
    return intl.formatMessage(messages[key], data);
  }

  handleSubmit = () => {
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

  handleLanKeyChange = (rule, value, cb) => {
    const { editIntl, form } = this.props;

    if (!editIntl[LAN_KEY]) {
      const { [MODULE_CODE]: moduleCode } = form.getFieldsValue([MODULE_CODE]);
      if (!moduleCode) {
        return cb(this.intl('chooseModule'));
      }
      const sk = `${moduleCode}.`;
      // 重置正则
      I18NRE.lastIndex = 0;
      if (value && value.length <= 255 && I18NRE.test(value)) {
        if (value.length <= sk.length || !value.startsWith(sk)) {
          cb(this.intl('keystartsWithModule', { startKey: sk }));
        } else {
          // 校验国际化是否存在
          i18nKeyExist({ i18n_key: value })
            .then((res) => {
              const { data } = res;
              if (data.length) {
                cb(this.intl('keyExisted'));
              } else {
                cb();
              }
            })
            .catch(() => {
              // FIXME 后台异常提示
              cb();
            });
        }
      } else {
        // 使用组件自带规则
        cb();
      }
    } else {
      // 修改状态不校验
      cb();
    }
  };

  handleModuleChange = (moduleCode) => {
    const { form } = this.props;
    createI18nKey({
      moduleCode
    }).then((res) => {
      const {
        data: {
          list: [lanKey]
        }
      } = res;
      form.setFieldsValue({
        [LAN_KEY]: lanKey
      });
    });
  };

  render() {
    const { modules, lanList, editIntl = {}, modalProps, form } = this.props;
    let intlValues = {};
    if (editIntl[LAN_VALUE]) {
      intlValues = editIntl[LAN_VALUE];
    }

    const { getFieldDecorator } = form;
    return (
      <Modal
        maskClosable={false}
        width={460}
        wrapClassName={style.editModal}
        title={
          editIntl[LAN_KEY]
            ? this.intl('editI18nModalTitle')
            : this.intl('addI18nModalTitle')
        }
        destroyOnClose
        {...modalProps}
        onOk={this.handleSubmit}
      >
        <Form>
          <Form.Item label={this.intl('belongModule')} key={MODULE_CODE}>
            {getFieldDecorator(MODULE_CODE, {
              initialValue: editIntl[MODULE_CODE],
              rules: [{ required: true, message: this.intl('chooseModule') }]
            })(
              <Select
                disabled={!!editIntl[MODULE_CODE]}
                onChange={this.handleModuleChange}
              >
                {modules.map((m) => {
                  return (
                    <Option value={m.moduleCode} key={m.moduleCode}>
                      {m.moduleName}
                    </Option>
                  );
                })}
              </Select>
            )}
          </Form.Item>

          <Form.Item label={this.intl('i18nKey')} key={LAN_KEY}>
            {getFieldDecorator(LAN_KEY, {
              initialValue: editIntl[LAN_KEY],
              rules: [
                { required: true, message: this.intl('i18nKeyRequired') },
                { max: 255, message: this.intl('i18nKeyMaxLength') },
                {
                  pattern: I18NRE,
                  message: this.intl('i18nKeyPattern')
                },
                {
                  validator: this.handleLanKeyChange
                }
              ]
            })(
              <Input
                onChange={this.handleMainLanKeyChange}
                disabled={!!editIntl[LAN_KEY]}
              />
            )}
          </Form.Item>

          {lanList.map((lan) => {
            const key = lan.languCode;
            const label = lan.languType;
            return (
              <Form.Item label={label} key={key}>
                {getFieldDecorator(`${LAN_VALUE}.${key}`, {
                  initialValue: intlValues[key] || '',
                  rules: [
                    {
                      max: 500,
                      message: this.intl('i18nValueMaxLength', { label })
                    }
                  ]
                })(<Input />)}
              </Form.Item>
            );
          })}
        </Form>
      </Modal>
    );
  }
}

const IntlForm = injectIntl(IntlEditForm);

export default Form.create({ name: 'editUserFormModal' })(IntlForm);
