import React from 'react';
import { injectIntl } from 'react-intl';
import SupResize from 'sup-rc-resize';
import { message } from 'sup-ui';
import {
  updateModelManageField,
  sortModelFields
} from 'root/services/customProperty';
import Tree from './Tree';
import List from './List';
import Modal from './Modal';
import SortModal from '../component/SortModal';
import styles from '../style.less';
import { MODEL_ROOT } from '../constant';
import messages from '../messages';
import { getNextSortRowData } from '../utils';

class CfModelManage extends React.Component {
  constructor(props) {
    super(props);

    const { intl } = props;

    this.state = {
      selectModel: MODEL_ROOT,
      formData: {},
      editModalProps: {
        visible: false,
        title: intl.formatMessage(messages.editModalTitle),
        onCancel: this.hideEditModal
      },
      sortModalProps: {
        visible: false,
        title: intl.formatMessage(messages.sortModalTitle),
        onCancel: this.hideSortModal
      },
      sortData: []
    };
  }

  handleSaveForm = (data) => {
    return updateModelManageField(data).then(() => {
      const { intl } = this.props;
      message.success(intl.formatMessage(messages.updateSuccess));
      // 刷新列表
      this.hideModal('editModalProps', () => {
        this.list.refreshList();
      });
    });
  };

  handleClickModel = (modelCode) => {
    this.setState({
      selectModel: modelCode
    });
  };

  showModal = (data, type) => {
    this.setState((d) => {
      d[type].visible = true;
      d = Object.assign(d, data);
      return d;
    });
  };

  hideModal = (type, cb) => {
    this.setState((d) => {
      d[type].visible = false;
      return d;
    }, cb);
  };

  showEditModal = (formData) => {
    this.showModal({ formData }, 'editModalProps');
  };

  showSortModal = (sortData) => {
    this.showModal({ sortData }, 'sortModalProps');
  };

  hideEditModal = () => {
    this.hideModal('editModalProps');
  };

  hideSortModal = () => {
    this.hideModal('sortModalProps');
  };

  handleSortRow = (dragIndex, hoverIndex) => {
    const { sortData } = this.state;
    const nextSortData = getNextSortRowData(sortData, dragIndex, hoverIndex);
    this.setState({
      sortData: nextSortData
    });
  };

  handleSaveSort = () => {
    const { sortData } = this.state;
    const { intl } = this.props;
    const data = sortData.map((d) => d.id);
    return sortModelFields(data)
      .then(() => {
        this.hideSortModal();
        this.list.refreshList().then(() => {
          message.success(intl.formatMessage(messages.sortSuccess));
        });
      })
      .catch((err) => {
        message.error(intl.formatMessage(messages.sortFail));
        throw new Error(err);
      });
  };

  render() {
    const {
      selectModel,
      editModalProps,
      formData,
      sortData,
      sortModalProps
    } = this.state;
    return (
      <div className={styles.layout}>
        <SupResize>
          <Tree handleClickModel={this.handleClickModel} />
          <List
            ref={(ref) => {
              this.list = ref;
            }}
            key={selectModel}
            selectModel={selectModel}
            editModal={this.showEditModal}
            sortModal={this.showSortModal}
          />
        </SupResize>
        <Modal
          handleSaveForm={this.handleSaveForm}
          modalProps={editModalProps}
          formData={formData}
        />
        <SortModal
          modalProps={sortModalProps}
          handleSortRow={this.handleSortRow}
          handleSaveSort={this.handleSaveSort}
          sortData={sortData}
        />
      </div>
    );
  }
}

export default injectIntl(CfModelManage);
