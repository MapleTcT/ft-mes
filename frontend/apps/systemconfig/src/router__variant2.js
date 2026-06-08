import dynamicRoute from 'root/utils/dynamicRoute';

const routes = [
  {
    path: '/sysconfig',
    component: dynamicRoute({
      component: () => import('./routes/sysconfig')
    })
  },
  {
    path: '/oauth',
    component: dynamicRoute({
      component: () => import('./routes/oauth')
    })
  }
];

if (process.env.NODE_ENV !== 'production') {
  routes.push({
    path: '/userdir',
    component: dynamicRoute({
      component: () => import('./routes/userDir')
    })
  });
  routes.push({
    path: '/certificateManager',
    component: dynamicRoute({
      component: () => import('./routes/certificateManager')
    })
  });
  routes.push({
    path: '/mirrorManager',
    component: dynamicRoute({
      component: () => import('./routes/mirrorManager')
    })
  });
  routes.push({
    path: '/',
    component: dynamicRoute({
      component: () => import('./layouts/header/header')
      // models: [() => import('root/store/mobxStore')]
    }),
    routes: [
      {
        path: '/demo',
        component: dynamicRoute({
          component: () => import('./routes/demo')
        })
      },
      // {
      //   path: '/sysconfig',
      //   component: dynamicRoute({
      //     component: () => import('./routes/sysconfig')
      //   })
      // },
      {
        path: '/demos',
        component: dynamicRoute({
          component: () => import('./routes/demo')
        })
      },
      {
        path: '/hooks',
        component: dynamicRoute({
          component: () => import('./routes/demo/hooks')
        })
      },
      {
        path: '/welcome',
        component: dynamicRoute({
          component: () => import('./routes/demo/welcome')
        })
      },
      {
        path: '/mobx',
        component: dynamicRoute({
          component: () => import('./routes/demo/mobx'),
          models: [() => import('root/store/mobxStore')]
        })
      },
      {
        path: '/mock',
        component: dynamicRoute({
          component: () => import('./routes/demo/mock'),
          models: [() => import('root/store/async')]
        })
      },
      {
        path: '/intl',
        component: dynamicRoute({
          component: () => import('./routes/demo/intl')
        })
      },
      {
        path: '/table',
        component: dynamicRoute({
          component: () => import('./routes/demo/table')
        })
      },
      {
        path: '/form',
        component: dynamicRoute({
          component: () => import('./routes/demo/form')
        })
      },
      {
        path: '/modal',
        component: dynamicRoute({
          component: () => import('./routes/demo/modal')
        })
      },
      {
        path: '/list',
        component: dynamicRoute({
          component: () => import('./routes/demo/list')
        })
      }
    ]
  });
}

export default routes;
