import React from 'react';
import { Button, message, Modal } from 'sup-ui';
import { injectIntl } from 'react-intl';
// import classnames from 'classnames';
// import { getSystemThemes } from '../../services/demoApi';
import {
  getPersonalTheme,
  getSystemThemes,
  updatePersonalTheme
} from '../../services/themeSetting';
import commonMessage from '../../components/themeOption/message.js';
import selfMessage from './message.js';
import { DELAYTIME } from '../../utils/constant.js';
import ThemeOption from '../../components/themeOption/index.js';
// import styles from './styles.less';

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
    getSystemThemes().then((res1) => {
      if (res1 && res1.data) {
        const { list = [] } = res1.data;
        getPersonalTheme().then((res2) => {
          if (res2 && res2.data) {
            const { data } = res2.data;
            this.setState({
              list: list.map((item) => {
                if (data.theme === item.theme) return { ...item, ...data };
                return { ...item, status: 0, font: data.font };
              })
            });
          }
        });
      }
    });
  }

  setLoading = () => {
    this.timeout = setTimeout(() => {
      this.setState({ showLoading: true });
    }, DELAYTIME);
  };

  closeLoading = () => {
    if (this.timeout) clearTimeout(this.timeout);
    this.setState({ showLoading: false });
  };

  // 恢复默认
  getDefaultConfig = () => {
    Modal.confirm({
      // title: '确认恢复为默认主题吗？',
      title: this.formatMessage(selfMessage.confirm_tit),
      content: this.formatMessage(commonMessage.confirm_content),
      onOk: () => {
        getSystemThemes().then((res) => {
          if (res && res.data) {
            const { list = [] } = res.data;
            const resetData = list.filter((item) => item.status === 1);
            updatePersonalTheme({}, 'delete').then((res2) => {
              if (res2.status === 200) {
                message.success(
                  this.formatMessage(commonMessage.message_default_success)
                );
                this.refreshTheme(resetData);
              }
            });
          }
        });
      }
    });
  };

  submitData = () => {
    const { themeRef } = this;
    let submitData = themeRef.state.data;
    Modal.confirm({
      title: this.formatMessage(commonMessage.confirm_title),
      content: this.formatMessage(commonMessage.confirm_content),
      onOk: () => {
        this.setLoading();
        submitData = submitData.filter((item) => item.status === 1);
        updatePersonalTheme(submitData[0])
          .then((res) => {
            this.closeLoading();
            if (res.status === 200) {
              message.success(
                this.formatMessage(commonMessage.message_confirm_success)
              );
              this.refreshTheme(submitData);
            }
          })
          .catch(() => {
            this.closeLoading();
          });
      }
    });
  };

  // 刷新当前页主题
  refreshTheme = (theme) => {
    window.localStorage.setItem('theme', JSON.stringify(theme[0]));
    setTimeout(() => {
      window.parent.location.reload();
    }, 1000);
  };

  render() {
    const { list, showLoading } = this.state;
    return (
      <>
        {list && (
          <ThemeOption
            title={this.formatMessage(selfMessage.header)}
            onRef={(res) => {
              this.themeRef = res;
            }}
            showTag
            showLoading={showLoading}
            data={list}
            config={[
              {
                title: this.formatMessage(selfMessage.title_theme),
                key: 'Theme'
              },
              { title: this.formatMessage(selfMessage.title_font), key: 'Font' }
            ]}
            buttons={
              <>
                <Button
                  style={{ width: '114px', marginRight: '20px' }}
                  type="primary"
                  onClick={this.submitData}
                >
                  {this.formatMessage(commonMessage.button_save)}
                </Button>
                <Button onClick={this.getDefaultConfig}>
                  {this.formatMessage(commonMessage.button_default)}
                </Button>
              </>
            }
          />
        )}
      </>
    );
  }
}
