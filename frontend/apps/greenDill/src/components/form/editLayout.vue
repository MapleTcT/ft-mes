<template>
  <div>
    <div class="m_box">
      <ul class="m_list">
        <template v-for="(item, index) in layout">
          <!-- 表格 datagrid -->
          <template v-if="item[0] && item[0].type == 'DATAGRID'">
            <DataGrid
              :ref="item[0].datagridCode"
              :isFileView="isFileView"
              :params="{layout:datagrid[item[0].datagridCode],item:item,isReadOnly:isReadOnly,dtID: params.dtID,tabIndex:layOut.tabIndex}"
            ></DataGrid>
          </template>
          <!-- 附件 -->
          <li
            class="mt20 list-space"
            v-else-if="item[0] && item[0].type == 'PROPERTYATTACHMENT' || item[1] && item[1].type == 'PROPERTYATTACHMENT'"
          >
            <template v-for="(tdItem, tdIndex) in item">
              <div class="m_box">
                <div class="">
                  <Label
                    v-if="tdItem.label"
                    :labelName="tdItem.label"
                    className="ml_text ml_text_upload"
                    :condition="tdItem.condition[0]"
                    :isNullable="isNullable(item) && viewType !== 'VIEW'"
                  ></Label>
                  <div
                    v-else-if="tdItem.condition && tdItem.condition[0].type=='attachment'"
                    :style="tdItem.condition[0] && getCssStyle(tdItem.condition[0].cssstyle, tdItem.condition[0])"
                  >
                    <upload
                      :ref="tdItem.condition[0].name"
                      :params="{
                        isAppendix: tdItem.condition && tdItem.condition[0].type=='attachment'?true:false,
                        isNullable :isNullable(item),
                        fileList: fileList, 
                        postUrl: uploadURL,
                        name:tdItem.condition[0].name,
                        propertyCode:tdItem.condition[0].propertyCode,
                        condition: tdItem.condition[0],
                        isFileView: isFileView
                    }"
                      v-on:updateAppendix="updateAppendix"
                    ></upload>
                  </div>
                </div>
              </div>
            </template>
          </li>

          <!-- 图片 -->
          <li
            class="mt20 list-space"
            v-else-if="item[0] && item[0].type == 'PICTURE' || item[1] && item[1].type == 'PICTURE'"
          >
            <template v-for="(tdItem, tdIndex) in item">
              <div class="m_box">
                <div class="">
                  <Label
                    v-if="tdItem.label"
                    :labelName="tdItem.label"
                    className="ml_text ml_text_upload"
                    :condition="tdItem.condition[0]"
                    :isNullable="isNullable(item) && viewType !== 'VIEW'"
                  ></Label>
                  <div
                    v-else-if="tdItem.condition && tdItem.condition[0].type=='picture'"
                    :style="tdItem.condition[0] && getCssStyle(tdItem.condition[0].cssstyle, tdItem.condition[0])"
                  >
                    <upload
                      :ref="tdItem.condition[0].name"
                      :params="{
                        isPicture: tdItem.condition && tdItem.condition[0].type=='picture'?true:false,
                        isNullable :isNullable(item),
                        //postUrl:'/foundation/workbench/uploadFile.action', 
                        postUrl: uploadURL,
                        name:tdItem.condition[0].name,
                        condition: tdItem.condition[0],
                        isFileView: isFileView
                    }"
                      v-on:updatePicture="getPickResult"
                      :imgValue="tdItem.condition[0].value"
                    ></upload>
                  </div>
                </div>
              </div>
            </template>
          </li>

          <!-- 列表 -->
          <template v-else>
            <!--非长文本-->
            <li
              v-if="!(item[1] && item[1].condition && item[1].condition[0] && item[1].condition[0].type =='longtext')"
              :class="index != layout.length-1 ? 'm-flex list-space list-bt': 'm-flex list-space'"
            >
              <template v-for="(tdItem, tdIndex) in item">
                <!-- {{tdItem.condition[0]}} -->
                <!-- label -->
                <Label
                  v-if="tdItem.label"
                  :labelName="tdItem.label"
                  :className="tdItem.condition[0] && tdItem.condition[0].colspan==2?'flex-item block': 'flex-item lt'"
                  :condition="tdItem.condition[0]"
                  :isNullable="isNullable(item) && viewType !== 'VIEW'"
                ></Label>
                <span
                  :style="tdItem.condition[0] && getCssStyle(tdItem.condition[0].cssstyle, tdItem.condition[0])"
                  v-else-if="tdItem.condition"
                  :class="tdItem.condition[0] && tdItem.condition[0].colspan==2?'flex-item block':'flex-item rt'"
                >
                  <inputTagment
                    v-if="tdItem.condition[0]"
                    :ref="tdItem.condition[0].name"
                    :params.sync="tdItem.condition[0]"
                  ></inputTagment>
                  <!-- 单位 -->
                  <i class="m-unit">{{getUnitByFormat(tdItem.condition[0])}}</i>
                </span>

              </template>
            </li>

            <!--长文本类型-->
            <li
              v-else
              :class="index != layout.length-1 ? 'm-longtext list-space list-bt': 'm-longtext list-space'"
            >
              <template v-for="(tdItem, tdIndex) in item">
                <!-- {{tdItem.condition[0]}} -->
                <!-- label -->
                <Label
                  v-if="tdItem.label"
                  :labelName="tdItem.label"
                  :className="tdItem.condition[0] && tdItem.condition[0].colspan==2?'flex-item flex-longlabel block': 'flex-item flex-longlabel lt'"
                  :condition="tdItem.condition[0]"
                  :isNullable="isNullable(item) && viewType !== 'VIEW'"
                ></Label>
                <span
                  :style="tdItem.condition[0] && getCssStyle(tdItem.condition[0].cssstyle, tdItem.condition[0])"
                  v-else-if="tdItem.condition"
                  :class="tdItem.condition[0] && tdItem.condition[0].colspan==2?'flex-item flex-longtext block':'flex-item flex-longtext lt'"
                >
                  <inputTagment
                    v-if="tdItem.condition[0]"
                    :ref="tdItem.condition[0].name"
                    :params.sync="tdItem.condition[0]"
                  ></inputTagment>
                </span>

              </template>
            </li>
          </template>
        </template>
      </ul>
    </div>
  </div>
</template>

<script>
import AsyncValidator from "async-validator";
import _commonJs from "../../assets/js/itemList/index.js";
// import inputTagment from "@/components/form/inputTagment";
import inputTagment from "@/components/cellComp/inputTagment/index.vue";
import DataGrid from "@/components/dataGrid/index.vue";
import { Toast, Indicator, MessageBox } from "mint-ui";
import upload from "@/components/cellComp/upload/index.vue";
import Label from "../cellComp/label/index.vue";
import { getValidateRule } from '@/assets/js/util/validate.js';
import { publicGetAxios, getQueryType, combineEvents } from '@/assets/js/util/common.js';
import { getdItemdition, initConfigEvent } from './util.js';
import interfaceURL from '../../assets/js/util/interface.js'

export default {
  name: "editLayout",
  data() {
    return {
      layout: "",
      userInfo: {},
      isWorkflow: "", //是否启用工作流
      urlParams: "",
      showUserPickpos: false,
      startdate: "",
      isReadOnly: "",
      enddate: "",
      dateVal: "",
      initDatagrid: "",
      delDtData: {},
      onSaveEvent: "",
      operatetype: "",
      onLoadEvent: "",
      formUrl: "", // form 地址
      eventBindConfig: "",
      editData: "", //编辑数据
      pictureFile: {}, // 图片
      pictureFileDel: {}, //删除的图片
      attachmentFile: {}, // 附件
      attachmentFileDel: "", //删除的附件字段
      pickPos: {
        name: "pickPos",
        isShow: false,
        slots: [{
          flex: 1,
          values: []
        }],
        value: ""
      },
      fileList: [],
      workFlowVarStatus: "",
      isShowDatagrid: true, //判断查看视图的表格是否显示空行
      viewType: "", //视图类型
      taskDescription: "",
      tableInfoId: "",
      fileTypeList: [], //附件类型
      filePathList: [], //附件路径
      propertyCodeList: [], //附件编码
      ids2delList: [] //删除的附件id
    };
  },
  props: ["params", "layOut", "formData", "isFileView"],
  beforeMount() {
    const $this = this;

    //视图类型
    if (window.pageConfig && window.pageConfig.viewType) {
      $this.viewType = window.pageConfig.viewType;
      if ($this.viewType == "VIEW") { //查看视图
        $this.isReadOnly = true;
      }
    }

    //是否启用工作流
    if (window.pageConfig && window.pageConfig.workflowEnabled) {
      $this.isWorkflow = window.pageConfig.workflowEnabled;
    }

    this.uploadURL = interfaceURL.uploadFile;

  },

  mounted() {
    var $this = this;

    // 获取地址参数
    $this.urlParams = $this.GetRequest(window.location.href)
    var url = $this.params.layoutUrl;

    $this.getLayoutData(url); // 获取布局数据
    // $this.exportFunc(`getLayoutData`, $this.getLayoutData); // 注册全局方法
  },
  methods: {
    // 获取布局数据
    getLayoutData: function (url) {
      let data = this.layOut; // props 参数
      if (this.isWorkflow) {
        if (this.formData && this.formData.tableInfoId) this.getDetailData();
      } else { //基础单据
        if (this.formData) this.getDetailData();
      }
      this.datagrid = this.sortDataGrid(data.dataGrid); // datagrid
      this.layout = this.sortLayout(data.elements); // 单元格布局分组
      this.onSaveEvent = data.pageConfig.onsave; // 自定义保存事件
      this.onLoadEvent = data.pageConfig.onload; // 自定义onload
    },

    sortDataGrid(data = []) {
      let curObj = {};
      data.forEach((item) => {
        const { code = '' } = item.config
        curObj[code] = item;
      });
      return curObj;
    },

    // 获取数据信息 赋值
    getDetailData: function () {
      const formData = this.formData;
      this.editData = formData;
      this.tableInfoId = formData.tableInfoId;
      this.taskDescription = formData.pending && formData.pending.taskDescription;
    },

    //布局单元格分组
    sortLayout: function (data = []) {
      const $this = this;
      if (!data) data = [];
      var list = [],
        col = [],
        sort = [],
        nameTit = "",
        conditions = [],
        datagrid = "";

      // 分组
      for (var i = 0; i < data.length; i++) {
        var item = data[i],
          templist = {};
        conditions = $this.filterCondition(item);
        nameTit = "";
        if (
          item.element &&
          item.element.showType &&
          item.element.showType.toLowerCase() == "label"
        ) {
          nameTit = item.element.namekey;
        }

        templist = {
          type: item.element && item.element.columnType,
          label: nameTit,
          datagridCode: item.element && item.element.code,
          condition: conditions
        };

        if (item.firstTd == "1" && i > 0) {
          // 一行
          sort.push(col);
          col = [];
        }

        col.push(templist);

        if (i == data.length - 1) {
          sort.push(col);
        }
      }

      return sort;
    },


    // 是否可空
    isNullable: function (list) {
      if (list && list.length > 0) {
        for (let i = 0; i < list.length; i++) {
          const element = list[i];
          try {
            if (element.condition && element.condition[0] && !element.condition[0].nullable) {
              // 标签且是查看
              if (element.type === 'label' && this.viewType == "VIEW") {
                return false;
              }
              return true;
              break;
            }
          } catch (error) {
            console.log(error)
          }
        }
        return false;
      } else {
        return false;
      }
    },


    //选择图片
    getPickResult: function (event, pictureFile, con, type) {
      const $this = this;
      var obj = event.target;
      let tempName = obj.name;
      if (tempName && tempName.indexOf('.') > -1) {
        let typeName = window.pageConfig && window.pageConfig.modelAlias;
        tempName = obj.name.replace(`${typeName}.`, '');
      }
      // $this.pictureFile[`ids2del`] = con.valueId?con.valueId:""; // 删除     
      if (type != "delete") {
        $this.pictureFile[`${tempName}Files.filePath`] = pictureFile && pictureFile.data && pictureFile.data.path ? [pictureFile.data.path] : "";
        $this.pictureFile[`${tempName}Files.fileType`] = ["pic"];
        $this.pictureFile[`${tempName}Files.propertyCode`] = [con.propertyCode]; // propertyCode

        let modelCode = window.pageConfig.modelCode;
        let type = _commonJs.getUploadType($this.isWorkflow, modelCode);// 区分流程单据和非流程单据
        $this.pictureFile[`${tempName}Files.type`] = type;

        $this.pictureFileDel = {};
        $this.pictureFileDel[`ids2del`] = con.valueId ? [con.valueId] : [];
      } else if (type == "delete") {
        $this.pictureFileDel = {};
        $this.pictureFileDel[`ids2del`] = con.valueId ? [con.valueId] : [];
      }
      vue.pictureFile = $this.pictureFile;
      vue.pictureFileDel = $this.pictureFileDel;

      obj.value = null;
    },

    //添加和删除附件
    updateAppendix: function (event, attachmentFile, con, type) {
      const $this = this;
      _commonJs.updateAppendixs(event, attachmentFile, con, type, $this);
    },

    // 筛选条件
    filterCondition: function (data) {
      let $this = this,
        condition = [];
      if (data && data.element) {
        data.element.tabIndex = this.layOut.tabIndex;
        data.element.funcbody = data.funcbody;
        data.element.funcname = data.funcname;
        condition.push(
          getdItemdition(data.element, data.sourcepropertyname, this.isReadOnly)
        );
        if (!condition[0]) condition[0] = {};
        let funcAll = {};
        condition[0].colspan = data.colspan;
        condition[0].cssstyle = data.cssstyle;
        condition[0].cssstyle_container = data.cssstyle_container;
        condition[0].funcbody = data.funcbody;
        condition[0].funcname = data.funcname;
        condition[0].callbackbody = data.callbackbody;
        condition[0].callbackname = data.callbackname;
      }

      condition[0] && condition[0].showType !== 'LABEL' && _commonJs.getValueByKeyEdit(condition[0], "", this);

      return condition;
    },

    //获取样式配置信息
    getCssStyle: function (config, element) {
      return _commonJs.getCssStyle(config, element);
    },

    // 获取单位
    getUnitByFormat: function (condition) {
      return _commonJs.getUnitsByFormat(condition);
    }


  },
  //引用组件
  components: {
    inputTagment,
    DataGrid,
    upload,
    Label
  }
};
</script>
<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less" scoped>
@import "../../assets/css/editView.less";
</style>


// WEBPACK FOOTER //
// src/components/form/editLayout.vue