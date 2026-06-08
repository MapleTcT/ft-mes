import React from 'react';
import { injectIntl } from 'react-intl';
import SupTree from 'sup-rc-tree';
import { signTree } from '../../services/eSign';
import style from './style.less';
import { toFormData } from './utils';
import messages from './messages';

class EsignSider extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      dataSource: [],
      selectedKeys: []
    };
  }

  componentDidMount = () => {
    this.initTree();
  }

  initTree = () => {
    const data = toFormData({ level: 0 });
    signTree(data).then((res) => {
      const list = res.data.data;
      const newList = list.map((item) => {
        let obj = {};
        obj = _.cloneDeep(item);
        obj.isLeaf = !item.isParent;
        return obj;
      });
      this.setState({
        dataSource: newList
      });
    });
  }

  onSelect = (selectedKeys, params) => {
    const activeName = params.node.props.item.name;
    // const pos = params.node.props.pos.split('-').length - 1;
    const code = selectedKeys[0];
    const { btnDetail } = this.props;
    if (params.node.props.item.isLeaf) {
      btnDetail(code, activeName);
    }
    this.setState({
      selectedKeys
    });
  }

  // eslint-disable-next-line compat/compat
  onLoadData = (treeNode) => new Promise((resolve) => {
    const pos = treeNode.props.pos.split('-').length - 1;
    const { code } = treeNode.props.item;
    let params = {};
    params = {
      level: pos,
      code
    };
    const data = toFormData(params);
    signTree(data).then((res) => {
      const list = res.data.data;
      if (list.length > 0) {
        const newList = list.map((item) => {
          let obj = {};
          obj = _.cloneDeep(item);
          obj.isLeaf = !item.isParent;
          return obj;
        });
        treeNode.props.item.children = newList;
        this.setState({
          // eslint-disable-next-line react/no-access-state-in-setstate
          dataSource: [...this.state.dataSource]
        }, () => {
          resolve();
        });
      } else resolve();
    });
  });

  render() {
    const { dataSource, selectedKeys } = this.state;
    return (
      <div className={style.siderTree}>
        <SupTree
          treeKey="code"
          treeTitle="name"
          showSearch={false}
          placeholder={this.props.intl.formatMessage(messages.searchTreeTips)}
          dataSource={dataSource}
          selectedKeys={selectedKeys}
          onSelect={this.onSelect}
          loadData={this.onLoadData}
        />
      </div>
    );
  }
}

export default injectIntl(EsignSider);
