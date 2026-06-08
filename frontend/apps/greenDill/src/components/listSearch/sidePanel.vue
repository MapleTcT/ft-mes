<template>
  <div class="m-sidePanel">
    <htmlPanel
      v-if="showRefer"
      :params="{url:referConfig.url}"
    ></htmlPanel>
    <div
      class="m-layout"
      @click="propCloseSidePanel"
    ></div>
    <div class="m-panel">
      <div class="mp-content">
        <div
          class="mp-box"
          v-for="(item,i) in sortData"
        >
          <h3 class="mp-tit">{{item.displayName}}</h3>
          <div class="mp-tablist">
            <ul v-for="(row,j) in item.sortTemp">
              <li
                v-for="(cell,k) in row"
                v-on:click="handleClick"
              >
                <a
                  :name="cell.name"
                  :class="{'selected':selectItem.indexOf(cell.name)>-1}"
                >{{cell.displayName}}</a>
              </li>
            </ul>
          </div>

        </div>
        <div
          class="mp-box"
          v-for="(item,i) in queryData"
        >
          <h3 class="mp-tit">{{item.name}}</h3>
          <div
            class="mp-con"
            v-for="(con,j) in item.condition"
          >
            <searchInput
              :ref="con.pickerName?con.pickerName:con.name"
              :params="con"
              :propSearchConfigData="propSearchConfigData"
              curShow='side'
            ></searchInput>

          </div>
        </div>
      </div>
      <div class="mp-foot">
        <a
          class="m_button btn_reset"
          @click="resetClassQuery"
        >{{$t('button.reset.text')}}</a>
        <a
          class="m_button btn_finish"
          v-on:click="getSearchData"
        >{{$t('button.finish.text')}}</a>
      </div>
    </div>
  </div>
</template>

<script>
// import htmlPanel from "@/components/htmlPanel"; // iframe
import htmlPanel from "@/components/cellComp/htmlPanel/index.vue"; // iframe
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import filtration from "@/assets/js/util/filtration"; // 搜索筛选
import searchInput from "@/components/listSearch/searchInput";
import { getQueryCondition } from './util.js';

export default {
  name: "sidePanel",
  data() {
    return {
      selectItem: [],
      sortData: [],
      dateVal: "",
      queryData: [],
      eventBindConfig: [],
      startdate: "",
      enddate: "",
      referConfig: {},
      showRefer: false
    };
  },
  props: ["propCloseSidePanel", "propSearchConfigData"],
  //数据监听
  watch: {
    propSearchConfigData: function (newVal, oldVal) {
      let $this = this;
      this.queryData = this.getQueryData(newVal.query);
      this.sortData = this.getSortData(newVal.dataGroupProperty);
    }
  },
  mounted() {
    const $this = this;
    // 注册全局方法
    // $this.exportFunc("setDefaultValue", $this.setDefaultValue);
    $this.exportFunc("getSearchData", $this.getSearchData); // 查询数据
  },
  methods: {
    //获取分类数据
    getSortData: function (data) {
      var $this = this;
      if (data && data.length > 0) {
        for (var i = 0; i < data.length; i++) {
          var item = data[i];
          item.sortTemp = $this.getTemp(item.dataClassificLis);
          item.dataClassificLis && $this.getDefault(item.dataClassificLis); //默认选中的条件
        }
      }
      return data;
    },
    //点击事件
    handleClick: function (e) {
      if (e.target.nodeName.toLowerCase() === "a") {
        let li = e.target.parentNode.parentNode.children;
        const tag = e.target.name;
        this.clearSiblings(li, tag);
        var dtIndex = this.selectItem.indexOf(tag);
        if (dtIndex > -1) {
          this.selectItem.splice(dtIndex, 1);
        } else {
          this.selectItem.push(tag);
        }
      }
    },

    // 单选 清空兄弟节点选中
    clearSiblings: function (siblings, tagName) {
      if (siblings) {
        for (let i = 0; i < siblings.length; i++) {
          const element = siblings[i].childNodes;
          if (element && element[0].name && element[0].name != tagName) {
            let tname = element[0].name;
            let dtIndex = this.selectItem.indexOf(tname);
            if (dtIndex > -1) {
              this.selectItem.splice(dtIndex, 1);
            }
          }
        }
      }
    },

    //获取模板数据分类
    getTemp: function (list) {
      if (!list) {
        return "";
      }
      var arrTemp = [];
      // var index = 0;
      // var sectionCount = 3;
      // for (var i = 0; i < list.length; i++) {
      //     index = parseInt(i / sectionCount);
      //     if (arrTemp.length <= index) {
      //         arrTemp.push([]);
      //     }
      //     arrTemp[index].push(list[i]);
      // }
      arrTemp.push(list);
      return arrTemp;
    },
    //获取默认选中
    getDefault: function (data) {
      var $this = this;
      for (var i = 0; i < data.length; i++) {
        var item = data[i];
        if (item.isDefault == true) {
          $this.selectItem.push(item.name);
        }
      }
    },
    //获取查询条件
    getQueryData: function (data) {
      var $this = this;
      var queryList = [];
      for (var i = 0; i < data.length; i++) {
        var item = data[i].element;
        item.cellCode = data[i].cellCode;
        var queryCon = getQueryCondition.bind(this)(data[i]);
        var info = {
          name: item.namekey,
          condition: queryCon
        };
        queryList.push(info);
      }
      return queryList;
    },

    //重置查询条件
    resetClassQuery: function () {
      this.selectItem = [];
      this.queryData = this.getQueryData(this.propSearchConfigData.query);
      this.startdate = this.curstart;
      this.enddate = this.curend;
      // this.dateVal = '';
    },

    // enter键查询
    searchByEnter: function () {
      var keycode = event.keyCode;
      var searchName = event.target.value;
      //keycode是键码，13也是电脑物理键盘的 enter
      if (keycode == "13") {
        console.log(2);
        event.preventDefault();
        this.getSearchData();
        // 失去焦点
        event.target.blur();
      }
    },

    combineQueryData: function () {
      let tempData;
      let arr = this.queryData;
      let temp = [];
      let searchUrl;
      for (var i = 0; i < arr.length; i++) {
        var el = arr[i].condition;
        for (var j = 0; j < el.length; j++) {
          temp.push(el[j]);
        }
      }

      for (var j = 0; j < temp.length; j++) {
        let name = temp[j].name;
        let value = temp[j].value;
        if (this.$refs[name] && !this.$refs[name][0].validate()) {
          return;
        }
      }

      searchUrl = this.propSearchConfigData.searchUrl + "query";
      let queryList = this.selectItem.join(",");
      tempData = filtration(temp, searchUrl, queryList);

      if (!tempData) {
        return false;
      }

      return tempData;

    },

    // 查询数据
    getSearchData: function () {
      this.$parent.getSearchData(this.combineQueryData());
      // 关闭侧滑菜单
      this.propCloseSidePanel();
      window.localStorage.setItem("queryType", "advancedQuery"); //高级查询
    }

  },
  components: {
    htmlPanel,
    searchInput
  }
};
</script>
<!-- Add "scoped" attribute to limit CSS to this component only -->

<style lang="less">
@import "./sidePanel.less";
</style>


// WEBPACK FOOTER //
// src/components/listSearch/sidePanel.vue