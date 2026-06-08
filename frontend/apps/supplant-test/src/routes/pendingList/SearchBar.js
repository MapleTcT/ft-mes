import React from 'react';
import { injectIntl } from 'react-intl';
import PropTypes from 'prop-types';
import { Form, Input, Row, Col, Button } from 'sup-ui';
import { SupReference } from 'sup-rc-reference';
import SortGroup from './SortGroup.js';
import defaultMessages from './messages.js';
import { getRefCompanyConfig } from '../../utils/index.js';

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
        name: intl.formatMessage(defaultMessages.processName),
        key: 'processName',
        component: <Input onKeyDown={this.handleKeyDown} />
      },
      {
        name: intl.formatMessage(defaultMessages.currestActivity),
        key: 'activityName',
        component: <Input onKeyDown={this.handleKeyDown} />
      },
      {
        name: intl.formatMessage(defaultMessages.assignor),
        key: 'assignorId',
        initialValue: [],
        component: <SupReference {...refProps} />
      },
      {
        name: intl.formatMessage(defaultMessages.tableNo),
        key: 'tableNo',
        component: <Input onKeyDown={this.handleKeyDown} />
      },
      {
        name: intl.formatMessage(defaultMessages.initiator),
        key: 'initiatorId',
        initialValue: [],
        component: <SupReference {...refProps} />
      }
    ];
  }

  state = {
    cols: 3,
    isExpand: false
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

  handleExpand = (flag = true) => this.setState({ isExpand: flag });

  handleSortChange = (data) => {
    const { isExpand } = this.state;
    const { onSearch } = this.props;
    if (!isExpand) return onSearch({ ...data });
    onSearch({ ...this.getFormValue(), ...data });
  };

  handleReset = () => {
    const { resetFields } = this.props.form;
    resetFields();
  };

  handleSearch = () => {
    const { isExpand } = this.state;
    const { value, onSearch } = this.props;
    if (!isExpand) {
      onSearch({ ...this.getSortSelect() });
      return;
    }
    onSearch({ ...value, ...this.getFormValue() });
  };

  getFormValue = () => {
    const { form } = this.props;
    const data = form.getFieldsValue();
    Object.keys(data).forEach((key) => {
      const fieldVal = data[key];
      const isRef = key === 'assignorId' || key === 'initiatorId';
      if (isRef && fieldVal) {
        data[key] = (fieldVal[0] || {}).userId || '';
      }
    });
    return data;
  };

  getSortSelect = () => {
    const {
      value: { category, timeType, taskType }
    } = this.props;
    return { category, timeType, taskType };
  };

  handleKeyDown = (e) => {
    if (e.keyCode === 13) {
      this.handleSearch();
    }
  };

  renderFileds = () => {
    const { cols } = this.state;
    const { getFieldDecorator } = this.props.form;
    const lines = Math.ceil(this.field.length / cols);
    const fieldDom = [];
    for (let i = 0; i < lines; i += 1) {
      const cells = [];
      for (let j = 0; j < cols; j += 1) {
        const item = this.field[i * cols + j];
        if (item) {
          const { key, name, initialValue, component } = item;
          cells.push(
            <Col span={24 / cols} key={key}>
              <Form.Item {...formItemLayout} label={name}>
                {getFieldDecorator(key, {
                  initialValue
                })(component)}
              </Form.Item>
            </Col>
          );
        } else break;
      }
      fieldDom.push(<Row key={i}>{cells}</Row>);
    }
    return fieldDom;
  };

  renderButton = () => {
    const { intl } = this.props;
    return (
      <div className="sup-searchbar-btn">
        <Button className="btn-search" onClick={this.handleSearch}>
          {intl.formatMessage(defaultMessages.search)}
        </Button>
        <Button className="btn-reset" onClick={this.handleReset}>
          {intl.formatMessage(defaultMessages.clear)}
        </Button>
        <Button className="btn-expand" onClick={() => this.handleExpand(false)}>
          <i className="icon-fold" />
        </Button>
      </div>
    );
  };

  render() {
    const { isExpand } = this.state;
    return (
      <Form className="sup-searchbar-form">
        <SortGroup
          value={this.getSortSelect()}
          onChange={this.handleSortChange}
        />
        <em className="sup-btn-expand" onClick={this.handleExpand} />
        {isExpand && this.renderFileds()}
        {isExpand && this.renderButton()}
      </Form>
    );
  }
}
const SearchBarForm = Form.create({
  name: 'pending'
})(SearchBar);

SearchBarForm.propTypes = {
  onSearch: PropTypes.func,
  value: PropTypes.oneOfType([PropTypes.object])
};

SearchBarForm.defaultProps = {
  onSearch: () => {},
  value: {}
};
export default SearchBarForm;
