<template>
  <div class="m-layout">
    <UserInfo
      ref="userInfo"
      :params="{createTime:createTime,isReadOnly:isReadOnly,isWorkflow:params.isWorkflow}"
    ></UserInfo>
    <Iform
      v-if="layOutList && formData"
      name="form"
      ref="form"
      :key="newKey"
      :params="{...params, getUserInfo:getUserInfo, ...requestParams,onSaveEvent:onSaveEvent, setType:this.setType}"
    >
      <tabComp
        ref="tabComp"
        :key="newKey"
        :params="{isFileView:isFileView,layOutList:layOutList,params:{...params,dtID: dtID},formData:formData}"
      >
      </tabComp>
    </Iform>
    <input
      id="operateType"
      hidden
      type="text"
      :value="operateType"
    />
    <input
      id="workFlowVarStatus"
      hidden
      type="text"
      name="workFlowVarStatus"
      :value="workFlowVarStatus"
    />
  </div>
</template>

<script>
import { Navbar, TabItem } from 'mint-ui';
import { publicGetAxios, tryCatch } from '@/assets/js/util/common.js';
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
// import layoutJson from './mock/layout23.json';
import editLayout from "@/components/form/editLayout.vue";
import Iform from "@/components/form/Iform";
import UserInfo from '../userInfo/index.vue';
import tabComp from '../tabComp/index.vue';

export default {
  name: "layout",
  data() {
    return {
      dtID: '', // 参照复制的id
      formParams: [],
      selected: 1,
      layOutList: null,
      createTime: new Date(),
      isReadOnly: false,
      formData: null,
      requestParams: {},
      onLoadEvent: '',
      onSaveEvent: '',
      operateType: '',
      workFlowVarStatus: '',
      tableInfoId: '',
      isFileView: true,
      status: '',
      newKey: 1,
      pending: ""
    };
  },
  props: ["params"],
  beforeMount() {
    //视图类型
    if (window.pageConfig && window.pageConfig.viewType) {
      this.viewType = window.pageConfig.viewType;
      if (this.viewType == "VIEW") { //查看视图
        this.isReadOnly = true;
      }
    }
  },
  mounted() {
    const $this = this;
    this.requestParams = $this.GetRequest(window.location.href); // 获取地址参数
    this.getLayout();
  },
  methods: {
    // 获取数据
    getLayout: function () {
      const url = this.params.layoutUrl;
      publicGetAxios({
        url: url
      }, (result) => {
        const layoutData = result.data.tab;
        const { isFileView = true } = result.data;
        this.isFileView = isFileView;
        const { pageConfig = {} } = layoutData[0];
        ({ onsave: this.onSaveEvent, onload: this.onLoadEvent } = pageConfig);
        this.layOutList = layoutData;
        this.layOutList.forEach((item, index) => {
          this.formParams.push(Object.assign({}, this.params, { layout: item }));
        });
        this.$nextTick(() => {
          this.getFormData();
        });
        this.$emit('getLayoutData', result.data);
      });
    },

    getFormData: function (url, isReferCopy, id) {
      const $this = this;
      let prefix; //url前缀
      let detailUrl;
      let dtID;
      const { requestParams = {} } = this;
      if (!requestParams.id && !isReferCopy) {
        $this.$refs.userInfo.getUserInfo();
        $this.formData = {};
        $this.createTime = new Date();
        this.$nextTick(() => {
          tryCatch(new Function($this.onLoadEvent));
        })
        return;
      };
      if (window.pageConfig) {
        if (requestParams.id && requestParams.pendingId) {
          detailUrl = window.pageConfig.interfaceApi.sourceData + `/${requestParams.id}?pendingId=${requestParams.pendingId}`;
        } else if (requestParams.id) {
          detailUrl = window.pageConfig.interfaceApi.sourceData + `/${requestParams.id}`;
        } else {
          detailUrl = window.pageConfig.interfaceApi.sourceData;
        }
      }
      if (url) {
        detailUrl = url;
        this.dtID = id;
      }

      publicGetAxios({
        url: detailUrl
      }, (result) => {
        if (result.data) {
          this.formData = result.data;
          this.pending = result.data.pending;
          this.tableInfoId = result.data.tableInfoId;
          this.status = result.data.status;
          this.createTime = result.data.createTime;
          this.$parent.isLoadData = isReferCopy ? false : true; //判断是否加载处理意见
          this.$forceUpdate();
        } else {
          this.formData = {};
        }
        if (isReferCopy) this.newKey = Math.random(); // 更新数据
        //获取制单人、制单时间、制单人岗位
        if (!isReferCopy) {
          $this.$refs.userInfo.getUserInfo(result.data);
        }
        $this.$nextTick(() => {
          if (!isReferCopy) tryCatch(new Function($this.onLoadEvent));
        });

      });
    },

    getUserInfo: function () {
      const refs = this.$refs.userInfo;
      return refs.getUserData();
    },

    getFileds: function () {
      return this.$refs.form.fields;
    },

    getFiledsByName: function (name = '') {
      const { fields = [] } = this.$refs.form;
      if (name) {
        const temp = name.split('_');
        for (let i = 0; i < fields.length; i++) {
          const el = fields[i];
          if (!el || (el && el.length === 0)) continue;
          for (let j = 0; j < el.length; j++) {
            const item = el[j];
            const { condition } = item.params;
            const { name: nameKey, newName, index, datagridCode = '' } = condition;
            if (temp.length > 1) {
              if (name === newName) return condition;
            } else {
              if (name === nameKey) return condition;
            }
          }
        }
      }
      // return this.$refs.form.fields;
    },

    getDataGrid: function (dtCode = '') {
      const { dataGrid = [] } = this.$refs.form;
      for (let i = 0; i < dataGrid.length; i++) {
        const el = dataGrid[i];
        if (el.dtCode === dtCode) {
          return el;
        }
      }
    },

    // 提交
    submitData: function (params) {
      this.$refs.form.submitData(params);
    },

    // 设置类型
    setType: function (params = {}) {
      ({ operateType: this.operateType, workFlowVarStatus: this.workFlowVarStatus } = params);
    }
  },
  //引用组件
  components: {
    editLayout,
    Iform,
    UserInfo,
    tabComp
  }
};
</script>


// WEBPACK FOOTER //
// src/components/layout/index.vue