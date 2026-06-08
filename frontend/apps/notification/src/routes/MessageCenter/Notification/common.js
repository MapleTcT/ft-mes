import React from 'react';
import {
  Form,
  Input,
  Button,
  Modal,
  message,
  Tabs
} from 'sup-ui';
import { injectIntl } from 'react-intl';
import { Prompt } from 'react-router-dom';
import commonMessage from 'root/common/messages';
import SysModel from 'root/components/SysModel';
import { getSysSetting, saveSysSetting } from 'root/services/messageCenter';
import styles from './notification.less';

const FormItem = Form.Item;
const { TabPane } = Tabs;
let winThis = null;
@injectIntl
@Form.create({
  onValuesChange: () => {
    winThis.setState({
      isPrompt: true
    });
  }
})
export default class Common extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      config: [],
      isPrompt: false
    };
    winThis = this;
  }

  componentWillMount() {
    const { data } = this.props;
    getSysSetting(data.systemConfigAppCode, data.systemConfigCode).then((res) => {
      this.setState({
        config: res.data.data.config
      });
    });
  }

  handleSubmit = (e) => {
    e.preventDefault();
    const { data, intl } = this.props;
    this.setState({
      isPrompt: false
    });
    this.props.form.validateFields((err, values) => {
      if (!err) {
        this.setState({
          lastSave: values
        });
        const submitValue = Object.keys(values).map((item) => {
          return {
            configId: item,
            value: [values[item].trim()]
          };
        });
        saveSysSetting({ config: submitValue }).then(() => {
          message.success(intl.formatMessage(commonMessage.commonSetting, { name: data.name }));
        });
      }
    });
  }

  renderHTML = (item) => {
    let html = null;
    let max = 255;
    item.verify.forEach((x) => {
      if (x.length) {
        max = x.length;
      }
    });
    switch (item.type) {
      case 0:
        html = (<Input maxLength={max} style={{ width: 365 }} />);
        break;
      case 6:
        html = (
          <Input.TextArea
            maxLength={max}
            style={{ width: 365, resize: 'none' }}
            autosize={{ minRows: 3, maxRows: 6 }}
          />
        );
        break;
      default:
        return html;
    }
    return html;
  }

  resetForm = () => {
    const { lastSave } = this.state;
    if (!lastSave) {
      this.props.form.resetFields();
    } else {
      this.props.form.setFieldsValue(lastSave);
    }
  }

  render() {
    const { getFieldDecorator } = this.props.form;
    const {
      config,
      isPrompt
    } = this.state;
    const { intl } = this.props;
    return (
      <div className={`${styles.wrap} tabWrap`}>
        <Tabs
          defaultActiveKey="1"
          style={{
            height: '100%'
          }}
          tabBarStyle={{
            height: 57,
            margin: '21px 0 0 0',
            background: 'transparent',
            textAlign: 'center',
            borderBottom: 0
          }}
          animated={false}
        >
          <TabPane
            className={styles.setTabpane}
            tab={intl.formatMessage(commonMessage.baseSet)}
            key="1"
          >
            <Form
              onSubmit={this.handleSubmit}
              layout="vertical"
              colon={false}
              style={{ width: 400, margin: '0 auto' }}
            >
              {
                config.map((item) => {
                  let retHtml = null;
                  retHtml = (
                    <FormItem
                      label={item.name}
                    >
                      {
                        getFieldDecorator(item.configId.toString(), {
                          initialValue: _.get(item, 'value[0]', ''),
                          rules: item.verify && item.verify.map((x) => {
                            return {
                              required: x.isRequire,
                              message: x.msg
                            };
                          })
                        })(
                          this.renderHTML(item)
                          // <Input maxLength={50} style={{ width: 365 }} />
                        )
                      }
                    </FormItem>
                  );
                  return retHtml;
                })
              }
            </Form>
            <div className={styles.buttonArea}>
              <Button
                type="primary"
                disabled={!this.props.form.isFieldsTouched()}
                onClick={this.handleSubmit}
                style={{ marginRight: 12, width: 110 }}
              >
                {intl.formatMessage(commonMessage.confirm)}
              </Button>
              <Button
                disabled={!this.props.form.isFieldsTouched()}
                onClick={this.resetForm}
              >
                {intl.formatMessage(commonMessage.cancel)}
              </Button>
            </div>
          </TabPane>
          <TabPane
            className={styles.modelSet}
            tab={intl.formatMessage(commonMessage.defaultModel)}
            key="2"
          >
            <SysModel {...this.props} />
          </TabPane>
        </Tabs>
        <Prompt
          message={(location) => {
            if (location.pathname === '/messageCenter' && this.props.form.isFieldsTouched() && isPrompt) {
              const _self = this;
              const { data } = this.props;
              Modal.confirm({
                title: intl.formatMessage(commonMessage.saveTip),
                content: intl.formatMessage(commonMessage.confirmTip),
                okText: intl.formatMessage(commonMessage.confirm),
                onOk() {
                  _self.props.form.validateFields((err, values) => {
                    if (!err) {
                      const submitValue = Object.keys(values).map((item) => {
                        return {
                          configId: item,
                          value: [values[item].trim()]
                        };
                      });
                      saveSysSetting({ config: submitValue }).then(() => {
                        message.success(intl.formatMessage(commonMessage.commonSetting, { name: data.name }));
                      });
                    }
                    window.history.go(-1);
                    _self.setState({
                      isPrompt: false
                    });
                  });
                },
                onCancel() {
                  window.location.reload();
                }
              });
              return false;
            } else {
              return true;
            }
          }}
        />
      </div>
    );
  }
}
