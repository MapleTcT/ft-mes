import React from 'react';
import ReactDom from 'react-dom';
import { injectIntl } from 'react-intl';
import SupTable from 'sup-rc-table';
import './index.less';
// 父子节点展开排序
const sortIndex = (data, index) => {
  const temp = [...data];
  let eq = index;
  for (let i = 0; i < temp.length; i += 1) {
    const item = temp[i];
    eq += 1;
    item.sort = eq;
    const { expendFlag = false, children } = item;
    if (expendFlag && children && children.length > 0) {
      const { sortData, total } = sortIndex(children, eq);
      item.children = sortData;
      eq = total;
    }
  }
  return { sortData: temp, total: eq };
};
@injectIntl
export default class TreeTable extends React.PureComponent {
  constructor(props) {
    super(props);
    const { sortData } = sortIndex(props.dataSource, 0);
    this.state = { dataSource: sortData };
  }

  static getDerivedStateFromProps(nextProps, prevState) {
    if ('dataSource' in nextProps) {
      const { dataSource } = nextProps;
      const { sortData } = sortIndex(dataSource, 0);
      return { ...prevState, dataSource: sortData };
    }
    return null;
  }

  componentDidMount() {
    const tableWrap = ReactDom.findDOMNode(this.tableRef);
    tableWrap.addEventListener('click', this.clickEvent, false);
  }

  componentWillUnmount() {
    const tableWrap = ReactDom.findDOMNode(this.tableRef);
    tableWrap.removeEventListener('click', this.clickEvent, false);
  }

  clickEvent = (e) => {
    const {
      target: { className }
    } = e;
    if (className.indexOf('sup-table-row-expand-icon') > -1) {
      this.updateDataSource(e.target);
    }
  };

  updateDataSource = (target) => {
    const tr = this.closest(target, '.sup-table-row');
    const { rowKey } = tr.dataset;
    const { dataSource } = this.props;
    const transData = this.updateNode(dataSource, rowKey);
    const { sortData } = sortIndex(transData, 0);
    this.setState({ dataSource: [...sortData] });
  };

  // 更新操作节点
  updateNode = (data, key) => {
    const temp = [...data];
    for (let i = 0; i < temp.length; i += 1) {
      const item = temp[i];
      const { rowIndex, expendFlag = false, children } = item;
      if (String(rowIndex) === key) {
        item.expendFlag = !expendFlag;
        break;
      } else if (children && children.length > 0) {
        item.children = this.updateNode(children, key);
      }
    }
    return temp;
  };

  closest = (el, selector) => {
    if (!el || el === null) return el;
    const matchesSelector =
      el.matches ||
      el.webkitMatchesSelector ||
      el.mozMatchesSelector ||
      el.msMatchesSelector;
    while (el) {
      if (matchesSelector.call(el, selector)) {
        break;
      }
      el = el.parentElement;
    }
    return el;
  };

  render() {
    const { dataSource } = this.state;
    return (
      <div className="sup-tree-table">
        <SupTable
          {...this.props}
          dataSource={dataSource}
          ref={(dom) => {
            this.tableRef = dom;
          }}
        />
      </div>
    );
  }
}
