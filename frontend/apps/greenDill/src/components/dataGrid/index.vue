<template>
  <li
    class="list-nobg"
    data-id="datagrid"
    :data-name="dtCode"
    v-if="isLoadFlag"
  >
    <!-- 编辑视图 -->
    <template
      v-for="(tdItem, k) in dataSource"
      v-if="!isReadOnly"
    >
      <!-- 表头 -->
      <span class="td-head">
        <!-- {{item[0].datagrid.tdName}} -->
        <h2 v-if="titName">{{titName}}（{{k+1}}）</h2>
        <h2 v-else>{{k+1}}</h2>
        <span
          v-if="!isReadOnly && btnConfig.DELETEROW && isEditable != false"
          v-on:click="delDataGrid(k)"
          class="td-del-btn"
        >
          {{$t('button.delete.text')}}
        </span>
      </span>
      <!-- 表内容 -->
      <ul>
        <template v-for="(gridItem, j) in tdItem">
          <li
            v-if="!(gridItem && gridItem.type =='longtext') && gridItem.columnType != 'PROPERTYATTACHMENT'"
            class="m-flex list-space"
            :class="gridItem.isHidden ? '' : 'bt'"
          >
            <span
              :style="gridItem && getCssStyle(gridItem.cssstyle, gridItem)"
              class="flex-item lt"
            >
              <label class="comp-label-con">
                <span class="comp-label">{{gridItem.label}}</span>
                <em
                  v-if="gridItem && !gridItem.nullable && gridItem.label"
                  class="mustFill"
                >*</em>
              </label>
            </span>
            <span
              :style="gridItem && getCssStyle(gridItem.cssstyle, gridItem)"
              :class="getUnitByFormat(gridItem)?'pr flex-item rt':'flex-item rt'"
            >
              <inputTagment
                :rowIndex="k"
                :newName="`${dtCode}_${k}_${gridItem.name}`"
                :ref="`${dtCode}_${k}_${gridItem.name}`"
                v-if="gridItem"
                :params.sync="gridItem"
              ></inputTagment>
              <!-- 单位 -->
              <i class="m-unit">{{getUnitByFormat(gridItem)}}</i>
            </span>
          </li>

          <!--长文本类型-->
          <li
            v-else-if="gridItem && gridItem.type =='longtext'"
            class="m-longtext list-space"
            :class="gridItem.isHidden ? '' : 'bt'"
          >
            <span
              :style="gridItem && getCssStyle(gridItem.cssstyle, gridItem)"
              class="flex-item lt"
            >
              <label class="comp-label-con">
                <span class="comp-label">{{gridItem.label}}</span>
                <em
                  v-if="gridItem && !gridItem.nullable"
                  class="mustFill"
                >*</em>
              </label>
            </span>
            <span
              :style="gridItem && getCssStyle(gridItem.cssstyle, gridItem)"
              :class="getUnitByFormat(gridItem)?'pr flex-item lt':'flex-item lt'"
            >
              <inputTagment
                :rowIndex="k"
                :newName="`${dtCode}_${k}_${gridItem.name}`"
                :ref="`${dtCode}_${k}_${gridItem.name}`"
                v-if="gridItem"
                :params.sync="gridItem"
              ></inputTagment>
            </span>
          </li>

          <!-- 附件 -->
          <li
            v-else-if="gridItem && gridItem.columnType == 'PROPERTYATTACHMENT'"
            class="mt20 list-space"
          >
            <span
              :style="gridItem && getCssStyle(gridItem.cssstyle, gridItem)"
              class="flex-item lt"
            >
              <label class="comp-label-con">
                <span class="comp-label">{{gridItem.label}}</span>
                <em
                  v-if="gridItem && !gridItem.nullable && gridItem.label"
                  class="mustFill"
                >*</em>
              </label>
            </span>
            <span
              :style="gridItem && getCssStyle(gridItem.cssstyle, gridItem)"
              :class="getUnitByFormat(gridItem)?'pr flex-item rt':'flex-item rt'"
            >
              <upload
                v-if="gridItem && gridItem.type=='attachment'"
                :rowIndex="k"
                :newName="`${dtCode}_${k}_${gridItem.name}`"
                :ref="gridItem.name"
                :params="{
                    isAppendix: gridItem && gridItem.type=='attachment'?true:false,
                    isNullable: isNullable(gridItem),
                    fileList: fileList, 
                    postUrl: uploadURL,
                    name: gridItem.name,
                    propertyCode: gridItem.propertyCode,
                    condition: gridItem,
                    isFileView: isFileView
                }"
                v-on:updateAppendix="updateAppendix"
              ></upload>
            </span>
          </li>
        </template>
      </ul>
    </template>

    <!-- 查看视图 -->
    <template v-else>
      <!-- 表头 -->
      <span
        class="td-head"
        v-if="isShowDatagrid"
      >
        <h2 v-if="titName">{{titName}}（{{k+1}}）</h2>
        <h2 v-else>{{k+1}}</h2>
      </span>
      <!-- 表内容 -->
      <ul v-if="isShowDatagrid">
        <template v-for="(gridItem, j) in tdItem">
          <li
            v-if="!(gridItem && gridItem.type =='longtext') && gridItem.columnType != 'PROPERTYATTACHMENT'"
            class="m-flex list-space"
            :class="gridItem.isHidden ? '' : 'bt'"
          >
            <span
              :style="gridItem && getCssStyle(gridItem.cssstyle, gridItem)"
              class="flex-item lt"
            >
              {{gridItem.label}}
            </span>
            <span
              :style="gridItem && getCssStyle(gridItem.cssstyle, gridItem)"
              :class="getUnitByFormat(gridItem)?'pr flex-item rt':'flex-item rt'"
            >
              <inputTagment
                :rowIndex="k"
                :newName="`${dtCode}_${k}_${gridItem.name}`"
                :ref="`${dtCode}_${k}_${gridItem.name}`"
                v-if="gridItem"
                :params.sync="gridItem"
              ></inputTagment>
              <!-- 单位 -->
              <i class="m-unit">{{getUnitByFormat(gridItem)}}</i>
            </span>
          </li>

          <!--长文本类型-->
          <li
            v-if="gridItem && gridItem.type =='longtext'"
            class="m-longtext list-space"
            :class="gridItem.isHidden ? '' : 'bt'"
          >
            <span
              :style="gridItem && getCssStyle(gridItem.cssstyle, gridItem)"
              class="flex-item lt"
            >
              {{gridItem.label}}
            </span>
            <span
              :style="gridItem && getCssStyle(gridItem.cssstyle, gridItem)"
              :class="getUnitByFormat(gridItem)?'pr flex-item lt':'flex-item lt'"
            >
              <inputTagment
                :rowIndex="k"
                :newName="`${dtCode}_${k}_${gridItem.name}`"
                :ref="`${dtCode}_${k}_${gridItem.name}`"
                v-if="gridItem"
                :params.sync="gridItem"
              ></inputTagment>
            </span>
          </li>

          <!-- 附件 -->
          <li
            v-else-if="gridItem && gridItem.columnType == 'PROPERTYATTACHMENT'"
            class="mt20 list-space"
          >
            <span
              :style="gridItem && getCssStyle(gridItem.cssstyle, gridItem)"
              class="flex-item lt"
            >
              {{gridItem.label}}
            </span>
            <span
              :style="gridItem && getCssStyle(gridItem.cssstyle, gridItem)"
              :class="getUnitByFormat(gridItem)?'pr flex-item rt':'flex-item rt'"
            >
              <upload
                v-if="gridItem && gridItem.type=='attachment'"
                :ref="`${dtCode}_${k}_${gridItem.name}`"
                :params="{
                    isAppendix: gridItem && gridItem.type=='attachment'?true:false,
                    isNullable: isNullable(gridItem),
                    fileList: fileList, 
                    postUrl: uploadURL,
                    name: gridItem.name,
                    propertyCode: gridItem.propertyCode,
                    condition: gridItem,
                    isFileView: isFileView
                }"
                v-on:updateAppendix="updateAppendix"
              ></upload>
            </span>
          </li>
        </template>
      </ul>
    </template>
    <!-- 表尾 -->
    <span
      class="td-footer list-space mt20"
      v-if="!isReadOnly && btnConfig && btnConfig.ADDROW && isEditable != false"
    >
      <span
        class="abb-icon-btn"
        v-on:click="addDataGrid()"
      >
        <i class="add-icon"></i>
        <span class="td-add-btn">{{$t('dataGrid.add.detail')}}</span>
      </span>
    </span>

  </li>

</template>

<script>
import { publicGetAxios, tryCatch, combineEvents } from '@/assets/js/util/common.js';
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
// import inputTagment from "@/components/form/inputTagment";
import inputTagment from "@/components/cellComp/inputTagment/index.vue";
import { getdItemdition, initConfigEvent } from "@/components/form/util.js";
import Emitter from "@/assets/js/emitter.js";
import "@/assets/css/editView.less";
import './index.less'
import upload from "@/components/cellComp/upload/index.vue";
import { merge, remove } from 'lodash';
import interfaceURL from '../../assets/js/util/interface.js'

export default {
  name: "dataGrid",
  mixins: [Emitter],
  data() {
    return {
      item: [],
      isReadOnly: false,
      dtLayout: [],
      delDtData: {},
      eventBindConfig: "",
      dtCode: '',
      titName: '',
      btnConfig: {},
      isLoadFlag: false,
      isEditable: true,
      initData: [],
      dtName: '',
      dataSource: [],
      isShowDatagrid: true,
      fileList: [],
      fileTypeList: [], //附件类型
      filePathList: [], //附件路径
      propertyCodeList: [], //附件编码
      ids2delList: [], //删除的附件id
    };
  },
  props: ["params", "isFileView"],
  beforeMount() {
    this.item = this.params.item;
    this.isReadOnly = this.params.isReadOnly;
    if (window.pageConfig && window.pageConfig.viewType) {
      this.viewType = window.pageConfig.viewType;
    }
    this.uploadURL = interfaceURL.uploadFile;
  },
  mounted() {
    // 获取datagrid数据
    const { layout = {}, dtID } = this.params;
    const { ptPageInit } = layout;
    this.dtLayout = this.sortLayout(); // 布局
    this.urlParams = this.GetRequest(window.location.href);
    this.dtID = dtID;
    this.$nextTick(() => {
      // this.initData = Object.assign({}, this.dtLayout[0]);
      this.initData = this.deepCopy(this.dtLayout[0]);
      if (ptPageInit && !this.dtID) tryCatch(new Function(ptPageInit));
      this.getDatagridByLayout();
    });
    this.dispatch("iForm", "form-dataGird", this);
  },
  methods: {
    sortLayout: function () {
      const { layout = {} } = this.params;
      const { elements = [], config = {}, datagridName = '', buttons = [], name, isEditable } = layout;
      const curEl = [];
      this.dtCode = config.DataGridCode;
      this.titName = datagridName;
      this.btnConfig = this.getdataGridBtn(buttons);
      this.isEditable = isEditable;
      this.dtName = name;
      elements.forEach((item = {}) => {
        if (this.isEditable == false) { //判断表格是否可编辑 
          this.isReadOnly = true;
        }
        item.tabIndex = this.params.tabIndex;
        let condition = getdItemdition(item, '', this.isReadOnly);
        condition.newName = `${this.dtCode}_0_${item.key}`;
        condition.isHidden = item.isHidden;
        condition.callbackbody = item.callbackbody;
        condition.callbackname = item.callbackname;
        condition.cssstyle = item.cssstyle;
        condition.funcbody = item.funcbody;
        condition.funcname = item.funcname;
        condition.showFormatFunc = item.showFormatFunc;
        condition.isEditable = isEditable;
        condition.isReadOnly = this.isReadOnly;
        let defaultType = ["BOOLEAN", "DATE", "DATETIME", "SYSTEMCODE"];
        if (defaultType.indexOf(item.columnType) > -1) {
          condition.defaultValue = item.defaultValue;
        }
        condition.datagridCode = this.dtCode;
        if (condition.type == "attachment") {
          condition.multiFile = {};
        }
        condition.index = 0;
        curEl.push(condition);
      })
      // this.initData = JSON.parse(JSON.stringify(curEl));
      // this.dataSource = JSON.parse(JSON.stringify([curEl]));
      this.dataSource = [curEl];
      return [curEl];
    },
    //获取样式配置信息
    getCssStyle: function (config, element) {
      return _commonJs.getCssStyle(config, element);
    },
    // 获取单位
    getUnitByFormat: function (condition) {
      return _commonJs.getUnitsByFormat(condition);
    },

    // 获取layout 加载datagrid数据
    getDatagridByLayout: function (url, callback, isReferCopy, dataGridCode) {
      const $this = this;
      let { dtLayout } = this;
      let configEvent = _commonJs.getEventBindConfig(
        dtLayout[0],
        true
      ); //获取额外的事件绑定配置
      $this.eventBindConfig = $this.eventBindConfig.concat(configEvent);
      $this.getDataGridData(url, callback, isReferCopy); // 加载datagrid数据
    },

    // 获取datagrid数据
    getDataGridData: function (url, callback, isReferCopy) {
      const $this = this;
      const { layout = {} } = this.params;
      const { config = {}, renderOver } = layout;
      const { dataSource = [] } = this;
      const { interfaceApi = {}, modelAlias } = window.pageConfig || {};
      const datagridKey = config.DataGridCode;
      let dataGridDataUrl = interfaceApi.datagridData;
      let alias = modelAlias;
      let dtCode = this.dtName;
      let urlParams = this.urlParams;
      let basciUrl;
      if (urlParams && urlParams.id) {
        basciUrl = `${dataGridDataUrl}${dtCode}?datagridCode=${datagridKey}&id=${urlParams.id}`;
      } else {
        basciUrl = `${dataGridDataUrl}${dtCode}?datagridCode=${datagridKey}&id=-1`;
      }
      if (this.dtID) {
        basciUrl = `${dataGridDataUrl}${dtCode}?datagridCode=${datagridKey}&id=${this.dtID}`;
      }
      if (url) {
        basciUrl = url;
      }
      // basciUrl = "static/data/datagridData.json";
      publicGetAxios({
        url: basciUrl,
        type: 'post',
        headers: {
          "Content-Type": "application/json;charset=UTF-8"
        },
        data: JSON.stringify({
          'pageSize': 65536,
          'maxPageSize': 500
        })
      }, (result) => {
        if (result.code === 200) {
          let resData = result.data;
          if (resData.result.length > 0) {
            let tempArr = [], tempEl = [];
            for (let i = 0; i < resData.result.length; i++) {
              let data = resData.result[i];
              tempEl = [];
              for (let j = 0; j < dataSource[0].length; j++) {
                const element = dataSource[0][j];
                let temp = this.deepCopy(element);
                // let temp = element;
                if (temp.iscustom) {
                  const modelKey = temp.name.split('.')[0];
                  let { attrMap } = data[modelKey] || {};
                  if (attrMap) data = merge(data, { [modelKey]: { ...attrMap } });
                }
                _commonJs.getValueByKeyEdit(temp, data, $this);
                temp.index = i;
                if (data.id) {
                  temp.id = data.id.toString();
                }
                temp.isloadData = true;
                // temp = JSON.parse(JSON.stringify(element));
                // if(temp.condition.valueId){
                //     temp.condition.valueId = {id: temp.condition.valueId};
                // }

                tempEl.push(temp);

              }
              tempArr.push(tempEl);
            }
            $this.dataSource = tempArr;

          } else if (resData && !$this.urlParams.id && !isReferCopy) { //新建单据给系统编码多选的slots赋值
            let dtEl = $this.dtLayout[0];
            for (let m = 0; m < dtEl.length; m++) {
              // let temp = dtEl[m];
              _commonJs.getValueByKeyEdit(dtEl[m], '', $this); //添加默认值
            }
          } else {
            if ($this.isReadOnly && resData.result.length == 0) {
              $this.isShowDatagrid = false;
              this.dataSource = [];
            }
          }

        }
        this.isLoadFlag = true;
        if (callback && typeof callback == "function") {
          callback(dtEl);
        }
        if (renderOver)
          this.$nextTick(() => {
            tryCatch(new Function(renderOver));
          })
      });
    },

    //增加一组diagrid数据
    addDataGrid: function () {
      const data = this.dataSource;
      if (data) {
        // this.initData = this.deepCopy(this.dtLayout[0]);
        var temp = this.deepCopy(this.initData);
        for (let i = 0; i < temp.length; i++) {
          const element = temp[i];
          const datagridCode = element.datagridCode;
          element.newName = `${datagridCode}_${data.length}_${element.name}`;
          element.index = data.length;
        }
        data.push(temp);
        this.$nextTick(() => {//TODO
          let tdFooterH = document.querySelector(".td-footer").scrollHeight;
          let ulH = document.querySelector(".list-nobg ul").scrollHeight;
          let top = document.querySelector(".m_centerBox").scrollTop;
          document.querySelector('.m_centerBox').scrollTop = top + tdFooterH + ulH;
          initConfigEvent(this);
        });
      }
    },

    //删除diagrid中的一条数据
    delDataGrid: function (index) {
      const $this = this;
      const data = this.dataSource;
      if (data && data[index] && data[index][0].isloadData) { // 数据加载，删除时记录
        let temp = data[index];
        let tempDt = {};
        let tempId = temp[0].id;
        // for (let i = 0; i < temp.length; i++) {
        //     const element = temp[i];
        //     tempDt[`${element.condition.name}`] = element.condition.valueId?element.condition.valueId:element.condition.value;
        // }
        if (this.delDtData[temp[0].datagridCode]) {
          this.delDtData[temp[0].datagridCode].push(tempId);
        } else {
          this.delDtData[temp[0].datagridCode] = [tempId];
        }
      }
      if (data) {
        const delData = data[index];
        data.splice(index, 1);
        delData.forEach(item => {
          this.dispatch("iForm", 'deleteFileds', item.newName ? item.newName : item.name); //删除防止校验
        });
      }
      // const temp = data.splice(index, 1);
    },

    // 过滤datagrid按钮，根据operatetype参数判断增行和删行按钮类型
    getdataGridBtn: function (config) {
      const $this = this;
      let btnconfig = {};
      if (config) {
        for (let i = 0; i < config.length; i++) {
          const element = config[i];
          btnconfig[element.operatetype] = element;
        }
      }

      return btnconfig;
    },

    getInterfaceParam: function () {
      const { dataSource = [], dtCode } = this;
      let dtAry = [];
      for (let i = 0; i < dataSource.length; i++) {
        const list = dataSource[i];
        let dtTemp = {};
        for (let j = 0; j < list.length; j++) {
          const temp = list[j];
          let nameKey = temp.name;
          const value = temp.formVal;
          if (!value && value !== 0 && !temp.multiFile) continue;
          if (nameKey.indexOf(".") > -1 && !temp.iscustom) {
            let nameIndex = nameKey.lastIndexOf(".");
            nameKey = nameKey.substring(0, nameIndex);
          }
          dtTemp.id = temp.id;

          // dtTemp.rowIndex = `${temp.condition.index}`; //行数

          // 多选控件参数组织
          if (temp.columnType == "MULTSELECT") {
            const { multiIds = [], deleteIds = [], AddIds = [] } = this.getCompByName('iForm').getMultiSelectData(temp) || {};
            dtTemp[`${temp.name}multiselectIDs`] = multiIds.join(',');
            dtTemp[`${temp.name}AddIds`] = AddIds.join(',');
            dtTemp[`${temp.name}DeleteIds`] = deleteIds.join(',');
            continue;
          }

          if (temp.isPrefer) { // 参照
            if (temp.iscustom) {
              nameKey = nameKey.replace('.id', '');
              dtTemp[`${nameKey}`] = `${temp.valueId}`;
            } else {
              // 修改保存id逻辑
              nameKey = nameKey.split('.')[0];
              if (dtTemp.hasOwnProperty(nameKey)) {
                continue;
              } else {
                dtTemp[`${nameKey}`] = temp.valueId ? { id: temp.valueId } : null;
              }
            }

            continue;
          }

          if (temp.columnType == "SYSTEMCODE") { // 系统编码
            if (temp.propertyCode.indexOf("||") == -1) { //非参照类型的系统编码
              if (temp.multable) { //系统编码多选
                dtTemp[`${nameKey}`] = temp.valueId && temp.valueId.length > 0 ? `${temp.valueId.join(',')}` : null;
              } else {//系统编码单选
                if (temp.valueId) {
                  // 自定义字段
                  if (temp.iscustom) {
                    dtTemp[`${nameKey}`] = `${temp.valueId}`;
                  } else {
                    dtTemp[`${nameKey}`] = {
                      id: `${temp.valueId}`,
                      value: `${value}`
                    };
                  }

                } else {
                  dtTemp[`${nameKey}`] = null;
                }
              }
            } else { //参照类型的系统编码
              continue;
            }
          } else if (temp.columnType == "BOOLEAN") { //布尔类型
            if (value === this.$t('option.boolean.yes')) {
              temp.valueId = true;
            } else if (value === this.$t('option.boolean.no')) {
              temp.valueId = false;
            } else {
              temp.valueId = null;
            }
            dtTemp[`${temp.name}`] = temp.valueId;
          } else if (temp.columnType == "INTEGER" || temp.columnType == "LONG" || temp.columnType == "DECIMAL" || temp.columnType == "MONEY") { //数字型和金额型
            let tempVal = value;
            if (tempVal && temp.showFormat == "PERCENT") { //百分比显示
              tempVal = Number(tempVal / 100).toString();
            }
            dtTemp[`${temp.name}`] = tempVal || tempVal === 0 ? Number(tempVal) : null;
          } else if (temp.datetype === "date" || temp.datetype === "datetime") { //日期和日期时间
            dtTemp[`${temp.name}`] = value ? _commonJs.timeStrToTimeStamp(value, temp.showFormat) : null;
          } else if (temp.type === "attachment") {
            Object.keys(temp.multiFile).forEach(attachment => {
              dtTemp[attachment] = temp.multiFile[attachment];
            });
          } else {
            dtTemp[`${temp.name}`] = value ? value : null;
          }
        }
        if (JSON.stringify(dtTemp) !== "{}") dtAry.push(dtTemp);
      }
      const delArrys = this.delDtData[`${dtCode}`];
      const res = {
        [`dgList`]: {
          [`${this.dtName}`]: dtAry.length === 0 ? null : JSON.stringify(dtAry),
        },
        [`dgDeletedIds`]: {
          [`${this.dtName}`]: delArrys ? delArrys.join(",") : null,
        }
      }
      return res;
    },

    // 是否可空
    isNullable: function (list) {
      if (list && list.length > 0) {
        for (let i = 0; i < list.length; i++) {
          const element = list[i];
          try {
            if (element.condition && element.condition[0] && !element.condition[0].nullable) {
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

    //添加和删除附件
    updateAppendix: function (event, attachmentFile, condition, type, index) {
      const $this = this;
      var obj = event.target;
      let tempName = condition.name;
      if (!condition.multiFile) condition.multiFile = {}
      const { [`${tempName}MultiFileIds`]: multiFileIds = [], [`${tempName}FileDeleteIds`]: deleteIds = [], [`${tempName}FileAddPaths`]: addPaths = [], [`${tempName}MultiFileNames`]: fileNames = [], [`${tempName}MultiFileIcons`]: fileIcons = [] } = condition.multiFile;

      if (type != "delete") {
        const attachData = attachmentFile.data;
        const attachPath = attachData.path;
        const attachName = attachPath.slice(attachPath.lastIndexOf("\\") + 1);
        addPaths.push(attachPath);
        fileNames.push(attachName);
        fileIcons.push(attachData.fileIcon);
      } else if (type == "delete") {
        const { name, id } = attachmentFile;
        const index = fileNames.indexOf(name);
        // 删除附件的id
        multiFileIds.forEach((item) => { if (item === id) deleteIds.push(item) });
        if (index > -1) {
          addPaths.splice(index, 1);
          fileNames.splice(index, 1);
          fileIcons.splice(index, 1);
          remove(multiFileIds, (n) => { return n === id })
        }
      }
      condition.multiFile[`${tempName}FileDeleteIds`] = deleteIds;
      condition.multiFile[`${tempName}FileAddPaths`] = addPaths;
      condition.multiFile[`${tempName}MultiFileNames`] = fileNames;
      condition.multiFile[`${tempName}MultiFileIcons`] = fileIcons;
      condition.value = fileNames;
      condition.valueId = multiFileIds;

      obj.value = null;
    },
  },
  //引用组件
  components: {
    inputTagment,
    upload
  }
};
</script>


// WEBPACK FOOTER //
// src/components/dataGrid/index.vue