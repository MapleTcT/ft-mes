import { injectIntl } from 'react-intl';
import BaseModal from '../component/BaseModal';
import Form from './Form';

class ViewManageModal extends BaseModal {
  getFormComp() {
    return Form;
  }

  prepareFormData(formData) {
    const i18nData = this.getI18nValue(formData);

    const {
      associatedProperty,
      fieldType,
      format,
      nullable,
      precision,
      multable,
      property,
      showCustom,
      colspan,
      associatedCode,
      readonly,
      propertyLayRec,
      align,
      textareaRow
    } = formData;

    const { type: propertyType } = property;

    const data = {
      associatedProperty,
      propertyLayRec,
      fieldType,
      format,
      align,
      nullable,
      property,
      showCustom,
      colspan,
      readonly,
      associatedCode,
      textareaRow,
      ...i18nData
    };

    if (propertyType === 'DECIMAL') {
      // 小数位数
      data.precision = precision;
    } else if (propertyType === 'SYSTEMCODE') {
      // 系统编码多选
      data.multable = multable;
    }
    return data;
  }
}

export default injectIntl(ViewManageModal);
