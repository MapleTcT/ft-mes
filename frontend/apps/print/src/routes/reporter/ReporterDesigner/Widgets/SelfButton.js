import React, { PureComponent } from 'react';
import { Button } from 'sup-ui';

export default class SelfButton extends PureComponent {
  // constructor(props) {
  //   super(props);
  //   this.state = {
  //     value: ''
  //   };
  // }

  handClick = () => {
    const { config: { functionName } } = this.props;
    // const chart = this.props.editChart;
    // if (chart) chart[functionName]();
    this.props.callFunction(functionName);
  }

  render() {
    // const { updataConfig, config: { fatherEvent, event } } = this.props;
    return (
      <Button onClick={this.handClick} size="small"> 转 换 </Button>
    );
  }
}
