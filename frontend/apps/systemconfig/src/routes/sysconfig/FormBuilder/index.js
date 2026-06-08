import React from 'react';
import { Form, Button, Spin } from 'sup-ui';
import { injectIntl } from 'react-intl';
import Fields from './field';

import { FIELD_PREFIX } from '../constants';
import messages from '../messages';

class FormBuilder extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      submiting: false
    };
  }

  render() {
    const { config, handleSaveConfig, form, intl, updateAuth } = this.props;

    const { submiting } = this.state;

    return (
      <Spin spinning={submiting}>
        <Form
          onSubmit={(e) => {
            e.preventDefault();
            this.props.form.validateFieldsAndScroll((err, values) => {
              if (!err) {
                this.setState({
                  submiting: true
                });
                handleSaveConfig(values, config, () => {
                  this.setState({
                    submiting: false
                  });
                });
              }
            });
          }}
        >
          {config.map((configInfo) => {
            return (
              <Fields
                key={configInfo.configId}
                field={{
                  ...configInfo,
                  configId: `${FIELD_PREFIX}${configInfo.configId}`
                }}
                form={form}
              />
            );
          })}

          {updateAuth ? (
            <Form.Item>
              <Button type="primary" htmlType="submit">
                {intl.formatMessage(messages.save)}
              </Button>
            </Form.Item>
          ) : null}
        </Form>
      </Spin>
    );
  }
}

const WrappedFormBuilderForm = Form.create({ name: 'formBuilder' })(
  FormBuilder
);

export default injectIntl(WrappedFormBuilderForm);
