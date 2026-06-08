import messages from '../messages';
/**
 * key 必须要和键名一致
 * chartType 必须要和spredjs里面的一致
*/
const InsertChartModulConfig = {
  coulmnChart: {
    title: messages.ColumnChart,
    key: 'coulmnChart',
    icon: 'iconColumnChart',
    list: [
      {
        pic: 'columnClusteredPic',
        chartType: 'columnClustered'
      }
    ]
  },
  lineChart: {
    title: messages.LineChar,
    key: 'lineChart',
    icon: 'iconLineChart',
    list: [
      {
        pic: 'linePic',
        chartType: 'line'
      }
    ]
  },
  pieChart: {
    title: messages.PieChart,
    key: 'pieChart',
    icon: 'iconPieChart',
    list: [
      {
        pic: 'piePic',
        chartType: 'pie'
      }
    ]
  },
  barClustered: {
    title: '条形图',
    key: 'barClustered',
    icon: 'iconBarChart',
    list: [
      {
        pic: 'barClusteredPic',
        chartType: 'barClustered'
      }
    ]
  }
};

export default InsertChartModulConfig;
