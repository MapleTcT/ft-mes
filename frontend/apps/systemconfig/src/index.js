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

import { message } from 'sup-ui';

import './index.css';
import 'sup-ui/dist/sup-ui.less';
import 'sup-rc-tree/dist/index.less';
import 'sup-rc-table/dist/index.less';
import routes from './router';
import changeTheme from './theme/changeTheme.js';

// 全局弹框提示配置
message.config({
  maxCount: 1
});

// import './mock/user';
if (process.env.NODE_ENV !== 'production') {
  // eslint-disable-next-line global-require
  // require('./mock/config-catalog');
  // require('./mock/i18n');
}

changeTheme();

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
  <SupIntlProvider moduleCode="systemConfig">
    <Router>
      <Switch>
        <Route exact path="/">
          <Redirect to="/sysconfig" />
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
