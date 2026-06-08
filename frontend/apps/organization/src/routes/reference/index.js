import React from 'react';
import { Checkbox } from 'sup-ui';
import { injectIntl } from 'react-intl';
import { SupReferenceView } from 'sup-rc-reference';
import { cloneDeep } from 'lodash';
import styles from './styles.less';

@injectIntl
export default class Reference extends React.Component {
  constructor() {
    super();
    this.state = {
      defaultValue: '',
      disabled: false,
      receiveData: 0
    };
    this.selected = [];
  }

  receivecode = {};

  componentWillMount() {
    this.requestParams = this.getUrlParams();
    const { type, crossCompanyFlag, multiSelect } = this.requestParams;
    this.getLoginInfo(); // 显示当前登录公司
    if (crossCompanyFlag !== 'true') {
      this.setState({ disabled: true });
    }
    this.setState({ type, multiple: multiSelect === 'true' });
    window.addEventListener('message', this.receiveOtherWindowMsg, false);
    window.getSelectItem = () => {
      return this.transSelectItem(this.selected);
    };
  }

  componentWillUnmount() {
    window.removeEventListener('message', this.receiveOtherWindowMsg);
  }

  callbackRefFunc = () => {
    const { requestParams } = this;
    const {
      location: { pathname = '' }
    } = window;
    let requestUrl = pathname.split('.action')[0];
    requestUrl = requestUrl && requestUrl.replace(/\//g, '_');
    if (!window.foundation) window.foundation = {};
    if (!window.foundation.common) window.foundation.common = {};
    window.foundation.common[requestUrl] = () => {
      try {
        if (window.parent[requestParams.callBackFuncName]) {
          window.parent[requestParams.callBackFuncName](this.selected);
        }
      } catch (error) {
        console.log(error);
      }
    };
  };

  receiveOtherWindowMsg = (event) => {
    const { data } = event || {};
    try {
      if (typeof data === 'object') {
        const { fromViewCode, refKey, action } = data || {};
        if (action === 'saveRef') {
          const selectItems = this.selected;
          const updateInfo = {
            fromViewCode,
            refKey,
            selectItems: this.transSelectItem(selectItems)
          };
          this.getIframeToPost(updateInfo);
        } else if (action === 'initRef') {
          const { referenceList = [] } = data;
          this.receivecode = {
            fromViewCode,
            refKey
          };
          if (referenceList) {
            this.setState({ referenceList, receiveData: 1 });
            this.selected = referenceList;
          }
        }
      }
    } catch (ex) {
      console.log('ex', ex);
    }
  };

  getUrlParams = (url = window.location.href) => {
    let curl = url;
    curl = decodeURI(curl); // 获取url中"?"符后的字串
    const theRequest = {};
    if (curl.indexOf('?') !== -1) {
      const str = curl.split('?');
      const strs = str[1].split('&');
      for (let i = 0; i < strs.length; i += 1) {
        // eslint-disable-next-line prefer-destructuring
        strs[i] = strs[i].split('#')[0];
        const [key, value] = strs[i].split('=');
        if (key !== '' && key !== undefined && key !== null) {
          theRequest[key] = value;
        }
      }
      return theRequest;
    }
    return {};
  };

  getIframeToPost = (updateInfo = {}, flag = false) => {
    const { fromViewCode, refKey } = updateInfo;
    const frams = window.parent.document.getElementsByTagName('iframe');
    const { IdName } = this.getUrlParams() || {};
    const Info = { ...updateInfo, IdName };
    let curFrame;
    if (frams && frams.length > 0) {
      for (let i = 0; i < frams.length; i += 1) {
        if (frams[i].name === `${fromViewCode}_${refKey}`) {
          curFrame = frams[i];
          break;
        }
      }
    }
    if (!flag) {
      if (curFrame) {
        curFrame.contentWindow.postMessage(Info, '*');
      } else {
        window.parent.postMessage(Info, '*');
      }
    }

    // window.parent.postMessage(updateInfo, '*');
  };

  transSelectItem = (selected = []) => {
    const { type } = this.state;
    const dtSource = selected.map((d = {}) => {
      const item = cloneDeep(d);
      delete item.children;
      delete item.positionFullPath;
      delete item.departmentFullPath;
      return {
        ...item,
        ...(type === 'user'
          ? {
            staffId: item.personId,
            staffName: item.name,
            name: item.userName || item.name
          }
          : {})
      };
    });
    return dtSource;
  };

  // 获取当前登录信息
  getLoginInfo = () => {
    const { companyId } = this.requestParams;
    let loginMsg = window.localStorage.getItem('loginMsg');
    if (companyId) {
      this.setState({ defaultValue: companyId });
      return;
    }
    if (loginMsg) {
      loginMsg = JSON.parse(loginMsg);
      const { currentCompany = {} } = loginMsg;
      const { id: defaultValue } = currentCompany;
      if (defaultValue) this.setState({ defaultValue });
    }
  };

  getSelectRows = () => {
    const { type, referenceList = [] } = this.state;
    if (type === 'user') {
      return referenceList.map((item) => {
        return { ...item, userName: item.userName || item.name };
      });
    } else if (type === 'company') {
      return referenceList.map((item) => {
        return { ...item, name: item.name || item.shortName };
      });
    }
    return referenceList;
  }

  render() {
    const {
      type = 'staff',
      disabled,
      defaultValue,
      multiple,
      receiveData,
      referenceList = []
    } = this.state;
    const isUserFlag = type === 'user';
    return (
      <div
        className={`${styles['ref-content']} ${
          isUserFlag ? styles['user-content'] : ''
        }`}
      >
        <SupReferenceView
          visible
          key={`${type}_${receiveData}`}
          ref={(res) => {
            this.referenceRef = res;
          }}
          multiple={multiple}
          isPage
          type={type}
          bindKey="id"
          selectedRowKeys={referenceList.map((item = {}) => {
            if (item.id) return item.id.toString();
            return '';
          })}
          selectedRows={this.getSelectRows()}
          onChange={(res, isDbclick) => {
            this.selected = res;
            if (isDbclick && !multiple) {
              this.getIframeToPost({
                ...this.receivecode,
                selectItems: this.transSelectItem(res)
              });
            }
          }}
          companyConfig={defaultValue ? { defaultValue, disabled } : {}}
        />
        {isUserFlag && (
          <div className={styles.accountBtn}>
            <Checkbox
              onChange={(res) => {
                if (this.referenceRef) {
                  this.referenceRef.setState({
                    hideHasAccount: res.target.checked
                  });
                }
              }}
            >
              <span>显示无账号人员</span>
            </Checkbox>
          </div>
        )}
      </div>
    );
  }
}
