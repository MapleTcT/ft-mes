import React from 'react';
import { injectIntl } from 'react-intl';
import EditForm from './EditForm';
import { getLoginInfo, getStaffInfo } from '../../services/changePass';

class CustomForm extends React.Component {
  constructor(props) {
    super(props);
    this.editForm = React.createRef();
    this.state = {
      staffInfo: {},
      isloading: false
    }
  }

  componentWillMount() {
    this.setState({
      isloading: true
    })
    getLoginInfo().then((res) => {
      const { data: { staff: { id }}} = res.data;
      getStaffInfo({personId: id}).then((rese) => {
        this.setState({
          staffInfo: rese.data.data,
          isloading: false
        });
      })
    });
  }

  render() {
    if(this.state.isloading) return null;
    return (
      <div>
        <EditForm
          ref={this.editForm}
          staffInfo={this.state.staffInfo}
        />
      </div>
    );
  }
}

export default injectIntl(CustomForm);
