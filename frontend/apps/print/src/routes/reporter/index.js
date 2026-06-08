import React from 'react';
import { Layout, message } from 'sup-ui';
import { injectIntl } from 'react-intl';
import GC from '@grapecity/spread-sheets';
// import SupResize from 'sup-rc-resize';
// import style from './style.less';
import ReporterDesigner from './ReporterDesigner/index';
import { getQueryString } from './utils/utils';
import {
  queryTemplateDesign,
  saveTemplateDesign
} from '../../services/templateDesigner';
import messages from './ReporterDesigner/messages';
import { getLicenseKey, getSource } from '../../utils/index.js';

window.GC = GC;
const spreadNS = GC.Spread.Sheets;

class Reporter extends React.Component {
  constructor(props) {
    super(props);

    const param = getQueryString(window.location.hash.split('?')[1]) || {};
    this.state = {
      isRuntime: !!parseInt(param.isRuntime, 10),
      config: {},
      prePrint: false,
      ctrl: null,
      isLoaded: false
    };
  }

  componentWillMount() {
    const { templateId, isRuntime } = getQueryString(window.location.hash.split('?')[1]) || '';
    if (!parseInt(isRuntime, 10)) this.requestTemplateData(templateId);
  }

  async componentDidMount() {
    // eslint-disable-next-line no-undef
    if (getSource() === '2') {
      await getLicenseKey().then((res) => {
        const { data = {} } = res.data || {};
        spreadNS.LicenseKey = data['spreadjs.licence'];
      });
      const hasLicenseKey = this.checkLicenseKey();
      // iframe调用的添加授权提示
      window.parent.postMessage({ action: 'printMessage', hasLicenseKey });
      if (!hasLicenseKey) return false;
    }
    this.setState({ isLoaded: true });
    window.addEventListener('message', (e) => {
      const { isRuntime } = getQueryString(window.location.hash.split('?')[1]) || {};
      const { type } = e.data;
      if (parseInt(isRuntime, 10)) {
        switch (type) {
          case 'print': {
            const { templateId, formId, pageId } = e.data;
            this.formId = formId;
            this.pageId = pageId;
            this.setState({
              ctrl: null
            });
            this.requestTemplateData(templateId, true);
            break;
          }
          default:
            break;
        }
      }
    });
  }

  // 判断是否有授权码
  checkLicenseKey = () => {
    const { intl } = this.props;
    const Workbook = new spreadNS.Workbook();
    if (!Workbook.getActiveSheet()) {
      message.error(intl.formatMessage(messages.licenseKeyTip));
      return false;
    }
    return true;
  };

  changePrintStatus = () => {
    this.setState({
      prePrint: false
    });
  };

  initConfig = () => {
    return {
      allDataSource: {},
      associatedObject: [],
      backgroundColor: '#ffffff',
      dataSource: { dynamicDataSource: [] },
      dataSourceInfo: { addDataSource: [] },
      dataTable: {},
      fillDataType: 'insert',
      horizontalAdaptive: 'horizontal',
      initLoadData: true,
      json: '',
      object: [],
      parameterAssociate: {},
      position: 'relative',
      runTimeEdit: false,
      runTimeShowSheet: true,
      runTimeZoomLargeSheet: false,
      runTimeZoomSheet: true,
      scrollbarVisible: 'auto',
      serviceInput: {},
      sizeType: 'auto',
      sqlInfo: {},
      statisticTask: {},
      verticalAdaptive: 'horizontal'
    };
  };

  requestTemplateData = (templateId, prePrint = false) => {
    let config = this.initConfig();
    queryTemplateDesign(templateId).then((res) => {
      const {
        status,
        data: {
          data: { content }
        }
      } = res;
      if (+status !== 200) return;
      try {
        if (content && content !== '') {
          config = JSON.parse(content);
        }
        if (prePrint) {
          import('./ReporterCtrl/ReporterCtrl').then((resData) => {
            this.setState({
              config,
              prePrint,
              ctrl: resData.default
            });
          });
        } else {
          this.setState({
            config
          });
        }
      } catch (error) {
        console.error(error);
      }
    });
  };

  saveTemplate = (config = {}, isEnable, callback) => {
    const { intl } = this.props;
    const { templateId } = getQueryString(window.location.hash.split('?')[1]) || '';
    const configStr = JSON.stringify(config);
    saveTemplateDesign(parseInt(templateId, 10), configStr, isEnable).then(
      (res) => {
        const { status } = res;
        if (+status === 200) {
          message.success(
            intl.formatMessage(
              isEnable ? messages.saveAndEnableSuccess : messages.saveSuccess
            )
          );
        } else {
          message.error(
            intl.formatMessage(
              isEnable ? messages.templateEnableFail : messages.saveFail
            )
          );
        }
        setTimeout(() => {
          if (callback && typeof callback === 'function') callback();
        }, 500);
      }
    );
  };

  switchChanged = (checked) => {
    this.setState({
      isRuntime: checked
    });
  };

  renderReporterCtrl = () => {
    const { config, ctrl: ReporterCtrl, prePrint } = this.state;
    const { templateId, appcode, code } = getQueryString(window.location.hash.split('?')[1]) || '';
    return (
      <ReporterCtrl
        {...this.props}
        isPreview
        templateId={templateId}
        config={config}
        prePrint={prePrint}
        entityCode={appcode}
        modelCode={code}
        formId={this.formId}
        pageId={this.pageId}
        changePrintStatus={this.changePrintStatus}
      />
    );
  };

  render() {
    const { isRuntime, config, ctrl, isLoaded } = this.state;
    const { templateId, appcode, code, name } = getQueryString(window.location.hash.split('?')[1]) || '';
    if (!isLoaded) return '';
    return (
      <div style={{ height: '100%', width: '100%' }}>
        <Layout style={{ height: '100%', width: '100%' }}>
          {isRuntime ? (
            ctrl ? (
              this.renderReporterCtrl()
            ) : null
          ) : (
              <ReporterDesigner
                {...this.props}
                templateId={templateId}
                templateName={name}
                entityCode={appcode}
                modelCode={code}
                config={config}
                saveTemplate={this.saveTemplate}
              />
            )}
        </Layout>
      </div>
    );
  }
}

export default injectIntl(Reporter);
