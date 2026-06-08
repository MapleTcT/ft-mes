import dynamicRoute from 'root/utils/dynamicRoute';

const routes = [
  {
    path: '/reLogin',
    name: 'reLogin',
    component: dynamicRoute({
      component: () => import('./routes/reLogin')
    })
  },
  {
    path: '/authorityList',
    name: 'authorityList',
    component: dynamicRoute({
      component: () => import('./routes/authorityList')
    })
  },
  {
    path: '/portal',
    name: 'portal',
    component: dynamicRoute({
      component: () => import('./routes/portal')
    })
  },
  {
    path: '/myProcess',
    name: 'myProcess',
    component: dynamicRoute({
      component: () => import('./routes/myProcess')
    })
  },
  {
    path: '/processList',
    name: 'processList',
    component: dynamicRoute({
      component: () => import('./routes/processList')
    })
  },
  {
    path: '/pendingNotice',
    name: 'pendingNotice',
    component: dynamicRoute({
      component: () => import('./routes/pendingNotice')
    })
  },
  {
    path: '/pendingList',
    name: 'pendingList',
    component: dynamicRoute({
      component: () => import('./routes/pendingList')
    })
  },
  {
    path: '/startProcess',
    name: 'startProcess',
    component: dynamicRoute({
      component: () => import('./routes/startProcess')
    })
  },
  {
    path: '/auditLog',
    name: 'auditLog',
    component: dynamicRoute({
      component: () => import('./routes/auditLog')
    })
  },
  {
    path: '/customFieldModelManage',
    name: 'ModelManage',
    component: dynamicRoute({
      component: () => import('./routes/customField/modelManage')
    })
  },
  {
    path: '/customFieldViewManage',
    name: 'ViewManage',
    component: dynamicRoute({
      component: () => import('./routes/customField/viewManage')
    })
  },
  {
    path: '/changePass',
    name: 'ChangePass',
    component: dynamicRoute({
      component: () => import('./routes/changePass')
    })
  },
  {
    path: '/personalInfo',
    name: 'PersonalInfo',
    component: dynamicRoute({
      component: () => import('./routes/personalInfo')
    })
  },
  {
    path: '/about', // 版本关于
    name: 'About',
    component: dynamicRoute({
      component: () => import('./routes/about')
    })
  }
];

if (process.env.NODE_ENV !== 'production') {
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
