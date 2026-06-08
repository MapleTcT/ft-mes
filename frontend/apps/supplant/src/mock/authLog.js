import Mock from 'mockjs';

const data = {
  'list|5-20': [
    {
      'rowIndex|+1': 1,
      moduleName: '报销单',
      formName: '报销单',
      operator: '张三',
      operateTime: '2020-12-14',
      beOperated: '李四',
      beOperatedCode: 'baoxiaodan',
      operateType: '新增',
      ip: '192.168.91.45',
      operateDesc: '新增单据',
      errorDesc: '新增失败',
      importFile: 'import.file',
      expendFlag: false,
      'children|1-5': [
        {
          'rowIndex|+1': 111,
          moduleName: '报销单',
          formName: '报销单',
          operator: '张三',
          operateTime: '2020-12-14',
          beOperated: '李四',
          beOperatedCode: 'baoxiaodan',
          operateType: '新增',
          ip: '192.168.91.45',
          operateDesc: '新增单据',
          errorDesc: '新增失败',
          importFile: 'import.file',
          expendFlag: false,
          'children|1-3': [
            {
              'rowIndex|+1': 1111,
              moduleName: '报销单',
              formName: '报销单',
              operator: '张三',
              operateTime: '2020-12-14',
              beOperated: '李四',
              beOperatedCode: 'baoxiaodan',
              operateType: '新增',
              ip: '192.168.91.45',
              operateDesc: '新增单据',
              errorDesc: '新增失败',
              importFile: 'import.file',
              expendFlag: false
            }
          ]
        }
      ]
    }
  ]
};

export default Mock.mock(data);
