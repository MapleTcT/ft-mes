// 系统关于界面
import React from 'react';
import { Spin } from 'sup-ui';
import { injectIntl } from 'react-intl';
import { getPlatformInfo } from 'root/services/about';
import style from './index.less';
import messages from './messages';

@injectIntl
export default class About extends React.PureComponent {
  state = {
    loading: true,
    adpVersion: null,
    supplantVersion: null
  };

  componentDidMount() {
    getPlatformInfo().then(({ data }) => {
      const {
        data: { adpVersion, supplantVersion }
      } = data;
      this.setState({
        loading: false,
        adpVersion,
        supplantVersion
      });
    });
  }

  intl(key) {
    const { intl } = this.props;
    return intl.formatMessage(messages[key]);
  }

  render() {
    const { loading, adpVersion, supplantVersion } = this.state;
    return (
      <div className={style.aboutWrap}>
        {loading ? (
          <Spin />
        ) : (
          <>
            <div className={style.aboutLogo}>
              <div className={style.aboutLogoImg} />
            </div>
            <div className={style.aboutVersion}>
              {supplantVersion && (
                <p>
                  {this.intl('supplantVersion')}
                  {supplantVersion}
                </p>
              )}
              {adpVersion && (
                <p>
                  {this.intl('adpVersion')}
                  {adpVersion}
                </p>
              )}
            </div>
            <div className={style.aboutCheckuth}>
              <a onClick={this.checkLicense}>{this.intl('viewLicense')}</a>
            </div>
          </>
        )}
      </div>
    );
  }

  checkLicense() {
    top.postMessage(
      {
        type: 'viewLicense'
      },
      '*'
    );
  }
}
