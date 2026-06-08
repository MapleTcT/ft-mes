import React from 'react';
import {
  Modal,
  Form,
  Row,
  Col,
  Input,
  Select,
  Checkbox,
  message,
  Tabs,
  DatePicker
} from 'sup-ui';
import moment from 'moment';
import { injectIntl } from 'react-intl';
import { Select as SysSelect } from 'sup-rc-syscode';
import { SupReference } from 'sup-rc-reference';
import { addPerson, updateInitPage, updatePerson } from 'root/services/personManage';
import UploadImg from './UploadImg.js';
import styles from './styles.less';
import commonMessage from './messages';

const { Option } = Select;
const { TextArea } = Input;
const { TabPane } = Tabs;

@injectIntl
@Form.create()
export default class AddPerson extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      positions: [],
      code: '',
      companyId: props.treeData.companyId,
      createUser: false,
      okButtonDisabled: false,
      description: '',
      email: '',
      gender: '',
      mainPosition: props.treeData.id,
      name: '',
      password: '',
      phone: '',
      roles: [],
      status: '',
      userDescription: '',
      userName: '',
      grandLeader: {},
      directLeader: {},
      activeKey: '0'
    };
  }

  componentDidMount() {
    const { personStatus, chooseData } = this.props;
    if (personStatus === 'modify') {
      updateInitPage({
        personId: chooseData.id
      }).then((res) => {
        const { entryDate } = res.data.data;
        this.setState({
          ...res.data.data,
          initEntryDate: entryDate ? moment(String(entryDate)) : entryDate
        });
      });
    } else {
      this.setState({
        positions: [{
          id: this.props.treeData.id,
          name: this.props.treeData.name
        }]
      });
    }
  }

  onOk = (e) => {
    e.preventDefault();
    const { personStatus, chooseData, intl } = this.props;
    this.props.form.validateFieldsAndScroll((err, values) => {
      if (!err) {
        let func = null;
        let search = null;
        let tipMessage = null;
        if (personStatus === 'add') {
          func = addPerson;
          search = Object.assign(values, {
            roles: _.get(values, 'roles', []).map((item) => item.id),
            directLeaderId: _.get(values, 'directLeaderId[0].id'),
            grandLeaderId: _.get(values, 'grandLeaderId[0].id')
          });
          tipMessage = intl.formatMessage(commonMessage.addPersonSuccess);
        } else {
          func = updatePerson;
          search = Object.assign(values, {
            id: chooseData.id,
            directLeaderId: _.get(values, 'directLeaderId[0].id'),
            grandLeaderId: _.get(values, 'grandLeaderId[0].id')
          });
          tipMessage = intl.formatMessage(commonMessage.modifyPersonSuccess);
        }
        search.entryDate = this.state.entryDate;
        search.idNumber = search.idNumber || null;
        this.setState({
          okButtonDisabled: true
        });
        func(search).then(() => {
          message.success(tipMessage);
          this.setState({
            okButtonDisabled: false
          });
          this.props.closeModal(true);
        }).catch(() => {
          this.setState({
            okButtonDisabled: false
          });
        });
      }
      const errKeys = Object.keys(err || {});
      const includeKeys = ['title', 'qualification', 'education', 'major', 'idNumber'];
      const { activeKey } = this.state;
      let currActveKey = activeKey;
      errKeys.forEach((key) => {
        if (includeKeys.includes(key)) currActveKey = '1';
        else currActveKey = '0';
      });
      if (activeKey !== currActveKey) {
        this.setState({ activeKey: currActveKey });
      }
    });
  }

  onCancel = () => {
    this.props.closeModal();
  }

  onTabClick = (e) => {
    this.setState({ activeKey: e });
  }

  onDateChange = (date, dateString) => {
    this.setState({
      entryDate: dateString
    });
  }

  renderSys = (code, value) => {
    const { personStatus, selectMenu } = this.props;
    let ret = (<div />);
    if (personStatus === 'modify') {
      if (value) {
        ret = (
          <SysSelect
            showSearch={false}
            size="small"
            style={{ width: '100%' }}
            entityCode={code}
            disabled={selectMenu === 'department'}
          />
        );
      }
    } else {
      ret = (
        <SysSelect
          showSearch={false}
          size="small"
          style={{ width: '100%' }}
          entityCode={code}
          disabled={code === 'sys_person_status' || selectMenu === 'department'}
        />
      );
    }
    return ret;
  }

  render() {
    const { getFieldDecorator, getFieldValue } = this.props.form;
    const { visible, chooseData, intl, personStatus, selectMenu } = this.props;
    const {
      positions,
      code,
      companyId,
      createUser,
      description,
      email,
      gender,
      mainPosition,
      name,
      password,
      phone,
      roles,
      status,
      userDescription,
      userName,
      okButtonDisabled,
      grandLeader,
      directLeader,
      initEntryDate
    } = this.state;
    const { title, qualification, education, major, idNumber, activeKey } = this.state;
    let footer = {};
    if (selectMenu === 'department') {
      footer = {
        footer: false
      };
    }
    return (
      <Modal
        visible={visible}
        destroyOnClose
        maskClosable={false}
        title={chooseData.name || intl.formatMessage(commonMessage.addPerson)}
        onOk={this.onOk}
        okButtonProps={{ disabled: okButtonDisabled }}
        onCancel={this.onCancel}
        width={580}
        bodyStyle={{
          padding: '24px 24px 18px 24px',
          height: '530px',
          overflowY: 'auto'
        }}
        {...footer}
      >
        <Form
          layout="vertical"
          style={{ width: 440, margin: '0 auto' }}
          className="modalForm"
        >
          <Tabs
            activeKey={activeKey}
            onTabClick={this.onTabClick}
          >
            <TabPane tab={intl.formatMessage(commonMessage.staffInfomation)} key="0">
              <Row>
                <Col span={12} style={{ width: 200, marginRight: 40 }}>
                  <Form.Item
                    label={intl.formatMessage(commonMessage.name)}
                    style={{ display: 'none' }}
                  >
                    {
                      getFieldDecorator('companyId', {
                        initialValue: companyId
                      })(
                        <Input maxLength={50} disabled={selectMenu === 'department'} />
                      )
                    }
                  </Form.Item>
                  <Form.Item
                    label={intl.formatMessage(commonMessage.name)}
                  >
                    {
                      getFieldDecorator('name', {
                        initialValue: name,
                        rules: [{
                          required: true,
                          message: intl.formatMessage(commonMessage.enterName)
                        }, {
                          max: 50,
                          message: intl.formatMessage(commonMessage.maxWord, { num: 50 })
                        }]
                      })(
                        <Input size="small" disabled={selectMenu === 'department'} />
                      )
                    }
                  </Form.Item>
                  <Form.Item
                    label={intl.formatMessage(commonMessage.code)}
                  >
                    {
                      getFieldDecorator('code', {
                        initialValue: code,
                        rules: [{
                          required: true,
                          message: intl.formatMessage(commonMessage.enterCode)
                        }, {
                          max: 50,
                          message: intl.formatMessage(commonMessage.maxWord, { num: 50 })
                        }]
                      })(
                        <Input disabled={personStatus === 'modify' || selectMenu === 'department'} size="small" />
                      )
                    }
                  </Form.Item>
                  <Form.Item
                    label={intl.formatMessage(commonMessage.sex)}
                  >
                    {
                      getFieldDecorator('gender', {
                        initialValue: gender,
                        rules: [{
                          required: true,
                          message: intl.formatMessage(commonMessage.enterSex)
                        }]
                      })(
                        this.renderSys('sys_gender', gender)
                      )
                    }
                  </Form.Item>
                  <Form.Item
                    label={intl.formatMessage(commonMessage.mainPost)}
                  >
                    {
                      getFieldDecorator('mainPosition', {
                        initialValue: mainPosition
                      })(
                        <Select style={{ width: '100%' }} disabled={personStatus === 'add' || selectMenu === 'department'} size="small">
                          {
                            positions.map((item) => {
                              return <Option value={item.id} key={item.id}>{item.name}</Option>;
                            })
                          }
                        </Select>
                      )
                    }
                  </Form.Item>
                  <Form.Item
                    label={intl.formatMessage(commonMessage.status)}
                  >
                    {
                      getFieldDecorator('status', {
                        initialValue: status
                      })(
                        this.renderSys('sys_person_status', status)
                      )
                    }
                  </Form.Item>
                  <Form.Item
                    label={intl.formatMessage(commonMessage.entryDate)}
                  >
                    {
                      getFieldDecorator('entryDate', {
                        initialValue: initEntryDate
                      })(
                        <DatePicker size="small" format="YYYY-MM-DD" onChange={this.onDateChange} />
                      )
                    }
                  </Form.Item>
                  <Form.Item label={intl.formatMessage(commonMessage.signImg)}>
                    {
                      getFieldDecorator('signPicUrl', {
                        initialValue: chooseData.signPicUrl
                      })(<UploadImg key="signPicUrl" width={96} height={36} />)
                    }
                  </Form.Item>
                  {
                    personStatus === 'add' ? (
                      <Form.Item
                        label={intl.formatMessage(commonMessage.account)}
                      >
                        {
                          getFieldDecorator('createUser', {
                            initialValue: createUser
                          })(
                            <Checkbox defaultChecked={createUser}>
                              {intl.formatMessage(commonMessage.createAccount)}
                            </Checkbox>
                          )
                        }
                      </Form.Item>
                    ) : null
                  }
                </Col>
                <Col span={12} style={{ width: 200 }}>
                  <Form.Item label={intl.formatMessage(commonMessage.headImg)}>
                    {
                      getFieldDecorator('avatarUrl', { initialValue: chooseData.avatarUrl })(<UploadImg key="avatarUrl" />)
                    }
                  </Form.Item>
                  <Form.Item
                    label={intl.formatMessage(commonMessage.directLeader)}
                  >
                    {
                      getFieldDecorator('directLeaderId', {
                        initialValue: [directLeader] || []
                      })(
                        <SupReference
                          size="small"
                          placeholder={intl.formatMessage(commonMessage.placeholder)}
                          referenceView={{
                            title: intl.formatMessage(commonMessage.directLeader),
                            type: 'staff',
                            companyConfig: {
                              parentId: this.props.rootId
                            }
                          }}
                        />
                      )
                    }
                  </Form.Item>
                  <Form.Item
                    label={intl.formatMessage(commonMessage.septumLeader)}
                  >
                    {
                      getFieldDecorator('grandLeaderId', {
                        initialValue: [grandLeader] || []
                      })(
                        <SupReference
                          size="small"
                          placeholder={intl.formatMessage(commonMessage.placeholder)}
                          referenceView={{
                            title: intl.formatMessage(commonMessage.septumLeader),
                            type: 'staff',
                            companyConfig: {
                              parentId: this.props.rootId
                            }
                          }}
                        />
                      )
                    }
                  </Form.Item>
                  <Form.Item
                    label={intl.formatMessage(commonMessage.phone)}
                  >
                    {
                      getFieldDecorator('phone', {
                        initialValue: phone,
                        rules: [
                          {
                            pattern: /^[0-9]*$/,
                            message: intl.formatMessage(commonMessage.phoneRule)
                          }, {
                            max: 50,
                            message: intl.formatMessage(commonMessage.maxWord, { num: 50 })
                          }
                        ]
                      })(
                        <Input style={{ width: '100%' }} size="small" disabled={selectMenu === 'department'} />
                      )
                    }
                  </Form.Item>
                  <Form.Item
                    label={intl.formatMessage(commonMessage.email)}
                  >
                    {
                      getFieldDecorator('email', {
                        initialValue: email,
                        rules: [
                          {
                            pattern: /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/,
                            message: intl.formatMessage(commonMessage.enterEmail)
                          }, {
                            max: 50,
                            message: intl.formatMessage(commonMessage.maxWord, { num: 50 })
                          }
                        ]
                      })(
                        <Input size="small" disabled={selectMenu === 'department'} />
                      )
                    }
                  </Form.Item>
                  <Form.Item
                    label={intl.formatMessage(commonMessage.description)}
                  >
                    {
                      getFieldDecorator('description', {
                        initialValue: description,
                        rules: [
                          {
                            max: 255,
                            message: intl.formatMessage(commonMessage.maxWord, { num: 255 })
                          }
                        ]
                      })(
                        <TextArea
                          disabled={selectMenu === 'department'}
                          autosize={{ minRows: 4, maxRows: 4 }}
                          style={{
                            resize: 'none'
                          }}
                        />
                      )
                    }
                  </Form.Item>
                </Col>
              </Row>
              <Row>
                {
                  personStatus === 'add' && getFieldValue('createUser') ? (
                    <div className={styles.moreForm}>
                      <i className={styles.arrow} />
                      <Row>
                        <Col span={12} style={{ width: 190, marginRight: 20 }}>
                          <Form.Item
                            label={intl.formatMessage(commonMessage.userName)}
                          >
                            {
                              getFieldDecorator('userName', {
                                initialValue: userName,
                                rules: [{
                                  required: true,
                                  message: intl.formatMessage(commonMessage.enterUserName)
                                }, {
                                  max: 50,
                                  message: intl.formatMessage(commonMessage.maxWord, { num: 50 })
                                }]
                              })(
                                <Input maxLength={50} size="small" />
                              )
                            }
                          </Form.Item>
                          <Form.Item
                            label={intl.formatMessage(commonMessage.password)}
                          >
                            {
                              getFieldDecorator('password', {
                                initialValue: password,
                                rules: [{
                                  required: true,
                                  message: intl.formatMessage(commonMessage.enterPassword)
                                }, {
                                  max: 50,
                                  message: intl.formatMessage(commonMessage.maxWord, { num: 50 })
                                }]
                              })(
                                <Input.Password maxLength={50} size="small" />
                              )
                            }
                          </Form.Item>
                        </Col>
                        <Col span={12} style={{ width: 190 }}>
                          <Form.Item
                            label={intl.formatMessage(commonMessage.role)}
                          >
                            {
                              getFieldDecorator('roles', {
                                initialValue: roles
                              })(
                                <SupReference
                                  size="small"
                                  multiple
                                  referenceView={{
                                    title: intl.formatMessage(commonMessage.role),
                                    type: 'role',
                                    companyConfig: {
                                      parentId: this.props.rootId
                                    }
                                  }}
                                />
                              )
                            }
                          </Form.Item>
                          <Form.Item
                            label={intl.formatMessage(commonMessage.userDescription)}
                          >
                            {
                              getFieldDecorator('userDescription', {
                                initialValue: userDescription,
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
                          </Form.Item>
                        </Col>
                      </Row>
                    </div>
                  ) : null
                }
              </Row>
            </TabPane>
            <TabPane tab={intl.formatMessage(commonMessage.extendedInformation)} key="1">
              <Row>
                <Col span={12} style={{ width: 200, marginRight: 40 }}>
                  <Form.Item
                    label={intl.formatMessage(commonMessage.technicalTitle)}
                  >
                    {
                      getFieldDecorator('title', {
                        initialValue: title
                      })(
                        <SysSelect
                          allowClear
                          showSearch={false}
                          size="small"
                          style={{ width: '100%' }}
                          entityCode="sys_person_title"
                          selectedDefaultFlag={personStatus === 'add'}
                        />
                      )
                    }
                  </Form.Item>
                  <Form.Item
                    label={intl.formatMessage(commonMessage.qualification)}
                  >
                    {
                      getFieldDecorator('qualification', {
                        initialValue: qualification,
                        rules: [{
                          max: 200,
                          message: intl.formatMessage(commonMessage.maxWord, { num: 200 })
                        }]
                      })(
                        <Input size="small" />
                      )
                    }
                  </Form.Item>
                  <Form.Item
                    label={intl.formatMessage(commonMessage.IDCard)}
                  >
                    {
                      getFieldDecorator('idNumber', {
                        initialValue: idNumber,
                        rules: [{
                          max: 200,
                          message: intl.formatMessage(commonMessage.maxWord, { num: 200 })
                        },
                        {
                          pattern: /^[^\u4e00-\u9fa5]+$/,
                          message: intl.formatMessage(commonMessage.checkIDCard)
                        }]
                      })(
                        <Input size="small" />
                      )
                    }
                  </Form.Item>
                </Col>
                <Col span={12} style={{ width: 200 }}>
                  <Form.Item
                    label={intl.formatMessage(commonMessage.education)}
                  >
                    {
                      getFieldDecorator('education', {
                        initialValue: education
                      })(
                        <SysSelect
                          allowClear
                          showSearch={false}
                          size="small"
                          style={{ width: '100%' }}
                          entityCode="sys_education"
                          selectedDefaultFlag={personStatus === 'add'}
                        />
                      )
                    }
                  </Form.Item>
                  <Form.Item
                    label={intl.formatMessage(commonMessage.major)}
                  >
                    {
                      getFieldDecorator('major', {
                        initialValue: major,
                        rules: [{
                          max: 200,
                          message: intl.formatMessage(commonMessage.maxWord, { num: 200 })
                        }]
                      })(
                        <Input size="small" />
                      )
                    }
                  </Form.Item>
                </Col>
              </Row>
            </TabPane>
          </Tabs>
        </Form>
      </Modal>
    );
  }
}
