import React from 'react';
import { Form, Input, Row, Col } from 'sup-ui';
import { injectIntl } from 'react-intl';
import { Select } from 'sup-rc-syscode';
import messages from './messages';
import style from './style.less';
import UploadImg from './UploadImg.js';

class EditForm extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    const {
      intl,
      form,
      staffInfo
    } = this.props;
    const { name, code, gender,  status, directLeader, grandLeader, phone, email, description, positions, mainPosition, avatarUrl } = staffInfo;
    const { getFieldDecorator } = form;
    const mainPos = positions ? positions.filter((item) => {
      return item.id === mainPosition;
    }) : '';
    return (
      <div className={style.formBox}>
        <Form>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item label={intl.formatMessage(messages.name)} colon={false}>
                {getFieldDecorator('name', {
                  initialValue: name
                })(<Input readOnly />)}
              </Form.Item>
              <Form.Item label={intl.formatMessage(messages.no)} colon={false}>
                {getFieldDecorator('code', {
                  initialValue: code
                })(<Input readOnly />)}
              </Form.Item>
              <Form.Item label={intl.formatMessage(messages.gender)} colon={false}>
                {getFieldDecorator('gender', {
                  initialValue: gender
                })(<Select entityCode="sys_gender" showArrow={false} open={false} />)}
              </Form.Item>
              <Form.Item label={intl.formatMessage(messages.majorPosition)} colon={false}>
                {getFieldDecorator('mainPosition', {
                  initialValue: mainPos ? mainPos[0].name : ''
                })(<Input readOnly />)}
              </Form.Item>
              <Form.Item label={intl.formatMessage(messages.status)} colon={false}>
                {getFieldDecorator('status', {
                  initialValue: status
                })(<Select entityCode="sys_person_status" showArrow={false} open={false} />)}
              </Form.Item>
              <Form.Item label= {intl.formatMessage(messages.directLeader)} colon={false}>
                {getFieldDecorator('directLeader', {
                  initialValue: directLeader ? directLeader.name : ''
                })(<Input readOnly />)}
              </Form.Item>
              <Form.Item label={intl.formatMessage(messages.grandLeader)} colon={false}>
                {getFieldDecorator('grandLeader', {
                  initialValue: grandLeader ? grandLeader.name : ''
                })(<Input readOnly />)}
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label={intl.formatMessage(messages.headImg)} colon={false}>
                {getFieldDecorator('imageUrl', {
                  initialValue: avatarUrl
                })(<UploadImg key="imageUrl"/>)}
              </Form.Item>
              <Form.Item label={intl.formatMessage(messages.phone)} colon={false}>
                {getFieldDecorator('phone', {
                  initialValue: phone
                })(<Input readOnly />)}
              </Form.Item>
              <Form.Item label={intl.formatMessage(messages.emailAddress)} colon={false}>
                {getFieldDecorator('email', {
                  initialValue: email
                })(<Input readOnly />)}
              </Form.Item>
              <Form.Item label={intl.formatMessage(messages.descrip)} colon={false}>
                {getFieldDecorator('description', {
                  initialValue: description
                })(<Input.TextArea readOnly className={style.reason} />)}
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </div>
    );
  }
}

const WrappedEditForm = Form.create({ name: 'editForm' })(injectIntl(EditForm));

export default WrappedEditForm;
