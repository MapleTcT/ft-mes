import React from 'react';
import { injectIntl } from 'react-intl';
import { Layout, message, Modal } from 'sup-ui';
import {
  fetchIntlLans,
  fetchModuleList,
  removeIntl,
  addIntl,
  updateIntl,
  updateIntlLans,
  getAuthority
} from 'root/services/intl';
import IntlSettingModal from './modal/Setting';

import IntlList from './List';
import IntlEditModal from './modal/Edit';
import { LAN_KEY, LAN_USED } from './constant';
import {
  clone,
  extractEditLan,
  autoCreateI18Keys,
  trimI18NValues
} from './utils';
import style from './style.less';
import messages from './messages';
// autoCreateI18Keys(100);

const { Content, Header } = Layout;

class IntlManage extends React.Component {
  constructor(props) {
    super(props);
    this.intlTable = React.createRef();
  }

  state = {
    editIntl: {},
    editModalVisible: false,
    settingModalVisible: false,
    lanList: [],
    editLanList: [],
    modules: [],
    btnAuth: []
  };

  intl(key, data) {
    const { intl } = this.props;
    return intl.formatMessage(messages[key], data);
  }

  // 获取国际化语言
  refreshIntlLans() {
    fetchIntlLans().then((res) => {
      const {
        data: { list }
      } = res;
      this.setState({
        lanList: list
      });
    });
  }

  hasAuth = (code) => {
    const { btnAuth } = this.state;
    if (!code) return btnAuth;
    if (typeof code === 'string') {
      return btnAuth.includes(code);
    }
    return code.some((d) => btnAuth.includes(d));
  };

  componentDidMount() {
    getAuthority('i18n').then(({ data: { list } }) => {
      this.setState({
        btnAuth: list
      });
      this.refreshIntlLans();
    });
  }

  handleUpdateIntlLanSetting = () => {
    const hasChecked = this.hasCheckedLan();
    const { editLanList } = this.state;
    if (hasChecked) {
      const submitLans = extractEditLan(editLanList);
      updateIntlLans(submitLans).then(() => {
        message.success(this.intl('languageUpdateSuccess'));
        // 考虑重新刷新国际化语言
        this.setState({
          lanList: editLanList
        });
        this.hideSettingModal();
        this.intlTable.current.refreshIntlList();
      });
    } else {
      message.info(this.intl('enableOneLanguageMsg'));
    }
  };

  hasCheckedLan() {
    const { editLanList } = this.state;
    const lan = editLanList.find((d) => d[LAN_USED]);
    return !!lan;
  }

  // eslint-disable-next-line camelcase
  handleSwitchChange = (checked, { languCode: lanCode }) => {
    const { editLanList } = this.state;

    const nextLanList = editLanList.map((d) => {
      if (d.languCode === lanCode) {
        d[LAN_USED] = checked;
      }
      return d;
    });

    this.setState({
      editLanList: nextLanList
    });
  };

  getIntlSettingModalProps = () => {
    const { settingModalVisible, editLanList } = this.state;
    return {
      modalProps: {
        visible: settingModalVisible,
        onCancel: this.hideSettingModal,
        onOk: this.handleUpdateIntlLanSetting
      },
      lanList: editLanList,
      handleSwitchChange: this.handleSwitchChange
    };
  };

  getIntlEditModalProps = () => {
    const { editModalVisible, editIntl, modules, lanList } = this.state;

    return {
      modalProps: {
        visible: editModalVisible,
        onCancel: this.hideEditModal
      },
      onSubmit: this.handleSubmitEdit,
      editIntl,
      modules,
      lanList: lanList.filter((d) => d.used)
    };
  };

  handleUpdateSuccess = () => {
    this.hideEditModal();
    this.intlTable.current.refreshIntlList();
  };

  // 国际化修改提交
  handleSubmitEdit = ({ i18nKey, i18nValues, moduleCode }) => {
    const data = {
      i18n_key: i18nKey,
      i18n_value: trimI18NValues(i18nValues), // 去除国际化值头尾空格
      moduleCode
    };
    const { editIntl } = this.state;
    if (editIntl[LAN_KEY]) {
      updateIntl(data).then(() => {
        message.success(this.intl('editIntlSuccess'));
        this.handleUpdateSuccess();
      });
    } else {
      addIntl(data).then(() => {
        message.success(this.intl('createIntlSuccess'));
        this.handleUpdateSuccess();
      });
    }
    // 新增
  };

  hideEditModal = () => {
    this.setState({
      editModalVisible: false
    });
  };

  hideSettingModal = () => {
    this.setState({
      settingModalVisible: false
    });
  };

  showEditModal = (editIntl = {}) => {
    fetchModuleList().then((res) => {
      const {
        data: { list }
      } = res;
      this.setState({
        editModalVisible: true,
        modules: list,
        editIntl
      });
    });
  };

  handleAddIntl = () => {
    this.showEditModal();
  };

  handleEditIntl = (editIntl) => {
    this.showEditModal(editIntl);
  };

  handleIntlSetting = () => {
    const { lanList } = this.state;
    this.setState({
      settingModalVisible: true,
      editLanList: clone(lanList)
    });
  };

  // eslint-disable-next-line camelcase
  handleRemoveIntl = ({ i18nKey }, cb) => {
    Modal.confirm({
      title: this.intl('removeModalTitle'),
      content: this.intl('removeModalContent'),
      onOk: () => {
        removeIntl(i18nKey).then(() => {
          message.success(this.intl('removeIntlSuccess'));
          if (cb) {
            cb();
          }
        });
      }
    });
  };

  render() {
    const { lanList } = this.state;

    return (
      <Layout className={style.layout}>
        <Header className={style.topHeader}>
          {this.intl('intlSettingHeader')}
        </Header>
        <Content>
          {/* 语言设置 */}
          <IntlSettingModal {...this.getIntlSettingModalProps()} />
          {/* 国际化新增编辑 */}
          <IntlEditModal {...this.getIntlEditModalProps()} />
          {/* 国际化列表 */}
          {lanList.length ? (
            <IntlList
              hasAuth={this.hasAuth}
              ref={this.intlTable}
              lanList={lanList}
              handleIntlSetting={this.handleIntlSetting}
              handleAddIntl={this.handleAddIntl}
              handleEditIntl={this.handleEditIntl}
              handleRemoveIntl={this.handleRemoveIntl}
            />
          ) : (
            <div
              style={{
                minHeight: 220,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#8c9cad',
                fontSize: 14
              }}
            >
              暂无数据
            </div>
          )}
        </Content>
      </Layout>
    );
  }
}

export default injectIntl(IntlManage);
