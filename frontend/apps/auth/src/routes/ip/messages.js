/*
 * @Author: DWP
 * @Date: 2020-08-05 13:42:01
 * @LastEditors: DWP
 * @LastEditTime: 2020-09-09 19:18:28
 */
import { defineMessages } from 'react-intl';

export default defineMessages({
  no: '序号',
  ip: '访问IP',
  operate: '操作',
  totalPage: '共{page}页（{total}条）',
  ipList: 'IP黑白名单管理',
  mgrType: '管控模式',
  black: '黑名单',
  white: '白名单',
  createTime: '加入时间',
  addIpTip: '支持*?通配符',
  addIpEi: '示例：192.168.1.*',
  pleaseDeleteAllIPs: '请先删除所有IP',
  userName: '用户名',
  ifDelete: '确认删除',
  whiteTip: '当前IP地址不在白名单中，是否添加',
  blackTip: '当前IP地址设置为黑名单，添加后您将列入黑名单且被强制登出，是否添加',
  allow: '是',
  deny: '否',
  confirmDelete: '删除 {ip} 吗?',
  lastItemTip: '列表将清空，管控模式自动调整为空，是否删除',
  includeSelfTip: '将自动添加当前操作的IP，是否删除',
  pleaseSelect: '请选择',
  add: '新增',
  sure: '确定',
  cancel: '取消',
  delete: '删除',
  addSuccess: '添加成功',
  deleteSuccess: '删除成功',
  deleteAndAddSuccess: '成功删除 {ip} 并添加当前操作IP',
  enterIP: '输入IP',
  tip: '提示',
  enterIPTip: '请输入IP',
  addTip: '新增 {ip} 吗?',
  addWhiteIP: '新增白名单IP',
  addBlackIP: '新增黑名单IP'
});
