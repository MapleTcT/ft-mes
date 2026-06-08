import React, { Component } from 'react';
import Modal from '../Modal/CommonModal';
import InsertFormula from './InsertFormula';
import messages from '../messages';

export default class InsertFormulaModal extends Component {
  handleCancel = () => {
    this.props.showOrHideModal(false);
  }

  handleOk = () => {
    this.formula.updateFormula();
    this.handleCancel();
  }

  getModalHeight = (formulaType) => {
    switch (formulaType) {
      case 'RTS': return 400;
      case 'RT/HIS': return 450;
      default: return 350;
    }
  }

  render() {
    const { intl, formulaType, formulaTitle, appId, allDataSource, formula, basicOperate } = this.props;
    return (
      <Modal
        visible
        width="450px"
        bodyStyle={{ height: this.getModalHeight(formulaType) }}
        title={formulaTitle}
        onCancel={this.handleCancel}
        onOk={this.handleOk}
        okText={intl.formatMessage(messages.ok)}
        cancelText={intl.formatMessage(messages.cancel)}
      >
        <InsertFormula
          ref={(node) => { this.formula = node; }}
          appId={appId}
          allDataSource={allDataSource}
          formula={formula}
          basicOperate={basicOperate}
          formulaType={formulaType}
          intl={intl}
          type="insertFormula"
        />
      </Modal>
    );
  }
}
