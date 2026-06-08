import { injectIntl } from 'react-intl';
import React from 'react';
import { Modal, Select, message } from 'sup-ui';
import SortModal from '../component/SortModal';
import messages from '../messages';
import { getNextSortRowData, isSingleKeyObj } from '../utils';
import style from '../style.less';

const { Option } = Select;

class ViewSortModal extends React.Component {
  constructor(props) {
    super(props);
    const { intl } = props;

    this.state = {
      sortModalVisible: false,
      sortModalProps: {
        visible: true,
        title: intl.formatMessage(messages.sortModalTitle),
        onCancel: this.hideModal
      }
    };
  }

  showSortModal = () => {
    const { modelCode } = this.props;
    const { intl } = this.props;
    if (!modelCode) {
      return message.info(intl.formatMessage(messages.chooseSortObject));
    }
    this.setState({
      sortModalVisible: true
    });
  };

  hideSortModal() {
    this.setState({
      sortModalVisible: false
    });
  }

  hideModal = () => {
    this.hideSortModal();
    this.props.hideSelectModal();
  };

  getSelectModels() {
    // FIXME 考虑优化
    const { sortData } = this.props;
    return Object.keys(sortData).map((key) => {
      const d = sortData[key];
      return (
        <Option key={d.code} title={d.displayNameInternational} value={d.code}>
          {d.displayNameInternational}
        </Option>
      );
    });
  }

  getSelectSortData() {
    const { sortData, modelCode } = this.props;
    return sortData[modelCode].children;
  }

  handleSortRow = (dragIndex, hoverIndex) => {
    const { sortData, modelCode } = this.props;
    const sortDataChildren = sortData[modelCode].children;
    const nextSortDataChildren = getNextSortRowData(
      sortDataChildren,
      dragIndex,
      hoverIndex
    );
    sortData[modelCode].children = nextSortDataChildren;
    this.props.handleSortRow(sortData);
  };

  handleSaveSort = () => {
    const { modelCode } = this.props;
    return this.props.handleSaveSort(modelCode).then(() => {
      this.hideModal();
    });
  };

  isSingleModel() {
    const { sortData } = this.props;
    return isSingleKeyObj(sortData);
  }

  render() {
    const { visible, intl, modelCode } = this.props;
    const { sortModalProps, sortModalVisible } = this.state;
    // 当前是否打开
    return visible ? (
      <>
        {sortModalVisible || this.isSingleModel() ? ( // 只有一个模型时直接打开, 不选择
          <SortModal
            sortData={this.getSelectSortData()}
            modalProps={sortModalProps}
            handleSortRow={this.handleSortRow}
            handleSaveSort={this.handleSaveSort}
          />
        ) : (
          <Modal
            className={style.modelSelectModal}
            visible
            title={intl.formatMessage(messages.chooseSortObject)}
            width={300}
            onOk={this.showSortModal}
            onCancel={this.hideModal}
          >
            <Select
              value={modelCode}
              style={{ width: 200 }}
              onChange={this.props.handleChangeModel}
            >
              {this.getSelectModels()}
            </Select>
          </Modal>
        )}
      </>
    ) : null;
  }
}

export default injectIntl(ViewSortModal);
