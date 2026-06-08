import React from 'react';
import 'braft-editor/dist/index.css';
import _ from 'lodash';
import { Select, Input, Form, Popover, message, Tooltip, Icon } from 'sup-ui';
import { injectIntl } from 'react-intl';
import commonMessage from 'root/common/messages';
import { getNotice, addContent, editContent, getDefaulTemp, getBaseModel } from 'root/services/messageCenter';
import BraftEditor from 'braft-editor';
import styles from './styles.less';

const { Option } = Select;
const { TextArea } = Input;
const FormItem = Form.Item;

@injectIntl
@Form.create({
  onValuesChange: (props) => {
    const { renderButton } = props;
    renderButton(false);
  }
})
export default class AddModel extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      cardVisibleBraft: false,
      cardVisibleText: false,
      braftError: false,
      noticeFunc: _.get(props, 'record.noticeType', undefined),
      noticeProtocol: _.get(props, 'record.protocol.protocol', undefined),
      contentType: '',
      noticeList: [],
      modelList: []
    };
  }

  componentWillMount() {
    if (this.props.record.noticeType || this.props.record.noticeType === 0) {
      const { noticeFunc } = this.state;
      Promise.all([getNotice(), getBaseModel(this.props.record.noticeType)]).then((res) => {
        this.setState({
          noticeList: res[0].data.list,
          modelList: res[1].data.list,
          contentType: _.get(res[0].data.list.find((x) => x.id === noticeFunc), 'contentType', '')
        });
      });
    } else {
      getNotice().then((res) => {
        this.setState({
          noticeList: res.data.list
        });
      });
    }
  }

  payContent = (content) => {
    if (this.noticeTextType()) {
      this.props.form.setFieldsValue({
        templateBraft: BraftEditor.createEditorState(content)
      });
    } else {
      this.props.form.setFieldsValue({
        templateTextArea: content
      });
    }
    this.setState({
      cardVisibleBraft: false,
      cardVisibleText: false
    });
  }

  selectChange = (value) => {
    this.props.form.setFieldsValue({
      templateBraft: '',
      templateTextArea: ''
    });
    Promise.all([getDefaulTemp(value), getBaseModel(value)])
      .then((res) => {
        this.setState({
          modelList: res[1].data.list,
          noticeFunc: value
        }, () => {
          const content = _.get(res[0], 'data.data.template', '');
          if (!content) {
            this.payContent(content);
          }
        });
      });
  }

  handleSubmit = (e) => {
    e.preventDefault();
    const { record, refreshTable, intl, status } = this.props;
    const protocol = this.returnProtocol();
    let validArr = [];
    if (protocol === 'email') {
      validArr = ['code', 'name', 'memo', 'noticeType', 'templateBraft'];
    } else {
      validArr = ['code', 'name', 'memo', 'noticeType', 'url', 'templateTextArea'];
    }
    this.props.form.validateFields(validArr, (err, value) => {
      if (!err) {
        let template = '';
        if (protocol === 'email') {
          const obj = {
            text: value.templateBraft.toHTML(),
            subject: value.name
          };
          template = JSON.stringify(obj);
        } else if (protocol === 'stationLetter') {
          const obj = {
            url: value.url,
            text: value.templateTextArea
          };
          template = JSON.stringify(obj);
        } else {
          template = value.templateTextArea;
        }
        Object.assign(value, {
          template,
          name: value.name.trim()
        }, record && {
          id: record.id
        });
        if (record && Object.keys(record).length > 0 && status !== 'add') {
          editContent(value).then(() => {
            message.success(intl.formatMessage(commonMessage.editSuccess));
            this.props.setModal1Visible(false);
            if (refreshTable) {
              refreshTable();
            }
          });
        } else {
          addContent(value).then(() => {
            message.success(intl.formatMessage(commonMessage.addSuccess));
            this.props.setModal1Visible(false);
            if (refreshTable) {
              refreshTable();
            }
          }).catch((error) => {
            message.error(error.data.message);
          });
        }
      }
    });
  }

  noticeTextType = () => {
    const { noticeFunc, noticeList, contentType } = this.state;
    let protocol = '';
    if (contentType || contentType === 0) {
      protocol = contentType;
    } else {
      protocol = _.get(noticeList.find((x) => x.id === noticeFunc), 'contentType', '');
    }
    return protocol === 1;
  }

  returnProtocol = () => {
    const { noticeFunc, noticeList, noticeProtocol } = this.state;
    let protocol = '';
    if (noticeProtocol) {
      protocol = noticeProtocol;
    } else {
      protocol = _.get(noticeList.find((x) => x.id === noticeFunc), 'protocol', '');
    }
    return protocol;
  }

  braftValid = (rule, value, callback) => {
    let validText = value;
    if (validText) {
      validText = value.toText();
    }
    if (!validText) {
      this.setState({
        braftError: true
      });
      callback(new Error(this.props.intl.formatMessage(commonMessage.contentRequire)));
    } else {
      this.setState({
        braftError: false
      });
      callback();
    }
  }

  render() {
    const { modelList, noticeFunc, noticeList, cardVisibleBraft, cardVisibleText, braftError } = this.state;
    const { getFieldDecorator } = this.props.form;
    const { intl, record = {} } = this.props;
    const content = (
      <div className={styles.templateList}>
        {
          modelList.length > 0 ? modelList.map((x) => {
            return (
              <p
                key={x.id}
                onClick={() => { this.payContent(x.template); }}
                className={styles.singleMenu}
              >
                {x.name}
              </p>
            );
          }) : (
            <p className={styles.noModal}>暂无模板</p>
          )
        }
      </div>
    );
    const braftValue = BraftEditor.createEditorState(_.get(JSON.parse(this.noticeTextType() ? (record.template || '{}') : '{}'), 'text', ''));
    return (
      <div
        style={{
          width: '100%',
          height: '100%'
        }}
        onClick={() => {
          if (this.state.cardVisibleBraft || this.state.cardVisibleText) {
            this.setState({
              cardVisibleBraft: false,
              cardVisibleText: false
            });
          }
        }}
      >
        <Form
          className="addmodel"
          onSubmit={this.handleSubmit}
          layout="vertical"
          colon={false}
          style={{ width: 600, paddingLeft: 80 }}
          ref={(node) => { this.node = node; }}
        >
          <FormItem
            label={intl.formatMessage(commonMessage.modelCode)}
          >
            {
              getFieldDecorator('code', {
                initialValue: record.code,
                rules: [{
                  required: true,
                  whitespace: true,
                  message: intl.formatMessage(commonMessage.enterModelCode)
                }, {
                  pattern: /^[a-zA-Z0-9_]*$/,
                  message: intl.formatMessage(commonMessage.modelCodeRule)
                }, {
                  max: 50,
                  message: intl.formatMessage(commonMessage.maxWord, { num: 50 })
                }]
              })(
                <Input disabled={!!record.code} />
              )
            }
          </FormItem>
          <FormItem
            label={intl.formatMessage(commonMessage.modelName)}
          >
            {
              getFieldDecorator('name', {
                initialValue: record.name,
                rules: [{
                  required: true,
                  whitespace: true,
                  message: intl.formatMessage(commonMessage.enterModelName)
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
            label={intl.formatMessage(commonMessage.notice)}
          >
            {
              getFieldDecorator('noticeType', {
                initialValue: record.noticeType || undefined,
                rules: [{
                  required: true,
                  message: intl.formatMessage(commonMessage.selectNotice)
                }]
              })(
                <Select
                  onChange={this.selectChange}
                  disabled={!!record.noticeType}
                  style={{ width: '100%' }}
                  getPopupContainer={(triggerNode) => triggerNode.parentElement}
                >
                  {
                    noticeList.map((x) => {
                      return (
                        <Option value={x.id}>{x.name}</Option>
                      );
                    })
                  }
                </Select>
              )
            }
          </FormItem>
          {
            this.returnProtocol() === 'stationLetter' ? (
              <FormItem
                label={
                  <div>
                    <span>URL</span>
                    <Tooltip
                      placement="bottomLeft"
                      title={
                        <div>
                          <p>{intl.formatMessage(commonMessage.urlRule)}</p>
                          <p>1、http://www.supos.com</p>
                          <p>{intl.formatMessage(commonMessage.urlRule2)}</p>
                        </div>
                      }
                      arrowPointAtCenter
                    >
                      <Icon
                        type="question-circle"
                        style={{
                          marginLeft: 6,
                          cursor: 'pointer',
                          fontSize: 12
                        }}
                      />
                    </Tooltip>
                  </div>
                }
              >
                {
                  getFieldDecorator('url', {
                    initialValue: record.template && _.get(JSON.parse(record.template), 'url', '')
                  })(
                    <Input />
                  )
                }
              </FormItem>
            ) : null
          }
          <FormItem
            label={intl.formatMessage(commonMessage.desc)}
          >
            {
              getFieldDecorator('memo', {
                initialValue: record.memo,
                rules: [
                  {
                    max: 255,
                    message: intl.formatMessage(commonMessage.maxWord, { num: 255 })
                  }
                ]
              })(
                <TextArea
                  style={{ resize: 'none' }}
                  autosize={{ minRows: 3, maxRows: 3 }}
                />
              )
            }
          </FormItem>
          <FormItem
            label={intl.formatMessage(commonMessage.content)}
            style={{ overflow: 'hidden', position: 'relative', display: this.noticeTextType() ? 'block' : 'none' }}
          >
            {
              noticeFunc ? (
                <div className={styles.contentHead}>
                  <Popover
                    content={content}
                    placement="bottomLeft"
                    visible={cardVisibleBraft}
                  >
                    <div
                      className={styles.menuButton}
                      onClick={(e) => {
                        e.stopPropagation();
                        this.setState({ cardVisibleBraft: true });
                      }}
                    >
                      {intl.formatMessage(commonMessage.model)}
                    </div>
                  </Popover>
                </div>
              ) : null
            }
            {
              getFieldDecorator('templateBraft', {
                initialValue: record.template ? braftValue : '',
                validateTrigger: 'onBlur',
                rules: [
                  {
                    required: true,
                    validator: this.braftValid
                  }
                ]
              })(
                <BraftEditor
                  style={{ marginTop: 12 }}
                  controlBarStyle={{ fontSize: 12, boxShadow: 'none' }}
                  contentStyle={{
                    height: 180,
                    fontSize: 12,
                    border: '1px solid',
                    borderColor: braftError ? '#eb2f96' : '#d9d9d9',
                    borderRadius: 4
                  }}
                  fontSizes={[
                    12, 14, 16, 18, 20, 24, 28, 30, 32, 36, 40, 48
                  ]}
                  controls={[
                    'font-size',
                    'line-height',
                    'text-color',
                    'bold',
                    'italic',
                    'underline',
                    'text-align'
                  ]}
                />
              )
            }
          </FormItem>
          <FormItem
            label={intl.formatMessage(commonMessage.content)}
            style={{ overflow: 'hidden', position: 'relative', display: this.noticeTextType() ? 'none' : 'block' }}
          >
            {
              noticeFunc ? (
                <div className={styles.contentHead}>
                  <Popover
                    content={content}
                    placement="bottomLeft"
                    visible={cardVisibleText}
                  >
                    <div
                      className={styles.menuButton}
                      onClick={(e) => {
                        e.stopPropagation();
                        this.setState({ cardVisibleText: true });
                      }}
                    >
                      {intl.formatMessage(commonMessage.model)}
                    </div>
                  </Popover>
                </div>
              ) : null
            }
            {
              getFieldDecorator('templateTextArea', {
                initialValue: record.protocol_code === 'stationLetter' ? _.get(JSON.parse(record.template), 'text', '') : record.template,
                rules: [
                  {
                    required: true,
                    message: '请填写内容'
                  }
                ]
              })(
                <TextArea
                  style={{ resize: 'none' }}
                  autosize={{ minRows: 6, maxRows: 6 }}
                  maxLength={255}
                />
              )
            }
          </FormItem>
        </Form>
      </div>
    );
  }
}
