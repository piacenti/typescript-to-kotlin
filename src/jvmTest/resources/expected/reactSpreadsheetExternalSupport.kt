/** A two-dimensional array of given type T in rows and columns */
typealias Matrix<T> = Array<Array<T?>>
/** The spreadsheet's write mode */
@Suppress("NAME_CONTAINS_ILLEGAL_CHARS")
// language=JavaScript
@JsName("""(/*union*/{view: 'view',edit: 'edit'}/*union*/)""")
enum class Mode { view , edit}
/** Function for getting the cells the cell's value is bound to */
typealias GetBindingsForCell<Cell : CellBase<*> > = (cell: Cell, `data`: Matrix<Cell>) -> Array<Point>
/** Type of the Spreadsheet Cell component */
typealias CellComponent<Cell : CellBase<*> > = React.ComponentType<CellComponentProps<Cell>>
/** Type of the Spreadsheet DataViewer component */
typealias DataViewerComponent<Cell : CellBase<*> > = React.ComponentType<DataViewerProps<Cell>>
/** Type of the Spreadsheet DataEditor component */
typealias DataEditorComponent<Cell : CellBase<*> > = React.ComponentType<DataEditorProps<Cell>>
/** Type of the Spreadsheet Table component */
typealias TableComponent = React.ComponentType<TableProps>
/** Type of the Row component */
typealias RowComponent = React.ComponentType<RowProps>
/** Type of the HeaderRow component */
typealias HeaderRowComponent = React.ComponentType<HeaderRowProps>
/** Type of the RowIndicator component */
typealias RowIndicatorComponent = React.ComponentType<RowIndicatorProps>
/** Type of the ColumnIndicator component */
typealias ColumnIndicatorComponent = React.ComponentType<ColumnIndicatorProps>
/** Type of the CornerIndicator component */
typealias CornerIndicatorComponent = React.ComponentType<CornerIndicatorProps>
/** Any = String | Boolean | Number*/
typealias FormulaParseResult = Any
typealias FormulaParseError = String
