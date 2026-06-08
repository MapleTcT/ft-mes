import React from 'react';
import { Modal } from 'sup-ui';
import { getI18nLanguage } from '../utils';
import style from '../style.less';
import { REFVIEW_FIELD, REFVIEW_CODE } from '../constant';

export default class BaseManageModal extends React.Component {
  state = {
    okButtonProps: {}
  };

  getFormData() {
    const { formData } = this.props;
    const viewCode = formData[REFVIEW_CODE];
    formData[REFVIEW_FIELD] = [
      {
        code: viewCode,
        name: '',
        isFake: true
      }
    ];

    return formData;
  }

  render() {
    const { modalProps } = this.props;
    const { okButtonProps } = this.state;
    const Form = this.getFormComp();

    return (
      <Modal
        className={style.modal}
        width={600}
        destroyOnClose
        maskClosable={false}
        {...modalProps}
        onOk={this.handleOk}
        okButtonProps={okButtonProps}
        afterClose={() => {
          this.toggleOkBtn(false);
        }}
      >
        <Form
          ref={(ref) => {
            this.form = ref;
          }}
          formData={this.getFormData()}
        />
      </Modal>
    );
  }

  toggleOkBtn = (disabled) => {
    this.setState((d) => {
      d.okButtonProps.disabled = disabled;
      return d;
    });
  };

  getI18nValue(formData) {
    const { i18nKey, i18nValue } = formData.i18n;
    return {
      displayName: i18nKey,
      displayNameInternational: i18nValue[getI18nLanguage()]
    };
  }

  saveI18nValue(formData, success, err) {
    const { i18nKey, i18nValue, moduleCode } = formData.i18n;
    this.form.instances.i18n.onSave(
      {
        moduleCode,
        i18n_key: i18nKey,
        i18n_value: i18nValue
      },
      success,
      err // FIXME 此回调暂不支持
    );
  }

  handleOk = () => {
    this.toggleOkBtn(true);
    this.form.validateFields((err, formData) => {
      if (err) {
        return this.toggleOkBtn(false);
      }

      // 先保存国际化
      // 再保存表单数据
      this.saveI18nValue(
        formData,
        () => {
          const formatedData = this.prepareFormData(formData);
          this.props.handleSaveForm(formatedData).catch(() => {
            this.toggleOkBtn(false);
          });
        },
        () => {
          this.toggleOkBtn(false);
        }
      );
    });
  };
}
