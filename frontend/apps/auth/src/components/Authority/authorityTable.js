import React from 'react';
import {
  Table,
  Checkbox,
  Button,
  message
} from 'sup-ui';
import { injectIntl } from 'react-intl';
import _ from 'lodash';
import { getPersonInfos, getPositionInfos, getDepartmentInfos } from 'root/services/authority';
import { SupReferenceView } from 'sup-rc-reference';
// import Edit from './edit';
import styles from './styles.less';
import commonMessage from './messages';

@injectIntl
export default class AuthorTable extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;
    this.state = {
      viewVisible: false,
      type: 'staff',
      key: props.status,
      defaultOption: [],
      // x: 1500,
      companyId: null,
      columns: [
        {
          width: 80,
          title: () => {
            const init = this.titleCheck('menu');
            return (
              <div style={{ marginLeft: 12 }}>
                <Checkbox
                  style={{ marginRight: 8 }}
                  {...init}
                  onChange={(e) => { this.titleChange(e.target.checked, 'menu'); }}
                />
                {intl.formatMessage(commonMessage.menu)}
              </div>
            );
          },
          dataIndex: 'nameDisplay',
          id: 'name',
          render: (text, record, index) => {
            const filterObj = this.state.data.filter((item) => item.menuInfoId === record.menuInfoId);
            const { length } = filterObj;
            const some = filterObj.some((item) => _.get(item, 'choose', false));
            const every = filterObj.every((item) => _.get(item, 'choose', false));
            const obj = {
              children: (
                <div className={styles.tdBox} title={text} style={{ marginLeft: 12 }}>
                  <Checkbox
                    style={{ marginRight: 8 }}
                    checked={every}
                    indeterminate={some && !every}
                    onChange={(e) => { this.menuCheck(e.target.checked, record, text); }}
                  />
                  {text}
                </div>
              ),
              props: {
                rowSpan: 1
              }
            };
            if (length > 1) {
              const RowIndex = this.state.data.findIndex((item) => item.menuInfoId === record.menuInfoId);
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
          title: () => {
            return <div style={{ marginLeft: 12 }}>{intl.formatMessage(commonMessage.operation)}</div>;
          },
          width: 80,
          id: 'op',
          dataIndex: 'op.nameDisplay',
          render: (text) => {
            return <div className={styles.tdBox} title={text} style={{ marginLeft: 12 }}>{text}</div>;
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
              title: () => {
                const titleDisabled = this.state.data.some((item) => _.get(item, 'op.enablePosrestrict', false));
                const init = this.titleCheck(`op.${props.status}.positionFlag`, 'op.enablePosrestrict', 'authority');
                return (
                  <div>
                    <Checkbox
                      style={{ marginRight: 8 }}
                      {...init}
                      disabled={!titleDisabled}
                      onChange={(e) => { this.titleChange(e.target.checked, `op.${props.status}.positionFlag`, 'op.enablePosrestrict', 'authority'); }}
                    />
                    {intl.formatMessage(commonMessage.positionAuthority)}
                  </div>
                );
              },
              dataIndex: `op.${props.status}.positionFlag`,
              render: (text, record) => {
                return (
                  <div className={styles.tdBox}>
                    {
                      record.op.enablePosrestrict ? (
                        <Checkbox
                          checked={text}
                          disabled={_.get(record, `op.${props.status}.noRestrictFlag`, false)}
                          onChange={(e) => { this.changeCheck(e.target.checked, record, `op.${props.status}.positionFlag`); }}
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
              title: () => {
                const titleDisabled = this.state.data.some((item) => _.get(item, 'op.enableDeptrict', false));
                const init = this.titleCheck(`op.${props.status}.departmentFlag`, 'op.enableDeptrict', 'authority');
                return (
                  <div>
                    <Checkbox
                      style={{ marginRight: 8 }}
                      {...init}
                      disabled={!titleDisabled}
                      onChange={(e) => { this.titleChange(e.target.checked, `op.${props.status}.departmentFlag`, 'op.enableDeptrict', 'authority'); }}
                    />
                    {intl.formatMessage(commonMessage.departmentAuthority)}
                  </div>
                );
              },
              dataIndex: `op.${props.status}.departmentFlag`,
              render: (text, record) => {
                return (
                  <div className={styles.tdBox}>
                    {
                      record.op.enableDeptrict ? (
                        <Checkbox
                          checked={text}
                          disabled={_.get(record, `op.${props.status}.noRestrictFlag`, false)}
                          onChange={(e) => { this.changeCheck(e.target.checked, record, `op.${props.status}.departmentFlag`); }}
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
                            disabled={_.get(record, `op.${props.status}.noRestrictFlag`, false)}
                            onChange={(e) => { this.changeCheck(e.target.checked, record, `op.${props.status}.assignPosFlag`); }}
                          />
                          <Button
                            title={intl.formatMessage(commonMessage.selectPosition)}
                            className={styles.plusButton}
                            shape="circle"
                            icon="plus"
                            disabled={_.get(record, `op.${props.status}.noRestrictFlag`, false)}
                            onClick={() => {
                              this.openSupReference('position',
                                record,
                                'positions',
                                `op.${props.status}.assignPosFlag`,
                                intl.formatMessage(commonMessage.positionChoose)
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
                            disabled={_.get(record, `op.${props.status}.noRestrictFlag`, false)}
                            onChange={(e) => { this.changeCheck(e.target.checked, record, `op.${props.status}.assignDeptFlag`); }}
                          />
                          <Button
                            title={intl.formatMessage(commonMessage.selectDepartment)}
                            className={styles.plusButton}
                            shape="circle"
                            icon="plus"
                            disabled={_.get(record, `op.${props.status}.noRestrictFlag`, false)}
                            onClick={() => {
                              this.openSupReference('department',
                                record,
                                'departments',
                                `op.${props.status}.assignDeptFlag`,
                                intl.formatMessage(commonMessage.departmentChoose)
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
                            disabled={_.get(record, `op.${props.status}.noRestrictFlag`, false)}
                            onChange={(e) => { this.changeCheck(e.target.checked, record, `op.${props.status}.assignStaffFlag`); }}
                          />
                          <Button
                            title={intl.formatMessage(commonMessage.selectStaff)}
                            className={styles.plusButton}
                            shape="circle"
                            icon="plus"
                            disabled={_.get(record, `op.${props.status}.noRestrictFlag`, false)}
                            onClick={() => {
                              this.openSupReference('staff',
                                record,
                                'staffs',
                                `op.${props.status}.assignStaffFlag`,
                                intl.formatMessage(commonMessage.staffChoose)
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
              title: () => {
                const titleDisabled = this.state.data.some((item) => _.get(item, 'op.enableDealerpermission', false));
                const init = this.titleCheck(`op.${props.status}.dealerPermissionFlag`, 'op.enableDealerpermission', 'authority');
                return (
                  <div>
                    <Checkbox
                      style={{ marginRight: 8 }}
                      {...init}
                      disabled={!titleDisabled}
                      onChange={(e) => { this.titleChange(e.target.checked, `op.${props.status}.dealerPermissionFlag`, 'op.enableDealerpermission', 'authority'); }}
                    />
                    {intl.formatMessage(commonMessage.handler)}
                  </div>
                );
              },
              dataIndex: `op.${props.status}.dealerPermissionFlag`,
              render: (text, record) => {
                return (
                  <div className={styles.tdBox}>
                    {
                      record.op.enableDealerpermission ? (
                        <Checkbox
                          checked={text}
                          disabled={_.get(record, `op.${props.status}.noRestrictFlag`, false)}
                          onChange={(e) => { this.changeCheck(e.target.checked, record, `op.${props.status}.dealerPermissionFlag`); }}
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
              title: () => {
                const init = this.titleCheck(`op.${props.status}.noRestrictFlag`, 'op.enableNorestrict', 'none');
                return (
                  <div>
                    <Checkbox
                      style={{ marginRight: 8 }}
                      {...init}
                      onChange={(e) => { this.titleChange(e.target.checked, `op.${props.status}.noRestrictFlag`, 'op.enableNorestrict', 'none'); }}
                    />
                    {intl.formatMessage(commonMessage.unlimited)}
                  </div>
                );
              },
              dataIndex: `op.${props.status}.noRestrictFlag`,
              render: (text, record) => {
                return (
                  <div className={styles.tdBox}>
                    {
                      record.op.enableNorestrict ? (
                        <Checkbox
                          checked={text}
                          disabled={this.disabledCheck(record)}
                          onChange={(e) => { this.changeCheck(e.target.checked, record, `op.${props.status}.noRestrictFlag`); }}
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
          id: 'choose',
          width: 80,
          title: () => {
            const init = this.titleCheck('choose');
            return (
              <div>
                <Checkbox
                  style={{ marginRight: 8 }}
                  {...init}
                  onChange={(e) => { this.titleChange(e.target.checked, 'choose'); }}
                />
                {intl.formatMessage(commonMessage.allSelected)}
              </div>
            );
          },
          dataIndex: 'choose',
          render: (text, record) => {
            return (
              <div className={styles.tdBox}>
                <Checkbox
                  checked={text}
                  onChange={(e) => { this.changeCheck(e.target.checked, record, 'choose'); }}
                />
              </div>
            );
          }
        }
      ],
      data: props.data
    };
    this.dataList = [];
    this.searchNode = null;
  }

  componentWillMount() {
    const companyJson = localStorage.getItem('loginMsg');
    if (companyJson) {
      this.setState({
        companyId: _.get(JSON.parse(companyJson), 'currentCompany.id', null)
      });
    }
  }

  componentDidMount() {
    // const { columns } = this.state;
    // const numList = []
    //   .concat(columns.filter((x) => x.id !== 'dataAuthority').map((x) => x.width))
    //   .concat(columns.find((x) => x.id === 'dataAuthority').children.map((x) => x.width));
    // const width = numList.reduce((a, b) => a + b);
    // this.setState({
    //   x: width
    // });
  }

  componentWillReceiveProps(nextProps) {
    this.setState({
      data: nextProps.data
    });
  }

  changeCheck = (value, record, key) => {
    _.set(record, key, value);
    // 未分配有全选联动逻辑
    if (this.props.type === 'unassign') {
      if (key !== 'choose') {
        if (!(this.disabledCheck(record) || _.get(record, `op.${this.state.key}.noRestrictFlag`, false))) {
          _.set(record, 'choose', false);
          _.set(record, 'menu', false);
        } else {
          _.set(record, 'choose', true);
          _.set(record, 'menu', true);
        }
      } else {
        this.chooseAll(record, value);
        _.set(record, 'menu', value);
      }
    }
    if (!(this.props.type === 'assign' && key === 'choose') && this.props.refreshButton) {
      this.props.refreshButton();
    }
    if (this.props.refreshDelete) {
      this.props.refreshDelete();
    }
    // 已分配的数据存入修改数据列表中
    if (this.props.type === 'assign' && this.props.storageUpdateList) {
      if (key !== 'choose') {
        _.set(record, 'choose', true);
        _.set(record, 'menu', true);
      }
      const premission = _.get(record, `op.${this.state.key}`, {});
      const tipName = _.get(record, 'op.nameDisplay', '-') === '-'
        ? _.get(record, 'nameDisplay', '') : `${_.get(record, 'nameDisplay', '')}-${_.get(record, 'op.nameDisplay', '')}`;
      this.props.storageUpdateList({
        tipName,
        ...premission
      });
    }
    this.forceUpdate();
  }

  chooseAll = (record, value) => {
    if (!this.disabledCheck(record) && value) {
      _.set(record, `op.${this.state.key}.noRestrictFlag`, true);
    } else if (!value) {
      _.set(record, `op.${this.state.key}.positionFlag`, false);
      _.set(record, `op.${this.state.key}.departmentFlag`, false);
      _.set(record, `op.${this.state.key}.assignPosFlag`, false);
      _.set(record, `op.${this.state.key}.assignDeptFlag`, false);
      _.set(record, `op.${this.state.key}.assignStaffFlag`, false);
      _.set(record, `op.${this.state.key}.dealerPermissionFlag`, false);
      _.set(record, `op.${this.state.key}.noRestrictFlag`, false);
    }
  }

  disabledCheck = (record) => {
    return _.get(record, `op.${this.state.key}.positionFlag`, false)
    || _.get(record, `op.${this.state.key}.departmentFlag`, false)
    || _.get(record, `op.${this.state.key}.assignPosFlag`, false)
    || _.get(record, `op.${this.state.key}.assignDeptFlag`, false)
    || _.get(record, `op.${this.state.key}.assignStaffFlag`, false)
    || _.get(record, `op.${this.state.key}.dealerPermissionFlag`, false);
  }

  openSupReference = (type, record, reKey, checkKey, title) => {
    const { key } = this.state;
    // _.set(record, checkKey, true);
    this.changeCheck(true, record, checkKey);
    const ids = _.get(record, `op.${key}.${reKey}`, []) || [];
    if (ids.length === 0) {
      this.setState({
        type,
        defaultOption: ids,
        viewVisible: true,
        singleData: record,
        reKey,
        title
      });
      return;
    }
    let getInfos = null;
    if (reKey === 'positions') {
      getInfos = getPositionInfos;
    } else if (reKey === 'departments') {
      getInfos = getDepartmentInfos;
    } else if (reKey === 'staffs') {
      getInfos = getPersonInfos;
    }
    getInfos({
      ids: ids.map((item) => item.id).join(',')
    }).then((res) => {
      this.setState({
        type,
        defaultOption: res.data.list,
        viewVisible: true,
        singleData: record,
        reKey,
        title
      });
    });
  }

  payRowAuthority = (data) => {
    const { intl } = this.props;
    const { singleData, reKey, key, type } = this.state;
    _.set(singleData, `op.${key}.${reKey}`, data);
    this.setState({ viewVisible: false });
    if (this.props.refreshDelete) {
      this.props.refreshDelete();
    }
    // 已分配的数据存入修改数据列表中
    if (this.props.type === 'assign' && this.props.storageUpdateList) {
      const premission = _.get(singleData, `op.${key}`, {});
      const tipName = _.get(singleData, 'op.nameDisplay', '-') === '-'
        ? _.get(singleData, 'nameDisplay', '') : `${_.get(singleData, 'nameDisplay', '')}-${_.get(singleData, 'op.nameDisplay', '')}`;
      this.props.storageUpdateList({
        tipName,
        ...premission
      });
    }
    if (data.length > 0) {
      switch (type) {
        case 'position':
          message.success(intl.formatMessage(commonMessage.positionSelected));
          break;
        case 'department':
          message.success(intl.formatMessage(commonMessage.departmentSelected));
          break;
        case 'staff':
          message.success(intl.formatMessage(commonMessage.staffSelected));
          break;
        default:
          return false;
      }
    }
  }

  titleCheck = (key, disabledKey, type) => {
    let some = false;
    let every = false;
    if (disabledKey) {
      if (type === 'authority') {
        const validArr = this.state.data
          .filter((item) => _.get(item, disabledKey, false))
          .filter((item) => !_.get(item, `op.${this.props.status}.noRestrictFlag`, false));
        some = validArr.length > 0
          ? validArr.some((item) => _.get(item, key, false))
          : false;
        every = validArr.length > 0
          ? validArr.every((item) => _.get(item, key, false))
          : false;
      } else if (type === 'none') {
        const validArr = this.state.data
          .filter((item) => _.get(item, disabledKey, false))
          .filter((item) => !this.disabledCheck(item));
        some = validArr.length > 0
          ? validArr.some((item) => _.get(item, key, false))
          : false;
        every = validArr.length > 0
          ? validArr.every((item) => _.get(item, key, false))
          : false;
      }
    } else {
      some = this.state.data.length > 0 ? this.state.data.some((item) => _.get(item, key, false)) : false;
      every = this.state.data.length > 0 ? this.state.data.every((item) => _.get(item, key, false)) : false;
    }
    return {
      checked: every,
      indeterminate: some && !every
    };
  }

  titleChange = (value, key, disabledKey, type) => {
    this.state.data.forEach((item) => {
      if (key === 'menu' || key === 'choose') {
        if (this.props.type === 'unassign') {
          this.chooseAll(item, value);
        }
        _.set(item, 'menu', value);
        _.set(item, 'choose', value);
      } else if (disabledKey && _.get(item, disabledKey, false)) {
        if (type === 'authority' && !_.get(item, `op.${this.props.status}.noRestrictFlag`, false)) {
          // 岗位权限,部门限制,处理人勾选,判定是否都为空,都为空则取消菜单和全选勾选
          // _.set(item, key, value);
          this.changeCheck(value, item, key);
          // 未分配列表,取消影响全选和菜单勾选
          if (this.props.type === 'unassign' && (value || (!value && !this.disabledCheck(item)))) {
            _.set(item, 'menu', value);
            // _.set(item, 'choose', value);
            this.changeCheck(value, item, 'choose');
          } else if (this.props.type === 'assign' && value) {
            _.set(item, 'menu', value);
            // _.set(item, 'choose', value);
            this.changeCheck(value, item, 'choose');
          }
        } else if (type === 'none' && !this.disabledCheck(item)) {
          // _.set(item, key, value);
          // _.set(item, 'menu', value);
          // _.set(item, 'choose', value);
          // this.changeCheck(value, item, 'choose');
          // this.titleChange(value, `op.${this.props.status}.noRestrictFlag`, 'op.enableNorestrict', 'none');
          this.changeCheck(value, item, key);
        }
      }
    });
    if (this.props.refreshDelete) {
      this.props.refreshDelete();
    }
    this.forceUpdate();
  }

  menuCheck = (value, record, key) => {
    this.state.data
      .filter((item) => item.nameDisplay === key)
      .forEach((item) => {
        // 如果勾选状态,且其他未勾选,默认勾选无限制
        // if (value && !this.disabledCheck(item) && this.props.type === 'unassign') {
        //   _.set(item, `op.${this.props.status}.noRestrictFlag`, value);
        // }
        // _.set(item, 'choose', value);
        this.changeCheck(value, item, 'choose');
        _.set(item, 'menu', value);
      });
    // _.set(record, 'menu', value);
    if (this.props.refreshDelete) {
      this.props.refreshDelete();
    }
    this.forceUpdate();
  }

  render() {
    const {
      columns,
      data,
      viewVisible,
      type,
      defaultOption,
      // x,
      companyId,
      title
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
          ref={(node) => { this.table = node; }}
          className="authority-table"
          rowKey="id"
          align="center"
          columns={columns}
          dataSource={data}
          bordered
          size="small"
          pagination={false}
          scroll={scroll}
        />
        {
          viewVisible ? (
            <SupReferenceView
              multiple
              title={title}
              destroyOnClose
              visible={viewVisible}
              selectedRowKeys={defaultOption.map((item) => item.code)}
              selectedRows={defaultOption}
              type={type}
              companyConfig={{
                disabled: true,
                parentId: Number(companyId)
              }}
              height="600px"
              onOk={this.payRowAuthority}
              onCancel={() => { this.setState({ viewVisible: false }); }}
            />
          ) : null
        }
      </div>
    );
  }
}
