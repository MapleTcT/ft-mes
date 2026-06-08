import React from 'react';
import {
  Form,
  Switch,
  Card,
  Input,
  Col,
  Row,
  TreeSelect,
  Button,
  InputNumber,
  Modal,
  message,
  Tabs
} from 'sup-ui';
import { injectIntl } from 'react-intl';
import { Prompt } from 'react-router-dom';
import commonMessage from 'root/common/messages';
import SysModel from 'root/components/SysModel';
import { getConfig, saveStationLetter, getMenuTree } from 'root/services/messageCenter';
import styles from './notification.less';

const FormItem = Form.Item;
const { TreeNode } = TreeSelect;
let winThis = null;
const { TabPane } = Tabs;

@injectIntl
@Form.create({
  onValuesChange: () => {
    winThis.setState({
      isPrompt: true
    });
  }
})
export default class Message extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      display: false,
      treeData: [],
      alarmLetter: {
        enable: false,
        linkPage: '',
        showGuide: false
      },
      bubbleLetter: {
        enable: false,
        showTotal: 100
      },
      showdowWindow: {
        enable: false,
        heigth: 600,
        linkPage: '',
        title: '',
        width: 800
      },
      isPrompt: false
      // treeValue: undefined
    };
    winThis = this;
  }

  componentWillMount() {
    const { id } = this.props;
    Promise.all([getConfig(id), getMenuTree()])
      .then((res) => {
        const configValue = _.get(res[0], 'data.data.configValue', '{}');
        this.setState({
          treeData: _.get(res[1], 'data.list', []),
          ...JSON.parse(configValue)
        }, () => {
          this.setState({
            display: true
          });
        });
      });
  }

  loop = (data) => {
    return data.map((item) => {
      if (item.children && item.children.length > 0) {
        return (
          <TreeNode key={item.id} value={item.code} title={item.nameDisplay} disabled={!item.url}>
            {this.loop(item.children)}
          </TreeNode>
        );
      }
      return <TreeNode key={item.id} value={item.code} title={item.nameDisplay} disabled={!item.url} />;
    });
  }

  handleSubmit = (e) => {
    e.preventDefault();
    const { intl } = this.props;
    this.setState({
      isPrompt: false
    });
    this.props.form.validateFields((err, values) => {
      if (!err) {
        this.setState({
          lastSave: values
        });
        saveStationLetter(this.transformData(values)).then(() => {
          message.success(intl.formatMessage(commonMessage.updateStationLetter));
        });
      }
    });
  }

  transformData = (values) => {
    return {
      alarmLetter: {
        enable: values.alarmLetter.enable,
        linkPage: values.alarmPage,
        showGuide: !!values.showGuide
      },
      bubbleLetter: {
        enable: values.bubbleLetter.enable,
        showTotal: values.total
      },
      showdowWindow: {
        enable: values.showdowWindow.enable,
        heigth: values.height,
        title: values.title ? values.title.trim() : values.title,
        width: values.width,
        linkPage: values.modelPage
      }
    };
  }

  renderAlarmContent = () => {
    const { intl } = this.props;
    const { getFieldDecorator } = this.props.form;
    const { alarmLetter, treeData } = this.state;
    return (
      <Row className={styles.content}>
        <Col span={10}>
          <FormItem>
            {getFieldDecorator('alarmPage', {
              initialValue: alarmLetter.linkPage
            })(
              <TreeSelect
                showSearch
                style={{ width: 200 }}
                dropdownStyle={{ maxHeight: 200, overflow: 'auto' }}
                placeholder={intl.formatMessage(commonMessage.selectLinkPage)}
                treeNodeFilterProp="title"
                allowClear
                treeDefaultExpandAll
              >
                {this.loop(treeData)}
              </TreeSelect>
            )}
          </FormItem>
        </Col>
      </Row>
    );
  }

  renderPopContent = () => {
    const { bubbleLetter } = this.state;
    const { getFieldDecorator } = this.props.form;
    const { intl } = this.props;
    return (
      <FormItem className={styles.content}>
        {
          getFieldDecorator('total', {
            initialValue: bubbleLetter.showTotal
          })(
            <InputNumber
              min={0}
              placeholder={intl.formatMessage(commonMessage.enterExhibitsCount)}
              style={{ width: 200, marginRight: 12 }}
            />
          )
        }
        {intl.formatMessage(commonMessage.items)}
      </FormItem>
    );
  }

  renderModelContent = () => {
    const { getFieldDecorator } = this.props.form;
    const { intl } = this.props;
    const { showdowWindow, treeData } = this.state;
    return (
      <div className={styles.content}>
        <FormItem label={intl.formatMessage(commonMessage.width)}>
          {
            getFieldDecorator('width', {
              initialValue: showdowWindow.width
            })(
              <InputNumber
                max={1000}
                min={420}
                style={{ width: 135, marginRight: 12 }}
              />
            )
          }
          px
        </FormItem>
        <FormItem label={intl.formatMessage(commonMessage.height)}>
          {
            getFieldDecorator('height', {
              initialValue: showdowWindow.heigth
            })(
              <InputNumber
                max={620}
                min={170}
                style={{ width: 135, marginRight: 12 }}
              />
            )
          }
          px
        </FormItem>
        <FormItem label={intl.formatMessage(commonMessage.title)}>
          {
            getFieldDecorator('title', {
              initialValue: showdowWindow.title
            })(
              <Input
                style={{ width: 200 }}
              />
            )
          }
        </FormItem>
        <FormItem label={intl.formatMessage(commonMessage.linkPage)}>
          {
            getFieldDecorator('modelPage', {
              initialValue: showdowWindow.linkPage
            })(
              <TreeSelect
                showSearch
                style={{ width: 200 }}
                dropdownStyle={{ maxHeight: 200, overflow: 'auto' }}
                placeholder={intl.formatMessage(commonMessage.selectLinkPage)}
                treeNodeFilterProp="title"
                allowClear
                treeDefaultExpandAll
              >
                {this.loop(treeData)}
              </TreeSelect>
            )
          }
        </FormItem>
      </div>
    );
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
    const { alarmLetter, bubbleLetter, showdowWindow, display, isPrompt } = this.state;
    const { intl, form } = this.props;
    const { getFieldDecorator, getFieldValue } = form;
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
              style={{ width: 600, display: display ? 'block' : 'none', margin: '0 auto' }}
            >
              <FormItem
                label={intl.formatMessage(commonMessage.exhibitsPattern)}
              >
                <Card
                  title={intl.formatMessage(commonMessage.alarmMessage)}
                  size="small"
                  extra={
                    getFieldDecorator('alarmLetter.enable', {
                      initialValue: alarmLetter.enable,
                      valuePropName: 'checked'
                    })(
                      <Switch />
                    )
    }
                >
                  {
                    getFieldValue('alarmLetter.enable') ? this.renderAlarmContent() : null
                  }
                </Card>
                <Card
                  title={intl.formatMessage(commonMessage.popMessage)}
                  size="small"
                  extra={
                    getFieldDecorator('bubbleLetter.enable', {
                      initialValue: bubbleLetter.enable,
                      valuePropName: 'checked'
                    })(
                      <Switch />
                    )
    }
                />
                <Card
                  title={intl.formatMessage(commonMessage.modalWindow)}
                  size="small"
                  extra={
                    getFieldDecorator('showdowWindow.enable', {
                      initialValue: showdowWindow.enable,
                      valuePropName: 'checked'
                    })(
                      <Switch />
                    )
    }
                >
                  {
                    getFieldValue('showdowWindow.enable') ? this.renderModelContent() : null
                  }
                </Card>
              </FormItem>
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
              Modal.confirm({
                title: intl.formatMessage(commonMessage.saveTip),
                content: intl.formatMessage(commonMessage.confirmTip),
                okText: intl.formatMessage(commonMessage.confirm),
                onOk() {
                  _self.props.form.validateFields((err, values) => {
                    if (!err) {
                      saveStationLetter(_self.transformData(values)).then(() => {
                        message.success(intl.formatMessage(commonMessage.updateStationLetter));
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
