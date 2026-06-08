<template>
  <form
    enctype="multipart/form-data"
    ref="form"
    method="post"
    name="form"
    :action="formUrl"
    v-on:submit.prevent="checkForm"
  >
    <slot></slot>
  </form>
</template>

<script>
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
export default {
  name: "iForm",
  data() {
    return {
      kl: 0,
      fields: [],
      formUrl: ""
    };
  },
  props: {},
  provide() {
    return {
      form: this
    };
  },
  methods: {
    resetFields() {
      this.fields.forEach(field => field.resetField());
    },
    validate(cb, type, status) {
      const $this = this;
      $this.operateType = type;
      $this.status = status;
      return new Promise(resolve => {
        let valid = true,
          count = 0;
        $this.fields.forEach(field => {
          field.validate(error => {
            if (error) valid = false;
            if (++count === $this.fields.length || !valid) {
              resolve(!valid);
              if (typeof cb === "function") cb(valid);
              throw new error();
            }
          }, $this.operateType, $this.status);
        });
      });
    },

    // 提交表单
    submit() {
      event.preventDefault();
      const $this = this;
      let forms = $this.$refs.form;
      let formData = new FormData();
      let fields = $this.fields;
      if (fields && fields.length > 0) {
        // formData.append("viewCode", "wupin_1.0.0.00_wupin_edit__mobile__");
        for (let i = 0; i < fields.length; i++) {
          const element = fields[i];
          let condition = element.getCondition();
          if (condition.datagridCode) {
            // datagrid
            continue;
          }

          // 多选控件参数组织
          if (condition.columnType == "MULTSELECT") {
            $this.getMultiSelectData(formData, condition);
            continue;
          }

          // 参照
          if (condition.isPrefer) {
            let index = condition.name.lastIndexOf(".");
            let idName = condition.name.substring(0, index);
            idName = `${idName}.id`;
            formData.append(idName, condition.valueId);
            formData.append(condition.name, condition.value);
            continue;
          }

          // complex
          if (condition.complex) {
            let nameTit = `${window._PAGECONFIG.userInfo.modelAlias}.extraCol`;
            let complex = sessionStorage.getItem(nameTit);
            let complexTemp = [];
            if (condition.valueId) {
              complexTemp = { name: condition.name, value: condition.valueId };
            } else if (condition.value) {
              complexTemp = { name: condition.name, value: condition.value };
            }
            if (complex) {
              complex = JSON.parse(complex);
              complex.push(complexTemp);
              sessionStorage.setItem(nameTit, JSON.stringify(complex));
            } else {
              // formData.append(nameTit, JSON.stringify([complexTemp]));
              sessionStorage.setItem(nameTit, JSON.stringify([complexTemp]));
            }

            continue;
          }

          // systemCode单选
          if (
            condition &&
            condition.columnType == "SYSTEMCODE" &&
            !condition.multable
          ) {
            formData.append(`${condition.name}.id`, condition.valueId);
            formData.append(`${condition.name}`, condition.value);

            continue;
          }

          // 格式化小数
          if (condition.precision) {
            let decimal = condition.value;
            if (_commonJs.isDecimal(decimal)) {
              decimal = Number(decimal)
                .toFixed(Number(condition.precision))
                .toString();
              // condition.value = decimal.toString();
            } else {
              decimal = "";
            }
            condition.value = decimal;
            formData.append(`${condition.name}`, condition.value);

            continue;
          }

          // 布尔型
          if (
            condition &&
            (condition.valueId || typeof condition.valueId == "boolean")
          ) {
            formData.append(condition.name, condition.valueId);
          } else {
            condition && formData.append(condition.name, condition.value);
          }
        }
      }
      return formData;
    },

    // 组织多选参数
    getMultiSelectData: function(formData, data) {
      if (!data.oldMultiLists || !data.newMultiLists) {
        return;
      }
      let oldData = data.oldMultiLists.ids;
      let newData = data.newMultiLists.ids;
      let deleteIds = [],
        AddIds = [],
        multiIds = [];
      oldData = oldData ? oldData : [];
      newData = newData ? newData : [];
      multiIds = newData ? newData : [];

      oldData.forEach(element => {
        if (newData.indexOf(element) == -1) {
          deleteIds.push(element);
        }
      });

      newData.forEach(element => {
        if (oldData.indexOf(element) == -1) {
          AddIds.push(element);
        }
      });

      formData.append(`${data.name}MultiIDs`, multiIds.join(","));
      formData.append(`${data.name}DeleteIds`, deleteIds.join(","));
      formData.append(`${data.name}AddIds`, AddIds.join(","));
      return formData;
    },

    deleteFileds: function(name) {
      const $this = this;
      if ($this.fields) {
        $this.fields.forEach((item, index) => {
          if (item && item.$refs[name]) {
            $this.fields.splice(index, 1);
          }
        });
      }
    }
  },
  created() {
    this.$on("form-add", field => {
      if (field) this.fields.push(field);
    });
  }
};
</script>



// WEBPACK FOOTER //
// src/components/form/Iform.vue