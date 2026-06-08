/*
 * @Author: DWP
 * @Date: 2020-11-16 17:12:38
 * @LastEditors: DWP
 * @LastEditTime: 2020-11-16 17:28:43
 */
/*
 * @Author: DWP
 * @Date: 2020-11-16 17:12:38
 * @LastEditors: DWP
 * @LastEditTime: 2020-11-16 17:26:54
 */
import React from 'react';

import {
  MODULE_USER_DIR,
  MODULE_CERTIFICATE_MGR,
  MODULE_MIRROR_MGR,
  MODULE_PWD_CONFIG,
  MODULE_IDENTITY_PROVIDERS
} from './constants';
import UserDir from '../userDir';
import certificateManager from '../certificateManager';
import mirrorManager from '../mirrorManager';
import PwdConfig from '../pwdConfig';
import Oauth from '../oauth';

const modulesMap = {
  [MODULE_USER_DIR]: UserDir,
  [MODULE_CERTIFICATE_MGR]: certificateManager,
  [MODULE_MIRROR_MGR]: mirrorManager,
  [MODULE_PWD_CONFIG]: PwdConfig,
  [MODULE_IDENTITY_PROVIDERS]: Oauth
};

export default function getModule(moduleCode, props) {
  const Module = modulesMap[moduleCode];
  if (!Module) return null;
  return <Module {...props} />;
}
