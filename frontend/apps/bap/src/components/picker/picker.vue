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
        >取消</a>
        <a
          class="m_comfirm"
          @click="sendOut('comfirm',name)"
        >确定</a>
      </div>
      <mt-picker
        :ref="name"
        :slots="slots"
      ></mt-picker>
    </div>
    <div
      class="m_shadow"
      @click="sendOut('cancel',name)"
    ></div>
  </div>
</template>
<script>
export default {
  name: "custom-picker",
  data: function() {
    return {
      handler: function(e) {
        e.preventDefault();
      }
    };
  },
  props: ["isShow", "name", "value", "slots", "con"],
  watch: {
    isShow: function(newVal, oldVal) {
      if (newVal) {
        //展开
        this.closeTouch();
      } else {
        this.openTouch();
      }
    }
  },
  methods: {
    sendOut: function(etype, name) {
      var pickObj = this.$refs[name].getValues()[0];
      this.$emit("pickercallback", etype, pickObj, name, this.con);
    },
    closeTouch() {
      document
        .getElementsByTagName("body")[0]
        .addEventListener("touchmove", this.handler, { passive: false }); //阻止默认事件
    },
    openTouch() {
      document
        .getElementsByTagName("body")[0]
        .removeEventListener("touchmove", this.handler, { passive: false }); //打开默认事件
    }
  },
  mounted() {},
  computed: {}
};
</script>


// WEBPACK FOOTER //
// src/components/picker/picker.vue