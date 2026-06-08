import React, { PureComponent } from 'react';
import * as _ from 'lodash';
import classnames from 'classnames';
import messages from '../messages';
import InsertChartModulConfig from './InsertChartModulConfig.js';
import styles from '../Reporter.less';
import Modal from './CommonModal';

export default class InsertChartModal extends PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      selectedKey: 'coulmnChart',
      selectedChart: 'columnClustered'
    };
  }

  handleOk = () => {
    const { selectedChart } = this.state;
    if (selectedChart) {
      this.props.addChart(selectedChart);
    }
    this.props.onOpenChartModal();
  }

  selectMenu = (key) => {
    this.setState({
      selectedKey: key,
      selectedChart: InsertChartModulConfig[key].list[0].chartType
    });
  }

  selectChartType = (type) => {
    this.setState({
      selectedChart: type
    });
  }

  render() {
    const { showInsertChartModal, onOpenChartModal, intl } = this.props;
    const { selectedKey, selectedChart } = this.state;
    return (
      <Modal
        destroyOnClose
        width="610px"
        bodyStyle={{ height: '315px' }}
        visible={showInsertChartModal}
        title={intl.formatMessage(messages.CellFormats)}
        onOk={this.handleOk}
        onCancel={onOpenChartModal}
        okText={intl.formatMessage(messages.ok)}
        cancelText={intl.formatMessage(messages.cancel)}
      >
        <div className={styles['chart-modal-content']}>
          <div className={styles['chart-classify-list']}>
            {
              _.map(_.values(InsertChartModulConfig), (item) => (
                <div
                  style={{ padding: '8px 8px 0 8px', cursor: 'pointer' }}
                  onClick={this.selectMenu.bind(this, item.key)}
                  key={item.key}
                >
                  <div
                    className={classnames(styles['menu-item-content'], {
                      [styles['menu-selected']]: selectedKey === item.key
                    })}
                  >
                    <div className={classnames(styles[item.icon], styles['menu-icon'])} />
                    <span style={{ lineHeight: '35px' }}>
                      {Object.prototype.toString.call(item.title) !== '[object Object]' ? item.title : intl.formatMessage(item.title)}
                    </span>
                  </div>
                </div>
              ))
            }
          </div>
          <div className={styles['chart-list']}>
            <div style={{ fontSize: 16, marginBottom: 12 }}>
              {Object.prototype.toString.call(InsertChartModulConfig[selectedKey].title) !== '[object Object]'
                ? InsertChartModulConfig[selectedKey].title
                : intl.formatMessage(InsertChartModulConfig[selectedKey].title)}
            </div>
            <div className={styles['chart-pic-box']}>
              {
                InsertChartModulConfig[selectedKey].list.map((item) => (
                  <div
                    key={item.chartType}
                    className={classnames(styles[item.pic], styles['chart-pic'], {
                      [styles['selected-chart']]: selectedChart === item.chartType
                    }
                    )}
                    onClick={this.selectChartType.bind(this, item.chartType)}
                  />
                ))
              }
            </div>
          </div>
        </div>
      </Modal>
    );
  }
}
