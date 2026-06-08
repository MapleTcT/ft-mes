// 为了冻结自适应, 独立为一个单独组件
import SupTable from 'sup-rc-table';
import React from 'react';

export default class GroupContentTable extends React.Component {
  componentDidMount() {
    if (this.table && this.table.changeOperationFixedStatus) {
      this.table.changeOperationFixedStatus();
    }
  }

  render() {
    const { props } = this;
    return (
      <SupTable
        ref={(ref) => {
          this.table = ref;
        }}
        {...props}
      />
    );
  }
}
