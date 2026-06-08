import React from 'react';
import { injectIntl } from 'react-intl';
import { Form, Input, Row, Col, Select, Button } from 'sup-ui';
import messages from './messages';
import style from './style.less';

const { Option } = Select;

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

class GlobalSearch extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      cols: 3,
      searchConditions: {}
    };
  }

  componentDidMount() {
    window.addEventListener('resize', this.handleResize);
  }

  componentWillMount() {
    window.removeEventListener('resize', this.handleResize);
    this.props.onRef(this);
  }

  handleResize = () => {
    const { clientWidth: width } = window.document.body;
    const fieldCols = width > 825 ? 3 : 2;
    this.setState({
      cols: fieldCols
    });
  };

  handleSearch = () => {
    const { searchConditions } = this.state;
    const { handleSearch } = this.props;
    handleSearch(searchConditions);
  };

  handleReset = () => {
    const { resetFields } = this.props.form;
    this.setState(
      {
        searchConditions: {}
      },
      () => {
        resetFields();
      }
    );
  };

  handleChange = (e) => {
    const { searchConditions } = this.state;
    const newSearchConditions = { ...searchConditions };
    if (e && e.target) {
      const searchInput = e.target.id.split('_')[1];
      newSearchConditions[searchInput] = e.target.value;
    } else {
      newSearchConditions.jobStatus = e;
    }
    this.setState(
      {
        searchConditions: newSearchConditions
      }
    );
  };

  renderSearch = () => {
    const { cols } = this.state;
    const { getFieldDecorator } = this.props.form;
    const { intl } = this.props;
    const fieldDom = [];
    const field = [
      {
        name: intl.formatMessage(messages.taskName),
        key: 'jobName',
        component: (
          <Input
            placeholder={`${intl.formatMessage(
              messages.pleaseEnter
            )}${intl.formatMessage(messages.taskName)}`}
            onChange={this.handleChange}
          />
        )
      },
      {
        name: intl.formatMessage(messages.taskCode),
        key: 'code',
        component: (
          <Input
            placeholder={`${intl.formatMessage(
              messages.pleaseEnter
            )}${intl.formatMessage(messages.taskCode)}`}
            onChange={this.handleChange}
          />
        )
      },
      {
        name: intl.formatMessage(messages.modalName),
        key: 'modelName',
        component: (
          <Input
            placeholder={`${intl.formatMessage(
              messages.pleaseEnter
            )}${intl.formatMessage(messages.modalName)}`}
            onChange={this.handleChange}
          />
        )
      },
      {
        name: intl.formatMessage(messages.interfaceUrl),
        key: 'serviceApi',
        component: (
          <Input
            placeholder={`${intl.formatMessage(
              messages.pleaseEnter
            )}${intl.formatMessage(messages.interfaceUrl)}`}
            onChange={this.handleChange}
          />
        )
      },
      {
        name: intl.formatMessage(messages.taskStatus),
        key: 'jobStatus',
        component: (
          <Select
            placeholder={intl.formatMessage(messages.pleaseEnter)}
            allowClear
            onChange={this.handleChange}
          >
            <Option key="0">{intl.formatMessage(messages.normalStatus)}</Option>
            <Option key="1">{intl.formatMessage(messages.stopStatus)}</Option>
            <Option key="2">
              {intl.formatMessage(messages.abnormalStatus)}
            </Option>
            <Option key="3">{intl.formatMessage(messages.awaitStatus)}</Option>
          </Select>
        )
      }
    ];
    const lines = Math.ceil(field.length / cols);

    for (let i = 0; i < lines; i += 1) {
      const cells = [];
      for (let j = 0; j < cols; j += 1) {
        const item = field[i * cols + j];
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

  renderSearchBtn = () => {
    const { intl } = this.props;
    return (
      <div className={style.globalSearchBtn}>
        <Button onClick={this.handleSearch}>
          {intl.formatMessage(messages.globalQuery)}
        </Button>
        <Button onClick={this.handleReset}>
          {intl.formatMessage(messages.globalReset)}
        </Button>
      </div>
    );
  };

  render() {
    return (
      <Form className={style.globalSearch}>
        {this.renderSearch()}
        {this.renderSearchBtn()}
      </Form>
    );
  }
}

const GlobalSearchForm = Form.create({
  name: 'pending'
})(injectIntl(GlobalSearch));
export default GlobalSearchForm;
