import React from 'react';
import 'braft-editor/dist/index.css';
import { injectIntl } from 'react-intl';
import { Select, Input, Button, Form, Modal, message, Row, Col } from 'sup-ui';
import commonMessage from 'root/common/messages';
import { addTheme, updateTheme, getThemeTree, topictmplmap, getReceiveRange } from 'root/services/messageCenter';
import AddModel from 'root/components/AddModel';
import { SupReference } from 'sup-rc-reference';
import FormTable from './formTable';
import ModelModal from './modelModal';
import styles from './styles.less';

const { Option } = Select;
const FormItem = Form.Item;
@injectIntl
@Form.create()
export default class ThemeForm extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.state = {
      visible: false,
      tableVisible: false,
      okButtonDisabled: true,
      deleteDisable: true,
      selectFormTable: [],
      formTableData: [],
      themeTypeList: [],
      receiveTitle: {
        staff: intl.formatMessage(commonMessage.staff),
        role: intl.formatMessage(commonMessage.role),
        department: intl.formatMessage(commonMessage.department),
        position: intl.formatMessage(commonMessage.position)
      },
      receiveItem: []
    };
  }

  componentWillMount() {
    const { record, renderButton } = this.props;
    renderButton(false);
    if (record.id) {
      Promise.all([topictmplmap(record.id), getReceiveRange(record.id)]).then((res) => {
        this.okModal(res[0].data.list);
        const rangeList = _.get(res[1], 'data.data', [{ staff: [] }]);
        this.setState({
          receiveItem: rangeList.length === 0 ? [{ staff: [] }] : rangeList
        });
      });
    } else {
      this.setState({
        receiveItem: [{ staff: [] }]
      });
    }
    getThemeTree().then((res) => {
      this.setState({
        themeTypeList: res.data.list
      }, () => {
        this.props.form.setFieldsValue({
          type: this.props.type
        });
      });
    });
  }

  handleSubmit = (e) => {
    e.preventDefault();
    const { record, refreshTable, intl } = this.props;
    const { receiveItem } = this.state;
    this.props.form.validateFields((err, value) => {
      if (err) {
        return false;
      }
      const postValue = Object.assign(value, {
        receiveRange: receiveItem
      }, {
        name: value.name.trim()
      });
      if (record && Object.keys(record).length > 0) {
        const update = Object.assign(postValue, {
          id: record.id
        });
        updateTheme(update).then(() => {
          message.success(intl.formatMessage(commonMessage.editSuccess));
          this.props.setModal1Visible(false);
          if (refreshTable) {
            refreshTable();
          }
        });
      } else {
        addTheme(postValue).then(() => {
          message.success(intl.formatMessage(commonMessage.addSuccess));
          this.props.setModal1Visible(false);
          if (refreshTable) {
            refreshTable({}, 'add');
          }
        }).catch((error) => {
          message.error(error.data.message);
        });
      }
    });
  }

  chooseModel = () => {
    this.setState({
      visible: true
    });
  }

  closeModal = () => {
    this.setState({
      visible: false
    });
  }

  delete = () => {
    // const selectRows = this.formTable.state.selectedRows;
    this.removeTableData(this.state.selectFormTable);
  }

  removeTableData = (record) => {
    const { formTableData } = this.state;
    const arr = formTableData;
    const recordIds = record.map((x) => x.id);
    this.setState({
      formTableData: arr.filter((x) => !recordIds.includes(x.id))
    }, () => {
      this.props.form.setFieldsValue({
        tmpIdList: this.state.formTableData.map((item) => item.id)
      });
    });
  }

  okModal = (formTableData) => {
    this.setState({
      formTableData,
      visible: false
    }, () => {
      this.props.form.setFieldsValue({
        tmpIdList: formTableData.map((item) => item.id)
      });
    });
  }

  setModal1Visible = (visible) => {
    this.setState({
      okButtonDisabled: true,
      tableVisible: visible
    });
  }

  submitAddModel = (e) => {
    this.addmodel.handleSubmit(e);
  }

  tableContent = (rule, value, callback) => {
    const { formTableData } = this.state;
    const { intl } = this.props;
    if (formTableData.length === 0) {
      return callback(intl.formatMessage(commonMessage.noSelectTemplate));
    }
    callback();
  }

  receiveSelect= (value, index) => {
    const { receiveItem } = this.state;
    const itemList = receiveItem;
    itemList[index] = {
      [value]: []
    };
    this.setState({
      receiveItem: itemList
    });
  }

  addItem = () => {
    const { receiveItem } = this.state;
    const itemList = receiveItem;
    receiveItem.push({});
    this.setState({
      receiveItem: itemList
    });
  }

  deleteItem = (index) => {
    const { receiveItem } = this.state;
    const obj = receiveItem;
    obj.splice(index, 1);
    this.setState({
      receiveItem: []
    }, () => {
      this.setState({
        receiveItem: obj
      });
    });
  }

  selectTable = (selectFormTable) => {
    this.setState({
      deleteDisable: selectFormTable.length === 0,
      selectFormTable
    });
  }

  chooseReceive = (data, index) => {
    const { receiveItem } = this.state;
    const obj = receiveItem;
    const key = Object.keys(obj[index]);
    obj[index] = {
      [key]: data
    };
    this.setState({
      receiveItem: obj
    });
  }

  render() {
    const { getFieldDecorator } = this.props.form;
    const {
      visible,
      tableVisible,
      formTableData,
      themeTypeList,
      receiveItem,
      receiveTitle
    } = this.state;
    const { record, intl } = this.props;
    // if (renderButton) {
    //   renderButton(!isFieldsTouched() && buttonClick);
    // }
    return (
      <Form
        ref={(node) => { this.themeForm = node; }}
        onSubmit={this.handleSubmit}
        layout="vertical"
        colon={false}
        style={{ width: 800, paddingLeft: 80 }}
      >
        <FormItem
          label={intl.formatMessage(commonMessage.themeCode)}
        >
          {
            getFieldDecorator('code', {
              initialValue: record.code,
              rules: [{
                required: true,
                whitespace: true,
                message: intl.formatMessage(commonMessage.enterThemeCode)
              }, {
                pattern: /^[a-zA-Z0-9_]*$/,
                message: intl.formatMessage(commonMessage.letterNumberUnderline)
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
          label={intl.formatMessage(commonMessage.themeName)}
        >
          {
            getFieldDecorator('name', {
              initialValue: record.name,
              rules: [{
                required: true,
                whitespace: true,
                message: intl.formatMessage(commonMessage.enterThemeName)
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
          label={intl.formatMessage(commonMessage.type)}
        >
          {
            getFieldDecorator('type', {
              rules: [{
                required: true,
                message: intl.formatMessage(commonMessage.selectType)
              }]
            })(
              <Select style={{ width: '100%' }} getPopupContainer={(triggerNode) => triggerNode.parentElement}>
                {
                  themeTypeList.map((x) => {
                    return (<Option value={x.id}>{x.name}</Option>);
                  })
                }
              </Select>
            )
          }
        </FormItem>
        <FormItem
          label={intl.formatMessage(commonMessage.receiveRange)}
        >
          {
            receiveItem.map((item, index) => {
              const name = Object.keys(item)[0];
              const x = item[name];
              return (
                <Row style={{ marginBottom: 6 }}>
                  <Col span={4} style={{ marginRight: 10 }}>
                    <Select value={name} onChange={(value) => { this.receiveSelect(value, index); }} getPopupContainer={(triggerNode) => triggerNode.parentElement}>
                      <Option key="staff" disabled={receiveItem.filter((y) => y.staff).length > 0}>人员</Option>
                      <Option key="role" disabled={receiveItem.filter((y) => y.role).length > 0}>角色</Option>
                      <Option key="department" disabled={receiveItem.filter((y) => y.department).length > 0}>部门</Option>
                      <Option key="position" disabled={receiveItem.filter((y) => y.position).length > 0}>岗位</Option>
                    </Select>
                  </Col>
                  <Col span={18} style={{ marginRight: 10, width: 550 }}>
                    {
                      name ? (
                        <SupReference
                          multiple
                          defalutValue={x}
                          referenceView={{
                            title: receiveTitle[name],
                            type: name
                          }}
                          onChange={(data) => { this.chooseReceive(data, index); }}
                        />
                      ) : (
                        <Input readOnly />
                      )
                    }
                  </Col>
                  <Col span={2} style={{ textAlign: 'right', width: 30 }}>
                    {
                      (receiveItem.length - 1) === index ? (
                        <Button icon="plus" disabled={receiveItem.length === 4} onClick={this.addItem} />
                      ) : (
                        <Button icon="delete" onClick={() => { this.deleteItem(index); }} />
                      )
                    }
                  </Col>
                </Row>
              );
            })
          }
        </FormItem>
        <FormItem
          label={intl.formatMessage(commonMessage.contentTemplate)}
          style={{ overflow: 'hidden' }}
        >
          <div className={styles.contentHead}>
            <Button type="primary" onClick={this.chooseModel} ghost>
              {intl.formatMessage(commonMessage.chooseTemplate)}
            </Button>
            <Button
              icon="delete"
              disabled={this.state.deleteDisable}
              onClick={() => { this.delete(null); }}
              style={{ marginLeft: 12 }}
            />
            <Button
              icon="plus"
              onClick={() => { this.setState({ tableVisible: true }); }}
              style={{ float: 'right' }}
            >
              {intl.formatMessage(commonMessage.modelAdd)}
            </Button>
          </div>
          {
            getFieldDecorator('tmpIdList', {
              rules: [
                {
                  required: true,
                  validator: this.tableContent
                }]
            })(
              <FormTable
                intl={intl}
                ref={(node) => { this.formTable = node; }}
                data={formTableData}
                removeTableData={this.removeTableData}
                selectTable={this.selectTable}
              />
            )
          }
        </FormItem>
        {
          visible ? (
            <ModelModal
              visible={visible}
              formTableData={formTableData}
              closeModal={this.closeModal}
              okModal={this.okModal}
              dom={this.themeForm}
            />
          ) : null
        }
        <Modal
          title={intl.formatMessage(commonMessage.modelAdd)}
          destroyOnClose
          visible={tableVisible}
          maskClosable={false}
          width={720}
          okButtonProps={{ disabled: this.state.okButtonDisabled }}
          okText={intl.formatMessage(commonMessage.confirm)}
          onOk={this.submitAddModel}
          onCancel={() => this.setModal1Visible(false)}
        >
          <div
            style={{
              height: 360,
              overflowY: 'auto'
            }}
          >
            <AddModel
              wrappedComponentRef={(node) => { this.addmodel = node; }}
              setModal1Visible={this.setModal1Visible}
              record={{}}
              renderButton={(value) => {
                if (this.state.okButtonDisabled) {
                  this.setState({
                    okButtonDisabled: value
                  });
                }
              }}
            />
          </div>
        </Modal>
      </Form>
    );
  }
}
