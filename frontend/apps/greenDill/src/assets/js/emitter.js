export default {
  methods: {
    // 获取父级组件方法
    dispatch(componentName, eventName, ...params) {
      let parent = this.$parent || this.$root;
      let name = parent.$options.name;
      while (parent && (!name || name !== componentName)) {
        parent = parent.$parent;
        if (parent) name = parent.$options.name;
      }
      if (parent) parent.$emit.apply(parent, [eventName].concat(...params));
    },
    // 获取父级组件
    getCompByName(componentName) {
      let parent = this.$parent || this.$root;
      let name = parent.$options.name;
      while (parent && (!name || name !== componentName)) {
        parent = parent.$parent;
        if (parent) name = parent.$options.name;
      }
      return parent;
    }
  }
};



// WEBPACK FOOTER //
// ./src/assets/js/emitter.js