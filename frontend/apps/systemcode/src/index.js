/*
 * @Author: DWP
 * @Date: 2020-07-27 16:59:17
 * @LastEditors: DWP
 * @LastEditTime: 2020-09-08 18:48:24
 */
import React from 'react';
import { render } from 'react-dom';
import '@babel/polyfill';
import { SupIntlProvider } from 'sup-rc-utility';
import {
  HashRouter as Router,
  Switch,
  Route
} from 'react-router-dom';

import './index.css';
import 'sup-ui/dist/sup-ui.less';
import 'sup-rc-tree/dist/index.less';
import 'sup-rc-i18n/dist/index.less';
import 'sup-rc-resize/dist/index.css';
import 'sup-rc-search/dist/index.less';
import routes from './router';
import changeTheme from './theme/changeTheme.js';

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
  <SupIntlProvider moduleCode="systemCode">
    <Router>
      <Switch>
        {routes.map((route, i) => (
          <RouteWithSubRoutes key={`route_${i + 1}`} {...route} routes={i === 0 ? routes : []} />
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
