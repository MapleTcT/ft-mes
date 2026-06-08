import React from 'react';
import 'braft-editor/dist/index.css';
import { Modal, Form, Input, message } from 'sup-ui';
import BraftEditor from 'braft-editor';
import { addAddSysModel, updateSysModel } from 'root/services/messageCenter';
import { injectIntl } from 'react-intl';
import commonMessage from 'root/common/messages';
// import commonMessage from 'root/common/messages';

const FormItem = Form.Item;
const { TextArea } = Input;

@injectIntl
@Form.create()
export default class SysModel extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      braftError: false,
      template: '',
      name: ''
    };
  }

  componentWillMount() {
    const { record } = this.props;
    this.setState({ ...record });
  }

  onOk = (e) => {
    e.preventDefault();
    const { contentType, protocolId, record, intl } = this.props;
    this.props.form.validateFields((err, value) => {
      if (!err) {
        const submit = {
          name: value.name.trim(),
          template: contentType === 1 ? value.template.toHTML() : value.template,
          protocolId,
          templateId: record.id
        };
        let func = null;
        let mes = null;
        if (Object.keys(record).length === 0) {
          func = addAddSysModel;
          mes = intl.formatMessage(commonMessage.addSuccess);
        } else {
          func = updateSysModel;
          mes = intl.formatMessage(commonMessage.editSuccess);
        }
        func(submit).then(() => {
          this.props.closeAdd(true);
          message.success(mes);
        }).catch((error) => {
          message.error(error.data.message);
        });
      }
    });
  }

  onCancel = () => {
    this.props.closeAdd();
  }

  nameValid = (rule, value, callback) => {
    const { data, record, intl } = this.props;
    if (data.filter((item) => item.name === value).length > 0 && Object.keys(record).length === 0) {
      callback(new Error(intl.formatMessage(commonMessage.titleError)));
    } else {
      callback();
    }
  }

  braftValid = (rule, value, callback) => {
    const { contentType, intl } = this.props;
    const validText = contentType === 1 ? value.toText() : value;
    if (!validText) {
      this.setState({
        braftError: true
      });
      callback(new Error(intl.formatMessage(commonMessage.contentRequire)));
    } else {
      this.setState({
        braftError: false
      });
      callback();
    }
  }

  render() {
    const {
      template,
      name,
      braftError
    } = this.state;
    const { visible, contentType, intl } = this.props;
    const { getFieldDecorator } = this.props.form;
    return (
      <Modal
        title={name || intl.formatMessage(commonMessage.modelAdd)}
        visible={visible}
        onOk={this.onOk}
        okText={intl.formatMessage(commonMessage.confirm)}
        onCancel={this.onCancel}
        maskClosable={false}
        width={630}
      >
        <Form
          className="addmodel"
          onSubmit={this.handleSubmit}
          layout="vertical"
          colon={false}
          style={{ width: 530, margin: '0 auto' }}
        >
          <FormItem
            label={intl.formatMessage(commonMessage.modelTitle)}
          >
            {
              getFieldDecorator('name', {
                initialValue: name,
                rules: [{
                  required: true,
                  whitespace: true,
                  message: intl.formatMessage(commonMessage.modelTitleRequire)
                },
                {
                  validator: this.nameValid
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
            label={intl.formatMessage(commonMessage.content)}
          >
            {
              getFieldDecorator('template', {
                initialValue: contentType === 1
                  ? BraftEditor.createEditorState(template)
                  : template,
                rules: [
                  {
                    required: true,
                    validateTrigger: 'onChange',
                    validator: this.braftValid
                  }
                ]
              })(
                contentType === 1 ? (
                  <BraftEditor
                    style={{ marginTop: 9 }}
                    controlBarStyle={{ fontSize: 12, boxShadow: 'none' }}
                    contentStyle={{
                      height: 150,
                      fontSize: 12,
                      border: '1px solid',
                      borderColor: braftError ? '#eb2f96' : '#d9d9d9',
                      borderRadius: 4
                    }}
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
                ) : (
                  <TextArea
                    style={{ resize: 'none' }}
                    autosize={{ minRows: 6, maxRows: 6 }}
                    maxLength={255}
                  />
                )
              )
            }
          </FormItem>
        </Form>
      </Modal>
    );
  }
}
