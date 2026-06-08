import message from './messages';

const reportBar = function (intl) {
  return {
    merge: {
      barTitle: intl.formatMessage(message.Merge),
      barIcon: 'iconMerge',
      barEvent: 'merge',
      selectedProps: 'isMerged'
    },
    autoWordWrap: {
      barTitle: intl.formatMessage(message.WordWrap),
      barIcon: 'iconWordWrap',
      barEvent: 'wordWrap',
      selectedState: 'wordWrap'
    },
    formatCells: {
      barTitle: intl.formatMessage(message.CellFormat),
      barIcon: 'iconCellFormat',
      barEvent: '',
      barMenu: [
        {
          key: 'formatCells',
          name: intl.formatMessage(message.HighlightRules),
          className: 'iconHighlightRules',
          children: [
            {
              key: 'greaterThan',
              name: intl.formatMessage(message.Than),
              className: 'iconGreater'
            }, {
              key: 'smaller',
              name: intl.formatMessage(message.Less),
              className: 'iconLess'
            }, {
              key: 'between',
              name: intl.formatMessage(message.Between),
              className: 'iconBetween'
            }, {
              key: 'equal',
              name: intl.formatMessage(message.Equal),
              className: 'iconEqual'
            }, {
              key: 'contains',
              name: intl.formatMessage(message.Include),
              className: 'iconInclude'
            }, {
              key: 'date',
              name: intl.formatMessage(message.Date),
              className: 'iconDate'
            }, {
              key: 'duplicate',
              name: intl.formatMessage(message.Duplicate),
              className: 'iconDuplicate'
            }
          ]
        }, {
          key: 'formatProRule',
          name: intl.formatMessage(message.Rules),
          className: 'iconRules',
          children: [
            {
              key: 'maxTen',
              name: intl.formatMessage(message.MaxCount, { count: 10 }),
              className: 'iconTopTen'
            }, {
              key: 'maxTenPct',
              name: intl.formatMessage(message.MaxCount, { count: '10%' }),
              className: 'iconTopTenPct'
            }, {
              key: 'minTen',
              name: intl.formatMessage(message.MinCount, { count: 10 }),
              className: 'iconLastTen'
            }, {
              key: 'minTenPct',
              name: intl.formatMessage(message.MinCount, { count: '10%' }),
              className: 'iconLastTenPct'
            }, {
              key: 'aboveAverage',
              name: intl.formatMessage(message.AboveAvg),
              className: 'iconAboveAvg'
            }, {
              key: 'belowAverage',
              name: intl.formatMessage(message.UnderAvg),
              className: 'iconUnderAvg'
            }
          ]
        }, {
          key: 'clearRules',
          name: intl.formatMessage(message.ClearRules),
          className: 'iconClearRules',
          children: [
            {
              key: 'removeRuleByRange',
              name: intl.formatMessage(message.ClearByRange),
              className: 'iconClearByRange'
            }, {
              key: 'clearAll',
              name: intl.formatMessage(message.ClearAllRule),
              className: 'iconClearAll'
            }
          ]
        }
      ]
    },
    basicFunc: {
      barTitle: intl.formatMessage(message.BasicFunc),
      barIcon: 'iconBasicFunc',
      barEvent: '',
      barMenu: [
        {
          key: 'SUM',
          name: intl.formatMessage(message.Sum),
          className: 'iconSum'
        }, {
          key: 'AVERAGE',
          name: intl.formatMessage(message.Avg),
          className: 'iconAvg'
        }, {
          key: 'COUNT',
          name: intl.formatMessage(message.Count),
          className: 'iconAvg'
        }, {
          key: 'MAX',
          name: intl.formatMessage(message.Max),
          className: 'iconAvg'
        }, {
          key: 'MIN',
          name: intl.formatMessage(message.Min),
          className: 'iconAvg'
        }
      ]
    },
    filter: {
      barTitle: intl.formatMessage(message.Filter),
      barIcon: 'iconFilter',
      barEvent: '',
      selectedProps: 'isFilted',
      barMenu: [
        {
          key: 'filter',
          name: intl.formatMessage(message.Filter),
          className: 'iconCustomFilter'
        }, {
          key: 'highFilter',
          name: intl.formatMessage(message.HighFilter),
          className: 'iconHighFilter'
        }
      ]
    },
    sort: {
      barTitle: intl.formatMessage(message.Sort),
      barIcon: 'iconSort',
      barEvent: '',
      barMenu: [
        {
          key: 'asc',
          name: intl.formatMessage(message.Asc),
          className: 'iconAsc'
        }, {
          key: 'desc',
          name: intl.formatMessage(message.Desc),
          className: 'iconDesc'
        }
      ]
    },
    cellSetting: {
      barTitle: intl.formatMessage(message.CellSet),
      barIcon: 'iconCellSetting',
      barEvent: 'cellSetting'
    },
    rowCol: {
      barTitle: intl.formatMessage(message.RowCol),
      barIcon: 'iconRowCol',
      barEvent: '',
      barMenu: [
        {
          key: 'RowHeight',
          name: intl.formatMessage(message.LineHeight),
          className: 'iconRowHeight'
        }, {
          key: 'autoFitRow',
          name: intl.formatMessage(message.FitLineHeight),
          className: 'iconAutoFitColumn'
        }, {
          key: 'ColumnWidth',
          name: intl.formatMessage(message.Width),
          className: 'iconColWidth'
        }, {
          key: 'autoFitColumn',
          name: intl.formatMessage(message.FitWidth),
          className: 'iconAutoFitRow'
        }, {
          key: 'insert',
          name: intl.formatMessage(message.Insert),
          className: 'iconInsert',
          children: [
            {
              key: 'insertRow',
              name: intl.formatMessage(message.InsetRow),
              className: 'iconInsertRow'
            }, {
              key: 'insertColumn',
              name: intl.formatMessage(message.InsetCol),
              className: 'iconInsertCol'
            }
          ]
        },
        {
          key: 'delete',
          name: intl.formatMessage(message.Delete),
          className: 'iconDelete',
          children: [
            {
              key: 'deleteRows',
              name: intl.formatMessage(message.DelRow),
              className: 'iconDeleteRow'
            }, {
              key: 'deleteColumns',
              name: intl.formatMessage(message.DelCol),
              className: 'iconDeleteCol'
            }
          ]
        }
      ]
    },
    frozenPane: {
      barTitle: intl.formatMessage(message.FrozenPane),
      barIcon: 'iconFrozenPane',
      barEvent: 'frozenPaneCheck',
      barMenu: [
        {
          key: 'cancelFrozenPane',
          name: intl.formatMessage(message.CancelFrozen),
          className: 'iconCancelFrozen',
          displayProps: 'cancelFrozenPane'
        },
        {
          key: 'curRowCol',
          name: intl.formatMessage(message.FrozenCell),
          className: 'iconCancelFrozen',
          displayProps: 'showFrozenPane'
        },
        {
          key: 'firstRow',
          name: intl.formatMessage(message.FrozenFirstRow),
          className: 'iconFrozenFirstRow',
          displayProps: 'showRowFrozen'
        },
        {
          key: 'firstColumn',
          name: intl.formatMessage(message.FrozenFirstCol),
          className: 'iconFrozenFirstCol',
          displayProps: 'showColFrozen'
        },
        {
          key: 'curRowColTrailing',
          name: intl.formatMessage(message.FrozenTrailingCell),
          className: 'iconCancelFrozen'
          // displayProps: 'showFrozenPane'
        }
      ]
    },
    analyze: {
      barTitle: intl.formatMessage(message.Analyze),
      barIcon: 'iconAnalyze',
      barEvent: '',
      barMenu: [
        {
          key: 'analyze',
          name: intl.formatMessage(message.Solve),
          className: 'iconFx'
        }
      ]
    },
    paste: {
      barTitle: intl.formatMessage(message.Paste),
      barIcon: 'iconPaste',
      disabledProps: 'canPaste',
      barEvent: '',
      barMenu: [
        {
          key: 'all',
          name: intl.formatMessage(message.AllPaste),
          className: ''
        }, {
          key: 'formulas',
          name: intl.formatMessage(message.Formulas),
          className: ''
        }, {
          key: 'values',
          name: intl.formatMessage(message.Value),
          className: ''
        }, {
          key: 'formatting',
          name: intl.formatMessage(message.Format),
          className: ''
        }
      ]
    },
    brush: {
      barTitle: intl.formatMessage(message.Brush),
      barIcon: 'iconBrush',
      barEvent: 'brush',
      barDoubleClickEvent: 'doubleClickBrush',
      selectedProps: 'canBrush'
    },
    clear: {
      barMenu: [
        {
          key: 'clearAll',
          name: intl.formatMessage(message.All),
          className: ''
        },
        {
          key: 'clearFormat',
          name: intl.formatMessage(message.Format),
          className: ''
        },
        {
          key: 'clearContent',
          name: intl.formatMessage(message.Content)
        }
      ]
    },
    open: {
      barTitle: intl.formatMessage(message.Open),
      barIcon: 'folder-open',
      barIconStyle: 'iconAlign',
      isSupIcon: true,
      barEvent: 'onOpen',
      btnStyle: { paddingTop: 5 }
    },
    export: {
      barTitle: intl.formatMessage(message.Export),
      barIcon: 'download',
      barIconStyle: 'iconAlign',
      isSupIcon: true,
      barEvent: 'onExport',
      btnStyle: { paddingTop: 5 }
    },
    print: {
      barTitle: intl.formatMessage(message.Print),
      barIcon: 'iconPrint',
      barIconStyle: 'iconAlign',
      barEvent: 'onPrint',
      btnStyle: { paddingTop: 5 }
    },
    save: {
      barTitle: intl.formatMessage(message.Save),
      barIcon: 'save',
      isSupIcon: true,
      barIconStyle: 'iconAlign',
      barEvent: 'onSave'
    },
    undo: {
      barTitle: intl.formatMessage(message.Undo),
      barIcon: 'undo',
      useAwesomeIcon: true,
      isSupIcon: true,
      barEvent: 'undo',
      disabledState: 'undo'
    },
    redo: {
      barTitle: intl.formatMessage(message.Redo),
      barIcon: 'redo',
      useAwesomeIcon: true,
      isSupIcon: true,
      barEvent: 'redo',
      disabledState: 'redo'
    },
    cut: {
      barTitle: intl.formatMessage(message.Cut),
      barIcon: 'iconCut',
      barIconStyle: 'iconSmall',
      barEvent: 'cut',
      useSpan: true
    },
    copy: {
      barTitle: intl.formatMessage(message.Copy),
      barIcon: 'iconCopy',
      barIconStyle: 'iconSmall',
      barEvent: 'copy',
      useSpan: true
    },
    pieChart: {
      barTitle: intl.formatMessage(message.PieChart),
      barIcon: 'iconPieChart',
      barIconStyle: 'iconAlign',
      barEvent: 'addChart',
      eventProp: 'pie',
      btnStyle: { paddingTop: 5 }
    },
    lineChart: {
      barTitle: intl.formatMessage(message.LineChar),
      barIcon: 'iconLineChart',
      barIconStyle: 'iconAlign',
      barEvent: 'addChart',
      eventProp: 'line',
      btnStyle: { paddingTop: 5 }
    },
    columnChart: {
      barTitle: intl.formatMessage(message.ColumnChart),
      barIcon: 'iconColumnChart',
      barIconStyle: 'iconAlign',
      barEvent: 'addChart',
      eventProp: 'columnClustered',
      btnStyle: { paddingTop: 5 }
    },
    barClustered: {
      barTitle: '条形图',
      barIcon: 'iconBarChart',
      barIconStyle: 'iconAlign',
      barEvent: 'addChart',
      eventProp: 'barClustered',
      btnStyle: { paddingTop: 5 }
    },
    bold: {
      barTitle: intl.formatMessage(message.Bold),
      barIcon: 'fa-bold',
      useAwesomeIcon: true,
      selectedState: 'fontWeight',
      defaultState: 'normal',
      barEvent: 'setFontStyle',
      eventProp: 'font-weight'
    },
    italic: {
      barTitle: intl.formatMessage(message.Italic),
      barIcon: 'fa-italic',
      useAwesomeIcon: true,
      selectedState: 'fontStyle',
      defaultState: 'normal',
      barEvent: 'setFontStyle',
      eventProp: 'font-style'
    },
    underline: {
      barTitle: intl.formatMessage(message.Underline),
      barIcon: 'fa-underline',
      useAwesomeIcon: true,
      selectedState: 'underline',
      barEvent: 'textDecoration'
    },
    vAlignTop: {
      barTitle: intl.formatMessage(message.TopAlign),
      barIcon: 'iconTopAlign',
      barIconStyle: 'iconAlign',
      selectedState: 'vAlign',
      defaultState: 'top'
    },
    vAlignCenter: {
      barTitle: intl.formatMessage(message.MiddelAlign),
      barIcon: 'iconMiddleAlign',
      barIconStyle: 'iconAlign',
      selectedState: 'vAlign',
      defaultState: 'center'
    },
    vAlignBottom: {
      barTitle: intl.formatMessage(message.BottomAlign),
      barIcon: 'iconBottomAlign',
      barIconStyle: 'iconAlign',
      selectedState: 'vAlign',
      defaultState: 'bottom'
    },
    hAlignLeft: {
      barTitle: intl.formatMessage(message.LeftAlign),
      barIcon: 'iconLeftAlign',
      barIconStyle: 'iconAlign',
      selectedState: 'hAlign',
      defaultState: 'left'
    },
    hAlignCenter: {
      barTitle: intl.formatMessage(message.CenterAlign),
      barIcon: 'iconCenterAlign',
      barIconStyle: 'iconAlign',
      selectedState: 'hAlign',
      defaultState: 'center'
    },
    hAlignRight: {
      barTitle: intl.formatMessage(message.RightAlign),
      barIcon: 'iconRightAlign',
      barIconStyle: 'iconAlign',
      selectedState: 'hAlign',
      defaultState: 'right'
    },
    sizePlus: {
      barTitle: intl.formatMessage(message.FontSizePlus),
      barIcon: 'iconFontSizePlus',
      barIconStyle: 'iconMini',
      barEvent: 'changeStyles',
      eventProp: 'up',
      btnStyle: { padding: '0 5px' }
    },
    sizeMinus: {
      barTitle: intl.formatMessage(message.FontSizeMinus),
      barIcon: 'iconFontSizeMinus',
      barIconStyle: 'iconMini',
      barEvent: 'changeStyles',
      eventProp: 'down',
      btnStyle: { padding: '0 5px' }
    },
    bgColor: {
      barTitle: intl.formatMessage(message.FillColor),
      barIcon: 'iconBgColor',
      eventProp: 'backColor',
      barState: 'backColor'
    },
    fontColor: {
      barTitle: intl.formatMessage(message.FontColor),
      barIcon: 'iconFontColor',
      eventProp: 'foreColor',
      barState: 'fontColor'
    },
    row: {
      barIcon: 'iconRow',
      barTitle: intl.formatMessage(message.Row),
      barMenu: [
        {
          key: 'insertRow',
          name: intl.formatMessage(message.InsetRow),
          className: 'iconInsertRow'
        }, {
          key: 'deleteRows',
          name: intl.formatMessage(message.DelRow),
          className: 'iconDeleteRow'
        }
      ]
    },
    col: {
      barIcon: 'iconCol',
      barTitle: intl.formatMessage(message.Col),
      barMenu: [
        {
          key: 'insertColumn',
          name: intl.formatMessage(message.InsetCol),
          className: 'iconInsertCol'
        }, {
          key: 'deleteColumns',
          name: intl.formatMessage(message.DelCol),
          className: 'iconDeleteCol'
        }
      ]
    },
    insertPicture: {
      barIcon: 'iconPicture',
      barTitle: intl.formatMessage(message.Image),
      barEvent: 'onOpen',
      eventProp: 'picture'
    },
    bcQRCode: {
      barIcon: 'iconQrCode',
      barTitle: intl.formatMessage(message.qrBarCode),
      barEvent: 'onOpenQrBarCode',
      barTip: intl.formatMessage(message.qrBarCodeTip)
    },
    bcEan13: {
      barIcon: 'iconBarCode',
      barTitle: intl.formatMessage(message.barCode),
      barEvent: 'onOpenBarCode',
      barTip: intl.formatMessage(message.barCodeTip)
    },
    insertChart: {
      barIcon: 'iconChart',
      barTitle: intl.formatMessage(message.Chart),
      barEvent: 'onOpenChartModal'
    },
    insertDateSource: {
      barIcon: 'iconDataSource',
      barTitle: intl.formatMessage(message.DataSource),
      barEvent: 'unfoldDataSourceSet'
    },
    openFile: {
      barTitle: intl.formatMessage(message.Open),
      barIcon: 'openFileIcon',
      barEvent: 'onOpen'
    },
    saveFile: {
      barTitle: intl.formatMessage(message.Save),
      barIcon: 'saveFileIcon',
      barEvent: 'onSave'
    },
    exportFile: {
      barTitle: intl.formatMessage(message.Export),
      barIcon: 'exportFileIcon',
      barEvent: 'onExport'
    },
    exit: {
      barTitle: intl.formatMessage(message.Quit),
      barIcon: 'exitIcon',
      barEvent: 'onExit'
    },
    undoBig: {
      barTitle: intl.formatMessage(message.Undo),
      barIcon: 'fa-reply fa-2x',
      useAwesomeIcon: true,
      barEvent: 'undo',
      disabledState: 'undo'
    },
    redoBig: {
      barTitle: intl.formatMessage(message.Redo),
      barIcon: 'fa-share fa-2x',
      useAwesomeIcon: true,
      barEvent: 'redo',
      disabledState: 'redo'
    },
    cutBig: {
      barTitle: intl.formatMessage(message.Cut),
      barIcon: 'iconCutBig',
      barEvent: 'cut',
      useSpan: true
    },
    copyBig: {
      barTitle: intl.formatMessage(message.Copy),
      barIcon: 'iconCopyBig',
      barEvent: 'copy',
      useSpan: true
    },
    printBig: {
      barTitle: intl.formatMessage(message.Print),
      barIcon: 'iconPrintBig',
      barEvent: 'onPrint',
      useSpan: true
    }
  };
};

/**
  * @description 根据格式的不同类型显示不同的title、提示语
  * @author liyongshuai
  * @param {string} type
  * @returns {object}
  */
const formatTitleTips = (type, intl) => {
  let title = '';
  let tips = '';
  switch (type) {
    case 'greaterThan':
      title = intl.formatMessage(message.Than);
      tips = intl.formatMessage(message.TitleTips1);
      break;
    case 'smaller':
      title = intl.formatMessage(message.Less);
      tips = intl.formatMessage(message.TitleTips2);
      break;
    case 'between':
      title = intl.formatMessage(message.Between);
      tips = intl.formatMessage(message.TitleTips3);
      break;
    case 'equal':
      title = intl.formatMessage(message.Equal);
      tips = intl.formatMessage(message.TitleTips4);
      break;
    case 'contains':
      title = intl.formatMessage(message.Includes);
      tips = intl.formatMessage(message.TitleTips5);
      break;
    case 'date':
      title = intl.formatMessage(message.Date);
      tips = intl.formatMessage(message.TitleTips6);
      break;
    case 'duplicate':
      title = intl.formatMessage(message.Duplicate);
      tips = intl.formatMessage(message.TitleTips7);
      break;
    case 'maxTen':
      title = intl.formatMessage(message.maxTenItem);
      tips = intl.formatMessage(message.TitleTips8);
      break;
    case 'maxTenPct':
      title = intl.formatMessage(message.PerTenMax);
      tips = intl.formatMessage(message.TitleTips9);
      break;
    case 'minTen':
      title = intl.formatMessage(message.minTenItem);
      tips = intl.formatMessage(message.TitleTips10);
      break;
    case 'minTenPct':
      title = intl.formatMessage(message.PerTenMin);
      tips = intl.formatMessage(message.TitleTips11);
      break;
    case 'aboveAverage':
      title = intl.formatMessage(message.AboveAvg);
      tips = intl.formatMessage(message.TitleTips12);
      break;
    case 'belowAverage':
      title = intl.formatMessage(message.UnderAvg);
      tips = intl.formatMessage(message.TitleTips13);
      break;
    default:
      break;
  }
  return { title, tips };
};

const cellSettings = function (intl) {
  return {
    top: {
      icon: 'iconTopBorder'
    },
    horizontal: {
      icon: 'iconInnerHorizontal'
    },
    bottom: {
      icon: 'iconBottomBorder'
    },
    up: {
      icon: 'iconDiagonalUp'
    },
    left: {
      icon: 'iconLeftBorder'
    },
    vertical: {
      icon: 'iconInnerVertical'
    },
    right: {
      icon: 'iconRightBorder'
    },
    down: {
      icon: 'iconDiagonalDown'
    },
    none: {
      icon: 'iconNone',
      spanContent: intl.formatMessage(message.Null),
      action: 'none'
    },
    all: {
      icon: 'iconAll',
      spanContent: intl.formatMessage(message.OutBorder),
      action: 'all'
    },
    inside: {
      icon: 'iconInside',
      spanContent: intl.formatMessage(message.Inside),
      action: 'inside'
    }
  };
};

export { reportBar, formatTitleTips, cellSettings };
