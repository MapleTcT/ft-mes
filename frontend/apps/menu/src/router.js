/*
 * @Author: DWP
 * @Date: 2020-07-21 17:22:10
 * @LastEditors: DWP
 * @LastEditTime: 2020-11-05 09:56:15
 */
import dynamicRoute from 'root/utils/dynamicRoute';

export default [
  {
    path: '/',
    exact: true,
    component: dynamicRoute({
      component: () => import('./routes/Menu')
    })
  },
  {
    path: '/design',
    exact: true,
    component: dynamicRoute({
      component: () => import('./routes/Menu')
    })
  },
  {
    path: '/supos',
    exact: true,
    component: dynamicRoute({
      component: () => import('./routes/Menu')
    })
  }
];
