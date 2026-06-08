import React from 'react';
import {
  Form,
  Input,
  Select,
  Modal,
  message,
  Icon,
  Tooltip,
  Row,
  Col
} from 'sup-ui';
import _ from 'lodash';
import { addCompanyTree, updateCompanyTree, companyTags } from 'root/services/groupManage.js';
import { injectIntl } from 'react-intl';
import commonMessage from './messages';

const FormItem = Form.Item;
const { Option } = Select;
const { TextArea } = Input;
@injectIntl
@Form.create()
export default class Detail extends React.Component {
  constructor() {
    super();
    this.state = {
      tags: [],
      okBtnDisabled: false
    };
  }

  componentWillMount() {
    companyTags().then((res) => {
      this.setState({
        tags: res.data
      });
    });
  }

  onOk = (e) => {
    e.preventDefault();
    const { data, status, intl } = this.props;
    const { okBtnDisabled } = this.state;
    if (okBtnDisabled) return;
    this.props.form.validateFieldsAndScroll((err, values) => {
      if (!err) {
        this.setState({ okBtnDisabled: true });
        const op = Object.assign(
          values,
          { parentId: data.id || null },
          status !== 'add' && { id: data.id, parentId: data.parentId }
        );
        if (status === 'add') {
          addCompanyTree(op).then(() => {
            message.success(intl.formatMessage(commonMessage.addSuccess));
            this.props.closeEdit();
          }).catch(() => {
            this.setState({ okBtnDisabled: false });
          });
        } else {
          updateCompanyTree(op).then(() => {
            message.success(intl.formatMessage(commonMessage.modifySuccess));
            this.props.closeEdit();
          }).catch(() => {
            this.setState({ okBtnDisabled: false });
          });
        }
      }
    });
  }

  onCancel = () => {
    this.props.closeEdit(false);
  }

  render() {
    const { getFieldDecorator } = this.props.form;
    const { visible, status, data = {}, intl } = this.props;
    const { tags, okBtnDisabled } = this.state;
    let initData = {};
    if (status !== 'add') {
      initData = data;
    }
    return (
      <Modal
        className="buttonModal"
        title={intl.formatMessage(commonMessage.baseInfo)}
        visible={visible}
        destroyOnClose
        maskClosable={false}
        okButtonProps={{
          disabled: okBtnDisabled
        }}
        onOk={this.onOk}
        onCancel={this.onCancel}
        width={500}
        bodyStyle={{
          maxHeight: '450px',
          overflowY: 'auto',
          paddingBottom: '10px'
        }}
      >
        <Form
          className="company"
          onSubmit={this.handleSubmit}
          layout="vertical"
          colon={false}
          style={{
            width: 380,
            margin: '0 auto'
          }}
        >
          <FormItem
            label={intl.formatMessage(commonMessage.fullName)}
          >
            {
              getFieldDecorator('fullName', {
                initialValue: initData.fullName,
                rules: [{
                  required: true,
                  message: intl.formatMessage(commonMessage.enterFullName)
                }, {
                  max: 200,
                  message: intl.formatMessage(commonMessage.maxWord, { num: 200 })
                }]
              })(
                <Input />
              )
            }
          </FormItem>
          <FormItem
            label={intl.formatMessage(commonMessage.shortName)}
          >
            {
              getFieldDecorator('shortName', {
                initialValue: initData.shortName,
                rules: [{
                  required: true,
                  message: intl.formatMessage(commonMessage.enterShortName)
                }, {
                  max: 50,
                  message: intl.formatMessage(commonMessage.maxWord, { num: 50 })
                }]
              })(
                <Input />
              )
            }
          </FormItem>
          <FormItem
            label={intl.formatMessage(commonMessage.code)}
          >
            {
              getFieldDecorator('code', {
                initialValue: initData.code,
                rules: [{
                  required: true,
                  message: intl.formatMessage(commonMessage.enterCode)
                }, {
                  pattern: /^[a-zA-Z0-9_]*$/,
                  message: intl.formatMessage(commonMessage.modelCodeRule)
                }, {
                  max: 50,
                  message: intl.formatMessage(commonMessage.maxWord, { num: 50 })
                }]
              })(
                <Input disabled={!!initData.code} />
              )
            }
          </FormItem>
          <FormItem
            label={intl.formatMessage(commonMessage.mark)}
          >
            {
              getFieldDecorator('tags', {
                initialValue: _.get(initData, 'tags', [])
              })(
                <Select
                  showArrow
                  mode="tags"
                  style={{ width: '100%' }}
                  getPopupContainer={(triggerNode) => triggerNode.parentElement}
                >
                  {
                    tags.map((item) => {
                      return <Option key={item.name}>{item.name}</Option>;
                    })
                  }
                </Select>
              )
            }
          </FormItem>
          <FormItem
            label={intl.formatMessage(commonMessage.desc)}
          >
            {
              getFieldDecorator('description', {
                initialValue: initData.description,
                rules: [{
                  max: 255,
                  message: intl.formatMessage(commonMessage.maxWord, { num: 255 })
                }]
              })(
                <TextArea
                  autosize={{ minRows: 2, maxRows: 2 }}
                  style={{
                    resize: 'none'
                  }}
                />
              )
            }
          </FormItem>
          {
            status === 'add' ? (
              <FormItem
                label={
                  <div>
                    <span style={{ marginRight: 6 }}>账号信息</span>
                    <Tooltip placement="topLeft" title="用户名密码创建后不可修改" arrowPointAtCenter>
                      <Icon type="question-circle" />
                    </Tooltip>
                  </div>
                }
              >
                <Row
                  style={{
                    padding: 10,
                    background: '#f8f8f8'
                  }}
                >
                  <Col span={11}>
                    <FormItem
                      label="系统管理员"
                    >
                      {
                        getFieldDecorator('userName', {
                          initialValue: initData.userName,
                          rules: [{
                            required: true,
                            message: '请输入账号'
                          }, {
                            max: 50,
                            message: intl.formatMessage(commonMessage.maxWord, { num: 50 })
                          }]
                        })(
                          <Input />
                        )
                      }
                    </FormItem>
                  </Col>
                  <Col span={11} offset={2}>
                    <FormItem
                      label="密码"
                    >
                      {
                        getFieldDecorator('password', {
                          initialValue: initData.password,
                          rules: [{
                            required: true,
                            message: '请输入密码'
                          }, {
                            max: 50,
                            message: intl.formatMessage(commonMessage.maxWord, { num: 50 })
                          }]
                        })(
                          <Input.Password />
                        )
                      }
                    </FormItem>
                  </Col>
                </Row>
              </FormItem>
            ) : null
          }
        </Form>
      </Modal>
    );
  }
}
