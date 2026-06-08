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
import './index.css';
import 'sup-ui/dist/sup-ui.less';
import routes from './router';
import changeTheme from './theme/changeTheme.js';

changeTheme();

if (process.env.NODE_ENV !== 'production') {
  // eslint-disable-next-line global-require
  // require('./mock/theme');
}

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
  <SupIntlProvider moduleCode="theme">
    <>
      <Router>
        <Switch>
          <Route exact path="/">
            <Redirect to="/themeManage" />
          </Route>
          {routes.map((route, i) => (
            <RouteWithSubRoutes key={`route_${i + 1}`} {...route} />
          ))}
        </Switch>
      </Router>
    </>
  </SupIntlProvider>
);

render(<App />, document.getElementById('root'));

if (module.hot) {
  module.hot.accept('./router', () => {
    render(<App />, document.getElementById('root'));
  });
}
