import kotlin.math.abs

const val BLACK_PIECE = 'B'
const val WHITE_PIECE = 'W'
const val NO_PIECE = ' '
const val BOARD_SIZE = 8

const val VALID_INPUT = "[a-h][1-8][a-h][1-8]"

fun main() {
    println("Pawns-Only Chess")

    println("First Player's name:")
    val firstPlayer = readLine()!!

    println("Second Player's name:")
    val secondPlayer = readLine()!!

    val player1 = Player(firstPlayer, WHITE_PIECE)
    val player2 = Player(secondPlayer, BLACK_PIECE)

    val board = Board()
    board.print()

    var activePlayer = player1
    val gameState = GameState(null)

    while (true) {
        println("${activePlayer.name}'s turn:")

        val input = readLine()!!
        if (input == "exit") {
            println("Bye!")
            return
        } else if (isInvalid(input)) {
            println("Invalid Input")
            continue
        }

        // determine from - to field
        // e.g. when input = c6c7 -> from = c6 and to = c7
        val from = Field(input.substring(0, 2))
        val to = Field(input.substring(2))

        // validate fields
        // 'from' field needs to have a piece of active player
        if (board.getPiece(from) != activePlayer.piece) {
            println("No ${activePlayer.color} pawn at $from")
            continue
        }

        try {
            executeMove(board, from, to, activePlayer, gameState)
        } catch (ex: InvalidInput) {
            println("Invalid Input")
            continue
        }

        board.print()

        // check for winning conditions
        if (hasPlayerWon(board, activePlayer)) {
            println("${activePlayer.colorCapitalized} wins!")
            println("Bye!")
            return
        }

        // continue with other player
        activePlayer = if (activePlayer == player1) player2 else player1

        if (hasStalemate(board, activePlayer)) {
            println("Stalemate!")
            println("Bye!")
            return
        }
    }
}

fun hasPlayerWon(board: Board, player: Player): Boolean {
    return board.hasPieceOnWinningRowFor(player) ||
            board.countPieces(player.opposingColor) == 0
}

fun hasStalemate(board: Board, player: Player): Boolean {
    val pieces = board.getFieldsWithPiecesFor(player)
    val possibleMoves = board.createPossibleMoves(pieces, player)
//        println("possible moves for ${player.color}: $possibleMoves")
    return possibleMoves.isEmpty()
}

// small helper class to encapsulate a field (e.g. "b5")
// it provides properties for rowNumber (1-8), column (a-h), rowIndex (0-7) and columnIndex (0-7)
class Field(val coord: String) {
    // returns 'a' -' h'
    val column get() = coord[0]

    // return 1 - 8
    val rowNumber get() = coord[1].toString().toInt()

    // return: index of row, "d1" -> row number 1 -> rowIndex 0
    // rowIndex is rowNumber - 1
    val rowIndex get() = rowNumber - 1

    // input: field on the board in form "a3" or "g5"
    // return: index of column, "a4" -> column 'a' -> colIndex 0
    val colIndex get() = column.code - 97 // a = 97 in ASCII

    override fun toString(): String {
        return coord
    }

    // build a neighbor field, direction is given by the Pair<Int, Int>
    // first is giving the row increment (-1, 0 or 1), second the column increment (-1, 0 or 1)
    // return a Field (something like "c6")
    fun getNeighbor(direction: Pair<Int, Int>): Field {
        val newRow = rowNumber + direction.first
        val newCol = (colIndex + direction.second + 97).toChar().toString()
        return Field("" + newCol + newRow)
    }
}

// we have to remember if in the previous move the opponent did a move from his start row
// because then we have the opportunity to execute an en passant move
class GameState(var enPassantBeatablePiece: Field?)

class Player(val name: String, val piece: Char) {
    val opposingColor get() = if (piece == WHITE_PIECE) BLACK_PIECE else WHITE_PIECE

    val color get() = if (piece == WHITE_PIECE) "white" else "black"

    val colorCapitalized get() = color.replaceFirstChar { it.uppercaseChar() }

    // startRowNumber is 2 for white and 7 for black, we need to know this to check if player can move 2 fields ahead
    val startRowNumber get() = if (piece == WHITE_PIECE) 2 else 7

    // winningRowNumber is 8 for white and 1 for black. if player reaches this row he has won
    val winningRowNumber get() = if (piece == WHITE_PIECE) 8 else 1

    // rowIncr is +1 for white (because white moves "up"   the board towards increasing row numbers: 1, 2, ..., 8)
    //        and -1 for black (because black moves "down" the board towards decreasing row numbers: 8, 7, ..., 1)
    val rowIncr get() = if (piece == WHITE_PIECE) 1 else -1

}

// class to wrap our board
// the board is an Array of Arrays of Chars
// we build from bottom to top, that means rowIndex is rowNumber - 1
class Board {
    private val board = arrayOf(
        Array(BOARD_SIZE) { NO_PIECE },
        Array(BOARD_SIZE) { WHITE_PIECE },
        Array(BOARD_SIZE) { NO_PIECE },
        Array(BOARD_SIZE) { NO_PIECE },
        Array(BOARD_SIZE) { NO_PIECE },
        Array(BOARD_SIZE) { NO_PIECE },
        Array(BOARD_SIZE) { BLACK_PIECE },
        Array(BOARD_SIZE) { NO_PIECE }
    )

    fun getPiece(field: Field): Char {
        return board[field.rowIndex][field.colIndex]
    }

    private fun setPiece(field: Field, newPiece: Char) {
        board[field.rowIndex][field.colIndex] = newPiece
    }

    fun isEmpty(field: Field): Boolean {
        return getPiece(field) == NO_PIECE
    }

    fun movePiece(piece: Char, from: Field, to: Field) {
        removePiece(from)
        setPiece(to, piece)
    }

    fun removePiece(field: Field) {
        setPiece(field, NO_PIECE)
    }

    fun hasPieceOnWinningRowFor(player: Player): Boolean {
        return board[player.winningRowNumber - 1].any { it == player.piece }
    }

    fun countPieces(color: Char): Int {
        return board.sumOf { chars -> chars.count { it == color } }
    }

    fun print() {
        val dividerLine = "  +---+---+---+---+---+---+---+---+"

        println(dividerLine)

        for (rowNumber in BOARD_SIZE downTo 1) {
            val row = board[rowNumber - 1] // rowIndex = rowNumber - 1
            println("$rowNumber | " + row.joinToString(" | ") + " |")
            println(dividerLine)
        }

        println("    a   b   c   d   e   f   g   h")
    }

    fun getFieldsWithPiecesFor(player: Player): List<Field> {
        return board.withIndex().flatMap { indexedRow ->
            board[indexedRow.index].withIndex()
                .filter { indexedCol -> indexedCol.value == player.piece }
                .map {  indexedValue ->
                    val col = (indexedValue.index + 97).toChar()
                    "" + col + (indexedRow.index + 1)
                }
        }
            .sorted()
            .map { s -> Field(s) }
    }

    fun createPossibleMoves(fields: List<Field>, player: Player): List<String> {
        return fields.flatMap { f -> createPossibleMoves(f, player) }.sorted()
    }

    // for given field build a list of all possible moves
    // - if in starting row: two fields ahead (if target field is empty)
    // - one field ahead (if target is empty)
    // - one field diagonal to the left (if target field has opponents piece)
    // - one field diagonal to the right (if target field has opponents piece)
    //
    // player is needed because we must know if we go up or down the rows,
    // whether we are on the starting row (then we can move 2 fields) and
    // what the opposing color is (to check if a diagonal move is possible)
    private fun createPossibleMoves(field: Field, player: Player): List<String> {
        val possibleFields = mutableListOf<String>()

        val straightAhead = field.getNeighbor(Pair(player.rowIncr, 0))
        if (isEmpty(straightAhead)) {
            possibleFields.add(field.coord + straightAhead.coord)
        }

        // field to the left, meaning to 'smaller' letter
        val diagonalLeftColumn = field.getNeighbor(Pair(player.rowIncr, -1))
        if (field.colIndex > 0 && getPiece(diagonalLeftColumn) == player.opposingColor) {
            possibleFields.add(field.coord + diagonalLeftColumn.coord)
        }

        // field to the right, meaning to 'higher' letters
        val diagonalRightColumn = field.getNeighbor(Pair(player.rowIncr, 1))
        if (field.colIndex < 7 && getPiece(diagonalRightColumn) == player.opposingColor) {
            possibleFields.add(field.coord + diagonalRightColumn.coord)
        }

        // if field is in 'startRowNumber', player can move 2 fields ahead (if target field is empty)
        val twoFieldsAhead = field.getNeighbor(Pair(2 * player.rowIncr, 0))
        if (field.rowNumber == player.startRowNumber && isEmpty(twoFieldsAhead)) {
            possibleFields.add(field.coord + twoFieldsAhead.coord)
        }
        return possibleFields
    }
}

// board will be updated when move was valid
fun executeMove(board: Board, from: Field, to: Field, player: Player, gameState: GameState) {
    val isStraightMove = from.column == to.column
    val isDiagonalMove = abs(from.colIndex - to.colIndex) == 1

    val isTargetFieldEmpty = board.isEmpty(to)
    val isTargetFieldOpposingColor = board.getPiece(to) == player.opposingColor

    // can move from its start row 2 fields, straight ahead, into empty field
    if (to.rowNumber == player.startRowNumber + 2 * player.rowIncr && isStraightMove && isTargetFieldEmpty) {
        board.movePiece(player.piece, from, to)

        // opponent can take this piece in next round
        gameState.enPassantBeatablePiece = to
        return
    }

    // can only move one field straight ahead, into empty field
    if (to.rowNumber == from.rowNumber + player.rowIncr && isStraightMove && isTargetFieldEmpty) {
        board.movePiece(player.piece, from, to)

        gameState.enPassantBeatablePiece = null // chance for en passant is gone!
        return
    }

    // can move one field and capture diagonal opponent piece
    if (to.rowNumber == from.rowNumber + player.rowIncr && isDiagonalMove && isTargetFieldOpposingColor) {
        board.movePiece(player.piece, from, to)

        gameState.enPassantBeatablePiece = null // chance for en passant is gone!
        return
    }

    // en passant
    if (to.rowNumber == from.rowNumber + player.rowIncr && isDiagonalMove && isTargetFieldEmpty &&
        to.column == gameState.enPassantBeatablePiece?.column) {
        board.movePiece(player.piece, from, to)

        board.removePiece(gameState.enPassantBeatablePiece!!)
        gameState.enPassantBeatablePiece = null
        return
    }

    // everything else is not allowed
    throw InvalidInput()
}

fun isInvalid(input: String): Boolean {
    return !VALID_INPUT.toRegex().matches(input)
}

class InvalidInput: IllegalArgumentException()