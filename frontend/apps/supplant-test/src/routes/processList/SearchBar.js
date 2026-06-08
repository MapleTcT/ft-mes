import React from 'react';
import { injectIntl } from 'react-intl';
import { Form, Input, Row, Col, Button, Select } from 'sup-ui';
import { SupReference } from 'sup-rc-reference';
import PropTypes from 'prop-types';
import DepartLower from './DepartLower.js';
import defaultMessages from './messages.js';
import { getRefCompanyConfig } from '../../utils/index.js';

const { Option } = Select;
const DFTKEY = 'flowName'; // 收起后默认查询字段

const formItemLayout = {
  labelCol: {
    xs: {
      span: 24
    },
    sm: {
      span: 8
    }
  },
  wrapperCol: {
    xs: {
      span: 24
    },
    sm: {
      span: 16
    }
  }
};

@injectIntl
class SearchBar extends React.PureComponent {
  constructor(props) {
    super(props);
    const { intl } = props;
    const refProps = {
      referenceView: {
        title: intl.formatMessage(defaultMessages.staff),
        type: 'user',
        companyConfig: getRefCompanyConfig()
      },
      suffix: <i className="icon-search" />
    };
    this.field = [
      {
        name: intl.formatMessage(defaultMessages.flowName),
        key: 'flowName',
        component: <Input onKeyDown={this.handleKeyDown} />
      },
      {
        name: intl.formatMessage(defaultMessages.tableNo),
        key: 'tableNo',
        component: <Input onKeyDown={this.handleKeyDown} />
      },
      {
        name: intl.formatMessage(defaultMessages.staffName),
        key: 'userId',
        initialValue: [],
        component: <SupReference {...refProps} />
      },
      {
        name: intl.formatMessage(defaultMessages.departmentName),
        key: 'departmentId',
        initialValue: [],
        component: <DepartLower />
      },
      {
        name: intl.formatMessage(defaultMessages.summary),
        key: 'summary',
        component: <Input onKeyDown={this.handleKeyDown} />
      }
    ];
  }

  state = {
    cols: 3,
    isExpand: false,
    seletedField: DFTKEY
  };

  componentDidMount() {
    this.handleResize();
    window.addEventListener('resize', this.handleResize);
  }

  componentWillUnmount() {
    window.removeEventListener('resize', this.handleResize);
  }

  handleResize = () => {
    const { clientWidth: width } = window.document.body;
    const fieldCols = width > 825 ? 3 : 2;
    this.setState({
      cols: fieldCols
    });
  };

  handleExpand = () => {
    const { isExpand: prev } = this.state;
    this.setState({ isExpand: !prev });
  };

  handleReset = () => {
    const { resetFields } = this.props.form;
    resetFields();
  };

  handleSearch = () => {
    const { onSearch } = this.props;
    const { isExpand, seletedField } = this.state;
    const { getFieldsValue } = this.props.form;
    let data = getFieldsValue();
    if (!isExpand) data = { [seletedField]: data[seletedField] };
    Object.keys(data).forEach((key) => {
      const value = data[key];
      const refKey = ['userId', 'departmentId'];
      if (refKey.includes(key) && value) {
        data[key] = (value[0] || {}).id || '';
        if (key === 'departmentId' && value[0]) {
          data.departmentLower = value[0].isLower;
        } else if (key === 'userId') {
          data[key] = (value[0] || {}).userId || '';
        }
      }
    });
    onSearch(data);
  };

  handleSelectChange = (val) => {
    if (val === 'all') {
      this.setState({ seletedField: DFTKEY, isExpand: true });
    } else {
      this.setState({ seletedField: val });
    }
  };

  handleKeyDown = (e) => {
    if (e.keyCode === 13) {
      this.handleSearch();
    }
  };

  renderSelect = () => {
    const { intl } = this.props;
    const { seletedField } = this.state;
    return (
      <div className="search-field-select">
        <Select
          className="m-select"
          value={seletedField}
          onChange={this.handleSelectChange}
        >
          <Option value="all">
            {intl.formatMessage(defaultMessages.showAll)}
          </Option>
          {this.field.map((item) => (
            <Option value={item.key}>{item.name}</Option>
          ))}
        </Select>
        {this.renderFileds()}
      </div>
    );
  };

  renderFileds = () => {
    const { cols, isExpand, seletedField } = this.state;
    const { getFieldDecorator } = this.props.form;
    const showField = isExpand
      ? this.field
      : this.field.filter((item) => item.key === seletedField);
    const lines = Math.ceil(showField.length / cols);
    const fieldDom = [];
    for (let i = 0; i < lines; i += 1) {
      const cells = [];
      for (let j = 0; j < cols; j += 1) {
        const item = showField[i * cols + j];
        if (item) {
          const { key, name, initialValue, component } = item;
          const layout = { ...formItemLayout };
          if (!isExpand) {
            layout.labelCol = { span: 0 };
            layout.wrapperCol = { span: 24 };
          }
          cells.push(
            <Col span={isExpand ? 24 / cols : 24} key={key}>
              <Form.Item {...layout} label={name}>
                {getFieldDecorator(key, {
                  initialValue
                })(component)}
              </Form.Item>
            </Col>
          );
        } else break;
      }
      fieldDom.push(
        <Row key={i} className="field-row">
          {cells}
        </Row>
      );
    }
    fieldDom.push(this.renderButton());
    return fieldDom;
  };

  renderButton = () => {
    const { intl } = this.props;
    const { isExpand } = this.state;
    return (
      <div className="sup-searchbar-btn">
        <Button className="btn-search" onClick={this.handleSearch}>
          {intl.formatMessage(defaultMessages.search)}
        </Button>
        <Button className="btn-reset" onClick={this.handleReset}>
          {intl.formatMessage(defaultMessages.clear)}
        </Button>
        <Button className="btn-expand" onClick={this.handleExpand}>
          <i className={isExpand ? 'icon-fold' : 'icon-expand'} />
        </Button>
      </div>
    );
  };

  render() {
    const { isExpand } = this.state;
    return (
      <Form className="sup-searchbar-form">
        {isExpand ? this.renderFileds() : this.renderSelect()}
      </Form>
    );
  }
}

const SearchBarForm = Form.create({
  name: 'searchbar'
})(SearchBar);

SearchBarForm.defaultProps = { onSearch: () => {}, type: 'pending' };
SearchBarForm.propTypes = { onSearch: PropTypes.func, type: PropTypes.string };
export default SearchBarForm;
