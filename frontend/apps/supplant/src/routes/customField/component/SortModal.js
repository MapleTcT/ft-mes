import React from 'react';
import { Modal } from 'sup-ui';
import { DndProvider } from 'react-dnd';
import HTML5Backend from 'react-dnd-html5-backend';
import style from '../style.less';
import SortItem from './SortItem';

export default class BaseSortModal extends React.Component {
  state = {
    okButtonProps: {}
  };

  render() {
    const { modalProps, sortData = [], handleSortRow } = this.props;
    const { okButtonProps } = this.state;

    return (
      <Modal
        className={style.sortModal}
        width={400}
        destroyOnClose
        maskClosable={false}
        {...modalProps}
        onOk={this.handleOk}
        okButtonProps={okButtonProps}
        afterClose={() => {
          this.toggleOkBtn(false);
        }}
      >
        <DndProvider backend={HTML5Backend}>
          <ul className={style.sortListWrap}>
            {sortData.map((item, index) => (
              <SortItem
                handleSortRow={handleSortRow}
                item={item}
                index={index}
                key={item.id}
              />
            ))}
          </ul>
        </DndProvider>
      </Modal>
    );
  }

  toggleOkBtn = (disabled) => {
    this.setState((d) => {
      d.okButtonProps.disabled = disabled;
      return d;
    });
  };

  handleOk = () => {
    // TODO 组织排序数据
    this.toggleOkBtn(true);
    this.props.handleSaveSort().catch(() => {
      this.toggleOkBtn(false);
    });
  };
}
