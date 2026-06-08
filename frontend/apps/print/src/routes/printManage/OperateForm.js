import React from 'react';
import {
  Modal,
  Form,
  Input,
  Select,
  TreeSelect,
  Button,
  message
} from 'sup-ui';
import { SupI18nSelect } from 'sup-rc-i18n';
import { injectIntl } from 'react-intl';
import {
  queryTags,
  addTag,
  saveData,
  copyData
} from '../../services/printManage.js';
import messages from './messages.js';
import style from './style.less';

const { Item } = Form;
const { TextArea } = Input;
const { Option } = Select;
const { TreeNode } = TreeSelect;

@Form.create()
@injectIntl
export default class OperateForm extends React.Component {
  constructor(props) {
    super(props);
    this.formatMessage = props.intl.formatMessage;
    const {
      formData: { pageDatas = [] }
    } = props;
    const modelCode = pageDatas.length ? pageDatas[0].modelCode : '';
    this.state = {
      selectModleCode: modelCode
    };
  }

  componentDidMount() {
    if (this.props.type !== 'alignPage') {
      this.getTagsOption();
    }
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.visible !== this.props.visible && nextProps.visible) {
      if (nextProps.type === 'alignPage') {
        const {
          formData: { pageDatas = [] }
        } = nextProps;
        const modelCode = pageDatas.length ? pageDatas[0].modelCode : '';
        this.setState({ selectModleCode: modelCode });
      } else {
        this.getTagsOption();
      }
    }
  }

  // 匹配页面列表数据
  matchPageCodes = (data, codes) => {
    const predicate = (node) => {
      if (codes.includes(node.id)) {
        return true;
      } else {
        return false;
      }
    };
    if (!(data && data.length)) {
      return [];
    }
    const newChildren = [];
    for (const node of data) {
      if (predicate(node)) {
        newChildren.push(node);
      }
      newChildren.push(...this.matchPageCodes(node.children, codes));
    }
    return newChildren.length ? newChildren : [];
  };

  // 保存
  saveForm = () => {
    const { validateFields } = this.props.form;
    const { type, formData } = this.props;
    const { selectModleCode = '' } = this.state;
    // const { treeData } = this.state;
    let newLanguage = localStorage.getItem('language') || 'zh_CN';
    let params = {};
    const lan = newLanguage.split('-');
    if (lan.length === 2) {
      newLanguage = `${lan[0]}_${lan[1].toUpperCase()}`;
    }
    validateFields((err, value) => {
      if (!err) {
        const {
          templateName,
          pageDatas,
          templateCode,
          labelNames,
          templateDesc
        } = value;
        if (type === 'alignPage') {
          // 关联页面
          let codes = [];
          if (pageDatas.length > 0) {
            codes = pageDatas.map((d) => {
              return {
                ...d,
                pageId: d.value,
                templateId: formData.id,
                modelCode: selectModleCode
              };
            });
          }
          params = { ...formData, pageDatas: codes };
          this.onSave(params);
        } else {
          const { i18nValue = {}, i18nKey, ...rest } = templateName;
          // 新增，编辑
          params = {
            templateName: i18nValue[newLanguage],
            i18nKey,
            templateCode,
            labelNames: labelNames.join(','),
            templateDesc
          };
          if (type === 'new') {
            params = {
              ...params,
              appId: formData.appId,
              enabled: 2 // 新增默认不开启模板
            };
          }
          if (type !== 'new') params = { ...formData, ...params };

          // 校验编码
          if (!this.checkCode(templateCode)) {
            message.error(this.formatMessage(messages.warning_code));
            return false;
          }

          // TODO 国际化保存
          this.i18nSelectRef.onSave(
            { ...rest, i18n_value: i18nValue, i18n_key: i18nKey },
            () => {
              this.onSave(params);
            }
          );
          // this.onSave(params);
        }
      }
    });
  };

  onSave = (params) => {
    const { type, callback = () => {} } = this.props;
    const saveFunc = type === 'copy' ? copyData : saveData;
    const tip = type === 'new'
      ? messages.success_add
      : type === 'copy'
        ? messages.success_copy
        : messages.success_edit;
    saveFunc(params, type).then((res) => {
      if (res.status === 200) {
        // 刷新列表
        message.success(this.formatMessage(tip));
        callback();
      }
    });
  };

  // 校验编码格式 字母、数字、下划线组合
  checkCode = (code = '') => {
    const reg = /^[0-9a-zA-Z_]{1,}$/;
    if (reg.test(code)) {
      return true;
    }
    return false;
  };

  renderEditForm = () => {
    const { tagOption } = this.state;
    const { formData = {} } = this.props;
    const { templateName, labelNames = '', templateDesc = '' } = formData;
    const { getFieldDecorator, setFieldsValue } = this.props.form;
    const languageCode = localStorage.getItem('language') || 'zh-cn';
    const i18nValue = templateName ? { [`${languageCode}`]: templateName } : {};
    return (
      <Form>
        <Item label={this.formatMessage(messages.label_template)}>
          {getFieldDecorator('templateName', {
            initialValue: {
              moduleCode: 'printer',
              i18nKey: formData.i18nKey || '',
              i18nValue
            },
            rules: [
              {
                required: true,
                validator: (rule, value, callback) => {
                  const hasValue = this.i18nSelectRef.getValidate({
                    required: true
                  });
                  const isInLength = this.i18nSelectRef.getValidate({
                    maxLength: 200
                  });

                  if (!hasValue) {
                    callback(this.formatMessage(messages.validate_name));
                  } else if (!isInLength) {
                    callback(this.formatMessage(messages.validate_maxWord));
                  } else {
                    callback();
                  }
                }
              }
            ]
          })(
            <SupI18nSelect
              ref={(ref) => {
                this.i18nSelectRef = ref;
              }}
              callback={(val) => {
                setFieldsValue({ templateName: val });
              }}
            />
          )}
        </Item>
        <Item label={this.formatMessage(messages.label_code)}>
          {getFieldDecorator('templateCode', {
            initialValue: formData.templateCode,
            rules: [
              {
                required: true,
                validator: (rule, value, callback) => {
                  if (!value || !value.length) {
                    callback(this.formatMessage(messages.validate_code));
                  } else if (value.length > 200) {
                    callback(this.formatMessage(messages.validate_maxWord));
                  } else {
                    callback();
                  }
                }
              }
            ]
          })(<Input />)}
        </Item>
        <Item label={this.formatMessage(messages.label_names)}>
          {getFieldDecorator('labelNames', {
            initialValue: labelNames ? labelNames.split(',') : []
          })(
            <Select
              mode="tags"
              placeholder={this.formatMessage(messages.placeholder_keyword)}
              onSelect={(item) => {
                let optionName = [];
                if (tagOption) optionName = tagOption.map((d) => d.labelName);
                if (!optionName.includes(item)) {
                  // 新增label
                  addTag({ labelName: item });
                }
              }}
            >
              {tagOption
                && tagOption.map((d) => {
                  return <Option value={d.labelName}>{d.labelName}</Option>;
                })}
            </Select>
          )}
        </Item>
        <Item label={this.formatMessage(messages.label_descrip)}>
          {getFieldDecorator('templateDesc', {
            initialValue: templateDesc || ''
          })(<TextArea rows={3} style={{ height: '68px' }} />)}
        </Item>
      </Form>
    );
  };

  renderTreeNode = (treeData) => {
    const { selectModleCode } = this.state;
    return treeData.map((item) => {
      const { children } = item;
      const disabledFlag = selectModleCode && selectModleCode !== item.modelCode;
      const title = `${item.name}(${item.code})`;
      if (children && children.length > 0) {
        return (
          <TreeNode
            disabled={disabledFlag}
            value={item.code}
            title={title}
            showTitle={item.name}
            modelCode={item.modelCode}
          >
            {this.renderTreeNode(children)}
          </TreeNode>
        );
      } else {
        return (
          <TreeNode
            disabled={disabledFlag}
            value={item.code}
            title={title}
            showTitle={item.name}
            modelCode={item.modelCode}
          />
        );
      }
    });
  };

  renderAlignPageForm = () => {
    const { formData = {}, treeData = [] } = this.props;
    const { pageDatas } = formData;
    const { getFieldDecorator } = this.props.form;
    const pageCodes = pageDatas
      ? pageDatas.map((d) => {
        return { label: d.label, value: d.pageId };
      })
      : [];
    return (
      <Form>
        {treeData.length === 0 ? (
          ''
        ) : (
          <Item label={this.formatMessage(messages.btn_alignPage)}>
            {getFieldDecorator('pageDatas', {
              initialValue: pageCodes
            })(
              <TreeSelect
                multiple
                showArrow
                allowClear
                treeNodeFilterProp="title"
                treeNodeLabelProp="showTitle"
                placeholder={`-${this.formatMessage(
                  messages.placeholder_select
                )}-`}
                onChange={(value, label, extra) => {
                  let modelCode = '';
                  const { allCheckedNodes = [] } = extra;
                  if (allCheckedNodes.length) {
                    const { node } = allCheckedNodes[0];
                    let { props } = allCheckedNodes[0];
                    if (!props && node) props = node.props;
                    modelCode = props.modelCode;
                  }
                  this.setState({ selectModleCode: modelCode });
                }}
                treeCheckable
                treeCheckStrictly
                treeDefaultExpandAll
                dropdownStyle={{ maxHeight: 400, overflow: 'auto' }}
              >
                {treeData && this.renderTreeNode(treeData)}
              </TreeSelect>
            )}
          </Item>
        )}
      </Form>
    );
  };

  getTagsOption = () => {
    queryTags().then((res) => {
      const {
        data: { data: list }
      } = res;
      this.setState({ tagOption: list });
    });
  };

  render() {
    const { visible, type, onCancel, title } = this.props;
    const isAlignPage = type === 'alignPage';
    return (
      <Modal
        className={style['edit-modal']}
        width="580px"
        destroyOnClose
        title={title}
        maskClosable={false}
        visible={visible}
        onCancel={onCancel}
        footer={
          <div className={style['footer-con']}>
            <Button
              style={{ width: '100px' }}
              type="primary"
              onClick={this.saveForm}
            >
              {this.formatMessage(messages.btn_submit)}
            </Button>
            <Button style={{ width: '90px' }} onClick={onCancel}>
              {this.formatMessage(messages.btn_cancel)}
            </Button>
          </div>
        }
      >
        <div className={style['edit-form']}>
          {isAlignPage ? this.renderAlignPageForm() : this.renderEditForm()}
        </div>
      </Modal>
    );
  }
}
