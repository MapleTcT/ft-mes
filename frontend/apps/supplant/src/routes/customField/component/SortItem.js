import React from 'react';
import { DragSource, DropTarget } from 'react-dnd';

class SortItem extends React.Component {
  static dragIndex = -1;

  render() {
    const {
      item,
      connectDragSource,
      connectDropTarget,
      isOver,
      index
    } = this.props;
    let className = '';
    if (isOver) {
      if (index > SortItem.dragingIndex) {
        className = 'drop-over-downward';
      }
      if (index < SortItem.dragingIndex) {
        className = 'drop-over-upward';
      }
    }

    return connectDragSource(
      connectDropTarget(
        <li className={className} key={item.id}>
          {item.displayNameInternational}
        </li>
      )
    );
  }
}

const rowSource = {
  beginDrag(props) {
    SortItem.dragingIndex = props.index;
    return {
      index: props.index
    };
  }
};

const rowTarget = {
  drop(props, monitor) {
    const dragIndex = monitor.getItem().index;
    const hoverIndex = props.index;
    if (dragIndex === hoverIndex) {
      return;
    }
    props.handleSortRow(dragIndex, hoverIndex);
    monitor.getItem().index = hoverIndex;
  }
};

const DragableBodyRow = DropTarget('row', rowTarget, (connect, monitor) => ({
  connectDropTarget: connect.dropTarget(),
  isOver: monitor.isOver()
}))(
  DragSource('row', rowSource, (connect) => ({
    connectDragSource: connect.dragSource()
  }))(SortItem)
);

export default DragableBodyRow;
