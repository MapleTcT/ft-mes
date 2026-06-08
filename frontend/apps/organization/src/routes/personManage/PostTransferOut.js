import React from 'react';
import {
  Modal,
  message,
  DatePicker,
  Input,
  Select,
  Form
} from 'sup-ui';
import moment from 'moment';
import { injectIntl } from 'react-intl';
import { getStaffPosition, transferOut } from 'root/services/personManage';
import styles from './styles.less';
import commonMessage from './messages';
// import commonMessage from './messages';

const FormItem = Form.Item;
const { Option } = Select;
const { TextArea } = Input;

@injectIntl
@Form.create()
export default class PostTransferOut extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      positionList: [],
      positionIds: [],
      mainPositionList: [],
      mainPositionId: '',
      remark: '',
      id: '',
      offTime: moment()
    };
    console.log(props);
  }

  componentWillMount() {
    const { companyId } = this.props;
    const { id } = this.props.staffData;
    Promise.all([getStaffPosition({ id, companyId }), getStaffPosition({ id })])
      .then((res) => {
        this.setState({
          id,
          positionList: res[0].data.list,
          mainPositionList: res[1].data.list,
          mainPositionId: res[1].data.list.find((item) => item.mainPosition).id
        });
      });
  }

  outChange = (value) => {
    const { getFieldValue, setFieldsValue } = this.props.form;
    if (value.includes(getFieldValue('mainPositionId'))) {
      setFieldsValue({
        mainPositionId: ''
      });
    }
  }

  onOk = (e) => {
    const { intl } = this.props;
    e.preventDefault();
    this.props.form.validateFields((err, values) => {
      if (!err) {
        values.offTime = moment(values.offTime).format('YYYY-MM-DD');
        transferOut(values).then(() => {
          message.success(intl.formatMessage(commonMessage.ourSuccess));
          this.props.closePostModal();
        });
      }
    });
  }

  onCancel = () => {
    this.props.closePostModal();
  }

  render() {
    const { visible, intl } = this.props;
    const { getFieldDecorator, getFieldValue } = this.props.form;
    const { id, positionIds, offTime, mainPositionId, mainPositionList, positionList, remark } = this.state;
    return (
      <Modal
        className="transferOut"
        visible={visible}
        maskClosable={false}
        destroyOnClose
        title={intl.formatMessage(commonMessage.transferOut)}
        onOk={this.onOk}
        onCancel={this.onCancel}
        width={580}
        bodyStyle={{
          maxHeight: '452px',
          overflowY: 'auto',
          padding: 12
        }}
      >
        <Form
          className="outForm"
          layout="vertical"
          colon={false}
          style={{ width: 300, margin: '0 auto' }}
        >
          <FormItem style={{ display: 'none' }}>
            {
              getFieldDecorator('id', {
                initialValue: id
              })(
                <Input type="hidden" />
              )
            }
          </FormItem>
          <FormItem
            label={intl.formatMessage(commonMessage.outPosition)}
          >
            {
              getFieldDecorator('positionIds', {
                initialValue: positionIds,
                rules: [
                  {
                    required: true,
                    message: intl.formatMessage(commonMessage.selectOutPosition)
                  }
                ]
              })(
                <Select
                  mode="multiple"
                  optionLabelProp="label"
                  style={{ width: '100%' }}
                  onChange={this.outChange}
                >
                  {
                    positionList.map((item, index) => {
                      return (
                        <Option key={item.id} value={item.id} label={item.name} style={{ background: index % 2 === 0 ? '#fff' : '#F1F4F8' }}>
                          <span title={item.name} role="img" className={styles.selectOp} aria-label={item.name}>
                            {item.name}
                          </span>
                          <span title={item.code} className={styles.selectOp}>{item.code}</span>
                          <span title={item.deptName} className={styles.selectOp}>{item.deptName}</span>
                        </Option>
                      );
                    })
                  }
                </Select>
              )
            }
          </FormItem>
          <FormItem
            label={intl.formatMessage(commonMessage.outTime)}
          >
            {
              getFieldDecorator('offTime', {
                initialValue: offTime,
                rules: [
                  {
                    required: true,
                    message: intl.formatMessage(commonMessage.selectOutTime)
                  }
                ]
              })(
                <DatePicker
                  style={{ width: '100%' }}
                  allowClear={false}
                />
              )
            }
          </FormItem>
          <FormItem
            label={intl.formatMessage(commonMessage.mainPosition)}
          >
            {
              getFieldDecorator('mainPositionId', {
                initialValue: mainPositionId,
                rules: [
                  {
                    required: true,
                    message: intl.formatMessage(commonMessage.selectMainPosition)
                  }
                ]
              })(
                <Select style={{ width: '100%' }}>
                  {
                    mainPositionList.map((item) => {
                      return (
                        <Option
                          key={item.id}
                          value={item.id}
                          disabled={getFieldValue('positionIds').includes(item.id)}
                        >
                          {item.fullPath}
                        </Option>
                      );
                    })
                  }
                </Select>
              )
            }
          </FormItem>
          <FormItem
            label={intl.formatMessage(commonMessage.memo)}
          >
            {
              getFieldDecorator('remark', {
                initialValue: remark,
                rules: [
                  {
                    max: 255,
                    message: intl.formatMessage(commonMessage.maxWord, { num: 255 })
                  }
                ]
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
        </Form>
      </Modal>
    );
  }
}
