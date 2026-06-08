/*
 * @Author: DWP
 * @Date: 2020-08-05 09:32:05
 * @LastEditors: DWP
 * @LastEditTime: 2020-08-07 09:35:12
 */
import React from 'react';
import { render } from 'react-dom';
import '@babel/polyfill';
import {
  HashRouter as Router,
  Switch,
  Route,
  Redirect
} from 'react-router-dom';

import { SupIntlProvider } from 'sup-rc-utility';

import './index.less';
import 'sup-ui/dist/sup-ui.less';
import 'sup-rc-table/dist/index.less';
import 'sup-rc-reference/dist/index.less';
import 'sup-rc-resize/dist/index.css';
import 'sup-rc-tree/dist/index.less';
import 'sup-rc-search/dist/index.less';
import routes from './router';
import changeTheme from './theme/changeTheme.js';

// import './mock/user';
if (process.env.NODE_ENV !== 'production') {
  // eslint-disable-next-line global-require
  // require('./mock/user');
  // eslint-disable-next-line global-require
  // require('./mock/role');
  // eslint-disable-next-line global-require
  // require('./mock/auth');
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
  <SupIntlProvider moduleCode="rbac">
    <Router>
      <Switch>
        <Route exact path="/">
          <Redirect to="/user" />
        </Route>
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
