import React from 'react';
import { injectIntl } from 'react-intl';
import { message } from 'sup-ui';
import { changePass } from '../../services/changePass';
import EditForm from './EditForm';
import messages from './messages';

class ChangePass extends React.Component {
  constructor(props) {
    super(props);
    this.editForm = React.createRef();
    window.handleChangePass=this.handleChangePass;
  }

  // 修改密码
  handleChangePass = (successCallBack = null, failCallBack = null) => {
    const editForm = this.editForm.current;
    const { intl } = this.props;
    editForm.validateFields().then(
      (data) => {
        changePass(data).then(() => {
          message.success(intl.formatMessage(messages.modifySuccess));
          if (successCallBack) successCallBack();
        }, (err) => {
          console.error(err)
          if (failCallBack) failCallBack();
        })
      }
    );
  }

  render() {
    return (
      <div>
        <EditForm
          ref={this.editForm}
        />
      </div>
    );
  }
}

export default injectIntl(ChangePass);
