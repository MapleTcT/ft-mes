import { injectIntl } from 'react-intl';
import BaseModal from '../component/BaseModal';
import Form from './Form';
import { PROPERTYTYPE, REFVIEW_FIELD } from '../constant';
// const { DECIMAL, OBJECT, SYSTEMCODE } = PROPERTY_TYPE;

class ModelManageModal extends BaseModal {
  getFormComp() {
    return Form;
  }

  prepareFormData(formData) {
    const i18nData = this.getI18nValue(formData);

    delete formData[PROPERTYTYPE];
    delete formData[REFVIEW_FIELD];

    if (formData.fillContent) {
      // 系统编码拼接固定字段
      formData.fillContent = {
        ...formData.fillContent,
        fillType: '3',
        fillName: '系统编码'
      };
    }

    const data = {
      ...formData,
      ...i18nData
    };

    return data;
  }
}

export default injectIntl(ModelManageModal);
