import GC from '@grapecity/spread-sheets';
// import GC from 'root/dependencies/spreadjs/gc.spread.sheets.all.12.0.7.min.js';

const spreadNS = GC.Spread.Sheets;

// Cell Type
function TagTriangleCell() {
  // spreadNS.CellTypes.Text.apply(this, arguments);
  this.typeName = 'TagTriangleCell';
}
TagTriangleCell.prototype = new spreadNS.CellTypes.Text();
const oldPaint = spreadNS.CellTypes.Text.prototype.paint;
TagTriangleCell.prototype.paint = function (ctx, value, x, y, w, h, style, context) {
  if (!ctx) {
    return;
  }

  const triangleSide = 10;
  const tranY = (y + h) - triangleSide;
  ctx.save();
  ctx.beginPath();
  ctx.lineWidth = 1;
  ctx.moveTo(x, tranY);
  ctx.lineTo(x, (y + h) - 1);
  ctx.lineTo(x + triangleSide, (y + h) - 1);
  ctx.fillStyle = '#ff9900';
  ctx.fill();
  ctx.restore();

  oldPaint.apply(this, [ctx, value, x, y, w, h, style, context]);
};

/**
 * cell内鼠标移动触发
 */
// TagTriangleCell.prototype.getHitInfo = function (x, y, cellStyle, cellRect, context) {
//   console.log("getHitInfo")
//   var xm = cellRect.x + cellRect.width / 2,
//           ym = cellRect.y + cellRect.height / 2,
//           size = 10;
//   var info = { x: x, y: y, row: context.row, col: context.col, cellRect: cellRect, sheetArea: context.sheetArea };
//   if (xm - size <= x && x <= xm + size && ym - size <= y && y <= ym + size) {
//       info.isReservedLocation = true;
//   }
//   return info;
// };

/**
 * cell内MouseUp触发
 */
// TagTriangleCell.prototype.processMouseUp = function (hitInfo) {
//   console.log("processMouseUp")
//   var sheet = hitInfo.sheet;
//   if (sheet) {
//       var row = hitInfo.row, col = hitInfo.col, sheetArea = hitInfo.sheetArea;
//       var newValue = sheet.getValue(row, col, sheetArea);
//       var spread = sheet.getParent();
//       console.log("newVal: ", newValue)
//       sheet.startEdit(true, newValue);
//       spread.commandManager().execute({cmd: "editCell", sheetName: sheet.name(), row: row, col: col, newValue: newValue});
//       return true;
//   }
//   return false;
// };

export default TagTriangleCell;
