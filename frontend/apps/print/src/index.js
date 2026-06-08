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
import 'sup-rc-resize/dist/index.css';
import 'sup-rc-tree/dist/index.less';
import 'sup-rc-table/dist/index.less';
import 'sup-rc-i18n/dist/index.less';
import routes from './router';
import changeTheme from './theme/changeTheme.js';
// import zh from './locale/zh-cn.json';
// import en from './locale/en.json';

if (process.env.NODE_ENV !== 'production') {
  // require('./services/mock.js');
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
  <SupIntlProvider moduleCode="printer">
    <Router>
      <Switch>
        <Route exact path="/">
          <Redirect to="/printManage" />
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
