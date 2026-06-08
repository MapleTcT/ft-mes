/* eslint-disable compat/compat */
import React, { Component } from 'react';

let defaultLoadingComponent = () => null;

function asyncComponent(config) {
  const { resolve } = config;

  return class DynamicComponent extends Component {
    constructor(...args) {
      super(...args);
      this.LoadingComponent = config.LoadingComponent || defaultLoadingComponent;
      this.state = {
        AsyncComponent: null
      };
      this.load();
    }

    componentDidMount() {
      this.mounted = true;
    }

    componentWillUnmount() {
      this.mounted = false;
    }

    load() {
      resolve().then((m) => {
        const AsyncComponent = m.component.default || m.component;
        if (this.mounted) {
          this.setState({ AsyncComponent, modelProps: m.model });
        } else {
          this.state.AsyncComponent = AsyncComponent; // eslint-disable-line
        }
      });
    }

    render() {
      const { AsyncComponent, modelProps } = this.state;
      const { LoadingComponent } = this;
      if (AsyncComponent) return <AsyncComponent {...this.props} store={modelProps} />;

      return <LoadingComponent {...this.props} store={modelProps} />;
    }
  };
}

export default function dynamic(config) {
  const { models: resolveModels = [], component: resolveComponent } = config;
  return asyncComponent({
    resolve: () => {
      const component = resolveComponent();
      const models = resolveModels.map((m) => m());
      return new Promise((resolve) => {
        Promise.all([...models, component]).then((ret) => {
          if (!models || !models.length) {
            return resolve({ component: ret[0] });
          } else {
            const len = models.length;
            const modelsProp = {};
            ret.slice(0, len).forEach((m) => {
              m = m.default || m;
              if (!m.nameSpace) {
                throw Error('store must has namespace property');
              }
              modelsProp[m.nameSpace] = m;
            });
            resolve({ component: ret[len], model: modelsProp });
          }
        });
      });
    },
    ...config
  });
}

dynamic.setDefaultLoadingComponent = (LoadingComponent) => {
  defaultLoadingComponent = LoadingComponent;
};
