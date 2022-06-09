@file:JsModule("react-spreadsheet")
@file:Suppress("unused", "PropertyName", "FunctionName")

/** A cell coordinates in the spreadsheet */
external interface Point  {
    /** The cell's column */
    var column: Number
    /** The cell's row */
    var row: Number
}

/**
 * Creates an empty matrix with given rows and columns
 * @param rows - integer, the amount of rows the matrix should have
 * @param columns - integer, the amount of columns the matrix should have
 * @returns an empty matrix with given rows and columns
 */
external fun <T> createEmpty(rows: Number, columns: Number): Matrix<T>

/** The base type of cell data in Spreadsheet */
external interface CellBase<Value >  {
    /** Whether the cell should not be editable */
    var readOnly: Boolean?
    /** Class to be given for the cell element */
    var className: String?
    /** The value of the cell */
    var value: Value
    /** Custom component to render when the cell is edited, if not defined would default to the component defined for the Spreadsheet */
    var DataEditor: DataEditorComponent<CellBase<Value>>?
    /** Custom component to render when the cell is viewed, if not defined would default to the component defined for the Spreadsheet */
    var DataViewer: DataViewerComponent<CellBase<Value>>?
}
/**
 * A cell with it's coordinates
 * @deprecated the component does not use cell descriptors anymore. Instead it passes cell point and cell value explicitly.
 */
external interface CellDescriptor<Cell> :Point {
    /** The cell's data */
    var `data`: Cell?
}  
/** Dimensions of an element */
external interface Dimensions  {
    /** The element's width in pixels */
    var width: Number
    /** The element's height in pixels */
    var height: Number
    /** The distance of the element from it's container top border in pixels */
    var top: Number
    /** The distance of the element from it's container left border in pixels */
    var left: Number
}
external interface CellChange<Cell : CellBase<*> >  {
    var prevCell: Cell?
    var nextCell: Cell?
}
/** Type of Spreadsheet Cell component props */
external interface CellComponentProps<Cell : CellBase<*> >  {
    /** The row of the cell */
    var row: Number
    /** The column of the cell */
    var column: Number
    /** The DataViewer component to be used by the cell */
    var DataViewer: DataViewerComponent<Cell>
    /** The FormulaParser instance to be used by the cell */
    var formulaParser: Parser
    /** Whether the cell is selected */
    var selected: Boolean
    /** Whether the cell is active */
    var active: Boolean
    /** Whether the cell is copied */
    var copied: Boolean
    /** Whether the user is dragging */
    var dragging: Boolean
    /** The mode of the cell */
    var mode: Mode
    /** The data of the cell */
    var `data`: Cell?
    /** Select the cell at the given point */
    var select: (point: Point) -> Unit
    /** Activate the cell at the given point */
    var activate: (point: Point) -> Unit
    /** Set the dimensions of the cell at the given point with the given dimensions */
    var setCellDimensions: (point: Point, dimensions: Dimensions) -> Unit
    /**
     * Calculate which cells should be updated when given cell updates.
     * Defaults to: internal implementation which infers dependencies according to formulas.
     */
    var getBindingsForCell: GetBindingsForCell<Cell>
    /** Set data of the cell */
    var setCellData: (cell: Cell) -> Unit
}
external interface DataComponentProps<Cell : CellBase<*>> :Point {
    /** The rendered cell by the component */
    var cell: Cell?
}  
/** Type of the Spreadsheet DataViewer component props */
external interface DataViewerProps<Cell : CellBase<*> > :DataComponentProps<Cell>   {
    /** Instance of `FormulaParser` */
    var formulaParser: Parser
    /** Set data of the cell */
    var setCellData: (cell: Cell) -> Unit
}
/** Type of the Spreadsheet DataEditor component props */
external interface DataEditorProps<Cell : CellBase<*> > :DataComponentProps<Cell>   {
    /** Callback to be called when the cell's value is changed */
    var onChange: (cell: Cell) -> Unit
    /** Callback to be called when edit mode should be exited */
    var exitEditMode: () -> Unit
}
/** Type of the Spreadsheet Table component props */
external interface TableProps : React.PropsWithChildren{
    /** Numebr of columns the table should render */
    var columns: Number
    /** Whether column indicators are hidden */
    var hideColumnIndicators: Boolean?
}
/** Type of the Spreadsheet Row component props */
external interface RowProps : React.PropsWithChildren{
    /** The row index of the table */
    var row: Number
}
/** Type of the Spreadsheet HeaderRow component props */
external interface HeaderRowProps : React.PropsWithChildren{}
/** Type of the Spreadsheet RowIndicator component props */
external interface RowIndicatorProps  {
    /** The row the indicator indicates */
    var row: Number
    /** A custom label for the indicator as provided in rowLabels */
    var label: React.ReactNode?
    /** Whether the entire row is selected */
    var selected: Boolean
    /** Callback to be called when the row is selected */
    var onSelect: (row: Number, extend: Boolean) -> Unit
}
/** Type of the Spreadsheet ColumnIndicator component props */
external interface ColumnIndicatorProps  {
    /** The column the indicator indicates */
    var column: Number
    /** A custom label for the indicator as provided in columnLabels */
    var label: React.ReactNode?
    /** Whether the entire column in selected */
    var selected: Boolean
    /** Callback to be called when the column is selected */
    var onSelect: (column: Number, extend: Boolean) -> Unit
}
/** Type of the Spreadsheet CornerIndicator component props */
external interface CornerIndicatorProps  {
    /** Whether the entire table is selected */
    var selected: Boolean
    /** Callback to select the entire table */
    var onSelect: () -> Unit
}

/** The Spreadsheet component props */
external interface Props<CellType : CellBase<*>>  {
    /** The spreadsheet's data */
    var `data`: Matrix<CellType>
    /** Class to be added to the spreadsheet element */
    var className: String?
    /** Use dark colors that complenent dark mode */
    var darkMode: Boolean?
    /**
     * Instance of `FormulaParser` to be used by the Spreadsheet.
     * Defaults to: internal instance created by the component.
     */
    var formulaParser: Parser?
    /**
     * Labels to use in column indicators.
     * Defaults to: alphabetical labels.
     */
    var columnLabels: Array<String>?
    /**
     * Labels to use in row indicators.
     * Defaults to: row index labels.
     */
    var rowLabels: Array<String>?
    /**
     * If set to true, hides the row indicators of the spreadsheet.
     * Defaults to: `false`.
     */
    var hideRowIndicators: Boolean?
    /**
     * If set to true, hides the column indicators of the spreadsheet.
     * Defaults to: `false`.
     */
    var hideColumnIndicators: Boolean?
    /** Component rendered above each column. */
    var ColumnIndicator: ColumnIndicatorComponent?
    /** Component rendered in the corner of row and column indicators. */
    var CornerIndicator: CornerIndicatorComponent?
    /** Component rendered next to each row. */
    var RowIndicator: RowIndicatorComponent?
    /** The Spreadsheet's table component. */
    var Table: TableComponent?
    /** The Spreadsheet's row component. */
    var Row: RowComponent?
    /** The spreadsheet's header row component */
    var HeaderRow: HeaderRowComponent?
    /** The Spreadsheet's cell component. */
    var Cell: CellComponent<CellType>?
    /** Component rendered for cells in view mode. */
    var DataViewer: DataViewerComponent<CellType>?
    /** Component rendered for cells in edit mode. */
    var DataEditor: DataEditorComponent<CellType>?
    /** Callback called on key down inside the spreadsheet. */
    var onKeyDown: (event: React.KeyboardEvent) -> Unit?
    /**
     * Calculate which cells should be updated when given cell updates.
     * Defaults to: internal implementation which infers dependencies according to formulas.
     */
    var getBindingsForCell: GetBindingsForCell<CellType>?
    /** Callback called when the Spreadsheet's data changes. */
    var onChange: (`data`: Matrix<CellType>) -> Unit?
    /** Callback called when the Spreadsheet's edit mode changes. */
    var onModeChange: (mode: Mode) -> Unit?
    /** Callback called when the Spreadsheet's selection changes. */
    var onSelect: (selected: Array<Point>) -> Unit?
    /** Callback called when Spreadsheet's active cell changes. */
    var onActivate: (active: Point) -> Unit?
    /** Callback called when the Spreadsheet loses focus */
    var onBlur: () -> Unit?
    var onCellCommit: (prevCell: CellType?, nextCell: CellType?, coords: Point?) -> Unit?
}
/**
 * The Spreadsheet component
 */
external fun <CellType : CellBase<Any>> Spreadsheet (props: Props<CellType>) : React.ReactElement

/** The default Spreadsheet DataEditor component */
external val DataEditor: React.FC<DataEditorProps<*>>

/** The default Spreadsheet DataViewer component */
external fun <Cell : CellBase<Value>, Value> DataViewer ( cell:Cell?, formulaParser:Parser  ) : React.ReactElement

/** Get the computed value of a cell. */
/** Any = Value | FormulaParseResult | FormulaParseError | null */
external fun <Cell : CellBase<Value>, Value> getComputedValue( cell:Cell?, formulaParser:hotFormulaParser.Parser  ): Any?
