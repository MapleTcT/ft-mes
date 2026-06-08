import React from 'react';
import { injectIntl } from 'react-intl';
import { Layout } from 'sup-ui';
import SiderTree from './SiderTree';
import ConfigBody from './ConfigBody';

class Auth extends React.Component {
  render() {
    return (
      <Layout style={{ height: '100%' }}>
        <SiderTree />
        <ConfigBody />
      </Layout>
    );
  }
}

export default injectIntl(Auth);
