<template>
  <div
    class="pick_pop"
    :class="{'showPicker':isShow}"
  >
    <div class="pick_wrapper">
      <div class="pick_toolbar">
        <a
          class="m_cancel"
          @click="sendOut('cancel',name)"
        >{{$t('button.cancel.text')}}</a>
        <a
          class="m_comfirm"
          @click="sendOut('comfirm',name)"
        >{{$t('button.ok.text')}}</a>
      </div>
      <mt-picker
        :ref="name"
        :slots="slots"
        :class="{'picker-android': isAndroidLessVersion}"
      ></mt-picker>
    </div>
    <div
      class="m_shadow"
      @click="sendOut('cancel',name)"
    ></div>
  </div>
</template>
<script>
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
export default {
  name: "custom-picker",
  data: function () {
    return {
      handler: function (e) {
        e.preventDefault();
      },
      isAndroidLessVersion: false
    };
  },
  props: {
    isShow: {
      type: Boolean,
    },
    name: {
      type: String
    },
    value: {
      type: [String, Array, Object]
    },
    slots: {
      type: Array
    },
    con: {
      type: Object,
    },
    callbackFunc: {
      type: Function,
      default: () => { }
    }
  },
  watch: {
    isShow: function (newVal, oldVal) {
      if (newVal) {
        this.closeTouch(); //展开
      } else {
        this.openTouch();
      }
      if (typeof this.$parent.$parent.callbackFunc === 'function') this.$parent.$parent.callbackFunc(newVal)
    }
  },
  methods: {
    sendOut: function (etype, name) {
      var pickObj = this.$refs[name].getValues()[0];
      this.$emit("pickercallback", etype, pickObj, name, this.con);
    },

    //阻止body的默认事件
    closeTouch() {
      document.getElementsByTagName("body")[0].addEventListener("touchmove", this.handler, { passive: false });
    },

    //打开body的默认事件
    openTouch() {
      document.getElementsByTagName("body")[0].removeEventListener("touchmove", this.handler, { passive: false });
    }
  },
  mounted() {
    // 获取当前安卓手机版本号小于等于6的进行样式处理
    this.isAndroidLessVersion = _commonJs.isLessEqualAndroidVersion(6);
  },
  computed: {}
};
</script>
<style lang="css">
.picker-android .picker-center-highlight:before,
.picker-android .picker-center-highlight:after {
  background-color: #fff;
}
.picker-android .picker-center-highlight {
  border-top: thin solid #cbcbcb;
  border-bottom: thin solid #cbcbcb;
}
</style>




// WEBPACK FOOTER //
// src/components/cellComp/picker/picker.vue