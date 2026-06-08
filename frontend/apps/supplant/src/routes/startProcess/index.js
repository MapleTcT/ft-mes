import React from 'react';
import { injectIntl } from 'react-intl';
import './index.less';
import { getStartProcess } from '../../services/process.js';

@injectIntl
export default class startProcess extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      columnNums: 5,
      processList: []
    };
  }

  componentDidMount() {
    getStartProcess().then((res) => {
      if (res.data) {
        this.setState({ processList: res.data.data });
      }
    });
    this.handleResize();
    window.addEventListener('resize', this.handleResize);
  }

  componentWillUnmount() {
    window.removeEventListener('resize', this.handleResize);
  }

  handleResize = () => {
    const { clientWidth: width } = window.document.body;
    const nums = Math.floor(width / 300);
    this.setState({
      columnNums: nums > 1 ? nums : 1
    });
  };

  // 数据分组
  sortData = (processes) => {
    const { columnNums } = this.state;
    // 流程排列辅助变量
    const columnItems = Array(columnNums)
      .fill('')
      .map(() => {
        return { itemNums: 0, totalRows: 0 };
      });
    // 重新排列流程
    const processList = Array(columnNums)
      .fill('')
      .map(() => {
        return [];
      });
    let columnNum = 0;

    for (const e of processes) {
      if (e.key === processes.length) {
        columnNum = columnItems.findIndex((ele) => {
          return (
            ele.totalRows ===
            // eslint-disable-next-line prefer-spread
            Math.min.apply(
              Math,
              columnItems.map((item) => {
                return item.totalRows;
              })
            )
          );
        });
      } else {
        while (
          (columnNum > 0 &&
            columnItems[columnNum].totalRows >=
              columnItems[columnNum - 1].totalRows) ||
          columnItems[columnNum].totalRows >
            columnItems[(columnNum + 1) % columnNums].totalRows
        ) {
          columnNum = (columnNum + 1) % columnNums;
        }
      }

      processList[columnNum].push(e);
      columnItems[columnNum].itemNums = +1;
      columnItems[columnNum].totalRows += e.items.length + 2;
      columnNum = (columnNum + 1) % columnNums;
    }
    return processList;
  };

  handleClick = (url) => window.open(url);

  render() {
    const { processList, columnNums } = this.state;
    const sortData = this.sortData(processList);
    const proportion = Math.floor(100 / columnNums);
    const processLayout = sortData.map((columns, index) => {
      return (
        <div
          className="start-process-colums"
          id={index + 1}
          key={`${index * 1 + 1}`}
          style={{ width: `${proportion}%` }}
        >
          {columns.map((columnItem) => {
            return (
              <div className="process-box">
                <p className="pb-title">{columnItem.name} </p>
                <ul className="pb-list">
                  {columnItem.items.map((e, i) => (
                    <li
                      key={`${i * 1}`}
                      title={e.flowName}
                      onClick={() => this.handleClick(e.openUrl)}
                    >
                      <i className="point" />
                      {e.flowName}
                    </li>
                  ))}
                </ul>
              </div>
            );
          })}
        </div>
      );
    });

    return <div className="sup-start-process">{processLayout}</div>;
  }
}
