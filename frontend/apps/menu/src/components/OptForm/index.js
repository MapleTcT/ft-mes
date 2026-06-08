/*
 * @Author: DWP
 * @Date: 2020-07-21 11:24:56
 * @LastEditors: DWP
 * @LastEditTime: 2021-02-24 14:47:32
 */
import React, { Component } from 'react';
import { Form, Input, Select, Button, Table, Checkbox, Icon } from 'sup-ui';
import { SupI18nSelect } from 'sup-rc-i18n';
import messages from 'root/common/messages';
import { EditableContext } from './EditableContext';
import EditableCell from './EditableCell';
import styles from './index.less';

const { Option } = Select;

@Form.create()
class OptForm extends Component {
  constructor(props) {
    super(props);

    const { intl } = props;

    this.columns = [
      {
        title: intl.formatMessage(messages.optType),
        dataIndex: 'methodType',
        width: 120
      },
      {
        title: intl.formatMessage(messages.optURL),
        dataIndex: 'url'
      },
      {
        title: intl.formatMessage(messages.operation),
        dataIndex: 'operation',
        width: 60,
        render: (text, record, i) => {
          if (this.state.activeKey === i) {
            return (
              <a
                className={styles.delete}
                onClick={() => this.deleteUrl(i)}
              >
                {intl.formatMessage(messages.delete)}
              </a>
            );
          }

          return (
            <Icon theme="filled" type="ellipsis" />
          );
        }
      }
    ];

    this.keyIndex = 0;

    this.state = {
      activeKey: '',
      validateStatus: []
    };
  }

  // 初始化国际化
  onChangeInitValue = (value, key) => {
    const { setFieldsValue } = this.props.form;
    setFieldsValue({
      [key]: value
    });
  }

  // 操作名称校验
  handleCheckName = (rule, value, callback) => {
    const { intl } = this.props;

    const hasValue = this.i18nName.getValidate({ required: true });
    const isInLength = this.i18nName.getValidate({ maxLength: 500 });

    if (!hasValue) {
      callback(intl.formatMessage(messages.enterOptNameTip));
    } else if (!isInLength) {
      callback(intl.formatMessage(messages.maxLengthTip, { count: 500 }));
    } else {
      callback();
    }
  }

  // 添加url
  addUrl = () => {
    const { updateUrl } = this.props;
    if (updateUrl) {
      updateUrl('add');
    }
  };

  // 删除url
  deleteUrl = (index) => {
    const { updateUrl, data: { urls = [] } = {} } = this.props;

    // 控制删除后鼠标当前聚焦行的操作为显示状态
    if (urls.length > 4 && index > 3) {
      const activeKey = index === urls.length - 1 ? index - 1 : index + 1;
      this.setState({
        activeKey
      });
    }

    if (updateUrl) {
      updateUrl('delete', index);
    }
  }

  // 鼠标移入表格行
  handleMouseEnter = (i) => {
    this.setState({
      activeKey: i
    });
  }

  // 鼠标移出表格行
  handleMouseLeave = () => {
    this.setState({
      activeKey: ''
    });
  }

  // 操作数据发生变化
  handleChangeUrls = (value, key, index) => {
    const { intl } = this.props;
    const { validateStatus } = this.state;
    const { urls } = this.props.form.getFieldsValue(['urls']);

    const result = _.cloneDeep(urls[index]);
    let isRepeat = false;

    result[key] = value;

    // 判断类型+url组合是否重复
    urls.forEach((item) => {
      if (result.url === item.url && result.methodType === item.methodType) {
        isRepeat = true;
      }
    });

    validateStatus[index] = isRepeat ? intl.formatMessage(messages.repeatOptTip) : '';

    this.setState({
      validateStatus
    });
  }

  // 校验操作URL（导入重复数据，提交时校验）
  validTableForm = () => {
    const { intl } = this.props;
    const { validateStatus } = this.state;
    const { urls } = this.props.form.getFieldsValue(['urls']);

    if (!urls || urls.length === 0) return;

    const tempUrls = [];
    // 判断类型+url组合是否重复
    urls.forEach((item, i) => {
      const key = `${item.url}_${item.methodType}`;
      if (tempUrls.includes(key)) {
        validateStatus[i] = intl.formatMessage(messages.repeatOptTip);
      } else {
        tempUrls.push(key);
      }
    });

    this.setState({
      validateStatus
    });
  }

  render() {
    const { validateStatus } = this.state;
    const { intl, data, menuinfoId, restrictData, optType, moduleCode } = this.props;
    const { getFieldDecorator } = this.props.form;
    const columns = this.columns.map((col) => {
      return {
        ...col,
        onCell: (record, i) => ({
          help: validateStatus[i] || undefined,
          validateStatus: validateStatus[i] ? 'error' : undefined,
          record,
          dataIndex: col.dataIndex,
          index: i
        })
      };
    });

    const components = {
      body: {
        cell: EditableCell
      }
    };

    const language = localStorage.getItem('language');
    const i18nParam = data.nameDisplay ? {
      i18nValue: {
        [language]: data.nameDisplay
      }
    } : {};
    let scroll = {};
    if (data.urls && data.urls.length * 50 > 180) {
      scroll = {
        y: 180
      };
    }
    return (
      <Form className={styles.form}>
        <Form.Item colon={false} style={{ display: 'none' }}>
          {
            getFieldDecorator('menuinfoId', {
              initialValue: menuinfoId
            })(
              <Input size="small" />
            )
          }
        </Form.Item>
        <Form.Item colon={false} style={{ display: 'none' }}>
          {
            getFieldDecorator('id', {
              initialValue: data.id
            })(
              <Input size="small" />
            )
          }
        </Form.Item>
        <Form.Item colon={false} label={intl.formatMessage(messages.optName)}>
          {
            getFieldDecorator('name', {
              initialValue: {
                moduleCode: moduleCode || 'rbac',
                i18nKey: data.name,
                ...i18nParam
              },
              rules: [
                {
                  required: true,
                  validator: this.handleCheckName
                }
              ]
            })(
              <SupI18nSelect
                ref={(ref) => { this.i18nName = ref; }}
                size="small"
                callback={(value) => this.onChangeInitValue(value, 'name')}
              />
            )
          }
        </Form.Item>
        <Form.Item colon={false} label={intl.formatMessage(messages.optCode)}>
          {
            getFieldDecorator('code', {
              initialValue: data.code,
              rules: [
                {
                  required: true,
                  message: intl.formatMessage(messages.enterCodeTip)
                },
                {
                  max: 200,
                  message: intl.formatMessage(messages.maxLengthTip, { count: 200 })
                },
                {
                  pattern: /^[A-Za-z0-9_]([A-Za-z0-9_.]*[A-Za-z0-9_]){0,200}?$/,
                  message: intl.formatMessage(messages.codeRuleTip)
                }
              ]
            })(
              <Input
                size="small"
                disabled={optType === 'updateOpt'}
              />
            )
          }
        </Form.Item>
        {
          window.menuSource === 'supplant' && (
            <Form.Item colon={false} label={intl.formatMessage(messages.optStyle)}>
              {
                getFieldDecorator('iconCls', {
                  initialValue: data.iconCls
                })(
                  <Select
                    size="small"
                  >
                    <Option value="cui-btn-add">{intl.formatMessage(messages.add)}</Option>
                    <Option value="cui-btn-edit">{intl.formatMessage(messages.edit)}</Option>
                    <Option value="cui-btn-del">{intl.formatMessage(messages.delete)}</Option>
                    <Option value="cui-btn-import">{intl.formatMessage(messages.import)}</Option>
                    <Option value="cui-btn-export">{intl.formatMessage(messages.export)}</Option>
                    <Option value="cui-btn-sort">{intl.formatMessage(messages.sort)}</Option>
                    <Option value="custom">{intl.formatMessage(messages.custom)}</Option>
                  </Select>
                )
              }
            </Form.Item>
          )
        }
        <Form.Item colon={false} label={intl.formatMessage(messages.desc)}>
          {
            getFieldDecorator('memo', {
              initialValue: data.memo,
              rules: [
                {
                  max: 255,
                  message: intl.formatMessage(messages.maxLengthTip, { count: 255 })
                }
              ]
            })(
              <Input.TextArea
                size="small"
                style={{ marginTop: 5 }}
              />
            )
          }
        </Form.Item>
        <Form.Item colon={false} label={intl.formatMessage(messages.restrictedCone)}>
          {
            getFieldDecorator('restrictedCone', {
              initialValue: data.restrictedCone || []
            })(
              <Checkbox.Group
                size="small"
                className={styles.checkboxGroup}
                disabled={window.menuSource !== 'supplant' && optType === 'updateOpt'}
              >
                {
                  restrictData && restrictData.length !== 0
                  && restrictData.map((item) => {
                    return (
                      <Checkbox
                        key={item}
                        value={item}
                        className={styles.checkbox}
                      >
                        {intl.formatMessage(messages[item])}
                      </Checkbox>
                    );
                  })
                }
              </Checkbox.Group>
            )
          }
        </Form.Item>
        <div>
          <div className={styles.urlHeader}>
            <span className={styles.urlTitle}>{intl.formatMessage(messages.optURL)}</span>
            <Button
              size="small"
              type="primary"
              ghost
              onClick={this.addUrl}
            >
              {`+ ${intl.formatMessage(messages.add)}`}
            </Button>
          </div>
          <EditableContext.Provider value={{ form: this.props.form, intl, changeItem: this.handleChangeUrls }}>
            <Table
              rowKey={(record) => record.id || record.key}
              components={components}
              dataSource={data.urls}
              columns={columns}
              scroll={scroll}
              pagination={false}
              onRow={(record, i) => {
                return {
                  onMouseEnter: () => { this.handleMouseEnter(i); },
                  onMouseLeave: () => { this.handleMouseLeave(); }
                };
              }}
            />
          </EditableContext.Provider>
        </div>
      </Form>
    );
  }
}

export default OptForm;
