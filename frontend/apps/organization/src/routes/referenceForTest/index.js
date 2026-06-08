import React from 'react';
import { Form, Row, Col, Select, Switch, Radio, Button } from 'sup-ui';
import { injectIntl } from 'react-intl';
import { SupReference } from 'sup-rc-reference';
import styles from './styles.less';

const { Item } = Form;
const { Option } = Select;

@injectIntl
@Form.create()
export default class Reference extends React.Component {
  constructor() {
    super();
    this.state = {
      type: 'staff',
      multiple: true,
      disabled: false,
      size: 'default',
      autoSize: false
    };
  }

  handleSubmit = (e) => {
    e.preventDefault();
    this.props.form.validateFields((err, values) => {
      if (!err) {
        console.log('Received values of form: ', values);
      }
    });
  };

  handleChange = (value) => {
    const { setFieldsValue } = this.props.form;
    this.setState({ type: value });
    setFieldsValue({ reference: [] });
  };

  checkMultiple = (value) => {
    this.setState({ multiple: value });
  };

  handleSize = (e) => {
    this.setState({ size: e.target.value });
  };

  render() {
    const { getFieldDecorator, setFieldsValue } = this.props.form;
    const { type, multiple, disabled, size, autoSize } = this.state;
    return (
      <div className={styles['ref-content']}>
        <div>
          <div>
            <Row style={{ margin: '20px 0' }}>
              <Col span={6}>
                <Select defaultValue="staff" onChange={this.handleChange}>
                  <Option value="staff">人员</Option>
                  <Option value="department">部门</Option>
                  <Option value="position">岗位</Option>
                  <Option value="role">角色</Option>
                  <Option value="user">用户</Option>
                  <Option value="company">公司</Option>
                </Select>
              </Col>
              <Col span={4}>
                <Switch
                  onChange={this.checkMultiple}
                  checkedChildren="多"
                  unCheckedChildren="单"
                  defaultChecked
                />
              </Col>
              <Col span={6}>
                <Radio.Group onChange={this.handleSize} value={this.state.size}>
                  <Radio value="small">小</Radio>
                  <Radio value="defalut" checked>
                    中
                  </Radio>
                  <Radio value="large">大</Radio>
                </Radio.Group>
              </Col>
              <Col span={4}>
                <Switch
                  onChange={(value) => {
                    this.setState({ autoSize: value });
                  }}
                  checkedChildren="自适应"
                  unCheckedChildren="收起"
                  defaultChecked
                />
              </Col>
              <Col span={4}>
                <Switch
                  onChange={(value) => {
                    this.setState({ disabled: value });
                  }}
                  checkedChildren="编辑"
                  unCheckedChildren="只读"
                  defaultChecked
                />
              </Col>
            </Row>
          </div>
          <Form onSubmit={this.handleSubmit}>
            <Item>
              {getFieldDecorator('reference', {
                initialValue: [],
                rules: [
                  {
                    required: true,
                    message: '不能为空'
                  }
                ]
              })(
                <SupReference
                  disabled={disabled}
                  ref={(res) => {
                    this.referenceRef = res;
                  }}
                  // placeholder="请输入人员"
                  size={size}
                  // showKey="id"
                  onChange={(res) => {
                    console.log('onchange', res);
                  }}
                  multiple={multiple}
                  onClick={(e) => {
                    console.log('click', e);
                  }}
                  onBlur={(e) => {
                    console.log('blur', e);
                  }}
                  autoSize={autoSize}
                  bindKey="id"
                  // suffix={<Icon type="search" />}
                  referenceView={{
                    type
                  }}
                  {...this.props}
                />
              )}
            </Item>
            <Item>
              <Button
                type="primary"
                htmlType="submit"
                className="login-form-button"
              >
                提交
              </Button>
              <Button
                type="primary"
                // htmlType="submit"
                className="login-for-button"
                onClick={() => {
                  setFieldsValue({ reference: [] });
                  // this.referenceRef.handelChange();
                }}
              >
                清空
              </Button>
            </Item>
          </Form>
        </div>
      </div>
    );
  }
}
