<template>
  <div>
    <div :class="`${prefix}-panel`">
      <div
        :class="`${prefix}-tit`"
        v-on:click="togglePanel"
      >
        <span>{{showName}}</span>
        <i :class="`${prefix}-drop-icon`"></i>
      </div>
      <div :class="`${prefix}-option-panel ${showOption}`">
        <ul>
          <li
            v-for="(list,index) in options"
            v-if="options.length"
            v-on:click="checkItem(list)"
            :class="`${prefix}-option-item ${list.id === currentId?'active':''}`"
            :key="list.id"
          >
            {{list.shortName}}
          </li>
        </ul>
      </div>
    </div>
    <div
      :class="`${prefix}-mask ${showOption}`"
      v-on:click="togglePanel"
    ></div>
  </div>
</template>

<script>
import { publicGetAxios } from '../../assets/js/util/common.js'
import serviceUrl from './service.js';

export default {
  name: "companyCheck",
  data() {
    return {
      prefix: 'm-companyCheck',
      showOption: "",
      options: [],
      showName: '',
      currentId: ''
    }
  },
  props: {
    defaultValue: {
      default: {},
      type: Object
    },
    switchCompany: {
      default: () => { },
      type: Function
    }
  },
  beforeMount() {
    const { name, id } = this.defaultValue;
    this.showName = name;
    this.currentId = id;
    //   获取公司数据
    publicGetAxios(
      { url: serviceUrl.company },
      (res) => {
        const { list } = res;
        this.options = list;
        this.$forceUpdate();
      }
    )

  },
  methods: {
    togglePanel() {
      this.showOption = this.showOption ? '' : 'show';
    },
    checkItem(list) {
      this.showName = list.shortName;
      this.currentId = list.id;
      this.switchCompany(list);
      this.togglePanel();
      this.$parent.$parent.$parent.updateCompanyId({ id: list.id, name: list.shortName });
    }
  }

}
</script>
<style lang="less" scoped>
@import "../../assets/css/groupSubset.less";
</style>


// WEBPACK FOOTER //
// src/components/refer/companySwitch.vue