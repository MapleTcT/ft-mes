import React from 'react';
import { Icon, Popover } from 'sup-ui';

export default class WithHelpWrapper extends React.Component {
  createContent() {
    const { tip } = this.props;
    return this[`${tip}Tip`]();
  }

  urlTip() {
    return (
      <div>
        <p>url地址，任务调度需要执行的url地址</p>
        <p>范例</p>
        <p>绝对路径：http://ip:port/foundation/user/add</p>
      </div>
    );
  }

  cronTip() {
    return (
      <div>
        <p>cron表达式，任务调度按照何时按照何种频次执行</p>
        <p>范例</p>
        <p>1、0 0 2 1 * ? * 表示在每月的1日的凌晨2点调整任务</p>
        <p>2、0 0 12 ? * WED 表示每个星期三中午12点</p>
        <p>3、每天晚上十二点 ： 0 0 0 * * ? </p>
        <p>4、每天凌晨1点 ： 0 0 1 * * ?</p>
        <p>5、每月1号凌晨1点 ： 0 0 1 1 * ?</p>
      </div>
    );
  }

  paramTip() {
    return (
      <div>
        <p>任务参数，任务调度调用接口url地址所需要的参数</p>
        <p>范例</p>
        <p>userName=admin&dateTime=2020-12-28(具体参数根据实际需要填写)</p>
      </div>
    );
  }

  render() {
    const { label } = this.props;

    return (
      <>
        {label}
        <Popover
          placement="bottomLeft"
          content={this.createContent()}
          title={null}
        >
          <Icon
            onClick={this.showTip}
            type="question-circle"
            style={{
              cursor: 'pointer',
              fontSize: 14,
              marginTop: 4,
              marginLeft: 5
            }}
          />
        </Popover>
      </>
    );
  }
}
