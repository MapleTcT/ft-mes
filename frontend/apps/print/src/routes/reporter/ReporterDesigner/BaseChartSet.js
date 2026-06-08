import { Component } from 'react';
import * as _ from 'lodash';

class BaseChartSet extends Component {
  setSeriesLineWidth = ({ value }) => {
    this.props.editChart.series().set(this.state.selectedSeriesIndex, { border: { width: value } });
    this.props.reportHasChanged();
  }

  getSeriesLineWidth = () => {
    return this.props.editChart.series().get(this.state.selectedSeriesIndex).border.width;
  }

  setSeriesLineColor = ({ value }) => {
    const { selectedSeriesIndex } = this.state;
    this.props.editChart.series().set(selectedSeriesIndex, { border: { color: value } });
    this.props.reportHasChanged();
  }

  getSeriesLineColor = () => {
    const { selectedSeriesIndex } = this.state;
    return this.getColorByThemeColor(this.props.editChart.series().get(selectedSeriesIndex).border.color);
  }

  setSeriesDirections = () => {
    this.props.editChart.switchDataOrientation();
    this.props.reportHasChanged();
  }

  getSeriesDirections = () => {
    return this.props.editChart.getDataOrientation();
  }

  getAxesGroup = () => {
    const { selectedSeriesIndex } = this.state;
    return this.props.editChart.series().get(selectedSeriesIndex).axisGroup;
  }

  setAxesGroup = ({ value }) => {
    const { editChart } = this.props;
    const { selectedSeriesIndex, selectedAxesType } = this.state;
    editChart.series().set(selectedSeriesIndex, { axisGroup: value });
    if (!editChart.axes()[selectedAxesType]) {
      const axesType = ['primaryCategory', 'primaryValue', 'secondaryCategory', 'secondaryValue'];
      for (let i = 0; i < axesType.length; i += 1) {
        if (editChart.axes()[axesType[i]]) this.setState({ selectedAxesType: axesType[i] });
        continue;
      }
    }
    this.props.reportHasChanged();
  }

  getSeriesColor = () => {
    return this.getColorByThemeColor(this.props.editChart.series().get(this.state.selectedSeriesIndex).backColor);
  }

  setSeriesColor = ({ value }) => {
    this.props.editChart.series().set(this.state.selectedSeriesIndex, { backColor: value });
    this.props.reportHasChanged();
  }

  setSeriesNameVisible = ({ value }) => {
    const { editChart } = this.props;
    const oldValue = editChart.dataLabels().showValue;
    editChart.dataLabels({ showSeriesName: value });
    editChart.dataLabels({ showValue: oldValue });
    this.props.reportHasChanged();
  }

  getSeriesNameVisible = () => {
    return this.props.editChart.dataLabels().showSeriesName;
  }

  setCategoryNameVisible = ({ value }) => {
    const { editChart } = this.props;
    const oldValue = editChart.dataLabels().showValue;
    editChart.dataLabels({ showCategoryName: value });
    editChart.dataLabels({ showValue: oldValue });
    this.props.reportHasChanged();
  }

  getCategoryNameVisible = () => {
    return this.props.editChart.dataLabels().showCategoryName;
  }

  setLengePosition = ({ value }) => {
    this.setState(() => {
      this.state.legend.position = value;
    });
    this.props.editChart.legend({ position: value });
  }

  getLengePosition = () => {
    const { editChart } = this.props;
    if (editChart.legend().visible) {
      return editChart.legend().position;
    }
    return this.state.legend.position;
  }

  setLengeVisible = ({ value }) => {
    const { editChart } = this.props;
    const obj = { visible: value };
    if (value) {
      obj.position = this.state.legend.position;
    }
    editChart.legend(obj);
  }

  getLengeVisible = () => {
    return this.props.editChart.legend().visible;
  }

  getAxesTitleText = () => {
    return _.get(this.props.editChart.axes(), `${this.state.selectedAxesType}.title.text`, '');
  }

  setAxesTitleText = ({ value }) => {
    const { selectedAxesType, axes } = this.state;
    let obj = {};
    if (value) {
      obj = {
        [selectedAxesType]: {
          title: {
            text: value,
            color: axes[selectedAxesType].title.color,
            fontSize: axes[selectedAxesType].title.fontSize,
            fontFamily: axes[selectedAxesType].title.fontFamily
          }
        }
      };
    } else {
      obj = {
        [selectedAxesType]: { title: { text: value } }
      };
    }
    this.props.editChart.axes(obj);
    this.props.reportHasChanged();
  }

  setAxesTitleFontSize = ({ value }) => {
    const { selectedAxesType } = this.state;
    this.setState(() => {
      this.state.axes[selectedAxesType].title.fontSize = value;
    });
    this.props.editChart.axes({ [selectedAxesType]: { title: { fontSize: value } } });
  }

  getAxesTitleFontSize = () => {
    const { axes, selectedAxesType } = this.state;
    const { text, fontSize } = this.props.editChart.axes()[selectedAxesType].title;
    if (text.length > 0) {
      return fontSize;
    }
    return axes[selectedAxesType].title.fontSize;
  }

  setAxesTitleFontFamily = ({ value }) => {
    const { selectedAxesType } = this.state;
    this.setState(() => {
      this.state.axes[selectedAxesType].title.fontFamily = value;
    });
    this.props.editChart.axes({ [selectedAxesType]: { title: { fontFamily: value } } });
  }

  getAxesTitleFontFamily = () => {
    const { axes, selectedAxesType } = this.state;
    const { text, fontFamily } = this.props.editChart.axes()[selectedAxesType].title;
    if (text.length > 0) {
      return fontFamily;
    }
    return axes[selectedAxesType].title.fontFamily;
  }

  setAxesTitleFontColor = ({ value }) => {
    const { selectedAxesType } = this.state;
    this.setState(() => {
      this.state.axes[selectedAxesType].title.color = value;
    });
    this.props.editChart.axes({ [selectedAxesType]: { title: { color: value } } });
  }

  getAxesTitleFontColor = () => {
    const { axes, selectedAxesType } = this.state;
    const { text, color } = this.props.editChart.axes()[selectedAxesType].title;
    if (text.length > 0) {
      return color;
    }
    return axes[selectedAxesType].title.color;
  }

  setMajorGridLineVisible = ({ value }) => {
    const { selectedAxesType, axes } = this.state;
    let obj = {};
    if (!value) {
      obj = {
        [selectedAxesType]: {
          majorGridLine: {
            visible: value,
            width: axes[selectedAxesType].majorGridLine.width,
            color: axes[selectedAxesType].majorGridLine.color
          }
        }
      };
    } else {
      obj = {
        [selectedAxesType]: { majorGridLine: { visible: value } }
      };
    }
    this.props.editChart.axes(obj);
    this.props.reportHasChanged();
  }

  getMajorGridLineVisible = () => {
    return _.get(this.props.editChart.axes(), `${this.state.selectedAxesType}.majorGridLine.visible`, false);
  }

  setMajorGridLineColor = ({ value }) => {
    const { selectedAxesType } = this.state;
    this.setState(() => {
      this.state.axes[selectedAxesType].majorGridLine.color = value;
    });
    this.props.editChart.axes({ [selectedAxesType]: { majorGridLine: { color: value } } });
  }

  getMajorGridLineColor = () => {
    const { axes, selectedAxesType } = this.state;
    const { visible, color } = this.props.editChart.axes()[selectedAxesType].majorGridLine;
    if (visible) {
      return color;
    }
    return axes[selectedAxesType].majorGridLine.color;
  }

  setMajorGridLineWidth = ({ value }) => {
    const { selectedAxesType } = this.state;
    this.setState(() => {
      this.state.axes[selectedAxesType].majorGridLine.width = value;
    });
    this.props.editChart.axes({ [selectedAxesType]: { majorGridLine: { width: value } } });
  }

  getMajorGridLineWidth = () => {
    const { axes, selectedAxesType } = this.state;
    const { visible, width } = this.props.editChart.axes()[selectedAxesType].majorGridLine;
    if (visible) {
      return width;
    }
    return axes[selectedAxesType].majorGridLine.width;
  }

  setMinorGridLineVisible = ({ value }) => {
    const { selectedAxesType, axes } = this.state;
    let obj = {};
    if (!value) {
      obj = {
        [selectedAxesType]: {
          minorGridLine: {
            visible: value,
            width: axes[selectedAxesType].minorGridLine.width,
            color: axes[selectedAxesType].minorGridLine.color
          }
        }
      };
    } else {
      obj = {
        [selectedAxesType]: { minorGridLine: { visible: value } }
      };
    }
    this.props.editChart.axes(obj);
    this.props.reportHasChanged();
  }

  getMinorGridLineVisible = () => {
    return _.get(this.props.editChart.axes(), `${this.state.selectedAxesType}.minorGridLine.visible`, false);
  }

  setMinorGridLineColor = ({ value }) => {
    const { selectedAxesType } = this.state;
    this.setState(() => {
      this.state.axes[selectedAxesType].minorGridLine.color = value;
    });
    this.props.editChart.axes({ [selectedAxesType]: { minorGridLine: { color: value } } });
  }

  getMinorGridLineColor = () => {
    const { axes, selectedAxesType } = this.state;
    const { visible, color } = this.props.editChart.axes()[selectedAxesType].minorGridLine;
    if (visible) {
      return color;
    }
    return axes[selectedAxesType].minorGridLine.color;
  }

  setMinorGridLineWidth = ({ value }) => {
    const { selectedAxesType } = this.state;
    this.setState(() => {
      this.state.axes[selectedAxesType].minorGridLine.width = value;
    });
    this.props.editChart.axes({ [selectedAxesType]: { minorGridLine: { width: value } } });
  }

  getMinorGridLineWidth = () => {
    const { axes, selectedAxesType } = this.state;
    const { visible, width } = this.props.editChart.axes()[selectedAxesType].minorGridLine;
    if (visible) {
      return width;
    }
    return axes[selectedAxesType].minorGridLine.width;
  }
}

export default BaseChartSet;
