/* eslint-disable */
import React from 'react';
import { injectIntl } from 'react-intl';
import { Layout, Table, Icon, Checkbox } from 'sup-ui';

const { Column, ColumnGroup } = Table;
const { Content } = Layout;

const operatesCols = ['posLimit', 'depLimit'];

const checkboxColumns = [
  {
    title: '岗位限制',
    key: 'posLimit',
    isSelectCheckbox: false
  },
  {
    title: '部门限制',
    key: 'depLimit',
    isSelectCheckbox: true
  }
];

class ContentBody extends React.Component {
  state = {
    operates: [
      {
        menuId: 1,
        id: 31,
        menuName: '事由',
        operateName: '查询',
        posLimit: {
          checked: true,
          disabled: false
        },
        depLimit: {
          checked: true,
          disabled: false
        },
        unlimited: {
          checked: false,
          disabled: true
        },
        rowSpan: 1
      },
      {
        menuId: 2,
        id: 32,
        menuName: '报销单',
        operateName: '新增',
        posLimit: {
          checked: true,
          disabled: false
        },
        depLimit: {
          checked: true,
          disabled: false
        },
        unlimited: {
          checked: false,
          disabled: true
        },
        rowSpan: 2
      },
      {
        menuName: '报销单',
        menuId: 2,
        id: 33,
        operateName: '删除',
        // 无权限设置
        // posLimit: {
        //   checked: true,
        //   disabled: false
        // },
        depLimit: {
          checked: false,
          disabled: false
        },
        unlimited: {
          checked: false,
          disabled: false
        },
        rowSpan: 0
      }
    ],
    menus: {
      // 行号
      1: [0],
      2: [1, 2]
    },
    headers: {
      posLimit: [0, 1],
      depLimit: [0, 1, 2]
    }
  };

  // 获取头部选择框属性
  getHeaderCheckboxProps(columnKey) {
    const { headers, operates } = this.state;

    const operateRows = headers[columnKey];
    const props = { checked: false };

    if (operateRows.length) {
      props.checked = true;

      for (let i = 0; i < operateRows.length; i++) {
        const operate = operates[operateRows[i]];

        const value = operate[columnKey];

        if (value.checked) {
          props.indeterminate = true;
          if (!props.checked) {
            break;
          }
        }
        if (!value.checked) {
          props.checked = false;
        }
      }
    }
    if (props.checked && props.indeterminate) {
      props.indeterminate = false;
    }
    return props;
  }

  // 处理表头权限变化事件
  handleHeadCheckedStatusChange(columnKey, checked) {
    const { headers, operates } = this.state;
    const operateRows = headers[columnKey];
    for (const i of operateRows) {
      operates[operateRows[i]][columnKey].checked = checked;
    }
    this.setState({
      operates
    });
  }

  // 表头显示
  renderHeadCheckbox(title, columnKey) {
    //FIXME
    if (['menuName', 'selectAll', 'unlimited'].indexOf(columnKey) >= 0) {
      return () => {
        return <Checkbox>{title}</Checkbox>;
      };
    }
    return () => {
      return (
        <Checkbox
          {...this.getHeaderCheckboxProps(columnKey)}
          onChange={(e) => {
            this.handleHeadCheckedStatusChange(columnKey, e.target.checked);
          }}
        >
          {title}
        </Checkbox>
      );
    };
  }

  // 处理普通权限变化事件
  handleCheckboxChange(checked, key, index) {
    const { operates } = this.state;
    operates[index][key].checked = checked;
    operates[index].unlimited.disabled = checked;
    this.setState({
      operates
    });
  }

  // 显示普通权限checkbox
  renderCheckbox(column, value, row, index) {
    const { isSelectCheckbox, key } = column;
    if (!value) return '-';
    return (
      <span>
        <Checkbox
          onChange={(e) => {
            this.handleCheckboxChange(e.target.checked, key, index);
          }}
          {...row[key]}
        />
        {isSelectCheckbox && <Icon style={{ marginLeft: 10 }} type="plus" />}
      </span>
    );
  }

  renderCheckboxColumn(column) {
    const { key, title } = column;

    return (
      <Column
        title={this.renderHeadCheckbox(title, key)}
        render={this.renderCheckbox.bind(this, column)}
        dataIndex={key}
        key={key}
      />
    );
  }

  // 处理无限制checkbox选中事件
  handleUnlimitedCheckboxChange = (checked, index) => {
    const { operates } = this.state;
    const operate = operates[index];

    operate.unlimited.checked = checked;

    // 统一设置其他权限不能编辑
    for (const col of operatesCols){
      if (operate[col]){
        operate[col].disabled = checked;
      }
    }

    this.setState({
      operates
    });
  }

  // 显示无限制checkbox
  renderUnlimitedCheckbox = (_, row, index) => {
    return (
      <span>
        <Checkbox
          onChange={(e) => {
            this.handleUnlimitedCheckboxChange(e.target.checked, index);
          }}
          {...row.unlimited}
        />
      </span>
    );
  }

  // 处理菜单选择事件
  handleMenuCheckboxOnChange(checked, menuId) {
    const { menus, operates } = this.state;
    const operatesRows = menus[menuId];
    for (const rowIndex of operatesRows) {
      const operate = operates[rowIndex];
      for (const col of operatesCols) {
        if (operate[col]) {
          operate[col].checked = checked;
        }
      }
      // 无限制禁止选中
      operate.unlimited.disabled = checked;
      // FIXME
      operate.unlimited.checked = !checked;
    }
    this.setState({
      operates
    });
  }

  // 获取菜单选择属性
  getMenuOpeartorCheckedStatus(menuId) {
    const { menus, operates } = this.state;
    const operatesRows = menus[menuId];
    const props = { checked: false };

    if (operatesRows.length) {
      props.checked = true;
      // 每行操作
      let finishied = false;

      for (let i = 0; i < operatesRows.length; i++) {
        const opearte = operates[operatesRows[i]];
        if (finishied) {
          break;
        }

        // 每个具体权限
        for (let ii = 0; ii < operatesCols.length; ii++) {
          const value = opearte[operatesCols[ii]];
          if (typeof value !== 'undefined') {
            if (value.checked) {
              props.indeterminate = true;
              if (!props.checked) {
                finishied = true;
                break;
              }
            }
            if (!value.checked) {
              props.checked = false;
            }
          }
        }
      }
    }
    if (props.checked && props.indeterminate) {
      props.indeterminate = false;
    }
    return props;
  }

  // 显示菜单
  menuSelectCheckboxRender = (value, row) => {
    const props = this.getMenuOpeartorCheckedStatus(row.menuId);

    const obj = {
      children: (
        <Checkbox
          {...props}
          onChange={(e) => {
            this.handleMenuCheckboxOnChange(e.target.checked, row.menuId);
          }}
        >
          {value}
        </Checkbox>
      ),
      props: {
        rowSpan: row.rowSpan
      }
    };
    return obj;
  };

  render() {
    const { operates } = this.state;

    return (
      <Content style={{ backgroundColor: '#fff' }}>
        <Table rowKey="id" dataSource={operates} pagination={false} bordered>
          <Column
            title={this.renderHeadCheckbox('菜单', 'menuName')}
            dataIndex="menuName"
            render={this.menuSelectCheckboxRender}
          />
          <Column title="操作项" key="operateName" dataIndex="operateName" />
          <ColumnGroup title="数据权限">
            {checkboxColumns.map((column) => {
              return this.renderCheckboxColumn(column);
            })}
            <Column
              title={this.renderHeadCheckbox('无限制', 'unlimited')}
              key="unlimited"
              render={this.renderUnlimitedCheckbox}
            />
          </ColumnGroup>
          <Column
            title={this.renderHeadCheckbox('全选', 'selectAll')}
            key="selectAll"
          />
        </Table>
      </Content>
    );
  }
}

export default injectIntl(ContentBody);
