import React from 'react';
import { injectIntl } from 'react-intl';
import { Cascader } from 'sup-ui';
import {
  getRelateModuleByCode,
  fetchTree,
  getPKProperty
} from 'root/services/customProperty';

const NODE_TYPE_MODULE = 'module';
const NODE_TYPE_ENTITY = 'entity';
const NODE_TYPE_MODEL = 'model';

const NODE_TYPE_TREE = [NODE_TYPE_MODULE, NODE_TYPE_ENTITY, NODE_TYPE_MODEL];

class SelectComp extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      options: [],
      defaultTreeLoaded: false // 已有数据, 未获取所有数据前,禁止选择
    };
  }

  componentDidMount() {
    const { moduleCode } = this.props;
    const defaultValue = this.getDefaultValue();
    getRelateModuleByCode({ moduleCode }).then(({ data }) => {
      const { list } = data;
      const options = list.map(({ code, name }) => {
        return {
          value: code,
          label: name,
          isLeaf: false,
          type: NODE_TYPE_MODULE
        };
      });
      if (defaultValue.length) {
        // 如果有初始value， 需要自动逐个轮询
        this.autoFetchTreeInfo(options, defaultValue, 0, options);
      } else {
        this.setState({
          options
        });
      }
    });
  }

  autoFetchTreeInfo(stateOptions, defaultValue, index, childOptions) {
    const value = defaultValue[index];
    if (value) {
      const option = childOptions.find((d) => d.value === value);
      if (index + 1 === NODE_TYPE_TREE.length) {
        // 获取参照视图
        this.props.onChangeModel(value, true);
      }
      this.fetchTreeInfo(option, true).then((nextOption) => {
        this.autoFetchTreeInfo(
          stateOptions,
          defaultValue,
          index + 1,
          nextOption.children
        );
      });
    } else {
      this.setState({ options: stateOptions, defaultTreeLoaded: true });
    }
  }

  fetchTreeInfo(targetOption, hideLoading) {
    if (!hideLoading) {
      targetOption.loading = true;
    }
    const { type, value } = targetOption;
    const nodeTypeIndex = NODE_TYPE_TREE.indexOf(type);
    if (nodeTypeIndex === -1) return Promise.resolve(targetOption);
    if (nodeTypeIndex === NODE_TYPE_TREE.length - 1) {
      return getPKProperty({
        modelCode: value
      }).then(({ data }) => {
        const {
          data: { code, displayNameInternational }
        } = data;
        targetOption.children = [
          {
            value: code,
            label: displayNameInternational,
            isLeaf: true
          }
        ];
        if (!hideLoading) {
          targetOption.loading = false;
        }
        return targetOption;
      });
    } else {
      const nextNodeType = NODE_TYPE_TREE[nodeTypeIndex + 1];
      return fetchTree({
        type: nextNodeType,
        code: value
      }).then(({ data }) => {
        const { list } = data;
        targetOption.children = list.map((d) => {
          const { code, name } = d;
          return {
            value: code,
            label: name,
            isLeaf: false,
            type: nextNodeType
          };
        });
        if (!hideLoading) {
          targetOption.loading = false;
        }
        return targetOption;
      });
    }
  }

  loadData = (selectedOptions) => {
    const targetOption = selectedOptions[selectedOptions.length - 1];
    this.fetchTreeInfo(targetOption).then(() => {
      this.setState({});
    });
  };

  onChange = (value, selectedOptions) => {
    // 修改选择对象值
    this.props.onChangeValue(value[NODE_TYPE_TREE.length]);
    if (selectedOptions.length === NODE_TYPE_TREE.length) {
      // 获取参照视图
      this.props.onChangeModel(value[NODE_TYPE_TREE.length - 1]);
    } else if (selectedOptions.length < NODE_TYPE_TREE.length) {
      // 重置参照视图
      this.props.onChangeModel(null);
    }
  };

  getDefaultValue() {
    const { associatedProperty } = this.props;
    let value = [];
    if (associatedProperty) {
      const { code, entityCode, modelCode, moduleCode } = associatedProperty;
      value = [moduleCode, entityCode, modelCode, code];
    }
    return value;
  }

  getDisableState() {
    return this.getDefaultValue().length > 0 && !this.state.defaultTreeLoaded;
  }

  render() {
    const { options } = this.state;
    return (
      <div>
        <Cascader
          disabled={this.getDisableState()}
          options={options}
          loadData={this.loadData}
          onChange={this.onChange}
          changeOnSelect
          placeholder="请选择"
          defaultValue={this.getDefaultValue()}
        />
      </div>
    );
  }
}

export default injectIntl(SelectComp, { forwardRef: true });
