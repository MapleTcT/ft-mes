<template>
  <div>
    <!-- <transition name="slide-fade"> -->
    <div v-if="showRefer">
      <htmlPanel
        v-if="showRefer"
        :params="{url: referConfig.url}"
      ></htmlPanel>
      <!-- <htmlPanel v-if="showRefer" :params="{url:'/vue/member.html#/'}"></htmlPanel> -->
    </div>
    <!-- </transition> -->
    <div class="m_search">
      <!-- <form onsubmit="return false" :exporturl="this.$parent.basicUrl"> -->
      <div class="m_box m_query">
        <div
          class="ms_select dropdown"
          id="dropdown"
          @click="actionSheet"
        >
          <div
            id="inputSelect"
            class="input_select flex-item"
          >
            <span id="selectContent">{{showActionVal}}</span>
            <img
              src="../../assets/images/btn_xl.png"
              alt="下拉按钮"
            >
          </div>
        </div>
        <div class="flex-item ml">
          <div
            class="ms_input"
            v-for="(item,index) in condition"
          >

            <searchInput
              :ref="item.pickerName?item.pickerName:item.name"
              :params="item"
              curShow='top'
            ></searchInput>
          </div>
        </div>
      </div>
      <div class="m_box m_control">
        <span
          :class="(hidden && !curRefer)?'ms_query ms_query_small':'ms_query'"
          v-if="propListType=='flow'"
        >
          <a
            href="javascript:;"
            v-if="!curRefer"
            v-on:click="getSearchData"
            data-id="pending"
            class="coloum2"
          >
            <label>
              <i class="icon"></i>仅查待办
            </label>
          </a>
          <a
            href="javascript:;"
            v-on:click="getSearchData"
            data-id="query"
            :class="curRefer?'coloum1':'coloum2'"
          >
            <label>
              <i class="icon"></i>查询
            </label>
          </a>
        </span>
        <span
          class="ms_query"
          v-if="propListType=='basic'"
          v-on:click="getSearchData"
        >
          <a class="coloum1">
            <label>
              <i class="icon"></i>查询
            </label>
          </a>
        </span>
        <span
          :class="(hidden && !curRefer)?'ms_filter ms_filter_right':'ms_filter'"
          @click="openSidePanel"
        >
          <i></i>
        </span>

        <!--头部按钮-->
        <HeadBtn v-if="hidden && !curRefer"></HeadBtn>
      </div>
      <mt-actionsheet
        :actions="actionData"
        v-model="sheetVisible"
      ></mt-actionsheet>
      <sidePanel
        :class="{'hideSidePanel':!sidePanelVisible}"
        :propSearchConfigData="propSearchConfigData"
        :propCloseSidePanel="closeSidePanel"
      ></sidePanel>
      <!-- </form> -->
    </div>
  </div>
</template>

<!-- Add "scoped" attribute to limit CSS to this component only -->

<style lang="less">
@import "../../assets/css/searchBar.less";
.slide-fade-enter-active {
  transition: all 0.3s ease;
}
.slide-fade-leave-active {
  transition: all 0.8s cubic-bezier(1, 0.5, 0.8, 1);
}
.slide-fade-enter, .slide-fade-leave-to
/* .slide-fade-leave-active for below version 2.1.8 */ {
  transform: translateX(10px);
  opacity: 0;
}
</style>

<script>
import sidePanel from "@/components/listSearch/sidePanel"; //侧边栏
import htmlPanel from "@/components/htmlPanel"; // iframe
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import filtration from "@/assets/js/util/filtration"; // 搜索筛选
import { Toast, Indicator } from "mint-ui";
import searchInput from "@/components/listSearch/searchInput";
import HeadBtn from "@/components/HeadBar/HeadBtn";

export default {
  name: "searchBar",
  data() {
    return {
      searchInpVal: "",
      showActionVal: "",
      actionData: [],
      sheetVisible: false,
      sidePanelVisible: false,
      condition: [],
      dateVal: "",
      queryData: [],
      eventBindConfig: [],
      startdate: "",
      enddate: "",
      curRefer: false, // 当前为参照
      referConfig: {},
      showRefer: false, // 是否显示参照
      hidden: false //是否显示头部
    };
  },
  props: ["propListType", "propSearchConfigData", "updatePanelActive"],
  //数据监听
  watch: {
    propSearchConfigData: function(newVal, oldVal) {
      let $this = this;
      this.curRefer = newVal.isRefer;
      this.queryData = newVal.query;
      this.actionData = this.getQueryList(newVal.query);
      this.setActionVal(this.actionData[0]); //默认选中第一项
      $this.eventBindConfig = _commonJs.getEventBindConfig(newVal.query, true); //获取额外的事件绑定配置
      newVal.url = this.$parent.basicUrl;
      $this.$nextTick(function() {
        // $this.bindFunc();
      });
    },
    sheetVisible: function(newVal, oldVal) {
      this.updatePanelActive(newVal);
    }
  },
  beforeMount() {
    if(window._PAGECONFIG && window._PAGECONFIG.navBar){
      this.hidden = window._PAGECONFIG.navBar.hidden?window._PAGECONFIG.navBar.hidden:false;
    }
  },
  mounted() {
    const $this = this;
  },
  methods: {
    getQueryList: function(data) {
      var $this = this;
      var queryList = [];
      for (var i = 0; i < data.length; i++) {
        var item = data[i].element;
        item.cellCode = data[i].cellCode;
        var queryCon = $this.getQueryCondition(data[i]);
        var info = {
          name: item.namekey,
          condition: queryCon,
          method: $this.setActionVal
        };
        queryList.push(info);
      }
      return queryList;
    },

    // 绑定自定义事件
    bindFunc: function() {
      const $this = this;
      // 判断是否为api里的事件
      if ($this.eventBindConfig.length > 0) {
        let evt = $this.eventBindConfig;
        for (let i = 0; i < evt.length; i++) {
          const elt = evt[i];
          if (elt.fbody && elt.fbody.indexOf("bapApi") > -1) {
            new Function(elt.split("bapApi.")[1])();
          } else if (elt.fbody && elt.fbody.indexOf("function") > -1) {
            _bapApi.bindEventConfig([elt], "searchBar");
          }
        }
      }
    },

    //清空搜索框
    emptySearchInp: function(obj) {
      if (obj.type == "section") {
        obj.value1 = "";
        obj.value2 = "";
      } else if (obj.type == "choose") {
        this.$store.commit("updateData", {
          name: "curChooseList",
          value: []
        });
      } else if (obj.type == "select") {
        obj.value = "";
        obj.valueId = "";
      } else if (obj.type == "date") {
        if (obj.pickerName == "pickerEnd") {
          this.enddate = this.curend;
        } else if (obj.pickerName == "pickerStart") {
          this.startdate = this.curstart;
        }
        obj.value = "";
      } else {
        obj.value = "";
      }
    },
    // 打开action sheet
    actionSheet: function() {
      this.sheetVisible = true;
    },
    //查询下拉菜单
    setActionVal: function(obj) {
      const $this = this;
      this.showActionVal = obj.name;
      this.condition = obj.condition;
      // setTimeout(function(){

      // })
      $this.$nextTick(function() {
        $this.bindFunc();
        $this.$parent &&
          $this.$parent.redrawListPanel &&
          $this.$parent.redrawListPanel();
      });
    },
    //打开侧边菜单
    openSidePanel: function() {
      this.sidePanelVisible = true;
      this.updatePanelActive(true);
    },
    //关闭侧边菜单
    closeSidePanel: function() {
      this.sidePanelVisible = false;
      this.updatePanelActive(false);
    },
    //获取查询条件配置信息
    getQueryCondition: function(item) {
      const data = item.element;
      var $this = this;
      var queryType = this.getQueryType(data.showType);
      var con = [];
      // var numberType = ["INTEGER", "LONG", "DECIMAL", "MONEY"];
      // var isNumber = numberType.indexOf(data.columnType) > -1; //数字型
      var temp = {
        type: queryType,
        placeholder: "",
        clickEvent: "",
        readonly: data.readonly,
        value: "",
        showformat: data.showFormat,
        code: data.code,
        exp: data.exp,
        layRec: data.layRec,
        name: data.name,
        systemCode: data.fill,
        columnType: data.columnType,
        columnName: data.columnName,
        isPrefer: false, // 是否是参照
        multable: data.multable,
        cellCode: data.cellCode,
        namekey: data.namekey,
        isTreeSystemCode: data.isTreeSystemCode, //是否系统编码树形
        isOnlyLeaf: data.isOnlyLeaf, //系统编码树形单选是否仅可选叶子节点       
        asscolumnname: item.asscolumnname,
      };
      switch (queryType) {
        case "date":
          var transform = {
            YMD: "date",
            YM: "date",
            Y: "date",
            DEFAULT: "date",
            YMD_HMS: "datetime",
            YMD_HM: "datetime",
            YMD_H: "datetime",
            code: data.code
          };

          var transformat = { //为了实现查询时拼接完成的日期和时间
            YMD: "date",
            YM: "yearMonth",
            Y: "year",
            DEFAULT: "date",
            YMD_HMS: "datetime",
            YMD_HM: "datetimehm",
            YMD_H: "datetimeh"
          };

          con = [
            {
              ...temp,
              clickEvent: $this.selectDate,
              readonly: true,
              name: `${data.name}_start`,
              placeholder: "请选择开始时间",
              pickerName: `${data.key}_pickerStart`,
              datetype: transform[data.showFormat],
              datatypeformat: transformat[data.showFormat]
            },
            {
              ...temp,
              clickEvent: $this.selectDate,
              readonly: true,
              name: `${data.name}_end`,
              placeholder: "请选择结束时间",
              pickerName: `${data.key}_pickerEnd`,
              datetype: transform[data.showFormat],
              datatypeformat: transformat[data.showFormat]
            }
          ];
          break;
        case "text":
          con = [{ ...temp, placeholder: "请输入" + data.namekey }];
          break;
        case "section":
          con = [{ ...temp, placeholder: "", value1: "", value2: "" }];
          break;
        case "select":
          con = [
            {
              ...temp,
              clickEvent: $this.showPickerPop, // 是否参照
              readonly: true,
              placeholder: "请选择" + data.namekey,
              value: "",
              pickerName: "pick_" + data.key,
              slots: [],
              isShowPicker: false
            }
          ];

          // 参照
          if (data.isrefselect) {
            con[0].clickEvent = new Function();
            con[0].referEvent = $this.openRefSelect;
            con[0].referenceview = data.referenceview;
            con[0].isPrefer = true;
            con[0].readonly = false;
          }

          // 系统编码树形
          if (data.columnType == 'SYSTEMCODE' && data.isTreeSystemCode) {
            con[0].readonly = false;
          }

          // 布尔
          if (data.selfType == "BOOLEAN") {
            let arr = ["是", "否"];
            con[0].slots = [
              {
                flex: 1,
                valueIndex: 0,
                defaultIndex: 0,
                value: arr[0],
                valueKey: 0,
                values: arr
              }
            ];
            con[0].selectData = ["1", "0"];
          }

          break;
        case "choose":
          con = [
            {
              ...temp,
              clickEvent: $this.showMember,
              readonly: true,
              placeholder: "请选择" + data.namekey.substring(0, 2),
              member: 1
            }
          ];
          break;
      }
      return con;
    },
    //获取查询类型
    getQueryType: function(ctype) {
      var type;
      if (ctype == "DATE" || ctype == "DATETIME") {
        type = "date";
      } else if (ctype == "MONEY") {
        type = "section";
      } else if (ctype == "SELECTCOMP" || ctype == "SELECT") {
        type = "select";
      } else if (ctype == "choose") {
        type = "choose";
      } else {
        type = "text";
      }
      return type;
    },
    //显示下拉
    showPickerPop: function(data) {
      // 下拉接口
      const $this = this;
      if (
        (!data.slots || (data.slots && data.slots.length == 0)) &&
        data.systemCode &&
        data.systemCode.fillContent
      ) {
        $this.getData(
          {
            url: "/foundation/systemCode/systemCodeJson.action",
            type: "post",
            data: {
              systemEntityCode: data.systemCode.fillContent
            }
          },
          function(res) {
            // console.log(res);
            if (res) {
              let arr = [""],
                keys = [""];
              for (const key in res) {
                if (res.hasOwnProperty(key)) {
                  const element = res[key];
                  arr.push(element);
                  keys.push(key);
                }
              }
              data.selectData = keys;
              data.slots = [
                {
                  flex: 1,
                  valueIndex: 0,
                  defaultIndex: 0,
                  value: arr[0],
                  valueKey: 0,
                  values: arr
                }
              ];
              data.isShowPicker = true;
            }
          }
        );
      } else {
        data.isShowPicker = true;
      }
    },
    //下拉选择事件
    pickerEvent: function(type, value, name, id) {
      var con = this.condition[0];
      if (type == "comfirm") {
        con.value = value;
        if (con.selectData && con.slots) {
          let index = con.slots[0].values.indexOf(value);
          con.valueId = con.selectData[index];
        }
      }
      con.isShowPicker = false;
    },

    // 打开参照
    openRefSelect: function(data) {
      const $this = this;
      if (!data.referenceview.url && data.referenceview.url == "") {
        this.myToast("无效地址");
        return false;
      }
      this.showRefer = true;
      // 获取参照参数，跨公司 回填到url
      $this.referConfig.url = _commonJs.resolveReferUrl(
        data.referenceview.url,
        data
      );
      Indicator.open({
        text: "加载中...",
        //文字
        spinnerType: "fading-circle"
        //样式
      });
      this.$nextTick(function() {
        // 回调方法
        let iframe = document.getElementById("newPage");
        iframe.onload = function() {
          Indicator.close();
          if (iframe) {
            // 去掉原生头部
            // try{
            //     window.mobilejs.open_dialog();
            // }catch(err){

            // }
            // 注册方法
            // 选择回调方法
            let code = "";
            if (data.referenceview.code)
              code = data.referenceview.code.replace(/\./gi, "_");
            iframe.contentWindow[`_callBack_ec_${code}`] = function(list) {
              // 回调赋值
              $this.condition = _commonJs.referCallBackObject(
                list,
                $this.condition
              );
              setTimeout(function() {
                $this.showRefer = false;
              }, 200);
              // 恢复原生头部
              // try{
              //     window.mobilejs.close_dialog();
              // }catch(err){

              // }
            };

            // 返回按钮方法
            iframe.contentWindow[`_close_ec_${code}`] = function() {
              // 关闭iframe
              setTimeout(function() {
                $this.showRefer = false;
              }, 200);
              // 恢复原生头部
              // try{
              //     window.mobilejs.close_dialog();
              // }catch(err){

              // }
            };
          }
        };
      });
    },

    showMember: function(item) {
      // console.log(item);
      let curPage = "";
      let text = item.placeholder;
      if (text.indexOf("人员") > -1) {
        curPage = "member";
      } else if (text.indexOf("部门") > -1) {
        curPage = "department";
      } else if (text.indexOf("岗位") > -1) {
        curPage = "station";
      }
      this.$store.commit("updateData", {
        name: "curShowPage",
        value: curPage
      });
    },

    // enter键查询
    searchByEnter: function() {
      var keycode = event.keyCode;
      var searchName = event.target.value;
      //keycode是键码，13也是电脑物理键盘的 enter
      if (keycode == "13") {
        console.log(2);
        event.preventDefault();
        this.getSearchData({
          dataId: "query"
        });
        // 失去焦点
        event.target.blur();
      }
    },

    // 查询数据
    getSearchData: function(config) {
      let $this = this;
      let temp;
      let name = this.condition[0].name;
      if (this.$refs[name] && !this.$refs[name][0].validate()) {
        return;
      }

      if (config && config.url && config.data) {
        temp = config;
        // this.$parent.basicUrl = url;
        // this.$parent.postData = data;
      } else {
        let basicUrl;
        if (this.propListType == "basic") {
          basicUrl = this.propSearchConfigData.searchUrl;
        } else if (this.propListType == "flow") {
          let dataId = event.target.parentElement.getAttribute("data-id");
          dataId = config && config.dataId ? config.dataId : dataId;
          basicUrl = this.propSearchConfigData.searchUrl[dataId];
          window.localStorage.setItem("searchType", dataId);
        }
        temp = filtration(this.condition, basicUrl);
      }

      if (!temp) {
        return false;
      }

      this.$parent.basicUrl = temp.url;
      temp.data = this.GetRequest(`${temp.data}`);
      temp.data["page.pageNo"] = 1;
      temp.data["page.pageSize"] = 20;
      this.$parent.postData = temp.data;
      this.$parent.getLoadData(
        this.$parent.basicUrl +
          `?${$this.propSearchConfigData.crossCompanyFlag}`,
        // this.$parent.basicUrl ,
        "",
        this.$parent.postData
      );
    }
  },
  computed: {
    getNewSelectData() {
      return this.$store.state.curChooseList
        ? this.$store.state.curChooseList.name
        : "";
    },
    getStoreItem: function(value) {
      return this.$store.state.curChooseList;
    }
  },
  //引用组件
  components: {
    sidePanel,
    htmlPanel,
    searchInput,
    HeadBtn
  }
};
</script>



// WEBPACK FOOTER //
// src/components/listSearch/searchBar.vue