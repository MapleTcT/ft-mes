<template>
  <Iform
    name="form"
    ref="form"
    :action="formUrl"
  >
    <div class="m_box">
      <ul class="m_list">
        <li class="bt list-space list-flex">
          <span class="ml_name">{{userInfo.userName}}</span>
          <span class="ml_time">{{userInfo.createTime}}</span>
          <div
            v-if="layoutType == 'flow'"
            class="ml_position fr"
            id="selectPosition"
            @click="showPickPos"
          >
            <!-- <label id="selectPositionShow">{{userInfo.positions?userInfo.positions[0].name:''}}</label> -->
            <label id="selectPositionShow">{{pickPos.value}}</label>
            <input
              type="hidden"
              id="selectPositionGet"
              value=""
            >
            <a
              v-if="showUserPickpos && !isReadOnly"
              class="m_dropdown"
            ></a>
          </div>
          <custom-picker
            v-if="showUserPickpos && !isReadOnly"
            :name="pickPos.name"
            :isShow="pickPos.isShow"
            :slots="pickPos.slots"
            :value="pickPos.value"
            v-on:pickercallback="pickerPosEvent"
          >
          </custom-picker>
        </li>
        <template v-for="(item, index) in layout">

          <!-- 表格 datagrid -->
          <template v-if="item[0] && item[0].type == 'DATAGRID'">
            <li class="list-nobg" data-id="datagrid" :data-name="item[0].datagrid.datagridCode">
              <template v-for="(tdlist, k) in item[0].datagrid.elements">
                <!-- 表头 -->
                <span class="td-head">
                  <!-- {{item[0].datagrid.tdName}} -->
                  <h2>{{item[0].datagrid.tdName}}{{k+1}}</h2>
                  <span
                    v-if="!isReadOnly && item[0].datagrid && item[0].datagrid.btnConfig && item[0].datagrid.btnConfig.DELETEROW"
                    v-on:click="delDataGrid(item[0].datagrid.elements, k)"
                    class="td-del-btn"
                  >删除</span>
                </span>
                <ul>
                  <li v-if="!(gridItem.condition && gridItem.condition.columnType == 'LONGTEXT')"
                    v-for="(gridItem, j) in tdlist"
                    class="m-flex list-space bt"
                    >
                    <span
                      :style="gridItem.condition && getCssStyle(gridItem.condition.cssstyle, gridItem.condition)"
                      class="flex-item lt"
                    ><em
                        v-if="gridItem.condition && gridItem.condition && !gridItem.condition.nullable"
                        class="mustFill"
                      >*</em>{{gridItem.label}}</span>
                    <span
                      :style="gridItem.condition && getCssStyle(gridItem.condition.cssstyle, gridItem.condition)"
                      :class="getUnitByFormat(gridItem.condition)?'pr flex-item rt':'flex-item rt'"
                    >

                      <inputTagment
                        :ref="`${item[0].datagrid.datagridCode}_${k+1}_${gridItem.condition.name}`"
                        v-if="gridItem.condition"
                        :params.sync="gridItem.condition"
                      ></inputTagment>
                      <!-- 单位 -->
                      <i class="m-unit">{{getUnitByFormat(gridItem.condition)}}</i>
                    </span>
                  </li>

                  <!--长文本类型-->
                  <li v-if="gridItem.condition && gridItem.condition.columnType == 'LONGTEXT'"
                    v-for="(gridItem, j) in tdlist"
                    class="m-longtext list-space bt"
                  >
                    <span
                      :style="gridItem.condition && getCssStyle(gridItem.condition.cssstyle, gridItem.condition)"
                      class="flex-item lt"
                    ><em
                        v-if="gridItem.condition && gridItem.condition && !gridItem.condition.nullable"
                        class="mustFill"
                      >*</em>{{gridItem.label}}</span>
                    <span
                      :style="gridItem.condition && getCssStyle(gridItem.condition.cssstyle, gridItem.condition)"
                      :class="getUnitByFormat(gridItem.condition)?'pr flex-item lt':'flex-item lt'"
                    >

                      <inputTagment
                        :ref="gridItem.condition.name"
                        v-if="gridItem.condition"
                        :params.sync="gridItem.condition"
                      ></inputTagment>
                      <!-- 单位 -->
                      <i class="m-unit">{{getUnitByFormat(gridItem.condition)}}</i>
                    </span>
                  </li>
                </ul>
              </template>
              <!-- 表尾 -->
              <span
                class="td-footer list-space mt20"
                v-if="!isReadOnly && item[0].datagrid.btnConfig && item[0].datagrid.btnConfig.ADDROW"
              >
                <span class="abb-icon-btn" v-on:click="addDataGrid(item[0].datagrid.elements, item[0].datagrid.initData)">
                    <i class="add-icon"></i>
                    <span class="td-add-btn">增加明细</span>
                </span>
              </span>

            </li>
          </template>

          <!-- 附件 -->
          <li
            class="mt20 list-space"
            v-else-if="item[0] && item[0].type == 'PROPERTYATTACHMENT' || item[1] && item[1].type == 'PROPERTYATTACHMENT'"
          >
            <template v-for="(tdItem, tdIndex) in item">
              <div class="m_box">
                <div class="">
                  <span
                    class="ml_text"
                    v-if="tdItem.condition && tdItem.condition[0].type=='text'"
                  ><em
                      v-if="isNullable(item)"
                      class="mustFill"
                    >*</em>{{tdItem.label}}</span>
                  <div
                    class="m_attachment"
                    v-if="tdItem.condition && tdItem.condition[0].type=='attachment'"
                  >
                    <div
                      class="ma_fileItem pickbox"
                      v-for="(item,i) in fileList"
                    >
                      <img
                        class="file_image fl"
                        src="../../assets/images/file_image.png"
                      >
                      <div class="file_box"><span class="fb_name">{{item.name}}</span><i class="fb_size">{{item.size}}</i></div>
                      <a
                        class="i_cancel fr"
                        @click="deleteFile(i)"
                      ><i>×</i></a>
                    </div>
                    <div class="ma_add">
                      <input
                        v-if="tdItem.condition[0].readonly"
                        disabled
                        type="file"
                        @change="getPickFileResult"
                      >
                      <input
                        v-else
                        type="file"
                        @change="getPickFileResult"
                      >
                      <a class="btn_add"></a>
                    </div>
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
                  <span
                    :style="tdItem.condition[0] && getCssStyle(tdItem.condition[0].cssstyle, tdItem.condition[0])"
                    class="ml_text"
                    v-if="tdItem.condition && tdItem.condition[0].type=='text'"
                  ><em
                      v-if="isNullable(item)"
                      class="mustFill"
                    >*</em>{{tdItem.label}}</span>
                  <selectAppendix
                    v-if="tdItem.condition && tdItem.condition[0].type=='picture'"
                    :ref="tdItem.condition[0].name"
                    :params="{
                                    isPicture: tdItem.condition && tdItem.condition[0].type=='picture'?true:false,
                                    isNullable :isNullable(item),
                                    postUrl:'/foundation/workbench/uploadFile', 
                                    name:tdItem.condition[0].name,
                                    con: tdItem.condition[0]
                                }"
                    v-on:updatePicture="getPickResult"
                    :imgValue="tdItem.condition[0].value"
                  ></selectAppendix>
                </div>
              </div>
            </template>
          </li>

          <!-- 列表 -->
          <template v-else>
            <li v-if="!(item[1] && item[1].condition && item[1].condition[0] && item[1].condition[0].type =='longtext')"
                :class="index != layout.length-1 ? 'm-flex list-space bt': 'm-flex list-space'"
            >
                <template v-for="(tdItem, tdIndex) in item">
                <!-- {{tdItem.condition[0]}} -->
                <!-- label -->
                <span
                    :style="tdItem.condition[0] && getCssStyle(tdItem.condition[0].cssstyle, tdItem.condition[0])"
                    v-if="tdItem.label && tdItem.label != ''"
                    :class="tdItem.condition[0] && tdItem.condition[0].colspan==2?'flex-item block': 'flex-item lt'"
                >
                    <label :name="tdItem.name"><em
                        v-if="isNullable(item)"
                        class="mustFill"
                    >*</em>{{tdItem.label}}</label>
                </span>
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
            <li v-else
                :class="index != layout.length-1 ? 'm-longtext list-space bt': 'm-longtext list-space'"
            >
                <template v-for="(tdItem, tdIndex) in item">
                <!-- {{tdItem.condition[0]}} -->
                <!-- label -->
                <span
                    :style="tdItem.condition[0] && getCssStyle(tdItem.condition[0].cssstyle, tdItem.condition[0])"
                    v-if="tdItem.label && tdItem.label != ''"
                    :class="tdItem.condition[0] && tdItem.condition[0].colspan==2?'flex-item flex-longlabel block': 'flex-item flex-longlabel lt'"
                >
                    <label :name="tdItem.name"><em
                        v-if="isNullable(item)"
                        class="mustFill"
                    >*</em>{{tdItem.label}}</label>
                </span>
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
                    <!-- 单位 -->
                    <i class="m-unit">{{getUnitByFormat(tdItem.condition[0])}}</i>
                </span>

                </template>
            </li>
          </template>

        </template>

      </ul>
    </div>
    <input id="operateType" hidden type="text" :value="operatetype" />
    <input id="workFlowVarStatus" hidden type="text" name="workFlowVarStatus" :value="workFlowVarStatus" />
  </Iform>
  <!-- </div> -->
</template>

<script>
import AsyncValidator from "async-validator";
import _commonJs from "../../assets/js/itemList/index.js";
import inputTagment from "@/components/form/inputTagment";
import Iform from "@/components/form/Iform";
import _bapApi from "@/assets/js/itemList/bapApi.js";
import selectPersonnel from "@/components/selectPersonnel";
import {Toast, Indicator, MessageBox} from "mint-ui";
import selectAppendix from "@/components/selectAppendix";

export default {
    name: "editForm",
    data() {
        return {
            layout: "",
            userInfo: {},
            layoutType: "",
            urlParams: "",
            showUserPickpos: false,
            startdate: "",
            isReadOnly: "",
            enddate: "",
            dateVal: "",
            datagrid: "",
            initDatagrid: "",
            delDtData: {},
            onSaveEvent: "",
            operatetype:"",
            onLoadEvent: "",
            slotsAll: {},
            dataGridBtn: {},
            formUrl: "", // form 地址
            eventBindConfig: "",
            editData: "", //编辑数据
            pictureFile: {}, // 图片
            attachmentFile: {}, // 附件
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
            workFlowVarStatus: ""
        };
    },
    props: ["params"],
    beforeMount() {
        const $this = this;
        // 查看页面只读
        if(window._PAGECONFIG && window._PAGECONFIG.params){
            $this.isReadOnly = window._PAGECONFIG.params.allReadOnly;
            $this.layoutType = window._PAGECONFIG.params.type;
        };

    },

    mounted() {
        var $this = this;

        // 获取地址参数
        $this.urlParams = $this.GetRequest(window.location.href)
        // var url = "../static/data/editLayoutData.json"; //编辑视图
        var url = $this.params.layoutUrl;
        //   "/msService/ec/view/viewJson?view.code=wupin_1.0.0.00_wupin_edit__mobile__&entity.code=wupin_1.0.0.00_wupin";

        // 格式起始和结束时间
        _commonJs.startAndEndTime($this, 1990);

        $this.getUserInfo(); // 获取用户信息

        $this.getLayoutData(url); // 获取布局数据

        // $this.exportFunc(`getLayoutData`, $this.getLayoutData); // 注册全局方法

    },
    methods: {
        // 获取布局数据
        getLayoutData: function (url) {
            const $this = this;
            $this.getData({
                    url: url
                },
                function (data) {
                    //布局json
                    data = JSON.parse(data);
                    // $this.layout = data;
                    console.log(data);
                    $this.datagrid = data.dataGrid; // datagrid
                    $this.layout = $this.sortLayout(data.elements); // 单元格布局分组
                    $this.onSaveEvent = data.pageConfig.onsave; // 自定义保存事件
                    $this.onLoadEvent = data.pageConfig.onload; // 自定义onload
                    $this.eventBindConfig = _commonJs.getEventBindConfig(
                        data.elements,
                        true,
                        true
                    ); //获取额外的事件绑定配置
                    console.log($this.layout);
                    $this.$nextTick(function () {
                        // 获取数据
                        let detailUrl = window._PAGECONFIG && window._PAGECONFIG.layoutDataUrl;
                        $this.getDetailData(detailUrl, function(){
                            // 加载onload事件
                            $this.$nextTick(function () {
                                if ($this.onLoadEvent) new Function($this.onLoadEvent)(); 
                            });
                        }); 
                        // 获取datagrid数据
                        $this.getDatagridByLayout('', function(dtEl){
                            // 渲染renderOver
                            $this.$nextTick(function () {
                                if(dtEl.renderOver) new Function(dtEl.renderOver)();
                            });
                        }); 
                        try {
                            // datagrid 初始化ptPageInit
                            if(data.dataGrid){
                                for (let i = 0; i < data.dataGrid.length; i++) {
                                    const pt = data.dataGrid[i];
                                    if(pt.ptPageInit) new Function(pt.ptPageInit)();
                                }
                            }

                            $this.initConfigEvent();
                            
                        } catch (e) {
                            console.log("自定义代码出错！");
                        }
                    });
                }
            );
        },

        initConfigEvent: function(){
            const $this = this;
            // 判断是否为api里的事件
            if ($this.eventBindConfig.length > 0) {
                let evt = $this.eventBindConfig;
                for (let i = 0; i < evt.length; i++) {
                    const elt = evt[i];
                    if (elt.fbody && elt.fbody.indexOf("bapApi") > -1) {
                        new Function(elt.split("bapApi.")[1])();
                    } else if (elt.fbody && elt.fbody.indexOf("function") > -1) {
                        _bapApi.bindEventConfig([elt]);
                    }
                }
            }
        },

        // 获取数据信息
        getDetailData: function(url, callback){
            const $this = this;
            $this.getData({
                // url: '/wupin/wupin/wupin/edit2?entityCode=wupin_1.0.0.00_wupin&buttonCode=wupin_1.0.0.00_wupin_list_BUTTON_edit&namespace=list_operate_wupin_list&id=1084&__pc__=bGlzdF9lZGl0X21vZGlmeV93dXBpbl8xLjAuMC4wMF93dXBpbl9saXN0fA__&dataJson=1#'
                url: url
                // url: "../static/data/edit.json"
            },function(res){
                if(res && $this.layout){
                    $this.editData = res;
                    for (let i = 0; i < $this.layout.length; i++) {
                        const elt = $this.layout[i];
                        for (let j = 0; j < elt.length; j++) {
                            const et = elt[j];
                            if(!et.label){
                                _commonJs.getValueByKeyEdit(et.condition[0], "", $this);
                            }
                            
                        }
                        
                    }
                }

                if(callback && typeof callback == "function"){
                    callback();
                }
                
            })
        },

        // 获取datagrid数据
        getDataGridData: function(dtEl, url, callback){
            const $this = this;
            let datagridKey = dtEl.config.DataGridCode;
            let dataGridDataUrl = window._PAGECONFIG && window._PAGECONFIG.dataGridDataUrl;
            let alias = window._PAGECONFIG && window._PAGECONFIG.userInfo.modelAlias;
            let dtCode = dtEl.datagridCode;
            let urlParams = $this.urlParams;
            let basciUrl = `${dataGridDataUrl}${dtCode}?datagridCode=${datagridKey}&${alias}.id=${urlParams.id}&rt=json`;
            if(url){
                basciUrl = url;
            }
            $this.getData({
                url: basciUrl,
                type: 'post',
                data: {
                    'dgPage.pageSize': 65536,
                    'dgPage.maxPageSize': 500
                }
                // url: `/wupin/wupin/wupin/data-dg1525423591689?wupin.id=1106&datagridCode=wupin_1.0.0.00_wupin_editdg1525423591689&rt=json`
                // url: `/wupin/wupin/wupin/data-${dtEl.datagridCode}?wupin.id=1106&datagridCode=${dtEl.config.DataGridCode}&rt=json`
            }, function(res){
                if(res && res.result.length > 0){
                    let tempArr = [],tempEl = [];
                    for (let i = 0; i < res.result.length; i++) {
                        const data = res.result[i];
                        tempEl = [];
                        for (let j = 0; j < dtEl.elements[0].length; j++) {
                            const element = dtEl.elements[0][j];
                            let temp = element;
                            _commonJs.getValueByKeyEdit(temp.condition, data, $this);
                            temp.condition.index = i;
                            temp.condition.id = data.id.toString();
                            temp.isloadData = true;
                            temp = JSON.parse(JSON.stringify(element));
                            // if(temp.condition.valueId){
                            //     temp.condition.valueId = {id: temp.condition.valueId};
                            // }
                            
                            tempEl.push(temp);
                            
                        }
                        tempArr.push(tempEl);
                    }
                    dtEl.elements = tempArr;
                    
                }

                if(callback && typeof callback == "function"){
                    callback(dtEl);
                }
               
            })
        },

        //布局单元格分组
        sortLayout: function (data) {
            const $this = this;
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
                datagrid = $this.filterDatagrid(item); // datagrid类型
                nameTit = "";
                if (
                    item.element &&
                    item.element.showType &&
                    item.element.showType.toLowerCase() == "label"
                ) {
                    nameTit = item.name;
                }

                templist = {
                    type: item.element && item.element.columnType,
                    label: nameTit,
                    datagrid: datagrid,
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

        // 头部用户信息
        getUserInfo: function () {
            const $this = this;
            let pageConfig = window._PAGECONFIG;
            if (pageConfig && pageConfig.userInfo) {
                let temp = pageConfig.userInfo;
                let time = temp.createTime ? new Date(temp.createTime.replace(/-/g,"/")) : new Date();
                time = _commonJs.getDateFormat("YMD_HM", time);
                $this.userInfo = {
                    userName: temp.staff.name,
                    createTime: time,
                    staffId: temp.staff.id,
                    positions: temp.positions[0]
                };
                if (temp.positions.length <= 1) {
                    $this.showUserPickpos = false;
                    $this.pickPos.value = temp.positions[0]?temp.positions[0].name:'';
                } else {
                    $this.showUserPickpos = true;
                    let slots = [];
                    for (let i = 0; i < temp.positions.length; i++) {
                        const element = temp.positions[i];
                        slots.push(element.name);
                    }
                    $this.pickPos.value = slots[0];
                    $this.pickPos.slots[0].values = slots;
                    $this.pickPos.positions = temp.positions;
                }

            }
        },
        
        // 展示用户多职位
        showPickPos: function () {
            if(this.isReadOnly){
                return false;
            }
            this.pickPos.isShow = true;
        },

        // 选择对应职位
        pickerPosEvent: function (type, value, name) {
            var con = this.pickPos;
            const $this = this;
            if (type == "comfirm"){
                con.value = value;
                con.positions.forEach(item => {
                    if(item.name == value){
                        $this.userInfo.positions = item;
                    }
                });
            } 
            con.isShow = false;
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
        
        //增加一组diagrid数据
        addDataGrid: function (data, initData) {
            const $this = this;
            if (data) {
                var temp = JSON.parse(JSON.stringify(initData));
                for (let i = 0; i < temp.length; i++) {
                    const element = temp[i];
                    const datagridCode = element.condition.datagridCode;
                    element.condition.newName = `${datagridCode}_${data.length+1}_${element.condition.name}`;
                    element.condition.index = data.length;
                }
                data.push(temp);
                this.$nextTick(function(){//TODO
                    let tdFooterH = document.querySelector(".td-footer").scrollHeight;
                    let ulH = document.querySelector(".list-nobg ul").scrollHeight;
                    let top = document.querySelector(".m_centerBox").scrollTop;
                    document.querySelector('.m_centerBox').scrollTop = top + tdFooterH + ulH;
                    $this.initConfigEvent();
                });

            }
        },

        //删除diagrid中的一条数据
        delDataGrid: function (data, index) {
            const $this = this;
            if(data && data[index] && data[index][0].isloadData){ // 数据加载，删除时记录
                let temp = data[index];
                let tempDt = {};
                let tempId = temp[0].condition.id;
                // for (let i = 0; i < temp.length; i++) {
                //     const element = temp[i];
                //     tempDt[`${element.condition.name}`] = element.condition.valueId?element.condition.valueId:element.condition.value;
                // }
                if(this.delDtData[temp[0].condition.datagridCode]){
                    this.delDtData[temp[0].condition.datagridCode].push(tempId);
                }else{
                    this.delDtData[temp[0].condition.datagridCode] = [tempId];
                }
            }
            if(data){
                const delData = data[index];
                data.splice(index, 1);
                delData.forEach(item => {
                    $this.$refs.form.deleteFileds(item.condition.newName?item.condition.newName:item.condition.name)
                    // this.dispatch("iForm", "form-del", ); 
                });
            }
            // const temp = data.splice(index, 1);

            

        },
        //选择图片
        getPickResult: function (event, pictureFile, con, type) {
            const $this = this;
            var obj = event.target; 
            let tempName = obj.name;
            if(tempName && tempName.indexOf('.') > -1){
                let typeName = window._PAGECONFIG.userInfo.modelAlias;
                tempName = obj.name.replace(`${typeName}.`, '');
            }    
            // $this.pictureFile[`ids2del`] = con.valueId?con.valueId:""; // 删除     
            if(type != "delete"){
                $this.pictureFile[`${tempName}Files.filePath`] = pictureFile?pictureFile:"";
                $this.pictureFile[`${tempName}Files.fileType`] = "pic";
                $this.pictureFile[`${tempName}Files.propertyCode`] = con.propertyCode; // propertyCode
                
                // 区分流程单据和非流程单据
                if(window._PAGECONFIG && window._PAGECONFIG.params && window._PAGECONFIG.params.type == "flow"){
                    $this.pictureFile[`${tempName}Files.type`] = 'Table';  
                }else{
                    $this.pictureFile[`${tempName}Files.type`] = window._PAGECONFIG && window._PAGECONFIG.documentType?window._PAGECONFIG.documentType:'';  
                }
                
            }else if(type == "delete"){
                $this.pictureFile = {};
                $this.pictureFile[`ids2del`] = con.valueId?con.valueId:"";
            }
            
            obj.value = null;
        },

        //选择附件
        getPickFileResult: function (event) {
            var obj = event.target;
            var reads = new FileReader();
            var f = obj.files[0];
            var type = f.name.slice(f.name.lastIndexOf(".") + 1).toLowerCase();
            this.fileList.push({
                name: f.name,
                size: (f.size / 1000).toFixed(0) + "kb"
            });
            obj.value = null; //清空选择
        },
        //删除附件
        deleteFile: function (index) {
            var data = this.fileList;
            data.splice(index, 1);
        },

        // 筛选条件
        filterCondition: function (data) {
            let $this = this,
                condition = [];
            if (data && data.element) {
                condition.push(
                    $this.getdItemdition(data.element, data.sourcepropertyname)
                );
                let funcAll = {};
                condition = [{
                    ...condition[0],
                    colspan: data.colspan,
                    cssstyle: data.cssstyle,
                    cssstyle_container: data.cssstyle_container,
                    funcbody: data.funcbody,
                    funcname: data.funcname,
                    callbackbody: data.callbackbody,
                    callbackname: data.callbackname
                }];
            }

            return condition;
        },

        // 筛选datagrid
        filterDatagrid: function (data) {
            let $this = this,
                datagrid = "",
                conditions = [];
            // dataGrid = $this.datagrid;
            if (
                data &&
                data.element &&
                $this.datagrid &&
                data.element.columnType == "DATAGRID"
            ) {
                for (let i = 0; i < $this.datagrid.length; i++) {
                    const td = $this.datagrid[i];
                    if (td.config && td.config.code == data.element.code) {
                        let el = td.elements;
                        conditions = [];
                        // 获取datagridcode
                        let datagridCode = td.name;
                        let targetModel = td.targetModel;
                        try {
                            for (let j = 0; j < el.length; j++) {
                                const temp = el[j];
                                if(typeof temp.isreadonly == "undefined") temp.isreadonly = false;
                                temp.readonly = temp.isreadonly;
                                let condt = $this.getdItemdition(temp);
                                condt = {
                                    ...condt,
                                    newName: `${datagridCode}_${i+1}_${condt.name}`,
                                    isHidden: temp.ishide,
                                    callbackbody: temp.callbackbody,
                                    callbackname: temp.callbackname,
                                    cssstyle: temp.cssstyle,
                                    funcbody: temp.funcbody,
                                    funcname: temp.funcname,
                                    showFormatFunc: temp.showFormatFunc,
                                    rules: window.validataJson && window.validataJson[datagridCode] ? $this.getRules(window.validataJson[datagridCode], temp.key, temp) : [],
                                    datagridCode: datagridCode,
                                    index: i
                                };
                                conditions.push({
                                    label: temp.label,
                                    condition: condt
                                });
                            }
                        } catch (error) {
                            console.log(error);
                        }

                        datagrid = {
                            elements: [conditions],
                            datagridCode: datagridCode,
                            targetModel: targetModel,
                            config: td.config,
                            tdName: td.datagridName,
                            btnConfig: $this.getdataGridBtn(td.buttons),
                            initData: JSON.parse(JSON.stringify(conditions)),
                            renderOver: td.renderOver
                        };
                        break;
                    }
                }
            }

            return datagrid;
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

        // 构造condition
        getdItemdition: function (data, referCode) {
            var $this = this;
            var queryType = this.getQueryType(data);
            var con = [];
            var numberType = ["INTEGER", "LONG", "DECIMAL", "MONEY"];
            var isPassword = "PASSWORDFIELD".indexOf(data.columnType) > 1; // 密码型
            var isNumber = numberType.indexOf(data.columnType) > -1; //数字型
            var temp = {
                type: queryType,
                columnType: data.columnType,
                placeholder: "",
                clickEvent: "",
                readonly: $this.isReadOnly?$this.isReadOnly:data.readonly,
                value: "",
                showFormat: data.showFormat,
                showType: data.showType,
                isNumber: isNumber,
                isPassword: isPassword,
                code: data.code,
                name: data.key,
                namekey: data.namekey,
                // systemCode: data.fill,
                isPrefer: data.isrefselect, // 是否是参照
                multable: data.multable,
                nullable: data.nullable,
                displayfield: data.displayfield,
                newMultiLists: {},
                complex: data.complex,
                propertyCode: data.propertyCode,
                precision: data.precision,
                isHidden: data.hide,
                valueId: "",
                rules: window.formData ? $this.getRules(window.formData, data.key, data) : [],
                regionType: data.regionType,
                isTreeSystemCode: data.isTreeSystemCode, //是否系统编码树形
                isOnlyLeaf: data.isOnlyLeaf, //系统编码树形单选是否仅可选叶子节点

            };

            if(typeof temp.readonly == "undefined") temp.readonly = false;

            switch (queryType) {
                case "label":
                    con = [{
                        ...temp
                    }];
                    break;
                case "date":
                    var transform = {
                        YMD: "date",
                        YM: "date",
                        Y: "date",
                        DEFAULT: "date",
                        YMD_HMS: "datetime",
                        YMD_HM: "datetime",
                        YMD_H: "datetime",
                        isNumber: isNumber,
                        code: data.code
                    };
                    con = [{
                            ...temp,
                            // clickEvent: $this.selectDate,
                            // readonly: true,
                            // placeholder: `请选择${data.namekey}`,
                            placeholder: temp.readonly?'':'请选择',
                            pickerName: `${data.key}_pickerStart`,
                            datetype: transform[data.showFormat]
                        }];
                    break;
                case "text":
                case "longtext":
                    con = [{
                        ...temp,
                        placeholder: temp.readonly?'':"请输入"
                    }];
                    break;
                case "section":
                    con = [{
                        ...temp,
                        placeholder: "",
                        value1: "",
                        value2: ""
                    }];
                    break;
                case "select":
                    con = [{
                        ...temp,
                        clickEvent: $this.showPickerPop, // 是否参照
                        // readonly: true,
                        placeholder: temp.readonly?'':"请选择",
                        fillContent: data.fill,
                        value: "",
                        pickerName: "pick_" + data.key,
                        slots: [],
                        // slots: [{
                        //     flex: 1,
                        //     values: ["开发工程师", "岗位1", "岗位2", "岗位3", "岗位4"]
                        // }],
                        isShowPicker: false
                    }];

                    // 多选
                    if(data.columnType == "SYSTEMCODE"){
                        $this.getMutableData(con[0], data.multable);
                    };

                    // 布尔
                    if(data.columnType == "BOOLEAN"){
                        let arr = ['', '是', '否'];
                        con[0].slots = [
                        {
                            flex: 1,
                            valueIndex: 0,
                            defaultIndex: 0,
                            value: arr[0],
                            valueKey: ['', true, false],
                            values: arr
                            }
                        ];
                    }

                    break;
                case "choose":
                    con = [{
                        ...temp,
                        // clickEvent: $this.showMember,
                        // readonly: true,
                        placeholder: temp.readonly?'':"请选择",
                        member: 1
                    }];
                    break;
                    // dataGrid类型
                case "datagrid":
                    con = "";
                    break;

                    // 附件
                case "attachment":
                    con = [{
                        ...temp
                    }];
                    break;

                    // 图片
                case "picture":
                    con = [{
                        ...temp
                    }];
                    break;

                default:
                    con = [{
                        ...temp
                    }];
                    break;
            }

            if(data.complex){
                $this.getSlotsByFill(con[0], data.multable);
                // $this.getSlotsByFill(con[0], true);
            }

            // 参照
            if (data.isrefselect) {
                con[0].clickEvent = new Function();
                con[0].referenceview = data.referenceview;
            }

            return con[0];
        },

        // 获取规则验证
        getRules: function (rulesData, key, element) {
            var rules = rulesData[key];
            // const keyName = key.split('.');
            var keys = Object.keys(rulesData);
            for (let i = 0; i < keys.length; i++) {
                const ikey = keys[i];
                const temp = ikey.replace('.id', '');
                if(key.indexOf(temp) > -1 && !element.readonly && !element.nullable){
                    rules = rulesData[ikey];
                    break;
                }
            }
            if (rules) {
                let temp = [];
                for (let index = 0; index < rules.length; index++) {
                    const element = rules[index];
                    let message = element.message.replace(/<[^>]+>/g, '');
                    if (element.validatorType.indexOf("required") > -1) {
                        temp.push({
                            required: true,
                            message: message,
                        })
                    } else {
                        temp.push({
                            pattern: element.expression,
                            message: message
                        })
                    }

                }
                return temp;
            } else {
                return [];
            }
        },

        //获取类型
        getQueryType: function (data) {
            var ctype = data.showType;
            var isDatagrid = data.regionType;
            var type;
            if (ctype == "DATE" || ctype == "DATETIME") {
                type = "date";
            }
            // else if (ctype == "MONEY") {
            //     type = "section";
            // }
            else if (ctype == "SELECTCOMP" || ctype == "RADIO" || ctype == "SELECT" || ctype == "MULTSELECT" || ctype == "CHECKBOX") {
                type = "select";
            } else if (ctype == "choose") {
                type = "choose";
            } else if (ctype == "PASSWORDFIELD") {
                type = "password";
            } else if (ctype == "DATAGRID") {
                // datagrid
                type = "datagrid";
            } else if (ctype == "PROPERTYATTACHMENT") {
                // 附件
                type = "attachment";
            } else if (ctype == "PICTURE") {
                // 图片
                type = "picture";
            } else if (ctype == "TEXTAREA" && isDatagrid != "DATAGRID") { //长文本
                type = "longtext"
            } else {
                type = "text";
            }
            return type;
        },

        // 取消 保存 提交数据
        submitData: function (params) {
            const $this = this;
            let operateType = params.operateType;
            let status = params.status;
            // console.log($this.$refs.form);
            if(params && operateType == "cancel"){ // 取消
                MessageBox({
                    title: '提示',
                    message: '确定取消修改?',
                    showCancelButton: true
                }).then(action => {
                    if(action == "confirm"){
                        // 调原生方法返回上一级
                        setTimeout(function(){
                            try {
                                window.mobilejs.webGoBack();
                            } catch (err) {
                                try {
                                    window.history.back();
                                } catch (err) {
                                    
                                }
                            }
                        },1000);
                    }
                });
                return ;
            }

            // 保存数据不校验非空, 校验格式, 作废单据不校验
            // 提交校验都验证
            $this.$refs.form.validate((valid) => {
                if (valid) {
                    $this.getFormData(params);                       
                    console.log("提交成功");
                } else {
                    console.log("校验失败");
                }
            }, operateType, status);

        },
        // 组织表单参数
        getFormData(params) {
            const $this = this;
            let urlParams =  $this.urlParams; // 获取href参数
            let formData = $this.$refs.form.submit(); // 获取表单参数
            let alias = window._PAGECONFIG && window._PAGECONFIG.userInfo.modelAlias;
            let complexNameTit = `${alias}.extraCol`; // complex参数
            let userid =  window._PAGECONFIG && window._PAGECONFIG.userInfo.userid; //user.id参数

            formData.append("viewCode", $this.params.viewCode);

            // 组织图片参数
            if($this.pictureFile){
                for (const key in $this.pictureFile) {
                    if ($this.pictureFile.hasOwnProperty(key)) {
                        const pic = $this.pictureFile[key];
                        formData.append(key, pic);
                    }
                }
                formData.append("__file_upload", true);
            }
             

            formData.append("operateType", params && params.operateType);

            $this.operatetype = params && params.operateType;

            $this.workFlowVarStatus = params && params.status;

            formData.append("bap_validate_user_id", userid);
            formData.append("viewselect", window._PAGECONFIG.mobileViewName);

            // 组织id参数
            urlParams.id && formData.append("id", urlParams.id);
            // formData.append("wupin.id", urlParams.id);


            // 组织complex参数
            if(sessionStorage.getItem(complexNameTit)){
                let complexData = sessionStorage.getItem(complexNameTit);
                complexData = JSON.parse(complexData);
                let complexHml = `<extra-data>`;
                for (let i = 0; i < complexData.length; i++) {
                    const element = complexData[i];
                    if(element.name){
                        let value = typeof element.value == "string" ?element.value:element.value.join(',');
                        if(element.id){
                            value = typeof element.id == "string" ?element.id:element.id.join(',');
                        }
                        complexHml += `<${element.name}><![CDATA[${value}]]></${element.name}>`;
                    }
                }
                complexHml += `</extra-data>`;
                formData.append(complexNameTit, complexHml);
            }
            
            // 人员信息
            if ($this.params.userInfo && $this.userInfo) {
                let userInfo = $this.userInfo;
                let positionLayRec = userInfo.positions?userInfo.positions.positionLayRec:'';
                let createPositionId = userInfo.positions?userInfo.positions.createPositionId:'';
                formData.append(`${alias}.createStaffId`, userInfo.staffId);
                formData.append(`${alias}.createTime`, userInfo.createTime);
                formData.append(`${alias}.positionLayRec`, positionLayRec);
                formData.append(`${alias}.createPositionId`, createPositionId);
            }
            urlParams.deploymentId && formData.append("deploymentId", urlParams.deploymentId);

            //非新增单据需传pendingId，参数pendingId从地址栏获取
            if (urlParams && urlParams.id) {
                urlParams.pendingId && formData.append("pendingId", urlParams.pendingId);              
            }

            //保存的处理意见
            if (params && params.saveCommentParams) { 
                formData.append("workFlowVar.comment", params.saveCommentParams.workFlowVar.comment);
            }

            // 工作流参数
            if (params && params.workParams) {
                formData.append("workFlowVar.outcomeDes", "");
                formData.append(
                    "workFlowVar.outcomeMapJson",
                    params.workParams.workFlowVar.outcomeMapJson
                );

                formData.append(
                    "workFlowVar.comment",
                    params.workParams.workFlowVar.comment
                );
                
                formData.append(
                    "workFlowVar.outcome",
                    params.workParams.workFlowVar.outcome
                );
                formData.append(
                    "activityName",
                    params.workParams.workFlowVar.activityName
                );

                var assignUser = params.workParams.workFlowVar.assignUser;
                

                formData.append("workFlowVarStatus", params && params.status);
                formData.append(`assignStaffSelect_${params.workParams.workFlowVar.outcome}MultiIDs`, assignUser);
                formData.append(`assignStaffSelect_${params.workParams.workFlowVar.outcome}AddIds`, assignUser);

            }

            //   let url = "/wupin/wupin/wupin/edit__mobile__/submit.action";
            let url = $this.params.submitUrl;

            $this.submitDataGridData(formData); //组织datagrid参数

            // 请求接口

            // loading加载
            Indicator.open({
                text: '加载中...',
                //文字
                spinnerType: 'fading-circle',
                //样式
            });
            console.log(formData)

            //自定义保存onSave
            var isSave = true; //为了判断能否阻断onSave事件
            setTimeout(function(){ //添加定时器为了获取operatetype和workFlowVarStatus的值
                if ($this.onSaveEvent) {
                    if ((new Function($this.onSaveEvent)()) == false) {
                        isSave = false;
                        setTimeout(function(){
                            Indicator.close();                            
                        },1000);
                        return false;
                    } else {
                        // new Function($this.onSaveEvent)();
                    }
                }     
            },100);

            setTimeout(function(){
                if (isSave) {
                    $this.$axios
                    .post(url, formData, {
                        headers: {
                            "Content-Type": "multipart/form-data"
                        }
                    })
                    .then(response => {
                        Indicator.close();
                        if(response.data && response.data.dealSuccessFlag){
                            Toast({
                                message: '操作成功',
                                iconClass: 'mintui mintui-success'
                            });
                            let openUrlFunc;
                            if(params.operateType == "save"){
                                // 刷新当前页
                                openUrlFunc = function(){
                                    let href = window.location.href;
                                    if(!urlParams.id){ // 新增
                                        href = href.replace(`deploymentId=${urlParams.deploymentId}`, `pendingId=${response.data.pendingId}`);
                                        href = `${href}&id=${response.data.id}&tableInfoId=${response.data.tableInfoId}`;
                                    }
                                    window.location.href = href;
                                }
                                
                            }else{
                                // 调原生方法返回上一级
                                openUrlFunc = function(){
                                    try {
                                        window.mobilejs.close();
                                    } catch (err) {
                                        try {
                                            window.history.back();
                                        } catch (err) {
                                            
                                        }
                                    }
                                }
                                
                            }
                            setTimeout(function(){
                                openUrlFunc();
                            },1000);
                            
                        }else{
                            Toast({
                                message: '操作失败',
                                iconClass: 'mintui mintui-error'
                            });
                        }
                    })
                    .catch(error => {
                        console.log(error);
                        Indicator.close();
                    });     
                }    
            },200);   
        },

        // 组织datagrid参数
        submitDataGridData: function (formData) {
            const $this = this;
            let layout = $this.layout;
            let dtAry = [],
                dtCode;
            for (let i = 0; i < layout.length; i++) {
                const element = layout[i];
                let dtCode = "", dtModelCode;
                for (let j = 0; j < element.length; j++) {
                    const dt = element[j];
                    let dtTemp = {};
                    if (dt.type == "DATAGRID") {
                        dtAry = [];
                        let dataEl = dt.datagrid.elements;
                        dtCode = dt.datagrid.datagridCode;
                        dtModelCode = dt.datagrid.targetModel;
                        // datagrid
                        for (let k = 0; k < dataEl.length; k++) {
                            const dgEl = dataEl[k];
                            dtTemp = {};
                            for (let m = 0; m < dgEl.length; m++) {
                                var temp = dgEl[m];
                                let nameKey = temp.condition.name;
                                if(nameKey.indexOf(".") > -1){
                                    let nameIndex = nameKey.lastIndexOf(".");
                                    nameKey = nameKey.substring(0,nameIndex);
                                }
                                dtTemp.id = temp.condition.id;

                                dtTemp.rowIndex = `${temp.condition.index}`; //行数

                                if(temp.condition.isPrefer){ // 参照
                                    if(temp.condition.valueId && !temp.condition.multable){
                                        dtTemp[`${nameKey}`] = {id: `${temp.condition.valueId}`};
                                    }
                                    continue;
                                }

                                if(temp.condition.columnType == "SYSTEMCODE" && temp.condition.name.indexOf(".") < 2){ // 系统编码
                                    if (temp.condition.multable) { //系统编码多选
                                        dtTemp[`${nameKey}`] = temp.condition.valueId?`${temp.condition.valueId}`:"";
                                    } else {//系统编码单选
                                        dtTemp[`${nameKey}`] = {id:temp.condition.valueId?`${temp.condition.valueId}`:""};
                                    }
                                } else if (temp.condition.columnType == "BOOLEAN") { //布尔类型
                                    dtTemp[`${temp.condition.name}`] = temp.condition.valueId;
                                } else {
                                    // dtTemp[`${nameKey}`] = {id:temp.condition.valueId};
                                    dtTemp[`${temp.condition.name}`] = temp.condition.value;
                                }
                            }
                            dtAry.push(dtTemp);
                        }
                        
                    }

                }
                let delArrys = $this.delDtData[`${dtCode}`];
                delArrys = delArrys?delArrys:"";
                // push到对象中
                dtCode && formData.append(`dgLists['${dtCode}']`, JSON.stringify(dtAry));
                dtCode && formData.append(`${dtCode}ListJson`, JSON.stringify(dtAry));
                
                window._PAGECONFIG && window._PAGECONFIG.datagridKey && dtCode && formData.append("datagridKey", window._PAGECONFIG.datagridKey);
                dtCode && dtModelCode && formData.append(`${dtCode}ModelCode`, `${dtModelCode}`);
                dtCode && delArrys && formData.append(`dgDeletedIds['${dtCode}']`, delArrys.join(",")); // 删除
            }
        },

        // 获取layout 加载datagrid数据
        getDatagridByLayout: function(url, callback){
            const $this = this;
            let layout = $this.layout;
            for (let i = 0; i < layout.length; i++) {
                const element = layout[i];
                let dtCode = "";
                for (let j = 0; j < element.length; j++) {
                    const dt = element[j];
                    let dtTemp = {};
                    if (dt.type == "DATAGRID") {
                        let dataEl = dt.datagrid.elements;
                        dtCode = dt.datagrid.datagridCode;
                        let configEvent = _commonJs.getEventBindConfig(
                            dataEl[0],
                            true
                        ); //获取额外的事件绑定配置
                        $this.eventBindConfig = $this.eventBindConfig.concat(configEvent);
                        // datagrid
                        $this.getDataGridData(dt.datagrid, url, callback); // 加载datagrid数据
                        
                    }

                }

            }
        },

        // 构造多选列表数据
        getMutableData: function(element, isMultable){
            const $this = this;
            if(!element || !element.fillContent){
                return ;
            }
            if(element && element.fillContent && element.fillContent.fillContent){
                $this.getData(
                {
                    url: "/foundation/systemCode/systemCodeJson.action",
                    type: "post",
                    data: {
                        systemEntityCode: element.fillContent.fillContent
                    }
                },
                function(res) {
                    if (res) {
                        let multables = [];
                        let arrs = [''], keys = [''];
                        for (const key in res) {
                            if (res.hasOwnProperty(key)) {
                                const el = res[key];
                                if(isMultable){
                                    multables.push({label:el, value: key});
                                }else{
                                    arrs.push(el);
                                    keys.push(key);
                                }
                                
                            }
                        }
                        if(isMultable){
                            $this.slotsAll[element.name] = multables;
                        }else{
                            $this.slotsAll[element.name] = [{
                                flex: 1,
                                valueIndex: 0,
                                defaultIndex: 0,
                                value: arrs[0],
                                valueKey: 0,
                                values: arrs,
                                valueKey: keys,
                            }];
                        }

                    }
                });
            }
            
            
        },

        // 构造radio下拉列表 checkbox fill自定义值
        getSlotsByFill: function (element, isMultable) {
            try {
                if (element && element.fillContent) {
                    let fillContent = element.fillContent.fillContent;
                    let keys = [''],
                        values = [''];
                    let multables = [];
                    let orders = element.fillContent.fillOrder.split(",");
                    for (let i = 0; i < orders.length; i++) {
                        const et = orders[i];
                        if (isMultable) {
                            multables.push({
                                label: fillContent[et],
                                value: et
                            });
                        } else {
                            keys.push(et);
                            values.push(fillContent[et]);
                        }

                    }
                    if (isMultable) {
                        element.slots = multables;
                    } else {
                        element.slots = [{
                            flex: 1,
                            valueIndex: 0,
                            defaultIndex: 0,
                            value: values[0],
                            valueKey: keys,
                            values: values
                        }];
                    }

                }
            } catch (error) {
                if (isMultable) {
                    element.slots = [{label:'',value:''}];
                } else {
                    element.slots = [{
                        flex: 1,
                        valueIndex: 0,
                        defaultIndex: 0,
                        value: [''],
                        valueKey: '',
                        values: ['']
                    }];
                }

            }

        },

        //获取样式配置信息
        getCssStyle: function (config, element) {
            return _commonJs.getCssStyle(config, element);
        },

        // 获取单位
        getUnitByFormat: function(condition){
            let numberType = ["INTEGER", "LONG", "DECIMAL"];
            if(condition){
                let fmt = condition.showFormat;
                // let unit = { PERCENT: "%", THOUSAND: "千元", TEN_THOUSAND: "万元" };
                let unit = { PERCENT: "%"};
                if(numberType.indexOf(condition.columnType) > -1){
                    if(fmt && unit[fmt]){
                        return unit[fmt];
                    }else{
                        return '';
                    }
                }
            }

            return '';
        }

    
    },
    //引用组件
    components: {
        inputTagment,
        selectPersonnel,
        Iform,
        selectAppendix
    }
};
</script>
<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less" scoped>
@import "../../assets/css/editView.less";
</style>



// WEBPACK FOOTER //
// src/components/form/editForm.vue