import React from 'react';
import { injectIntl } from 'react-intl';
// import { Select as SysSelect } from 'sup-rc-syscode';
import {
  Form,
  Row,
  Input,
  Modal,
  Col,
  Select,
  Checkbox,
  Icon,
  Popover
} from 'sup-ui';
import { SupReference } from 'sup-rc-reference';
import { SupI18nSelect } from 'sup-rc-i18n';
import messages from './messages.js';
import './index.less';

const { Option } = Select;
const { TextArea } = Input;

@injectIntl
@Form.create()
export default class PortalEditForm extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.formatMessage = intl.formatMessage;
  }

  state = {
    range: [{ id: 'id', name: 'name' }],
    tabs: [{ url: 'url', name: 'name' }],
    applyTab: false
  };

  applyTabChange = (e) => {
    const { checked } = e.target;
    // const { form } = this.props;
    // form.setFieldsValue({
    //   applyTab: checked
    // });
    this.setState({
      applyTab: checked
    });
  };

  onCancel = () => {
    this.props.onCancel();
  };

  onChangePortalName = (value, key) => {
    const { setFieldsValue } = this.props.form;
    setFieldsValue({
      [key]: value
    });
  };

  handleTab = (tabIndex) => {
    const { tabs } = this.state;
    if (tabIndex) {
      const prevTabs = [...tabs];
      prevTabs.splice(tabIndex, 1);
      this.setState({
        tabs: prevTabs
      });
    } else if (tabs.length < 9) {
      this.setState({
        tabs: tabs.concat([{ name: '', url: '' }])
      });
    }
  };

  applyAuthChange = (e, key) => {
    const { checked } = e.target;
    this.setState({
      [key]: checked
    });
  };

  renderDesc = (title, type) => {
    const desc = {
      url: 'portlet加载时发送的请求地址',
      onload: 'onload事件，portlet加载时触发',
      resize: 'resize事件，portlet大小发生变化时触发',
      more: '更多事件，点击更多按钮时触发'
    };
    const example = {
      url: '/foundation/user/add.action',
      onload: 'console.log("onlad事件");',
      resize: 'console.log("resize事件");',
      more: 'console.log("更多事件");'
    };
    const attention = {
      url:
        '注意：1. url以http或https开头时只能选择iframe方式打开；2.url建议定制，不建议直接用模块配置的列表url，如果要用请解决pt的展现样式（如高度）和数据权限问题',
      onload: '注意：直接编写js代码，无需定义方法。',
      resize: '注意：直接编写js代码，无需定义方法。',
      more: '注意：直接编写js代码，无需定义方法。'
    };

    return (
      <div>
        <span style={{ paddingRight: '10px' }}>{title}</span>
        <Popover
          placement="bottomLeft"
          title="说明"
          overlayStyle={{ width: 400 }}
          content={
            <div>
              <div style={{ padding: '4px 0' }}>{desc[type]}</div>
              <div
                style={{
                  background: '#f2f2f2',
                  height: '24px',
                  lineHeight: '24px',
                  margin: '4px 0'
                }}
              >
                范例
              </div>
              <div
                style={{
                  height: '24px',
                  display: 'inline-block',
                  borderBottom: '1px dashed #e4e4e4',
                  width: '100%'
                }}
              >
                {example[type]}
              </div>
              <div style={{ paddingTop: '6px' }}>{attention[type]}</div>
            </div>
          }
          trigger="click"
        >
          <span style={{ cursor: 'pointer' }}>?</span>
        </Popover>
      </div>
    );
  };

  render() {
    const { getFieldDecorator } = this.props.form;
    const {
      range,
      tabs,
      applyTab,
      applyAuth,
      applyIframe,
      portalName,
      nameDisplay
    } = this.state;
    const language = localStorage.getItem('language');
    const i18nParam = {};
    if (nameDisplay) {
      i18nParam[language] = nameDisplay;
    }
    return (
      <Modal
        visible
        maskClosable={false}
        destroyOnClose
        width={580}
        title={this.formatMessage(messages.portalBtnAddUser)}
        bodyStyle={{
          padding: '24px 24px 18px 24px',
          maxHeight: '450px',
          overflowY: 'auto'
        }}
        onOk={this.onOk}
        onCancel={this.onCancel}
        afterClose={this.resetUserType}
      >
        <Form
          layout="vertical"
          style={{ width: 440, margin: '0 auto' }}
          className="portal-form"
        >
          <Row>
            <Col span={12} style={{ width: 200, marginRight: 40 }}>
              <Form.Item
                label={this.formatMessage(messages.portalCode)}
                key="portalCode"
              >
                {getFieldDecorator('portalCode', {
                  rules: [
                    {
                      required: true
                    }
                  ]
                })(<Input maxLength={50} />)}
              </Form.Item>
              <Form.Item
                label={this.formatMessage(messages.portalModel)}
                key="portalModel"
              >
                {getFieldDecorator('portalModel', {
                  rules: [
                    {
                      required: true
                    }
                  ]
                })(<Input maxLength={50} />)}
              </Form.Item>
              <Form.Item
                label={this.formatMessage(messages.portalApplyAuth)}
                key="portalApplyAuth"
              >
                {getFieldDecorator(
                  'portalApplyAuth',
                  {}
                )(
                  <Checkbox
                    defaultChecked={false}
                    onChange={(e) => this.applyAuthChange(e, 'applyAuth')}
                  />
                )}
              </Form.Item>
              <Form.Item
                label={this.formatMessage(messages.portalApplyIframe)}
                key="portalApplyIframe"
              >
                {getFieldDecorator(
                  'portalApplyIframe',
                  {}
                )(
                  <Checkbox
                    defaultChecked={false}
                    onChange={(e) => this.applyAuthChange(e, 'applyIframe')}
                  />
                )}
              </Form.Item>
              <Form.Item
                label={this.formatMessage(messages.portalApplyTab)}
                key="portalApplyTab"
              >
                {getFieldDecorator('portalApplyTab', {
                  initialValue: applyTab
                })(
                  <Checkbox
                    defaultChecked={applyTab}
                    onChange={this.applyTabChange}
                  />
                )}
              </Form.Item>
            </Col>
            <Col span={12} style={{ width: 200 }}>
              <Form.Item
                label={this.formatMessage(messages.portalName)}
                key="portalName"
              >
                {getFieldDecorator('portalName', {
                  initialValue: {
                    moduleCode: 'rbac',
                    i18nKey: portalName,
                    ...i18nParam
                  },
                  rules: [
                    {
                      required: true
                    }
                  ]
                })(
                  <SupI18nSelect
                    ref={(ref) => {
                      this.i18nName = ref;
                    }}
                    size="small"
                    callback={(value) => {
                      this.onChangePortalName(value, 'portalName');
                    }}
                  />
                )}
              </Form.Item>
              <Form.Item
                label={this.formatMessage(messages.portalRange)}
                key="portalRange"
              >
                {getFieldDecorator('portalRange', {
                  rules: [
                    {
                      required: true
                    }
                  ]
                })(
                  <Select style={{ width: '100%' }} size="small">
                    {range.map((item) => {
                      return (
                        <Option value={item.id} key={item.id}>
                          {item.name}
                        </Option>
                      );
                    })}
                  </Select>
                )}
              </Form.Item>
              {applyAuth ? (
                <Form.Item
                  label={this.formatMessage(messages.portalRelatedMenu)}
                  key="portalRelatedMenu"
                >
                  {getFieldDecorator('portalRelatedMenu', {
                    rules: [
                      {
                        required: true
                      }
                    ]
                  })(
                    <SupReference
                      size="small"
                      multiple
                      referenceView={{
                        title: this.formatMessage(messages.portalRelatedMenu),
                        type: 'menu',
                        companyConfig: {}
                      }}
                    />
                  )}
                </Form.Item>
              ) : (
                <div style={{ height: '58px' }} />
              )}
              {applyIframe ? (
                <Form.Item
                  label={this.formatMessage(messages.portalHeight)}
                  key="portalHeight"
                >
                  {getFieldDecorator('portalHeight', {
                    rules: [
                      {
                        required: true
                      }
                    ]
                  })(<Input maxLength={50} />)}
                </Form.Item>
              ) : null}
            </Col>
          </Row>
          {applyTab ? (
            tabs.map((tab, tabIndex) => (
              // eslint-disable-next-line react/jsx-indent
              <Row>
                <Col span={12} style={{ width: 200, marginRight: 40 }}>
                  <Form.Item
                    label={this.formatMessage(messages.portalTab)}
                    key="portalTab"
                  >
                    {getFieldDecorator('portalTab', {
                      initialValue: tab.name,
                      rules: [
                        {
                          required: true
                        }
                      ]
                    })(<Input maxLength={50} />)}
                  </Form.Item>
                </Col>
                <Col span={12} style={{ width: 200 }}>
                  <Form.Item
                    label={this.formatMessage(messages.portalURL)}
                    key="portalUrl"
                  >
                    {getFieldDecorator('portalUrl', {
                      initialValue: tab.url,
                      rules: [
                        {
                          required: true
                        }
                      ]
                    })(<Input />)}
                  </Form.Item>
                  <Icon
                    type={tabIndex ? 'minus' : 'plus'}
                    onClick={() => this.handleTab(tabIndex)}
                    style={{
                      position: 'absolute',
                      fontSize: '14px',
                      right: '-24px',
                      top: '30px',
                      cursor: 'pointer'
                    }}
                  />
                </Col>
              </Row>
              // eslint-disable-next-line indent
            ))
          ) : (
            <Row>
              <Form.Item
                label={this.renderDesc(
                  this.formatMessage(messages.portalURL),
                  'url'
                )}
                key="portalURL"
              >
                {getFieldDecorator('portalURL', {
                  rules: [
                    {
                      required: true
                    }
                  ]
                })(<Input />)}
              </Form.Item>
            </Row>
          )}
          <Row>
            <Form.Item
              label={this.renderDesc(
                this.formatMessage(messages.portalOnload),
                'onload'
              )}
              key="portalOnload"
            >
              {getFieldDecorator('portalOnload', {})(<TextArea />)}
            </Form.Item>
            <Form.Item
              label={this.renderDesc(
                this.formatMessage(messages.portalResize),
                'resize'
              )}
              key="portalResize"
            >
              {getFieldDecorator('portalResize', {})(<TextArea />)}
            </Form.Item>
            <Form.Item
              label={this.renderDesc(
                this.formatMessage(messages.portalMore),
                'more'
              )}
              key="portalMore"
            >
              {getFieldDecorator('portalMore', {})(<TextArea />)}
            </Form.Item>
            <Form.Item
              label={this.formatMessage(messages.portalMemos)}
              key="portalMemos"
            >
              {getFieldDecorator('portalMemos', {})(<TextArea />)}
            </Form.Item>
          </Row>
        </Form>
      </Modal>
    );
  }
}
