import React from 'react';
import { render } from 'react-dom';
import '@babel/polyfill';
import { HashRouter as Router, Switch, Route } from 'react-router-dom';
import { SupIntlProvider } from 'sup-rc-utility';

// import SupTheme from './theme/index.js';
import './index.less';
import 'sup-ui/dist/sup-ui.less';
import 'sup-rc-table/dist/index.less';
import 'sup-rc-reference/dist/index.less';
import 'sup-rc-resize/dist/index.css';
import 'sup-rc-tree/dist/index.less';
import 'sup-rc-search/dist/index.less';
import 'sup-rc-i18n/dist/index.less';

import './theme/color/default.less';

import routes from './router';
import changeTheme from './theme/changeTheme.js';

if (process.env.NODE_ENV !== 'production') {
  /* eslint-disable */
  require('./mock/authority-list');
  require('./mock/portal');
  require('./mock/startProcess');
  /* eslint-enable */
}

changeTheme(); // 切换主题

function RouteWithSubRoutes(route) {
  return (
    <Route
      path={route.path}
      render={(props) => (
        <route.component
          {...props}
          routes={route.routes}
          store={route.store}
          RouteWithSubRoutes={RouteWithSubRoutes}
        />
      )}
    />
  );
}

const App = () => (
  <SupIntlProvider moduleCode="authorityList">
    <Router>
      <Switch>
        {routes.map((route, i) => (
          <RouteWithSubRoutes key={`route_${i + 1}`} {...route} />
        ))}
      </Switch>
    </Router>
  </SupIntlProvider>
);

render(<App />, document.getElementById('root'));

if (module.hot) {
  module.hot.accept('./router', () => {
    render(<App />, document.getElementById('root'));
  });
}
