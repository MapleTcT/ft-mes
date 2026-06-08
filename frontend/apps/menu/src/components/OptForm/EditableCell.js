/*
 * @Author: DWP
 * @Date: 2020-07-22 20:56:19
 * @LastEditors: DWP
 * @LastEditTime: 2020-08-20 18:12:54
 */
import React, { Component } from 'react';
import { Input, Select, Form } from 'sup-ui';
import messages from 'root/common/messages';
import { EditableContext } from './EditableContext';

const { Option } = Select;

class EditableCell extends Component {
  renderCell = ({ form, intl, changeItem }) => {
    const {
      dataIndex,
      record,
      index,
      children,
      help,
      validateStatus,
      ...restProps
    } = this.props;

    switch (dataIndex) {
      case 'operation':
        return (
          <td {...restProps}>
            <div style={{ paddingTop: 8 }}>
              {children}
              {
                record.id && (
                  <Form.Item style={{ display: 'none' }}>
                    {
                      form.getFieldDecorator(`urls[${index - 0}].id`, {
                        initialValue: record.id
                      })(
                        <Input />
                      )
                    }
                  </Form.Item>
                )
              }
            </div>
          </td>
        );
      case 'methodType':
        return (
          <td {...restProps}>
            <Form.Item style={{ margin: 0 }}>
              {
                form.getFieldDecorator(`urls[${index - 0}].methodType`, {
                  rules: [
                    {
                      required: true,
                      message: intl.formatMessage(messages.typeTip)
                    }
                  ],
                  initialValue: record.methodType
                })(
                  <Select
                    size="small"
                    style={{ width: 80 }}
                    onChange={(value) => { changeItem(value, dataIndex, index); }}
                  >
                    <Option value={0}>GET</Option>
                    <Option value={1}>POST</Option>
                    <Option value={2}>PUT</Option>
                    <Option value={3}>DELETE</Option>
                  </Select>
                )
              }
            </Form.Item>
          </td>
        );
      case 'url':
        return (
          <td {...restProps}>
            <Form.Item
              style={{ margin: 0 }}
              help={help}
              validateStatus={validateStatus}
            >
              {
                form.getFieldDecorator(`urls[${index - 0}].url`, {
                  rules: [
                    {
                      required: true,
                      message: intl.formatMessage(messages.urlTip)
                    }
                  ],
                  initialValue: record.url
                })(
                  <Input
                    size="small"
                    style={{ width: '100%' }}
                    onChange={(e) => { changeItem(e.target.value, dataIndex, index); }}
                  />
                )
              }
            </Form.Item>
          </td>
        );
      default:
        return null;
    }
  };

  render() {
    return <EditableContext.Consumer>{this.renderCell}</EditableContext.Consumer>;
  }
}

export default EditableCell;
