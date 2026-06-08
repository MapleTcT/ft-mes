import React from 'react';
import { Form, Input, Row, Col } from 'sup-ui';
import { injectIntl } from 'react-intl';
import messages from './messages';
import style from './style.less';

class DetailsForm extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    const {
      intl,
      form,
      row
    } = this.props;
    const { getFieldDecorator } = form;
    return (
      <>
        <Form>
          <Row gutter={24}>
            <Col span={8}>
              <Form.Item label={intl.formatMessage(messages.businessMod)} colon={false}>
                {getFieldDecorator('moduleName', { initialValue: row.moduleName })(<Input readonly="true" />)}
              </Form.Item>
              <Form.Item label={intl.formatMessage(messages.businessKey)} colon={false}>
                {getFieldDecorator('businessKey', { initialValue: row.businessKey })(<Input readonly="true" />)}
              </Form.Item>
              {
                row.processId ? (
                  <Form.Item label={intl.formatMessage(messages.processName)} colon={false}>
                    {getFieldDecorator('processName', { initialValue: row.processName })(<Input readonly="true" />)}
                  </Form.Item>
                ) : null
              }
              <Form.Item label={intl.formatMessage(messages.firstSigner)} colon={false}>
                {getFieldDecorator('firstUserName', { initialValue: row.firstUserName })(<Input readonly="true" />)}
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item label={intl.formatMessage(messages.entityName)} colon={false}>
                {getFieldDecorator('entityName', { initialValue: row.entityName })(<Input readonly="true" />)}
              </Form.Item>
              <Form.Item label={intl.formatMessage(messages.btnName)} colon={false}>
                {getFieldDecorator('buttonName', { initialValue: row.buttonName })(<Input readonly="true" />)}
              </Form.Item>
              {
                row.processId ? (
                  <Form.Item label={intl.formatMessage(messages.activityName)} colon={false}>
                    {getFieldDecorator('taskName', { initialValue: row.taskName })(<Input readonly="true" />)}
                  </Form.Item>
                ) : null
              }
              <Form.Item label={intl.formatMessage(messages.firstSignTime)} colon={false}>
                {getFieldDecorator('firstSignTimeStr', { initialValue: row.firstSignTimeStr })(<Input readonly="true" />)}
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item label={intl.formatMessage(messages.modelName)} colon={false}>
                {getFieldDecorator('modelName', { initialValue: row.modelName })(<Input readonly="true" />)}
              </Form.Item>
              <Form.Item label={intl.formatMessage(messages.ipAddress)} colon={false}>
                {getFieldDecorator('ipAddress', { initialValue: row.ipAddress })(<Input readonly="true" />)}
              </Form.Item>
              {
                row.processId ? (
                  <Form.Item label={intl.formatMessage(messages.migrationLineName)} colon={false}>
                    {getFieldDecorator('transitionName', { initialValue: row.transitionName })(<Input readonly="true" />)}
                  </Form.Item>
                ) : null
              }
              <Form.Item label={intl.formatMessage(messages.signType)} colon={false}>
                {getFieldDecorator('signatureType', { initialValue: row.signatureType })(<Input readonly="true" />)}
              </Form.Item>
            </Col>
          </Row>
          <Form.Item label={intl.formatMessage(messages.firstSignReasons)} colon={false}>
            {getFieldDecorator('firstReason', { initialValue: row.firstReason })(<Input.TextArea readonly="true" className={style.reason} />)}
          </Form.Item>
          <Form.Item label={intl.formatMessage(messages.firstRemarks)} colon={false}>
            {getFieldDecorator('firstRemark', { initialValue: row.firstRemark })(<Input.TextArea readonly="true" className={style.reason} />)}
          </Form.Item>
          {
            row.signatureType === '双签' ? (
              <div>
                <Row gutter={24}>
                  <Col span={8}>
                    <Form.Item label={intl.formatMessage(messages.secondSigner)} colon={false}>
                      {getFieldDecorator('secondUserName', { initialValue: row.secondUserName })(<Input readonly="true" />)}
                    </Form.Item>
                  </Col>
                  <Col span={8}>
                    <Form.Item label={intl.formatMessage(messages.secondSignTime)} colon={false}>
                      {getFieldDecorator('secondSignTimeStr', { initialValue: row.secondSignTimeStr })(<Input readonly="true" />)}
                    </Form.Item>
                  </Col>
                </Row>
                <Form.Item label={intl.formatMessage(messages.secondSignReasons)} colon={false}>
                  {getFieldDecorator('secondReason', { initialValue: row.secondReason })(<Input.TextArea readonly="true" className={style.reason} />)}
                </Form.Item>
                <Form.Item label={intl.formatMessage(messages.secondRemarks)} colon={false}>
                  {getFieldDecorator('secondRemark', { initialValue: row.secondRemark })(<Input.TextArea readonly="true" className={style.reason} />)}
                </Form.Item>
              </div>
            ) : null
          }
        </Form>
      </>
    );
  }
}

const WrappedEditForm = Form.create({ name: 'detailsForm' })(injectIntl(DetailsForm));

export default WrappedEditForm;
