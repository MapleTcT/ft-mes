import React from 'react';
import {
  Table,
  Checkbox,
  Icon,
  Modal,
  Tag,
  Button
} from 'sup-ui';
import { injectIntl } from 'react-intl';
import _ from 'lodash';
// import Edit from './edit';
import { getPersonInfos, getPositionInfos, getDepartmentInfos } from 'root/services/authority';
import styles from './styles.less';
import commonMessage from './messages';

@injectIntl
export default class ExtendAuthority extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.state = {
      columns: [
        {
          align: 'center',
          width: 80,
          dataIndex: 'nameDisplay',
          id: 'name',
          title: intl.formatMessage(commonMessage.menu),
          render: (text, record, index) => {
            const filterObj = this.state.data.filter((item) => item.menuInfoId === record.menuInfoId);
            const { length } = filterObj;
            const obj = {
              children: (
                <div className={styles.tdBox} title={text}>
                  {text}
                </div>
              ),
              props: {
                rowSpan: 1
              }
            };
            if (length > 1) {
              const RowIndex = this.state.data.findIndex((item) => item.nameDisplay === text);
              if (RowIndex === index) {
                obj.props.rowSpan = length;
              } else {
                obj.props.rowSpan = 0;
              }
            }
            return obj;
          }
        },
        {
          align: 'center',
          title: intl.formatMessage(commonMessage.operation),
          width: 80,
          id: 'op',
          dataIndex: 'op.nameDisplay',
          render: (text) => {
            return <div className={styles.tdBox} title={text}>{text}</div>;
          }
        },
        {
          align: 'center',
          id: 'dataAuthority',
          title: intl.formatMessage(commonMessage.dataAuthority),
          children: [
            {
              align: 'center',
              width: 80,
              title: intl.formatMessage(commonMessage.positionAuthority),
              dataIndex: `op.${props.status}.positionFlag`,
              render: (text, record) => {
                return (
                  <div className={styles.tdBox}>
                    {
                      record.op.enablePosrestrict ? (
                        <Checkbox
                          checked={text}
                          disabled
                        />
                      ) : '-'
                    }
                  </div>
                );
              }
            },
            {
              align: 'center',
              width: 80,
              title: intl.formatMessage(commonMessage.departmentAuthority),
              dataIndex: `op.${props.status}.departmentFlag`,
              render: (text, record) => {
                return (
                  <div className={styles.tdBox}>
                    {
                      record.op.enableDeptrict ? (
                        <Checkbox
                          checked={text}
                          disabled
                        />
                      ) : '-'
                    }
                  </div>
                );
              }
            },
            {
              align: 'center',
              width: 70,
              title: intl.formatMessage(commonMessage.appointPosition),
              dataIndex: `op.${props.status}.assignPosFlag`,
              render: (text, record) => {
                let style = { border: 'none' };
                if (text) {
                  const data = _.get(record, `op.${props.status}.positions`, []) || [];
                  if (data.length === 0) {
                    style = { border: '1px solid red' };
                  }
                }
                return (
                  <div className={styles.tdBox} style={style}>
                    {
                      record.op.enableAssignpos ? (
                        <div>
                          <Checkbox
                            checked={text}
                            disabled
                          />
                          <Icon
                            type="show"
                            theme="filled"
                            style={{ cursor: `${text ? 'cursor' : 'not-allowed'}` }}
                            className={styles.checkDetail}
                            title={intl.formatMessage(commonMessage.selectPosition)}
                            onClick={() => {
                              if (!text) return;
                              this.checkDetail(
                                'position',
                                intl.formatMessage(commonMessage.selectPosition),
                                _.get(record, 'op.userPermission.positions', [])
                              );
                            }}
                          />
                        </div>
                      ) : '-'
                    }
                  </div>
                );
              }
            },
            {
              align: 'center',
              width: 70,
              title: intl.formatMessage(commonMessage.appointDepartment),
              dataIndex: `op.${props.status}.assignDeptFlag`,
              render: (text, record) => {
                let style = { border: 'none' };
                if (text) {
                  const data = _.get(record, `op.${props.status}.departments`, []) || [];
                  if (data.length === 0) {
                    style = { border: '1px solid red' };
                  }
                }
                return (
                  <div className={styles.tdBox} style={style}>
                    {
                      record.op.enableAssignDept ? (
                        <div>
                          <Checkbox
                            checked={text}
                            disabled
                          />
                          <Icon
                            type="show"
                            theme="filled"
                            className={styles.checkDetail}
                            style={{ cursor: `${text ? 'cursor' : 'not-allowed'}` }}
                            title={intl.formatMessage(commonMessage.selectDepartment)}
                            onClick={() => {
                              if (!text) return;
                              this.checkDetail(
                                'department',
                                intl.formatMessage(commonMessage.selectDepartment),
                                _.get(record, 'op.userPermission.departments', [])
                              );
                            }}
                          />
                        </div>
                      ) : '-'
                    }
                  </div>
                );
              }
            },
            {
              align: 'center',
              width: 70,
              title: intl.formatMessage(commonMessage.appointPerson),
              dataIndex: `op.${props.status}.assignStaffFlag`,
              render: (text, record) => {
                let style = { border: 'none' };
                if (text) {
                  const data = _.get(record, `op.${props.status}.staffs`, []) || [];
                  if (data.length === 0) {
                    style = { border: '1px solid red' };
                  }
                }
                return (
                  <div className={styles.tdBox} style={style}>
                    {
                      record.op.enableAssignstaff ? (
                        <div>
                          <Checkbox
                            checked={text}
                            disabled
                          />
                          <Icon
                            type="show"
                            theme="filled"
                            style={{ cursor: `${text ? 'cursor' : 'not-allowed'}` }}
                            className={styles.checkDetail}
                            title={intl.formatMessage(commonMessage.selectStaff)}
                            onClick={() => {
                              if (!text) return;
                              this.checkDetail(
                                'staff',
                                intl.formatMessage(commonMessage.selectStaff),
                                _.get(record, 'op.userPermission.staffs', [])
                              );
                            }}
                          />
                        </div>
                      ) : '-'
                    }
                  </div>
                );
              }
            },
            {
              align: 'center',
              width: 80,
              title: intl.formatMessage(commonMessage.handler),
              dataIndex: `op.${props.status}.dealerPermissionFlag`,
              render: (text, record) => {
                return (
                  <div className={styles.tdBox}>
                    {
                      record.op.enableDealerpermission ? (
                        <Checkbox
                          checked={text}
                          disabled
                        />
                      ) : '-'
                    }
                  </div>
                );
              }
            },
            {
              align: 'center',
              width: 80,
              title: intl.formatMessage(commonMessage.unlimited),
              dataIndex: `op.${props.status}.noRestrictFlag`,
              render: (text, record) => {
                return (
                  <div className={styles.tdBox}>
                    {
                      record.op.enableNorestrict ? (
                        <Checkbox
                          checked={text}
                          disabled
                        />
                      ) : '-'
                    }
                  </div>
                );
              }
            }
          ]
        },
        {
          align: 'center',
          width: 80,
          title: intl.formatMessage(commonMessage.roleResource),
          dataIndex: 'operate',
          render: (text, record) => {
            return (
              <a onClick={() => {
                this.checkDetail('role', intl.formatMessage(commonMessage.roleResource), _.get(record, 'op.userPermission.roles', []));
              }}
              >
                {intl.formatMessage(commonMessage.check)}
              </a>
            );
          }
        }
      ],
      data: props.data,
      visible: false,
      tagData: [],
      tagWidth: 0
    };
    this.dataList = [];
    this.searchNode = null;
  }

  componentWillMount() {

  }

  componentWillReceiveProps(nextProps) {
    this.setState({
      data: nextProps.data
    });
  }

  onCancel = () => {
    this.setState({
      visible: false
    });
  }

  checkDetail = (type, modelTitle, tagData = []) => {
    let tagWidth = 0;
    let getInfos = null;
    switch (type) {
      case 'position':
        tagWidth = 88;
        getInfos = getPositionInfos;
        break;
      case 'department':
        tagWidth = 88;
        getInfos = getDepartmentInfos;
        break;
      case 'staff':
        tagWidth = 75;
        getInfos = getPersonInfos;
        break;
      case 'role':
        tagWidth = 110;
        break;
      default:
        return false;
    }
    if (type === 'role') {
      this.setModelInfo(tagWidth, modelTitle, tagData);
    } else {
      getInfos({
        ids: tagData.map((item) => item.id).join(',')
      }).then((res) => {
        this.setModelInfo(tagWidth, modelTitle, _.get(res, 'data.list', []));
      });
    }
  }

  setModelInfo = (tagWidth, modelTitle, tagData) => {
    this.setState({
      tagData,
      modelTitle,
      tagWidth,
      visible: true
    });
  }

  render() {
    const {
      columns,
      data,
      visible,
      modelTitle,
      tagData,
      tagWidth
    } = this.state;
    let scroll = {};
    // 36*77为table每行的高度
    if (this.props.contentHeight < data.length * 36.77) {
      scroll = {
        y: this.props.contentHeight
      };
    }
    return (
      <div>
        <Table
          className="extend-table"
          rowKey="id"
          align="center"
          columns={columns}
          dataSource={data}
          bordered
          size="small"
          pagination={false}
          scroll={scroll}
          loading={this.props.loading}
        />
        <Modal
          title={modelTitle}
          destroyOnClose
          visible={visible}
          width={460}
          onCancel={this.onCancel}
          maskClosable={false}
          bodyStyle={{
            padding: '20px 22px',
            maxHeight: '530px',
            minHeight: '134px',
            overflowY: 'auto'
          }}
          footer={[<Button type="primary" onClick={this.onCancel} key="confirm">关闭</Button>]}
        >
          <div>
            {
              tagData.map((item) => {
                return (
                  <Tag key={item.id} className={styles.detailTag} style={{ maxWidth: `${tagWidth}px` }} title={item.name}>{item.name}</Tag>
                );
              })
            }
          </div>
        </Modal>
      </div>
    );
  }
}
