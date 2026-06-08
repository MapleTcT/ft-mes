import React from 'react';
import { injectIntl } from 'react-intl';
import SupTree from 'sup-rc-tree';
// import { Icon, Menu, Dropdown, Modal, message } from 'sup-ui';
// import { getPortalInfoList } from '../../services/portal.js';
import defaultMessages from './messages.js';
import { menuList } from '../../mock/portal.js';

@injectIntl
export default class Sider extends React.PureComponent {
  constructor(props) {
    super(props);
    // const { intl } = props;
    this.state = {
      treeData: []
    };
  }

  componentWillMount() {
    const { list } = menuList;
    const treeData = list.map(({ code, id, name }) => ({
      code,
      title: name,
      key: id
    }));
    this.setState({
      treeData: [
        {
          key: 'root',
          code: 'root',
          title: '模块列表',
          children: treeData
        }
      ]
    });
  }

  onSelect = (keys, e) => {
    this.selectedMenu = e.node.props.dataRef;
    this.resCode = this.selectedMenu.resCode;
  };

  render() {
    const { treeData } = this.state;
    const { intl } = this.props;
    return (
      <SupTree
        placeholder={intl.formatMessage(
          defaultMessages.portalTreeSearchPlaceholder
        )}
        // className={style.supTree + noBorderCls}
        // selectedKeys={selectRoleKeys}
        defaultExpandAll
        showSearch
        switchCompany={false}
        // showAdd={enableAddRole}
        onAdd={this.handleAddRoleButtonClick}
        onSelectCompany={this.handleCompanyTreeChange}
        dataSource={treeData}
        onSelect={this.onSelect}
        autoExpandRoot
        optRender={this.optRender}
        // fuzzyParams={{
        //   url: getSearchRoleByKeywordAPI(),
        //   param: 'keyword',
        //   callback: ({ list }) => {
        //     return list.map(({ code, name }) => {
        //       return {
        //         key: code,
        //         title: name
        //       };
        //     });
        //   }
        // }}
      />
    );
  }
}
