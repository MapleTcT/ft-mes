import dynamicRoute from 'root/utils/dynamicRoute';

export default [
  {
    path: '/',
    exact: true,
    component: dynamicRoute({
      component: () => import('./routes/CodeMgr')
    })
  }
];
