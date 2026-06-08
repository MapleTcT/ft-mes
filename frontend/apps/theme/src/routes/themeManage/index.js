import React from 'react';
import { Button, message, Modal } from 'sup-ui';
import { injectIntl } from 'react-intl';
// import { getSystemThemes } from "../../services/demoApi";
import {
  getSystemThemes,
  updateSystemTheme
} from '../../services/themeSetting';
import commonMessage from '../../components/themeOption/message.js';
import selfMessage from './message.js';
import { DELAYTIME } from '../../utils/constant.js';
import ThemeOption from '../../components/themeOption/index.js';
import THEMECONFIG from '../../components/themeOption/constant.js';

@injectIntl
export default class ThemeManage extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      list: null
    };
    this.formatMessage = props.intl.formatMessage;
  }

  componentWillMount() {
    this.refreshTheme();
  }

  refreshTheme = () => {
    getSystemThemes().then((res) => {
      if (res && res.data) {
        const { list = [] } = res.data;
        this.setState({ list });
      }
    });
  };

  setLoading = () => {
    this.timeout = setTimeout(() => {
      this.setState({ showLoading: true });
    }, DELAYTIME);
  };

  closeLoading = () => {
    if (this.timeout) clearTimeout(this.timeout);
    this.setState({ showLoading: false });
  };

  submitData = () => {
    const { themeRef } = this;
    let submitData = themeRef.state.data;
    submitData = submitData.filter((item) => item.status === 1);
    const { intl: themeTit = '' } = THEMECONFIG[submitData[0].theme] || {};
    Modal.confirm({
      // title: `将 ${themeTit || ''} 设为默认主题吗？`,
      title: this.formatMessage(selfMessage.confirm_title, {
        themeTit: this.formatMessage(commonMessage[themeTit])
      }),
      content: this.formatMessage(commonMessage.confirm_content),
      onOk: () => {
        this.setLoading();
        updateSystemTheme(submitData[0])
          .then((res) => {
            this.closeLoading();
            if (res.status === 200) {
              message.success(
                this.formatMessage(selfMessage.message_confirm_success)
              );
              // this.setState({ list: themeRef.state.data });
              this.refreshTheme();
            }
          })
          .catch(() => {
            this.closeLoading();
          });
      }
    });
  };

  render() {
    const { list, showLoading } = this.state;
    return (
      <ThemeOption
        title={this.formatMessage(selfMessage.header)}
        onRef={(res) => {
          this.themeRef = res;
        }}
        showLoading={showLoading}
        checkFont
        data={list}
        config={[
          { title: this.formatMessage(selfMessage.title_theme), key: 'Theme' },
          { title: this.formatMessage(selfMessage.title_logo), key: 'Logo' },
          { title: this.formatMessage(selfMessage.title_font), key: 'Font' }
        ]}
        buttons={
          <>
            <Button type="primary" onClick={this.submitData}>
              {this.formatMessage(commonMessage.button_setting)}
            </Button>
          </>
        }
      />
    );
  }
}
