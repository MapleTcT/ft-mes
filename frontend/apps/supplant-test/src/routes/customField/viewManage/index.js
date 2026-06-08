import React from 'react';
import { injectIntl } from 'react-intl';
import SupResize from 'sup-rc-resize';
import {
  updateViewManageField,
  sortViewFields
} from 'root/services/customProperty';
import { message } from 'sup-ui';
import styles from '../style.less';
import Tree from './Tree';
import List from './List';
import Modal from './Modal';
import ViewSortModal from './ViewSort';
import messages from '../messages';
import { VIEW_ROOT } from '../constant';
import { isSingleKeyObj } from '../utils';

class CfViewManage extends React.Component {
  constructor(props) {
    super(props);

    const { intl } = props;

    this.state = {
      selectView: VIEW_ROOT,
      formData: {},
      sortData: [],
      sortModelCode: null,
      sortVisible: false,
      modalProps: {
        visible: false,
        title: intl.formatMessage(messages.editModalTitle),
        onCancel: this.handleCancelModal
      }
    };
  }

  handleChangeModel = (value) => {
    this.setState({
      sortModelCode: value
    });
  };

  handleClickView = (viewCode) => {
    this.setState({
      selectView: viewCode
    });
  };

  handleCancelModal = () => {
    this.hideModal();
  };

  hideModal = (cb) => {
    this.setState((d) => {
      d.modalProps.visible = false;
      return d;
    }, cb);
  };

  editModal = (data) => {
    this.setState((d) => {
      d.modalProps.visible = true;
      d.formData = data;
      return d;
    });
  };

  showSelectModal = (sortData) => {
    const nextState = {
      sortData,
      sortVisible: true,
      sortModelCode: null
    };
    if (isSingleKeyObj(sortData)) {
      // 只有一个模型时不弹出选择窗口
      const [sortModelCode] = Object.keys(sortData);
      nextState.sortModelCode = sortModelCode;
    }
    this.setState(nextState);
  };

  handleSortRow = (sortData) => {
    this.setState({
      sortData
    });
  };

  hideSelectModal = () => {
    this.setState({
      sortVisible: false
    });
  };

  handleSaveSort = () => {
    const { sortData, sortModelCode } = this.state;
    const sortIds = sortData[sortModelCode].children.map((d) => d.id);
    const { intl } = this.props;
    return sortViewFields(sortIds)
      .then(() => {
        this.list.refreshList().then(() => {
          message.success(intl.formatMessage(messages.sortSuccess));
        });
      })
      .catch((err) => {
        message.error(intl.formatMessage(messages.sortFail));
        throw new Error(err);
      });
  };

  handleSaveForm = (data) => {
    return updateViewManageField(data).then(() => {
      const { intl } = this.props;
      message.success(intl.formatMessage(messages.updateSuccess));
      // 刷新列表
      this.hideModal(() => {
        this.list.refreshList();
      });
    });
  };

  render() {
    const {
      selectView,
      modalProps,
      formData,
      sortData,
      sortVisible,
      sortModelCode
    } = this.state;
    return (
      <div className={styles.layout}>
        <SupResize>
          <Tree handleClickView={this.handleClickView} />
          <List
            ref={(ref) => {
              this.list = ref;
            }}
            key={selectView}
            selectView={selectView}
            editModal={this.editModal}
            sortModal={this.showSelectModal}
          />
        </SupResize>
        <Modal
          handleSaveForm={this.handleSaveForm}
          modalProps={modalProps}
          formData={formData}
        />
        <ViewSortModal
          visible={sortVisible}
          sortData={sortData}
          modelCode={sortModelCode}
          handleChangeModel={this.handleChangeModel}
          handleSortRow={this.handleSortRow}
          handleSaveSort={this.handleSaveSort}
          hideSelectModal={this.hideSelectModal}
        />
      </div>
    );
  }
}

export default injectIntl(CfViewManage);
